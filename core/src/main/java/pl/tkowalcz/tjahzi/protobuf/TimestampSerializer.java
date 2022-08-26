package pl.tkowalcz.tjahzi.protobuf;

import io.netty.buffer.ByteBuf;

import static pl.tkowalcz.tjahzi.protobuf.Protobuf.VARINT_TYPE;
import static pl.tkowalcz.tjahzi.protobuf.Protobuf.writeUnsignedVarint;

public class TimestampSerializer {

    public static void serialize(
            long epochMillisecond,
            long nanoOfMillisecond,
            ByteBuf target
    ) {
        long timestampSeconds = epochMillisecond / 1000;
        long timestampNanos = (epochMillisecond % 1000) * 1000_000 + nanoOfMillisecond;

        int messageStartIndex = target.writerIndex();

        target.writeInt(0);
        target.writeByte(1 << 3 | VARINT_TYPE);
        writeUnsignedVarint(timestampSeconds, target);
        target.writeByte(2 << 3 | VARINT_TYPE);
        writeUnsignedVarint(timestampNanos, target);

        Protobuf.writeSize(target, messageStartIndex);
    }
}
