package pl.tkowalcz.tjahzi.log4j2.labels;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

abstract class LabelBaseTest {

    public abstract LabelBase createCUT(String name, String value, String pattern);

    @Test
    void shouldValidateNamePattern() {
        // Given
        LabelBase correctLabel = createCUT("aaa_bbb_ccc", "fobar", null);
        LabelBase tooShortLabel = createCUT("", "foobar", null);
        LabelBase labelWithInvalidChars = createCUT("log-level", "foobar", null);

        // When & Then
        assertThat(correctLabel.hasValidName()).isTrue();
        assertThat(tooShortLabel.hasValidName()).isFalse();
        assertThat(labelWithInvalidChars.hasValidName()).isFalse();
    }
}
