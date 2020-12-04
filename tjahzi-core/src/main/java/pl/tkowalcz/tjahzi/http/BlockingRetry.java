package pl.tkowalcz.tjahzi.http;

import java.util.function.Consumer;

class BlockingRetry implements Retry {

    private final ExponentialBackoffStrategy strategy = ExponentialBackoffStrategy.withDefault();
    private final Consumer<BlockingRetry> operation;

    private int retries;

    BlockingRetry(Consumer<BlockingRetry> operation, int retries) {
        this.operation = operation;
        this.retries = retries;
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
