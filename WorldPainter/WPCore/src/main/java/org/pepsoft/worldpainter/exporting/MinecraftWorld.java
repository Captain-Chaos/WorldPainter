/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.exporting;

import org.pepsoft.minecraft.*;
import org.pepsoft.worldpainter.Platform;

/**
 *
 * @author pepijn
 */
public interface MinecraftWorld extends ChunkProvider {
    /**
     * Returns {@link Constants#BLK_AIR} if {@code height} is too large.
     * 
     * @deprecated Use {@link #getMaterialAt(int, int, int)}
     */
    int getBlockTypeAt(int x, int y, int height);

    /**
     * Returns {@code 0} if {@code height} is too large.
     * 
     * @deprecated Use {@link #getMaterialAt(int, int, int)}
     */
    int getDataAt(int x, int y, int height);

    /**
     * Returns {@code null} if {@code height} is too large.
     */
    Material getMaterialAt(int x, int y, int height);

    /**
     * Fails silently if {@code height} is too large.
     * 
     * @deprecated Use {@link #setMaterialAt(int, int, int, Material)}
     */
    void setBlockTypeAt(int x, int y, int height, int blockType);

    /**
     * Fails silently if {@code height} is too large.
     * 
     * @deprecated Use {@link #setMaterialAt(int, int, int, Material)}
     */
    void setDataAt(int x, int y, int height, int data);

    /**
     * Fails silently if {@code height} is too large.
     */
    void setMaterialAt(int x, int y, int height, Material material);

    /**
     * Mark a block to be updated by Minecraft when next loaded. Coordinates in world coordinates.
     *
     * <p>The default implementation uses {@link #getChunk(int, int)} to get the chunk, and if it exists invokes
     * {@link Chunk#markForUpdateChunk(int, int, int)} on it.
     */
    default void markForUpdateWorld(int x, int y, int height) {
        Chunk chunk = getChunkForEditing(x >> 4, y >> 4);
        if (chunk != null) {
            chunk.markForUpdateChunk(x & 0xf, height, y & 0xf);
        }
    }

    int getMinHeight();

    int getMaxHeight();

    void addEntity(int x, int y, int height, Entity entity);

    void addEntity(double x, double y, double height, Entity entity);

    void addTileEntity(int x, int y, int height, TileEntity tileEntity);
    
    /**
     * Returns {@code 0} if {@code height} is too large.
     */
    int getBlockLightLevel(int x, int y, int height);

    /**
     * Fails silently if {@code height} is too large.
     */
    void setBlockLightLevel(int x, int y, int height, int blockLightLevel);

    /**
     * Returns {@code 15} if {@code height} is too large.
     */
    int getSkyLightLevel(int x, int y, int height);

    /**
     * Fails silently if {@code height} is too large.
     */
    void setSkyLightLevel(int x, int y, int height, int skyLightLevel);

    /**
     * Determine whether the world contains any data in a particular chunk (a
     * 16 by 16 block area).
     *
     * @param x The X coordinate in chunk coordinates.
     * @param y The Y coordinate in chunk coordinates.
     * @return {@code true} if data is present for the specified chunk.
     */
    boolean isChunkPresent(int x, int y);

    /**
     * Add a chunk. Not all implementations support adding chunks, and some may
     * only accept chunks belonging to a specific {@link Platform}.
     *
     * @param chunk The chunk to add.
     */
    void addChunk(Chunk chunk);

    /**
     * Get the Z coordinate of the highest non-air block in a specific column. This is allowed to be an approximation,
     * as long as it is the same or higher than the actual highest non-air block.
     *
     * @param x The X coordinate of the column.
     * @param y The Y coordinate of the column.
     * @return The Z coordinate of the highest non-air block in the specified column or {@link Integer#MIN_VALUE} if the
     * column is empty or no data is present
     * for the specified coordinates.
     */
    int getHighestNonAirBlock(int x, int y);
}