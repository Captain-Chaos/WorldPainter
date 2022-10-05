package org.pepsoft.worldpainter.layers.tunnel;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.RODelegatingDimension;
import org.pepsoft.worldpainter.RODelegatingTile;
import org.pepsoft.worldpainter.Tile;

import java.awt.*;

import static org.pepsoft.worldpainter.Constants.TILE_SIZE_BITS;

// TODO make TunnelDimension much more intelligent, so that "distance to edge" can work and the normal exporting can be
//  used rather than the incidental exporting
abstract class TunnelDimension extends RODelegatingDimension<TunnelDimension.TunnelTile> {
    TunnelDimension(Dimension dimension, TunnelLayer layer, TunnelLayerHelper helper) {
        super(dimension);
        this.layer = layer;
        this.helper = helper;
        minZ = dimension.getMinHeight() + (dimension.isBottomless() ? 0 : 1);
        maxZ = dimension.getMaxHeight() - 1;
    }

    @Override
    public float getHeightAt(int x, int y) {
        final float terrainHeight = dimension.getHeightAt(x, y);
        if (dimension.getBitLayerValueAt(layer, x, y)) {
            // Potentially in cave/tunnel
            final int intTerrainHeight = Math.round(terrainHeight);
            int actualFloorLevel = helper.calculateFloorLevel(x, y, intTerrainHeight, minZ, maxZ);
            int actualRoofLevel = helper.calculateRoofLevel(x, y, intTerrainHeight, minZ, maxZ, actualFloorLevel);
            if (actualRoofLevel <= actualFloorLevel) {
                return determineHeight(true, actualFloorLevel, actualRoofLevel, terrainHeight);
            }
            final int floorLedgeHeight = helper.calculateBottomLedgeHeight(x, y);
            final int roofLedgeHeight = helper.calculateTopLedgeHeight(x, y);
            actualFloorLevel += floorLedgeHeight;
            actualRoofLevel -= roofLedgeHeight;
            return determineHeight(true, actualFloorLevel, actualRoofLevel, terrainHeight);
        } else {
            // Definitely outside cave/tunnel
            return determineHeight(false, Integer.MAX_VALUE, Integer.MIN_VALUE, terrainHeight);
        }
    }

    @Override
    public float getHeightAt(Point coords) {
        return getHeightAt(coords.x, coords.y);
    }

    @Override
    public int getWaterLevelAt(int x, int y) {
        final float terrainHeight = dimension.getHeightAt(x, y);
        if (dimension.getBitLayerValueAt(layer, x, y)) {
            // Potentially in cave/tunnel
            final int intTerrainHeight = Math.round(terrainHeight);
            int actualFloorLevel = helper.calculateFloorLevel(x, y, intTerrainHeight, minZ, maxZ);
            int actualRoofLevel = helper.calculateRoofLevel(x, y, intTerrainHeight, minZ, maxZ, actualFloorLevel);
            if (actualRoofLevel <= actualFloorLevel) {
                return dimension.getWaterLevelAt(x, y);
            }
            final int floorLedgeHeight = helper.calculateBottomLedgeHeight(x, y);
            final int roofLedgeHeight = helper.calculateTopLedgeHeight(x, y);
            actualFloorLevel += floorLedgeHeight;
            actualRoofLevel -= roofLedgeHeight;
            if (actualRoofLevel <= actualFloorLevel) {
                return dimension.getWaterLevelAt(x, y);
            } else {
                final int floodLevel = layer.getFloodLevel();
                return (floodLevel == Integer.MIN_VALUE) ? getMinHeight() : floodLevel;
            }
        } else {
            // Definitely outside cave/tunnel
            return dimension.getWaterLevelAt(x, y);
        }
    }

    @Override
    public int getWaterLevelAt(Point coords) {
        return getWaterLevelAt(coords.x, coords.y);
    }

    @Override
    public int getIntHeightAt(int x, int y) {
        return Math.round(getHeightAt(x, y));
    }

    @Override
    public int getIntHeightAt(int x, int y, int defaultValue) {
        float height = getHeightAt(x, y);
        return (height == -Float.MAX_VALUE) ? defaultValue : Math.round(height);
    }

    @Override
    public int getIntHeightAt(Point coords) {
        return getIntHeightAt(coords.x, coords.y);
    }

    @Override
    protected TunnelTile wrapTile(Tile tile) {
        return new TunnelTile(tile);
    }

    protected abstract float determineHeight(boolean inTunnelLayer, int tunnelFloorLevel, int tunnelRoofLevel, float realHeight);
    
    private final TunnelLayer layer;
    private final int minZ, maxZ;
    private final TunnelLayerHelper helper;

    /**
     * A {@link Tile} of which the terrain height follows the floor of a particular
     * Custom Cave/Tunnel Layer.
     *
     * Created by pepijn on 31-7-15.
     */
    final class TunnelTile extends RODelegatingTile {
        TunnelTile(Tile tile) {
            super(tile);
        }

        @Override
        public float getHeight(int x, int y) {
            final float terrainHeight = tile.getHeight(x, y);
            if (tile.getBitLayerValue(layer, x, y)) {
                // Potentially in cave/tunnel
                final int intTerrainHeight = Math.round(terrainHeight);
                final int worldX = (tile.getX() << TILE_SIZE_BITS) | x, worldY = (tile.getY() << TILE_SIZE_BITS) | y;
                int actualFloorLevel = helper.calculateFloorLevel(worldX, worldY, intTerrainHeight, minZ, maxZ);
                int actualRoofLevel = helper.calculateRoofLevel(worldX, worldY, intTerrainHeight, minZ, maxZ, actualFloorLevel);
                if (actualRoofLevel <= actualFloorLevel) {
                    return determineHeight(true, actualFloorLevel, actualRoofLevel, terrainHeight);
                }
                final int floorLedgeHeight = helper.calculateBottomLedgeHeight(worldX, worldY);
                final int roofLedgeHeight = helper.calculateTopLedgeHeight(worldX, worldY);
                actualFloorLevel += floorLedgeHeight;
                actualRoofLevel -= roofLedgeHeight;
                return determineHeight(true, actualFloorLevel, actualRoofLevel, terrainHeight);
            } else {
                // Definitely outside cave/tunnel
                return determineHeight(false, Integer.MAX_VALUE, Integer.MIN_VALUE, terrainHeight);
            }
        }

        @Override
        public int getIntHeight(int x, int y) {
            return Math.round(getHeight(x, y));
        }
    }
}