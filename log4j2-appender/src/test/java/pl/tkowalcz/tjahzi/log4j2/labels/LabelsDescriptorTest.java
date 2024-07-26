package pl.tkowalcz.tjahzi.log4j2.labels;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LabelsDescriptorTest {

    @Test
    void shouldCombineDynamicAndStaticLabels() {
        // Given
        Map<String, String> actualStaticLabels = Map.of("foo", "bar", "123", "456");
        Map<String, LabelPrinter> actualDynamicLabels = Map.of(
                "stuff", MDCLookup.of("zzz", "yyy")
        );

        LabelsDescriptor descriptor = new LabelsDescriptor(
                "level",
                actualStaticLabels,
                actualDynamicLabels
        );

        // When
        Map<String, String> staticLabels = descriptor.getStaticLabels();
        Map<String, LabelPrinter> dynamicLabels = descriptor.getDynamicLabels();
        Map<String, LabelPrinter> allLabels = descriptor.getAllLabels();

        // Then
        assertThat(staticLabels).isEqualTo(actualStaticLabels);
        assertThat(dynamicLabels).isEqualTo(actualDynamicLabels);
        assertThat(allLabels)
                .containsExactlyInAnyOrderEntriesOf(Map.<String, LabelPrinter>of(
                        "foo", Literal.of("bar"),
                        "123", Literal.of("456"),
                        "stuff", MDCLookup.of("zzz", "yyy")
                ));
    }
}