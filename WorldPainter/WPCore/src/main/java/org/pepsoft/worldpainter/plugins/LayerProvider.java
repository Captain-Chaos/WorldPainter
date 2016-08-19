/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.plugins;

import org.pepsoft.worldpainter.layers.Layer;

import java.util.List;

/**
 *
 * @author pepijn
 */
public interface LayerProvider extends Plugin {
    List<Layer> getLayers();
}