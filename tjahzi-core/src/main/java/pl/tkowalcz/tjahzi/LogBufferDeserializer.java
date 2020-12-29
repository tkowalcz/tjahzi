package pl.tkowalcz.tjahzi;

import com.google.protobuf.Timestamp;
import javolution.text.TextBuilder;
import logproto.Logproto;
import org.agrona.DirectBuffer;
import pl.tkowalcz.tjahzi.http.TextBuilders;

import java.util.LinkedHashMap;
import java.util.Map;

public class LogBufferDeserializer {

    public Logproto.StreamAdapter deserialize(
            DirectBuffer buffer,
            int index,
            Map<String, String> staticLabels
    ) {
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
                staticLabels,
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
            Map<String, String> staticLabels,
            long timestamp,
            Map<String, String> labels,
            String line
    ) {
        long timestampSeconds = timestamp / 1000;
        int timestampNanos = (int) (timestamp % 1000) * 1000_000;

        return Logproto.StreamAdapter.newBuilder()
                .setLabels(
                        buildLabelsStringIncludingStatic(labels, staticLabels)
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
            Map<String, String> labels) {
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
