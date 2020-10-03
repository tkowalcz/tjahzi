package pl.tkowalcz.thjazi.http;

import com.google.protobuf.Timestamp;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.compression.Snappy;
import io.netty.handler.codec.http.*;
import javolution.text.TextBuilder;
import logproto.Logproto;

import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ThreadFactory;

public class NettyHttpClient implements AutoCloseable {

    private final EventLoopGroup group;
    private final String logEndpoint;

    private final Channel channel;
    private final Bootstrap bootstrap;

    private final Clock clock;
    private final String host;
    private final int port;

    public NettyHttpClient(
            Clock clock,
            ClientConfiguration clientConfiguration
    ) {
        this.clock = clock;
        host = clientConfiguration.getHost();
        port = clientConfiguration.getPort();

        ThreadGroup threadGroup = new ThreadGroup("Thjazi Loki client");
        ThreadFactory threadFactory = r -> new Thread(threadGroup, r, "thjazi-worker");
        group = new NioEventLoopGroup(1, threadFactory);

        bootstrap = new Bootstrap();
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

        logEndpoint = clientConfiguration.getLogEndpoint();
    }

    public void log(
            long timestamp,
            Map<String, String> labels,
            String line
    ) throws IOException {
        CharSequence labelsString = buildLabelsString(labels);
        Logproto.PushRequest pushRequest = Logproto.PushRequest.newBuilder()
                .addStreams(Logproto.StreamAdapter.newBuilder()
                        .setLabels(labelsString.toString())
                        .addEntries(Logproto.EntryAdapter.newBuilder()
                                .setTimestamp(Timestamp.newBuilder()
                                        .setSeconds(timestamp / 1000)
                                        .setNanos((int) (timestamp % 1000) * 1000_000))
                                .setLine(line)
                        )
                )
                .build();

        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer();
        pushRequest.writeTo(
                new ByteBufOutputStream(buffer)
        );

        ByteBuf output = PooledByteBufAllocator.DEFAULT.buffer();
        Snappy snappy = new Snappy();
        snappy.encode(buffer, output, buffer.readableBytes());

        FullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1,
                HttpMethod.POST,
               logEndpoint,
                output
        );

        request.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/x-protobuf");
        request.headers().set(HttpHeaderNames.CONTENT_LENGTH, output.readableBytes());
        request.headers().set(HttpHeaderNames.HOST, host);

        ChannelFuture channelFuture = channel.writeAndFlush(request);
        System.out.println(channelFuture.awaitUninterruptibly()
                .isSuccess());
    }

    private CharSequence buildLabelsString(Map<String, String> labels) {
        TextBuilder textBuilder = TextBuilders.threadLocal();

        textBuilder.append("{ ");
        labels.forEach((key, value) -> textBuilder.append(key)
                .append("=")
                .append("\"")
                .append(value)
                .append("\","));

        textBuilder.setCharAt(textBuilder.length() - 1, '}');
        return textBuilder;
    }

    @Override
    public void close() {
        group.shutdownGracefully();
    }
}
