package pl.tkowalcz.tjahzi.reload4j;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LokiAppenderThrowableTest {

    @Test
    void shouldAppendStackTraceWhenLayoutIgnoresThrowable() {
        // Given
        LokiAppender appender = new LokiAppender();
        appender.setLayout(new PatternLayout("%m"));

        LoggingEvent event = new LoggingEvent(
                Logger.class.getName(),
                Logger.getLogger(LokiAppenderThrowableTest.class),
                Level.ERROR,
                "boom",
                new RuntimeException("Kaboom!")
        );

        // When
        String logLine = appender.formatLogLine(event);

        // Then
        assertThat(logLine).startsWith("boom");
        assertThat(logLine).contains("java.lang.RuntimeException: Kaboom!");
        assertThat(logLine).contains("at " + LokiAppenderThrowableTest.class.getName());
    }

    @Test
    void shouldNotAlterLogLineWhenThereIsNoThrowable() {
        // Given
        LokiAppender appender = new LokiAppender();
        appender.setLayout(new PatternLayout("%m"));

        LoggingEvent event = new LoggingEvent(
                Logger.class.getName(),
                Logger.getLogger(LokiAppenderThrowableTest.class),
                Level.INFO,
                "Hello World",
                null
        );

        // When
        String logLine = appender.formatLogLine(event);

        // Then
        assertThat(logLine).isEqualTo("Hello World");
    }
}
