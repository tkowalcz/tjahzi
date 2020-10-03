package pl.tkowalcz.thjazi;

import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;
import org.agrona.concurrent.ringbuffer.RingBufferDescriptor;

import java.nio.ByteBuffer;

public class ThjaziInitializer {

    public LoggingSystem createLoggingSystem(
            int bufferSizeBytes,
            boolean offHeap) {

        verifyIsPowerOfTwo(bufferSizeBytes);
        ByteBuffer javaBuffer = allocateJavaBuffer(bufferSizeBytes, offHeap);

        return new LoggingSystem(
                new ManyToOneRingBuffer(
                        new UnsafeBuffer(javaBuffer)
                )
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
