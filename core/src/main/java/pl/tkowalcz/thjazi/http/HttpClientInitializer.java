package pl.tkowalcz.thjazi.http;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.timeout.IdleStateHandler;

class HttpClientInitializer extends ChannelInitializer<SocketChannel> {

    private final int requestTimeoutMillis;

    HttpClientInitializer(int requestTimeoutMillis) {
        this.requestTimeoutMillis = requestTimeoutMillis;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();
        p.addLast(new IdleStateHandler(requestTimeoutMillis, 0, 60));
        p.addLast(new HttpClientCodec());
        p.addLast(new HttpObjectAggregator(1048576));
        p.addLast(new HttpContentDecompressor());
    }
}
