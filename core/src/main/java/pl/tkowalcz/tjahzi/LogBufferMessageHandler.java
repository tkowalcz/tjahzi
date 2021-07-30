package pl.tkowalcz.tjahzi;

import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.MessageHandler;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;

import java.util.Map;

public class LogBufferMessageHandler implements MessageHandler {

    private final LogBufferTranscoder logBufferTranscoder;
    private final OutputBuffer outputBuffer;

    public LogBufferMessageHandler(
            ManyToOneRingBuffer logBuffer,
            Map<String, String> staticLabels,
            OutputBuffer outputBuffer
    ) {
        this.outputBuffer = outputBuffer;

        this.logBufferTranscoder = new LogBufferTranscoder(
                staticLabels,
                logBuffer.buffer()
        );
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
        logBufferTranscoder.deserializeIntoByteBuf(
                buffer,
                index,
                outputBuffer
        );
    }
}
