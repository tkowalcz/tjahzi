package pl.tkowalcz.tjahzi.logback;

import ch.qos.logback.classic.LoggerContext;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import pl.tkowalcz.tjahzi.logback.infra.HttpsNginxIntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static pl.tkowalcz.tjahzi.logback.infra.LokiAssert.assertThat;

class LokiAppenderHttpsCustomEndpointTest extends HttpsNginxIntegrationTest {

    @Test
    void shouldSendData() {
        // Given
        LoggerContext context = loadConfig("appender-test-custom-endpoint-configuration.xml");
        Logger logger = context.getLogger(LokiAppenderHttpsCustomEndpointTest.class);

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
                                hasItem(
                                        hasItems(
                                                hasItems(
                                                        containsString("p.t.t.l.LokiAppenderHttpsCustomEndpointTest - " + expectedLogLine)
                                                )
                                        )
                                )
                        )
                );
    }
}
