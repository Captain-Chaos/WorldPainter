package com.gmail.frogocomics;

import java.util.Optional;

/**
 * Stores various types of tasks, usually found in global operations and brush tools.
 *
 * @author Jeff Chen
 */
public enum TaskTypes {
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
    FLOOD_FILL,
    TEXT,
    WATER_FLOOD,
    LAVA_FLOOD,
    SPONGE,
    RISE,
    FLATTEN,
    SMOOTH,
    MOUNTAIN,
    PYRAMID,
    PYRAMID_ROTATED,
    MASK_IMPORT,
    //Other
    UNKNOWN, //Represents an unknown operations. Should not be used.
    CUSTOM;   //Represents a custom operation from perhaps a plugin

    /**
     * Get a {@link String} to display in the gui about information of the current running
     * operation.
     *
     * @param type The type of operation to get information about.
     * @param string This can be the name of the terrain or layer involved in the operation, and
     *               will not be utilized for all possible {@link TaskTypes}s. If an empty optional
     *               is specified for a {@link TaskTypes} that requires a string, "Unknown" will
     *               be used.
     * @return Returns a string to be used in the gui about the current operation being worked on.
     */
    public static String getStatus(TaskTypes type, Optional<String> string) {
        switch(type) {
            case TERRAIN_FILL:
                return "Filling with terrain " + stringOrUnknown(string);
            case TERRAIN_RESET:
                return "Resetting all terrain to default";
            case LAYER_FILL:
                return "Filling with layer" + stringOrUnknown(string);
            case LAYER_INVERT:
                return "Inverting layer" + stringOrUnknown(string);
            case BIOME_FILL:
                return "Filling with biome" + stringOrUnknown(string);
            case BIOME_PERMANENT:
                return "Making automatic biomes permanent";
            case BIOME_RESET:
                return "Resetting biomes to automatic";
            case WATER_LAVA_RESET:
                return "Resetting all water or lava";
            case PAINT:
                return "Painting terrain or layer";
            case PENCIL:
                return "Drawing terrain or layer";
            case FLOOD_FILL:
                return "Floodfilling with " + stringOrUnknown(string);
            case TEXT:
                return "Adding text on terrain";
            case WATER_FLOOD:
                return "Flooding with water";
            case LAVA_FLOOD:
                return "Flooding with lava";
            case SPONGE:
                return "Removing water";
            case RISE:
                return "Raising or lowering terrain";
            case FLATTEN:
                return "Flattening terrain";
            case SMOOTH:
                return "Smoothing terrain";
            case MOUNTAIN:
                return "Creating mountain";
            case PYRAMID:
                return "Creating pyramid";
            case PYRAMID_ROTATED:
                return "Creating rotated pyramid";
            case MASK_IMPORT:
                return "Import mask as terrain or layer";
            case UNKNOWN:
                return "Unknown Operation";
            case CUSTOM:
                return null;
            default:
                return "Should not happen!";
        }
    }

    /**
     * Converts an {@link Optional<String>} to a {@link String} but converts it to "Unknonwn" if
     * the Optional is empty.
     *
     * @param s The Optional to convert.
     * @return Returns the String in the optional or "Unknown" if the Optional is empty.
     */
    private static String stringOrUnknown(Optional<String> s) {
        if(s.isPresent()) {
            return s.get();
        }
        return "Unknown";
    }
}
