package pl.tkowalcz.tjahzi.log4j2;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import pl.tkowalcz.tjahzi.log4j2.infra.IntegrationTest;
import pl.tkowalcz.tjahzi.log4j2.infra.LokiAssert;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;

class LogLineFragmentationTest extends IntegrationTest {

    @Test
    void shouldFragmentMessageOverLimit() {
        // Given
        loadConfig("appender-test-large-log-line-fragmentation.xml");
        Logger logger = LogManager.getLogger(LogLineFragmentationTest.class);

        String unfragmentedLogLine = RandomStringUtils.randomAlphanumeric(100 * 1024 - 1);
        String fragmentedLogLine = RandomStringUtils.randomAlphanumeric(110 * 1024);

        // When
        logger.info(unfragmentedLogLine);
        logger.info(fragmentedLogLine);

        // Then
        LokiAssert.assertThat(loki)
                .returns(response -> response
                        .body("data.result.size()", equalTo(1))
                        .body("data.result[0].stream.server", equalTo("127.0.0.1"))
                        .body("data.result[0].values.size()", equalTo(3))
                        .body(
                                "data.result.values",
                                hasItems(
                                        hasItems(hasItems(fragmentedLogLine.substring(102_400, 110 * 1024))),
                                        hasItems(hasItems(fragmentedLogLine.substring(0, 102_400))),
                                        hasItems(hasItems(unfragmentedLogLine))
                                )
                        )
                );
    }
}
