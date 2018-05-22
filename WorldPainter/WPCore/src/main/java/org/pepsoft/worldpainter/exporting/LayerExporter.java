/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.exporting;

import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.exporters.ExporterSettings;

/**
 * An exporter of {@link Layer}s. The exporter should also implement at least
 * one of {@link FirstPassLayerExporter}, {@link SecondPassLayerExporter}, and
 * optionally also {@link IncidentalLayerExporter}.
 *
 * <p><strong>Please note:</strong> a layer exporter may be invoked for
 * different areas on different threads simultaneously, so it must be thread-
 * safe!
 *
 * @author pepijn
 */
public interface LayerExporter {
    Layer getLayer();

    /**
     * Set the exporter settings to use for exporting the layer.
     *
     * @param settings The settings to use for exporting the layer.
     */
    void setSettings(ExporterSettings settings);
}