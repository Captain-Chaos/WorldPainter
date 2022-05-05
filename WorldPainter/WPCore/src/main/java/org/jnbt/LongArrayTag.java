package org.jnbt;

/**
 * The {@code TAG_Long_Array} tag.
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
        if (value.length <= 4) {
            for (long l: value) {
                String hexDigits = Long.toHexString(l).toUpperCase();
                hex.append("000000000000000", 0, 16 - hexDigits.length());
                hex.append(hexDigits).append(" ");
            }
        } else {
            for (int i = 0; i < 4; i++) {
                if (i != 2) {
                    String hexDigits = Long.toHexString(value[(i <= 2) ? i : (value.length - 1)]).toUpperCase();
                    hex.append("000000000000000", 0, 16 - hexDigits.length());
                    hex.append(hexDigits).append(" ");
                } else {
                    hex.append("(");
                    hex.append(value.length - 3);
                    hex.append(" more) ");
                }
            }
        }
        String name = getName();
        String append = "";
        if ((name != null) && (! name.equals(""))) {
            append = "(\"" + this.getName() + "\")";
        }
        return "TAG_Long_Array" + append + ": " + ((hex.length() > 0) ? hex.substring(0, hex.length() - 1) : "empty");
    }

    @Override
    public LongArrayTag clone() {
        LongArrayTag clone = (LongArrayTag) super.clone();
        clone.value = value.clone();
        return clone;
    }

    private static final long serialVersionUID = 1L;
}