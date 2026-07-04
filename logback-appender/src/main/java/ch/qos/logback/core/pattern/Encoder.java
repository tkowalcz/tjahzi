package ch.qos.logback.core.pattern;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;

public class Encoder {

    /**
     * 10kb should be enough for everyone. Even in the case of 1000s of threads we will allocate at most 10s of MB of buffers.
     * Larger messages will be fragmented.
     */
    private static final int ENCODER_BUFFER_SIZE = Integer.getInteger("tjahzi.logback.layout.encoder.bufferSize", 10 * 1024);

    private final ByteBuffer buffer = ByteBuffer.allocate(ENCODER_BUFFER_SIZE);

    private final char[] temporaryTextArray = new char[1024];
    private final CharBuffer temporaryTextBuffer = CharBuffer.wrap(temporaryTextArray);
    private final CharsetEncoder charsetEncoder = StandardCharsets.UTF_8.newEncoder();

    private StringBuilder input;
    private int offset;
    private int remainingInput;

    /**
     * Encodes as much of the input as fits into the internal buffer in one go. Input that does
     * not fit is dropped. Use {@link #startEncoding(StringBuilder)} and {@link #encodeFragment()}
     * to fragment messages larger than the buffer without losing data.
     */
    public void encode(StringBuilder input) {
        startEncoding(input);
        encodeFragment();
    }

    public void startEncoding(StringBuilder input) {
        this.input = input;
        this.offset = 0;
        this.remainingInput = input.length();

        temporaryTextBuffer.clear().limit(0);
    }

    /**
     * Encodes input into the internal buffer until either the input is exhausted or the buffer
     * fills up. Flips the buffer so that it is ready for reading. Returns true if the buffer
     * filled up and there is more input to encode - consume the buffer, call
     * {@link #continueEncoding()} and invoke this method again to encode the remainder.
     */
    public boolean encodeFragment() {
        while (true) {
            if (!temporaryTextBuffer.hasRemaining() && remainingInput > 0) {
                int length = Math.min(remainingInput, temporaryTextArray.length);
                input.getChars(offset, offset + length, temporaryTextArray, 0);
                temporaryTextBuffer.clear().limit(length);

                offset += length;
                remainingInput -= length;
            }

            boolean endOfInput = remainingInput == 0;
            CoderResult coderResult = charsetEncoder.encode(
                    temporaryTextBuffer,
                    buffer,
                    endOfInput
            );

            if (coderResult.isError()) {
                throw new RuntimeException(coderResult.toString());
            }

            if (coderResult.isOverflow()) {
                buffer.flip();
                return true;
            }

            if (endOfInput) {
                buffer.flip();
                return false;
            }
        }
    }

    /**
     * Prepares the internal buffer for encoding the next fragment. Call after consuming
     * a fragment produced by {@link #encodeFragment()}.
     */
    public void continueEncoding() {
        buffer.clear();
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public void clear() {
        buffer.clear();
        charsetEncoder.reset();
    }
}
