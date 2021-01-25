package pl.tkowalcz.tjahzi.protobuf;

import io.netty.buffer.ByteBuf;

import static pl.tkowalcz.tjahzi.protobuf.Protobuf.LENGTH_DELIMITED_TYPE;

public class PushRequestSerializer {

    public static final int STREAM_FIELD_NUMBER = 1;

    public static void serialize(
            long timestamp,
            String logLine,
            String labels,
            ByteBuf target
    ) {
        int messageStartIndex = target.writerIndex();
        target.writeInt(0);

        target.writeByte(STREAM_FIELD_NUMBER << 3 | LENGTH_DELIMITED_TYPE);
        StreamSerializer.serialize(timestamp, logLine, labels, target);

        Protobuf.writeSize(target, messageStartIndex);
    }
}
