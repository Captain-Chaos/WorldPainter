package org.pepsoft.worldpainter.operations;

import org.pepsoft.worldpainter.brushes.Brush;

/**
 * A WorldPainter {@link Operation} which uses a {@link Brush}. WorldPainter
 * will invoke {@link #setBrush(Brush)} automatically prior to activation to set
 * the currently selected brush by the user.
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
}