package pl.tkowalcz.tjahzi.protobuf;

import com.google.common.primitives.Ints;
import io.netty.buffer.ByteBuf;

public class Protobuf {

    public static final int LENGTH_DELIMITED_TYPE = 0x2;
    public static final int END_GROUP_TYPE = 0x4;

    public static final int VARINT_TYPE = 0x0;

    public static void writeSize(ByteBuf target, int messageStartIndex) {
        int messageSize = target.writerIndex() - messageStartIndex - Integer.BYTES;
        target.setInt(
                messageStartIndex,
                getFixed32Varint(messageSize)
        );
    }

    public static int getFixed32Varint(int value) {
        int byte1 = (value & 0x07F) | 0x80;
        int byte2 = ((value >>> 7) & 0x07F) | 0x80;
        int byte3 = ((value >>> 14) & 0x07F) | 0x80;
        int byte4 = ((value >>> 21) & 0x07F);

        return Ints.fromBytes(
                (byte) byte1,
                (byte) byte2,
                (byte) byte3,
                (byte) byte4
        );
    }

    public static void writeUnsignedVarint(long value, ByteBuf target) {
        while (true) {
            if ((value & ~0x07FL) == 0) {
                target.writeByte((int) value);
                return;
            } else {
                target.writeByte(((int) value & 0x7F) | 0x80);
                value >>>= 7;
            }
        }
    }
}
