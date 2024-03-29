/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers.renderers;

import org.pepsoft.worldpainter.layers.Layer;

/**
 * An editor view renderer of a {@link Layer} with data size of {@link Layer.DataSize#NIBBLE NIBBLE}.
 *
 * @author pepijn
 */
public interface NibbleLayerRenderer extends LayerRenderer {
    int getPixelColour(int x, int y, int underlyingColour, int value);
}