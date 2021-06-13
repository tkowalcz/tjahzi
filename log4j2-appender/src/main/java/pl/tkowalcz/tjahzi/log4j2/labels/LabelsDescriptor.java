package pl.tkowalcz.tjahzi.log4j2.labels;

import java.util.Map;

public class LabelsDescriptor {

    private final String logLevelLabel;

    private final Map<String, String> staticLabels;
    private final Map<String, LabelPrinter> dynamicLabels;

    public LabelsDescriptor(
            String logLevelLabel,
            Map<String, String> staticLabels,
            Map<String, LabelPrinter> dynamicLabels
    ) {
        this.logLevelLabel = logLevelLabel;
        this.staticLabels = staticLabels;
        this.dynamicLabels = dynamicLabels;
    }

    public String getLogLevelLabel() {
        return logLevelLabel;
    }

    public Map<String, String> getStaticLabels() {
        return staticLabels;
    }

    public Map<String, LabelPrinter> getDynamicLabels() {
        return dynamicLabels;
    }
}

