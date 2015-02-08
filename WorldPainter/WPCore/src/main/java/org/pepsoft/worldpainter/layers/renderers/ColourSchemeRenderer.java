/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers.renderers;

import org.pepsoft.worldpainter.ColourScheme;

/**
 *
 * @author pepijn
 */
public interface ColourSchemeRenderer extends LayerRenderer {
    void setColourScheme(ColourScheme colourScheme);
}
