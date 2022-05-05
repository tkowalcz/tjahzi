package pl.tkowalcz.tjahzi.http;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ClientConfigurationBuilderTest {

    static Stream<Arguments> validConfigOptions() {
        return Stream.of(
                Arguments.of(
                        "Should take arguments from url",
                        "https://example.net:8443/monitoring/loki/api/v1/push",
                        "example.net",
                        8443,
                        "/monitoring/loki/api/v1/push",
                        true
                ),
                Arguments.of(
                        "Should not enable ssl if prefix is not https",
                        "http://example.net:8443/monitoring/loki/api/v1/push",
                        "example.net",
                        8443,
                        "/monitoring/loki/api/v1/push",
                        false
                ),
                Arguments.of(
                        "Should use default logEndpoint if none is provided",
                        "http://example.net:8443",
                        "example.net",
                        8443,
                        "/loki/api/v1/push",
                        false
                ),
                Arguments.of(
                        "Should use default http port if none is provided",
                        "http://example.net",
                        "example.net",
                        80,
                        "/loki/api/v1/push",
                        false
                ),
                Arguments.of(
                        "Should use default https port if none is provided",
                        "https://example.net",
                        "example.net",
                        443,
                        "/loki/api/v1/push",
                        true
                )
        );
    }

    static Stream<Arguments> invalidConfigOptions() {
        return Stream.of(
                Arguments.of(
                        "Should reject unknown protocol",
                        ClientConfiguration.builder().withUrl("file://example.net"),
                        "Unknown protocol scheme, must be one of 'https' or 'http' (case insensitive). Provided: 'file'"
                ),
                Arguments.of(
                        "Should not allow url and host",
                        ClientConfiguration.builder().withUrl("file://example.net").withHost("blah"),
                        "Only one of 'url' or 'host' can be configured. Current configuration sets url to 'file://example.net' and host to 'blah'"
                ),
                Arguments.of(
                        "Should not allow url and port",
                        ClientConfiguration.builder().withUrl("file://example.net").withPort(10),
                        "Only one of 'url' or 'port' can be configured. If using url you must configure port as part of url string. Current configuration sets url to 'file://example.net' and port to '10'"
                ),
                Arguments.of(
                        "Should not allow url and useSSL to be defined",
                        ClientConfiguration.builder().withUrl("file://example.net").withUseSSL(true),
                        "Only one of 'url' or 'useSSL' can be configured. Please use 'https' prefix in url to enable SSL. Current configuration sets url to 'file://example.net' and useSSL to 'true'"
                ),
                Arguments.of(
                        "Should require max requests in flight to be positive",
                        ClientConfiguration.builder().withHost("example.net").withMaxRequestsInFlight(-10),
                        "Property maxRequestsInFlight must be greater than 0"
                ),
                Arguments.of(
                        "Should require at least url or host to be specified",
                        ClientConfiguration.builder(),
                        "One of 'url' or 'host' must be configured."
                ),
                Arguments.of(
                        "Should not allow url and logEndpoint to be specified at the same time",
                        ClientConfiguration.builder().withUrl("http://example.net").withLogEndpoint("blah") ,
                        "Only one of 'url' or 'logEndpoint' can be configured. Embed log endpoint path into url. Log endpoint: 'blah', url: 'http://example.net'"
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validConfigOptions")
    void shouldCorrectlyInterpretUrl(
            String testCase,
            String url,
            String expectedHost,
            int expectedPort,
            String expectedLogEndpoint,
            boolean expectedUseSSL
    ) {
        // Given
        ClientConfigurationBuilder configurationBuilder = new ClientConfigurationBuilder();

        // When
        ClientConfiguration configuration = configurationBuilder
                .withUrl(url)
                .build();

        // Then
        assertThat(configuration.getHost()).isEqualTo(expectedHost);
        assertThat(configuration.getPort()).isEqualTo(expectedPort);
        assertThat(configuration.getLogEndpoint()).isEqualTo(expectedLogEndpoint);
        assertThat(configuration.isUseSSL()).isEqualTo(expectedUseSSL);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidConfigOptions")
    void shouldRejectUnknownProtocolScheme(
            String testCase,
            ClientConfigurationBuilder clientConfigurationBuilder,
            String expectedExceptionMessage
    ) {
        // When
        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                clientConfigurationBuilder::build
        );

        // Then
        assertThat(exception).hasMessage(expectedExceptionMessage);
    }
}
