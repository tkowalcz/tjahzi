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

        validateAndConfigureConnectionParameters();

        return new ClientConfiguration(
                logEndpoint,
                host,
                port,
                useSSL,
                username,
                password,
                connectionTimeoutMillis,
                requestTimeoutMillis,
                maxRequestsInFlight,
                maxRetries
        );
    }

    private void validateAndConfigureConnectionParameters() {
        if (url != null && host != null) {
            throw new IllegalArgumentException("Only one of 'url' or 'host' can be configured. " +
                    "Current configuration sets url to '" + url + "' and host to '" + host + "'");
        }

        if (url == null && host == null) {
            throw new IllegalArgumentException("One of 'url' or 'host' must be configured.");
        }

        if (host != null) {
            validateAndConfigureConnectionParametersNoUrl();
        }

        validateAndConfigureConnectionParametersWithUrl();
    }

    private void validateAndConfigureConnectionParametersWithUrl() {
        try {
            URL parsedUrl = new URL(url);

            String protocolScheme = parsedUrl.getProtocol();
            if (!HTTPS_STRING.equalsIgnoreCase(protocolScheme) && !HTTP_STRING.equalsIgnoreCase(protocolScheme)) {
                throw new IllegalArgumentException("Unknown protocol scheme, must be one of 'https' or 'http' (case insensitive). Provided: '" + protocolScheme + "'");
            }

            host = parsedUrl.getHost();
            port = parsedUrl.getPort();

            if (port == -1) {
                if (parsedUrl.getProtocol().equalsIgnoreCase(HTTPS_STRING)) {
                    port = HTTPS_PORT;
                } else {
                    port = HTTP_PORT;
                }
            }

            if (parsedUrl.getPath() != null && !parsedUrl.getPath().isEmpty()) {
                if (logEndpoint != null) {
                    throw new IllegalArgumentException("If Loki connection URL contains path part then you cannot at the " +
                            "same time define log endpoint. Url: '" + url + "', log endpoint: '" + logEndpoint + "'");
                }

                logEndpoint = parsedUrl.getPath();
            } else {
                logEndpoint = DEFAULT_LOG_ENDPOINT;
            }

            if (HTTPS_STRING.equalsIgnoreCase(parsedUrl.getProtocol())) {
                useSSL = true;
            }
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Creating configuration with Loki URL failed, url: '" + url + "'", e);
        }
    }

    private void validateAndConfigureConnectionParametersNoUrl() {
        if (logEndpoint == null) {
            logEndpoint = DEFAULT_LOG_ENDPOINT;
        }

        if (!useSSL) {
            useSSL = port == HTTPS_PORT;
        }
    }
}
