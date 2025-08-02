package pl.tkowalcz.tjahzi.reload4j;

import org.apache.log4j.xml.UnrecognizedElementHandler;
import org.w3c.dom.Element;

import java.util.Properties;

public class Header implements UnrecognizedElementHandler {

    private String name;
    private String value;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean parseUnrecognizedElement(Element element, Properties props) throws Exception {
        if ("name".equals(element.getNodeName())) {
            String textContent = element.getTextContent();
            if (textContent != null) {
                setName(textContent.trim());
                return true;
            }
        } else if ("value".equals(element.getNodeName())) {
            String textContent = element.getTextContent();
            if (textContent != null) {
                setValue(textContent.trim());
                return true;
            }
        }

        return false;
    }
}
