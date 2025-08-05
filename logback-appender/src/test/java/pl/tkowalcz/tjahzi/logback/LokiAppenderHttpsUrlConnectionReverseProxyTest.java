package pl.tkowalcz.tjahzi.logback;

import ch.qos.logback.classic.LoggerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.NginxContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import pl.tkowalcz.tjahzi.logback.infra.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static pl.tkowalcz.tjahzi.logback.infra.LokiAssert.assertThat;

class LokiAppenderHttpsUrlConnectionReverseProxyTest extends IntegrationTest {

    @Container
    public NginxContainer<?> nginx = new NginxContainer<>("nginx:1.25")
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
            .waitingFor(Wait
                    .forHttp("/")
                    .forStatusCode(200)
                    .forStatusCode(400)
            )
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
        LoggerContext context = loadConfig("appender-test-url-configuration.xml");
        Logger logger = context.getLogger(LokiAppenderHttpsUrlConnectionReverseProxyTest.class);

        String expectedLogLine = "Hello World";

        // When
        logger.info(expectedLogLine);

        // Then
        assertThat(loki)
                .returns(response -> response
                        .body("data.result.size()", equalTo(1))
                        .body("data.result[0].stream.server", equalTo("127.0.0.1"))
                        .body(
                                "data.result.values",
                                hasItem(
                                        hasItems(
                                                hasItems(
                                                        containsString("p.t.t.l.LokiAppenderHttpsUrlConnectionReverseProxyTest - " + expectedLogLine)
                                                )
                                        )
                                )
                        )
                );
    }
}
