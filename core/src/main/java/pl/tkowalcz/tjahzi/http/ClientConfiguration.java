package pl.tkowalcz.tjahzi.http;

public class ClientConfiguration {

    private final String logEndpoint;
    private final String host;
    private final int port;

    private final int connectionTimeoutMillis;
    private final int requestTimeoutMillis;
    private final int maxRequestsInFlight;

    private final int maxRetries;

    public ClientConfiguration(
            String logEndpoint,
            String host,
            int port,
            int connectionTimeoutMillis,
            int requestTimeoutMillis,
            int maxRequestsInFlight,
            int maxRetries) {
        this.logEndpoint = logEndpoint;
        this.host = host;
        this.port = port;
        this.connectionTimeoutMillis = connectionTimeoutMillis;
        this.requestTimeoutMillis = requestTimeoutMillis;
        this.maxRequestsInFlight = maxRequestsInFlight;

        this.maxRetries = maxRetries;
    }

    public String getLogEndpoint() {
        return logEndpoint;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getConnectionTimeoutMillis() {
        return connectionTimeoutMillis;
    }

    public int getRequestTimeoutMillis() {
        return requestTimeoutMillis;
    }

    public int getMaxRequestsInFlight() {
        return maxRequestsInFlight;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public static ClientConfigurationBuilder builder() {
        return new ClientConfigurationBuilder();
    }
}
