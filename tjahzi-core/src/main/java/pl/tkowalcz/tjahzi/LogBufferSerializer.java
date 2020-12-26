package pl.tkowalcz.tjahzi;

import org.agrona.concurrent.AtomicBuffer;

import java.nio.ByteBuffer;
import java.util.Map;

public class LogBufferSerializer {

    private final AtomicBuffer buffer;

    public LogBufferSerializer(AtomicBuffer buffer) {
        this.buffer = buffer;
    }

    public int calculateRequiredSizeAscii(
            Map<String, String> labels,
            String logLevelLabel,
            String logLevel,
            ByteBuffer line
    ) {
        int logLineSize = Integer.BYTES + line.remaining();
        int logLevelSize = calculateOptionalLabelSizeAscii(logLevelLabel, logLevel);
        int headerAndLabelsSize = calculateRequiredSizeAscii(labels);

        return headerAndLabelsSize + logLineSize + logLevelSize;
    }

    public void writeTo(
            int cursor,
            long timestamp,
            Map<String, String> labels,
            String logLevelLabel,
            String logLevel,
            ByteBuffer line
    ) {
        cursor = writeHeader(
                cursor,
                timestamp,
                labels,
                logLevelLabel != null
        );

        cursor = writeLabels(cursor, labels);
        cursor = writeOptionalLabel(
                cursor,
                logLevelLabel,
                logLevel
        );

        writeLogLine(cursor, line);
    }

    private int writeHeader(
            int cursor,
            long timestamp,
            Map<String, String> labels,
            boolean hasLogLevelLabel
    ) {
        buffer.putLong(cursor, timestamp);
        cursor += Long.BYTES;

        int labelsCount = labels.size();
        if (hasLogLevelLabel) {
            labelsCount++;
        }

        buffer.putInt(cursor, labelsCount);
        cursor += Integer.BYTES;
        return cursor;
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

    private void writeLogLine(int cursor, ByteBuffer line) {
        buffer.putInt(cursor, line.remaining());
        cursor += Integer.BYTES;

        buffer.putBytes(
                cursor,
                line,
                line.remaining()
        );
    }

    private int writeLabel(
            int cursor,
            String key,
            String value
    ) {
        cursor += buffer.putStringAscii(cursor, key);
        cursor += buffer.putStringAscii(cursor, value);

        return cursor;
    }

    private int writeOptionalLabel(
            int cursor,
            String key,
            String value
    ) {
        if (key != null) {
            cursor += buffer.putStringAscii(cursor, key);
            cursor += buffer.putStringAscii(cursor, value);
        }

        return cursor;
    }

    private int calculateRequiredSizeAscii(Map<String, String> labels) {
        int timestampSize = Long.BYTES;
        int labelsCountSize = Integer.BYTES;
        int sum = timestampSize + labelsCountSize;

        for (Map.Entry<String, String> entry : labels.entrySet()) {
            sum += calculateLabelSizeAscii(entry.getKey(), entry.getValue());
        }

        return sum;
    }

    private int calculateOptionalLabelSizeAscii(String key, String value) {
        if (key == null) {
            return 0;
        }

        return Integer.BYTES + key.length()
                + Integer.BYTES + value.length();
    }

    private int calculateLabelSizeAscii(String key, String value) {
        return Integer.BYTES + key.length()
                + Integer.BYTES + value.length();
    }
}
