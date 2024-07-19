package pl.tkowalcz.tjahzi.log4j2;

import com.alibaba.dcm.DnsCacheManipulator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.NginxContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.tkowalcz.tjahzi.log4j2.infra.IntegrationTest;
import pl.tkowalcz.tjahzi.log4j2.infra.LokiAssert;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.hamcrest.CoreMatchers.*;

@SuppressWarnings({"rawtypes", "resource"})
@Testcontainers
public class ServerNameIndicatorTest {

    public static final String MARVEL_LOKI_ADDRESS = "marvel.com";
    public static final String DC_LOKI_ADDRESS = "dc.com";

    public static Network network = Network.newNetwork();

    @Container
    public GenericContainer loki1 = new GenericContainer("grafana/loki:3.1.0")
            .withNetwork(network)
            .withNetworkAliases("loki1")
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

    @Container
    public GenericContainer loki2 = new GenericContainer("grafana/loki:3.1.0")
            .withNetwork(network)
            .withNetworkAliases("loki2")
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


    @Container
    public NginxContainer<?> nginx = new NginxContainer<>("nginx:latest")
            .withNetwork(network)
            .dependsOn(loki1, loki2)
            .withClasspathResourceMapping("loki.sni.nginx.conf",
                    "/etc/nginx/conf.d/loki.sni.conf",
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

    @BeforeEach
    public void setUp() throws UnknownHostException {
        InetAddress inetAddress = InetAddress.getByName(nginx.getHost());

        DnsCacheManipulator.setDnsCache(MARVEL_LOKI_ADDRESS, inetAddress.getHostAddress());
        DnsCacheManipulator.setDnsCache(DC_LOKI_ADDRESS, inetAddress.getHostAddress());
    }

    @Test
    void shouldSendDataToTwoDifferentServersBasedOnSNI() {
        // Given
        setLokiConnectionProperties(MARVEL_LOKI_ADDRESS);
        Logger logger = reInitLogger();

        String expectedLogLine = "Hello Marvel!";

        // When
        logger.info(expectedLogLine);

        // Then
        assertLokiInstanceContains(loki1, expectedLogLine);

        // Given
        setLokiConnectionProperties(DC_LOKI_ADDRESS);
        logger = reInitLogger();

        expectedLogLine = "Hello DC!";

        // When
        logger.info(expectedLogLine);

        // Then
        assertLokiInstanceContains(loki2, expectedLogLine);
    }

    private void assertLokiInstanceContains(GenericContainer loki1, String expectedLogLine) {
        LokiAssert.assertThat(loki1)
                .returns(response -> response
                        .body("data.result.size()", equalTo(1))
                        .body("data.result[0].stream.server", equalTo("127.0.0.1"))
                        .body(
                                "data.result.values",
                                hasItems(
                                        hasItems(
                                                hasItems(
                                                        containsString("INFO ServerNameIndicatorTest - " + expectedLogLine)
                                                )
                                        )
                                )
                        )
                );
    }

    private static Logger reInitLogger() {
        IntegrationTest.loadConfig("appender-test-custom-endpoint-configuration.xml");
        return LogManager.getLogger(ServerNameIndicatorTest.class);
    }

    private void setLokiConnectionProperties(String marvelLokiAddress) {
        System.setProperty("loki.host", marvelLokiAddress);
        System.setProperty("loki.port", nginx.getFirstMappedPort().toString());
    }
}
