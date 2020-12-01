package pl.tkowalcz.tjahzi.http;

public class HttpClientFactory {

    private static final HttpClientFactory INSTANCE = new HttpClientFactory();

    public NettyHttpClient getHttpClient(
            ClientConfiguration clientConfiguration,
            String... additionalHeaders) {
        return new NettyHttpClient(
                clientConfiguration,
                additionalHeaders
        );
    }

    public static HttpClientFactory defaultFactory() {
        return INSTANCE;
    }
}
