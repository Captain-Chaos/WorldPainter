package org.pepsoft.minecraft.mcpe;

import org.pepsoft.minecraft.Chunk;
import org.pepsoft.minecraft.Entity;
import org.pepsoft.minecraft.Material;
import org.pepsoft.minecraft.TileEntity;

import java.awt.*;
import java.util.List;

/**
 * Created by Pepijn on 11-12-2016.
 */
public class MCPEChunk implements Chunk {
    public MCPEChunk(int x, int z) {
        this.x = x;
        this.z = z;
        data = new byte[83200];
    }

    public MCPEChunk(int x, int z, byte[] data) {
        if (data.length != 83200) {
            throw new IllegalArgumentException();
        }
        this.x = x;
        this.z = z;
        this.data = data;
    }

    public byte[] toBytes() {
        return data;
    }

    @Override
    public int getBlockLightLevel(int x, int y, int z) {
        if ((z % 2) == 0) {
            return data[BLOCK_LIGHT_OFFSET + (x << 8) + (z << 4) + (y >> 1)] & 0x0f;
        } else {
            return (data[BLOCK_LIGHT_OFFSET + (x << 8) + (z << 4) + (y >> 1)] & 0xf0) >> 4;
        }
    }

    @Override
    public void setBlockLightLevel(int x, int y, int z, int blockLightLevel) {
        if ((z % 2) == 0) {
            data[BLOCK_LIGHT_OFFSET + (x << 8) + (z << 4) + (y >> 1)] = (byte) ((data[BLOCK_LIGHT_OFFSET + (x << 8) + (z << 4) + (y >> 1)] & 0xf0) | blockLightLevel);
        } else {
            data[BLOCK_LIGHT_OFFSET + (x << 8) + (z << 4) + (y >> 1)] = (byte) ((data[BLOCK_LIGHT_OFFSET + (x << 8) + (z << 4) + (y >> 1)] & 0x0f) | (blockLightLevel << 4));
        }
    }

    @Override
    public int getBlockType(int x, int y, int z) {
        return data[(x << 11) | (z << 7) | y] & 0xff;
    }

    @Override
    public void setBlockType(int x, int y, int z, int blockType) {
        data[(x << 11) | (z << 7) | y] = (byte) blockType;
    }

    @Override
    public int getDataValue(int x, int y, int z) {
        if ((z % 2) == 0) {
            return data[DATA_OFFSET + ((x << 10) | (z << 6) | (y >> 1))] & 0x0f;
        } else {
            return (data[DATA_OFFSET + ((x << 10) | (z << 6) | (y >> 1))] & 0xf0) >> 4;
        }
    }

    @Override
    public void setDataValue(int x, int y, int z, int dataValue) {
        if ((z % 2) == 0) {
            data[DATA_OFFSET + (x << 8) + (z << 4) + (y >> 1)] = (byte) ((data[DATA_OFFSET + (x << 8) + (z << 4) + (y >> 1)] & 0xf0) | dataValue);
        } else {
            data[DATA_OFFSET + (x << 8) + (z << 4) + (y >> 1)] = (byte) ((data[DATA_OFFSET + (x << 8) + (z << 4) + (y >> 1)] & 0x0f) | (dataValue << 4));
        }
    }

    @Override
    public int getHeight(int x, int z) {
        return data[COLUMN_HEIGHTS_OFFSET + (x << 4) + z] & 0xff; // TODO ?
    }

    @Override
    public void setHeight(int x, int z, int height) {
        data[COLUMN_HEIGHTS_OFFSET + (x << 4) + z] = (byte) height;
    }

    @Override
    public int getSkyLightLevel(int x, int y, int z) {
        if ((z % 2) == 0) {
            return data[SKY_LIGHT_OFFSET + (x << 8) + (z << 4) + (y >> 1)] & 0x0f;
        } else {
            return (data[SKY_LIGHT_OFFSET + (x << 8) + (z << 4) + (y >> 1)] & 0xf0) >> 4;
        }
    }

    @Override
    public void setSkyLightLevel(int x, int y, int z, int skyLightLevel) {
        if ((z % 2) == 0) {
            data[SKY_LIGHT_OFFSET + (x << 8) + (z << 4) + (y >> 1)] = (byte) ((data[SKY_LIGHT_OFFSET + (x << 8) + (z << 4) + (y >> 1)] & 0xf0) | skyLightLevel);
        } else {
            data[SKY_LIGHT_OFFSET + (x << 8) + (z << 4) + (y >> 1)] = (byte) ((data[SKY_LIGHT_OFFSET + (x << 8) + (z << 4) + (y >> 1)] & 0x0f) | (skyLightLevel << 4));
        }
    }

    @Override
    public int getxPos() {
        return x;
    }

    @Override
    public int getzPos() {
        return z;
    }

    @Override
    public Point getCoords() {
        return new Point(x, z);
    }

    @Override
    public boolean isTerrainPopulated() {
        return terrainPopulated; // TODO
    }

    @Override
    public void setTerrainPopulated(boolean terrainPopulated) {
        this.terrainPopulated = terrainPopulated;
    }

    @Override
    public Material getMaterial(int x, int y, int z) {
        return Material.get(getBlockType(x, y, z), getDataValue(z, y, z));
    }

    @Override
    public void setMaterial(int x, int y, int z, Material material) {
        setBlockType(x, y, z, material.blockType);
        setDataValue(x, y, z, material.data);
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
    public int getMaxHeight() {
        return 0;
    }

    @Override
    public boolean isBiomesAvailable() {
        return true;
    }

    @Override
    public int getBiome(int x, int z) {
        return data[BIOMES_OFFSET + (x << 6) + (z << 2)] & 0xff;
    }

    @Override
    public void setBiome(int x, int z, int biome) {
        data[BIOMES_OFFSET + (x << 6) + (z << 2)] = (byte) biome;
    }

    @Override
    public boolean isReadOnly() {
        return readOnly;
    }

    @Override
    public boolean isLightPopulated() {
        return lightPopulated; // TODO
    }

    @Override
    public void setLightPopulated(boolean lightPopulated) {
        this.lightPopulated = lightPopulated;
    }

    @Override
    public long getInhabitedTime() {
        return inhabitedTime; // TODO
    }

    @Override
    public void setInhabitedTime(long inhabitedTime) {
        this.inhabitedTime = inhabitedTime;
    }

    @Override
    public int getHighestNonAirBlock(int x, int z) {
        return 0; // TODO
    }

    @Override
    public int getHighestNonAirBlock() {
        return 0; // TODO
    }

    private final int x, z;
    private final byte[] data;
    private final boolean readOnly = false;
    private boolean terrainPopulated, lightPopulated;
    private long inhabitedTime;

    private static final int DATA_OFFSET           = 32768;
    private static final int SKY_LIGHT_OFFSET      = 49152;
    private static final int BLOCK_LIGHT_OFFSET    = 65536;
    private static final int COLUMN_HEIGHTS_OFFSET = 81920; // TODO: ?
    private static final int BIOMES_OFFSET         = 82176; // TODO: ?
}