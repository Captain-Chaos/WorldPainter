/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.pepsoft.worldpainter.plugins.LayerProvider;
import org.pepsoft.worldpainter.plugins.WPPluginManager;

/**
 *
 * @author pepijn
 */
public final class LayerManager {
    private LayerManager() {
        layers = new ArrayList<>();
        List<LayerProvider> layerProviders = WPPluginManager.getInstance().getPlugins(LayerProvider.class);
        for (LayerProvider layerProvider: layerProviders) {
            layers.addAll(layerProvider.getLayers());
        }
        for (Layer layer: layers) {
            layersByName.put(layer.getName(), layer);
        }
    }
    
    public List<Layer> getLayers() {
        return Collections.unmodifiableList(layers);
    }
    
    public Layer getLayer(String name) {
        return layersByName.get(name);
    }
    
    public static LayerManager getInstance() {
        return INSTANCE;
    }
    
    private final List<Layer> layers;
    private final Map<String, Layer> layersByName = new HashMap<>();
    
    private static final LayerManager INSTANCE = new LayerManager();
}