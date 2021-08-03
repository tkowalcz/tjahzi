package pl.tkowalcz.tjahzi.log4j2;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import pl.tkowalcz.tjahzi.log4j2.infra.IntegrationTest;
import pl.tkowalcz.tjahzi.log4j2.infra.LokiAssert;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static org.hamcrest.CoreMatchers.*;

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

        URI uri = new URI("file://" + Paths.get(uriBefore).toFile().getParent() + "/appender-test-reload.xml");
        Files.copy(Paths.get(uriBefore), Paths.get(uri), StandardCopyOption.REPLACE_EXISTING);

        loadConfig(uri);
        Logger logger = LogManager.getLogger(LokiAppenderReloadTest.class);

        String expectedLogLine = "Test";

        // When
        logger.info(expectedLogLine);
        Files.copy(Paths.get(uriAfter), Paths.get(uri), StandardCopyOption.REPLACE_EXISTING);

        // Then
        LokiAssert.assertThat(loki)
                .returns(response -> response
                        .body("data.result.size()", equalTo(1))
                        .body("data.result[0].stream.server", equalTo("127.0.0.1"))
                        .body("data.result[0].values.size()", equalTo(1))
                        .body(
                                "data.result.values",
                                hasItems(
                                        hasItems(
                                                hasItems(
                                                        containsString("INFO LokiAppenderReloadTest - Test")
                                                )
                                        )
                                )
                        )
                );
    }
}
