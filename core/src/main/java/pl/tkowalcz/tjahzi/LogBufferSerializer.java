package pl.tkowalcz.tjahzi;

import org.agrona.concurrent.AtomicBuffer;

import java.nio.ByteBuffer;

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

        int timestampSize = Long.BYTES;
        int labelsCountSize = Integer.BYTES;
        int headerAndLabelsSize = timestampSize + labelsCountSize + labelSerializer.sizeBytes();

        return headerAndLabelsSize + logLineSize;
    }

    public void writeTo(
            int cursor,
            long timestamp,
            LabelSerializer serializedLabels,
            ByteBuffer line
    ) {
        cursor = writeHeader(
                cursor,
                timestamp,
                serializedLabels
        );

        cursor = writeLabels(cursor, serializedLabels);
        writeLogLine(cursor, line);
    }

    private int writeHeader(
            int cursor,
            long timestamp,
            LabelSerializer serializedLabels
    ) {
        buffer.putLong(cursor, timestamp);
        cursor += Long.BYTES;

        buffer.putInt(cursor, serializedLabels.getLabelsCount());
        cursor += Integer.BYTES;
        return cursor;
    }

    private int writeLabels(int cursor, LabelSerializer serializedLabels) {
        int remaining = serializedLabels.sizeBytes();
        buffer.putBytes(cursor, serializedLabels.getBuffer(), 0, remaining);

        return cursor + remaining;
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
}
