package org.pepsoft.worldpainter.layers.renderers;

import java.io.Serializable;

public interface IndexedLayerRenderer<T extends Serializable> extends LayerRenderer {
    int getPixelColour(int x, int y, int underlyingColour, T value);
}
