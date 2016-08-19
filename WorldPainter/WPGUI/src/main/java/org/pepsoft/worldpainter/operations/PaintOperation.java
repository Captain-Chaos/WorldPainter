package org.pepsoft.worldpainter.operations;

import org.pepsoft.worldpainter.painting.Paint;

/**
 * A WorldPainter {@link Operation} which uses a {@link Paint}. WorldPainter
 * will invoke {@link #setPaint(Paint)} automatically prior to activation to set
 * the currently selected paint by the user.
 *
 * <p>Created by pepijn on 28-05-15.
 */
public interface PaintOperation extends Operation {
    /**
     * Get the currently configured paint.
     *
     * @return The currently configured paint.
     */
    Paint getPaint();

    /**
     * Set the paint to use for operations.
     *
     * @param paint The paint to use for operations.
     */
    void setPaint(Paint paint);
}