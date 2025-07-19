package org.pepsoft.worldpainter;

import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.util.ProgressReceiver.OperationCancelled;
import org.pepsoft.util.mdc.MDCCapturingRuntimeException;
import org.pepsoft.util.mdc.MDCThreadPoolExecutor;
import org.pepsoft.worldpainter.heightMaps.AbstractHeightMap;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.NotPresent;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.pepsoft.worldpainter.Constants.*;
import static org.pepsoft.worldpainter.util.ThreadUtils.chooseThreadCount;

/**
 * Utility class for scaling a set of {@link Tile}s.
 */
public class ScalingHelper {
    /**
     * Create a {@code ScalingHelper} for scaling a loose collection of {@code Tile}s.
     */
    public ScalingHelper(Map<Point, Tile> tiles, TileFactory tileFactory, float scale) {
        unscaledTiles = tiles;
        if (tileFactory instanceof HeightMapTileFactory heightMapTileFactory) {
            scaledTileFactory = new HeightMapTileFactory(tileFactory.getSeed(), heightMapTileFactory.getHeightMap().scaled(scale), tileFactory.getMinHeight(), tileFactory.getMaxHeight(), heightMapTileFactory.isFloodWithLava(), heightMapTileFactory.getTheme());
        } else {
            scaledTileFactory = tileFactory;
        }
        this.scale = scale;
        minHeight = tileFactory.getMinHeight();
        maxHeight = tileFactory.getMaxHeight();
        heightMap = new AbstractHeightMap() {
            @Override
            public double getHeight(int x, int y) {
                final Tile tile = tiles.get(new Point(x >> TILE_SIZE_BITS, y >> TILE_SIZE_BITS));
                if (tile != null) {
                    return tile.getHeight(x & TILE_SIZE_MASK, y & TILE_SIZE_MASK);
                } else {
                    return additionalTiles.computeIfAbsent(new Point(x >> TILE_SIZE_BITS, y >> TILE_SIZE_BITS),
                            coords -> tileFactory.createTile(coords.x, coords.y)).getHeight(x & TILE_SIZE_MASK, y & TILE_SIZE_MASK);
                }
            }

            @Override
            public Icon getIcon() {
                return null;
            }

            @Override
            public double[] getRange() {
                return new double[] { minHeight, maxHeight };
            }

            @Serial
            private void writeObject(ObjectOutputStream out) throws IOException {
                throw new NotSerializableException();
            }

            private final Map<Point, Tile> additionalTiles = new ConcurrentHashMap<>();
        }.smoothed().scaled(scale).clamped(minHeight, maxHeight);
        final Set<Layer> allLayers = new HashSet<>();
        for (Tile tile: tiles.values()) {
            allLayers.addAll(tile.getLayers());
        }
        layerHeightMaps = allLayers.stream()
                .filter(layer -> ! layer.discrete)
                .collect(toMap(identity(), layer -> new AbstractHeightMap() {
                    @Override
                    public double getHeight(int x, int y) {
                        final Tile tile = tiles.get(new Point(x >> TILE_SIZE_BITS, y >> TILE_SIZE_BITS));
                        if (tile != null) {
                            return switch (dataSize) {
                                case BIT, BIT_PER_CHUNK -> tile.getBitLayerValue(layer, x & TILE_SIZE_MASK, y & TILE_SIZE_MASK) ? 1f : 0f;
                                case NIBBLE, BYTE -> tile.getLayerValue(layer, x & TILE_SIZE_MASK, y & TILE_SIZE_MASK);
                                default -> throw new IllegalStateException("Unsupported data size " + dataSize + " for layer " + layer);
                            };
                        } else {
                            return layer.getDefaultValue();
                        }
                    }

                    @Override
                    public Icon getIcon() {
                        return null;
                    }

                    @Override
                    public double[] getRange() {
                        return switch (dataSize) {
                            case BIT, BIT_PER_CHUNK -> new double[] {0, 1};
                            case NIBBLE -> new double[] {0, 15};
                            case BYTE -> new double[] {0, 255};
                            default -> throw new IllegalStateException("Unsupported data size " + dataSize + " for layer " + layer);
                        };
                    }

                    @Serial
                    private void writeObject(ObjectOutputStream out) throws IOException {
                        throw new NotSerializableException();
                    }

                    private final Layer.DataSize dataSize = layer.dataSize;
                }.smoothed().scaled(scale).clamped(0, layer.dataSize.maxValue)));
        discreteLayers = allLayers.stream().filter(layer -> layer.discrete).collect(toSet());
        tileCoords = new HashSet<>();
        int lowestTileX = Integer.MAX_VALUE, lowestTileY = Integer.MAX_VALUE, highestTileX = Integer.MIN_VALUE, highestTileY = Integer.MIN_VALUE;
        for (Tile tile: tiles.values()) {
            final Point coordsOfTile = new Point(tile.getX(), tile.getY());
            final int scaledTileX1 = Math.round((coordsOfTile.x << TILE_SIZE_BITS) * scale) >> TILE_SIZE_BITS;
            final int scaledTileX2 = Math.round(((coordsOfTile.x << TILE_SIZE_BITS) + TILE_SIZE - 1) * scale) >> TILE_SIZE_BITS;
            final int scaledTileY1 = Math.round((coordsOfTile.y << TILE_SIZE_BITS) * scale) >> TILE_SIZE_BITS;
            final int scaledTileY2 = Math.round(((coordsOfTile.y << TILE_SIZE_BITS) + TILE_SIZE - 1) * scale) >> TILE_SIZE_BITS;
            for (int x = scaledTileX1; x <= scaledTileX2; x++) {
                for (int y = scaledTileY1; y <= scaledTileY2; y++) {
                    if (x < lowestTileX) {
                        lowestTileX = x;
                    }
                    if (x > highestTileX) {
                        highestTileX = x;
                    }
                    if (y < lowestTileY) {
                        lowestTileY = y;
                    }
                    if (y > highestTileY) {
                        highestTileY = y;
                    }
                    tileCoords.add(new Point(x, y));
                }
            }
        }
        this.lowestTileX = lowestTileX;
        this.lowestTileY = lowestTileY;
        this.highestTileX = highestTileX;
        this.highestTileY = highestTileY;
    }

    public Tile createScaledTile(int tileX, int tileY) {
        if (! tileCoords.contains(new Point(tileX, tileY))) {
            return null;
        }
        final Tile scaledTile = scaledTileFactory.createTile(tileX, tileY);
        final int[] notPresentBlocksPerChunk = new int[64];
        Tile cachedUnscaledTile = null;
        int cachedTileX = Integer.MIN_VALUE, cachedTileY = Integer.MIN_VALUE;
        for (int xInTile = 0; xInTile < TILE_SIZE; xInTile++) {
            for (int yInTile = 0; yInTile < TILE_SIZE; yInTile++) {
                final int x = xInTile + (tileX << TILE_SIZE_BITS), y = yInTile + (tileY << TILE_SIZE_BITS);
                final float scaledX = (float) x / scale, scaledY = (float) y / scale;
                final int intX = (int) scaledX, intY = (int) scaledY;
                final int unscaledTileX = intX >> TILE_SIZE_BITS, unscaledTileY = intY >> TILE_SIZE_BITS;
                if ((unscaledTileX != cachedTileX) || (unscaledTileY != cachedTileY)) {
                    cachedUnscaledTile = unscaledTiles.get(new Point(unscaledTileX, unscaledTileY));
                    cachedTileX = unscaledTileX;
                    cachedTileY = unscaledTileY;
                }
                if (cachedUnscaledTile == null) {
                    // When scaling down, the unscaled tile may not exist
                    notPresentBlocksPerChunk[((xInTile >> 4) << 3) | (yInTile >> 4)]++;
                    continue;
                }
                scaledTile.setHeight(xInTile, yInTile, (float) heightMap.getHeight(x, y));
                scaledTile.setWaterLevel(xInTile, yInTile, cachedUnscaledTile.getWaterLevel(intX & TILE_SIZE_MASK, intY & TILE_SIZE_MASK));
                scaledTile.setTerrain(xInTile, yInTile, cachedUnscaledTile.getTerrain(intX & TILE_SIZE_MASK, intY & TILE_SIZE_MASK)); // TODO smooth scaling
                for (Map.Entry<Layer, HeightMap> entry: layerHeightMaps.entrySet()) {
                    final Layer layer = entry.getKey();
                    final int layerValue = (int) Math.round(entry.getValue().getHeight(x, y));
                    if (layerValue != layer.getDefaultValue()) {
                        switch (layer.dataSize) {
                            case BIT:
                            case BIT_PER_CHUNK:
                                scaledTile.setBitLayerValue(layer, xInTile, yInTile, true);
                                break;
                            case NIBBLE:
                            case BYTE:
                                scaledTile.setLayerValue(layer, xInTile, yInTile, layerValue);
                                break;
                        }
                    }
                }
                for (Layer layer: discreteLayers) {
                    switch (layer.dataSize) {
                        case BIT:
                        case BIT_PER_CHUNK:
                            if (cachedUnscaledTile.getBitLayerValue(layer, intX & TILE_SIZE_MASK, intY & TILE_SIZE_MASK)) {
                                scaledTile.setBitLayerValue(layer, xInTile, yInTile, true); // TODO smooth scaling
                            }
                            break;
                        case NIBBLE:
                        case BYTE:
                            final int layerValue = cachedUnscaledTile.getLayerValue(layer, intX & TILE_SIZE_MASK, intY & TILE_SIZE_MASK);
                            if (layerValue != layer.getDefaultValue()) {
                                scaledTile.setLayerValue(layer, xInTile, yInTile, layerValue); // TODO smooth scaling
                            }
                            break;
                    }
                }
            }
        }
        for (int i = 0; i < 64; i++) {
            if (notPresentBlocksPerChunk[i] == 256) {
                scaledTile.setBitLayerValue(NotPresent.INSTANCE, (i >> 3) << 4, (i & 7) << 4, true);
            }
        }
        // TODO seeds
        return scaledTile;
    }

    public Set<Tile> getAllScaledTiles(ProgressReceiver progressReceiver) throws OperationCancelled {
        final Set<Tile> result = ConcurrentHashMap.newKeySet();
        final AtomicLong count = new AtomicLong();
        final int tileCount = tileCoords.size();
        final ExecutorService executor = MDCThreadPoolExecutor.newFixedThreadPool(chooseThreadCount("scaling", tileCount));
        final Throwable[] exception = new Throwable[1];
        for (Point coordsOfTile: tileCoords) {
            executor.execute(() -> {
                if (exception[0] != null) {
                    return;
                }
                try {
                    // Create an ordinary, but scaled, tile
                    final int tileX = coordsOfTile.x, tileY = coordsOfTile.y;
                    final Tile scaledTile = createScaledTile(tileX, tileY);
                    result.add(scaledTile);
                    if (progressReceiver != null) {
                        progressReceiver.setProgress((float) count.incrementAndGet() / tileCount);
                    }
                } catch (Throwable t) {
                    if (exception[0] == null) {
                        exception[0] = t;
                    }
                }
            });
        }
        executor.shutdown();
        try {
            if (! executor.awaitTermination(365, DAYS)) {
                throw new MDCCapturingRuntimeException("Scaling operation did not complete within one year");
            }
        } catch (InterruptedException e) {
            throw new MDCCapturingRuntimeException("Thread interrupted while waiting for completion of scaling operation", e);
        }
        if (exception[0] != null) {
            if (exception[0] instanceof OperationCancelled) {
                throw (OperationCancelled) exception[0];
            } else {
                throw new MDCCapturingRuntimeException("Exception while scaling tiles", exception[0]);
            }
        }
        return result;
    }

    public int getLowestTileX() {
        return lowestTileX;
    }

    public int getLowestTileY() {
        return lowestTileY;
    }

    public int getHighestTileX() {
        return highestTileX;
    }

    public int getHighestTileY() {
        return highestTileY;
    }

    public Set<Point> getTileCoords() {
        return tileCoords;
    }

    public float getHeightAt(int x, int y) {
        return (float) heightMap.getHeight(x, y);
    }

    public int getLayerValueAt(Layer layer, int x, int y) {
        return layerHeightMaps.containsKey(layer) ? (int) Math.round(layerHeightMaps.get(layer).getHeight(x, y)) : layer.getDefaultValue();
    }

    public boolean getBitLayerValueAt(Layer layer, int x, int y) {
        return (layerHeightMaps.containsKey(layer) ? layerHeightMaps.get(layer).getHeight(x, y) : layer.getDefaultValue()) >= 0.5f;
    }

    private final Map<Point, Tile> unscaledTiles;
    private final TileFactory scaledTileFactory;
    private final float scale;
    private final int minHeight, maxHeight, lowestTileX, lowestTileY, highestTileX, highestTileY;
    private final HeightMap heightMap;
    private final Map<Layer, HeightMap> layerHeightMaps;
    private final Set<Layer> discreteLayers;
    private final Set<Point> tileCoords;
}