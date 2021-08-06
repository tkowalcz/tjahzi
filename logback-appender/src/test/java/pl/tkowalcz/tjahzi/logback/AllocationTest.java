package pl.tkowalcz.tjahzi.logback;

import ch.qos.logback.classic.LoggerContext;
import com.google.monitoring.runtime.instrumentation.AllocationRecorder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.testcontainers.shaded.com.google.common.util.concurrent.Uninterruptibles;
import pl.tkowalcz.tjahzi.logback.infra.IntegrationTest;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class AllocationTest extends IntegrationTest {

    @Test
    @Disabled
    void shouldSendData() {
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
        LoggerContext context = loadConfig("basic-appender-test-configuration.xml");

        String logLine = "Cupcake ipsum dolor sit amet cake wafer. " +
                "Souffle jelly beans biscuit topping. " +
                "Danish bonbon gummies powder caramels. ";

        Logger logger = context.getLogger(AllocationTest.class);

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

            if (desc.contains("LoggingEvent")) {
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
