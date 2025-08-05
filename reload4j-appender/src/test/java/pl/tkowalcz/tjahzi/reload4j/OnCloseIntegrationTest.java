package pl.tkowalcz.tjahzi.reload4j;

import org.apache.log4j.Logger;
import org.junit.jupiter.api.Test;
import pl.tkowalcz.tjahzi.reload4j.infra.IntegrationTest;
import pl.tkowalcz.tjahzi.stats.MonitoringModule;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OnCloseIntegrationTest extends IntegrationTest {

    @Test
    void shouldCallOnCloseWhenAppenderIsStopped() {
        // Given
        loadConfig("basic-appender-test-configuration.xml");
        LokiAppender appender = (LokiAppender) Logger.getRootLogger().getAppender("loki");

        MonitoringModule monitoringModule = mock(MonitoringModule.class);
        appender.setMonitoringModule(monitoringModule);

        // When
        appender.close();

        // Then
        verify(monitoringModule).onClose();
    }
}
