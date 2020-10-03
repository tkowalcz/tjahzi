package pl.tkowalcz.thjazi.http;

import java.time.Clock;

public class HttpClientFactory {

    private static final HttpClientFactory INSTANCE = new HttpClientFactory();

    public NettyHttpClient getHttpClient(ClientConfiguration clientConfiguration) {
        return new NettyHttpClient(
                Clock.systemUTC(),
                clientConfiguration
        );
    }

    public static HttpClientFactory defaultFactory() {
        return INSTANCE;
    }
}
