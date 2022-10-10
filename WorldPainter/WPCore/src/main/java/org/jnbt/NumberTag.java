package org.jnbt;

/**
 * An NBT tag which contains a numerical value, with methods to extract it as
 * various primitive types regardless of the underlying type.
 */
public abstract class NumberTag extends Tag {
    /**
     * Creates the tag with the specified name.
     *
     * @param name The name.
     */
    public NumberTag(String name) {
        super(name);
    }

    /**
     * Get the value as an {@code int}. Performs the same operation as casting
     * the value to an {@code int} (meaning that if the value is too large it
     * will overflow without throwing an exception).
     *
     * @return The value as an {@code int}.
     */
    public abstract int intValue();

    public long longValue() {
        return intValue();
    }

    private static final long serialVersionUID = 4199522300454384098L;
}