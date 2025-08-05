package pl.tkowalcz.tjahzi.stats;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class LoggingMonitoringModuleTest {

    @Test
    void shouldLogVerbose() {
        // Given
        StringBuilder actual = new StringBuilder();
        LoggingMonitoringModule module = new LoggingMonitoringModule(actual::append);

        // When
        module.addAgentError(new RuntimeException("Test agent error"));
        module.addPipelineError(new RuntimeException("Test pipeline error"));
        module.incrementDroppedPuts(new RuntimeException("Test dropped puts error"));
        module.incrementHttpErrors(500, "Internal Server Error");
        module.incrementFailedHttpRequests();
        module.incrementChannelInactive();

        // Then
        assertThat(actual).contains(
                "Test agent error",
                "Test pipeline error",
                "Test dropped puts error",
                "Internal Server Error"
        );
    }

    @Test
    void shouldInheritStandardMonitoringFunctionality() {
        // Given
        LoggingMonitoringModule module = new LoggingMonitoringModule(new StringBuilder()::append);

        // When
        module.incrementSentHttpRequests(1024);
        module.incrementRetriedHttpRequests();
        module.incrementHttpConnectAttempts();
        module.incrementHttpResponses();

        // Then
        assertThat(module.getSentHttpRequests()).isOne();
        assertThat(module.getRetriedHttpRequests()).isOne();
        assertThat(module.getHttpConnectAttempts()).isOne();
        assertThat(module.getHttpResponses()).isOne();
    }

    @Test
    void shouldLogOnClose() {
        // Given
        StringBuilder actual = new StringBuilder();
        LoggingMonitoringModule module = new LoggingMonitoringModule(actual::append);

        // When
        assertDoesNotThrow(module::onClose);

        // Then
        assertThat(actual).contains("[Tjahzi] Appender is being closed");
    }
}
