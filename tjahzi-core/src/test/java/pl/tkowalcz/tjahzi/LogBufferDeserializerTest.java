package pl.tkowalcz.tjahzi;

import com.google.common.collect.Streams;
import com.google.protobuf.Timestamp;
import logproto.Logproto;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.tkowalcz.tjahzi.LogBufferSerializerDeserializerTest.toPropertyStream;

class LogBufferDeserializerTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("pl.tkowalcz.tjahzi.LogBufferSerializerDeserializerTest#variousMessageConfigurations")
    void shouldDeserializeMessageAndAddStaticLabels(
            String testName,
            Map<String, String> labels,
            String logLevelLabel,
            String logLevel,
            String logLine,
            int expectedSize) {
        // Given

        UnsafeBuffer buffer = new UnsafeBuffer(new byte[expectedSize]);
        LogBufferSerializer serializer = new LogBufferSerializer(buffer);
        serializer.writeTo(
                0,
                32042L,
                labels,
                logLevelLabel,
                logLevel,
                ByteBuffer.wrap(logLine.getBytes())
        );

        Map<String, String> staticLabels = Map.of(
                "foo", "bar",
                "bazz", "buzz"
        );

        // When
        LogBufferDeserializer deserializer = new LogBufferDeserializer();
        Logproto.StreamAdapter stream = deserializer.deserialize(
                buffer,
                0,
                staticLabels
        );

        // Then
        assertThat(stream.getEntriesList()).hasSize(1);
        assertThat(stream.getEntriesList().get(0).getTimestamp()).isEqualTo(
                Timestamp.newBuilder()
                        .setSeconds(32)
                        .setNanos(42_000_000)
                        .build()
        );
        assertThat(stream.getEntriesList().get(0).getLine()).isEqualTo(logLine);

        Stream<String> logLevelStream = logLevelLabel == null ? Stream.of() : toPropertyStream(Map.of(logLevelLabel, logLevel));
        Stream<String> incomingLabelsStream = toPropertyStream(labels);
        Stream<String> staticLabelsStream = toPropertyStream(staticLabels);

        assertThat(stream.getLabels()).isEqualToIgnoringWhitespace(
                Streams.concat(
                        incomingLabelsStream,
                        logLevelStream,
                        staticLabelsStream
                )
                        .collect(joining(",", "{", "}"))
        );
    }
}
