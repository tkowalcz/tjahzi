package pl.tkowalcz.tjahzi.protobuf;

import io.netty.buffer.ByteBuf;
import pl.tkowalcz.tjahzi.StructuredMetadataPointer;

import static pl.tkowalcz.tjahzi.protobuf.Protobuf.LENGTH_DELIMITED_TYPE;
import static pl.tkowalcz.tjahzi.protobuf.Protobuf.writeSize;

public class EntrySerializer {

    public static final int TIMESTAMP_FIELD_NUMBER = 1;
    public static final int LOG_LINE_FIELD_NUMBER = 2;
    public static final int STRUCTURED_METADATA_FIELD_NUMBER = 3;

    public static void serialize(
            long epochMillisecond,
            long nanoOfMillisecond,
            ByteBuf logLine,
            StructuredMetadataPointer structuredMetadata,
            ByteBuf target) {
        int messageStartIndex = target.writerIndex();
        target.writeIntLE(0);

        target.writeByte(TIMESTAMP_FIELD_NUMBER << 3 | LENGTH_DELIMITED_TYPE);
        TimestampSerializer.serialize(epochMillisecond, nanoOfMillisecond, target);

        target.writeByte(LOG_LINE_FIELD_NUMBER << 3 | LENGTH_DELIMITED_TYPE);
        StringSerializer.serialize(logLine, target);

        if (structuredMetadata.hasBytes()) {
            readStructuredMetadata(structuredMetadata, target);
        }

        writeSize(target, messageStartIndex);
    }

    private static void readStructuredMetadata(StructuredMetadataPointer pointer, ByteBuf target) {
        int labelsCount = pointer.readInt();

        for (int i = 0; i < labelsCount; i++) {
            target.writeByte(STRUCTURED_METADATA_FIELD_NUMBER << 3 | LENGTH_DELIMITED_TYPE);
            String name = pointer.getStringAscii();
            String value = pointer.getStringAscii();

            LabelSerializer.serialize(name, value, target);
        }
    }
}
