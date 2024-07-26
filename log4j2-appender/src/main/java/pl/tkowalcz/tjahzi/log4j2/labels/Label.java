package pl.tkowalcz.tjahzi.log4j2.labels;

import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.PluginValue;

@Plugin(name = "label", category = Node.CATEGORY, printObject = true)
public class Label extends LabelBase {

    Label(String name, String value, String pattern) {
        super(name, value, pattern);
    }

    @PluginFactory
    public static Label createLabel(
            @PluginAttribute("name") String name,
            @PluginValue("value") String value,
            @PluginValue("pattern") String pattern
    ) {
        return new Label(name, value, pattern);
    }

    @Override
    public String toString() {
        return "Label{" +
               "name='" + getName() + '\'' +
               ", pattern='" + getPattern() + '\'' +
               ", value='" + getValue() + '\'' +
               '}';
    }
}
