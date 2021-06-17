package pl.tkowalcz.tjahzi.log4j2.labels;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LabelPrinterFactory {

    private static final Pattern CONTEXT_PATTERN = Pattern.compile("\\$\\{ctx:([^}:]+)(:-[^}]+)?}");

    public static LabelPrinter parse(Label label) {
        return parse(label.getValue());
    }

    public static LabelPrinter parse(String string) {
        Matcher matcher = CONTEXT_PATTERN.matcher(string);

        List<LabelPrinter> result = new ArrayList<>();
        int lastAppend = 0;
        while (matcher.find()) {
            int start = lastAppend;
            int end = matcher.start();

            String substring = string.substring(start, end);
            result.add(Literal.of(substring));

            String defaultValue = null;
            if (matcher.groupCount() == 2) {
                defaultValue = matcher.group(2);
            }

            String group = matcher.group(1);
            result.add(MDCLookup.of(group, defaultValue));
            lastAppend = matcher.end();
        }

        String substring = string.substring(lastAppend);
        result.add(Literal.of(substring));

        if (result.size() == 1) {
            return result.get(0);
        }

        return AggregateLabelPrinter.of(result);
    }
}
