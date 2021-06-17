package pl.tkowalcz.tjahzi.log4j2;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.status.StatusLogger;
import pl.tkowalcz.tjahzi.LabelSerializer;
import pl.tkowalcz.tjahzi.LabelSerializers;
import pl.tkowalcz.tjahzi.LoggingSystem;
import pl.tkowalcz.tjahzi.TjahziLogger;
import pl.tkowalcz.tjahzi.log4j2.labels.LabelPrinter;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public class AppenderLogic implements BiConsumer<LogEvent, ByteBuffer> {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private static final Map<String, CharSequence> EMPTY_MAP = new HashMap<>();

    private final LoggingSystem loggingSystem;

    private final String logLevelLabel;
    private final TjahziLogger logger;

    private final Map<String, LabelPrinter> lokiLabels;

    public AppenderLogic(
            LoggingSystem loggingSystem,
            String logLevelLabel,
            Map<String, LabelPrinter> lokiLabels
    ) {
        this.loggingSystem = loggingSystem;
        this.logLevelLabel = logLevelLabel;

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
        LabelSerializer labelSerializer = LabelSerializers.threadLocal();
        processDynamicLabelsIfAny(labelSerializer, event);

        logger.log(
                event.getTimeMillis(),
                labelSerializer,
                byteBuffer
        );
    }

    private void processDynamicLabelsIfAny(LabelSerializer labelSerializer, LogEvent event) {
        if (logLevelLabel != null) {
            labelSerializer.appendLabelName(logLevelLabel);
            labelSerializer.appendWholeLabelValue(event.getLevel().toString());
        }

        for (Map.Entry<String, LabelPrinter> labelPrinter : lokiLabels.entrySet()) {
            labelSerializer.appendLabelName(labelPrinter.getKey());

            labelSerializer.startAppendingLabelValue();
            labelPrinter.getValue().append(event, labelSerializer::appendPartialLabelValue);
            labelSerializer.finishAppendingLabelValue();
        }
    }
}
