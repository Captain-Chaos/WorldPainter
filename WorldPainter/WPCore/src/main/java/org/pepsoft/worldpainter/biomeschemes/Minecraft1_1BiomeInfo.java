package org.pepsoft.worldpainter.biomeschemes;

/**
 * This is a non functional biome scheme, intended only for providing
 * information about supported biomes but without the ability to actually
 * calculate biomes. The {@link #getBiomes(int, int, int, int, int[])} method
 * always throws an {@link UnsupportedOperationException}.
 */
public final class Minecraft1_1BiomeInfo extends AbstractMinecraft1_1BiomeScheme {
    private Minecraft1_1BiomeInfo() {
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

    public static final Minecraft1_1BiomeInfo INSTANCE = new Minecraft1_1BiomeInfo();
}