package pl.tkowalcz.tjahzi.log4j2;

import com.google.common.collect.Maps;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;
import pl.tkowalcz.tjahzi.github.GitHubDocs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.counting;

public class LabelFactory {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private final String logLevelLabel;
    private final Label[] labels;

    public LabelFactory(String logLevelLabel, Label... labels) {
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
            LOGGER.error(
                    "There are duplicated labels which is not allowed by Loki. " +
                            "These labels will be deduplicated undeterministically: {}",
                    duplicatedLabels
            );
        }
    }

    private HashMap<String, String> convertAndLogViolations() {
        HashMap<String, String> lokiLabels = Maps.newHashMap();

        stream(labels)
                .flatMap(label -> {
                            if (label.hasValidName()) {
                                return Stream.of(label);
                            }

                            LOGGER.error("Label '{}' contains invalid characters - ignoring it. {}",
                                    label.getName(),
                                    GitHubDocs.LABEL_NAMING.getLogMessage()
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
            LOGGER.error("Log level label '{}' contains invalid characters - ignoring it. {}",
                    logLevelLabel,
                    GitHubDocs.LABEL_NAMING.getLogMessage()
            );

            return null;
        }

        if (existingLabels.remove(logLevelLabel) != null) {
            LOGGER.error("Log level label '{} conflicts with label defined in configuration - ignoring it.",
                    logLevelLabel
            );
        }

        return logLevelLabel;
    }
}
