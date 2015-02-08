package org.pepsoft.worldpainter.operations;

import org.pepsoft.worldpainter.App;
import org.pepsoft.worldpainter.MapDragControl;
import org.pepsoft.worldpainter.RadiusControl;
import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.WorldPainter;

/**
 * @author SchmitzP
 */
public class CustomTerrainPaint extends TerrainPaint {
    public CustomTerrainPaint(WorldPainter view, RadiusControl radiusControl, MapDragControl mapDragControl, int customTerrainIndex) {
        super(view, radiusControl, mapDragControl, Terrain.getCustomTerrain(customTerrainIndex));
        this.customTerrainIndex = customTerrainIndex;
    }

    @Override
    protected void activate() {
        if (Terrain.getCustomMaterial(customTerrainIndex) == null) {
            App.getInstance().editCustomMaterial(customTerrainIndex);
            if (Terrain.getCustomMaterial(customTerrainIndex) == null) {
                // User did not select a custom material; don't activate the
                // paint operation and deactivate the button
                App.getInstance().deselectTool();
                return;
            }
        }
        super.activate();
    }

    private final int customTerrainIndex;
}