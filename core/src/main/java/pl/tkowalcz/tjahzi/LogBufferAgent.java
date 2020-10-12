package pl.tkowalcz.tjahzi;

import com.google.protobuf.Timestamp;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.PooledByteBufAllocator;
import javolution.text.TextBuilder;
import logproto.Logproto;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.MessageHandler;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;
import pl.tkowalcz.tjahzi.http.NettyHttpClient;
import pl.tkowalcz.tjahzi.http.TextBuilders;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LogBufferAgent implements Agent, MessageHandler {

    public static final int MESSAGE_COUNT_LIMIT = 100;

    private final ManyToOneRingBuffer logBuffer;
    private final NettyHttpClient httpClient;

    private Logproto.PushRequest.Builder request = Logproto.PushRequest.newBuilder();

    public LogBufferAgent(
            ManyToOneRingBuffer logBuffer,
            NettyHttpClient httpClient
    ) {
        this.logBuffer = logBuffer;
        this.httpClient = httpClient;
    }

    @Override
    public int doWork() throws IOException {
        int workDone = logBuffer.read(this, MESSAGE_COUNT_LIMIT);

        if (workDone > 0) {
            ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer();
            request.build().writeTo(
                    new ByteBufOutputStream(buffer)
            );

//            StringBuilder dump = new StringBuilder();
//            ByteBufUtil.appendPrettyHexDump(dump, buffer.duplicate());
//            System.out.println(dump);

            httpClient.log(buffer);
            request = Logproto.PushRequest.newBuilder();
        }

        return workDone;
    }

    @Override
    public void onMessage(int msgTypeId, MutableDirectBuffer buffer, int index, int length) {
        if (msgTypeId == TjahziLogger.LOG_MESSAGE_TYPE_ID) {
            processMessage(buffer, index);
        } else {
            // Ignore
        }
    }

    private void processMessage(MutableDirectBuffer buffer, int index) {
        long timestamp = buffer.getLong(index);
        index += Long.BYTES;

        int labelsCount = buffer.getInt(index);
        index += Integer.BYTES;

        Map<String, String> labels = new HashMap<>();
        for (int i = 0; i < labelsCount; i++) {
            String key = buffer.getStringAscii(index);
            index += key.length() + Integer.BYTES;

            String value = buffer.getStringAscii(index);
            index += value.length() + Integer.BYTES;

            labels.put(key, value);
        }

        String line = buffer.getStringAscii(index);

        Logproto.StreamAdapter stream = Logproto.StreamAdapter.newBuilder()
                .setLabels(buildLabelsString(labels).toString())
                .addEntries(Logproto.EntryAdapter.newBuilder()
                        .setTimestamp(Timestamp.newBuilder()
                                .setSeconds(timestamp / 1000)
                                .setNanos((int) (timestamp % 1000) * 1000_000))
                        .setLine(line)
                )
                .build();

        request.addStreams(stream);
    }

    private CharSequence buildLabelsString(Map<String, String> labels) {
        TextBuilder textBuilder = TextBuilders.threadLocal();

        textBuilder.append("{ ");
        labels.forEach((key, value) -> textBuilder.append(key)
                .append("=")
                .append("\"")
                .append(value)
                .append("\","));

        textBuilder.setCharAt(textBuilder.length() - 1, '}');
        return textBuilder;
    }

    @Override
    public String roleName() {
        return "ReadingLogBufferAndSendingHttp";
    }
}
