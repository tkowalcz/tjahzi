package pl.tkowalcz.tjahzi;

import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;
import pl.tkowalcz.tjahzi.stats.MonitoringModule;

import java.nio.ByteBuffer;
import java.util.Map;

public class TjahziLogger {

    public static final int LOG_MESSAGE_TYPE_ID = 5;

    private final ManyToOneRingBuffer logBuffer;
    private final MonitoringModule monitoringModule;

    private final LogBufferSerializer serializer;

    public TjahziLogger(ManyToOneRingBuffer logBuffer, MonitoringModule monitoringModule) {
        this.logBuffer = logBuffer;
        this.monitoringModule = monitoringModule;

        this.serializer = new LogBufferSerializer(logBuffer.buffer());
    }

    public TjahziLogger log(long timestamp,
                            Map<String, String> labels,
                            String logLevelLabel,
                            String logLevel,
                            ByteBuffer line) {
        int requiredSize = serializer.calculateRequiredSizeAscii(
                labels,
                logLevelLabel,
                logLevel,
                line
        );

        int claim = logBuffer.tryClaim(LOG_MESSAGE_TYPE_ID, requiredSize);
        if (claim > 0) {
            putMessageOnRing(
                    timestamp,
                    labels,
                    logLevelLabel,
                    logLevel,
                    line,
                    claim
            );
        } else {
            monitoringModule.incrementDroppedPuts();
        }

        return this;
    }

    private void putMessageOnRing(
            long timestamp,
            Map<String, String> labels,
            String logLevelLabel,
            String logLevel,
            ByteBuffer line,
            int claim
    ) {
        try {
            serializer
                    .writeTo(
                            claim,
                            timestamp,
                            labels,
                            logLevelLabel,
                            logLevel,
                            line
                    );

            logBuffer.commit(claim);
        } catch (Throwable t) {
            logBuffer.abort(claim);
            monitoringModule.incrementDroppedPuts(t);
        }
    }
}
