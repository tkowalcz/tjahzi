package pl.tkowalcz.tjahzi.log4j2;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.assertThat;

class BufferSizeConfigurationTest {

    @Test
    void shouldUseDefaultWhenLogBufferSizeIsNotSetViaConfiguration() throws URISyntaxException {
        // Given
        URI uri = getClass()
                .getClassLoader()
                .getResource("appender-test-with-buffer-size-megabytes-unset.xml")
                .toURI();

        // When
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        context.setConfigLocation(uri);

        // Then
        LokiAppender loki = context.getConfiguration().getAppender("Loki");
        assertThat(loki.getLoggingSystem().getLogBufferSize()).isEqualTo(32 * 1024 * 1024);
    }

    @Test
    void shouldSetLogBufferSizeViaConfiguration() throws URISyntaxException {
        // Given
        URI uri = getClass()
                .getClassLoader()
                .getResource("appender-test-with-buffer-size-megabytes-set-to-64.xml")
                .toURI();

        // When
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        context.setConfigLocation(uri);

        // Then
        LokiAppender loki = context.getConfiguration().getAppender("Loki");
        assertThat(loki.getLoggingSystem().getLogBufferSize()).isEqualTo(64 * 1024 * 1024);
    }
}
