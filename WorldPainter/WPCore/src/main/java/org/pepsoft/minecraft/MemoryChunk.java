package org.pepsoft.minecraft;

import org.pepsoft.worldpainter.exporting.MinecraftWorld;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.pepsoft.minecraft.Constants.BLK_AIR;

/**
 * A platform-independent, in-memory chunk.
 * 
 * @author Pepijn
 */
public class MemoryChunk implements Chunk, MinecraftWorld, Serializable {
    public MemoryChunk(int xPos, int zPos, int maxHeight) {
        this.xPos = xPos;
        this.zPos = zPos;
        this.maxHeight = maxHeight;

        sections = new Section[maxHeight >> 4];
        heightMap = new int[256];
        entities = new ArrayList<>();
        tileEntities = new ArrayList<>();
        readOnly = false;
        lightPopulated = true;
    }

    public boolean isSectionPresent(int y) {
        return sections[y] != null;
    }

    public Section[] getSections() {
        return sections;
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
    public MinecraftCoords getCoords() {
        return new MinecraftCoords(xPos, zPos);
    }

    @Override
    public int getBlockType(int x, int y, int z) {
        Section section = sections[y >> 4];
        if (section == null) {
            return 0;
        } else {
        	Material mat = section.blocks[blockOffset(x, y, z)];
        	return (mat != null) ? mat.blockType : BLK_AIR;
        }
    }

    @Override
    public void setBlockType(int x, int y, int z, int blockType) {
        if (readOnly) {
            return;
        }
        if (sections[y >> 4] == null && blockType == BLK_AIR) {
		    return;
		}
        throw new UnsupportedOperationException("Setting block type no longer supported");
    }

    @Override
    public int getDataValue(int x, int y, int z) {
        Section section = sections[y >> 4];
        if (section == null) {
            return 0;
        } else {
        	Material mat = section.blocks[blockOffset(x, y, z)];
        	return (mat != null) ? mat.data : 0;
        }
    }

    @Override
    public void setDataValue(int x, int y, int z, int dataValue) {
        if (readOnly) {
            return;
        }
        if (sections[y >> 4] == null && dataValue == 0) {
		    return;
		}
        throw new UnsupportedOperationException("Setting block type no longer supported");
    }

    @Override
    public int getSkyLightLevel(int x, int y, int z) {
        int level = y >> 4;
        if (sections[level] == null) {
            return 15;
        } else {
            return getDataByte(sections[level].skyLight, x, y, z);
        }
    }

    @Override
    public void setSkyLightLevel(int x, int y, int z, int skyLightLevel) {
        if (readOnly) {
            return;
        }
        int level = y >> 4;
        Section section = sections[level];
        if (section == null) {
            if (skyLightLevel == 15) {
                return;
            }
            section = new Section((byte) level);
            sections[level] = section;
        }
        setDataByte(section.skyLight, x, y, z, skyLightLevel);
    }

    @Override
    public int getBlockLightLevel(int x, int y, int z) {
        int level = y >> 4;
        if (sections[level] == null) {
            return 0;
        } else {
            return getDataByte(sections[level].blockLight, x, y, z);
        }
    }

    @Override
    public void setBlockLightLevel(int x, int y, int z, int blockLightLevel) {
        if (readOnly) {
            return;
        }
        int level = y >> 4;
        Section section = sections[level];
        if (section == null) {
            if (blockLightLevel == 0) {
                return;
            }
            section = new Section((byte) level);
            sections[level] = section;
        }
        setDataByte(section.blockLight, x, y, z, blockLightLevel);
    }

    @Override
    public int getHeight(int x, int z) {
        return heightMap[x + z * 16];
    }

    @Override
    public void setHeight(int x, int z, int height) {
        if (readOnly) {
            return;
        }
        heightMap[x + z * 16] = height;
    }
    
    @Override
    public boolean isBiomesAvailable() {
        return biomes != null;
    }
    
    @Override
    public int getBiome(int x, int z) {
        return biomes[x + z * 16] & 0xFF;
    }
    
    @Override
    public void setBiome(int x, int z, int biome) {
        if (readOnly) {
            return;
        }
        if (biomes == null) {
            biomes = new byte[256];
        }
        biomes[x + z * 16] = (byte) biome;
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
        Section section = sections[y >> 4];
        if (section == null) {
            return Material.AIR;
        } else {
        	Material mat = section.blocks[blockOffset(x, y, z)];
        	return (mat != null) ? mat : Material.AIR;
        }
    }

    @Override
    public void setMaterial(int x, int y, int z, Material material) {
        if (readOnly) {
            return;
        }
        int level = y >> 4;
        Section section = sections[level];
        if (section == null) {
            if (material == Material.AIR) {
                return;
            }
            section = new Section((byte) level);
            sections[level] = section;
        }
        section.blocks[blockOffset(x, y, z)] = material;
    }

    @Override
    public boolean isReadOnly() {
        return readOnly;
    }

    @Override
    public boolean isLightPopulated() {
        return lightPopulated;
    }

    @Override
    public void setLightPopulated(boolean lightPopulated) {
        this.lightPopulated = lightPopulated;
    }

    @Override
    public long getInhabitedTime() {
        return inhabitedTime;
    }

    @Override
    public void setInhabitedTime(long inhabitedTime) {
        this.inhabitedTime = inhabitedTime;
    }

    @Override
    public int getHighestNonAirBlock(int x, int z) {
        for (int yy = sections.length - 1; yy >= 0; yy--) {
            if (sections[yy] != null) {
                final Material[] blocks = sections[yy].blocks;
                final int base = blockOffset(x, 0, z);
                for (int i = blockOffset(x, 15, z); i >= base; i -= 256) {
                    if ((blocks[i] != null) && (blocks[i] != Material.AIR)) {
                        return (yy << 4) | ((i - base) >> 8);
                    }
                }
            }
        }
        return -1;
    }

    @Override
    public int getHighestNonAirBlock() {
        for (int yy = sections.length - 1; yy >= 0; yy--) {
            if (sections[yy] != null) {
                final Material[] blocks = sections[yy].blocks;
                for (int i = blocks.length - 1; i >= 0; i--) {
                    if ((blocks[i] != null) && (blocks[i] != Material.AIR)) {
                        return (yy << 4) | (i >> 8);
                    }
                }
            }
        }
        return -1;
    }

    // MinecraftWorld

    @Override
    public int getBlockTypeAt(int x, int y, int height) {
        return getBlockType(x, height, y);
    }

    @Override
    public int getDataAt(int x, int y, int height) {
        return getDataValue(x, height, y);
    }

    @Override
    public Material getMaterialAt(int x, int y, int height) {
        return getMaterial(x, height, y);
    }

    @Override
    public void setBlockTypeAt(int x, int y, int height, int blockType) {
        setBlockType(x, height, y, blockType);
    }

    @Override
    public void setDataAt(int x, int y, int height, int data) {
        setDataValue(x, height, y, data);
    }

    @Override
    public void setMaterialAt(int x, int y, int height, Material material) {
        setMaterial(x, height, y, material);
    }

    @Override
    public boolean isChunkPresent(int x, int y) {
        return ((x == xPos) && (y == zPos));
    }

    @Override
    public void addChunk(Chunk chunk) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public void addEntity(int x, int y, int height, Entity entity) {
        entity = (Entity) entity.clone();
        entity.setPos(new double[] {x, height, y});
        getEntities().add(entity);
    }

    @Override
    public void addEntity(double x, double y, double height, Entity entity) {
        entity = (Entity) entity.clone();
        entity.setPos(new double[] {x, height, y});
        getEntities().add(entity);
    }

    @Override
    public void addTileEntity(int x, int y, int height, TileEntity tileEntity) {
        tileEntity = (TileEntity) tileEntity.clone();
        tileEntity.setX(x);
        tileEntity.setZ(y);
        tileEntity.setY(height);
        getTileEntities().add(tileEntity);
    }

    // ChunkProvider

    @Override
    public Chunk getChunk(int x, int z) {
        if ((x == xPos) && (z == zPos)) {
            return this;
        } else {
            return null;
        }
    }

    @Override
    public Chunk getChunkForEditing(int x, int z) {
        return getChunk(x, z);
    }

    @Override
    public void close() {
        // Do nothing
    }

    // Object

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MemoryChunk other = (MemoryChunk) obj;
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
        int hash = 3;
        hash = 37 * hash + this.xPos;
        hash = 37 * hash + this.zPos;
        return hash;
    }
    
    /**
     * @throws UnsupportedOperationException
     */
    @Override
    public MCRegionChunk clone() {
        throw new UnsupportedOperationException("ChunkImlp2.clone() not supported");
    }
    
    private int getDataByte(byte[] array, int x, int y, int z) {
        int blockOffset = blockOffset(x, y, z);
        byte dataByte = array[blockOffset / 2];
        if (blockOffset % 2 == 0) {
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
        return x | ((z | ((y & 0xF) << 4)) << 4);
    }

    public final boolean readOnly;

    final Section[] sections;
    final int[] heightMap;
    final int xPos, zPos;
    byte[] biomes;
    boolean terrainPopulated, lightPopulated;
    final List<Entity> entities;
    final List<TileEntity> tileEntities;
    final int maxHeight;
    long inhabitedTime;

    private static final long serialVersionUID = 1L;

    public static class Section implements Serializable {
        Section(byte level) {
            this.level = level;
            blocks = new Material[256 * 16];
            skyLight = new byte[128 * 16];
            Arrays.fill(skyLight, (byte) 0xff);
            blockLight = new byte[128 * 16];
        }

        /**
         * Indicates whether the section is empty, meaning all block ID's, data
         * values and block light values are 0, and all sky light values are 15.
         * 
         * @return <code>true</code> if the section is empty
         */
        boolean isEmpty() {
            for (Material b: blocks) {
                if ((b != null) && (b != Material.AIR)) {
                    return false;
                }
            }
            for (byte b: skyLight) {
                if (b != (byte) -1) {
                    return false;
                }
            }
            for (byte b: blockLight) {
                if (b != (byte) 0) {
                    return false;
                }
            }
            return true;
        }
        
        final byte level;
        final Material[] blocks;
        final byte[] skyLight;
        final byte[] blockLight;
        
        private static final long serialVersionUID = 1L;
    }    
}