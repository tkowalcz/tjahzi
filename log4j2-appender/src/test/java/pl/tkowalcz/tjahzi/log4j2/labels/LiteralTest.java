package pl.tkowalcz.tjahzi.log4j2.labels;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LiteralTest {

    @Test
    void shouldAppendItsContents() {
        // Given
        String expected = "constant";
        StringBuilder actual = new StringBuilder();

        Literal literal = Literal.of(expected);

        // When
        literal.append(null, actual::append);

        // Then
        assertThat(actual.toString()).isEqualTo(expected);
    }
}
