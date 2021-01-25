package pl.tkowalcz.tjahzi.protobuf;

import io.netty.buffer.ByteBuf;

import static pl.tkowalcz.tjahzi.protobuf.Protobuf.VARINT_TYPE;
import static pl.tkowalcz.tjahzi.protobuf.Protobuf.writeUnsignedVarint;

public class TimestampSerializer {

    public static void serialize(
            long timestamp,
            ByteBuf target
    ) {
        long timestampSeconds = timestamp / 1000;
        int timestampNanos = (int) (timestamp % 1000) * 1000_000;

        int messageStartIndex = target.writerIndex();

        target.writeInt(0);
        target.writeByte(1 << 3 | VARINT_TYPE);
        writeUnsignedVarint(timestampSeconds, target);
        target.writeByte(2 << 3 | VARINT_TYPE);
        writeUnsignedVarint(timestampNanos, target);

        Protobuf.writeSize(target, messageStartIndex);
    }
}
