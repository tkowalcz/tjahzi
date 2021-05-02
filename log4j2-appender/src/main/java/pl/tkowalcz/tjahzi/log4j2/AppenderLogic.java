package pl.tkowalcz.tjahzi.log4j2;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.lookup.Interpolator;
import org.apache.logging.log4j.core.lookup.StrLookup;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.status.StatusLogger;
import pl.tkowalcz.tjahzi.LoggingSystem;
import pl.tkowalcz.tjahzi.TjahziLogger;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public class AppenderLogic implements BiConsumer<LogEvent, ByteBuffer> {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private static final Map<String, String> EMPTY_MAP = new HashMap<>();

    private final LoggingSystem loggingSystem;

    private final String logLevelLabel;
    private final TjahziLogger logger;

    private final Map<String, String> lokiLabels;
    private final StrSubstitutor strSubstitutor;

    public AppenderLogic(
            LoggingSystem loggingSystem,
            String logLevelLabel,
            Map<String, String> lokiLabels
    ) {
        this.loggingSystem = loggingSystem;
        this.logLevelLabel = logLevelLabel;

        logger = loggingSystem.createLogger();
        this.lokiLabels = lokiLabels;

        Interpolator interpolator = new Interpolator();
        strSubstitutor = new StrSubstitutor(new StrLookup() {
            @Override
            public String lookup(String key) {
                return null;
            }

            @Override
            public String lookup(LogEvent event, String key) {
                return interpolator.lookup(event, key);
            }
        });
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

        Map<String, String> dynamicLabels = processDynamicLabelsIfAny(event);

        logger.log(
                event.getTimeMillis(),
                dynamicLabels,
                logLevelLabel,
                logLevel,
                byteBuffer
        );
    }

    private Map<String, String> processDynamicLabelsIfAny(LogEvent event) {
        if (lokiLabels.isEmpty()) {
            return EMPTY_MAP;
        }

        LinkedHashMap<String, String> dynamicLabels = new LinkedHashMap<>();
        for (Map.Entry<String, String> label : lokiLabels.entrySet()) {
            String replaced = strSubstitutor.replace(event, label.getValue());
            dynamicLabels.put(label.getKey(), replaced);
        }

        return dynamicLabels;
    }
}
