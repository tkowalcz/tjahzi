package pl.tkowalcz.tjahzi.log4j2.labels;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LabelTest {

    @Test
    void shouldValidateNamePattern() {
        // Given
        Label correctLabel = Label.createLabel("aaa_bbb_ccc", "fobar", null);
        Label tooShortLabel = Label.createLabel("", "foobar", null);
        Label labelWithInvalidChars = Label.createLabel("log-level", "foobar", null);

        // When & Then
        assertThat(correctLabel.hasValidName()).isTrue();
        assertThat(tooShortLabel.hasValidName()).isFalse();
        assertThat(labelWithInvalidChars.hasValidName()).isFalse();
    }
}
