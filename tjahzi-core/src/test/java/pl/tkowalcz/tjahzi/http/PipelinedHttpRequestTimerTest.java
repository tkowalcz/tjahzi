package pl.tkowalcz.tjahzi.http;

import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.*;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.apache.commons.lang.mutable.MutableLong;
import pl.tkowalcz.tjahzi.stats.SettableClock;
import pl.tkowalcz.tjahzi.stats.StandardMonitoringModule;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

class PipelinedHttpRequestTimerTest {

    @Test
    void shouldCloseChannelIfNoKeepalive() {
        // Given
        SettableClock clock = new SettableClock();

        StandardMonitoringModule monitoringModule = new StandardMonitoringModule() {
            @Override
            public Clock getClock() {
                return clock;
            }
        };

        HttpClientInitializer initializer = new HttpClientInitializer(
                monitoringModule,
                10_000,
                42
        );

        FullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1,
                HttpMethod.POST,
                ClientConfigurationBuilder.DEFAULT_LOG_ENDPOINT,
                Unpooled.wrappedBuffer("Boo hoo".getBytes())
        );

        HttpHeaders headers = new DefaultHttpHeaders();
        headers.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);

        DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.NO_CONTENT,
                Unpooled.wrappedBuffer("OK".getBytes()),
                headers,
                EmptyHttpHeaders.INSTANCE
        );

        // When
        EmbeddedChannel channel = new EmbeddedChannel(initializer);
        channel.writeOneOutbound(request);
        channel.flush();

        channel.writeOneInbound(response);
        channel.flush();

        // Then
        assertThat(channel.isOpen()).isFalse();
    }

    @Test
    void shouldNotCloseChannelOnKeepalive() {
        // Given
        SettableClock clock = new SettableClock();

        StandardMonitoringModule monitoringModule = new StandardMonitoringModule() {
            @Override
            public Clock getClock() {
                return clock;
            }
        };

        HttpClientInitializer initializer = new HttpClientInitializer(
                monitoringModule,
                10_000,
                42
        );

        FullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1,
                HttpMethod.POST,
                ClientConfigurationBuilder.DEFAULT_LOG_ENDPOINT,
                Unpooled.wrappedBuffer("Boo hoo".getBytes())
        );

        HttpHeaders headers = new DefaultHttpHeaders();
        headers.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);

        DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.NO_CONTENT,
                Unpooled.wrappedBuffer("OK".getBytes()),
                headers,
                EmptyHttpHeaders.INSTANCE
        );

        // When
        EmbeddedChannel channel = new EmbeddedChannel(initializer);
        channel.writeOneOutbound(request);
        channel.flush();

        channel.writeOneInbound(response);
        channel.flush();

        // Then
        assertThat(channel.isOpen()).isTrue();
    }

    @Test
    void shouldReportRequestMetrics() {
        // Given
        SettableClock clock = new SettableClock();
        MutableLong requestRTT = new MutableLong();

        StandardMonitoringModule monitoringModule = new StandardMonitoringModule() {
            @Override
            public Clock getClock() {
                return clock;
            }

            @Override
            public void recordResponseTime(long time) {
                requestRTT.setValue(time);
            }
        };

        HttpClientInitializer initializer = new HttpClientInitializer(
                monitoringModule,
                10_000,
                42
        );

        FullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1,
                HttpMethod.POST,
                ClientConfigurationBuilder.DEFAULT_LOG_ENDPOINT,
                Unpooled.wrappedBuffer("Boo hoo".getBytes())
        );

        HttpHeaders headers = new DefaultHttpHeaders();
        headers.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);

        DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.NO_CONTENT
        );

        // When
        EmbeddedChannel channel = new EmbeddedChannel(initializer);
        channel.writeOneOutbound(request);
        channel.flush();

        clock.tick();

        channel.writeOneInbound(response);
        channel.flush();

        // Then
        assertThat(monitoringModule.getFailedHttpRequests()).isZero();
        assertThat(monitoringModule.getSentHttpRequests()).isEqualTo(1);
        assertThat(monitoringModule.getHttpResponses()).isEqualTo(1);
        assertThat(monitoringModule.getSentBytes()).isEqualTo(7);

        assertThat(requestRTT.intValue()).isEqualTo(SettableClock.TICK_AMOUNT);
    }

    @Test
    void shouldReportRequestMetricsOnError() {
        // Given
        SettableClock clock = new SettableClock();
        MutableLong requestRTT = new MutableLong();

        StandardMonitoringModule monitoringModule = new StandardMonitoringModule() {
            @Override
            public Clock getClock() {
                return clock;
            }

            @Override
            public void recordResponseTime(long time) {
                requestRTT.setValue(time);
            }
        };

        HttpClientInitializer initializer = new HttpClientInitializer(
                monitoringModule,
                10_000,
                42
        );

        FullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1,
                HttpMethod.POST,
                ClientConfigurationBuilder.DEFAULT_LOG_ENDPOINT,
                Unpooled.wrappedBuffer("Boo hoo".getBytes())
        );

        HttpHeaders headers = new DefaultHttpHeaders();
        headers.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);

        DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.INTERNAL_SERVER_ERROR
        );

        // When
        EmbeddedChannel channel = new EmbeddedChannel(initializer);
        channel.writeOneOutbound(request);
        channel.flush();

        clock.tick();

        channel.writeOneInbound(response);
        channel.flush();

        // Then
        assertThat(monitoringModule.getFailedHttpRequests()).isEqualTo(1);
        assertThat(monitoringModule.getSentHttpRequests()).isEqualTo(1);
        assertThat(monitoringModule.getHttpResponses()).isEqualTo(1);
        assertThat(monitoringModule.getSentBytes()).isEqualTo(7);

        assertThat(requestRTT.intValue()).isEqualTo(SettableClock.TICK_AMOUNT);
    }
}
