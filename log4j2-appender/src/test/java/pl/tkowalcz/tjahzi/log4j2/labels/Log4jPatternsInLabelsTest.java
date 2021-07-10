package pl.tkowalcz.tjahzi.log4j2.labels;

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
class Log4jPatternsInLabelsTest {

    @Container
    public GenericContainer loki = new GenericContainer("grafana/loki:latest")
            .withCommand("-config.file=/etc/loki-config.yaml")
            .withClasspathResourceMapping(
                    "loki-config.yaml",
                    "/etc/loki-config.yaml",
                    BindMode.READ_ONLY
            )
            .waitingFor(
                    Wait
                            .forHttp("/ready")
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
                .getResource("log4j-patterns-in-labels-configuration.xml")
                .toURI();

        ((org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false))
                .setConfigLocation(uri);

        Logger logger = LogManager.getLogger(Log4jPatternsInLabelsTest.class);

        // When
        MDC.put("tid", "req-230rq9ubou");
        logger.info("Test1");

        MDC.clear();
        logger.info("Test2");

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
                            .body("data.result.size()", equalTo(2))
                            .body("data.result.stream.server", everyItem(equalTo("127.0.0.1")))
                            .body("data.result.stream.class_pattern", everyItem(equalTo("p.t.t.l.l.Log4jPatternsInLabelsTest")))
                            .body("data.result.stream.sequence_number", contains("2", "1"))
                            .body("data.result.stream.mdc_tid", contains("", "req-230rq9ubou"))
                            .body("data.result.values",
                                    hasItems(
                                            hasItems(hasItems("Log4jPatternsInLabelsTest - Test2")),
                                            hasItems(hasItems("Log4jPatternsInLabelsTest - Test1"))
                                    )
                            );
                });
    }
}
