package pl.tkowalcz.tjahzi.log4j2;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class BufferSizeConfigurationTest {

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
    void shouldSendData() throws URISyntaxException {
        // Given
        System.setProperty("loki.host", loki.getHost());
        System.setProperty("loki.port", loki.getFirstMappedPort().toString());

        URI uri = getClass()
                .getClassLoader()
                .getResource("appender-test-with-custom-settings-log4j2-configuration.xml")
                .toURI();

        // When
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        context.setConfigLocation(uri);

        // Then
        LokiAppender loki = context.getConfiguration().getAppender("Loki");
        assertThat(loki.getLoggingSystem().getLogBufferSize()).isEqualTo(64 * 1024 * 1024);
    }
}
