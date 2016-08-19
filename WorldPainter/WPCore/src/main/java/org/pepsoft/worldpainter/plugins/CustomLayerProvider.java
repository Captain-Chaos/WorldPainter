package org.pepsoft.worldpainter.plugins;

import org.pepsoft.worldpainter.layers.CustomLayer;

import java.util.List;

/**
 * Created by Pepijn Schmitz on 19-08-16.
 */
public interface CustomLayerProvider extends Plugin {
    List<Class<? extends CustomLayer>> getCustomLayers();
}