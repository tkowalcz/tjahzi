package pl.tkowalcz.tjahzi;

import com.github.tomakehurst.wiremock.WireMockServer;
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
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

class ShutdownFlushTest {

    private WireMockServer wireMockServer;

    private TjahziInitializer initializer;
    private StandardMonitoringModule monitoringModule;

    private LabelSerializer labelSerializer;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(
                wireMockConfig().dynamicPort()
        );

        wireMockServer.start();

        initializer = new TjahziInitializer();
        monitoringModule = new StandardMonitoringModule();

        labelSerializer = LabelSerializerCreator.from("level", "warn");
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void shouldDeliverLastBatchBeforeCloseReturns() {
        // Given
        wireMockServer.stubFor(
                post(urlEqualTo("/loki/api/v1/push"))
                        .willReturn(
                                aResponse().withStatus(204)
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

        // Batch thresholds high enough that nothing is sent until shutdown -
        // the message below can only arrive through the close-time flush.
        LoggingSystem loggingSystem = initializer.createLoggingSystem(
                httpClient,
                monitoringModule,
                Map.of(),
                100 * 1024 * 1024,
                TimeUnit.HOURS.toMillis(1),
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
                new LabelSerializer(),
                ByteBuffer.wrap("Do not lose me".getBytes())
        );

        // When
        loggingSystem.close(
                (int) TimeUnit.SECONDS.toMillis(10),
                System.out::println
        );

        // Then - no awaiting: delivery must have been acknowledged before close returned
        wireMockServer.verify(
                1,
                postRequestedFor(urlMatching("/loki/api/v1/push"))
        );
    }
}
