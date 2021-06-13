package pl.tkowalcz.tjahzi.log4j2.labels;

import org.apache.logging.log4j.core.LogEvent;

import java.util.function.Consumer;

public class MDCLookup implements LabelPrinter {

    private final String variableName;

    public MDCLookup(String variableName) {
        this.variableName = variableName;
    }

    public static LabelPrinter of(String group) {
        return new MDCLookup(group);
    }

    @Override
    public void append(LogEvent event, Consumer<String> appendable) {
        Object value = event.getContextData().getValue(variableName);
        if (value != null) {
            appendable.accept(value.toString());
        }
    }
}
