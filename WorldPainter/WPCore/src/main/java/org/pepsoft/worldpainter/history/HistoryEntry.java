package org.pepsoft.worldpainter.history;

import org.pepsoft.worldpainter.Version;

import java.io.Serializable;
import java.text.MessageFormat;

/**
 * Created by Pepijn Schmitz on 07-07-15.
 */
public class HistoryEntry implements Serializable {
    public HistoryEntry(int key, Serializable... args) {
        this.key = key;
        timestamp = System.currentTimeMillis();
        this.args = (args.length > 0) ? args : null;
    }

    public String getText() {
        switch (key) {
            case WORLD_LEGACY_PRE_0_2:
                return "World created with WorldPainter older than 0.2";
            case WORLD_LEGACY_PRE_2_0_0:
                return "World created with WorldPainter older than 2.0.0";
            case WORLD_CREATED:
                return MessageFormat.format("World created with WorldPainter {0}", wpVersion);
            case WORLD_IMPORTED_FROM_MINECRAFT_MAP:
                return MessageFormat.format("World imported from Minecraft map {0} at {1} with WorldPainter {2}", args[0], args[1], wpVersion);
            case WORLD_IMPORTED_FROM_HEIGHT_MAP:
                return MessageFormat.format("World imported from height map {0} with WorldPainter {1}", args[0], wpVersion);
            case WORLD_RECOVERED:
                return MessageFormat.format("World recovered from corrupted file with WorldPainter {0}", wpVersion);
            case WORLD_LOADED:
                return MessageFormat.format("World loaded from file {0}", args[0]);
            case WORLD_SAVED:
                return MessageFormat.format("World saved to file {0}", args[0]);
            case WORLD_EXPORTED_FULL:
                return MessageFormat.format("World fully exported as Minecraft map named {0} at {1} with WorldPainter {2}", args[0], args[1], wpVersion);
            case WORLD_EXPORTED_PARTIAL:
                return MessageFormat.format("World partially exported as Minecraft map named {0} at {1} with WorldPainter {2}", args[0], args[1], wpVersion);
            case WORLD_MERGED_FULL:
                return MessageFormat.format("World fully merged with Minecraft map named {0} at {1} with WorldPainter {2}", args[0], args[1], wpVersion);
            case WORLD_MERGED_PARTIAL:
                return MessageFormat.format("World partially merged with Minecraft map named {0} at {1} with WorldPainter {2}", args[0], args[1], wpVersion);
            case WORLD_DIMENSION_ADDED:
                return MessageFormat.format("Dimension {0} added to world", args[0]);
            case WORLD_DIMENSION_REMOVED:
                return MessageFormat.format("Dimension {0} removed from world", args[0]);
            case WORLD_TILES_ADDED:
                return MessageFormat.format("{0} tiles added to dimension {1}", args[1], args[0]);
            case WORLD_TILES_REMOVED:
                return MessageFormat.format("{0} tiles removed from dimension {1}", args[1], args[0]);
            case WORLD_DIMENSION_SHIFTED_HORIZONTALLY:
                return MessageFormat.format("Dimension {0} shifted {1} blocks east and {2} blocks south", args[0], args[1], args[2]);
            case WORLD_DIMENSION_SHIFTED_VERTICALLY:
                return MessageFormat.format("Dimension {0} shifted {1} blocks up", args[0], args[1]);
            case WORLD_DIMENSION_ROTATED:
                return MessageFormat.format("Dimension {0} rotated {1} degrees", args[0], args[1]);
            case WORLD_MAX_HEIGHT_CHANGED:
                return MessageFormat.format("Maximum height of world changed to {0}", args[0]);
            case WORLD_HEIGHT_MAP_IMPORTED_TO_DIMENSION:
                return MessageFormat.format("Height map {1} imported into dimension {0}", args[0], args[1]);
            case WORLD_MASK_IMPORTED_TO_DIMENSION:
                return MessageFormat.format("Mask {1} imported into dimension {0} as layer {2}", args[0], args[1], args[2]);
            case WORLD_RECOVERED_FROM_AUTOSAVE:
                return MessageFormat.format("World recovered from autosave with WorldPainter {0}", wpVersion);
            default:
                return MessageFormat.format("Unknown event ID {0} by WorldPainter {0} ({1})", key, wpVersion, wpBuild);
        }
    }

    public final int key;
    public final long timestamp;
    public final String wpVersion = Version.VERSION, wpBuild = Version.BUILD, userId = System.getProperty("user.name");
    public final Serializable[] args;

    public static final int WORLD_LEGACY_PRE_0_2                   =  1;
    public static final int WORLD_LEGACY_PRE_2_0_0                 =  2;
    public static final int WORLD_CREATED                          =  3;
    public static final int WORLD_IMPORTED_FROM_MINECRAFT_MAP      =  4; // arg 0: level name as String, arg 1: directory as File
    public static final int WORLD_IMPORTED_FROM_HEIGHT_MAP         =  5; // arg 0: height map file as File
    public static final int WORLD_RECOVERED                        =  6;
    public static final int WORLD_LOADED                           =  7; // arg 0: file as File
    public static final int WORLD_SAVED                            =  8; // arg 0: file as File
    public static final int WORLD_EXPORTED_FULL                    =  9; // arg 0: level name as String, arg 1: directory as File
    public static final int WORLD_EXPORTED_PARTIAL                 = 10; // arg 0: level name as String, arg 1: directory as File, arg 2: name(s) of dimension(s) as String
    public static final int WORLD_MERGED_FULL                      = 11; // arg 0: level name as String, arg 1: directory as File
    public static final int WORLD_MERGED_PARTIAL                   = 12; // arg 0: level name as String, arg 1: directory as File, arg 2: name(s) of dimension(s) as String
    public static final int WORLD_DIMENSION_ADDED                  = 13; // arg 0: name of dimension as String
    public static final int WORLD_DIMENSION_REMOVED                = 14; // arg 0: name of dimension as String
    public static final int WORLD_TILES_ADDED                      = 15; // arg 0: name of dimension as String, arg 1: number of tiles added as Integer
    public static final int WORLD_TILES_REMOVED                    = 16; // arg 0: name of dimension as String, arg 1: number of tiles removed as Integer
    public static final int WORLD_DIMENSION_SHIFTED_HORIZONTALLY   = 17; // arg 0: name of dimension as String, arg 1: number of blocks shifted east as Integer, arg 2: number of blocks shifted south as Integer
    public static final int WORLD_DIMENSION_SHIFTED_VERTICALLY     = 18; // arg 0: name of dimension as String, arg 1: number of blocks shifted up as Integer
    public static final int WORLD_DIMENSION_ROTATED                = 19; // arg 0: name of dimension as String, arg 1: number of degrees rotated clockwise as Integer
    public static final int WORLD_MAX_HEIGHT_CHANGED               = 20; // arg 0: new maxHeight as Integer
    public static final int WORLD_HEIGHT_MAP_IMPORTED_TO_DIMENSION = 21; // arg 0: name of dimension as String, arg 1: height map file as File
    public static final int WORLD_MASK_IMPORTED_TO_DIMENSION       = 22; // arg 0: name of dimension as String, arg 1: mask file as File, arg 2: name of aspect to which the mask was applied
    public static final int WORLD_RECOVERED_FROM_AUTOSAVE          = 23;

    private static final long serialVersionUID = 1L;
}