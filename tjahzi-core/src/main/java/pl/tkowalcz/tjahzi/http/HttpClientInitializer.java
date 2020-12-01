package pl.tkowalcz.tjahzi.http;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.timeout.IdleStateHandler;

class HttpClientInitializer extends ChannelInitializer<SocketChannel> {

    private static final int BYTES_IN_MEGABYTE = 1024 * 1024;

    private final ResponseHandler responseHandler;
    private final int requestTimeoutMillis;

    HttpClientInitializer(ResponseHandler responseHandler, int requestTimeoutMillis) {
        this.responseHandler = responseHandler;
        this.requestTimeoutMillis = requestTimeoutMillis;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();
        p.addLast(new IdleStateHandler(requestTimeoutMillis, 0, 60));
        p.addLast(new HttpClientCodec());
        p.addLast(new HttpObjectAggregator(BYTES_IN_MEGABYTE));
        p.addLast(new HttpContentDecompressor());
        p.addLast(responseHandler);
    }
}
