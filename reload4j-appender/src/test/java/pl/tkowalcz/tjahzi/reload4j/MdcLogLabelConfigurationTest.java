package pl.tkowalcz.tjahzi.reload4j;

import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static pl.tkowalcz.tjahzi.reload4j.infra.LokiAssert.assertThat;

class MdcLogLabelConfigurationTest extends pl.tkowalcz.tjahzi.reload4j.infra.IntegrationTest {

    @Test
    void shouldSendMdcLogLabelAsConfigured() {
        // Given
        loadConfig("appender-test-with-md-log-label-set.xml");
        Logger logger = Logger.getLogger(MdcLogLabelConfigurationTest.class);

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
                );
    }
}
