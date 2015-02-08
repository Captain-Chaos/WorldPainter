/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

import org.pepsoft.util.swing.TiledImageViewer;

/**
 *
 * @author pepijn
 */
public abstract class WorldPainterView extends TiledImageViewer {
    public WorldPainterView() {
        // Do nothing
    }

    public WorldPainterView(boolean leftClickDrags, int threads, boolean paintCentre) {
        super(leftClickDrags, threads, paintCentre);
    }
    
    public abstract Dimension getDimension();

    public abstract void setDimension(Dimension dimension);

    public abstract void updateStatusBar(int x, int y);
    
    public abstract boolean isDrawRadius();
    
    public abstract void setDrawRadius(boolean drawRadius);
    
    public abstract boolean isInhibitUpdates();
    
    public abstract void setInhibitUpdates(boolean inhibitUpdates);
}