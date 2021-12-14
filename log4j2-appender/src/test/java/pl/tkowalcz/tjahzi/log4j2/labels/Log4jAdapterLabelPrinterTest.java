package pl.tkowalcz.tjahzi.log4j2.labels;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.NullConfiguration;
import org.apache.logging.log4j.core.impl.JdkMapAdapterStringMap;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.pattern.PatternFormatter;
import org.apache.logging.log4j.core.pattern.PatternParser;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class Log4jAdapterLabelPrinterTest {

    @Test
    void shouldHandleEmptyListOfFormatters() {
        // Given
        Log4jAdapterLabelPrinter printer = new Log4jAdapterLabelPrinter(List.of());

        MutableLogEvent event = new MutableLogEvent();
        StringBuilder builder = new StringBuilder();

        // When
        printer.append(event, builder::append);

        // Then
        assertThat(builder).isEmpty();
    }

    @Test
    void shouldInvokeFormatters() {
        // Given
        PatternParser patternParser = new PatternParser(null, PatternLayout.KEY, null);
        List<PatternFormatter> patternFormatters = patternParser.parse("%mdc{tid} %5p %c{1} - %m");

        Log4jAdapterLabelPrinter printer = new Log4jAdapterLabelPrinter(patternFormatters);

        Map<String, String> mdcMap = Map.of(
                "tid", "req-43tnfwenb"
        );

        MutableLogEvent event = new MutableLogEvent();
        event.setContextData(new JdkMapAdapterStringMap(mdcMap));
        event.setLoggerName(getClass().getName());
        event.setMessage(new SimpleMessage("Test"));
        event.setLevel(Level.ERROR);
        event.setLoggerFqcn(getClass().getCanonicalName());

        StringBuilder builder = new StringBuilder();

        // When
        printer.append(event, builder::append);

        // Then
        assertThat(builder.toString()).isEqualTo("req-43tnfwenb ERROR Log4jAdapterLabelPrinterTest - Test");
    }

    @Test
    void shouldSubstituteEnvVariables() {
        // Given
        System.setProperty("server", "127.0.0.1");

        PatternParser patternParser = new PatternParser(
                new NullConfiguration(),
                PatternLayout.KEY,
                null
        );
        List<PatternFormatter> patternFormatters = patternParser.parse("${sys:server} %5p %c{1} - %m");

        Log4jAdapterLabelPrinter printer = new Log4jAdapterLabelPrinter(patternFormatters);

        MutableLogEvent event = new MutableLogEvent();
        event.setLoggerName(getClass().getName());
        event.setMessage(new SimpleMessage("Test"));
        event.setLevel(Level.ERROR);
        event.setLoggerFqcn(getClass().getCanonicalName());

        StringBuilder builder = new StringBuilder();

        // When
        printer.append(event, builder::append);

        // Then
        assertThat(builder.toString()).isEqualTo("127.0.0.1 ERROR Log4jAdapterLabelPrinterTest - Test");
    }

    private static Stream<Arguments> shouldCorrectlyIdentifyStaticPattern() {
        return Stream.of(
                Arguments.of("Empty", "", true),
                Arguments.of("Literal value", "literal value", true),
                Arguments.of("Literal value with variable", "Literal & variable: ${server}", false),
                Arguments.of("Contains pattern", "With pattern %c{1}", true) // Since 2.15.0 no interpolation possible
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void shouldCorrectlyIdentifyStaticPattern(String name, String pattern, boolean isStatic) {
        // Given
        PatternParser patternParser = new PatternParser(
                new NullConfiguration(),
                PatternLayout.KEY,
                null
        );

        List<PatternFormatter> patternFormatters = patternParser.parse(pattern);
        Log4jAdapterLabelPrinter labelPrinter = Log4jAdapterLabelPrinter.of(patternFormatters);

        // When
        boolean actual = labelPrinter.isStatic();

        // Then
        assertThat(actual).isEqualTo(isStatic);
    }
}
