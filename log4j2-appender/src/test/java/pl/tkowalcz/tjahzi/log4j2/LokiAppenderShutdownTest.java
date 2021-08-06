package pl.tkowalcz.tjahzi.log4j2;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import pl.tkowalcz.tjahzi.log4j2.infra.IntegrationTest;

import static org.hamcrest.CoreMatchers.*;
import static pl.tkowalcz.tjahzi.log4j2.infra.LokiAssert.assertThat;

class LokiAppenderShutdownTest extends IntegrationTest {

    @Test
    void shouldNotLooseDataWhenShuttingDown() {
        // Given
        loadConfig("appender-test-shutdown.xml");
        Logger logger = LogManager.getLogger(LokiAppenderShutdownTest.class);

        String expectedLogLine = "Test";

        // When
        logger.info(expectedLogLine);
        LogManager.shutdown();

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
                                                                containsString("INFO LokiAppenderShutdownTest - Test")
                                                        )
                                                )
                                        )
                                )
                );
    }
}
