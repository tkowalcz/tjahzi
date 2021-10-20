package pl.tkowalcz.tjahzi.log4j2.utils;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class StringBuildersTest {

    @Test
    void shouldClearBufferBeforeUse() {
        // Given
        StringBuilder stringBuilder = StringBuilders.threadLocal();
        stringBuilder.append("abc");

        // When
        StringBuilder actual = StringBuilders.threadLocal();

        // Then
        assertThat(actual.toString()).isEmpty();
    }

    @Test
    void shouldReturnSameBufferInsideSameThread() {
        // When
        StringBuilder stringBuilder1 = StringBuilders.threadLocal();
        StringBuilder stringBuilder2 = StringBuilders.threadLocal();

        // Then
        assertThat(stringBuilder1).isSameAs(stringBuilder2);
    }

    @Test
    void shouldReturnDifferentBufferInsideDifferentThreads() throws InterruptedException {
        // Given
        AtomicReference<StringBuilder> captor1 = new AtomicReference<>();
        AtomicReference<StringBuilder> captor2 = new AtomicReference<>();

        // When
        Thread thread1 = new Thread(() -> captor1.set(StringBuilders.threadLocal()));
        thread1.start();
        thread1.join();

        Thread thread2 = new Thread(() -> captor2.set(StringBuilders.threadLocal()));
        thread2.start();
        thread2.join();

        // Then
        assertThat(captor1.get()).isNotNull();
        assertThat(captor2.get()).isNotNull();

        assertThat(captor1.get()).isNotSameAs(captor2.get());
    }
}
