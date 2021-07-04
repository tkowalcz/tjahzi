package pl.tkowalcz.tjahzi.log4j2.labels;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.pattern.PatternParser;
import org.apache.logging.log4j.status.StatusLogger;
import pl.tkowalcz.tjahzi.github.GitHubDocs;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.toMap;

public class LabelFactory {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private final String logLevelLabel;
    private final Label[] labels;

    private final PatternParser patternParser;

    public LabelFactory(Configuration configuration, String logLevelLabel, Label... labels) {
        this.logLevelLabel = logLevelLabel;
        this.labels = labels;

        this.patternParser = new PatternParser(configuration, PatternLayout.KEY, null);
    }

    public LabelsDescriptor convertLabelsDroppingInvalid() {
        detectAndLogDuplicateLabels();

        Map<String, LabelPrinter> allLabels = convertAndLogViolations();

        Map<String, LabelPrinter> dynamicLabels = new HashMap<>();
        allLabels
                .entrySet()
                .stream()
                .filter(entry -> !entry.getValue().isStatic())
                .forEach(entry -> dynamicLabels.put(entry.getKey(), entry.getValue()));

        Map<String, String> staticLabels = new LinkedHashMap<>();
        allLabels
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().isStatic())
                .forEach(entry -> staticLabels.put(entry.getKey(), entry.getValue().toStringWithoutEvent()));

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

    private Map<String, LabelPrinter> convertAndLogViolations() {
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
                        this::toLabelOrLog4jPattern,
                        (original, duplicate) -> duplicate)
                );
    }

    private LabelPrinter toLabelOrLog4jPattern(Label label) {
        if (label.getPattern() != null) {
            return Log4jAdapterLabelPrinter.of(patternParser.parse(label.getPattern()));
        }

        return LabelPrinterFactory.parse(label);
    }

    private static String validateLogLevelLabel(
            String logLevelLabel,
            Map<String, ?> staticLabels,
            Map<String, ?> dynamicLabels
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
