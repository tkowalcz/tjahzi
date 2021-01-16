package pl.tkowalcz.tjahzi.stats;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.time.Clock;

public interface MonitoringModule {

    Clock getClock();

    void incrementDroppedPuts();

    void incrementDroppedPuts(Throwable throwable);

    void incrementSentHttpRequests(int sizeBytes);

    void incrementFailedHttpRequests();

    void incrementRetriedHttpRequests();

    void addAgentError(Throwable throwable);

    void incrementHttpConnectAttempts();

    void addPipelineError(Throwable cause);

    void incrementChannelInactive();

    void incrementHttpResponses();

    void incrementHttpErrors(HttpResponseStatus status, ByteBuf content);

    void recordResponseTime(long time);
}
