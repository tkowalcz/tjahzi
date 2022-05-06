package pl.tkowalcz.tjahzi.http;

import io.netty.handler.codec.http.HttpHeaders;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class HttpHeadersFactoryTest {

    @Test
    void shouldAddAuthHeaderIfCredentialsAreProvided_NoAdditionalHeaders() {
        // Given
        ClientConfiguration configuration = ClientConfiguration.builder()
                .withHost("blah")
                .withUsername("foob")
                .withPassword("ar")
                .build();

        String[] additionalHeaders = new String[0];

        // When
        HttpHeaders httpHeaders = HttpHeadersFactory.createHeaders(
                configuration,
                additionalHeaders
        );

        // Then
        assertThat(httpHeaders.entries())
                .hasSize(1)
                .containsExactly(
                        entry("authorization", "Basic Zm9vYjphcg==")
                );
    }

    @Test
    void shouldAddAuthHeaderIfCredentialsAreProvided_WithAdditionalHeaders() {
        // Given
        ClientConfiguration configuration = ClientConfiguration.builder()
                .withHost("blah")
                .withUsername("foob")
                .withPassword("ar")
                .build();

        String[] additionalHeaders = new String[]{
                "droid", "K2SO",
                "not-droid", "Jyn Erso",
        };

        // When
        HttpHeaders httpHeaders = HttpHeadersFactory.createHeaders(
                configuration,
                additionalHeaders
        );

        // Then
        assertThat(httpHeaders.entries())
                .hasSize(3)
                .containsExactlyInAnyOrder(
                        entry("authorization", "Basic Zm9vYjphcg=="),
                        entry("droid", "K2SO"),
                        entry("not-droid", "Jyn Erso")
                );
    }

    @Test
    void shouldNotAddAuthHeader_NoAdditionalHeaders() {
        // Given
        ClientConfiguration configuration = ClientConfiguration.builder()
                .withHost("blah")
                .build();

        String[] additionalHeaders = new String[0];

        // When
        HttpHeaders httpHeaders = HttpHeadersFactory.createHeaders(
                configuration,
                additionalHeaders
        );

        // Then
        assertThat(httpHeaders.entries())
                .isEmpty();
    }

    @Test
    void shouldNOtAddAuthHeader_WithAdditionalHeaders() {
        // Given
        ClientConfiguration configuration = ClientConfiguration.builder()
                .withHost("blah")
                .build();

        String[] additionalHeaders = new String[]{
                "droid", "K2SO",
                "not-droid", "Jyn Erso",
        };

        // When
        HttpHeaders httpHeaders = HttpHeadersFactory.createHeaders(
                configuration,
                additionalHeaders
        );

        // Then
        assertThat(httpHeaders.entries())
                .hasSize(2)
                .containsExactlyInAnyOrder(
                        entry("droid", "K2SO"),
                        entry("not-droid", "Jyn Erso")
                );
    }
}
