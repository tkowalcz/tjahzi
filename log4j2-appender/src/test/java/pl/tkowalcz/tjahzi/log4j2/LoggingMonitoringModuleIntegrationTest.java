package pl.tkowalcz.tjahzi.log4j2;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.status.StatusListener;
import org.apache.logging.log4j.status.StatusLogger;
import org.awaitility.Durations;
import org.junit.jupiter.api.Test;
import pl.tkowalcz.tjahzi.log4j2.infra.IntegrationTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class LoggingMonitoringModuleIntegrationTest extends IntegrationTest {

    @Test
    void shouldLogVerboseWhenEnabled() {
        // Given
        loadConfig("appender-test-with-log-internal-errors.xml");

        TestStatusListener listener = new TestStatusListener();
        StatusLogger.getLogger().registerListener(listener);

        Logger logger = LogManager.getLogger(LoggingMonitoringModuleIntegrationTest.class);

        // When
        logger.info("Test message");

        // Then
        await()
                .atMost(Durations.TEN_SECONDS)
                .untilAsserted(() -> assertThat(listener.getMessages()).contains("[Tjahzi] HTTP request failed."));
    }

    private static class TestStatusListener implements StatusListener {

        private final List<String> messages = new ArrayList<>();

        @Override
        public void log(StatusData data) {
            messages.add(data.getMessage().getFormattedMessage());
        }

        @Override
        public Level getStatusLevel() {
            return Level.ALL;
        }

        @Override
        public void close() {
        }

        public List<String> getMessages() {
            return messages;
        }
    }
}
