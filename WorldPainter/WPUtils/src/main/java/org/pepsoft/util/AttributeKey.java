package org.pepsoft.util;

import java.io.Serializable;
import java.util.Map;

/**
 * A utility class for getting a typed attribute value with a default
 * conveniently. This class is {@link Serializable}, but only if the concrete
 * type of all the values is serializable!
 *
 * @param <T> The value type of the attribute.
 */
public final class AttributeKey<T> {
    public AttributeKey(String key) {
        this(key, null);
    }

    public AttributeKey(String key, T defaultValue) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        this.key = key;
        this.defaultValue = defaultValue;
    }

    @SuppressWarnings("unchecked") // Responsibility of client
    public T get(Map<String, ?> values) {
        return ((values != null) && values.containsKey(key)) ? (T) values.get(key) : defaultValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AttributeKey<?> that = (AttributeKey<?>) o;

        if (!key.equals(that.key)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    public final String key;
    public final T defaultValue;
}