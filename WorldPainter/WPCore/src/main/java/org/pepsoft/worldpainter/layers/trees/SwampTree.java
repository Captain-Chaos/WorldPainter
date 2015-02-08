/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers.trees;

import java.util.Random;
import static org.pepsoft.minecraft.Material.*;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;

/**
 *
 * @author pepijn
 */
public class SwampTree extends DeciduousTree {
    public SwampTree() {
        super(WOOD_OAK, LEAVES_OAK);
    }
    
    @Override
    public void renderTree(int x, int y, int height, int strength, MinecraftWorld world, Dimension dimension, Random random) {
        int size = Math.min(2 + strength / 3, 5) + random.nextInt(3);
        if ((height + size) >= world.getMaxHeight()) {
            // Tree won't fit under the sky
            return;
        }
        renderTrunk(x, y, height, size, world);
        renderCanopy(x, y, height, size, world, random);
        renderVines(x, y, height, size, world, random);
    }
    
    private static final long serialVersionUID = 1L;
}