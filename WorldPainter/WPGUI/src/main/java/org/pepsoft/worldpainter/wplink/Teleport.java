package org.pepsoft.worldpainter.wplink;

import org.pepsoft.simplerpc.Message;
import org.pepsoft.worldpainter.WorldPainter;
import org.pepsoft.worldpainter.WorldPainterView;
import org.pepsoft.worldpainter.operations.MouseOrTabletOperation;

public class Teleport extends MouseOrTabletOperation {
    public Teleport(WorldPainterView view) {
        super("Teleport", "Teleport the player to the selected location in Minecraft via the WPLink plugin", view, "operation.Teleport");
    }

    @Override
    protected void tick(int centreX, int centreY, boolean inverse, boolean first, float dynamicLevel) {
        if (first) {
            ((WorldPainter) getView()).rpcClient.sendMessage(new Message("TELEPORT", 1, "x", centreX, "z", centreY));
        }
    }
}
