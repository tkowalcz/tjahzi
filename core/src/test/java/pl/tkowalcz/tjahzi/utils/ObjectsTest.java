package pl.tkowalcz.tjahzi.utils;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ObjectsTest {

    @Test
    void shouldAllowIndexSmallerThanLength() {
        // Given
        int index = 12;
        int length = 20;

        // When & Then
        assertDoesNotThrow(() -> Objects.checkIndex(index, length));
    }

    @Test
    void shouldThrowIfIndexNegative() {
        // Given
        int index = -5;
        int length = 20;

        // When & Then
        Assertions.assertThat(
                assertThrows(IndexOutOfBoundsException.class, () ->
                        Objects.checkIndex(index, length)
                )
        ).hasMessage("Index out of bounds for input params: index = -5, length = 20");
    }

    @Test
    void shouldThrowIfIndexEqualToLength() {
        // Given
        int index = 20;
        int length = 20;

        // When & Then
        Assertions.assertThat(
                assertThrows(IndexOutOfBoundsException.class, () ->
                        Objects.checkIndex(index, length)
                )
        ).hasMessage("Index out of bounds for input params: index = 20, length = 20");
    }

    @Test
    void shouldThrowIfIndexGreaterThanLength() {
        // Given
        int index = 21;
        int length = 20;

        // When & Then
        Assertions.assertThat(
                assertThrows(IndexOutOfBoundsException.class, () ->
                        Objects.checkIndex(index, length)
                )
        ).hasMessage("Index out of bounds for input params: index = 21, length = 20");
    }

    @Test
    void shouldAllowStartAndEndSmallerThanLength() {
        // Given
        int start = 12;
        int end = 19;
        int length = 20;

        // When & Then
        assertDoesNotThrow(() -> Objects.checkFromToIndex(start, end, length));
    }

    @Test
    void shouldThrowIfStartNegative() {
        // Given
        int start = -12;
        int end = 12;
        int length = 20;

        // When & Then
        Assertions.assertThat(
                assertThrows(IndexOutOfBoundsException.class, () ->
                        Objects.checkFromToIndex(start, end, length)
                )
        ).hasMessage("Index out of bounds for input params: start = -12, end = 12, length = 20");
    }

    @Test
    void shouldNotThrowIfStartEqualToEnd() {
        // Given
        int start = 12;
        int end = 12;
        int length = 20;

        // When & Then
        assertDoesNotThrow(() -> Objects.checkFromToIndex(start, end, length));
    }

    @Test
    void shouldThrowIfStartGreaterThanEnd() {
        // Given
        int start = 15;
        int end = 12;
        int length = 20;

        // When & Then
        Assertions.assertThat(
                assertThrows(IndexOutOfBoundsException.class, () ->
                        Objects.checkFromToIndex(start, end, length)
                )
        ).hasMessage("Index out of bounds for input params: start = 15, end = 12, length = 20");
    }

    @Test
    void shouldNotThrowIfEndEqualToLength() {
        // Given
        int start = 15;
        int end = 20;
        int length = 20;

        // When & Then
        assertDoesNotThrow(() -> Objects.checkFromToIndex(start, end, length));
    }

    @Test
    void shouldThrowIfEndEqualGreaterThanLength() {
        // Given
        int start = 15;
        int end = 21;
        int length = 20;

        // When & Then
        Assertions.assertThat(
                assertThrows(IndexOutOfBoundsException.class, () ->
                        Objects.checkFromToIndex(start, end, length)
                )
        ).hasMessage("Index out of bounds for input params: start = 15, end = 21, length = 20");
    }
}
