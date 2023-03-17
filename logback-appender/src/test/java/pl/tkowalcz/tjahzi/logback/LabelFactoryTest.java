package pl.tkowalcz.tjahzi.logback;

import ch.qos.logback.core.ConsoleAppender;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LabelFactoryTest {

    @Test
    void shouldRemoveDuplicatedLabels() {
        // Given
        Label thisIsADuplicateAndShouldBeDropped = Label.createLabel("ip", "127.0.0.1");
        Label thisShouldStay = Label.createLabel("ip", "10.0.2.34");
        Label thisTooShouldStay = Label.createLabel("hostname", "hostname");

        LabelFactory labelFactory = new LabelFactory(
                new ConsoleAppender<>(),
                "log_level",
                "logger_name",
                "thread_name",
                thisIsADuplicateAndShouldBeDropped,
                thisShouldStay,
                thisTooShouldStay
        );

        // When
        HashMap<String, String> actual = labelFactory.convertLabelsDroppingInvalid();

        // Then
        assertThat(actual).containsOnly(
                asEntry(thisShouldStay),
                asEntry(thisTooShouldStay)
        );
    }

    @Test
    void shouldSkipLabelsWithInvalidName() {
        // Given
        Label thisShouldStay = Label.createLabel("ip", "10.0.2.34");
        Label thisShouldStayToo = Label.createLabel("region", "coruscant-west-2");
        Label invalidNameShouldBeDropped = Label.createLabel("host-name", "hostname");

        LabelFactory labelFactory = new LabelFactory(
                new ConsoleAppender<>(),
                "log_level",
                "logger_name",
                "thread_name",
                thisShouldStayToo,
                thisShouldStay,
                invalidNameShouldBeDropped
        );

        // When
        HashMap<String, String> actual = labelFactory.convertLabelsDroppingInvalid();

        // Then
        assertThat(actual).containsOnly(
                asEntry(thisShouldStay),
                asEntry(thisShouldStayToo)
        );
    }

    @Test
    void shouldAcceptNullPredefinedLabels() {
        // Given
        Label label1 = Label.createLabel("ip", "10.0.2.34");
        Label label2 = Label.createLabel("region", "coruscant-west-2");

        LabelFactory labelFactory = new LabelFactory(
                new ConsoleAppender<>(),
                null,
                null,
                null,
                label2,
                label1
        );

        // When
        HashMap<String, String> actual = labelFactory.convertLabelsDroppingInvalid();

        // Then
        assertThat(actual).containsOnly(
                asEntry(label1),
                asEntry(label2)
        );
    }

    @Test
    void logLevelLabelShouldOverrideConflictingLabel() {
        // Given
        String logLevelLabel = "log_level";

        Label thisShouldStay = Label.createLabel("ip", "10.0.2.34");
        Label thisShouldBeRemovedDueToConflict = Label.createLabel(logLevelLabel, "coruscant-west-2");

        LabelFactory labelFactory = new LabelFactory(
                new ConsoleAppender<>(),
                logLevelLabel,
                null,
                null,
                thisShouldBeRemovedDueToConflict,
                thisShouldStay
        );

        HashMap<String, String> labels = new HashMap<>(
                Map.ofEntries(
                        asEntry(thisShouldStay),
                        asEntry(thisShouldBeRemovedDueToConflict)
                )
        );

        // When
        String actual = labelFactory.validateLogLevelLabel(labels);

        // Then
        assertThat(labels).containsOnly(
                asEntry(thisShouldStay)
        );

        assertThat(actual).isEqualTo(logLevelLabel);
    }

    @Test
    void shouldDisableLogLevelLabelIfItHasInvalidName() {
        // Given
        String logLevelLabel = "log-level";

        LabelFactory labelFactory = new LabelFactory(new ConsoleAppender<>(), logLevelLabel, null, null);

        // When
        String actual = labelFactory.validateLogLevelLabel(new HashMap<>());

        // Then
        assertThat(actual).isNull();
    }

    @Test
    void loggerNameLabelShouldOverrideConflictingLabel() {
        // Given
        String loggerNameLabel = "logger_name";

        Label thisShouldStay = Label.createLabel("ip", "10.0.2.34");
        Label thisShouldBeRemovedDueToConflict = Label.createLabel(loggerNameLabel, "coruscant-west-2");

        LabelFactory labelFactory = new LabelFactory(
                new ConsoleAppender<>(),
                null,
                loggerNameLabel,
                null,
                thisShouldBeRemovedDueToConflict,
                thisShouldStay
        );

        HashMap<String, String> labels = new HashMap<>(
                Map.ofEntries(
                        asEntry(thisShouldStay),
                        asEntry(thisShouldBeRemovedDueToConflict)
                )
        );

        // When
        String actual = labelFactory.validateLoggerNameLabel(labels);

        // Then
        assertThat(labels).containsOnly(
                asEntry(thisShouldStay)
        );

        assertThat(actual).isEqualTo(loggerNameLabel);
    }

    @Test
    void shouldDisableLoggerNameLabelIfItHasInvalidName() {
        // Given
        String loggerNameLabel = "logger-name";

        LabelFactory labelFactory = new LabelFactory(new ConsoleAppender<>(), null, loggerNameLabel, null);

        // When
        String actual = labelFactory.validateLogLevelLabel(new HashMap<>());

        // Then
        assertThat(actual).isNull();
    }

    @Test
    void threadNameLabelShouldOverrideConflictingLabel() {
        // Given
        String threadNameLabel = "thread_name";

        Label thisShouldStay = Label.createLabel("ip", "10.0.2.34");
        Label thisShouldBeRemovedDueToConflict = Label.createLabel(threadNameLabel, "coruscant-west-2");

        LabelFactory labelFactory = new LabelFactory(
                new ConsoleAppender<>(),
                threadNameLabel,
                null,
                null,
                thisShouldBeRemovedDueToConflict,
                thisShouldStay
        );

        HashMap<String, String> labels = new HashMap<>(
                Map.ofEntries(
                        asEntry(thisShouldStay),
                        asEntry(thisShouldBeRemovedDueToConflict)
                )
        );

        // When
        String actual = labelFactory.validateLogLevelLabel(labels);

        // Then
        assertThat(labels).containsOnly(
                asEntry(thisShouldStay)
        );

        assertThat(actual).isEqualTo(threadNameLabel);
    }

    @Test
    void shouldDisableThreadNameLabelIfItHasInvalidName() {
        // Given
        String threadNameLabel = "thread-name";

        LabelFactory labelFactory = new LabelFactory(new ConsoleAppender<>(), threadNameLabel, null, null);

        // When
        String actual = labelFactory.validateLogLevelLabel(new HashMap<>());

        // Then
        assertThat(actual).isNull();
    }

    @Test
    void shouldHandleDisabledPredefinedLabels() {
        // Given
        String logLevelLabel = null;
        String loggerNameLabel = null;
        String threadNameLabel = null;
        LabelFactory labelFactory = new LabelFactory(
                new ConsoleAppender<>(),
                logLevelLabel,
                loggerNameLabel,
                threadNameLabel
        );

        // When
        String actual = labelFactory.validateLogLevelLabel(new HashMap<>());

        // Then
        assertThat(actual).isNull();
    }

    public Map.Entry<String, String> asEntry(Label label) {
        return Map.entry(label.getName(), label.getValue());
    }
}
