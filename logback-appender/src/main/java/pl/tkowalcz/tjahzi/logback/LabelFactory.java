package pl.tkowalcz.tjahzi.logback;

import ch.qos.logback.core.spi.ContextAware;
import pl.tkowalcz.tjahzi.github.GitHubDocs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.counting;

public class LabelFactory {

    private final ContextAware internalLogger;

    private final String logLevelLabel;
    private final Label[] labels;

    public LabelFactory(
            ContextAware internalLogger,
            String logLevelLabel,
            Label... labels
    ) {
        this.internalLogger = internalLogger;
        this.logLevelLabel = logLevelLabel;
        this.labels = labels;
    }

    public HashMap<String, String> convertLabelsDroppingInvalid() {
        detectAndLogDuplicateLabels();
        return convertAndLogViolations();
    }

    public String validateLogLevelLabel(HashMap<String, String> existingLabels) {
        if (logLevelLabel != null) {
            return validateLogLevelLabelAgainst(
                    existingLabels,
                    logLevelLabel
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
            internalLogger.addWarn(
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

                            internalLogger.addWarn(
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

    private String validateLogLevelLabelAgainst(
            Map<String, String> existingLabels,
            String logLevelLabel
    ) {
        if (!Label.hasValidName(logLevelLabel)) {
            internalLogger.addWarn(
                    String.format(
                            "Ignoring log level label '%s' - contains invalid characters. %s\n",
                            logLevelLabel,
                            GitHubDocs.LABEL_NAMING.getLogMessage()
                    )
            );

            return null;
        }

        if (existingLabels.remove(logLevelLabel) != null) {
            internalLogger.addWarn(
                    String.format(
                            "Ignoring log level label '%s' - conflicts with label defined in configuration.\n",
                            logLevelLabel
                    )
            );
        }

        return logLevelLabel;
    }
}
