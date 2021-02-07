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

    public void addLogLine(CharSequence labels, long timestamp, ByteBuf logLine) {
        PushRequestSerializer.serialize(target);
        StreamSerializer.serialize(
                timestamp,
                logLine,
                labels,
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
