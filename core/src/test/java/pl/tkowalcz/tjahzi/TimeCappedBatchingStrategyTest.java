package pl.tkowalcz.tjahzi;

import org.junit.jupiter.api.Test;
import pl.tkowalcz.tjahzi.stats.SettableClock;

class TimeCappedBatchingStrategyTest {

    @Test
    void should() {
        // Given
        SettableClock clock = new SettableClock();
        clock.setMillis(42L);

        TimeCappedBatchingStrategy strategy = new TimeCappedBatchingStrategy(
                clock,
                null,
                10,
                1000,
                10_000
        );

        // When
        strategy.shouldContinueShutdown();
    }
}
