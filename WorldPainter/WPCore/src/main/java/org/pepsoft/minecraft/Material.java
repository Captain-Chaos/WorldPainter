/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.minecraft;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ObjectStreamException;
import java.io.Serializable;

import static org.pepsoft.minecraft.Block.BLOCK_TYPE_NAMES;
import static org.pepsoft.minecraft.Constants.*;

/**
 * A representation of a Minecraft block ID and data value, or one possible in-
 * game block type. Implements the Enumeration pattern, meaning there is only
 * ever one instance of this class for each block ID and data value combination,
 * allowing use of the equals operator (==) for comparing instances.
 *
 * @author pepijn
 */
@SuppressWarnings("PointlessBitwiseExpression") // legibility
public final class Material implements Serializable, Comparable<Material> {
    private Material(int blockType, int data) {
        this.blockType = blockType;
        this.data = data;
        index = (blockType << 4) | data;
        block = Block.BLOCKS[blockType];
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
                        return MATERIALS[((blockType) << 4) | (data & 0x0C)];
                    case WEST:
                        return MATERIALS[((blockType) << 4) | ((data & 0x0C) | 1)];
                    case SOUTH:
                        return MATERIALS[((blockType) << 4) | ((data & 0x0C) | 2)];
                    case NORTH:
                        return MATERIALS[((blockType) << 4) | ((data & 0x0C) | 3)];
                }
                break;
            case BLK_TORCH:
            case BLK_REDSTONE_TORCH_OFF:
            case BLK_REDSTONE_TORCH_ON:
                switch (direction) {
                    case EAST:
                        return MATERIALS[((blockType) << 4) | (1)];
                    case WEST:
                        return MATERIALS[((blockType) << 4) | (2)];
                    case SOUTH:
                        return MATERIALS[((blockType) << 4) | (3)];
                    case NORTH:
                        return MATERIALS[((blockType) << 4) | (4)];
                }
                break;
            case BLK_RAILS:
                if (data < 2) {
                    // Straight
                    switch (direction) {
                        case NORTH:
                        case SOUTH:
                            return MATERIALS[((blockType) << 4) | (0)];
                        case EAST:
                        case WEST:
                            return MATERIALS[((blockType) << 4) | (1)];
                    }
                } else {
                    // Sloped or round
                    boolean round = data > 5;
                    switch (direction) {
                        case EAST:
                            return MATERIALS[((blockType) << 4) | (round ? 6 : 2)];
                        case WEST:
                            return MATERIALS[((blockType) << 4) | (round ? 7 : 3)];
                        case NORTH:
                            return MATERIALS[((blockType) << 4) | (round ? 8 : 4)];
                        case SOUTH:
                            return MATERIALS[((blockType) << 4) | (round ? 9 : 5)];
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
                            return MATERIALS[((blockType) << 4) | (data & 0x08)];
                        case EAST:
                        case WEST:
                            return MATERIALS[((blockType) << 4) | ((data & 0x08) | 1)];
                    }
                } else {
                    // Sloped
                    switch (direction) {
                        case EAST:
                            return MATERIALS[((blockType) << 4) | ((data & 0x08) | 2)];
                        case WEST:
                            return MATERIALS[((blockType) << 4) | ((data & 0x08) | 3)];
                        case NORTH:
                            return MATERIALS[((blockType) << 4) | ((data & 0x08) | 4)];
                        case SOUTH:
                            return MATERIALS[((blockType) << 4) | ((data & 0x08) | 5)];
                    }
                }
                break;
            case BLK_LEVER:
                boolean ground = (data & 0x07) > 4;
                switch (direction) {
                    case EAST:
                        return MATERIALS[((blockType) << 4) | ((data & 0x08) | (ground ? 6 : 1))];
                    case WEST:
                        return MATERIALS[((blockType) << 4) | ((data & 0x08) | (ground ? 6 : 2))];
                    case SOUTH:
                        return MATERIALS[((blockType) << 4) | ((data & 0x08) | (ground ? 5 : 3))];
                    case NORTH:
                        return MATERIALS[((blockType) << 4) | ((data & 0x08) | (ground ? 5 : 4))];
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
                            return MATERIALS[((blockType) << 4) | (data & 0xC)];
                        case NORTH:
                            return MATERIALS[((blockType) << 4) | ((data & 0xC) | 1)];
                        case EAST:
                            return MATERIALS[((blockType) << 4) | ((data & 0xC) | 2)];
                        case SOUTH:
                            return MATERIALS[((blockType) << 4) | ((data & 0xC) | 3)];
                    }
                }
                break;
            case BLK_STONE_BUTTON:
            case BLK_WOODEN_BUTTON:
                switch (direction) {
                    case EAST:
                        return MATERIALS[((blockType) << 4) | ((data & 0x8) | 1)];
                    case WEST:
                        return MATERIALS[((blockType) << 4) | ((data & 0x8) | 2)];
                    case SOUTH:
                        return MATERIALS[((blockType) << 4) | ((data & 0x8) | 3)];
                    case NORTH:
                        return MATERIALS[((blockType) << 4) | ((data & 0x8) | 4)];
                }
                break;
            case BLK_SIGN:
            case BLK_STANDING_BANNER:
                switch (direction) {
                    case SOUTH:
                        return MATERIALS[((blockType) << 4) | (data & 0x03)];
                    case WEST:
                        return MATERIALS[((blockType) << 4) | ((data & 0x03) | 4)];
                    case NORTH:
                        return MATERIALS[((blockType) << 4) | ((data & 0x03) | 8)];
                    case EAST:
                        return MATERIALS[((blockType) << 4) | ((data & 0x03) | 12)];
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
                        return MATERIALS[((blockType) << 4) | 2];
                    case SOUTH:
                        return MATERIALS[((blockType) << 4) | 3];
                    case WEST:
                        return MATERIALS[((blockType) << 4) | 4];
                    case EAST:
                        return MATERIALS[((blockType) << 4) | 5];
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
                        return MATERIALS[((blockType) << 4) | (data & 0x0C)];
                    case WEST:
                        return MATERIALS[((blockType) << 4) | ((data & 0x0C) | 1)];
                    case NORTH:
                        return MATERIALS[((blockType) << 4) | ((data & 0x0C) | 2)];
                    case EAST:
                        return MATERIALS[((blockType) << 4) | ((data & 0x0C) | 3)];
                }
                break;
            case BLK_REDSTONE_REPEATER_OFF:
            case BLK_REDSTONE_REPEATER_ON:
            case BLK_REDSTONE_COMPARATOR_UNPOWERED:
                switch (direction) {
                    case NORTH:
                        return MATERIALS[((blockType) << 4) | (data & 0x0C)];
                    case EAST:
                        return MATERIALS[((blockType) << 4) | ((data & 0x0C) | 1)];
                    case SOUTH:
                        return MATERIALS[((blockType) << 4) | ((data & 0x0C) | 2)];
                    case WEST:
                        return MATERIALS[((blockType) << 4) | ((data & 0x0C) | 3)];
                }
                break;
            case BLK_TRAPDOOR:
            case BLK_IRON_TRAPDOOR: // TODO: assumption
                switch (direction) {
                    case SOUTH:
                        return MATERIALS[((blockType) << 4) | (data & 0x0C)];
                    case NORTH:
                        return MATERIALS[((blockType) << 4) | ((data & 0x0C) | 1)];
                    case EAST:
                        return MATERIALS[((blockType) << 4) | ((data & 0x0C) | 2)];
                    case WEST:
                        return MATERIALS[((blockType) << 4) | ((data & 0x0C) | 3)];
                }
                break;
            case BLK_PISTON:
            case BLK_PISTON_HEAD:
                switch (direction) {
                    case NORTH:
                        return MATERIALS[((blockType) << 4) | ((data & 0x08) | 2)];
                    case SOUTH:
                        return MATERIALS[((blockType) << 4) | ((data & 0x08) | 3)];
                    case WEST:
                        return MATERIALS[((blockType) << 4) | ((data & 0x08) | 4)];
                    case EAST:
                        return MATERIALS[((blockType) << 4) | ((data & 0x08) | 5)];
                }
                break;
            case BLK_WOOD:
            case BLK_WOOD2:
            case BLK_BONE_BLOCK: // TODO: assumption!
                switch (direction) {
                    case NORTH:
                    case SOUTH:
                        return MATERIALS[((blockType) << 4) | ((data & 0x3) | 0x8)];
                    case EAST:
                    case WEST:
                        return MATERIALS[((blockType) << 4) | ((data & 0x3) | 0x4)];
                }
                break;
            case BLK_COCOA_PLANT:
                switch (direction) {
                    case SOUTH:
                        return MATERIALS[((blockType) << 4) | (data & 0xC)];
                    case WEST:
                        return MATERIALS[((blockType) << 4) | ((data & 0xC) | 0x1)];
                    case NORTH:
                        return MATERIALS[((blockType) << 4) | ((data & 0xC) | 0x2)];
                    case EAST:
                        return MATERIALS[((blockType) << 4) | ((data & 0xC) | 0x3)];
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
                    return MATERIALS[((blockType) << 4) | (((bitMask << steps) & 0xF0) >> 4)];
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
                        return MATERIALS[((blockType) << 4) | (corner ? 1 : 2)];
                    case EAST:
                        return MATERIALS[((blockType) << 4) | (corner ? 3 : 6)];
                    case SOUTH:
                        return MATERIALS[((blockType) << 4) | (corner ? 9 : 8)];
                    case WEST:
                        return MATERIALS[((blockType) << 4) | (corner ? 7 : 4)];
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
            return MATERIALS[((blockType) << 4) | ((north ? 4 : 0)
                    | (east  ? 8 : 0)
                    | (south ? 1 : 0)
                    | (west  ? 2 : 0))];
        } else if ((blockType == BLK_HUGE_BROWN_MUSHROOM) || (blockType == BLK_HUGE_RED_MUSHROOM)) {
            if ((axis == Direction.EAST) || (axis == Direction.WEST)) {
            // TODO: this is wrong. Probably a leftover from the coordinate shift. It should be:
//            if ((axis == Direction.NORTH) || (axis == Direction.SOUTH)) {
                switch (data) {
                    case 1:
                        return MATERIALS[((blockType) << 4) | (3)];
                    case 3:
                        return MATERIALS[((blockType) << 4) | (1)];
                    case 4:
                        return MATERIALS[((blockType) << 4) | (6)];
                    case 6:
                        return MATERIALS[((blockType) << 4) | (4)];
                    case 7:
                        return MATERIALS[((blockType) << 4) | (9)];
                    case 9:
                        return MATERIALS[((blockType) << 4) | (7)];
                    default:
                        return this;
                }
            } else {
                switch (data) {
                    case 1:
                        return MATERIALS[((blockType) << 4) | (7)];
                    case 2:
                        return MATERIALS[((blockType) << 4) | (8)];
                    case 3:
                        return MATERIALS[((blockType) << 4) | (9)];
                    case 7:
                        return MATERIALS[((blockType) << 4) | (1)];
                    case 8:
                        return MATERIALS[((blockType) << 4) | (2)];
                    case 9:
                        return MATERIALS[((blockType) << 4) | (3)];
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
                        return MATERIALS[((blockType) << 4) | (15)];
                    case 2:
                        return MATERIALS[((blockType) << 4) | (14)];
                    case 3:
                        return MATERIALS[((blockType) << 4) | (13)];
                    case 4:
                        return MATERIALS[((blockType) << 4) | (12)];
                    case 5:
                        return MATERIALS[((blockType) << 4) | (11)];
                    case 6:
                        return MATERIALS[((blockType) << 4) | (10)];
                    case 7:
                        return MATERIALS[((blockType) << 4) | (9)];
                    case 9:
                        return MATERIALS[((blockType) << 4) | (7)];
                    case 10:
                        return MATERIALS[((blockType) << 4) | (6)];
                    case 11:
                        return MATERIALS[((blockType) << 4) | (5)];
                    case 12:
                        return MATERIALS[((blockType) << 4) | (4)];
                    case 13:
                        return MATERIALS[((blockType) << 4) | (3)];
                    case 14:
                        return MATERIALS[((blockType) << 4) | (2)];
                    case 15:
                        return MATERIALS[((blockType) << 4) | (1)];
                    default:
                        return this;
                }
            } else {
                switch (data) {
                    case 0:
                        return MATERIALS[((blockType) << 4) | (8)];
                    case 1:
                        return MATERIALS[((blockType) << 4) | (7)];
                    case 2:
                        return MATERIALS[((blockType) << 4) | (6)];
                    case 3:
                        return MATERIALS[((blockType) << 4) | (5)];
                    case 5:
                        return MATERIALS[((blockType) << 4) | (3)];
                    case 6:
                        return MATERIALS[((blockType) << 4) | (2)];
                    case 7:
                        return MATERIALS[((blockType) << 4) | (1)];
                    case 8:
                        return MATERIALS[((blockType) << 4) | (0)];
                    case 9:
                        return MATERIALS[((blockType) << 4) | (15)];
                    case 10:
                        return MATERIALS[((blockType) << 4) | (14)];
                    case 11:
                        return MATERIALS[((blockType) << 4) | (13)];
                    case 13:
                        return MATERIALS[((blockType) << 4) | (11)];
                    case 14:
                        return MATERIALS[((blockType) << 4) | (10)];
                    case 15:
                        return MATERIALS[((blockType) << 4) | (9)];
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
                return MATERIALS[((blockType) << 4) | (data ^ 0x4)];
            case BLK_SLAB:
            case BLK_RED_SANDSTONE_SLAB:
            case BLK_WOODEN_SLAB:
            case BLK_PURPUR_SLAB:
                return MATERIALS[((blockType) << 4) | (data ^ 0x8)];
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
        return (blockType < 256) && (TEXTURE_OFFSETS[blockType] != 0);
    }
    
    /**
     * Gets the relevant image for this material from the specified Minecraft
     * texture pack terrain image.
     */
    public BufferedImage getImage(BufferedImage terrain) {
        if ((blockType < 256) && (TEXTURE_OFFSETS[blockType] != 0)) {
            int imageX = (TEXTURE_OFFSETS[blockType] % 16) * 16;
            int imageY = (TEXTURE_OFFSETS[blockType] / 16) * 16;
            return terrain.getSubimage(imageX, imageY, 16, 16);
        } else {
            return null;
        }
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
        if ((blockType < 256) && (TEXTURE_OFFSETS[blockType] != 0)) {
            int imageX = (TEXTURE_OFFSETS[blockType] % 16) * 16;
            int imageY = (TEXTURE_OFFSETS[blockType] / 16) * 16;
            g2.drawImage(terrain, x, y, x + 16, y + 16, imageX, imageY, imageX + 16, imageY + 16, null);
        }
    }
    
    public static Material get(int blockType) {
        return MATERIALS[(blockType) << 4];
    }
    
    public static Material get(int blockType, int data) {
        return MATERIALS[(blockType << 4) | data];
    }

    public static Material get(Block blockType) {
        return MATERIALS[blockType.id << 4];
    }

    public static Material get(Block blockType, int data) {
        return MATERIALS[(blockType.id << 4) | data];
    }

    /**
     * Get the material corresponding to a combined index consisting of the
     * block ID shifted left four bits and or-ed with the data value. In other
     * words the index is a 16-bit unsigned integer, with bit 0-3 indicating
     * the data value and bit 4-15 indicating the block ID.
     *
     * @param index The combined index of the material to get.
     * @return The indicated material.
     */
    public static Material getByCombinedIndex(int index) {
        return MATERIALS[index];
    }

    @Override
    public String toString() {
        if ((blockType < BLOCK_TYPE_NAMES.length) && (BLOCK_TYPE_NAMES[blockType] != null)) {
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

    // Comparable
    
    @Override
    public int compareTo(Material o) {
        if (blockType != o.blockType) {
            return blockType - o.blockType;
        } else {
            return data - o.data;
        }
    }
    
    private Object readResolve() throws ObjectStreamException {
        return MATERIALS[(blockType << 4) | data];
    }
    
    public final int blockType, data;
    public final transient int index;
    public final transient Block block;
    
    private static final Material[] MATERIALS = new Material[65536];
    
    static {
        for (int i = 0; i < 65536; i++) {
            MATERIALS[i] = new Material(i >> 4, i & 0xF);
        }
    }

    public static final Material AIR = MATERIALS[(BLK_AIR) << 4];
    public static final Material DANDELION = MATERIALS[(BLK_DANDELION) << 4];
    public static final Material ROSE = MATERIALS[(BLK_ROSE) << 4];
    public static final Material GRASS = MATERIALS[(BLK_GRASS) << 4];
    public static final Material DIRT = MATERIALS[(BLK_DIRT) << 4];
    public static final Material STONE = MATERIALS[(BLK_STONE) << 4];
    public static final Material GRANITE = MATERIALS[((BLK_STONE) << 4) | (DATA_STONE_GRANITE)];
    public static final Material DIORITE = MATERIALS[((BLK_STONE) << 4) | (DATA_STONE_DIORITE)];
    public static final Material ANDESITE = MATERIALS[((BLK_STONE) << 4) | (DATA_STONE_ANDESITE)];
    public static final Material COBBLESTONE = MATERIALS[(BLK_COBBLESTONE) << 4];
    public static final Material SNOW = MATERIALS[(BLK_SNOW) << 4];
    public static final Material SNOW_EIGHT_LAYERS = MATERIALS[((BLK_SNOW) << 4) | 7];
    public static final Material DEAD_SHRUBS = MATERIALS[(BLK_DEAD_SHRUBS) << 4];
    public static final Material CACTUS = MATERIALS[(BLK_CACTUS) << 4];
    public static final Material SAND = MATERIALS[(BLK_SAND) << 4];
    public static final Material FIRE = MATERIALS[(BLK_FIRE) << 4];
    public static final Material GLOWSTONE = MATERIALS[(BLK_GLOWSTONE) << 4];
    public static final Material SOUL_SAND = MATERIALS[(BLK_SOUL_SAND) << 4];
    public static final Material LAVA = MATERIALS[(BLK_LAVA) << 4];
    public static final Material NETHERRACK = MATERIALS[(BLK_NETHERRACK) << 4];
    public static final Material COAL = MATERIALS[(BLK_COAL) << 4];
    public static final Material GRAVEL = MATERIALS[(BLK_GRAVEL) << 4];
    public static final Material REDSTONE_ORE = MATERIALS[(BLK_REDSTONE_ORE) << 4];
    public static final Material IRON_ORE = MATERIALS[(BLK_IRON_ORE) << 4];
    public static final Material WATER = MATERIALS[(BLK_WATER) << 4];
    public static final Material GOLD_ORE = MATERIALS[(BLK_GOLD_ORE) << 4];
    public static final Material LAPIS_LAZULI_ORE = MATERIALS[(BLK_LAPIS_LAZULI_ORE) << 4];
    public static final Material DIAMOND_ORE = MATERIALS[(BLK_DIAMOND_ORE) << 4];
    public static final Material BEDROCK = MATERIALS[(BLK_BEDROCK) << 4];
    public static final Material STATIONARY_WATER = MATERIALS[(BLK_STATIONARY_WATER) << 4];
    public static final Material STATIONARY_LAVA = MATERIALS[(BLK_STATIONARY_LAVA) << 4];
    public static final Material SNOW_BLOCK = MATERIALS[(BLK_SNOW_BLOCK) << 4];
    public static final Material SANDSTONE = MATERIALS[(BLK_SANDSTONE) << 4];
    public static final Material CLAY = MATERIALS[(BLK_CLAY) << 4];
    public static final Material MOSSY_COBBLESTONE = MATERIALS[(BLK_MOSSY_COBBLESTONE) << 4];
    public static final Material OBSIDIAN = MATERIALS[(BLK_OBSIDIAN) << 4];
    public static final Material FENCE = MATERIALS[(BLK_FENCE) << 4];
    public static final Material GLASS_PANE = MATERIALS[(BLK_GLASS_PANE) << 4];
    public static final Material STONE_BRICKS = MATERIALS[(BLK_STONE_BRICKS) << 4];
    public static final Material BRICKS = MATERIALS[(BLK_BRICKS) << 4];
    public static final Material COBWEB = MATERIALS[(BLK_COBWEB) << 4];
    public static final Material DIAMOND_BLOCK = MATERIALS[(BLK_DIAMOND_BLOCK) << 4];
    public static final Material GOLD_BLOCK = MATERIALS[(BLK_GOLD_BLOCK) << 4];
    public static final Material IRON_BLOCK = MATERIALS[(BLK_IRON_BLOCK) << 4];
    public static final Material LAPIS_LAZULI_BLOCK = MATERIALS[(BLK_LAPIS_LAZULI_BLOCK) << 4];
    public static final Material MYCELIUM = MATERIALS[(BLK_MYCELIUM) << 4];
    public static final Material TILLED_DIRT = MATERIALS[(BLK_TILLED_DIRT) << 4];
    public static final Material ICE = MATERIALS[(BLK_ICE) << 4];
    public static final Material TORCH = MATERIALS[(BLK_TORCH) << 4];
    public static final Material COBBLESTONE_STAIRS = MATERIALS[(BLK_COBBLESTONE_STAIRS) << 4];
    public static final Material GLASS = MATERIALS[(BLK_GLASS) << 4];
    public static final Material WOODEN_STAIRS = MATERIALS[(BLK_WOODEN_STAIRS) << 4];
    public static final Material CHEST_NORTH = MATERIALS[((BLK_CHEST) << 4) | (2)];
    public static final Material CHEST_SOUTH = MATERIALS[((BLK_CHEST) << 4) | (3)];
    public static final Material CHEST_WEST = MATERIALS[((BLK_CHEST) << 4) | (4)];
    public static final Material CHEST_EAST = MATERIALS[((BLK_CHEST) << 4) | (5)];
    public static final Material WALL_SIGN = MATERIALS[(BLK_WALL_SIGN) << 4];
    public static final Material BRICK_STAIRS = MATERIALS[(BLK_BRICK_STAIRS) << 4];
    public static final Material STONE_BRICK_STAIRS = MATERIALS[(BLK_STONE_BRICK_STAIRS) << 4];
    public static final Material LADDER = MATERIALS[(BLK_LADDER) << 4];
    public static final Material TRAPDOOR = MATERIALS[(BLK_TRAPDOOR) << 4];
    public static final Material WHEAT = MATERIALS[(BLK_WHEAT) << 4];
    public static final Material LILY_PAD = MATERIALS[(BLK_LILY_PAD) << 4];
    public static final Material RED_MUSHROOM = MATERIALS[(BLK_RED_MUSHROOM) << 4];
    public static final Material BROWN_MUSHROOM = MATERIALS[(BLK_BROWN_MUSHROOM) << 4];
    public static final Material SUGAR_CANE = MATERIALS[(BLK_SUGAR_CANE) << 4];
    public static final Material EMERALD_ORE = MATERIALS[(BLK_EMERALD_ORE) << 4];
    public static final Material EMERALD_BLOCK = MATERIALS[(BLK_EMERALD_BLOCK) << 4];
    public static final Material PERMADIRT = MATERIALS[((BLK_DIRT) << 4) | (1)];
    public static final Material PODZOL = MATERIALS[((BLK_DIRT) << 4) | (2)];
    public static final Material RED_SAND = MATERIALS[((BLK_SAND) << 4) | (1)];
    public static final Material HARDENED_CLAY = MATERIALS[(BLK_HARDENED_CLAY) << 4];
    public static final Material WHITE_CLAY = MATERIALS[(BLK_STAINED_CLAY) << 4];
    public static final Material ORANGE_CLAY = MATERIALS[((BLK_STAINED_CLAY) << 4) | (DATA_ORANGE)];
    public static final Material MAGENTA_CLAY = MATERIALS[((BLK_STAINED_CLAY) << 4) | (DATA_MAGENTA)];
    public static final Material LIGHT_BLUE_CLAY = MATERIALS[((BLK_STAINED_CLAY) << 4) | (DATA_LIGHT_BLUE)];
    public static final Material YELLOW_CLAY = MATERIALS[((BLK_STAINED_CLAY) << 4) | (DATA_YELLOW)];
    public static final Material LIME_CLAY = MATERIALS[((BLK_STAINED_CLAY) << 4) | (DATA_LIME)];
    public static final Material PINK_CLAY = MATERIALS[((BLK_STAINED_CLAY) << 4) | (DATA_PINK)];
    public static final Material GREY_CLAY = MATERIALS[((BLK_STAINED_CLAY) << 4) | (DATA_GREY)];
    public static final Material LIGHT_GREY_CLAY = MATERIALS[((BLK_STAINED_CLAY) << 4) | (DATA_LIGHT_GREY)];
    public static final Material CYAN_CLAY = MATERIALS[((BLK_STAINED_CLAY) << 4) | (DATA_CYAN)];
    public static final Material PURPLE_CLAY = MATERIALS[((BLK_STAINED_CLAY) << 4) | (DATA_PURPLE)];
    public static final Material BLUE_CLAY = MATERIALS[((BLK_STAINED_CLAY) << 4) | (DATA_BLUE)];
    public static final Material BROWN_CLAY = MATERIALS[((BLK_STAINED_CLAY) << 4) | (DATA_BROWN)];
    public static final Material GREEN_CLAY = MATERIALS[((BLK_STAINED_CLAY) << 4) | (DATA_GREEN)];
    public static final Material RED_CLAY = MATERIALS[((BLK_STAINED_CLAY) << 4) | (DATA_RED)];
    public static final Material BLACK_CLAY = MATERIALS[((BLK_STAINED_CLAY) << 4) | (DATA_BLACK)];
    public static final Material RED_SANDSTONE = MATERIALS[(BLK_RED_SANDSTONE) << 4];

    public static final Material TALL_GRASS = MATERIALS[((BLK_TALL_GRASS) << 4) | (DATA_TALL_GRASS)];
    public static final Material FERN = MATERIALS[((BLK_TALL_GRASS) << 4) | (DATA_FERN)];

    public static final Material WOOD_OAK = MATERIALS[((BLK_WOOD) << 4) | (DATA_OAK)];
    public static final Material WOOD_BIRCH = MATERIALS[((BLK_WOOD) << 4) | (DATA_BIRCH)];
    public static final Material WOOD_PINE = MATERIALS[((BLK_WOOD) << 4) | (DATA_PINE)];
    public static final Material WOOD_JUNGLE = MATERIALS[((BLK_WOOD) << 4) | (DATA_JUNGLE)];
    public static final Material WOOD_ACACIA = MATERIALS[((BLK_WOOD2) << 4) | (DATA_ACACIA)];
    public static final Material WOOD_DARK_OAK = MATERIALS[((BLK_WOOD2) << 4) | (DATA_DARK_OAK)];

    public static final Material LEAVES_OAK = MATERIALS[((BLK_LEAVES) << 4) | (DATA_OAK)];
    public static final Material LEAVES_BIRCH = MATERIALS[((BLK_LEAVES) << 4) | (DATA_BIRCH)];
    public static final Material LEAVES_PINE = MATERIALS[((BLK_LEAVES) << 4) | (DATA_PINE)];
    public static final Material LEAVES_JUNGLE = MATERIALS[((BLK_LEAVES) << 4) | (DATA_JUNGLE)];
    public static final Material LEAVES_ACACIA = MATERIALS[((BLK_LEAVES2) << 4) | (DATA_ACACIA)];
    public static final Material LEAVES_DARK_OAK = MATERIALS[((BLK_LEAVES2) << 4) | (DATA_DARK_OAK)];

    public static final Material WOODEN_PLANK_OAK = MATERIALS[((BLK_WOODEN_PLANK) << 4) | (DATA_OAK)];
    public static final Material WOODEN_PLANK_BIRCH = MATERIALS[((BLK_WOODEN_PLANK) << 4) | (DATA_BIRCH)];
    public static final Material WOODEN_PLANK_PINE = MATERIALS[((BLK_WOODEN_PLANK) << 4) | (DATA_PINE)];
    public static final Material WOODEN_PLANK_JUNGLE = MATERIALS[((BLK_WOODEN_PLANK) << 4) | (DATA_JUNGLE)];
    public static final Material WOODEN_PLANK_ACACIA = MATERIALS[((BLK_WOODEN_PLANK) << 4) | (4 + DATA_ACACIA)];
    public static final Material WOODEN_PLANK_DARK_WOOD = MATERIALS[((BLK_WOODEN_PLANK) << 4) | (4 + DATA_DARK_OAK)];

    public static final Material WOOL_WHITE = MATERIALS[((BLK_WOOL) << 4) | (DATA_WHITE)];
    public static final Material WOOL_ORANGE = MATERIALS[((BLK_WOOL) << 4) | (DATA_ORANGE)];
    public static final Material WOOL_MAGENTA = MATERIALS[((BLK_WOOL) << 4) | (DATA_MAGENTA)];
    public static final Material WOOL_LIGHT_BLUE = MATERIALS[((BLK_WOOL) << 4) | (DATA_LIGHT_BLUE)];
    public static final Material WOOL_YELLOW = MATERIALS[((BLK_WOOL) << 4) | (DATA_YELLOW)];
    public static final Material WOOL_LIME = MATERIALS[((BLK_WOOL) << 4) | (DATA_LIME)];
    public static final Material WOOL_PINK = MATERIALS[((BLK_WOOL) << 4) | (DATA_PINK)];
    public static final Material WOOL_GREY = MATERIALS[((BLK_WOOL) << 4) | (DATA_GREY)];
    public static final Material WOOL_LIGHT_GREY = MATERIALS[((BLK_WOOL) << 4) | (DATA_LIGHT_GREY)];
    public static final Material WOOL_CYAN = MATERIALS[((BLK_WOOL) << 4) | (DATA_CYAN)];
    public static final Material WOOL_PURPLE = MATERIALS[((BLK_WOOL) << 4) | (DATA_PURPLE)];
    public static final Material WOOL_BLUE = MATERIALS[((BLK_WOOL) << 4) | (DATA_BLUE)];
    public static final Material WOOL_BROWN = MATERIALS[((BLK_WOOL) << 4) | (DATA_BROWN)];
    public static final Material WOOL_GREEN = MATERIALS[((BLK_WOOL) << 4) | (DATA_GREEN)];
    public static final Material WOOL_RED = MATERIALS[((BLK_WOOL) << 4) | (DATA_RED)];
    public static final Material WOOL_BLACK = MATERIALS[((BLK_WOOL) << 4) | (DATA_BLACK)];

    public static final Material COBBLESTONE_SLAB = MATERIALS[((BLK_SLAB) << 4) | (DATA_SLAB_COBBLESTONE)];

    public static final Material DOOR_OPEN_LEFT_BOTTOM = MATERIALS[((BLK_WOODEN_DOOR) << 4) | (DATA_DOOR_BOTTOM | DATA_DOOR_BOTTOM_OPEN)];
    public static final Material DOOR_OPEN_LEFT_TOP = MATERIALS[((BLK_WOODEN_DOOR) << 4) | (DATA_DOOR_TOP | DATA_DOOR_TOP_HINGE_LEFT)];
    public static final Material DOOR_OPEN_RIGHT_BOTTOM = MATERIALS[((BLK_WOODEN_DOOR) << 4) | (DATA_DOOR_BOTTOM | DATA_DOOR_BOTTOM_OPEN)];
    public static final Material DOOR_OPEN_RIGHT_TOP = MATERIALS[((BLK_WOODEN_DOOR) << 4) | (DATA_DOOR_TOP | DATA_DOOR_TOP_HINGE_RIGHT)];
    public static final Material DOOR_CLOSED_LEFT_BOTTOM = MATERIALS[((BLK_WOODEN_DOOR) << 4) | (DATA_DOOR_BOTTOM | DATA_DOOR_BOTTOM_CLOSED)];
    public static final Material DOOR_CLOSED_LEFT_TOP = MATERIALS[((BLK_WOODEN_DOOR) << 4) | (DATA_DOOR_TOP | DATA_DOOR_TOP_HINGE_LEFT)];
    public static final Material DOOR_CLOSED_RIGHT_BOTTOM = MATERIALS[((BLK_WOODEN_DOOR) << 4) | (DATA_DOOR_BOTTOM | DATA_DOOR_BOTTOM_CLOSED)];
    public static final Material DOOR_CLOSED_RIGHT_TOP = MATERIALS[((BLK_WOODEN_DOOR) << 4) | (DATA_DOOR_TOP | DATA_DOOR_TOP_HINGE_RIGHT)];

    public static final Material BED_FOOT = MATERIALS[((BLK_BED) << 4) | (DATA_BED_FOOT)];
    public static final Material BED_HEAD = MATERIALS[((BLK_BED) << 4) | (DATA_BED_HEAD)];

    public static final Material COCOA_PLANT = MATERIALS[(BLK_COCOA_PLANT) << 4];
    public static final Material COCOA_PLANT_HALF_RIPE = MATERIALS[((BLK_COCOA_PLANT) << 4) | (0x4)];
    public static final Material COCOA_PLANT_RIPE = MATERIALS[((BLK_COCOA_PLANT) << 4) | (0x8)];

    public static final Material PUMPKIN_NO_FACE = MATERIALS[((BLK_PUMPKIN) << 4) | (DATA_PUMPKIN_NO_FACE)];
    public static final Material PUMPKIN_NORTH_FACE = MATERIALS[((BLK_PUMPKIN) << 4) | (DATA_PUMPKIN_NORTH_FACE)];
    public static final Material PUMPKIN_EAST_FACE = MATERIALS[((BLK_PUMPKIN) << 4) | (DATA_PUMPKIN_EAST_FACE)];
    public static final Material PUMPKIN_SOUTH_FACE = MATERIALS[((BLK_PUMPKIN) << 4) | (DATA_PUMPKIN_SOUTH_FACE)];
    public static final Material PUMPKIN_WEST_FACE = MATERIALS[((BLK_PUMPKIN) << 4) | (DATA_PUMPKIN_WEST_FACE)];

    private static final int[] REVERSE_TEXTURE_OFFSETS = {
        0, BLK_STONE, BLK_DIRT, BLK_GRASS, BLK_WOODEN_PLANK, BLK_DOUBLE_SLAB, 0, BLK_BRICKS, BLK_TNT, 0, 0, BLK_COBWEB, BLK_ROSE, BLK_DANDELION, 0, BLK_SAPLING,
        BLK_COBBLESTONE, BLK_BEDROCK, BLK_SAND, BLK_GRAVEL, BLK_WOOD, 0, BLK_IRON_BLOCK, BLK_GOLD_BLOCK, BLK_DIAMOND_BLOCK, 0, 0, BLK_CHEST, BLK_RED_MUSHROOM, BLK_BROWN_MUSHROOM, 0, BLK_FIRE,
        BLK_GOLD_ORE, BLK_IRON_ORE, BLK_COAL, BLK_BOOKCASE, BLK_MOSSY_COBBLESTONE, BLK_OBSIDIAN, 0, BLK_TALL_GRASS, 0, 0, 0, 0, BLK_FURNACE, 0, BLK_DISPENSER, 0,
        BLK_SPONGE, BLK_GLASS, BLK_DIAMOND_ORE, BLK_REDSTONE_ORE, 0, BLK_LEAVES, BLK_STONE_BRICKS, BLK_DEAD_SHRUBS, 0, 0, 0, 0, BLK_CRAFTING_TABLE, 0, 0, 0,
        BLK_WOOL, BLK_MONSTER_SPAWNER, BLK_SNOW_BLOCK, BLK_ICE, BLK_SNOW, 0, BLK_CACTUS, 0, 0, BLK_SUGAR_CANE, BLK_NOTE_BLOCK, BLK_JUKEBOX, BLK_LILY_PAD, BLK_MYCELIUM, 0, 0,
        BLK_TORCH, BLK_WOODEN_DOOR, BLK_IRON_DOOR, BLK_LADDER, BLK_TRAPDOOR, BLK_IRON_BARS, 0, 0, 0, 0, 0, 0, 0, 0, 0, BLK_WHEAT,
        BLK_LEVER, 0, 0, BLK_REDSTONE_TORCH_ON, 0, 0, 0, BLK_NETHERRACK, BLK_SOUL_SAND, BLK_GLOWSTONE, 0, BLK_PISTON_HEAD, BLK_PISTON, 0, 0, BLK_PUMPKIN_STEM,
        0, 0, 0, BLK_REDSTONE_TORCH_OFF, 0, 0, 0, BLK_PUMPKIN, BLK_JACK_O_LANTERN, BLK_CAKE, 0, 0, 0, BLK_HUGE_RED_MUSHROOM, BLK_HUGE_BROWN_MUSHROOM, 0,
        BLK_RAILS, 0, 0, 0, 0, 0, 0, 0, BLK_MELON, 0, 0, 0, 0, 0, 0, BLK_VINES,
        BLK_LAPIS_LAZULI_BLOCK, 0, 0, 0, 0, BLK_BED, 0, 0, 0, 0, BLK_CAULDRON, 0, 0, BLK_BREWING_STAND, 0, BLK_END_PORTAL_FRAME,
        BLK_LAPIS_LAZULI_ORE, 0, 0, 0, BLK_REDSTONE_WIRE, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, BLK_END_STONE,
        0, 0, 0, BLK_POWERED_RAILS, 0, 0, BLK_ENCHANTMENT_TABLE, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        BLK_SANDSTONE, 0, 0, BLK_DETECTOR_RAILS, 0, 0, 0, 0, 0, 0, 0, 0, 0, BLK_WATER, BLK_STATIONARY_WATER, 0,
        0, 0, 0, BLK_REDSTONE_LANTERN_OFF, BLK_REDSTONE_LANTERN_ON, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        BLK_NETHER_BRICK, 0, 0, 0, BLK_NETHER_WART, 0, 0, 0, 0, 0, 0, 0, 0, BLK_LAVA, BLK_STATIONARY_LAVA, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    };
    private static final int[] TEXTURE_OFFSETS = new int[256];
    
    static {
        for (int i = 0; i < 256; i++) {
            int blockType = REVERSE_TEXTURE_OFFSETS[i];
            if (blockType != 0) {
                TEXTURE_OFFSETS[blockType] = i;
                
                // Patches
                if (blockType == BLK_GLASS) {
                    TEXTURE_OFFSETS[BLK_GLASS_PANE] = i;
                } else if (blockType == BLK_BRICKS) {
                    TEXTURE_OFFSETS[BLK_BRICK_STAIRS] = i;
                } else if (blockType == BLK_COBBLESTONE) {
                    TEXTURE_OFFSETS[BLK_COBBLESTONE_STAIRS] = i;
                } else if (blockType == BLK_WOODEN_PLANK) {
                    TEXTURE_OFFSETS[BLK_WOODEN_STAIRS] = i;
                } else if (blockType == BLK_STONE_BRICKS) {
                    TEXTURE_OFFSETS[BLK_STONE_BRICK_STAIRS] = i;
                }
            }
        }
    }
    
    private static final long serialVersionUID = 2011101001L;
}