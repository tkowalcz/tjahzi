package pl.tkowalcz.tjahzi.logback.util;

import ch.qos.logback.classic.LoggerContext;
import pl.tkowalcz.tjahzi.logback.LokiAppender;

import java.util.Spliterators;
import java.util.stream.StreamSupport;

public class TestUtil {

    public static LokiAppender getLokiAppender(LoggerContext context) {
        return (LokiAppender) context
                .getLoggerList()
                .stream()
                .flatMap(
                        logger ->
                                StreamSupport.stream(
                                        Spliterators.spliteratorUnknownSize(logger.iteratorForAppenders(), 0),
                                        false
                                )
                )
                .filter(appender -> appender instanceof LokiAppender)
                .findAny()
                .orElseThrow(() -> new AssertionError("Expected to find Loki appender"));
    }
}
