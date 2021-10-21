package pl.tkowalcz.tjahzi.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;

import java.util.ArrayList;
import java.util.List;

public abstract class LokiAppenderConfigurator extends UnsynchronizedAppenderBase<ILoggingEvent> {

    static final int BYTES_IN_MEGABYTE = 1024 * 1024;

    private String host;
    private int port;

    private int connectTimeoutMillis = 5000;
    private int readTimeoutMillis = 60_000;
    private int maxRetries = 3;

    private int bufferSizeMegabytes = 32;
    private boolean useOffHeapBuffer = true;

    private String logLevelLabel;
    private final List<String> mdcLogLabels = new ArrayList<>();

    private long batchSize = 10_2400;
    private long batchWait = 5;
    private long shutdownTimeoutSeconds = 10;
    private long logShipperWakeupIntervalMillis = 250;

    private int maxRequestsInFlight = 100;

    private final List<Header> headers = new ArrayList<>();
    private final List<Label> labels = new ArrayList<>();

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public void setConnectTimeoutMillis(int connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    public int getReadTimeoutMillis() {
        return readTimeoutMillis;
    }

    public void setReadTimeoutMillis(int readTimeoutMillis) {
        this.readTimeoutMillis = readTimeoutMillis;
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

    public void setBatchSize(long batchSize) {
        this.batchSize = batchSize;
    }

    public void setBatchWait(long batchWait) {
        this.batchWait = batchWait;
    }

    public long getBatchSize() {
        return batchSize;
    }

    public long getBatchWait() {
        return batchWait;
    }

    public long getShutdownTimeoutSeconds() {
        return shutdownTimeoutSeconds;
    }

    public void setShutdownTimeoutSeconds(long shutdownTimeoutSeconds) {
        this.shutdownTimeoutSeconds = shutdownTimeoutSeconds;
    }

    public long getLogShipperWakeupIntervalMillis() {
        return logShipperWakeupIntervalMillis;
    }

    public void setLogShipperWakeupIntervalMillis(long logShipperWakeupIntervalMillis) {
        this.logShipperWakeupIntervalMillis = logShipperWakeupIntervalMillis;
    }

    public int getMaxRequestsInFlight() {
        return maxRequestsInFlight;
    }

    public void setMaxRequestsInFlight(int maxRequestsInFlight) {
        this.maxRequestsInFlight = maxRequestsInFlight;
    }

    public void addHeader(Header header) {
        headers.add(header);
    }

    public List<Header> getHeaders() {
        return headers;
    }

    public void addLabel(Label label) {
        labels.add(label);
    }

    public List<Label> getLabels() {
        return labels;
    }

    public List<String> getMdcLogLabels() {
        return mdcLogLabels;
    }

    public void addMdcLogLabel(String mdcLogLabel) {
        this.mdcLogLabels.add(mdcLogLabel);
    }
}
