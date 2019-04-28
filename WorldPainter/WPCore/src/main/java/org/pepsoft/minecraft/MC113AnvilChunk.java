/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft;

import org.jnbt.*;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.*;

import static java.util.stream.Collectors.toList;
import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.minecraft.MC113AnvilChunk.Status.LIQUID_CARVED;
import static org.pepsoft.minecraft.MC113AnvilChunk.Status.POSTPROCESSED;
import static org.pepsoft.minecraft.Material.AIR;

/**
 * An "Anvil" chunk for Minecraft 1.13 and higher.
 * 
 * @author pepijn
 */
public final class MC113AnvilChunk extends NBTChunk implements MinecraftWorld {
    public MC113AnvilChunk(int xPos, int zPos, int maxHeight) {
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

        setTerrainPopulated(false);
    }

    public MC113AnvilChunk(CompoundTag tag, int maxHeight) {
        this(tag, maxHeight, false);
    }

    public MC113AnvilChunk(CompoundTag tag, int maxHeight, boolean readOnly) {
        super((CompoundTag) tag.getTag(TAG_LEVEL));
        try {
            this.maxHeight = maxHeight;
            this.readOnly = readOnly;

            sections = new Section[maxHeight >> 4];
            List<CompoundTag> sectionTags = getList(TAG_SECTIONS);
            for (CompoundTag sectionTag: sectionTags) {
                Section section = new Section(sectionTag);
                if (section.level >= 0) {
                    // MC 1.14 has sections with y == -1; we're not sure yet if
                    // this is a bug
                    sections[section.level] = section;
                }
            }
            biomes = getIntArray(TAG_BIOMES);
            heightMaps = new EnumMap<>(HeightmapType.class);
            Map<String, Tag> heightMapTags = getMap(TAG_HEIGHT_MAPS);
            if (heightMapTags != null) {
                for (Map.Entry<String, Tag> entry: heightMapTags.entrySet()) {
                    heightMaps.put(HeightmapType.valueOf(entry.getKey()), ((LongArrayTag) entry.getValue()).getValue());
                }
            }
            List<CompoundTag> entityTags = getList(TAG_ENTITIES);
            if (entityTags != null) {
                entities = new ArrayList<>(entityTags.size());
                entities.addAll(entityTags.stream().map(Entity::fromNBT).collect(toList()));
            } else {
                entities = new ArrayList<>();
            }
            List<CompoundTag> tileEntityTags = getList(TAG_TILE_ENTITIES);
            if (tileEntityTags != null) {
                tileEntities = new ArrayList<>(tileEntityTags.size());
                tileEntities.addAll(tileEntityTags.stream().map(TileEntity::fromNBT).collect(toList()));
            } else {
                tileEntities = new ArrayList<>();
            }
            // TODO: last update is ignored, is that correct?
            xPos = getInt(TAG_X_POS);
            zPos = getInt(TAG_Z_POS);
            status = Status.valueOf(getString(TAG_STATUS).toUpperCase());
            lightPopulated = getBoolean(TAG_LIGHT_POPULATED);
            inhabitedTime = getLong(TAG_INHABITED_TIME);
        } catch (RuntimeException e) {
            logger.error("{} while creating chunk from NBT: {}", e.getClass().getSimpleName(), tag);
            throw e;
        }
    }

    public boolean isSectionPresent(int y) {
        return sections[y] != null;
    }

    public Section[] getSections() {
        return sections;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }

    public Map<HeightmapType, long[]> getHeightMaps() {
        return heightMaps;
    }

    // Chunk

    @Override
    public Tag toNBT() {
        List<Tag> sectionTags = new ArrayList<>(maxHeight >> 4);
        for (Section section: sections) {
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
        setLong(TAG_INHABITED_TIME, inhabitedTime);

        CompoundTag tag = new CompoundTag("", Collections.emptyMap());
        tag.setTag(TAG_LEVEL, super.toNBT());
        tag.setTag(TAG_DATA_VERSION, new IntTag(TAG_DATA_VERSION, DATA_VERSION_MC_1_13_2));
        return tag;
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
        return getMaterial(x, y, z).blockType;
    }

    /**
     * Not supported. Always throws {@link UnsupportedOperationException}.
     *
     * @deprecated Use {@link #setMaterial(int, int, int, Material)}
     * @throws UnsupportedOperationException Always
     */
    @Override
    public void setBlockType(int x, int y, int z, int blockType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getDataValue(int x, int y, int z) {
        return getMaterial(x, y, z).data;
    }

    /**
     * Not supported. Always throws {@link UnsupportedOperationException}.
     *
     * @deprecated Use {@link #setMaterial(int, int, int, Material)}
     * @throws UnsupportedOperationException Always
     */
    @Override
    public void setDataValue(int x, int y, int z, int dataValue) {
        throw new UnsupportedOperationException();
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
        // TODO: how necessary is this? Will Minecraft create these if they're missing?
        return 62;
    }

    @Override
    public void setHeight(int x, int z, int height) {
        if (readOnly) {
            return;
        }
        // TODO: how necessary is this? Will Minecraft create these if they're missing?
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
        return status.ordinal() > LIQUID_CARVED.ordinal();
    }

    @Override
    public void setTerrainPopulated(boolean terrainPopulated) {
        if (readOnly) {
            return;
        }
        // TODO: this is a guess, is this useful?
        if (terrainPopulated) {
            status = POSTPROCESSED;
        } else {
            status = LIQUID_CARVED;
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
            return AIR;
        } else {
            Material material = section.materials[blockOffset(x, y, z)];
            return (material != null) ? material : AIR;
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
            if (material == AIR) {
                return;
            }
            section = new Section((byte) level);
            sections[level] = section;
        }
        section.materials[blockOffset(x, y, z)] = (material == AIR) ? null : material;
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
                final Material[] materials = sections[yy].materials;
                final int base = blockOffset(x, 0, z);
                for (int i = blockOffset(x, 15, z); i >= base; i -= 256) {
                    if ((materials[i] != null) && (materials[i] != AIR)) {
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
                final Material[] materials = sections[yy].materials;
                for (int i = materials.length - 1; i >= 0; i--) {
                    if ((materials[i] != null) && (materials[i] != AIR)) {
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

    /**
     * Not supported. Always throws {@link UnsupportedOperationException}.
     *
     * @deprecated Use {@link #setMaterial(int, int, int, Material)}
     * @throws UnsupportedOperationException Always
     */
    @Override
    public void setBlockTypeAt(int x, int y, int height, int blockType) {
        throw new UnsupportedOperationException();
    }

    /**
     * Not supported. Always throws {@link UnsupportedOperationException}.
     *
     * @deprecated Use {@link #setMaterial(int, int, int, Material)}
     * @throws UnsupportedOperationException Always
     */
    @Override
    public void setDataAt(int x, int y, int height, int data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setMaterialAt(int x, int y, int height, Material material) {
        setMaterial(x, height, y, material);
    }

    @Override
    public boolean isChunkPresent(int x, int y) {
        return ((x == xPos) && (y == zPos));
    }

    /**
     * Not supported. Always throws {@link UnsupportedOperationException}.
     *
     * @throws UnsupportedOperationException Always
     */
    @Override
    public void addChunk(Chunk chunk) {
        throw new UnsupportedOperationException();
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
        final MC113AnvilChunk other = (MC113AnvilChunk) obj;
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
     * @throws UnsupportedOperationException Always
     */
    @Override
    public MC113AnvilChunk clone() {
        throw new UnsupportedOperationException("MC113AnvilChunk.clone() not supported");
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

    private void writeObject(ObjectOutputStream out) throws IOException {
        throw new IOException("MC113AnvilChunk is not serializable");
    }

    static int blockOffset(int x, int y, int z) {
        return x | ((z | ((y & 0xF) << 4)) << 4);
    }

    public final boolean readOnly;

    final Section[] sections;
    final int xPos, zPos;
    int[] biomes;
    boolean lightPopulated; // TODO: is this still used by MC 1.13?
    final List<Entity> entities;
    final List<TileEntity> tileEntities;
    final int maxHeight;
    long inhabitedTime;
    Status status;
    final Map<HeightmapType, long[]> heightMaps;

    private static final Logger logger = LoggerFactory.getLogger(MC113AnvilChunk.class);

    public static class Section extends AbstractNBTItem {
        Section(CompoundTag tag) {
            super(tag);
            level = getByte(TAG_Y2);
            materials = new Material[4096];
            long[] blockStates = getLongArray(TAG_BLOCK_STATES);
            List<CompoundTag> palette = getList(TAG_PALETTE);
            if ((blockStates != null) && (palette != null)) {
                int wordSize = blockStates.length * 64 / 4096;
                if (wordSize == 4) {
                    // Optimised special case
                    for (int w = 0; w < 4096; w += 16) {
                        long data = blockStates[w >> 4];
                        materials[w] = getMaterial(palette, (int) (data & 0xf));
                        materials[w + 1] = getMaterial(palette, (int) ((data & 0xf0) >> 4));
                        materials[w + 2] = getMaterial(palette, (int) ((data & 0xf00) >> 8));
                        materials[w + 3] = getMaterial(palette, (int) ((data & 0xf000) >> 12));
                        materials[w + 4] = getMaterial(palette, (int) ((data & 0xf0000) >> 16));
                        materials[w + 5] = getMaterial(palette, (int) ((data & 0xf00000) >> 20));
                        materials[w + 6] = getMaterial(palette, (int) ((data & 0xf000000) >> 24));
                        materials[w + 7] = getMaterial(palette, (int) ((data & 0xf0000000L) >> 28));
                        materials[w + 8] = getMaterial(palette, (int) ((data & 0xf00000000L) >> 32));
                        materials[w + 9] = getMaterial(palette, (int) ((data & 0xf000000000L) >> 36));
                        materials[w + 10] = getMaterial(palette, (int) ((data & 0xf0000000000L) >> 40));
                        materials[w + 11] = getMaterial(palette, (int) ((data & 0xf00000000000L) >> 44));
                        materials[w + 12] = getMaterial(palette, (int) ((data & 0xf000000000000L) >> 48));
                        materials[w + 13] = getMaterial(palette, (int) ((data & 0xf0000000000000L) >> 52));
                        materials[w + 14] = getMaterial(palette, (int) ((data & 0xf00000000000000L) >> 56));
                        materials[w + 15] = getMaterial(palette, (int) ((data & 0xf000000000000000L) >>> 60));
                    }
                } else {
                    BitSet bitSet = BitSet.valueOf(blockStates);
                    for (int w = 0; w < 4096; w++) {
                        int wordOffset = w * wordSize;
                        int index = 0;
                        for (int b = 0; b < wordSize; b++) {
                            index |= bitSet.get(wordOffset + b) ? 1 << b : 0;
                        }
                        materials[w] = getMaterial(palette, index);
                    }
                }
            } else {
                logger.warn("Block states and/or palette missing from section @ y=" + level);
            }
            skyLight = getByteArray(TAG_SKY_LIGHT);
            blockLight = getByteArray(TAG_BLOCK_LIGHT);
        }

        Section(byte level) {
            super(new CompoundTag("", new HashMap<>()));
            this.level = level;
            materials = new Material[4096];
            skyLight = new byte[128 * 16];
            Arrays.fill(skyLight, (byte) 0xff);
            blockLight = new byte[128 * 16];
        }

        @Override
        public Tag toNBT() {
            setByte(TAG_Y2, level);

            // Create the palette. We have to do this first, because otherwise
            // we don't know how many bits the indices will be and therefore how
            // big to make the blockStates array
            Map<Material, Integer> reversePalette = new HashMap<>();
            List<Material> palette = new LinkedList<>();
            for (Material material: materials) {
                if (material == null) {
                    material = AIR;
                }
                if (! reversePalette.containsKey(material)) {
                    reversePalette.put(material, palette.size());
                    palette.add(material);
                }
            }
            List<Tag> paletteList = new ArrayList<>(palette.size());
            for (Material material: palette) {
                CompoundTag paletteEntry = new CompoundTag("", Collections.emptyMap());
                paletteEntry.setTag(TAG_NAME, new StringTag(TAG_NAME, material.name));
                if (material.getProperties() != null) {
                    CompoundTag propertiesTag = new CompoundTag(TAG_PROPERTIES, Collections.emptyMap());
                    for (Map.Entry<String, String> property: material.getProperties().entrySet()) {
                        propertiesTag.setTag(property.getKey(), new StringTag(property.getKey(), property.getValue()));
                    }
                    paletteEntry.setTag(TAG_PROPERTIES, propertiesTag);
                }
                paletteList.add(paletteEntry);
            }
            setList(TAG_PALETTE, CompoundTag.class, paletteList);

            // Create the blockStates array and fill it, using the appropriate
            // length palette indices so that it just fits
            int paletteIndexSize = Math.max((int) Math.ceil(Math.log(palette.size()) / Math.log(2)), 4);
            if (paletteIndexSize == 4) {
                // Optimised special case
                long[] blockStates = new long[256];
                for (int i = 0; i < 4096; i += 16) {
                    blockStates[i >> 4] =
                           reversePalette.get(materials[i]      != null ? materials[i]      : AIR)
                        | (reversePalette.get(materials[i +  1] != null ? materials[i +  1] : AIR) << 4)
                        | (reversePalette.get(materials[i +  2] != null ? materials[i +  2] : AIR) << 8)
                        | (reversePalette.get(materials[i +  3] != null ? materials[i +  3] : AIR) << 12)
                        | (reversePalette.get(materials[i +  4] != null ? materials[i +  4] : AIR) << 16)
                        | (reversePalette.get(materials[i +  5] != null ? materials[i +  5] : AIR) << 20)
                        | (reversePalette.get(materials[i +  6] != null ? materials[i +  6] : AIR) << 24)
                        | ((long) (reversePalette.get(materials[i +  7] != null ? materials[i +  7] : AIR)) << 28)
                        | ((long) (reversePalette.get(materials[i +  8] != null ? materials[i +  8] : AIR)) << 32)
                        | ((long) (reversePalette.get(materials[i +  9] != null ? materials[i +  9] : AIR)) << 36)
                        | ((long) (reversePalette.get(materials[i + 10] != null ? materials[i + 10] : AIR)) << 40)
                        | ((long) (reversePalette.get(materials[i + 11] != null ? materials[i + 11] : AIR)) << 44)
                        | ((long) (reversePalette.get(materials[i + 12] != null ? materials[i + 12] : AIR)) << 48)
                        | ((long) (reversePalette.get(materials[i + 13] != null ? materials[i + 13] : AIR)) << 52)
                        | ((long) (reversePalette.get(materials[i + 14] != null ? materials[i + 14] : AIR)) << 56)
                        | ((long) (reversePalette.get(materials[i + 15] != null ? materials[i + 15] : AIR)) << 60);
                }
                setLongArray(TAG_BLOCK_STATES, blockStates);
            } else {
                BitSet blockStates = new BitSet(4096 * paletteIndexSize);
                for (int i = 0; i < 4096; i++) {
                    int offset = i * paletteIndexSize;
                    int index = reversePalette.get(materials[i] != null ? materials[i] : AIR);
                    for (int j = 0; j < paletteIndexSize; j++) {
                        if ((index & (1 << j)) != 0) {
                            blockStates.set(offset + j);
                        }
                    }
                }
                long[] blockStatesArray = blockStates.toLongArray();
                // Pad with zeros if necessary
                int requiredLength = 64 * paletteIndexSize;
                if (blockStatesArray.length != requiredLength) {
                    long[] expandedArray = new long[requiredLength];
                    System.arraycopy(blockStatesArray, 0, expandedArray, 0, blockStatesArray.length);
                    setLongArray(TAG_BLOCK_STATES, expandedArray);
                } else {
                    setLongArray(TAG_BLOCK_STATES, blockStatesArray);
                }
            }

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
            for (Material material: materials) {
                if (material != null) {
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

        private void writeObject(ObjectOutputStream out) throws IOException {
            throw new IOException("MC113AnvilChunk.Section is not serializable");
        }

        private Material getMaterial(List<CompoundTag> palette, int index) {
            CompoundTag blockSpecTag = palette.get(index);
            String name = ((StringTag) blockSpecTag.getTag(TAG_NAME)).getValue();
            CompoundTag propertiesTag = (CompoundTag) blockSpecTag.getTag(TAG_PROPERTIES);
            if (name.equals(MC_AIR) && propertiesTag == null) {
                return null;
            }
            Map<String, String> properties;
            if (propertiesTag != null) {
                properties = new HashMap<>();
                for (Map.Entry<String, Tag> entry : propertiesTag.getValue().entrySet()) {
                    properties.put(entry.getKey(), ((StringTag) entry.getValue()).getValue());
                }
            } else {
                properties = null;
            }
            return Material.get(name, properties);
        }

        final byte level;
        final byte[] skyLight;
        final byte[] blockLight;
        final Material[] materials;
    }

    public enum HeightmapType {
        // Observed in generated Minecraft maps. Uses unknown:
        LIGHT, LIQUID, RAIN, SOLID, OCEAN_FLOOR, MOTION_BLOCKING_NO_LEAVES, LIGHT_BLOCKING, MOTION_BLOCKING,
        OCEAN_FLOOR_WG, WORLD_SURFACE_WG, WORLD_SURFACE
    }

    /**
     * The chunk generation status.
     */
    public enum Status {
        // These have lately been observed to occur in this order of generation:
        EMPTY, CARVED, LIQUID_CARVED, DECORATED, FULLCHUNK, POSTPROCESSED,

        // These have not lately been observed and may not (longer) be in use by Minecraft:
        LIGHTED, FINALIZED, MOBS_SPAWNED,

        // New for 1.14 (do they replace the ones from 1.13?)
        FULL, STRUCTURE_STARTS, LIQUID_CARVERS, FEATURES, HEIGHTMAPS
    }
}