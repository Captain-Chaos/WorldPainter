/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.operations;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Dimension.Anchor;
import org.pepsoft.worldpainter.World2;
import org.pepsoft.worldpainter.WorldPainter;

import javax.swing.*;
import java.awt.*;

import static org.pepsoft.worldpainter.Constants.DIM_NORMAL;
import static org.pepsoft.worldpainter.Dimension.Role.DETAIL;

/**
 *
 * @author pepijn
 */
public class SetSpawnPoint extends MouseOrTabletOperation {
    public SetSpawnPoint(WorldPainter view) {
        super("Spawn", "Change the spawn point", view, "operation.setSpawnPoint", "spawn");
    }

    @Override
    public JPanel getOptionsPanel() {
        return OPTIONS_PANEL;
    }

    @Override
    protected void tick(int centreX, int centreY, boolean inverse, boolean first, float dynamicLevel) {
        if (first) {
            final Dimension dimension = getDimension();
            if (dimension == null) {
                // Probably some kind of race condition
                return;
            }
            final Anchor anchor = dimension.getAnchor();
            if (anchor.dim != DIM_NORMAL) {
                throw new IllegalArgumentException("Cannot set spawn point on dimensions other than DIM_NORMAL");
            }
            World2 world = dimension.getWorld();
            int spawnHeight = dimension.getIntHeightAt(centreX, centreY);
            if (spawnHeight == Integer.MIN_VALUE) {
                // No tile
                if (JOptionPane.showConfirmDialog(getView(), "<html>Are you sure you want to set the spawn <em>outside</em> the boundary of the world?</html>") != JOptionPane.OK_OPTION) {
                    return;
                }
            }
            world.setSpawnPoint(new Point(centreX, centreY));
            world.setSpawnPointDimension((anchor.role == DETAIL) ? null : anchor);
        }
    }

    private static final JPanel OPTIONS_PANEL = new StandardOptionsPanel("Spawn", "<p>Click to set the location of the initial spawn point");
}