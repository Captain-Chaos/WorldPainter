package org.pepsoft.util;

import java.util.*;

/**
 * A dynamically sized, sparse {@link List}.
 *
 * @param <T> The element type of the list.
 */
public class DynamicList<T> extends AbstractList<T> {
    public DynamicList() {
        this(DEFAULT_BUCKET_SIZE);
    }

    public DynamicList(int bucketSize) {
        this.bucketSize = bucketSize;
    }

    public DynamicList(Collection<T> collection) {
        this(collection, DEFAULT_BUCKET_SIZE);
    }

    public DynamicList(Collection<T> collection, int bucketSize) {
        this.bucketSize = bucketSize;
        addAll(collection);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get(int index) {
        int bucketIndex = index / bucketSize;
        Object[] bucket = buckets.get(bucketIndex);
        return (bucket != null) ? (T) bucket[index - bucketIndex * bucketSize] : null;
    }

    @Override
    public int size() {
        return size;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T set(int index, T element) {
        if (index >= size) {
            size = index + 1;
        }
        int bucketIndex = index / bucketSize;
        Object[] bucket = buckets.get(bucketIndex);
        T previousElement;
        if (bucket == null) {
            previousElement = null;
            if (element != null) {
                bucket = new Object[bucketSize];
                bucket[index - bucketIndex * bucketSize] = element;
                buckets.put(bucketIndex, bucket);
            }
        } else {
            previousElement = (T) bucket[index - bucketIndex * bucketSize];
            bucket[index - bucketIndex * bucketSize] = element;
        }
        return previousElement;
    }

    private final int bucketSize;
    private final Map<Integer, Object[]> buckets = new HashMap<>();
    private int size;

    public static final int DEFAULT_BUCKET_SIZE = 16;
}
