package pl.tkowalcz.tjahzi.utils;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TextBuilderTest {

    @Test
    void shouldTrackLength() {
        // Given
        TextBuilder textBuilder = new TextBuilder();

        // When
        textBuilder.append("abcd");
        textBuilder.append('f');
        textBuilder.append("1234567890", 3, 7);
        textBuilder.setCharAt(3, 'n');

        // Then
        assertThat(textBuilder.length()).isEqualTo(9);
        assertThat(textBuilder.toString()).isEqualTo("abcnf4567");
    }

    @Test
    void shouldClearBuilder() {
        // Given
        TextBuilder textBuilder = new TextBuilder();
        textBuilder.append("abcd");

        // When
        textBuilder.clear();

        // Then
        assertThat(textBuilder.length()).isZero();
        assertThat(textBuilder.toString()).isEmpty();
    }

    @Test
    void shouldSetCharAtIndex() {
        // Given
        TextBuilder textBuilder = new TextBuilder();
        textBuilder.append("1234567890");

        // When
        textBuilder.setCharAt(3, 'f');

        // Then
        assertThat(textBuilder.length()).isEqualTo(10);
        assertThat(textBuilder.toString()).isEqualTo("123f567890");
    }

    @Test
    void shouldNotAllowSettingCharAtWrongIndex() {
        // Given
        TextBuilder textBuilder = new TextBuilder();
        textBuilder.append("1234567890");

        // When & Then
        assertThrows(
                IndexOutOfBoundsException.class,
                () -> textBuilder.setCharAt(-10, 'f')
        );

        assertThrows(
                IndexOutOfBoundsException.class,
                () -> textBuilder.setCharAt(11, 'f')
        );
    }

    @Test
    void shouldReturnCharAt() {
        // Given
        TextBuilder textBuilder = new TextBuilder();
        textBuilder.append("1234567890");

        // When
        char actual = textBuilder.charAt(6);

        // Then
        assertThat(actual).isEqualTo('7');
    }

    @Test
    void shouldNotAllowReturningCharAtWrongIndex() {
        // Given
        TextBuilder textBuilder = new TextBuilder();
        textBuilder.append("1234567890");

        // When & Then
        assertThrows(
                IndexOutOfBoundsException.class,
                () -> textBuilder.charAt(-10)
        );

        assertThrows(
                IndexOutOfBoundsException.class,
                () -> textBuilder.charAt(11)
        );
    }

    @Test
    void shouldReturnSubstring() {
        // Given
        TextBuilder textBuilder = new TextBuilder();
        textBuilder.append("1234567890");

        // When
        CharSequence actual = textBuilder.subSequence(3, 8);

        // Then
        assertThat(actual).isEqualTo("45678");
    }

    @Test
    void shouldVerifySubstringArguments() {
        // Given
        TextBuilder textBuilder = new TextBuilder();
        textBuilder.append("1234567890");

        // When & Then
        assertThrows(
                IndexOutOfBoundsException.class,
                () -> textBuilder.subSequence(-1, 8)
        );

        assertThrows(
                IndexOutOfBoundsException.class,
                () -> textBuilder.subSequence(50, 8)
        );

        assertThrows(
                IndexOutOfBoundsException.class,
                () -> textBuilder.subSequence(3, -1)
        );

        assertThrows(
                IndexOutOfBoundsException.class,
                () -> textBuilder.subSequence(3, 120)
        );

        assertThrows(
                IndexOutOfBoundsException.class,
                () -> textBuilder.subSequence(6, 5)
        );
    }

    @Test
    void shouldReturnStringRepresentation() {
        // Given
        TextBuilder textBuilder = new TextBuilder();
        textBuilder.append("1234567890");

        // When
        CharSequence actual = textBuilder.toString();

        // Then
        assertThat(actual).isEqualTo("1234567890");
    }

    @Test
    void shouldConstructJSON() {
        // Given
        TextBuilder textBuilder = new TextBuilder();

        // When
        textBuilder.append("{ ");
        textBuilder.append("aaaa=bbb");
        textBuilder.append(',');
        textBuilder.append("foo=bar");
        textBuilder.append(',');
        textBuilder.setCharAt(textBuilder.length() - 1, '}');

        // Then
        CharSequence actual = textBuilder.toString();
        assertThat(actual).isEqualTo("{ aaaa=bbb,foo=bar}");
    }

    @Test
    void shouldConstructEmptyJSON() {
        // Given
        TextBuilder textBuilder = new TextBuilder();

        // When
        textBuilder.append("{ ");
        textBuilder.setCharAt(textBuilder.length() - 1, '}');

        // Then
        CharSequence actual = textBuilder.toString();
        assertThat(actual).isEqualTo("{}");
    }

    @Test
    void shouldGrowBackendStorage() {
        // Given
        String expected = StringUtils.repeat("abcd", 121);
        TextBuilder textBuilder = new TextBuilder();

        // When
        int i;
        for (i = 0; i < 121; i++) {
            textBuilder.append('a');
            textBuilder.append("bcd");
        }

        // Then
        assertThat(textBuilder.length()).isEqualTo(expected.length());
        assertThat(textBuilder.toString()).isEqualTo(expected);
    }
}
