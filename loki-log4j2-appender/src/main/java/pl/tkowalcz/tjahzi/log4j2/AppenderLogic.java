package pl.tkowalcz.tjahzi.log4j2;

import org.apache.logging.log4j.Logger;
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

    protected static final Logger LOGGER = StatusLogger.getLogger();

    private final LoggingSystem loggingSystem;

    private final String logLevelLabel;
    private final TjahziLogger logger;

    public AppenderLogic(
            LoggingSystem loggingSystem,
            String logLevelLabel
    ) {
        this.loggingSystem = loggingSystem;
        this.logLevelLabel = logLevelLabel;

        logger = loggingSystem.createLogger();
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
        String logLevel = event.getLevel().toString();

        logger.log(
                event.getTimeMillis(),
                Map.of(),
                logLevelLabel,
                logLevel,
                byteBuffer
        );
    }
}
