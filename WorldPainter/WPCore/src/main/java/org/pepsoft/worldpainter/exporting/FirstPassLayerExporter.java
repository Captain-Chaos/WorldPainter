/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.exporting;

import org.pepsoft.minecraft.Chunk;
import org.pepsoft.worldpainter.HeightMap;
import org.pepsoft.worldpainter.Tile;

/**
 * An exporter which will be invoked when the chunk is first generated. Should
 * not need any information from neighbouring chunks.
 *
 * @author pepijn
 */
public interface FirstPassLayerExporter extends LayerExporter {
    /**
     * Render the layer to the chunk. If the layer operates underground it can go all the way down to bedrock.
     *
     * @param tile           The tile that is currently being rendered.
     * @param chunk          The chunk that is currently being rendered.
     */
    void render(Tile tile, Chunk chunk);

    /**
     * Render the chunk.
     *
     * <p>The default implementation forwards to {@link #render(Tile, Chunk)}}, ignoring {@code minHeightField}, and can
     * be used by implementations to which it doesn't apply.
     *
     * @param tile           The tile that is currently being rendered.
     * @param chunk          The chunk that is currently being rendered.
     * @param minHeightField A field of minimum heights, below which the layer should not be rendered.
     */
    default void render(Tile tile, Chunk chunk, HeightMap minHeightField) {
        render(tile, chunk);
    }
}