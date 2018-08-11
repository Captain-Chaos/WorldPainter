package org.pepsoft.util;

/**
 * An implementation of {@link AttributeKey} for {@link String}-typed values.
 */
public final class StringAttributeKey extends AttributeKey<String> {
    public StringAttributeKey(String key) {
        super(key);
    }

    public StringAttributeKey(String key, String defaultValue) {
        super(key, defaultValue);
    }

    @Override
    public String toString(String value) {
        return value;
    }

    @Override
    public String toValue(String str) {
        return str;
    }
}