package org.pepsoft.worldpainter.biomeschemes;

/**
 * This is a non functional biome scheme, intended only for providing information about supported biomes but without the
 * ability to actually calculate biomes. The {@link #getBiomes(int, int, int, int, int[])} method always throws an
 * {@link UnsupportedOperationException}.
 */
public final class Minecraft1_12BiomeInfo extends AbstractMinecraft1_7BiomeScheme {
    private Minecraft1_12BiomeInfo() {
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

    public static final Minecraft1_12BiomeInfo INSTANCE = new Minecraft1_12BiomeInfo();
}