package pl.tkowalcz.tjahzi.http;

import io.netty.channel.nio.NioEventLoopGroup;
import org.awaitility.Durations;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class EventLoopGroupRetryTest {

    private NioEventLoopGroup eventLoopGroup;

    @BeforeEach
    void setUp() {
        eventLoopGroup = new NioEventLoopGroup(Executors.defaultThreadFactory());
    }

    @AfterEach
    void tearDown() {
        eventLoopGroup.shutdownGracefully();
    }

    @Test
    void shouldRetryAndInvokeOperation() {
        // Given
        AtomicInteger retryCounter = new AtomicInteger();

        EventLoopGroupRetry retry = new EventLoopGroupRetry(
                eventLoopGroup,
                __ -> {
                    retryCounter.incrementAndGet();
                    __.retry();
                },
                new ExponentialBackoffStrategy(250, 1000, 2)
        );

        // When
        retry.retry();

        // Then
        await().atMost(Durations.TEN_SECONDS).untilAsserted(() -> {
            assertThat(retryCounter).hasValueGreaterThan(3);
        });
    }

    @Test
    void shouldNotRetryOnItsOwn() {
        // Given
        AtomicInteger retryCounter = new AtomicInteger();

        EventLoopGroupRetry retry = new EventLoopGroupRetry(
                eventLoopGroup,
                __ -> {
                    retryCounter.incrementAndGet();
                },
                new ExponentialBackoffStrategy(1, 1, 1)
        );

        // When
        retry.retry();

        // Then
        await().atMost(Durations.TEN_SECONDS).untilAsserted(() -> {
            assertThat(retryCounter).hasValue(1);
        });
    }
}
