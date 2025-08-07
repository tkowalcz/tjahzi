package pl.tkowalcz.tjahzi.log4j2;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.impl.JdkMapAdapterStringMap;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import pl.tkowalcz.tjahzi.LabelSerializer;
import pl.tkowalcz.tjahzi.LabelSerializers;
import pl.tkowalcz.tjahzi.LoggingSystem;
import pl.tkowalcz.tjahzi.TjahziLogger;
import pl.tkowalcz.tjahzi.log4j2.labels.LabelPrinter;
import pl.tkowalcz.tjahzi.log4j2.labels.LabelSerializerAssert;
import pl.tkowalcz.tjahzi.log4j2.labels.MDCLookup;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;

import static org.mockito.Mockito.*;

class AppenderLogicTest {

    @Test
    void shouldWorkWithNoDynamicLabelsOrStructuredMetadata() {
        // Given
        TjahziLogger tjahziLogger = mock(TjahziLogger.class);

        LoggingSystem loggingSystem = mock(LoggingSystem.class);
        when(loggingSystem.createLogger()).thenReturn(tjahziLogger);

        long timestamp = 42L;
        String logLevelLabel = "log_level";
        Level logLevel = Level.INFO;

        AppenderLogic logic = new AppenderLogic(
                loggingSystem,
                logLevelLabel,
                Map.of(),
                Collections.emptyMap(),
                ByteBufferDestinationRepository.DEFAULT_MAX_LINE_SIZE_BYTES
        );

        MutableLogEvent logEvent = new MutableLogEvent();
        logEvent.setLevel(logLevel);
        logEvent.setLoggerName("foobar");
        logEvent.setTimeMillis(timestamp);
        logEvent.setContextData(new JdkMapAdapterStringMap(Map.of()));

        LabelSerializer labelSerializer = LabelSerializers.threadLocal().getFirst()
                .appendLabel(logLevelLabel, "INFO");

        // When
        logic.accept(logEvent, ByteBuffer.allocate(1024));

        // Then
        verify(tjahziLogger).log(
                eq(timestamp),
                eq(0L),
                eq(labelSerializer),
                any(),
                any()
        );
    }

    @Test
    void shouldPerformSubstitutionOnDynamicLabels() {
        // Given
        TjahziLogger tjahziLogger = mock(TjahziLogger.class);

        LoggingSystem loggingSystem = mock(LoggingSystem.class);
        when(loggingSystem.createLogger()).thenReturn(tjahziLogger);

        long timestamp = 42L;
        String logLevelLabel = "log_level";
        Level logLevel = Level.INFO;

        Map<String, LabelPrinter> dynamicLabels = Map.of(
                "id", MDCLookup.of("foo", ""),
                "tennant", MDCLookup.of("tennant", ""),
                "region", MDCLookup.of("region", "")
        );

        Map<String, String> contextMap = Map.of(
                "foo", "123",
                "tennant", "david",
                "region", "us_east_1"
        );

        Map<String, String> expected = Map.of(
                "id", "123",
                "tennant", "david",
                "region", "us_east_1"
        );

        AppenderLogic logic = new AppenderLogic(
                loggingSystem,
                logLevelLabel,
                dynamicLabels,
                Collections.emptyMap(),
                ByteBufferDestinationRepository.DEFAULT_MAX_LINE_SIZE_BYTES
        );

        MutableLogEvent logEvent = new MutableLogEvent();
        logEvent.setLevel(logLevel);
        logEvent.setLoggerName("foobar");
        logEvent.setTimeMillis(timestamp);
        logEvent.setContextData(new JdkMapAdapterStringMap(contextMap));

        // When
        logic.accept(logEvent, ByteBuffer.allocate(1024));

        // Then
        ArgumentCaptor<LabelSerializer> labelSerializerCaptor = ArgumentCaptor.forClass(LabelSerializer.class);
        verify(tjahziLogger).log(
                eq(timestamp),
                eq(0L),
                labelSerializerCaptor.capture(),
                any(),
                any()
        );

        LabelSerializerAssert.assertThat(labelSerializerCaptor.getValue())
                .hasLabelCount(4)
                .contains(expected);
    }

    @Test
    void shouldPerformSubstitutionOnStructuredMetadata() {
        // Given
        TjahziLogger tjahziLogger = mock(TjahziLogger.class);

        LoggingSystem loggingSystem = mock(LoggingSystem.class);
        when(loggingSystem.createLogger()).thenReturn(tjahziLogger);

        long timestamp = 42L;
        String logLevelLabel = "log_level";
        Level logLevel = Level.INFO;

        Map<String, LabelPrinter> dynamicLabels = Map.of(
                "id", MDCLookup.of("foo", "")
        );

        Map<String, LabelPrinter> structuredMetadata = Map.of(
                "tennant", MDCLookup.of("tennant", ""),
                "region", MDCLookup.of("region", "")
        );

        Map<String, String> contextMap = Map.of(
                "foo", "123",
                "tennant", "david",
                "region", "us_east_1"
        );

        Map<String, String> expected = Map.of(
                "tennant", "david",
                "region", "us_east_1"
        );

        AppenderLogic logic = new AppenderLogic(
                loggingSystem,
                logLevelLabel,
                dynamicLabels,
                structuredMetadata,
                ByteBufferDestinationRepository.DEFAULT_MAX_LINE_SIZE_BYTES
        );

        MutableLogEvent logEvent = new MutableLogEvent();
        logEvent.setLevel(logLevel);
        logEvent.setLoggerName("foobar");
        logEvent.setTimeMillis(timestamp);
        logEvent.setContextData(new JdkMapAdapterStringMap(contextMap));

        // When
        logic.accept(logEvent, ByteBuffer.allocate(1024));

        // Then
        ArgumentCaptor<LabelSerializer> labelSerializerCaptor = ArgumentCaptor.forClass(LabelSerializer.class);
        ArgumentCaptor<LabelSerializer> structuredMetadataCaptor = ArgumentCaptor.forClass(LabelSerializer.class);

        verify(tjahziLogger).log(
                eq(timestamp),
                eq(0L),
                labelSerializerCaptor.capture(),
                structuredMetadataCaptor.capture(),
                any()
        );

        LabelSerializerAssert.assertThat(labelSerializerCaptor.getValue())
                .hasLabelCount(2)
                .contains("id", "123");

        LabelSerializerAssert.assertThat(structuredMetadataCaptor.getValue())
                .hasLabelCount(2)
                .contains(expected);
    }
}
