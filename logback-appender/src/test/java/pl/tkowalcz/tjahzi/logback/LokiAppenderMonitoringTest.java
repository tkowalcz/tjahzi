package pl.tkowalcz.tjahzi.logback;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import org.junit.jupiter.api.Test;
import pl.tkowalcz.tjahzi.logback.infra.IntegrationTest;
import pl.tkowalcz.tjahzi.logback.infra.TestUtil;
import pl.tkowalcz.tjahzi.stats.DropwizardMonitoringModule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class LokiAppenderMonitoringTest {

    @Test
    void shouldInjectMonitoringAndUseIt() {
        // Given
        System.setProperty("loki.host", "somewhere");
        System.setProperty("loki.port", "42");

        LoggerContext context = IntegrationTest.loadConfig("basic-appender-test-configuration.xml");

        LokiAppender loki = TestUtil.getLokiAppender(context);

        MetricRegistry metricRegistry = new MetricRegistry();
        loki.setMonitoringModule(
                new DropwizardMonitoringModule(
                        metricRegistry,
                        "appender.loki"
                )
        );

        // When
        Logger logger = context.getLogger(LokiAppenderLargeBatchesTest.class);
        logger.info("Test test test");

        // Then
        assertThat(metricRegistry.getMetrics()).isNotEmpty();

        Counter connectAttemptsCounter = metricRegistry
                .getCounters()
                .get("appender.loki.httpConnectAttempts");
        assertThat(connectAttemptsCounter).isNotNull();

        await().untilAsserted(() -> {
                    assertThat(connectAttemptsCounter.getCount()).isEqualTo(1);
                }
        );
    }
}
