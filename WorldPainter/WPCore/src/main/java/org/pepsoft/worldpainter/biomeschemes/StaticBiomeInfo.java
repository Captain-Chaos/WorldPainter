package org.pepsoft.worldpainter.biomeschemes;

import org.pepsoft.worldpainter.BiomeScheme;

/**
 * This is a non functional biome scheme, intended only for providing
 * information about supported biomes but without the ability to actually
 * calculate biomes. The {@link #getBiomes(int, int, int, int, int[])} method
 * always throws an {@link UnsupportedOperationException}.
 */
public final class StaticBiomeInfo extends AbstractMinecraft1_21BiomeScheme {
    private StaticBiomeInfo() {
        // Enforce singleton pattern
    }

    @Override
    public void setSeed(long seed) {
        // Do nothing
    }

    @Override
    public void getBiomes(int x, int y, int width, int height, int[] buffer) {
        throw new UnsupportedOperationException("Not supported");
    }

    public static final BiomeScheme INSTANCE = new StaticBiomeInfo();
}
