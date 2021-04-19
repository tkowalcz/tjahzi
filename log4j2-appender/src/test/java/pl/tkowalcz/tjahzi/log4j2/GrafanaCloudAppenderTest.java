package pl.tkowalcz.tjahzi.log4j2;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.parsing.Parser;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;

class GrafanaCloudAppenderTest {

    @Test
    void shouldSendData() throws Exception {
        // Given
        System.setProperty("loki.host", "logs-prod-us-central1.grafana.net");
        System.setProperty("loki.port", "443");

        URI uri = getClass()
                .getClassLoader()
                .getResource("grafana-cloud-appender-test-configuration.xml")
                .toURI();

        ((org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false))
                .setConfigLocation(uri);

        String expectedLogLine = RandomStringUtils.randomAlphabetic(42);
        long expectedTimestamp = System.currentTimeMillis();

        // When
        Logger logger = LogManager.getLogger(GrafanaCloudAppenderTest.class);
        logger.info(expectedLogLine);

        // Then
        RestAssured.registerParser("text/plain", Parser.JSON);

        Awaitility
                .await()
                .atMost(Durations.ONE_MINUTE)
                .pollInterval(Durations.ONE_SECOND)
                .ignoreExceptions()
                .untilAsserted(() -> {
                    given()
                            .contentType(ContentType.URLENC)
                            .urlEncodingEnabled(false)
                            .formParam("query=%7Bserver%3D%22127.0.0.1%22%7D")
                            .auth()
                            .preemptive()
                            .basic(
                                    System.getenv("grafana_username"),
                                    System.getenv("grafana_password")
                            )
                            .when()
                            .get("https://logs-prod-us-central1.grafana.net/loki/api/v1/query_range")
                            .then()
                            .log()
                            .all()
                            .statusCode(200)
                            .body("status", equalTo("success"))
                            .body("data.result.size()", equalTo(1))
                            .body("data.result[0].stream.server", equalTo("127.0.0.1"))
                            .body("data.result[0].values", hasItems(
                                    new BaseMatcher<>() {
                                        @Override
                                        public boolean matches(Object o) {
                                            List<Object> list = (List<Object>) o;
                                            if (list.size() != 2) {
                                                return false;
                                            }

                                            long actualTimestamp = Long.parseLong(list.get(0).toString());
                                            String actualLogLine = list.get(1).toString();

                                            return actualLogLine.contains(expectedLogLine)
                                                    && (expectedTimestamp - actualTimestamp) < TimeUnit.MINUTES.toMillis(1);
                                        }

                                        @Override
                                        public void describeTo(Description description) {

                                        }
                                    }

                                    )
                            );
                });
    }
}
