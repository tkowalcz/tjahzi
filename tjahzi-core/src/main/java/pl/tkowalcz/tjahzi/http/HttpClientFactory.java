package pl.tkowalcz.tjahzi.http;

import pl.tkowalcz.tjahzi.stats.MonitoringModule;

public class HttpClientFactory {

    private static final HttpClientFactory INSTANCE = new HttpClientFactory();

    public NettyHttpClient getHttpClient(
            ClientConfiguration clientConfiguration,
            MonitoringModule monitoringModule,
            String... additionalHeaders) {
        return new NettyHttpClient(
                clientConfiguration,
                monitoringModule,
                additionalHeaders
        );
    }

    public static HttpClientFactory defaultFactory() {
        return INSTANCE;
    }
}
