package pl.tkowalcz.thjazi;

import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;

public class LoggingSystem {

    private final ManyToOneRingBuffer logBuffer;

    public LoggingSystem(ManyToOneRingBuffer logBuffer) {
        this.logBuffer = logBuffer;
    }

    public ThjaziLogger createLogger() {
        return new ThjaziLogger(logBuffer);
    }
}
