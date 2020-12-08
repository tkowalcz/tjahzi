package pl.tkowalcz.tjahzi.http;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

class EventLoopGroupRetry implements Retry, Runnable {

    private final ScheduledExecutorService group;
    private final Consumer<EventLoopGroupRetry> operation;
    private final ExponentialBackoffStrategy strategy;

    // VisibleForTesting
    public EventLoopGroupRetry(
            ScheduledExecutorService group,
            Consumer<EventLoopGroupRetry> operation,
            ExponentialBackoffStrategy strategy) {
        this.group = group;
        this.operation = operation;
        this.strategy = strategy;
    }

    public EventLoopGroupRetry(ScheduledExecutorService group, Consumer<EventLoopGroupRetry> operation) {
        this(
                group,
                operation,
                ExponentialBackoffStrategy.withDefault()
        );
    }

    @Override
    public void run() {
        operation.accept(this);
    }

    @Override
    public void retry() {
        group.schedule(
                this,
                strategy.getAsLong(),
                TimeUnit.MILLISECONDS
        );
    }
}
