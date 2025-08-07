package pl.tkowalcz.tjahzi.log4j2;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.status.StatusLogger;
import pl.tkowalcz.tjahzi.*;
import pl.tkowalcz.tjahzi.log4j2.labels.LabelPrinter;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public class AppenderLogic implements BiConsumer<LogEvent, ByteBuffer> {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private final LoggingSystem loggingSystem;

    private final String logLevelLabel;
    private final TjahziLogger logger;

    private final Map<String, LabelPrinter> lokiLabels;
    private final Map<String, LabelPrinter> structuredMetadata;

    private final ByteBufferDestinationRepository destinationRepository;

    public AppenderLogic(
            LoggingSystem loggingSystem,
            String logLevelLabel,
            Map<String, LabelPrinter> lokiLabels,
            Map<String, LabelPrinter> structuredMetadata,
            int maxLogLineSizeBytes
    ) {
        this.loggingSystem = loggingSystem;
        this.logLevelLabel = logLevelLabel;
        this.logger = loggingSystem.createLogger();

        this.lokiLabels = lokiLabels;
        this.structuredMetadata = structuredMetadata;

        this.destinationRepository = new ByteBufferDestinationRepository(maxLogLineSizeBytes);
    }

    public void append(
            Layout<? extends Serializable> layout,
            LogEvent event
    ) {
        TjahziByteBufferDestination destination = destinationRepository.threadLocalDestination(
                this,
                event
        );

        layout.encode(event, destination);
        destination.drainRemaining();
    }

    public void close(long timeout, TimeUnit timeUnit) {
        loggingSystem.close(
                (int) timeUnit.toMillis(timeout),
                thread -> LOGGER.error("Loki appender was unable to stop thread on shutdown: {}", thread)
        );
    }

    @Override
    public void accept(LogEvent event, ByteBuffer byteBuffer) {
        LabelSerializerPair labelSerializerPair = LabelSerializers.threadLocal();
        LabelSerializer labelSerializer = labelSerializerPair.getFirst();
        LabelSerializer metadataSerializer = labelSerializerPair.getSecond();

        processDynamicLabelsIfAny(labelSerializer, event);
        processStructuredMetadata(metadataSerializer, event);

        logger.log(
                event.getInstant().getEpochMillisecond(),
                event.getInstant().getNanoOfMillisecond(),
                labelSerializer,
                metadataSerializer,
                byteBuffer
        );
    }

    private void processDynamicLabelsIfAny(LabelSerializer labelSerializer, LogEvent event) {
        if (logLevelLabel != null) {
            labelSerializer.appendLabel(logLevelLabel, event.getLevel().toString());
        }

        for (Map.Entry<String, LabelPrinter> labelPrinter : lokiLabels.entrySet()) {
            labelSerializer.appendLabelName(labelPrinter.getKey());

            labelSerializer.startAppendingLabelValue();
            labelPrinter.getValue().append(event, labelSerializer::appendPartialLabelValue);
            labelSerializer.finishAppendingLabelValue();
        }
    }

    private void processStructuredMetadata(LabelSerializer labelSerializer, LogEvent event) {
        for (Map.Entry<String, LabelPrinter> labelPrinter : structuredMetadata.entrySet()) {
            labelSerializer.appendLabelName(labelPrinter.getKey());

            labelSerializer.startAppendingLabelValue();
            labelPrinter.getValue().append(event, labelSerializer::appendPartialLabelValue);
            labelSerializer.finishAppendingLabelValue();
        }
    }
}
