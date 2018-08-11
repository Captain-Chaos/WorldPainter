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
public class AttributeKey<T> {
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

    /**
     * Get a value for this attribute from a map of string to strings.
     *
     * @param values The map from which to get the value.
     * @return The value of this attribute in the specified map, or the default
     * value if the map does not contain the attribute.
     */
    public T getFromString(Map<String, String> values) {
        return ((values != null) && values.containsKey(key)) ? toValue(values.get(key)) : defaultValue;
    }

    /**
     * Get a value for this attribute from a map of string to native value
     * types.
     *
     * @param values The map from which to get the value.
     * @return The value of this attribute in the specified map, or the default
     * value if the map does not contain the attribute.
     */
    @SuppressWarnings("unchecked") // Responsibility of client
    public T get(Map<String, ?> values) {
        return ((values != null) && values.containsKey(key)) ? (T) values.get(key) : defaultValue;
    }

    /**
     * Convert an instance of {@link T} to a string.
     *
     * @param value The value to convert. Will never be <code>null</code>.
     * @return A string representation of the value.
     */
    public String toString(T value) {
        return value.toString();
    }

    /**
     * Convert a string representation of {@link T} to its native type.
     *
     * <p>This implementation always throws an
     * {@link UnsupportedOperationException}. Subclasses should override this
     * method to implement it.
     *
     * @param str The string representation to convert. Will never be
     *            <code>null</code>.
     * @return A corresponding instance of {@link T}.
     */
    public T toValue(String str) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (! (o instanceof AttributeKey)) return false;

        AttributeKey<?> that = (AttributeKey<?>) o;

        if (!key.equals(that.key)) return false;

        return true;
    }

    @Override
    public final int hashCode() {
        return key.hashCode();
    }

    public final String key;
    public final T defaultValue;
}