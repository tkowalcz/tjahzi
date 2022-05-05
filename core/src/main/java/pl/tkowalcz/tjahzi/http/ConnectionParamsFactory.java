package pl.tkowalcz.tjahzi.http;

import java.net.MalformedURLException;
import java.net.URL;

public class ConnectionParamsFactory {

    public static final int HTTPS_PORT = 443;
    public static final int HTTP_PORT = 80;
    public static final String HTTPS_STRING = "https";
    public static final String HTTP_STRING = "http";

    public static final String DEFAULT_LOG_ENDPOINT = "/loki/api/v1/push";

    public static ConnectionParams create(String url, String host, int port, String logEndpoint, boolean useSSL) {
        if (url != null && host != null) {
            throw new IllegalArgumentException("Only one of 'url' or 'host' can be configured. " +
                    "Current configuration sets url to '" + url + "' and host to '" + host + "'");
        }

        if (url != null && port != 0) {
            throw new IllegalArgumentException("Only one of 'url' or 'port' can be configured. " +
                    "If using url you must configure port as part of url string. Current configuration sets url to '" + url + "' and port to '" + port + "'");
        }

        if (url != null && useSSL) {
            throw new IllegalArgumentException("Only one of 'url' or 'useSSL' can be configured. " +
                    "Please use 'https' prefix in url to enable SSL. Current configuration sets url to '" + url + "' and useSSL to '" + useSSL + "'");
        }

        if (url == null && host == null) {
            throw new IllegalArgumentException("One of 'url' or 'host' must be configured.");
        }

        if (host != null) {
            return validateAndConfigureConnectionParametersNoUrl(host, port, logEndpoint, useSSL);
        }

        return validateAndConfigureConnectionParametersWithUrl(url, logEndpoint, useSSL);
    }

    private static ConnectionParams validateAndConfigureConnectionParametersNoUrl(String host, int port, String logEndpoint, boolean useSSL) {
        if (logEndpoint == null) {
            logEndpoint = DEFAULT_LOG_ENDPOINT;
        }

        if (!useSSL) {
            useSSL = port == HTTPS_PORT;
        }

        return new ConnectionParams(host, port, logEndpoint, useSSL);
    }

    private static ConnectionParams validateAndConfigureConnectionParametersWithUrl(String url, String logEndpoint, boolean useSSL) {
        try {
            URL parsedUrl = new URL(url);
            String host = parsedUrl.getHost();
            int port = configurePort(parsedUrl);

            useSSL = validateProtocolSchemeAndConfigureSSL(parsedUrl);
            logEndpoint = configureLogEndpoint(parsedUrl, logEndpoint);

            return new ConnectionParams(host, port, logEndpoint, useSSL);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Creating configuration with Loki URL failed, url: '" + url + "'", e);
        }
    }

    private static String configureLogEndpoint(URL parsedUrl, String logEndpoint) {
        if (logEndpoint != null) {
            throw new IllegalArgumentException("Only one of 'url' or 'logEndpoint' can be configured. " +
                    "Embed log endpoint path into url. " +
                    "Log endpoint: '" + logEndpoint + "', url: '" + parsedUrl + "'");
        }

        if (parsedUrl.getPath() != null && !parsedUrl.getPath().isEmpty()) {
            return parsedUrl.getPath();
        } else {
            return DEFAULT_LOG_ENDPOINT;
        }
    }

    private static int configurePort(URL parsedUrl) {
        int port = parsedUrl.getPort();

        if (port == -1) {
            if (HTTPS_STRING.equalsIgnoreCase(parsedUrl.getProtocol())) {
                return HTTPS_PORT;
            } else {
                return HTTP_PORT;
            }
        }

        return port;
    }

    private static boolean validateProtocolSchemeAndConfigureSSL(URL parsedUrl) {
        String protocolScheme = parsedUrl.getProtocol();

        if (HTTPS_STRING.equalsIgnoreCase(protocolScheme)) {
            return true;
        }

        if (HTTP_STRING.equalsIgnoreCase(protocolScheme)) {
            return false;
        }

        throw new IllegalArgumentException("Unknown protocol scheme, must be one of 'https' or 'http' (case insensitive). Provided: '" + protocolScheme + "'");
    }
}
