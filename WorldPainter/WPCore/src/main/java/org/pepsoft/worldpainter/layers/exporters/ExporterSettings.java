/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers.exporters;

import org.pepsoft.worldpainter.layers.Layer;

import java.io.Serializable;

/**
 *
 * @author pepijn
 */
public interface ExporterSettings extends Serializable, Cloneable {
    boolean isApplyEverywhere();
    
    Layer getLayer();
    
    ExporterSettings clone();
}