package pl.tkowalcz.tjahzi.http;

import io.netty.channel.nio.NioEventLoopGroup;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

class EventLoopGroupRetry implements Retry, Runnable {

    private final NioEventLoopGroup group;
    private final Consumer<EventLoopGroupRetry> operation;
    private final ExponentialBackoffStrategy strategy = ExponentialBackoffStrategy.withDefault();

    public EventLoopGroupRetry(NioEventLoopGroup group, Consumer<EventLoopGroupRetry> operation) {
        this.group = group;
        this.operation = operation;
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
