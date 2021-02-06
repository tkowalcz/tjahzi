package pl.tkowalcz.tjahzi;

import io.netty.buffer.ByteBuf;
import pl.tkowalcz.tjahzi.protobuf.PushRequestSerializer;
import pl.tkowalcz.tjahzi.protobuf.StreamSerializer;

public class OutputBuffer {

    private final ByteBuf target;
    private int requestStartIndex;

    public OutputBuffer(ByteBuf target) {
        this.target = target;
//        clear();
    }

    public void clear() {
        target.clear();
//        requestStartIndex = PushRequestSerializer.open(target);
    }

    public void addLogLine(CharSequence labels, long timestamp, ByteBuf logLine) {
        PushRequestSerializer.open(target);
        StreamSerializer.serialize(
                timestamp,
                logLine,
                labels,
                target
        );
    }

    public ByteBuf close() {
        PushRequestSerializer.close(target, requestStartIndex);
        return target;
    }

    public int getBytesPending() {
        return target.readableBytes();
    }
}
