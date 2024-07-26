package pl.tkowalcz.tjahzi.log4j2.labels;

import org.apache.logging.log4j.core.LogEvent;

import java.util.Objects;
import java.util.function.Consumer;

public class Literal implements LabelPrinter {

    private final String contents;

    private Literal(String contents) {
        this.contents = contents;
    }

    @Override
    public boolean isStatic() {
        return true;
    }

    public static Literal of(String string) {
        return new Literal(string);
    }

    @Override
    public void append(LogEvent event, Consumer<CharSequence> appendable) {
        appendable.accept(contents);
    }

    @Override
    public final boolean equals(Object other) {
        if (other instanceof Literal) {
            Literal that = (Literal) other;
            return Objects.equals(that.contents, contents);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(contents);
    }
}
