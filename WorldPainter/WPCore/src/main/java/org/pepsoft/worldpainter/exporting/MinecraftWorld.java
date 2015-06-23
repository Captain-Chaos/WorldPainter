/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.exporting;

import org.pepsoft.minecraft.Chunk;
import org.pepsoft.minecraft.ChunkProvider;
import org.pepsoft.minecraft.Constants;
import org.pepsoft.minecraft.Entity;
import org.pepsoft.minecraft.Material;
import org.pepsoft.minecraft.TileEntity;

/**
 *
 * @author pepijn
 */
public interface MinecraftWorld extends ChunkProvider {
    /**
     * Returns {@link Constants#BLK_AIR} if {@code height} is too large.
     */
    int getBlockTypeAt(int x, int y, int height);

    /**
     * Returns {@code 0} if {@code height} is too large.
     */
    int getDataAt(int x, int y, int height);

    /**
     * Returns {@code null} if {@code height} is too large.
     */
    Material getMaterialAt(int x, int y, int height);

    /**
     * Fails silently if {@code height} is too large.
     */
    void setBlockTypeAt(int x, int y, int height, int blockType);

    /**
     * Fails silently if {@code height} is too large.
     */
    void setDataAt(int x, int y, int height, int data);

    /**
     * Fails silently if {@code height} is too large.
     */
    void setMaterialAt(int x, int y, int height, Material material);
    
    int getMaxHeight();

    void addEntity(int x, int y, int height, Entity entity);

    void addEntity(double x, double y, double height, Entity entity);

    void addTileEntity(int x, int y, int height, TileEntity tileEntity);
    
    /**
     * Returns {@code 0} if {@code height} is too large.
     */
    int getBlockLightLevel(int x, int y, int height);

    /**
     * Returns {@code 15} if {@code height} is too large.
     */
    void setBlockLightLevel(int x, int y, int height, int blockLightLevel);

    /**
     * Fails silently if {@code height} is too large.
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
     * @return <code>true</code> if data is present for the specified chunk.
     */
    boolean isChunkPresent(int x, int y);

    void addChunk(Chunk chunk);

    /**
     * Get the Z coordinate of the highest non-air (block ID zero, any data
     * value) block in a specific column.
     *
     * @param x The X coordinate of the column.
     * @param y The Y coordinate of the column.
     * @return The Z coordinate of the highest non-air (block ID zero, any data
     *     value) block in the specified column or <code>-1</code> if the column
     *     is empty or no data is present for the specified coordinates.
     */
    int getHighestNonAirBlock(int x, int y);
}