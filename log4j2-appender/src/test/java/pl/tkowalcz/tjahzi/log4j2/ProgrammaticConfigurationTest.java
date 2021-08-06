package pl.tkowalcz.tjahzi.log4j2;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.junit.jupiter.api.Test;
import pl.tkowalcz.tjahzi.log4j2.infra.IntegrationTest;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.greaterThan;
import static pl.tkowalcz.tjahzi.log4j2.infra.LokiAssert.assertThat;

public class ProgrammaticConfigurationTest extends IntegrationTest {

    @Test
    void shouldInitializeAppenderProgrammatically() {
        // When
        ConfigurationBuilder<BuiltConfiguration> builder = createConfiguration();
        Configurator.reconfigure(builder.build());

        Logger logger = LogManager.getLogger(ProgrammaticConfigurationTest.class);

        // Then
        logger.warn("Here I come!");

        assertThat(loki)
                .returns(response ->
                        response
                                .body("data.result.size()", equalTo(1))
                                .body("data.result[0].stream.server", equalTo("127.0.0.1"))
                                .body("data.result[0].values.size()", greaterThan(0))
                                .body(
                                        "data.result.values",
                                        hasItems(
                                                hasItems(
                                                        hasItems(
                                                                containsString("WARN ProgrammaticConfigurationTest - Here I come!")
                                                        )
                                                )
                                        )
                                )
                );
    }

    private ConfigurationBuilder<BuiltConfiguration> createConfiguration() {
        ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();

        builder.setConfigurationName("loki-programmatically");
        builder.setStatusLevel(Level.ALL);
        builder.setPackages("pl.tkowalcz.tjahzi.log4j2");

        builder.add(
                builder.newRootLogger(Level.ALL)
                        .add(builder.newAppenderRef("Loki"))
        );

        AppenderComponentBuilder lokiAppenderBuilder = builder.newAppender("Loki", "Loki")
                .addAttribute("host", "${sys:loki.host}")
                .addAttribute("port", "${sys:loki.port}")
                .add(
                        builder.newFilter("ThresholdFilter", Filter.Result.ACCEPT, Filter.Result.DENY)
                                .addAttribute("level", "ALL")
                )
                .add(
                        builder.newLayout("PatternLayout")
                                .addAttribute("pattern", "%X{tid} [%t] %d{MM-dd HH:mm:ss.SSS} %5p %c{1} - %m%n%exception{full}")
                )
                .addComponent(
                        builder.newComponent("Header")
                                .addAttribute("name", "server")
                                .addAttribute("value", "127.0.0.1")
                )
                .addComponent(
                        builder.newComponent("Label")
                                .addAttribute("name", "server")
                                .addAttribute("value", "127.0.0.1")
                );
        builder.add(lokiAppenderBuilder);

        return builder;
    }
}
