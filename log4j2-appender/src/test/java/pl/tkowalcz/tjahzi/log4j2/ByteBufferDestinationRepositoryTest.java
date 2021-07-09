package pl.tkowalcz.tjahzi.log4j2;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;

class ByteBufferDestinationRepositoryTest {

    @Test
    void shouldInitializeBufferToDesiredSize() {
        // Given
        int capacity = 42;
        ByteBufferDestinationRepository repository = new ByteBufferDestinationRepository(capacity);

        BiConsumer<LogEvent, ByteBuffer> drain = (logEvent, byteBuffer) -> {
        };
        MutableLogEvent logEvent = new MutableLogEvent();

        // When
        TjahziByteBufferDestination destination = repository.threadLocalDestination(drain, logEvent);

        // Then
        assertThat(destination.getByteBuffer().capacity()).isEqualTo(capacity);
        assertThat(destination.getByteBuffer().remaining()).isEqualTo(capacity);
    }

    @Test
    void shouldClearBufferBeforeUse() {
        // Given
        int capacity = 42;
        ByteBufferDestinationRepository repository = new ByteBufferDestinationRepository(capacity);

        BiConsumer<LogEvent, ByteBuffer> drain = (logEvent, byteBuffer) -> {
        };
        MutableLogEvent logEvent = new MutableLogEvent();

        TjahziByteBufferDestination destination = repository.threadLocalDestination(drain, logEvent);
        destination.writeBytes("blah".getBytes(), 0, 4);

        // When
        TjahziByteBufferDestination actual = repository.threadLocalDestination(drain, logEvent);

        // Then
        assertThat(actual.getByteBuffer().remaining()).isEqualTo(capacity);
        assertThat(actual.getByteBuffer().capacity()).isEqualTo(capacity);
    }

    @Test
    void shouldReturnSameBufferInsideSameThread() {
        // Given
        int capacity = 42;
        ByteBufferDestinationRepository repository = new ByteBufferDestinationRepository(capacity);

        BiConsumer<LogEvent, ByteBuffer> drain = (logEvent, byteBuffer) -> {
        };
        MutableLogEvent logEvent = new MutableLogEvent();

        // When
        TjahziByteBufferDestination actual1 = repository.threadLocalDestination(drain, logEvent);
        TjahziByteBufferDestination actual2 = repository.threadLocalDestination(drain, logEvent);

        // Then
        assertThat(actual1).isSameAs(actual2);
    }

    @Test
    void shouldReturnDifferentSerializerInsideDifferentThreads() throws InterruptedException {
        // Given
        int capacity = 42;
        ByteBufferDestinationRepository repository = new ByteBufferDestinationRepository(capacity);

        BiConsumer<LogEvent, ByteBuffer> drain = (logEvent, byteBuffer) -> {
        };
        MutableLogEvent logEvent = new MutableLogEvent();

        AtomicReference<TjahziByteBufferDestination> captor1 = new AtomicReference<>();
        AtomicReference<TjahziByteBufferDestination> captor2 = new AtomicReference<>();

        // When
        Thread thread1 = new Thread(() -> captor1.set(repository.threadLocalDestination(drain, logEvent)));
        thread1.start();
        thread1.join();

        Thread thread2 = new Thread(() -> captor2.set(repository.threadLocalDestination(drain, logEvent)));
        thread2.start();
        thread2.join();

        // Then
        assertThat(captor1.get()).isNotNull();
        assertThat(captor2.get()).isNotNull();

        assertThat(captor1.get()).isNotSameAs(captor2.get());
    }
}
