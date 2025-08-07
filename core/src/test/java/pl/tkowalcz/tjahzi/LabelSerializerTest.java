package pl.tkowalcz.tjahzi;

import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("ConstantConditions")
class LabelSerializerTest {

    @Test
    void shouldAppendLabelAndCountIt() {
        // Given
        LabelSerializer serializer = new LabelSerializer();

        // When
        serializer.appendLabel("Foo", "Bar");

        // Then
        assertThat(serializer.getLabelsCount()).isEqualTo(1);
        assertThat(serializer.sizeBytes()).isEqualTo(14);
        assertThat(serializer.toString()).isEqualTo("FooBar");
    }

    @Test
    void shouldAppendLabelsAndCountThem() {
        // Given
        LabelSerializer serializer = new LabelSerializer();

        // When
        serializer.appendLabel("Foo", "Bar");
        serializer.appendLabel("Key", "Value");
        serializer.appendLabel("Kang", "Kodos");

        // Then
        assertThat(serializer.getLabelsCount()).isEqualTo(3);
        assertThat(serializer.sizeBytes()).isEqualTo(47);
        assertThat(serializer.toString()).isEqualTo("FooBarKeyValueKangKodos");
    }

    @Test
    void shouldAppendMultipartLabelAndCountItAsOne() {
        // Given
        LabelSerializer serializer = new LabelSerializer();

        // When
        serializer.appendLabelName("Aliens");
        serializer.startAppendingLabelValue();
        serializer.appendPartialLabelValue("Kang");
        serializer.appendPartialLabelValue("Kodos");
        serializer.appendPartialLabelValue("Johnson");
        serializer.finishAppendingLabelValue();

        // Then
        assertThat(serializer.getLabelsCount()).isEqualTo(1);
        assertThat(serializer.sizeBytes()).isEqualTo(30);
        assertThat(serializer.toString()).isEqualTo("AliensKangKodosJohnson");
    }

    @Test
    void shouldAppendMultipartLabelsMixedWithRegular() {
        // Given
        LabelSerializer serializer = new LabelSerializer();

        // When
        serializer.appendLabel("Foo", "Bar");

        serializer
                .appendLabelName("Aliens")
                .startAppendingLabelValue()
                .appendPartialLabelValue("Kang")
                .appendPartialLabelValue("Kodos")
                .appendPartialLabelValue("Johnson")
                .finishAppendingLabelValue()
                .appendLabelName("Omicronians")
                .startAppendingLabelValue()
                .appendPartialLabelValue("Lrrr")
                .appendPartialLabelValue("RULER OF THE PLANET OMICRON PERSEI EIGHT")
                .finishAppendingLabelValue()
                .appendLabel("Popplers", "Problem");

        // Then
        assertThat(serializer.getLabelsCount()).isEqualTo(4);
        assertThat(serializer.sizeBytes()).isEqualTo(130);
        assertThat(serializer.toString()).isEqualTo("FooBarAliensKangKodosJohnsonOmicroniansLrrrRULER OF THE PLANET OMICRON PERSEI EIGHTPopplersProblem");
    }

    @Test
    void shouldClearSerializerContent() {
        // Given
        LabelSerializer serializer = new LabelSerializer();
        serializer.appendLabel("Foo", "Bar");

        // When
        serializer.clear();

        // Then
        assertThat(serializer.getLabelsCount()).isZero();
        assertThat(serializer.sizeBytes()).isEqualTo(0);
        assertThat(serializer.toString()).isEqualTo("");
    }

    @Test
    void shouldNotEqualToNull() {
        // Given
        LabelSerializer serializer = new LabelSerializer()
                .appendLabel("foo", "bar");
        LabelSerializer other = null;

        // When
        boolean actual = serializer.equals(other);

        // Then
        assertThat(actual).isFalse();
    }

    @Test
    void shouldNotEqualToOtherBufferOfEqualLength() {
        // Given
        LabelSerializer serializer = new LabelSerializer()
                .appendLabel("foo", "bar");
        LabelSerializer other = new LabelSerializer()
                .appendLabel("bar", "foo");

        // When
        boolean actual = serializer.equals(other);

        // Then
        assertThat(actual).isFalse();
    }

    @Test
    void shouldNotEqualToOtherBufferThatPreviouslyContainedSameContents() {
        // Given
        LabelSerializer serializer = new LabelSerializer()
                .appendLabel("foo", "bar");

        LabelSerializer other = new LabelSerializer()
                .appendLabel("foo", "bar");

        // When
        serializer.clear();
        boolean actual = serializer.equals(other);

        // Then
        assertThat(actual).isFalse();
    }

    @Test
    void shouldEqualToOtherBufferContainingSameData() {
        // Given
        LabelSerializer serializer = new LabelSerializer()
                .appendLabel("foo", "bar");

        LabelSerializer other = new LabelSerializer()
                .appendLabel("foo", "bar");

        // When
        boolean actual = serializer.equals(other);

        // Then
        assertThat(actual).isTrue();
    }

    @Test
    void shouldWriteLabelsToBuffer() {
        // Given
        LabelSerializer labelSerializer = new LabelSerializer()
                .appendLabel("Foo", "Bar")
                .appendLabelName("Aliens")
                .startAppendingLabelValue()
                .appendPartialLabelValue("Kang")
                .appendPartialLabelValue("Kodos")
                .appendPartialLabelValue("Johnson")
                .finishAppendingLabelValue();

        int bufferSize = labelSerializer.sizeBytes() + Integer.BYTES * 2;
        int offset = 16;
        ByteBuffer target = ByteBuffer.allocate(bufferSize + offset)
                .order(ByteOrder.LITTLE_ENDIAN);

        // When
        labelSerializer.writeLabels(new UnsafeBuffer(target), offset);

        // Then
        for (int i = 0; i < offset; i++) {
            assertThat(target.get()).isZero();
        }

        assertThat(target.getInt()).isEqualTo(labelSerializer.sizeBytes());
        assertThat(target.getInt()).isEqualTo(labelSerializer.getLabelsCount());
    }
}
