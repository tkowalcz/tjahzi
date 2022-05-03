package pl.tkowalcz.tjahzi.logback;

import ch.qos.logback.classic.LoggerContext;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.tkowalcz.tjahzi.logback.infra.IntegrationTest;

import static org.hamcrest.CoreMatchers.*;
import static pl.tkowalcz.tjahzi.logback.infra.LokiAssert.assertThat;

class LokiAppenderUrlConnectionTest extends IntegrationTest {

    @Test
    void shouldSendData() {
        // Given
        LoggerContext context = loadConfig("appender-test-url-configuration.xml");
        Logger logger = context.getLogger(LokiAppenderUrlConnectionTest.class);

        String expectedLogLine = "Hello World";

        // When
        logger.info(expectedLogLine);

        // Then
        assertThat(loki)
                .returns(response -> response
                        .body("data.result.size()", equalTo(1))
                        .body("data.result[0].stream.server", equalTo("127.0.0.1"))
                        .body(
                                "data.result.values",
                                hasItems(
                                        hasItems(
                                                hasItems(
                                                        containsString("p.t.tjahzi.logback.LokiAppenderTest - " + expectedLogLine)
                                                )
                                        )
                                )
                        )
                );
    }
}
