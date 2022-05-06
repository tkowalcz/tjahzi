package pl.tkowalcz.tjahzi.http;

import java.net.MalformedURLException;
import java.net.URL;

public class ConnectionParams {

    private final String host;
    private final int port;

    private final String logEndpoint;
    private final boolean useSSL;

    public ConnectionParams(String host, int port, String logEndpoint, boolean useSSL) {
        this.host = host;
        this.port = port;
        this.logEndpoint = logEndpoint;
        this.useSSL = useSSL;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getLogEndpoint() {
        return logEndpoint;
    }

    public boolean isUseSSL() {
        return useSSL;
    }
}
