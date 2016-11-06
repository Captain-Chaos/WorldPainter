/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

import org.pepsoft.util.swing.TiledImageViewer;

/**
 * A Swing component for viewing a WorldPainter {@link Dimension}.
 *
 * @author pepijn
 */
public abstract class WorldPainterView extends TiledImageViewer {
    protected WorldPainterView() {
        // Do nothing
    }

    protected WorldPainterView(boolean leftClickDrags, int threads, boolean paintCentre) {
        super(leftClickDrags, threads, paintCentre);
    }

    /**
     * Get the currently displayed {@link Dimension}.
     *
     * @return The currently displayed {@link Dimension}.
     */
    public abstract Dimension getDimension();

    /**
     * Set the {@link Dimension} to display.
     *
     * @param dimension The {@link Dimension} to display.
     */
    public abstract void setDimension(Dimension dimension);

    /**
     * Updates the status bar, if any, with information current for a specific
     * location on the dimension.
     *
     * @param x The X coordinate in world coordinates for which to update the
     *          status bar.
     * @param y The T coordinate in world coordinates for which to update the
     *          status bar.
     */
    public abstract void updateStatusBar(int x, int y);

    /**
     * Determine whether the brush radius is currently being displayed.
     *
     * @return <code>true</code> if the brush radius is currently being
     * displayed.
     */
    public abstract boolean isDrawBrush();

    /**
     * Set whether the brush radius should be displayed.
     *
     * @param drawBrush Whether the brush radius should be displayed.
     */
    public abstract void setDrawBrush(boolean drawBrush);

    /**
     * Determine whether view updates are currently inhibited, meaning that
     * changes to the dimension are not reflected on the screen.
     *
     * @return Whether view updates are currently inhibited.
     */
    public abstract boolean isInhibitUpdates();

    /**
     * Set whether view updates should be inhibited, meaning the changes to the
     * dimension are not reflected on the screen. This may provide improved
     * performance while a dimension is being changed intensively.
     *
     * <p>It is the responsibility of the view to make sure that the current
     * state of the dimension is reflected on the screen again after this is set
     * to <code>false</code>.
     *
     * @param inhibitUpdates Whether view updates should be inhibited.
     */
    public abstract void setInhibitUpdates(boolean inhibitUpdates);
}