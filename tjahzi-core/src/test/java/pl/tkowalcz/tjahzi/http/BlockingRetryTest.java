package pl.tkowalcz.tjahzi.http;

import org.junit.jupiter.api.Test;
import pl.tkowalcz.tjahzi.stats.StandardMonitoringModule;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class BlockingRetryTest {

    @Test
    void shouldRetryAndInvokeOperation() {
        // Given
        AtomicInteger retryCounter = new AtomicInteger();
        StandardMonitoringModule monitoringModule = new StandardMonitoringModule();

        BlockingRetry retry = new BlockingRetry(
                __ -> {
                    retryCounter.incrementAndGet();
                    __.retry();
                },
                new ExponentialBackoffStrategy(1, 1, 1),
                5,
                monitoringModule
        );

        // When
        retry.retry();

        // Then
        assertThat(retryCounter).hasValue(5);
        assertThat(monitoringModule.getRetriedHttpRequests()).isEqualTo(5);
        assertThat(monitoringModule.getFailedHttpRequests()).isEqualTo(1);
    }

    @Test
    void shouldNotRetryOnItsOwn() {
        // Given
        AtomicInteger retryCounter = new AtomicInteger();
        StandardMonitoringModule monitoringModule = new StandardMonitoringModule();

        BlockingRetry retry = new BlockingRetry(
                __ -> {
                    retryCounter.incrementAndGet();
                },
                new ExponentialBackoffStrategy(1, 1, 1),
                5,
                monitoringModule
        );

        // When
        retry.retry();

        // Then
        assertThat(retryCounter).hasValue(1);
        assertThat(monitoringModule.getRetriedHttpRequests()).isEqualTo(1);
        assertThat(monitoringModule.getFailedHttpRequests()).isEqualTo(0);
    }
}
