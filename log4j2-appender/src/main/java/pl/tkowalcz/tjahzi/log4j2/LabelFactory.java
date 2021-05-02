package pl.tkowalcz.tjahzi.log4j2;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;
import pl.tkowalcz.tjahzi.github.GitHubDocs;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.toMap;

public class LabelFactory {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private static final Pattern DYNAMIC_LABEL_PATTERN = Pattern.compile("\\$\\{ctx:[^}:]+}");
    private static final Predicate<Map.Entry<String, String>> IS_DYNAMIC_LABEL = (entry) -> DYNAMIC_LABEL_PATTERN.matcher(entry.getValue()).find();

    private final String logLevelLabel;
    private final Label[] labels;

    public LabelFactory(String logLevelLabel, Label... labels) {
        this.logLevelLabel = logLevelLabel;
        this.labels = labels;
    }

    public LabelsDescriptor convertLabelsDroppingInvalid() {
        detectAndLogDuplicateLabels();

        Map<String, String> allLabels = convertAndLogViolations();

        Map<String, String> dynamicLabels = allLabels
                .entrySet()
                .stream()
                .filter(IS_DYNAMIC_LABEL)
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

        Map<String, String> staticLabels = allLabels
                .entrySet()
                .stream()
                .filter(IS_DYNAMIC_LABEL.negate())
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

        String actualLogLevelLabel = validateLogLevelLabel(
                logLevelLabel,
                staticLabels,
                dynamicLabels
        );

        return new LabelsDescriptor(
                actualLogLevelLabel,
                staticLabels,
                dynamicLabels
        );
    }

    private void detectAndLogDuplicateLabels() {
        List<String> duplicatedLabels = stream(labels)
                .collect(Collectors.groupingBy(Label::getName, counting()))
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (!duplicatedLabels.isEmpty()) {
            LOGGER.warn(
                    "There are duplicated labels which is not allowed by Loki. " +
                            "These labels will be deduplicated non-deterministically: {}",
                    duplicatedLabels
            );
        }
    }

    private Map<String, String> convertAndLogViolations() {
        return stream(labels)
                .flatMap(label -> {
                            if (label.hasValidName()) {
                                return Stream.of(label);
                            }

                            LOGGER.error(
                                    "Ignoring label '{}' - contains invalid characters. {}",
                                    label.getName(),
                                    GitHubDocs.LABEL_NAMING.getLogMessage()
                            );

                            return Stream.of();
                        }
                )
                .collect(toMap(
                        Label::getName,
                        Label::getValue,
                        (original, duplicate) -> duplicate)
                );
    }

    private static String validateLogLevelLabel(
            String logLevelLabel,
            Map<String, String> staticLabels,
            Map<String, String> dynamicLabels
    ) {
        if (logLevelLabel == null) {
            return null;
        }

        if (!Label.hasValidName(logLevelLabel)) {
            LOGGER.error(
                    "Ignoring log level label '{}' - contains invalid characters. {}",
                    logLevelLabel,
                    GitHubDocs.LABEL_NAMING.getLogMessage()
            );

            return null;
        }

        if (staticLabels.remove(logLevelLabel) != null) {
            LOGGER.error("Log level label '{} conflicts with label defined in configuration - ignoring it.",
                    logLevelLabel
            );
        }

        if (dynamicLabels.remove(logLevelLabel) != null) {
            LOGGER.error("Log level label '{} conflicts with label defined in configuration - ignoring it.",
                    logLevelLabel
            );
        }

        return logLevelLabel;
    }
}
