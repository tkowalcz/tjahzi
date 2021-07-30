package pl.tkowalcz.tjahzi;

import java.time.Clock;

public class TimeCappedBatchingStrategy {

    private final Clock clock;
    private final OutputBuffer outputBuffer;

    private final long batchSize;
    private final long batchWaitMillis;
    private final long shutdownTimeoutMillis;

    private long timeoutDeadline;
    private long shutdownDeadline;

    public TimeCappedBatchingStrategy(
            Clock clock,
            OutputBuffer outputBuffer,
            long batchSize,
            long batchWaitMillis,
            long shutdownTimeoutMillis
    ) {
        this.clock = clock;
        this.outputBuffer = outputBuffer;
        this.batchSize = batchSize;
        this.batchWaitMillis = batchWaitMillis;
        this.shutdownTimeoutMillis = shutdownTimeoutMillis;

        this.timeoutDeadline = clock.millis() + batchWaitMillis;
    }

    public boolean shouldProceed() {
        long currentTimeMillis = clock.millis();

        boolean shouldProceed = exceededBatchSizeThreshold() || exceededWaitTimeThreshold(currentTimeMillis);
        if (shouldProceed) {
            timeoutDeadline = currentTimeMillis + batchWaitMillis;
        }

        return shouldProceed;
    }

    public boolean shouldContinueShutdown() {
        return clock.millis() < shutdownDeadline;
    }

    public void initShutdown() {
        shutdownDeadline = clock.millis() + shutdownTimeoutMillis;
    }

    private boolean exceededWaitTimeThreshold(long currentTimeMillis) {
        return currentTimeMillis > timeoutDeadline & outputBuffer.getBytesPending() > 0;
    }

    private boolean exceededBatchSizeThreshold() {
        return outputBuffer.getBytesPending() > batchSize;
    }
}
