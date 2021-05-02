package pl.tkowalcz.tjahzi.log4j2;

import java.util.Map;

public class LabelsDescriptor {

    private final String logLevelLabel;

    private final Map<String, String> staticLabels;
    private final Map<String, String> dynamicLabels;

    public LabelsDescriptor(
            String logLevelLabel,
            Map<String, String> staticLabels,
            Map<String, String> dynamicLabels
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

    public Map<String, String> getDynamicLabels() {
        return dynamicLabels;
    }
}

