package org.pepsoft.worldpainter.biomeschemes;

import org.pepsoft.worldpainter.BiomeScheme;

/**
 * An abstract base class for biome info classes ({@link BiomeScheme}'s which just provide static information about the
 * supported biomes and cannot calculate biomes).
 */
public abstract class AbstractBiomeInfo implements BiomeScheme {
    protected AbstractBiomeInfo(String[] names, boolean[][][] patterns) {
        this.names = names;
        this.patterns = patterns;
        int highestId = names.length - 1;
        for (int i = highestId; i >= 0; i--) {
            if (names[i] != null) {
                highestId = i;
                break;
            }
        }
        this.highestId = highestId;
    }

    @Override
    public final void setSeed(long seed) {
        // Do nothing
    }

    @Override
    public int getBiomeCount() {
        return highestId + 1;
    }

    @Override
    public int[] getBiomes(int x, int y, int width, int height) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public void getBiomes(int x, int y, int width, int height, int[] buffer) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public boolean[][] getPattern(int biome) {
        return patterns[biome];
    }

    @Override
    public String getBiomeName(int biome) {
        return names[biome];
    }

    @Override
    public boolean isBiomePresent(int biome) {
        return (biome >= 0) && (biome <= highestId) && (names[biome] != null);
    }

    private final String[] names;
    private final boolean[][][] patterns;
    private final int highestId;
}