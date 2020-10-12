package pl.tkowalcz.tjahzi.http;

public class HttpClientFactory {

    private static final HttpClientFactory INSTANCE = new HttpClientFactory();

    public NettyHttpClient getHttpClient(ClientConfiguration clientConfiguration) {
        return new NettyHttpClient(clientConfiguration);
    }

    public static HttpClientFactory defaultFactory() {
        return INSTANCE;
    }
}
