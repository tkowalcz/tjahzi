package pl.tkowalcz.tjahzi.log4j2;

import org.apache.logging.log4j.util.Strings;

import java.util.Objects;

/**
 * Class copied from org.apache.logging.log4j.core.config.Property. I have no idea how to reuse that class while assigning
 * it a custom tag name e.g. headers/labels.
 */
public class Property {

    private final String name;
    private final String value;
    private final boolean valueNeedsLookup;

    protected Property(final String name, final String value) {
        this.name = name;
        this.value = value;
        this.valueNeedsLookup = value != null && value.contains("${");
    }

    /**
     * Returns the property name.
     *
     * @return the property name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the property value.
     *
     * @return the value of the property.
     */
    public String getValue() {
        return Objects.toString(value, Strings.EMPTY); // LOG4J2-1313 null would be same as Property not existing
    }

    /**
     * Returns {@code true} if the value contains a substitutable property that requires a lookup to be resolved.
     *
     * @return {@code true} if the value contains {@code "${"}, {@code false} otherwise
     */
    public boolean isValueNeedsLookup() {
        return valueNeedsLookup;
    }

    @Override
    public String toString() {
        return name + '=' + getValue();
    }
}
