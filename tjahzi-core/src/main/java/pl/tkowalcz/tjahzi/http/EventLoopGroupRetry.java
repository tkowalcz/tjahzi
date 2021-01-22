package pl.tkowalcz.tjahzi.http;

import pl.tkowalcz.tjahzi.stats.MonitoringModule;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

class EventLoopGroupRetry implements Retry, Runnable {

    private final ScheduledExecutorService group;
    private final Consumer<EventLoopGroupRetry> operation;
    private final ExponentialBackoffStrategy strategy;

    private final MonitoringModule monitoringModule;

    // VisibleForTesting
    public EventLoopGroupRetry(
            ScheduledExecutorService group,
            Consumer<EventLoopGroupRetry> operation,
            ExponentialBackoffStrategy strategy,
            MonitoringModule monitoringModule
    ) {
        this.group = group;
        this.operation = operation;
        this.strategy = strategy;

        this.monitoringModule = monitoringModule;
    }

    public EventLoopGroupRetry(
            ScheduledExecutorService group,
            Consumer<EventLoopGroupRetry> operation,
            MonitoringModule monitoringModule
    ) {
        this(
                group,
                operation,
                ExponentialBackoffStrategy.withDefault(),
                monitoringModule
        );
    }

    @Override
    public void run() {
        monitoringModule.incrementHttpConnectAttempts();
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
