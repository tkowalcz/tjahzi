package pl.tkowalcz.tjahzi;

import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;

import java.util.function.Consumer;

public class LoggingSystem {

    private final ManyToOneRingBuffer logBuffer;
    private final AgentRunner runner;

    public LoggingSystem(
            ManyToOneRingBuffer logBuffer,
            AgentRunner runner
    ) {
        this.logBuffer = logBuffer;
        this.runner = runner;
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
    }
}
