package org.jnbt;

/**
 * The <code>TAG_Int_Array</code> tag.
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

    @Override
    public int[] getValue() {
        return value;
    }

    @Override
    public String toString() {
        StringBuilder hex = new StringBuilder();
        for(int i : value) {
            String hexDigits = Integer.toHexString(i).toUpperCase();
            hex.append("0000000".substring(0, 8 - hexDigits.length()));
            hex.append(hexDigits).append(" ");
        }
        String name = getName();
        String append = "";
        if(name != null && !name.equals("")) {
            append = "(\"" + this.getName() + "\")";
        }
        return "TAG_Int_Array" + append + ": " + hex.toString();
    }

    @Override
    public IntArrayTag clone() {
        IntArrayTag clone = (IntArrayTag) super.clone();
        clone.value = value.clone();
        return clone;
    }

    private static final long serialVersionUID = 1L;
}