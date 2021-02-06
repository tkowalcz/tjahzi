package pl.tkowalcz.tjahzi.protobuf;

import io.netty.buffer.ByteBuf;

import static pl.tkowalcz.tjahzi.protobuf.Protobuf.LENGTH_DELIMITED_TYPE;
import static pl.tkowalcz.tjahzi.protobuf.Protobuf.writeSize;

public class EntrySerializer {

    public static final int TIMESTAMP_FIELD_NUMBER = 1;
    public static final int LOG_LINE_FIELD_NUMBER = 2;

    public static void serialize(
            long timestamp,
            ByteBuf logLine,
            ByteBuf target
    ) {
        int messageStartIndex = target.writerIndex();
        target.writeInt(0);

        target.writeByte(TIMESTAMP_FIELD_NUMBER << 3 | LENGTH_DELIMITED_TYPE);
        TimestampSerializer.serialize(timestamp, target);

        target.writeByte(LOG_LINE_FIELD_NUMBER << 3 | LENGTH_DELIMITED_TYPE);
        StringSerializer.serialize(logLine, target);

        writeSize(target, messageStartIndex);
    }
}
