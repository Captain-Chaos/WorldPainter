package org.pepsoft.worldpainter.layers.tunnel;

import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.heightMaps.NoiseHeightMap;

import java.awt.*;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.*;

/**
 * A {@link Dimension} of which the terrain height follows the floor of a
 * particular Custom Cave/Tunnel Layer.
 *
 * Created by pepijn on 31-7-15.
 */
public class TunnelFloorDimension extends RODelegatingDimension {
    public TunnelFloorDimension(Dimension dimension, TunnelLayer layer) {
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
        minZ = dimension.isBottomless() ? 0 : 1;
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
        if (dimension.getBitLayerValueAt(layer, x, y)) {
            // Potentially in cave/tunnel
            final int terrainHeight = dimension.getIntHeightAt(x, y);
            int actualFloorLevel = TunnelLayerExporter.calculateLevel(floorMode, floorLevel, terrainHeight, floorMin, floorMax, minZ, maxZ, (floorNoise != null) ? ((int) floorNoise.getHeight(x, y) - floorNoiseOffset) : 0);
            int actualRoofLevel = TunnelLayerExporter.calculateLevel(roofMode, roofLevel, terrainHeight, roofMin, roofMax, minZ, maxZ, (roofNoise != null) ? ((int) roofNoise.getHeight(x, y) - roofNoiseOffset) : 0);
            if (actualRoofLevel <= actualFloorLevel) {
                return dimension.getHeightAt(x, y);
            }
            final float distanceToWall = dimension.getDistanceToEdge(layer, x, y, maxWallDepth) - 1;
            final int floorLedgeHeight = TunnelLayerExporter.calculateLedgeHeight(floorWallDepth, distanceToWall);
            final int roofLedgeHeight = TunnelLayerExporter.calculateLedgeHeight(roofWallDepth, distanceToWall);
            actualFloorLevel += floorLedgeHeight;
            actualRoofLevel -= roofLedgeHeight;
            if (actualRoofLevel <= actualFloorLevel) {
                return dimension.getHeightAt(x, y);
            } else {
                return actualFloorLevel;
            }
        } else {
            // Definitely outside cave/tunnel
            return dimension.getHeightAt(x, y);
        }
    }

    @Override
    public int getIntHeightAt(int x, int y, int defaultValue) {
        float height = getHeightAt(x, y);
        return (height == Float.MIN_VALUE) ? -1 : (int) (height + 0.5f);
    }

    @Override
    public Tile getTile(Point coords) {
        Reference<TunnelFloorTile> cachedTileRef = tileCache.get(coords);
        TunnelFloorTile cachedTile = (cachedTileRef != null) ? cachedTileRef.get() : null;
        if (cachedTile == null) {
            Tile tile = dimension.getTile(coords);
            if (tile != null) {
                cachedTile = new TunnelFloorTile(tile);
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
            Reference<TunnelFloorTile> cachedTileRef = tileCache.get(new Point(tile.getX(), tile.getY()));
            TunnelFloorTile cachedTile = (cachedTileRef != null) ? cachedTileRef.get() : null;
            if (cachedTile != null) {
                wrappedTiles.add(cachedTile);
            } else {
                wrappedTiles.add(new TunnelFloorTile(tile));
            }
        }
        return wrappedTiles;
    }

    final TunnelLayer layer;
    final TunnelLayer.Mode floorMode, roofMode;
    final int floorLevel, floorMin, floorMax, minZ, maxZ, roofLevel, roofMin, roofMax, floorWallDepth, roofWallDepth, maxWallDepth;
    final NoiseHeightMap floorNoise, roofNoise;
    final int floorNoiseOffset, roofNoiseOffset;
    private final Map<Point, Reference<TunnelFloorTile>> tileCache = new HashMap<>();

    /**
     * A {@link Tile} of which the terrain height follows the floor of a particular
     * Custom Cave/Tunnel Layer.
     *
     * Created by pepijn on 31-7-15.
     */
    public class TunnelFloorTile extends RODelegatingTile {
        public TunnelFloorTile(Tile tile) {
            super(tile);
            xOffset = tile.getX() << Constants.TILE_SIZE_BITS;
            yOffset = tile.getY() << Constants.TILE_SIZE_BITS;
        }

        @Override
        public float getHeight(int x, int y) {
            if (tile.getBitLayerValue(layer, x, y)) {
                // Potentially in cave/tunnel
                final int terrainHeight = tile.getIntHeight(x, y);
                int actualFloorLevel = TunnelLayerExporter.calculateLevel(floorMode, floorLevel, terrainHeight, floorMin, floorMax, minZ, maxZ, (floorNoise != null) ? ((int) floorNoise.getHeight(xOffset | x, yOffset | y) - floorNoiseOffset) : 0);
                int actualRoofLevel = TunnelLayerExporter.calculateLevel(roofMode, roofLevel, terrainHeight, roofMin, roofMax, minZ, maxZ, (roofNoise != null) ? ((int) roofNoise.getHeight(xOffset | x, yOffset | y) - roofNoiseOffset) : 0);
                if (actualRoofLevel <= actualFloorLevel) {
                    return tile.getHeight(x, y);
                }
                final float distanceToWall = dimension.getDistanceToEdge(layer, xOffset | x, yOffset | y, maxWallDepth) - 1;
                final int floorLedgeHeight = TunnelLayerExporter.calculateLedgeHeight(floorWallDepth, distanceToWall);
                final int roofLedgeHeight = TunnelLayerExporter.calculateLedgeHeight(roofWallDepth, distanceToWall);
                actualFloorLevel += floorLedgeHeight;
                actualRoofLevel -= roofLedgeHeight;
                if (actualRoofLevel <= actualFloorLevel) {
                    return tile.getHeight(x, y);
                } else {
                    return actualFloorLevel;
                }
            } else {
                // Definitely outside cave/tunnel
                return tile.getHeight(x, y);
            }
        }

        @Override
        public int getIntHeight(int x, int y) {
            return (int) (getHeight(x, y) + 0.5f);
        }

        final int xOffset, yOffset;
    }
}