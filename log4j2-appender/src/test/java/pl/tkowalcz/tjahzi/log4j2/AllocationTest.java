package pl.tkowalcz.tjahzi.log4j2;

import com.google.monitoring.runtime.instrumentation.AllocationRecorder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.google.common.util.concurrent.Uninterruptibles;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Testcontainers
public class AllocationTest {

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
    @Disabled
    void shouldSendData() throws URISyntaxException {
        AtomicLong totalAllocatedMemory = new AtomicLong();
        Map<String, AtomicLong> allocatedMemory = new HashMap<>();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            allocatedMemory
                    .entrySet()
                    .stream()
                    .sorted(Comparator.<Map.Entry<String, AtomicLong>>comparingLong(e -> e.getValue().get()).reversed())
                    .limit(30)
                    .forEach(System.out::println);

            System.out.println("totalAllocatedMemory = " + totalAllocatedMemory);
        }));

        // Given
        System.setProperty("loki.host", loki.getHost());
        System.setProperty("loki.port", loki.getFirstMappedPort().toString());

        URI uri = getClass()
                .getClassLoader()
                .getResource("basic-appender-test-log4j2-configuration.xml")
                .toURI();

        ((org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false))
                .setConfigLocation(uri);

        String logLine = "Cupcake ipsum dolor sit amet cake wafer. " +
                "Souffle jelly beans biscuit topping. " +
                "Danish bonbon gummies powder caramels. ";

        Logger logger = LogManager.getLogger(pl.tkowalcz.tjahzi.log4j2.LokiAppenderTest.class);

        // When
        System.out.println("Warmup...");
        for (int i = 0; i < 2000; i++) {
            logger.info(logLine);
            Uninterruptibles.sleepUninterruptibly(10, TimeUnit.MILLISECONDS);
        }

        AllocationRecorder.addSampler((count, desc, newObj, size) -> {
            if (desc.contains("AllocationTest")) {
                return;
            }

            if (desc.contains("instrumentation")) {
                return;
            }

            if (desc.contains("asm")) {
                return;
            }

            if (desc.contains("Object")) {
                return;
            }

            allocatedMemory.computeIfAbsent(desc, __ -> new AtomicLong()).addAndGet(size);
            totalAllocatedMemory.addAndGet(size);
        });

        System.out.println("Test...");
        for (int i = 0; i < 1000; i++) {
            logger.info(logLine);
            Uninterruptibles.sleepUninterruptibly(10, TimeUnit.MILLISECONDS);
        }

        // Then

    }
}
