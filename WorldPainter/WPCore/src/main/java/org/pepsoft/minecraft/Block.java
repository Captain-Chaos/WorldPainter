package org.pepsoft.minecraft;

import java.io.ObjectStreamException;
import java.io.Serializable;

import static org.pepsoft.minecraft.Constants.*;

/**
 * A database of Minecraft block information. Accessed by using the block ID as
 * index in the {@link #BLOCKS} array. Implements the Enumeration pattern,
 * meaning there is only ever one instance of this class for each block ID,
 * allowing use of the equals operator (==) for comparing instances.
 *
 * Created by pepijn on 17-3-15.
 */
public final class Block implements Serializable {
    private Block(int id, int transparency, String name, boolean terrain,
                 boolean insubstantial, boolean veryInsubstantial, boolean resource, boolean tileEntity, boolean treeRelated,
                 boolean vegetation, int blockLight, boolean natural) {
        this.id = id;
        this.transparency = transparency;
        this.name = name;
        this.transparent = (transparency == 0);
        this.translucent = (transparency < 15);
        this.opaque = (transparency == 15);
        this.terrain = terrain;
        this.insubstantial = insubstantial;
        this.veryInsubstantial = veryInsubstantial;
        this.solid = ! veryInsubstantial;
        this.resource = resource;
        this.tileEntity = tileEntity;
        this.treeRelated = treeRelated;
        this.vegetation = vegetation;
        this.blockLight = blockLight;
        this.lightSource = (blockLight > 0);
        this.natural = natural;

        // Sanity checks
        if ((id < 0) || (id > 4095)
                || (transparency < 0) || (transparency > 15)
                || (insubstantial && (! veryInsubstantial))
                || (blockLight < 0) || (blockLight > 15)
                || (treeRelated && vegetation)) {
            throw new IllegalArgumentException();
        }

        // Determine the category
        if (id == BLK_AIR) {
            category = CATEGORY_AIR;
        } else if ((id == BLK_WATER) || (id == BLK_STATIONARY_WATER) || (id == BLK_LAVA) || (id == BLK_STATIONARY_LAVA)) {
            category = CATEGORY_FLUID;
        } else if (veryInsubstantial) {
            category = CATEGORY_INSUBSTANTIAL;
        } else if (! natural) {
            category = CATEGORY_MAN_MADE;
        } else if (resource) {
            category = CATEGORY_RESOURCE;
        } else {
            category = CATEGORY_NATURAL_SOLID;
        }
    }

    @Override public boolean equals(Object o) {
        return (o instanceof Block) && (((Block) o).id == id);
    }

    @Override public int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        return (name != null) ? name : Integer.toString(id);
    }

    /**
     * The block ID.
     */
    public final int id;

    /**
     * How much light the block blocks.
     */
    public final transient int transparency;

    /**
     * The name of the block.
     */
    public final transient String name;

    /**
     * Whether the block is fully transparent ({@link #transparency} == 0)
     */
    public final transient boolean transparent;

    /**
     * Whether the block is translucent ({@link #transparency} < 15)
     */
    public final transient boolean translucent;

    /**
     * Whether the block is fully opaque ({@link #transparency} == 15)
     */
    public final transient boolean opaque;

    /**
     * Whether the block is part of Minecraft-generated natural ground.
     */
    public final transient boolean terrain;

    /**
     * Whether the block is insubstantial, meaning that they are fully
     * transparent, not man-made, removing them would have no effect on the
     * surrounding blocks and be otherwise inconsequential. In other words
     * mostly decorative blocks that users presumably would not mind being
     * removed.
     */
    public final transient boolean insubstantial;

    /**
     * Whether the block is even more insubstantial. Implies
     * {@link #insubstantial} and adds air, water, lava and leaves.
     */
    public final transient boolean veryInsubstantial;

    /**
     * Whether the block is solid (meaning not {@link #insubstantial} or
     * {@link #veryInsubstantial}).
     */
    public final transient boolean solid;

    /**
     * Whether the block is a mineable ore or resource.
     */
    public final transient boolean resource;

    /**
     * Whether the block is a tile entity.
     */
    public final transient boolean tileEntity;

    /**
     * Whether the block is part of or attached to naturally occurring
     * trees or giant mushrooms. Also includes saplings, but not normal
     * mushrooms.
     */
    public final transient boolean treeRelated;

    /**
     * Whether the block is a plant. Excludes {@link #treeRelated} blocks.
     */
    public final transient boolean vegetation;

    /**
     * The amount of blocklight emitted by this block.
     */
    public final transient int blockLight;

    /**
     * Whether the block is a source of blocklight ({@link #blockLight} > 0).
     */
    public final transient boolean lightSource;

    /**
     * Whether the block can occur as part of a pristine Minecraft-generated
     * landscape, <em>excluding</em> artificial structures such as abandoned
     * mineshafts, villages, temples, strongholds, etc.
     */
    public final transient boolean natural;

    /**
     * Type of block encoded in a single category
     */
    public final transient int category;

    private Object readResolve() throws ObjectStreamException {
        return BLOCKS[id];
    }

    public static final Block[] BLOCKS = new Block[4096];

    static {
        System.arraycopy(new Block[] {
//                         ID, Tr,                              Name, Terra, Insub, VryIn, Resou, TileE, TreeR, Veget, Li, Natural
                new Block(  0,  0,                             "Air", false, false,  true, false, false, false, false,  0,  true),
                new Block(  1, 15,                           "Stone",  true, false, false, false, false, false, false,  0,  true),
                new Block(  2, 15,                           "Grass",  true, false, false, false, false, false, false,  0,  true),
                new Block(  3, 15,                            "Dirt",  true, false, false, false, false, false, false,  0,  true),
                new Block(  4, 15,                     "Cobblestone", false, false, false, false, false, false, false,  0, false),
                new Block(  5, 15,                    "Wooden Plank", false, false, false, false, false, false, false,  0, false),
                new Block(  6,  0,                         "Sapling", false,  true,  true, false, false,  true, false,  0, false),
                new Block(  7, 15,                         "Bedrock",  true, false, false, false, false, false, false,  0,  true),
                new Block(  8,  3,                           "Water", false, false,  true, false, false, false, false,  0,  true),
                new Block(  9,  3,                "Stationary Water", false, false,  true, false, false, false, false,  0,  true),
//                         ID, Tr,                              Name, Terra, Insub, VryIn, Resou, TileE, TreeR, Veget, Li, Natural
                new Block( 10,  0,                            "Lava", false, false,  true, false, false, false, false, 15,  true),
                new Block( 11,  0,                 "Stationary Lava", false, false,  true, false, false, false, false, 15,  true),
                new Block( 12, 15,                            "Sand",  true, false, false, false, false, false, false,  0,  true),
                new Block( 13, 15,                          "Gravel",  true, false, false, false, false, false, false,  0,  true),
                new Block( 14, 15,                        "Gold Ore",  true, false, false,  true, false, false, false,  0,  true),
                new Block( 15, 15,                        "Iron Ore",  true, false, false,  true, false, false, false,  0,  true),
                new Block( 16, 15,                        "Coal Ore",  true, false, false,  true, false, false, false,  0,  true),
                new Block( 17, 15,                            "Wood", false, false, false, false, false,  true, false,  0,  true),
                new Block( 18,  1,                          "Leaves", false, false,  true, false, false,  true, false,  0,  true),
                new Block( 19, 15,                          "Sponge", false, false, false, false, false, false, false,  0, false),
//                         ID, Tr,                              Name, Terra, Insub, VryIn, Resou, TileE, TreeR, Veget, Li, Natural
                new Block( 20,  0,                           "Glass", false, false, false, false, false, false, false,  0, false),
                new Block( 21, 15,                "Lapis Lazuli Ore",  true, false, false,  true, false, false, false,  0,  true),
                new Block( 22, 15,              "Lapis Lazuli Block", false, false, false, false, false, false, false,  0, false),
                new Block( 23, 15,                       "Dispenser", false, false, false, false,  true, false, false,  0, false),
                new Block( 24, 15,                       "Sandstone",  true, false, false, false, false, false, false,  0,  true),
                new Block( 25, 15,                      "Note Block", false, false, false, false,  true, false, false,  0, false),
                new Block( 26,  0,                             "Bed", false, false, false, false, false, false, false,  0, false),
                new Block( 27,  0,                    "Powered Rail", false, false, false, false, false, false, false,  0, false),
                new Block( 28,  0,                   "Detector Rail", false, false, false, false, false, false, false,  0, false),
                new Block( 29, 15,                   "Sticky Piston", false, false, false, false, false, false, false,  0, false),
//                         ID, Tr,                              Name, Terra, Insub, VryIn, Resou, TileE, TreeR, Veget, Li, Natural
                new Block( 30,  0,                          "Cobweb", false,  true,  true, false, false, false, false,  0, false),
                new Block( 31,  0,                      "Tall Grass", false,  true,  true, false, false, false,  true,  0,  true),
                new Block( 32,  0,                       "Dead Bush", false,  true,  true, false, false, false,  true,  0,  true),
                new Block( 33,  0,                          "Piston", false, false, false, false, false, false, false,  0, false),
                new Block( 34,  0,                "Piston Extension", false, false, false, false,  true, false, false,  0, false),
                new Block( 35, 15,                            "Wool", false, false, false, false, false, false, false,  0, false),
                new Block( 36, 15,                              null, false, false, false, false, false, false, false,  0, false),
                new Block( 37,  0,                       "Dandelion", false,  true,  true, false, false, false,  true,  0,  true),
                new Block( 38,  0,                          "Flower", false,  true,  true, false, false, false,  true,  0,  true),
                new Block( 39,  0,                  "Brown Mushroom", false,  true,  true, false, false, false,  true,  1,  true),
//                         ID, Tr,                              Name, Terra, Insub, VryIn, Resou, TileE, TreeR, Veget, Li, Natural
                new Block( 40,  0,                    "Red Mushroom", false,  true,  true, false, false, false,  true,  0,  true),
                new Block( 41, 15,                      "Gold Block", false, false, false, false, false, false, false,  0, false),
                new Block( 42, 15,                      "Iron Block", false, false, false, false, false, false, false,  0, false),
                new Block( 43, 15,                    "Double Slabs", false, false, false, false, false, false, false,  0, false),
                new Block( 44, 15,                            "Slab", false, false, false, false, false, false, false,  0, false),
                new Block( 45, 15,                     "Brick Block", false, false, false, false, false, false, false,  0, false),
                new Block( 46, 15,                             "TNT", false, false, false, false, false, false, false,  0, false),
                new Block( 47, 15,                       "Bookshelf", false, false, false, false, false, false, false,  0, false),
                new Block( 48, 15,               "Mossy Cobblestone", false, false, false, false, false, false, false,  0, false),
                new Block( 49, 15,                        "Obsidian",  true, false, false, false, false, false, false,  0,  true),
//                         ID, Tr,                              Name, Terra, Insub, VryIn, Resou, TileE, TreeR, Veget, Li, Natural
                new Block( 50,  0,                           "Torch", false, false, false, false, false, false, false, 14, false),
                new Block( 51,  0,                            "Fire", false,  true,  true, false, false, false, false, 15,  true),
                new Block( 52, 15,                 "Monster Spawner", false, false, false, false,  true, false, false,  0, false),
                new Block( 53, 15,                   "Wooden Stairs", false, false, false, false, false, false, false,  0, false),
                new Block( 54,  0,                           "Chest", false, false, false, false,  true, false, false,  0, false),
                new Block( 55,  0,                   "Redstone Wire", false, false, false, false, false, false, false,  0, false),
                new Block( 56, 15,                     "Diamond Ore",  true, false, false,  true, false, false, false,  0,  true),
                new Block( 57, 15,                   "Diamond Block", false, false, false, false, false, false, false,  0, false),
                new Block( 58, 15,                  "Crafting Table", false, false, false, false, false, false, false,  0, false),
                new Block( 59,  0,                           "Wheat", false,  true,  true, false, false, false,  true,  0, false),
//                         ID, Tr,                              Name, Terra, Insub, VryIn, Resou, TileE, TreeR, Veget, Li, Natural
                new Block( 60, 15,                     "Tilled Dirt",  true, false, false, false, false, false, false,  0, false),
                new Block( 61, 15,                         "Furnace", false, false, false, false,  true, false, false,  0, false),
                new Block( 62, 15,                 "Burning Furnace", false, false, false, false,  true, false, false, 13, false),
                new Block( 63,  0,                       "Sign Post", false, false, false, false,  true, false, false,  0, false),
                new Block( 64,  0,                     "Wooden Door", false, false, false, false, false, false, false,  0, false),
                new Block( 65,  0,                          "Ladder", false, false, false, false, false, false, false,  0, false),
                new Block( 66,  0,                           "Rails", false, false, false, false, false, false, false,  0, false),
                new Block( 67, 15,              "Cobblestone Stairs", false, false, false, false, false, false, false,  0, false),
                new Block( 68,  0,                       "Wall Sign", false, false, false, false,  true, false, false,  0, false),
                new Block( 69,  0,                           "Lever", false, false, false, false, false, false, false,  0, false),
//                         ID, Tr,                              Name, Terra, Insub, VryIn, Resou, TileE, TreeR, Veget, Li, Natural
                new Block( 70,  0,            "Stone Pressure Plate", false, false, false, false, false, false, false,  0, false),
                new Block( 71,  0,                       "Iron Door", false, false, false, false, false, false, false,  0, false),
                new Block( 72,  0,           "Wooden Pressure Plate", false, false, false, false, false, false, false,  0, false),
                new Block( 73, 15,                    "Redstone Ore",  true, false, false,  true, false, false, false,  0,  true),
                new Block( 74, 15,            "Glowing Redstone Ore",  true, false, false, false, false, false, false,  9,  true),
                new Block( 75,  0,            "Redstone Torch (off)", false, false, false, false, false, false, false,  0, false),
                new Block( 76,  0,             "Redstone Torch (on)", false, false, false, false, false, false, false,  7, false),
                new Block( 77,  0,                    "Stone Button", false, false, false, false, false, false, false,  0, false),
                new Block( 78,  0,                            "Snow", false,  true,  true, false, false, false, false,  0,  true),
                new Block( 79,  3,                             "Ice", false, false, false, false, false, false, false,  0,  true),
//                         ID, Tr,                              Name, Terra, Insub, VryIn, Resou, TileE, TreeR, Veget, Li, Natural
                new Block( 80, 15,                      "Snow Block",  true, false, false, false, false, false, false,  0,  true),
                new Block( 81,  0,                          "Cactus", false,  true,  true, false, false, false,  true,  0,  true),
                new Block( 82, 15,                      "Clay Block",  true, false, false, false, false, false, false,  0,  true),
                new Block( 83,  0,                      "Sugar Cane", false,  true,  true, false, false, false,  true,  0,  true),
                new Block( 84, 15,                         "Jukebox", false, false, false, false,  true, false, false,  0, false),
                new Block( 85,  0,                           "Fence", false, false, false, false, false, false, false,  0, false),
                new Block( 86,  0,                         "Pumpkin", false,  true,  true, false, false, false,  true,  0,  true),
                new Block( 87, 15,                      "Netherrack",  true, false, false, false, false, false, false,  0,  true),
                new Block( 88, 15,                       "Soul Sand",  true, false, false, false, false, false, false,  0,  true),
                new Block( 89, 15,                 "Glowstone Block", false, false, false, false, false, false, false, 15,  true),
//                         ID, Tr,                              Name, Terra, Insub, VryIn, Resou, TileE, TreeR, Veget, Li, Natural
                new Block( 90,  0,                          "Portal", false, false, false, false, false, false, false, 11, false),
                new Block( 91, 15,                  "Jack-O-Lantern", false, false, false, false, false, false, false, 15, false),
                new Block( 92,  0,                            "Cake", false, false, false, false, false, false, false,  0, false),
                new Block( 93,  0,         "Redstone Repeater (off)", false, false, false, false, false, false, false,  0, false),
                new Block( 94,  0,          "Redstone Repeater (on)", false, false, false, false, false, false, false,  9, false),
                new Block( 95, 15,                   "Stained Glass", false, false, false, false, false, false, false,  0, false),
                new Block( 96,  0,                        "Trapdoor", false, false, false, false, false, false, false,  0, false),
                new Block( 97, 15,               "Hidden Silverfish",  true, false, false, false, false, false, false,  0,  true),
                new Block( 98, 15,                    "Stone Bricks", false, false, false, false, false, false, false,  0, false),
                new Block( 99, 15,             "Huge Brown Mushroom", false, false, false, false, false,  true, false,  0,  true),
//                         ID, Tr,                              Name, Terra, Insub, VryIn, Resou, TileE, TreeR, Veget, Li, Natural
                new Block(100, 15,               "Huge Red Mushroom", false, false, false, false, false,  true, false,  0,  true),
                new Block(101,  0,                       "Iron Bars", false, false, false, false, false, false, false,  0, false),
                new Block(102,  0,                      "Glass Pane", false, false, false, false, false, false, false,  0, false),
                new Block(103, 15,                           "Melon", false,  true,  true, false, false, false,  true,  0, false),
                new Block(104,  0,                    "Pumpkin Stem", false,  true,  true, false, false, false,  true,  0, false),
                new Block(105,  0,                      "Melon Stem", false,  true,  true, false, false, false,  true,  0, false),
                new Block(106,  0,                           "Vines", false,  true,  true, false, false,  true, false,  0,  true),
                new Block(107,  0,                      "Fence Gate", false, false, false, false, false, false, false,  0, false),
                new Block(108, 15,                    "Brick Stairs", false, false, false, false, false, false, false,  0, false),
                new Block(109, 15,              "Stone Brick Stairs", false, false, false, false, false, false, false,  0, false),
//                         ID, Tr,                              Name, Terra, Insub, VryIn, Resou, TileE, TreeR, Veget, Li, Natural
                new Block(110, 15,                        "Mycelium",  true, false, false, false, false, false, false,  0,  true),
                new Block(111,  0,                        "Lily Pad", false,  true,  true, false, false, false,  true,  0,  true),
                new Block(112, 15,                    "Nether Brick", false, false, false, false, false, false, false,  0, false),
                new Block(113,  0,              "Nether Brick Fence", false, false, false, false, false, false, false,  0, false),
                new Block(114, 15,             "Nether Brick Stairs", false, false, false, false, false, false, false,  0, false),
                new Block(115,  0,                     "Nether Wart", false,  true,  true, false, false, false,  true,  0,  true),
                new Block(116,  0,               "Enchantment Table", false, false, false, false,  true, false, false,  0, false),
                new Block(117,  0,                   "Brewing Stand", false, false, false, false,  true, false, false,  1, false),
                new Block(118,  0,                        "Cauldron", false, false, false, false, false, false, false,  0, false),
                new Block(119,  0,                      "End Portal", false, false, false, false,  true, false, false, 15, false),
//                         ID, Tr,                              Name, Terra, Insub, VryIn, Resou, TileE, TreeR, Veget, Li, Natural
                new Block(120, 15,                "End Portal Frame", false, false, false, false, false, false, false,  1, false),
                new Block(121, 15,                       "End Stone",  true, false, false, false, false, false, false,  0,  true),
                new Block(122,  0,                      "Dragon Egg", false, false, false, false, false, false, false,  1, false),
                new Block(123, 15,             "Redstone Lamp (off)", false, false, false, false, false, false, false,  0, false),
                new Block(124, 15,              "Redstone Lamp (on)", false, false, false, false, false, false, false, 15, false),
                new Block(125, 15,              "Wooden Double Slab", false, false, false, false, false, false, false,  0, false),
                new Block(126, 15,                     "Wooden Slab", false, false, false, false, false, false, false,  0, false),
                new Block(127,  0,                     "Cocoa Plant", false,  true,  true, false, false,  true, false,  0,  true),
                new Block(128, 15,                "Sandstone Stairs", false, false, false, false, false, false, false,  0, false),
                new Block(129, 15,                     "Emerald Ore", false, false, false,  true, false, false, false,  0,  true),
//                         ID, Tr,                              Name, Terra, Insub, VryIn, Resou, TileE, TreeR, Veget, Li, Natural
                new Block(130,  0,                     "Ender Chest", false, false, false, false,  true, false, false,  7, false),
                new Block(131,  0,                   "Tripwire Hook", false, false, false, false, false, false, false,  0, false),
                new Block(132,  0,                        "Tripwire", false, false, false, false, false, false, false,  0, false),
                new Block(133, 15,                   "Emerald Block", false, false, false, false, false, false, false,  0, false),
                new Block(134, 15,                "Pine Wood Stairs", false, false, false, false, false, false, false,  0, false),
                new Block(135, 15,               "Birch Wood Stairs", false, false, false, false, false, false, false,  0, false),
                new Block(136, 15,              "Jungle Wood Stairs", false, false, false, false, false, false, false,  0, false),
                new Block(137, 15,                   "Command Block", false, false, false, false,  true, false, false,  0, false),
                new Block(138, 15,                          "Beacon", false, false, false, false,  true, false, false, 15, false),
                new Block(139,  0,                "Cobblestone Wall", false, false, false, false, false, false, false,  0, false),
//                         ID, Tr,                              Name, Terra, Insub, VryIn, Resou, TileE, TreeR, Veget, Li, Natural
                new Block(140,  0,                      "Flower Pot", false, false, false, false,  true, false, false,  0, false),
                new Block(141,  0,                         "Carrots", false,  true,  true, false, false, false,  true,  0, false),
                new Block(142,  0,                        "Potatoes", false,  true,  true, false, false, false,  true,  0, false),
                new Block(143,  0,                   "Wooden Button", false, false, false, false, false, false, false,  0, false),
                new Block(144,  0,                            "Head", false, false, false, false,  true, false, false,  0, false),
                new Block(145, 15,                           "Anvil", false, false, false, false, false, false, false,  0, false),
                new Block(146,  0,                   "Trapped Chest", false, false, false, false,  true, false, false,  0, false),
                new Block(147,  0, "Weighted Pressure Plate (light)", false, false, false, false, false, false, false,  0, false),
                new Block(148,  0, "Weighted Pressure Plate (heavy)", false, false, false, false, false, false, false,  0, false),
                new Block(149,  0, "Redstone Comparator (unpowered)", false, false, false, false,  true, false, false,  0, false),
//                         ID, Tr,                              Name, Terra, Insub, VryIn, Resou, TileE, TreeR, Veget, Li, Natural
                new Block(150,  0,   "Redstone Comparator (powered)", false, false, false, false,  true, false, false,  0, false),
                new Block(151,  0,                 "Daylight Sensor", false, false, false, false,  true, false, false,  0, false),
                new Block(152, 15,                  "Redstone Block", false, false, false, false, false, false, false,  0, false),
                new Block(153, 15,               "Nether Quartz Ore",  true, false, false,  true, false, false, false,  0,  true),
                new Block(154,  0,                          "Hopper", false, false, false, false,  true, false, false,  0, false),
                new Block(155, 15,                    "Quartz Block", false, false, false, false, false, false, false,  0, false),
                new Block(156, 15,                   "Quartz Stairs", false, false, false, false, false, false, false,  0, false),
                new Block(157,  0,                  "Activator Rail", false, false, false, false, false, false, false,  0, false),
                new Block(158,  0,                         "Dropper", false, false, false, false,  true, false, false,  0, false),
                new Block(159, 15,                    "Stained Clay",  true, false, false, false, false, false, false,  0,  true),
//                         ID, Tr,                              Name, Terra, Insub, VryIn, Resou, TileE, TreeR, Veget, Li, Natural
                new Block(160,  0,              "Stained Glass Pane", false, false, false, false, false, false, false,  0, false),
                new Block(161,  1,                        "Leaves 2", false, false,  true, false, false,  true, false,  0,  true),
                new Block(162, 15,                          "Wood 2", false, false, false, false, false,  true, false,  0,  true),
                new Block(163, 15,              "Acacia Wood Stairs", false, false, false, false, false, false, false,  0, false),
                new Block(164, 15,            "Dark Oak Wood Stairs", false, false, false, false, false, false, false,  0, false),
                new Block(165,  0,                     "Slime Block", false, false, false, false, false, false, false,  0, false),
                new Block(166,  0,                         "Barrier", false, false, false, false, false, false, false,  0, false),
                new Block(167,  0,                   "Iron Trapdoor", false, false, false, false, false, false, false,  0, false),
                new Block(168, 15,                      "Prismarine", false, false, false, false, false, false, false,  0, false),
                new Block(169, 15,                     "Sea Lantern", false, false, false, false, false, false, false, 15, false),
//                         ID, Tr,                              Name, Terra, Insub, VryIn, Resou, TileE, TreeR, Veget, Li, Natural
                new Block(170, 15,                        "Hay Bale", false, false, false, false, false, false, false,  0, false),
                new Block(171,  0,                          "Carpet", false, false, false, false, false, false, false,  0, false),
                new Block(172, 15,                   "Hardened Clay",  true, false, false, false, false, false, false,  0,  true),
                new Block(173, 15,                      "Coal Block", false, false, false, false, false, false, false,  0, false),
                new Block(174, 15,                      "Packed Ice", false, false, false, false, false, false, false,  0,  true),
                new Block(175,  0,                    "Large Flower", false,  true,  true, false, false, false,  true,  0,  true),
                new Block(176,  0,                 "Standing Banner", false, false, false, false,  true, false, false,  0, false),
                new Block(177,  0,                     "Wall Banner", false, false, false, false,  true, false, false,  0, false),
                new Block(178,  0,        "Inverted Daylight Sensor", false, false, false, false,  true, false, false,  0, false),
                new Block(179, 15,                   "Red Sandstone",  true, false, false, false, false, false, false,  0,  true),
//                         ID, Tr,                              Name, Terra, Insub, VryIn, Resou, TileE, TreeR, Veget, Li, Natural
                new Block(180, 15,            "Red Sandstone Stairs", false, false, false, false, false, false, false,  0, false),
                new Block(181, 15,       "Double Red Sandstone Slab", false, false, false, false, false, false, false,  0, false),
                new Block(182, 15,              "Red Sandstone Slab", false, false, false, false, false, false, false,  0, false),
                new Block(183,  0,            "Pine Wood Fence Gate", false, false, false, false, false, false, false,  0, false),
                new Block(184,  0,           "Birch Wood Fence Gate", false, false, false, false, false, false, false,  0, false),
                new Block(185,  0,          "Jungle Wood Fence Gate", false, false, false, false, false, false, false,  0, false),
                new Block(186,  0,        "Dark Oak Wood Fence Gate", false, false, false, false, false, false, false,  0, false),
                new Block(187,  0,          "Acacia Wood Fence Gate", false, false, false, false, false, false, false,  0, false),
                new Block(188,  0,                 "Pine Wood Fence", false, false, false, false, false, false, false,  0, false),
                new Block(189,  0,                "Birch Wood Fence", false, false, false, false, false, false, false,  0, false),
//                         ID, Tr,                              Name, Terra, Insub, VryIn, Resou, TileE, TreeR, Veget, Li, Natural
                new Block(190,  0,               "Jungle Wood Fence", false, false, false, false, false, false, false,  0, false),
                new Block(191,  0,             "Dark Oak Wood Fence", false, false, false, false, false, false, false,  0, false),
                new Block(192,  0,               "Acacia Wood Fence", false, false, false, false, false, false, false,  0, false),
                new Block(193,  0,                  "Pine Wood Door", false, false, false, false, false, false, false,  0, false),
                new Block(194,  0,                 "Birch Wood Door", false, false, false, false, false, false, false,  0, false),
                new Block(195,  0,                "Jungle Wood Door", false, false, false, false, false, false, false,  0, false),
                new Block(196,  0,                "Acacia Wood Door", false, false, false, false, false, false, false,  0, false),
                new Block(197,  0,              "Dark Oak Wood Door", false, false, false, false, false, false, false,  0, false),
                new Block(198,  0,                         "End Rod", false, false, false, false, false, false, false, 14, false),
                new Block(199,  0,                    "Chorus Plant", false, true,  true,  false, false, false, true,   0,  true),
//                         ID, Tr,                              Name, Terra, Insub, VryIn, Resou, TileE, TreeR, Veget, Li, Natural
                new Block(200,  0,                   "Chorus Flower", false, true,  true,  false, false, false, true,   0,  true),
                new Block(201, 15,                    "Purpur Block", false, false, false, false, false, false, false,  0, false),
                new Block(202, 15,                   "Purpur Pillar", false, false, false, false, false, false, false,  0, false),
                new Block(203, 15,                   "Purpur Stairs", false, false, false, false, false, false, false,  0, false),
                new Block(204, 15,              "Double Purpur Slab", false, false, false, false, false, false, false,  0, false),
                new Block(205, 15,                     "Purpur Slab", false, false, false, false, false, false, false,  0, false),
                new Block(206, 15,                "End Stone Bricks", false, false, false, false, false, false, false,  0, false),
                new Block(207,  0,                       "Beetroots", false, true,  true,  false, false, false, true,   0, false),
                new Block(208, 15,                      "Grass Path", true,  false, false, false, false, false, false,  0, false),
                new Block(209, 15,                     "End Gateway", false, false, false, false, false, false, false,  0, false),
//                         ID, Tr,                              Name, Terra, Insub, VryIn, Resou, TileE, TreeR, Veget, Li, Natural
                new Block(210, 15,         "Repeating Command Block", false, false, false, false, true,  false, false,  0, false),
                new Block(211, 15,             "Chain Command Block", false, false, false, false, true,  false, false,  0, false),
                new Block(212,  3,                     "Frosted Ice", false, false, false, false, false, false, false,  0, false),
                new Block(213, 15,                              null, false, false, false, false, false, false, false,  0, false),
                new Block(214, 15,                              null, false, false, false, false, false, false, false,  0, false),
                new Block(215, 15,                              null, false, false, false, false, false, false, false,  0, false),
                new Block(216, 15,                              null, false, false, false, false, false, false, false,  0, false),
                new Block(217, 15,                              null, false, false, false, false, false, false, false,  0, false),
                new Block(218, 15,                              null, false, false, false, false, false, false, false,  0, false),
                new Block(219, 15,                              null, false, false, false, false, false, false, false,  0, false),
//                         ID, Tr,                              Name, Terra, Insub, VryIn, Resou, TileE, TreeR, Veget, Li, Natural
                new Block(220, 15,                              null, false, false, false, false, false, false, false,  0, false),
                new Block(221, 15,                              null, false, false, false, false, false, false, false,  0, false),
                new Block(222, 15,                              null, false, false, false, false, false, false, false,  0, false),
                new Block(223, 15,                              null, false, false, false, false, false, false, false,  0, false),
                new Block(224, 15,                              null, false, false, false, false, false, false, false,  0, false),
                new Block(225, 15,                              null, false, false, false, false, false, false, false,  0, false),
                new Block(226, 15,                              null, false, false, false, false, false, false, false,  0, false),
                new Block(227, 15,                              null, false, false, false, false, false, false, false,  0, false),
                new Block(228, 15,                              null, false, false, false, false, false, false, false,  0, false),
                new Block(229, 15,                              null, false, false, false, false, false, false, false,  0, false),
//                         ID, Tr,                              Name, Terra, Insub, VryIn, Resou, TileE, TreeR, Veget, Li, Natural
                new Block(230, 15,                              null, false, false, false, false, false, false, false,  0, false),
                new Block(231, 15,                              null, false, false, false, false, false, false, false,  0, false),
                new Block(232, 15,                              null, false, false, false, false, false, false, false,  0, false),
                new Block(233, 15,                              null, false, false, false, false, false, false, false,  0, false),
                new Block(234, 15,                              null, false, false, false, false, false, false, false,  0, false),
                new Block(235, 15,                              null, false, false, false, false, false, false, false,  0, false),
                new Block(236, 15,                              null, false, false, false, false, false, false, false,  0, false),
                new Block(237, 15,                              null, false, false, false, false, false, false, false,  0, false),
                new Block(238, 15,                              null, false, false, false, false, false, false, false,  0, false),
                new Block(239, 15,                              null, false, false, false, false, false, false, false,  0, false),
//                         ID, Tr,                              Name, Terra, Insub, VryIn, Resou, TileE, TreeR, Veget, Li, Natural
                new Block(240, 15,                              null, false, false, false, false, false, false, false,  0, false),
                new Block(241, 15,                              null, false, false, false, false, false, false, false,  0, false),
                new Block(242, 15,                              null, false, false, false, false, false, false, false,  0, false),
                new Block(243, 15,                              null, false, false, false, false, false, false, false,  0, false),
                new Block(244, 15,                              null, false, false, false, false, false, false, false,  0, false),
                new Block(245, 15,                              null, false, false, false, false, false, false, false,  0, false),
                new Block(246, 15,                              null, false, false, false, false, false, false, false,  0, false),
                new Block(247, 15,                              null, false, false, false, false, false, false, false,  0, false),
                new Block(248, 15,                              null, false, false, false, false, false, false, false,  0, false),
                new Block(249, 15,                              null, false, false, false, false, false, false, false,  0, false),
//                         ID, Tr,                              Name, Terra, Insub, VryIn, Resou, TileE, TreeR, Veget, Li, Natural
                new Block(250, 15,                              null, false, false, false, false, false, false, false,  0, false),
                new Block(251, 15,                              null, false, false, false, false, false, false, false,  0, false),
                new Block(252, 15,                              null, false, false, false, false, false, false, false,  0, false),
                new Block(253, 15,                              null, false, false, false, false, false, false, false,  0, false),
                new Block(254, 15,                              null, false, false, false, false, false, false, false,  0, false),
                new Block(255, 15,                              null, false, false, false, false, false, false, false,  0, false),
//                         ID, Tr,                              Name, Terra, Insub, VryIn, Resou, TileE, TreeR, Veget, Li, Natural
        }, 0, BLOCKS, 0, 256);

        for (int i = 256; i < 4096; i++) {
            BLOCKS[i] = new Block(i, 15, null, false, false, false, false, false, false, false, 0, false);
        }
    }

    public static final String[] BLOCK_TYPE_NAMES = new String[HIGHEST_KNOWN_BLOCK_ID + 1];
    public static final int[] BLOCK_TRANSPARENCY = new int[256];
    public static final int[] LIGHT_SOURCES = new int[256];

    static {
        for (int i = 0; i < 256; i++) {
            if (i <= HIGHEST_KNOWN_BLOCK_ID) {
                BLOCK_TYPE_NAMES[i] = BLOCKS[i].name;
            }
            BLOCK_TRANSPARENCY[i] = BLOCKS[i].transparency;
            LIGHT_SOURCES[i] = BLOCKS[i].blockLight;
        }
    }

    public static final int CATEGORY_AIR           = 0;
    public static final int CATEGORY_FLUID         = 1;
    public static final int CATEGORY_INSUBSTANTIAL = 2;
    public static final int CATEGORY_MAN_MADE      = 3;
    public static final int CATEGORY_RESOURCE      = 4;
    public static final int CATEGORY_NATURAL_SOLID = 5;

    private static final long serialVersionUID = 3037884633022467720L;
}