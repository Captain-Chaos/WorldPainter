/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers;

import org.pepsoft.worldpainter.DefaultPlugin;
import org.pepsoft.worldpainter.plugins.LayerProvider;
import org.pepsoft.worldpainter.plugins.WPPluginManager;

import java.util.*;

/**
 *
 * @author pepijn
 */
public final class LayerManager {
    private LayerManager() {
        layers = new ArrayList<>();
        List<LayerProvider> layerProviders = WPPluginManager.getInstance().getPlugins(LayerProvider.class);
        // Put our own layers first
        for (LayerProvider layerProvider: layerProviders) {
            if (layerProvider instanceof DefaultPlugin) {
                layers.addAll(layerProvider.getLayers());
            }
        }
        for (LayerProvider layerProvider: layerProviders) {
            if (! (layerProvider instanceof DefaultPlugin)) {
                layers.addAll(layerProvider.getLayers());
            }
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