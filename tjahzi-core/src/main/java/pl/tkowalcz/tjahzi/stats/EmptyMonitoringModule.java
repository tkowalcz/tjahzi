package pl.tkowalcz.tjahzi.stats;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.time.Clock;

public class EmptyMonitoringModule implements MonitoringModule {

    @Override
    public Clock getClock() {
        return Clock.systemUTC();
    }

    @Override
    public void incrementDroppedPuts() {
    }

    @Override
    public void incrementDroppedPuts(Throwable throwable) {
    }

    @Override
    public void incrementSentHttpRequests(int sizeBytes) {
    }

    @Override
    public void incrementFailedHttpRequests() {
    }

    @Override
    public void incrementRetriedHttpRequests() {
    }

    @Override
    public void addAgentError(Throwable throwable) {
    }

    @Override
    public void incrementHttpConnectAttempts() {
    }

    @Override
    public void addPipelineError(Throwable cause) {
    }

    @Override
    public void incrementChannelInactive() {
    }

    @Override
    public void incrementHttpResponses() {
    }

    @Override
    public void incrementHttpErrors(HttpResponseStatus status, ByteBuf responseContent) {
    }

    @Override
    public void recordResponseTime(long time) {
    }
}
