package pl.tkowalcz.tjahzi;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.tkowalcz.tjahzi.http.ClientConfiguration;
import pl.tkowalcz.tjahzi.http.HttpClientFactory;
import pl.tkowalcz.tjahzi.http.NettyHttpClient;
import pl.tkowalcz.tjahzi.stats.StandardMonitoringModule;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class ResourcesCleanupOnCloseTest {

    private WireMockServer wireMockServer;
    private TjahziInitializer initializer;
    private StandardMonitoringModule monitoringModule;
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
        monitoringModule = new StandardMonitoringModule();
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();

        if (loggingSystem != null) {
            loggingSystem.close(
                    (int) TimeUnit.SECONDS.toMillis(10),
                    System.out::println
            );
        }
    }

    @Test
    void shouldStopThreads() {
        // Given
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

        LoggingSystem loggingSystem = initializer.createLoggingSystem(
                httpClient,
                monitoringModule,
                Map.of(),
                0,
                0,
                1024 * 1024,
                250,
                10_000,
                false,
                false
        );
        loggingSystem.start();

        // Verify our assumptions that we can find threads started by Tjahzi
        Awaitility.await().untilAsserted(() -> {
            ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
            ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(false, false);

            assertThat(threadInfos)
                    .extracting(ThreadInfo::getThreadName)
                    .anyMatch(threadName -> threadName.contains("LogShipper"))
                    .anyMatch(threadName -> threadName.contains("tjahzi-worker"));
        });

        //When
        loggingSystem.close(
                (int) TimeUnit.SECONDS.toMillis(10),
                System.out::println
        );

        // Then
        Awaitility.await().untilAsserted(() -> {
            ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
            ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(false, false);

            assertThat(threadInfos)
                    .extracting(ThreadInfo::getThreadName)
                    .doesNotContain(
                            "LogShipper",
                            "tjahzi-worker"
                    );
        });
    }

    @Test
    void shouldStartThreadsAsDaemon() {
        // Given
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

        // When
        loggingSystem = initializer.createLoggingSystem(
                httpClient,
                monitoringModule,
                Map.of(),
                0,
                0,
                1024 * 1024,
                250,
                10_000,
                false,
                true
        );
        loggingSystem.start();

        // Then
        Awaitility.await().untilAsserted(() -> {
            ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
            ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(false, false);

            Optional<ThreadInfo> maybeLogShipper = Arrays.stream(threadInfos)
                    .filter(thread -> thread.getThreadName().contains("LogShipper"))
                    .findAny();

            Optional<ThreadInfo> maybeTjahziWorker = Arrays.stream(threadInfos)
                    .filter(thread -> thread.getThreadName().contains("tjahzi-worker"))
                    .findAny();

            assertThat(maybeLogShipper)
                    .hasValueSatisfying(ThreadInfo::isDaemon);

            assertThat(maybeTjahziWorker)
                    .hasValueSatisfying(ThreadInfo::isDaemon);
        });
    }
}
