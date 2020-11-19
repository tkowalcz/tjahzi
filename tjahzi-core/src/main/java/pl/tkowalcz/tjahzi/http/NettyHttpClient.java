package pl.tkowalcz.tjahzi.http;

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

    public static final String PROTOBUF_MIME_TYPE = "application/x-protobuf";

    private final EventLoopGroup group;
    private final String logEndpoint;

    private final Channel channel;
    private final String host;
    private final HttpHeaders headers;

    private final Snappy snappy = new Snappy();

    public NettyHttpClient(
            ClientConfiguration clientConfiguration,
            String[] additionalHeaders) {
        host = clientConfiguration.getHost();
        logEndpoint = clientConfiguration.getLogEndpoint();

        ThreadGroup threadGroup = new ThreadGroup("Tjahzi Loki client");
        ThreadFactory threadFactory = r -> new Thread(threadGroup, r, "tjahzi-worker");
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

        headers = new ReadOnlyHttpHeaders(
                true,
                additionalHeaders
        );
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

        request.headers()
                .add(headers)
                .set(HttpHeaderNames.CONTENT_TYPE, PROTOBUF_MIME_TYPE)
                .set(HttpHeaderNames.CONTENT_LENGTH, output.readableBytes())
                .set(HttpHeaderNames.HOST, host);

        channel.writeAndFlush(request);
    }

    @Override
    public void close() {
        group.shutdownGracefully();
    }
}
