package pl.tkowalcz.tjahzi.log4j2;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.status.StatusLogger;
import pl.tkowalcz.tjahzi.LoggingSystem;
import pl.tkowalcz.tjahzi.TjahziInitializer;
import pl.tkowalcz.tjahzi.github.GitHubDocs;
import pl.tkowalcz.tjahzi.http.ClientConfiguration;
import pl.tkowalcz.tjahzi.http.HttpClientFactory;
import pl.tkowalcz.tjahzi.http.NettyHttpClient;
import pl.tkowalcz.tjahzi.log4j2.labels.Label;
import pl.tkowalcz.tjahzi.log4j2.labels.LabelFactory;
import pl.tkowalcz.tjahzi.log4j2.labels.LabelsDescriptor;
import pl.tkowalcz.tjahzi.log4j2.labels.StructuredMetadata;
import pl.tkowalcz.tjahzi.stats.LoggingMonitoringModule;
import pl.tkowalcz.tjahzi.stats.MutableMonitoringModuleWrapper;
import pl.tkowalcz.tjahzi.stats.StandardMonitoringModule;

import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.util.Arrays.stream;

/**
 * Builds LokiAppender instances.
 *
 * @param <B> The type to build
 */
public class LokiAppenderBuilder<B extends LokiAppenderBuilder<B>> extends AbstractAppender.Builder<B>
        implements org.apache.logging.log4j.core.util.Builder<LokiAppender> {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private static final int BYTES_IN_MEGABYTE = 1024 * 1024;
    private static final int BYTES_IN_KILOBYTE = 1024;

    @PluginBuilderAttribute
    private String url;

    @PluginBuilderAttribute
    private String logEndpoint;

    @PluginBuilderAttribute
    private String host;

    @PluginBuilderAttribute
    private int port;

    @PluginBuilderAttribute
    private boolean useSSL;

    @PluginBuilderAttribute
    private boolean useDaemonThreads;

    @PluginBuilderAttribute
    private String username;

    @PluginBuilderAttribute(sensitive = true)
    private String password;

    @PluginBuilderAttribute
    private int connectTimeoutMillis = 5000;

    @PluginBuilderAttribute
    private int readTimeoutMillis = 60_000;

    @PluginBuilderAttribute
    private int maxRetries = 3;

    @PluginBuilderAttribute
    private int bufferSizeMegabytes = 32;

    @PluginBuilderAttribute
    private String logLevelLabel;

    @PluginBuilderAttribute
    private long batchSize = 10_2400;

    @PluginBuilderAttribute
    private long batchWait = 5;

    @PluginBuilderAttribute
    private long logShipperWakeupIntervalMillis = 10;

    @PluginBuilderAttribute
    private int shutdownTimeoutSeconds = 10;

    @PluginBuilderAttribute
    private int maxLogLineSizeKilobytes = 10;

    @PluginBuilderAttribute
    private int maxRequestsInFlight = 100;

    @PluginBuilderAttribute
    private boolean verbose;

    @PluginBuilderAttribute
    private String truststorePath;

    @PluginBuilderAttribute(sensitive = true)
    private String truststorePassword;

    @PluginBuilderAttribute
    private String truststoreType;

    @PluginElement("Headers")
    private Header[] headers;

    @PluginElement("Labels")
    private Label[] labels;

    @PluginElement("Metadata")
    private StructuredMetadata[] metadata;

    @Override
    public LokiAppender build() {
        ClientConfiguration configurationBuilder = ClientConfiguration.builder()
                .withUrl(url)
                .withLogEndpoint(logEndpoint)
                .withHost(host)
                .withPort(port)
                .withUseSSL(useSSL)
                .withUsername(username)
                .withPassword(password)
                .withConnectionTimeoutMillis(connectTimeoutMillis)
                .withMaxRetries(maxRetries)
                .withRequestTimeoutMillis(readTimeoutMillis)
                .withMaxRequestsInFlight(maxRequestsInFlight)
                .withTrustStorePath(truststorePath)
                .withTrustStorePassword(truststorePassword)
                .withTrustStoreType(truststoreType)
                .build();

        String[] additionalHeaders = stream(headers)
                .flatMap(header -> Stream.of(header.getName(), header.getValue()))
                .toArray(String[]::new);

        MutableMonitoringModuleWrapper monitoringModuleWrapper = new MutableMonitoringModuleWrapper();
        if (verbose) {
            monitoringModuleWrapper.setMonitoringModule(new LoggingMonitoringModule(LOGGER::error));
        } else {
            monitoringModuleWrapper.setMonitoringModule(new StandardMonitoringModule());
        }

        NettyHttpClient httpClient = HttpClientFactory
                .defaultFactory()
                .getHttpClient(
                        configurationBuilder,
                        monitoringModuleWrapper,
                        additionalHeaders
                );

        int bufferSizeBytes = getBufferSizeMegabytes() * BYTES_IN_MEGABYTE;
        if (!TjahziInitializer.isCorrectSize(bufferSizeBytes)) {
            LOGGER.warn("Invalid log buffer size {} - using nearest power of two greater than provided value, no less than 1MB. {}",
                    bufferSizeBytes,
                    GitHubDocs.LOG_BUFFER_SIZING.getLogMessage()
            );
        }

        LabelFactory labelFactory = new LabelFactory(
                getConfiguration(),
                logLevelLabel,
                labels
        );

        LabelsDescriptor labelsDescriptor = labelFactory.convertLabelsDroppingInvalid();
        logLevelLabel = labelsDescriptor.getLogLevelLabel();

        LabelFactory metadataFactory = new LabelFactory(
                getConfiguration(),
                logLevelLabel,
                metadata
        );

        LabelsDescriptor metadataDescriptor = metadataFactory.convertLabelsDroppingInvalid();

        LoggingSystem loggingSystem = new TjahziInitializer().createLoggingSystem(
                httpClient,
                monitoringModuleWrapper,
                labelsDescriptor.getStaticLabels(),
                batchSize,
                TimeUnit.SECONDS.toMillis(batchWait),
                bufferSizeBytes,
                logShipperWakeupIntervalMillis,
                TimeUnit.SECONDS.toMillis(shutdownTimeoutSeconds),
                useDaemonThreads
        );

        int maxLogLineSizeBytes = Math.toIntExact(getMaxLogLineSizeKilobytes() * BYTES_IN_KILOBYTE);
        return new LokiAppender(
                getName(),
                getLayout(),
                getFilter(),
                isIgnoreExceptions(),
                getPropertyArray(),
                logLevelLabel,
                labelsDescriptor.getDynamicLabels(),
                metadataDescriptor.getAllLabels(),
                maxLogLineSizeBytes,
                loggingSystem,
                monitoringModuleWrapper
        );
    }

    public String getHost() {
        return host;
    }

    public String getUrl() {
        return url;
    }

    public B setUrl(String url) {
        this.url = url;
        return asBuilder();
    }

    public String getLogEndpoint() {
        return logEndpoint;
    }

    public B setLogEndpoint(String logEndpoint) {
        this.logEndpoint = logEndpoint;
        return asBuilder();
    }

    public boolean isUseSSL() {
        return useSSL;
    }

    public B setUseSSL(boolean useSSL) {
        this.useSSL = useSSL;
        return asBuilder();
    }

    public boolean isUseDaemonThreads() {
        return useDaemonThreads;
    }

    public B setUseDaemonThreads(boolean useDaemonThreads) {
        this.useDaemonThreads = useDaemonThreads;
        return asBuilder();
    }

    public String getUsername() {
        return username;
    }

    public B setUsername(String username) {
        this.username = username;
        return asBuilder();
    }

    public String getPassword() {
        return password;
    }

    public B setPassword(String password) {
        this.password = password;
        return asBuilder();
    }

    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public int getReadTimeoutMillis() {
        return readTimeoutMillis;
    }

    public Header[] getHeaders() {
        return headers;
    }

    public B setHost(String host) {
        this.host = host;
        return asBuilder();
    }

    public B setConnectTimeoutMillis(final int connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
        return asBuilder();
    }

    public B setReadTimeoutMillis(final int readTimeoutMillis) {
        this.readTimeoutMillis = readTimeoutMillis;
        return asBuilder();
    }

    public int getPort() {
        return port;
    }

    public B setPort(int port) {
        this.port = port;
        return asBuilder();
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public B setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
        return asBuilder();
    }

    public int getBufferSizeMegabytes() {
        return bufferSizeMegabytes;
    }

    public B setBufferSizeMegabytes(int bufferSizeMegabytes) {
        this.bufferSizeMegabytes = bufferSizeMegabytes;
        return asBuilder();
    }

    public String getLogLevelLabel() {
        return logLevelLabel;
    }

    public B setLogLevelLabel(String logLevelLabel) {
        this.logLevelLabel = logLevelLabel;
        return asBuilder();
    }

    public long getBatchSize() {
        return batchSize;
    }

    public B setBatchSize(long batchSize) {
        this.batchSize = batchSize;
        return asBuilder();
    }

    public long getBatchWait() {
        return batchWait;
    }

    public B setBatchWait(long batchWait) {
        this.batchWait = batchWait;
        return asBuilder();
    }

    public long getLogShipperWakeupIntervalMillis() {
        return logShipperWakeupIntervalMillis;
    }

    public B setLogShipperWakeupIntervalMillis(long logShipperWakeupIntervalMillis) {
        this.logShipperWakeupIntervalMillis = logShipperWakeupIntervalMillis;
        return asBuilder();
    }

    public int getShutdownTimeoutSeconds() {
        return shutdownTimeoutSeconds;
    }

    public B setShutdownTimeoutSeconds(int shutdownTimeoutSeconds) {
        this.shutdownTimeoutSeconds = shutdownTimeoutSeconds;
        return asBuilder();
    }

    public B setMaxLogLineSizeKilobytes(int maxLogLineSizeKilobytes) {
        this.maxLogLineSizeKilobytes = maxLogLineSizeKilobytes;
        return asBuilder();
    }

    public long getMaxLogLineSizeKilobytes() {
        return maxLogLineSizeKilobytes;
    }

    public B setMaxRequestsInFlight(int maxRequestsInFlight) {
        this.maxRequestsInFlight = maxRequestsInFlight;
        return asBuilder();
    }

    public int getMaxRequestsInFlight() {
        return maxRequestsInFlight;
    }

    public B setHeaders(Header[] headers) {
        this.headers = headers;
        return asBuilder();
    }

    public Label[] getLabels() {
        return labels;
    }

    public B setLabels(Label[] labels) {
        this.labels = labels;
        return asBuilder();
    }

    public StructuredMetadata[] getMetadata() {
        return metadata;
    }

    public B setMetadata(StructuredMetadata[] metadata) {
        this.metadata = metadata;
        return asBuilder();
    }

    public boolean isVerbose() {
        return verbose;
    }

    public B setVerbose(boolean verbose) {
        this.verbose = verbose;
        return asBuilder();
    }

    public String getTruststorePath() {
        return truststorePath;
    }

    public B setTruststorePath(String truststorePath) {
        this.truststorePath = truststorePath;
        return asBuilder();
    }

    public String getTruststorePassword() {
        return truststorePassword;
    }

    public B setTruststorePassword(String truststorePassword) {
        this.truststorePassword = truststorePassword;
        return asBuilder();
    }

    public String getTruststoreType() {
        return truststoreType;
    }

    public B setTruststoreType(String truststoreType) {
        this.truststoreType = truststoreType;
        return asBuilder();
    }
}
