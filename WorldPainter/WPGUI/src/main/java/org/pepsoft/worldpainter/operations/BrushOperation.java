package org.pepsoft.worldpainter.operations;

import org.pepsoft.worldpainter.brushes.Brush;

/**
 * An operation which uses the brush.
 *
 * Created by pepijn on 5-7-15.
 */
public interface BrushOperation extends Operation {
    Brush getBrush();
    void setBrush(Brush brush);
}