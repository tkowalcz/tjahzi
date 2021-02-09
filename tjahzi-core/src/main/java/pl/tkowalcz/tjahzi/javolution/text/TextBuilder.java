/*
 * Javolution - Java(TM) Solution for Real-Time and Embedded Systems
 * Copyright (C) 2012 - Javolution (http://javolution.org/)
 * All rights reserved.
 * 
 * Permission to use, copy, modify, and distribute this software is
 * freely granted, provided that this notice is preserved.
 */
package pl.tkowalcz.tjahzi.javolution.text;

import java.io.Serializable;

/**
 * <p> An {@link Appendable} text whose capacity expands 
 *     gently without incurring expensive resize/copy operations ever.</p>
 *     
 * <p> This class is not intended for large documents manipulations.
 *
 * @author  <a href="mailto:jean-marie@dautelle.com">Jean-Marie Dautelle</a>
 * @version 5.3, January 20, 2008
 */
public class TextBuilder implements Appendable, CharSequence, Serializable {

    private static final long serialVersionUID = 0x600L; // Version.
    // We do a full resize (and copy) only when the capacity is less than C1.
    // For large collections, multi-dimensional arrays are employed.
    private static final int B0 = 5; // Initial capacity in bits.
    private static final int C0 = 1 << B0; // Initial capacity (32)
    private static final int B1 = 10; // Low array maximum capacity in bits.
    private static final int C1 = 1 << B1; // Low array maximum capacity (1024).
    private static final int M1 = C1 - 1; // Mask.
    // Resizes up to 1024 maximum (32, 64, 128, 256, 512, 1024). 
    private char[] _low = new char[C0];
    // For larger capacity use multi-dimensional array.
    private char[][] _high = new char[1][];

    /**
     * Holds the current length.
     */
    private int _length;

    /**
     * Holds current capacity.
     */
    private int _capacity = C0;

    /**
     * Creates a text builder of small initial capacity.
     */
    public TextBuilder() {
        _high[0] = _low;
    }

    /**
     * Creates a text builder holding the specified <code>String</code>
     * (convenience method).
     * 
     * @param str the initial string content of this text builder.
     */
    public TextBuilder(String str) {
        this();
        append(str);
    }

    /**
     * Creates a text builder of specified initial capacity.
     * Unless the text length exceeds the specified capacity, operations 
     * on this text builder will not allocate memory.
     * 
     * @param capacity the initial capacity.
     */
    public TextBuilder(int capacity) {
        this();
        while (capacity > _capacity) {
            increaseCapacity();
        }
    }

    /**
     * Returns the length (character count) of this text builder.
     *
     * @return the number of characters (16-bits Unicode).
     */
    public final int length() {
        return _length;
    }

    /**
     * Returns the character at the specified index.
     *
     * @param  index the index of the character.
     * @return the character at the specified index.
     * @throws IndexOutOfBoundsException if 
     *         <code>(index < 0) || (index >= this.length())</code>.
     */
    public final char charAt(int index) {
        if (index >= _length)
            throw new IndexOutOfBoundsException();
        return index < C1 ? _low[index] : _high[index >> B1][index & M1];
    }

    /**
     * Sets the character at the specified position.
     *
     * @param index the index of the character to modify.
     * @param c the new character. 
     * @throws IndexOutOfBoundsException if <code>(index < 0) || 
     *          (index >= this.length())</code>
     */
    public final void setCharAt(int index, char c) {
        if ((index < 0) || (index >= _length))
            throw new IndexOutOfBoundsException();
        _high[index >> B1][index & M1] = c;
    }

    /**
     * Convenience method equivalent to {@link #setLength(int, char)
     *  setLength(newLength, '\u0000')}.
     *
     * @param newLength the new length of this builder.
     * @throws IndexOutOfBoundsException if <code>(newLength < 0)</code>
     */
    public final void setLength(int newLength) {
        setLength(newLength, '\u0000');
    }

    /**
     * Sets the length of this character builder.
     * If the length is greater than the current length; the 
     * specified character is inserted.
     *
     * @param newLength the new length of this builder.
     * @param fillChar the character to be appended if required.
     * @throws IndexOutOfBoundsException if <code>(newLength < 0)</code>
     */
    public final void setLength(int newLength, char fillChar) {
        if (newLength < 0)
            throw new IndexOutOfBoundsException();
        if (newLength <= _length)
            _length = newLength;
        else
            for (int i = _length; i++ < newLength;) {
                append(fillChar);
            }
    }

    /**
     * Returns a {@link java.lang.CharSequence} corresponding
     * to the character sequence between the specified indexes.
     *
     * @param  start the index of the first character inclusive.
     * @param  end the index of the last character exclusive.
     * @return a character sequence.
     * @throws IndexOutOfBoundsException if <code>(start < 0) || (end < 0) ||
     *         (start > end) || (end > this.length())</code>
     */
    public final java.lang.CharSequence subSequence(int start, int end) {
        if ((start < 0) || (end < 0) || (start > end) || (end > _length))
            throw new IndexOutOfBoundsException();
        return new String(_low, start, end - start);
    }

    /**
     * Appends the specified character.
     *
     * @param  c the character to append.
     * @return <code>this</code>
     */
    public final TextBuilder append(char c) {
        if (_length >= _capacity)
            increaseCapacity();
        _high[_length >> B1][_length & M1] = c;
        _length++;
        return this;
    }

    /**
     * Appends the specified character sequence. If the specified character
     * sequence is <code>null</code> this method is equivalent to
     * <code>append("null")</code>.
     *
     * @param  csq the character sequence to append or <code>null</code>.
     * @return <code>this</code>
     */
    public final TextBuilder append(CharSequence csq) {
        return (csq == null) ? append("null") : append(csq, 0, csq.length());
    }

    /**
     * Appends a subsequence of the specified character sequence.
     * If the specified character sequence is <code>null</code> this method 
     * is equivalent to <code>append("null")</code>. 
     *
     * @param  csq the character sequence to append or <code>null</code>.
     * @param  start the index of the first character to append.
     * @param  end the index after the last character to append.
     * @return <code>this</code>
     * @throws IndexOutOfBoundsException if <code>(start < 0) || (end < 0) 
     *         || (start > end) || (end > csq.length())</code>
     */
    public final TextBuilder append(CharSequence csq, int start,
            int end) {
        if (csq == null)
            return append("null");
        if ((start < 0) || (end < 0) || (start > end) || (end > csq.length()))
            throw new IndexOutOfBoundsException();
        for (int i = start; i < end;) {
            append(csq.charAt(i++));
        }
        return this;
    }

    /**
     * Appends the specified string to this text builder. 
     * If the specified string is <code>null</code> this method 
     * is equivalent to <code>append("null")</code>. 
     *
     * @param str the string to append or <code>null</code>.
     * @return <code>this</code>
     */
    public final TextBuilder append(String str) {
        return (str == null) ? append("null") : append(str, 0, str.length());
    }

    /**
     * Appends a subsequence of the specified string.
     * If the specified character sequence is <code>null</code> this method 
     * is equivalent to <code>append("null")</code>. 
     *
     * @param  str the string to append or <code>null</code>.
     * @param  start the index of the first character to append.
     * @param  end the index after the last character to append.
     * @return <code>this</code>
     * @throws IndexOutOfBoundsException if <code>(start < 0) || (end < 0) 
     *         || (start > end) || (end > str.length())</code>
     */
    public final TextBuilder append(String str, int start, int end) {
        if (str == null)
            return append("null");
        if ((start < 0) || (end < 0) || (start > end) || (end > str.length()))
            throw new IndexOutOfBoundsException("start: " + start + ", end: "
                    + end + ", str.length(): " + str.length());
        int newLength = _length + end - start;
        while (_capacity < newLength) {
            increaseCapacity();
        }
        for (int i = start, j = _length; i < end;) {
            char[] chars = _high[j >> B1];
            int dstBegin = j & M1;
            int inc = Math.min(C1 - dstBegin, end - i);
            str.getChars(i, (i += inc), chars, dstBegin);
            j += inc;
        }
        _length = newLength;
        return this;
    }

    /**
     * Appends the characters from the char array argument.
     *
     * @param  chars the character array source.
     * @return <code>this</code>
     */
    public final TextBuilder append(char chars[]) {
        append(chars, 0, chars.length);
        return this;
    }

    /**
     * Appends the characters from a subarray of the char array argument.
     *
     * @param  chars the character array source.
     * @param  offset the index of the first character to append.
     * @param  length the number of character to append.
     * @return <code>this</code>
     * @throws IndexOutOfBoundsException if <code>(offset < 0) || 
     *         (length < 0) || ((offset + length) > chars.length)</code>
     */
    public final TextBuilder append(char chars[], int offset, int length) {
        final int end = offset + length;
        if ((offset < 0) || (length < 0) || (end > chars.length))
            throw new IndexOutOfBoundsException();
        int newLength = _length + length;
        while (_capacity < newLength) {
            increaseCapacity();
        }
        for (int i = offset, j = _length; i < end;) {
            char[] dstChars = _high[j >> B1];
            int dstBegin = j & M1;
            int inc = Math.min(C1 - dstBegin, end - i);
            System.arraycopy(chars, i, dstChars, dstBegin, inc);
            i += inc;
            j += inc;
        }
        _length = newLength;
        return this;
    }

    /**
     * Appends the textual representation of the specified <code>boolean</code>
     * argument.
     *
     * @param  b the <code>boolean</code> to format.
     * @return <code>this</code>
     */
    public final TextBuilder append(boolean b) {
        return b ? append("true") : append("false");
    }

    /**
     * Appends the decimal representation of the specified <code>int</code>
     * argument.
     *
     * @param  i the <code>int</code> to format.
     * @return <code>this</code>
     */
    public final TextBuilder append(int i) {
        if (i <= 0) {
            if (i == 0)
                return append("0");
            if (i == Integer.MIN_VALUE) // Negation would overflow.
                return append("-2147483648");
            append('-');
            i = -i;
        }
        int digits = digitLength(i);
        if (_capacity < _length + digits)
            increaseCapacity();
        _length += digits;
        for (int index = _length - 1;; index--) {
            int j = i / 10;
            _high[index >> B1][index & M1] = (char) ('0' + i - (j * 10));
            if (j == 0)
                return this;
            i = j;
        }
    }

    /**
     * Appends the radix representation of the specified <code>int</code>
     * argument.
     *
     * @param  i the <code>int</code> to format.
     * @param  radix the radix (e.g. <code>16</code> for hexadecimal).
     * @return <code>this</code>
     */
    public final TextBuilder append(int i, int radix) {
        if (radix == 10)
            return append(i); // Faster.
        if (radix < 2 || radix > 36)
            throw new IllegalArgumentException("radix: " + radix);
        if (i < 0) {
            append('-');
            if (i == Integer.MIN_VALUE) { // Negative would overflow.
                appendPositive(-(i / radix), radix);
                return (TextBuilder) append(DIGIT_TO_CHAR[-(i % radix)]);
            }
            i = -i;
        }
        appendPositive(i, radix);
        return this;
    }

    private void appendPositive(int l1, int radix) {
        if (l1 >= radix) {
            int l2 = l1 / radix;
            // appendPositive(l2, radix);
            if (l2 >= radix) {
                int l3 = l2 / radix;
                // appendPositive(l3, radix);
                if (l3 >= radix) {
                    int l4 = l3 / radix;
                    appendPositive(l4, radix);
                    append(DIGIT_TO_CHAR[l3 - (l4 * radix)]);
                } else
                    append(DIGIT_TO_CHAR[l3]);
                append(DIGIT_TO_CHAR[l2 - (l3 * radix)]);
            } else
                append(DIGIT_TO_CHAR[l2]);
            append(DIGIT_TO_CHAR[l1 - (l2 * radix)]);
        } else
            append(DIGIT_TO_CHAR[l1]);
    }

    private final static char[] DIGIT_TO_CHAR = { '0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i',
            'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
            'w', 'x', 'y', 'z' };

    /**
     * Appends the decimal representation of the specified <code>long</code>
     * argument.
     *
     * @param  l the <code>long</code> to format.
     * @return <code>this</code>
     */
    public final TextBuilder append(long l) {
        if (l <= 0) {
            if (l == 0)
                return append("0");
            if (l == Long.MIN_VALUE) // Negation would overflow.
                return append("-9223372036854775808");
            append('-');
            l = -l;
        }
        if (l <= Integer.MAX_VALUE)
            return append((int) l);
        append(l / 1000000000);
        int i = (int) (l % 1000000000);
        int digits = digitLength(i);
        append("000000000", 0, 9 - digits);
        return append(i);
    }

    /**
     * Appends the radix representation of the specified <code>long</code>
     * argument.
     *
     * @param  l the <code>long</code> to format.
     * @param  radix the radix (e.g. <code>16</code> for hexadecimal).
     * @return <code>this</code>
     */
    public final TextBuilder append(long l, int radix) {
        if (radix == 10)
            return append(l); // Faster.
        if (radix < 2 || radix > 36)
            throw new IllegalArgumentException("radix: " + radix);
        if (l < 0) {
            append('-');
            if (l == Long.MIN_VALUE) { // Negative would overflow.
                appendPositive(-(l / radix), radix);
                return (TextBuilder) append(DIGIT_TO_CHAR[(int) -(l % radix)]);
            }
            l = -l;
        }
        appendPositive(l, radix);
        return this;
    }

    private void appendPositive(long l1, int radix) {
        if (l1 >= radix) {
            long l2 = l1 / radix;
            // appendPositive(l2, radix);
            if (l2 >= radix) {
                long l3 = l2 / radix;
                // appendPositive(l3, radix);
                if (l3 >= radix) {
                    long l4 = l3 / radix;
                    appendPositive(l4, radix);
                    append(DIGIT_TO_CHAR[(int) (l3 - (l4 * radix))]);
                } else
                    append(DIGIT_TO_CHAR[(int) l3]);
                append(DIGIT_TO_CHAR[(int) (l2 - (l3 * radix))]);
            } else
                append(DIGIT_TO_CHAR[(int) l2]);
            append(DIGIT_TO_CHAR[(int) (l1 - (l2 * radix))]);
        } else
            append(DIGIT_TO_CHAR[(int) l1]);
    }

    private static final long[] POW10_LONG = new long[] { 1L, 10L, 100L, 1000L,
            10000L, 100000L, 1000000L, 10000000L, 100000000L, 1000000000L,
            10000000000L, 100000000000L, 1000000000000L, 10000000000000L,
            100000000000000L, 1000000000000000L, 10000000000000000L,
            100000000000000000L, 1000000000000000000L };

    /**
     * Inserts the specified character sequence at the specified location.
     *
     * @param index the insertion position.
     * @param csq the character sequence being inserted.
     * @return <code>this</code>
     * @throws IndexOutOfBoundsException if <code>(index < 0) || 
     *         (index > this.length())</code>
     */
    public final TextBuilder insert(int index, java.lang.CharSequence csq) {
        if ((index < 0) || (index > _length))
            throw new IndexOutOfBoundsException("index: " + index);
        final int shift = csq.length();
        int newLength = _length + shift;
        while (newLength >= _capacity) {
            increaseCapacity();
        }
        _length = newLength;
        for (int i = _length - shift; --i >= index;) {
            this.setCharAt(i + shift, this.charAt(i));
        }
        for (int i = csq.length(); --i >= 0;) {
            this.setCharAt(index + i, csq.charAt(i));
        }
        return this;
    }

    /**
     * Removes all the characters of this text builder 
     * (equivalent to <code>this.delete(start, this.length())</code>).
     * 
     * @return <code>this.delete(0, this.length())</code>
     */
    public final TextBuilder clear() {
        _length = 0;
        return this;
    }

    /**
     * Removes the characters between the specified indices.
     * 
     * @param start the beginning index, inclusive.
     * @param end the ending index, exclusive.
     * @return <code>this</code>
     * @throws IndexOutOfBoundsException if <code>(start < 0) || (end < 0) 
     *         || (start > end) || (end > this.length())</code>
     */
    public final TextBuilder delete(int start, int end) {
        if ((start < 0) || (end < 0) || (start > end) || (end > this.length()))
            throw new IndexOutOfBoundsException();
        for (int i = end, j = start; i < _length;) {
            this.setCharAt(j++, this.charAt(i++));
        }
        _length -= end - start;
        return this;
    }

    /**
     * Reverses this character sequence.
     *
     * @return <code>this</code>
     */
    public final TextBuilder reverse() {
        final int n = _length - 1;
        for (int j = (n - 1) >> 1; j >= 0;) {
            char c = charAt(j);
            setCharAt(j, charAt(n - j));
            setCharAt(n - j--, c);
        }
        return this;
    }

    /**
     * Returns the hash code for this text builder.
     *
     * @return the hash code value.
     */
    @Override
    public final int hashCode() {
        int h = 0;
        for (int i = 0; i < _length;) {
            h = 31 * h + charAt(i++);
        }
        return h;
    }

    /**
     * Compares this text builder against the specified object for equality.
     * Returns <code>true</code> if the specified object is a text builder 
     * having the same character content.
     * 
     * @param  obj the object to compare with or <code>null</code>.
     * @return <code>true</code> if that is a text builder with the same 
     *         character content as this text; <code>false</code> otherwise.
     */
    @Override
    public final boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof TextBuilder))
            return false;
        TextBuilder that = (TextBuilder) obj;
        if (this._length != that._length)
            return false;
        for (int i = 0; i < _length;) {
            if (this.charAt(i) != that.charAt(i++))
                return false;
        }
        return true;
    }

    /**
     * Indicates if this text builder has the same character content as the 
     * specified character sequence.
     *
     * @param csq the character sequence to compare with.
     * @return <code>true</code> if the specified character sequence has the 
     *        same character content as this text; <code>false</code> otherwise.
     */
    public final boolean contentEquals(java.lang.CharSequence csq) {
        if (csq.length() != _length)
            return false;
        for (int i = 0; i < _length;) {
            char c = _high[i >> B1][i & M1];
            if (csq.charAt(i++) != c)
                return false;
        }
        return true;
    }

    /**
     * Increases this text builder capacity.
     */
    private void increaseCapacity() {
        if (_capacity < C1) { // For small capacity, resize.
            _capacity <<= 1;
            char[] tmp = new char[_capacity];
            System.arraycopy(_low, 0, tmp, 0, _length);
            _low = tmp;
            _high[0] = tmp;
        } else { // Add a new low block of 1024 elements.
            int j = _capacity >> B1;
            if (j >= _high.length) { // Resizes _high.
                char[][] tmp = new char[_high.length * 2][];
                System.arraycopy(_high, 0, tmp, 0, _high.length);
                _high = tmp;
            }
            _high[j] = new char[C1];
            _capacity += C1;
        }
    }

    /**
     * Returns the number of digits of the decimal representation of the
     * specified <code>int</code> value, excluding the sign character if any.
     *
     * @param i the <code>int</code> value for which the digit length is returned.
     * @return <code>String.valueOf(i).length()</code> for zero or positive values;
     *         <code>String.valueOf(i).length() - 1</code> for negative values.
     */
    private static int digitLength(int i) {
        if (i >= 0)
            return (i >= 100000) ? (i >= 10000000) ? (i >= 1000000000) ? 10
                    : (i >= 100000000) ? 9 : 8 : (i >= 1000000) ? 7 : 6
                    : (i >= 100) ? (i >= 10000) ? 5 : (i >= 1000) ? 4 : 3
                    : (i >= 10) ? 2 : 1;
        if (i == Integer.MIN_VALUE)
            return 10; // "2147483648".length()
        return digitLength(-i); // No overflow possible.
    }
}
