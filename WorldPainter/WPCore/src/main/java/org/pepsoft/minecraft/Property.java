package org.pepsoft.minecraft;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * An accessor helper for a property of a {@link Material}.
 *
 * @param <V> The value type of the property.
 */
public final class Property<V> {
    public Property(String name, Class<V> type) {
        this.name = name;
        this.type = type;
        Method method = null;
        if (type != String.class) {
            try {
                method = type.getMethod("parse", String.class);
            } catch (NoSuchMethodException e) {
                try {
                    method = type.getMethod("valueOf", String.class);
                } catch (NoSuchMethodException e2) {
                    throw new IllegalArgumentException("Type " + type + " has no parse(String) or valueOf(String) methods");
                }
            }
        }
        valueOfMethod = method;
    }

    @SuppressWarnings("unchecked") // Responsibility of client
    public V fromString(String str) {
        if (valueOfMethod == null) {
            return (V) str;
        } else {
            try {
                return (V) valueOfMethod.invoke(null, str);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e.getClass().getSimpleName() + " when trying to parse\"" + str + "\" to " + type, e);
            }
        }
    }

    public final String name;
    public final Class<V> type;
    private final Method valueOfMethod;
}
