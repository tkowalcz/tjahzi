package pl.tkowalcz.tjahzi;

import com.google.protobuf.Timestamp;
import io.netty.util.internal.StringUtil;
import javolution.text.TextBuilder;
import logproto.Logproto;
import org.agrona.DirectBuffer;
import pl.tkowalcz.tjahzi.http.TextBuilders;

import java.util.LinkedHashMap;
import java.util.Map;

public class LogBufferDeserializer {

    private final Map<String, String> staticLabels;
    private final String staticLabelsString;

    public LogBufferDeserializer(Map<String, String> staticLabels) {
        this.staticLabels = staticLabels;
        this.staticLabelsString = buildLabelsStringIncludingStatic(
                Map.of(),
                StringUtil.EMPTY_STRING,
                staticLabels
        );
    }

    public Logproto.StreamAdapter deserializeIntoProtobuf(DirectBuffer buffer, int index) {
        long timestamp = buffer.getLong(index);
        index += Long.BYTES;

        Map<String, String> labels = new LinkedHashMap<>();
        index = readLabels(
                buffer,
                index,
                labels
        );

        String line = buffer.getStringAscii(index);

        return createGrpcStream(
                timestamp,
                labels,
                line
        );
    }

    private int readLabels(
            DirectBuffer buffer,
            int index,
            Map<String, String> labels
    ) {
        int labelsCount = buffer.getInt(index);
        index += Integer.BYTES;

        for (int i = 0; i < labelsCount; i++) {
            String key = buffer.getStringAscii(index);
            index += key.length() + Integer.BYTES;

            String value = buffer.getStringAscii(index);
            index += value.length() + Integer.BYTES;

            labels.put(key, value);
        }

        return index;
    }

    private Logproto.StreamAdapter createGrpcStream(
            long timestamp,
            Map<String, String> labels,
            String line
    ) {
        long timestampSeconds = timestamp / 1000;
        int timestampNanos = (int) (timestamp % 1000) * 1000_000;

        return Logproto.StreamAdapter.newBuilder()
                .setLabels(
                        buildLabelsStringIncludingStatic(
                                staticLabels,
                                staticLabelsString,
                                labels
                        )
                )
                .addEntries(
                        Logproto.EntryAdapter.newBuilder()
                                .setTimestamp(
                                        Timestamp.newBuilder()
                                                .setSeconds(timestampSeconds)
                                                .setNanos(timestampNanos)
                                )
                                .setLine(line)
                )
                .build();
    }

    private static String buildLabelsStringIncludingStatic(
            Map<String, String> staticLabels,
            String staticLabelsString,
            Map<String, String> labels
    ) {
        if (labels.isEmpty()) {
            return staticLabelsString;
        }

        TextBuilder textBuilder = TextBuilders.threadLocal();

        textBuilder.append("{ ");
        staticLabels.forEach((key, value) -> {
            if (!labels.containsKey(key)) {
                textBuilder.append(key)
                        .append("=")
                        .append("\"")
                        .append(value)
                        .append("\",");
            }
        });

        labels.forEach((key, value) -> textBuilder.append(key)
                .append("=")
                .append("\"")
                .append(value)
                .append("\","));


        textBuilder.setCharAt(textBuilder.length() - 1, '}');
        return textBuilder.toString();
    }
}
