package pl.tkowalcz.thjazi;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.parsing.Parser;
import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.tkowalcz.thjazi.http.ClientConfiguration;
import pl.tkowalcz.thjazi.http.HttpClientFactory;
import pl.tkowalcz.thjazi.http.NettyHttpClient;

import java.util.Map;
import java.util.function.Consumer;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;

@Testcontainers
class LoggingSystemSanityCheckTest {

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
            .withExposedPorts(3100)
            .withLogConsumer((Consumer<OutputFrame>) outputFrame -> {
                        if (outputFrame.getBytes() != null) {
                            System.out.print(new String(outputFrame.getBytes()));
                        }
                    }
            );

    @Test
    void sendData() {
        ClientConfiguration clientConfiguration = ClientConfiguration.builder()
                .withConnectionTimeoutMillis(10_000)
                .withHost(loki.getHost())
                .withPort(loki.getFirstMappedPort())
                .withMaxRetries(1)
                .build();

        NettyHttpClient httpClient = HttpClientFactory.defaultFactory()
                .getHttpClient(clientConfiguration);

        ThjaziInitializer initializer = new ThjaziInitializer();
        LoggingSystem loggingSystem = initializer.createLoggingSystem(
                httpClient,
                1024 * 1024,
                false
        );

        long timestamp = System.currentTimeMillis();

        ThjaziLogger logger = loggingSystem.createLogger();
        logger.log(
                timestamp,
                Map.of("version", "0.43", "server", "127.0.0.1"),
                "Test"
        );

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
                            .statusCode(200)
                            .body("status", equalTo("success"))
                            .body("data.result.size()", equalTo(1))
                            .body("data.result[0].stream.server", equalTo("127.0.0.1"))
                            .body("data.result[0].stream.version", equalTo("0.43"))
                            .body("data.result[0].values[0]", hasItems("" + (timestamp * 1000_000), "Test"));
                });
    }
}
