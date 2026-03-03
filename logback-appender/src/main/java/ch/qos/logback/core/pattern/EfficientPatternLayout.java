package ch.qos.logback.core.pattern;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.pattern.EnsureExceptionHandling;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.nio.ByteBuffer;
import java.util.Map;

public class EfficientPatternLayout extends PatternLayoutBase<ILoggingEvent> {

    private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);

    public EfficientPatternLayout() {
        this.postCompileProcessor = new EnsureExceptionHandling();
    }

    public Map<String, String> getDefaultConverterMap() {
        return PatternLayout.defaultConverterMap;
    }

    public ByteBuffer doEfficientLayout(ILoggingEvent event) {
        if (!isStarted()) {
            return EMPTY_BUFFER;
        }

        return efficientWriteLoopOnConverters(event);
    }

    protected ByteBuffer efficientWriteLoopOnConverters(ILoggingEvent event) {
        StringBuilder strBuilder = StringBuilders.threadLocal();
        Converter<ILoggingEvent> c = head;
        while (c != null) {
            c.write(strBuilder, event);
            c = c.getNext();
        }

        Encoder encoder = Encoders.threadLocal();
        encoder.encode(strBuilder);

        return encoder.getBuffer();
    }

    @Override
    protected String getPresentationHeaderPrefix() {
        return PatternLayout.HEADER_PREFIX;
    }

    @Override
    public String doLayout(ILoggingEvent event) {
        ByteBuffer byteBuffer = doEfficientLayout(event);

        return new String(
                byteBuffer.array(),
                byteBuffer.position(),
                byteBuffer.limit()
        );
    }
}
