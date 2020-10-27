package pl.tkowalcz.tjahzi;

import org.agrona.concurrent.AtomicBuffer;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;

import java.nio.ByteBuffer;
import java.util.Map;

public class TjahziLogger {

    public static final int LOG_MESSAGE_TYPE_ID = 5;

    private final ManyToOneRingBuffer logBuffer;

    public TjahziLogger(ManyToOneRingBuffer logBuffer) {
        this.logBuffer = logBuffer;
    }

    public TjahziLogger log(long timestamp,
                            Map<String, String> labels,
                            String line) {
        int requiredSize = calculateRequiredSizeAscii(labels, line);
        int claim = logBuffer.tryClaim(LOG_MESSAGE_TYPE_ID, requiredSize);

        int cursor = claim;
        if (requiredSize > 0) {
            try {
                AtomicBuffer buffer = this.logBuffer.buffer();

                buffer.putLong(cursor, timestamp);
                cursor += Long.BYTES;

                buffer.putInt(cursor, labels.size());
                cursor += Integer.BYTES;

                for (Map.Entry<String, String> entry : labels.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();

                    cursor += buffer.putStringAscii(cursor, key);
                    cursor += buffer.putStringAscii(cursor, value);
                }

                buffer.putStringAscii(cursor, line);
                logBuffer.commit(claim);
            } catch (Throwable t) {
                logBuffer.abort(claim);
            }
        }

        return this;
    }

    public TjahziLogger log(long timestamp,
                            Map<String, String> labels,
                            ByteBuffer line) {
        int requiredSize = calculateRequiredSizeAscii(labels, line);
        int claim = logBuffer.tryClaim(LOG_MESSAGE_TYPE_ID, requiredSize);

        int cursor = claim;
        if (requiredSize > 0) {
            try {
                AtomicBuffer buffer = this.logBuffer.buffer();

                buffer.putLong(cursor, timestamp);
                cursor += Long.BYTES;

                buffer.putInt(cursor, labels.size());
                cursor += Integer.BYTES;

                for (Map.Entry<String, String> entry : labels.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();

                    cursor += buffer.putStringAscii(cursor, key);
                    cursor += buffer.putStringAscii(cursor, value);
                }

                buffer.putInt(cursor, line.remaining());
                cursor += Integer.BYTES;
                buffer.putBytes(
                        cursor,
                        line,
                        line.remaining()
                );
                logBuffer.commit(claim);
            } catch (Throwable t) {
                logBuffer.abort(claim);
            }
        }

        return this;
    }

    private int calculateRequiredSizeAscii(Map<String, String> labels) {
        return Long.BYTES +
                Integer.BYTES +
                labels
                        .entrySet()
                        .stream()
                        .mapToInt(entry -> entry.getKey().length() + entry.getValue().length())
                        .map(size -> size + 2 * Integer.BYTES)
                        .sum();
    }

    private int calculateRequiredSizeAscii(
            Map<String, String> labels,
            String line
    ) {
        return calculateRequiredSizeAscii(labels) +
                line.length() + Integer.BYTES;
    }

    private int calculateRequiredSizeAscii(
            Map<String, String> labels,
            ByteBuffer line
    ) {
        return calculateRequiredSizeAscii(labels) +
                line.remaining() + Integer.BYTES;
    }
}
