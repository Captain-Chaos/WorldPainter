/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.util;

import java.util.AbstractList;
import java.util.BitSet;
import java.util.List;
import java.util.RandomAccess;

/**
 *
 * @author pepijn
 */
public final class CollectionUtils {
    private CollectionUtils() {
        // Prevent instantiation
    }
    
    public static BitSet bitSetOf(int... values) {
        BitSet bitSet = new BitSet(values[values.length - 1] + 1);
        for (int value: values) {
            bitSet.set(value);
        }
        return bitSet;
    }

    /**
     * Creates an unmodifiable and efficient (for {@link RandomAccess} lists) {@link List} which is the concatenation of
     * several other {@link List}s.
     */
    @SafeVarargs
    public static <T> List<T> listOf(final List<? extends T>... lists) {
        final int[] cumulativeSizes = new int[lists.length];
        int runningTotal = 0;
        for (int i = 0; i < lists.length; i++) {
            runningTotal += lists[i].size();
            cumulativeSizes[i] = runningTotal;
        }
        final int totalSize = runningTotal;
        abstract class AbstractRandomAccessList<E> extends AbstractList<E> implements RandomAccess {}
        return new AbstractRandomAccessList<T>() {
            @Override
            public int size() {
                return totalSize;
            }

            @Override
            public T get(int index) {
                if (index < cumulativeSizes[0]) {
                    return lists[0].get(index);
                } else {
                    for (int i = 1; i < cumulativeSizes.length; i++) {
                        if (index < cumulativeSizes[i]) {
                            return lists[i].get(index - cumulativeSizes[i - 1]);
                        }
                    }
                    throw new IndexOutOfBoundsException();
                }
            }
        };
    }

    /**
     * Returns the highest value from an array of {@code int}s, or {@code Integer.MIN_VALUE} if the array is empty.
     */
    public static int max(int[] values) {
        int max = Integer.MIN_VALUE;
        for (int i = 0, valuesLength = values.length; i < valuesLength; i++) {
            if (values[i] > max) {
                max = values[i];
            }
        }
        return max;
    }

    /**
     * Returns the highest value from an array of {@code short}s, interpreted as unsigned values, or
     * {@code Integer.MIN_VALUE} if the array is empty.
     */
    public static int unsignedMax(short[] values) {
        int max = Integer.MIN_VALUE;
        for (int i = 0, valuesLength = values.length; i < valuesLength; i++) {
            if ((values[i] & 0xffff) > max) {
                max = values[i] & 0xffff;
            }
        }
        return max;
    }

    /**
     * Returns the highest value from an array of {@code byte}s, interpreted as unsigned values, or
     * {@code Integer.MIN_VALUE} if the array is empty.
     */
    public static int unsignedMax(byte[] values) {
        int max = Integer.MIN_VALUE;
        for (int i = 0, valuesLength = values.length; i < valuesLength; i++) {
            if ((values[i] & 0xff) > max) {
                max = values[i] & 0xff;
            }
        }
        return max;
    }
}