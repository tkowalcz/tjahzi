package pl.tkowalcz.tjahzi;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LabelSerializerPairTest {

    @Test
    void shouldClearBothSerializers() {
        // Given
        LabelSerializerPair pair = new LabelSerializerPair();
        pair.getFirst().appendLabel("foo", "bar");
        pair.getSecond().appendLabel("123", "456");

        // When
        pair.clear();

        // Then
        assertThat(pair.getFirst().getLabelsCount()).isEqualTo(0);
        assertThat(pair.getSecond().getLabelsCount()).isEqualTo(0);
    }
}