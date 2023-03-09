package pl.tkowalcz.tjahzi.logback;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.junit.jupiter.api.Test;
import pl.tkowalcz.tjahzi.logback.infra.IntegrationTest;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static pl.tkowalcz.tjahzi.logback.infra.LokiAssert.assertThat;

class LogLevelLabelConfigurationTest extends IntegrationTest {

    @Test
    void shouldWorkWithNoLogLevelConfigured() {
        // Given
        LoggerContext context = loadConfig("appender-test-with-log-label-unset.xml");

        // When
        Logger logger = context.getLogger(LogLevelLabelConfigurationTest.class);
        logger.info("Test");

        // Then
        assertThat(loki)
                .withFormParam("query=%7Btest%3D%22shouldWorkWithNoLogLevelConfigured%22%7D")
                .returns(response -> response
                        .body("status", equalTo("success"))
                        .body("data.result.size()", equalTo(1))
                        .body("data.result[0].stream.log_level", nullValue())
                );
    }

    @Test
    void shouldSendLogLevelAsConfigured() {
        // Given
        LoggerContext context = loadConfig("appender-test-with-log-label-set.xml");

        // When
        String originalThreadName=Thread.currentThread().getName();
        Thread.currentThread().setName("appender-test-with-log-label-set.xml");
        Logger logger = context.getLogger(LogLevelLabelConfigurationTest.class);
        logger.info("Test");
        Thread.currentThread().setName(originalThreadName);

        // Then
        assertThat(loki)
                .withFormParam("query=%7Btest%3D%22shouldSendLogLevelAsConfigured%22%7D")
                .returns(response -> response
                        .body("status", equalTo("success"))
                        .body("data.result.size()", equalTo(1))
                        .body("data.result[0].stream.log_level", equalTo("INFO"))
                        .body("data.result[0].stream.logger_name", equalTo("pl.tkowalcz.tjahzi.logback.LogLevelLabelConfigurationTest"))
                        .body("data.result[0].stream.thread_name", equalTo("appender-test-with-log-label-set.xml"))
                );
    }
}
