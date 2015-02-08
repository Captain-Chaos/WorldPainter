/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.exporting;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.exporters.ExporterSettings;

/**
 *
 * @author pepijn
 */
public interface LayerExporter<L extends Layer> {
    L getLayer();

    /**
     * Set the exporter settings to use for future invocations of
     * {@link #render(Dimension, Tile, ChunkImpl)}.
     *
     * @param settings The settings to use for exporting the layer.
     */
    void setSettings(ExporterSettings<L> settings);
}