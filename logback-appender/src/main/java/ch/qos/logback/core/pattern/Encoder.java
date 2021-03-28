package ch.qos.logback.core.pattern;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;

public class Encoder {

    /**
     * 10kb should be enough for everyone. Even in case of 1000s of threads we will allocate at most 10s of MB of buffers.
     * Larger messages will be fragmented.
     */
    private static final int ENCODER_BUFFER_SIZE = Integer.getInteger("tjahzi.logback.layout.encoder.bufferSize", 10 * 1024);

    private final ByteBuffer buffer = ByteBuffer.allocate(ENCODER_BUFFER_SIZE);

    private final char[] temporaryTextArray = new char[1024];
    private final CharBuffer temporaryTextBuffer = CharBuffer.wrap(temporaryTextArray);
    private final CharsetEncoder charsetEncoder = StandardCharsets.UTF_8.newEncoder();

    public void encode(StringBuilder input) {
        int offset = 0;
        int sizeOfInput = input.length();
        while (sizeOfInput > 0) {
            int length = Math.min(sizeOfInput, temporaryTextArray.length);
            sizeOfInput -= length;

            encodeRound(input, offset, length, sizeOfInput == 0);
            offset += length;
        }

        buffer.flip();
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public void clear() {
        buffer.clear();
        charsetEncoder.reset();
    }

    private void encodeRound(StringBuilder input, int offset, int length, boolean endOfInput) {
        input.getChars(offset, offset + length, temporaryTextArray, 0);
        temporaryTextBuffer.clear().limit(length);

        CoderResult coderResult = charsetEncoder.encode(
                temporaryTextBuffer,
                buffer,
                endOfInput
        );

        if (coderResult.isError()) {
            throw new RuntimeException(coderResult.toString());
        }
    }
}
