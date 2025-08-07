package pl.tkowalcz.tjahzi;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.awaitility.Durations;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.tkowalcz.tjahzi.http.ClientConfiguration;
import pl.tkowalcz.tjahzi.http.HttpClientFactory;
import pl.tkowalcz.tjahzi.http.NettyHttpClient;
import pl.tkowalcz.tjahzi.stats.StandardMonitoringModule;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.awaitility.Awaitility.await;

class HeadersTest {

    private WireMockServer wireMockServer;

    private TjahziInitializer initializer;
    private LoggingSystem loggingSystem;
    private StandardMonitoringModule monitoringModule;

    private LabelSerializer labelSerializer;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(
                wireMockConfig()
                        .dynamicPort()
                        .dynamicHttpsPort()
        );

        wireMockServer.start();

        initializer = new TjahziInitializer();
        monitoringModule = new StandardMonitoringModule();

        labelSerializer = LabelSerializerCreator.from("level", "warn");
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
                        monitoringModule,
                        "X-Scope-OrgID", "Circus",
                        "C", "Control"
                );

        loggingSystem = initializer.createLoggingSystem(
                httpClient,
                monitoringModule,
                Map.of(),
                0,
                0,
                1024 * 1024,
                250,
                10_000,
                false
        );
        loggingSystem.start();

        // When
        TjahziLogger logger = loggingSystem.createLogger();
        logger.log(
                System.currentTimeMillis(),
                0,
                labelSerializer,
                new LabelSerializer(),
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
                        clientConfiguration,
                        monitoringModule
                );

        loggingSystem = initializer.createLoggingSystem(
                httpClient,
                monitoringModule,
                Map.of(),
                0,
                0,
                1024 * 1024,
                250,
                10_000,
                false
        );
        loggingSystem.start();

        // WHen
        TjahziLogger logger = loggingSystem.createLogger();
        logger.log(
                System.currentTimeMillis(),
                0,
                labelSerializer,
                new LabelSerializer(),
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
                        monitoringModule,
                        "content-type", "text/plain",
                        "content-length", "5232423423",
                        "host", "remote"
                );

        loggingSystem = initializer.createLoggingSystem(
                httpClient,
                monitoringModule,
                Map.of(),
                0,
                0,
                1024 * 1024,
                250,
                10_000,
                false
        );
        loggingSystem.start();

        // When
        TjahziLogger logger = loggingSystem.createLogger();
        logger.log(
                System.currentTimeMillis(),
                0,
                labelSerializer,
                new LabelSerializer(),
                ByteBuffer.wrap("Test".getBytes())
        );

        // Then
        await()
                .atMost(Durations.FIVE_SECONDS)
                .untilAsserted(() ->
                        wireMockServer.verify(
                                postRequestedFor(urlMatching("/loki/api/v1/push"))
                                        .withHeader("content-type", matching("application/x-protobuf"))
                                        .withHeader("content-length", matching("[4-5][0-9]"))
                                        .withHeader("host", matching("localhost"))
                        ));
    }
}
