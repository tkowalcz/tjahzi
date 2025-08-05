package pl.tkowalcz.tjahzi.reload4j;

import org.apache.log4j.Logger;
import org.junit.jupiter.api.Test;
import pl.tkowalcz.tjahzi.reload4j.infra.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static pl.tkowalcz.tjahzi.reload4j.infra.LokiAssert.assertThat;

class LokiAppenderMonitoringTest extends IntegrationTest {

    @Test
    void shouldSendLogsSuccessfullyWithMonitoringEnabled() {
        // Given
        loadConfig("basic-appender-test-configuration.xml");
        Logger logger = Logger.getLogger(LokiAppenderMonitoringTest.class);

        String expectedLogLine = "Test message for monitoring";

        // When
        logger.info(expectedLogLine);

        // Then - verify the log was sent to Loki successfully
        assertThat(loki)
                .returns(response -> response
                        .body("data.result.size()", equalTo(1))
                        .body("data.result[0].stream.logger", equalTo("pl.tkowalcz.tjahzi.reload4j.LokiAppenderMonitoringTest"))
                        .body(
                                "data.result.values",
                                org.hamcrest.Matchers.hasItems(
                                        org.hamcrest.Matchers.hasItems(
                                                org.hamcrest.Matchers.hasItems(
                                                        containsString("INFO  pl.tkowalcz.tjahzi.reload4j.LokiAppenderMonitoringTest - " + expectedLogLine)
                                                )
                                        )
                                )
                        )
                );
    }

    @Test
    void shouldHandleLoggingWithoutMonitoringModule() {
        // Given
        loadConfig("basic-appender-test-configuration.xml");
        Logger logger = Logger.getLogger(LokiAppenderMonitoringTest.class);

        String expectedLogLine = "Test message without monitoring";

        // When - log without setting up monitoring
        logger.warn(expectedLogLine);

        // Then - verify the log was still sent successfully
        assertThat(loki)
                .returns(response -> response
                        .body("data.result.size()", equalTo(1))
                        .body("data.result[0].stream.level", equalTo("WARN"))
                        .body(
                                "data.result.values",
                                org.hamcrest.Matchers.hasItems(
                                        org.hamcrest.Matchers.hasItems(
                                                org.hamcrest.Matchers.hasItems(
                                                        containsString("WARN  pl.tkowalcz.tjahzi.reload4j.LokiAppenderMonitoringTest - " + expectedLogLine)
                                                )
                                        )
                                )
                        )
                );
    }
}
