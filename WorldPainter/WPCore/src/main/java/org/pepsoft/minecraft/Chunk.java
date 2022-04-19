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
    @Deprecated
    int getBlockType(int x, int y, int z);

    /**
     * @deprecated Use {@link #setMaterial(int, int, int, Material)}
     */
    @Deprecated
    void setBlockType(int x, int y, int z, int blockType);

    /**
     * @deprecated Use {@link #getMaterial(int, int, int)}
     */
    @Deprecated
    int getDataValue(int x, int y, int z);

    /**
     * @deprecated Use {@link #setMaterial(int, int, int, Material)}
     */
    @Deprecated
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

    /**
     * Get a list of entities contained in this chunk. This list must be an editable live view; in other words it must
     * be possible to add or remove entities by editing this list.
     *
     * @return An editable live view of the entities contained in this chunk.
     */
    List<Entity> getEntities();

    /**
     * Get a list of tile entities (also known as block entities) contained in this chunk. This list must be an editable
     * live view; in other words it must be possible to add or remove tile entities by editing this list.
     *
     * @return An editable live view of the tile entities contained in this chunk.
     */
    List<TileEntity> getTileEntities();

    default int getMinHeight() {
        return 0;
    }

    int getMaxHeight();

    /**
     * Indicates whether 2D biomes are supported. See {@link #getBiome(int, int)} and {@link #setBiome(int, int, int)}.
     *
     * <p>The default implementation returns {@code false}.
     */
    default boolean isBiomesSupported() {
        return false;
    }

    /**
     * Indicates whether 2D biomes are available. See {@link #getBiome(int, int)} and {@link #setBiome(int, int, int)}.
     *
     * <p>The default implementation returns {@code false}.
     */
    default boolean isBiomesAvailable() {
        return false;
    }

    /**
     * Indicates whether 3D biomes are supported. See {@link #get3DBiome(int, int, int)} and
     * {@link #set3DBiome(int, int, int, int)}.
     *
     * <p>The default implemenation returns {@code false}.
     */
    default boolean is3DBiomesSupported() {
        return false;
    }

    /**
     * Indicates whether 3D biomes are available. See {@link #get3DBiome(int, int, int)} and
     * {@link #set3DBiome(int, int, int, int)}.
     *
     * <p>The default implemenation returns {@code false}.
     */
    default boolean is3DBiomesAvailable() {
        return false;
    }

    /**
     * Indicates whether named biomes are supported. See {@link #getNamedBiome(int, int, int)} and
     * {@link #setNamedBiome(int, int, int, String)}.
     *
     * <p>The default implementation returns {@code false}.
     */
    default boolean isNamedBiomesSupported() {
        return false;
    }

    /**
     * Indicates whether named biomes are available. See {@link #getNamedBiome(int, int, int)} and
     * {@link #setNamedBiome(int, int, int, String)}.
     *
     * <p>The default implementation returns {@code false}.
     */
    default boolean isNamedBiomesAvailable() {
        return false;
    }

    /**
     * Get a 2D biome, stored per column. Throws an {@link UnsupportedOperationException} when invoked on a format which
     * does not support 2D biomes.
     */
    default int getBiome(int x, int z) {
        throw new UnsupportedOperationException("Not supported");
    }

    /**
     * Set a 2D biome, stored per column. Throws an {@link UnsupportedOperationException} when invoked on a format which
     * does not support 2D biomes.
     */
    default void setBiome(int x, int z, int biome) {
        throw new UnsupportedOperationException("Not supported");
    }

    /**
     * Get a 3D biome, stored per 4x4x4 block. The coordinates are that of the 4x4x4 block, not of an individual block.
     * Throws an {@link UnsupportedOperationException} when invoked on a format which does not support 3D biomes.
     */
    default int get3DBiome(int x, int y, int z) {
        throw new UnsupportedOperationException("Not supported");
    }

    /**
     * Set a 3D biome, stored per 4x4x4 block. The coordinates are that of the 4x4x4 block, not of an individual block.
     * Throws an {@link UnsupportedOperationException} when invoked on a format which does not support 3D biomes.
     */
    default void set3DBiome(int x, int y, int z, int biome) {
        throw new UnsupportedOperationException("Not supported");
    }

    /**
     * Get a modern biome, stored per 4x4x4 block. The coordinates are that of the 4x4x4 block, not of an individual
     * block. Throws an {@link UnsupportedOperationException} when invoked on a format which does not support modern
     * biomes.
     */
    default String getNamedBiome(int x, int y, int z) {
        throw new UnsupportedOperationException("Not supported");
    }

    /**
     * Set a modern biome, stored per 4x4x4 block. The coordinates are that of the 4x4x4 block, not of an individual
     * block. Throws an {@link UnsupportedOperationException} when invoked on a format which does not support named
     * biomes.
     */
    default void setNamedBiome(int x, int y, int z, String biome) {
        throw new UnsupportedOperationException("Not supported");
    }

    /**
     * Mark a block to be updated by Minecraft when next loaded. Coordinates local to chunk.
     *
     * <p>The default implementation does nothing.
     */
    default void markForUpdateChunk(int x, int y, int z) {
        // Do nothing
    }

    boolean isReadOnly();

    boolean isLightPopulated();

    void setLightPopulated(boolean lightPopulated);

    long getInhabitedTime();

    void setInhabitedTime(long inhabitedTime);

    /**
     * Get the Y coordinate of the highest non-air (block ID zero, any data value) block in a specific column. This is
     * allowed to be an approximation, as long as it is the same or higher than the actual highest non-air block.
     *
     * @param x The X coordinate of the column relative to the chunk.
     * @param z The Z coordinate of the column relative to the chunk.
     * @return The Y coordinate of the highest non-air (block ID zero, any data value) block in the specified column or
     * {@link Integer#MIN_VALUE} if the column is empty.
     */
    int getHighestNonAirBlock(int x, int z);

    /**
     * Get the Y coordinate of the highest non-air (block ID zero, any data value) block in the chunk. This is allowed
     * to be an approximation, as long as it is the same or higher than the actual highest non-air block.
     *
     * @return The Y coordinate of the highest non-air (block ID zero, any data value) block in the chunk or
     * {@link Integer#MIN_VALUE} if the chunk is empty.
     */
    int getHighestNonAirBlock();
}