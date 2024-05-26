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
        synchronized (WorldPainterView.class) {
            if (instance == null) {
                instance = this;
            } else {
                throw new IllegalStateException("WorldPainterView instance already exists");
            }
        }
    }

    protected WorldPainterView(boolean leftClickDrags, boolean paintCentre) {
        super(leftClickDrags, paintCentre);
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
     * @return {@code true} if the brush radius is currently being
     * displayed.
     */
    public abstract boolean isDrawBrush();

    /**
     * Set whether the brush radius should be displayed.
     *
     * @param drawBrush Whether the brush radius should be displayed.
     */
    public abstract void setDrawBrush(boolean drawBrush);

    public abstract MapDragControl getMapDragControl();

    public abstract RadiusControl getRadiusControl();

    /**
     * Get the single instance of {@link WorldPainterView}, if any. In headless mode this will return {@code null}.
     */
    public static WorldPainterView getInstance() {
        return instance;
    }

    private static WorldPainterView instance;
}