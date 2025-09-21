package pl.tkowalcz.tjahzi.logback;

import pl.tkowalcz.tjahzi.LoggingSystem;
import pl.tkowalcz.tjahzi.TjahziInitializer;
import pl.tkowalcz.tjahzi.github.GitHubDocs;
import pl.tkowalcz.tjahzi.http.ClientConfiguration;
import pl.tkowalcz.tjahzi.http.HttpClientFactory;
import pl.tkowalcz.tjahzi.http.NettyHttpClient;
import pl.tkowalcz.tjahzi.stats.LoggingMonitoringModule;
import pl.tkowalcz.tjahzi.stats.MutableMonitoringModuleWrapper;
import pl.tkowalcz.tjahzi.stats.StandardMonitoringModule;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class LokiAppenderFactory {

    private final LokiAppenderConfigurator configurator;

    private final HashMap<String, String> lokiLabels;
    private final String logLevelLabel;
    private final String loggerNameLabel;
    private final String threadNameLabel;
    private final List<String> mdcLogLabels;
    private final HashMap<String, String> structuredMetadata;
    private final MutableMonitoringModuleWrapper monitoringModuleWrapper;

    public LokiAppenderFactory(LokiAppenderConfigurator configurator) {
        this.configurator = configurator;

        LabelFactory labelFactory = new LabelFactory(
                configurator,
                configurator.getLogLevelLabel(),
                configurator.getLoggerNameLabel(),
                configurator.getThreadNameLabel(),
                configurator.getLabels().toArray(new Label[0])
        );

        lokiLabels = labelFactory.convertLabelsDroppingInvalid();
        logLevelLabel = labelFactory.validateLogLevelLabel(lokiLabels);
        loggerNameLabel = labelFactory.validateLoggerNameLabel(lokiLabels);
        threadNameLabel = labelFactory.validateThreadNameLabel(lokiLabels);

        LabelFactory structuredMetadataFactory = new LabelFactory(
                configurator,
                configurator.getLogLevelLabel(),
                configurator.getLoggerNameLabel(),
                configurator.getThreadNameLabel(),
                configurator.getMetadatas().toArray(new Label[0])
        );

        structuredMetadata = structuredMetadataFactory.convertLabelsDroppingInvalid();
        mdcLogLabels = configurator.getMdcLogLabels();
        monitoringModuleWrapper = new MutableMonitoringModuleWrapper();
    }

    public LoggingSystem createAppender() {
        ClientConfiguration configurationBuilder = ClientConfiguration.builder()
                .withUrl(configurator.getUrl())
                .withLogEndpoint(configurator.getLogEndpoint())
                .withHost(configurator.getHost())
                .withPort(configurator.getPort())
                .withUseSSL(configurator.isUseSSL())
                .withUsername(configurator.getUsername())
                .withPassword(configurator.getPassword())
                .withConnectionTimeoutMillis(configurator.getConnectTimeoutMillis())
                .withMaxRetries(configurator.getMaxRetries())
                .withRequestTimeoutMillis(configurator.getReadTimeoutMillis())
                .withMaxRequestsInFlight(configurator.getMaxRequestsInFlight())
                .withTrustStorePath(configurator.getTruststorePath())
                .withTrustStorePassword(configurator.getTruststorePassword())
                .withTrustStoreType(configurator.getTruststoreType())
                .build();

        String[] additionalHeaders = configurator.getHeaders().stream()
                .flatMap(header -> Stream.of(header.getName(), header.getValue()))
                .toArray(String[]::new);

        if (configurator.isVerbose()) {
            monitoringModuleWrapper.setMonitoringModule(new LoggingMonitoringModule(configurator::addError));
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

        int bufferSizeBytes = configurator.getBufferSizeMegabytes() * LokiAppenderConfigurator.BYTES_IN_MEGABYTE;
        if (!TjahziInitializer.isCorrectSize(bufferSizeBytes)) {
            configurator.addWarn(
                    String.format(
                            "Invalid log buffer size %d - using nearest power of two greater than provided value, no less than 1MB. %s\n",
                            bufferSizeBytes,
                            GitHubDocs.LOG_BUFFER_SIZING.getLogMessage()
                    )
            );
        }

        return new TjahziInitializer().createLoggingSystem(
                httpClient,
                monitoringModuleWrapper,
                lokiLabels,
                configurator.getBatchSize(),
                TimeUnit.SECONDS.toMillis(configurator.getBatchWait()),
                bufferSizeBytes,
                configurator.getLogShipperWakeupIntervalMillis(),
                TimeUnit.SECONDS.toMillis(configurator.getShutdownTimeoutSeconds()),
                configurator.isUseDaemonThreads()
        );
    }

    public String getLogLevelLabel() {
        return logLevelLabel;
    }

    public String getLoggerNameLabel() {
        return loggerNameLabel;
    }

    public String getThreadNameLabel() {
        return threadNameLabel;
    }

    public HashMap<String, String> getStructuredMetadata() {
        return structuredMetadata;
    }

    public MutableMonitoringModuleWrapper getMonitoringModuleWrapper() {
        return monitoringModuleWrapper;
    }

    public List<String> getMdcLogLabels() {
        return mdcLogLabels;
    }
}
