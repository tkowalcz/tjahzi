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

class LabelsContextSubstitutionTest extends IntegrationTest {

    @Test
    void shouldSendData() {
        // Given
        loadConfig("labels-context-substitution-test-configuration.xml");

        Logger logger = LogManager.getLogger(LabelsContextSubstitutionTest.class);

        // When
        MDC.put("object", "bus_ticket");
        MDC.put("owner", "wally");
        logger.info("Test1");

        MDC.put("object", "comb");
        MDC.put("owner", "jennifer");
        logger.info("Test2");

        MDC.clear();
        logger.info("Test3");

        // Then
        assertThat(loki)
                .returns(response -> response
                        .body("data.result.size()", equalTo(3))
                        .body("data.result.stream.server", everyItem(equalTo("127.0.0.1")))
                        .body("data.result.stream.object", contains("prefix_", "prefix_bus_ticket", "prefix_comb"))
                        .body("data.result.stream.owner", contains("_suffix", "wally_suffix", "jennifer_suffix"))
                        .body("data.result.stream.default_value_test", contains("use_this_if_missing", "use_this_if_missing", "use_this_if_missing"))
                        .body("data.result.values",
                                hasItems(
                                        hasItems(hasItems("LabelsContextSubstitutionTest - Test3")),
                                        hasItems(hasItems("LabelsContextSubstitutionTest - Test1")),
                                        hasItems(hasItems("LabelsContextSubstitutionTest - Test2"))
                                )
                        )
                );
    }
}
