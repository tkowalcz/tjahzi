package pl.tkowalcz.tjahzi;

import io.netty.buffer.ByteBuf;
import pl.tkowalcz.tjahzi.protobuf.PushRequestSerializer;
import pl.tkowalcz.tjahzi.protobuf.StreamSerializer;

public class OutputBuffer {

    private final ByteBuf target;

    public OutputBuffer(ByteBuf target) {
        this.target = target;
    }

    public void clear() {
        target.clear();
    }

    public void addLogLine(
            CharSequence labels,
            long epochMillisecond,
            long nanoOfMillisecond,
            StructuredMetadataPointer structuredMetadata,
            ByteBuf logLine
    ) {
        PushRequestSerializer.serialize(target);
        StreamSerializer.serialize(
                epochMillisecond,
                nanoOfMillisecond,
                logLine,
                labels,
                structuredMetadata,
                target
        );
    }

    public int getBytesPending() {
        return target.readableBytes();
    }

    public ByteBuf close() {
        return target;
    }
}
