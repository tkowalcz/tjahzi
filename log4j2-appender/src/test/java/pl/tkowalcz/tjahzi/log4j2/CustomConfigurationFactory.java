//package pl.tkowalcz.tjahzi.log4j2;
//
//import org.apache.logging.log4j.Level;
//import org.apache.logging.log4j.core.Filter;
//import org.apache.logging.log4j.core.LoggerContext;
//import org.apache.logging.log4j.core.appender.ConsoleAppender;
//import org.apache.logging.log4j.core.config.Configuration;
//import org.apache.logging.log4j.core.config.ConfigurationFactory;
//import org.apache.logging.log4j.core.config.ConfigurationSource;
//import org.apache.logging.log4j.core.config.Order;
//import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
//import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
//import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
//import org.apache.logging.log4j.core.config.plugins.Plugin;
//
//import java.net.URI;
//
//@Plugin(name = "CustomConfigurationFactory", category = ConfigurationFactory.CATEGORY)
//@Order(50)
//public class CustomConfigurationFactory extends ConfigurationFactory {
//
//    static Configuration createConfiguration(final String name, ConfigurationBuilder<BuiltConfiguration> builder) {
//        builder.setConfigurationName(name);
//        builder.setStatusLevel(Level.ERROR);
//        builder.setPackages("pl.tkowalcz.tjahzi.log4j2");
//
//        builder.add(
//                builder.newRootLogger(Level.INFO)
//                        .add(builder.newAppenderRef("Loki"))
//        );
//
//        AppenderComponentBuilder lokiAppenderBuilder = builder.newAppender("Loki", "Loki")
//                .addAttribute("host", "${sys:loki.host}")
//                .addAttribute("port", "${sys:loki.port}")
//                .add(
//                        builder.newFilter("ThresholdFilter", Filter.Result.ACCEPT, Filter.Result.DENY)
//                                .addAttribute("level", "ALL")
//                )
//                .add(
//                        builder.newLayout("PatternLayout").
//                                addAttribute("pattern", "%X{tid} [%t] %d{MM-dd HH:mm:ss.SSS} %5p %c{1} - %m%n%exception{full}")
//                )
//                .addAttribute("Header", "");
//        builder.add(lokiAppenderBuilder);
//
//        return builder.build();
//    }
//
//    @Override
//    public Configuration getConfiguration(final LoggerContext loggerContext, final ConfigurationSource source) {
//        return getConfiguration(loggerContext, source.toString(), null);
//    }
//
//    @Override
//    public Configuration getConfiguration(
//            final LoggerContext loggerContext,
//            final String name,
//            final URI configLocation
//    ) {
//        ConfigurationBuilder<BuiltConfiguration> builder = newConfigurationBuilder();
//        return createConfiguration(name, builder);
//    }
//
//    @Override
//    protected String[] getSupportedTypes() {
//        return new String[]{"*"};
//    }
//}
