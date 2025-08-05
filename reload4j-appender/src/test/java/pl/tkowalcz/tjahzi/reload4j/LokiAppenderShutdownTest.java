package pl.tkowalcz.tjahzi.reload4j;

import org.apache.log4j.Logger;
import org.junit.jupiter.api.Test;
import pl.tkowalcz.tjahzi.reload4j.infra.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static pl.tkowalcz.tjahzi.reload4j.infra.LokiAssert.assertThat;

class LokiAppenderShutdownTest extends IntegrationTest {

    @Test
    void shouldNotLooseDataWhenShuttingDown() {
        // Given
        loadConfig("appender-test-shutdown.xml");
        Logger logger = Logger.getLogger(LokiAppenderShutdownTest.class);

        String expectedLogLine = "Test";

        // When
        logger.info(expectedLogLine);
        Logger.getRootLogger().getLoggerRepository().shutdown();

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
                                                                containsString("INFO  pl.tkowalcz.tjahzi.reload4j.LokiAppenderShutdownTest - Test")
                                                        )
                                                )
                                        )
                                )
                );
    }
}
