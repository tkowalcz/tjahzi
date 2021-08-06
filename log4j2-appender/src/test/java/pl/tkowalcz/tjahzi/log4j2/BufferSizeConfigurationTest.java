package pl.tkowalcz.tjahzi.log4j2;

import org.apache.logging.log4j.core.LoggerContext;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.tkowalcz.tjahzi.log4j2.infra.IntegrationTest.loadConfig;

class BufferSizeConfigurationTest {

    @Test
    void shouldUseDefaultWhenLogBufferSizeIsNotSetViaConfiguration() throws URISyntaxException {
        // When
        LoggerContext context = loadConfig("appender-test-with-buffer-size-megabytes-unset.xml");

        // Then
        LokiAppender loki = context.getConfiguration().getAppender("Loki");
        assertThat(loki.getLoggingSystem().getLogBufferSize()).isEqualTo(32 * 1024 * 1024);
        assertThat(loki.getLoggingSystem().getLogBufferByteRemainsSize() == 0);
    }

    @Test
    void shouldSetLogBufferSizeViaConfiguration() throws URISyntaxException {
        // When
        LoggerContext context = loadConfig("appender-test-with-buffer-size-megabytes-set-to-64.xml");

        // Then
        LokiAppender loki = context.getConfiguration().getAppender("Loki");
        assertThat(loki.getLoggingSystem().getLogBufferSize()).isEqualTo(64 * 1024 * 1024);
        assertThat(loki.getLoggingSystem().getLogBufferByteRemainsSize() == 0);
    }
}
