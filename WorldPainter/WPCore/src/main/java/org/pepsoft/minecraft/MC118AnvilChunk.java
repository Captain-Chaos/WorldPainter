/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft;

import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.NotNull;
import org.jnbt.*;
import org.pepsoft.minecraft.MC115AnvilChunk.Section.IncompleteSectionException;
import org.pepsoft.util.PackedArrayCube;
import org.pepsoft.worldpainter.exception.WPRuntimeException;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.*;

import static java.lang.Integer.MIN_VALUE;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.minecraft.Material.AIR;
import static org.pepsoft.minecraft.Material.WATERLOGGED;

/**
 * An "Anvil" chunk for Minecraft 1.15 and higher.
 * 
 * @author pepijn
 */
public final class MC118AnvilChunk extends NBTChunk implements SectionedChunk, MinecraftWorld {
    public MC118AnvilChunk(int xPos, int zPos, int maxHeight) {
        super(new CompoundTag("", new HashMap<>()));
        this.xPos = xPos;
        this.zPos = zPos;
        this.maxHeight = maxHeight;

        sections = new Section[(maxHeight >> 4) + UNDERGROUND_SECTIONS];
        heightMaps = new HashMap<>();
        entities = new ArrayList<>();
        tileEntities = new ArrayList<>();
        readOnly = false;
        lightPopulated = true;

        setTerrainPopulated(true);

//        debugChunk = (xPos == (debugWorldX >> 4)) && (zPos == (debugWorldZ >> 4));
    }

    public MC118AnvilChunk(CompoundTag tag, int maxHeight) {
        this(tag, maxHeight, false);
    }

    @SuppressWarnings("ConstantConditions") // Guaranteed by containsTag()
    public MC118AnvilChunk(CompoundTag tag, int maxHeight, boolean readOnly) {
        super(tag);
        try {
            this.maxHeight = maxHeight;
            this.readOnly = readOnly;

            sections = new Section[(maxHeight >> 4) + UNDERGROUND_SECTIONS];
            List<CompoundTag> sectionTags = getList(TAG_SECTIONS_);
            // MC 1.18 has chunks without any sections; we're not sure yet if
            // this is a bug
            if (sectionTags != null) {
                for (CompoundTag sectionTag: sectionTags) {
                    try {
                        Section section = new Section(sectionTag);
                        if ((section.level >= -UNDERGROUND_SECTIONS) && (section.level < (sections.length - UNDERGROUND_SECTIONS))) {
                            sections[section.level + UNDERGROUND_SECTIONS] = section;
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
            // TODOMC118: last update is ignored, is that correct?
            xPos = getInt(TAG_X_POS_);
            zPos = getInt(TAG_Z_POS_);
            status = getString(TAG_STATUS).intern();
            lightPopulated = getBoolean(TAG_LIGHT_POPULATED);
            inhabitedTime = getLong(TAG_INHABITED_TIME);
            if (containsTag(TAG_LIQUID_TICKS)) {
                liquidTicks.addAll(getList(TAG_LIQUID_TICKS));
            }

//            debugChunk = (xPos == (debugWorldX >> 4)) && (zPos == (debugWorldZ >> 4));
        } catch (Section.ExceptionParsingSectionException e) {
            // Already reported; just rethrow
            throw e;
        } catch (RuntimeException e) {
            logger.error("{} while creating chunk from NBT", e.getClass().getSimpleName());
//            logger.error("{} while creating chunk from NBT: {}", e.getClass().getSimpleName(), tag);
            throw e;
        }
    }

    public boolean isSectionPresent(int y) {
        return sections[y + UNDERGROUND_SECTIONS] != null;
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

    private void addLiquidTick(int x, int y, int z) {
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
        String id;
        Material material = getMaterial(x & 0xf, y, z & 0xf);
        if (material.isNamed(MC_WATER) || material.is(WATERLOGGED)) {
            id = MC_WATER;
        } else {
            id = material.name;
        }
        liquidTicks.add(new CompoundTag("", ImmutableMap.<String, Tag>builder()
                .put(TAG_X_, new IntTag(TAG_X_, x))
                .put(TAG_Y_, new IntTag(TAG_Y_, y))
                .put(TAG_Z_, new IntTag(TAG_Z_, z))
                .put(TAG_I_, new StringTag(TAG_I_, id))
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
        // TODOMC118

        if (sections != null) {
            List<CompoundTag> sectionTags = new ArrayList<>(maxHeight >> 4);
            for (Section section: sections) {
                if ((section != null) && (! section.isEmpty())) {
                    sectionTags.add(section.toNBT());
                }
            }
            setList(TAG_SECTIONS_, CompoundTag.class, sectionTags);
        }
        // TODOMC118 heightMaps
        List<CompoundTag> entityTags = new ArrayList<>(entities.size());
        entities.stream().map(Entity::toNBT).forEach(entityTags::add);
        setList(TAG_ENTITIES, CompoundTag.class, entityTags);
        List<CompoundTag> tileEntityTags = new ArrayList<>(entities.size());
        tileEntities.stream().map(TileEntity::toNBT).forEach(tileEntityTags::add);
        setList(TAG_TILE_ENTITIES, CompoundTag.class, tileEntityTags);
        setLong(TAG_LAST_UPDATE, System.currentTimeMillis()); // TODOMC118: is this correct?
        setInt(TAG_X_POS_, xPos);
        setInt(TAG_Y_POS_, -UNDERGROUND_SECTIONS);
        setInt(TAG_Z_POS_, zPos);
        setString(TAG_STATUS, status);
        setBoolean(TAG_LIGHT_POPULATED, lightPopulated);
        setLong(TAG_INHABITED_TIME, inhabitedTime);
        setList(TAG_LIQUID_TICKS, CompoundTag.class, liquidTicks);

        setTag(TAG_DATA_VERSION, new IntTag(TAG_DATA_VERSION, DATA_VERSION_MC_1_18_0));
        return super.toNBT();
    }

    @Override
    public int getMinHeight() {
        return -(UNDERGROUND_SECTIONS << 4);
    }

    @Override
    public int getMaxHeight() {
        return maxHeight;
    }

    @Override
    public boolean isNamedBiomesAvailable() {
        return true;
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
        if (sections[level + UNDERGROUND_SECTIONS] == null) {
            return 15;
        } else {
            return getDataByte(sections[level + UNDERGROUND_SECTIONS].skyLight, x, y, z);
        }
    }

    @Override
    public void setSkyLightLevel(int x, int y, int z, int skyLightLevel) {
        if (readOnly) {
            return;
        }
        int level = y >> 4;
        Section section = sections[level + UNDERGROUND_SECTIONS];
        if (section == null) {
            if (skyLightLevel == 15) {
                return;
            }
            section = new Section((byte) level);
            sections[level + UNDERGROUND_SECTIONS] = section;
        }
        setDataByte(section.skyLight, x, y, z, skyLightLevel);
    }

    @Override
    public int getBlockLightLevel(int x, int y, int z) {
        int level = y >> 4;
        if (sections[level + UNDERGROUND_SECTIONS] == null) {
            return 0;
        } else {
            return getDataByte(sections[level + UNDERGROUND_SECTIONS].blockLight, x, y, z);
        }
    }

    @Override
    public void setBlockLightLevel(int x, int y, int z, int blockLightLevel) {
        if (readOnly) {
            return;
        }
        int level = y >> 4;
        Section section = sections[level + UNDERGROUND_SECTIONS];
        if (section == null) {
            if (blockLightLevel == 0) {
                return;
            }
            section = new Section((byte) level);
            sections[level + UNDERGROUND_SECTIONS] = section;
        }
        setDataByte(section.blockLight, x, y, z, blockLightLevel);
    }

    @Override
    public int getHeight(int x, int z) {
        // TODOMC118: how necessary is this? Will Minecraft create these if they're missing?
        return 62;
    }

    @Override
    public void setHeight(int x, int z, int height) {
        if (readOnly) {
            return;
        }
        // TODOMC118: how necessary is this? Will Minecraft create these if they're missing?
    }

    @Override
    public String getNamedBiome(int x, int y, int z) {
        final Section section = sections[(y >> 2) + UNDERGROUND_SECTIONS];
        return (section != null)
                ? ((section.singleBiome != null) ? section.singleBiome : section.biomes.getValue(x, z, y & 0x3))
                : null;
    }

    @SuppressWarnings("StringEquality") // Interned string
    @Override
    public void setNamedBiome(int x, int y, int z, String biome) {
        if (readOnly) {
            return;
        }
        biome = biome.intern();
        final int level = y >> 2;
        Section section = sections[level + UNDERGROUND_SECTIONS];
        if (section == null) {
            section = new Section((byte) level);
            section.singleBiome = biome;
            sections[level + UNDERGROUND_SECTIONS] = section;
        }
        if (section.singleBiome != null) {
            if (biome != section.singleBiome) {
                section.biomes = new PackedArrayCube<>(4, 1, String.class);
                section.biomes.fill(section.singleBiome);
                section.biomes.setValue(x, z, y & 0x3, biome);
                section.singleBiome = null;
            } else {
                return;
            }
        } else if (section.biomes == null) {
            section.biomes = new PackedArrayCube<>(4, 1, String.class);
        }
        section.biomes.setValue(x, z, y & 0x3, biome);
    }

    @Override
    public void markForUpdateChunk(int x, int y, int z) {
        Material material = getMaterial(x, y, z);
        if (material.isNamedOneOf(MC_WATER, MC_LAVA) || material.containsWater()) {
            addLiquidTick(x, y, z);
        } else {
            throw new UnsupportedOperationException("Don't know how to mark " + material + " for update");
        }
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
        if (terrainPopulated) {
            status = STATUS_FULL;
        } else {
            status = STATUS_LIQUID_CARVERS;
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
        Section section = sections[(y >> 4) + UNDERGROUND_SECTIONS];
        if (section == null) {
            return AIR;
        } else {
            if (section.singleMaterial != null) {
                return section.singleMaterial;
            } else {
                Material material = section.materials.getValue(x, z, y & 0xf);
                return (material != null) ? material : AIR;
            }
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
        Section section = sections[level + UNDERGROUND_SECTIONS];
        if (section == null) {
            if (material == AIR) {
                return;
            }
            section = new Section((byte) level);
            sections[level + UNDERGROUND_SECTIONS] = section;
        }
        if (section.singleMaterial != null) {
            if (material != section.singleMaterial) {
                section.materials = new PackedArrayCube<>(16, 4, Material.class);
                if (section.singleMaterial != AIR) {
                    section.materials.fill(section.singleMaterial);
                }
                section.singleMaterial = null;
            } else {
                return;
            }
        }
        section.materials.setValue(x, z, y & 0xf, (material == AIR) ? null : material);
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
                if (sections[yy].singleMaterial == AIR) {
                    continue;
                } else if (sections[yy].singleMaterial != null) {
                    return ((yy - UNDERGROUND_SECTIONS) << 4) | (blockOffset(x, 15, z) >> 8);
                }
                for (int y = 15; y >= 0; y--) {
                    final Material material = sections[yy].materials.getValue(x, z, y);
                    if ((material != null) && (material != AIR)) {
                        return ((yy - UNDERGROUND_SECTIONS) << 4) | y;
                    }
                }
            }
        }
        return MIN_VALUE;
    }

    @Override
    public int getHighestNonAirBlock() {
        for (int yy = sections.length - 1; yy >= 0; yy--) {
            if (sections[yy] != null) {
                if (sections[yy].singleMaterial == AIR) {
                    continue;
                } else if (sections[yy].singleMaterial != null) {
                    return ((yy - UNDERGROUND_SECTIONS) << 4) | (4095 >> 8);
                }
                for (int y = 15; y >= 0; y--) {
                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                            final Material material = sections[yy].materials.getValue(x, z, y);
                            if ((material != null) && (material != AIR)) {
                                return ((yy - UNDERGROUND_SECTIONS) << 4) | y;
                            }
                        }
                    }
                }
            }
        }
        return MIN_VALUE;
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
        final MC118AnvilChunk other = (MC118AnvilChunk) obj;
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
    public MC118AnvilChunk clone() {
        throw new UnsupportedOperationException("MC113AnvilChunk.clone() not supported");
    }

    /**
     * Fix negative values caused by an earlier bug where biomes ids were cast to a byte.
     */
    private void fixNegativeValues(int[] biomes) {
        for (int i = 0; i < biomes.length; i++) {
            if (biomes[i] < 0) {
                biomes[i] = biomes[i] & 0xff;
            }
        }
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
        throw new IOException("MC114AnvilChunk is not serializable");
    }

    static int blockOffset(int x, int y, int z) {
        return x | ((z | ((y & 0xF) << 4)) << 4);
    }

    public final boolean readOnly;

    final Section[] sections;
    final int xPos, zPos;
    boolean lightPopulated; // TODOMC118: is this still used by MC 1.15?
    final List<Entity> entities;
    final List<TileEntity> tileEntities;
    final int maxHeight;
    long inhabitedTime;
    String status;
    final Map<String, long[]> heightMaps;
    final List<CompoundTag> liquidTicks = new ArrayList<>();
    final boolean debugChunk = false;

    private static long debugWorldX, debugWorldZ, debugXInChunk, debugZInChunk;

    public static final int UNDERGROUND_SECTIONS = 4, LIGHT_ARRAY_SIZE = 2048;
    private static final Random RANDOM = new Random();
    private static final Logger logger = LoggerFactory.getLogger(MC118AnvilChunk.class);

    public static class Section extends AbstractNBTItem implements SectionedChunk.Section {
        @SuppressWarnings("unchecked") // Guaranteed by Minecraft
        Section(CompoundTag tag) {
            super(tag);
            try {
                Tag levelTag = getTag(TAG_Y);
                level = (levelTag instanceof ByteTag) ? ((ByteTag) levelTag).intValue() : ((IntTag) levelTag).intValue();
                final CompoundTag blockStatesTag = (CompoundTag) getTag(TAG_BLOCK_STATES_);
                List<CompoundTag> paletteList = ((ListTag<CompoundTag>) blockStatesTag.getTag(TAG_PALETTE_)).getValue();
                if (paletteList != null) {
                    final Material[] palette = new Material[paletteList.size()];
                    for (int i = 0; i < palette.length; i++) {
                        palette[i] = getMaterial(paletteList, i);
                    }
                    final LongArrayTag blockStatesDataTag = (LongArrayTag) blockStatesTag.getTag(TAG_DATA_);
                    if (blockStatesDataTag != null) {
                        final long[] blockStates = blockStatesDataTag.getValue();
                        materials = new PackedArrayCube<>(16, blockStates, palette, 4, Material.class);
                    } else if (palette.length == 1) {
                        // Entire section filled with one material
                        singleMaterial = (palette[0] == null) ? AIR : palette[0];
                    } else {
                        throw new IncompleteSectionException("block_states.data tag missing");
                    }
                } else {
                    throw new IncompleteSectionException("block_states.palette tag missing");
                }

                final CompoundTag biomesTag = (CompoundTag) getTag(TAG_BIOMES_);
                List<StringTag> biomesPaletteList = ((ListTag<StringTag>) biomesTag.getTag(TAG_PALETTE_)).getValue();
                if (biomesPaletteList != null) {
                    final String[] palette = new String[biomesPaletteList.size()];
                    for (int i = 0; i < palette.length; i++) {
                        palette[i] = biomesPaletteList.get(i).getValue().intern();
                    }
                    final LongArrayTag biomeDataTag = (LongArrayTag) biomesTag.getTag(TAG_DATA_);
                    if (biomeDataTag != null) {
                        final long[] biomeData = biomeDataTag.getValue();
                        biomes = new PackedArrayCube<>(4, biomeData, palette, 1, String.class);
                    } else if (palette.length == 1) {
                        // Entire section filled with one biome
                        singleBiome = palette[0];
                    } else {
                        throw new IncompleteSectionException("biomes.data tag missing");
                    }
                } else {
                    throw new IncompleteSectionException("biomes.palette tag missing");
                }

                // TODOMC118 optimise when data array not present
                byte[] skyLightBytes = getByteArray(TAG_SKY_LIGHT);
                if (skyLightBytes == null) {
                    skyLightBytes = new byte[128 * 16];
                    Arrays.fill(skyLightBytes, (byte) 0xff);
                }
                skyLight = skyLightBytes;

                // TODOMC118 optimise when data array not present
                byte[] blockLightBytes = getByteArray(TAG_BLOCK_LIGHT);
                if (blockLightBytes == null) {
                    blockLightBytes = new byte[128 * 16];
                }
                blockLight = blockLightBytes;
            } catch (IncompleteSectionException e) {
                // Just propagate it
                throw e;
            } catch (RuntimeException e) {
                logger.error("{} while creating chunk from NBT", e.getClass().getSimpleName());
//                logger.error("{} while creating chunk from NBT: {}", e.getClass().getSimpleName(), tag);
                throw new ExceptionParsingSectionException(e);
            }
        }

        Section(byte level) {
            super(new CompoundTag("", new HashMap<>()));
            this.level = level;
            singleMaterial = AIR;
            // TODOMC118 same treatment for sky and block light
            skyLight = new byte[128 * 16];
            Arrays.fill(skyLight, (byte) 0xff);
            blockLight = new byte[128 * 16];
        }

        @Override
        public CompoundTag toNBT() {
            setByte(TAG_Y, (byte) level); // TODOMC118 this is sometimes a byte and sometimes an int; how to determine which it should be?

            if (singleMaterial != null) {
                setMap(TAG_BLOCK_STATES_, ImmutableMap.of(TAG_PALETTE_, new ListTag<>(TAG_PALETTE_, CompoundTag.class, singletonList(createPaletteEntry(singleMaterial)))));
            } else {
                PackedArrayCube<Material>.PackedData packedMaterials = materials.pack();
                List<CompoundTag> palette = new ArrayList<>(packedMaterials.palette.length);
                for (Material material: packedMaterials.palette) {
                    palette.add(createPaletteEntry(material));
                }
                setMap(TAG_BLOCK_STATES_, ImmutableMap.of(TAG_PALETTE_, new ListTag<>(TAG_PALETTE_, CompoundTag.class, palette), TAG_DATA_, new LongArrayTag(TAG_DATA_, packedMaterials.data)));
            }

            if (singleBiome != null) {
                setMap(TAG_BIOMES_, ImmutableMap.of(TAG_PALETTE_, new ListTag<>(TAG_PALETTE_, StringTag.class, singletonList(new StringTag("", singleBiome)))));
            } else if (biomes != null) {
                PackedArrayCube<String>.PackedData packedBiomes = biomes.pack();
                List<StringTag> palette = new ArrayList<>(packedBiomes.palette.length);
                for (String biome: packedBiomes.palette) {
                    palette.add(new StringTag("", biome));
                }
                setMap(TAG_BIOMES_, ImmutableMap.of(TAG_PALETTE_, new ListTag<>(TAG_PALETTE_, StringTag.class, palette), TAG_DATA_, new LongArrayTag(TAG_DATA_, packedBiomes.data)));
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
        @SuppressWarnings("StringEquality") // Interned strings
        @Override
        public boolean isEmpty() {
            if ((singleMaterial != null) && (singleMaterial != AIR)) {
                return false;
            } else if ((materials != null) && (! materials.isEmpty())) {
                return false;
            }
            if ((singleBiome != null) && (singleBiome != MC_PLAINS)) {
                return false;
            } else if ((biomes != null) && (! biomes.isEmpty())) {
                return false;
            }
            // TODOMC118 what to do when singleBiome is set and the section is otherwise empty?
            for (int i = 0; i < LIGHT_ARRAY_SIZE; i++) {
                if (skyLight[i] != (byte) -1) {
                    return false;
                }
            }
            for (int i = 0; i < LIGHT_ARRAY_SIZE; i++) {
                if (blockLight[i] != (byte) 0) {
                    return false;
                }
            }
            return true;
        }

        private void writeObject(ObjectOutputStream out) throws IOException {
            throw new IOException("MC113AnvilChunk.Section is not serializable");
        }

        private Material getMaterial(List<CompoundTag> palette, int index) {
            final CompoundTag blockSpecTag = palette.get(index);
            final String name = ((StringTag) blockSpecTag.getTag(TAG_NAME)).getValue();
            final CompoundTag propertiesTag = (CompoundTag) blockSpecTag.getTag(TAG_PROPERTIES);
            if (name.equals(MC_AIR) && propertiesTag == null) {
                return null;
            }
            final Map<String, String> properties;
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

        @NotNull
        private CompoundTag createPaletteEntry(Material material) {
            CompoundTag paletteEntry = new CompoundTag("", Collections.emptyMap());
            if (material != null) {
                paletteEntry.setTag(TAG_NAME, new StringTag(TAG_NAME, material.name));
                if (material.getProperties() != null) {
                    CompoundTag propertiesTag = new CompoundTag(TAG_PROPERTIES, Collections.emptyMap());
                    for (Map.Entry<String, String> property: material.getProperties().entrySet()) {
                        propertiesTag.setTag(property.getKey(), new StringTag(property.getKey(), property.getValue()));
                    }
                    paletteEntry.setTag(TAG_PROPERTIES, propertiesTag);
                }
            } else {
                paletteEntry.setTag(TAG_NAME, new StringTag(TAG_NAME, MC_AIR));
            }
            return paletteEntry;
        }

        public final int level;
        final byte[] skyLight;
        final byte[] blockLight;
        // Exactly one of these should be set:
        PackedArrayCube<Material> materials;
        Material singleMaterial;
        // At most one of these should be set:
        PackedArrayCube<String> biomes;
        String singleBiome;

        static class IncompleteSectionException extends WPRuntimeException {
            IncompleteSectionException(String message) {
                super(message);
            }
        }

        static class ExceptionParsingSectionException extends WPRuntimeException {
            ExceptionParsingSectionException(Throwable cause) {
                super(cause);
            }
        }
    }
}