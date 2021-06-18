package pl.tkowalcz.tjahzi;

import com.google.common.collect.Streams;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Timestamp;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.PooledByteBufAllocator;
import logproto.Logproto;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;

class LogBufferSerializerDeserializerTest {

    static Stream<Arguments> variousMessageConfigurations() {
        return Stream.of(
                Arguments.of(
                        "Message with all fields filled",
                        Map.of("ip", "10.0.0.42",
                                "hostname", "razor-crest",
                                "region", "outer-rim"
                        ),
                        "log_level",
                        "WARN",
                        "[Mando] You have something I want.",
                        38 // log line
                                + 21 // log level
                                + 45 // labels string sizes
                                + 6 * 4 // labels string length indicators
                                + 8 // timestamp
                                + 4 // labels count
                ),
                Arguments.of(
                        "Message with empty labels",
                        Map.of(),
                        "log_level",
                        "WARN",
                        "[Mando] You have something I want.",
                        38 // log line
                                + 21 // log level
                                + 8 // timestamp
                                + 4 // labels count
                ),
                Arguments.of(
                        "Message with null log level",
                        Map.of("ip", "10.0.0.42",
                                "hostname", "razor-crest",
                                "region", "outer-rim"
                        ),
                        null,
                        null,
                        "[Mando] You have something I want.",
                        38 // log line
                                + 45 // labels string sizes
                                + 6 * 4 // labels string length indicators
                                + 8 // timestamp
                                + 4 // labels count
                )
        );
    }

    @SuppressWarnings("unused")
    @ParameterizedTest(name = "{0}")
    @MethodSource("variousMessageConfigurations")
    void shouldSerializeMessage(
            String testName,
            Map<String, String> labels,
            String logLevelLabel,
            String logLevel,
            String logLine,
            int expectedSize) throws InvalidProtocolBufferException {

        // Given
        UnsafeBuffer buffer = new UnsafeBuffer(ByteBuffer.wrap(new byte[expectedSize]));
        LogBufferSerializer serializer = new LogBufferSerializer(buffer);

        LabelSerializer labelSerializer = LabelSerializers.from(labels);
        if (logLevelLabel != null) {
            labelSerializer.appendLabel(logLevelLabel, logLevel);
        }

        // When
        serializer.writeTo(
                0,
                32042L,
                labelSerializer,
                ByteBuffer.wrap(logLine.getBytes())
        );

        // Then
        OutputBuffer outputBuffer = new OutputBuffer(PooledByteBufAllocator.DEFAULT.buffer());

        LogBufferTranscoder deserializer = new LogBufferTranscoder(Map.of(), buffer);
        deserializer.deserializeIntoByteBuf(
                buffer,
                0,
                outputBuffer
        );

        ByteBuf target = outputBuffer.close();
        Logproto.PushRequest pushRequest = Logproto.PushRequest
                .parser()
                .parsePartialFrom(new ByteBufInputStream(target));

        assertThat(pushRequest).isNotNull();
        assertThat(pushRequest.getStreamsCount()).isEqualTo(1);
        Logproto.StreamAdapter stream = pushRequest.getStreams(0);

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

        assertThat(stream.getLabels()).isEqualToIgnoringWhitespace(
                Streams.concat(
                        incomingLabelsStream,
                        logLevelStream
                )
                        .collect(joining(",", "{", "}"))
        );
    }

    public static Stream<String> toPropertyStream(Map<String, String> labels) {
        return labels.entrySet()
                .stream()
                .map(entry -> entry.getKey() + "=\"" + entry.getValue() + "\"");
    }

    @SafeVarargs
    public static Stream<String> toPropertyStream(Map.Entry<String, String>... labels) {
        return Arrays.stream(labels)
                .map(entry -> entry.getKey() + "=\"" + entry.getValue() + "\"");
    }
}
