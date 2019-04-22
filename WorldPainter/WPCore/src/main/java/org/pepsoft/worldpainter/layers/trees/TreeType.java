/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.layers.trees;

import org.pepsoft.minecraft.Direction;
import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.exporting.Cursor;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;

import java.io.Serializable;
import java.util.Random;

import static org.pepsoft.minecraft.Constants.MC_VINE;
import static org.pepsoft.minecraft.Material.*;

/**
 *
 * @author pepijn
 */
public abstract class TreeType implements Serializable {
    protected TreeType(Material trunkMaterial, Material leafMaterial) {
        this.trunkMaterial = trunkMaterial;
        this.leafMaterial = leafMaterial;
    }
    
    public abstract void renderTree(int blockInWorldX, int blockInWorldY, int height, int strength, MinecraftWorld minecraftWorld, Dimension dimension, Random random);
    
    protected void renderTrunk(int blockInWorldX, int blockInWorldY, int height, int size, MinecraftWorld minecraftWorld) {
        minecraftWorld.setMaterialAt(blockInWorldX, blockInWorldY, height, DIRT);
        for (int i = 1; i < size; i++) {
            minecraftWorld.setMaterialAt(blockInWorldX, blockInWorldY, height + i, trunkMaterial);
        }
        minecraftWorld.setMaterialAt(blockInWorldX, blockInWorldY, height + size, getCapMaterial());
    }
    
    protected final Material getCapMaterial() {
        if (trunkMaterial.name.endsWith("_log")) {
            String woodType = trunkMaterial.name.substring(0, trunkMaterial.name.length() - 4);
            return Material.get(woodType + "_wood");
        } else {
            return trunkMaterial;
        }
    }
    
    protected void maybePlaceLeaves(int x, int y, int h, int r, float distance, MinecraftWorld minecraftWorld, Random random) {
        if (minecraftWorld.getMaterialAt(x, y, h) == AIR) {
            if (((r > 0) ? random.nextInt(r) : 0) + 1.5f >= distance) {
                minecraftWorld.setMaterialAt(x, y, h, leafMaterial);
            }
        }
    }
    
    protected void renderVines(int x, int y, int z, int size, MinecraftWorld world, Random random) {
        renderVines(x, y, z, size, world, random, VINE_INCIDENCE, 3, 4);
    }
    
    protected void renderVines(int x, int y, int z, int size, MinecraftWorld world, Random random, int vineIncidence, int vineLengthBase, int vineLengthVariation) {
        int r = Math.max(1, Math.min(size / 2 + 1, 4));
        int maxZ = world.getMaxHeight() - 1;
        for (int dz = 0; dz <= size + r; dz++) {
            if (z + dz > maxZ) {
                break;
            }
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    if (random.nextInt(vineIncidence) == 0) {
                        Cursor cursor = new Cursor(world, x + dx, y + dy, z + dz, Direction.NORTH);
                        if (cursor.isFreeOrInsubstantial()) {
                            int turns = random.nextInt(4);
                            for (int i = 0; i < turns; i++) {
                                cursor.turnRight();
                            }
                            if (isTreeBlock(cursor.getBlockInFront())) {
                                int vineLength = random.nextInt(vineLengthVariation) + vineLengthBase;
                                for (int i = 0; i < vineLength; i++) {
                                    if (((z + dz - i) <= 0) || (! addVine(world, x + dx, y + dy, z + dz - i, cursor.getDirection()))) {
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    private boolean isTreeBlock(Material material) {
        if (material != null) {
            final String simpleName = material.simpleName;
            return simpleName.endsWith("_log")
                    || simpleName.endsWith("_bark")
                    || simpleName.endsWith("_leaves");
        } else {
            return false;
        }
    }
    
    private boolean addVine(MinecraftWorld world, int x, int y, int z, Direction direction) {
        Material existingBlock = world.getMaterialAt(x, y, z);
        if ((existingBlock == null) || ((existingBlock != AIR) && (existingBlock.isNotNamed(MC_VINE)))) {
            return false;
        }
        Material vine = existingBlock.isNamed(MC_VINE) ? existingBlock : VINE;
        switch (direction) {
            case NORTH:
                vine = vine.withProperty(NORTH, true);
                break;
            case EAST:
                vine = vine.withProperty(EAST, true);
                break;
            case SOUTH:
                vine = vine.withProperty(SOUTH, true);
                break;
            case WEST:
                vine = vine.withProperty(WEST, true);
                break;
            default:
                throw new InternalError();
        }
        world.setMaterialAt(x, y, z, vine);
        return true;
    }
    
    protected final Material trunkMaterial, leafMaterial;
    
    private static final int VINE_INCIDENCE = 5;
    private static final long serialVersionUID = 1L;
}