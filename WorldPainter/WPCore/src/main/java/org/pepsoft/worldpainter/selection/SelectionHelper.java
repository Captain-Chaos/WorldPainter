package org.pepsoft.worldpainter.selection;

import org.pepsoft.util.MathUtils;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.layers.*;

import java.awt.*;
import java.util.*;

import static org.pepsoft.worldpainter.Constants.TILE_SIZE;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE_BITS;

// TODO: allow new tiles to be filled with void

/**
 * A helper class for maintaining a selection as an optimised combination of
 * per-chunk and per-block layers, and working with said selections.
 *
 * <p>This class is stateful and <strong>NOT</strong> reentrant!
 *
 * <p>Created by Pepijn Schmitz on 03-11-16.
 */
public class SelectionHelper {
    public SelectionHelper(Dimension dimension) {
        this.dimension = dimension;
    }

    public void addToSelection(Shape shape) {
        editSelection(shape, true);
    }

    public void removeFromSelection(Shape shape) {
        editSelection(shape, false);
    }

    /**
     * Calculate the bounding rectangle of the current selection.
     *
     * @return The bounding rectangle of the current selection, or
     * <code>null</code> if there is no active selection.
     */
    public Rectangle getSelectionBounds() {
        final int[] lowestX = {Integer.MAX_VALUE};
        final int[] highestX = {Integer.MIN_VALUE};
        final int[] lowestY = {Integer.MAX_VALUE};
        final int[] highestY = {Integer.MIN_VALUE};
        dimension.streamTiles()
                .filter(tile -> (tile.hasLayer(SelectionChunk.INSTANCE) || tile.hasLayer(SelectionBlock.INSTANCE)))
                .forEach(tile -> {
                    final boolean tileHasChunkSelection = tile.hasLayer(SelectionChunk.INSTANCE);
                    final boolean tileHasBlockSelection = tile.hasLayer(SelectionBlock.INSTANCE);
                    for (int chunkX = 0; chunkX < TILE_SIZE; chunkX += 16) {
                        for (int chunkY = 0; chunkY < TILE_SIZE; chunkY += 16) {
                            if (tileHasChunkSelection && tile.getBitLayerValue(SelectionChunk.INSTANCE, chunkX, chunkY)) {
                                final int x1 = (tile.getX() << TILE_SIZE_BITS) | chunkX;
                                final int x2 = x1 + 15;
                                final int y1 = (tile.getY() << TILE_SIZE_BITS) | chunkY;
                                final int y2 = y1 + 15;
                                if (x1 < lowestX[0]) {
                                    lowestX[0] = x1;
                                }
                                if (x2 > highestX[0]) {
                                    highestX[0] = x2;
                                }
                                if (y1 < lowestY[0]) {
                                    lowestY[0] = y1;
                                }
                                if (y2 > highestY[0]) {
                                    highestY[0] = y2;
                                }
                            } else if (tileHasBlockSelection) {
                                for (int dx = 0; dx < 16; dx++) {
                                    for (int dy = 0; dy < 16; dy++) {
                                        if (tile.getBitLayerValue(SelectionBlock.INSTANCE, chunkX + dx, chunkY + dy)) {
                                            final int x = ((tile.getX() << TILE_SIZE_BITS) | chunkX) + dx;
                                            final int y = ((tile.getY() << TILE_SIZE_BITS) | chunkY) + dy;
                                            if (x < lowestX[0]) {
                                                lowestX[0] = x;
                                            }
                                            if (x > highestX[0]) {
                                                highestX[0] = x;
                                            }
                                            if (y < lowestY[0]) {
                                                lowestY[0] = y;
                                            }
                                            if (y > highestY[0]) {
                                                highestY[0] = y;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                });
        if (lowestX[0] != Integer.MAX_VALUE) {
            return new Rectangle(lowestX[0], lowestY[0], highestX[0] - lowestX[0] + 1, highestY[0] - lowestY[0] + 1);
        } else {
            return null;
        }
    }

    public void copySelection(int targetX, int targetY, ProgressReceiver progressReceiver) throws ProgressReceiver.OperationCancelled {
        Rectangle bounds = getSelectionBounds();
        if (bounds == null) {
            // No selection
            return;
        }
        final int dx = targetX - bounds.x;
        final int dy = targetY - bounds.y;
        if ((dx == 0) && (dy == 0)) {
            // Target is at the same location as the selection
            return;
        }
        final int tileX1 = bounds.x >> TILE_SIZE_BITS;
        final int tileX2 = (bounds.x + bounds.width - 1) >> TILE_SIZE_BITS;
        final int tileY1 = bounds.y >> TILE_SIZE_BITS;
        final int tileY2 = (bounds.y + bounds.height - 1) >> TILE_SIZE_BITS;

        // Make sure to copy in the right direction to avoid problems if the
        // destination overlaps the selection
        clearUndoOnNewTileCreation = options.createNewTiles;
        if (dx > 0) {
            // Shifting right
            if (dy > 0) {
                // Shifting right and down
                for (int tileX = tileX2; tileX >= tileX1; tileX--) {
                    for (int tileY = tileY2; tileY >= tileY1; tileY--) {
                        Tile tile = dimension.getTile(tileX, tileY);
                        if (tile != null) {
                            for (int xInTile = TILE_SIZE - 1; xInTile >= 0; xInTile--) {
                                for (int yInTile = TILE_SIZE - 1; yInTile >= 0; yInTile--) {
                                    processColumn(tile, xInTile, yInTile, dx, dy);
                                }
                            }
                        }
                    }
                    if (progressReceiver != null) {
                        progressReceiver.setProgress((float) (tileX2 - tileX + 1) / (tileX2 - tileX1 + 1));
                    }
                }
            } else {
                // Shifting right or right and up
                for (int tileX = tileX2; tileX >= tileX1; tileX--) {
                    for (int tileY = tileY1; tileY <= tileY2; tileY++) {
                        Tile tile = dimension.getTile(tileX, tileY);
                        if (tile != null) {
                            for (int xInTile = TILE_SIZE - 1; xInTile >= 0; xInTile--) {
                                for (int yInTile = 0; yInTile < TILE_SIZE; yInTile++) {
                                    processColumn(tile, xInTile, yInTile, dx, dy);
                                }
                            }
                        }
                    }
                    if (progressReceiver != null) {
                        progressReceiver.setProgress((float) (tileX2 - tileX + 1) / (tileX2 - tileX1 + 1));
                    }
                }
            }
        } else {
            // Shifting left or not horizontally
            if (dy > 0) {
                // Shifting down or left and down
                for (int tileX = tileX1; tileX <= tileX2; tileX++) {
                    for (int tileY = tileY2; tileY >= tileY1; tileY--) {
                        Tile tile = dimension.getTile(tileX, tileY);
                        if (tile != null) {
                            for (int xInTile = 0; xInTile < TILE_SIZE; xInTile++) {
                                for (int yInTile = TILE_SIZE - 1; yInTile >= 0; yInTile--) {
                                    processColumn(tile, xInTile, yInTile,  dx, dy);
                                }
                            }
                        }
                    }
                    if (progressReceiver != null) {
                        progressReceiver.setProgress((float) (tileX - tileX1 + 1) / (tileX2 - tileX1 + 1));
                    }
                }
            } else {
                // Shifting up or left and up
                for (int tileX = tileX1; tileX <= tileX2; tileX++) {
                    for (int tileY = tileY1; tileY <= tileY2; tileY++) {
                        Tile tile = dimension.getTile(tileX, tileY);
                        if (tile != null) {
                            for (int xInTile = 0; xInTile < TILE_SIZE; xInTile++) {
                                for (int yInTile = 0; yInTile < TILE_SIZE; yInTile++) {
                                    processColumn(tile, xInTile, yInTile, dx, dy);
                                }
                            }
                        }
                    }
                    if (progressReceiver != null) {
                        progressReceiver.setProgress((float) (tileX - tileX1 + 1) / (tileX2 - tileX1 + 1));
                    }
                }
            }
        }
    }

    public void clearSelection() {
        dimension.clearLayerData(SelectionChunk.INSTANCE);
        dimension.clearLayerData(SelectionBlock.INSTANCE);
    }

    public SelectionOptions getOptions() {
        return options;
    }

    public void setOptions(SelectionOptions options) {
        this.options = options;
    }

    private void processColumn(Tile tile, int xInTile, int yInTile, int dx, int dy) {
        if (tile.getBitLayerValue(SelectionChunk.INSTANCE, xInTile, yInTile)
                || tile.getBitLayerValue(SelectionBlock.INSTANCE, xInTile, yInTile)) {
            final int srcX = (tile.getX() << TILE_SIZE_BITS) | xInTile;
            final int srcY = (tile.getY() << TILE_SIZE_BITS) | yInTile;
            final int dstX = srcX + dx;
            final int dstY = srcY + dy;
            if (options.createNewTiles && (! dimension.isTilePresent(dstX >> TILE_SIZE_BITS, dstY >> TILE_SIZE_BITS))) {
                if (clearUndoOnNewTileCreation) {
                    dimension.clearUndo();
                    clearUndoOnNewTileCreation = false;
                }
                dimension.addTile(dimension.getTileFactory().createTile(dstX >> TILE_SIZE_BITS, dstY >> TILE_SIZE_BITS));
            }
            if (options.doBlending) {
                float distanceFromEdge = distanceToSelectionEdge(srcX, srcY);
                if (distanceFromEdge < 16.0f) {
                    float blend = (float) (-Math.cos(distanceFromEdge / DISTANCE_TO_BLEND) / 2 + 0.5);
                    copyColumn(tile, xInTile, yInTile, dstX, dstY, blend);
                } else {
                    copyColumn(tile, xInTile, yInTile, dstX, dstY);
                }
            } else {
                copyColumn(tile, xInTile, yInTile, dstX, dstY);
            }
        }
    }

    private void copyColumn(Tile srcTile, int srcXInTile, int srcYInTile, int dstX, int dstY) {
        if (options.copyHeights) {
            dimension.setRawHeightAt(dstX, dstY, srcTile.getRawHeight(srcXInTile, srcYInTile));
        }
        if (options.copyTerrain) {
            dimension.setTerrainAt(dstX, dstY, srcTile.getTerrain(srcXInTile, srcYInTile));
        }
        if (options.copyFluids) {
            dimension.setWaterLevelAt(dstX, dstY, srcTile.getWaterLevel(srcXInTile, srcYInTile));
            dimension.setBitLayerValueAt(FloodWithLava.INSTANCE, dstX, dstY, srcTile.getBitLayerValue(FloodWithLava.INSTANCE, srcXInTile, srcYInTile));
        }
        if (options.copyLayers) {
            if (options.removeExistingLayers) {
                dimension.clearLayerData(dstX, dstY, SKIP_LAYERS);
            }
            Map<Layer, Integer> layerValues = srcTile.getLayersAt(srcXInTile, srcYInTile);
            layerValues.forEach((layer, value) -> {
                if (SKIP_LAYERS.contains(layer)) {
                    return;
                }
                switch (layer.getDataSize()) {
                    case BIT:
                    case BIT_PER_CHUNK:
                        dimension.setBitLayerValueAt(layer, dstX, dstY, value != 0);
                        break;
                    case BYTE:
                    case NIBBLE:
                        dimension.setLayerValueAt(layer, dstX, dstY, value);
                        break;
                    case NONE:
                        throw new UnsupportedOperationException("Don't know how to copy layer " + layer);
                }
            });
        }
        if (options.copyAnnotations) {
            dimension.setLayerValueAt(Annotations.INSTANCE, dstX, dstY, srcTile.getLayerValue(Annotations.INSTANCE, srcXInTile, srcYInTile));
        }
        if (options.copyBiomes) {
            dimension.setLayerValueAt(Biome.INSTANCE, dstX, dstY, srcTile.getLayerValue(Biome.INSTANCE, srcXInTile, srcYInTile));
        }
    }

    private void copyColumn(Tile srcTile, int srcXInTile, int srcYInTile, int dstX, int dstY, float blend) {
        if (options.copyHeights) {
            dimension.setRawHeightAt(dstX, dstY, (int) (blend * srcTile.getRawHeight(srcXInTile, srcYInTile) + (1 - blend) * dimension.getRawHeightAt(dstX, dstY) + 0.5f));
        }
        if (options.copyTerrain && (RANDOM.nextFloat() <= blend)) {
            dimension.setTerrainAt(dstX, dstY, srcTile.getTerrain(srcXInTile, srcYInTile));
        }
        if (options.copyFluids) {
            dimension.setWaterLevelAt(dstX, dstY, (int) (blend * srcTile.getWaterLevel(srcXInTile, srcYInTile) + (1 - blend) * dimension.getWaterLevelAt(dstX, dstY) + 0.5f));
            if (RANDOM.nextFloat() <= blend) {
                dimension.setBitLayerValueAt(FloodWithLava.INSTANCE, dstX, dstY, srcTile.getBitLayerValue(FloodWithLava.INSTANCE, srcXInTile, srcYInTile));
            }
        }
        if (options.copyLayers) {
            if (options.removeExistingLayers && (RANDOM.nextFloat() <= blend)) {
                dimension.clearLayerData(dstX, dstY, SKIP_LAYERS);
            }
            Map<Layer, Integer> layerValues = srcTile.getLayersAt(srcXInTile, srcYInTile);
            layerValues.forEach((layer, value) -> {
                if (SKIP_LAYERS.contains(layer)) {
                    return;
                }
                switch (layer.getDataSize()) {
                    case BIT:
                    case BIT_PER_CHUNK:
                        if (RANDOM.nextFloat() <= blend) {
                            dimension.setBitLayerValueAt(layer, dstX, dstY, value != 0);
                        }
                        break;
                    case BYTE:
                    case NIBBLE:
                        dimension.setLayerValueAt(layer, dstX, dstY, (int) (blend * value + (1 - blend) * dimension.getLayerValueAt(layer, dstX, dstY) + 0.5f));
                        break;
                    case NONE:
                        throw new UnsupportedOperationException("Don't know how to copy layer " + layer);
                }
            });
        }
        if (options.copyAnnotations && (RANDOM.nextFloat() <= blend)) {
            dimension.setLayerValueAt(Annotations.INSTANCE, dstX, dstY, srcTile.getLayerValue(Annotations.INSTANCE, srcXInTile, srcYInTile));
        }
        if (options.copyBiomes && (RANDOM.nextFloat() <= blend)) {
            dimension.setLayerValueAt(Biome.INSTANCE, dstX, dstY, srcTile.getLayerValue(Biome.INSTANCE, srcXInTile, srcYInTile));
        }
    }

    /**
     * Calculate the distance to the edge of the selected area, to a maximum of
     * 16 blocks.
     *
     * @param x The X coordinate to test.
     * @param y The Y coordinate to test.
     * @return The distance to the edge of the selection if less than 16, or 16
     *     if the distance is 16 or greater, or 0 if the specified coordinates
     *     are not in the selection.
     */
    private float distanceToSelectionEdge(int x, int y) {
        // First check if the chunk and all surrounding chunks are selected, in
        // which case the distance cannot be less than 16 and we're done
        final int chunkX = x >> 4, chunkY = y >> 4;
        boolean nonSelectedChunkFound = false;
outer:  for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (! dimension.getBitLayerValueAt(SelectionChunk.INSTANCE, (chunkX + dx) << 4, (chunkY + dy) << 4)) {
                    nonSelectedChunkFound = true;
                    break outer;
                }
            }
        }
        if (! nonSelectedChunkFound) {
            return 16.0f;
        }

        // Not all chunks are selected. First check if the specified coordinates
        // are even in the selection; if not, we're done
        if (! (dimension.getBitLayerValueAt(SelectionChunk.INSTANCE, x, y) || dimension.getBitLayerValueAt(SelectionBlock.INSTANCE, x, y))) {
            return 0.0f;
        }

        // We're in the selection and there's a not fully selected chunk nearby;
        // check all blocks in a circle around the specified location in order
        // of more or less increasing distance to be able to bail out early if
        // a non selected block is found
        float distance = 16.0f;
        for (int i = 1; i <= 16; i++) {
            if (((! isSelected(x - i, y))
                        || (! isSelected(x + i, y))
                        || (! isSelected(x, y - i))
                        || (! isSelected(x, y + i)))
                    && (i < distance)) {
                // If we get here there's no possible way a shorter
                // distance could be found later, so return immediately
                return i;
            }
            for (int d = 1; d <= i; d++) {
                if ((! isSelected(x - i, y - d))
                        || (! isSelected(x + d, y - i))
                        || (! isSelected(x + i, y + d))
                        || (! isSelected(x - d, y + i))
                        || ((d < i) && ((! isSelected(x - i, y + d))
                            || (! isSelected(x - d, y - i))
                            || (! isSelected(x + i, y - d))
                            || (! isSelected(x + d, y + i))))) {
                    float tDistance = MathUtils.getDistance(i, d);
                    if (tDistance < distance) {
                        distance = tDistance;
                    }
                    // We won't find a shorter distance this round, so
                    // skip to the next round
                    break;
                }
            }
        }
        return distance;
    }

    private boolean isSelected(int x, int y) {
        return dimension.getBitLayerValueAt(SelectionChunk.INSTANCE, x, y) || dimension.getBitLayerValueAt(SelectionBlock.INSTANCE, x, y);
    }

    private void editSelection(Shape shape, boolean add) {
        // Determine the bounding box of the selection in tile coordinates
        final Rectangle shapeBounds = shape.getBounds();
        final int tileX1 = shapeBounds.x >> TILE_SIZE_BITS;
        final int tileX2 = (shapeBounds.x + shapeBounds.width - 1) >> TILE_SIZE_BITS;
        final int tileY1 = shapeBounds.y >> TILE_SIZE_BITS;
        final int tileY2 = (shapeBounds.y + shapeBounds.height - 1) >> TILE_SIZE_BITS;

        // Iterate over all the tiles in the bounding box
        for (int tileX = tileX1; tileX <= tileX2; tileX++) {
            for (int tileY = tileY1; tileY <= tileY2; tileY++) {
                Tile tile = dimension.getTileForEditing(tileX, tileY);
                if (tile != null) {
                    Rectangle tileBounds = new Rectangle(tileX << TILE_SIZE_BITS, tileY << TILE_SIZE_BITS, TILE_SIZE, TILE_SIZE);
                    if (shape.contains(tileBounds)) {
                        // The tile lies entirely inside the selection
                        if (add) {
                            tile.clearLayerData(SelectionBlock.INSTANCE);
                            fillTile(tile, SelectionChunk.INSTANCE);
                        } else {
                            tile.clearLayerData(SelectionBlock.INSTANCE);
                            tile.clearLayerData(SelectionChunk.INSTANCE);
                        }
                    } else if (shape.intersects(tileBounds)) {
                        // The tile intersects the selection, but does not
                        // lie entirely inside it; go chunk by chunk
                        for (int chunkX = 0; chunkX < TILE_SIZE; chunkX += 16) {
                            for (int chunkY = 0; chunkY < TILE_SIZE; chunkY += 16) {
                                Rectangle chunkBounds = new Rectangle(tileBounds.x + chunkX, tileBounds.y + chunkY, 16, 16);
                                if (shape.contains(chunkBounds)) {
                                    // The chunk lies entirely inside the
                                    // selection
                                    if (add) {
                                        clearTile(tile, SelectionBlock.INSTANCE, chunkX, chunkY, 16, 16);
                                        tile.setBitLayerValue(SelectionChunk.INSTANCE, chunkX, chunkY, true);
                                    } else {
                                        clearTile(tile, SelectionBlock.INSTANCE, chunkX, chunkY, 16, 16);
                                        tile.setBitLayerValue(SelectionChunk.INSTANCE, chunkX, chunkY, false);
                                    }
                                } else if (shape.intersects(chunkBounds)) {
                                    // The chunk intersects the selection,
                                    // but does not lie entirely inside it;
                                    // go block by block
                                    if (add && (! tile.getBitLayerValue(SelectionChunk.INSTANCE, chunkX, chunkY))) {
                                        // The chunk is not yet entirely selected
                                        for (int dx = 0; dx < 16; dx++) {
                                            for (int dy = 0; dy < 16; dy++) {
                                                int blockX = chunkBounds.x + dx;
                                                int blockY = chunkBounds.y + dy;
                                                if (shape.contains(blockX, blockY)) {
                                                    tile.setBitLayerValue(SelectionBlock.INSTANCE, chunkX + dx, chunkY + dy, true);
                                                }
                                            }
                                        }
                                    } else if (! add) {
                                        if (tile.getBitLayerValue(SelectionChunk.INSTANCE, chunkX, chunkY)) {
                                            // The chunk is entirely selected
                                            tile.setBitLayerValue(SelectionChunk.INSTANCE, chunkX, chunkY, false);
                                            for (int dx = 0; dx < 16; dx++) {
                                                for (int dy = 0; dy < 16; dy++) {
                                                    int blockX = chunkBounds.x + dx;
                                                    int blockY = chunkBounds.y + dy;
                                                    tile.setBitLayerValue(SelectionBlock.INSTANCE, chunkX + dx, chunkY + dy, ! shape.contains(blockX, blockY));
                                                }
                                            }
                                        } else {
                                            for (int dx = 0; dx < 16; dx++) {
                                                for (int dy = 0; dy < 16; dy++) {
                                                    int blockX = chunkBounds.x + dx;
                                                    int blockY = chunkBounds.y + dy;
                                                    if (shape.contains(blockX, blockY)) {
                                                        tile.setBitLayerValue(SelectionBlock.INSTANCE, chunkX + dx, chunkY + dy, false);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void fillTile(Tile tile, Layer layer) {
        switch(layer.getDataSize()) {
            case BIT_PER_CHUNK:
                for (int x = 0; x < TILE_SIZE; x++) {
                    for (int y = 0; y < TILE_SIZE; y++) {
                        tile.setBitLayerValue(layer, x, y, true);
                    }
                }
                break;
            default:
                throw new UnsupportedOperationException("Data size " + layer.getDataSize() + " not supported");
        }
    }

    private void clearTile(Tile tile, Layer layer, int x, int y, int w, int h) {
        switch(layer.getDataSize()) {
            case BIT:
                for (int dx = 0; dx < w; dx++) {
                    for (int dy = 0; dy < h; dy++) {
                        tile.setBitLayerValue(layer, x + dx, y + dy, false);
                    }
                }
                break;
            default:
                throw new UnsupportedOperationException("Data size " + layer.getDataSize() + " not supported");
        }
    }

    private final Dimension dimension;
    private SelectionOptions options;
    private boolean clearUndoOnNewTileCreation;

    private static final double DISTANCE_TO_BLEND = 16.0 / Math.PI;
    private static final Random RANDOM = new Random();
    private static final Set<Layer> SKIP_LAYERS = new HashSet<>(Arrays.asList(Biome.INSTANCE, SelectionChunk.INSTANCE,
            SelectionBlock.INSTANCE, NotPresent.INSTANCE, Annotations.INSTANCE, FloodWithLava.INSTANCE));
}