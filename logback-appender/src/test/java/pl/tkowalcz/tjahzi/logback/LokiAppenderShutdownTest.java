package pl.tkowalcz.tjahzi.logback;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import pl.tkowalcz.tjahzi.logback.infra.IntegrationTest;

import static org.hamcrest.CoreMatchers.*;
import static pl.tkowalcz.tjahzi.logback.infra.LokiAssert.assertThat;

class LokiAppenderShutdownTest extends IntegrationTest {

    @Test
    void shouldNotLooseDataWhenShuttingDown() {
        // Given
        LoggerContext context = loadConfig("appender-test-shutdown.xml");
        Logger logger = context.getLogger(LokiAppenderShutdownTest.class);

        String expectedLogLine = "Test";

        // When
        logger.info(expectedLogLine);
        ((LoggerContext) LoggerFactory.getILoggerFactory()).stop();

        // Then
        assertThat(loki)
                .returns(response ->
                        response
                                .body("data.result.size()", equalTo(1))
                                .body("data.result[0].stream.server", equalTo("127.0.0.1"))
                                .body("data.result[0].values.size()", equalTo(1))
                                .body(
                                        "data.result.values",
                                        hasItems(
                                                hasItems(
                                                        hasItems(
                                                                containsString("INFO  p.t.t.l.LokiAppenderShutdownTest - Test")
                                                        )
                                                )
                                        )
                                )
                );
    }
}
