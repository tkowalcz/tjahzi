package pl.tkowalcz.tjahzi.reload4j;

import org.apache.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.tkowalcz.tjahzi.reload4j.infra.HttpsNginxIntegrationTest;
import pl.tkowalcz.tjahzi.reload4j.infra.TruststoreUtil;

import java.io.File;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static pl.tkowalcz.tjahzi.reload4j.infra.LokiAssert.assertThat;

class LokiAppenderHttpsJksTruststoreTest extends HttpsNginxIntegrationTest {

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        // Prepare JKS truststore and pass it explicitly to appender
        File ts = TruststoreUtil.createJksTruststoreFromPem("nginx/nginx-selfsigned.crt", "changeit");
        System.setProperty("loki.truststore.path", ts.getAbsolutePath());
        System.setProperty("loki.truststore.password", "changeit");
        System.setProperty("loki.truststore.type", "JKS");
    }

    @Test
    void shouldSendDataUsingJksTruststore() {
        // Given
        loadConfig("appender-test-custom-endpoint-configuration.xml");
        Logger logger = Logger.getLogger(LokiAppenderHttpsJksTruststoreTest.class);

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
                                                        containsString("LokiAppenderHttpsJksTruststoreTest - " + expectedLogLine)
                                                )
                                        )
                                )
                        )
                );
    }

}
