package pl.tkowalcz.tjahzi.log4j2.labels;

import org.junit.jupiter.api.Test;

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
        LabelsDescriptor actual = labelFactory.convertLabelsDroppingInvalid();

        // Then
        assertThat(actual.getStaticLabels())
                .containsOnly(
                        asEntry(thisShouldStay),
                        asEntry(thisTooShouldStay)
                );
    }

    @Test
    void shouldIdentifyDynamicLabels() {
        // Given
        Label thisIsAStaticLabel = Label.createLabel("ip", "127.0.0.1");
        Label thisIsAlsoAStaticLabel = Label.createLabel("region", "us-east-1");
        Label thisIsADynamicLabel = Label.createLabel("tenant", "${ctx:tenant}");
        Label thisIsAnotherDynamicLabel = Label.createLabel("tenant", "foo_${ctx:baz}_bar");

        LabelFactory labelFactory = new LabelFactory(
                "log_level",
                thisIsAStaticLabel,
                thisIsAlsoAStaticLabel,
                thisIsADynamicLabel,
                thisIsAnotherDynamicLabel
        );

        // When
        LabelsDescriptor actual = labelFactory.convertLabelsDroppingInvalid();

        // Then
        assertThat(actual.getStaticLabels())
                .containsOnly(
                        asEntry(thisIsAStaticLabel),
                        asEntry(thisIsAlsoAStaticLabel)
                );

        assertThat(actual.getDynamicLabels().keySet())
                .containsOnly(
                        thisIsADynamicLabel.getName(),
                        thisIsAnotherDynamicLabel.getName()
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
        LabelsDescriptor actual = labelFactory.convertLabelsDroppingInvalid();

        // Then
        assertThat(actual.getStaticLabels()).containsOnly(
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
        LabelsDescriptor actual = labelFactory.convertLabelsDroppingInvalid();

        // Then
        assertThat(actual.getStaticLabels()).containsOnly(
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

        // When
        LabelsDescriptor actual = labelFactory.convertLabelsDroppingInvalid();

        // Then
        assertThat(actual.getStaticLabels()).containsOnly(
                asEntry(thisShouldStay)
        );

        assertThat(actual.getLogLevelLabel()).isEqualTo(logLevelLabel);
    }

    @Test
    void shouldDisableLogLevelLabelIfItHasInvalidName() {
        // Given
        String logLevelLabel = "log-level";

        LabelFactory labelFactory = new LabelFactory(logLevelLabel);

        // When
        LabelsDescriptor actual = labelFactory.convertLabelsDroppingInvalid();

        // Then
        assertThat(actual.getLogLevelLabel()).isNull();
    }

    @Test
    void shouldHandleDisabledLogLevelLabel() {
        // Given
        String logLevelLabel = null;
        LabelFactory labelFactory = new LabelFactory(logLevelLabel);

        // When
        LabelsDescriptor actual = labelFactory.convertLabelsDroppingInvalid();

        // Then
        assertThat(actual.getLogLevelLabel()).isNull();
    }

    public Map.Entry<String, String> asEntry(Label label) {
        return Map.entry(label.getName(), label.getValue());
    }
}
