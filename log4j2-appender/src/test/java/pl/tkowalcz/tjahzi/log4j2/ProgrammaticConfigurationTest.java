package pl.tkowalcz.tjahzi.log4j2;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.parsing.Parser;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.greaterThan;

@Testcontainers
public class ProgrammaticConfigurationTest {

    @Container
    public GenericContainer loki = new GenericContainer("grafana/loki:latest")
            .withCommand("-config.file=/etc/loki-config.yaml")
            .withClasspathResourceMapping("loki-config.yaml",
                    "/etc/loki-config.yaml",
                    BindMode.READ_ONLY
            )
            .waitingFor(
                    Wait.forHttp("/ready")
                            .forPort(3100)
            )
            .withExposedPorts(3100);

    @Test
    void shouldInitializeAppenderProgrammatically() throws Exception {
        // Given
        System.setProperty("loki.host", loki.getHost());
        System.setProperty("loki.port", loki.getFirstMappedPort().toString());

        // When
        ConfigurationBuilder<BuiltConfiguration> builder = createConfiguration();
        Configurator.reconfigure(builder.build());

        // Then
        long expectedTimestamp = System.currentTimeMillis();

        Logger logger = LogManager.getLogger(ProgrammaticConfigurationTest.class);
        logger.warn("Here I come!");

        RestAssured.port = loki.getFirstMappedPort();
        RestAssured.baseURI = "http://" + loki.getHost();
        RestAssured.registerParser("text/plain", Parser.JSON);

        Awaitility
                .await()
                .atMost(Durations.TEN_SECONDS)
                .pollInterval(Durations.ONE_SECOND)
                .ignoreExceptions()
                .untilAsserted(
                        () -> given()
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
                                .body("data.result[0].values.size()", greaterThan(0))
                                .body(
                                        "data.result.values",
                                        hasItems(
                                                hasItems(
                                                        hasItems(
                                                                containsString("WARN ProgrammaticConfigurationTest - Here I come!")
                                                        )
                                                )
                                        )
                                )
                );
    }

    private ConfigurationBuilder<BuiltConfiguration> createConfiguration() {
        ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();

        builder.setConfigurationName("loki-programmatically");
        builder.setStatusLevel(Level.ALL);
        builder.setPackages("pl.tkowalcz.tjahzi.log4j2");

        builder.add(
                builder.newRootLogger(Level.ALL)
                        .add(builder.newAppenderRef("Loki"))
        );

        AppenderComponentBuilder lokiAppenderBuilder = builder.newAppender("Loki", "Loki")
                .addAttribute("host", "${sys:loki.host}")
                .addAttribute("port", "${sys:loki.port}")
                .add(
                        builder.newFilter("ThresholdFilter", Filter.Result.ACCEPT, Filter.Result.DENY)
                                .addAttribute("level", "ALL")
                )
                .add(
                        builder.newLayout("PatternLayout")
                                .addAttribute("pattern", "%X{tid} [%t] %d{MM-dd HH:mm:ss.SSS} %5p %c{1} - %m%n%exception{full}")
                )
                .addComponent(
                        builder.newComponent("Header")
                                .addAttribute("name", "server")
                                .addAttribute("value", "127.0.0.1")
                )
                .addComponent(
                        builder.newComponent("Label")
                                .addAttribute("name", "server")
                                .addAttribute("value", "127.0.0.1")
                );
        builder.add(lokiAppenderBuilder);

        return builder;
    }
}
