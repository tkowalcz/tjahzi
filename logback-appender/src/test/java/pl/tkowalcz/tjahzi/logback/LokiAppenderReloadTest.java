package pl.tkowalcz.tjahzi.logback;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.junit.jupiter.api.Test;
import pl.tkowalcz.tjahzi.logback.infra.IntegrationTest;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static org.hamcrest.CoreMatchers.*;
import static pl.tkowalcz.tjahzi.logback.infra.LokiAssert.assertThat;

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

        LoggerContext context = loadConfig(uri);
        Logger logger = context.getLogger(LokiAppenderReloadTest.class);

        String expectedLogLine = "Test";

        // When
        logger.info(expectedLogLine);
        Files.copy(Paths.get(uriAfter), Paths.get(uri), StandardCopyOption.REPLACE_EXISTING);

        // Then
        assertThat(loki)
                .returns(response -> response
                        .body("data.result.size()", equalTo(1))
                        .body("data.result[0].stream.server", equalTo("127.0.0.1"))
                        .body("data.result[0].values.size()", equalTo(1))
                        .body(
                                "data.result.values",
                                hasItems(
                                        hasItems(
                                                hasItems(
                                                        containsString("INFO  p.t.t.l.LokiAppenderReloadTest - Test")
                                                )
                                        )
                                )
                        )
                );
    }
}
