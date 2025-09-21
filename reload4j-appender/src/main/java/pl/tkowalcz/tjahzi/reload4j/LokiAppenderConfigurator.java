package pl.tkowalcz.tjahzi.reload4j;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.xml.DOMConfigurator;
import org.apache.log4j.xml.UnrecognizedElementHandler;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public abstract class LokiAppenderConfigurator extends AppenderSkeleton implements UnrecognizedElementHandler {

    private String url;
    private String logEndpoint;

    private String host;
    private int port;

    private boolean useSSL;

    private boolean useDaemonThreads;

    private boolean verbose;

    private String username;

    private String password;

    private String truststorePath;
    private String truststorePassword;
    private String truststoreType;

    private int connectTimeoutMillis = 5000;
    private int readTimeoutMillis = 60_000;
    private int maxRetries = 3;

    private int bufferSizeMegabytes = 32;

    private String logLevelLabel;
    private String loggerNameLabel;
    private String threadNameLabel;
    private final List<String> mdcLogLabels = new ArrayList<>();

    private long batchSize = 10_2400;
    private long batchWait = 5;
    private long shutdownTimeoutSeconds = 10;
    private long logShipperWakeupIntervalMillis = 10;

    private int maxRequestsInFlight = 100;

    private final List<Header> headers = new ArrayList<>();
    private final List<Label> labels = new ArrayList<>();

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getLogEndpoint() {
        return logEndpoint;
    }

    public void setLogEndpoint(String logEndpoint) {
        this.logEndpoint = logEndpoint;
    }

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

    public boolean isUseSSL() {
        return useSSL;
    }

    public void setUseSSL(boolean useSSL) {
        this.useSSL = useSSL;
    }

    public boolean isUseDaemonThreads() {
        return useDaemonThreads;
    }

    public void setUseDaemonThreads(boolean useDaemonThreads) {
        this.useDaemonThreads = useDaemonThreads;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTruststorePath() {
        return truststorePath;
    }

    public void setTruststorePath(String truststorePath) {
        this.truststorePath = truststorePath;
    }

    public String getTruststorePassword() {
        return truststorePassword;
    }

    public void setTruststorePassword(String truststorePassword) {
        this.truststorePassword = truststorePassword;
    }

    public String getTruststoreType() {
        return truststoreType;
    }

    public void setTruststoreType(String truststoreType) {
        this.truststoreType = truststoreType;
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

    public String getLogLevelLabel() {
        return logLevelLabel;
    }

    public void setLogLevelLabel(String logLevelLabel) {
        this.logLevelLabel = logLevelLabel;
    }

    public String getLoggerNameLabel() {
        return loggerNameLabel;
    }

    public void setLoggerNameLabel(String loggerNameLabel) {
        this.loggerNameLabel = loggerNameLabel;
    }

    public String getThreadNameLabel() {
        return threadNameLabel;
    }

    public void setThreadNameLabel(String threadNameLabel) {
        this.threadNameLabel = threadNameLabel;
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

    @Override
    public boolean parseUnrecognizedElement(Element element, Properties props) throws Exception {
        if ("label".equalsIgnoreCase(element.getNodeName())) {
            element.setAttribute("class", Label.class.getName());
            Object label = DOMConfigurator.parseElement(element, props, Label.class);
            if (label != null) {
                addLabel((Label) label);
                return true;
            }
        } else if ("header".equalsIgnoreCase(element.getNodeName())) {
            element.setAttribute("class", Header.class.getName());
            Object header = DOMConfigurator.parseElement(element, props, Header.class);
            if (header != null) {
                addHeader((Header) header);
                return true;
            }
        } else if ("mdcLogLabel".equalsIgnoreCase(element.getNodeName())) {
            String textContent = element.getTextContent();
            if (textContent != null) {
                addMdcLogLabel(textContent.trim());
                return true;
            }
        }

        return false;
    }
}
