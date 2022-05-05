package pl.tkowalcz.tjahzi.http;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ClientConfigurationBuilderTest {

    static Stream<Arguments> configOptions() {
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
                        "Should use default logEndpoint is none is provided",
                        "http://example.net:8443",
                        "example.net",
                        8443,
                        "/loki/api/v1/push",
                        false
                ),
                Arguments.of(
                        "Should use default logEndpoint is none is provided",
                        "http://example.net",
                        "example.net",
                        8443,
                        "/loki/api/v1/push",
                        false
                ),
                Arguments.of(
                        "Should reject unknown protocol",
                        "loki://example.net"
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("configOptions")
    void shouldGetConnectionParamsFromUrl(
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

    @Test
    void shouldRejectUnknownProtocolScheme() {
        // Given
        ClientConfigurationBuilder configurationBuilder = new ClientConfigurationBuilder();

        // When
        ClientConfiguration configuration = configurationBuilder
                .withUrl("loki://example.net:8443/monitoring/loki/api/v1/push")
                .build();
    }
}
