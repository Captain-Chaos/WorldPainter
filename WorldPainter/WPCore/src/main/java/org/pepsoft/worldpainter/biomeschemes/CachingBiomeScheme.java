/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.biomeschemes;

import java.util.Set;
import org.pepsoft.worldpainter.BiomeScheme;
import org.pepsoft.worldpainter.ColourScheme;

/**
 *
 * @author pepijn
 */
public class CachingBiomeScheme implements BiomeScheme {
    public CachingBiomeScheme(BiomeScheme biomeScheme) {
        this.biomeScheme = biomeScheme;
    }

    @Override
    public void setSeed(long seed) {
        biomeScheme.setSeed(seed);
        cachedBiomes = null;
    }

    @Override
    public int getBiomeCount() {
        return biomeScheme.getBiomeCount();
    }

    @Override
    public int[] getBiomes(int x, int y, int width, int height) {
        return biomeScheme.getBiomes(x, y, width, height);
    }

    @Override
    public void getBiomes(int x, int y, int width, int height, int[] buffer) {
        biomeScheme.getBiomes(x, y, width, height, buffer);
    }

    @Override
    public String[] getBiomeNames() {
        return biomeScheme.getBiomeNames();
    }

    @Override
    public Set<Integer> getDryBiomes() {
        return biomeScheme.getDryBiomes();
    }

    @Override
    public Set<Integer> getColdBiomes() {
        return biomeScheme.getColdBiomes();
    }

    @Override
    public Set<Integer> getForestedBiomes() {
        return biomeScheme.getForestedBiomes();
    }

    @Override
    public Set<Integer> getSwampyBiomes() {
        return biomeScheme.getSwampyBiomes();
    }

    @Override
    public int getColour(int biome, ColourScheme colourScheme) {
        return biomeScheme.getColour(biome, colourScheme);
    }

    @Override
    public boolean[][] getPattern(int biome) {
        return biomeScheme.getPattern(biome);
    }

    @Override
    public String getBiomeName(int biome) {
        return biomeScheme.getBiomeName(biome);
    }

    @Override
    public boolean isBiomePresent(int biome) {
        return biomeScheme.isBiomePresent(biome);
    }
    
    public int getBiome(int x, int y) {
        if ((x < cachedBiomesX - BIOME_CACHE_RADIUS) || (x > cachedBiomesX + BIOME_CACHE_RADIUS)
                || (y < cachedBiomesY - BIOME_CACHE_RADIUS) || (y > cachedBiomesY + BIOME_CACHE_RADIUS)) {
            cachedBiomes = biomeScheme.getBiomes(x - BIOME_CACHE_RADIUS, y - BIOME_CACHE_RADIUS, BIOME_CACHE_SIZE, BIOME_CACHE_SIZE);
            cachedBiomesX = x;
            cachedBiomesY = y;
        }
        return cachedBiomes[(y - cachedBiomesY + BIOME_CACHE_RADIUS) * BIOME_CACHE_SIZE + x - cachedBiomesX + BIOME_CACHE_RADIUS];
    }
    
    private final BiomeScheme biomeScheme;
    private int[] cachedBiomes;
    private int cachedBiomesX = Integer.MIN_VALUE, cachedBiomesY = Integer.MIN_VALUE;
    
    private static final int BIOME_CACHE_RADIUS = 255;
    private static final int BIOME_CACHE_SIZE   = BIOME_CACHE_RADIUS * 2 + 1;
}