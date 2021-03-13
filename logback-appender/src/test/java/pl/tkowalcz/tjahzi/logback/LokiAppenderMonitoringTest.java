package pl.tkowalcz.tjahzi.logback;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.tkowalcz.tjahzi.logback.util.TestUtil;
import pl.tkowalcz.tjahzi.stats.DropwizardMonitoringModule;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Testcontainers
class LokiAppenderMonitoringTest {

    @Test
    void shouldInjectMonitoringAndUseIt() throws Exception {
        // Given
        System.setProperty("loki.host", "somewhere");
        System.setProperty("loki.port", "42");

        URI uri = getClass()
                .getClassLoader()
                .getResource("basic-appender-test-configuration.xml")
                .toURI();

        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);

        context.reset();
        configurator.doConfigure(uri.toURL());

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
