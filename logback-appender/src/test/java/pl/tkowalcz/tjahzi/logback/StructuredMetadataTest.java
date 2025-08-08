package pl.tkowalcz.tjahzi.logback;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import pl.tkowalcz.tjahzi.logback.infra.IntegrationTest;

import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static pl.tkowalcz.tjahzi.logback.infra.LokiAssert.assertThat;

class StructuredMetadataTest extends IntegrationTest {

    @Test
    void shouldSendStructuredMetadataAlongLabels() {
        // Given
        LoggerContext context = loadConfig("appender-test-with-structured-metadata.xml");
        Logger logger = context.getLogger(LokiAppenderLargeBatchesTest.class);

        // When
        String traceIdValue = UUID.randomUUID().toString();
        String spanIdValue = UUID.randomUUID().toString();

        MDC.put("trace_id", traceIdValue);
        MDC.put("span_id", spanIdValue);

        logger.info("Test");

        // Then
        assertThat(loki)
                .returns(response -> response
                        .body("data.result.size()", greaterThanOrEqualTo(1))
                        .body("data.result[0].stream.server", equalTo("127.0.0.1"))
                        .body("data.result[0].stream.trace_id", equalTo(traceIdValue))
                        .body("data.result[0].stream.span_id", equalTo(spanIdValue))

                        .body("data.result[0].stream.fixed", equalTo("stuff"))
                        .body("data.result[0].stream.thread", equalTo("123"))
                );
    }
}
