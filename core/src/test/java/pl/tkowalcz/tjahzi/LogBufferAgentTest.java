package pl.tkowalcz.tjahzi;

import io.netty.buffer.PooledByteBufAllocator;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

class LogBufferAgentTest {

    private ManyToOneRingBuffer logBuffer;
    private TjahziLogger logger;
    private SettableClock clock;

    @BeforeEach
    void setUp() {
        logBuffer = new ManyToOneRingBuffer(
                new UnsafeBuffer(
                        ByteBuffer.allocateDirect(1024 * 1024 + RingBufferDescriptor.TRAILER_LENGTH)
                )
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

        OutputBuffer outputBuffer = new OutputBuffer(PooledByteBufAllocator.DEFAULT.buffer());
        LogShipperAgent agent = new LogShipperAgent(
                new TimeCappedBatchingStrategy(
                        clock,
                        outputBuffer,
                        sentBatchAfter5kb,
                        waitForever,
                        10_000
                ),
                logBuffer,
                outputBuffer,
                httpClient,
                new LogBufferMessageHandler(
                        logBuffer,
                        Map.of(),
                        outputBuffer
                ),
                new StandardMonitoringModule()
        );

        for (int i = 0; i < 100; i++) {
            logger.log(
                    42L,
                    0,
                    LabelSerializerCreator.from(Map.of()),
                    new LabelSerializer(),
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
        verify(httpClient).log((OutputBuffer) any());
    }

    @Test
    void shouldNotSendDataBelowSizeLimit() throws IOException {
        // Given
        int wait5s = 5000;
        int sentBatchAfter5kb = 5 * 1024;

        NettyHttpClient httpClient = mock(NettyHttpClient.class);
        OutputBuffer outputBuffer = new OutputBuffer(PooledByteBufAllocator.DEFAULT.buffer());
        LogShipperAgent agent = new LogShipperAgent(
                new TimeCappedBatchingStrategy(
                        clock,
                        outputBuffer,
                        sentBatchAfter5kb,
                        wait5s,
                        10_000
                ),
                logBuffer,
                outputBuffer,
                httpClient,
                new LogBufferMessageHandler(
                        logBuffer,
                        Map.of(),
                        outputBuffer
                ),
                new StandardMonitoringModule()
        );

        logger.log(
                42L,
                0,
                LabelSerializerCreator.from(Map.of()),
                new LabelSerializer(),
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
        OutputBuffer outputBuffer = new OutputBuffer(PooledByteBufAllocator.DEFAULT.buffer());
        LogShipperAgent agent = new LogShipperAgent(
                new TimeCappedBatchingStrategy(
                        clock,
                        outputBuffer,
                        sentBatchAfter5kb,
                        wait5s,
                        10_000
                ),
                logBuffer,
                outputBuffer,
                httpClient,
                new LogBufferMessageHandler(
                        logBuffer,
                        Map.of(),
                        outputBuffer
                ),
                new StandardMonitoringModule()
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
                new UnsafeBuffer(
                        ByteBuffer.allocateDirect(1024 + RingBufferDescriptor.TRAILER_LENGTH)
                )
        );

        TjahziLogger logger = new TjahziLogger(logBuffer, new StandardMonitoringModule());

        NettyHttpClient httpClient = mock(NettyHttpClient.class);
        SettableClock clock = new SettableClock();

        OutputBuffer outputBuffer = new OutputBuffer(PooledByteBufAllocator.DEFAULT.buffer());
        LogShipperAgent agent = new LogShipperAgent(
                new TimeCappedBatchingStrategy(
                        clock,
                        outputBuffer,
                        sentBatchAfter5kb,
                        wait5s,
                        10_000
                ),
                logBuffer,
                outputBuffer,
                httpClient,
                new LogBufferMessageHandler(
                        logBuffer,
                        Map.of(),
                        outputBuffer
                ),
                new StandardMonitoringModule()
        );

        // When
        logger.log(
                42L,
                0,
                LabelSerializerCreator.from(Map.of()),
                new LabelSerializer(),
                ByteBuffer.wrap("Test".getBytes())
        );
        clock.tick(wait5s + 1);
        agent.doWork();

        logger.log(
                42L,
                0,
                LabelSerializerCreator.from(Map.of()),
                new LabelSerializer(),
                ByteBuffer.wrap("Test".getBytes())
        );
        clock.tick(wait5s + 1);
        agent.doWork();

        // Then
        verify(httpClient, times(2)).log((OutputBuffer) any());
    }

    @Test
    void shouldDrainAllMessagesOnClose() throws IOException {
        // Given
        ManyToOneRingBuffer logBuffer = new ManyToOneRingBuffer(
                new UnsafeBuffer(
                        ByteBuffer.allocateDirect(64 * 1024 + RingBufferDescriptor.TRAILER_LENGTH)
                )
        );

        TjahziLogger logger = new TjahziLogger(logBuffer, new StandardMonitoringModule());

        NettyHttpClient httpClient = mock(NettyHttpClient.class);
        SettableClock clock = new SettableClock();

        OutputBuffer outputBuffer = new OutputBuffer(PooledByteBufAllocator.DEFAULT.buffer());
        LogShipperAgent agent = new LogShipperAgent(
                new TimeCappedBatchingStrategy(
                        clock,
                        outputBuffer,
                        Integer.MAX_VALUE,
                        Integer.MAX_VALUE,
                        10_000
                ),
                logBuffer,
                outputBuffer,
                httpClient,
                new LogBufferMessageHandler(
                        logBuffer,
                        Map.of(),
                        outputBuffer
                ),
                new StandardMonitoringModule()
        );

        for (int i = 0; i < LogShipperAgent.MAX_MESSAGES_TO_RETRIEVE * 2 + 1; i++) {
            logger.log(
                    42L,
                    0,
                    LabelSerializerCreator.from(Map.of()),
                    new LabelSerializer(),
                    ByteBuffer.wrap("Test".getBytes())
            );
        }

        // When
        agent.onClose();

        // Then - three batches of (100, 100, 1) messages and no trailing empty push
        verify(httpClient, times(3)).log((OutputBuffer) any());
        assertThat(logBuffer.size()).isZero();
    }

    @Test
    void shouldCountMessagesAbandonedWhenShutdownDeadlineIsExceeded() throws IOException {
        // Given
        ManyToOneRingBuffer logBuffer = new ManyToOneRingBuffer(
                new UnsafeBuffer(
                        ByteBuffer.allocateDirect(64 * 1024 + RingBufferDescriptor.TRAILER_LENGTH)
                )
        );

        StandardMonitoringModule monitoringModule = new StandardMonitoringModule();
        TjahziLogger logger = new TjahziLogger(logBuffer, monitoringModule);

        NettyHttpClient httpClient = mock(NettyHttpClient.class);
        SettableClock clock = new SettableClock();

        OutputBuffer outputBuffer = new OutputBuffer(PooledByteBufAllocator.DEFAULT.buffer());
        LogShipperAgent agent = new LogShipperAgent(
                new TimeCappedBatchingStrategy(
                        clock,
                        outputBuffer,
                        Integer.MAX_VALUE,
                        Integer.MAX_VALUE,
                        0
                ),
                logBuffer,
                outputBuffer,
                httpClient,
                new LogBufferMessageHandler(
                        logBuffer,
                        Map.of(),
                        outputBuffer
                ),
                monitoringModule
        );

        for (int i = 0; i < LogShipperAgent.MAX_MESSAGES_TO_RETRIEVE * 2 + 1; i++) {
            logger.log(
                    42L,
                    0,
                    LabelSerializerCreator.from(Map.of()),
                    new LabelSerializer(),
                    ByteBuffer.wrap("Test".getBytes())
            );
        }

        // When - shutdown timeout of zero allows only a single drain iteration
        agent.onClose();

        // Then - 100 messages went out, the remaining 101 were abandoned but counted
        verify(httpClient, times(1)).log((OutputBuffer) any());
        assertThat(monitoringModule.getDroppedPuts()).isEqualTo(101);
        assertThat(logBuffer.size()).isZero();
    }
}
