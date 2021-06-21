package pl.tkowalcz.tjahzi.log4j2.labels;

import org.apache.logging.log4j.core.impl.JdkMapAdapterStringMap;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class MDCLookupTest {

    private static Stream<Arguments> shouldLookUpItsValueInContext() {
        return Stream.of(
                Arguments.of("Should replace with context value", Map.of("tenant", "Kang"), "David", "Kang"),
                Arguments.of("Should replace with context value (default is null)", Map.of("tenant", "Kang"), null, "Kang"),
                Arguments.of("Should replace with context value even if it is an empty string", Map.of("tenant", ""), "David", ""),
                Arguments.of("Should replace with context value even if it is an empty string (default is null)", Map.of("tenant", ""), null, ""),
                Arguments.of("Should replace with default value", Map.of(), "David", "David"),
                Arguments.of("Should replace with default value even if it si null", Map.of(), null, "")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void shouldLookUpItsValueInContext(
            String displayName,
            Map<String, String> mdcMap,
            String defaultValue,
            String expected
    ) {
        // Given
        MutableLogEvent event = new MutableLogEvent();
        event.setContextData(new JdkMapAdapterStringMap(mdcMap));

        MDCLookup lookup = MDCLookup.of("tenant", defaultValue);
        StringBuilder actual = new StringBuilder();

        // When
        lookup.append(event, actual::append);

        // Then
        assertThat(actual.toString()).isEqualTo(expected);
    }
}
