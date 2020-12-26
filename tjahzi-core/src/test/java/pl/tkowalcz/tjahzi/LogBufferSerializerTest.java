package pl.tkowalcz.tjahzi;

import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class LogBufferSerializerTest {

    private static Stream<Arguments> variousMessageConfigurations() {
        return Stream.of(
                Arguments.of(
                        "Message with all fields filled",
                        Map.of("ip", "10.0.0.42",
                                "hostname", "razor-crest",
                                "region", "outer-rim"
                        ),
                        "log_level",
                        "WARN",
                        ByteBuffer.wrap("[Mando] You have something I want.".getBytes()),
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
                        ByteBuffer.wrap("[Mando] You have something I want.".getBytes()),
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
                        ByteBuffer.wrap("[Mando] You have something I want.".getBytes()),
                        38 // log line
                                + 45 // labels string sizes
                                + 6 * 4 // labels string length indicators
                                + 8 // timestamp
                                + 4 // labels count
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("variousMessageConfigurations")
    void shouldCalculateSizeOfMessage(
            String testName,
            Map<String, String> labels,
            String logLevelLabel,
            String logLevel,
            ByteBuffer logLine,
            int expectedSize) {
        // Given
        UnsafeBuffer buffer = new UnsafeBuffer(new byte[expectedSize]);
        LogBufferSerializer serializer = new LogBufferSerializer(buffer);

        // When
        int actual = serializer.calculateRequiredSizeAscii(
                labels,
                logLevelLabel,
                logLevel,
                logLine
        );

        // Then
        assertThat(actual).isEqualTo(expectedSize);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("variousMessageConfigurations")
    void shouldSerializeMessage(
            String testName,
            Map<String, String> labels,
            String logLevelLabel,
            String logLevel,
            ByteBuffer logLine,
            int expectedSize) {
        // Given
        UnsafeBuffer buffer = new UnsafeBuffer(new byte[expectedSize]);
        LogBufferSerializer serializer = new LogBufferSerializer(buffer);

        // When
        serializer.writeTo(
                0,
                42L,
                labels,
                logLevelLabel,
                logLevel,
                logLine
        );

        // Then
        // There was no buffer overflow so seems legit.
        // TODO: verify contents after extracting deserialization code to separate class.
    }
}
