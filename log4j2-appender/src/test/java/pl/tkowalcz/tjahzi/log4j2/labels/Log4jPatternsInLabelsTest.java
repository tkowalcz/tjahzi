package pl.tkowalcz.tjahzi.log4j2.labels;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import pl.tkowalcz.tjahzi.log4j2.infra.IntegrationTest;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.core.Every.everyItem;
import static pl.tkowalcz.tjahzi.log4j2.infra.LokiAssert.assertThat;

class Log4jPatternsInLabelsTest extends IntegrationTest {

    @Test
    void shouldSendData() {
        // Given
        loadConfig("log4j-patterns-in-labels-configuration.xml");

        Logger logger = LogManager.getLogger(Log4jPatternsInLabelsTest.class);

        // When
        MDC.put("tid", "req-230rq9ubou");
        logger.info("Test1");

        MDC.clear();
        logger.info("Test2");

        // Then
        assertThat(loki)
                .returns(response -> response
                        .body("data.result.size()", equalTo(2))
                        .body("data.result.stream.server", everyItem(equalTo("127.0.0.1")))
                        .body("data.result.stream.class_pattern", everyItem(equalTo("p.t.t.l.l.Log4jPatternsInLabelsTest")))
                        .body("data.result.stream.sequence_number", contains("2", "1"))
                        .body("data.result.stream.mdc_tid", contains("", "req-230rq9ubou"))
                        .body("data.result.values",
                                hasItems(
                                        hasItems(hasItems("Log4jPatternsInLabelsTest - Test2")),
                                        hasItems(hasItems("Log4jPatternsInLabelsTest - Test1"))
                                )
                        )
                );
    }
}
