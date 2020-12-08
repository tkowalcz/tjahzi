package pl.tkowalcz.tjahzi.http;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class BlockingRetryTest {

    @Test
    void shouldRetryAndInvokeOperation() {
        // Given
        AtomicInteger retryCounter = new AtomicInteger();

        BlockingRetry retry = new BlockingRetry(
                __ -> {
                    retryCounter.incrementAndGet();
                    __.retry();
                },
                new ExponentialBackoffStrategy(1, 1, 1),
                5
        );

        // When
        retry.retry();

        // Then
        assertThat(retryCounter).hasValue(5);
    }

    @Test
    void shouldNotRetryOnItsOwn() {
        // Given
        AtomicInteger retryCounter = new AtomicInteger();

        BlockingRetry retry = new BlockingRetry(
                __ -> {
                    retryCounter.incrementAndGet();
                },
                new ExponentialBackoffStrategy(1, 1, 1),
                5
        );

        // When
        retry.retry();

        // Then
        assertThat(retryCounter).hasValue(1);
    }
}
