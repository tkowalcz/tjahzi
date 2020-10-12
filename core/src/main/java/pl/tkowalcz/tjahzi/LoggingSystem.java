package pl.tkowalcz.tjahzi;

import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;

public class LoggingSystem {

    private final ManyToOneRingBuffer logBuffer;

    public LoggingSystem(ManyToOneRingBuffer logBuffer) {
        this.logBuffer = logBuffer;
    }

    public TjahziLogger createLogger() {
        return new TjahziLogger(logBuffer);
    }
}
