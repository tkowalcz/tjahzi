package pl.tkowalcz.tjahzi.log4j2.labels;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.pattern.LiteralPatternConverter;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternFormatter;
import pl.tkowalcz.tjahzi.log4j2.utils.StringBuilders;

import java.util.List;
import java.util.function.Consumer;

@SuppressWarnings("ForLoopReplaceableByForEach")
public class Log4jAdapterLabelPrinter implements LabelPrinter {

    private final List<PatternFormatter> formatters;

    public Log4jAdapterLabelPrinter(List<PatternFormatter> formatters) {
        this.formatters = formatters;
    }

    @Override
    public void append(LogEvent event, Consumer<CharSequence> appendable) {
        StringBuilder outputBuffer = StringBuilders.threadLocal();

        for (int i = 0; i < formatters.size(); i++) {
            formatters.get(i).format(event, outputBuffer);
        }

        appendable.accept(outputBuffer);
    }

    @Override
    public boolean isStatic() {
        for (int i = 0; i < formatters.size(); i++) {
            LogEventPatternConverter converter = formatters.get(i).getConverter();

            if (!(converter instanceof LiteralPatternConverter)) {
                return false;
            }

            if (((LiteralPatternConverter) converter).getLiteral().contains("$")) {
                return false;
            }

        }

        return true;
    }

    public static Log4jAdapterLabelPrinter of(List<PatternFormatter> parse) {
        return new Log4jAdapterLabelPrinter(parse);
    }
}
