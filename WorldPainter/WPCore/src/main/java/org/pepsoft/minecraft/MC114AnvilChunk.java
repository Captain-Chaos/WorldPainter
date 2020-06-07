/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.jnbt.*;
import org.pepsoft.minecraft.MC114AnvilChunk.Section.IncompleteSectionException;
import org.pepsoft.worldpainter.exception.WPRuntimeException;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.*;

import static java.util.stream.Collectors.toList;
import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.minecraft.Material.AIR;

/**
 * An "Anvil" chunk for Minecraft 1.14 and higher.
 * 
 * @author pepijn
 */
public final class MC114AnvilChunk extends NBTChunk implements MinecraftWorld {
    public MC114AnvilChunk(int xPos, int zPos, int maxHeight) {
        super(new CompoundTag(TAG_LEVEL, new HashMap<>()));
        this.xPos = xPos;
        this.zPos = zPos;
        this.maxHeight = maxHeight;

        sections = new Section[maxHeight >> 4];
        heightMaps = new HashMap<>();
        entities = new ArrayList<>();
        tileEntities = new ArrayList<>();
        readOnly = false;
        lightPopulated = true;
        liquidTicks = new ArrayList<>();

        setTerrainPopulated(true);

        debugChunk = (xPos == (debugWorldX >> 4)) && (zPos == (debugWorldZ >> 4));
    }

    public MC114AnvilChunk(CompoundTag tag, int maxHeight) {
        this(tag, maxHeight, false);
    }

    public MC114AnvilChunk(CompoundTag tag, int maxHeight, boolean readOnly) {
        super((CompoundTag) tag.getTag(TAG_LEVEL));
        try {
            this.maxHeight = maxHeight;
            this.readOnly = readOnly;

            sections = new Section[maxHeight >> 4];
            List<CompoundTag> sectionTags = getList(TAG_SECTIONS);
            // MC 1.14 has chunks without any sections; we're not sure yet if
            // this is a bug
            if (sectionTags != null) {
                for (CompoundTag sectionTag: sectionTags) {
                    try {
                        Section section = new Section(sectionTag);
                        if ((section.level >= 0) && (section.level < sections.length)) {
                            sections[section.level] = section;
                        } else if (! section.isEmpty()) {
                            logger.warn("Ignoring non-empty out of bounds chunk section @ " + getxPos() + "," + section.level + "," + getzPos());
                        }
                    } catch (IncompleteSectionException e) {
                        // Ignore sections that don't have blocks
                        if (logger.isDebugEnabled()) {
                            logger.debug("Block states and/or palette missing from section @ y=" + ((ByteTag) sectionTag.getTag(TAG_Y)).getValue());
                        }
                    }
                }
            }
            Tag biomesTag = getTag(TAG_BIOMES);
            if (biomesTag instanceof IntArrayTag) {
                biomes = getIntArray(TAG_BIOMES);
            } else if (biomesTag instanceof ByteArrayTag) {
                byte[] biomesArray = ((ByteArrayTag) biomesTag).getValue();
                biomes = new int[biomesArray.length];
                for (int i = 0; i < biomesArray.length; i++) {
                    biomes[i] = biomesArray[i] & 0xff;
                }
            }
            heightMaps = new HashMap<>();
            Map<String, Tag> heightMapTags = getMap(TAG_HEIGHT_MAPS);
            if (heightMapTags != null) {
                for (Map.Entry<String, Tag> entry: heightMapTags.entrySet()) {
                    heightMaps.put(entry.getKey().intern(), ((LongArrayTag) entry.getValue()).getValue());
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
            xPos = getInt(TAG_X_POS_);
            zPos = getInt(TAG_Z_POS_);
            status = getString(TAG_STATUS).intern();
            lightPopulated = getBoolean(TAG_LIGHT_POPULATED);
            inhabitedTime = getLong(TAG_INHABITED_TIME);
            liquidTicks = getList(TAG_LIQUID_TICKS);

            debugChunk = (xPos == (debugWorldX >> 4)) && (zPos == (debugWorldZ >> 4));
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

    public void setStatus(String status) {
        this.status = status.intern();
    }

    public String getStatus() {
        return status;
    }

    public Map<String, long[]> getHeightMaps() {
        return heightMaps;
    }

    public void addLiquidTick(int x, int y, int z) {
        // Liquid ticks are in world coordinates for some reason
        x = (xPos << 4) | x;
        z = (zPos << 4) | z;
        for (CompoundTag liquidTick: liquidTicks) {
            if ((x == ((IntTag) liquidTick.getTag(TAG_X_)).getValue())
                    && (y == ((IntTag) liquidTick.getTag(TAG_Y_)).getValue())
                    && (z == ((IntTag) liquidTick.getTag(TAG_Z_)).getValue())) {
                // There is already a liquid tick scheduled for this block
                return;
            }
        }
        liquidTicks.add(new CompoundTag("", ImmutableMap.<String, Tag>builder()
                .put(TAG_X_, new IntTag(TAG_X_, x))
                .put(TAG_Y_, new IntTag(TAG_Y_, y))
                .put(TAG_Z_, new IntTag(TAG_Z_, z))
                .put(TAG_I_, new StringTag(TAG_I_, getMaterial(x & 0xf, y, z & 0xf).name))
                .put(TAG_P_, new IntTag(TAG_P_, 0))
                .put(TAG_T_, new IntTag(TAG_T_, RANDOM.nextInt(30) + 1)).build()));
    }

    public static void setDebugColumn(int worldX, int worldZ) {
        debugWorldX = worldX;
        debugXInChunk = debugWorldX & 0xf;
        debugWorldZ = worldZ;
        debugZInChunk = debugWorldZ & 0xf;
    }

    // Chunk

    @Override
    public CompoundTag toNBT() {
        if (sections != null) {
            List<CompoundTag> sectionTags = new ArrayList<>(maxHeight >> 4);
            for (Section section: sections) {
                if ((section != null) && (!section.isEmpty())) {
                    sectionTags.add(section.toNBT());
                }
            }
            setList(TAG_SECTIONS, CompoundTag.class, sectionTags);
        }
        if (biomes != null) {
            setIntArray(TAG_BIOMES, biomes);
        }
        // TODO heightMaps
        List<CompoundTag> entityTags = new ArrayList<>(entities.size());
        entities.stream().map(Entity::toNBT).forEach(entityTags::add);
        setList(TAG_ENTITIES, CompoundTag.class, entityTags);
        List<CompoundTag> tileEntityTags = new ArrayList<>(entities.size());
        tileEntities.stream().map(TileEntity::toNBT).forEach(tileEntityTags::add);
        setList(TAG_TILE_ENTITIES, CompoundTag.class, tileEntityTags);
        setLong(TAG_LAST_UPDATE, System.currentTimeMillis()); // TODO: is this correct?
        setInt(TAG_X_POS_, xPos);
        setInt(TAG_Z_POS_, zPos);
        setString(TAG_STATUS, status);
        setBoolean(TAG_LIGHT_POPULATED, lightPopulated);
        setLong(TAG_INHABITED_TIME, inhabitedTime);
        setList(TAG_LIQUID_TICKS, CompoundTag.class, liquidTicks);

        CompoundTag tag = new CompoundTag("", Collections.emptyMap());
        tag.setTag(TAG_LEVEL, super.toNBT());
        tag.setTag(TAG_DATA_VERSION, new IntTag(TAG_DATA_VERSION, DATA_VERSION_MC_1_14_4));
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
    @Deprecated
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
    @Deprecated
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
        return (biomes != null) && (biomes.length > 0);
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
        return true;
    }

    @Override
    public void setTerrainPopulated(boolean terrainPopulated) {
        if (readOnly) {
            return;
        }
        String[] statuses = {"empty",   // -> biomes reset; bottom part of chunks completely regenerated; spawn buried; no proper generation
                "structure_starts",     // -> biomes reset; bottom part of chunks completely regenerated; spawn buried; no proper generation
                "structure_references", // -> biomes reset; bottom part of chunks completely regenerated; spawn buried; no proper generation
                "biomes",               // -> bottom part of chunks completely regenerated; no proper generation
                "noise",                // -> no proper generation
                "surface",              // -> no proper generation
                "carvers",              // -> no proper generation
                "liquid_carvers",       // -> no proper generation
                "features",             // -> no generation
                "light",                // -> no generation
                "spawn",                // -> no generation
                "heightmaps",           // -> no generation
                "full"};                // -> no generation
        // TODO: this is a guess, is this useful?
        if (terrainPopulated) {
            status = STATUS_FULL;
        } else {
            throw new UnsupportedOperationException("Terrain population not support for Minecraft 1.14");
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
        if (debugChunk && logger.isDebugEnabled() && (x == debugXInChunk) && (z == debugZInChunk)) {
            logger.debug("Setting material @ {},{},{} to {}", x, y, z, material, new Throwable("Stacktrace"));
        }
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
    @Deprecated
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
    @Deprecated
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
        final MC114AnvilChunk other = (MC114AnvilChunk) obj;
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
    public MC114AnvilChunk clone() {
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
    boolean lightPopulated; // TODO: is this still used by MC 1.14?
    final List<Entity> entities;
    final List<TileEntity> tileEntities;
    final int maxHeight;
    long inhabitedTime;
    String status;
    final Map<String, long[]> heightMaps;
    final List<CompoundTag> liquidTicks;
    final boolean debugChunk;

    private static long debugWorldX, debugWorldZ, debugXInChunk, debugZInChunk;

    private static final Random RANDOM = new Random();
    private static final Logger logger = LoggerFactory.getLogger(MC114AnvilChunk.class);

    public static class Section extends AbstractNBTItem {
        Section(CompoundTag tag) {
            super(tag);
            level = getByte(TAG_Y);
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
                throw new IncompleteSectionException();
            }
            byte[] skyLightBytes = getByteArray(TAG_SKY_LIGHT);
            if (skyLightBytes == null) {
                skyLightBytes = new byte[128 * 16];
                Arrays.fill(skyLightBytes, (byte) 0xff);
            }
            skyLight = skyLightBytes;
            byte[] blockLightBytes = getByteArray(TAG_BLOCK_LIGHT);
            if (blockLightBytes == null) {
                blockLightBytes = new byte[128 * 16];
            }
            blockLight = blockLightBytes;
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
        public CompoundTag toNBT() {
            setByte(TAG_Y, level);

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
            List<CompoundTag> paletteList = new ArrayList<>(palette.size());
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
         * @return {@code true} if the section is empty
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

        public final byte level;
        final byte[] skyLight;
        final byte[] blockLight;
        final Material[] materials;

        static class IncompleteSectionException extends WPRuntimeException {
            // Empty
        }
    }

    private static final Set<String> populatedStatuses = ImmutableSet.of(
            // 1.14:
            "features", "light", "full",

            // 1.13:
            "decorated", "fullchunk", "postprocessed");
}