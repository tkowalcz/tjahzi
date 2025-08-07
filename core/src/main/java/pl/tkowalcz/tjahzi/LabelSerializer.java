package pl.tkowalcz.tjahzi;

import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.concurrent.AtomicBuffer;

import java.nio.ByteOrder;

public class LabelSerializer {

    private final ExpandableArrayBuffer buffer = new ExpandableArrayBuffer();

    private int cursor;
    private int labelsCount;

    private int lastSizePosition;

    public void clear() {
        cursor = 0;
        labelsCount = 0;
    }

    public int sizeBytes() {
        return cursor;
    }

    public int getLabelsCount() {
        return labelsCount;
    }

    public LabelSerializer appendLabelName(String key) {
        cursor += buffer.putStringAscii(cursor, key, ByteOrder.LITTLE_ENDIAN);
        labelsCount++;

        return this;
    }

    public LabelSerializer startAppendingLabelValue() {
        lastSizePosition = cursor;
        cursor += Integer.BYTES;

        return this;
    }

    public LabelSerializer appendPartialLabelValue(CharSequence value) {
        cursor += buffer.putStringWithoutLengthAscii(cursor, value);

        return this;
    }

    public LabelSerializer finishAppendingLabelValue() {
        buffer.putInt(
                lastSizePosition,
                cursor - lastSizePosition - Integer.BYTES,
                ByteOrder.LITTLE_ENDIAN
        );

        return this;
    }

    public LabelSerializer appendLabel(String key, String value) {
        appendLabelName(key);

        startAppendingLabelValue();
        appendPartialLabelValue(value);
        finishAppendingLabelValue();

        return this;
    }

    public DirectBuffer getBuffer() {
        return buffer;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof LabelSerializer) {
            LabelSerializer that = (LabelSerializer) o;

            if (this.cursor != that.cursor) {
                return false;
            }

            byte[] thisArray = this.buffer.byteArray();
            byte[] thatArray = that.buffer.byteArray();

            for (int i = 0; i < cursor; i++) {
                if (thisArray[i] != thatArray[i]) {
                    return false;
                }
            }

            return true;
        }

        return false;
    }

    public int writeLabels(AtomicBuffer target, int targetCursor) {
        int remaining = sizeBytes();
        target.putInt(targetCursor, remaining, ByteOrder.LITTLE_ENDIAN);
        targetCursor += Integer.BYTES;

        target.putInt(targetCursor, labelsCount, ByteOrder.LITTLE_ENDIAN);
        targetCursor += Integer.BYTES;

        target.putBytes(targetCursor, buffer, 0, remaining);

        return targetCursor + remaining;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        int index = 0;
        while (index < cursor) {
            index += buffer.getStringAscii(index, result, ByteOrder.LITTLE_ENDIAN) + Integer.BYTES;
        }

        return result.toString();
    }
}
