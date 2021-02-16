package pl.tkowalcz.tjahzi.log4j2;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.parsing.Parser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;

@Testcontainers
class LokiAppenderLargeBatchesTestTest {

    @Container
    public GenericContainer loki = new GenericContainer("grafana/loki:latest")
            .withCommand("-config.file=/etc/loki-config.yaml")
            .withClasspathResourceMapping("loki-config.yaml",
                    "/etc/loki-config.yaml",
                    BindMode.READ_ONLY)
            .waitingFor(
                    Wait.forHttp("/ready")
                            .forPort(3100)
            )
            .withExposedPorts(3100);

    @Test
    void shouldSendData() throws URISyntaxException {
        // Given
        System.setProperty("loki.host", loki.getHost());
        System.setProperty("loki.port", loki.getFirstMappedPort().toString());

        URI uri = getClass()
                .getClassLoader()
                .getResource("appender-test-large-batches.xml")
                .toURI();

        ((org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false))
                .setConfigLocation(uri);

        String expectedLogLine = "Cupcake ipsum dolor sit amet cake wafer. " +
                "Souffle jelly beans biscuit topping. " +
                "Danish bonbon gummies powder caramels. " +
                "Danish jelly beans sweet roll topping jelly beans oat cake toffee. " +
                "Chocolate cake sesame snaps brownie biscuit cheesecake. " +
                "Ice cream dessert sweet donut marshmallow. " +
                "Muffin bear claw cookie jelly-o sugar plum jelly beans apple pie fruitcake cookie. " +
                "Tootsie roll carrot cake pastry jujubes jelly beans chupa chups. " +
                "Souffle cake muffin liquorice tart souffle pie sesame snaps.";

        long expectedTimestamp = System.currentTimeMillis();
        Logger logger = LogManager.getLogger(LokiAppenderLargeBatchesTestTest.class);

        // When
        for (int i = 0; i < 1000; i++) {
            logger.info(i + " " + expectedLogLine);
        }

        // Then
        RestAssured.port = loki.getFirstMappedPort();
        RestAssured.baseURI = "http://" + loki.getHost();
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
                            .formParam("&start=" + expectedTimestamp + "&limit=1000&query=%7Bserver%3D%22127.0.0.1%22%7D")
                            .when()
                            .get("/loki/api/v1/query_range")
                            .then()
                            .log()
                            .all()
                            .statusCode(200)
                            .body("status", equalTo("success"))
                            .body("data.result.size()", equalTo(1))
                            .body("data.result[0].stream.server", equalTo("127.0.0.1"))
                            .body("data.result[0].values.size()", equalTo(1000))
                            .body("data.result[0].values", hasItems(new BaseMatcher<>() {
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

                            ));
                });
    }
}
