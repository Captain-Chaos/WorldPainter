package org.pepsoft.util;

/**
 * Utility methods for working with arrays.
 */
public final class ArrayUtils {
    private ArrayUtils() {
        // Prevent instantiation
    }

    public static int indexOf(int[] array, int value) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == value) {
                return i;
            }
        }
        return -1;
    }
}