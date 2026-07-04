package pl.tkowalcz.tjahzi.nativetest;

import ch.qos.logback.classic.LoggerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * End to end smoke test for GraalVM native image support. Loads the Loki
 * appender through logback.xml (exercising the reflective configuration
 * path), ships one log line to a Loki instance on localhost:3100, flushes
 * on shutdown and verifies via the query API that the line arrived.
 * Exits with a non zero code on failure.
 */
public class NativeSmokeTest {

    public static void main(String[] args) throws Exception {
        String expectedLogLine = "Native smoke test " + System.nanoTime();

        Logger logger = LoggerFactory.getLogger(NativeSmokeTest.class);
        logger.info(expectedLogLine);

        // Stopping the context flushes the appender - close() waits for
        // the batch to be acknowledged by Loki before returning.
        ((LoggerContext) LoggerFactory.getILoggerFactory()).stop();

        String query = URLEncoder.encode("{server=\"native\"}", StandardCharsets.UTF_8);
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
                System.out.println("SUCCESS: log line delivered and queried back from Loki");
                return;
            }

            Thread.sleep(1_000);
        }

        System.err.println("FAILURE: log line did not arrive in Loki within 30 seconds");
        System.exit(1);
    }
}
