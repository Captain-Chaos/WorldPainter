/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.jnbt.CompoundTag;
import org.jnbt.Tag;
import static org.pepsoft.minecraft.Constants.*;

/**
 * An "MCRegion" chunk.
 * 
 * @author pepijn
 */
public final class ChunkImpl extends AbstractNBTItem implements Chunk {
    public ChunkImpl(int xPos, int zPos, int maxHeight) {
        super(new CompoundTag(TAG_LEVEL, new HashMap<>()));
        this.xPos = xPos;
        this.zPos = zPos;
        this.maxHeight = maxHeight;

        blocks = new byte[256 * maxHeight];
        data = new byte[128 * maxHeight];
        skyLight = new byte[128 * maxHeight];
        blockLight = new byte[128 * maxHeight];
        heightMap = new byte[256];
        entities = new ArrayList<>();
        tileEntities = new ArrayList<>();
        readOnly = false;
    }

    public ChunkImpl(CompoundTag tag, int maxHeight) {
        this(tag, maxHeight, false);
    }

    public ChunkImpl(CompoundTag tag, int maxHeight, boolean readOnly) {
        super((CompoundTag) tag.getTag(TAG_LEVEL));
        this.maxHeight = maxHeight;
        this.readOnly = readOnly;
        
        blocks = getByteArray(TAG_BLOCKS);
        data = getByteArray(TAG_DATA);
        skyLight = getByteArray(TAG_SKY_LIGHT);
        blockLight = getByteArray(TAG_BLOCK_LIGHT);
        heightMap = getByteArray(TAG_HEIGHT_MAP);
        List<CompoundTag> entityTags = getList(TAG_ENTITIES);
        entities = new ArrayList<>(entityTags.size());
        entities.addAll(entityTags.stream().map(Entity::fromNBT).collect(Collectors.toList()));
        List<CompoundTag> tileEntityTags = getList(TAG_TILE_ENTITIES);
        tileEntities = new ArrayList<>(tileEntityTags.size());
        tileEntities.addAll(tileEntityTags.stream().map(TileEntity::fromNBT).collect(Collectors.toList()));
        // TODO: last update is ignored, is that correct?
        xPos = getInt(TAG_X_POS);
        zPos = getInt(TAG_Z_POS);
        terrainPopulated = getBoolean(TAG_TERRAIN_POPULATED);
    }

    @Override
    public Tag toNBT() {
        setByteArray(TAG_BLOCKS, blocks);
        setByteArray(TAG_DATA, data);
        setByteArray(TAG_SKY_LIGHT, skyLight);
        setByteArray(TAG_BLOCK_LIGHT, blockLight);
        setByteArray(TAG_HEIGHT_MAP, heightMap);
        List<Tag> entityTags = new ArrayList<>(entities.size());
        entityTags.addAll(entities.stream().map(Entity::toNBT).collect(Collectors.toList()));
        setList(TAG_ENTITIES, CompoundTag.class, entityTags);
        List<Tag> tileEntityTags = new ArrayList<>(entities.size());
        tileEntityTags.addAll(tileEntities.stream().map(TileEntity::toNBT).collect(Collectors.toList()));
        setList(TAG_TILE_ENTITIES, CompoundTag.class, tileEntityTags);
        setLong(TAG_LAST_UPDATE, System.currentTimeMillis());
        setInt(TAG_X_POS, xPos);
        setInt(TAG_Z_POS, zPos);
        setBoolean(TAG_TERRAIN_POPULATED, terrainPopulated);

        return new CompoundTag("", Collections.singletonMap("", super.toNBT()));
    }

    @Override
    public int getMaxHeight() {
        return maxHeight;
    }

    @Override
    public int getxPos() {
        return xPos;
    }

    @Override
    public int getzPos() {
        return zPos;
    }

    @Override
    public Point getCoords() {
        return new Point(xPos, zPos);
    }

    @Override
    public int getBlockType(int x, int y, int z) {
        return blocks[blockOffset(x, y, z)] & 0xFF;
    }

    @Override
    public void setBlockType(int x, int y, int z, int blockType) {
        if (readOnly) {
            return;
        }
        blocks[blockOffset(x, y, z)] = (byte) blockType;
    }

    @Override
    public int getDataValue(int x, int y, int z) {
        return getDataByte(data, x, y, z);
    }

    @Override
    public void setDataValue(int x, int y, int z, int dataValue) {
        if (readOnly) {
            return;
        }
        setDataByte(data, x, y, z, dataValue);
    }

    @Override
    public int getSkyLightLevel(int x, int y, int z) {
        return getDataByte(skyLight, x, y, z);
    }

    @Override
    public void setSkyLightLevel(int x, int y, int z, int skyLightLevel) {
        if (readOnly) {
            return;
        }
        setDataByte(skyLight, x, y, z, skyLightLevel);
    }

    @Override
    public int getBlockLightLevel(int x, int y, int z) {
        return getDataByte(blockLight, x, y, z);
    }

    @Override
    public void setBlockLightLevel(int x, int y, int z, int blockLightLevel) {
        if (readOnly) {
            return;
        }
        setDataByte(blockLight, x, y, z, blockLightLevel);
    }

    @Override
    public boolean isBiomesAvailable() {
        throw new UnsupportedOperationException("Not supported");
    }
    
    @Override
    public int getBiome(int x, int z) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public void setBiome(int x, int z, int biome) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public int getHeight(int x, int z) {
        return heightMap[x + z * 16] & 0xFF;
    }

    @Override
    public void setHeight(int x, int z, int height) {
        if (readOnly) {
            return;
        }
        heightMap[x + z * 16] = (byte) (Math.min(height, 255));
    }

    @Override
    public boolean isTerrainPopulated() {
        return terrainPopulated;
    }

    @Override
    public void setTerrainPopulated(boolean terrainPopulated) {
        if (readOnly) {
            return;
        }
        this.terrainPopulated = terrainPopulated;
    }

    @Override
    public List<Entity> getEntities() {
        return entities;
    }

    @Override
    public List<TileEntity> getTileEntities() {
        return tileEntities;
    }

    @Override
    public Material getMaterial(int x, int y, int z) {
        return Material.get(getBlockType(x, y, z), getDataValue(x, y, z));
    }

    @Override
    public void setMaterial(int x, int y, int z, Material material) {
        setBlockType(x, y, z, material.blockType);
        setDataValue(x, y, z, material.data);
    }

    @Override
    public boolean isReadOnly() {
        return readOnly;
    }

    @Override
    public boolean isLightPopulated() {
        return false;
    }

    @Override
    public void setLightPopulated(boolean lightPopulated) {
        // Do nothing
    }

    @Override
    public long getInhabitedTime() {
        return 0;
    }

    @Override
    public void setInhabitedTime(long inhabitedTime) {
        // Do nothing
    }

    @Override
    public int getHighestNonAirBlock(int x, int z) {
        final int base = blockOffset(x, 0, z);
        for (int i = blockOffset(x, maxHeight - 1, z); i >= base; i--) {
            if (blocks[i] != 0) {
                return i - base;
            }
        }
        return -1;
    }

    @Override
    public int getHighestNonAirBlock() {
        for (int y = maxHeight - 1; y >= 0; y--) {
            for (int i = 0; i < blocks.length; i += maxHeight) {
                if (blocks[i | y] != 0) {
                    return y;
                }
            }
        }
        return -1;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ChunkImpl other = (ChunkImpl) obj;
        if (this.xPos != other.xPos) {
            return false;
        }
        if (this.zPos != other.zPos) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + this.xPos;
        hash = 67 * hash + this.zPos;
        return hash;
    }

    /**
     * @throws UnsupportedOperationException
     */
    @Override
    public ChunkImpl clone() {
        throw new UnsupportedOperationException("ChunkImlp.clone() not supported");
    }

    private int getDataByte(byte[] array, int x, int y, int z) {
        byte dataByte = array[blockOffset(x, y, z) / 2];
        if (blockOffset(x, y, z) % 2 == 0) {
            // Even byte -> least significant bits
            return dataByte & 0x0F;
        } else {
            // Odd byte -> most significant bits
            return (dataByte & 0xF0) >> 4;
        }
    }

    private void setDataByte(byte[] array, int x, int y, int z, int dataValue) {
        int blockOffset = blockOffset(x, y, z);
        int offset = blockOffset / 2;
        byte dataByte = array[offset];
        if (blockOffset % 2 == 0) {
            // Even byte -> least significant bits
            dataByte &= 0xF0;
            dataByte |= (dataValue & 0x0F);
        } else {
            // Odd byte -> most significant bits
            dataByte &= 0x0F;
            dataByte |= ((dataValue & 0x0F) << 4);
        }
        array[offset] = dataByte;
    }

    private int blockOffset(int x, int y, int z) {
        return y + (z + x * 16) * maxHeight;
    }

    public final boolean readOnly;

    final byte[] blocks;
    final byte[] data;
    final byte[] skyLight;
    final byte[] blockLight;
    final byte[] heightMap;
    final int xPos, zPos;
    boolean terrainPopulated;
    final List<Entity> entities;
    final List<TileEntity> tileEntities;
    final int maxHeight;

    private static final long serialVersionUID = 1L;
}