package ch.qos.logback.core.pattern;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static java.nio.ByteBuffer.wrap;
import static java.util.Arrays.copyOf;
import static org.assertj.core.api.Assertions.assertThat;

class EncoderTest {

    @Test
    void shouldHandleEmptyInput() {
        // Given
        Encoder encoder = Encoders.threadLocal();
        String expected = "ABC";

        // When
        encoder.encode(new StringBuilder(expected));

        // Then
        ByteBuffer actual = encoder.getBuffer();
        ByteBuffer actual1 = wrap(expected.getBytes(StandardCharsets.UTF_8));

        assertThat(actual).isEqualTo(actual1);
    }

    @Test
    void shouldEncode() {
        // Given
        Encoder encoder = Encoders.threadLocal();
        String expected = "ABC";

        // When
        encoder.encode(new StringBuilder(expected));

        // Then
        ByteBuffer actual = encoder.getBuffer();
        ByteBuffer actual1 = wrap(expected.getBytes(StandardCharsets.UTF_8));

        assertThat(actual).isEqualTo(actual1);
    }

    @Test
    void shouldClearInternalBuffer() {
        // Given
        Encoder encoder = new Encoder();

        StringBuilder firstText = new StringBuilder("first");
        StringBuilder secondText = new StringBuilder("second");

        encoder.encode(firstText);
        assertThat(encoder.getBuffer()).isEqualTo(wrap(firstText.toString().getBytes()));

        // When
        encoder.clear();

        // Then
        assertThat(encoder.getBuffer().position()).isZero();

        encoder.encode(secondText);
        assertThat(encoder.getBuffer()).isEqualTo(wrap(secondText.toString().getBytes()));
    }

    @Test
    void shouldEncodeTextLargerThanTempArray() {
        // Given
        Encoder encoder = new Encoder();

        String toEncode = RandomStringUtils.randomAlphanumeric(4 * 1024 + 41);
        ByteBuffer expected = wrap(toEncode.getBytes());

        // When
        encoder.encode(new StringBuilder(toEncode));

        // Then
        assertThat(encoder.getBuffer()).isEqualTo(expected);
    }

    @Test
    void shouldTruncateLongerMessages() {
        // Given
        Encoder encoder = new Encoder();

        String toEncode = RandomStringUtils.randomAlphanumeric(40 * 1024 + 41);
        ByteBuffer expected = wrap(
                copyOf(
                        toEncode.getBytes(),
                        10 * 1024
                )
        );

        // When
        encoder.encode(new StringBuilder(toEncode));

        // Then
        assertThat(encoder.getBuffer()).isEqualTo(expected);
    }
}
