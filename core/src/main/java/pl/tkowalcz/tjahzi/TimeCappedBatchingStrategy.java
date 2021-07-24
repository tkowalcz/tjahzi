package pl.tkowalcz.tjahzi;

import java.time.Clock;

public class TimeCappedBatchingStrategy {

    private final Clock clock;
    private final OutputBuffer outputBuffer;

    private final long batchSize;
    private final long batchWaitMillis;
    private final long shutdownTimeout;

    private long timeoutDeadline;
    private long shutdownDeadline;

    public TimeCappedBatchingStrategy(
            Clock clock,
            OutputBuffer outputBuffer,
            long batchSize,
            long batchWaitMillis,
            long shutdownTimeout
    ) {
        this.clock = clock;
        this.outputBuffer = outputBuffer;
        this.batchSize = batchSize;
        this.batchWaitMillis = batchWaitMillis;
        this.shutdownTimeout = shutdownTimeout;

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
        shutdownDeadline = clock.millis() + shutdownTimeout;
    }

    private boolean exceededWaitTimeThreshold(long currentTimeMillis) {
        return currentTimeMillis > timeoutDeadline & outputBuffer.getBytesPending() > 0;
    }

    private boolean exceededBatchSizeThreshold() {
        return outputBuffer.getBytesPending() > batchSize;
    }
}
