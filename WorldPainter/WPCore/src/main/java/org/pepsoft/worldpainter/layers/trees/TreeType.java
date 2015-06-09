/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.layers.trees;

import java.io.Serializable;
import java.util.Random;
import static org.pepsoft.minecraft.Constants.*;
import org.pepsoft.minecraft.Direction;
import org.pepsoft.minecraft.Material;
import static org.pepsoft.minecraft.Material.*;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.exporting.Cursor;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;

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
        if ((trunkMaterial.blockType == BLK_WOOD) || (trunkMaterial.blockType == BLK_WOOD2)) {
            return Material.get(trunkMaterial.blockType, (trunkMaterial.data & 0x3) | 0xC);
        } else {
            return trunkMaterial;
        }
    }
    
    protected void maybePlaceLeaves(int x, int y, int h, int r, float distance, MinecraftWorld minecraftWorld, Random random) {
        if (minecraftWorld.getBlockTypeAt(x, y, h) == BLK_AIR) {
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
            final int blockType = material.blockType;
            return blockType == BLK_WOOD
                    || blockType == BLK_WOOD2
                    || blockType == BLK_LEAVES
                    || blockType == BLK_LEAVES2;
        } else {
            return false;
        }
    }
    
    private boolean addVine(MinecraftWorld world, int x, int y, int z, Direction direction) {
        Material existingBlock = world.getMaterialAt(x, y, z);
        if ((existingBlock == null) || ((existingBlock != AIR) && (existingBlock.blockType != BLK_VINES))) {
            return false;
        }
        int data = existingBlock.data;
        switch (direction) {
            case NORTH:
                data |= 0x4;
                break;
            case EAST:
                data |= 0x8;
                break;
            case SOUTH:
                data |= 0x1;
                break;
            case WEST:
                data |= 0x2;
                break;
        }
        if (existingBlock == AIR) {
            world.setBlockTypeAt(x, y, z, BLK_VINES);
        }
        world.setDataAt(x, y, z, data);
        return true;
    }
    
    protected final Material trunkMaterial, leafMaterial;
    
    private static final int VINE_INCIDENCE = 5;
    private static final long serialVersionUID = 1L;
}