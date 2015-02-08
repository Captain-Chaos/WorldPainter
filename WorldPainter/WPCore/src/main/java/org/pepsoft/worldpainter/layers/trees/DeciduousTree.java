/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers.trees;

import java.util.Random;
import org.pepsoft.minecraft.Material;
import org.pepsoft.util.MathUtils;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;

/**
 *
 * @author pepijn
 */
public abstract class DeciduousTree extends TreeType {
    protected DeciduousTree(Material trunkMaterial, Material leafMaterial) {
        super(trunkMaterial, leafMaterial);
    }
    
    @Override
    public void renderTree(int blockInWorldX, int blockInWorldY, int height, int strength, MinecraftWorld minecraftWorld, Dimension dimension, Random random) {
        int size = Math.min(2 + strength / 3, 5) + random.nextInt(3);
        if ((height + size) >= minecraftWorld.getMaxHeight()) {
            // Tree won't fit under the sky
            return;
        }
        renderTrunk(blockInWorldX, blockInWorldY, height, size, minecraftWorld);
        renderCanopy(blockInWorldX, blockInWorldY, height, size, minecraftWorld, random);
    }

    protected void renderCanopy(int blockInWorldX, int blockInWorldY, int height, int size, MinecraftWorld minecraftWorld, Random random) {
        int r = Math.max(1, Math.min(size / 2 + 1, 4));
        int maxZ = minecraftWorld.getMaxHeight() - 1;
        for (int z = 0; z <= size + r; z++) {
            if (height + z > maxZ) {
                break;
            }
            float distance = Math.abs(z - size - 1);
            if (z > size) {
                maybePlaceLeaves(blockInWorldX, blockInWorldY, height + z, r, distance, minecraftWorld, random);
            }
            int maxDistance = Math.min(4 - Math.abs(z - size), r);
            for (int d = 1; d <= maxDistance; d++) {
                for (int i = 0; i < d; i++) {
                    distance = MathUtils.getDistance(d - i, i, z - size - 1);
                    maybePlaceLeaves(blockInWorldX - d + i, blockInWorldY - i,     height + z, r, distance, minecraftWorld, random);
                    maybePlaceLeaves(blockInWorldX + i,     blockInWorldY - d + i, height + z, r, distance, minecraftWorld, random);
                    maybePlaceLeaves(blockInWorldX + d - i, blockInWorldY + i,     height + z, r, distance, minecraftWorld, random);
                    maybePlaceLeaves(blockInWorldX - i,     blockInWorldY + d - i, height + z, r, distance, minecraftWorld, random);
                }
            }
        }
    }
    
    private static final long serialVersionUID = 1L;
}