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

class MdcLogLabelConfigurationTest extends IntegrationTest {

    @Test
    void shouldSendMdcLogLabelAsConfigured() {
        // Given
        loadConfig("appender-test-with-md-log-label-set.xml");
        Logger logger = LogManager.getLogger(MdcLogLabelConfigurationTest.class);

        // When
        String tid = UUID.randomUUID().toString();
        ThreadContext.put("tid", tid);

        logger.info("Test");

        // Then
        assertThat(loki)
                .returns(response -> response
                        .body("data.result.size()", greaterThanOrEqualTo(1))
                        .body("data.result[0].stream.server", equalTo("127.0.0.1"))
                        .body("data.result[0].stream.tid1", equalTo(tid))
                        .body("data.result[0].stream.tid2", equalTo(tid))
                        .body("data.result[0].stream.tid3", equalTo(tid))
                );
    }
}
