package pl.tkowalcz.tjahzi.http;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class BlockingRetryTest {

    @Test
    void shouldInvokeOperation() {
        // Given
        AtomicInteger invocationCounter = new AtomicInteger();
        AtomicInteger failureCounter = new AtomicInteger();

        BlockingRetry retry = new BlockingRetry(
                __ -> invocationCounter.incrementAndGet(),
                failureCounter::incrementAndGet,
                new ExponentialBackoffStrategy(1, 1, 1),
                5
        );

        // When
        retry.retry();

        // Then
        assertThat(invocationCounter).hasValue(1);
        assertThat(failureCounter).hasValue(0);
    }

    @Test
    void shouldRetryAndInvokeFailureCallback() {
        // Given
        AtomicInteger invocationCounter = new AtomicInteger();
        AtomicInteger failureCounter = new AtomicInteger();

        BlockingRetry retry = new BlockingRetry(
                __ -> {
                    invocationCounter.incrementAndGet();
                    __.retry();
                },
                failureCounter::incrementAndGet,
                new ExponentialBackoffStrategy(1, 1, 1),
                5
        );

        // When
        retry.retry();

        // Then
        assertThat(invocationCounter).hasValue(5);
        assertThat(failureCounter).hasValue(1);
    }

    @Test
    void shouldNotRetryOnItsOwn() {
        // Given
        AtomicInteger retryCounter = new AtomicInteger();
        AtomicInteger failureCounter = new AtomicInteger();

        BlockingRetry retry = new BlockingRetry(
                __ -> retryCounter.incrementAndGet(),
                () -> {
                },
                new ExponentialBackoffStrategy(1, 1, 1),
                5
        );

        // When
        retry.retry();

        // Then
        assertThat(retryCounter).hasValue(1);
        assertThat(failureCounter).hasValue(0);
    }
}
