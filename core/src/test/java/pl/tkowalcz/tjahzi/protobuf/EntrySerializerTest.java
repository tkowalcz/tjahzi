package pl.tkowalcz.tjahzi.protobuf;

import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import logproto.Logproto;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.tkowalcz.tjahzi.LabelSerializer;
import pl.tkowalcz.tjahzi.LabelSerializers;
import pl.tkowalcz.tjahzi.StructuredMetadataPointer;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.tkowalcz.tjahzi.protobuf.Protobuf.LENGTH_DELIMITED_TYPE;

class EntrySerializerTest {

    private ByteBuf logLine;

    @BeforeEach
    void setUp() {
        // Prepare the log line
        logLine = ByteBufAllocator.DEFAULT.buffer();
        logLine.writeInt(0);

        ByteBufUtil.writeUtf8(logLine, "Test");
        logLine.setIntLE(0, logLine.readableBytes() - Integer.BYTES);
    }

    @Test
    void shouldProperlySerialiseLogLineWithoutMetadata() throws InvalidProtocolBufferException {
        // Given
        ByteBuf target = ByteBufAllocator.DEFAULT.buffer();
        target.writeByte(2 << 3 | LENGTH_DELIMITED_TYPE);

        long epochMillisecond = 123_000L;
        long nanoOfMillisecond = 42L;

        // When
        EntrySerializer.serialize(
                epochMillisecond,
                nanoOfMillisecond,
                logLine,
                new StructuredMetadataPointer(null, 0, 0),
                target
        );

        // Then
        Logproto.StreamAdapter streamAdapter = Logproto.StreamAdapter.parseFrom(ByteBufUtil.getBytes(target));
        assertThat(streamAdapter.getEntriesList()).hasSize(1);

        Logproto.EntryAdapter entry = streamAdapter.getEntries(0);
        assertThat(entry.getTimestamp().getSeconds()).isEqualTo(epochMillisecond / 1000);
        assertThat(entry.getTimestamp().getNanos()).isEqualTo(nanoOfMillisecond);

        assertThat(entry.getLine()).isEqualTo("Test");
        assertThat(entry.getStructuredMetadataList()).hasSize(0);
    }

    @Test
    void shouldProperlySerialiseLogLineWithMetadata() throws InvalidProtocolBufferException {
        // Given
        ByteBuf logLine = ByteBufAllocator.DEFAULT.buffer();
        logLine.writeInt(0);
        ByteBufUtil.writeUtf8(logLine, "Test");
        logLine.setIntLE(0, logLine.readableBytes() - Integer.BYTES);

        ByteBuf target = ByteBufAllocator.DEFAULT.buffer();
        target.writeByte(2 << 3 | LENGTH_DELIMITED_TYPE);

        long epochMillisecond = 123_000L;
        long nanoOfMillisecond = 42L;

        LabelSerializer labelSerializer = LabelSerializers.threadLocal().getFirst();
        labelSerializer.appendLabel("abc", "def");
        labelSerializer.appendLabel("123", "456");
        DirectBuffer buffer = labelSerializer.getBuffer();
        int sizeBytes = labelSerializer.sizeBytes();

        UnsafeBuffer metadataBuffer = new UnsafeBuffer(new byte[sizeBytes + Integer.BYTES]);
        metadataBuffer.putInt(0, 2);
        metadataBuffer.putBytes(Integer.BYTES, buffer, 0, sizeBytes);

        // When
        EntrySerializer.serialize(
                epochMillisecond,
                nanoOfMillisecond,
                logLine,
                new StructuredMetadataPointer(metadataBuffer, 0, sizeBytes),
                target
        );

        // Then
        Logproto.StreamAdapter streamAdapter = Logproto.StreamAdapter.parseFrom(ByteBufUtil.getBytes(target));
        assertThat(streamAdapter.getEntriesList()).hasSize(1);

        Logproto.EntryAdapter entry = streamAdapter.getEntries(0);
        assertThat(entry.getTimestamp().getSeconds()).isEqualTo(epochMillisecond / 1000);
        assertThat(entry.getTimestamp().getNanos()).isEqualTo(nanoOfMillisecond);

        assertThat(entry.getLine()).isEqualTo("Test");

        assertThat(entry.getStructuredMetadataList()).hasSize(2);
        assertThat(entry.getStructuredMetadata(0).getName()).isEqualTo("abc");
        assertThat(entry.getStructuredMetadata(0).getValue()).isEqualTo("def");
        assertThat(entry.getStructuredMetadata(1).getName()).isEqualTo("123");
        assertThat(entry.getStructuredMetadata(1).getValue()).isEqualTo("456");
    }
}
