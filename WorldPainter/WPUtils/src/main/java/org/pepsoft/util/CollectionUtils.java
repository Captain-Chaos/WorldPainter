/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.util;

import java.util.AbstractList;
import java.util.BitSet;
import java.util.List;

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
     * Creates an unmodifiable and efficient {@link List} which is the concatenation of several other {@link List}s.
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
        return new AbstractList<T>() {
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
}