package pl.tkowalcz.tjahzi.log4j2;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.NginxContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.junit.jupiter.Container;
import pl.tkowalcz.tjahzi.log4j2.infra.IntegrationTest;
import pl.tkowalcz.tjahzi.log4j2.infra.LokiAssert;

import static org.hamcrest.CoreMatchers.*;

class LokiAppenderHttpsCustomEndpointTest extends IntegrationTest {

    @Container
    public NginxContainer<?> nginx = new NginxContainer<>("nginx:latest")
            .withNetwork(network)
            .withClasspathResourceMapping("loki.reverse.nginx.conf",
                    "/etc/nginx/conf.d/loki.reverse.conf",
                    BindMode.READ_ONLY
            )
            .withClasspathResourceMapping("nginx/nginx-selfsigned.crt",
                    "/etc/nginx/nginx-selfsigned.crt",
                    BindMode.READ_ONLY
            )
            .withClasspathResourceMapping("nginx/nginx-selfsigned.key",
                    "/etc/nginx/nginx-selfsigned.key",
                    BindMode.READ_ONLY
            )
            .withClasspathResourceMapping("nginx/passwords",
                    "/etc/nginx/passwords",
                    BindMode.READ_ONLY
            )
            .waitingFor(new HttpWaitStrategy())
            .withExposedPorts(81);

    @Override
    @BeforeEach
    public void setUp() {
        System.setProperty("loki.host", nginx.getHost());
        System.setProperty("loki.port", nginx.getFirstMappedPort().toString());
    }

    @Test
    void shouldSendData() {
        // Given
        loadConfig("appender-test-custom-endpoint-configuration.xml");
        Logger logger = LogManager.getLogger(LokiAppenderHttpsCustomEndpointTest.class);

        String expectedLogLine = "Hello World";

        // When
        logger.info(expectedLogLine);

        // Then
        LokiAssert.assertThat(loki)
                .returns(response -> response
                        .body("data.result.size()", equalTo(1))
                        .body("data.result[0].stream.server", equalTo("127.0.0.1"))
                        .body(
                                "data.result.values",
                                hasItems(
                                        hasItems(
                                                hasItems(
                                                        containsString("INFO LokiAppenderHttpsCustomEndpointTest - " + expectedLogLine)
                                                )
                                        )
                                )
                        )
                );
    }
}
