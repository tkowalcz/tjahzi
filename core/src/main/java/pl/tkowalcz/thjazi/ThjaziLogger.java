package pl.tkowalcz.thjazi;

import org.agrona.concurrent.AtomicBuffer;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;

import java.util.Map;

public class ThjaziLogger {

    public static final int LOG_MESSAGE_TYPE_ID = 5;

    private final ManyToOneRingBuffer logBuffer;

    public ThjaziLogger(ManyToOneRingBuffer logBuffer) {
        this.logBuffer = logBuffer;
    }

    public ThjaziLogger log(long timestamp,
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

                claim += buffer.putStringAscii(cursor, line);
                logBuffer.commit(claim);
            } catch (Throwable t) {
                logBuffer.abort(claim);
            }
        }

        return this;
    }

    private int calculateRequiredSizeAscii(
            Map<String, String> labels,
            String line
    ) {
        return Long.BYTES +
                Integer.BYTES +
                labels
                        .entrySet()
                        .stream()
                        .mapToInt(entry -> entry.getKey().length() + entry.getValue().length())
                        .map(size -> size + 2 * Integer.BYTES)
                        .sum() +
                line.length() + Integer.BYTES;

    }
}