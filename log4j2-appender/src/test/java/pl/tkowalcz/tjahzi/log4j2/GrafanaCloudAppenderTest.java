package pl.tkowalcz.tjahzi.log4j2;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.*;
import static pl.tkowalcz.tjahzi.log4j2.infra.IntegrationTest.loadConfig;
import static pl.tkowalcz.tjahzi.log4j2.infra.LokiAssert.assertThat;

class GrafanaCloudAppenderTest {

    @Test
    void shouldSendData() {
        // Given
        System.setProperty("loki.host", "logs-prod-us-central1.grafana.net");
        System.setProperty("loki.port", "443");

        loadConfig("grafana-cloud-appender-test-configuration.xml");
        Logger logger = LogManager.getLogger(GrafanaCloudAppenderTest.class);

        String expectedLogLine = RandomStringUtils.randomAlphabetic(42);

        // When
        logger.info(expectedLogLine);

        // Then
        assertThat()
                .calling("https://logs-prod-us-central1.grafana.net/loki/api/v1/query_range")
                .withCredentials(
                        System.getenv("grafana_username"),
                        System.getenv("grafana_password")
                )
                .returns(response -> response
                        .body("data.result.size()", equalTo(1))
                        .body("data.result[0].stream.server", equalTo("127.0.0.1"))
                        .body(
                                "data.result.values",
                                hasItems(
                                        hasItems(
                                                hasItems(
                                                        containsString(expectedLogLine)
                                                )
                                        )
                                )
                        )
                );
    }
}
