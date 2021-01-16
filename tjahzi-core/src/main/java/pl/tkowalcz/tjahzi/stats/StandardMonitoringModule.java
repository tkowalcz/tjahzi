package pl.tkowalcz.tjahzi.stats;

import com.google.common.util.concurrent.Uninterruptibles;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.agrona.concurrent.SystemEpochClock;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.concurrent.errors.DistinctErrorLog;

import java.nio.charset.Charset;
import java.time.Clock;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class StandardMonitoringModule implements MonitoringModule, Runnable {

    private static final int ERROR_LOG_CAPACITY = 1024;

    private final AtomicLong droppedPuts = new AtomicLong();
    private final AtomicLong httpConnectAttempts = new AtomicLong();

    private final AtomicLong sentHttpRequests = new AtomicLong();
    private final AtomicLong sentBytes = new AtomicLong();

    private final AtomicLong failedHttpRequests = new AtomicLong();
    private final AtomicLong retriedHttpRequests = new AtomicLong();
    private final AtomicLong httpResponses = new AtomicLong();
    private final AtomicLong channelInactive = new AtomicLong();
    private final AtomicLong agentErrors = new AtomicLong();

    private final DistinctErrorLog distinctErrorLog;

    public StandardMonitoringModule() {
        StatsDumpingThread thread = new StatsDumpingThread(this);
        if (thread.isEnabled()) {
            thread.start();
        }

        distinctErrorLog = new DistinctErrorLog(
                new UnsafeBuffer(new byte[ERROR_LOG_CAPACITY]),
                new SystemEpochClock()
        );

        distinctErrorLog.record(new NullPointerException());
    }

    @Override
    public Clock getClock() {
        return Clock.systemUTC();
    }

    @Override
    public void incrementDroppedPuts() {
        droppedPuts.incrementAndGet();
    }

    public long getDroppedPuts() {
        return droppedPuts.get();
    }

    @Override
    public void incrementDroppedPuts(Throwable throwable) {
        incrementDroppedPuts();
        distinctErrorLog.record(throwable);
    }

    @Override
    public void incrementSentHttpRequests(int sizeBytes) {
        sentHttpRequests.incrementAndGet();
        sentBytes.addAndGet(sizeBytes);
    }

    public long getSentHttpRequests() {
        return sentHttpRequests.get();
    }

    @Override
    public void incrementFailedHttpRequests() {
        failedHttpRequests.incrementAndGet();
    }

    public long getFailedHttpRequests() {
        return failedHttpRequests.get();
    }

    @Override
    public void incrementRetriedHttpRequests() {
        retriedHttpRequests.incrementAndGet();
    }

    public long getRetriedHttpRequests() {
        return retriedHttpRequests.get();
    }

    @Override
    public void addAgentError(Throwable throwable) {
        agentErrors.incrementAndGet();
        distinctErrorLog.record(throwable);
    }

    public long getAgentErrors() {
        return agentErrors.get();
    }

    @Override
    public void incrementHttpConnectAttempts() {
        httpConnectAttempts.incrementAndGet();
    }

    public long getHttpConnectAttempts() {
        return httpConnectAttempts.get();
    }

    @Override
    public void incrementChannelInactive() {
        channelInactive.incrementAndGet();
    }

    public long getChannelInactive() {
        return channelInactive.get();
    }

    @Override
    public void incrementHttpResponses() {
        httpResponses.incrementAndGet();
    }

    public long getHttpResponses() {
        return httpResponses.get();
    }

    @Override
    public void addPipelineError(Throwable cause) {
        distinctErrorLog.record(cause);
    }

    @Override
    public void incrementHttpErrors(HttpResponseStatus status, ByteBuf content) {
        distinctErrorLog.record(
                new RuntimeException(
                        content.toString(Charset.defaultCharset())
                )
        );
    }

    @Override
    public void recordResponseTime(long time) {
    }

    @Override
    public void run() {
        while (true) {
            System.out.println(toString());
            Uninterruptibles.sleepUninterruptibly(10, TimeUnit.SECONDS);
        }
    }

    @Override
    public String toString() {
        return "StandardMonitoringModule{" +
                "droppedPuts=" + droppedPuts +
                ", httpConnectAttempts=" + httpConnectAttempts +
                ", sentHttpRequests=" + sentHttpRequests +
                ", sentKilobytes=" + (sentBytes.longValue() / 1024) +
                ", failedHttpRequests=" + failedHttpRequests +
                ", retriedHttpRequests=" + retriedHttpRequests +
                ", httpResponses=" + httpResponses +
                ", channelInactive=" + channelInactive +
                ", agentErrors=" + agentErrors +
                '}';
    }
}
