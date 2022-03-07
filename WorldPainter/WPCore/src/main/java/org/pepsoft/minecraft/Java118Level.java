package org.pepsoft.minecraft;

import org.jnbt.*;
import org.pepsoft.worldpainter.Platform;

import java.util.HashMap;
import java.util.Map;

import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.worldpainter.Constants.*;

/**
 * The {@code level.dat} file for a Minecraft 1.18 map.
 */
public class Java118Level extends JavaLevel {
    public Java118Level(int mapHeight, Platform platform) {
        super(mapHeight, platform);
    }

    public Java118Level(CompoundTag tag, int mapHeight) {
        super(tag, mapHeight);
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
        this.generators.put(dim, generator);
    }

    @Override
    public CompoundTag toNBT() {
        setMap(TAG_WORLD_GEN_SETTINGS, getOrCreateWorldGenSettings());
        return super.toNBT();
    }

    private Map<String, Tag> getOrCreateWorldGenSettings() {
        Map<String, Tag> worldGenSettings = getMap(TAG_WORLD_GEN_SETTINGS);
        if (worldGenSettings == null) {
            worldGenSettings = new HashMap<>();
            worldGenSettings.put(TAG_GENERATE_FEATURES_, new ByteTag(TAG_GENERATE_FEATURES_, mapFeatures ? (byte) 1 : (byte) 0));
        }
        worldGenSettings.put(TAG_SEED_, new LongTag(TAG_SEED_, seed));

        CompoundTag dimensionsTag;
        if (worldGenSettings.containsKey(TAG_DIMENSIONS_)) {
            dimensionsTag = (CompoundTag) worldGenSettings.get(TAG_DIMENSIONS_);
        } else {
            dimensionsTag = new CompoundTag(TAG_DIMENSIONS_, new HashMap<>());
            worldGenSettings.put(TAG_DIMENSIONS_, dimensionsTag);
        }

        dimensions.forEach((dim, dimension) -> {
            if (! generators.containsKey(dim)) {
                return;
            }

            String dimensionName;
            switch (dim) {
                case DIM_NORMAL:
                    dimensionName = MC_OVERWORLD;
                    break;
                case DIM_NETHER:
                    dimensionName = MC_THE_NETHER;
                    break;
                case DIM_END:
                    dimensionName = MC_THE_END;
                    break;
                default:
                    throw new IllegalArgumentException("Dimension " + dim + " not supported");
            }

            CompoundTag dimensionTag = (CompoundTag) dimensionsTag.getTag(dimensionName);
            if (dimensionTag == null) {
                dimensionTag = new CompoundTag(dimensionName, new HashMap<>());
                dimensionTag.getValue().put(TAG_TYPE_, new StringTag(TAG_TYPE_, dimensionName)); // TODO check for Nether and End
                dimensionsTag.getValue().put(dimensionName, dimensionTag);
            }

            final String generatorType;
            final Tag settingsTag;
            final boolean includeBiomeSource;
            final MapGenerator generator = generators.get(dim);
            switch (generator.getType()) {
                case DEFAULT:
                    generatorType = MC_NOISE;
                    settingsTag = new StringTag(TAG_SETTINGS_, MC_OVERWORLD);
                    includeBiomeSource = true;
                    break;
                case LARGE_BIOMES:
                    generatorType = MC_NOISE;
                    settingsTag = new StringTag(TAG_SETTINGS_, MC_LARGE_BIOMES);
                    includeBiomeSource = true;
                    // TODO: dimension-specific seed
                    break;
                case FLAT:
                    generatorType = MC_FLAT;
                    // TODO make this configurable:
                    settingsTag = ((SuperflatGenerator) generator).getSettings().toMinecraft1_18_0();
                    includeBiomeSource = false;
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
            if (includeBiomeSource) {
                generatorTag.setTag(TAG_SEED_, new LongTag(TAG_SEED_, seed));
            }
            generatorTag.setTag(TAG_TYPE_, new StringTag(TAG_TYPE_, generatorType));
            generatorTag.setTag(TAG_SETTINGS_, settingsTag);

            if (includeBiomeSource) {
                CompoundTag biomeSourceTag = (CompoundTag) generatorTag.getTag(TAG_BIOME_SOURCE_);
                if (biomeSourceTag == null) {
                    biomeSourceTag = new CompoundTag(TAG_BIOME_SOURCE_, new HashMap<>());
                    biomeSourceTag.setTag(TAG_PRESET_, new StringTag(TAG_PRESET_, dimensionName)); // Check for Nether and End
                    biomeSourceTag.setTag(TAG_TYPE_, new StringTag(TAG_TYPE_, MC_MULTI_NOISE)); // Check for Nether and End
                    generatorTag.getValue().put(TAG_BIOME_SOURCE_, biomeSourceTag);
                }
            }
        });

        return worldGenSettings;
    }

    private final Map<Integer, MapGenerator> generators = new HashMap<>();
    private long seed;
    private boolean mapFeatures;
}