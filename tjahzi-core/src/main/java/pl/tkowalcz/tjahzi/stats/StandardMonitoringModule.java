package pl.tkowalcz.tjahzi.stats;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.agrona.concurrent.SystemEpochClock;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.concurrent.errors.DistinctErrorLog;

import java.nio.charset.Charset;
import java.time.Clock;
import java.util.concurrent.atomic.AtomicLong;

public class StandardMonitoringModule implements MonitoringModule {

    private AtomicLong droppedPuts = new AtomicLong();
    private AtomicLong httpConnectAttempts = new AtomicLong();

    private AtomicLong sentHttpRequests = new AtomicLong();
    private AtomicLong failedHttpRequests = new AtomicLong();
    private AtomicLong retriedHttpRequests = new AtomicLong();
    private AtomicLong httpResponses = new AtomicLong();
    private AtomicLong channelInactive = new AtomicLong();
    private AtomicLong agentErrors = new AtomicLong();

    private final DistinctErrorLog distinctErrorLog;

    public StandardMonitoringModule() {
        distinctErrorLog = new DistinctErrorLog(
                new UnsafeBuffer(new byte[1024]),
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
    public void incrementSentHttpRequests() {
        sentHttpRequests.incrementAndGet();
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
}
