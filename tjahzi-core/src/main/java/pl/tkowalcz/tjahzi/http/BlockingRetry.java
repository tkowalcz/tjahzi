package pl.tkowalcz.tjahzi.http;

import java.util.function.Consumer;

class BlockingRetry implements Retry {

    private final ExponentialBackoffStrategy strategy;
    private final Consumer<BlockingRetry> operation;

    private int retries;

    // VisibleForTesting
    BlockingRetry(
            Consumer<BlockingRetry> operation,
            ExponentialBackoffStrategy strategy,
            int retries) {
        this.operation = operation;
        this.strategy = strategy;
        this.retries = retries;
    }

    BlockingRetry(Consumer<BlockingRetry> operation, int retries) {
        this(
                operation,
                ExponentialBackoffStrategy.withDefault(),
                retries
        );
    }

    @Override
    public void retry() {
        if (retries > 0) {
            retries--;

            try {
                Thread.sleep(strategy.getAsLong(), 0);
            } catch (InterruptedException ignore) {
                return;
            }

            operation.accept(this);
        }
    }
}
