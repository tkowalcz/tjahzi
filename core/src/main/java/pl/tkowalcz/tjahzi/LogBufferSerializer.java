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
            LabelSerializer structuredMetadata,
            ByteBuffer line
    ) {
        int logLineSize = Integer.BYTES + line.remaining();

        int nanosecondResolutionTimestampSize = 2 * Long.BYTES;
        int labelsCountSize = Integer.BYTES + Integer.BYTES;
        int headerAndLabelsSize = nanosecondResolutionTimestampSize
                                  + labelsCountSize
                                  + labelSerializer.sizeBytes();

        int structuredMetadataSize = structuredMetadata.sizeBytes() + Integer.BYTES + Integer.BYTES;
        return headerAndLabelsSize + logLineSize + structuredMetadataSize;
    }

    public void writeTo(
            int cursor,
            long epochMillisecond,
            long nanoOfMillisecond,
            LabelSerializer serializedLabels,
            LabelSerializer structuredMetadata,
            ByteBuffer line
    ) {
        cursor = writeHeader(
                cursor,
                epochMillisecond,
                nanoOfMillisecond
        );

        cursor = serializedLabels.writeLabels(buffer, cursor);
        cursor = structuredMetadata.writeLabels(buffer, cursor);
        writeLogLine(cursor, line);
    }

    private int writeHeader(
            int cursor,
            long epochMillisecond,
            long nanoOfMillisecond
    ) {
        buffer.putLong(cursor, epochMillisecond, ByteOrder.LITTLE_ENDIAN);
        cursor += Long.BYTES;

        buffer.putLong(cursor, nanoOfMillisecond, ByteOrder.LITTLE_ENDIAN);
        cursor += Long.BYTES;

        return cursor;
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
