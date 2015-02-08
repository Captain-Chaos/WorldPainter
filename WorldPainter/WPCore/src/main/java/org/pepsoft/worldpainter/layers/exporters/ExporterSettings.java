/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers.exporters;

import java.io.Serializable;
import org.pepsoft.worldpainter.layers.Layer;

/**
 *
 * @author pepijn
 */
public interface ExporterSettings<L extends Layer> extends Serializable, Cloneable {
    boolean isApplyEverywhere();
    
    L getLayer();
    
    ExporterSettings<L> clone();
}