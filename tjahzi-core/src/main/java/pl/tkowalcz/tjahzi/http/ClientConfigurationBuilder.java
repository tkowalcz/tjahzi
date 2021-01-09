package pl.tkowalcz.tjahzi.http;

public class ClientConfigurationBuilder {

    public static final String DEFAULT_LOG_ENDPOINT = "/loki/api/v1/push";

    public static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 5_000;
    public static final int DEFAULT_REQUEST_TIMEOUT_MILLIS = 60_000;

    public static final int DEFAULT_MAX_IN_FLIGHT_REQUESTS = 10;

    public static final int DEFAULT_MAX_RETRIES = 0;

    private String logEndpoint = DEFAULT_LOG_ENDPOINT;
    private String host;
    private int port;

    private int connectionTimeoutMillis = DEFAULT_CONNECT_TIMEOUT_MILLIS;
    private int requestTimeoutMillis = DEFAULT_REQUEST_TIMEOUT_MILLIS;
    private int maxInFlightRequests = DEFAULT_MAX_IN_FLIGHT_REQUESTS;

    private int maxRetries = DEFAULT_MAX_RETRIES;

    public ClientConfigurationBuilder withLogEndpoint(String logEndpoint) {
        this.logEndpoint = logEndpoint;
        return this;
    }

    public ClientConfigurationBuilder withHost(String host) {
        this.host = host;
        return this;
    }

    public ClientConfigurationBuilder withPort(int port) {
        this.port = port;
        return this;
    }

    public ClientConfigurationBuilder withConnectionTimeoutMillis(int connectionTimeoutMillis) {
        this.connectionTimeoutMillis = connectionTimeoutMillis;
        return this;
    }

    public ClientConfigurationBuilder withRequestTimeoutMillis(int requestTimeoutMillis) {
        this.requestTimeoutMillis = requestTimeoutMillis;
        return this;
    }

    public ClientConfigurationBuilder withMaxInFlightRequests(int maxInFlightRequests) {
        this.maxInFlightRequests = maxInFlightRequests;
        return this;
    }

    public ClientConfigurationBuilder withMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
        return this;
    }

    public ClientConfiguration build() {
        return new ClientConfiguration(
                logEndpoint,
                host,
                port,
                connectionTimeoutMillis,
                requestTimeoutMillis,
                maxInFlightRequests,
                maxRetries
        );
    }
}
