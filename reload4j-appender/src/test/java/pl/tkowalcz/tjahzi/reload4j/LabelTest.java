package pl.tkowalcz.tjahzi.reload4j;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LabelTest {

    @Test
    void shouldValidateNamePattern() {
        // Given
        Label correctLabel = Label.createLabel("aaa_bbb_ccc", "fobar");
        Label tooShortLabel = Label.createLabel("", "foobar");
        Label labelWithInvalidChars = Label.createLabel("log-level", "foobar");

        // When & Then
        assertThat(correctLabel.hasValidName()).isTrue();
        assertThat(tooShortLabel.hasValidName()).isFalse();
        assertThat(labelWithInvalidChars.hasValidName()).isFalse();
    }
}
