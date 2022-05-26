package org.pepsoft.minecraft;

import org.jnbt.CompoundTag;
import org.jnbt.StringTag;
import org.jnbt.Tag;
import org.pepsoft.worldpainter.Generator;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.World2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.function.Consumer;

import static org.pepsoft.worldpainter.World2.Warning.SUPERFLAT_SETTINGS_RESET;

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

    public static MapGenerator fromLegacySettings(Generator type, long seed, String name, Object options, Platform platform, Consumer<World2.Warning> warningConsumer) {
        switch (type) {
            case DEFAULT:
            case LARGE_BIOMES:
            case BUFFET:
            case CUSTOMIZED:
                return new SeededGenerator(type, seed);
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
                        logger.error("Could not parse legacy Superflat preset \"{}\"; reverting to default settings", options, e);
                        if (warningConsumer != null) {
                            warningConsumer.accept(SUPERFLAT_SETTINGS_RESET);
                        }
                        return new SuperflatGenerator(SuperflatPreset.defaultPreset(platform));
                    }
                    logger.error("Could not parse legacy Superflat preset \"{}\"; reverting to default settings", options);
                    if (warningConsumer != null) {
                        warningConsumer.accept(SUPERFLAT_SETTINGS_RESET);
                    }
                    return new SuperflatGenerator(SuperflatPreset.defaultPreset(platform));
                } else {
                    logger.warn("No legacy Superflat preset present; reverting to default settings");
                    if (warningConsumer != null) {
                        warningConsumer.accept(SUPERFLAT_SETTINGS_RESET);
                    }
                    return new SuperflatGenerator(SuperflatPreset.defaultPreset(platform));
                }
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