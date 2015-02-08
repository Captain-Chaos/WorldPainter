/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.exporting;

import java.awt.Rectangle;
import java.util.List;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.layers.Layer;

/**
 * An exporter which will be invoked in a second pass, after all chunks have
 * been generated. This is for exporters which need information from, or make
 * changes to, neighbouring chunks.
 *
 * @author pepijn
 */
public interface SecondPassLayerExporter<L extends Layer> extends LayerExporter<L> {
    /**
     * Render a chunk. This is synonymous with invoking
     * <code>render(<em>world</em>, <em>tile</em>, <em>chunkX</em>, <em>chunkY</em>, <em>minecraftWorld</em>, null)</code>.
     *
     * @param dimension The dimension that is being rendered.
     * @param area The area to render.
     * @param exportedArea The area which will actually be exported. May be smaller than <code>area</code>. May be used to for instance avoid objects getting cut off at area boundaries.
     * @param minecraftWorld The chunk cache to be used for getting the chunks.
     * @return An optional list of fixups which should be executed after all regions have been exported.
     */
    List<Fixup> render(Dimension dimension, Rectangle area, Rectangle exportedArea, MinecraftWorld minecraftWorld);
}