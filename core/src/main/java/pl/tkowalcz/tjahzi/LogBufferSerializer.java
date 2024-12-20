package pl.tkowalcz.tjahzi;

import org.agrona.concurrent.AtomicBuffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class LogBufferSerializer {

    private final AtomicBuffer buffer;

    public LogBufferSerializer(AtomicBuffer buffer) {
        this.buffer = buffer;
    }

    public int calculateRequiredSize(
            LabelSerializer labelSerializer,
            ByteBuffer line
    ) {
        int logLineSize = Integer.BYTES + line.remaining();

        int nanosecondResolutionTimestampSize = 2 * Long.BYTES;
        int labelsCountSize = Integer.BYTES;
        int headerAndLabelsSize = nanosecondResolutionTimestampSize
                + labelsCountSize
                + labelSerializer.sizeBytes();

        return headerAndLabelsSize + logLineSize;
    }

    public void writeTo(
            int cursor,
            long epochMillisecond,
            long nanoOfMillisecond,
            LabelSerializer serializedLabels,
            ByteBuffer line
    ) {
        cursor = writeHeader(
                cursor,
                epochMillisecond,
                nanoOfMillisecond,
                serializedLabels
        );

        cursor = writeLabels(cursor, serializedLabels);
        writeLogLine(cursor, line);
    }

    private int writeHeader(
            int cursor,
            long epochMillisecond,
            long nanoOfMillisecond,
            LabelSerializer serializedLabels
    ) {
        buffer.putLong(cursor, epochMillisecond, ByteOrder.LITTLE_ENDIAN);
        cursor += Long.BYTES;

        buffer.putLong(cursor, nanoOfMillisecond, ByteOrder.LITTLE_ENDIAN);
        cursor += Long.BYTES;

        buffer.putInt(cursor, serializedLabels.getLabelsCount(), ByteOrder.LITTLE_ENDIAN);
        cursor += Integer.BYTES;
        return cursor;
    }

    private int writeLabels(int cursor, LabelSerializer serializedLabels) {
        int remaining = serializedLabels.sizeBytes();
        buffer.putBytes(cursor, serializedLabels.getBuffer(), 0, remaining);

        return cursor + remaining;
    }

    private void writeLogLine(int cursor, ByteBuffer line) {
        buffer.putInt(cursor, line.remaining(), ByteOrder.LITTLE_ENDIAN);
        cursor += Integer.BYTES;

        buffer.putBytes(
                cursor,
                line,
                line.remaining()
        );
    }
}
