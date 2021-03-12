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
                thisIsADuplicateAndShouldBeDropped,
                thisShouldStay,
                thisTooShouldStay
        );

        // When
        HashMap<String, String> actual = labelFactory.convertLabelsDroppingInvalid();

        // Then
        assertThat(actual).containsOnly(
                thisShouldStay.asEntry(),
                thisTooShouldStay.asEntry()
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
                thisShouldStayToo,
                thisShouldStay,
                invalidNameShouldBeDropped
        );

        // When
        HashMap<String, String> actual = labelFactory.convertLabelsDroppingInvalid();

        // Then
        assertThat(actual).containsOnly(
                thisShouldStay.asEntry(),
                thisShouldStayToo.asEntry()
        );
    }

    @Test
    void shouldAcceptNullLogLevel() {
        // Given
        Label label1 = Label.createLabel("ip", "10.0.2.34");
        Label label2 = Label.createLabel("region", "coruscant-west-2");

        LabelFactory labelFactory = new LabelFactory(
                new ConsoleAppender<>(),
                null,
                label2,
                label1
        );

        // When
        HashMap<String, String> actual = labelFactory.convertLabelsDroppingInvalid();

        // Then
        assertThat(actual).containsOnly(
                label1.asEntry(),
                label2.asEntry()
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
                thisShouldBeRemovedDueToConflict,
                thisShouldStay
        );

        HashMap<String, String> labels = new HashMap<>(
                Map.ofEntries(
                        thisShouldStay.asEntry(),
                        thisShouldBeRemovedDueToConflict.asEntry()
                )
        );

        // When
        String actual = labelFactory.validateLogLevelLabel(labels);

        // Then
        assertThat(labels).containsOnly(
                thisShouldStay.asEntry()
        );

        assertThat(actual).isEqualTo(logLevelLabel);
    }

    @Test
    void shouldDisableLogLevelLabelIfItHasInvalidName() {
        // Given
        String logLevelLabel = "log-level";

        LabelFactory labelFactory = new LabelFactory(new ConsoleAppender<>(), logLevelLabel);

        // When
        String actual = labelFactory.validateLogLevelLabel(new HashMap<>());

        // Then
        assertThat(actual).isNull();
    }

    @Test
    void shouldHandleDisabledLogLevelLabel() {
        // Given
        String logLevelLabel = null;
        LabelFactory labelFactory = new LabelFactory(new ConsoleAppender<>(), logLevelLabel);

        // When
        String actual = labelFactory.validateLogLevelLabel(new HashMap<>());

        // Then
        assertThat(actual).isNull();
    }
}
