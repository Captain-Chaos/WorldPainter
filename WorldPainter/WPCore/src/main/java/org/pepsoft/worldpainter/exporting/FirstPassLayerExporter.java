/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.exporting;

import org.pepsoft.minecraft.Chunk;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.layers.Layer;

/**
 * An exporter which will be invoked when the chunk is first generated. Should
 * not need any information from neighbouring chunks.
 *
 * @author pepijn
 */
public interface FirstPassLayerExporter<L extends Layer> extends LayerExporter<L> {
    /**
     * Render the chunk.
     *
     * @param dimension The dimension that is being rendered.
     * @param tile The tile that is currently being rendered.
     * @param chunk The chunk that is currently being rendered.
     */
    void render(Dimension dimension, Tile tile, Chunk chunk);
}