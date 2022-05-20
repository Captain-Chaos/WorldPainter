/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.exporting;

import org.pepsoft.minecraft.Chunk;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.exporters.ExporterSettings;

import java.awt.*;
import java.util.function.Supplier;

/**
 * An abstract base class for {@link LayerExporter} implementations.
 *
 * @author pepijn
 */
public abstract class AbstractLayerExporter<L extends Layer> implements LayerExporter {
    public AbstractLayerExporter(Dimension dimension, Platform platform, ExporterSettings settings, L layer) {
        this.dimension = dimension;
        this.platform = platform;
        this.settings = settings;
        this.layer = layer;
        minHeight = Math.max(dimension.getMinHeight(), platform.minZ);
        maxHeight = Math.min(dimension.getMaxHeight(), platform.maxMaxHeight);
    }

    public final Dimension getDimension() {
        return dimension;
    }

    @Override
    public final L getLayer() {
        return layer;
    }
    
    /**
     * A visitor of chunks.
     */
    @FunctionalInterface
    public interface ChunksInTilesVisitor {
        /**
         * Visit a chunk.
         *
         * @param tile          The tile on which the chunk is located.
         * @param chunkX        The global X coordinate of the chunk.
         * @param chunkZ        The global Z coorindate of the chunk.
         * @param chunkSupplier The lazy chunk loader. The chunk will not be actually loaded until
         * {@link Supplier#get()} is invoked on this supplier.
         * @return {@code true} is more chunks should be visited; {@code false} if the process should be aborted.
         */
        boolean visitChunk(Tile tile, int chunkX, int chunkZ, Supplier<Chunk> chunkSupplier);
    }

    /**
     * Visits the existent chunks in a particular rectangular area, and containing a particular layer, for editing. The
     * chunks are lazily loaded. The visitor can abort the process by returning {@code false}.
     *
     * @param dimension The dimension in which to check which tiles contain the layer.
     * @param world     The {@link MinecraftWorld} in which to visit the existent chunks.
     * @param layer     The layer to which to constrain the visited tiles.
     * @param area      The area in block coordinates in which to visit the chunks.
     * @param visitor   The visitor to invoke for each existing chunk that is in the specified area, and which is on a
     *                  tile containing the specified layer.
     * @return {@code true} is all chunks were visited, {@code false} if the visitor returned {@code false}.
     */
    protected final boolean visitChunksForLayerInAreaForEditing(MinecraftWorld world, Layer layer, Rectangle area, Dimension dimension, ChunksInTilesVisitor visitor) {
        final int chunkX1 = area.x >> 4, chunkX2 = (area.x + area.width - 1) >> 4;
        final int chunkZ1 = area.y >> 4, chunkZ2 = (area.y + area.height - 1) >> 4;
        final int tileX1 = chunkX1 >> 3, tileX2 = chunkX2 >> 3, tileY1 = chunkZ1 >> 3, tileY2 = chunkZ2 >> 3;
        final boolean applyEverywhere = (dimension.getLayerSettings(layer) != null) && dimension.getLayerSettings(layer).isApplyEverywhere();
        for (int tileX = tileX1; tileX <= tileX2; tileX++) {
            for (int tileY = tileY1; tileY <= tileY2; tileY++) {
                final Tile tile = dimension.getTile(tileX, tileY);
                if ((tile != null) && (applyEverywhere || tile.containsOneOf(layer))) {
                    for (int chunkXInTile = 0; chunkXInTile < 8; chunkXInTile++) {
                        for (int chunkYInTile = 0; chunkYInTile < 8; chunkYInTile++) {
                            final int chunkX = (tileX << 3) | chunkXInTile, chunkZ = (tileY << 3) | chunkYInTile;
                            if ((chunkX >= chunkX1) && (chunkX <= chunkX2)
                                    && (chunkZ >= chunkZ1) && (chunkZ <= chunkZ2)
                                    && (world.isChunkPresent(chunkX, chunkZ))
                                    && (! visitor.visitChunk(tile, chunkX, chunkZ, () -> world.getChunkForEditing(chunkX, chunkZ)))) {
                                return false;
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    protected final Dimension dimension;
    protected final Platform platform;
    protected final L layer;
    protected final int minHeight, maxHeight;
    protected final ExporterSettings settings;
}