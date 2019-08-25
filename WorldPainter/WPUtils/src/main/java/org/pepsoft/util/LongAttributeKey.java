package org.pepsoft.util;

/**
 * An implementation of {@link AttributeKey} for {@link Long}-typed values.
 */
public final class LongAttributeKey extends AttributeKey<Long> {
    public LongAttributeKey(String key) {
        super(key);
    }

    public LongAttributeKey(String key, Long defaultValue) {
        super(key, defaultValue);
    }

    @Override
    public String toString(Long value) {
        return value.toString();
    }

    @Override
    public Long toValue(String str) {
        return Long.valueOf(str);
    }
}