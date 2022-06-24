/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.exporting;

import org.pepsoft.worldpainter.layers.Layer;

/**
 * An exporter of {@link Layer}s. The exporter should also implement at least one of {@link FirstPassLayerExporter},
 * {@link SecondPassLayerExporter}, and optionally also {@link IncidentalLayerExporter}.
 *
 * <p>Layer exporters are created separately per dimension and per parallelly exported region within a dimension. They
 * do not need to be thread-safe and may keep state, e.g. between multiple phases.
 *
 * @author pepijn
 */
public interface LayerExporter {
    Layer getLayer();
}