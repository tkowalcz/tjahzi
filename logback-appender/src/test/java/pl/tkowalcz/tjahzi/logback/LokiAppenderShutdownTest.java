package pl.tkowalcz.tjahzi.logback;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class LokiAppenderShutdownTest {

    @Container
    public GenericContainer loki = new GenericContainer("grafana/loki:latest")
            .withCommand("-config.file=/etc/loki-config.yaml")
            .withClasspathResourceMapping("loki-config.yaml",
                    "/etc/loki-config.yaml",
                    BindMode.READ_ONLY)
            .waitingFor(
                    Wait.forHttp("/ready")
                            .forPort(3100)
            )
            .withExposedPorts(3100);

    @Test
    void shouldSendData() throws Exception {
        // Given
        System.setProperty("loki.host", loki.getHost());
        System.setProperty("loki.port", loki.getFirstMappedPort().toString());

        URI uri = getClass()
                .getClassLoader()
                .getResource("basic-appender-test-configuration.xml")
                .toURI();

        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);

        context.reset();
        configurator.doConfigure(uri.toURL());

        // Verify our assumptions that we can find threads started by Tjahzi
        Awaitility.await().untilAsserted(() -> {
            ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
            ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(false, false);

            assertThat(threadInfos)
                    .extracting(ThreadInfo::getThreadName)
                    .contains(
                            "ReadingLogBufferAndSendingHttp",
                            "tjahzi-worker"
                    );
        });

        // When
        context.stop();

        // Then
        Awaitility.await().untilAsserted(() -> {
            ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
            ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(false, false);

            assertThat(threadInfos)
                    .extracting(ThreadInfo::getThreadName)
                    .doesNotContain(
                            "ReadingLogBufferAndSendingHttp",
                            "tjahzi-worker"
                    );
        });
    }
}
