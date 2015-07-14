/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.jnbt.CompoundTag;
import org.jnbt.Tag;
import static org.pepsoft.minecraft.Constants.*;

/**
 * An "Anvil" chunk.
 * 
 * @author pepijn
 */
public final class ChunkImpl2 extends AbstractNBTItem implements Chunk {
    public ChunkImpl2(int xPos, int zPos, int maxHeight) {
        super(new CompoundTag(TAG_LEVEL, new HashMap<>()));
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

    public ChunkImpl2(CompoundTag tag, int maxHeight) {
        this(tag, maxHeight, false);
    }

    public ChunkImpl2(CompoundTag tag, int maxHeight, boolean readOnly) {
        super((CompoundTag) tag.getTag(TAG_LEVEL));
        this.maxHeight = maxHeight;
        this.readOnly = readOnly;
        
        sections = new Section[maxHeight >> 4];
        List<CompoundTag> sectionTags = getList(TAG_SECTIONS);
        for (CompoundTag sectionTag: sectionTags) {
            Section section = new Section(sectionTag);
            sections[section.level] = section;
        }
//        for (Section section: sections) {
//            if (section != null) {
//                for (byte skyLightLevelByte: section.skyLight) {
//                    int skyLightLevel = skyLightLevelByte & 0x0F;
//                    if ((skyLightLevel < 0) || (skyLightLevel > 15)) {
//                        throw new IllegalStateException("skyLightLevel " + skyLightLevel + " in section " + section.level);
//                    }
//                    skyLightLevel = (skyLightLevelByte >> 4) & 0x0F;
//                    if ((skyLightLevel < 0) || (skyLightLevel > 15)) {
//                        throw new IllegalStateException("skyLightLevel " + skyLightLevel + " in section " + section.level);
//                    }
//                }
//            }
//        }
        biomes = getByteArray(TAG_BIOMES);
        heightMap = getIntArray(TAG_HEIGHT_MAP);
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
        lightPopulated = getBoolean(TAG_LIGHT_POPULATED);
        inhabitedTime = getLong(TAG_INHABITED_TIME);
    }

    public boolean isSectionPresent(int y) {
        return sections[y] != null;
    }

    public Section[] getSections() {
        return sections;
    }

    @Override
    public Tag toNBT() {
        List<Tag> sectionTags = new ArrayList<>(maxHeight >> 4);
        for (Section section: sections) {
//            if (section != null) {
//                for (byte skyLightLevelByte: section.skyLight) {
//                    int skyLightLevel = skyLightLevelByte & 0x0F;
//                    if ((skyLightLevel < 0) || (skyLightLevel > 15)) {
//                        throw new IllegalStateException("skyLightLevel " + skyLightLevel + " in section " + section.level);
//                    }
//                    skyLightLevel = (skyLightLevelByte >> 4) & 0x0F;
//                    if ((skyLightLevel < 0) || (skyLightLevel > 15)) {
//                        throw new IllegalStateException("skyLightLevel " + skyLightLevel + " in section " + section.level);
//                    }
//                }
//            }
            if ((section != null) && (! section.isEmpty())) {
                sectionTags.add(section.toNBT());
            }
        }
        setList(TAG_SECTIONS, CompoundTag.class, sectionTags);
        if (biomes != null) {
            setByteArray(TAG_BIOMES, biomes);
        }
        setIntArray(TAG_HEIGHT_MAP, heightMap);
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
        setBoolean(TAG_LIGHT_POPULATED, lightPopulated);
        if (inhabitedTime != 0) {
            setLong(TAG_INHABITED_TIME, inhabitedTime);
        }

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
        Section section = sections[y >> 4];
        if (section == null) {
            return 0;
        } else {
            if (section.add != null) {
                return (section.blocks[blockOffset(x, y, z)] & 0xFF) | (getDataByte(section.add, x, y, z) << 8);
            } else {
                return section.blocks[blockOffset(x, y, z)] & 0xFF;
            }
        }
    }

    @Override
    public void setBlockType(int x, int y, int z, int blockType) {
        if (readOnly) {
            return;
        }
        int level = y >> 4;
        Section section = sections[level];
        if (section == null) {
            if (blockType == BLK_AIR) {
                return;
            }
            section = new Section((byte) level);
            sections[level] = section;
        }
        section.blocks[blockOffset(x, y, z)] = (byte) blockType;
        if (blockType > 255) {
            if (section.add == null) {
                section.add = new byte[128 * 16];
            }
            setDataByte(section.add, x, y, z, blockType >> 8);
        } else if (section.add != null) {
            // An extended block might have been set earlier, so zero out the
            // high portion
            setDataByte(section.add, x, y, z, 0);
        }
    }

    @Override
    public int getDataValue(int x, int y, int z) {
        int level = y >> 4;
        if (sections[level] == null) {
            return 0;
        } else {
            return getDataByte(sections[level].data, x, y, z);
        }
    }

    @Override
    public void setDataValue(int x, int y, int z, int dataValue) {
//        if ((dataValue < 0) || (dataValue > 15)) {
//            throw new IllegalArgumentException("dataValue " + dataValue);
//        }
        if (readOnly) {
            return;
        }
        int level = y >> 4;
        Section section = sections[level];
        if (section == null) {
            if (dataValue == 0) {
                return;
            }
            section = new Section((byte) level);
            sections[level] = section;
        }
        setDataByte(section.data, x, y, z, dataValue);
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
//        if ((skyLightLevel < 0) || (skyLightLevel > 15)) {
//            throw new IllegalArgumentException("skyLightLevel " + skyLightLevel);
//        }
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
//        if ((blockLightLevel < 0) || (blockLightLevel > 15)) {
//            throw new IllegalArgumentException("blockLightLevel " + blockLightLevel);
//        }
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
            if (section.add != null) {
                return Material.get((section.blocks[blockOffset(x, y, z)] & 0xFF) | (getDataByte(section.add, x, y, z) << 8), getDataByte(section.data, x, y, z));
            } else {
                return Material.get(section.blocks[blockOffset(x, y, z)] & 0xFF, getDataByte(section.data, x, y, z));
            }
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
        int blockType = material.blockType;
        section.blocks[blockOffset(x, y, z)] = (byte) blockType;
        if (blockType > 255) {
            if (section.add == null) {
                section.add = new byte[128 * 16];
            }
            setDataByte(section.add, x, y, z, blockType >> 8);
        } else if (section.add != null) {
            // An extended block might have been set earlier, so zero out the
            // high portion
            setDataByte(section.add, x, y, z, 0);
        }
        setDataByte(section.data, x, y, z, material.data);
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
                final byte[] blocks = sections[yy].blocks;
                final int base = blockOffset(x, 0, z);
                for (int i = blockOffset(x, 15, z); i >= base; i -= 256) {
                    if (blocks[i] != 0) {
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
                final byte[] blocks = sections[yy].blocks;
                for (int i = blocks.length - 1; i >= 0; i--) {
                    if (blocks[i] != 0) {
                        return (yy << 4) | (i >> 8);
                    }
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
        final ChunkImpl2 other = (ChunkImpl2) obj;
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
    public ChunkImpl clone() {
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
    
    public static class Section extends AbstractNBTItem {
        Section(CompoundTag tag) {
            super(tag);
            level = getByte(TAG_Y2);
            blocks = getByteArray(TAG_BLOCKS);
            if (containsTag(TAG_ADD)) {
                add = getByteArray(TAG_ADD);
            }
            data = getByteArray(TAG_DATA);
            skyLight = getByteArray(TAG_SKY_LIGHT);
            blockLight = getByteArray(TAG_BLOCK_LIGHT);
        }

        Section(byte level) {
            super(new CompoundTag("", new HashMap<>()));
            this.level = level;
            blocks = new byte[256 * 16];
            data = new byte[128 * 16];
            skyLight = new byte[128 * 16];
            Arrays.fill(skyLight, (byte) 0xff);
            blockLight = new byte[128 * 16];
        }

        @Override
        public Tag toNBT() {
            setByte(TAG_Y2, level);
            setByteArray(TAG_BLOCKS, blocks);
            if (add != null) {
                for (byte b: add) {
                    if (b != 0) {
                        setByteArray(TAG_ADD, add);
                        break;
                    }
                }
            }
            setByteArray(TAG_DATA, data);
            setByteArray(TAG_SKY_LIGHT, skyLight);
            setByteArray(TAG_BLOCK_LIGHT, blockLight);
            return super.toNBT();
        }
        
        /**
         * Indicates whether the section is empty, meaning all block ID's, data
         * values and block light values are 0, and all sky light values are 15.
         * 
         * @return <code>true</code> if the section is empty
         */
        boolean isEmpty() {
            for (byte b: blocks) {
                if (b != (byte) 0) {
                    return false;
                }
            }
            if (add != null) {
                for (byte b: add) {
                    if (b != (byte) 0) {
                        return false;
                    }
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
            for (byte b: data) {
                if (b != (byte) 0) {
                    return false;
                }
            }
            return true;
        }
        
        final byte level;
        final byte[] blocks;
        final byte[] data;
        final byte[] skyLight;
        final byte[] blockLight;
        byte[] add;
        
        private static final long serialVersionUID = 1L;
    }
}