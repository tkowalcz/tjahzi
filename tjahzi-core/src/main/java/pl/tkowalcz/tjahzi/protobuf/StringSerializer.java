package pl.tkowalcz.tjahzi.protobuf;

import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;

import static pl.tkowalcz.tjahzi.protobuf.Protobuf.writeUnsignedVarint;

public class StringSerializer {

    public static void serialize(
            String logLine,
            ByteBuf target
    ) {
        writeUnsignedVarint(logLine.length(), target);
        target.writeCharSequence(logLine, StandardCharsets.US_ASCII);
    }
}
