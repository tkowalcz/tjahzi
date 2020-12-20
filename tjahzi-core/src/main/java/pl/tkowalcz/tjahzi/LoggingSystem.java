package pl.tkowalcz.tjahzi;

import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;

import java.io.Closeable;
import java.util.function.Consumer;

public class LoggingSystem {

    private final ManyToOneRingBuffer logBuffer;
    private final AgentRunner runner;
    private final Closeable[] resourcesToCleanup;

    public LoggingSystem(
            ManyToOneRingBuffer logBuffer,
            AgentRunner runner,
            Closeable... resourcesToCleanup
    ) {
        this.logBuffer = logBuffer;
        this.runner = runner;
        this.resourcesToCleanup = resourcesToCleanup;
    }

    public TjahziLogger createLogger() {
        return new TjahziLogger(logBuffer);
    }

    public void close(
            int retryCloseTimeoutMs,
            Consumer<Thread> closeFailAction) {
        runner.close(
                retryCloseTimeoutMs,
                closeFailAction
        );

        for (Closeable resource : resourcesToCleanup) {
            try {
                resource.close();
            } catch (Exception ignore) {
            }
        }
    }

    public int getLogBufferSize() {
        return logBuffer.capacity();
    }
}
