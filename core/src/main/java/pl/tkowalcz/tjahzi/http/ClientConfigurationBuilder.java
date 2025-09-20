package pl.tkowalcz.tjahzi.http;

import java.net.MalformedURLException;
import java.net.URL;

public class ClientConfigurationBuilder {

    public static final int HTTPS_PORT = 443;
    public static final int HTTP_PORT = 80;
    public static final String HTTPS_STRING = "https";
    public static final String HTTP_STRING = "http";

    public static final String DEFAULT_LOG_ENDPOINT = "/loki/api/v1/push";

    public static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 5_000;
    public static final int DEFAULT_REQUEST_TIMEOUT_MILLIS = 60_000;

    public static final int DEFAULT_MAX_REQUESTS_IN_FLIGHT = 100;

    public static final int DEFAULT_MAX_RETRIES = 0;

    private String logEndpoint;

    private String url;

    private String host;
    private int port;

    private boolean useSSL;

    private String username;
    private String password;

    private String trustStorePath;
    private String trustStorePassword;
    private String trustStoreType;

    private int connectionTimeoutMillis = DEFAULT_CONNECT_TIMEOUT_MILLIS;
    private int requestTimeoutMillis = DEFAULT_REQUEST_TIMEOUT_MILLIS;
    private int maxRequestsInFlight = DEFAULT_MAX_REQUESTS_IN_FLIGHT;

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

    public ClientConfigurationBuilder withUseSSL(boolean useSSL) {
        this.useSSL = useSSL;
        return this;
    }

    public ClientConfigurationBuilder withUsername(String username) {
        this.username = username;
        return this;
    }

    public ClientConfigurationBuilder withPassword(String password) {
        this.password = password;
        return this;
    }

    public ClientConfigurationBuilder withTrustStorePath(String trustStorePath) {
        this.trustStorePath = trustStorePath;
        return this;
    }

    public ClientConfigurationBuilder withTrustStorePassword(String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
        return this;
    }

    public ClientConfigurationBuilder withTrustStoreType(String trustStoreType) {
        this.trustStoreType = trustStoreType;
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

    public ClientConfigurationBuilder withMaxRequestsInFlight(int maxRequestsInFlight) {
        this.maxRequestsInFlight = maxRequestsInFlight;
        return this;
    }

    public ClientConfigurationBuilder withMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
        return this;
    }

    public ClientConfigurationBuilder withUrl(String url) {
        this.url = url;
        return this;
    }

    public ClientConfiguration build() {
        if (maxRequestsInFlight <= 0) {
            throw new IllegalArgumentException("Property maxRequestsInFlight must be greater than 0");
        }

        ConnectionParams connectionParams = ConnectionParamsFactory.create(
                url,
                host,
                port,
                logEndpoint,
                useSSL
        );

        return new ClientConfiguration(
                connectionParams.getLogEndpoint(),
                connectionParams.getHost(),
                connectionParams.getPort(),
                connectionParams.isUseSSL(),
                username,
                password,
                trustStorePath,
                trustStorePassword,
                trustStoreType,
                connectionTimeoutMillis,
                requestTimeoutMillis,
                maxRequestsInFlight,
                maxRetries
        );
    }
}
