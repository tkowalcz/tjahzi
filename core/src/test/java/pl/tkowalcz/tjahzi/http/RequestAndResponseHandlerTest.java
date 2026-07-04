package pl.tkowalcz.tjahzi.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.junit.jupiter.api.Test;
import pl.tkowalcz.tjahzi.stats.StandardMonitoringModule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RequestAndResponseHandlerTest {

    private static EmbeddedChannel createChannel(
            StandardMonitoringModule monitoringModule,
            int requestTimeoutMillis,
            int maxRetries
    ) {
        HttpClientInitializer initializer = new HttpClientInitializer(
                monitoringModule,
                mock(SslContext.class),
                "localhost",
                3100,
                requestTimeoutMillis,
                42,
                maxRetries
        );

        return new EmbeddedChannel(initializer);
    }

    private static FullHttpRequest createRequest() {
        return new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1,
                HttpMethod.POST,
                ClientConfigurationBuilder.DEFAULT_LOG_ENDPOINT,
                Unpooled.wrappedBuffer("Boo hoo".getBytes())
        );
    }

    private static DefaultFullHttpResponse createResponse(HttpResponseStatus status) {
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                status
        );

        HttpUtil.setKeepAlive(response, true);
        return response;
    }

    private static void drainOutbound(EmbeddedChannel channel) {
        Object message;
        while ((message = channel.readOutbound()) != null) {
            if (message instanceof ByteBuf) {
                ((ByteBuf) message).release();
            }
        }
    }

    @Test
    void shouldRetryRequestAfterServerError() {
        // Given
        StandardMonitoringModule monitoringModule = new StandardMonitoringModule();
        EmbeddedChannel channel = createChannel(monitoringModule, 10_000, 1);

        // When
        channel.writeOneOutbound(createRequest());
        channel.flush();
        drainOutbound(channel);

        channel.writeOneInbound(createResponse(HttpResponseStatus.INTERNAL_SERVER_ERROR));
        channel.flush();

        // Then
        Awaitility.await()
                .atMost(Durations.FIVE_SECONDS)
                .untilAsserted(() -> {
                    channel.runScheduledPendingTasks();
                    assertThat(monitoringModule.getSentHttpRequests()).isEqualTo(2);
                });

        assertThat(monitoringModule.getRetriedHttpRequests()).isEqualTo(1);
        drainOutbound(channel);
        channel.finishAndReleaseAll();
    }

    @Test
    void shouldGiveUpRetryingWhenRetriesAreExhausted() {
        // Given
        StandardMonitoringModule monitoringModule = new StandardMonitoringModule();
        EmbeddedChannel channel = createChannel(monitoringModule, 10_000, 0);

        FullHttpRequest request = createRequest();
        ByteBuf content = request.content();

        // When
        channel.writeOneOutbound(request);
        channel.flush();
        drainOutbound(channel);

        channel.writeOneInbound(createResponse(HttpResponseStatus.INTERNAL_SERVER_ERROR));
        channel.flush();
        channel.runScheduledPendingTasks();

        // Then
        assertThat(monitoringModule.getSentHttpRequests()).isEqualTo(1);
        assertThat(monitoringModule.getRetriedHttpRequests()).isEqualTo(0);
        assertThat(monitoringModule.getFailedHttpRequests()).isEqualTo(1);
        assertThat(content.refCnt()).isEqualTo(0);

        channel.finishAndReleaseAll();
    }

    @Test
    void shouldNotRetryClientErrors() {
        // Given
        StandardMonitoringModule monitoringModule = new StandardMonitoringModule();
        EmbeddedChannel channel = createChannel(monitoringModule, 10_000, 3);

        FullHttpRequest request = createRequest();
        ByteBuf content = request.content();

        // When
        channel.writeOneOutbound(request);
        channel.flush();
        drainOutbound(channel);

        channel.writeOneInbound(createResponse(HttpResponseStatus.BAD_REQUEST));
        channel.flush();
        channel.runScheduledPendingTasks();

        // Then
        assertThat(monitoringModule.getSentHttpRequests()).isEqualTo(1);
        assertThat(monitoringModule.getRetriedHttpRequests()).isEqualTo(0);
        assertThat(content.refCnt()).isEqualTo(0);

        channel.finishAndReleaseAll();
    }

    @Test
    void shouldCloseChannelWhenResponseDoesNotArriveInTime() {
        // Given
        StandardMonitoringModule monitoringModule = new StandardMonitoringModule();
        EmbeddedChannel channel = createChannel(monitoringModule, 50, 0);

        // When
        channel.writeOneOutbound(createRequest());
        channel.flush();
        drainOutbound(channel);

        // Then
        Awaitility.await()
                .atMost(Durations.FIVE_SECONDS)
                .untilAsserted(() -> {
                    channel.runScheduledPendingTasks();
                    assertThat(channel.isActive()).isFalse();
                });

        assertThat(monitoringModule.getFailedHttpRequests()).isEqualTo(1);
        channel.finishAndReleaseAll();
    }

    @Test
    void shouldNotCloseIdleChannelWithNoRequestsInFlight() throws InterruptedException {
        // Given
        StandardMonitoringModule monitoringModule = new StandardMonitoringModule();
        EmbeddedChannel channel = createChannel(monitoringModule, 50, 0);

        // When
        Thread.sleep(200);
        channel.runScheduledPendingTasks();

        // Then
        assertThat(channel.isActive()).isTrue();
        channel.finishAndReleaseAll();
    }
}
