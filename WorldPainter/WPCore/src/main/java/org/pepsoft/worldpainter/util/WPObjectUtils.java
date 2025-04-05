package org.pepsoft.worldpainter.util;

import org.pepsoft.minecraft.Direction;
import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;
import org.pepsoft.worldpainter.objects.WPObject;

import java.util.EnumSet;
import java.util.Set;

import static org.pepsoft.minecraft.Constants.MC_COBBLESTONE_WALL;
import static org.pepsoft.minecraft.Material.*;
import static org.pepsoft.worldpainter.objects.WPObject.LEAF_DECAY_NO_CHANGE;
import static org.pepsoft.worldpainter.objects.WPObject.LEAF_DECAY_ON;

/**
 * Created by isc21004 on 03-06-15.
 */
public class WPObjectUtils {
    /**
     * Place a block in a {@link MinecraftWorld}, taking into account what is already there, and automatically managing
     * a number of material aspects.
     *
     * @param world             The world in which to place the block.
     * @param x                 The X coordinate at which to place the block.
     * @param y                 The Y coordinate (Z in Minecraft) at which to place the block.
     * @param height            The height (Y coordinate in Minecraft) at which to place the block.
     * @param material          The material to place.
     * @param leafDecayMode     The leaf decay mode to apply, as one of the {@code LEAF_DECAY_*} constants in {@link WPObject}.
     * @param waterloggedLeaves Whether the platform to which the block will be exported supports waterlogged leaf blocks.
     * @param connectBlocks     Whether any connections of the block (such as fence posts might make) to surrounding objects should be automatically managed.
     * @param manageWaterlogged Whether the {@code waterlogged} property of the block should be automatically adjusted according to whether the existing block contains water.
     */
    public static void placeBlock(MinecraftWorld world, int x, int y, int height, Material material, int leafDecayMode, boolean waterloggedLeaves, boolean connectBlocks, boolean manageWaterlogged) {
        if (material.leafBlock && (leafDecayMode != LEAF_DECAY_NO_CHANGE)) {
            if (leafDecayMode == LEAF_DECAY_ON) {
                material = material.withProperty(PERSISTENT, false);
            } else {
                material = material.withProperty(PERSISTENT, true);
            }
        }
        final Material existingMaterial = world.getMaterialAt(x, y, height);
        final boolean existingMaterialContainsWater = existingMaterial.containsWater();
        // Manage the waterlogged property, but only if requested, and we're confident what it should be based on the
        // block that is already there
        if (manageWaterlogged
                && (existingMaterial.translucent || existingMaterial.hasProperty(WATERLOGGED))
                && (material.leafBlock ? waterloggedLeaves : material.hasProperty(WATERLOGGED))) {
            if (existingMaterialContainsWater) {
                material = material.withProperty(WATERLOGGED, true);
            } else {
                material = material.withProperty(WATERLOGGED, false);
            }
        }
        // Manage the cardinal direction properties for connecting blocks, if requested
        Set<Direction> checkReverseConnections = null;
        if (connectBlocks) {
            if (material.connectingBlock) {
                // The object block is a connecting block; check around it for other connecting blocks or solid blocks
                if (wouldConnect(material, world.getMaterialAt(x - 1, y, height))) {
                    material = material.withProperty(WEST, true);
                    checkReverseConnections = EnumSet.of(Direction.WEST);
                }
                if (wouldConnect(material, world.getMaterialAt(x, y - 1, height))) {
                    material = material.withProperty(NORTH, true);
                    if (checkReverseConnections == null) {
                        checkReverseConnections = EnumSet.of(Direction.NORTH);
                    } else {
                        checkReverseConnections.add(Direction.NORTH);
                    }
                }
                if (wouldConnect(material, world.getMaterialAt(x + 1, y, height))) {
                    material = material.withProperty(EAST, true);
                    if (checkReverseConnections == null) {
                        checkReverseConnections = EnumSet.of(Direction.EAST);
                    } else {
                        checkReverseConnections.add(Direction.EAST);
                    }
                }
                if (wouldConnect(material, world.getMaterialAt(x, y + 1, height))) {
                    material = material.withProperty(SOUTH, true);
                    if (checkReverseConnections == null) {
                        checkReverseConnections = EnumSet.of(Direction.SOUTH);
                    } else {
                        checkReverseConnections.add(Direction.SOUTH);
                    }
                }
            } else if (material.solid && material.opaque) {
                // The object block is not a connecting block, but it *is* solid and opaque, so check for surrounding
                // connecting blocks which might need to be connected to it
                if (wouldConnect(material, world.getMaterialAt(x - 1, y, height))) {
                    checkReverseConnections = EnumSet.of(Direction.WEST);
                }
                if (wouldConnect(material, world.getMaterialAt(x, y - 1, height))) {
                    if (checkReverseConnections == null) {
                        checkReverseConnections = EnumSet.of(Direction.NORTH);
                    } else {
                        checkReverseConnections.add(Direction.NORTH);
                    }
                }
                if (wouldConnect(material, world.getMaterialAt(x + 1, y, height))) {
                    if (checkReverseConnections == null) {
                        checkReverseConnections = EnumSet.of(Direction.EAST);
                    } else {
                        checkReverseConnections.add(Direction.EAST);
                    }
                }
                if (wouldConnect(material, world.getMaterialAt(x, y + 1, height))) {
                    if (checkReverseConnections == null) {
                        checkReverseConnections = EnumSet.of(Direction.SOUTH);
                    } else {
                        checkReverseConnections.add(Direction.SOUTH);
                    }
                }
            }
        }
        // Don't replace water with insubstantial blocks that don't have a waterlogged property (assume such a block
        // would be washed away), except air. We are slightly guessing at what the user would want to happen here...
        if ((! material.veryInsubstantial) || (! existingMaterialContainsWater) || material.containsWater() || material.empty) {
            world.setMaterialAt(x, y, height, material);
            if (connectBlocks && (checkReverseConnections != null)) {
                makeReverseConnections(world, x, y, height, checkReverseConnections);
            }
        }
    }

    /**
     * Determine whether two blocks would connect to each other in some way (forming a fence, for instance).
     */
    @SuppressWarnings("StringEquality") // String is interned
    public static boolean wouldConnect(Material blockTypeOne, Material blockTypeTwo) {
        if (blockTypeOne.veryInsubstantial || blockTypeTwo.veryInsubstantial) {
            return false;
        } else if (blockTypeOne.connectingBlock || blockTypeOne.isNamed(MC_COBBLESTONE_WALL) || blockTypeTwo.connectingBlock || blockTypeTwo.isNamed(MC_COBBLESTONE_WALL)) {
            // TODO encode this into a "connects" property on the material and just check the name
            return (blockTypeOne.name == blockTypeTwo.name)
                    || (blockTypeOne.solid && blockTypeOne.opaque)
                    || (blockTypeTwo.solid && blockTypeTwo.opaque);
        } else {
            return false;
        }
    }

    /**
     * Check neighbouring blocks to see if they need to be connected back to the object block.
     */
    private static void makeReverseConnections(MinecraftWorld world, int x, int y, int height, Set<Direction> directions) {
        for (Direction direction: directions) {
            final int neighbourX = x + direction.getDx(), neighbourY = y + direction.getDy();
            Material neighbouringMaterial = world.getMaterialAt(neighbourX, neighbourY, height);
            if (neighbouringMaterial.connectingBlock) {
                switch (direction) {
                    case WEST:
                        neighbouringMaterial = neighbouringMaterial.withProperty(EAST, true);
                        break;
                    case NORTH:
                        neighbouringMaterial = neighbouringMaterial.withProperty(SOUTH, true);
                        break;
                    case EAST:
                        neighbouringMaterial = neighbouringMaterial.withProperty(WEST, true);
                        break;
                    case SOUTH:
                        neighbouringMaterial = neighbouringMaterial.withProperty(NORTH, true);
                        break;
                }
                world.setMaterialAt(neighbourX, neighbourY, height, neighbouringMaterial);
            }
        }
    }
}