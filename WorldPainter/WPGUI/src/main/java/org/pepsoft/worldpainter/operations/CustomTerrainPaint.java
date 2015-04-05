package org.pepsoft.worldpainter.operations;

import org.pepsoft.worldpainter.*;

import javax.swing.*;

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
            // Doing this in the same event causes the proper activation of the
            // operation to fail somehow, so do it later:
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    App.getInstance().showCustomTerrainButtonPopup(customTerrainIndex);
//                    if (Terrain.getCustomMaterial(customTerrainIndex) == null) {
//                        // User did not select a custom material; don't activate the
//                        // paint operation and deactivate the button
//                        App.getInstance().deselectTool();
//                        return;
//                    }
                    // TODO: is it necessary to detect when the user doesn't select a material?
                }
            });
        }
        super.activate();
    }

    private final int customTerrainIndex;
}