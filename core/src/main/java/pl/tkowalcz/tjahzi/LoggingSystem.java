package pl.tkowalcz.tjahzi;

import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;
import pl.tkowalcz.tjahzi.stats.MonitoringModule;

import java.io.Closeable;
import java.util.function.Consumer;

public class LoggingSystem {

    private final ManyToOneRingBuffer logBuffer;
    private final AgentRunner runner;

    private final MonitoringModule monitoringModule;
    private final boolean useDaemonThreads;
    private final Closeable[] resourcesToCleanup;

    public LoggingSystem(
            ManyToOneRingBuffer logBuffer,
            AgentRunner runner,
            MonitoringModule monitoringModule,
            boolean useDaemonThreads,
            Closeable... resourcesToCleanup
    ) {
        this.logBuffer = logBuffer;
        this.runner = runner;

        this.monitoringModule = monitoringModule;
        this.useDaemonThreads = useDaemonThreads;
        this.resourcesToCleanup = resourcesToCleanup;
    }

    public TjahziLogger createLogger() {
        return new TjahziLogger(logBuffer, monitoringModule);
    }

    public void start() {
        AgentRunner.startOnThread(
                runner,
                runnable -> {
                    Thread result = new Thread(runnable);
                    result.setDaemon(useDaemonThreads);

                    return result;
                }
        );
    }

    public void close(
            int retryCloseTimeoutMs,
            Consumer<Thread> closeFailAction) {
        runner.close(
                retryCloseTimeoutMs,
                closeFailAction
        );

        for (Closeable resource : resourcesToCleanup) {
            try {
                resource.close();
            } catch (Exception ignore) {
            }
        }
    }

    public int getLogBufferSize() {
        return logBuffer.capacity();
    }

    public int getLogBufferByteRemainsSize() {
        return logBuffer.size();
    }
}
