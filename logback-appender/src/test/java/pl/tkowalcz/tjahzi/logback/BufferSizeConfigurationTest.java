package pl.tkowalcz.tjahzi.logback;

import ch.qos.logback.classic.LoggerContext;
import org.junit.jupiter.api.Test;
import pl.tkowalcz.tjahzi.logback.infra.TestUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.tkowalcz.tjahzi.logback.infra.IntegrationTest.loadConfig;

class BufferSizeConfigurationTest {

    @Test
    void shouldUseDefaultWhenLogBufferSizeIsNotSetViaConfiguration() throws Exception {
        // When
        LoggerContext context = loadConfig("appender-test-with-buffer-size-megabytes-unset.xml");

        // Then
        LokiAppender loki = TestUtil.getLokiAppender(context);
        assertThat(loki.getLoggingSystem().getLogBufferSize()).isEqualTo(32 * 1024 * 1024);
    }

    @Test
    void shouldSetLogBufferSizeViaConfiguration() throws Exception {
        // When
        LoggerContext context = loadConfig("appender-test-with-buffer-size-megabytes-set-to-64.xml");

        // Then
        LokiAppender loki = TestUtil.getLokiAppender(context);
        assertThat(loki.getLoggingSystem().getLogBufferSize()).isEqualTo(64 * 1024 * 1024);
    }
}
