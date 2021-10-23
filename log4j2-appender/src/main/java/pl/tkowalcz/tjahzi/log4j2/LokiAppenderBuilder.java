package pl.tkowalcz.tjahzi.log4j2;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
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
    @Required(message = "No Loki address provided for LokiAppender")
    private String host;

    @PluginBuilderAttribute
    @Required(message = "No Loki port provided for LokiAppender")
    private int port;

    @PluginBuilderAttribute
    private String username;

    @PluginBuilderAttribute
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
    private boolean useOffHeapBuffer = true;

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

    @PluginElement("Headers")
    private Header[] headers;

    @PluginElement("Labels")
    private Label[] labels;

    @Override
    public LokiAppender build() {
        ClientConfiguration configurationBuilder = ClientConfiguration.builder()
                .withHost(host)
                .withPort(port)
                .withUsername(username)
                .withPassword(password)
                .withConnectionTimeoutMillis(connectTimeoutMillis)
                .withMaxRetries(maxRetries)
                .withRequestTimeoutMillis(readTimeoutMillis)
                .withMaxRequestsInFlight(maxRequestsInFlight)
                .build();

        String[] additionalHeaders = stream(headers)
                .flatMap(header -> Stream.of(header.getName(), header.getValue()))
                .toArray(String[]::new);

        MutableMonitoringModuleWrapper monitoringModuleWrapper = new MutableMonitoringModuleWrapper();
        monitoringModuleWrapper.setMonitoringModule(new StandardMonitoringModule());

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

        LoggingSystem loggingSystem = new TjahziInitializer().createLoggingSystem(
                httpClient,
                monitoringModuleWrapper,
                labelsDescriptor.getStaticLabels(),
                batchSize,
                TimeUnit.SECONDS.toMillis(batchWait),
                bufferSizeBytes,
                TimeUnit.SECONDS.toMillis(shutdownTimeoutSeconds),
                logShipperWakeupIntervalMillis,
                isUseOffHeapBuffer()
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
                maxLogLineSizeBytes,
                loggingSystem,
                monitoringModuleWrapper
        );
    }

    public String getHost() {
        return host;
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

    public void setPort(int port) {
        this.port = port;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public int getBufferSizeMegabytes() {
        return bufferSizeMegabytes;
    }

    public void setBufferSizeMegabytes(int bufferSizeMegabytes) {
        this.bufferSizeMegabytes = bufferSizeMegabytes;
    }

    public boolean isUseOffHeapBuffer() {
        return useOffHeapBuffer;
    }

    public void setUseOffHeapBuffer(boolean useOffHeapBuffer) {
        this.useOffHeapBuffer = useOffHeapBuffer;
    }

    public String getLogLevelLabel() {
        return logLevelLabel;
    }

    public void setLogLevelLabel(String logLevelLabel) {
        this.logLevelLabel = logLevelLabel;
    }

    public long getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(long batchSize) {
        this.batchSize = batchSize;
    }

    public long getBatchWait() {
        return batchWait;
    }

    public void setBatchWait(long batchWait) {
        this.batchWait = batchWait;
    }

    public long getLogShipperWakeupIntervalMillis() {
        return logShipperWakeupIntervalMillis;
    }

    public void setLogShipperWakeupIntervalMillis(long logShipperWakeupIntervalMillis) {
        this.logShipperWakeupIntervalMillis = logShipperWakeupIntervalMillis;
    }

    public int getShutdownTimeoutSeconds() {
        return shutdownTimeoutSeconds;
    }

    public void setShutdownTimeoutSeconds(int shutdownTimeoutSeconds) {
        this.shutdownTimeoutSeconds = shutdownTimeoutSeconds;
    }

    public void setMaxLogLineSizeKilobytes(int maxLogLineSizeKilobytes) {
        this.maxLogLineSizeKilobytes = maxLogLineSizeKilobytes;
    }

    public long getMaxLogLineSizeKilobytes() {
        return maxLogLineSizeKilobytes;
    }

    public void setMaxRequestsInFlight(int maxRequestsInFlight) {
        this.maxRequestsInFlight = maxRequestsInFlight;
    }

    public int getMaxRequestsInFlight() {
        return maxRequestsInFlight;
    }

    public void setHeaders(Header[] headers) {
        this.headers = headers;
    }

    public Label[] getLabels() {
        return labels;
    }

    public void setLabels(Label[] labels) {
        this.labels = labels;
    }
}
