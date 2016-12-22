package org.pepsoft.worldpainter.selection;

import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.renderers.LayerRenderer;
import org.pepsoft.worldpainter.layers.renderers.TransparentColourRenderer;

/**
 * A chunk-level layer which indicates that an entire chunk belongs to the
 * selection.
 *
 * <p>Created by Pepijn Schmitz on 03-11-16.
 */
public class SelectionChunk extends Layer {
    public SelectionChunk() {
        super(SelectionChunk.class.getName(), "SelectionChunk", "Selected area with chunk resolution", DataSize.BIT_PER_CHUNK, 85);
    }

    @Override
    public LayerRenderer getRenderer() {
        return RENDERER;
    }

    public static final SelectionChunk INSTANCE = new SelectionChunk();

    private static final LayerRenderer RENDERER = new TransparentColourRenderer(0xffff00);
}
