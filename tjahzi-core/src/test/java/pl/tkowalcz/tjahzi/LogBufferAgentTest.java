package pl.tkowalcz.tjahzi;

import logproto.Logproto;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;
import org.agrona.concurrent.ringbuffer.RingBufferDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.tkowalcz.tjahzi.http.NettyHttpClient;
import pl.tkowalcz.tjahzi.stats.SettableClock;
import pl.tkowalcz.tjahzi.stats.StandardMonitoringModule;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

class LogBufferAgentTest {

    private ManyToOneRingBuffer logBuffer;
    private TjahziLogger logger;
    private SettableClock clock;

    @BeforeEach
    void setUp() {
        logBuffer = new ManyToOneRingBuffer(
                new UnsafeBuffer(new byte[1024 * 1024 + RingBufferDescriptor.TRAILER_LENGTH])
        );

        logger = new TjahziLogger(logBuffer, new StandardMonitoringModule());
        clock = new SettableClock();
    }

    @Test
    void shouldSendDataIfOverSizeLimit() throws IOException {
        // Given
        long waitForever = Long.MAX_VALUE;
        int sentBatchAfter5kb = 5 * 1024;

        NettyHttpClient httpClient = mock(NettyHttpClient.class);
        LogBufferAgent agent = new LogBufferAgent(
                clock,
                logBuffer,
                httpClient,
                sentBatchAfter5kb,
                waitForever,
                Map.of()
        );

        for (int i = 0; i < 100; i++) {
            logger.log(
                    42L,
                    Map.of(),
                    null,
                    null,
                    ByteBuffer.wrap((
                            "Cupcake ipsum dolor sit amet cake wafer. " +
                                    "Soufflé jelly beans biscuit topping. " +
                                    "Danish bonbon gummies powder caramels. " +
                                    "Danish jelly beans sweet roll topping jelly beans oat cake toffee. " +
                                    "Chocolate cake sesame snaps brownie biscuit cheesecake. " +
                                    "Ice cream dessert sweet donut marshmallow. " +
                                    "Muffin bear claw cookie jelly-o sugar plum jelly beans apple pie fruitcake cookie. " +
                                    "Tootsie roll carrot cake pastry jujubes jelly beans chupa chups. " +
                                    "Soufflé cake muffin liquorice tart soufflé pie sesame snaps."
                    ).getBytes())
            );
        }

        // When
        agent.doWork();

        // Then
        verify(httpClient).log((Logproto.PushRequest.Builder) any());
    }

    @Test
    void shouldNotSendDataBelowSizeLimit() throws IOException {
        // Given
        int wait5s = 5000;
        int sentBatchAfter5kb = 5 * 1024;

        NettyHttpClient httpClient = mock(NettyHttpClient.class);
        LogBufferAgent agent = new LogBufferAgent(
                clock,
                logBuffer,
                httpClient,
                sentBatchAfter5kb,
                wait5s,
                Map.of()
        );

        logger.log(
                42L,
                Map.of(),
                null,
                null,
                ByteBuffer.wrap("Test".getBytes())
        );

        // When
        agent.doWork();

        // Then
        verifyZeroInteractions(httpClient);
    }

    @Test
    void shouldNotAttemptSendingDataIfThereIsNothingToSend() throws IOException {
        // Given
        int wait5s = 5000;
        int sentBatchAfter5kb = 5 * 1024;

        NettyHttpClient httpClient = mock(NettyHttpClient.class);
        LogBufferAgent agent = new LogBufferAgent(
                clock,
                logBuffer,
                httpClient,
                sentBatchAfter5kb,
                wait5s,
                Map.of()
        );

        // When
        agent.doWork();
        clock.tick(wait5s + 1);
        agent.doWork();
        clock.tick(wait5s + 1);
        agent.doWork();

        // Then
        verifyZeroInteractions(httpClient);
    }

    @Test
    void shouldSendDataBelowSizeLimitIfTimeoutExpires() throws IOException {
        // Given
        int wait5s = 5000;
        int sentBatchAfter5kb = 5 * 1024;

        ManyToOneRingBuffer logBuffer = new ManyToOneRingBuffer(
                new UnsafeBuffer(new byte[1024 + RingBufferDescriptor.TRAILER_LENGTH])
        );

        TjahziLogger logger = new TjahziLogger(logBuffer, new StandardMonitoringModule());

        NettyHttpClient httpClient = mock(NettyHttpClient.class);
        SettableClock clock = new SettableClock();

        LogBufferAgent agent = new LogBufferAgent(
                clock,
                logBuffer,
                httpClient,
                sentBatchAfter5kb,
                wait5s,
                Map.of()
        );

        // When
        logger.log(
                42L,
                Map.of(),
                null,
                null,
                ByteBuffer.wrap("Test".getBytes())
        );
        clock.tick(wait5s + 1);
        agent.doWork();

        logger.log(
                42L,
                Map.of(),
                null,
                null,
                ByteBuffer.wrap("Test".getBytes())
        );
        clock.tick(wait5s + 1);
        agent.doWork();

        // Then
        verify(httpClient, times(2)).log((Logproto.PushRequest.Builder) any());
    }
}
