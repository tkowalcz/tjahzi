package pl.tkowalcz.tjahzi.log4j2;

import org.apache.logging.log4j.core.LoggerContext;
import org.junit.jupiter.api.Test;
import pl.tkowalcz.tjahzi.log4j2.infra.IntegrationTest;
import pl.tkowalcz.tjahzi.stats.MonitoringModule;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OnCloseIntegrationTest extends IntegrationTest {

    @Test
    void shouldCallOnCloseWhenAppenderIsStopped() {
        // Given
        LoggerContext context = loadConfig("basic-appender-test-configuration.xml");

        LokiAppender appender = context.getConfiguration().getAppender("Loki");

        MonitoringModule monitoringModule = mock(MonitoringModule.class);
        appender.setMonitoringModule(monitoringModule);

        // When
        appender.stop();

        // Then
        verify(monitoringModule).onClose();
    }
}
