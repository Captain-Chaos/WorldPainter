/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.operations;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.MapDragControl;
import org.pepsoft.worldpainter.RadiusControl;
import org.pepsoft.worldpainter.WorldPainterView;
import org.pepsoft.worldpainter.layers.Bo2Layer;
import org.pepsoft.worldpainter.layers.CombinedLayer;
import org.pepsoft.worldpainter.layers.CustomLayer;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.groundcover.GroundCoverLayer;
import org.pepsoft.worldpainter.layers.pockets.UndergroundPocketsLayer;
import org.pepsoft.worldpainter.layers.tunnel.TunnelLayer;

/**
 *
 * @author pepijn
 */
public class LayerPaint extends RadiusOperation {
    public LayerPaint(WorldPainterView view, RadiusControl radiusControl, MapDragControl mapDragControl, Layer layer) {
        super(layer.getName(), layer.getDescription(), view, radiusControl, mapDragControl, 100, createStatisticsKey(layer));
        this.layer = layer;
    }

    public final Layer getLayer() {
        return layer;
    }

    @Override
    protected void tick(int centreX, int centreY, boolean inverse, boolean first, float dynamicLevel) {
        Dimension dimension = getDimension();
        dimension.setEventsInhibited(true);
        try {
            int radius = getEffectiveRadius();
            for (int x = centreX - radius; x <= centreX + radius; x++) {
                for (int y = centreY - radius; y <= centreY + radius; y++) {
                    switch (layer.getDataSize()) {
                        case BIT:
                        case BIT_PER_CHUNK:
                            float strength = dynamicLevel * (inverse ? getFullStrength(centreX, centreY, x, y) : getStrength(centreX, centreY, x, y));
                            if ((strength > 0.95f) || (Math.random() < strength)) {
                                dimension.setBitLayerValueAt(layer, x, y, ! inverse);
                            }
                            break;
                        case NIBBLE:
                            int currentValue = dimension.getLayerValueAt(layer, x, y);
                            strength = dynamicLevel * (inverse ? getFullStrength(centreX, centreY, x, y) : getStrength(centreX, centreY, x, y));
                            if (strength != 0f) {
                                int targetValue = inverse ? (15 - (int) (strength * 14 + 1)) : (int) (strength * 14 + 1);
                                if (inverse ? (targetValue < currentValue) : (targetValue > currentValue)) {
                                    dimension.setLayerValueAt(layer, x, y, targetValue);
                                }
                            }
                            break;
                        default:
                            throw new UnsupportedOperationException("Don't know how to paint data size " + layer.getDataSize());
                    }
                }
            }
        } finally {
            dimension.setEventsInhibited(false);
        }
    }

    private static String createStatisticsKey(Layer layer) {
        StringBuilder sb = new StringBuilder();
        sb.append("operation.layerPaint.");
        if (layer instanceof Bo2Layer) {
            sb.append("custom.objects.");
        } else if (layer instanceof GroundCoverLayer) {
            sb.append("custom.groundCover.");
        } else if (layer instanceof UndergroundPocketsLayer) {
            sb.append("custom.undergroundPockets.");
        } else if (layer instanceof CombinedLayer) {
            sb.append("custom.combined.");
        } else if (layer instanceof TunnelLayer) {
            sb.append("custom.cavesAndTunnels.");
        } else if (layer instanceof CustomLayer) {
            sb.append("custom.unknown.");
        }
        sb.append(layer.getName().replaceAll("[ \\t\\n\\x0B\\f\\r\\.]", ""));
        return sb.toString();
    }

    private final Layer layer;
}