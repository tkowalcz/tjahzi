package pl.tkowalcz.thjazi.http;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.compression.Snappy;
import io.netty.handler.codec.http.*;

import java.util.concurrent.ThreadFactory;

public class NettyHttpClient implements AutoCloseable {

    private final EventLoopGroup group;
    private final String logEndpoint;

    private final Channel channel;
    private final String host;

    private final Snappy snappy = new Snappy();

    public NettyHttpClient(ClientConfiguration clientConfiguration) {
        host = clientConfiguration.getHost();
        logEndpoint = clientConfiguration.getLogEndpoint();

        ThreadGroup threadGroup = new ThreadGroup("Thjazi Loki client");
        ThreadFactory threadFactory = r -> new Thread(threadGroup, r, "thjazi-worker");
        group = new NioEventLoopGroup(1, threadFactory);

        Bootstrap bootstrap = new Bootstrap();
        channel = bootstrap.group(group)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, false)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, clientConfiguration.getConnectionTimeoutMillis())
                .channel(NioSocketChannel.class)
                .handler(new HttpClientInitializer(clientConfiguration.getRequestTimeoutMillis()))
                .remoteAddress(
                        clientConfiguration.getHost(),
                        clientConfiguration.getPort()
                )
                .connect()
                .syncUninterruptibly()
                .channel();
    }

    public void log(ByteBuf dataBuffer) {
        ByteBuf output = PooledByteBufAllocator.DEFAULT.buffer();
        snappy.encode(dataBuffer, output, dataBuffer.readableBytes());

        FullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1,
                HttpMethod.POST,
                logEndpoint,
                output
        );

        request.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/x-protobuf");
        request.headers().set(HttpHeaderNames.CONTENT_LENGTH, output.readableBytes());
        request.headers().set(HttpHeaderNames.HOST, host);

        channel.writeAndFlush(request);
    }

    @Override
    public void close() {
        group.shutdownGracefully();
    }
}
