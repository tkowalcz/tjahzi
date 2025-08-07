package pl.tkowalcz.tjahzi.protobuf;

import io.netty.buffer.ByteBuf;

import static pl.tkowalcz.tjahzi.protobuf.Protobuf.LENGTH_DELIMITED_TYPE;
import static pl.tkowalcz.tjahzi.protobuf.Protobuf.writeSize;

public class LabelSerializer {

    public static final int NAME_FIELD_NUMBER = 1;
    public static final int VALUE_FIELD_NUMBER = 2;

    public static void serialize(CharSequence name, CharSequence value, ByteBuf target) {
        int messageStartIndex = target.writerIndex();
        target.writeInt(0);

        if (name.length() > 0) {
            target.writeByte(NAME_FIELD_NUMBER << 3 | LENGTH_DELIMITED_TYPE);
            StringSerializer.serialize(name, target);
        }

        if (value.length() > 0) {
            target.writeByte(VALUE_FIELD_NUMBER << 3 | LENGTH_DELIMITED_TYPE);
            StringSerializer.serialize(value, target);
        }

        writeSize(target, messageStartIndex);
    }
}
