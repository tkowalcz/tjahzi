package pl.tkowalcz.tjahzi.logback;

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.EfficientPatternLayout;
import pl.tkowalcz.tjahzi.LabelSerializer;
import pl.tkowalcz.tjahzi.LabelSerializers;
import pl.tkowalcz.tjahzi.LoggingSystem;
import pl.tkowalcz.tjahzi.TjahziLogger;
import pl.tkowalcz.tjahzi.stats.MonitoringModule;
import pl.tkowalcz.tjahzi.stats.MutableMonitoringModuleWrapper;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class LokiAppender extends LokiAppenderConfigurator {

    private EfficientPatternLayout efficientLayout;
    private PatternLayoutEncoder encoder;

    private LoggingSystem loggingSystem;
    private Function<ILoggingEvent, ByteBuffer> actualEncoder;

    private TjahziLogger logger;
    private String logLevelLabel;
    private String loggerNameLabel;
    private String threadNameLabel;
    private List<String> mdcLogLabels;

    private MutableMonitoringModuleWrapper monitoringModuleWrapper;

    public EfficientPatternLayout getEfficientLayout() {
        return efficientLayout;
    }

    public void setEfficientLayout(EfficientPatternLayout efficientLayout) {
        this.efficientLayout = efficientLayout;
    }

    public PatternLayoutEncoder getEncoder() {
        return encoder;
    }

    public void setEncoder(PatternLayoutEncoder encoder) {
        this.encoder = encoder;
    }

    /**
     * This is an entry point to set monitoring (statistics) hooks for this appender. This
     * API is in beta and is subject to change (and probably will).
     */
    public void setMonitoringModule(MonitoringModule monitoringModule) {
        monitoringModuleWrapper.setMonitoringModule(monitoringModule);
    }

    // @VisibleForTesting
    public LoggingSystem getLoggingSystem() {
        return loggingSystem;
    }

    @Override
    protected void append(ILoggingEvent event) {
        String logLevel = event.getLevel().toString();
        String loggerName = event.getLoggerName();
        String threadName = event.getThreadName();
        ByteBuffer logLine = actualEncoder.apply(event);
        Map<String, String> mdcPropertyMap = event.getMDCPropertyMap();

        LabelSerializer labelSerializer = LabelSerializers.threadLocal();
        appendLogLabel(labelSerializer, logLevel);
        appendLoggerLabel(labelSerializer, loggerName);
        appendThreadLabel(labelSerializer, threadName);
        appendMdcLogLabels(labelSerializer, mdcPropertyMap);

        logger.log(
                event.getTimeStamp(),
                0L,
                labelSerializer,
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
                                    Map<String, String> mdcPropertyMap) {
        for (int i = 0; i < mdcLogLabels.size(); i++) {
            String mdcLogLabel = mdcLogLabels.get(i);

            if (mdcPropertyMap.containsKey(mdcLogLabel)) {
                serializer.appendLabel(mdcLogLabel, mdcPropertyMap.get(mdcLogLabel));
            }
        }
    }

    @Override
    public void start() {
        if (encoder == null) {
            if (efficientLayout == null) {
                addError("No encoder set for the appender named [" + name + "]. You can also try setting efficientLayout instead.");
                return;
            }

            actualEncoder = efficientLayout::doEfficientLayout;
        } else {
            encoder.start();
            actualEncoder = event -> ByteBuffer.wrap(encoder.encode(event));
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
        super.start();
    }

    @Override
    public void stop() {
        if (monitoringModuleWrapper != null) {
            monitoringModuleWrapper.onClose();
        }

        loggingSystem.close(
                (int) TimeUnit.SECONDS.toMillis(getShutdownTimeoutSeconds()),
                thread -> addError("Loki appender was unable to stop thread on shutdown: " + thread)
        );

        super.stop();
    }
}
