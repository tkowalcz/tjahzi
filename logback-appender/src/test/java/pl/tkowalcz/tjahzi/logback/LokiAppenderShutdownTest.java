package pl.tkowalcz.tjahzi.logback;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.parsing.Parser;
import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

@Testcontainers
class LokiAppenderShutdownTest {

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
    void shouldNotLooseDataWhenShuttingDown() throws Exception {
        // Given
        System.setProperty("loki.host", loki.getHost());
        System.setProperty("loki.port", loki.getFirstMappedPort().toString());

        URI uri = getClass()
                .getClassLoader()
                .getResource("appender-test-shutdown.xml")
                .toURI();

        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);

        context.reset();
        configurator.doConfigure(uri.toURL());

        String expectedLogLine = "Test";

        long expectedTimestamp = System.currentTimeMillis();
        Logger logger = context.getLogger(LokiAppenderReloadTest.class);

        // When
        logger.info(expectedLogLine);
        context.stop();

        // Then
        RestAssured.port = loki.getFirstMappedPort();
        RestAssured.baseURI = "http://" + loki.getHost();
        RestAssured.registerParser("text/plain", Parser.JSON);

        Awaitility
                .await()
                .atMost(Durations.ONE_MINUTE)
                .pollInterval(Durations.ONE_SECOND)
                .ignoreExceptions()
                .untilAsserted(() -> given()
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
