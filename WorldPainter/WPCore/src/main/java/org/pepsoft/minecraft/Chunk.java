/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft;

import java.util.List;

/**
 * This API's coordinate system is the Minecraft coordinate system (W <- x -> E, down <- y -> up, N <- z -> S).
 *
 * @author pepijn
 */
public interface Chunk {
    int getBlockLightLevel(int x, int y, int z);

    void setBlockLightLevel(int x, int y, int z, int blockLightLevel);

    /**
     * @deprecated Use {@link #getMaterial(int, int, int)}
     */
    int getBlockType(int x, int y, int z);

    /**
     * @deprecated Use {@link #setMaterial(int, int, int, Material)}
     */
    void setBlockType(int x, int y, int z, int blockType);

    /**
     * @deprecated Use {@link #getMaterial(int, int, int)}
     */
    int getDataValue(int x, int y, int z);

    /**
     * @deprecated Use {@link #setMaterial(int, int, int, Material)}
     */
    void setDataValue(int x, int y, int z, int dataValue);

    int getHeight(int x, int z);

    void setHeight(int x, int z, int height);

    int getSkyLightLevel(int x, int y, int z);

    void setSkyLightLevel(int x, int y, int z, int skyLightLevel);

    int getxPos();

    int getzPos();

    MinecraftCoords getCoords();

    boolean isTerrainPopulated();

    void setTerrainPopulated(boolean terrainPopulated);
    
    Material getMaterial(int x, int y, int z);
    
    void setMaterial(int x, int y, int z, Material material);

    List<Entity> getEntities();

    List<TileEntity> getTileEntities();
    
    int getMaxHeight();

    boolean isBiomesAvailable();
    
    int getBiome(int x, int z);

    void setBiome(int x, int z, int biome);

    boolean isReadOnly();

    boolean isLightPopulated();

    void setLightPopulated(boolean lightPopulated);

    long getInhabitedTime();

    void setInhabitedTime(long inhabitedTime);

    /**
     * Get the Y coordinate of the highest non-air (block ID zero, any data
     * value) block in a specific column. This is allowed to be an
     * approximation, as long as it is the same or higher than the actual
     * highest non-air block.
     *
     * @param x The X coordinate of the column relative to the chunk.
     * @param z The Z coordinate of the column relative to the chunk.
     * @return The Y coordinate of the highest non-air (block ID zero, any data
     *     value) block in the specified column or <code>-1</code> if the column
     *     is empty.
     */
    int getHighestNonAirBlock(int x, int z);

    /**
     * Get the Y coordinate of the highest non-air (block ID zero, any data
     * value) block in the chunk. This is allowed to be an approximation, as
     * long as it is the same or higher than the actual highest non-air block.
     *
     * @return The Y coordinate of the highest non-air (block ID zero, any data
     *     value) block in the chunk or <code>-1</code> if the chunk is empty.
     */
    int getHighestNonAirBlock();
}