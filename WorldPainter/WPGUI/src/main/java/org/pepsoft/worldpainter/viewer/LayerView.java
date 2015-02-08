/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.viewer;

import java.awt.Rectangle;
import javax.swing.JComponent;

/**
 * A component responsible for painting a single layer in the view.
 *
 * @author pepijn
 */
public abstract class LayerView extends JComponent {
    public LayerView(Layer layer) {
        this.layer = layer;
    }

    public final Layer getLayer() {
        return layer;
    }
    
    public abstract void setVisibleRegion(Rectangle visibleRegion);
    
    public abstract void start();
    
    public abstract void shutdown();
    
    protected final Layer layer;
}