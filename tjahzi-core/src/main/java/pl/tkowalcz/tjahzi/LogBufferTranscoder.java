package pl.tkowalcz.tjahzi;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.internal.StringUtil;
import javolution.text.TextBuilder;
import org.agrona.DirectBuffer;
import pl.tkowalcz.tjahzi.http.TextBuilders;

import java.nio.ByteBuffer;
import java.util.Map;

public class LogBufferTranscoder {

    private final Map<String, String> staticLabels;
    private final String staticLabelsString;

    public LogBufferTranscoder(Map<String, String> staticLabels) {
        this.staticLabels = staticLabels;
        this.staticLabelsString = buildLabelsStringIncludingStatic(
                staticLabels,
                StringUtil.EMPTY_STRING,
                TextBuilders.threadLocal().append("{ ")
        ).toString();
    }

    public void deserializeIntoByteBuf(DirectBuffer buffer, int index, OutputBuffer outputBuffer) {
        long timestamp = buffer.getLong(index);
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

        ByteBuffer byteBuffer = buffer.byteBuffer();
        ByteBuf logLine = Unpooled.wrappedBuffer(byteBuffer)
                .readerIndex(index);

        outputBuffer.addLogLine(actualLabels, timestamp, logLine);
    }

    private int readLabels(
            DirectBuffer buffer,
            int index,
            TextBuilder labelsBuilder
    ) {
        int labelsCount = buffer.getInt(index);
        index += Integer.BYTES;

        for (int i = 0; i < labelsCount; i++) {
            index += buffer.getStringAscii(index, labelsBuilder) + Integer.BYTES;
            labelsBuilder.append("=").append("\"");
            index += buffer.getStringAscii(index, labelsBuilder) + Integer.BYTES;
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

        staticLabels.forEach((key, value) -> {
//            if (!labels.containsKey(key)) {
            labels.append(key)
                    .append("=")
                    .append("\"")
                    .append(value)
                    .append("\",");
//            }
        });

        labels.setCharAt(labels.length() - 1, '}');
        return labels;
    }
}
