/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter;

import org.pepsoft.util.Version;
import org.pepsoft.worldpainter.vo.AttributeKeyVO;

/**
 *
 * @author pepijn
 */
public final class Constants {
    private Constants() {
        // Prevent instantiation
    }

    /**
     * Size in blocks of a tile. <b>Must be a power of two!</b>
     */
    public static final int TILE_SIZE = 128;
    /**
     * The number of bits to shift to the left to multiply with {@link #TILE_SIZE}.
     */
    public static final int TILE_SIZE_BITS = 7;
    public static final int TILE_SIZE_MASK = TILE_SIZE - 1;
    
    public static final float PETA_BLOBS     = 2097.169f;
    public static final float TERA_BLOBS     = 1048.583f;
    public static final float GIGANTIC_BLOBS =  524.309f;
    public static final float ENORMOUS_BLOBS =  262.147f;
    public static final float HUGE_BLOBS     =  131.101f;
    public static final float LARGE_BLOBS    =   65.537f;
    public static final float MEDIUM_BLOBS   =   32.771f;
    public static final float SMALL_BLOBS    =   16.411f;
    public static final float TINY_BLOBS     =    4.099f;
    
    public static final int UNKNOWN_MATERIAL_COLOUR = 0xFF00FF;

    public static final int DIM_END_CEILING    = -3;
    public static final int DIM_NETHER_CEILING = -2;
    public static final int DIM_NORMAL_CEILING = -1;
    public static final int DIM_NORMAL         =  0;
    public static final int DIM_NETHER         =  1;
    public static final int DIM_END            =  2;

    public static final AttributeKeyVO<Integer> ATTRIBUTE_KEY_MAX_HEIGHT = new AttributeKeyVO<>("maxHeight");
    public static final AttributeKeyVO<Boolean> ATTRIBUTE_KEY_MAP_FEATURES = new AttributeKeyVO<>("mapFeatures");
    public static final AttributeKeyVO<String> ATTRIBUTE_KEY_GAME_TYPE_NAME = new AttributeKeyVO<>("gameTypeName");
    public static final AttributeKeyVO<Boolean> ATTRIBUTE_KEY_ALLOW_CHEATS = new AttributeKeyVO<>("allowCheats");
    public static final AttributeKeyVO<String> ATTRIBUTE_KEY_GENERATOR = new AttributeKeyVO<>("generator");
    public static final AttributeKeyVO<String> ATTRIBUTE_KEY_GENERATOR_OPTIONS = new AttributeKeyVO<>("generatorOptions");
    public static final AttributeKeyVO<Integer> ATTRIBUTE_KEY_TILES = new AttributeKeyVO<>("tiles");
    public static final AttributeKeyVO<Integer> ATTRIBUTE_KEY_NETHER_TILES = new AttributeKeyVO<>("nether.tiles");
    public static final AttributeKeyVO<Integer> ATTRIBUTE_KEY_END_TILES = new AttributeKeyVO<>("end.tiles");
    public static final AttributeKeyVO<Integer> ATTRIBUTE_KEY_EXPORTED_DIMENSION = new AttributeKeyVO<>("exportedDimension");
    public static final AttributeKeyVO<Integer> ATTRIBUTE_KEY_EXPORTED_DIMENSION_TILES = new AttributeKeyVO<>("exportedDimension.tiles");
    public static final AttributeKeyVO<Boolean> ATTRIBUTE_KEY_IMPORTED_WORLD = new AttributeKeyVO<>("importedWorld");
    public static final AttributeKeyVO<String> ATTRIBUTE_KEY_PLUGINS = new AttributeKeyVO<>("plugins");
    public static final AttributeKeyVO<String> ATTRIBUTE_KEY_SCRIPT_FILENAME = new AttributeKeyVO<>("script.fileName");
    public static final AttributeKeyVO<String> ATTRIBUTE_KEY_SCRIPT_NAME = new AttributeKeyVO<>("script.name");
    public static final AttributeKeyVO<String> ATTRIBUTE_KEY_PLATFORM = new AttributeKeyVO<>("platform");
    public static final AttributeKeyVO<Boolean> ATTRIBUTE_KEY_SAFE_MODE = new AttributeKeyVO<>("safeMode");

    public static final String EVENT_KEY_ACTION_NEW_WORLD         = "action.newWorld";
    public static final String EVENT_KEY_ACTION_EXPORT_WORLD      = "action.exportWorld";
    public static final String EVENT_KEY_ACTION_IMPORT_MAP        = "action.importMap";
    public static final String EVENT_KEY_ACTION_MERGE_WORLD       = "action.mergeWorld";
    public static final String EVENT_KEY_ACTION_OPEN_WORLD        = "action.openWorld";
    public static final String EVENT_KEY_ACTION_SAVE_WORLD        = "action.saveWorld";
    public static final String EVENT_KEY_ACTION_MIGRATE_WORLD     = "action.migrateWorld";
    public static final String EVENT_KEY_ACTION_MIGRATE_HEIGHT    = "action.migrateHeight";
    public static final String EVENT_KEY_ACTION_MIGRATE_ROTATION  = "action.migrateRotation";
    public static final String EVENT_KEY_DONATION_DONATE          = "donation.donate";
    public static final String EVENT_KEY_DONATION_DONATE_BITCOIN  = "donation.donateBitcoin";
    public static final String EVENT_KEY_DONATION_ALREADY_DONATED = "donation.alreadyDonated";
    public static final String EVENT_KEY_DONATION_ASK_LATER       = "donation.askLater";
    public static final String EVENT_KEY_DONATION_NO_THANKS       = "donation.noThanks";
    public static final String EVENT_KEY_DONATION_CLOSED          = "donation.closed";

    public static final int BIOME_ALGORITHM_1_1                 =  4;
    public static final int BIOME_ALGORITHM_1_2_AND_1_3_DEFAULT =  5;
    public static final int BIOME_ALGORITHM_1_3_LARGE           =  8;
    public static final int BIOME_ALGORITHM_1_7_DEFAULT         =  9;
    public static final int BIOME_ALGORITHM_1_7_LARGE           = 10;

    public static final Version V_1_12_2 = new Version(1, 12, 2);

    public static final int MIN_HEIGHT = -8388608;
    public static final int MAX_HEIGHT = 8388608;
}