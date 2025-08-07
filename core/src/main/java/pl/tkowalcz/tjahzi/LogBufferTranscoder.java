package pl.tkowalcz.tjahzi;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.AtomicBuffer;
import pl.tkowalcz.tjahzi.utils.TextBuilder;
import pl.tkowalcz.tjahzi.utils.TextBuilders;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;

public class LogBufferTranscoder {

    private final Map<String, String> staticLabels;
    private final String staticLabelsString;

    private final ByteBuf logLineHolder;
    private final StructuredMetadataPointer structuredMetadataPointer = new StructuredMetadataPointer();

    public LogBufferTranscoder(Map<String, String> staticLabels,
                               AtomicBuffer buffer) {
        this.staticLabels = staticLabels;
        this.staticLabelsString = buildLabelsStringIncludingStatic(
                staticLabels,
                "",
                TextBuilders.threadLocal().append("{ ")
        ).toString();

        ByteBuffer byteBuffer = buffer.byteBuffer();
        logLineHolder = Unpooled.wrappedBuffer(byteBuffer);
    }

    public void deserializeIntoByteBuf(DirectBuffer buffer, int index, OutputBuffer outputBuffer) {
        long epochMillisecond = buffer.getLong(index, ByteOrder.LITTLE_ENDIAN);
        index += Long.BYTES;

        long nanoOfMillisecond = buffer.getLong(index, ByteOrder.LITTLE_ENDIAN);
        index += Long.BYTES;

        TextBuilder labelsBuilder = TextBuilders.threadLocal();
        labelsBuilder.append("{ ");

        index = readLabels(
                buffer,
                index,
                labelsBuilder
        );

        CharSequence actualLabels = buildLabelsStringIncludingStatic(
                staticLabels,
                staticLabelsString,
                labelsBuilder
        );

        int size = buffer.getInt(index, ByteOrder.LITTLE_ENDIAN);
        index += Integer.BYTES;

        int structuredMetadataPosition = index;
        index += size + Integer.BYTES;

        structuredMetadataPointer.wrap(
                buffer,
                structuredMetadataPosition,
                size
        );

        logLineHolder.readerIndex(index);
        outputBuffer.addLogLine(
                actualLabels,
                epochMillisecond,
                nanoOfMillisecond,
                structuredMetadataPointer,
                logLineHolder
        );
    }

    private static int readLabels(
            DirectBuffer buffer,
            int index,
            TextBuilder labelsBuilder
    ) {
        int labelsSizeIgnored = buffer.getInt(index, ByteOrder.LITTLE_ENDIAN);
        index += Integer.BYTES;

        int labelsCount = buffer.getInt(index, ByteOrder.LITTLE_ENDIAN);
        index += Integer.BYTES;

        for (int i = 0; i < labelsCount; i++) {
            index += buffer.getStringAscii(index, labelsBuilder, ByteOrder.LITTLE_ENDIAN) + Integer.BYTES;
            labelsBuilder.append("=").append("\"");
            index += buffer.getStringAscii(index, labelsBuilder, ByteOrder.LITTLE_ENDIAN) + Integer.BYTES;
            labelsBuilder.append("\",");
        }

        return index;
    }

    private static CharSequence buildLabelsStringIncludingStatic(
            Map<String, String> staticLabels,
            String staticLabelsString,
            TextBuilder labels
    ) {
        if (labels.length() == 0) {
            return staticLabelsString;
        }

        staticLabels.forEach(
                (key, value) -> labels.append(key)
                        .append("=")
                        .append("\"")
                        .append(value)
                        .append("\",")
        );

        labels.setCharAt(labels.length() - 1, '}');
        return labels;
    }
}

