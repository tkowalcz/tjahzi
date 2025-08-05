package pl.tkowalcz.tjahzi.reload4j;

import java.util.regex.Pattern;

public class Label extends Header {

    private static final Pattern LABEL_NAME_PATTER = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");

    public static Label createLabel(String name, String value) {
        Label result = new Label();
        result.setName(name);
        result.setValue(value);

        return result;
    }

    public boolean hasValidName() {
        return hasValidName(getName());
    }

    public static boolean hasValidName(String label) {
        return LABEL_NAME_PATTER.matcher(label).matches();
    }
}
