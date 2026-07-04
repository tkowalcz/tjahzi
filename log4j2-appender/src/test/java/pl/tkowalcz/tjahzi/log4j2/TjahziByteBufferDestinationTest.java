package pl.tkowalcz.tjahzi.log4j2;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;

class TjahziByteBufferDestinationTest {

    @Test
    void shouldDrainBufferFilledExactlyToCapacity() {
        // Given
        int capacity = 4;
        TjahziByteBufferDestination destination = new TjahziByteBufferDestination(capacity);

        List<byte[]> drainedChunks = new ArrayList<>();
        BiConsumer<LogEvent, ByteBuffer> drain = (logEvent, byteBuffer) -> {
            byte[] chunk = new byte[byteBuffer.remaining()];
            byteBuffer.get(chunk);
            drainedChunks.add(chunk);
        };

        MutableLogEvent logEvent = new MutableLogEvent();
        destination.initialize(drain, logEvent);

        destination.writeBytes("blah".getBytes(), 0, capacity);

        // When
        destination.drainRemaining();

        // Then
        assertThat(drainedChunks).containsExactly("blah".getBytes());
    }

    @Test
    void shouldNotDrainEmptyBuffer() {
        // Given
        TjahziByteBufferDestination destination = new TjahziByteBufferDestination(4);

        List<byte[]> drainedChunks = new ArrayList<>();
        BiConsumer<LogEvent, ByteBuffer> drain = (logEvent, byteBuffer) -> {
            byte[] chunk = new byte[byteBuffer.remaining()];
            byteBuffer.get(chunk);
            drainedChunks.add(chunk);
        };

        MutableLogEvent logEvent = new MutableLogEvent();
        destination.initialize(drain, logEvent);

        // When
        destination.drainRemaining();

        // Then
        assertThat(drainedChunks).isEmpty();
    }

    @Test
    void shouldDrainPartiallyFilledBuffer() {
        // Given
        TjahziByteBufferDestination destination = new TjahziByteBufferDestination(16);

        List<byte[]> drainedChunks = new ArrayList<>();
        BiConsumer<LogEvent, ByteBuffer> drain = (logEvent, byteBuffer) -> {
            byte[] chunk = new byte[byteBuffer.remaining()];
            byteBuffer.get(chunk);
            drainedChunks.add(chunk);
        };

        MutableLogEvent logEvent = new MutableLogEvent();
        destination.initialize(drain, logEvent);

        destination.writeBytes("blah".getBytes(), 0, 4);

        // When
        destination.drainRemaining();

        // Then
        assertThat(drainedChunks).containsExactly("blah".getBytes());
    }
}
