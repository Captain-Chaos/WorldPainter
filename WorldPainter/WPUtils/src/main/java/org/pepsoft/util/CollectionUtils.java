/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.util;

import java.util.*;

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

    /**
     * Prepends a {@code null} to a collection.
     *
     * @param collection The collection to which to prepend a {@code null}.
     * @return A list containing a {@code null} and then the contents of {@code collection}.
     * @param <T> The type of collection.
     */
    public static <T> List<T> nullAnd(Collection<T> collection) {
        final List<T> list = new ArrayList<>(collection.size() + 1);
        list.add(null);
        list.addAll(collection);
        return list;
    }

    /**
     * Null-safe method for copying a {@link List}.
     *
     * @param list The list to copy.
     * @return A new {@link ArrayList} with the contents of the provided list, or {@code null} if {@code list} was null.
     * @param <E> The element type of the list.
     */
    public static <E> ArrayList<E> copyOf(List<E> list) {
        return (list != null) ? new ArrayList<>(list) : null;
    }

    /**
     * Null-safe method for copying a {@link Set}.
     *
     * @param set The set to copy.
     * @return A new {@link HashSet} with the contents of the provided set, or {@code null} if {@code set} was null.
     * @param <E> The element type of the set.
     */
    public static <E> HashSet<E> copyOf(Set<E> set) {
        return (set != null) ? new HashSet<>(set) : null;
    }

    /**
     * Null-safe method for copying a {@link Map}.
     *
     * @param map The map to copy.
     * @return A new {@link HashMap} with the contents of the provided map, or {@code null} if {@code map} was null.
     * @param <K> The key type of the map.
     * @param <V> The value type of the map.
     */
    public static <K,V> HashMap<K,V> copyOf(Map<K,V> map) {
        return (map != null) ? new HashMap<>(map) : null;
    }
}