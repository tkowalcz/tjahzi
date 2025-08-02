package pl.tkowalcz.tjahzi.reload4j.infra;

import org.apache.log4j.LogManager;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.xml.DOMConfigurator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

@Testcontainers
public class IntegrationTest {

    public static Network network = Network.newNetwork();

    @Container
    public GenericContainer loki = new GenericContainer("grafana/loki:3.1.0")
            .withNetwork(network)
            .withNetworkAliases("loki")
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
    public void setUp() {
        System.setProperty("loki.host", loki.getHost());
        System.setProperty("loki.port", loki.getFirstMappedPort().toString());
    }

    public static void loadConfig(String fileName) {
        try {
            URL resource = IntegrationTest.class
                    .getClassLoader()
                    .getResource(fileName);

            if (resource == null) {
                Assertions.fail("Resource " + fileName + " not found");
            }

            URI uri = resource.toURI();
            loadConfig(uri);
        } catch (URISyntaxException e) {
            Assertions.fail(e);
        }
    }

    public static void loadConfig(URI uri) {
        try {
            LogManager.resetConfiguration();

            String path = uri.getPath();
            if (path.endsWith(".properties")) {
                PropertyConfigurator.configure(uri.toURL());
            } else if (path.endsWith(".xml")) {
                DOMConfigurator.configure(uri.toURL());
            } else {
                Assertions.fail("Unsupported configuration file format: " + path);
            }
        } catch (Exception e) {
            Assertions.fail("Failed to load configuration: " + e.getMessage(), e);
        }
    }
}
