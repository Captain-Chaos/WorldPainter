/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft;

import java.util.*;

/**
 *
 * @author pepijn
 */
public final class Constants {
    private Constants() {
        // Prevent instantiation
    }
    
    public static final String TAG_LEVEL                   = "Level";
    public static final String TAG_BLOCKS                  = "Blocks";
    public static final String TAG_DATA                    = "Data";
    public static final String TAG_SKY_LIGHT               = "SkyLight";
    public static final String TAG_BLOCK_LIGHT             = "BlockLight";
    public static final String TAG_HEIGHT_MAP              = "HeightMap";
    public static final String TAG_ENTITIES                = "Entities";
    public static final String TAG_TILE_ENTITIES           = "TileEntities";
    public static final String TAG_LAST_UPDATE             = "LastUpdate";
    public static final String TAG_X_POS                   = "xPos";
    public static final String TAG_Z_POS                   = "zPos";
    public static final String TAG_TERRAIN_POPULATED       = "TerrainPopulated";
    public static final String TAG_TIME                    = "Time";
    public static final String TAG_LAST_PLAYED             = "LastPlayed";
    public static final String TAG_PLAYER                  = "Player";
    public static final String TAG_INVENTORY               = "Inventory";
    public static final String TAG_SCORE                   = "Score";
    public static final String TAG_DIMENSION               = "Dimension";
    public static final String TAG_SPAWN_X                 = "SpawnX";
    public static final String TAG_SPAWN_Y                 = "SpawnY";
    public static final String TAG_SPAWN_Z                 = "SpawnZ";
    public static final String TAG_SIZE_ON_DISK            = "SizeOnDisk";
    public static final String TAG_RANDOM_SEED             = "RandomSeed";
    public static final String TAG_VERSION                 = "version";
    public static final String TAG_LEVEL_NAME              = "LevelName";
    public static final String TAG_ID                      = "id";
    public static final String TAG_POS                     = "Pos";
    public static final String TAG_MOTION                  = "Motion";
    public static final String TAG_ROTATION                = "Rotation";
    public static final String TAG_FALL_DISTANCE           = "FallDistance";
    public static final String TAG_FIRE                    = "Fire";
    public static final String TAG_AIR                     = "Air";
    public static final String TAG_ON_GROUND               = "OnGround";
    public static final String TAG_ATTACK_TIME             = "AttackTime";
    public static final String TAG_DEATH_TIME              = "DeathTime";
    public static final String TAG_HEALTH                  = "Health";
    public static final String TAG_HURT_TIME               = "HurtTime";
    public static final String TAG_DAMAGE                  = "Damage";
    public static final String TAG_COUNT                   = "Count";
    public static final String TAG_SLOT                    = "Slot";
    public static final String TAG_X                       = "x";
    public static final String TAG_Y                       = "y";
    public static final String TAG_Z                       = "z";
    public static final String TAG_ITEMS                   = "Items";
    public static final String TAG_MAP_FEATURES            = "MapFeatures";
    public static final String TAG_GAME_TYPE               = "GameType";
    public static final String TAG_GENERATOR_NAME          = "generatorName";
    public static final String TAG_Y2                      = "Y";
    public static final String TAG_SECTIONS                = "Sections";
    public static final String TAG_ADD                     = "Add";
    public static final String TAG_BIOMES                  = "Biomes";
    public static final String TAG_GENERATOR_VERSION       = "generatorVersion";
    public static final String TAG_PROFESSION              = "Profession";
    public static final String TAG_TEXT1                   = "Text1";
    public static final String TAG_TEXT2                   = "Text2";
    public static final String TAG_TEXT3                   = "Text3";
    public static final String TAG_TEXT4                   = "Text4";
    public static final String TAG_DIR                     = "Dir";
    public static final String TAG_MOTIVE                  = "Motive";
    public static final String TAG_TILE_X                  = "TileX";
    public static final String TAG_TILE_Y                  = "TileY";
    public static final String TAG_TILE_Z                  = "TileZ";
    public static final String TAG_ALLOW_COMMANDS          = "allowCommands";
    public static final String TAG_GENERATOR_OPTIONS       = "generatorOptions";
    public static final String TAG_HARDCORE                = "hardcore";
    public static final String TAG_DIFFICULTY              = "Difficulty";
    public static final String TAG_DIFFICULTY_LOCKED       = "DifficultyLocked";
    public static final String TAG_LIGHT_POPULATED         = "LightPopulated";
    public static final String TAG_INHABITED_TIME          = "InhabitedTime";
    public static final String TAG_BORDER_CENTER_X         = "BorderCenterX";
    public static final String TAG_BORDER_CENTER_Z         = "BorderCenterZ";
    public static final String TAG_BORDER_SIZE             = "BorderSize";
    public static final String TAG_BORDER_SAFE_ZONE        = "BorderSafeZone";
    public static final String TAG_BORDER_WARNING_BLOCKS   = "BorderWarningBlocks";
    public static final String TAG_BORDER_WARNING_TIME     = "BorderWarningTime";
    public static final String TAG_BORDER_SIZE_LERP_TARGET = "BorderSizeLerpTarget";
    public static final String TAG_BORDER_SIZE_LERP_TIME   = "BorderSizeLerpTime";
    public static final String TAG_BORDER_DAMAGE_PER_BLOCK = "BorderDamagePerBlock";
    
    /**
     * Possibly unofficial, SpoutCraft-specific
     */
    public static final String TAG_MAP_HEIGHT = "MapHeight";

    public static final int SUPPORTED_VERSION_1 = 0x4abc;
    public static final int SUPPORTED_VERSION_2 = 0x4abd;

    public static final int BLK_AIR                   =   0;
    public static final int BLK_STONE                 =   1;
    public static final int BLK_GRASS                 =   2;
    public static final int BLK_DIRT                  =   3;
    public static final int BLK_COBBLESTONE           =   4;
    public static final int BLK_WOODEN_PLANK          =   5;
    public static final int BLK_SAPLING               =   6;
    public static final int BLK_BEDROCK               =   7;
    public static final int BLK_WATER                 =   8;
    public static final int BLK_STATIONARY_WATER      =   9;
    public static final int BLK_LAVA                  =  10;
    public static final int BLK_STATIONARY_LAVA       =  11;
    public static final int BLK_SAND                  =  12;
    public static final int BLK_GRAVEL                =  13;
    public static final int BLK_GOLD_ORE              =  14;
    public static final int BLK_IRON_ORE              =  15;
    public static final int BLK_COAL                  =  16;
    public static final int BLK_WOOD                  =  17;
    public static final int BLK_LEAVES                =  18;
    public static final int BLK_SPONGE                =  19;
    public static final int BLK_GLASS                 =  20;
    public static final int BLK_LAPIS_LAZULI_ORE      =  21;
    public static final int BLK_LAPIS_LAZULI_BLOCK    =  22;
    public static final int BLK_DISPENSER             =  23;
    public static final int BLK_SANDSTONE             =  24;
    public static final int BLK_NOTE_BLOCK            =  25;
    public static final int BLK_BED                   =  26;
    public static final int BLK_POWERED_RAILS         =  27;
    public static final int BLK_DETECTOR_RAILS        =  28;

    public static final int BLK_COBWEB                =  30;
    public static final int BLK_TALL_GRASS            =  31;
    public static final int BLK_DEAD_SHRUBS           =  32;
    public static final int BLK_PISTON                =  33;
    public static final int BLK_PISTON_HEAD           =  34;
    public static final int BLK_WOOL                  =  35;
    public static final int BLK_PISTON_EXTENSION      =  36;
    public static final int BLK_DANDELION             =  37;
    public static final int BLK_ROSE                  =  38;
    public static final int BLK_BROWN_MUSHROOM        =  39;
    public static final int BLK_RED_MUSHROOM          =  40;
    public static final int BLK_GOLD_BLOCK            =  41;
    public static final int BLK_IRON_BLOCK            =  42;
    public static final int BLK_DOUBLE_SLAB           =  43;
    public static final int BLK_SLAB                  =  44;
    public static final int BLK_BRICKS                =  45;
    public static final int BLK_TNT                   =  46;
    public static final int BLK_BOOKCASE              =  47;
    public static final int BLK_MOSSY_COBBLESTONE     =  48;
    public static final int BLK_OBSIDIAN              =  49;
    public static final int BLK_TORCH                 =  50;
    public static final int BLK_FIRE                  =  51;
    public static final int BLK_MONSTER_SPAWNER       =  52;
    public static final int BLK_WOODEN_STAIRS         =  53;
    public static final int BLK_CHEST                 =  54;
    public static final int BLK_REDSTONE_WIRE         =  55;
    public static final int BLK_DIAMOND_ORE           =  56;
    public static final int BLK_DIAMOND_BLOCK         =  57;
    public static final int BLK_CRAFTING_TABLE        =  58;
    public static final int BLK_WHEAT                 =  59;
    public static final int BLK_TILLED_DIRT           =  60;
    public static final int BLK_FURNACE               =  61;
    public static final int BLK_BURNING_FURNACE       =  62;
    public static final int BLK_SIGN                  =  63;
    public static final int BLK_WOODEN_DOOR           =  64;
    public static final int BLK_LADDER                =  65;
    public static final int BLK_RAILS                 =  66;
    public static final int BLK_COBBLESTONE_STAIRS    =  67;
    public static final int BLK_WALL_SIGN             =  68;
    public static final int BLK_LEVER                 =  69;
    public static final int BLK_STONE_PRESSURE_PLATE  =  70;
    public static final int BLK_IRON_DOOR             =  71;
    public static final int BLK_WOODEN_PRESSURE_PLATE =  72;
    public static final int BLK_REDSTONE_ORE          =  73;
    public static final int BLK_GLOWING_REDSTONE_ORE  =  74;
    public static final int BLK_REDSTONE_TORCH_OFF    =  75;
    public static final int BLK_REDSTONE_TORCH_ON     =  76;
    public static final int BLK_STONE_BUTTON          =  77;
    public static final int BLK_SNOW                  =  78;
    public static final int BLK_ICE                   =  79;
    public static final int BLK_SNOW_BLOCK            =  80;
    public static final int BLK_CACTUS                =  81;
    public static final int BLK_CLAY                  =  82;
    public static final int BLK_SUGAR_CANE            =  83;
    public static final int BLK_JUKEBOX               =  84;
    public static final int BLK_FENCE                 =  85;
    public static final int BLK_PUMPKIN               =  86;
    public static final int BLK_NETHERRACK            =  87;
    public static final int BLK_SOUL_SAND             =  88;
    public static final int BLK_GLOWSTONE             =  89;
    public static final int BLK_PORTAL                =  90;
    public static final int BLK_JACK_O_LANTERN        =  91;
    public static final int BLK_CAKE                  =  92;
    public static final int BLK_REDSTONE_REPEATER_OFF =  93;
    public static final int BLK_REDSTONE_REPEATER_ON  =  94;

    public static final int BLK_TRAPDOOR              =  96;
    public static final int BLK_HIDDEN_SILVERFISH     =  97;
    public static final int BLK_STONE_BRICKS          =  98;
    public static final int BLK_HUGE_BROWN_MUSHROOM   =  99;
    public static final int BLK_HUGE_RED_MUSHROOM     = 100;
    public static final int BLK_IRON_BARS             = 101;
    public static final int BLK_GLASS_PANE            = 102;
    public static final int BLK_MELON                 = 103;
    public static final int BLK_PUMPKIN_STEM          = 104;
    public static final int BLK_MELON_STEM            = 105;
    public static final int BLK_VINES                 = 106;
    public static final int BLK_FENCE_GATE            = 107;
    public static final int BLK_BRICK_STAIRS          = 108;
    public static final int BLK_STONE_BRICK_STAIRS    = 109;
    public static final int BLK_MYCELIUM              = 110;
    public static final int BLK_LILY_PAD              = 111;
    public static final int BLK_NETHER_BRICK          = 112;
    public static final int BLK_NETHER_BRICK_FENCE    = 113;
    public static final int BLK_NETHER_BRICK_STAIRS   = 114;
    public static final int BLK_NETHER_WART           = 115;
    public static final int BLK_ENCHANTMENT_TABLE     = 116;
    public static final int BLK_BREWING_STAND         = 117;
    public static final int BLK_CAULDRON              = 118;
    public static final int BLK_END_PORTAL            = 119;
    public static final int BLK_END_PORTAL_FRAME      = 120;
    public static final int BLK_END_STONE             = 121;
    public static final int BLK_DRAGON_EGG            = 122;
    public static final int BLK_REDSTONE_LANTERN_OFF  = 123;
    public static final int BLK_REDSTONE_LANTERN_ON   = 124;
    public static final int BLK_DOUBLE_WOODEN_SLAB    = 125;
    public static final int BLK_WOODEN_SLAB           = 126;
    public static final int BLK_COCOA_PLANT           = 127;
    public static final int BLK_SANDSTONE_STAIRS      = 128;
    public static final int BLK_EMERALD_ORE           = 129;
    public static final int BLK_ENDER_CHEST           = 130;
    public static final int BLK_TRIPWIRE_HOOK         = 131;
    public static final int BLK_TRIPWIRE              = 132;
    public static final int BLK_EMERALD_BLOCK         = 133;
    public static final int BLK_PINE_WOOD_STAIRS      = 134;
    public static final int BLK_BIRCH_WOOD_STAIRS     = 135;
    public static final int BLK_JUNGLE_WOOD_STAIRS    = 136;
    public static final int BLK_COMMAND_BLOCK         = 137;
    public static final int BLK_BEACON                = 138;
    public static final int BLK_COBBLESTONE_WALL      = 139;
    public static final int BLK_FLOWER_POT            = 140;
    public static final int BLK_CARROTS               = 141;
    public static final int BLK_POTATOES              = 142;
    public static final int BLK_WOODEN_BUTTON         = 143;
    public static final int BLK_HEAD                  = 144;
    public static final int BLK_ANVIL                 = 144;
    
    public static final int BLK_TRAPPED_CHEST                 = 146;
    public static final int BLK_WEIGHTED_PRESSURE_PLATE_LIGHT = 147;
    public static final int BLK_WEIGHTED_PRESSURE_PLATE_HEAVY = 148;
    public static final int BLK_REDSTONE_COMPARATOR_UNPOWERED = 149;
    public static final int BLK_REDSTONE_COMPARATOR_POWERED   = 150;
    public static final int BLK_DAYLIGHT_SENSOR               = 151;
    
    public static final int BLK_QUARTZ_ORE = 153;
    public static final int BLK_HOPPER     = 154;
    
    public static final int BLK_QUARTZ_STAIRS        = 156;
    public static final int BLK_ACTIVATOR_RAIL       = 157;
    public static final int BLK_DROPPER              = 158;
    public static final int BLK_STAINED_CLAY         = 159;
    public static final int BLK_STAINED_GLASS_PANE   = 160;
    public static final int BLK_LEAVES2              = 161;
    public static final int BLK_WOOD2                = 162;
    public static final int BLK_ACACIA_WOOD_STAIRS   = 163;
    public static final int BLK_DARK_OAK_WOOD_STAIRS = 164;
    public static final int BLK_SLIME_BLOCK          = 165;
    public static final int BLK_BARRIER              = 166;
    public static final int BLK_IRON_TRAPDOOR        = 167;

    public static final int BLK_CARPET                    = 171;
    public static final int BLK_HARDENED_CLAY             = 172;
    public static final int BLK_COAL_BLOCK                = 173;
    public static final int BLK_PACKED_ICE                = 174;
    public static final int BLK_LARGE_FLOWERS             = 175;
    public static final int BLK_STANDING_BANNER           = 176;
    public static final int BLK_WALL_BANNER               = 177;
    public static final int BLK_DAYLIGHT_SENSOR_INVERTED  = 178;
    public static final int BLK_RED_SANDSTONE             = 179;
    public static final int BLK_RED_SANDSTONE_STAIRS      = 180;
    public static final int BLK_DOUBLE_RED_SANDSTONE_SLAB = 181;
    public static final int BLK_RED_SANDSTONE_SLAB        = 182;
    public static final int BLK_PINE_WOOD_FENCE_GATE      = 183;
    public static final int BLK_BIRCH_WOOD_FENCE_GATE     = 184;
    public static final int BLK_JUNGLE_WOOD_FENCE_GATE    = 185;
    public static final int BLK_DARK_OAK_WOOD_FENCE_GATE  = 186;
    public static final int BLK_ACACIA_WOOD_FENCE_GATE    = 187;
    public static final int BLK_PINE_WOOD_FENCE           = 188;
    public static final int BLK_BIRCH_WOOD_FENCE          = 189;
    public static final int BLK_JUNGLE_WOOD_FENCE         = 190;
    public static final int BLK_DARK_OAK_WOOD_FENCE       = 191;
    public static final int BLK_ACACIA_WOOD_FENCE         = 192;
    public static final int BLK_PINE_WOOD_DOOR            = 193;
    public static final int BLK_BIRCH_WOOD_DOOR           = 194;
    public static final int BLK_JUNGLE_WOOD_DOOR          = 195;
    public static final int BLK_ACACIA_WOOD_DOOR          = 196;
    public static final int BLK_DARK_OAK_WOOD_DOOR        = 197;
    public static final int BLK_END_ROD                   = 198;
    public static final int BLK_CHORUS_PLANT              = 199;
    public static final int BLK_CHORUS_FLOWER             = 200;
    public static final int BLK_PURPUR_BLOCK              = 201;
    public static final int BLK_PURPUR_PILLAR             = 202;
    public static final int BLK_PURPUR_STAIRS             = 203;
    public static final int BLK_DOUBLE_PURPUR_SLAB        = 204;
    public static final int BLK_PURPUR_SLAB               = 205;
    public static final int BLK_END_BRICKS                = 206;
    public static final int BLK_BEETROOTS                 = 207;
    public static final int BLK_GRASS_PATH                = 208;
    public static final int BLK_END_GATEWAY               = 209;
    public static final int BLK_REPEATING_COMMAND_BLOCK   = 210;
    public static final int BLK_CHAIN_COMMAND_BLOCK       = 211;
    public static final int BLK_FROSTED_ICE               = 212;

    public static final int HIGHEST_KNOWN_BLOCK_ID = BLK_FROSTED_ICE;

    public static final int DATA_OAK      = 0;
    public static final int DATA_PINE     = 1;
    public static final int DATA_BIRCH    = 2;
    public static final int DATA_JUNGLE   = 3;
    public static final int DATA_ACACIA   = 0;
    public static final int DATA_DARK_OAK = 1;
    
    public static final int DATA_DEAD_SHRUB = 0;
    public static final int DATA_TALL_GRASS = 1;
    public static final int DATA_FERN       = 2;
    
    public static final int DATA_DOOR_BOTTOM              = 0;
    public static final int DATA_DOOR_BOTTOM_CLOSED       = 0;
    public static final int DATA_DOOR_BOTTOM_OPEN         = 4;
    public static final int DATA_DOOR_BOTTOM_FACING_WEST  = 0;
    public static final int DATA_DOOR_BOTTOM_FACING_NORTH = 1;
    public static final int DATA_DOOR_BOTTOM_FACING_EAST  = 2;
    public static final int DATA_DOOR_BOTTOM_FACING_SOUTH = 3;
    public static final int DATA_DOOR_TOP                 = 8;
    public static final int DATA_DOOR_TOP_HINGE_RIGHT     = 0;
    public static final int DATA_DOOR_TOP_HINGE_LEFT      = 1;
    
    public static final int DATA_SLAB_STONE       = 0;
    public static final int DATA_SLAB_SANDSTONE   = 1;
    public static final int DATA_SLAB_WOOD        = 2;
    public static final int DATA_SLAB_COBBLESTONE = 3;
    public static final int DATA_SLAB_BRICK       = 4;
    public static final int DATA_SLAB_STONE_BRICK = 5;

    public static final int DATA_ASCENDING_EAST  = 0;
    public static final int DATA_ASCENDING_WEST  = 1;
    public static final int DATA_ASCENDING_SOUTH = 2;
    public static final int DATA_ASCENDING_NORTH = 3;
    
    public static final int DATA_BED_FOOT = 0;
    public static final int DATA_BED_HEAD = 8;
    public static final int DATA_BED_SOUTH = 0;
    public static final int DATA_BED_WEST  = 1;
    public static final int DATA_BED_NORTH = 2;
    public static final int DATA_BED_EAST  = 3;

    public static final int DATA_PUMPKIN_SOUTH_FACE = 0;
    public static final int DATA_PUMPKIN_WEST_FACE  = 1;
    public static final int DATA_PUMPKIN_NORTH_FACE = 2;
    public static final int DATA_PUMPKIN_EAST_FACE  = 3;
    public static final int DATA_PUMPKIN_NO_FACE    = 4;
    
    public static final int DATA_WHITE      =  0;
    public static final int DATA_ORANGE     =  1;
    public static final int DATA_MAGENTA    =  2;
    public static final int DATA_LIGHT_BLUE =  3;
    public static final int DATA_YELLOW     =  4;
    public static final int DATA_LIME       =  5;
    public static final int DATA_PINK       =  6;
    public static final int DATA_GREY       =  7;
    public static final int DATA_LIGHT_GREY =  8;
    public static final int DATA_CYAN       =  9;
    public static final int DATA_PURPLE     = 10;
    public static final int DATA_BLUE       = 11;
    public static final int DATA_BROWN      = 12;
    public static final int DATA_GREEN      = 13;
    public static final int DATA_RED        = 14;
    public static final int DATA_BLACK      = 15;

    public static final String[] COLOUR_NAMES = {"White", "Orange", "Magenta", "Light Blue", "Yellow", "Lime", "Pink",
        "Grey", "Light Grey", "Cyan", "Purple", "Blue", "Brown", "Green", "Red", "Black"};

    public static final int DATA_STONE_STONE             = 0;
    public static final int DATA_STONE_GRANITE           = 1;
    public static final int DATA_STONE_GRANITE_POLISHED  = 2;
    public static final int DATA_STONE_DIORITE           = 3;
    public static final int DATA_STONE_DIORITE_POLISHED  = 4;
    public static final int DATA_STONE_ANDESITE          = 5;
    public static final int DATA_STONE_ANDESITE_POLISHED = 6;

    public static final int ITM_FLINT_AND_STEEL = 259;
    
    public static final int ITM_COAL            = 263;
    
    public static final int ITM_DIAMOND         = 264;
    public static final int ITM_IRON_INGOT      = 265;
    public static final int ITM_GOLD_INGOT      = 266;
    
    public static final int ITM_DIAMOND_SWORD   = 276;
    public static final int ITM_DIAMOND_SHOVEL  = 277;
    public static final int ITM_DIAMOND_PICKAXE = 278;
    public static final int ITM_DIAMOND_AXE     = 279;
    
    public static final int ITM_WATER_BUCKET    = 326;
    
    public static final int ITM_SUGAR_CANE      = 338;
    
    public static final int ITM_BONE            = 352;
    
    public static final int ITM_BED             = 355;

    public static final int ITM_EYE_OF_ENDER    = 381;
    
    public static final String ID_CHEST        = "Chest";
    public static final String ID_VILLAGER     = "Villager";
    public static final String ID_PLAYER       = "Player";
    public static final String ID_SIGN         = "Sign";
    public static final String ID_PAINTING     = "Painting";
    public static final String ID_AIRPORTAL    = "Airportal";
    public static final String ID_BEACON       = "Beacon";
    public static final String ID_CAULDRON     = "Cauldron";
    public static final String ID_COMPARATOR   = "Comparator";
    public static final String ID_CONTROL      = "Control";
    public static final String ID_DLDETECTOR   = "DLDetector";
    public static final String ID_DROPPER      = "Dropper";
    public static final String ID_ENCHANTTABLE = "EnchantTable";
    public static final String ID_ENDERCHEST   = "EnderChest";
    public static final String ID_FLOWERPOT    = "FlowerPot";
    public static final String ID_FURNACE      = "Furnace";
    public static final String ID_HOPPER       = "Hopper";
    public static final String ID_MOBSPAWNER   = "MobSpawner";
    public static final String ID_MUSIC        = "Music";
    public static final String ID_PISTON       = "Piston";
    public static final String ID_RECORDPLAYER = "RecordPlayer";
    public static final String ID_SKULL        = "Skull";
    public static final String ID_TRAP         = "Trap";
    public static final String ID_BANNER       = "Banner";

    public static final int GAME_TYPE_SURVIVAL  = 0;
    public static final int GAME_TYPE_CREATIVE  = 1;
    public static final int GAME_TYPE_ADVENTURE = 2;
    
    public static final int PROFESSION_FARMER     = 0;
    public static final int PROFESSION_LIBRARIAN  = 1;
    public static final int PROFESSION_PRIEST     = 2;
    public static final int PROFESSION_BLACKSMITH = 3;
    public static final int PROFESSION_BUTCHER    = 4;
    
    public static final String MOTIVE_KEBAB           = "Kebab";
    public static final String MOTIVE_AZTEC           = "Aztec";
    public static final String MOTIVE_ALBAN           = "Alban";
    public static final String MOTIVE_AZTEC2          = "Aztec2";
    public static final String MOTIVE_BOMB            = "Bomb";
    public static final String MOTIVE_PLANT           = "Plant";
    public static final String MOTIVE_WASTELAND       = "Wasteland";
    public static final String MOTIVE_WANDERER        = "Wanderer";
    public static final String MOTIVE_GRAHAM          = "Graham";
    public static final String MOTIVE_POOL            = "Pool";
    public static final String MOTIVE_COURBET         = "Courbet";
    public static final String MOTIVE_SUNSET          = "Sunset";
    public static final String MOTIVE_SEA             = "Sea";
    public static final String MOTIVE_CREEBET         = "Creebet";
    public static final String MOTIVE_MATCH           = "Match";
    public static final String MOTIVE_BUST            = "Bust";
    public static final String MOTIVE_STAGE           = "Stage";
    public static final String MOTIVE_VOID            = "Void";
    public static final String MOTIVE_SKULL_AND_ROSES = "SkullAndRoses";
    public static final String MOTIVE_FIGHTERS        = "Fighters";
    public static final String MOTIVE_SKELETON        = "Skeleton";
    public static final String MOTIVE_DONKEY_KONG     = "DonkeyKong";
    public static final String MOTIVE_POINTER         = "Pointer";
    public static final String MOTIVE_PIGSCENE        = "Pigscene";
    public static final String MOTIVE_SKULL           = "Skull";
    
    /** 1x1 paintings */
    public static final String[] SMALL_PAINTINGS = {MOTIVE_KEBAB, MOTIVE_AZTEC, MOTIVE_ALBAN, MOTIVE_AZTEC2, MOTIVE_BOMB, MOTIVE_PLANT, MOTIVE_WASTELAND};

    /** 2x1 paintings */
    public static final String[] WIDE_PAINTINGS = {MOTIVE_POOL, MOTIVE_COURBET, MOTIVE_SUNSET, MOTIVE_SEA, MOTIVE_CREEBET};

    /** 1x2 paintings */
    public static final String[] TALL_PAINTINGS = {MOTIVE_WANDERER, MOTIVE_GRAHAM};

    /** 2x2 paintings */
    public static final String[] LARGE_PAINTINGS = {MOTIVE_MATCH, MOTIVE_BUST, MOTIVE_STAGE, MOTIVE_VOID, MOTIVE_SKULL_AND_ROSES};
    
    /**
     * A map from tile entity ID's to the corresponding block ID's.
     */
    public static final Map<String, Set<Integer>> TILE_ENTITY_MAP = new HashMap<>();
    
    static {
        TILE_ENTITY_MAP.put(ID_AIRPORTAL, Collections.singleton(BLK_END_PORTAL));
        TILE_ENTITY_MAP.put(ID_BEACON, Collections.singleton(BLK_BEACON));
        TILE_ENTITY_MAP.put(ID_CAULDRON, Collections.singleton(BLK_BREWING_STAND));
        TILE_ENTITY_MAP.put(ID_CHEST, Collections.unmodifiableSet(new HashSet<>(Arrays.asList(BLK_CHEST, BLK_TRAPPED_CHEST))));
        TILE_ENTITY_MAP.put(ID_COMPARATOR, Collections.unmodifiableSet(new HashSet<>(Arrays.asList(BLK_REDSTONE_COMPARATOR_UNPOWERED, BLK_REDSTONE_COMPARATOR_POWERED))));
        TILE_ENTITY_MAP.put(ID_CONTROL, Collections.unmodifiableSet(new HashSet<>(Arrays.asList(BLK_COMMAND_BLOCK, BLK_CHAIN_COMMAND_BLOCK, BLK_REPEATING_COMMAND_BLOCK))));
        TILE_ENTITY_MAP.put(ID_DLDETECTOR, Collections.unmodifiableSet(new HashSet<>(Arrays.asList(BLK_DAYLIGHT_SENSOR, BLK_DAYLIGHT_SENSOR_INVERTED))));
        TILE_ENTITY_MAP.put(ID_DROPPER, Collections.singleton(BLK_DROPPER));
        TILE_ENTITY_MAP.put(ID_ENCHANTTABLE, Collections.singleton(BLK_ENCHANTMENT_TABLE));
        TILE_ENTITY_MAP.put(ID_ENDERCHEST, Collections.singleton(BLK_ENDER_CHEST));
        TILE_ENTITY_MAP.put(ID_FLOWERPOT, Collections.singleton(BLK_FLOWER_POT));
        TILE_ENTITY_MAP.put(ID_FURNACE, Collections.unmodifiableSet(new HashSet<>(Arrays.asList(BLK_FURNACE, BLK_BURNING_FURNACE))));
        TILE_ENTITY_MAP.put(ID_HOPPER, Collections.singleton(BLK_HOPPER));
        TILE_ENTITY_MAP.put(ID_MOBSPAWNER, Collections.singleton(BLK_MONSTER_SPAWNER));
        TILE_ENTITY_MAP.put(ID_MUSIC, Collections.singleton(BLK_NOTE_BLOCK));
        TILE_ENTITY_MAP.put(ID_PISTON, Collections.singleton(BLK_PISTON_HEAD));
        TILE_ENTITY_MAP.put(ID_RECORDPLAYER, Collections.singleton(BLK_JUKEBOX));
        TILE_ENTITY_MAP.put(ID_SIGN, Collections.unmodifiableSet(new HashSet<>(Arrays.asList(BLK_SIGN, BLK_WALL_SIGN))));
        TILE_ENTITY_MAP.put(ID_SKULL, Collections.singleton(BLK_HEAD));
        TILE_ENTITY_MAP.put(ID_TRAP, Collections.singleton(BLK_DISPENSER));
        TILE_ENTITY_MAP.put(ID_BANNER, Collections.unmodifiableSet(new HashSet<>(Arrays.asList(BLK_STANDING_BANNER, BLK_WALL_BANNER))));

        // Make sure the tile entity flag in the block database is consistent
        // with the tile entity map:
        Set<Integer> allTileEntityIds = new HashSet<>();
        for (Set<Integer> blockIdSet: TILE_ENTITY_MAP.values()) {
            allTileEntityIds.addAll(blockIdSet);
            for (int blockId: blockIdSet) {
                if (! Block.BLOCKS[blockId].tileEntity) {
                    throw new AssertionError("Block " + blockId + " not marked as tile entity!");
                }
            }
        }
        for (Block block: Block.BLOCKS) {
            if (block.tileEntity && (! allTileEntityIds.contains(block.id))) {
                throw new AssertionError("Block " + block.id + " marked as tile entity but not present in tile entity map!");
            }
        }
    }
    
    public static final int DEFAULT_MAX_HEIGHT_1 = 128;
    public static final int DEFAULT_MAX_HEIGHT_2 = 256;
    
    public static final int DIFFICULTY_PEACEFUL = 0;
    public static final int DIFFICULTY_EASY     = 1;
    public static final int DIFFICULTY_NORMAL   = 2;
    public static final int DIFFICULTY_HARD     = 3;
}