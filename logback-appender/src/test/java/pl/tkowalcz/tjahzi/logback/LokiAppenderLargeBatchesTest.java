package pl.tkowalcz.tjahzi.logback;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.jupiter.api.Test;
import pl.tkowalcz.tjahzi.logback.infra.IntegrationTest;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static pl.tkowalcz.tjahzi.logback.infra.LokiAssert.assertThat;

class LokiAppenderLargeBatchesTest extends IntegrationTest {

    @Test
    void shouldSendData() {
        // Given
        LoggerContext context = loadConfig("appender-test-large-batches.xml");
        Logger logger = context.getLogger(LokiAppenderLargeBatchesTest.class);

        String expectedLogLine = "Cupcake ipsum dolor sit amet cake wafer. " +
                "Souffle jelly beans biscuit topping. " +
                "Danish bonbon gummies powder caramels. " +
                "Danish jelly beans sweet roll topping jelly beans oat cake toffee. " +
                "Chocolate cake sesame snaps brownie biscuit cheesecake. " +
                "Ice cream dessert sweet donut marshmallow. " +
                "Muffin bear claw cookie jelly-o sugar plum jelly beans apple pie fruitcake cookie. " +
                "Tootsie roll carrot cake pastry jujubes jelly beans chupa chups. " +
                "Souffle cake muffin liquorice tart souffle pie sesame snaps.";

        long expectedTimestamp = System.currentTimeMillis();

        // When
        for (int i = 0; i < 1000; i++) {
            logger.info(i + " " + expectedLogLine);
        }

        // Then
        assertThat(loki)
                .withFormParam("&start=" + expectedTimestamp + "&limit=1000&query=%7Bserver%3D%22127.0.0.1%22%7D")
                .returns(response -> response
                        .body("data.result.size()", equalTo(1))
                        .body("data.result[0].stream.server", equalTo("127.0.0.1"))
                        .body("data.result[0].values.size()", equalTo(1000))
                        .body("data.result[0].values", hasItems(new BaseMatcher<>() {
                                                                    @Override
                                                                    public boolean matches(Object o) {
                                                                        List<Object> list = (List<Object>) o;
                                                                        if (list.size() != 2) {
                                                                            return false;
                                                                        }

                                                                        long actualTimestamp = Long.parseLong(list.get(0).toString());
                                                                        String actualLogLine = list.get(1).toString();

                                                                        return actualLogLine.contains(expectedLogLine)
                                                                                && (expectedTimestamp - actualTimestamp) < TimeUnit.MINUTES.toMillis(1);
                                                                    }

                                                                    @Override
                                                                    public void describeTo(Description description) {

                                                                    }
                                                                }

                        ))
                );
    }
}
