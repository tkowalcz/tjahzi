package pl.tkowalcz.tjahzi.stats;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.time.Clock;

public interface MonitoringModule {

    Clock getClock();

    void incrementDroppedPuts();

    void incrementDroppedPuts(Throwable throwable);

//    long getDroppedPuts();

    void incrementSentHttpRequests();

//    long getSentHttpRequests();

    void incrementFailedHttpRequests();

//    long getFailedHttpRequests();

    void incrementRetriedHttpRequests();

//    long getRetriedHttpRequests();

    void addAgentError(Throwable throwable);

//    long getAgentErrors();

    void incrementHttpConnectAttempts();

//    long getHttpConnectAttempts();

    void addPipelineError(Throwable cause);

    void incrementChannelInactive();

//    long getChannelInactive();

    void incrementHttpResponses();

//    long getHttpResponses();

    void incrementHttpErrors(HttpResponseStatus status, ByteBuf content);

    void recordResponseTime(long time);
}
