/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.layers;

import java.util.List;
import org.pepsoft.worldpainter.plugins.LayerEditorProvider;
import org.pepsoft.worldpainter.plugins.WPPluginManager;

/**
 *
 * @author Pepijn Schmitz
 */
public class LayerEditorManager {
    private LayerEditorManager() {
        providers = WPPluginManager.getInstance().getPlugins(LayerEditorProvider.class);
    }
    
    public <L extends Layer> LayerEditor<L> createEditor(Class<L> layerType) {
        for (LayerEditorProvider provider: providers) {
            LayerEditor<L> editor = provider.createLayerEditor(layerType);
            if (editor != null) {
                return editor;
            }
        }
        return null;
    }
    
    public static LayerEditorManager getInstance() {
        return INSTANCE;
    }
    
    private final List<LayerEditorProvider> providers;
    
    private static final LayerEditorManager INSTANCE = new LayerEditorManager();
}