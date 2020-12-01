package pl.tkowalcz.tjahzi.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.compression.Snappy;
import io.netty.handler.codec.http.*;

import java.io.Closeable;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class NettyHttpClient implements Closeable {

    public static final String PROTOBUF_MIME_TYPE = "application/x-protobuf";

    private final ClientConfiguration clientConfiguration;
    private final HttpHeaders headers;

    private final Snappy snappy = new Snappy();

    private final EventLoopGroup group;
        private volatile ChannelFuture lokiConnection;
//    private HttpConnection lokiConnection;

    public NettyHttpClient(
            ClientConfiguration clientConfiguration,
            String[] additionalHeaders) {
        this.clientConfiguration = clientConfiguration;
        this.headers = new ReadOnlyHttpHeaders(
                true,
                additionalHeaders
        );

        ThreadGroup threadGroup = new ThreadGroup("Tjahzi Loki client");
        ThreadFactory threadFactory = r -> new Thread(threadGroup, r, "tjahzi-worker");
        NioEventLoopGroup group = new NioEventLoopGroup(1, threadFactory);
        this.group = group;

        foobar(clientConfiguration);
    }

    private void foobar(ClientConfiguration clientConfiguration) {
        lokiConnection = BootstrapUtil.initConnection(
                group,
                clientConfiguration
        );

        lokiConnection.addListener(future -> {
            System.out.println("NettyHttpClient.foobar");
            if (!future.isSuccess()) {
                group.schedule(() -> {
                            foobar(clientConfiguration);
                        },
                        1000, TimeUnit.MILLISECONDS);
                // TODO: increase error counters

            }
        });
    }

    public void log(ByteBuf dataBuffer) {
        log(dataBuffer, clientConfiguration.getMaxRetries());
    }

    public void log(ByteBuf dataBuffer, int retries) {
        lokiConnection.awaitUninterruptibly();
        if (lokiConnection.isSuccess()) {
            ByteBuf output = PooledByteBufAllocator.DEFAULT.buffer();
            snappy.encode(dataBuffer, output, dataBuffer.readableBytes());

            FullHttpRequest request = new DefaultFullHttpRequest(
                    HttpVersion.HTTP_1_1,
                    HttpMethod.POST,
                    clientConfiguration.getLogEndpoint(),
                    output
            );

            request.headers()
                    .add(headers)
                    .set(HttpHeaderNames.CONTENT_TYPE, PROTOBUF_MIME_TYPE)
                    .set(HttpHeaderNames.CONTENT_LENGTH, output.readableBytes())
                    .set(HttpHeaderNames.HOST, clientConfiguration.getHost());

            lokiConnection.channel().writeAndFlush(request);
        } else {
            // TODO: increase error counters

            if (retries > 0) {
                log(dataBuffer, retries - 1);
            }

            // Dropping message on the floor
        }
    }

    @Override
    public void close() {
        group.shutdownGracefully();
    }
}
