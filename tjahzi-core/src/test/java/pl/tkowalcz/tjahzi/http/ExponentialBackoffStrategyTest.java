package pl.tkowalcz.tjahzi.http;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class ExponentialBackoffStrategyTest {

    @Test
    void shouldReturnInitialBackoffOnFirstTry() {
        // Given
        int expected = 1000;

        ExponentialBackoffStrategy backoff = new ExponentialBackoffStrategy(
                expected,
                10_000,
                2
        );

        // When
        long actual = backoff.getAsLong();

        // Then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldNotGoOverMaximumBackoff() {
        // Given
        int expected = 10_000;

        ExponentialBackoffStrategy backoff = new ExponentialBackoffStrategy(
                4000,
                expected,
                2
        );

        // When
        for (int i = 0; i < 1000; i++) {
            backoff.getAsLong();
        }

        long actual = backoff.getAsLong();

        // Then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldIncreaseBackoffExponentially() {
        // Given
        ExponentialBackoffStrategy backoff = new ExponentialBackoffStrategy(
                250,
                10_000,
                3
        );

        ArrayList<Long> actual = new ArrayList<>();

        // When
        actual.add(backoff.getAsLong());
        actual.add(backoff.getAsLong());
        actual.add(backoff.getAsLong());
        actual.add(backoff.getAsLong());
        actual.add(backoff.getAsLong());

        // Then
        assertThat(actual).containsExactly(
                250L, 750L, 2250L, 6750L, 10_000L
        );
    }
}
