package pl.tkowalcz.tjahzi.log4j2.labels;

import org.apache.logging.log4j.core.LogEvent;

import java.util.Objects;
import java.util.function.Consumer;

public class MDCLookup implements LabelPrinter {

    private final String variableName;
    private final String defaultValue;

    public MDCLookup(String variableName, String defaultValue) {
        this.variableName = variableName;
        this.defaultValue = defaultValue == null ? "" : defaultValue;
    }

    public static MDCLookup of(String variableName, String defaultValue) {
        return new MDCLookup(variableName, defaultValue);
    }

    @Override
    public void append(LogEvent event, Consumer<CharSequence> appendable) {
        Object value = event.getContextData().getValue(variableName);
        if (value != null) {
            appendable.accept(value.toString());
        } else {
            appendable.accept(defaultValue);
        }
    }

    @Override
    public final boolean equals(Object other) {
        if (other instanceof MDCLookup) {
            MDCLookup that = (MDCLookup) other;
            return Objects.equals(this.variableName, that.variableName)
                   && Objects.equals(this.defaultValue, that.defaultValue);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(variableName, defaultValue);
    }
}
