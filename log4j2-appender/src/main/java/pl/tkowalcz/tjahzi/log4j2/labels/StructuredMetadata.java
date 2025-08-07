package pl.tkowalcz.tjahzi.log4j2.labels;

import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.PluginValue;

@Plugin(name = "Metadata", category = Node.CATEGORY, printObject = true)
public class StructuredMetadata extends LabelBase {

    private StructuredMetadata(String name, String value, String pattern) {
        super(name, value, pattern);
    }

    @PluginFactory
    public static StructuredMetadata createLabel(
            @PluginAttribute("name") String name,
            @PluginValue("value") String value,
            @PluginValue("pattern") String pattern
    ) {
        return new StructuredMetadata(name, value, pattern);
    }

    @Override
    public String toString() {
        return "StructuredMetadata{" +
               "name='" + getName() + '\'' +
               ", value='" + getValue() + '\'' +
               '}';
    }
}
