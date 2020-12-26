package pl.tkowalcz.tjahzi;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.PooledByteBufAllocator;
import logproto.Logproto;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.MessageHandler;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;
import pl.tkowalcz.tjahzi.http.NettyHttpClient;

import java.io.IOException;
import java.util.Map;

public class LogBufferAgent implements Agent, MessageHandler {

    public static final int MESSAGE_COUNT_LIMIT = 100;

    private final ManyToOneRingBuffer logBuffer;
    private final NettyHttpClient httpClient;

    private final Map<String, String> staticLabels;
    private final LogBufferDeserializer logBufferDeserializer = new LogBufferDeserializer();

    private Logproto.PushRequest.Builder request = Logproto.PushRequest.newBuilder();

    public LogBufferAgent(
            ManyToOneRingBuffer logBuffer,
            NettyHttpClient httpClient,
            Map<String, String> staticLabels
    ) {
        this.logBuffer = logBuffer;
        this.httpClient = httpClient;

        this.staticLabels = staticLabels;
    }

    @Override
    public int doWork() throws IOException {
        int workDone = logBuffer.read(this, MESSAGE_COUNT_LIMIT);

        if (workDone > 0) {
            ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer();
            request.build().writeTo(
                    new ByteBufOutputStream(buffer)
            );

            httpClient.log(buffer);
            request = Logproto.PushRequest.newBuilder();
        }

        return workDone;
    }

    @Override
    public void onMessage(
            int msgTypeId,
            MutableDirectBuffer buffer,
            int index,
            int length
    ) {
        if (msgTypeId == TjahziLogger.LOG_MESSAGE_TYPE_ID) {
            processMessage(buffer, index);
        } else {
            // Ignore
        }
    }

    private void processMessage(MutableDirectBuffer buffer, int index) {
        Logproto.StreamAdapter stream = logBufferDeserializer.deserialize(
                buffer,
                index,
                staticLabels
        );

        request.addStreams(stream);
    }

    @Override
    public String roleName() {
        return "ReadingLogBufferAndSendingHttp";
    }
}
