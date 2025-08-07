package pl.tkowalcz.tjahzi.log4j2;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.Test;
import pl.tkowalcz.tjahzi.log4j2.infra.IntegrationTest;

import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static pl.tkowalcz.tjahzi.log4j2.infra.LokiAssert.assertThat;

class StructuredMetadataTest extends IntegrationTest {

    @Test
    void shouldSendStructuredMetadataAlongLabels() {
        // Given
        loadConfig("appender-test-with-structured-metadata.xml");
        Logger logger = LogManager.getLogger(MdcLogLabelConfigurationTest.class);

        // When
        String tid = UUID.randomUUID().toString();
        String threadId = String.valueOf(Thread.currentThread().getId());

        ThreadContext.put("tx_id", tid);
        ThreadContext.put("thread_id", threadId);

        logger.info("Test");

        // Then
        assertThat(loki)
                .returns(response -> response
                        .body("data.result.size()", greaterThanOrEqualTo(1))
                        .body("data.result[0].stream.server", equalTo("127.0.0.1"))
                        .body("data.result[0].stream.tx_id_label", equalTo(tid))

                        .body("data.result[0].stream.fixed", equalTo("stuff"))
                        .body("data.result[0].stream.tx_id_metadata", equalTo(tid))
                        .body("data.result[0].stream.thread_id_metadata", equalTo(threadId))
                );
    }
}
