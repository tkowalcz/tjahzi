package pl.tkowalcz.tjahzi.protobuf;

import io.netty.buffer.ByteBuf;

import static pl.tkowalcz.tjahzi.protobuf.Protobuf.LENGTH_DELIMITED_TYPE;

public class PushRequestSerializer {

    public static final int STREAM_FIELD_NUMBER = 1;

    public static void serialize(ByteBuf target) {
        target.writeByte(STREAM_FIELD_NUMBER << 3 | LENGTH_DELIMITED_TYPE);
    }
}
