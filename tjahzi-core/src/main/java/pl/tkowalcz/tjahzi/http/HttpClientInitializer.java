package pl.tkowalcz.tjahzi.http;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.timeout.IdleStateHandler;
import pl.tkowalcz.tjahzi.stats.MonitoringModule;
import pl.tkowalcz.tjahzi.stats.PipelinedHttpRequestTimer;

class HttpClientInitializer extends ChannelInitializer<SocketChannel> {

    private static final int BYTES_IN_MEGABYTE = 1024 * 1024;
    private static final int MAX_CONTENT_LENGTH = 10 * BYTES_IN_MEGABYTE;

    private final MonitoringModule monitoringModule;
    private final ResponseHandler responseHandler;

    private final int requestTimeoutMillis;
    private final int maxNumberOfRequests;

    HttpClientInitializer(
            MonitoringModule monitoringModule,
            int requestTimeoutMillis,
            int maxNumberOfRequests
    ) {
        this.monitoringModule = monitoringModule;
        this.responseHandler = new ResponseHandler(monitoringModule);

        this.requestTimeoutMillis = requestTimeoutMillis;
        this.maxNumberOfRequests = maxNumberOfRequests;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();
        p.addLast(new IdleStateHandler(
                        requestTimeoutMillis,
                        0,
                        60
                )
        );
        p.addLast(new HttpClientCodec());
        p.addLast(new HttpObjectAggregator(MAX_CONTENT_LENGTH));
        p.addLast(new HttpContentDecompressor());
        p.addLast(responseHandler);
        p.addLast(
                new PipelinedHttpRequestTimer(
                        monitoringModule,
                        maxNumberOfRequests
                )
        );
    }
}
