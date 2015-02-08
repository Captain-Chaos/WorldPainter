/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers.renderers;

/**
 *
 * @author pepijn
 */
public interface ByteLayerRenderer extends LayerRenderer {
    int getPixelColour(int x, int y, int underlyingColour, int value);
}