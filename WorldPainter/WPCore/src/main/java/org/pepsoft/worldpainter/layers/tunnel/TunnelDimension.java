package org.pepsoft.worldpainter.layers.tunnel;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.heightMaps.NoiseHeightMap;

import java.awt.*;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

// TODO make TunnelDimension much more intelligent, so that "distance to edge" can work and the normal exporting can be
//  used rather than the incidental exporting
abstract class TunnelDimension extends RODelegatingDimension {
    TunnelDimension(Dimension dimension, TunnelLayer layer) {
        super(dimension);
        this.layer = layer;
        floorMode = layer.getFloorMode();
        roofMode = layer.getRoofMode();
        floorWallDepth = layer.getFloorWallDepth();
        roofWallDepth = layer.getRoofWallDepth();
        floorLevel = layer.getFloorLevel();
        roofLevel = layer.getRoofLevel();
        maxWallDepth = Math.max(floorWallDepth, roofWallDepth) + 1;
        floorMin = layer.getFloorMin();
        floorMax = layer.getFloorMax();
        roofMin = layer.getRoofMin();
        roofMax = layer.getRoofMax();
        minZ = dimension.getMinHeight() + (dimension.isBottomless() ? 0 : 1);
        maxZ = dimension.getMaxHeight() - 1;
        if (layer.getFloorNoise() != null) {
            floorNoise = new NoiseHeightMap(layer.getFloorNoise(), TunnelLayerExporter.FLOOR_NOISE_SEED_OFFSET);
            floorNoiseOffset = layer.getFloorNoise().getRange();
        } else {
            floorNoise = null;
            floorNoiseOffset = 0;
        }
        if (layer.getRoofNoise()!= null) {
            roofNoise = new NoiseHeightMap(layer.getRoofNoise(), TunnelLayerExporter.ROOF_NOISE_SEED_OFFSET);
            roofNoiseOffset = layer.getRoofNoise().getRange();
        } else {
            roofNoise = null;
            roofNoiseOffset = 0;
        }
    }

    @Override
    public float getHeightAt(int x, int y) {
        final float terrainHeight = dimension.getHeightAt(x, y);
        if (dimension.getBitLayerValueAt(layer, x, y)) {
            // Potentially in cave/tunnel
            final int intTerrainHeight = Math.round(terrainHeight);
            int actualFloorLevel = TunnelLayerExporter.calculateLevel(floorMode, floorLevel, intTerrainHeight, floorMin, floorMax, minZ, maxZ, (floorNoise != null) ? ((int) floorNoise.getHeight(x, y) - floorNoiseOffset) : 0);
            int actualRoofLevel = TunnelLayerExporter.calculateLevel(roofMode, roofLevel, intTerrainHeight, roofMin, roofMax, minZ, maxZ, (roofNoise != null) ? ((int) roofNoise.getHeight(x, y) - roofNoiseOffset) : 0);
            if (actualRoofLevel <= actualFloorLevel) {
                return determineHeight(true, actualFloorLevel, actualRoofLevel, terrainHeight);
            }
            final float distanceToWall = dimension.getDistanceToEdge(layer, x, y, maxWallDepth) - 1;
            final int floorLedgeHeight = TunnelLayerExporter.calculateLedgeHeight(floorWallDepth, distanceToWall);
            final int roofLedgeHeight = TunnelLayerExporter.calculateLedgeHeight(roofWallDepth, distanceToWall);
            actualFloorLevel += floorLedgeHeight;
            actualRoofLevel -= roofLedgeHeight;
            return determineHeight(true, actualFloorLevel, actualRoofLevel, terrainHeight);
        } else {
            // Definitely outside cave/tunnel
            return determineHeight(false, Integer.MAX_VALUE, Integer.MIN_VALUE, terrainHeight);
        }
    }

    @Override
    public int getIntHeightAt(int x, int y, int defaultValue) {
        float height = getHeightAt(x, y);
        return (height == Float.MIN_VALUE) ? Integer.MIN_VALUE : Math.round(height);
    }

    @Override
    public Tile getTile(Point coords) {
        Reference<TunnelTile> cachedTileRef = tileCache.get(coords);
        TunnelTile cachedTile = (cachedTileRef != null) ? cachedTileRef.get() : null;
        if (cachedTile == null) {
            Tile tile = dimension.getTile(coords);
            if (tile != null) {
                cachedTile = new TunnelTile(tile);
                tileCache.put(coords, new SoftReference<>(cachedTile));
            }
        }
        return cachedTile;
    }

    @Override
    public Collection<? extends Tile> getTiles() {
        Collection<? extends Tile> tiles = dimension.getTiles();
        java.util.List<Tile> wrappedTiles = new ArrayList<>(tiles.size());
        for (Tile tile: tiles) {
            Reference<TunnelTile> cachedTileRef = tileCache.get(new Point(tile.getX(), tile.getY()));
            TunnelTile cachedTile = (cachedTileRef != null) ? cachedTileRef.get() : null;
            if (cachedTile != null) {
                wrappedTiles.add(cachedTile);
            } else {
                wrappedTiles.add(new TunnelTile(tile));
            }
        }
        return wrappedTiles;
    }
    
    protected abstract float determineHeight(boolean inTunnel, int tunnelFloorLevel, int tunnelRoofLevel, float realHeight);
    
    private final TunnelLayer layer;
    private final TunnelLayer.Mode floorMode, roofMode;
    private final int floorLevel, floorMin, floorMax, minZ, maxZ, roofLevel, roofMin, roofMax, floorWallDepth, roofWallDepth, maxWallDepth;
    private final NoiseHeightMap floorNoise, roofNoise;
    private final int floorNoiseOffset, roofNoiseOffset;
    private final Map<Point, Reference<TunnelTile>> tileCache = new HashMap<>();

    /**
     * A {@link Tile} of which the terrain height follows the floor of a particular
     * Custom Cave/Tunnel Layer.
     *
     * Created by pepijn on 31-7-15.
     */
    final class TunnelTile extends RODelegatingTile {
        TunnelTile(Tile tile) {
            super(tile);
            xOffset = tile.getX() << Constants.TILE_SIZE_BITS;
            yOffset = tile.getY() << Constants.TILE_SIZE_BITS;
        }

        @Override
        public float getHeight(int x, int y) {
            final float terrainHeight = tile.getHeight(x, y);
            if (tile.getBitLayerValue(layer, x, y)) {
                // Potentially in cave/tunnel
                final int intTerrainHeight = Math.round(terrainHeight);
                int actualFloorLevel = TunnelLayerExporter.calculateLevel(floorMode, floorLevel, intTerrainHeight, floorMin, floorMax, minZ, maxZ, (floorNoise != null) ? ((int) floorNoise.getHeight(xOffset | x, yOffset | y) - floorNoiseOffset) : 0);
                int actualRoofLevel = TunnelLayerExporter.calculateLevel(roofMode, roofLevel, intTerrainHeight, roofMin, roofMax, minZ, maxZ, (roofNoise != null) ? ((int) roofNoise.getHeight(xOffset | x, yOffset | y) - roofNoiseOffset) : 0);
                if (actualRoofLevel <= actualFloorLevel) {
                    return determineHeight(true, actualFloorLevel, actualRoofLevel, terrainHeight);
                }
                final float distanceToWall = dimension.getDistanceToEdge(layer, xOffset | x, yOffset | y, maxWallDepth) - 1;
                final int floorLedgeHeight = TunnelLayerExporter.calculateLedgeHeight(floorWallDepth, distanceToWall);
                final int roofLedgeHeight = TunnelLayerExporter.calculateLedgeHeight(roofWallDepth, distanceToWall);
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

        private final int xOffset, yOffset;
    }
}