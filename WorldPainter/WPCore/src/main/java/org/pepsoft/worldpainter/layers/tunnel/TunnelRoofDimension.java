package org.pepsoft.worldpainter.layers.tunnel;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;

/**
 * A {@link Dimension} of which the terrain height follows the roof of a particular Custom Cave/Tunnel Layer. The
 * heights are inverted, so that this dimension may be used in combination with an inverted {@link MinecraftWorld} to
 * export cave roof layers upside down.
 *
 * Created by pepijn on 31-7-15.
 */
public class TunnelRoofDimension extends TunnelDimension {
    public TunnelRoofDimension(Dimension dimension, TunnelLayer layer) {
        super(dimension, layer);
        reflectionPoint = dimension.getMaxHeight() + dimension.getMinHeight() - 1;
    }

    @Override
    public int getWaterLevelAt(int x, int y) {
        return getMinHeight();
    }

    @Override
    protected float determineHeight(boolean inTunnelLayer, int tunnelFloorLevel, int tunnelRoofLevel, float realHeight) {
        return reflectionPoint - ((inTunnelLayer && (tunnelRoofLevel > tunnelFloorLevel)) ? tunnelRoofLevel + 1 : realHeight);
    }

    private final int reflectionPoint;
}