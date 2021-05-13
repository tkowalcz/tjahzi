package pl.tkowalcz.tjahzi.log4j2;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.parsing.Parser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.core.Every.everyItem;

@Testcontainers
class LabelsContextSubstitutionTest {

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
    void shouldSendData() throws Exception {
        // Given
        System.setProperty("loki.host", loki.getHost());
        System.setProperty("loki.port", loki.getFirstMappedPort().toString());

        URI uri = getClass()
                .getClassLoader()
                .getResource("labels-context-substitution-test-configuration.xml")
                .toURI();

        ((org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false))
                .setConfigLocation(uri);

        Logger logger = LogManager.getLogger(LokiAppenderTest.class);

        // When
        MDC.put("object", "bust_ticket");
        MDC.put("owner", "wally");
        logger.info("Test1");

        MDC.put("object", "comb");
        MDC.put("owner", "jennifer");
        logger.info("Test2");

        MDC.clear();
        logger.info("Test3");

        // Then
        RestAssured.port = loki.getFirstMappedPort();
        RestAssured.baseURI = "http://" + loki.getHost();
        RestAssured.registerParser("text/plain", Parser.JSON);

        Awaitility
                .await()
                .atMost(Durations.TEN_SECONDS)
                .pollInterval(Durations.ONE_SECOND)
                .ignoreExceptions()
                .untilAsserted(() -> {
                    given()
                            .contentType(ContentType.URLENC)
                            .urlEncodingEnabled(false)
                            .formParam("query=%7Bserver%3D%22127.0.0.1%22%7D")
                            .when()
                            .get("/loki/api/v1/query_range")
                            .then()
                            .log()
                            .all()
                            .statusCode(200)
                            .body("status", equalTo("success"))
                            .body("data.result.size()", equalTo(3))
                            .body("data.result.stream.server", everyItem(equalTo("127.0.0.1")))
                            .body("data.result.stream.object", contains("prefix_${ctx:object}", "prefix_bust_ticket", "prefix_comb"))
                            .body("data.result.stream.owner", contains("${ctx:owner}_suffix", "wally_suffix", "jennifer_suffix"))
                            .body("data.result.stream.default_value_test", contains("use_this_if_missing", "use_this_if_missing", "use_this_if_missing"))
                            .body("data.result.values",
                                    hasItems(
                                            hasItems(hasItems(("LokiAppenderTest - Test3"))),
                                            hasItems(hasItems(("LokiAppenderTest - Test1"))),
                                            hasItems(hasItems(("LokiAppenderTest - Test2")))
                                    )
                            );
                });
    }
}
