/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.viewer;

import java.awt.Rectangle;

/**
 * Describes a layer of tiles which can be viewed
 * 
 * @author pepijn
 */
public interface Layer {
    /**
     * The short name of the layer.
     * 
     * @return The short name of the layer.
     */
    String getName();
    
    /**
     * The long description of the layer.
     * 
     * @return The long description of the layer.
     */
    String getDescription();
    
    /**
     * The bounds of the layer.
     * 
     * @return The bounds of the layer.
     */
    Rectangle getBounds();
}