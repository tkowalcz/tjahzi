package pl.tkowalcz.tjahzi.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCountUtil;
import pl.tkowalcz.tjahzi.OutputBuffer;
import pl.tkowalcz.tjahzi.stats.MonitoringModule;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class NettyHttpClient implements Closeable {

    public static final String PROTOBUF_MIME_TYPE = "application/x-protobuf";

    private final ClientConfiguration clientConfiguration;
    private final HttpHeaders headers;

    private final Snappy snappy = new Snappy();
    private final HttpConnection lokiConnection;

    public NettyHttpClient(
            ClientConfiguration clientConfiguration,
            MonitoringModule monitoringModule,
            String... additionalHeaders
    ) {
        this.clientConfiguration = clientConfiguration;

        String[] finalAdditionalHeaders = BootstrapUtil.createAuthString(
                clientConfiguration.getUsername(),
                clientConfiguration.getPassword()
        ).map(authString -> {
            ArrayList<String> newHeaders = new ArrayList<>(Arrays.asList(additionalHeaders));
            newHeaders.add(HttpHeaderNames.AUTHORIZATION.toString());
            newHeaders.add(authString);

            return newHeaders.toArray(new String[0]);
        }).orElse(additionalHeaders);

        this.headers = new ReadOnlyHttpHeaders(
                true,
                finalAdditionalHeaders
        );

        lokiConnection = new HttpConnection(clientConfiguration, monitoringModule);
    }

    public void log(ByteBuf dataBuffer) {
        FullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1,
                HttpMethod.POST,
                clientConfiguration.getLogEndpoint(),
                dataBuffer
        );

        request.headers()
                .add(headers)
                .set(HttpHeaderNames.CONTENT_TYPE, PROTOBUF_MIME_TYPE)
                .set(HttpHeaderNames.CONTENT_LENGTH, dataBuffer.readableBytes())
                .set(HttpHeaderNames.HOST, clientConfiguration.getHost());

        lokiConnection.execute(request);
    }

    @Override
    public void close() {
        lokiConnection.close();
    }

    public void log(OutputBuffer outputBuffer) throws IOException {
        ByteBuf dataBuffer = outputBuffer.close();
        ByteBuf compressedBuffer = PooledByteBufAllocator.DEFAULT.buffer();

        try {
            snappy.encode(dataBuffer, compressedBuffer, dataBuffer.readableBytes());
        } catch (Exception e) {
            ReferenceCountUtil.safeRelease(compressedBuffer);
            throw e;
        }

        log(compressedBuffer);
    }
}
