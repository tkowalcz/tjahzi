package pl.tkowalcz.tjahzi.log4j2;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.layout.ByteBufferDestination;
import org.apache.logging.log4j.core.layout.ByteBufferDestinationHelper;

import java.nio.ByteBuffer;
import java.util.function.BiConsumer;

public class TjahziByteBufferDestination implements ByteBufferDestination {

    private final ByteBuffer buffer;

    private BiConsumer<LogEvent, ByteBuffer> drain;
    private LogEvent context;

    public TjahziByteBufferDestination(int maxLogLineSizeBytes) {
        buffer = ByteBuffer.allocate(maxLogLineSizeBytes);
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

        buffer.clear();
    }
}
