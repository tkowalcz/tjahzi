package ch.qos.logback.core.pattern;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class EncodersTest {

    @Test
    void shouldClearBufferBeforeUse() {
        // Given
        Encoder encoder = Encoders.threadLocal();
        encoder.encode(new StringBuilder("abc"));

        // When
        Encoder actual = Encoders.threadLocal();

        // Then
        assertThat(actual.getBuffer().position()).isZero();
    }

    @Test
    void shouldReturnSameBufferInsideSameThread() {
        // When
        Encoder encoder1 = Encoders.threadLocal();
        Encoder encoder2 = Encoders.threadLocal();

        // Then
        assertThat(encoder1).isSameAs(encoder2);
    }

    @Test
    void shouldReturnDifferentBufferInsideDifferentThreads() throws InterruptedException {
        // Given
        AtomicReference<Encoder> captor1 = new AtomicReference<>();
        AtomicReference<Encoder> captor2 = new AtomicReference<>();

        // When
        Thread thread1 = new Thread(() -> captor1.set(Encoders.threadLocal()));
        thread1.start();
        thread1.join();

        Thread thread2 = new Thread(() -> captor2.set(Encoders.threadLocal()));
        thread2.start();
        thread2.join();

        // Then
        assertThat(captor1.get()).isNotNull();
        assertThat(captor2.get()).isNotNull();

        assertThat(captor1.get()).isNotSameAs(captor2.get());
    }
}
