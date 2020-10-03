package pl.tkowalcz.thjazi;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.parsing.Parser;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.tkowalcz.thjazi.http.ClientConfiguration;
import pl.tkowalcz.thjazi.http.HttpClientFactory;
import pl.tkowalcz.thjazi.http.NettyHttpClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
            .withExposedPorts(3100);
//            .withLogConsumer((Consumer<OutputFrame>) outputFrame -> {
//                        if (outputFrame.getBytes() != null) {
//                            System.out.print(new String(outputFrame.getBytes()));
//                        }
//                    }
//            );

//    public static void main(String[] args) throws IOException {
//        ClientConfiguration clientConfiguration = ClientConfiguration.builder()
//                .withConnectionTimeoutMillis(10_000)
//                .withHost("localhost")
//                .withPort(3100)
//                .withMaxRetries(1)
//                .build();
//
//        NettyHttpClient httpClient = HttpClientFactory.defaultFactory()
//                .getHttpClient(clientConfiguration);
//
//        httpClient.log(
//                System.currentTimeMillis(),
//                "Test"
//        );
//    }

    @Test
    void sendData() throws IOException {
        ThjaziInitializer initializer = new ThjaziInitializer();
        LoggingSystem loggingSystem = initializer.createLoggingSystem(
                1024 * 1024,
                false
        );

        ThjaziLogger logger = loggingSystem.createLogger();
        logger.log(
                System.currentTimeMillis(),
                new HashMap<>(),
                "Test"
        );

        ClientConfiguration clientConfiguration = ClientConfiguration.builder()
                .withConnectionTimeoutMillis(10_000)
                .withHost(loki.getHost())
                .withPort(loki.getFirstMappedPort())
                .withMaxRetries(1)
                .build();

        NettyHttpClient httpClient = HttpClientFactory.defaultFactory()
                .getHttpClient(clientConfiguration);

        long timestamp = System.currentTimeMillis();

        httpClient.log(
                timestamp,
                Map.of("foo", "bar2", "app", "lokki"),
                "Test"
        );

        RestAssured.port = loki.getFirstMappedPort();
        RestAssured.baseURI = "http://" + loki.getHost();
        RestAssured.registerParser("text/plain", Parser.JSON);

//        String response = given()
        given()
                .contentType(ContentType.URLENC)
                .urlEncodingEnabled(false)
                .formParam("query=%7Bfoo%3D%22bar2%22%7D")
                .when()
                .get("/loki/api/v1/query_range")
                .then()
//                .extract().asString();
//
//        Map<Object, Object> list = JsonPath.from(response)
//                .getMap("data.result[0]");
//
//        System.out.println("list = " + list);
                .log()
                .all()
                .statusCode(200)
                .body("status", equalTo("success"))
                .body("data.result.size()", equalTo(1))
                .body("data.result[0].stream.foo", equalTo("bar2"))
                .body("data.result[0].values[0]", hasItems("" + (timestamp * 1000_000), "Test"));
    }
}
