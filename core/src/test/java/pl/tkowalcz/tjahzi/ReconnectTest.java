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

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.awaitility.Awaitility.await;

public class ReconnectTest {

    private WireMockServer wireMockServer;

    private TjahziInitializer initializer;
    private LoggingSystem loggingSystem;
    private StandardMonitoringModule monitoringModule;

    private LabelSerializer labelSerializer;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(
                wireMockConfig()
                        .port(12322)
        );

        wireMockServer.stubFor(
                post(urlEqualTo("/loki/api/v1/push"))
                        .willReturn(
                                aResponse().withStatus(200)
                        ));

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
    void shouldEventuallyReconnectIfLokiWasDownWhenStarting() {
        // Given
        ClientConfiguration clientConfiguration = ClientConfiguration.builder()
                .withConnectionTimeoutMillis(10_000)
                .withHost("localhost")
                .withPort(12322)
                .withMaxRetries(10)
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
                10_000,
                1000,
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
                ByteBuffer.wrap("Test".getBytes())
        );

        wireMockServer.start();

        // Then
        await()
                .atMost(Durations.TEN_SECONDS)
                .untilAsserted(() ->
                        wireMockServer.verify(
                                postRequestedFor(urlMatching("/loki/api/v1/push"))
                        )
                );
    }

    @Test
    void shouldReconnectIfLokiFailed() {
        // Given
        ClientConfiguration clientConfiguration = ClientConfiguration.builder()
                .withConnectionTimeoutMillis(10_000)
                .withHost("localhost")
                .withPort(12322)
                .withMaxRetries(10)
                .build();

        NettyHttpClient httpClient = HttpClientFactory.defaultFactory()
                .getHttpClient(
                        clientConfiguration,
                        monitoringModule
                );

        wireMockServer.start();

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

        TjahziLogger logger = loggingSystem.createLogger();
        logger.log(
                System.currentTimeMillis(),
                0,
                labelSerializer,
                ByteBuffer.wrap("Test".getBytes())
        );

        await()
                .atMost(Durations.FIVE_SECONDS)
                .untilAsserted(() ->
                        wireMockServer.verify(
                                1,
                                postRequestedFor(urlMatching("/loki/api/v1/push"))
                        )
                );

        wireMockServer.stop();
        await().until(() -> !wireMockServer.isRunning());

        // When
        logger.log(
                System.currentTimeMillis(),
                0,
                labelSerializer,
                ByteBuffer.wrap("Test".getBytes())
        );

        wireMockServer.start();

        // Then
        await()
                .atMost(Durations.TEN_MINUTES)
                .untilAsserted(() ->
                        wireMockServer.verify(
                                1,
                                postRequestedFor(urlMatching("/loki/api/v1/push"))
                        )
                );
    }
}
