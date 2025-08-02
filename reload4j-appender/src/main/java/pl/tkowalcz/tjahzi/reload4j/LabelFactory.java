package pl.tkowalcz.tjahzi.reload4j;

import org.apache.log4j.helpers.LogLog;
import pl.tkowalcz.tjahzi.github.GitHubDocs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.counting;

public class LabelFactory {

    private final String logLevelLabel;
    private final String loggerNameLabel;
    private final String threadNameLabel;
    private final Label[] labels;

    public LabelFactory(
            String logLevelLabel,
            String loggerNameLabel,
            String threadNameLabel,
            Label... labels
    ) {
        this.logLevelLabel = logLevelLabel;
        this.loggerNameLabel = loggerNameLabel;
        this.threadNameLabel = threadNameLabel;
        this.labels = labels;
    }

    public HashMap<String, String> convertLabelsDroppingInvalid() {
        detectAndLogDuplicateLabels();
        return convertAndLogViolations();
    }

    public String validateLogLevelLabel(HashMap<String, String> existingLabels) {
        if (logLevelLabel != null) {
            return validatePredefinedLabelAgainst(
                    existingLabels,
                    logLevelLabel,
                    "log level label"
            );
        }

        return null;
    }

    public String validateLoggerNameLabel(HashMap<String, String> existingLabels) {
        if (loggerNameLabel != null) {
            return validatePredefinedLabelAgainst(
                    existingLabels,
                    loggerNameLabel,
                    "logger name label"
            );
        }

        return null;
    }

    public String validateThreadNameLabel(HashMap<String, String> existingLabels) {
        if (threadNameLabel != null) {
            return validatePredefinedLabelAgainst(
                    existingLabels,
                    threadNameLabel,
                    "thread name label"
            );
        }

        return null;
    }

    private void detectAndLogDuplicateLabels() {
        List<String> duplicatedLabels = stream(labels)
                .collect(Collectors.groupingBy(Label::getName, counting()))
                .entrySet()
                .stream().filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (!duplicatedLabels.isEmpty()) {
            LogLog.warn(
                    String.format(
                            "There are duplicated labels which is not allowed by Loki. " +
                            "These labels will be deduplicated non-deterministically: %s\n",
                            duplicatedLabels
                    )
            );
        }
    }

    private HashMap<String, String> convertAndLogViolations() {
        HashMap<String, String> lokiLabels = new HashMap<>();

        stream(labels)
                .flatMap(label -> {
                            if (label.hasValidName()) {
                                return Stream.of(label);
                            }

                            LogLog.warn(
                                    String.format(
                                            "Ignoring label '%s' - contains invalid characters. %s\n",
                                            label.getName(),
                                            GitHubDocs.LABEL_NAMING.getLogMessage()
                                    )
                            );

                            return Stream.of();
                        }
                )
                .forEach(__ -> lokiLabels.put(__.getName(), __.getValue()));

        return lokiLabels;
    }

    private String validatePredefinedLabelAgainst(
            Map<String, String> existingLabels,
            String predefinedLabel,
            String predefinedLabelDescription
    ) {
        if (!Label.hasValidName(predefinedLabel)) {
            LogLog.warn(
                    String.format(
                            "Ignoring %s '%s' - contains invalid characters. %s\n",
                            predefinedLabelDescription,
                            predefinedLabel,
                            GitHubDocs.LABEL_NAMING.getLogMessage()
                    )
            );

            return null;
        }

        if (existingLabels.remove(predefinedLabel) != null) {
            LogLog.warn(
                    String.format(
                            "Ignoring %s '%s' - conflicts with label defined in configuration.\n",
                            predefinedLabelDescription,
                            predefinedLabel
                    )
            );
        }

        return predefinedLabel;
    }
}
