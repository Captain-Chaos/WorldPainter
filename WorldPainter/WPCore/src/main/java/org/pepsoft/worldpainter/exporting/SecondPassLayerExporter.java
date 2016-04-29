/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.exporting;

import org.pepsoft.worldpainter.Dimension;

import java.awt.*;
import java.util.List;

/**
 * An exporter which will be invoked in a second pass, after all chunks have
 * been generated. This is for exporters which need information from, or make
 * changes to, neighbouring chunks.
 *
 * @author pepijn
 */
public interface SecondPassLayerExporter extends LayerExporter {
    /**
     * Export an area of the map.
     *
     * @param dimension The dimension that is being exported.
     * @param area The area to process.
     * @param exportedArea The area which will actually be exported. May be smaller than <code>area</code>. May be used to for instance avoid objects getting cut off at area boundaries.
     * @param minecraftWorld The {@link MinecraftWorld} to which to export the layer.
     * @return An optional list of fixups which should be executed after all regions have been exported.
     */
    List<Fixup> render(Dimension dimension, Rectangle area, Rectangle exportedArea, MinecraftWorld minecraftWorld);
}