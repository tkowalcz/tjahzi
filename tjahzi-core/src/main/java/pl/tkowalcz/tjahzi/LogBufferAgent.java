package pl.tkowalcz.tjahzi;

import logproto.Logproto;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.MessageHandler;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;
import pl.tkowalcz.tjahzi.http.NettyHttpClient;

import java.io.IOException;
import java.time.Clock;
import java.util.Map;

public class LogBufferAgent implements Agent, MessageHandler {

    public static final int MAX_MESSAGES_TO_RETRIEVE = 100;

    private final Clock clock;

    private final ManyToOneRingBuffer logBuffer;
    private final NettyHttpClient httpClient;
    private final long batchSize;
    private final long batchWaitMillis;

    private final LogBufferDeserializer logBufferDeserializer;

    private Logproto.PushRequest.Builder request = Logproto.PushRequest.newBuilder();
    private int estimatedBytesPending;
    private long timeoutDeadline;

    public LogBufferAgent(
            Clock clock,
            ManyToOneRingBuffer logBuffer,
            NettyHttpClient httpClient,
            long batchSizeBytes,
            long batchWaitMillis,
            Map<String, String> staticLabels
    ) {
        this.clock = clock;

        this.logBuffer = logBuffer;
        this.batchSize = batchSizeBytes;
        this.batchWaitMillis = batchWaitMillis;
        this.httpClient = httpClient;

        this.logBufferDeserializer = new LogBufferDeserializer(staticLabels);
    }

    @Override
    public int doWork() throws IOException {
        int workDone = logBuffer.read(this, MAX_MESSAGES_TO_RETRIEVE);

        long currentTimeMillis = clock.millis();
        if (exceededBatchSizeThreshold() || exceededWaitTimeThreshold(currentTimeMillis)) {
            try {
                httpClient.log(request);
            } finally {
                request = Logproto.PushRequest.newBuilder();

                estimatedBytesPending = 0;
                timeoutDeadline = currentTimeMillis + batchWaitMillis;
            }
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
            estimatedBytesPending += length;
            processMessage(buffer, index);
        } else {
            // Ignore
        }
    }

    @Override
    public String roleName() {
        return "ReadingLogBufferAndSendingHttp";
    }

    private boolean exceededWaitTimeThreshold(long currentTimeMillis) {
        return currentTimeMillis > timeoutDeadline & estimatedBytesPending > 0;
    }

    private boolean exceededBatchSizeThreshold() {
        return estimatedBytesPending > batchSize;
    }

    private void processMessage(MutableDirectBuffer buffer, int index) {
        Logproto.StreamAdapter stream = logBufferDeserializer.deserializeIntoProtobuf(
                buffer,
                index
        );

        request.addStreams(stream);
    }
}
