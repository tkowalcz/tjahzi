package pl.tkowalcz.tjahzi;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class LabelSerializersTest {

    @Test
    void shouldClearBufferBeforeUse() {
        // Given
        LabelSerializerPair labelSerializer = LabelSerializers.threadLocal();
        labelSerializer.getFirst().appendLabel("key", "abc");
        labelSerializer.getSecond().appendLabel("key", "abc");

        // When
        LabelSerializerPair actual = LabelSerializers.threadLocal();

        // Then
        assertThat(actual.getFirst().getLabelsCount()).isZero();
        assertThat(actual.getSecond().getLabelsCount()).isZero();
    }

    @Test
    void shouldReturnSameBufferInsideSameThread() {
        // When
        LabelSerializerPair labelSerializer1 = LabelSerializers.threadLocal();
        LabelSerializerPair labelSerializer2 = LabelSerializers.threadLocal();

        // Then
        assertThat(labelSerializer1).isSameAs(labelSerializer2);
        assertThat(labelSerializer1.getFirst()).isSameAs(labelSerializer2.getFirst());
        assertThat(labelSerializer1.getSecond()).isSameAs(labelSerializer2.getSecond());
    }

    @Test
    void shouldReturnDifferentSerializerInsideDifferentThreads() throws InterruptedException {
        // Given
        AtomicReference<LabelSerializerPair> captor1 = new AtomicReference<>();
        AtomicReference<LabelSerializerPair> captor2 = new AtomicReference<>();

        // When
        Thread thread1 = new Thread(() -> captor1.set(LabelSerializers.threadLocal()));
        thread1.start();
        thread1.join();

        Thread thread2 = new Thread(() -> captor2.set(LabelSerializers.threadLocal()));
        thread2.start();
        thread2.join();

        // Then
        assertThat(captor1.get()).isNotNull();
        assertThat(captor2.get()).isNotNull();

        assertThat(captor1.get()).isNotSameAs(captor2.get());
    }
}
