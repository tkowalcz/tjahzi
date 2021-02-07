package pl.tkowalcz.tjahzi;

import io.netty.buffer.PooledByteBufAllocator;
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

    private final OutputBuffer outputBuffer;
    private final LogBufferTranscoder logBufferTranscoder;

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

        this.outputBuffer = new OutputBuffer(PooledByteBufAllocator.DEFAULT.buffer());
        this.logBufferTranscoder = new LogBufferTranscoder(
                staticLabels,
                logBuffer.buffer()
        );
    }

    @Override
    public int doWork() throws IOException {
        int workDone = logBuffer.read(this, MAX_MESSAGES_TO_RETRIEVE);

        long currentTimeMillis = clock.millis();
        if (exceededBatchSizeThreshold() || exceededWaitTimeThreshold(currentTimeMillis)) {
            try {
                httpClient.log(outputBuffer);
            } finally {
                outputBuffer.clear();
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
        return currentTimeMillis > timeoutDeadline & outputBuffer.getBytesPending() > 0;
    }

    private boolean exceededBatchSizeThreshold() {
        return outputBuffer.getBytesPending() > batchSize;
    }

    private void processMessage(MutableDirectBuffer buffer, int index) {
        logBufferTranscoder.deserializeIntoByteBuf(
                buffer,
                index,
                outputBuffer
        );
    }
}
