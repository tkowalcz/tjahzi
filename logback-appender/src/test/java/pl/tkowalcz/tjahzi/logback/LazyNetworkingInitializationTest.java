package pl.tkowalcz.tjahzi.logback;

import ch.qos.logback.classic.LoggerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.tkowalcz.tjahzi.logback.infra.TestUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.tkowalcz.tjahzi.logback.infra.IntegrationTest.loadConfig;

class LazyNetworkingInitializationTest {

    @BeforeEach
    void setUp() {
        System.setProperty("loki.host", "localhost");
        System.setProperty("loki.port", "42390");
    }

    @Test
    void shouldNotInitializeNetworkingUntilFirstAppend() {
        // When - configuration loads and the appender starts. This is the phase
        // that runs inside SLF4J initialization in real applications, so no
        // Netty (and hence no SLF4J re-entrant logging) may happen here.
        LoggerContext context = loadConfig("appender-test-with-buffer-size-megabytes-unset.xml");
        LokiAppender loki = TestUtil.getLokiAppender(context);

        // Then
        assertThat(loki.isStarted()).isTrue();
        assertThat(loki.peekLoggingSystem()).isNull();

        // When - the first log event flows through the appender
        context.getLogger(LazyNetworkingInitializationTest.class).info("first message");

        // Then
        assertThat(loki.peekLoggingSystem()).isNotNull();

        context.stop();
    }

    @Test
    void shouldInitializeNetworkingWhenLoggingSystemIsAccessedDirectly() {
        // Given
        LoggerContext context = loadConfig("appender-test-with-buffer-size-megabytes-unset.xml");
        LokiAppender loki = TestUtil.getLokiAppender(context);

        // When - tests and monitoring hooks access the logging system directly
        // without any event having been logged
        long bufferSize = loki.getLoggingSystem().getLogBufferSize();

        // Then
        assertThat(bufferSize).isEqualTo(32 * 1024 * 1024);

        context.stop();
    }

    @Test
    void shouldStopCleanlyWhenNoEventWasEverLogged() {
        // Given
        LoggerContext context = loadConfig("appender-test-with-buffer-size-megabytes-unset.xml");
        LokiAppender loki = TestUtil.getLokiAppender(context);

        // When - context stops before any append; must not throw and must not
        // boot the networking stack just to shut it down
        context.stop();

        // Then
        assertThat(loki.peekLoggingSystem()).isNull();
    }
}
