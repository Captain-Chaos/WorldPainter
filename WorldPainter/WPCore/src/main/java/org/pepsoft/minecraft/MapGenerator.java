package org.pepsoft.minecraft;

import org.jnbt.CompoundTag;
import org.jnbt.StringTag;
import org.pepsoft.worldpainter.Generator;
import org.pepsoft.worldpainter.Platform;

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
                if ((options instanceof String) && (! ((String) options).trim().isEmpty())) {
                    return new SuperflatGenerator(SuperflatPreset.fromMinecraft1_12_2((String) options));
                } else if (options instanceof StringTag) {
                    return new SuperflatGenerator(SuperflatPreset.fromMinecraft1_12_2(((StringTag) options).getValue()));
                } else if (options instanceof CompoundTag) {
                    return new SuperflatGenerator(SuperflatPreset.fromMinecraft1_15_2((CompoundTag) options));
                } else if (options == null) {
                    return new SuperflatGenerator(SuperflatPreset.defaultPreset(platform));
                } else {
                    throw new IllegalArgumentException("Don't know how to process options of type " + options.getClass());
                }
            case LARGE_BIOMES:
            case BUFFET:
            case CUSTOMIZED:
                return new SeededGenerator(type, seed);
            case CUSTOM:
                return new CustomGenerator(name);
            case UNKNOWN:
                return UNKNOWN;
            default:
                throw new InternalError("Unknown generator type " + type);
        }
    }

    public static final MapGenerator UNKNOWN = new MapGenerator(Generator.UNKNOWN) {
        private static final long serialVersionUID = 1L;
    };

    private static final long serialVersionUID = 1L;
}