package org.pepsoft.worldpainter.layers.tunnel;

import org.pepsoft.worldpainter.Dimension;

/**
 * A {@link Dimension} of which the terrain height follows the floor of a
 * particular Custom Cave/Tunnel Layer.
 *
 * Created by pepijn on 31-7-15.
 */
public class TunnelFloorDimension extends TunnelDimension {
    public TunnelFloorDimension(Dimension dimension, TunnelLayer layer) {
        super(dimension, layer);
    }

    @Override
    protected float determineHeight(boolean inTunnel, int tunnelFloorLevel, int tunnelRoofLevel, float realHeight) {
        return (inTunnel && (tunnelFloorLevel >= tunnelRoofLevel)) ? tunnelFloorLevel : realHeight;
    }
}