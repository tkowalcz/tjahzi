package pl.tkowalcz.tjahzi.log4j2;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.tkowalcz.tjahzi.stats.DropwizardMonitoringModule;

import java.net.URI;
import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Testcontainers
class LokiAppenderMonitoringTest {

    @Test
    void shouldInjectMonitoringAndUseIt() throws URISyntaxException {
        // Given
        URI uri = getClass()
                .getClassLoader()
                .getResource("basic-appender-test-configuration.xml")
                .toURI();

        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        context.setConfigLocation(uri);

        LokiAppender loki = context.getConfiguration().getAppender("Loki");
        MetricRegistry metricRegistry = new MetricRegistry();
        loki.setMonitoringModule(
                new DropwizardMonitoringModule(
                        metricRegistry,
                        "appender.loki"
                )
        );

        // When
        Logger logger = LogManager.getLogger(LokiAppenderMonitoringTest.class);
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
