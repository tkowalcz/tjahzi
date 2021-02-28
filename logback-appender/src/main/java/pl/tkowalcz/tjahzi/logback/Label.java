package pl.tkowalcz.tjahzi.logback;

import java.util.regex.Pattern;

public class Label {

    private static final Pattern LABEL_NAME_PATTER = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");

    private String name;
    private String value;

    public boolean hasValidName() {
        return hasValidName(getName());
    }

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

    public static boolean hasValidName(String label) {
        return LABEL_NAME_PATTER.matcher(label).matches();
    }
}
