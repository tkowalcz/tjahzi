package pl.tkowalcz.tjahzi.logback;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import pl.tkowalcz.tjahzi.logback.util.TestUtil;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class BufferSizeConfigurationTest {

    @Test
    void shouldUseDefaultWhenLogBufferSizeIsNotSetViaConfiguration() throws Exception {
        // Given
        URI uri = getClass()
                .getClassLoader()
                .getResource("appender-test-with-buffer-size-megabytes-unset.xml")
                .toURI();

        // When
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);

        context.reset();
        configurator.doConfigure(uri.toURL());

        // Then
        LokiAppender loki = TestUtil.getLokiAppender(context);
        assertThat(loki.getLoggingSystem().getLogBufferSize()).isEqualTo(32 * 1024 * 1024);
    }

    @Test
    void shouldSetLogBufferSizeViaConfiguration() throws Exception {
        // Given
        URI uri = getClass()
                .getClassLoader()
                .getResource("appender-test-with-buffer-size-megabytes-set-to-64.xml")
                .toURI();

        // When
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);

        context.reset();
        configurator.doConfigure(uri.toURL());

        // Then
        LokiAppender loki = TestUtil.getLokiAppender(context);
        assertThat(loki.getLoggingSystem().getLogBufferSize()).isEqualTo(64 * 1024 * 1024);
    }
}
