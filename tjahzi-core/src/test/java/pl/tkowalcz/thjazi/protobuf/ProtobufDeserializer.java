//package pl.tkowalcz.thjazi.protobuf;
//
//import com.google.protobuf.CodedInputStream;
//import com.google.protobuf.ExtensionRegistryLite;
//import io.netty.buffer.ByteBuf;
//import io.netty.buffer.ByteBufInputStream;
//import io.netty.buffer.ByteBufOutputStream;
//import io.netty.buffer.PooledByteBufAllocator;
//import logproto.Logproto;
//import org.agrona.concurrent.UnsafeBuffer;
//import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;
//import org.agrona.concurrent.ringbuffer.RingBufferDescriptor;
//import org.junit.jupiter.api.Test;
//import pl.tkowalcz.tjahzi.LogBufferDeserializer;
//import pl.tkowalcz.tjahzi.LogBufferSerializer;
//import pl.tkowalcz.tjahzi.protobuf.PushRequestSerializer;
//
//import java.io.IOException;
//import java.nio.ByteBuffer;
//import java.util.Map;
//
//public class ProtobufDeserializer {
//
//    @Test
//    void shouldDeserialize() throws IOException {
//        ManyToOneRingBuffer logBuffer = new ManyToOneRingBuffer(
//                new UnsafeBuffer(new byte[1024 + RingBufferDescriptor.TRAILER_LENGTH])
//        );
//
//        LogBufferSerializer serializer = new LogBufferSerializer(logBuffer.buffer());
//        serializer.writeTo(
//                0,
//                System.currentTimeMillis(),
//                Map.of("faaaoo", "baqwefewr", "a232aa", "bbbrgwew"),
//                "LEVEL",
//                "DEBUG",
//                ByteBuffer.wrap("Test".getBytes())
//        );
//
//        LogBufferDeserializer deserializer = new LogBufferDeserializer(Map.of());
//        Logproto.StreamAdapter streamAdapter = deserializer.deserializeIntoProtobuf(
//                logBuffer.buffer(),
//                0
//        );
//
//        Logproto.PushRequest.Builder builder = Logproto.PushRequest.newBuilder();
//        builder.addStreams(streamAdapter);
//
//        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer();
//        builder.build().writeTo(
//                new ByteBufOutputStream(buffer)
//        );
//
//        ByteBuf streamBuffer = PooledByteBufAllocator.DEFAULT.buffer();
//        PushRequestSerializer.serialize(
//                System.currentTimeMillis(),
//                "FooBarBazTest",
//                "{foo=bar,bar=baz,baz=foo}",
//                streamBuffer
//        );
//
//        CodedInputStream decoder = CodedInputStream.newInstance(new ByteBufInputStream(streamBuffer));
//        Logproto.PushRequest pushRequest = decoder.readMessage(Logproto.PushRequest.parser(), ExtensionRegistryLite.getEmptyRegistry());
//        Logproto.StreamAdapter stream = pushRequest.getStreams(0);
//        System.out.println("labels = " + stream.getLabels());
//        System.out.println("timestamp1Seconds = " + stream.getEntries(0).getTimestamp().getSeconds());
//        System.out.println("timestamp1Nano = " + stream.getEntries(0).getTimestamp().getNanos());
//        System.out.println("logline = " + stream.getEntries(0).getLine());
//    }
//}
