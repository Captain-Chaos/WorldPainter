package org.pepsoft.minecraft;

import org.jnbt.CompoundTag;
import org.jnbt.StringTag;
import org.jnbt.Tag;
import org.pepsoft.worldpainter.Platform;

import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.minecraft.MapGenerator.UNKNOWN;
import static org.pepsoft.worldpainter.Constants.*;
import static org.pepsoft.worldpainter.Generator.*;

/**
 * The {@code level.dat} file for a Minecraft 1.17 or earlier map.
 */
public class Java117Level extends JavaLevel {
    public Java117Level(int mapHeight, Platform platform) {
        super(mapHeight, platform);
    }

    public Java117Level(CompoundTag tag, int mapHeight) {
        super(tag, mapHeight);
    }

    @Override
    public long getSeed() {
        return getLong(TAG_RANDOM_SEED);
    }

    @Override
    public void setSeed(long seed) {
        setLong(TAG_RANDOM_SEED, seed);
    }

    @Override
    public boolean isMapFeatures() {
        return getBoolean(TAG_MAP_FEATURES);
    }

    @Override
    public void setMapFeatures(boolean mapFeatures) {
        setBoolean(TAG_MAP_FEATURES, mapFeatures);
    }

    @Override
    public MapGenerator getGenerator(int dim) {
        switch (dim) {
            case DIM_NORMAL:
                String generatorName = getGeneratorName();
                if (generatorName == null) {
                    return UNKNOWN;
                } else if ("FLAT".equalsIgnoreCase(generatorName)) {
                    final Tag generatorOptions = getGeneratorOptions();
                    final SuperflatPreset preset = (generatorOptions instanceof StringTag)
                            ? SuperflatPreset.fromMinecraft1_12_2(((StringTag) generatorOptions).getValue())
                            : SuperflatPreset.fromMinecraft1_15_2(((CompoundTag) generatorOptions));
                    return new SuperflatGenerator(preset);
                } else if ("largeBiomes".equals(generatorName)) {
                    return new SeededGenerator(LARGE_BIOMES, getSeed());
                } else if ("DEFAULT".equalsIgnoreCase(generatorName)) {
                    return new SeededGenerator(DEFAULT, getSeed());
                } else if ("BUFFET".equalsIgnoreCase(generatorName)) {
                    return new SeededGenerator(BUFFET, getSeed());
                } else if ("CUSTOMIZED".equalsIgnoreCase(generatorName)) {
                    return new SeededGenerator(CUSTOMIZED, getSeed());
                } else {
                    return new CustomGenerator(getGeneratorName());
                }
            case DIM_NETHER:
            case DIM_END:
                return new SeededGenerator(DEFAULT, getSeed());
            default:
                return UNKNOWN;
        }
    }

    @Override
    public void setGenerator(int dim, MapGenerator generator) {
        switch (dim) {
            case DIM_NORMAL:
                if (generator instanceof SeededGenerator) {
                    setSeed(((SeededGenerator) generator).getSeed());
                }
                switch (generator.getType()) {
                    case UNKNOWN:
                        // Do nothing
                        break;
                    case DEFAULT:
                        if (getVersion() == VERSION_MCREGION) {
                            setString(TAG_GENERATOR_NAME_, "DEFAULT");
                        } else {
                            setString(TAG_GENERATOR_NAME_, "default");
                            setInt(TAG_GENERATOR_VERSION_, 1);
                        }
                        break;
                    case FLAT:
                        if (getVersion() == VERSION_MCREGION) {
                            setString(TAG_GENERATOR_NAME_, "FLAT");
                        } else {
                            setString(TAG_GENERATOR_NAME_, "flat");
                        }
                        break;
                    case LARGE_BIOMES:
                        if (getVersion() == VERSION_MCREGION) {
                            throw new IllegalArgumentException("Large biomes not supported for Minecraft 1.1 maps");
                        } else {
                            setString(TAG_GENERATOR_NAME_, "largeBiomes");
                            setInt(TAG_GENERATOR_VERSION_, 0);
                        }
                        break;
                    case BUFFET:
                    case CUSTOMIZED:
                        throw new IllegalArgumentException(generator.getType().getDisplayName() + " not supported for Exporting");
                    default:
                        throw new IllegalArgumentException("Use setGeneratorName(String) for generator " + generator);
                }
            default:
                if (generator.getType() != DEFAULT) {
                    throw new IllegalArgumentException("Generator type " + generator.getType().getDisplayName() + " not supported for dimension " + dim);
                }
                // Do nothing
                break;
        }
    }

    private Tag getGeneratorOptions() {
        return getTag(TAG_GENERATOR_OPTIONS_);
    }

    private void setGeneratorOptions(Tag generatorOptions) {
        setTag(TAG_GENERATOR_OPTIONS_, generatorOptions);
    }

    public String getGeneratorName() {
        return getString(TAG_GENERATOR_NAME_);
    }

    public int getGeneratorVersion() {
        return getInt(TAG_GENERATOR_VERSION_);
    }

    public void setGeneratorName(String generatorName) {
        setString(TAG_GENERATOR_NAME_, generatorName);
    }
}