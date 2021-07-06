package pl.tkowalcz.tjahzi.log4j2;

import org.apache.logging.log4j.core.LogEvent;

import java.nio.ByteBuffer;
import java.util.function.BiConsumer;

public class ByteBufferDestinationRepository {

    public static final int DEFAULT_MAX_LINE_SIZE_BYTES = 10 * 1024;

    private final ThreadLocal<TjahziByteBufferDestination> threadLocal = new ThreadLocal<>();
    private final int maxLogLineSizeBytes;

    public ByteBufferDestinationRepository(int maxLogLineSizeBytes) {
        this.maxLogLineSizeBytes = maxLogLineSizeBytes;
    }

    public TjahziByteBufferDestination threadLocalDestination(
            BiConsumer<LogEvent, ByteBuffer> drain,
            LogEvent context
    ) {
        TjahziByteBufferDestination result = threadLocal.get();

        if (result == null) {
            result = new TjahziByteBufferDestination(maxLogLineSizeBytes);
            threadLocal.set(result);
        }

        result.initialize(drain, context);
        return result;
    }
}
