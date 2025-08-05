package pl.tkowalcz.tjahzi.logback;

import ch.qos.logback.classic.LoggerContext;
import org.junit.jupiter.api.Test;
import pl.tkowalcz.tjahzi.logback.infra.IntegrationTest;
import pl.tkowalcz.tjahzi.stats.MonitoringModule;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OnCloseIntegrationTest extends IntegrationTest {

    @Test
    void shouldCallOnCloseWhenAppenderIsStopped() {
        // Given
        LoggerContext context = loadConfig("basic-appender-test-configuration.xml");

        LokiAppender appender = (LokiAppender) context.getLogger("ROOT").getAppender("Loki");

        MonitoringModule monitoringModule = mock(MonitoringModule.class);
        appender.setMonitoringModule(monitoringModule);

        // When
        appender.stop();

        // Then
        verify(monitoringModule).onClose();
    }
}
