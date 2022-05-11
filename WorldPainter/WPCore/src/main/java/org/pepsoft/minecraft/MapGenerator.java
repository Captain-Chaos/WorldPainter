package org.pepsoft.minecraft;

import org.jnbt.CompoundTag;
import org.jnbt.StringTag;
import org.jnbt.Tag;
import org.pepsoft.worldpainter.Generator;
import org.pepsoft.worldpainter.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

import static org.pepsoft.worldpainter.Generator.DEFAULT;

public abstract class MapGenerator implements Serializable {
    protected MapGenerator(Generator type) {
        this.type = type;
    }

    public Generator getType() {
        return type;
    }

    @Override
    public String toString() {
        return type.name();
    }

    private final Generator type;

    public static MapGenerator fromLegacySettings(Generator type, long seed, String name, Object options, Platform platform) {
        switch (type) {
            case DEFAULT:
                return new SeededGenerator(DEFAULT, seed);
            case FLAT:
                if (options != null) {
                    try {
                        if ((options instanceof String) && (!((String) options).trim().isEmpty())) {
                            return new SuperflatGenerator(SuperflatPreset.fromMinecraft1_12_2((String) options));
                        } else if (options instanceof StringTag) {
                            return new SuperflatGenerator(SuperflatPreset.fromMinecraft1_12_2(((StringTag) options).getValue()));
                        } else if (options instanceof CompoundTag) {
                            return new SuperflatGenerator(SuperflatPreset.fromMinecraft1_15_2((CompoundTag) options));
                        }
                    } catch (IllegalArgumentException e) {
                        logger.warn("Could not parse legacy Superflat preset \"{}\"; reverting to default settings", options, e);
                        return new SuperflatGenerator(SuperflatPreset.defaultPreset(platform));
                    }
                    throw new IllegalArgumentException("Don't know how to process options of type " + options.getClass());
                } else {
                    return new SuperflatGenerator(SuperflatPreset.defaultPreset(platform));
                }
            case LARGE_BIOMES:
            case BUFFET:
            case CUSTOMIZED:
                return new SeededGenerator(type, seed);
            case CUSTOM:
                return new CustomGenerator(name, (options instanceof Tag) ? ((Tag) options) : null);
            case UNKNOWN:
                return UNKNOWN;
            default:
                throw new InternalError("Unknown generator type " + type);
        }
    }

    public static final MapGenerator UNKNOWN = new MapGenerator(Generator.UNKNOWN) {
        private static final long serialVersionUID = 1L;
    };

    private static final Logger logger = LoggerFactory.getLogger(MapGenerator.class);
    private static final long serialVersionUID = 1L;
}