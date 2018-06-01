/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.minecraft;

import com.google.common.collect.ImmutableMap;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.List;

import static org.pepsoft.minecraft.Block.BLOCK_TYPE_NAMES;
import static org.pepsoft.minecraft.Constants.*;

/**
 * A representation of a Minecraft material, or one possible in- game block
 * type. Implements the Enumeration pattern, meaning there is only ever one
 * instance of this class for each block type (including properties), allowing
 * use of the equals operator (==) for comparing instances.
 *
 * <p>Supports modern post-Minecraft 1.13 materials based on names and
 * properties, as well as legacy pre-1.13 materials based on numerical IDs, and
 * provides continuity between them.
 *
 * @author pepijn
 */
@SuppressWarnings({"PointlessBitwiseExpression", "deprecation"}) // legibility; only deprecated for client code
public final class Material implements Serializable {
    /**
     * Legacy constructor, which creates a pre-Minecraft 1.13 block type from a
     * block ID and data value.
     *
     * @param blockType The block ID of the legacy block for which to create a
     *                  material.
     * @param data The data value of the legacy block for which to create a
     *             material.
     */
    @SuppressWarnings("unchecked") // Guaranteed by contents of file
    private Material(int blockType, int data) {
        this.blockType = blockType;
        this.data = data;
        index = (blockType << 4) | data;

        // TODOMC13: migrate this information to this class
        Block block = Block.BLOCKS[blockType];
        transparency = block.transparency;
        transparent = block.transparent;
        translucent = block.translucent;
        opaque = block.opaque;
        terrain = block.terrain;
        insubstantial = block.insubstantial;
        veryInsubstantial = block.veryInsubstantial;
        solid = block.solid;
        resource = block.resource;
        tileEntity = block.tileEntity;
        treeRelated = block.treeRelated;
        vegetation = block.vegetation;
        blockLight = block.blockLight;
        lightSource = block.lightSource;
        natural = block.natural;
        category = block.category;

        Map<String, Object> blockSpec = BLOCK_SPECS.get(index);
        if (blockSpec != null) {
            String name = ((String) blockSpec.get("name")).intern();
            int p = name.indexOf(':');
            if (p != -1) {
                namespace = name.substring(0, p);
                simpleName = name.substring(p + 1);
            } else {
                namespace = null;
                simpleName = name;
            }
            Map<String, String> properties;
            if (blockSpec.containsKey("properties")) {
                properties = new HashMap<>();
                for (Map.Entry<String, String> entry: ((Map<String, String>) blockSpec.get("properties")).entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    properties.put(key, value);
                }
            } else {
                properties = null;
            }
            identity = new Identity(name, properties);
        } else {
            namespace = "legacy";
            simpleName = "block_" + blockType;
            identity = new Identity(namespace + ":" + simpleName, Collections.singletonMap("data_value", Integer.toString(data)));
        }
        name = identity.name;
        stringRep = createStringRep();
        legacyStringRep = createLegacyStringRep();
        if (namespace != null) {
            ALL_NAMESPACES.add(namespace);
            SIMPLE_NAMES_BY_NAMESPACE.computeIfAbsent(namespace, name -> new HashSet<>(Collections.singleton(name))).add(simpleName);
        }
        if (! DEFAULT_MATERIALS_BY_NAME.containsKey(identity.name)) {
            DEFAULT_MATERIALS_BY_NAME.put(identity.name, this);
        }
    }

    /**
     * Creates a new material based on a block name and optionally one or more
     * properties in the form of an {@link Identity} object.
     *
     * @param identity The identity of the material to create.
     */
    private Material(Identity identity) {
        blockType = -1;
        data = -1;
        index = -1;

        // Use reasonable defaults for unknown blocks
        // TODOMC13: don't guess at this information, if possible
        transparency = 0;
        transparent = true;
        translucent = true;
        opaque = false;
        terrain = false;
        insubstantial = false;
        veryInsubstantial = false;
        solid = true;
        resource = false;
        tileEntity = false;
        treeRelated = false;
        vegetation = false;
        blockLight = 0;
        lightSource = false;
        natural = false;
        category = CATEGORY_UNKNOWN;

        this.identity = identity;
        name = identity.name;
        int p = name.indexOf(':');
        if (p != -1) {
            namespace = name.substring(0, p);
            simpleName = name.substring(p + 1);
        } else {
            namespace = null;
            simpleName = name;
        }
        stringRep = createStringRep();
        legacyStringRep = createLegacyStringRep();
        ALL_NAMESPACES.add(namespace);
        Set<String> simpleNames = SIMPLE_NAMES_BY_NAMESPACE.computeIfAbsent(namespace, name -> new HashSet<>(Collections.singleton(name)));
        simpleNames.add(simpleName);
        if (! DEFAULT_MATERIALS_BY_NAME.containsKey(name)) {
            DEFAULT_MATERIALS_BY_NAME.put(name, this);
        }
    }

    /**
     * Get the properties of this material.
     *
     * @return The properties of this material. May be <code>null</code>.
     */
    public Map<String, String> getProperties() {
        return identity.properties;
    }

    /**
     * Indicates whether a specific property is present on this material.
     *
     * @param property The property to check for presence.
     * @return <code>true</code> if the specified property is present.
     */
    public boolean hasProperty(Property<?> property) {
        return (identity.properties != null) && identity.properties.containsKey(property.name);
    }

    /**
     * Get the value of a property as the correct type. Convenience method which
     * transforms the property value from a string using an instance of the
     * {@link Property} helper class.
     *
     * @param property The property helper corresponding to the property of
     *                 which to get the value.
     * @param <T> The property type.
     * @return The value of the specified property transformed to the specified
     * type.
     */
    @SuppressWarnings("unchecked") // Responsibility of client
    public <T> T getProperty(Property<T> property) {
        return (identity.properties != null) ? property.fromString(identity.properties.get(property.name)) : null;
    }

    /**
     * Returns a material identical to this one, except with the specified
     * property set to the specified value.
     *
     * @param property The property that should be set.
     * @param value The value to which it should be set.
     * @return A material identical to this one, except with the specified
     * property set.
     */
    public <T> Material withProperty(Property<T> property, T value) {
        Map<String, String> newProperties = new HashMap<>();
        if (identity.properties != null) {
            newProperties.putAll(identity.properties);
        }
        newProperties.put(property.name, value.toString());
        return get(identity.name, newProperties);
    }

    /**
     * Indicates whether a specific property is present on this material.
     *
     * @param name The name of the property to check for presence.
     * @return <code>true</code> if the specified property is present.
     */
    public boolean hasProperty(String name) {
        return (identity.properties != null) && identity.properties.containsKey(name);
    }

    /**
     * Get the value of a property as a string.
     *
     * @param name The name of the property of which to get the value.
     * @return The value of the specified property as a string.
     */
    public String getProperty(String name) {
        return (identity.properties != null) ? identity.properties.get(name) : null;
    }

    /**
     * Returns a material identical to this one, except with the specified
     * property set to the specified value.
     *
     * @param name The name of the property that should be set.
     * @param value The value to which it should be set.
     * @return A material identical to this one, except with the specified
     * property set.
     */
    public Material withProperty(String name, String value) {
        Map<String, String> newProperties = new HashMap<>();
        if (identity.properties != null) {
            newProperties.putAll(identity.properties);
        }
        newProperties.put(name, value);
        return get(identity.name, newProperties);
    }

    /**
     * Get the cardinal direction this block is pointing, if applicable.
     * 
     * @return The cardinal direction in which this block is pointing, or
     *     <code>null</code> if it has no direction, or is not pointing in a
     *     cardinal direction (but for instance up or down)
     */
    public Direction getDirection() {
        switch (blockType) {
            case BLK_BRICK_STAIRS:
            case BLK_COBBLESTONE_STAIRS:
            case BLK_NETHER_BRICK_STAIRS:
            case BLK_STONE_BRICK_STAIRS:
            case BLK_WOODEN_STAIRS:
            case BLK_SANDSTONE_STAIRS:
            case BLK_PINE_WOOD_STAIRS:
            case BLK_BIRCH_WOOD_STAIRS:
            case BLK_JUNGLE_WOOD_STAIRS:
            case BLK_QUARTZ_STAIRS:
            case BLK_ACACIA_WOOD_STAIRS:
            case BLK_DARK_OAK_WOOD_STAIRS:
            case BLK_RED_SANDSTONE_STAIRS:
            case BLK_PURPUR_STAIRS:
                switch (data & 0x03) {
                    case 0:
                        return Direction.EAST;
                    case 1:
                        return Direction.WEST;
                    case 2:
                        return Direction.SOUTH;
                    case 3:
                        return Direction.NORTH;
                }
                break;
            case BLK_TORCH:
            case BLK_REDSTONE_TORCH_OFF:
            case BLK_REDSTONE_TORCH_ON:
                switch (data) {
                    case 1:
                        return Direction.EAST;
                    case 2:
                        return Direction.WEST;
                    case 3:
                        return Direction.SOUTH;
                    case 4:
                        return Direction.NORTH;
                }
                break;
            case BLK_RAILS:
                switch (data & 0x0F) {
                    case 0:
                        return Direction.NORTH;
                    case 1:
                        return Direction.EAST;
                    case 2:
                        return Direction.EAST;
                    case 3:
                        return Direction.WEST;
                    case 4:
                        return Direction.NORTH;
                    case 5:
                        return Direction.SOUTH;
                    case 6:
                        return Direction.EAST;
                    case 7:
                        return Direction.WEST;
                    case 8:
                        return Direction.NORTH;
                    case 9:
                        return Direction.SOUTH;
                }
                break;
            case BLK_POWERED_RAILS:
            case BLK_DETECTOR_RAILS:
                switch (data & 0x07) {
                    case 0:
                        return Direction.NORTH;
                    case 1:
                        return Direction.EAST;
                    case 2:
                        return Direction.EAST;
                    case 3:
                        return Direction.WEST;
                    case 4:
                        return Direction.NORTH;
                    case 5:
                        return Direction.SOUTH;
                }
                break;
            case BLK_LEVER:
                switch (data & 0x07) {
                    case 1:
                        return Direction.EAST;
                    case 2:
                        return Direction.WEST;
                    case 3:
                        return Direction.SOUTH;
                    case 4:
                        return Direction.NORTH;
                    case 5:
                        return Direction.NORTH;
                    case 6:
                        return Direction.EAST;
                }
                break;
            case BLK_WOODEN_DOOR:
            case BLK_IRON_DOOR:
            case BLK_PINE_WOOD_DOOR:
            case BLK_BIRCH_WOOD_DOOR:
            case BLK_JUNGLE_WOOD_DOOR:
            case BLK_ACACIA_WOOD_DOOR:
            case BLK_DARK_OAK_WOOD_DOOR:
                switch (data & 0x0B) {
                    case 0:
                        return Direction.WEST;
                    case 1:
                        return Direction.NORTH;
                    case 2:
                        return Direction.EAST;
                    case 3:
                        return Direction.SOUTH;
                }
                break;
            case BLK_STONE_BUTTON:
            case BLK_WOODEN_BUTTON:
                switch (data & 0x07) {
                    case 1:
                        return Direction.EAST;
                    case 2:
                        return Direction.WEST;
                    case 3:
                        return Direction.SOUTH;
                    case 4:
                        return Direction.NORTH;
                }
                break;
            case BLK_SIGN:
            case BLK_STANDING_BANNER:
                switch (data & 0x0C) {
                    case 0:
                        return Direction.SOUTH;
                    case 4:
                        return Direction.WEST;
                    case 8:
                        return Direction.NORTH;
                    case 12:
                        return Direction.EAST;
                }
                break;
            case BLK_LADDER:
            case BLK_WALL_SIGN:
            case BLK_FURNACE:
            case BLK_DISPENSER:
            case BLK_CHEST:
            case BLK_WALL_BANNER:
            case BLK_OBSERVER:
            case BLK_WHITE_SHULKER_BOX: // TODO: check assumption!
            case BLK_ORANGE_SHULKER_BOX: // TODO: check assumption!
            case BLK_MAGENTA_SHULKER_BOX: // TODO: check assumption!
            case BLK_LIGHT_BLUE_SHULKER_BOX: // TODO: check assumption!
            case BLK_YELLOW_SHULKER_BOX: // TODO: check assumption!
            case BLK_LIME_SHULKER_BOX: // TODO: check assumption!
            case BLK_PINK_SHULKER_BOX: // TODO: check assumption!
            case BLK_GREY_SHULKER_BOX: // TODO: check assumption!
            case BLK_LIGHT_GREY_SHULKER_BOX: // TODO: check assumption!
            case BLK_CYAN_SHULKER_BOX: // TODO: check assumption!
            case BLK_PURPLE_SHULKER_BOX: // TODO: check assumption!
            case BLK_BLUE_SHULKER_BOX: // TODO: check assumption!
            case BLK_BROWN_SHULKER_BOX: // TODO: check assumption!
            case BLK_GREEN_SHULKER_BOX: // TODO: check assumption!
            case BLK_RED_SHULKER_BOX: // TODO: check assumption!
            case BLK_BLACK_SHULKER_BOX: // TODO: check assumption!
                switch (data) {
                    case 2:
                        return Direction.NORTH;
                    case 3:
                        return Direction.SOUTH;
                    case 4:
                        return Direction.WEST;
                    case 5:
                        return Direction.EAST;
                }
                break;
            case BLK_PUMPKIN:
            case BLK_JACK_O_LANTERN:
            case BLK_BED:
            case BLK_FENCE_GATE:
            case BLK_PINE_WOOD_FENCE_GATE:
            case BLK_BIRCH_WOOD_FENCE_GATE:
            case BLK_JUNGLE_WOOD_FENCE_GATE:
            case BLK_DARK_OAK_WOOD_FENCE_GATE:
            case BLK_ACACIA_WOOD_FENCE_GATE:
            case BLK_TRIPWIRE_HOOK:
            case BLK_WHITE_GLAZED_TERRACOTTA:
            case BLK_ORANGE_GLAZED_TERRACOTTA:
            case BLK_MAGENTA_GLAZED_TERRACOTTA:
            case BLK_LIGHT_BLUE_GLAZED_TERRACOTTA:
            case BLK_YELLOW_GLAZED_TERRACOTTA:
            case BLK_LIME_GLAZED_TERRACOTTA:
            case BLK_PINK_GLAZED_TERRACOTTA:
            case BLK_GREY_GLAZED_TERRACOTTA:
            case BLK_LIGHT_GREY_GLAZED_TERRACOTTA:
            case BLK_CYAN_GLAZED_TERRACOTTA:
            case BLK_PURPLE_GLAZED_TERRACOTTA:
            case BLK_BLUE_GLAZED_TERRACOTTA:
            case BLK_BROWN_GLAZED_TERRACOTTA:
            case BLK_GREEN_GLAZED_TERRACOTTA:
            case BLK_RED_GLAZED_TERRACOTTA:
            case BLK_BLACK_GLAZED_TERRACOTTA:
                switch (data & 0x03) {
                    case 0:
                        return Direction.SOUTH;
                    case 1:
                        return Direction.WEST;
                    case 2:
                        return Direction.NORTH;
                    case 3:
                        return Direction.EAST;
                }
                break;
            case BLK_REDSTONE_REPEATER_OFF:
            case BLK_REDSTONE_REPEATER_ON:
            case BLK_REDSTONE_COMPARATOR_UNPOWERED:
                switch (data & 0x03) {
                    case 0:
                        return Direction.NORTH;
                    case 1:
                        return Direction.EAST;
                    case 2:
                        return Direction.SOUTH;
                    case 3:
                        return Direction.WEST;
                }
                break;
            case BLK_TRAPDOOR:
            case BLK_IRON_TRAPDOOR: // TODO: assumption
                switch (data & 0x03) {
                    case 0:
                        return Direction.SOUTH;
                    case 1:
                        return Direction.NORTH;
                    case 2:
                        return Direction.EAST;
                    case 3:
                        return Direction.WEST;
                }
                break;
            case BLK_PISTON:
            case BLK_PISTON_HEAD:
                switch (data & 0x07) {
                    case 2:
                        return Direction.NORTH;
                    case 3:
                        return Direction.SOUTH;
                    case 4:
                        return Direction.WEST;
                    case 5:
                        return Direction.EAST;
                }
                break;
            case BLK_WOOD:
            case BLK_WOOD2:
            case BLK_BONE_BLOCK: // TODO: assumption!
                switch (data & 0xC) {
                    case 0x4:
                        return Direction.EAST;
                    case 0x8:
                        return Direction.NORTH;
                    default:
                        return null;
                }
            case BLK_COCOA_PLANT:
                switch (data & 0x3) {
                    case 0x0:
                        return Direction.SOUTH;
                    case 0x1:
                        return Direction.WEST;
                    case 0x2:
                        return Direction.NORTH;
                    case 0x3:
                        return Direction.EAST;
                }
        }
        return null;
    }
    
    /**
     * Get a material that is pointing in the specified direction, if applicable
     * for the block type. Throws an exception otherwise.
     * 
     * @param direction The direction in which the returned material should
     *     point
     * @return A material with the same block type, but pointing in the
     *     specified direction
     * @throws IllegalArgumentException If this block type does not have the
     *     concept of direction
     */
    public Material setDirection(Direction direction) {
        switch (blockType) {
            case BLK_BRICK_STAIRS:
            case BLK_COBBLESTONE_STAIRS:
            case BLK_NETHER_BRICK_STAIRS:
            case BLK_STONE_BRICK_STAIRS:
            case BLK_WOODEN_STAIRS:
            case BLK_SANDSTONE_STAIRS:
            case BLK_PINE_WOOD_STAIRS:
            case BLK_BIRCH_WOOD_STAIRS:
            case BLK_JUNGLE_WOOD_STAIRS:
            case BLK_QUARTZ_STAIRS:
            case BLK_ACACIA_WOOD_STAIRS:
            case BLK_DARK_OAK_WOOD_STAIRS:
            case BLK_RED_SANDSTONE_STAIRS:
                switch (direction) {
                    case EAST:
                        return LEGACY_MATERIALS[((blockType) << 4) | (data & 0x0C)];
                    case WEST:
                        return LEGACY_MATERIALS[((blockType) << 4) | ((data & 0x0C) | 1)];
                    case SOUTH:
                        return LEGACY_MATERIALS[((blockType) << 4) | ((data & 0x0C) | 2)];
                    case NORTH:
                        return LEGACY_MATERIALS[((blockType) << 4) | ((data & 0x0C) | 3)];
                }
                break;
            case BLK_TORCH:
            case BLK_REDSTONE_TORCH_OFF:
            case BLK_REDSTONE_TORCH_ON:
                switch (direction) {
                    case EAST:
                        return LEGACY_MATERIALS[((blockType) << 4) | (1)];
                    case WEST:
                        return LEGACY_MATERIALS[((blockType) << 4) | (2)];
                    case SOUTH:
                        return LEGACY_MATERIALS[((blockType) << 4) | (3)];
                    case NORTH:
                        return LEGACY_MATERIALS[((blockType) << 4) | (4)];
                }
                break;
            case BLK_RAILS:
                if (data < 2) {
                    // Straight
                    switch (direction) {
                        case NORTH:
                        case SOUTH:
                            return LEGACY_MATERIALS[((blockType) << 4) | (0)];
                        case EAST:
                        case WEST:
                            return LEGACY_MATERIALS[((blockType) << 4) | (1)];
                    }
                } else {
                    // Sloped or round
                    boolean round = data > 5;
                    switch (direction) {
                        case EAST:
                            return LEGACY_MATERIALS[((blockType) << 4) | (round ? 6 : 2)];
                        case WEST:
                            return LEGACY_MATERIALS[((blockType) << 4) | (round ? 7 : 3)];
                        case NORTH:
                            return LEGACY_MATERIALS[((blockType) << 4) | (round ? 8 : 4)];
                        case SOUTH:
                            return LEGACY_MATERIALS[((blockType) << 4) | (round ? 9 : 5)];
                    }
                }
                break;
            case BLK_POWERED_RAILS:
            case BLK_DETECTOR_RAILS:
                if (data < 2) {
                    // Straight
                    switch (direction) {
                        case NORTH:
                        case SOUTH:
                            return LEGACY_MATERIALS[((blockType) << 4) | (data & 0x08)];
                        case EAST:
                        case WEST:
                            return LEGACY_MATERIALS[((blockType) << 4) | ((data & 0x08) | 1)];
                    }
                } else {
                    // Sloped
                    switch (direction) {
                        case EAST:
                            return LEGACY_MATERIALS[((blockType) << 4) | ((data & 0x08) | 2)];
                        case WEST:
                            return LEGACY_MATERIALS[((blockType) << 4) | ((data & 0x08) | 3)];
                        case NORTH:
                            return LEGACY_MATERIALS[((blockType) << 4) | ((data & 0x08) | 4)];
                        case SOUTH:
                            return LEGACY_MATERIALS[((blockType) << 4) | ((data & 0x08) | 5)];
                    }
                }
                break;
            case BLK_LEVER:
                boolean ground = (data & 0x07) > 4;
                switch (direction) {
                    case EAST:
                        return LEGACY_MATERIALS[((blockType) << 4) | ((data & 0x08) | (ground ? 6 : 1))];
                    case WEST:
                        return LEGACY_MATERIALS[((blockType) << 4) | ((data & 0x08) | (ground ? 6 : 2))];
                    case SOUTH:
                        return LEGACY_MATERIALS[((blockType) << 4) | ((data & 0x08) | (ground ? 5 : 3))];
                    case NORTH:
                        return LEGACY_MATERIALS[((blockType) << 4) | ((data & 0x08) | (ground ? 5 : 4))];
                }
                break;
            case BLK_WOODEN_DOOR:
            case BLK_IRON_DOOR:
            case BLK_PINE_WOOD_DOOR:
            case BLK_BIRCH_WOOD_DOOR:
            case BLK_JUNGLE_WOOD_DOOR:
            case BLK_ACACIA_WOOD_DOOR:
            case BLK_DARK_OAK_WOOD_DOOR:
                if ((data & 0x8) != 0) {
                    return this;
                } else {
                    switch (direction) {
                        case WEST:
                            return LEGACY_MATERIALS[((blockType) << 4) | (data & 0xC)];
                        case NORTH:
                            return LEGACY_MATERIALS[((blockType) << 4) | ((data & 0xC) | 1)];
                        case EAST:
                            return LEGACY_MATERIALS[((blockType) << 4) | ((data & 0xC) | 2)];
                        case SOUTH:
                            return LEGACY_MATERIALS[((blockType) << 4) | ((data & 0xC) | 3)];
                    }
                }
                break;
            case BLK_STONE_BUTTON:
            case BLK_WOODEN_BUTTON:
                switch (direction) {
                    case EAST:
                        return LEGACY_MATERIALS[((blockType) << 4) | ((data & 0x8) | 1)];
                    case WEST:
                        return LEGACY_MATERIALS[((blockType) << 4) | ((data & 0x8) | 2)];
                    case SOUTH:
                        return LEGACY_MATERIALS[((blockType) << 4) | ((data & 0x8) | 3)];
                    case NORTH:
                        return LEGACY_MATERIALS[((blockType) << 4) | ((data & 0x8) | 4)];
                }
                break;
            case BLK_SIGN:
            case BLK_STANDING_BANNER:
                switch (direction) {
                    case SOUTH:
                        return LEGACY_MATERIALS[((blockType) << 4) | (data & 0x03)];
                    case WEST:
                        return LEGACY_MATERIALS[((blockType) << 4) | ((data & 0x03) | 4)];
                    case NORTH:
                        return LEGACY_MATERIALS[((blockType) << 4) | ((data & 0x03) | 8)];
                    case EAST:
                        return LEGACY_MATERIALS[((blockType) << 4) | ((data & 0x03) | 12)];
                }
                break;
            case BLK_LADDER:
            case BLK_WALL_SIGN:
            case BLK_FURNACE:
            case BLK_DISPENSER:
            case BLK_CHEST:
            case BLK_WALL_BANNER:
            case BLK_OBSERVER:
            case BLK_WHITE_SHULKER_BOX: // TODO: check assumption!
            case BLK_ORANGE_SHULKER_BOX: // TODO: check assumption!
            case BLK_MAGENTA_SHULKER_BOX: // TODO: check assumption!
            case BLK_LIGHT_BLUE_SHULKER_BOX: // TODO: check assumption!
            case BLK_YELLOW_SHULKER_BOX: // TODO: check assumption!
            case BLK_LIME_SHULKER_BOX: // TODO: check assumption!
            case BLK_PINK_SHULKER_BOX: // TODO: check assumption!
            case BLK_GREY_SHULKER_BOX: // TODO: check assumption!
            case BLK_LIGHT_GREY_SHULKER_BOX: // TODO: check assumption!
            case BLK_CYAN_SHULKER_BOX: // TODO: check assumption!
            case BLK_PURPLE_SHULKER_BOX: // TODO: check assumption!
            case BLK_BLUE_SHULKER_BOX: // TODO: check assumption!
            case BLK_BROWN_SHULKER_BOX: // TODO: check assumption!
            case BLK_GREEN_SHULKER_BOX: // TODO: check assumption!
            case BLK_RED_SHULKER_BOX: // TODO: check assumption!
            case BLK_BLACK_SHULKER_BOX: // TODO: check assumption!
                switch (direction) {
                    case NORTH:
                        return LEGACY_MATERIALS[((blockType) << 4) | 2];
                    case SOUTH:
                        return LEGACY_MATERIALS[((blockType) << 4) | 3];
                    case WEST:
                        return LEGACY_MATERIALS[((blockType) << 4) | 4];
                    case EAST:
                        return LEGACY_MATERIALS[((blockType) << 4) | 5];
                }
                break;
            case BLK_PUMPKIN:
            case BLK_JACK_O_LANTERN:
            case BLK_BED:
            case BLK_FENCE_GATE:
            case BLK_PINE_WOOD_FENCE_GATE:
            case BLK_BIRCH_WOOD_FENCE_GATE:
            case BLK_JUNGLE_WOOD_FENCE_GATE:
            case BLK_DARK_OAK_WOOD_FENCE_GATE:
            case BLK_ACACIA_WOOD_FENCE_GATE:
            case BLK_TRIPWIRE_HOOK:
            case BLK_WHITE_GLAZED_TERRACOTTA:
            case BLK_ORANGE_GLAZED_TERRACOTTA:
            case BLK_MAGENTA_GLAZED_TERRACOTTA:
            case BLK_LIGHT_BLUE_GLAZED_TERRACOTTA:
            case BLK_YELLOW_GLAZED_TERRACOTTA:
            case BLK_LIME_GLAZED_TERRACOTTA:
            case BLK_PINK_GLAZED_TERRACOTTA:
            case BLK_GREY_GLAZED_TERRACOTTA:
            case BLK_LIGHT_GREY_GLAZED_TERRACOTTA:
            case BLK_CYAN_GLAZED_TERRACOTTA:
            case BLK_PURPLE_GLAZED_TERRACOTTA:
            case BLK_BLUE_GLAZED_TERRACOTTA:
            case BLK_BROWN_GLAZED_TERRACOTTA:
            case BLK_GREEN_GLAZED_TERRACOTTA:
            case BLK_RED_GLAZED_TERRACOTTA:
            case BLK_BLACK_GLAZED_TERRACOTTA:
                switch (direction) {
                    case SOUTH:
                        return LEGACY_MATERIALS[((blockType) << 4) | (data & 0x0C)];
                    case WEST:
                        return LEGACY_MATERIALS[((blockType) << 4) | ((data & 0x0C) | 1)];
                    case NORTH:
                        return LEGACY_MATERIALS[((blockType) << 4) | ((data & 0x0C) | 2)];
                    case EAST:
                        return LEGACY_MATERIALS[((blockType) << 4) | ((data & 0x0C) | 3)];
                }
                break;
            case BLK_REDSTONE_REPEATER_OFF:
            case BLK_REDSTONE_REPEATER_ON:
            case BLK_REDSTONE_COMPARATOR_UNPOWERED:
                switch (direction) {
                    case NORTH:
                        return LEGACY_MATERIALS[((blockType) << 4) | (data & 0x0C)];
                    case EAST:
                        return LEGACY_MATERIALS[((blockType) << 4) | ((data & 0x0C) | 1)];
                    case SOUTH:
                        return LEGACY_MATERIALS[((blockType) << 4) | ((data & 0x0C) | 2)];
                    case WEST:
                        return LEGACY_MATERIALS[((blockType) << 4) | ((data & 0x0C) | 3)];
                }
                break;
            case BLK_TRAPDOOR:
            case BLK_IRON_TRAPDOOR: // TODO: assumption
                switch (direction) {
                    case SOUTH:
                        return LEGACY_MATERIALS[((blockType) << 4) | (data & 0x0C)];
                    case NORTH:
                        return LEGACY_MATERIALS[((blockType) << 4) | ((data & 0x0C) | 1)];
                    case EAST:
                        return LEGACY_MATERIALS[((blockType) << 4) | ((data & 0x0C) | 2)];
                    case WEST:
                        return LEGACY_MATERIALS[((blockType) << 4) | ((data & 0x0C) | 3)];
                }
                break;
            case BLK_PISTON:
            case BLK_PISTON_HEAD:
                switch (direction) {
                    case NORTH:
                        return LEGACY_MATERIALS[((blockType) << 4) | ((data & 0x08) | 2)];
                    case SOUTH:
                        return LEGACY_MATERIALS[((blockType) << 4) | ((data & 0x08) | 3)];
                    case WEST:
                        return LEGACY_MATERIALS[((blockType) << 4) | ((data & 0x08) | 4)];
                    case EAST:
                        return LEGACY_MATERIALS[((blockType) << 4) | ((data & 0x08) | 5)];
                }
                break;
            case BLK_WOOD:
            case BLK_WOOD2:
            case BLK_BONE_BLOCK: // TODO: assumption!
                switch (direction) {
                    case NORTH:
                    case SOUTH:
                        return LEGACY_MATERIALS[((blockType) << 4) | ((data & 0x3) | 0x8)];
                    case EAST:
                    case WEST:
                        return LEGACY_MATERIALS[((blockType) << 4) | ((data & 0x3) | 0x4)];
                }
                break;
            case BLK_COCOA_PLANT:
                switch (direction) {
                    case SOUTH:
                        return LEGACY_MATERIALS[((blockType) << 4) | (data & 0xC)];
                    case WEST:
                        return LEGACY_MATERIALS[((blockType) << 4) | ((data & 0xC) | 0x1)];
                    case NORTH:
                        return LEGACY_MATERIALS[((blockType) << 4) | ((data & 0xC) | 0x2)];
                    case EAST:
                        return LEGACY_MATERIALS[((blockType) << 4) | ((data & 0xC) | 0x3)];
                }
                break;
        }
        throw new IllegalArgumentException("Block type " + blockType + " has no direction");
    }
    
    /**
     * If applicable, return a Material that is rotated a specific number of
     * quarter turns.
     * 
     * @param steps The number of 90 degree turns to turn the material
     *     clockwise. May be negative to turn the material anti clockwise
     * @return The rotated material (or the same one if rotation does not apply
     *     to this material)
     */
    public Material rotate(int steps) {
        switch (blockType) {
            case BLK_VINES:
                int bitMask = (data << 4) | data;
                steps = steps & 0x3;
                if (steps > 0) {
                    return LEGACY_MATERIALS[((blockType) << 4) | (((bitMask << steps) & 0xF0) >> 4)];
                } else {
                    return this;
                }
            case BLK_HUGE_BROWN_MUSHROOM:
            case BLK_HUGE_RED_MUSHROOM:
                Direction direction;
                switch (data) {
                    case 1:
                    case 2:
                        direction = Direction.NORTH;
                        break;
                    case 3:
                    case 6:
                        direction = Direction.EAST;
                        break;
                    case 9:
                    case 8:
                        direction = Direction.SOUTH;
                        break;
                    case 7:
                    case 4:
                        direction = Direction.WEST;
                        break;
                    default:
                        return this;
                }
                boolean corner = (data & 0x01) != 0;
                direction = direction.rotate(steps);
                switch (direction) {
                    case NORTH:
                        return LEGACY_MATERIALS[((blockType) << 4) | (corner ? 1 : 2)];
                    case EAST:
                        return LEGACY_MATERIALS[((blockType) << 4) | (corner ? 3 : 6)];
                    case SOUTH:
                        return LEGACY_MATERIALS[((blockType) << 4) | (corner ? 9 : 8)];
                    case WEST:
                        return LEGACY_MATERIALS[((blockType) << 4) | (corner ? 7 : 4)];
                    default:
                        throw new InternalError();
                }
            default:
                direction = getDirection();
                if (direction != null) {
                    return setDirection(direction.rotate(steps));
                } else {
                    return this;
                }
        }
    }
    
    /**
     * If applicable, return a Material that is the mirror image of this one in
     * a specific axis.
     * 
     * @param axis Indicates the axis in which to mirror the material.
     * @return The mirrored material (or the same one if mirroring does not
     *     apply to this material)
     */
    public Material mirror(Direction axis) {
        if (blockType == BLK_VINES) {
            boolean north = (data & 4) != 0;
            boolean east =  (data & 8) != 0;
            boolean south = (data & 1) != 0;
            boolean west =  (data & 2) != 0;
            if ((axis == Direction.EAST) || (axis == Direction.WEST)) {
            // TODO: this is wrong. Probably a leftover from the coordinate shift. It should be:
//            if ((axis == Direction.NORTH) || (axis == Direction.SOUTH)) {
                boolean tmp = east;
                east = west;
                west = tmp;
            } else {
                boolean tmp = north;
                north = south;
                south = tmp;
            }
            return LEGACY_MATERIALS[((blockType) << 4) | ((north ? 4 : 0)
                    | (east  ? 8 : 0)
                    | (south ? 1 : 0)
                    | (west  ? 2 : 0))];
        } else if ((blockType == BLK_HUGE_BROWN_MUSHROOM) || (blockType == BLK_HUGE_RED_MUSHROOM)) {
            if ((axis == Direction.EAST) || (axis == Direction.WEST)) {
            // TODO: this is wrong. Probably a leftover from the coordinate shift. It should be:
//            if ((axis == Direction.NORTH) || (axis == Direction.SOUTH)) {
                switch (data) {
                    case 1:
                        return LEGACY_MATERIALS[((blockType) << 4) | (3)];
                    case 3:
                        return LEGACY_MATERIALS[((blockType) << 4) | (1)];
                    case 4:
                        return LEGACY_MATERIALS[((blockType) << 4) | (6)];
                    case 6:
                        return LEGACY_MATERIALS[((blockType) << 4) | (4)];
                    case 7:
                        return LEGACY_MATERIALS[((blockType) << 4) | (9)];
                    case 9:
                        return LEGACY_MATERIALS[((blockType) << 4) | (7)];
                    default:
                        return this;
                }
            } else {
                switch (data) {
                    case 1:
                        return LEGACY_MATERIALS[((blockType) << 4) | (7)];
                    case 2:
                        return LEGACY_MATERIALS[((blockType) << 4) | (8)];
                    case 3:
                        return LEGACY_MATERIALS[((blockType) << 4) | (9)];
                    case 7:
                        return LEGACY_MATERIALS[((blockType) << 4) | (1)];
                    case 8:
                        return LEGACY_MATERIALS[((blockType) << 4) | (2)];
                    case 9:
                        return LEGACY_MATERIALS[((blockType) << 4) | (3)];
                    default:
                        return this;
                }
            }
        } else if ((blockType == BLK_SIGN) || (blockType == BLK_STANDING_BANNER)) {
            if ((axis == Direction.EAST) || (axis == Direction.WEST)) {
            // TODO: this is wrong. Probably a leftover from the coordinate shift. It should be:
//            if ((axis == Direction.NORTH) || (axis == Direction.SOUTH)) {
                switch (data) {
                    case 1:
                        return LEGACY_MATERIALS[((blockType) << 4) | (15)];
                    case 2:
                        return LEGACY_MATERIALS[((blockType) << 4) | (14)];
                    case 3:
                        return LEGACY_MATERIALS[((blockType) << 4) | (13)];
                    case 4:
                        return LEGACY_MATERIALS[((blockType) << 4) | (12)];
                    case 5:
                        return LEGACY_MATERIALS[((blockType) << 4) | (11)];
                    case 6:
                        return LEGACY_MATERIALS[((blockType) << 4) | (10)];
                    case 7:
                        return LEGACY_MATERIALS[((blockType) << 4) | (9)];
                    case 9:
                        return LEGACY_MATERIALS[((blockType) << 4) | (7)];
                    case 10:
                        return LEGACY_MATERIALS[((blockType) << 4) | (6)];
                    case 11:
                        return LEGACY_MATERIALS[((blockType) << 4) | (5)];
                    case 12:
                        return LEGACY_MATERIALS[((blockType) << 4) | (4)];
                    case 13:
                        return LEGACY_MATERIALS[((blockType) << 4) | (3)];
                    case 14:
                        return LEGACY_MATERIALS[((blockType) << 4) | (2)];
                    case 15:
                        return LEGACY_MATERIALS[((blockType) << 4) | (1)];
                    default:
                        return this;
                }
            } else {
                switch (data) {
                    case 0:
                        return LEGACY_MATERIALS[((blockType) << 4) | (8)];
                    case 1:
                        return LEGACY_MATERIALS[((blockType) << 4) | (7)];
                    case 2:
                        return LEGACY_MATERIALS[((blockType) << 4) | (6)];
                    case 3:
                        return LEGACY_MATERIALS[((blockType) << 4) | (5)];
                    case 5:
                        return LEGACY_MATERIALS[((blockType) << 4) | (3)];
                    case 6:
                        return LEGACY_MATERIALS[((blockType) << 4) | (2)];
                    case 7:
                        return LEGACY_MATERIALS[((blockType) << 4) | (1)];
                    case 8:
                        return LEGACY_MATERIALS[((blockType) << 4) | (0)];
                    case 9:
                        return LEGACY_MATERIALS[((blockType) << 4) | (15)];
                    case 10:
                        return LEGACY_MATERIALS[((blockType) << 4) | (14)];
                    case 11:
                        return LEGACY_MATERIALS[((blockType) << 4) | (13)];
                    case 13:
                        return LEGACY_MATERIALS[((blockType) << 4) | (11)];
                    case 14:
                        return LEGACY_MATERIALS[((blockType) << 4) | (10)];
                    case 15:
                        return LEGACY_MATERIALS[((blockType) << 4) | (9)];
                    default:
                        return this;
                }
            }
        } else {
            Direction direction = getDirection();
            switch (axis) {
                case EAST:
                case WEST:
                // TODO: this is wrong. Probably a leftover from the coordinate shift. It should be:
//                case NORTH:
//                case SOUTH:
                    if ((direction == Direction.EAST) || (direction == Direction.WEST)) {
                        return rotate(2);
                    } else {
                        return this;
                    }
                case NORTH:
                case SOUTH:
                // TODO: this is wrong. Probably a leftover from the coordinate shift. It should be:
//                case EAST:
//                case WEST:
                    if ((direction == Direction.NORTH) || (direction == Direction.SOUTH)) {
                        return rotate(2);
                    } else {
                        return this;
                    }
                default:
                    throw new InternalError();
            }
        }
    }

    /**
     * Gets a vertically mirrored version of the material.
     *
     * @return A vertically mirrored version of the material.
     */
    public Material invert() {
        switch (blockType) {
            case BLK_BRICK_STAIRS:
            case BLK_COBBLESTONE_STAIRS:
            case BLK_NETHER_BRICK_STAIRS:
            case BLK_STONE_BRICK_STAIRS:
            case BLK_WOODEN_STAIRS:
            case BLK_SANDSTONE_STAIRS:
            case BLK_PINE_WOOD_STAIRS:
            case BLK_BIRCH_WOOD_STAIRS:
            case BLK_JUNGLE_WOOD_STAIRS:
            case BLK_QUARTZ_STAIRS:
            case BLK_ACACIA_WOOD_STAIRS:
            case BLK_DARK_OAK_WOOD_STAIRS:
            case BLK_RED_SANDSTONE_STAIRS:
            case BLK_PURPUR_STAIRS:
                return LEGACY_MATERIALS[((blockType) << 4) | (data ^ 0x4)];
            case BLK_SLAB:
            case BLK_RED_SANDSTONE_SLAB:
            case BLK_WOODEN_SLAB:
            case BLK_PURPUR_SLAB:
                return LEGACY_MATERIALS[((blockType) << 4) | (data ^ 0x8)];
            default:
                return this;
        }
    }

    /**
     * Indicates whether the material has an associated image which can be
     * retrieved with the {@link #getImage(BufferedImage)} or painted with the
     * {@link #paintImage(Graphics2D, int, int, BufferedImage)} method.
     * 
     * @return <code>true</code> if this material has an associated image. 
     */
    public boolean hasImage() {
        return false; // TODO
    }
    
    /**
     * Gets the relevant image for this material from the specified Minecraft
     * texture pack terrain image.
     */
    public BufferedImage getImage(BufferedImage terrain) {
        throw new UnsupportedOperationException("Not supported yet"); // TODO
    }
    
    /**
     * Paints the relevant image for this material from the specified Minecraft
     * texture pack terrain image to the specified location on a graphics
     * canvas.
     * 
     * @param g2 The graphics canvas to paint the image on.
     * @param x The X coordinate to paint the image to.
     * @param y The Y coordinate to paint the image to.
     * @param terrain The texture pack terrain image to get the image from.
     */
    public void paintImage(Graphics2D g2, int x, int y, BufferedImage terrain) {
        throw new UnsupportedOperationException("Not supported yet"); // TODO
    }

    /**
     * Compare the material in name only, disregarding its properties.
     *
     * @param name The name to test this material for.
     * @return <code>true</code> if the material has the specified name.
     */
    @SuppressWarnings("StringEquality") // name is interned so there are many circumstances in which the comparison might work and be faster than equals()
    public boolean isNamed(String name) {
        return (name == this.name) || name.equals(this.name);
    }

    /**
     * Compare the material in name only, disregarding its properties.
     *
     * @param name The name to test this material for.
     * @return <code>true</code> if the material <em>does not</em> have the
     * specified name.
     */
    @SuppressWarnings("StringEquality") // name is interned so there are many circumstances in which the comparison might work and be faster than equals()
    public boolean isNotNamed(String name) {
        return (name != this.name) && (! name.equals(this.name));
    }

    /**
     * Compare two materials in name only, disregarding their properties.
     *
     * @param material The material to compare this material with.
     * @return <code>true</code> if the specified material has the same name as
     * this one.
     */
    @SuppressWarnings("StringEquality") // name is interned so there are many circumstances in which the comparison might work and be faster than equals()
    public boolean isNamedSameAs(Material material) {
        return (material.name == this.name) || material.name.equals(this.name);
    }

    /**
     * Compare two materials in name only, disregarding their properties.
     *
     * @param material The material to compare this material with.
     * @return <code>true</code> if the specified material <em>does not</em>
     * have the same name as this one.
     */
    @SuppressWarnings("StringEquality") // name is interned so there are many circumstances in which the comparison might work and be faster than equals()
    public boolean isNotNamedSameAs(Material material) {
        return (material.name != this.name) && (! material.name.equals(this.name));
    }

    private String createStringRep() {
        StringBuilder sb = new StringBuilder();
        if (! namespace.equals(MINECRAFT)) {
            sb.append(namespace);
            sb.append(':');
        }
        sb.append(simpleName);
        if (identity.properties != null) {
            boolean[] first = {true};
            identity.properties.forEach((key, value) -> {
                if (value.equals("false") || value.equals("0")) {
                    // Skip
                } else {
                    if (first[0]) {
                        sb.append('(');
                        first[0] = false;
                    } else {
                        sb.append(", ");
                    }
                    if (value.equals("true")) {
                        sb.append(key);
                    } else {
                        sb.append(key);
                        sb.append(": ");
                        sb.append(value);
                    }
                }
            });
            if (! first[0]) {
                sb.append(')');
            }
        }
        return sb.toString();
    }

    private String createLegacyStringRep() {
        if (blockType < 0) {
            return createStringRep();
        } else if ((blockType < BLOCK_TYPE_NAMES.length) && (BLOCK_TYPE_NAMES[blockType] != null)) {
            if (data > 0) {
                return BLOCK_TYPE_NAMES[blockType] + " (" + data + ")";
            } else {
                return BLOCK_TYPE_NAMES[blockType];
            }
        } else if (data > 0) {
            return blockType + " (" + data + ")";
        } else {
            return Integer.toString(blockType);
        }
    }

    /**
     * Get a legacy (pre-1.13) material by block ID. The data value is assumed
     * to be zero.
     *
     * @param blockType The block ID.
     * @return The requested material.
     */
    public static Material get(int blockType) {
        return getByCombinedIndex(blockType << 4);
    }

    /**
     * Get a legacy (pre-1.13) material by block ID and data value.
     *
     * @param blockType The block ID.
     * @param data The data value.
     * @return The requested material.
     */
    public static Material get(int blockType, int data) {
        return getByCombinedIndex((blockType << 4) | data);
    }

    /**
     * Get a legacy (pre-1.13) material by {@link Block} reference. The data
     * value is assumed to be zero.
     *
     * @param blockType The block type.
     * @return The requested material.
     */
    public static Material get(Block blockType) {
        return getByCombinedIndex(blockType.id << 4);
    }

    /**
     * Get a legacy (pre-1.13) material by {@link Block} reference and data
     * value.
     *
     * @param blockType The block type.
     * @param data The data value.
     * @return The requested material.
     */
    public static Material get(Block blockType, int data) {
        return getByCombinedIndex((blockType.id << 4) | data);
    }

    /**
     * Get a legacy (pre-1.13) material corresponding to a combined index
     * consisting of the block ID shifted left four bits and or-ed with the data
     * value. In other words the index is a 16-bit unsigned integer, with bit
     * 0-3 indicating the data value and bit 4-15 indicating the block ID.
     *
     * @param index The combined index of the material to get.
     * @return The indicated material.
     */
    public static Material getByCombinedIndex(int index) {
        if (index >= LEGACY_MATERIALS.length) {
            return get(new Identity("legacy:block_" + (index >> 4), Collections.singletonMap("data_value", Integer.toString(index & 0xf))));
        } else {
            return LEGACY_MATERIALS[index];
        }
    }

    /**
     * Get the single instance of the material with the given identity.
     *
     * @param identity The identity of the material to get.
     * @return The single instance of the specified material.
     */
    public static Material get(Identity identity) {
        synchronized (ALL_MATERIALS) {
            Material material = ALL_MATERIALS.get(identity);
            if (material == null) {
                material = new Material(identity);
                ALL_MATERIALS.put(identity, material);
            }
            return material;
        }
    }

    /**
     * Get the single instance of the material with the given name and
     * properties.
     *
     * @param name The name of the material to get.
     * @param properties The properties of the material to get. May be
     *                   <code>null</code>.
     * @return The single instance of the specified material.
     */
    public static Material get(String name, Map<String, String> properties) {
        return get(new Identity(name, properties));
    }

    /**
     * Get the single instance of the material with the given name and
     * properties.
     *
     * @param name The name of the material to get.
     * @param properties The properties of the material to get, as a list of
     *                   key-value pairs. The keys must be <code>String</code>s.
     *                   May be <code>null</code>.
     * @return The single instance of the specified material.
     */
    public static Material get(String name, Object... properties) {
        Map<String, String> propertyMap;
        if (properties != null) {
            propertyMap = new HashMap<>();
            for (int i = 0; i < properties.length; i += 2) {
                propertyMap.put((String) properties[i], properties[i + 1].toString());
            }
        } else {
            propertyMap = null;
        }
        return get(name, propertyMap);
    }

    /**
     * Get a known material by name only, disregarding the properties.
     *
     * @param name The name of the material to get.
     * @return A material with the specified name and unspecified properties, or
     * <code>null</code> if no material by that name is known.
     */
    public static Material getDefault(String name) {
        for (Material material: ALL_MATERIALS.values()) {
            if (material.identity.name.equals(name)) {
                return material;
            }
        }
        return null;
    }

    public static Set<String> getAllNamespaces() {
        return Collections.unmodifiableSet(ALL_NAMESPACES);
    }
    
    public static Set<String> getAllSimpleNamesForNamespace(String namespace) {
        return SIMPLE_NAMES_BY_NAMESPACE.containsKey(namespace) ? Collections.unmodifiableSet(SIMPLE_NAMES_BY_NAMESPACE.get(namespace)) : Collections.EMPTY_SET;
    }

    // Object

    public boolean equals(Object o) {
        return (o instanceof Material)
            && identity.equals(((Material) o).identity);
    }

    public int hashCode() {
        return identity.hashCode();
    }

    /**
     * Get the modern style (name and property-based) name of this material. For
     * brevity, the namespace is omitted if it isn't <code>minecraft</code> and
     * properties with value <code>"false"</code> or <code>"0"</code> are also
     * omitted.
     *
     * @return The modern style name of this material.
     */
    @Override
    public String toString() {
        return stringRep;
    }

    /**
     * For legacy materials (pre-1.13; with a numerical block ID), get the
     * legacy style block name for this material. For modern materials, returns
     * the same as {@link #toString()}.
     *
     * @return The legacy (pre-1.13) style block name for this material, if
     * applicable.
     */
    public String toLegacyString() {
        return legacyStringRep;
    }

    private Object readResolve() throws ObjectStreamException {
        if (blockType != -1) {
            int index = (blockType << 4) | data;
            if (index >= LEGACY_MATERIALS.length) {
                return get(new Identity("legacy:block_" + blockType, Collections.singletonMap("data_value", Integer.toString(data))));
            } else {
                return LEGACY_MATERIALS[index];
            }
        } else {
            return get(identity);
        }
    }

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
     * Whether the block is part of Minecraft-generated natural ground; more
     * specifically whether the block type should be assigned a terrain type
     * when importing a Minecraft map.
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

    /** @deprecated Use names and properties. */
    public final int blockType;
    /** @deprecated Use names and properties. */
    public final int data;
    public final transient int index;
    public final transient String simpleName, namespace;
    private final Identity identity;
    private final transient String stringRep, legacyStringRep;

    private static final Map<Integer, Map<String, Object>> BLOCK_SPECS = new HashMap<>();

    static {
        // Read MC block database
        try (InputStreamReader in = new InputStreamReader(Block.class.getResourceAsStream("mc-blocks.json"))) {
            java.util.List<?> list = (List<?>) new JSONParser().parse(in);
            for (Object listEntry: list) {
                @SuppressWarnings("unchecked") // Guaranteed by contents of file
                Map<String, Object> blockSpec = (Map<String, Object>) listEntry;
                int blockId = ((Number) blockSpec.get("blockId")).intValue();
                int dataValue = ((Number) blockSpec.get("dataValue")).intValue();
                BLOCK_SPECS.put((blockId << 4) | dataValue, blockSpec);
            }
        } catch (IOException e) {
            throw new RuntimeException("I/O error while reading Minecraft block database mc-blocks.json from classpath", e);
        } catch (ParseException e) {
            throw new RuntimeException("JSON parsing error while reading Minecraft block database mc-blocks.json from classpath", e);
        }
    }

    /**
     * To save space we only store the 256-ish vanilla blocks as pre-created
     * legacy materials. 12-bit block ids above 255 are created on the fly.
     */
    private static final Material[] LEGACY_MATERIALS = new Material[4096];
    private static final Map<Identity, Material> ALL_MATERIALS = new HashMap<>();
    private static final Set<String> ALL_NAMESPACES = new HashSet<>();
    private static final Map<String, Set<String>> SIMPLE_NAMES_BY_NAMESPACE = new HashMap<>();
    private static final Map<String, Material> DEFAULT_MATERIALS_BY_NAME = new HashMap<>();

    static {
        for (int i = 0; i < 4096; i++) {
            LEGACY_MATERIALS[i] = new Material(i >> 4, i & 0xF);
            ALL_MATERIALS.put(LEGACY_MATERIALS[i].identity, LEGACY_MATERIALS[i]);
        }
    }

    // Legacy materials (with numerical IDs for compatibility with pre-1.13
    // Minecraft)

    public static final Material AIR = LEGACY_MATERIALS[(BLK_AIR) << 4];
    public static final Material DANDELION = LEGACY_MATERIALS[(BLK_DANDELION) << 4];
    public static final Material ROSE = LEGACY_MATERIALS[(BLK_ROSE) << 4];
    public static final Material GRASS = LEGACY_MATERIALS[(BLK_GRASS) << 4];
    public static final Material DIRT = LEGACY_MATERIALS[(BLK_DIRT) << 4];
    public static final Material STONE = LEGACY_MATERIALS[(BLK_STONE) << 4];
    public static final Material GRANITE = LEGACY_MATERIALS[((BLK_STONE) << 4) | (DATA_STONE_GRANITE)];
    public static final Material DIORITE = LEGACY_MATERIALS[((BLK_STONE) << 4) | (DATA_STONE_DIORITE)];
    public static final Material ANDESITE = LEGACY_MATERIALS[((BLK_STONE) << 4) | (DATA_STONE_ANDESITE)];
    public static final Material COBBLESTONE = LEGACY_MATERIALS[(BLK_COBBLESTONE) << 4];
    public static final Material SNOW = LEGACY_MATERIALS[(BLK_SNOW) << 4];
    public static final Material DEAD_SHRUBS = LEGACY_MATERIALS[(BLK_DEAD_SHRUBS) << 4];
    public static final Material CACTUS = LEGACY_MATERIALS[(BLK_CACTUS) << 4];
    public static final Material SAND = LEGACY_MATERIALS[(BLK_SAND) << 4];
    public static final Material FIRE = LEGACY_MATERIALS[(BLK_FIRE) << 4];
    public static final Material GLOWSTONE = LEGACY_MATERIALS[(BLK_GLOWSTONE) << 4];
    public static final Material SOUL_SAND = LEGACY_MATERIALS[(BLK_SOUL_SAND) << 4];
    public static final Material LAVA = LEGACY_MATERIALS[(BLK_LAVA) << 4];
    public static final Material NETHERRACK = LEGACY_MATERIALS[(BLK_NETHERRACK) << 4];
    public static final Material END_STONE = LEGACY_MATERIALS[BLK_END_STONE << 4];
    public static final Material CHORUS_PLANT = LEGACY_MATERIALS[BLK_CHORUS_PLANT << 4];
    public static final Material COAL = LEGACY_MATERIALS[(BLK_COAL) << 4];
    public static final Material GRAVEL = LEGACY_MATERIALS[(BLK_GRAVEL) << 4];
    public static final Material REDSTONE_ORE = LEGACY_MATERIALS[(BLK_REDSTONE_ORE) << 4];
    public static final Material IRON_ORE = LEGACY_MATERIALS[(BLK_IRON_ORE) << 4];
    public static final Material WATER = LEGACY_MATERIALS[(BLK_WATER) << 4];
    public static final Material GOLD_ORE = LEGACY_MATERIALS[(BLK_GOLD_ORE) << 4];
    public static final Material LAPIS_LAZULI_ORE = LEGACY_MATERIALS[(BLK_LAPIS_LAZULI_ORE) << 4];
    public static final Material DIAMOND_ORE = LEGACY_MATERIALS[(BLK_DIAMOND_ORE) << 4];
    public static final Material BEDROCK = LEGACY_MATERIALS[(BLK_BEDROCK) << 4];
    public static final Material STATIONARY_WATER = LEGACY_MATERIALS[(BLK_STATIONARY_WATER) << 4];
    public static final Material STATIONARY_LAVA = LEGACY_MATERIALS[(BLK_STATIONARY_LAVA) << 4];
    public static final Material SNOW_BLOCK = LEGACY_MATERIALS[(BLK_SNOW_BLOCK) << 4];
    public static final Material SANDSTONE = LEGACY_MATERIALS[(BLK_SANDSTONE) << 4];
    public static final Material CLAY = LEGACY_MATERIALS[(BLK_CLAY) << 4];
    public static final Material MOSSY_COBBLESTONE = LEGACY_MATERIALS[(BLK_MOSSY_COBBLESTONE) << 4];
    public static final Material OBSIDIAN = LEGACY_MATERIALS[(BLK_OBSIDIAN) << 4];
    public static final Material FENCE = LEGACY_MATERIALS[(BLK_FENCE) << 4];
    public static final Material GLASS_PANE = LEGACY_MATERIALS[(BLK_GLASS_PANE) << 4];
    public static final Material STONE_BRICKS = LEGACY_MATERIALS[(BLK_STONE_BRICKS) << 4];
    public static final Material BRICKS = LEGACY_MATERIALS[(BLK_BRICKS) << 4];
    public static final Material COBWEB = LEGACY_MATERIALS[(BLK_COBWEB) << 4];
    public static final Material DIAMOND_BLOCK = LEGACY_MATERIALS[(BLK_DIAMOND_BLOCK) << 4];
    public static final Material GOLD_BLOCK = LEGACY_MATERIALS[(BLK_GOLD_BLOCK) << 4];
    public static final Material IRON_BLOCK = LEGACY_MATERIALS[(BLK_IRON_BLOCK) << 4];
    public static final Material LAPIS_LAZULI_BLOCK = LEGACY_MATERIALS[(BLK_LAPIS_LAZULI_BLOCK) << 4];
    public static final Material MYCELIUM = LEGACY_MATERIALS[(BLK_MYCELIUM) << 4];
    public static final Material TILLED_DIRT = LEGACY_MATERIALS[(BLK_TILLED_DIRT) << 4];
    public static final Material ICE = LEGACY_MATERIALS[(BLK_ICE) << 4];
    public static final Material PACKED_ICE = LEGACY_MATERIALS[BLK_PACKED_ICE << 4];
    public static final Material TORCH = LEGACY_MATERIALS[(BLK_TORCH) << 4];
    public static final Material COBBLESTONE_STAIRS = LEGACY_MATERIALS[(BLK_COBBLESTONE_STAIRS) << 4];
    public static final Material GLASS = LEGACY_MATERIALS[(BLK_GLASS) << 4];
    public static final Material WOODEN_STAIRS = LEGACY_MATERIALS[(BLK_WOODEN_STAIRS) << 4];
    public static final Material CHEST_NORTH = LEGACY_MATERIALS[((BLK_CHEST) << 4) | (2)];
    public static final Material CHEST_SOUTH = LEGACY_MATERIALS[((BLK_CHEST) << 4) | (3)];
    public static final Material CHEST_WEST = LEGACY_MATERIALS[((BLK_CHEST) << 4) | (4)];
    public static final Material CHEST_EAST = LEGACY_MATERIALS[((BLK_CHEST) << 4) | (5)];
    public static final Material WALL_SIGN = LEGACY_MATERIALS[(BLK_WALL_SIGN) << 4];
    public static final Material BRICK_STAIRS = LEGACY_MATERIALS[(BLK_BRICK_STAIRS) << 4];
    public static final Material STONE_BRICK_STAIRS = LEGACY_MATERIALS[(BLK_STONE_BRICK_STAIRS) << 4];
    public static final Material LADDER = LEGACY_MATERIALS[(BLK_LADDER) << 4];
    public static final Material TRAPDOOR = LEGACY_MATERIALS[(BLK_TRAPDOOR) << 4];
    public static final Material WHEAT = LEGACY_MATERIALS[(BLK_WHEAT) << 4];
    public static final Material LILY_PAD = LEGACY_MATERIALS[(BLK_LILY_PAD) << 4];
    public static final Material RED_MUSHROOM = LEGACY_MATERIALS[(BLK_RED_MUSHROOM) << 4];
    public static final Material BROWN_MUSHROOM = LEGACY_MATERIALS[(BLK_BROWN_MUSHROOM) << 4];
    public static final Material SUGAR_CANE = LEGACY_MATERIALS[(BLK_SUGAR_CANE) << 4];
    public static final Material EMERALD_ORE = LEGACY_MATERIALS[(BLK_EMERALD_ORE) << 4];
    public static final Material EMERALD_BLOCK = LEGACY_MATERIALS[(BLK_EMERALD_BLOCK) << 4];
    public static final Material PERMADIRT = LEGACY_MATERIALS[((BLK_DIRT) << 4) | (1)];
    public static final Material PODZOL = LEGACY_MATERIALS[((BLK_DIRT) << 4) | (2)];
    public static final Material RED_SAND = LEGACY_MATERIALS[((BLK_SAND) << 4) | (1)];
    public static final Material HARDENED_CLAY = LEGACY_MATERIALS[(BLK_HARDENED_CLAY) << 4];
    public static final Material WHITE_CLAY = LEGACY_MATERIALS[(BLK_STAINED_CLAY) << 4];
    public static final Material ORANGE_CLAY = LEGACY_MATERIALS[((BLK_STAINED_CLAY) << 4) | (DATA_ORANGE)];
    public static final Material MAGENTA_CLAY = LEGACY_MATERIALS[((BLK_STAINED_CLAY) << 4) | (DATA_MAGENTA)];
    public static final Material LIGHT_BLUE_CLAY = LEGACY_MATERIALS[((BLK_STAINED_CLAY) << 4) | (DATA_LIGHT_BLUE)];
    public static final Material YELLOW_CLAY = LEGACY_MATERIALS[((BLK_STAINED_CLAY) << 4) | (DATA_YELLOW)];
    public static final Material LIME_CLAY = LEGACY_MATERIALS[((BLK_STAINED_CLAY) << 4) | (DATA_LIME)];
    public static final Material PINK_CLAY = LEGACY_MATERIALS[((BLK_STAINED_CLAY) << 4) | (DATA_PINK)];
    public static final Material GREY_CLAY = LEGACY_MATERIALS[((BLK_STAINED_CLAY) << 4) | (DATA_GREY)];
    public static final Material LIGHT_GREY_CLAY = LEGACY_MATERIALS[((BLK_STAINED_CLAY) << 4) | (DATA_LIGHT_GREY)];
    public static final Material CYAN_CLAY = LEGACY_MATERIALS[((BLK_STAINED_CLAY) << 4) | (DATA_CYAN)];
    public static final Material PURPLE_CLAY = LEGACY_MATERIALS[((BLK_STAINED_CLAY) << 4) | (DATA_PURPLE)];
    public static final Material BLUE_CLAY = LEGACY_MATERIALS[((BLK_STAINED_CLAY) << 4) | (DATA_BLUE)];
    public static final Material BROWN_CLAY = LEGACY_MATERIALS[((BLK_STAINED_CLAY) << 4) | (DATA_BROWN)];
    public static final Material GREEN_CLAY = LEGACY_MATERIALS[((BLK_STAINED_CLAY) << 4) | (DATA_GREEN)];
    public static final Material RED_CLAY = LEGACY_MATERIALS[((BLK_STAINED_CLAY) << 4) | (DATA_RED)];
    public static final Material BLACK_CLAY = LEGACY_MATERIALS[((BLK_STAINED_CLAY) << 4) | (DATA_BLACK)];
    public static final Material RED_SANDSTONE = LEGACY_MATERIALS[(BLK_RED_SANDSTONE) << 4];
    public static final Material QUARTZ_ORE = LEGACY_MATERIALS[BLK_QUARTZ_ORE << 4];

    public static final Material TALL_GRASS = LEGACY_MATERIALS[((BLK_TALL_GRASS) << 4) | (DATA_TALL_GRASS)];
    public static final Material FERN = LEGACY_MATERIALS[((BLK_TALL_GRASS) << 4) | (DATA_FERN)];

    public static final Material WOOD_OAK = LEGACY_MATERIALS[((BLK_WOOD) << 4) | (DATA_OAK)];
    public static final Material WOOD_BIRCH = LEGACY_MATERIALS[((BLK_WOOD) << 4) | (DATA_BIRCH)];
    public static final Material WOOD_PINE = LEGACY_MATERIALS[((BLK_WOOD) << 4) | (DATA_PINE)];
    public static final Material WOOD_JUNGLE = LEGACY_MATERIALS[((BLK_WOOD) << 4) | (DATA_JUNGLE)];
    public static final Material WOOD_ACACIA = LEGACY_MATERIALS[((BLK_WOOD2) << 4) | (DATA_ACACIA)];
    public static final Material WOOD_DARK_OAK = LEGACY_MATERIALS[((BLK_WOOD2) << 4) | (DATA_DARK_OAK)];

    public static final Material LEAVES_OAK = LEGACY_MATERIALS[((BLK_LEAVES) << 4) | (DATA_OAK)];
    public static final Material LEAVES_BIRCH = LEGACY_MATERIALS[((BLK_LEAVES) << 4) | (DATA_BIRCH)];
    public static final Material LEAVES_PINE = LEGACY_MATERIALS[((BLK_LEAVES) << 4) | (DATA_PINE)];
    public static final Material LEAVES_JUNGLE = LEGACY_MATERIALS[((BLK_LEAVES) << 4) | (DATA_JUNGLE)];
    public static final Material LEAVES_ACACIA = LEGACY_MATERIALS[((BLK_LEAVES2) << 4) | (DATA_ACACIA)];
    public static final Material LEAVES_DARK_OAK = LEGACY_MATERIALS[((BLK_LEAVES2) << 4) | (DATA_DARK_OAK)];

    public static final Material WOODEN_PLANK_OAK = LEGACY_MATERIALS[((BLK_WOODEN_PLANK) << 4) | (DATA_OAK)];
    public static final Material WOODEN_PLANK_BIRCH = LEGACY_MATERIALS[((BLK_WOODEN_PLANK) << 4) | (DATA_BIRCH)];
    public static final Material WOODEN_PLANK_PINE = LEGACY_MATERIALS[((BLK_WOODEN_PLANK) << 4) | (DATA_PINE)];
    public static final Material WOODEN_PLANK_JUNGLE = LEGACY_MATERIALS[((BLK_WOODEN_PLANK) << 4) | (DATA_JUNGLE)];
    public static final Material WOODEN_PLANK_ACACIA = LEGACY_MATERIALS[((BLK_WOODEN_PLANK) << 4) | (4 + DATA_ACACIA)];
    public static final Material WOODEN_PLANK_DARK_WOOD = LEGACY_MATERIALS[((BLK_WOODEN_PLANK) << 4) | (4 + DATA_DARK_OAK)];

    public static final Material WOOL_WHITE = LEGACY_MATERIALS[((BLK_WOOL) << 4) | (DATA_WHITE)];
    public static final Material WOOL_ORANGE = LEGACY_MATERIALS[((BLK_WOOL) << 4) | (DATA_ORANGE)];
    public static final Material WOOL_MAGENTA = LEGACY_MATERIALS[((BLK_WOOL) << 4) | (DATA_MAGENTA)];
    public static final Material WOOL_LIGHT_BLUE = LEGACY_MATERIALS[((BLK_WOOL) << 4) | (DATA_LIGHT_BLUE)];
    public static final Material WOOL_YELLOW = LEGACY_MATERIALS[((BLK_WOOL) << 4) | (DATA_YELLOW)];
    public static final Material WOOL_LIME = LEGACY_MATERIALS[((BLK_WOOL) << 4) | (DATA_LIME)];
    public static final Material WOOL_PINK = LEGACY_MATERIALS[((BLK_WOOL) << 4) | (DATA_PINK)];
    public static final Material WOOL_GREY = LEGACY_MATERIALS[((BLK_WOOL) << 4) | (DATA_GREY)];
    public static final Material WOOL_LIGHT_GREY = LEGACY_MATERIALS[((BLK_WOOL) << 4) | (DATA_LIGHT_GREY)];
    public static final Material WOOL_CYAN = LEGACY_MATERIALS[((BLK_WOOL) << 4) | (DATA_CYAN)];
    public static final Material WOOL_PURPLE = LEGACY_MATERIALS[((BLK_WOOL) << 4) | (DATA_PURPLE)];
    public static final Material WOOL_BLUE = LEGACY_MATERIALS[((BLK_WOOL) << 4) | (DATA_BLUE)];
    public static final Material WOOL_BROWN = LEGACY_MATERIALS[((BLK_WOOL) << 4) | (DATA_BROWN)];
    public static final Material WOOL_GREEN = LEGACY_MATERIALS[((BLK_WOOL) << 4) | (DATA_GREEN)];
    public static final Material WOOL_RED = LEGACY_MATERIALS[((BLK_WOOL) << 4) | (DATA_RED)];
    public static final Material WOOL_BLACK = LEGACY_MATERIALS[((BLK_WOOL) << 4) | (DATA_BLACK)];

    public static final Material COBBLESTONE_SLAB = LEGACY_MATERIALS[((BLK_SLAB) << 4) | (DATA_SLAB_COBBLESTONE)];

    public static final Material DOOR_OPEN_LEFT_BOTTOM = LEGACY_MATERIALS[((BLK_WOODEN_DOOR) << 4) | (DATA_DOOR_BOTTOM | DATA_DOOR_BOTTOM_OPEN)];
    public static final Material DOOR_OPEN_LEFT_TOP = LEGACY_MATERIALS[((BLK_WOODEN_DOOR) << 4) | (DATA_DOOR_TOP | DATA_DOOR_TOP_HINGE_LEFT)];
    public static final Material DOOR_OPEN_RIGHT_BOTTOM = LEGACY_MATERIALS[((BLK_WOODEN_DOOR) << 4) | (DATA_DOOR_BOTTOM | DATA_DOOR_BOTTOM_OPEN)];
    public static final Material DOOR_OPEN_RIGHT_TOP = LEGACY_MATERIALS[((BLK_WOODEN_DOOR) << 4) | (DATA_DOOR_TOP | DATA_DOOR_TOP_HINGE_RIGHT)];
    public static final Material DOOR_CLOSED_LEFT_BOTTOM = LEGACY_MATERIALS[((BLK_WOODEN_DOOR) << 4) | (DATA_DOOR_BOTTOM | DATA_DOOR_BOTTOM_CLOSED)];
    public static final Material DOOR_CLOSED_LEFT_TOP = LEGACY_MATERIALS[((BLK_WOODEN_DOOR) << 4) | (DATA_DOOR_TOP | DATA_DOOR_TOP_HINGE_LEFT)];
    public static final Material DOOR_CLOSED_RIGHT_BOTTOM = LEGACY_MATERIALS[((BLK_WOODEN_DOOR) << 4) | (DATA_DOOR_BOTTOM | DATA_DOOR_BOTTOM_CLOSED)];
    public static final Material DOOR_CLOSED_RIGHT_TOP = LEGACY_MATERIALS[((BLK_WOODEN_DOOR) << 4) | (DATA_DOOR_TOP | DATA_DOOR_TOP_HINGE_RIGHT)];

    public static final Material BED_FOOT = LEGACY_MATERIALS[((BLK_BED) << 4) | (DATA_BED_FOOT)];
    public static final Material BED_HEAD = LEGACY_MATERIALS[((BLK_BED) << 4) | (DATA_BED_HEAD)];

    public static final Material COCOA_PLANT = LEGACY_MATERIALS[(BLK_COCOA_PLANT) << 4];
    public static final Material COCOA_PLANT_HALF_RIPE = LEGACY_MATERIALS[((BLK_COCOA_PLANT) << 4) | (0x4)];
    public static final Material COCOA_PLANT_RIPE = LEGACY_MATERIALS[((BLK_COCOA_PLANT) << 4) | (0x8)];

    public static final Material PUMPKIN_NO_FACE = LEGACY_MATERIALS[((BLK_PUMPKIN) << 4) | (DATA_PUMPKIN_NO_FACE)];
    public static final Material PUMPKIN_NORTH_FACE = LEGACY_MATERIALS[((BLK_PUMPKIN) << 4) | (DATA_PUMPKIN_NORTH_FACE)];
    public static final Material PUMPKIN_EAST_FACE = LEGACY_MATERIALS[((BLK_PUMPKIN) << 4) | (DATA_PUMPKIN_EAST_FACE)];
    public static final Material PUMPKIN_SOUTH_FACE = LEGACY_MATERIALS[((BLK_PUMPKIN) << 4) | (DATA_PUMPKIN_SOUTH_FACE)];
    public static final Material PUMPKIN_WEST_FACE = LEGACY_MATERIALS[((BLK_PUMPKIN) << 4) | (DATA_PUMPKIN_WEST_FACE)];

    // Modern materials (based on MC 1.13 block names and properties)

    /**
     * A vine with no directions turned on, which is not a valid block in
     * Minecraft, so you must set at least one direction.
     */
    public static final Material VINE = get(MC_VINE, MC_NORTH, false, MC_EAST, false, MC_SOUTH, false, MC_WEST, false, MC_UP, false);
    public static final Material TERRACOTTA = get(MC_TERRACOTTA);

    // MC 1.13 block property access helpers

    public static final Property<Boolean> SNOWY  = new Property<>(MC_SNOWY,  Boolean.class);
    public static final Property<Boolean> NORTH  = new Property<>(MC_NORTH,  Boolean.class);
    public static final Property<Boolean> EAST   = new Property<>(MC_EAST,   Boolean.class);
    public static final Property<Boolean> SOUTH  = new Property<>(MC_SOUTH,  Boolean.class);
    public static final Property<Boolean> WEST   = new Property<>(MC_WEST,   Boolean.class);
    public static final Property<Boolean> UP     = new Property<>(MC_UP,     Boolean.class);
    public static final Property<Integer> LAYERS = new Property<>(MC_LAYERS, Integer.class);

    // Namespaces

    public static final String MINECRAFT = "minecraft";

    // Material type categories

    public static final int CATEGORY_AIR           = 0;
    public static final int CATEGORY_FLUID         = 1;
    public static final int CATEGORY_INSUBSTANTIAL = 2;
    public static final int CATEGORY_MAN_MADE      = 3;
    public static final int CATEGORY_RESOURCE      = 4;
    public static final int CATEGORY_NATURAL_SOLID = 5;
    public static final int CATEGORY_UNKNOWN       = 6;

    private static final long serialVersionUID = 2011101001L;

    static final class Identity implements Serializable {
        Identity(String name, Map<String, String> properties) {
            if (name == null) {
                throw new NullPointerException();
            }
            this.name = name.intern();
            this.properties = (properties != null) ? ImmutableMap.copyOf(properties) : null;
        }

        @SuppressWarnings("StringEquality") // Interned string
        @Override
        public boolean equals(Object o) {
            return (o instanceof Identity)
                && name.equals(((Identity) o).name)
                && ((properties != null) ? properties.equals(((Identity) o).properties) : (((Identity) o).properties == null));
        }

        @Override
        public int hashCode() {
            return name.hashCode() * 37 + ((properties != null) ? properties.hashCode() : 0);
        }

        final String name;
        final Map<String, String> properties;

        private static final long serialVersionUID = 1L;
    }

    public static final class Property<T> {
        public Property(String name, Class<T> type) {
            this.name = name;
            this.type = type;
            Method method = null;
            if (type != String.class) {
                try {
                    method = type.getMethod("valueOf", String.class);
                } catch (NoSuchMethodException e) {
                    throw new IllegalArgumentException("Type " + type + " has no valueOf(String) method");
                }
            }
            valueOfMethod = method;
        }

        @SuppressWarnings("unchecked") // Responsibility of client
        public T fromString(String str) {
            if (valueOfMethod == null) {
                return (T) str;
            } else {
                try {
                    return (T) valueOfMethod.invoke(null, str);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e.getClass().getSimpleName() + " when trying to parse\"" + str + "\" to " + type, e);
                }
            }
        }

        public final String name;
        public final Class<T> type;
        private final Method valueOfMethod;
    }
}