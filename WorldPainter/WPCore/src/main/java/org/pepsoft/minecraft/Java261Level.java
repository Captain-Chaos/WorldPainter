package org.pepsoft.minecraft;

import org.jnbt.*;
import org.pepsoft.worldpainter.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.minecraft.SuperflatPreset.defaultPreset;
import static org.pepsoft.worldpainter.Constants.*;
import static org.pepsoft.worldpainter.DefaultPlugin.ATTRIBUTE_EXPORT_DATA_VERSION;
import static org.pepsoft.worldpainter.Generator.*;

/**
 * The {@code level.dat} file for a Minecraft 26.1 or later map.
 */
public class Java261Level extends JavaLevel {
    Java261Level(int minHeight, int maxHeight, Platform platform) {
        super(minHeight, maxHeight, platform);
    }

    Java261Level(CompoundTag tag, int minHeight, int maxHeight) {
        super(tag, minHeight, maxHeight);
    }

    @Override
    public void save(File worldDir) throws IOException {
        super.save(worldDir);

        // Write world_gen_settings.dat file
        File minecraftDataDir = new File(worldDir, "data/minecraft");
        if ((! minecraftDataDir.exists()) && (! minecraftDataDir.mkdirs())) {
            throw new IOException("Could not create " + minecraftDataDir);
        }
        File worldGenSettingsFile = new File(minecraftDataDir, "world_gen_settings.dat");
        try (NBTOutputStream out = new NBTOutputStream(new GZIPOutputStream(new FileOutputStream(worldGenSettingsFile)))) {
            out.writeTag(getOrCreateWorldGenSettings());
        }
    }

    @Override
    public long getSeed() {
        return seed;
    }

    @Override
    public void setSeed(long seed) {
        this.seed = seed;
    }

    @Override
    public boolean isMapFeatures() {
        return mapFeatures;
    }

    @Override
    public MapGenerator getGenerator(int dim) {
        return generators.get(dim);
    }

    @Override
    public void setMapFeatures(boolean mapFeatures) {
        this.mapFeatures = mapFeatures;
    }

    @Override
    public void setGenerator(int dim, MapGenerator generator) {
        if (generator == null) {
            throw new NullPointerException("generator");
        }
        this.generators.put(dim, generator);
    }

    @SuppressWarnings("ConstantConditions") // Guaranteed by Minecraft
    void loadWorldGenSettings(File worldDir) throws IOException {
        final CompoundTag compoundTag;
        try (NBTInputStream in = new NBTInputStream(new GZIPInputStream(new FileInputStream(new File(worldDir, "data/minecraft/world_gen_settings.dat"))))) {
            compoundTag = (CompoundTag) in.readTag();
        }
        final Map<String, Tag> worldGenSettings = ((CompoundTag) compoundTag.getTag("data")).getValue();
        mapFeatures = ((ByteTag) worldGenSettings.get(TAG_GENERATE_STRUCTURES_)).getValue() == (byte) 1;
        seed = ((LongTag) worldGenSettings.get(TAG_SEED_)).getValue();
        final CompoundTag dimensionsTag = (CompoundTag) worldGenSettings.get(TAG_DIMENSIONS_);
        if (dimensionsTag == null) {
            return;
        }

        for (Tag tag: dimensionsTag.getValue().values()) {
            final CompoundTag dimensionTag = (CompoundTag) tag;
            final String type = ((StringTag) dimensionTag.getTag(TAG_TYPE_)).getValue();
            final CompoundTag generatorTag = (CompoundTag) dimensionTag.getTag(TAG_GENERATOR_);
            final String generatorType = ((StringTag) generatorTag.getTag(TAG_TYPE_)).getValue();
            final Tag generatorSettings = generatorTag.getTag(TAG_SETTINGS_);
            final long generatorSeed = generatorTag.containsTag(TAG_SEED_) ? ((LongTag) generatorTag.getTag(TAG_SEED_)).getValue() : seed;
            final MapGenerator generator;
            if (generatorType.equals(MC_NOISE) && (generatorSettings instanceof StringTag)) {
                final String noiseSettings = ((StringTag) generatorSettings).getValue();
                if (noiseSettings.equals(MC_OVERWORLD)) {
                    generator = new SeededGenerator(DEFAULT, generatorSeed);
                } else if (noiseSettings.equals(MC_LARGE_BIOMES)) {
                    generator = new SeededGenerator(LARGE_BIOMES, generatorSeed);
                } else if (noiseSettings.equals(MC_NETHER)) {
                    generator = new SeededGenerator(NETHER, generatorSeed);
                } else if (noiseSettings.equals(MC_END)) {
                    generator = new SeededGenerator(END, generatorSeed);
                } else if (noiseSettings.equals(MC_AMPLIFIED)) {
                    generator = new SeededGenerator(AMPLIFIED, generatorSeed);
                } else {
                    generator = new CustomGenerator(generatorType, generatorTag);
                }
            } else if (generatorType.equals(MC_FLAT)) {
                generator = new SuperflatGenerator(SuperflatPreset.fromMinecraft1_18_0((CompoundTag) generatorSettings));
            } else {
                generator = new CustomGenerator(generatorType, generatorTag);
            }
            switch (type) {
                case MC_OVERWORLD:
                    generators.put(DIM_NORMAL, generator);
                    break;
                case MC_THE_NETHER:
                    generators.put(DIM_NETHER, generator);
                    break;
                case MC_THE_END:
                    generators.put(DIM_END, generator);
                    break;
                default:
                    logger.warn("Ignoring dimension of unknown type {}", type);
                    break;
            }
        }
    }

    private CompoundTag getOrCreateWorldGenSettings() {
        Map<String, Tag> worldGenSettings = getMap(TAG_WORLD_GEN_SETTINGS);
        if (worldGenSettings == null) {
            worldGenSettings = new HashMap<>();
            worldGenSettings.put(TAG_GENERATE_STRUCTURES_, new ByteTag(TAG_GENERATE_STRUCTURES_, mapFeatures ? (byte) 1 : (byte) 0));
        }
        worldGenSettings.put(TAG_SEED_, new LongTag(TAG_SEED_, seed));

        CompoundTag dimensionsTag;
        if (worldGenSettings.containsKey(TAG_DIMENSIONS_)) {
            dimensionsTag = (CompoundTag) worldGenSettings.get(TAG_DIMENSIONS_);
        } else {
            dimensionsTag = new CompoundTag(TAG_DIMENSIONS_, new HashMap<>());
            worldGenSettings.put(TAG_DIMENSIONS_, dimensionsTag);
        }

        for (int dim: platform.supportedDimensions) {
            final String dimensionName;
            final MapGenerator generator;
            switch (dim) {
                case DIM_NORMAL:
                    dimensionName = MC_OVERWORLD;
                    generator = generators.getOrDefault(DIM_NORMAL, new SeededGenerator(LARGE_BIOMES, seed));
                    break;
                case DIM_NETHER:
                    dimensionName = MC_THE_NETHER;
                    generator = generators.getOrDefault(DIM_NETHER, new SeededGenerator(NETHER, seed));
                    break;
                case DIM_END:
                    dimensionName = MC_THE_END;
                    generator = generators.getOrDefault(DIM_END, new SeededGenerator(END, seed));
                    break;
                default:
                    throw new IllegalArgumentException("Dimension " + dim + " not supported");
            }

            CompoundTag dimensionTag = (CompoundTag) dimensionsTag.getTag(dimensionName);
            if (dimensionTag == null) {
                dimensionTag = new CompoundTag(dimensionName, new HashMap<>());
                dimensionTag.getValue().put(TAG_TYPE_, new StringTag(TAG_TYPE_, dimensionName));
                dimensionsTag.getValue().put(dimensionName, dimensionTag);
            }

            if ((generator.getType() == CUSTOM) && (((CustomGenerator) generator).getSettings() instanceof CompoundTag)) {
                dimensionTag.getValue().put(TAG_GENERATOR_, ((CustomGenerator) generator).getSettings());
            } else {
                final String generatorType, biomeSourceType, biomeSourcePreset;
                final Tag settingsTag;
                switch (generator.getType()) {
                    case DEFAULT:
                        generatorType = MC_NOISE;
                        settingsTag = new StringTag(TAG_SETTINGS_, MC_OVERWORLD);
                        biomeSourceType = MC_MULTI_NOISE;
                        biomeSourcePreset = MC_OVERWORLD;
                        break;
                    case LARGE_BIOMES:
                        generatorType = MC_NOISE;
                        settingsTag = new StringTag(TAG_SETTINGS_, MC_LARGE_BIOMES);
                        biomeSourceType = MC_MULTI_NOISE;
                        biomeSourcePreset = MC_OVERWORLD;
                        break;
                    case AMPLIFIED:
                        generatorType = MC_NOISE;
                        settingsTag = new StringTag(TAG_SETTINGS_, MC_AMPLIFIED);
                        biomeSourceType = MC_MULTI_NOISE;
                        biomeSourcePreset = MC_OVERWORLD;
                        break;
                    case FLAT:
                        generatorType = MC_FLAT;
                        // TODO make this configurable:
                        final SuperflatPreset settings = ((SuperflatGenerator) generator).getSettings();
                        settingsTag = ((settings != null) ? settings : defaultPreset(platform)).toMinecraft1_18_0();
                        biomeSourceType = biomeSourcePreset = null;
                        break;
                    case NETHER:
                        generatorType = MC_NOISE;
                        settingsTag = new StringTag(TAG_SETTINGS_, MC_NETHER);
                        biomeSourceType = MC_MULTI_NOISE;
                        biomeSourcePreset = MC_NETHER;
                        break;
                    case END:
                        generatorType = MC_NOISE;
                        settingsTag = new StringTag(TAG_SETTINGS_, MC_END);
                        biomeSourceType = MC_THE_END;
                        biomeSourcePreset = null;
                        break;
                    default:
                        throw new IllegalArgumentException("Generator " + generator + " not supported for Minecraft 1.18+");
                }
                final long seed = (generator instanceof SeededGenerator) ? ((SeededGenerator) generator).getSeed() : 0L;
                CompoundTag generatorTag = (CompoundTag) dimensionTag.getTag(TAG_GENERATOR_);
                if (generatorTag == null) {
                    generatorTag = new CompoundTag(TAG_GENERATOR_, new HashMap<>());
                    dimensionTag.getValue().put(TAG_GENERATOR_, generatorTag);
                }
                if (biomeSourceType != null) {
                    generatorTag.setTag(TAG_SEED_, new LongTag(TAG_SEED_, seed));
                }
                generatorTag.setTag(TAG_TYPE_, new StringTag(TAG_TYPE_, generatorType));
                if (settingsTag != null) {
                    generatorTag.setTag(TAG_SETTINGS_, settingsTag);
                }

                if (biomeSourceType != null) {
                    CompoundTag biomeSourceTag = (CompoundTag) generatorTag.getTag(TAG_BIOME_SOURCE_);
                    if (biomeSourceTag == null) {
                        biomeSourceTag = new CompoundTag(TAG_BIOME_SOURCE_, new HashMap<>());
                        generatorTag.getValue().put(TAG_BIOME_SOURCE_, biomeSourceTag);
                    }
                    biomeSourceTag.setTag(TAG_TYPE_, new StringTag(TAG_TYPE_, biomeSourceType));
                    switch (biomeSourceType) {
                        case MC_MULTI_NOISE:
                            biomeSourceTag.setTag(TAG_PRESET_, new StringTag(TAG_PRESET_, biomeSourcePreset));
                            break;
                        case MC_THE_END:
                            biomeSourceTag.setTag(TAG_SEED_, new LongTag(TAG_SEED_, seed));
                            break;
                    }
                }
            }
        }

        return new CompoundTag("", Map.of(
                "data", new CompoundTag("data", worldGenSettings),
                "DataVersion", new IntTag("DataVersion", platform.getAttribute(ATTRIBUTE_EXPORT_DATA_VERSION))
        ));
    }

    private final Map<Integer, MapGenerator> generators = new HashMap<>();
    private long seed;
    private boolean mapFeatures;

    private static final Logger logger = LoggerFactory.getLogger(Java261Level.class);
}