/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.operations;

import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.layers.FloodWithLava;

import javax.swing.*;

/**
 *
 * @author pepijn
 */
public class Sponge extends RadiusOperation {
    public Sponge(WorldPainterView view, RadiusControl radiusControl, MapDragControl mapDragControl) {
        super("Sponge", "Dry up or reset water and lava", view, radiusControl, mapDragControl, 100, "operation.sponge");
    }

    @Override
    public JPanel getOptionsPanel() {
        return OPTIONS_PANEL;
    }

    @Override
    protected void tick(int centreX, int centreY, boolean inverse, boolean first, float dynamicLevel) {
        final Dimension dimension = getDimension();
        if (dimension == null) {
            // Probably some kind of race condition
            return;
        }
        final int waterHeight, minHeight = dimension.getMinHeight();
        final TileFactory tileFactory = dimension.getTileFactory();
        if (tileFactory instanceof HeightMapTileFactory) {
            waterHeight = ((HeightMapTileFactory) tileFactory).getWaterHeight();
        } else {
            // If we can't determine the water height disable the inverse
            // functionality, which resets to the default water height
            waterHeight = -1;
        }
        dimension.setEventsInhibited(true);
        try {
            final int radius = getEffectiveRadius();
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    if (getStrength(centreX, centreY, centreX + dx, centreY + dy) != 0f) {
                        if (inverse) {
                            if (waterHeight != -1) {
                                dimension.setWaterLevelAt(centreX + dx, centreY + dy, waterHeight);
                                dimension.setBitLayerValueAt(FloodWithLava.INSTANCE, centreX + dx, centreY + dy, false);
                            }
                        } else {
                            dimension.setWaterLevelAt(centreX + dx, centreY + dy, minHeight);
                        }
                    }
                }
            }
        } finally {
            dimension.setEventsInhibited(false);
        }
    }

    private static final JPanel OPTIONS_PANEL = new StandardOptionsPanel("Sponge", "<ul><li>Left-click to remove water and lava<li>Right-click to reset to the default fluid type and height</ul>");
}