package org.pepsoft.util;

/**
 * A tuple of two values. Abstract in order to force creation of specific
 * semantic subclasses.
 *
 * <p>Created by Pepijn on 26-2-2017.
 */
public abstract class Pair<T, U> {
    protected Pair(T value1, U value2) {
        this.value1 = value1;
        this.value2 = value2;
    }

    protected final T getValue1() {
        return value1;
    }

    protected final U getValue2() {
        return value2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Pair<?, ?> pair = (Pair<?, ?>) o;

        if (value1 != null ? !value1.equals(pair.value1) : pair.value1 != null) return false;
        return value2 != null ? value2.equals(pair.value2) : pair.value2 == null;
    }

    @Override
    public int hashCode() {
        int result = value1 != null ? value1.hashCode() : 0;
        result = 31 * result + (value2 != null ? value2.hashCode() : 0);
        return result;
    }

    private final T value1;
    private final U value2;
}