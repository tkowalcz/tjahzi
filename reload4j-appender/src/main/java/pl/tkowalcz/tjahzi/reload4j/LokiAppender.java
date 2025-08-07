package pl.tkowalcz.tjahzi.reload4j;

import org.apache.log4j.spi.LoggingEvent;
import pl.tkowalcz.tjahzi.LabelSerializer;
import pl.tkowalcz.tjahzi.LabelSerializers;
import pl.tkowalcz.tjahzi.LoggingSystem;
import pl.tkowalcz.tjahzi.TjahziLogger;
import pl.tkowalcz.tjahzi.stats.MonitoringModule;
import pl.tkowalcz.tjahzi.stats.MutableMonitoringModuleWrapper;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class LokiAppender extends LokiAppenderConfigurator {

    private LoggingSystem loggingSystem;
    private TjahziLogger logger;

    private String logLevelLabel;
    private String loggerNameLabel;
    private String threadNameLabel;
    private List<String> mdcLogLabels;

    private MutableMonitoringModuleWrapper monitoringModuleWrapper;

    /**
     * This is an entry point to set monitoring (statistics) hooks for this appender. This
     * API is in beta and is subject to change (and probably will).
     */
    public void setMonitoringModule(MonitoringModule monitoringModule) {
        if (monitoringModuleWrapper != null) {
            monitoringModuleWrapper.setMonitoringModule(monitoringModule);
        }
    }

    @Override
    protected void append(LoggingEvent event) {
        String formattedMessage = layout.format(event);
        ByteBuffer logLine = ByteBuffer.wrap(formattedMessage.getBytes(StandardCharsets.UTF_8));

        String logLevel = event.getLevel().toString();
        String loggerName = event.getLoggerName();
        String threadName = event.getThreadName();

        @SuppressWarnings("unchecked")
        Map<String, Object> mdcPropertyMap = event.getProperties();

        LabelSerializer labelSerializer = LabelSerializers.threadLocal().getFirst();
        appendLogLabel(labelSerializer, logLevel);
        appendLoggerLabel(labelSerializer, loggerName);
        appendThreadLabel(labelSerializer, threadName);
        appendMdcLogLabels(labelSerializer, mdcPropertyMap);

        logger.log(
                event.getTimeStamp(),
                0L,
                labelSerializer,
                new LabelSerializer(),
                logLine
        );
    }

    private void appendLogLabel(LabelSerializer labelSerializer, String logLevel) {
        if (logLevelLabel != null) {
            labelSerializer.appendLabel(logLevelLabel, logLevel);
        }
    }

    private void appendLoggerLabel(LabelSerializer labelSerializer, String loggerName) {
        if (loggerNameLabel != null) {
            labelSerializer.appendLabel(loggerNameLabel, loggerName);
        }
    }

    private void appendThreadLabel(LabelSerializer labelSerializer, String threadName) {
        if (threadNameLabel != null) {
            labelSerializer.appendLabel(threadNameLabel, threadName);
        }
    }

    @SuppressWarnings("ForLoopReplaceableByForEach") // Allocator goes brrrr
    private void appendMdcLogLabels(LabelSerializer serializer,
                                    Map<String, Object> mdcPropertyMap) {
        for (int i = 0; i < mdcLogLabels.size(); i++) {
            String mdcLogLabel = mdcLogLabels.get(i);

            if (mdcPropertyMap.containsKey(mdcLogLabel)) {
                serializer.appendLabel(mdcLogLabel, mdcPropertyMap.get(mdcLogLabel).toString());
            }
        }
    }

    @Override
    public void activateOptions() {
        if (layout == null) {
            errorHandler.error("No layout set for the appender named [" + name + "].");
            return;
        }

        LokiAppenderFactory lokiAppenderFactory = new LokiAppenderFactory(this);

        loggingSystem = lokiAppenderFactory.createAppender();
        logLevelLabel = lokiAppenderFactory.getLogLevelLabel();
        loggerNameLabel = lokiAppenderFactory.getLoggerNameLabel();
        threadNameLabel = lokiAppenderFactory.getThreadNameLabel();
        mdcLogLabels = lokiAppenderFactory.getMdcLogLabels();
        monitoringModuleWrapper = lokiAppenderFactory.getMonitoringModuleWrapper();

        logger = loggingSystem.createLogger();
        loggingSystem.start();
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }

        if (monitoringModuleWrapper != null) {
            monitoringModuleWrapper.onClose();
        }

        if (loggingSystem != null) {
            loggingSystem.close(
                    (int) TimeUnit.SECONDS.toMillis(10), // Default 10 second timeout
                    thread -> errorHandler.error("Loki appender was unable to stop thread on shutdown: " + thread)
            );
        }

        closed = true;
    }

    @Override
    public boolean requiresLayout() {
        return true;
    }
}
