/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.operations;

import java.awt.Point;
import javax.swing.JOptionPane;

import org.pepsoft.worldpainter.Constants;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.World2;
import org.pepsoft.worldpainter.WorldPainter;

/**
 *
 * @author pepijn
 */
public class SetSpawnPoint extends MouseOrTabletOperation {
    public SetSpawnPoint(WorldPainter view) {
        super("Spawn", "Change the spawn point", view, "operation.setSpawnPoint", "spawn");
    }

    @Override
    protected void tick(int centreX, int centreY, boolean inverse, boolean first, float dynamicLevel) {
        if (first) {
            Dimension dimension = getDimension();
            if ((dimension.getDim() != Constants.DIM_NORMAL) && (dimension.getDim() != Constants.DIM_NORMAL_CEILING)) {
                throw new IllegalArgumentException("Cannot set spawn point on dimensions other than 0");
            }
            World2 world = dimension.getWorld();
            int spawnHeight = dimension.getIntHeightAt(centreX, centreY);
            if (spawnHeight == -1) {
                // No tile
                if (JOptionPane.showConfirmDialog(getView(), "<html>Are you sure you want to set the spawn <em>outside</em> the boundary of the world?</html>") != JOptionPane.OK_OPTION) {
                    return;
                }
            }
            world.setSpawnPoint(new Point(centreX, centreY));
        }
    }
}