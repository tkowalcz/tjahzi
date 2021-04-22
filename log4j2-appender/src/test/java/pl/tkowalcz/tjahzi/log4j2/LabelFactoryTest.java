package pl.tkowalcz.tjahzi.log4j2;

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
                "log_level",
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
                "log_level",
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
    void shouldAcceptNullLogLevel() {
        // Given
        Label label1 = Label.createLabel("ip", "10.0.2.34");
        Label label2 = Label.createLabel("region", "coruscant-west-2");

        LabelFactory labelFactory = new LabelFactory(
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
                logLevelLabel,
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

        LabelFactory labelFactory = new LabelFactory(logLevelLabel);

        // When
        String actual = labelFactory.validateLogLevelLabel(new HashMap<>());

        // Then
        assertThat(actual).isNull();
    }

    @Test
    void shouldHandleDisabledLogLevelLabel() {
        // Given
        String logLevelLabel = null;
        LabelFactory labelFactory = new LabelFactory(logLevelLabel);

        // When
        String actual = labelFactory.validateLogLevelLabel(new HashMap<>());

        // Then
        assertThat(actual).isNull();
    }

    public Map.Entry<String, String> asEntry(Label label) {
        return Map.entry(label.getName(), label.getValue());
    }
}
