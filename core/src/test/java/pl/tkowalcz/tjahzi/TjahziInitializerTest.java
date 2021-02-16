package pl.tkowalcz.tjahzi;

import org.agrona.concurrent.ringbuffer.RingBufferDescriptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.tkowalcz.tjahzi.TjahziInitializer.MIN_BUFFER_SIZE_BYTES;

class TjahziInitializerTest {

    public static IntStream fromZeroToRingBufferTrailerLength() {
        return IntStream.range(0, RingBufferDescriptor.TRAILER_LENGTH + 1);
    }

    public static IntStream powersOfTwo() {
        return IntStream.range(20, 30) // [1024*1024, 1073741824]
                .mapToDouble(exponent -> Math.pow(2, exponent))
                .mapToInt(value -> (int) value);
    }

    // Given
    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE, 10, 80, 150, 246, 1023})
    void shouldUseMinBufferSizeForInputsSmallerThan1MB(int input) {
        // When
        int actual = TjahziInitializer.findNearestPowerOfTwo(input);

        // Then
        assertThat(actual).isEqualTo(MIN_BUFFER_SIZE_BYTES);
    }

    @ParameterizedTest
    @MethodSource("fromZeroToRingBufferTrailerLength")
    void shouldScaleDownToNotExceedInteger_MAX_VALUE(int modifier) {
        // Given
        int requestedBufferSize = Integer.MAX_VALUE - modifier;
        int expected = (int) Math.pow(2, 30);

        // When
        int actual = TjahziInitializer.findNearestPowerOfTwo(requestedBufferSize);

        // Then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldReturnSameValueIfPowerOfTwo() {
        // Given
        for (long bufferSize = MIN_BUFFER_SIZE_BYTES; bufferSize < Integer.MAX_VALUE; bufferSize <<= 1) {
            // When
            int actual = TjahziInitializer.findNearestPowerOfTwo((int) bufferSize);

            // Then
            assertThat(actual)
                    .isEqualTo(bufferSize);
        }
    }

    @ParameterizedTest
    @MethodSource("powersOfTwo")
    void shouldRoundToNearestPowerOfTwo(int power) {
        // Given
        int bufferSizeBelowPower = power - 1;
        int bufferSize = power;
        int bufferSizeAbovePower = power + 1;

        if (bufferSizeBelowPower <= MIN_BUFFER_SIZE_BYTES) {
            // Skip this part of check as it would pass invalid value to test method
            bufferSizeBelowPower = power;
        }

        // When
        int actualBufferSizeBelowPower = TjahziInitializer.findNearestPowerOfTwo(bufferSizeBelowPower);
        int actualBufferSize = TjahziInitializer.findNearestPowerOfTwo(bufferSize);
        int actualBufferSizeAbovePower = TjahziInitializer.findNearestPowerOfTwo(bufferSizeAbovePower);

        // Then
        assertThat(actualBufferSizeBelowPower)
                .isEqualTo(power);

        assertThat(actualBufferSize)
                .isEqualTo(power);

        assertThat(actualBufferSizeAbovePower)
                .isEqualTo(power << 1);
    }
}
