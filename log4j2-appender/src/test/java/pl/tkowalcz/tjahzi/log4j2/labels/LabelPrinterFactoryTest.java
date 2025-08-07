package pl.tkowalcz.tjahzi.log4j2.labels;

import org.apache.logging.log4j.core.impl.JdkMapAdapterStringMap;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import pl.tkowalcz.tjahzi.LabelSerializer;
import pl.tkowalcz.tjahzi.LabelSerializers;

import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class LabelPrinterFactoryTest {

    private static Stream<Arguments> shouldParseEmptyString() {
        return Stream.of(
                Arguments.of("Should accept empty string", "", ""),
                Arguments.of("Should handle no substitution", "Macaroon ice cream gummies donut jujubes", "Macaroon ice cream gummies donut jujubes"),
                Arguments.of("Should handle one substitution", "fafaewff ${ctx:tid} eawfawefweafwe", "fafaewff req-43tnfwenb eawfawefweafwe"),
                Arguments.of("Should handle substitution with no matching MDC", "fafaewff ${ctx:tenant} eawfawefweafwe", "fafaewff  eawfawefweafwe"),
                Arguments.of("Should handle many substitutions", "fooba${ctx:tenant}r${ctx:baz}_42", "foobarbaz_42"),
                Arguments.of("Should use default values if no matching MDC", "tid-${ctx:request-id:-empty}_${ctx:tid}", "tid-empty_req-43tnfwenb")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void shouldParseEmptyString(String displayName, String pattern, String expected) {
        // Given
        Map<String, String> mdcMap = Map.of(
                "tidd", "req-23142rwfnw",
                "tid", "req-43tnfwenb",
                "baz", "baz"
        );

        MutableLogEvent event = new MutableLogEvent();
        event.setContextData(new JdkMapAdapterStringMap(mdcMap));

        LabelSerializer labelSerializer = LabelSerializers.threadLocal().getFirst();

        // When
        LabelPrinter printer = LabelPrinterFactory.parse(pattern);

        labelSerializer.startAppendingLabelValue();
        printer.append(event, labelSerializer::appendPartialLabelValue);
        labelSerializer.finishAppendingLabelValue();

        // Then
        assertThat(labelSerializer.toString()).isEqualTo(expected);
    }
}
