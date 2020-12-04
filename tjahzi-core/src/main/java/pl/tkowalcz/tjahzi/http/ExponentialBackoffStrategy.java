package pl.tkowalcz.tjahzi.http;

import java.util.function.LongSupplier;

public class ExponentialBackoffStrategy implements LongSupplier {

    private final long maximumBackoffMillis;
    private final double multiplier;

    private long nextBackoff;

    public ExponentialBackoffStrategy(
            long initialBackoffMillis,
            long maximumBackoffMillis,
            double multiplier
    ) {
        this.maximumBackoffMillis = maximumBackoffMillis;
        this.multiplier = multiplier;

        this.nextBackoff = initialBackoffMillis;
    }

    @Override
    public long getAsLong() {
        long result = nextBackoff;

        nextBackoff = (long) Math.min(nextBackoff * multiplier, maximumBackoffMillis);
        return result;
    }

    public static ExponentialBackoffStrategy withDefault() {
        return new ExponentialBackoffStrategy(
                250,
                30_000,
                3
        );
    }
}
