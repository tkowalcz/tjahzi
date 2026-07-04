package pl.tkowalcz.tjahzi;

import org.agrona.concurrent.Agent;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;
import pl.tkowalcz.tjahzi.http.NettyHttpClient;
import pl.tkowalcz.tjahzi.stats.MonitoringModule;

import java.io.IOException;

public class LogShipperAgent implements Agent {

    public static final int MAX_MESSAGES_TO_RETRIEVE = 100;

    private final ManyToOneRingBuffer logBuffer;
    private final NettyHttpClient httpClient;

    private final OutputBuffer outputBuffer;

    private final LogBufferMessageHandler messageHandler;
    private final TimeCappedBatchingStrategy batchStrategy;
    private final MonitoringModule monitoringModule;

    public LogShipperAgent(
            TimeCappedBatchingStrategy batchStrategy,
            ManyToOneRingBuffer logBuffer,
            OutputBuffer outputBuffer,
            NettyHttpClient httpClient,
            LogBufferMessageHandler messageHandler,
            MonitoringModule monitoringModule) {
        this.batchStrategy = batchStrategy;

        this.logBuffer = logBuffer;
        this.httpClient = httpClient;
        this.messageHandler = messageHandler;

        this.outputBuffer = outputBuffer;
        this.monitoringModule = monitoringModule;
    }

    private int doWork(boolean isTerminating) throws IOException {
        int workDone = logBuffer.read(messageHandler, MAX_MESSAGES_TO_RETRIEVE);

        if (outputBuffer.getBytesPending() > 0
                && (isTerminating || batchStrategy.shouldProceed())) {
            try {
                httpClient.log(outputBuffer);
            } finally {
                outputBuffer.clear();
            }
        }

        return workDone;
    }

    @Override
    public int doWork() throws IOException {
        return doWork(false);
    }

    @Override
    public void onClose() {
        batchStrategy.initShutdown();

        int workDone = Integer.MAX_VALUE;
        do {
            try {
                workDone = doWork(true);
            } catch (Exception ignore) {
                // We are shutting down, what else can we do?
            }

        } while (workDone != 0 && batchStrategy.shouldContinueShutdown());

        // Whatever is still in the buffer will never be delivered - make the
        // loss visible in monitoring instead of dropping it silently.
        logBuffer.read(
                (msgTypeId, buffer, index, length) -> monitoringModule.incrementDroppedPuts(),
                Integer.MAX_VALUE
        );
    }

    @Override
    public String roleName() {
        return "LogShipper";
    }
}
