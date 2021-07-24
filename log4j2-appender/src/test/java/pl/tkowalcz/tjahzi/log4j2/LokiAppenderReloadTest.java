package pl.tkowalcz.tjahzi.log4j2;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.parsing.Parser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

@Testcontainers
class LokiAppenderReloadTest {

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
    void shouldNotLooseDataWhenConfigChanges() throws Exception {
        // Given
        System.setProperty("loki.host", loki.getHost());
        System.setProperty("loki.port", loki.getFirstMappedPort().toString());

        URI uriBefore = getClass()
                .getClassLoader()
                .getResource("appender-test-reload-before.xml")
                .toURI();

        URI uriAfter = getClass()
                .getClassLoader()
                .getResource("appender-test-reload-after.xml")
                .toURI();

        URI uri = new URI("file://" + Paths.get(uriBefore).toFile().getParent() + "/appender-test-reload.xml");
        Files.copy(Paths.get(uriBefore), Paths.get(uri), StandardCopyOption.REPLACE_EXISTING);

        ((org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false))
                .setConfigLocation(uri);

        String expectedLogLine = "Test";

        long expectedTimestamp = System.currentTimeMillis();
        Logger logger = LogManager.getLogger(LokiAppenderReloadTest.class);

        // When
        logger.info(expectedLogLine);
        Files.copy(Paths.get(uriAfter), Paths.get(uri), StandardCopyOption.REPLACE_EXISTING);

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
                                                        containsString("INFO LokiAppenderReloadTest - Test")
                                                )
                                        )
                                )
                        )
                );
    }
}
