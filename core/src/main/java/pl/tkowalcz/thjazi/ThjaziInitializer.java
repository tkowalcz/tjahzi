package pl.tkowalcz.thjazi;

import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.SleepingMillisIdleStrategy;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;
import org.agrona.concurrent.ringbuffer.RingBufferDescriptor;
import pl.tkowalcz.thjazi.http.NettyHttpClient;

import java.nio.ByteBuffer;

public class ThjaziInitializer {

    public LoggingSystem createLoggingSystem(
            NettyHttpClient httpClient,
            int bufferSizeBytes,
            boolean offHeap) {

        verifyIsPowerOfTwo(bufferSizeBytes);
        ByteBuffer javaBuffer = allocateJavaBuffer(bufferSizeBytes, offHeap);

        ManyToOneRingBuffer logBuffer = new ManyToOneRingBuffer(
                new UnsafeBuffer(javaBuffer)
        );

        LogBufferAgent agent = new LogBufferAgent(
                logBuffer,
                httpClient
        );

        AgentRunner runner = new AgentRunner(
                new SleepingMillisIdleStrategy(),
                Throwable::printStackTrace,
                null,
                agent
        );

        AgentRunner.startOnThread(runner);

        return new LoggingSystem(
                logBuffer
        );
    }

    private void verifyIsPowerOfTwo(int bufferSize) {
        if (bufferSize < 0 || Integer.bitCount(bufferSize) != 1) {
            throw new IllegalArgumentException("Invalid buffer size (should be power of two: " + bufferSize);
        }
    }

    private ByteBuffer allocateJavaBuffer(
            int bufferSize,
            boolean offHeap) {
        int totalSize = bufferSize + RingBufferDescriptor.TRAILER_LENGTH;

        if (offHeap) {
            return ByteBuffer.allocateDirect(totalSize);
        }

        return ByteBuffer.allocate(totalSize);
    }
}
