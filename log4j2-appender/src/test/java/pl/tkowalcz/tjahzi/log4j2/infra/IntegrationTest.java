package pl.tkowalcz.tjahzi.log4j2.infra;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

@Testcontainers
public class IntegrationTest {

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

    @BeforeEach
    void setUp() {
        System.setProperty("loki.host", loki.getHost());
        System.setProperty("loki.port", loki.getFirstMappedPort().toString());
    }

    public static LoggerContext loadConfig(String fileName) {
        try {
            URL resource = IntegrationTest.class
                    .getClassLoader()
                    .getResource(fileName);

            if (resource == null) {
                Assertions.fail("Resource " + fileName + " not found");
            }

            URI uri = resource
                    .toURI();

            return loadConfig(uri);
        } catch (URISyntaxException e) {
            Assertions.fail(e);
        }

        return null;
    }

    public static LoggerContext loadConfig(URI uri) {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        context.setConfigLocation(uri);

        return context;
    }
}
