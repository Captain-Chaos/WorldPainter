package org.pepsoft.worldpainter.layers.trees;

import java.util.Random;
import static org.pepsoft.minecraft.Material.*;
import org.pepsoft.util.MathUtils;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;

/**
 * Created with IntelliJ IDEA.
 * User: pepijn
 * Date: 20-10-12
 * Time: 16:45
 * To change this template use File | Settings | File Templates.
 */
public class Bush extends TreeType {
    public Bush() {
        super(WOOD_OAK, LEAVES_OAK);
    }

    @Override
    public void renderTree(int blockInWorldX, int blockInWorldY, int height, int strength, MinecraftWorld minecraftWorld, Dimension dimension, Random random) {
        minecraftWorld.setMaterialAt(blockInWorldX, blockInWorldY, height + 1, trunkMaterial);
        int r = (strength < 2) ? 2 : (2 + random.nextInt(Math.min(strength, 6) / 2));
        int maxZ = minecraftWorld.getMaxHeight() - 1;
        for (int dz = -r; dz <=  r; dz++) {
            int z = height + dz;
            if ((z < 1) || (z > maxZ)) {
                break;
            }
            float distance = Math.abs(dz - 1);
            maybePlaceLeaves(blockInWorldX, blockInWorldY, z, r, distance, minecraftWorld, random);
            int maxDistance = Math.min(4 - Math.abs(dz - 1), r);
            for (int d = 1; d <= maxDistance; d++) {
                for (int i = 0; i < d; i++) {
                    distance = MathUtils.getDistance(d - i, i, dz - 1);
                    maybePlaceLeaves(blockInWorldX - d + i, blockInWorldY - i,     z, r, distance, minecraftWorld, random);
                    maybePlaceLeaves(blockInWorldX + i,     blockInWorldY - d + i, z, r, distance, minecraftWorld, random);
                    maybePlaceLeaves(blockInWorldX + d - i, blockInWorldY + i,     z, r, distance, minecraftWorld, random);
                    maybePlaceLeaves(blockInWorldX - i,     blockInWorldY + d - i, z, r, distance, minecraftWorld, random);
                }
            }
        }
    }

    private static final long serialVersionUID = 1L;
}