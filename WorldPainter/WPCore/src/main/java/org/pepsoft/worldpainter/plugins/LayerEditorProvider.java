/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.plugins;

import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.LayerEditor;

/**
 * A provider of {@link LayerEditor}s.
 * 
 * @author Pepijn Schmitz
 */
public interface LayerEditorProvider extends Plugin {
    /**
     * Create a layer editor for the specified layer type and target platform.
     * May return <code>null</code> in case this provider does not support the
     * specified layer type.
     * 
     * @param <L> The type of layer for which an editor is requested.
     * @param platform The target platform for which to configure the layer.
     * @param layerType The class object of the layer type for which an editor
     *     is requested.
     * @return A new editor for the specified layer type, or <code>null</code>
     *     if this provider does not support the specified layer type.
     */
    <L extends Layer> LayerEditor<L> createLayerEditor(Platform platform, Class<L> layerType);
}