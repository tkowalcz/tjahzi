/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package pl.tkowalcz.tjahzi.http;

import io.netty.buffer.ByteBuf;

import java.util.Arrays;

/**
 * Uncompresses an input {@link ByteBuf} encoded with Snappy compression into an
 * output {@link ByteBuf}.
 * <p>
 * See <a href="https://github.com/google/snappy/blob/master/format_description.txt">snappy format</a>.
 */
public final class Snappy {

    private static final int MIN_COMPRESSIBLE_BYTES = 15;

    // constants for the tag types
    private static final int LITERAL = 0;
    private static final int COPY_1_BYTE_OFFSET = 1;
    private static final int COPY_2_BYTE_OFFSET = 2;

    private static final int MAX_HT_SIZE = 1 << 14;
    private final short[] hashTable = new short[MAX_HT_SIZE];

    public void encode(final ByteBuf in, final ByteBuf out, final int length) {
        // Write the preamble length to the output buffer
        for (int i = 0; ; i++) {
            int b = length >>> i * 7;
            if ((b & 0xFFFFFF80) != 0) {
                out.writeByte(b & 0x7f | 0x80);
            } else {
                out.writeByte(b);
                break;
            }
        }

        while (in.readableBytes() > 0) {
            int min = Math.min(Short.MAX_VALUE, in.readableBytes());

            ByteBuf slice = in.readSlice(min);
            encode0(slice, out, slice.readableBytes());
        }
    }

    private void encode0(final ByteBuf in, final ByteBuf out, final int length) {
        int inIndex = in.readerIndex();
        final int baseIndex = inIndex;

        final short[] table = getHashTable(length);
        final int shift = Integer.numberOfLeadingZeros(table.length) + 1;

        int nextEmit = inIndex;

        if (length - inIndex >= MIN_COMPRESSIBLE_BYTES) {
            int nextHash = hash(in, ++inIndex, shift);
            outer:
            while (true) {
                int skip = 32;

                int candidate;
                int nextIndex = inIndex;
                do {
                    inIndex = nextIndex;
                    int hash = nextHash;
                    int bytesBetweenHashLookups = skip++ >> 5;
                    nextIndex = inIndex + bytesBetweenHashLookups;

                    // We need at least 4 remaining bytes to read the hash
                    if (nextIndex > length - 4) {
                        break outer;
                    }

                    nextHash = hash(in, nextIndex, shift);

                    candidate = baseIndex + table[hash];

                    table[hash] = (short) (inIndex - baseIndex);
                }
                while (in.getInt(inIndex) != in.getInt(candidate));

                encodeLiteral(in, out, inIndex - nextEmit);

                int insertTail;
                do {
                    int base = inIndex;
                    int matched = 4 + findMatchingLength(in, candidate + 4, inIndex + 4, length);
                    inIndex += matched;
                    int offset = base - candidate;
                    encodeCopy(out, offset, matched);
                    in.readerIndex(in.readerIndex() + matched);
                    insertTail = inIndex - 1;
                    nextEmit = inIndex;
                    if (inIndex >= length - 4) {
                        break outer;
                    }

                    int prevHash = hash(in, insertTail, shift);
                    table[prevHash] = (short) (inIndex - baseIndex - 1);
                    int currentHash = hash(in, insertTail + 1, shift);
                    candidate = baseIndex + table[currentHash];
                    table[currentHash] = (short) (inIndex - baseIndex);
                }
                while (in.getInt(insertTail + 1) == in.getInt(candidate));

                nextHash = hash(in, insertTail + 2, shift);
                ++inIndex;
            }
        }

        // If there are any remaining characters, write them out as a literal
        if (nextEmit < length) {
            encodeLiteral(in, out, length - nextEmit);
        }
    }

    /**
     * Hashes the 4 bytes located at index, shifting the resulting hash into
     * the appropriate range for our hash table.
     *
     * @param in    The input buffer to read 4 bytes from
     * @param index The index to read at
     * @param shift The shift value, for ensuring that the resulting value is
     *              withing the range of our hash table size
     * @return A 32-bit hash of 4 bytes located at index
     */
    private static int hash(ByteBuf in, int index, int shift) {
        return in.getInt(index) * 0x1e35a7bd >>> shift;
    }

    /**
     * Creates an appropriately sized hashtable for the given input size
     *
     * @param inputSize The size of our input, ie. the number of bytes we need to encode
     * @return An appropriately sized empty hashtable
     */
    private short[] getHashTable(int inputSize) {
        Arrays.fill(hashTable, (short) 0);
        return hashTable;
    }

    /**
     * Iterates over the supplied input buffer between the supplied minIndex and
     * maxIndex to find how long our matched copy overlaps with an already-written
     * literal value.
     *
     * @param in       The input buffer to scan over
     * @param minIndex The index in the input buffer to start scanning from
     * @param inIndex  The index of the start of our copy
     * @param maxIndex The length of our input buffer
     * @return The number of bytes for which our candidate copy is a repeat of
     */
    private static int findMatchingLength(ByteBuf in, int minIndex, int inIndex, int maxIndex) {
        int matched = 0;

        while (inIndex <= maxIndex - 4 &&
                in.getInt(inIndex) == in.getInt(minIndex + matched)) {
            inIndex += 4;
            matched += 4;
        }

        while (inIndex < maxIndex && in.getByte(minIndex + matched) == in.getByte(inIndex)) {
            ++inIndex;
            ++matched;
        }

        return matched;
    }

    /**
     * Calculates the minimum number of bits required to encode a value.  This can
     * then in turn be used to calculate the number of septets or octets (as
     * appropriate) to use to encode a length parameter.
     *
     * @param value The value to calculate the minimum number of bits required to encode
     * @return The minimum number of bits required to encode the supplied value
     */
    private static int bitsToEncode(int value) {
        int highestOneBit = Integer.highestOneBit(value);
        int bitLength = 0;
        while ((highestOneBit >>= 1) != 0) {
            bitLength++;
        }

        return bitLength;
    }

    /**
     * Writes a literal to the supplied output buffer by directly copying from
     * the input buffer.  The literal is taken from the current readerIndex
     * up to the supplied length.
     *
     * @param in     The input buffer to copy from
     * @param out    The output buffer to copy to
     * @param length The length of the literal to copy
     */
    static void encodeLiteral(ByteBuf in, ByteBuf out, int length) {
        if (length < 61) {
            out.writeByte(length - 1 << 2);
        } else {
            int bitLength = bitsToEncode(length - 1);
            int bytesToEncode = 1 + bitLength / 8;
            out.writeByte(59 + bytesToEncode << 2);
            for (int i = 0; i < bytesToEncode; i++) {
                out.writeByte(length - 1 >> i * 8 & 0x0ff);
            }
        }

        out.writeBytes(in, length);
    }

    private static void encodeCopyWithOffset(ByteBuf out, int offset, int length) {
        if (length < 12 && offset < 2048) {
            out.writeByte(COPY_1_BYTE_OFFSET | length - 4 << 2 | offset >> 8 << 5);
            out.writeByte(offset & 0x0ff);
        } else {
            out.writeByte(COPY_2_BYTE_OFFSET | length - 1 << 2);
            out.writeByte(offset & 0x0ff);
            out.writeByte(offset >> 8 & 0x0ff);
        }
    }

    /**
     * Encodes a series of copies, each at most 64 bytes in length.
     *
     * @param out    The output buffer to write the copy pointer to
     * @param offset The offset at which the original instance lies
     * @param length The length of the original instance
     */
    private static void encodeCopy(ByteBuf out, int offset, int length) {
        while (length >= 68) {
            encodeCopyWithOffset(out, offset, 64);
            length -= 64;
        }

        if (length > 64) {
            encodeCopyWithOffset(out, offset, 60);
            length -= 60;
        }

        encodeCopyWithOffset(out, offset, length);
    }
}
