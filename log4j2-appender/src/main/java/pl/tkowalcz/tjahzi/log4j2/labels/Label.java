package pl.tkowalcz.tjahzi.log4j2.labels;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.PluginValue;
import org.apache.logging.log4j.status.StatusLogger;

import java.util.regex.Pattern;

@Plugin(name = "label", category = Node.CATEGORY, printObject = true)
public class Label {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private static final Pattern LABEL_NAME_PATTER = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");

    private final String name;
    private final String value;
    private final String pattern;

    private Label(String name, String value, String pattern) {
        this.name = name;
        this.value = value;
        this.pattern = pattern;
    }

    @PluginFactory
    public static Label createLabel(
            @PluginAttribute("name") String name,
            @PluginValue("value") String value,
            @PluginValue("pattern") String pattern
    ) {
        if (name == null) {
            LOGGER.error("Property name cannot be null");
        }

        if (pattern == null && value == null) {
            LOGGER.error("Property must have pattern or value specified");
        }

        return new Label(name, value, pattern);
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

    @Override
    public String toString() {
        return "Label{" +
                "name='" + name + '\'' +
                ", pattern='" + pattern + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
