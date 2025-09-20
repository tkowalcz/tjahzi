package pl.tkowalcz.tjahzi.reload4j;

import org.apache.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.tkowalcz.tjahzi.reload4j.infra.HttpsNginxIntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static pl.tkowalcz.tjahzi.reload4j.infra.LokiAssert.assertThat;
import static pl.tkowalcz.tjahzi.reload4j.infra.TruststoreUtil.setupTruststoreSysProps;

class LokiAppenderSelfSignedCertWithNginxTest extends HttpsNginxIntegrationTest {

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        setupTruststoreSysProps("nginx/nginx-selfsigned.crt", "changeit");
    }

    @Test
    void shouldSendData() {
        // Given
        loadConfig("appender-test-cert-selfsigned-configuration.xml");
        Logger logger = Logger.getLogger(LokiAppenderSelfSignedCertWithNginxTest.class);

        String expectedLogLine = "Hello World";

        // When
        logger.info(expectedLogLine);

        // Then
        assertThat(loki)
                .returns(response -> response
                        .body("data.result.size()", equalTo(1))
                        .body("data.result[0].stream.server", equalTo("127.0.0.1"))
                        .body(
                                "data.result.values",
                                hasItems(
                                        hasItems(
                                                hasItems(
                                                        containsString("LokiAppenderSelfSignedCertWithNginxTest - " + expectedLogLine)
                                                )
                                        )
                                )
                        )
                );
    }
}
