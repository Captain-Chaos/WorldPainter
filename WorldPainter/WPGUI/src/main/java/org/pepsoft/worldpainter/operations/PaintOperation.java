package org.pepsoft.worldpainter.operations;

import org.pepsoft.worldpainter.painting.Paint;

/**
 * Created by pepijn on 28-05-15.
 */
public interface PaintOperation extends Operation {
    Paint getPaint();
    void setPaint(Paint paint);
}