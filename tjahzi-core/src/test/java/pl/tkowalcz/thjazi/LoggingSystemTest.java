package pl.tkowalcz.tjahzi;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.parsing.Parser;
import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.tkowalcz.tjahzi.http.ClientConfiguration;
import pl.tkowalcz.tjahzi.http.HttpClientFactory;
import pl.tkowalcz.tjahzi.http.NettyHttpClient;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;

@Testcontainers
class LoggingSystemTest {

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

        TjahziInitializer initializer = new TjahziInitializer();
        LoggingSystem loggingSystem = initializer.createLoggingSystem(
                httpClient,
                1024 * 1024,
                false
        );

        long timestamp = System.currentTimeMillis();

        TjahziLogger logger = loggingSystem.createLogger();
        for (int i = 0; i < 1000; i++) {
            logger.log(
                    timestamp + i,
                    Map.of("version", "0.43", "server", "127.0.0.1"),
                    "Test" + i
            );
        }

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
                            .log()
                            .all()
                            .body("status", equalTo("success"))
                            .body("data.result.size()", equalTo(1))
                            .body("data.result[0].stream.server", equalTo("127.0.0.1"))
                            .body("data.result[0].stream.version", equalTo("0.43"))
                            .body("data.result[0].values", hasItems(new BaseMatcher<>() {

                                int index = 999;

                                @Override
                                public void describeTo(Description description) {

                                }

                                @Override
                                public boolean matches(Object o) {
                                    List<Object> list = (List<Object>) o;
                                    if (list.size() != 2) {
                                        return false;
                                    }

                                    long actualTimestamp = Long.parseLong(list.get(0).toString());
                                    long expectedTimestamp = (timestamp + index) * 1000_000;

                                    String actualLogLine = list.get(1).toString();
                                    Object expectedLogLine = "Test" + index;

                                    index--;

                                    return actualTimestamp == expectedTimestamp
                                            && actualLogLine.equals(expectedLogLine);
                                }
                            }));
                });
    }
}
