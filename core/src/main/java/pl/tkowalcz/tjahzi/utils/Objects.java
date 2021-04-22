package pl.tkowalcz.tjahzi.utils;

public class Objects {

    public static void checkIndex(int index, int length) {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException(
                    String.format(
                            "Index out of bounds for input params: index = %d, length = %d",
                            index,
                            length
                    )
            );
        }
    }

    public static void checkFromToIndex(int start, int end, int length) {
        if (start < 0 || start > end || end > length) {
            throw new IndexOutOfBoundsException(
                    String.format(
                            "Index out of bounds for input params: start = %d, end = %d, length = %d",
                            start,
                            end,
                            length
                    )
            );
        }
    }
}
