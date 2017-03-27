/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.objects;

import org.pepsoft.minecraft.Chunk;
import org.pepsoft.minecraft.Entity;
import org.pepsoft.minecraft.Material;
import org.pepsoft.minecraft.TileEntity;
import org.pepsoft.util.AttributeKey;
import org.pepsoft.util.Box;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;

import javax.vecmath.Point3i;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.pepsoft.minecraft.Constants.BLK_AIR;
import static org.pepsoft.minecraft.Material.AIR;

/**
 * A memory only combination of {@link MinecraftWorld} and {@link WPObject},
 * allowing to render worlds and layers to it and then treat it as an object,
 * for instance for generating previews. As such it does not support entities,
 * tile entities, lighting information, etc., just basic block info. Trying to
 * use the unsupported features will fail silently, except for the chunk related
 * operations, which will throw an {@link UnsupportedOperationException}. The
 * exception is adding chunks, which works by copying the block data over.
 * 
 * <p>For the <code>MinecraftWorld</code> interface the supported coordinates
 * are those specified by the <code>volume</code> parameter. For the
 * <code>WPObject</code> interface, the coordinates are translated so that the
 * lower north west corner is at 0,0,0. In other words, the
 * <code>WPObject</code> has no offset and all the coordinates are positive.
 *
 * <p>An offset may in fact be specified but it has no effect on the coordinates
 * used by this object; it is purely meant to communicate to a consumer of the
 * <code>WPObject</code> that the object should be shifted when placed.
 * 
 * @author SchmitzP
 */
public final class MinecraftWorldObject implements MinecraftWorld, WPObject {
    /**
     * Create a new <code>MinecraftWorldObject</code> which is initialised with
     * all air and has no offset.
     *
     * @param name The name of the world/object.
     * @param volume The volume of blocks in world coordinates which the object should encompass. Blocks outside this
     *               volume are returned as air for read operations and silently ignored for write operations.
     * @param maxHeight The height to return from {@link MinecraftWorld#getMaxHeight()}. Must be a power of two and may
     *                  be higher than the volume; that just means the blocks between the top of the volume and
     *                  maxHeight won't be stored.
     */
    public MinecraftWorldObject(String name, Box volume, int maxHeight) {
        this(name, volume, maxHeight, null, new Point3i(0, 0, 0));
    }

    /**
     * Create a new <code>MinecraftWorldObject</code>.
     *
     * @param name The name of the world/object.
     * @param volume The volume of blocks in world coordinates which the object should encompass. Blocks outside this
     *               volume are returned as air for read operations and silently ignored for write operations.
     * @param maxHeight The height to return from {@link MinecraftWorld#getMaxHeight()}. Must be a power of two and may
     *                  be higher than the volume; that just means the blocks between the top of the volume and
     *                  maxHeight won't be stored.
     * @param lowestBlocks An optional column of materials with which the bottom of the volume should be filled. All
     *                     other blocks will be initialised as air. May be <code>null</code>, in which case all blocks
     *                     will be initialised as air.
     * @param offset The offset to return from {@link WPObject#getOffset()}.
     */
    public MinecraftWorldObject(String name, Box volume, int maxHeight, Material[] lowestBlocks, Point3i offset) {
        this.name = name;
        this.volume = volume;
        this.maxHeight = maxHeight;
        this.offset = offset;
        dx = volume.getX1();
        dy = volume.getY1();
        dz = volume.getZ1();
        dimensions = new Point3i(volume.getWidth(), volume.getLength(), volume.getHeight());
        data = new short[volume.getWidth()][volume.getLength()][volume.getHeight()];
        if (lowestBlocks != null) {
            this.lowestBlocks = new short[lowestBlocks.length];
            for (int i = 0; i < lowestBlocks.length; i++) {
                this.lowestBlocks[i] = (short) ((lowestBlocks[i].blockType << 4) | lowestBlocks[i].data);
            }
            for (short[][] slice: data) {
                for (short[] row: slice) {
                    System.arraycopy(this.lowestBlocks, 0, row, 0, this.lowestBlocks.length);
                }
            }
        } else {
            this.lowestBlocks = null;
        }
    }

    // Copy constructor for clone()
    private MinecraftWorldObject(String name, Box volume, int maxHeight, short[][][] data, short[] lowestBlocks, Point3i offset) {
        this.name = name;
        this.volume = volume;
        this.maxHeight = maxHeight;
        this.data = data;
        this.lowestBlocks = lowestBlocks;
        dx = volume.getX1();
        dy = volume.getY1();
        dz = volume.getZ1();
        dimensions = new Point3i(volume.getWidth(), volume.getLength(), volume.getHeight());
        this.offset = offset;
    }
    
    public void reset() {
        for (short[][] slice: data) {
            for (short[] row: slice) {
                Arrays.fill(row, (short) 0);
                if ((lowestBlocks != null) && (lowestBlocks.length > 0)) {
                    System.arraycopy(lowestBlocks, 0, row, 0, lowestBlocks.length);
                }
            }
        }
    }

    public Box getVolume() {
        return volume.clone();
    }

    // MinecraftWorld
    
    @Override
    public int getBlockTypeAt(int x, int y, int height) {
        if (volume.contains(x, y, height)) {
            return data[x - dx][y - dy][height - dz] >> 4;
        } else {
            return BLK_AIR;
        }
    }

    @Override
    public int getDataAt(int x, int y, int height) {
        if (volume.contains(x, y, height)) {
            return data[x - dx][y - dy][height - dz] & 0xf;
        } else {
            return 0;
        }
    }

    @Override
    public Material getMaterialAt(int x, int y, int height) {
        if (volume.contains(x, y, height)) {
            return Material.getByCombinedIndex(data[x - dx][y - dy][height - dz]);
        } else {
            return AIR;
        }
    }

    @Override
    public void setBlockTypeAt(int x, int y, int height, int blockType) {
        if (volume.contains(x, y, height)) {
            data[x - dx][y - dy][height - dz] = (short) ((blockType << 4) | (data[x - dx][y - dy][height - dz] & 0xf));
        }
    }

    @Override
    public void setDataAt(int x, int y, int height, int data) {
        if (volume.contains(x, y, height)) {
            this.data[x - dx][y - dy][height - dz] = (short) ((this.data[x - dx][y - dy][height - dz] & 0xfff0) | data);
        }
    }

    @Override
    public void setMaterialAt(int x, int y, int height, Material material) {
        if (volume.contains(x, y, height)) {
            data[x - dx][y - dy][height - dz] = (short) material.index;
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
    public boolean isChunkPresent(int x, int y) {
        return volume.containsXY(x << 4, y << 4);
    }

    /**
     * Copies the block IDs and data values from the specified chunk to this
     * object, insofar as it intersects the object bounds.
     *
     * @param chunk The chunk to copy.
     */
    public void addChunk(Chunk chunk) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int blockX = (chunk.getxPos() << 4) | x, blockY = (chunk.getzPos() << 4) | z;
                if (volume.containsXY(blockX, blockY)) {
                    for (int y = Math.min(chunk.getHighestNonAirBlock(x, z), dz + volume.getHeight() - 1); y >= dz; y--) {
                        data[blockX - dx][blockY - dy][y - dz] = (short) chunk.getMaterial(x, y, z).index;
                    }
                }
            }
        }
    }

    @Override
    public int getHighestNonAirBlock(int x, int y) {
        if (volume.containsXY(x, y)) {
            for (int z = volume.getHeight() - 1; z >= 0; z--) {
                if (data[x - dx][y - dy][z] != 0) {
                    return z + dz;
                }
            }
            return -1;
        } else {
            return -1;
        }
    }

    @Override
    public Chunk getChunk(int x, int z) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public Chunk getChunkForEditing(int x, int z) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public void close() {
        // Do nothing
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
        return Material.getByCombinedIndex(data[x][y][z]);
    }

    @Override
    public boolean getMask(int x, int y, int z) {
        return data[x][y][z] != 0;
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
    public void prepareForExport(Dimension dimension) {
        // Do nothing
    }

    @Override
    public Map<String, Serializable> getAttributes() {
        return null;
    }

    @Override
    public <T extends Serializable> T getAttribute(AttributeKey<T> key) {
        if (key.equals(ATTRIBUTE_OFFSET)) {
            //noinspection unchecked // Responsibility of caller
            return (T) offset;
        } else {
            return key.defaultValue;
        }
    }

    @Override
    public void setAttributes(Map<String, Serializable> attributes) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public <T extends Serializable> void setAttribute(AttributeKey<T> key, T value) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public Point3i getOffset() {
        return getAttribute(ATTRIBUTE_OFFSET);
    }

    @Override
    @SuppressWarnings("CloneDoesntCallSuperClone")
    public MinecraftWorldObject clone() {
        return new MinecraftWorldObject(name, volume.clone(), maxHeight, data.clone(), lowestBlocks.clone(), (Point3i) offset.clone());
    }
    
    private final String name;
    private final Box volume;
    private final int dx, dy, dz, maxHeight;
    private final Point3i dimensions, offset;
    private final short[][][] data;
    private final short[] lowestBlocks;
}