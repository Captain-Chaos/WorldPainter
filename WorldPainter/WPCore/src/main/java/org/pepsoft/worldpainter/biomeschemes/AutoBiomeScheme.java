/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.biomeschemes;

import org.pepsoft.worldpainter.Dimension;

/**
 *
 * @author pepijn
 */
public class AutoBiomeScheme extends AbstractMinecraft1_7BiomeScheme {
    public AutoBiomeScheme(Dimension dimension) {
        this.dimension = dimension;
    }
    
    @Override
    public void setSeed(long seed) {
        // Do nothing
    }

    @Override
    public void getBiomes(int x, int y, int width, int height, int[] buffer) {
        for (int dx = 0; dx < width; dx++) {
            for (int dy = 0; dy < height; dy++) {
                int autoBiome = dimension.getAutoBiome(x + dx, y + dy);
                buffer[dx + dy * width] = ((autoBiome != -1) ? autoBiome : DEFAULT_BIOME);
            }
        }
    }
    
    private final Dimension dimension;
    
    private static final int DEFAULT_BIOME = BIOME_PLAINS;
}