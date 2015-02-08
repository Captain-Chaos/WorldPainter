/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.objects;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.vecmath.Point3i;
import org.pepsoft.minecraft.Chunk;
import static org.pepsoft.minecraft.Constants.*;
import org.pepsoft.minecraft.Entity;
import org.pepsoft.minecraft.Material;
import static org.pepsoft.minecraft.Material.*;
import org.pepsoft.minecraft.TileEntity;
import org.pepsoft.util.Box;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;

/**
 * A memory only combination of {@link MinecraftWorld} and {@link WPObject},
 * allowing to render worlds and layers to it and then treat it as an object,
 * for instance for generating previews. As such it does not support entities,
 * tile entities, lighting information, etc., just basic block info. Trying to
 * use the unsupported features will fail silently, except for the chunk related
 * operations, which will throw an {@link UnsupportedOperationException}.
 * 
 * <p>For the <code>MinecraftWorld</code> interface the supported coordinates
 * are those specified by the <code>volume</code> parameter. For the
 * <code>WPObject</code> interface, the coordinates are translated so that the
 * lower north west corner is at 0,0,0. In other words, the
 * <code>WPObject</code> has no offset and all the coordinates are positive.
 * 
 * @author SchmitzP
 */
public final class MinecraftWorldObject implements MinecraftWorld, WPObject {
    public MinecraftWorldObject(String name, Box volume, int maxHeight) {
        this(name, volume, maxHeight, null);
    }
    
    public MinecraftWorldObject(String name, Box volume, int maxHeight, Material[] lowestBlocks) {
        this.name = name;
        this.volume = volume;
        this.maxHeight = maxHeight;
        this.lowestBlocks = lowestBlocks;
        dx = volume.getX1();
        dy = volume.getY1();
        dz = volume.getZ1();
        dimensions = new Point3i(volume.getWidth(), volume.getLength(), volume.getHeight());
        data = new Material[volume.getWidth()][volume.getLength()][volume.getHeight()];
    }

    // Copy constructor for clone()
    private MinecraftWorldObject(String name, Box volume, int maxHeight, Material[][][] data, Material[] lowestBlocks) {
        this.name = name;
        this.volume = volume;
        this.maxHeight = maxHeight;
        this.data = data;
        this.lowestBlocks = lowestBlocks;
        dx = volume.getX1();
        dy = volume.getY1();
        dz = volume.getZ1();
        dimensions = new Point3i(volume.getWidth(), volume.getLength(), volume.getHeight());
    }
    
    public void reset() {
        for (Material[][] slice: data) {
            for (Material[] row: slice) {
                Arrays.fill(row, AIR);
                if ((lowestBlocks != null) && (lowestBlocks.length > 0)) {
                    System.arraycopy(lowestBlocks, 0, row, 0, lowestBlocks.length);
                }
            }
        }
    }
    
    // MinecraftWorld
    
    @Override
    public int getBlockTypeAt(int x, int y, int height) {
        if (volume.contains(x, y, height)) {
            return data[x - dx][y - dy][height - dz].getBlockType();
        } else {
            return BLK_AIR;
        }
    }

    @Override
    public int getDataAt(int x, int y, int height) {
        if (volume.contains(x, y, height)) {
            return data[x - dx][y - dy][height - dz].getData();
        } else {
            return 0;
        }
    }

    @Override
    public Material getMaterialAt(int x, int y, int height) {
        if (volume.contains(x, y, height)) {
            return data[x - dx][y - dy][height - dz];
        } else {
            return AIR;
        }
    }

    @Override
    public void setBlockTypeAt(int x, int y, int height, int blockType) {
        if (volume.contains(x, y, height)) {
            data[x - dx][y - dy][height - dz] = Material.get(blockType, data[x - dx][y - dy][height - dz].getData());
        }
    }

    @Override
    public void setDataAt(int x, int y, int height, int data) {
        if (volume.contains(x, y, height)) {
            this.data[x - dx][y - dy][height - dz] = Material.get(this.data[x - dx][y - dy][height - dz].getBlockType(), data);
        }
    }

    @Override
    public void setMaterialAt(int x, int y, int height, Material material) {
        if (volume.contains(x, y, height)) {
            data[x - dx][y - dy][height - dz] = material;
        }
    }

    @Override
    public int getMaxHeight() {
        return maxHeight;
    }

    @Override
    public void addEntity(int x, int y, int height, Entity entity) {
        // Do nothing
    }

    @Override
    public void addEntity(double x, double y, double height, Entity entity) {
        // Do nothing
    }

    @Override
    public void addTileEntity(int x, int y, int height, TileEntity tileEntity) {
        // Do nothing
    }

    @Override
    public int getBlockLightLevel(int x, int y, int height) {
        return 0;
    }

    @Override
    public void setBlockLightLevel(int x, int y, int height, int blockLightLevel) {
        // Do nothing
    }

    @Override
    public int getSkyLightLevel(int x, int y, int height) {
        return 15;
    }

    @Override
    public void setSkyLightLevel(int x, int y, int height, int skyLightLevel) {
        // Do nothing
    }

    @Override
    public void addChunk(Chunk chunk) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public Chunk getChunk(int x, int z) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public Chunk getChunkForEditing(int x, int z) {
        throw new UnsupportedOperationException("Not supported");
    }

    // WPObject
    
    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public Point3i getDimensions() {
        return dimensions;
    }

    @Override
    public Material getMaterial(int x, int y, int z) {
        return data[x][y][z];
    }

    @Override
    public boolean getMask(int x, int y, int z) {
        return data[x][y][z] != AIR;
    }

    @Override
    public List<Entity> getEntities() {
        return null;
    }

    @Override
    public List<TileEntity> getTileEntities() {
        return null;
    }

    @Override
    public Map<String, Serializable> getAttributes() {
        return null;
    }

    @Override
    public <T extends Serializable> T getAttribute(String key, T _default) {
        return _default;
    }

    @Override
    public void setAttributes(Map<String, Serializable> attributes) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public void setAttribute(String key, Serializable value) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    @SuppressWarnings("CloneDoesntCallSuperClone")
    public MinecraftWorldObject clone() {
        return new MinecraftWorldObject(name, volume.clone(), maxHeight, data.clone(), lowestBlocks.clone());
    }
    
    private final String name;
    private final Box volume;
    private final int dx, dy, dz, maxHeight;
    private final Point3i dimensions;
    private final Material[][][] data;
    private final Material[] lowestBlocks;
}