package pl.tkowalcz.tjahzi.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.handler.codec.compression.Snappy;
import io.netty.handler.codec.http.*;

import java.io.Closeable;

public class NettyHttpClient implements Closeable {

    public static final String PROTOBUF_MIME_TYPE = "application/x-protobuf";

    private final ClientConfiguration clientConfiguration;
    private final HttpHeaders headers;

    private final Snappy snappy = new Snappy();

    //        private volatile ChannelFuture lokiConnection;
    private HttpConnection lokiConnection;

    public NettyHttpClient(
            ClientConfiguration clientConfiguration,
            String[] additionalHeaders) {
        this.clientConfiguration = clientConfiguration;
        this.headers = new ReadOnlyHttpHeaders(
                true,
                additionalHeaders
        );

        lokiConnection = new HttpConnection(clientConfiguration);
    }

    public void log(ByteBuf dataBuffer) {
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

        lokiConnection.execute(request);
    }

    @Override
    public void close() {
        lokiConnection.close();
    }
}
