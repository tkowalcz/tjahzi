package pl.tkowalcz.tjahzi;

import org.agrona.concurrent.Agent;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;
import pl.tkowalcz.tjahzi.http.NettyHttpClient;

import java.io.IOException;

public class LogBufferAgent implements Agent {

    public static final int MAX_MESSAGES_TO_RETRIEVE = 100;

    private final ManyToOneRingBuffer logBuffer;
    private final NettyHttpClient httpClient;

    private final OutputBuffer outputBuffer;

    private final LogBufferMessageHandler messageHandler;
    private final TimeCappedBatchingStrategy batchStrategy;

    public LogBufferAgent(
            TimeCappedBatchingStrategy batchStrategy,
            ManyToOneRingBuffer logBuffer,
            OutputBuffer outputBuffer,
            NettyHttpClient httpClient,
            LogBufferMessageHandler messageHandler) {
        this.batchStrategy = batchStrategy;

        this.logBuffer = logBuffer;
        this.httpClient = httpClient;
        this.messageHandler = messageHandler;

        this.outputBuffer = outputBuffer;
    }

    private int doWork(boolean isTerminating) throws IOException {
        int workDone = logBuffer.read(messageHandler, MAX_MESSAGES_TO_RETRIEVE);

        if (isTerminating || batchStrategy.shouldProceed()) {
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
    }

    @Override
    public String roleName() {
        return "ReadingLogBufferAndSendingHttp";
    }
}
