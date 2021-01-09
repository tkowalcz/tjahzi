package pl.tkowalcz.tjahzi.stats;

import java.time.Clock;

public class TimingRingBuffer {

    private final Clock clock;
    private final long[] times;

    private int readPosition;
    private int writePosition;

    public TimingRingBuffer(Clock clock, int maxNumberOfRequests) {
        this.clock = clock;
        times = new long[maxNumberOfRequests];
    }

    public void record() {
        int index = Math.abs(writePosition % times.length);
        writePosition++;

        times[index] = clock.millis();
    }

    public long measure() {
        int index = Math.abs(readPosition % times.length);
        readPosition++;

        return clock.millis() - times[index];
    }
}
