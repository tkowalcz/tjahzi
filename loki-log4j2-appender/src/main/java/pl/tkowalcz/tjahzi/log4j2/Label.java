package pl.tkowalcz.tjahzi.log4j2;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.PluginValue;
import org.apache.logging.log4j.status.StatusLogger;

import java.util.Map;
import java.util.regex.Pattern;

@Plugin(name = "label", category = Node.CATEGORY, printObject = true)
public class Label extends Property {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private static final Pattern LABEL_NAME_PATTER = Pattern.compile("[a-zA-Z_:][a-zA-Z0-9_:]*");

    private Label(String name, String value) {
        super(name, value);
    }

    @PluginFactory
    public static Label createLabel(
            @PluginAttribute("name") String name,
            @PluginValue("value") String value) {
        if (name == null) {
            LOGGER.error("Property name cannot be null");
        }

        return new Label(name, value);
    }

    public boolean hasValidName() {
        return hasValidName(getName());
    }

    // @VisibleForTests
    public Map.Entry<String, String> asEntry() {
        return Map.entry(getName(), getValue());
    }

    public static boolean hasValidName(String label) {
        return LABEL_NAME_PATTER.matcher(label).matches();
    }
}
