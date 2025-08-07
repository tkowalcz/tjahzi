package pl.tkowalcz.tjahzi;

import org.agrona.DirectBuffer;

public class StructuredMetadataPointer {

    private DirectBuffer buffer;
    private int index;
    private int size;

    public StructuredMetadataPointer() {
    }

    public StructuredMetadataPointer(DirectBuffer buffer, int index, int size) {
        this.buffer = buffer;
        this.index = index;
        this.size = size;
    }

    public void wrap(DirectBuffer buffer, int index, int size) {
        this.buffer = buffer;
        this.index = index;
        this.size = size;
    }

    public DirectBuffer getBuffer() {
        return buffer;
    }

    public int getIndex() {
        return index;
    }

    public int getSize() {
        return size;
    }

    public boolean hasBytes() {
        return size > 0;
    }

    public int readInt() {
        int result = buffer.getInt(index);
        index += Integer.BYTES;

        return result;
    }

    public String getStringAscii() {
        String stringAscii = buffer.getStringAscii(index);
        index += stringAscii.length() + Integer.BYTES;

        return stringAscii;
    }
}
