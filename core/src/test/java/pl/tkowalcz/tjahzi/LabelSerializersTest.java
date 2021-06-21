package pl.tkowalcz.tjahzi;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class LabelSerializersTest {

    @Test
    void shouldClearBufferBeforeUse() {
        // Given
        LabelSerializer labelSerializer = LabelSerializers.threadLocal();
        labelSerializer.appendLabel("key", "abc");

        // When
        LabelSerializer actual = LabelSerializers.threadLocal();

        // Then
        assertThat(actual.getLabelsCount()).isZero();
    }

    @Test
    void shouldReturnSameBufferInsideSameThread() {
        // When
        LabelSerializer labelSerializer1 = LabelSerializers.threadLocal();
        LabelSerializer labelSerializer2 = LabelSerializers.threadLocal();

        // Then
        assertThat(labelSerializer1).isSameAs(labelSerializer2);
    }

    @Test
    void shouldReturnDifferentSerializerInsideDifferentThreads() throws InterruptedException {
        // Given
        AtomicReference<LabelSerializer> captor1 = new AtomicReference<>();
        AtomicReference<LabelSerializer> captor2 = new AtomicReference<>();

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
