package org.pepsoft.worldpainter.layers;

public class NotPresentBlock extends Layer {
    private NotPresentBlock() {
        super(NotPresentBlock.class.getName(), "Not Present", "Mark blocks that are to be considered as not present", DataSize.BIT, false, 91);
    }

    public static final NotPresentBlock INSTANCE = new NotPresentBlock();

    private static final long serialVersionUID = 1L;
}