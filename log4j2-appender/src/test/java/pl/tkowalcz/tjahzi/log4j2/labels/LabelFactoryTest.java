package pl.tkowalcz.tjahzi.log4j2.labels;

import org.apache.logging.log4j.core.config.NullConfiguration;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LabelFactoryTest {

    @Test
    void shouldRemoveDuplicatedLabels() {
        // Given
        Label thisIsADuplicateAndShouldBeDropped = Label.createLabel("ip", "127.0.0.1", null);
        Label thisShouldStay = Label.createLabel("ip", "10.0.2.34", null);
        Label thisTooShouldStay = Label.createLabel("hostname", "hostname", null);

        LabelFactory labelFactory = new LabelFactory(
                new NullConfiguration(),
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
        Label thisIsAStaticLabel = Label.createLabel("ip", "127.0.0.1", null);
        Label thisIsAlsoAStaticLabel = Label.createLabel("region", "us-east-1", null);
        Label thisIsADynamicLabel = Label.createLabel("tenant", "${ctx:tenant}", null);
        Label thisIsAnotherDynamicLabel = Label.createLabel("tenant", "foo_${ctx:baz}_bar", null);
        Label thisIsAStaticPatternLabel = Label.createLabel("AZ", null, "us-east-1a");
        Label thisIsADynamicPatternLabel = Label.createLabel("iam_role", null, "${iam}");

        LabelFactory labelFactory = new LabelFactory(
                new NullConfiguration(),
                "log_level",
                thisIsAStaticLabel,
                thisIsAlsoAStaticLabel,
                thisIsADynamicLabel,
                thisIsAnotherDynamicLabel,
                thisIsAStaticPatternLabel,
                thisIsADynamicPatternLabel
        );

        // When
        LabelsDescriptor actual = labelFactory.convertLabelsDroppingInvalid();

        // Then
        assertThat(actual.getStaticLabels())
                .containsOnly(
                        asEntry(thisIsAStaticLabel),
                        asEntry(thisIsAlsoAStaticLabel),
                        asEntryUsingPattern(thisIsAStaticPatternLabel)
                );

        assertThat(actual.getDynamicLabels().keySet())
                .containsOnly(
                        thisIsADynamicLabel.getName(),
                        thisIsAnotherDynamicLabel.getName(),
                        thisIsADynamicPatternLabel.getName()
                );
    }

    @Test
    void shouldSkipLabelsWithInvalidName() {
        // Given
        Label thisShouldStay = Label.createLabel("ip", "10.0.2.34", null);
        Label thisShouldStayToo = Label.createLabel("region", "coruscant-west-2", null);
        Label invalidNameShouldBeDropped = Label.createLabel("host-name", "hostname", null);

        LabelFactory labelFactory = new LabelFactory(
                new NullConfiguration(),
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
        Label label1 = Label.createLabel("ip", "10.0.2.34", null);
        Label label2 = Label.createLabel("region", "coruscant-west-2", null);

        LabelFactory labelFactory = new LabelFactory(
                new NullConfiguration(),
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

        Label thisShouldStay = Label.createLabel("ip", "10.0.2.34", null);
        Label thisShouldBeRemovedDueToConflict = Label.createLabel(logLevelLabel, "coruscant-west-2", null);

        LabelFactory labelFactory = new LabelFactory(
                Mockito.mock(XmlConfiguration.class),
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

        LabelFactory labelFactory = new LabelFactory(null, logLevelLabel);

        // When
        LabelsDescriptor actual = labelFactory.convertLabelsDroppingInvalid();

        // Then
        assertThat(actual.getLogLevelLabel()).isNull();
    }

    @Test
    void shouldHandleDisabledLogLevelLabel() {
        // Given
        String logLevelLabel = null;
        LabelFactory labelFactory = new LabelFactory(null, logLevelLabel);

        // When
        LabelsDescriptor actual = labelFactory.convertLabelsDroppingInvalid();

        // Then
        assertThat(actual.getLogLevelLabel()).isNull();
    }

    public Map.Entry<String, String> asEntry(Label label) {
        return Map.entry(label.getName(), label.getValue());
    }

    public Map.Entry<String, String> asEntryUsingPattern(Label label) {
        return Map.entry(label.getName(), label.getPattern());
    }
}
