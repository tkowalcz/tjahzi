package pl.tkowalcz.thjazi.protobuf;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.PooledByteBufAllocator;
import logproto.Logproto;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;
import org.agrona.concurrent.ringbuffer.RingBufferDescriptor;
import org.junit.jupiter.api.Test;
import pl.tkowalcz.tjahzi.LogBufferSerializer;
import pl.tkowalcz.tjahzi.LogBufferTranscoder;
import pl.tkowalcz.tjahzi.OutputBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

public class ProtobufDeserializer {

    @Test
    void shouldDeserialize() throws IOException {
        UnsafeBuffer buffer = new UnsafeBuffer(
                ByteBuffer.wrap(
                        new byte[1024 + RingBufferDescriptor.TRAILER_LENGTH]
                )
        );

        ManyToOneRingBuffer logBuffer = new ManyToOneRingBuffer(
                buffer
        );

        LogBufferSerializer serializer = new LogBufferSerializer(logBuffer.buffer());
        serializer.writeTo(
                0,
                System.currentTimeMillis(),
                Map.of("faaaoo", "baqwefewr", "a232aa", "bbbrgwew"),
                "LEVEL",
                "DEBUG",
                ByteBuffer.wrap("Test".getBytes())
        );

        LogBufferTranscoder deserializer = new LogBufferTranscoder(Map.of(), buffer);
        OutputBuffer outputBuffer = new OutputBuffer(PooledByteBufAllocator.DEFAULT.buffer());
        deserializer.deserializeIntoByteBuf(
                logBuffer.buffer(),
                0,
                outputBuffer
        );

        ByteBuf target = outputBuffer.close();

        Logproto.PushRequest pushRequest = Logproto.PushRequest.parser().parsePartialFrom(new ByteBufInputStream(target));
        Logproto.StreamAdapter stream = pushRequest.getStreams(0);
        System.out.println("labels = " + stream.getLabels());
        System.out.println("timestamp1Seconds = " + stream.getEntries(0).getTimestamp().getSeconds());
        System.out.println("timestamp1Nano = " + stream.getEntries(0).getTimestamp().getNanos());
        System.out.println("logline = " + stream.getEntries(0).getLine());
    }
}
