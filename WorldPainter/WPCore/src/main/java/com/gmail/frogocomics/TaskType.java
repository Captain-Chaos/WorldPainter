package com.gmail.frogocomics;

/**
 * Stores various types of tasks, usually found in global operations
 */
public enum TaskType {
    //From global operations
    TERRAIN_FILL,
    TERRAIN_RESET,
    LAYER_FILL,
    LAYER_REMOVE,
    LAYER_INVERT,
    BIOME_FILL,
    BIOME_PERMANENT,
    BIOME_RESET,
    WATER_LAVA_RESET,

    //Tools
    PAINT,
    PENCIL,
    BUCKET,
    TEXT,
    WATER_FLOOD,
    LAVA_FLOOD,
    SPONGE,
    RISE,
    FATTEN,
    SMOOTH,
    MOUNTAIN,
    PYRAMID,
    PYRAMID_ROTATED,

    //Other
    UNKNOWN,
    CUSTOM
}
