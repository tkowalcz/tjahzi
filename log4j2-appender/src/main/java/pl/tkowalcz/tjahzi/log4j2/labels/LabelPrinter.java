package pl.tkowalcz.tjahzi.log4j2.labels;

import org.apache.logging.log4j.core.LogEvent;
import pl.tkowalcz.tjahzi.utils.TextBuilder;

import java.util.function.Consumer;

public interface LabelPrinter {

    void append(LogEvent event, Consumer<String> appendable);

    default boolean isStatic() {
        return false;
    }

    default String toStringWithoutEvent() {
        TextBuilder builder = new TextBuilder();
        append(null, builder::append);

        return builder.toString();
    }
}
