package pl.tkowalcz.tjahzi.utils;

import java.util.Arrays;

public class TextBuilder implements Appendable, CharSequence {

    private static final int INITIAL_SIZE = 8;
    private static final int CAPACITY_INCREMENT = 64;

    private char[] chars = new char[INITIAL_SIZE];
    private int length;

    public TextBuilder clear() {
        length = 0;
        return this;
    }

    public TextBuilder append(CharSequence string) {
        ensureCapacity(string.length());

        for (int i = 0; i < string.length(); i++) {
            chars[length++] = string.charAt(i);
        }

        return this;
    }

    @Override
    public TextBuilder append(CharSequence sequence, int start, int end) {
        Objects.checkFromToIndex(start, end, sequence.length());
        ensureCapacity(sequence.length());

        for (int i = start; i < end; i++) {
            chars[length++] = sequence.charAt(i);
        }

        return this;
    }

    @Override
    public TextBuilder append(char c) {
        ensureCapacity(1);
        chars[length++] = c;
        return this;
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public char charAt(int index) {
        Objects.checkIndex(index, length);
        return chars[index];
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        Objects.checkFromToIndex(start, end, length);
        return new String(chars, start, end - start);
    }

    public TextBuilder setCharAt(int index, char c) {
        Objects.checkIndex(index, length);
        chars[index] = c;

        return this;
    }

    @Override
    public String toString() {
        return new String(chars, 0, length);
    }

    private void ensureCapacity(int requiredCapacity) {
        if (length + requiredCapacity >= chars.length) {
            int missingCapacity = length + requiredCapacity - chars.length;
            int expandedSize = chars.length + Math.max(CAPACITY_INCREMENT, missingCapacity);
            chars = Arrays.copyOf(chars, expandedSize);
        }
    }
}
