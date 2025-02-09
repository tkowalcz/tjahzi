package pl.tkowalcz.tjahzi;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Timestamp;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.PooledByteBufAllocator;
import logproto.Logproto;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.tkowalcz.tjahzi.LogBufferSerializerDeserializerTest.toPropertyStream;

class LogBufferTranscoderTest {

    @SuppressWarnings("unused")
    @ParameterizedTest(name = "{0}")
    @MethodSource("pl.tkowalcz.tjahzi.LogBufferSerializerDeserializerTest#variousMessageConfigurations")
    void shouldDeserializeMessageAndAddStaticLabels(
            String testName,
            Map<String, String> labels,
            String logLevelLabel,
            String logLevel,
            String logLine,
            int expectedSize) throws InvalidProtocolBufferException {
        // Given
        UnsafeBuffer buffer = new UnsafeBuffer(
                ByteBuffer.wrap(new byte[expectedSize])
        );
        LogBufferSerializer serializer = new LogBufferSerializer(buffer);

        LabelSerializer labelSerializer = LabelSerializerCreator.from(labels);
        if (logLevelLabel != null) {
            labelSerializer.appendLabel(logLevelLabel, logLevel);
        }

        serializer.writeTo(
                0,
                32042L,
                882L,
                labelSerializer,
                ByteBuffer.wrap(logLine.getBytes())
        );

        Map<String, String> staticLabels = new LinkedHashMap<>();
        staticLabels.put("foo", "bar");
        staticLabels.put("bazz", "buzz");

        OutputBuffer outputBuffer = new OutputBuffer(PooledByteBufAllocator.DEFAULT.buffer());

        // When
        LogBufferTranscoder deserializer = new LogBufferTranscoder(staticLabels, buffer);
        deserializer.deserializeIntoByteBuf(
                buffer,
                0,
                outputBuffer
        );

        // Then
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
                        .setNanos(42_000_882)
                        .build()
        );
        assertThat(stream.getEntriesList().get(0).getLine()).isEqualTo(logLine);

        Stream<String> logLevelStream = logLevelLabel == null ? Stream.of() : toPropertyStream(Map.entry(logLevelLabel, logLevel));
        Stream<String> incomingLabelsStream = toPropertyStream(labels);
        Stream<String> staticLabelsStream = toPropertyStream(staticLabels);

        assertThat(stream.getLabels()).isEqualToIgnoringWhitespace(
                Stream.of(
                                incomingLabelsStream,
                                logLevelStream,
                                staticLabelsStream
                        )
                        .flatMap(Function.identity())
                        .collect(joining(",", "{", "}"))
        );
    }

    @Test
    @Disabled
    void shouldOverrideStaticLabelsWithIncoming() throws InvalidProtocolBufferException {
        // Given
        Map<String, String> staticLabels = new LinkedHashMap<>();
        staticLabels.put("ip", "10.0.0.42");
        staticLabels.put("hostname", "razor-crest");
        staticLabels.put("region", "outer-rim");

        Map<String, String> incomingLabels = new LinkedHashMap<>();
        incomingLabels.put("hostname", "-\\_(?)_/-");
        incomingLabels.put("region", "busted");

        UnsafeBuffer buffer = new UnsafeBuffer(ByteBuffer.wrap(new byte[256]));
        LogBufferSerializer serializer = new LogBufferSerializer(buffer);

        LabelSerializer labelSerializer = LabelSerializerCreator.from(incomingLabels);
        labelSerializer.appendLabelName("log_level");
        labelSerializer.appendLabelName("WARN");

        serializer.writeTo(
                0,
                32042L,
                0,
                labelSerializer,
                ByteBuffer.wrap("[Mando] You have something I want.".getBytes())
        );

        OutputBuffer outputBuffer = new OutputBuffer(PooledByteBufAllocator.DEFAULT.buffer());

        // When
        LogBufferTranscoder deserializer = new LogBufferTranscoder(staticLabels, buffer);
        deserializer.deserializeIntoByteBuf(
                buffer,
                0,
                outputBuffer
        );

        // Then
        ByteBuf target = outputBuffer.close();
        Logproto.PushRequest pushRequest = Logproto.PushRequest
                .parser()
                .parsePartialFrom(new ByteBufInputStream(target));

        assertThat(pushRequest).isNotNull();
        assertThat(pushRequest.getStreamsCount()).isEqualTo(1);

        Stream<String> incomingLabelsStream = toPropertyStream(
                Map.entry("ip", "10.0.0.42"),
                Map.entry("hostname", "-\\_(?)_/-"),
                Map.entry("region", "busted"),
                Map.entry("log_level", "WARN")
        );

        assertThat(pushRequest.getStreams(0).getLabels()).isEqualToIgnoringWhitespace(
                incomingLabelsStream
                        .collect(joining(",", "{", "}"))
        );
    }
}
