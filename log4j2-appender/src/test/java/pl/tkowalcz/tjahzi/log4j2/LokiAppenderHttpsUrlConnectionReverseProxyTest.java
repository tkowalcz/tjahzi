package pl.tkowalcz.tjahzi.log4j2;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.tkowalcz.tjahzi.log4j2.infra.HttpsNginxIntegrationTest;
import pl.tkowalcz.tjahzi.log4j2.infra.TruststoreUtil;

import java.io.File;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static pl.tkowalcz.tjahzi.log4j2.infra.LokiAssert.assertThat;

class LokiAppenderHttpsUrlConnectionReverseProxyTest extends HttpsNginxIntegrationTest {

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        File truststore = TruststoreUtil.createPkcs12TruststoreFromPem("nginx/nginx-selfsigned.crt", "changeit");
        System.setProperty("javax.net.ssl.trustStore", truststore.getAbsolutePath());
        System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
        System.setProperty("javax.net.ssl.trustStoreType", "PKCS12");
    }

    @Test
    void shouldSendData() {
        // Given
        loadConfig("appender-test-url-configuration.xml");
        Logger logger = LogManager.getLogger(LokiAppenderHttpsUrlConnectionReverseProxyTest.class);

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
                                                        containsString("INFO LokiAppenderHttpsUrlConnectionReverseProxyTest - " + expectedLogLine)
                                                )
                                        )
                                )
                        )
                );
    }
}
