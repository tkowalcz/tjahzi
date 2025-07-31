package pl.tkowalcz.tjahzi.logback;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.status.Status;
import org.awaitility.Durations;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import pl.tkowalcz.tjahzi.logback.infra.IntegrationTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class LoggingMonitoringModuleIntegrationTest extends IntegrationTest {

    @Test
    void shouldLogVerboseWhenEnabled() {
        // Given
        LoggerContext context = loadConfig("appender-test-with-log-internal-errors.xml");
        Logger logger = (Logger) LoggerFactory.getLogger(LoggingMonitoringModuleIntegrationTest.class);

        // When
        logger.info("Test message");

        // Then
        context.getStatusManager().getCopyOfStatusList().forEach(System.out::println);

        await().atMost(Durations.FOREVER)
                .untilAsserted(() -> {
                    List<Status> statuses = context.getStatusManager().getCopyOfStatusList();

                    assertThat(statuses)
                            .extracting("message")
                            .contains("[Tjahzi] HTTP request failed.");
                });
    }
}
