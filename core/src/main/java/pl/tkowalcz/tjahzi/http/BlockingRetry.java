package pl.tkowalcz.tjahzi.http;

import java.util.function.Consumer;

class BlockingRetry implements Retry {

    private final Consumer<BlockingRetry> operation;
    private final Runnable failureAction;

    private final ExponentialBackoffStrategy strategy;
    private int retries;

    // VisibleForTesting
    BlockingRetry(
            Consumer<BlockingRetry> operation,
            Runnable failureAction,
            ExponentialBackoffStrategy strategy,
            int retries
    ) {
        this.operation = operation;
        this.failureAction = failureAction;
        this.strategy = strategy;

        this.retries = retries;
    }

    BlockingRetry(
            Consumer<BlockingRetry> operation,
            Runnable failureAction,
            int retries
    ) {
        this(
                operation,
                failureAction,
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
        } else {
            failureAction.run();
        }
    }
}
