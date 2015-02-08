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

    void addChunk(Chunk chunk);
}