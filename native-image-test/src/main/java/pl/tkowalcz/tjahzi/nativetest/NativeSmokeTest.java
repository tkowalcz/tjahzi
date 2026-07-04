package pl.tkowalcz.tjahzi.nativetest;

import ch.qos.logback.classic.LoggerContext;
import org.apache.log4j.xml.DOMConfigurator;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * End to end smoke test for GraalVM native image support. Exercises all
 * three appenders (logback, log4j2, reload4j) through their reflective
 * configuration paths, ships one log line via each to a Loki instance on
 * localhost:3100, flushes on shutdown and verifies via the query API that
 * every line arrived. Exits with a non zero code on failure.
 */
public class NativeSmokeTest {

    public static void main(String[] args) throws Exception {
        long runId = System.nanoTime();

        String logbackLine = "Native smoke test logback " + runId;
        String log4j2Line = "Native smoke test log4j2 " + runId;
        String reload4jLine = "Native smoke test reload4j " + runId;

        // Logback - configured from logback.xml on the classpath.
        LoggerFactory.getLogger(NativeSmokeTest.class).info(logbackLine);

        // Log4j2 - configured from log4j2.xml on the classpath.
        org.apache.logging.log4j.LogManager.getLogger(NativeSmokeTest.class).info(log4j2Line);

        // Reload4j - configured explicitly from log4j-native.xml.
        DOMConfigurator.configure(NativeSmokeTest.class.getClassLoader().getResource("log4j-native.xml"));
        org.apache.log4j.Logger.getLogger(NativeSmokeTest.class).info(reload4jLine);

        // Stop all three logging systems - close() waits for the batches to
        // be acknowledged by Loki before returning.
        ((LoggerContext) LoggerFactory.getILoggerFactory()).stop();
        org.apache.logging.log4j.LogManager.shutdown();
        org.apache.log4j.LogManager.shutdown();

        boolean success = verify("native", logbackLine)
                & verify("native-log4j2", log4j2Line)
                & verify("native-reload4j", reload4jLine);

        if (!success) {
            System.exit(1);
        }
    }

    private static boolean verify(String serverLabel, String expectedLogLine) throws Exception {
        String query = URLEncoder.encode(
                "{server=\"" + serverLabel + "\"}",
                StandardCharsets.UTF_8
        );

        HttpRequest request = HttpRequest
                .newBuilder(URI.create("http://localhost:3100/loki/api/v1/query_range?query=" + query))
                .GET()
                .build();

        HttpClient httpClient = HttpClient.newHttpClient();
        for (int i = 0; i < 30; i++) {
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.body().contains(expectedLogLine)) {
                System.out.println("SUCCESS: " + serverLabel + " line delivered and queried back from Loki");
                return true;
            }

            Thread.sleep(1_000);
        }

        System.err.println("FAILURE: " + serverLabel + " line did not arrive in Loki within 30 seconds");
        return false;
    }
}
