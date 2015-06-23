/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft;

import java.awt.Point;
import java.util.List;

/**
 *
 * @author pepijn
 */
public interface Chunk extends NBTItem {
    int getBlockLightLevel(int x, int y, int z);

    void setBlockLightLevel(int x, int y, int z, int blockLightLevel);

    int getBlockType(int x, int y, int z);

    void setBlockType(int x, int y, int z, int blockType);

    int getDataValue(int x, int y, int z);

    void setDataValue(int x, int y, int z, int dataValue);

    int getHeight(int x, int z);

    void setHeight(int x, int z, int height);

    int getSkyLightLevel(int x, int y, int z);

    void setSkyLightLevel(int x, int y, int z, int skyLightLevel);

    int getxPos();

    int getzPos();

    Point getCoords();

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
     * value) block in a specific column.
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
     * value) block in the chunk.
     *
     * @return The Y coordinate of the highest non-air (block ID zero, any data
     *     value) block in the chunk or <code>-1</code> if the chunk is empty.
     */
    int getHighestNonAirBlock();
}