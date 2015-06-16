/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.heightMaps;

import java.awt.Rectangle;
import java.util.Random;
import org.pepsoft.util.MathUtils;
import org.pepsoft.util.PerlinNoise;
import org.pepsoft.worldpainter.HeightMap;
import static org.pepsoft.worldpainter.Constants.*;

/**
 *
 * @author pepijn
 */
public final class NoiseHeightMap implements HeightMap {
    public NoiseHeightMap(float range, double scale, int octaves) {
        this(null, range, scale, octaves, new Random().nextLong());
    }
    
    public NoiseHeightMap(float range, double scale, int octaves, long seedOffset) {
        this(null, range, scale, octaves, seedOffset);
    }

    public NoiseHeightMap(String name, float range, double scale, int octaves) {
        this(name, range, scale, octaves, new Random().nextLong());
    }
    
    public NoiseHeightMap(String name, float range, double scale, int octaves, long seedOffset) {
        if (octaves > 10) {
            throw new IllegalArgumentException("More than 10 octaves not supported");
        }
        this.range = range;
        this.scale = scale;
        this.octaves = octaves;
        this.seedOffset = seedOffset;
        this.name = name;
        perlinNoise = new PerlinNoise(seedOffset);
    }

    public float getRange() {
        return range;
    }

    public double getScale() {
        return scale;
    }

    public int getOctaves() {
        return octaves;
    }

    public long getSeedOffset() {
        return seedOffset;
    }

    // HeightMap
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public long getSeed() {
        return perlinNoise.getSeed() - seedOffset;
    }

    @Override
    public void setSeed(long seed) {
        if ((perlinNoise.getSeed() - seedOffset) != seed) {
            perlinNoise.setSeed(seed + seedOffset);
        }
    }

    @Override
    public float getHeight(int x, int y) {
        return getValue((double) x, (double) y);
    }

    @Override
    public int getColour(int x, int y) {
        int value = MathUtils.clamp(0, (int) (getHeight(x, y) + 0.5f), 255);
        return (value << 16) | (value << 8) | value;
    }

    @Override
    public Rectangle getExtent() {
        return null;
    }

    public float getValue(double x) {
        if (octaves == 1) {
            return (perlinNoise.getPerlinNoise(x / LARGE_BLOBS / scale) + 0.5f) * range;
        } else {
            float noise = 0;
            for (int i = 0; i < octaves; i++) {
                noise += perlinNoise.getPerlinNoise(x / LARGE_BLOBS / scale * FACTORS[i]);
            }
            noise /= octaves;
            return (noise + 0.5f) * range;
        }
    }
    
    public float getValue(double x, double y) {
        if (octaves == 1) {
            return (perlinNoise.getPerlinNoise(x / LARGE_BLOBS / scale, y / LARGE_BLOBS / scale) + 0.5f) * range;
        } else {
            float noise = 0;
            for (int i = 0; i < octaves; i++) {
                noise += perlinNoise.getPerlinNoise(x / LARGE_BLOBS / scale * FACTORS[i], y / LARGE_BLOBS / scale * FACTORS[i]);
            }
            noise /= octaves;
            return (noise + 0.5f) * range;
        }
    }

    public float getValue(double x, double y, double z) {
        if (octaves == 1) {
            return (perlinNoise.getPerlinNoise(x / LARGE_BLOBS / scale, y / LARGE_BLOBS / scale, z / LARGE_BLOBS / scale) + 0.5f) * range;
        } else {
            float noise = 0;
            for (int i = 0; i < octaves; i++) {
                noise += perlinNoise.getPerlinNoise(x / LARGE_BLOBS / scale * FACTORS[i], y / LARGE_BLOBS / scale * FACTORS[i], z / LARGE_BLOBS / scale * FACTORS[i]);
            }
            noise /= octaves;
            return (noise + 0.5f) * range;
        }
    }
    
    @Override
    public float getBaseHeight() {
        return 0.0f;
    }

    @Override
    public NoiseHeightMap clone() {
        NoiseHeightMap clone = new NoiseHeightMap(name, range, scale, octaves, seedOffset);
        clone.setSeed(getSeed());
        return clone;
    }
    
    public static void main(String[] args) {
        System.out.print('{');
        for (int i = 1; i <= 10; i++) {
            System.out.print(Math.pow(2.0, i - 1));
        }
    }
    
    private final PerlinNoise perlinNoise;
    private final float range;
    private final double scale;
    private final int octaves;
    private final long seedOffset;
    private final String name;
    
    private static final int[] FACTORS = {1, 2, 4, 8, 16, 32, 64, 128, 256, 512};
    private static final long serialVersionUID = 1L;
}