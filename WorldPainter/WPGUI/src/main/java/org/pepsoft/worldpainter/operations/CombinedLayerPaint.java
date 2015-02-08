/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.operations;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.MapDragControl;
import org.pepsoft.worldpainter.RadiusControl;
import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.WorldPainterView;
import org.pepsoft.worldpainter.layers.Biome;
import org.pepsoft.worldpainter.layers.CombinedLayer;

/**
 *
 * @author pepijn
 */
public class CombinedLayerPaint extends LayerPaint {
    public CombinedLayerPaint(WorldPainterView view, RadiusControl radiusControl, MapDragControl mapDragControl, CombinedLayer layer) {
        super(view, radiusControl, mapDragControl, layer);
    }
    
    @Override
    protected void tick(int centreX, int centreY, boolean inverse, boolean first, float dynamicLevel) {
        final Dimension dimension = getDimension();
        final CombinedLayer layer = (CombinedLayer) getLayer();
        final Terrain terrain = layer.getTerrain();
        final int biome = layer.getBiome();
        dimension.setEventsInhibited(true);
        try {
            int radius = getEffectiveRadius();
            for (int x = centreX - radius; x <= centreX + radius; x++) {
                for (int y = centreY - radius; y <= centreY + radius; y++) {
                    int currentValue = dimension.getLayerValueAt(layer, x, y);
                    float strength = dynamicLevel * (inverse ? getFullStrength(centreX, centreY, x, y) : getStrength(centreX, centreY, x, y));
                    if (strength != 0f) {
                        int targetValue = inverse ? (15 - (int) (strength * 14 + 1)) : (int) (strength * 14 + 1);
                        if (inverse ? (targetValue < currentValue) : (targetValue > currentValue)) {
                            dimension.setLayerValueAt(layer, x, y, targetValue);
                        }
                        if ((terrain != null) && ((strength > 0.95f) || (Math.random() < strength))) {
                            if (inverse) {
                                dimension.applyTheme(x, y);
                            } else {
                                dimension.setTerrainAt(x, y, terrain);
                            }
                        }
                        if ((biome != -1) && ((strength > 0.95f) || (Math.random() < strength))) {
                            if (inverse) {
                                dimension.setLayerValueAt(Biome.INSTANCE, x, y, 255);
                            } else {
                                dimension.setLayerValueAt(Biome.INSTANCE, x, y, biome);
                            }
                        }
                    }
                }
            }
        } finally {
            dimension.setEventsInhibited(false);
        }
    }
}