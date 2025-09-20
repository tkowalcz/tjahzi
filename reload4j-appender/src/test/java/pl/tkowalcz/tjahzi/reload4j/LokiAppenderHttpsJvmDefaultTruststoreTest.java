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

class LokiAppenderHttpsJvmDefaultTruststoreTest extends HttpsNginxIntegrationTest {

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        // Clear appender-specific truststore props to exercise JVM default truststore branch
        System.clearProperty("loki.truststore.path");
        System.clearProperty("loki.truststore.password");
        System.clearProperty("loki.truststore.type");

        // Set JVM default truststore to a generated PKCS12 containing the self-signed cert
        File ts = TruststoreUtil.createPkcs12TruststoreFromPem("nginx/nginx-selfsigned.crt", "changeit");
        System.setProperty("javax.net.ssl.trustStore", ts.getAbsolutePath());
        System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
    }

    @Test
    void shouldSendDataUsingJvmDefaultTruststore() {
        // Given
        loadConfig("appender-test-custom-endpoint-configuration.xml");
        Logger logger = Logger.getLogger(LokiAppenderHttpsJvmDefaultTruststoreTest.class);

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
                                                        containsString("LokiAppenderHttpsJvmDefaultTruststoreTest - " + expectedLogLine)
                                                )
                                        )
                                )
                        )
                );
    }

}
