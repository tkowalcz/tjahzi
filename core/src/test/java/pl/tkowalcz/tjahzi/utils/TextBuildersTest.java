package pl.tkowalcz.tjahzi.utils;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TextBuildersTest {

    @Test
    void shouldClearBufferBeforeUse() {
        // Given
        TextBuilder textBuilder = TextBuilders.threadLocal();
        textBuilder.append("abc");

        // When
        TextBuilder actual = TextBuilders.threadLocal();

        // Then
        assertThat(actual.toString()).isEmpty();
    }

    @Test
    void shouldReturnSameBufferInsideSameThread() {
        // When
        TextBuilder textBuilder1 = TextBuilders.threadLocal();
        TextBuilder textBuilder2 = TextBuilders.threadLocal();

        // Then
        assertThat(textBuilder1).isSameAs(textBuilder2);
    }

    @Test
    void shouldReturnDifferentBufferInsideDifferentThreads() throws InterruptedException {
        // Given
        AtomicReference<TextBuilder> captor1 = new AtomicReference<>();
        AtomicReference<TextBuilder> captor2 = new AtomicReference<>();

        // When
        Thread thread1 = new Thread(() -> captor1.set(TextBuilders.threadLocal()));
        thread1.start();
        thread1.join();

        Thread thread2 = new Thread(() -> captor2.set(TextBuilders.threadLocal()));
        thread2.start();
        thread2.join();

        // Then
        assertThat(captor1.get()).isNotNull();
        assertThat(captor2.get()).isNotNull();

        assertThat(captor1.get()).isNotSameAs(captor2.get());
    }
}
