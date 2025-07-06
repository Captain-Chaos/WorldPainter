package org.pepsoft.worldpainter.operations;

import org.pepsoft.worldpainter.brushes.Brush;

/**
 * A WorldPainter {@link Operation} which uses a {@link Brush}. WorldPainter will invoke {@link #setBrush(Brush)} and
 * {@link #setRadius(int)} automatically prior to activation to set the currently selected brush and brush size by the
 * user.
 *
 * <p>Created by pepijn on 5-7-15.
 */
public interface BrushOperation extends Operation {
    /**
     * Get the currently configured brush.
     *
     * @return The currently configured brush.
     */
    Brush getBrush();

    /**
     * Set the brush to use for operations.
     *
     * @param brush The brush to use for operations.
     */
    void setBrush(Brush brush);

    /**
     * Get the currently configured radius.
     *
     * @return The currently configured radius.
     */
    int getRadius();

    /**
     * Set the radius. <strong>Note:</strong> it is the responsibility of the implementor to propagate this value to the
     * configured brush!
     *
     * @param radius The radius to use for operations.
     */
    void setRadius(int radius);
}