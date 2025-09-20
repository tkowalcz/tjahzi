package pl.tkowalcz.tjahzi.reload4j.infra;

import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.NginxContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;

public class HttpsNginxIntegrationTest extends IntegrationTest {

    @Container
    @SuppressWarnings("resource")
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
    public void setUp() throws Exception {
        System.setProperty("loki.host", nginx.getHost());
        System.setProperty("loki.port", nginx.getFirstMappedPort().toString());
    }
}
