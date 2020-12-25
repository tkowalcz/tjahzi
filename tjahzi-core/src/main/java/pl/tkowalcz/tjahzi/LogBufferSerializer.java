package pl.tkowalcz.tjahzi;

import org.agrona.concurrent.AtomicBuffer;

import java.nio.ByteBuffer;
import java.util.Map;

public class LogBufferSerializer {

    private final AtomicBuffer buffer;

    public LogBufferSerializer(AtomicBuffer buffer) {
        this.buffer = buffer;
    }

    public void writeTo(
            int cursor,
            long timestamp,
            Map<String, String> labels,
            String logLevelLabel,
            String logLevel,
            ByteBuffer line
    ) {
        cursor = writeHeader(cursor, timestamp, labels);
        cursor = writeLabels(cursor, labels);
        cursor = writeLabel(
                cursor,
                logLevelLabel,
                logLevel
        );
        writeLogLine(cursor, line);
    }

    private void writeLogLine(int cursor, ByteBuffer line) {
        buffer.putInt(cursor, line.remaining());
        cursor += Integer.BYTES;

        buffer.putBytes(
                cursor,
                line,
                line.remaining()
        );
    }

    private int writeLabels(int cursor, Map<String, String> labels) {
        for (Map.Entry<String, String> entry : labels.entrySet()) {
            cursor = writeLabel(
                    cursor,
                    entry.getKey(),
                    entry.getValue()
            );
        }
        return cursor;
    }

    private int writeHeader(int cursor, long timestamp, Map<String, String> labels) {
        buffer.putLong(cursor, timestamp);
        cursor += Long.BYTES;

        buffer.putInt(cursor, labels.size() + 1);
        cursor += Integer.BYTES;
        return cursor;
    }

    public int writeLabel(
            int cursor,
            String key,
            String value
    ) {
        cursor += buffer.putStringAscii(cursor, key);
        cursor += buffer.putStringAscii(cursor, value);

        return cursor;
    }

    private int calculateRequiredSizeAscii(Map<String, String> labels) {
        int timestampSize = Long.BYTES;
        int labelsCountSize = Integer.BYTES;
        int sum = timestampSize + labelsCountSize;

        for (Map.Entry<String, String> entry : labels.entrySet()) {
            sum += calculateLabeSizeAscii(entry.getKey(), entry.getValue());
        }

        return sum;
    }

    private int calculateLabeSizeAscii(String key, String value) {
        return Integer.BYTES + key.length()
                + Integer.BYTES + value.length();
    }

    public int calculateRequiredSizeAscii(
            Map<String, String> labels,
            String logLevelLabel,
            String logLevel,
            ByteBuffer line
    ) {
        int lineSize = Integer.BYTES + line.remaining();
        int logLevelSize = calculateLabeSizeAscii(logLevelLabel, logLevel);

        return calculateRequiredSizeAscii(labels) + lineSize + logLevelSize;
    }
}
