/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.layers.trees;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;
import java.util.Random;
import org.pepsoft.util.MathUtils;
import static org.pepsoft.minecraft.Material.*;

/**
 *
 * @author pepijn
 */
public class PineTree extends TreeType {
    public PineTree() {
        super(WOOD_PINE, LEAVES_PINE);
    }
    
    @Override
    public void renderTree(int blockInWorldX, int blockInWorldY, int height, int strength, MinecraftWorld minecraftWorld, Dimension dimension, Random random) {
        int size = Math.min(5 + strength / 2, 7) + random.nextInt(3);
        if (height + size >= minecraftWorld.getMaxHeight()) {
            // Tree won't fit under the sky
            return;
        }
        renderTrunk(blockInWorldX, blockInWorldY, height, size, minecraftWorld);
        renderCanopy(blockInWorldX, blockInWorldY, height, size, minecraftWorld, random);
    }

    private void renderCanopy(int blockInWorldX, int blockInWorldY, int height, int size, MinecraftWorld minecraftWorld, Random random) {
        // The height of the top of the canopy relative to the ground
        int canopyTop = Math.min(size + 5, (int) (size * 1.5625f)) - random.nextInt(2);
        // The height of the lowest branches of the canopy
        int canopyStart = Math.min(size / 2 + 1, 4 + random.nextInt(3));
        // The total height of the canopy
        int canopyHeight = canopyTop - canopyStart + 1;
        // Iterate over each potential leaf block, bottom to top
        int maxZ = minecraftWorld.getMaxHeight() - 1;
        for (int z = 0; z < canopyHeight; z++) {
            if (height + z + canopyStart > maxZ) {
                break;
            }
            int r = (canopyHeight - z) * 4 / 10;
            if ((z + canopyStart) > size) {
                maybePlaceLeaves(blockInWorldX, blockInWorldY, height + z + canopyStart, r, 0.0f, minecraftWorld, random);
            }
            int maxDistance = Math.min(4 - (z + canopyStart - size), r);
            if (r > 0) {
                for (int d = 1; d <= maxDistance; d++) {
                    for (int i = 0; i < d; i++) {
                        float distance = MathUtils.getDistance(d - i, i);
                        maybePlaceLeaves(blockInWorldX - d + i, blockInWorldY - i,     height + z + canopyStart, r, distance, minecraftWorld, random);
                        maybePlaceLeaves(blockInWorldX + i,     blockInWorldY - d + i, height + z + canopyStart, r, distance, minecraftWorld, random);
                        maybePlaceLeaves(blockInWorldX + d - i, blockInWorldY + i,     height + z + canopyStart, r, distance, minecraftWorld, random);
                        maybePlaceLeaves(blockInWorldX - i,     blockInWorldY + d - i, height + z + canopyStart, r, distance, minecraftWorld, random);
                    }
                }
            }
        }
    }

    private static final long serialVersionUID = 1L;
}