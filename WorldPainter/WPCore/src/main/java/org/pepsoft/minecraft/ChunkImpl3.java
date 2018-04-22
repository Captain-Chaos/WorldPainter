/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft;

import org.jnbt.CompoundTag;
import org.jnbt.LongArrayTag;
import org.jnbt.Tag;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;

import java.awt.*;
import java.util.*;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.pepsoft.minecraft.Constants.*;

/**
 * An "Anvil" chunk.
 * 
 * @author pepijn
 */
public final class ChunkImpl3 extends AbstractNBTItem implements Chunk, MinecraftWorld {
    public ChunkImpl3(int xPos, int zPos, int maxHeight) {
        super(new CompoundTag(TAG_LEVEL, new HashMap<>()));
        this.xPos = xPos;
        this.zPos = zPos;
        this.maxHeight = maxHeight;

        sections = new Section[maxHeight >> 4];
        heightMaps = new EnumMap<>(HeightmapType.class);
        entities = new ArrayList<>();
        tileEntities = new ArrayList<>();
        readOnly = false;
        lightPopulated = true;
    }

    public ChunkImpl3(CompoundTag tag, int maxHeight) {
        this(tag, maxHeight, false);
    }

    public ChunkImpl3(CompoundTag tag, int maxHeight, boolean readOnly) {
        super((CompoundTag) tag.getTag(TAG_LEVEL));
        this.maxHeight = maxHeight;
        this.readOnly = readOnly;
        
        sections = new Section[maxHeight >> 4];
        List<CompoundTag> sectionTags = getList(TAG_SECTIONS);
        for (CompoundTag sectionTag: sectionTags) {
            Section section = new Section(sectionTag);
            sections[section.level] = section;
        }
        biomes = getIntArray(TAG_BIOMES);
        heightMaps = new EnumMap<>(HeightmapType.class);
        Map<String, Tag> heightMapTags = getMap(TAG_HEIGHT_MAPS);
        for (Map.Entry<String, Tag> entry : heightMapTags.entrySet()) {
            heightMaps.put(HeightmapType.valueOf(entry.getKey()), ((LongArrayTag) entry.getValue()).getValue());
        }
        List<CompoundTag> entityTags = getList(TAG_ENTITIES);
        entities = new ArrayList<>(entityTags.size());
        entities.addAll(entityTags.stream().map(Entity::fromNBT).collect(toList()));
        List<CompoundTag> tileEntityTags = getList(TAG_TILE_ENTITIES);
        tileEntities = new ArrayList<>(tileEntityTags.size());
        tileEntities.addAll(tileEntityTags.stream().map(TileEntity::fromNBT).collect(toList()));
        // TODO: last update is ignored, is that correct?
        xPos = getInt(TAG_X_POS);
        zPos = getInt(TAG_Z_POS);
        status = Status.valueOf(getString(TAG_STATUS).toUpperCase());
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
            setIntArray(TAG_BIOMES, biomes);
        }
        // TODO heightMaps
        List<Tag> entityTags = new ArrayList<>(entities.size());
        entities.stream().map(Entity::toNBT).forEach(entityTags::add);
        setList(TAG_ENTITIES, CompoundTag.class, entityTags);
        List<Tag> tileEntityTags = new ArrayList<>(entities.size());
        tileEntities.stream().map(TileEntity::toNBT).forEach(tileEntityTags::add);
        setList(TAG_TILE_ENTITIES, CompoundTag.class, tileEntityTags);
        setLong(TAG_LAST_UPDATE, System.currentTimeMillis()); // TODO: is this correct?
        setInt(TAG_X_POS, xPos);
        setInt(TAG_Z_POS, zPos);
        setString(TAG_STATUS, status.name().toLowerCase());
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
            throw new UnsupportedOperationException(); // TODO
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
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public int getDataValue(int x, int y, int z) {
        int level = y >> 4;
        if (sections[level] == null) {
            return 0;
        } else {
            throw new UnsupportedOperationException(); // TODO
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
        throw new UnsupportedOperationException(); // TODO
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
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public void setHeight(int x, int z, int height) {
        if (readOnly) {
            return;
        }
        throw new UnsupportedOperationException(); // TODO
    }
    
    @Override
    public boolean isBiomesAvailable() {
        return biomes != null;
    }
    
    @Override
    public int getBiome(int x, int z) {
        return biomes[x + z * 16];
    }
    
    @Override
    public void setBiome(int x, int z, int biome) {
        if (readOnly) {
            return;
        }
        if (biomes == null) {
            biomes = new int[256];
        }
        biomes[x + z * 16] = biome;
    }

    @Override
    public boolean isTerrainPopulated() {
        return status == Status.POSTPROCESSED;
    }

    @Override
    public void setTerrainPopulated(boolean terrainPopulated) {
        if (readOnly) {
            return;
        }
        // TODO: this is a guess, is this useful?
        if (terrainPopulated) {
            status = Status.POSTPROCESSED;
        } else {
            status = Status.CARVED;
        }
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
            throw new UnsupportedOperationException(); // TODO
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
        throw new UnsupportedOperationException(); // TODO
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
                throw new UnsupportedOperationException(); // TODO
            }
        }
        return -1;
    }

    @Override
    public int getHighestNonAirBlock() {
        for (int yy = sections.length - 1; yy >= 0; yy--) {
            if (sections[yy] != null) {
                throw new UnsupportedOperationException(); // TODO
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
        final ChunkImpl3 other = (ChunkImpl3) obj;
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
        throw new UnsupportedOperationException("ChunkImlp3.clone() not supported");
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
    final int xPos, zPos;
    int[] biomes;
    boolean lightPopulated;
    final List<Entity> entities;
    final List<TileEntity> tileEntities;
    final int maxHeight;
    long inhabitedTime;
    Status status;
    final Map<HeightmapType, long[]> heightMaps;

    private static final long serialVersionUID = 1L;

    public static class Section extends AbstractNBTItem {
        Section(CompoundTag tag) {
            super(tag);
            level = getByte(TAG_Y2);
            blockStates = getLongArray(TAG_BLOCK_STATES);
            palette = new Material[0]; // TODO
            skyLight = getByteArray(TAG_SKY_LIGHT);
            blockLight = getByteArray(TAG_BLOCK_LIGHT);
        }

        Section(byte level) {
            super(new CompoundTag("", new HashMap<>()));
            this.level = level;
            blockStates = new long[256];
            palette = new Material[0]; // TODO
            skyLight = new byte[128 * 16];
            Arrays.fill(skyLight, (byte) 0xff);
            blockLight = new byte[128 * 16];
        }

        @Override
        public Tag toNBT() {
            setByte(TAG_Y2, level);
            // TODO: store blockStates and palette
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
            // TODO: check blockStates
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
        final byte[] skyLight;
        final byte[] blockLight;
        final long[] blockStates;
        final Material[] palette;

        private static final long serialVersionUID = 1L;
    }

    public enum HeightmapType {LIGHT, LIQUID, RAIN, SOLID}
    public enum Status {CARVED, DECORATED, EMPTY, FULLCHUNK, LIGHTED, LIQUID_CARVED, POSTPROCESSED, FINALIZED}
}