package org.pepsoft.worldpainter.exporting;

import org.pepsoft.util.MathUtils;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.layers.FloodWithLava;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.NotPresent;
import org.pepsoft.worldpainter.layers.ReadOnly;
import org.pepsoft.worldpainter.layers.exporters.ExporterSettings;

import java.awt.*;
import java.util.List;
import java.util.*;

import static org.pepsoft.worldpainter.Constants.*;

/**
 * A {@link Dimension} which combines multiple layered {@code Dimensions}s into one, returning data from the first
 * dimension in which the chunk is present (it exists and is not marked NotPresent or ReadOnly).
 *
 * <p><strong>Note</strong> that not all operations are implemented. Only those needed for exporting are guaranteed to
 * be implemented. Others may throw an {@link UnsupportedOperationException}.
 */
class FlatteningDimension extends RODelegatingDimension<FlatteningDimension.FlatteningTile> {
    public FlatteningDimension(Dimension... dimensions) {
        super(dimensions[0]);
        this.dimensions = dimensions.clone();
        int lowestX = Integer.MAX_VALUE, highestX = Integer.MIN_VALUE, lowestY = Integer.MAX_VALUE, highestY = Integer.MIN_VALUE;
        Rectangle extent = dimensions[0].getExtent();
        for (Dimension dimension: dimensions) {
            tileCoords.addAll(dimension.getTileCoords());
            if (dimension.getLowestX() < lowestX) {
                lowestX = dimension.getLowestX();
            }
            if (dimension.getHighestX() > highestX) {
                highestX = dimension.getHighestX();
            }
            if (dimension.getLowestY() < lowestY) {
                lowestY = dimension.getLowestY();
            }
            if (dimension.getHighestY() > highestY) {
                highestY = dimension.getHighestY();
            }
            minimumLayers.addAll(dimension.getMinimumLayers());
            layerSettings.putAll(dimension.getAllLayerSettings());
            if (extent != null) {
                final Rectangle dimExtent = dimension.getExtent();
                if (dimExtent != null) {
                    extent = extent.union(dimExtent);
                } else {
                    extent = null;
                }
            }
        }
        this.lowestX = lowestX;
        this.highestX = highestX;
        this.lowestY = lowestY;
        this.highestY = highestY;
        this.extent = extent;
        minHeight = dimensions[0].getMinHeight();
        maxHeight = dimensions[0].getMaxHeight();
    }

    @Override
    public int getTileCount() {
        return tileCoords.size();
    }

    @Override
    public int getIntHeightAt(int x, int y, int defaultValue) {
        final Tile tile = getTile(x >> TILE_SIZE_BITS, y >> TILE_SIZE_BITS);
        return (tile != null) ? tile.getIntHeight(x & TILE_SIZE_MASK, y & TILE_SIZE_MASK) : defaultValue;
    }

    @Override
    public float getHeightAt(int x, int y) {
        final Tile tile = getTile(x >> TILE_SIZE_BITS, y >> TILE_SIZE_BITS);
        return (tile != null) ? tile.getHeight(x & TILE_SIZE_MASK, y & TILE_SIZE_MASK) : Float.MIN_VALUE;
    }

    @Override
    public int getRawHeightAt(int x, int y) {
        final Tile tile = getTile(x >> TILE_SIZE_BITS, y >> TILE_SIZE_BITS);
        return (tile != null) ? tile.getRawHeight(x & TILE_SIZE_MASK, y & TILE_SIZE_MASK) : Integer.MIN_VALUE;
    }

    @Override
    public Set<Terrain> getAllTerrains() {
        if (allTerrains == null) {
            allTerrains = new HashSet<>();
            for (Dimension dimension: dimensions) {
                allTerrains.addAll(dimension.getAllTerrains());
            }
        }
        return allTerrains;
    }

    @Override
    public int getWaterLevelAt(int x, int y) {
        final Tile tile = getTile(x >> TILE_SIZE_BITS, y >> TILE_SIZE_BITS);
        return (tile != null) ? tile.getWaterLevel(x & TILE_SIZE_MASK, y & TILE_SIZE_MASK) : Integer.MIN_VALUE;
    }

    @Override
    public int getLayerValueAt(Layer layer, int x, int y) {
        final Tile tile = getTile(x >> TILE_SIZE_BITS, y >> TILE_SIZE_BITS);
        return (tile != null) ? tile.getLayerValue(layer, x & TILE_SIZE_MASK, y & TILE_SIZE_MASK) : layer.getDefaultValue();
    }

    @Override
    public boolean getBitLayerValueAt(Layer layer, int x, int y) {
        final Tile tile = getTile(x >> TILE_SIZE_BITS, y >> TILE_SIZE_BITS);
        return (tile != null) && tile.getBitLayerValue(layer, x & TILE_SIZE_MASK, y & TILE_SIZE_MASK);
    }

    @Override
    public float getDistanceToEdge(Layer layer, int x, int y, float maxDistance) {
        return doGetDistanceToEdge(layer, x, y, maxDistance);
    }

    @Override
    public Set<Layer> getAllLayers(boolean applyCombinedLayers) {
        // TODO this is wrong in principle, since layers from lower dimensions might actually be entirely obscured by
        //  higher dimensions. This should not cause errors or faults, but it is not maximally efficient
        if (applyCombinedLayers) {
            if (allLayersCombinedApplied == null) {
                allLayersCombinedApplied = new HashSet<>();
                for (Dimension dimension: dimensions) {
                    allLayersCombinedApplied.addAll(dimension.getAllLayers(true));
                }
            }
            return allLayersCombinedApplied;
        } else {
            if (allLayers == null) {
                allLayers = new HashSet<>();
                for (Dimension dimension: dimensions) {
                    allLayers.addAll(dimension.getAllLayers(false));
                }
            }
            return allLayers;
        }
    }

    @Override
    public boolean containsOneOf(Layer... layers) {
        for (Layer layer: layers) {
            if (getAllLayers(false).contains(layer)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<Layer> getMinimumLayers() {
        return minimumLayers;
    }

    @Override
    public int getHeight() {
        return highestY - lowestY + 1;
    }

    @Override
    public int getHighestX() {
        return highestX;
    }

    @Override
    public int getHighestY() {
        return highestY;
    }

    @Override
    public ExporterSettings getLayerSettings(Layer layer) {
        return layerSettings.get(layer);
    }

    @Override
    public Map<Layer, ExporterSettings> getAllLayerSettings() {
        return layerSettings;
    }

    @Override
    public int getLowestX() {
        return lowestX;
    }

    @Override
    public int getLowestY() {
        return lowestY;
    }

    @Override
    public int getFloodedCount(int x, int y, int r, boolean lava) {
        return doGetFloodedCount(x, y, r, lava);
    }

    @Override
    public float getSlope(int x, int y) {
        return doGetSlope(x, y);
    }

    @Override
    public Set<Point> getTileCoords() {
        return tileCoords;
    }

    @Override
    public int getWidth() {
        return highestX - lowestX + 1;
    }

    @Override
    public synchronized boolean isBorderTile(int x, int y) {
        boolean result = false;
        for (Dimension dimension: dimensions) {
            if (dimension.isTilePresent(x, y)) {
                return false;
            } else if (dimension.isBorderTile(x, y)) {
                result = true;
            }
        }
        return result;
    }

    @Override
    public Rectangle getExtent() {
        return extent;
    }

    @Override
    public int getLowestIntHeight() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getHightestIntHeight() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<Layer, Integer> getLayersAt(int x, int y) {
        final Tile tile = getTile(x >> TILE_SIZE_BITS, y >> TILE_SIZE_BITS);
        return (tile != null) ? tile.getLayersAt(x & TILE_SIZE_MASK, y & TILE_SIZE_MASK) : null;
    }

    @Override
    public TileVisitationBuilder visitTiles() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Tile doGetTile(Point coords) {
        return tileCoords.contains(coords) ? new FlatteningTile(coords.x, coords.y) : null;
    }

    private final Dimension[] dimensions;
    private final Set<Point> tileCoords = new HashSet<>();
    private final int lowestX, highestX, lowestY, highestY;
    private final Set<Layer> minimumLayers = new HashSet<>();
    private final Map<Layer, ExporterSettings> layerSettings = new HashMap<>();
    private final Rectangle extent;
    private final int minHeight, maxHeight;
    private Set<Terrain> allTerrains;
    private Set<Layer> allLayers, allLayersCombinedApplied;

    class FlatteningTile extends ReadOnlyTile {
        FlatteningTile(int tileX, int tileY) {
            super(tileX, tileY, minHeight, maxHeight, false);

            // Since this only gets called if at least one of the layers has a tile here, it is guaranteed that this
            // will result in at least one tile:
            final List<Tile> tiles = new ArrayList<>(dimensions.length);
            final SortedSet<Layer> layers = new TreeSet<>();
            for (Dimension dimension: dimensions) {
                final Tile layerTile = dimension.getTile(tileX, tileY);
                if (layerTile != null) {
                    tiles.add(layerTile);
                    layers.addAll(layerTile.getLayers());
                }
            }
            this.tiles = tiles.toArray(new Tile[tiles.size()]);
            this.layers = new ArrayList<>(layers);

            // For each chunk, find the first layer that is not marked NotPresent, except if a layer is marked ReadOnly,
            // in which case use that layer
            final int lastTile = this.tiles.length - 1;
            for (int x = 0; x < 8; x++) {
                for (int y = 0; y < 8; y++) {
                    final int xInChunk = x << 4, yInChunk = y << 4;
                    layerToUse[x][y] = lastTile;
                    for (int i = 0; i < lastTile; i++) {
                        if (this.tiles[i].getBitLayerValue(ReadOnly.INSTANCE, xInChunk, yInChunk)) {
                            layerToUse[x][y] = i;
                            break;
                        } else if ((! this.tiles[i].getBitLayerValue(NotPresent.INSTANCE, xInChunk, yInChunk)) && (layerToUse[x][y] > i)) {
                            layerToUse[x][y] = i;
                        }
                    }
                }
            }
        }

        @Override
        public synchronized int getFloodedCount(int x, int y, int r, boolean lava) {
            // TODO we just copied this from Dimension; are the coordinates correct like this?
            int count = 0;
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    final int xx = x + dx, yy = y + dy;
                    if ((getWaterLevelAt(xx, yy) > getIntHeightAt(xx, yy))
                            && (lava ? getBitLayerValueAt(FloodWithLava.INSTANCE, xx, yy)
                            : (! getBitLayerValueAt(FloodWithLava.INSTANCE, xx, yy)))) {
                        count++;
                    }
                }
            }
            return count;
        }

        @Override
        public List<Layer> getActiveLayers(int x, int y) {
            throw new UnsupportedOperationException();
        }

        @Override
        public float getSlope(int x, int y) {
            return doGetSlope(x, y);
        }

        @Override
        public int getIntHeight(int x, int y) {
            return Math.round(tiles[layerToUse[x >> 4][y >> 4]].getHeight(x, y));
        }

        @Override
        public float getHeight(int x, int y) {
            return tiles[layerToUse[x >> 4][y >> 4]].getHeight(x, y);
        }

        @Override
        public int getRawHeight(int x, int y) {
            return tiles[layerToUse[x >> 4][y >> 4]].getRawHeight(x, y);
        }

        @Override
        public Terrain getTerrain(int x, int y) {
            return tiles[layerToUse[x >> 4][y >> 4]].getTerrain(x, y);
        }

        @Override
        public synchronized Set<Terrain> getAllTerrains() {
            final Set<Terrain> allTerrains = new HashSet<>();
            for (int x = 0; x < TILE_SIZE; x++) {
                for (int y = 0; y < TILE_SIZE; y++) {
                    allTerrains.add(getTerrain(x, y));
                }
            }
            return allTerrains;
        }

        @Override
        public int getWaterLevel(int x, int y) {
            return tiles[layerToUse[x >> 4][y >> 4]].getWaterLevel(x, y);
        }

        @Override
        public List<Layer> getLayers() {
            return layers;
        }

        @Override
        public boolean containsOneOf(Layer... layers) {
            for (Layer layer: layers) {
                if (this.layers.contains(layer)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean hasLayer(Layer layer) {
            return layers.contains(layer);
        }

        @Override
        public List<Layer> getLayers(Set<Layer> additionalLayers) {
            return doGetLayers(additionalLayers);
        }

        @Override
        public boolean getBitLayerValue(Layer layer, int x, int y) {
            return tiles[layerToUse[x >> 4][y >> 4]].getBitLayerValue(layer, x, y);
        }

        @Override
        public int getBitLayerCount(Layer layer, int x, int y, int r) {
            // TODO we just copied this from Dimension; are the coordinates correct like this?
            int count = 0;
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    if (getBitLayerValueAt(layer, x + dx, y + dy)) {
                        count++;
                    }
                }
            }
            return count;
        }

        @Override
        public Map<Layer, Integer> getLayersAt(int x, int y) {
            return tiles[layerToUse[x >> 4][y >> 4]].getLayersAt(x, y);
        }

        @Override
        public float getDistanceToEdge(Layer layer, int x, int y, float maxDistance) {
            // TODO we just copied this from Dimension; are the coordinates correct like this?
            final int r = (int) Math.ceil(maxDistance);
            if (! getBitLayerValueAt(layer, x, y)) {
                return 0;
            }
            float distance = maxDistance;
            for (int i = 1; i <= r; i++) {
                if (((! getBitLayerValueAt(layer, x - i, y))
                        || (! getBitLayerValueAt(layer, x + i, y))
                        || (! getBitLayerValueAt(layer, x, y - i))
                        || (! getBitLayerValueAt(layer, x, y + i)))
                        && (i < distance)) {
                    // If we get here there's no possible way a shorter distance could be found later, so return
                    // immediately
                    return i;
                }
                for (int d = 1; d <= i; d++) {
                    if ((! getBitLayerValueAt(layer, x - i, y - d))
                            || (! getBitLayerValueAt(layer, x + d, y - i))
                            || (! getBitLayerValueAt(layer, x + i, y + d))
                            || (! getBitLayerValueAt(layer, x - d, y + i))
                            || ((d < i) && ((! getBitLayerValueAt(layer, x - i, y + d))
                            || (! getBitLayerValueAt(layer, x - d, y - i))
                            || (! getBitLayerValueAt(layer, x + i, y - d))
                            || (! getBitLayerValueAt(layer, x + d, y + i))))) {
                        float tDistance = MathUtils.getDistance(i, d);
                        if (tDistance < distance) {
                            distance = tDistance;
                        }
                        // We won't find a shorter distance this round, so skip to the next round
                        break;
                    }
                }
            }
            return distance;
        }

        @Override
        public int getLayerValue(Layer layer, int x, int y) {
            return tiles[layerToUse[x >> 4][y >> 4]].getLayerValue(layer, x, y);
        }

        private final Tile[] tiles;
        private final int[][] layerToUse = new int[8][8];
        private final List<Layer> layers;
    }
}