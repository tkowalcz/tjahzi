package pl.tkowalcz.tjahzi;

import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;

public class LabelSerializer {

    private final ExpandableArrayBuffer buffer = new ExpandableArrayBuffer();

    private int cursor;
    private int labelsCount;

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
        cursor += buffer.putStringAscii(cursor, key);
        labelsCount++;

        return this;
    }

    public void appendLabelValue(String value) {
        cursor += buffer.putStringAscii(cursor, value);
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

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        int index = 0;
        while (index < cursor) {
            index += buffer.getStringAscii(index, result) + Integer.BYTES;
        }

        return result.toString();
    }
}
