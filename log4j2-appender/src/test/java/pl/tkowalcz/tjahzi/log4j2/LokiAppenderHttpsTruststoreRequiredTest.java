package pl.tkowalcz.tjahzi.log4j2;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.tkowalcz.tjahzi.log4j2.infra.HttpsNginxIntegrationTest;
import pl.tkowalcz.tjahzi.log4j2.infra.LokiAssert;

import static org.hamcrest.CoreMatchers.equalTo;

class LokiAppenderHttpsTruststoreRequiredTest extends HttpsNginxIntegrationTest {

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        // Ensure truststore properties are not set
        System.clearProperty("loki.truststore.path");
        System.clearProperty("loki.truststore.password");
        System.clearProperty("loki.truststore.type");
    }

    @Test
    void shouldNotSendDataWithoutTruststore() {
        // Given
        loadConfig("appender-test-custom-endpoint-configuration-negative.xml");
        Logger logger = LogManager.getLogger(LokiAppenderHttpsTruststoreRequiredTest.class);

        String expectedLogLine = "Hello World - should not arrive";

        // When
        logger.info(expectedLogLine);

        // Then - query for a unique label used only by this test; expect no results
        LokiAssert.assertThat(loki)
                .withFormParam("query=%7Bscenario%3D%22tls-missing-truststore%22%7D")
                .returns(response -> response
                        .body("data.result.size()", equalTo(0))
                );
    }
}
