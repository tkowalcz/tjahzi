package pl.tkowalcz.tjahzi.http;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.IdleStateHandler;
import pl.tkowalcz.tjahzi.stats.MonitoringModule;

class HttpClientInitializer extends ChannelInitializer<Channel> {

    private static final int BYTES_IN_MEGABYTE = 1024 * 1024;
    private static final int MAX_CONTENT_LENGTH = 10 * BYTES_IN_MEGABYTE;

    private final MonitoringModule monitoringModule;
    private final RequestAndResponseHandler responseHandler;

    private final SslContext sslContext;

    private final String host;
    private final int port;
    private final int requestTimeoutMillis;
    private final int maxRequestsInFlight;

    HttpClientInitializer(
            MonitoringModule monitoringModule,
            SslContext sslContext,
            String host,
            int port,
            int requestTimeoutMillis,
            int maxRequestsInFlight
    ) {
        this.monitoringModule = monitoringModule;
        this.responseHandler = new RequestAndResponseHandler(monitoringModule);
        this.sslContext = sslContext;
        this.host = host;
        this.port = port;

        this.requestTimeoutMillis = requestTimeoutMillis;
        this.maxRequestsInFlight = maxRequestsInFlight;
    }

    @Override
    protected void initChannel(Channel ch) {
        ChannelPipeline p = ch.pipeline();
        p.addLast(new IdleStateHandler(
                        requestTimeoutMillis,
                        0,
                        60
                )
        );

        if (sslContext != null) {
            p.addLast(sslContext.newHandler(ch.alloc(), host, port));
        }

        p.addLast(new HttpClientCodec());
        p.addLast(new HttpObjectAggregator(MAX_CONTENT_LENGTH));
        p.addLast(new HttpContentDecompressor());
        p.addLast(
                new PipelinedHttpRequestTimer(
                        monitoringModule,
                        maxRequestsInFlight
                )
        );
        p.addLast(responseHandler);
    }
}
