package pl.tkowalcz.tjahzi.log4j2;

import com.google.common.collect.Maps;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import pl.tkowalcz.tjahzi.LoggingSystem;
import pl.tkowalcz.tjahzi.TjahziInitializer;
import pl.tkowalcz.tjahzi.github.GitHubDocs;
import pl.tkowalcz.tjahzi.http.ClientConfiguration;
import pl.tkowalcz.tjahzi.http.HttpClientFactory;
import pl.tkowalcz.tjahzi.http.NettyHttpClient;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.util.Arrays.stream;

/**
 * Loki Appender.
 */
@Plugin(name = "Loki", category = Node.CATEGORY, elementType = Appender.ELEMENT_TYPE, printObject = true)
public class LokiAppender extends AbstractAppender {

    /**
     * Builds LokiAppender instances.
     *
     * @param <B> The type to build
     */
    public static class Builder<B extends LokiAppender.Builder<B>> extends AbstractAppender.Builder<B>
            implements org.apache.logging.log4j.core.util.Builder<LokiAppender> {

        public static final int BYTES_IN_MEGABYTE = 1024 * 1024;

        @PluginBuilderAttribute
        @Required(message = "No Loki address provided for LokiAppender")
        private String host;

        @PluginBuilderAttribute
        @Required(message = "No Loki port provided for LokiAppender")
        private int port;

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

        @PluginElement("Headers")
        private Header[] headers;

        @PluginElement("Labels")
        private Label[] labels;

        @Override
        public LokiAppender build() {
            ClientConfiguration configurationBuilder = ClientConfiguration.builder()
                    .withHost(host)
                    .withConnectionTimeoutMillis(connectTimeoutMillis)
                    .withPort(port)
                    .withMaxRetries(maxRetries)
                    .withRequestTimeoutMillis(readTimeoutMillis)
                    .build();

            String[] additionalHeaders = stream(headers)
                    .flatMap(header -> Stream.of(header.getName(), header.getValue()))
                    .toArray(String[]::new);

            NettyHttpClient httpClient = HttpClientFactory
                    .defaultFactory()
                    .getHttpClient(
                            configurationBuilder,
                            additionalHeaders
                    );

            int bufferSizeBytes = getBufferSizeMegabytes() * BYTES_IN_MEGABYTE;
            if (TjahziInitializer.isCorrectSize(bufferSizeBytes)) {
                LOGGER.warn("Invalid log buffer size {} - using nearest power of two greater than provided value, no less than 1MB. {}",
                        bufferSizeBytes,
                        GitHubDocs.LOG_BUFFER_SIZING.getLogMessage()
                );
            }

            HashMap<String, String> lokiLabels = Maps.newHashMap();
            stream(labels).forEach(__ -> lokiLabels.put(__.getName(), __.getValue()));

            LoggingSystem loggingSystem = new TjahziInitializer().createLoggingSystem(
                    httpClient,
                    lokiLabels,
                    bufferSizeBytes,
                    isUseOffHeapBuffer()
            );

            return new LokiAppender(
                    getName(),
                    getLayout(),
                    getFilter(),
                    isIgnoreExceptions(),
                    getPropertyArray(),
                    logLevelLabel,
                    loggingSystem
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

    /**
     * @return a builder for a LokiAppender.
     */
    @PluginBuilderFactory
    public static <B extends LokiAppender.Builder<B>> B newBuilder() {
        return new LokiAppender.Builder<B>().asBuilder();
    }

    private final LoggingSystem loggingSystem;
    private final AppenderLogic appenderLogic;

    private LokiAppender(
            String name,
            Layout<? extends Serializable> layout,
            Filter filter,
            boolean ignoreExceptions,
            Property[] properties,
            String logLevelLabel,
            LoggingSystem loggingSystem) {
        super(
                name,
                filter,
                layout,
                ignoreExceptions,
                properties
        );
        Objects.requireNonNull(layout, "layout");

        this.loggingSystem = loggingSystem;
        this.appenderLogic = new AppenderLogic(
                loggingSystem,
                logLevelLabel
        );
    }

    public LoggingSystem getLoggingSystem() {
        return loggingSystem;
    }

    @Override
    public void start() {
        loggingSystem.start();
        super.start();
    }

    @Override
    public void append(final LogEvent event) {
        appenderLogic.append(getLayout(), event);
    }

    @Override
    public boolean stop(
            long timeout,
            TimeUnit timeUnit
    ) {
        setStopping();

        boolean stopped = super.stop(
                timeout,
                timeUnit,
                false
        );

        appenderLogic.close(
                timeout,
                timeUnit
        );

        setStopped();
        return stopped;
    }

    @Override
    public String toString() {
        return "LokiAppender{" +
                "name=" + getName() +
                ", state=" + getState() +
                '}';
    }
}
