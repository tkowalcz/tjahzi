package pl.tkowalcz.tjahzi.log4j2;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.layout.ByteBufferDestination;
import org.apache.logging.log4j.core.layout.ByteBufferDestinationHelper;

import java.nio.ByteBuffer;
import java.util.function.BiConsumer;

public class ByteBufferDestinations implements ByteBufferDestination {

    private static final ThreadLocal<ByteBufferDestinations> THREAD_LOCAL = ThreadLocal.withInitial(ByteBufferDestinations::new);

    /**
     * 10kb should be enough for everyone. Even in case of 1000s of threads we will allocate at most 10s of MB of buffers.
     * Larger messages will be fragmented.
     */
    private final ByteBuffer buffer = ByteBuffer.allocate(10 * 1024);

    private BiConsumer<LogEvent, ByteBuffer> drain;
    private LogEvent context;

    public static ByteBufferDestinations threadLocal() {
        ByteBufferDestinations result = THREAD_LOCAL.get();
        result.getByteBuffer().clear();

        return result;
    }

    public void drainRemaining() {
        if (buffer.hasRemaining()) {
            drain(buffer);
        }
    }

    @Override
    public ByteBuffer getByteBuffer() {
        return buffer;
    }

    @Override
    public ByteBuffer drain(ByteBuffer buf) {
        buf.flip();
        drain.accept(context, buf);

        buf.clear();
        return buf;
    }

    @Override
    public void writeBytes(ByteBuffer data) {
        ByteBufferDestinationHelper.writeToUnsynchronized(data, this);
    }

    @Override
    public void writeBytes(byte[] data, int offset, int length) {
        ByteBufferDestinationHelper.writeToUnsynchronized(data, offset, length, this);
    }

    public void initialize(BiConsumer<LogEvent, ByteBuffer> drain, LogEvent context) {
        this.context = context;
        this.drain = drain;
    }
}
