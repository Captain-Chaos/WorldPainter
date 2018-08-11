package org.pepsoft.util;

/**
 * An implementation of {@link AttributeKey} for {@link Integer}-typed values.
 */
public final class IntegerAttributeKey extends AttributeKey<Integer> {
    public IntegerAttributeKey(String key) {
        super(key);
    }

    public IntegerAttributeKey(String key, Integer defaultValue) {
        super(key, defaultValue);
    }

    @Override
    public String toString(Integer value) {
        return value.toString();
    }

    @Override
    public Integer toValue(String str) {
        return Integer.valueOf(str);
    }
}