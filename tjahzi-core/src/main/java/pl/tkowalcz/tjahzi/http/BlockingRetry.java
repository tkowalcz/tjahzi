package pl.tkowalcz.tjahzi.http;

import pl.tkowalcz.tjahzi.stats.MonitoringModule;

import java.util.function.Consumer;

class BlockingRetry implements Retry {

    private final ExponentialBackoffStrategy strategy;
    private final Consumer<BlockingRetry> operation;
    private final MonitoringModule monitoringModule;

    private int retries;

    // VisibleForTesting
    BlockingRetry(
            Consumer<BlockingRetry> operation,
            ExponentialBackoffStrategy strategy,
            int retries,
            MonitoringModule monitoringModule
    ) {
        this.operation = operation;
        this.strategy = strategy;

        this.retries = retries;
        this.monitoringModule = monitoringModule;
    }

    BlockingRetry(
            Consumer<BlockingRetry> operation,
            int retries,
            MonitoringModule monitoringModule) {
        this(
                operation,
                ExponentialBackoffStrategy.withDefault(),
                retries,
                monitoringModule
        );
    }

    @Override
    public void retry() {
        if (retries > 0) {
            retries--;
            monitoringModule.incrementRetriedHttpRequests();

            try {
                Thread.sleep(strategy.getAsLong(), 0);
            } catch (InterruptedException ignore) {
                return;
            }

            operation.accept(this);
        } else {
            monitoringModule.incrementFailedHttpRequests();
        }
    }
}
