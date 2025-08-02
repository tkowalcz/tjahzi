package pl.tkowalcz.tjahzi.reload4j;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.awaitility.Durations;
import org.junit.jupiter.api.Test;
import pl.tkowalcz.tjahzi.reload4j.infra.IntegrationTest;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static pl.tkowalcz.tjahzi.reload4j.infra.LokiAssert.assertThat;

class LokiAppenderReloadTest extends IntegrationTest {

    @Test
    void shouldNotLooseDataWhenConfigChanges() throws Exception {
        // Given
        URI uriBefore = getClass()
                .getClassLoader()
                .getResource("appender-test-reload-before.xml")
                .toURI();

        URI uriAfter = getClass()
                .getClassLoader()
                .getResource("appender-test-reload-after.xml")
                .toURI();

        URI uri = new File(Paths.get(uriBefore).toFile().getParent(), "appender-test-reload.xml").toURI();
        Files.copy(Paths.get(uriBefore), Paths.get(uri), StandardCopyOption.REPLACE_EXISTING);

        loadConfig(uri);
        DOMConfigurator.configureAndWatch(uri.toURL().getFile(), 1000);

        Logger logger = Logger.getLogger(LokiAppenderReloadTest.class);
        String expectedLogLine = "Test";

        // When
        Files.copy(Paths.get(uriAfter), Paths.get(uri), StandardCopyOption.REPLACE_EXISTING);
        await()
                .atMost(Durations.ONE_MINUTE)
                .ignoreExceptions()
                .untilAsserted(() -> {
                    logger.info(expectedLogLine);

                    // Then
                    assertThat(loki)
                            .withFormParam("query=%7Bserver%3D%22127.0.0.2%22%7D")
                            .returns(Durations.TWO_SECONDS, response -> response
                                    .body("data.result.size()", equalTo(1))
                                    .body("data.result[0].stream.server", equalTo("127.0.0.2"))
                                    .body("data.result[0].values.size()", greaterThanOrEqualTo(1))
                                    .body(
                                            "data.result.values",
                                            hasItems(
                                                    hasItems(
                                                            hasItems(
                                                                    containsString("INFO  pl.tkowalcz.tjahzi.reload4j.LokiAppenderReloadTest - Test")
                                                            )
                                                    )
                                            )
                                    )
                            );
                });
    }
}
