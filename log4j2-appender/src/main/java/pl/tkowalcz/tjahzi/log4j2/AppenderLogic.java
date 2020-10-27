package pl.tkowalcz.tjahzi.log4j2;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.status.StatusLogger;
import pl.tkowalcz.tjahzi.LoggingSystem;
import pl.tkowalcz.tjahzi.TjahziLogger;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public class AppenderLogic implements BiConsumer<LogEvent, ByteBuffer> {

    protected static final org.apache.logging.log4j.Logger LOGGER = StatusLogger.getLogger();

    private final LoggingSystem loggingSystem;
    private final TjahziLogger logger;

    private final Map<String, String> lokiLabels;

    public AppenderLogic(
            LoggingSystem loggingSystem,
            Map<String, String> lokiLabels
    ) {
        this.loggingSystem = loggingSystem;
        logger = loggingSystem.createLogger();

        this.lokiLabels = lokiLabels;
    }

    public void append(
            Layout<? extends Serializable> layout,
            LogEvent event
    ) {
        ByteBufferDestinations destination = ByteBufferDestinations.threadLocal();
        destination.initialize(this, event);

        layout.encode(event, destination);
        destination.drainRemaining();
    }

    public void close(long timeout, TimeUnit timeUnit) {
        loggingSystem.close(
                (int) timeUnit.toMillis(timeout),
                thread -> LOGGER.error("Loki appender was unable to stop thread on shutdown: " + thread)
        );
    }

    @Override
    public void accept(LogEvent event, ByteBuffer byteBuffer) {
        logger.log(
                event.getTimeMillis(),
                lokiLabels,
                byteBuffer
        );
    }
}
