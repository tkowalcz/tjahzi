package pl.tkowalcz.tjahzi.logback.infra;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

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
            URI uri = IntegrationTest.class
                    .getClassLoader()
                    .getResource(fileName)
                    .toURI();

            return loadConfig(uri);
        } catch (URISyntaxException e) {
            Assertions.fail(e);
        }

        throw new IllegalStateException();
    }

    public static LoggerContext loadConfig(URI uri) {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        try {
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);

            context.reset();
            configurator.doConfigure(uri.toURL());
        } catch (MalformedURLException | JoranException e) {
            Assertions.fail(e);
        }

        return context;
    }
}
