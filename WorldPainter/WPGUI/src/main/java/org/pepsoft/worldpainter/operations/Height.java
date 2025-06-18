/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.operations;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.WorldPainter;
import org.pepsoft.worldpainter.panels.DefaultFilter;

import javax.swing.*;
import java.awt.*;

/**
 *
 * @author pepijn
 */
public class Height extends RadiusOperation {
    public Height(WorldPainter view) {
        super("Height", "Raise or lower the terrain", view, 100, "operation.height");
    }

    @Override
    public JPanel getOptionsPanel() {
        return optionsPanel;
    }

//    @Override
//    protected void altPressed() {
//        dimensionSnapshot = getDimension().getSnapshot();
//    }
//
//    @Override
//    protected void altReleased() {
//        dimensionSnapshot = null;
//    }

    @Override
    protected void tick(int centreX, int centreY, boolean inverse, boolean first, float dynamicLevel) {
        final float adjustment = (float) Math.pow(dynamicLevel * getLevel() * 2, 2.0);
        final Dimension dimension = getDimension();
        if (dimension == null) {
            // Probably some kind of race condition
            return;
        }
        final float minZ, maxZ;
        if (getFilter() instanceof DefaultFilter) {
            final DefaultFilter filter = (DefaultFilter) getFilter();
            if (filter.getAboveLevel() != Integer.MIN_VALUE) {
                minZ = Math.max(filter.getAboveLevel(), dimension.getMinHeight());
            } else {
                minZ = dimension.getMinHeight();
            }
            if (filter.getBelowLevel() != Integer.MIN_VALUE) {
                maxZ = Math.min(filter.getBelowLevel(), dimension.getMaxHeight());
            } else {
                maxZ = dimension.getMaxHeight() - 1;
            }
        } else {
            minZ = dimension.getMinHeight();
            maxZ = dimension.getMaxHeight() - 1;
        }
        boolean applyTheme = options.isApplyTheme();
        dimension.setEventsInhibited(true);
        try {
            final Rectangle boundingBox = getBoundingBox();
            for (int x = centreX + boundingBox.x; x < centreX + boundingBox.x + boundingBox.width; x++) {
                for (int y = centreY + boundingBox.y; y <= centreY + boundingBox.y + boundingBox.height; y++) {
                    final float currentHeight = dimension.getHeightAt(x, y);
                    final float targetHeight = inverse ? Math.max(currentHeight - adjustment, minZ) : Math.min(currentHeight + adjustment, maxZ);
                    final float strength = getFullStrength(centreX, centreY, x, y);
                    if (strength > 0.0f) {
                        final float newHeight = strength * targetHeight + (1 - strength) * currentHeight;
                        if (inverse ? (newHeight < currentHeight) : (newHeight > currentHeight)) {
                            dimension.setHeightAt(x, y, newHeight);
                            if (applyTheme) {
                                dimension.applyTheme(x, y);
                            }
                        }
                    }
                }
            }
        } finally {
            dimension.setEventsInhibited(false);
        }
    }

    private final TerrainShapingOptions<Height> options = new TerrainShapingOptions<>();
    private final TerrainShapingOptionsPanel optionsPanel = new TerrainShapingOptionsPanel("Height", "<ul><li>Left-click to raise the terrain<li>Right-click to lower the terrain</ul>", options);
//    private Dimension dimensionSnapshot;
}