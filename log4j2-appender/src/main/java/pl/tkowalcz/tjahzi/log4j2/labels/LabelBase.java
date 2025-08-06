package pl.tkowalcz.tjahzi.log4j2.labels;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

import java.util.regex.Pattern;

public abstract class LabelBase {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private static final Pattern LABEL_NAME_PATTER = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");

    private final String name;
    private final String value;
    private final String pattern;

    LabelBase(String name, String value, String pattern) {
        this.name = name;
        this.value = value;
        this.pattern = pattern;

        if (name == null) {
            LOGGER.error("Property name cannot be null");
        }

        if (pattern == null && value == null) {
            LOGGER.error("Property must have pattern or value specified");
        }
    }

    public boolean hasValidName() {
        return hasValidName(getName());
    }

    public String getName() {
        return name;
    }

    public String getPattern() {
        return pattern;
    }

    public String getValue() {
        return value;
    }

    public static boolean hasValidName(String label) {
        return LABEL_NAME_PATTER.matcher(label).matches();
    }
}
