package pl.tkowalcz.tjahzi.reload4j.infra;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.parsing.Parser;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.testcontainers.containers.GenericContainer;

import java.time.Duration;
import java.util.function.Consumer;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

@SuppressWarnings("rawtypes")
public class LokiAssert {

    private String url = "/loki/api/v1/query_range";
    private String param = "query=%7Bserver%3D%22127.0.0.1%22%7D";

    private String username;
    private String password;

    public LokiAssert() {
        RestAssured.reset();
        RestAssured.registerParser("text/plain", Parser.JSON);
    }

    public LokiAssert(GenericContainer loki) {
        RestAssured.port = loki.getFirstMappedPort();
        RestAssured.baseURI = "http://" + loki.getHost();
        RestAssured.registerParser("text/plain", Parser.JSON);
    }

    public static LokiAssert assertThat() {
        return new LokiAssert();
    }

    public LokiAssert withFormParam(String param) {
        this.param = param;
        return this;
    }

    public static LokiAssert assertThat(GenericContainer loki) {
        return new LokiAssert(loki);
    }

    public void returns(Duration duration, Consumer<ValidatableResponse> restOfAssertionsGoHere) {
        Awaitility
                .await()
                .atMost(duration)
                .pollInterval(Durations.ONE_SECOND)
                .ignoreExceptions()
                .untilAsserted(() -> {
                    RequestSpecification requestSpecification = given()
                            .contentType(ContentType.URLENC)
                            .urlEncodingEnabled(false)
                            .formParam(param);

                    if (username != null || password != null) {
                        requestSpecification = requestSpecification
                                .auth()
                                .preemptive()
                                .basic(
                                        username,
                                        password
                                );
                    }

                    ValidatableResponse validatableResponse = requestSpecification
                            .when()
                            .get(url)
                            .then()
                            .log()
                            .all()
                            .statusCode(200)
                            .body("status", equalTo("success"));

                    restOfAssertionsGoHere.accept(validatableResponse);
                });
    }

    public void returns(Consumer<ValidatableResponse> restOfAssertionsGoHere) {
        returns(Durations.ONE_MINUTE, restOfAssertionsGoHere);
    }

    public LokiAssert withCredentials(String username, String password) {
        this.username = username;
        this.password = password;

        return this;
    }

    public LokiAssert calling(String url) {
        this.url = url;

        return this;
    }
}