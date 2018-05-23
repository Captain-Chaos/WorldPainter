/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers.trees;

import org.pepsoft.minecraft.Direction;
import org.pepsoft.minecraft.Material;
import org.pepsoft.util.MathUtils;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.exporting.Cursor;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;

import java.util.Random;

import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.minecraft.Material.*;

/**
 *
 * @author pepijn
 */
public class JungleTree extends TreeType {
    public JungleTree() {
        super(WOOD_JUNGLE, LEAVES_JUNGLE);
    }
    
    @Override
    public void renderTree(int x, int y, int height, int strength, MinecraftWorld world, Dimension dimension, Random random) {
        int size = (int) (random.nextInt(strength) + Math.pow(random.nextDouble(), 4) * strength * 3 + 0.5);
        if ((height + size) >= world.getMaxHeight()) {
            // Tree won't fit under the sky
            return;
        }
        renderTrunk(x, y, height, size, world);
        if (size < 12) {
            renderCanopy(x, y, height, size, world, random);
        } else if (size < 32) {
            renderCanopy(x,     y,     height, size, world, random);
            renderCanopy(x + 1, y,     height, size, world, random);
            renderCanopy(x,     y + 1, height, size, world, random);
            renderCanopy(x + 1, y + 1, height, size, world, random);
        } else {
            renderCanopy(x - 1, y,     height, size, world, random);
            renderCanopy(x + 1, y - 1, height, size, world, random);
            renderCanopy(x + 2, y + 1, height, size, world, random);
            renderCanopy(x    , y + 2, height, size, world, random);
        }
        for (int i = 0; i < size / 10 + 1; i++) {
            renderBranch(x, y, height, size, (float) (random.nextDouble() * Math.PI * 2), world, random);
        }
        for (int z = 4; z < size - 2; z++) {
            if (random.nextInt(5) == 0) {
                renderBranch(x, y, height, z, (float) (random.nextDouble() * Math.PI * 2), world, random);
            }
        }
        if (size >= 24) {
            for (int i = 0; i < size / 10 + 1; i++) {
                renderRoot(x, y, height, size, (float) (random.nextDouble() * Math.PI * 2), world, random);
            }
        }
        renderVines(x, y, height, size, world, random, VINE_INCIDENCE, 5, 5);
        if ((size < 12) &&  (random.nextInt(5) == 0)) {
            renderCocoaPods(x, y, height, size, world, random);
        }
    }

    private void renderRoot(int x, int y, int height, int size, float angle, MinecraftWorld world, Random random) {
        int h = (int) (size / 5f + 0.5f);
        Material capMaterial = getCapMaterial();
        int i = 1, maxDepth = -1;
        int previousDx = Integer.MIN_VALUE, previousDy = Integer.MIN_VALUE;
        while (h > 0) {
            int dx = (int) (Math.sin(angle) * i + 0.5f);
            int dy = (int) (Math.cos(angle) * i + 0.5f);
            if ((dx == previousDx) && (dy == previousDy)) {
                i++;
                continue;
            } else {
                previousDx = dx;
                previousDy = dy;
            }
            int worldX = x + dx, worldY = y + dy;
            int depth = 0;
            for (int z = height + h; z > 0; z--) {
                if (! world.getMaterialAt(worldX, worldY, z).veryInsubstantial) {
                    depth++;
                }
                world.setMaterialAt(worldX, worldY, z, (z < (height + h)) ? trunkMaterial : capMaterial);
                if (depth > maxDepth) {
                    break;
                }
            }
            if (random.nextInt(15) == 0) {
                world.setMaterialAt(worldX, worldY, height + h + 1, random.nextBoolean() ? RED_MUSHROOM : BROWN_MUSHROOM);
//                System.out.println("Rendered mushroom @ " + worldX + "," + worldY + "," + (height + h + 1));
            }
            h -= (random.nextInt(2) + 1);
            i++;
            maxDepth++;
        }
    }

    @Override
    protected void renderTrunk(int blockInWorldX, int blockInWorldY, int height, int size, MinecraftWorld minecraftWorld) {
        if (size < 12) {
            super.renderTrunk(blockInWorldX, blockInWorldY, height, size, minecraftWorld);
        } else if (size < 32) {
            minecraftWorld.setMaterialAt(blockInWorldX,     blockInWorldY,     height, DIRT);
            minecraftWorld.setMaterialAt(blockInWorldX + 1, blockInWorldY,     height, DIRT);
            minecraftWorld.setMaterialAt(blockInWorldX,     blockInWorldY + 1, height, DIRT);
            minecraftWorld.setMaterialAt(blockInWorldX + 1, blockInWorldY + 1, height, DIRT);
            for (int i = 1; i < size; i++) {
                minecraftWorld.setMaterialAt(blockInWorldX,     blockInWorldY,     height + i, trunkMaterial);
                minecraftWorld.setMaterialAt(blockInWorldX + 1, blockInWorldY,     height + i, trunkMaterial);
                minecraftWorld.setMaterialAt(blockInWorldX,     blockInWorldY + 1, height + i, trunkMaterial);
                minecraftWorld.setMaterialAt(blockInWorldX + 1, blockInWorldY + 1, height + i, trunkMaterial);
            }
            Material capMaterial = getCapMaterial();
            minecraftWorld.setMaterialAt(blockInWorldX,     blockInWorldY,     height + size, capMaterial);
            minecraftWorld.setMaterialAt(blockInWorldX + 1, blockInWorldY,     height + size, capMaterial);
            minecraftWorld.setMaterialAt(blockInWorldX,     blockInWorldY + 1, height + size, capMaterial);
            minecraftWorld.setMaterialAt(blockInWorldX + 1, blockInWorldY + 1, height + size, capMaterial);
        } else {
            Material capMaterial = getCapMaterial();
            for (int dx = -1; dx < 3; dx++) {
                for (int dy = -1; dy < 3; dy++) {
                    if (((dx == -1) || (dx == 2)) && ((dy == -1) || (dy == 2))) {
                        continue;
                    }
                    minecraftWorld.setMaterialAt(blockInWorldX + dx, blockInWorldY + dy, height, DIRT);
                    for (int i = 1; i < size; i++) {
                        minecraftWorld.setMaterialAt(blockInWorldX + dx, blockInWorldY + dy, height + i, trunkMaterial);
                    }
                    minecraftWorld.setMaterialAt(blockInWorldX + dx, blockInWorldY + dy, height + size, capMaterial);
                }
            }
        }
    }

    protected void renderCanopy(int x, int y, int height, int size, MinecraftWorld world, Random random) {
        final int r = Math.max(Math.min(size, 4), 2);
        int maxZ = world.getMaxHeight() - 1;
        for (int z = Math.max(size - r, 1); z <= size + r; z++) {
            // z includes the +1 due to height referring to the block
            // *underneath* the tree
            if (height + z > maxZ) {
                break;
            }
            float distance = Math.abs(z - size) * 2;
            maybePlaceLeaves(x, y, height + z, r, distance, world, random);
            int maxDistance = Math.min(4 - Math.abs(z - size), r);
//            if (z > size) {
//                System.out.print("+");
//            } else {
//                System.out.print("-");
//            }
            for (int d = 1; d <= maxDistance; d++) {
                for (int i = 0; i < d; i++) {
                    distance = (int) (MathUtils.getDistance(d - i, i, (z - size) * 2) + 0.5f);
//                    System.out.printf("%.1f", distance);
                    maybePlaceLeaves(x - d + i, y - i,     height +z, r, distance + random.nextInt(2), world, random);
                    maybePlaceLeaves(x + i,     y - d + i, height + z, r, distance + random.nextInt(2), world, random);
                    maybePlaceLeaves(x + d - i, y + i,     height + z, r, distance + random.nextInt(2), world, random);
                    maybePlaceLeaves(x - i,     y + d - i, height + z, r, distance + random.nextInt(2), world, random);
                }
            }
        }
//        System.out.println();
    }
    
    protected void renderBranch(int x, int y, int height, int size, float angle, MinecraftWorld world, Random random) {
        int l = (int) (size / 5f + 0.5f);
        Material branchMaterial, capMaterial;
        if ((trunkMaterial.blockType == BLK_WOOD) || (trunkMaterial.blockType == BLK_WOOD2)) {
            branchMaterial = ((angle < (0.25 * Math.PI)) || ((angle > (0.75 * Math.PI)) && (angle < (1.25 * Math.PI))) || (angle > (1.75 * Math.PI)))
                ? Material.get(trunkMaterial.blockType, (trunkMaterial.data & 0x3) | 0x8)
                : Material.get(trunkMaterial.blockType, (trunkMaterial.data & 0x3) | 0x4);
            capMaterial = getCapMaterial();
        } else {
            branchMaterial = capMaterial = trunkMaterial;
        }
        float slope = random.nextFloat() / 10 - 0.5f;
        for (int i = 1; i < l; i++) {
            int dx = (int) (Math.sin(angle) * i + 0.5f);
            int dy = (int) (Math.cos(angle) * i + 0.5f);
            int dz = (int) (i * slope);
            if (! world.getMaterialAt(x + dx, y + dy, height + size + dz + 1).veryInsubstantial) {
                continue;
            }
            world.setMaterialAt(x + dx, y + dy, height + size + dz, (i < (l - 1)) ? branchMaterial : capMaterial);
            if (random.nextInt(25) == 0) {
                world.setMaterialAt(x + dx, y + dy, height + size + dz + 1, random.nextBoolean() ? RED_MUSHROOM : BROWN_MUSHROOM);
            }
            if (((i > 1) && (((i - 1) % 3) == 0)) || (i == (l - 1))) {
                renderCanopy(x + dx, y + dy, height + dz, size, world, random);
            }
        }
    }
    
    @Override
    protected void maybePlaceLeaves(int x, int y, int h, int r, float distance, MinecraftWorld minecraftWorld, Random random) {
        if (minecraftWorld.getMaterialAt(x, y, h) == AIR) {
            if (distance <= r) {
                minecraftWorld.setMaterialAt(x, y, h, leafMaterial);
            }
        }
    }

    protected void renderCocoaPods(int x, int y, int height, int size, MinecraftWorld world, Random random) {
        if (size < 12) {
            renderCocoaPods(x - 1, y,     height, size, world, random, Direction.EAST);
            renderCocoaPods(x,     y - 1, height, size, world, random, Direction.SOUTH);
            renderCocoaPods(x + 1, y,     height, size, world, random, Direction.WEST);
            renderCocoaPods(x,     y + 1, height, size, world, random, Direction.NORTH);
        } else if (size < 32) {
            renderCocoaPods(x - 1, y,     height, size, world, random, Direction.EAST);
            renderCocoaPods(x - 1, y + 1, height, size, world, random, Direction.EAST);
            renderCocoaPods(x,     y - 1, height, size, world, random, Direction.SOUTH);
            renderCocoaPods(x + 1, y - 1, height, size, world, random, Direction.SOUTH);
            renderCocoaPods(x + 2, y,     height, size, world, random, Direction.WEST);
            renderCocoaPods(x + 2, y + 1, height, size, world, random, Direction.WEST);
            renderCocoaPods(x,     y + 2, height, size, world, random, Direction.NORTH);
            renderCocoaPods(x + 1, y + 2, height, size, world, random, Direction.NORTH);
        }
    }
    
    protected void renderCocoaPods(int x, int y, int height, int size, MinecraftWorld world, Random random, Direction direction) {
        Cursor cursor = new Cursor(world, x, y, height + 1, direction);
        for (int i = 0; i < size; i++) {
            if ((random.nextInt(15) == 0) && cursor.isFreeOrInsubstantial()) {
//                System.out.println("Placing cocoa plant @ " + cursor.getX() + "," + cursor.getY() + "," + cursor.getHeight());
                cursor.setBlockWithDirection(COCOA_PODS[random.nextInt(3)]);
            }
            cursor.up();
        }
    }
    
    private static final int VINE_INCIDENCE = 3;
    private static final Material[] COCOA_PODS = {COCOA_PLANT, COCOA_PLANT_HALF_RIPE, COCOA_PLANT_RIPE};
    private static final long serialVersionUID = 1L;
}