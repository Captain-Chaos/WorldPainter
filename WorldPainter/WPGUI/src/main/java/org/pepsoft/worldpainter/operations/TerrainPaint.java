/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.operations;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.MapDragControl;
import org.pepsoft.worldpainter.RadiusControl;
import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.WorldPainter;

/**
 *
 * @author pepijn
 */
public class TerrainPaint extends RadiusOperation implements TerrainOperation {
    public TerrainPaint(WorldPainter view, RadiusControl radiusControl, MapDragControl mapDragControl, Terrain terrain) {
        super(terrain.getName(), terrain.getDescription(), view, radiusControl, mapDragControl, 100, "operation.terrainPaint." + terrain.getName());
        setTerrain(terrain);
    }

    @Override
    public final Terrain getTerrain() {
        return terrain;
    }

    @Override
    public final void setTerrain(Terrain terrain) {
        if (terrain == null) {
            throw new NullPointerException();
        }
        this.terrain = terrain;
    }

    @Override
    protected void tick(int centreX, int centreY, boolean inverse, boolean first, float dynamicLevel) {
        Dimension dimension = getDimension();
        dimension.setEventsInhibited(true);
        try {
            int radius = getEffectiveRadius();
            for (int x = centreX - radius; x <= centreX + radius; x++) {
                for (int y = centreY - radius; y <= centreY + radius; y++) {
                    float strength = dynamicLevel * getStrength(centreX, centreY, x, y);
                    if ((strength > 0.95f) || (Math.random() < strength)) {
                        if (inverse) {
                            dimension.applyTheme(x, y);
                        } else {
                            dimension.setTerrainAt(x, y, terrain);
                        }
                    }
                }
            }
        } finally {
            dimension.setEventsInhibited(false);
        }
    }

    private Terrain terrain;
}