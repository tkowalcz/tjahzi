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
import java.util.function.Function;

public class LokiAppender extends LokiAppenderConfigurator {

    private EfficientPatternLayout efficientLayout;
    private PatternLayoutEncoder encoder;

    private LoggingSystem loggingSystem;
    private Function<ILoggingEvent, ByteBuffer> actualEncoder;

    private TjahziLogger logger;
    private String logLevelLabel;
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
        ByteBuffer logLine = actualEncoder.apply(event);

        LabelSerializer labelSerializer = LabelSerializers.threadLocal();
        labelSerializer
                .appendLabelName(logLevelLabel)
                .appendWholeLabelValue(logLevel);

        logger.log(
                event.getTimeStamp(),
                labelSerializer,
                logLine
        );
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
        monitoringModuleWrapper = lokiAppenderFactory.getMonitoringModuleWrapper();

        logger = loggingSystem.createLogger();
        loggingSystem.start();
        super.start();
    }

    @Override
    public void stop() {
        loggingSystem.close(
                1000,
                thread -> addError("Loki appender was unable to stop thread on shutdown: " + thread)
        );

        super.stop();
    }
}
