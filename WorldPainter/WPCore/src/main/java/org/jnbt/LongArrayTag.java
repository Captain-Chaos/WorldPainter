package org.jnbt;

/**
 * The <code>TAG_Long_Array</code> tag.
 */
public final class LongArrayTag extends Tag {
    /**
     * The value.
     */
    private long[] value;

    /**
     * Creates the tag.
     * @param name The name.
     * @param value The value.
     */
    public LongArrayTag(String name, long[] value) {
        super(name);
        this.value = value;
    }

    public long[] getValue() {
        return value;
    }

    @Override
    public String toString() {
        StringBuilder hex = new StringBuilder();
        for(long l: value) {
            String hexDigits = Long.toHexString(l).toUpperCase();
            hex.append("000000000000000".substring(0, 16 - hexDigits.length()));
            hex.append(hexDigits).append(" ");
        }
        String name = getName();
        String append = "";
        if(name != null && !name.equals("")) {
            append = "(\"" + this.getName() + "\")";
        }
        return "TAG_Long_Array" + append + ": " + hex.toString();
    }

    @Override
    public LongArrayTag clone() {
        LongArrayTag clone = (LongArrayTag) super.clone();
        clone.value = value.clone();
        return clone;
    }

    private static final long serialVersionUID = 1L;
}