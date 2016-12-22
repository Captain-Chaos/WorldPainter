package org.pepsoft.worldpainter.selection;

import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.renderers.LayerRenderer;
import org.pepsoft.worldpainter.layers.renderers.TransparentColourRenderer;

/**
 * A block-level layer which indicates that a block belongs to the selection.
 *
 * <p>Created by Pepijn Schmitz on 03-11-16.
 */
public class SelectionBlock extends Layer {
    public SelectionBlock() {
        super(SelectionBlock.class.getName(), "SelectionBlock", "Selected area with block resolution", DataSize.BIT, 85);
    }

    @Override
    public LayerRenderer getRenderer() {
        return RENDERER;
    }

    public static final SelectionBlock INSTANCE = new SelectionBlock();

    private static final LayerRenderer RENDERER = new TransparentColourRenderer(0xffff00);
}