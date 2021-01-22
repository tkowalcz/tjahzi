package pl.tkowalcz.tjahzi.stats;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

public class SettableClock extends Clock {

    public static final long TICK_AMOUNT = 11L;

    private long millis;

    public void setMillis(long millis) {
        this.millis = millis;
    }

    public void tick() {
        millis += TICK_AMOUNT;
    }

    public void tick(long amount) {
        millis += amount;
    }

    @Override
    public ZoneId getZone() {
        return ZoneId.systemDefault();
    }

    @Override
    public Clock withZone(ZoneId zone) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Instant instant() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long millis() {
        return millis;
    }
}
