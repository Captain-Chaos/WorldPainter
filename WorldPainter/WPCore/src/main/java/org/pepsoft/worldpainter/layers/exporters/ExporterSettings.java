/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers.exporters;

import org.pepsoft.worldpainter.HeightTransform;
import org.pepsoft.worldpainter.layers.Layer;

import java.io.Serializable;

/**
 *
 * @author pepijn
 */
public interface ExporterSettings extends Serializable, Cloneable {
    boolean isApplyEverywhere();
    
    Layer getLayer();
    
    /**
     * This is invoked by WorldPainter when the dimension height is transformed (minHeight and/or maxHeight changes,
     * and/or a shift and/or scaling operation applied), so that the layer settings may be adjusted accordingly, if
     * applicable.
     * 
     * <p>The default implementation does nothing.
     */
    default void setMinMaxHeight(int oldMinHeight, int newMinHeight, int oldMaxHeight, int newMaxHeight, HeightTransform transform) {
        // Do nothing
    }
    
    ExporterSettings clone();
}