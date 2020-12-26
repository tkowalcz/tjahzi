package pl.tkowalcz.tjahzi;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.awaitility.Durations;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.tkowalcz.tjahzi.http.ClientConfiguration;
import pl.tkowalcz.tjahzi.http.HttpClientFactory;
import pl.tkowalcz.tjahzi.http.NettyHttpClient;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.awaitility.Awaitility.await;

class HeadersTest {

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

        wireMockServer.start();

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
        wireMockServer.stubFor(
                post(urlEqualTo("/loki/api/v1/push"))
                        .willReturn(
                                aResponse().withStatus(200)
                        ));

        ClientConfiguration clientConfiguration = ClientConfiguration.builder()
                .withConnectionTimeoutMillis(10_000)
                .withHost("localhost")
                .withPort(wireMockServer.port())
                .withMaxRetries(1)
                .build();

        NettyHttpClient httpClient = HttpClientFactory.defaultFactory()
                .getHttpClient(
                        clientConfiguration,
                        "X-Scope-OrgID", "Circus",
                        "C", "Control"
                );

        loggingSystem = initializer.createLoggingSystem(
                httpClient,
                1024 * 1024,
                false
        );

        // WHen
        TjahziLogger logger = loggingSystem.createLogger();
        logger.log(
                System.currentTimeMillis(),
                Map.of(),
                "level",
                "warn",
                ByteBuffer.wrap("Test".getBytes())
        );

        // Then
        await()
                .atMost(Durations.FIVE_SECONDS)
                .untilAsserted(() ->
                        wireMockServer.verify(
                                postRequestedFor(urlMatching("/loki/api/v1/push"))
                                        .withHeader("X-Scope-OrgID", equalTo("Circus"))
                        ));
    }

    @Test
    void shouldHandleCaseWithNoAdditionalHeaders() {
        // Given
        wireMockServer.stubFor(
                post(urlEqualTo("/loki/api/v1/push"))
                        .willReturn(
                                aResponse().withStatus(200)
                        ));

        ClientConfiguration clientConfiguration = ClientConfiguration.builder()
                .withConnectionTimeoutMillis(10_000)
                .withHost("localhost")
                .withPort(wireMockServer.port())
                .withMaxRetries(1)
                .build();

        NettyHttpClient httpClient = HttpClientFactory.defaultFactory()
                .getHttpClient(
                        clientConfiguration
                );

        loggingSystem = initializer.createLoggingSystem(
                httpClient,
                1024 * 1024,
                false
        );

        // WHen
        TjahziLogger logger = loggingSystem.createLogger();
        logger.log(
                System.currentTimeMillis(),
                Map.of(),
                "level",
                "warn",
                ByteBuffer.wrap("Test".getBytes())
        );

        // Then
        await()
                .atMost(Durations.FIVE_SECONDS)
                .untilAsserted(() ->
                        wireMockServer.verify(
                                postRequestedFor(urlMatching("/loki/api/v1/push"))
                        ));
    }

    @Test
    void shouldNotOverrideCrucialHeaders() {
        // Given
        wireMockServer.stubFor(
                post(urlEqualTo("/loki/api/v1/push"))
                        .willReturn(
                                aResponse().withStatus(200)
                        ));

        ClientConfiguration clientConfiguration = ClientConfiguration.builder()
                .withConnectionTimeoutMillis(10_000)
                .withHost("localhost")
                .withPort(wireMockServer.port())
                .withMaxRetries(1)
                .build();

        NettyHttpClient httpClient = HttpClientFactory.defaultFactory()
                .getHttpClient(
                        clientConfiguration,
                        "content-type", "text/plain",
                        "content-length", "5232423423",
                        "host", "remote"
                );

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
                "level",
                "warn",
                ByteBuffer.wrap("Test".getBytes())
        );

        // Then
        await()
                .atMost(Durations.FIVE_SECONDS)
                .untilAsserted(() ->
                        wireMockServer.verify(
                                postRequestedFor(urlMatching("/loki/api/v1/push"))
                                        .withHeader("content-type", matching("application/x-protobuf"))
                                        .withHeader("content-length", matching("42|43"))
                                        .withHeader("host", matching("localhost"))
                        ));
    }
}
