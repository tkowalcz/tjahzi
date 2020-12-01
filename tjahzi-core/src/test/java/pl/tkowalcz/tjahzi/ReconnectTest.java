package pl.tkowalcz.tjahzi;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.awaitility.Durations;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.tkowalcz.tjahzi.http.ClientConfiguration;
import pl.tkowalcz.tjahzi.http.HttpClientFactory;
import pl.tkowalcz.tjahzi.http.NettyHttpClient;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.awaitility.Awaitility.await;

public class ReconnectTest {

    private WireMockServer wireMockServer;

    private TjahziInitializer initializer;
    private LoggingSystem loggingSystem;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(
                wireMockConfig()
                        .dynamicPort()
                        .dynamicHttpsPort()
        );

//        wireMockServer.start();

        initializer = new TjahziInitializer();
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();

        if (loggingSystem != null) {
            loggingSystem.close(
                    (int) TimeUnit.SECONDS.toMillis(10),
                    System.out::println
            );

            loggingSystem = null;
        }
    }

    @Test
    void shouldIncludeAdditionalHeaders() {
        // Given
//        wireMockServer.stubFor(
//                get(urlEqualTo("/loki/api/v1/push"))
//                        .willReturn(
//                                aResponse().withStatus(200)
//                        ));

        ClientConfiguration clientConfiguration = ClientConfiguration.builder()
                .withConnectionTimeoutMillis(10_000)
                .withHost("localhost")
                .withPort(12322)
                .withMaxRetries(1)
                .build();

        NettyHttpClient httpClient = HttpClientFactory.defaultFactory()
                .getHttpClient(clientConfiguration);

        loggingSystem = initializer.createLoggingSystem(
                httpClient,
                1024 * 1024,
                false
        );

        // When
        TjahziLogger logger = loggingSystem.createLogger();
        logger.log(
                System.currentTimeMillis(),
                Map.of(),
                "Test"
        );

        // Then
        await()
                .atMost(Durations.TEN_MINUTES)
                .untilAsserted(() ->
                        wireMockServer.verify(
                                postRequestedFor(urlMatching("/loki/api/v1/push"))
                                        .withHeader("X-Scope-OrgID", equalTo("Circus"))
                        ));
    }
}
