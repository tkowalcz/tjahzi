package pl.tkowalcz.tjahzi;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.parsing.Parser;
import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.tkowalcz.tjahzi.http.ClientConfiguration;
import pl.tkowalcz.tjahzi.http.HttpClientFactory;
import pl.tkowalcz.tjahzi.http.NettyHttpClient;
import pl.tkowalcz.tjahzi.stats.StandardMonitoringModule;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;

@Testcontainers
class LoggingSystemTest {

    @Container
    public GenericContainer loki = new GenericContainer("grafana/loki:3.1.0")
            .withCommand("-config.file=/etc/loki-config.yaml")
            .withClasspathResourceMapping("loki-config.yaml",
                    "/etc/loki-config.yaml",
                    BindMode.READ_ONLY)
            .waitingFor(
                    Wait.forHttp("/ready")
                            .forPort(3100)
            )
            .withExposedPorts(3100);

    private final StandardMonitoringModule monitoringModule = new StandardMonitoringModule();
    private LoggingSystem loggingSystem;

    @BeforeEach
    void setUp() {
        ClientConfiguration clientConfiguration = ClientConfiguration.builder()
                .withConnectionTimeoutMillis(10_000)
                .withHost(loki.getHost())
                .withHost("localhost")
                .withPort(loki.getFirstMappedPort())
                .withMaxRetries(1)
                .build();

        NettyHttpClient httpClient = HttpClientFactory.defaultFactory()
                .getHttpClient(
                        clientConfiguration,
                        monitoringModule
                );

        TjahziInitializer initializer = new TjahziInitializer();
        loggingSystem = initializer.createLoggingSystem(
                httpClient,
                monitoringModule,
                Map.of("version", "0.43", "server", "127.0.0.1"),
                1024 * 1024,
                0,
                0,
                250,
                10_000,
                false
        );

        loggingSystem.start();
    }

    @AfterEach
    void tearDown() {
        if (loggingSystem != null) {
            loggingSystem.close(
                    (int) TimeUnit.SECONDS.toMillis(10),
                    System.out::println
            );
        }
    }

    @Test
    void sendData() {
        // Given
        long timestamp = System.currentTimeMillis();

        // When
        TjahziLogger logger = loggingSystem.createLogger();
        for (int i = 0; i < 1000; i++) {
            logger.log(
                    timestamp + i,
                    9974,
                    LabelSerializerCreator.from(
                            Map.of("level", "warn")
                    ),
                    ByteBuffer.wrap(("Test" + i).getBytes())
            );
        }

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
                            .statusCode(200)
                            .log()
                            .all()
                            .body("status", equalTo("success"))
                            .body("data.result.size()", equalTo(1))
                            .body("data.result[0].stream.server", equalTo("127.0.0.1"))
                            .body("data.result[0].stream.version", equalTo("0.43"))
                            .body("data.result[0].stream.level", equalTo("warn"))
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
                                    long expectedTimestamp = (timestamp + index) * 1000_000 + 9974;

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
