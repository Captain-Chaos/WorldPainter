package org.jnbt;

/**
 * The {@code TAG_Int_Array} tag.
 */
public final class IntArrayTag extends Tag {
    /**
     * The value.
     */
    private int[] value;

    /**
     * Creates the tag.
     * @param name The name.
     * @param value The value.
     */
    public IntArrayTag(String name, int[] value) {
        super(name);
        this.value = value;
    }

    public int[] getValue() {
        return value;
    }

    @Override
    public String toString() {
        StringBuilder hex = new StringBuilder();
        if (value.length <= 8) {
            for (int i: value) {
                String hexDigits = Integer.toHexString(i).toUpperCase();
                hex.append("00000000", 0, 8 - hexDigits.length());
                hex.append(hexDigits).append(" ");
            }
        } else {
            for (int i = 0; i < 8; i++) {
                if (i != 6) {
                    String hexDigits = Integer.toHexString(value[(i <= 6) ? i : (value.length - 1)]).toUpperCase();
                    hex.append("00000000", 0, 8 - hexDigits.length());
                    hex.append(hexDigits).append(" ");
                } else {
                    hex.append("(");
                    hex.append(value.length - 7);
                    hex.append(" more) ");
                }
            }
        }
        String name = getName();
        String append = "";
        if ((name != null) && (! name.equals(""))) {
            append = "(\"" + this.getName() + "\")";
        }
        return "TAG_Int_Array" + append + ": " + ((hex.length() > 0) ? hex.substring(0, hex.length() - 1) : "empty");
    }

    @Override
    public IntArrayTag clone() {
        IntArrayTag clone = (IntArrayTag) super.clone();
        clone.value = value.clone();
        return clone;
    }

    private static final long serialVersionUID = 1L;
}