package pl.tkowalcz.tjahzi.protobuf;

import io.netty.buffer.ByteBuf;

import static pl.tkowalcz.tjahzi.protobuf.Protobuf.LENGTH_DELIMITED_TYPE;

public class StreamSerializer {

    public static final int LABELS_FIELD_NUMBER = 1;
    public static final int ENTRY_FIELD_NUMBER = 2;

    public static void serialize(
            long timestamp,
            ByteBuf logLine,
            CharSequence labels,
            ByteBuf target
    ) {
        int messageStartIndex = target.writerIndex();
        target.writeInt(0);

        target.writeByte(LABELS_FIELD_NUMBER << 3 | LENGTH_DELIMITED_TYPE);
        StringSerializer.serialize(labels, target);

        target.writeByte(ENTRY_FIELD_NUMBER << 3 | LENGTH_DELIMITED_TYPE);
        EntrySerializer.serialize(timestamp, logLine, target);

        Protobuf.writeSize(target, messageStartIndex);
    }
}
