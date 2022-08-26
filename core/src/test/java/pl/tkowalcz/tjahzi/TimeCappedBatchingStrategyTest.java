package pl.tkowalcz.tjahzi;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.PooledByteBufAllocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.tkowalcz.tjahzi.stats.SettableClock;

import static org.assertj.core.api.Assertions.assertThat;

class TimeCappedBatchingStrategyTest {

    private OutputBuffer outputBufferWthData;
    private OutputBuffer outputBufferWithNoData;
    private ByteBuf logLine;

    @BeforeEach
    void setUp() {
        outputBufferWithNoData = new OutputBuffer(PooledByteBufAllocator.DEFAULT.buffer());
        outputBufferWthData = new OutputBuffer(PooledByteBufAllocator.DEFAULT.buffer());

        logLine = PooledByteBufAllocator.DEFAULT.buffer();
        logLine.writeIntLE(4);
        ByteBufUtil.writeAscii(logLine, "aaba");

        outputBufferWthData.addLogLine(
                "foobar",
                44L,
                0,
                logLine.duplicate()
        );
    }

    @Test
    void shouldProceedWhenWaitTimeIsOver() {
        // Given
        int batchWaitMillis = 1000;

        SettableClock clock = new SettableClock();
        clock.setMillis(42L);

        TimeCappedBatchingStrategy strategy = new TimeCappedBatchingStrategy(
                clock,
                outputBufferWthData,
                10,
                batchWaitMillis,
                10_000
        );

        // When
        clock.setMillis(clock.millis() + batchWaitMillis + 1);

        // Then
        assertThat(strategy.shouldProceed()).isTrue();
    }

    @Test
    void shouldNotProceedWhenWaitTimeIsOverButThereIsNothingToSend() {
        // Given
        int batchWaitMillis = 1000;

        SettableClock clock = new SettableClock();
        clock.setMillis(42L);

        TimeCappedBatchingStrategy strategy = new TimeCappedBatchingStrategy(
                clock,
                outputBufferWithNoData,
                10,
                batchWaitMillis,
                10_000
        );

        // When
        clock.setMillis(clock.millis() + batchWaitMillis);

        // Then
        assertThat(strategy.shouldProceed()).isFalse();
    }

    @Test
    void shouldNotProceedWhenWaitTimeIsNotOver() {
        // Given
        int batchSize = 100;
        int batchWaitMillis = 1000;

        SettableClock clock = new SettableClock();
        clock.setMillis(42L);

        TimeCappedBatchingStrategy strategy = new TimeCappedBatchingStrategy(
                clock,
                outputBufferWthData,
                batchSize,
                batchWaitMillis,
                10_000
        );

        // When
        clock.setMillis(clock.millis() + batchWaitMillis - 10);

        // Then
        assertThat(strategy.shouldProceed()).isFalse();
    }

    @Test
    void shouldProceedWhenBatchSizeIsReached() {
        // Given
        int batchWaitMillis = 1000;
        int batchSize = 4;

        SettableClock clock = new SettableClock();
        clock.setMillis(42L);

        TimeCappedBatchingStrategy strategy = new TimeCappedBatchingStrategy(
                clock,
                outputBufferWithNoData,
                batchSize,
                batchWaitMillis,
                10_000
        );

        // When
        outputBufferWithNoData.addLogLine("foobar", 44L, 0, logLine);

        // Then
        assertThat(strategy.shouldProceed()).isTrue();
    }

    @Test
    void shouldProceedWhenBatchSizeReachedAndWaitTimeIsOver() {
        // Given
        int batchWaitMillis = 1000;
        int batchSize = 4;

        SettableClock clock = new SettableClock();
        clock.setMillis(42L);

        TimeCappedBatchingStrategy strategy = new TimeCappedBatchingStrategy(
                clock,
                outputBufferWithNoData,
                batchSize,
                batchWaitMillis,
                10_000
        );

        // When
        outputBufferWithNoData.addLogLine("foobar", 44L, 0, logLine);
        clock.setMillis(clock.millis() + batchWaitMillis);

        // Then
        assertThat(strategy.shouldProceed()).isTrue();
    }

    @Test
    void shouldAllowShutdownUntilDeadlinePasses() {
        // Given
        int batchSize = 4;
        int batchWaitMillis = 1000;
        int shutdownTimeout = 10_000;

        long initialTime = 42L;

        SettableClock clock = new SettableClock();
        clock.setMillis(initialTime);

        TimeCappedBatchingStrategy strategy = new TimeCappedBatchingStrategy(
                clock,
                outputBufferWthData,
                batchSize,
                batchWaitMillis,
                shutdownTimeout
        );

        // When & Then
        strategy.initShutdown();
        assertThat(strategy.shouldContinueShutdown()).isTrue();

        clock.setMillis(initialTime + 1000);
        assertThat(strategy.shouldContinueShutdown()).isTrue();

        clock.setMillis(initialTime + 9999);
        assertThat(strategy.shouldContinueShutdown()).isTrue();

        clock.setMillis(initialTime + 10_000);
        assertThat(strategy.shouldContinueShutdown()).isFalse();
    }
}
