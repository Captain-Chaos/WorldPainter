/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.heightMaps;

import org.pepsoft.util.IconUtils;
import org.pepsoft.util.MathUtils;
import org.pepsoft.util.PerlinNoise;
import org.pepsoft.worldpainter.NoiseSettings;

import javax.swing.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Random;

import static org.pepsoft.worldpainter.Constants.LARGE_BLOBS;

/**
 *
 * @author pepijn
 */
public final class NoiseHeightMap extends AbstractHeightMap {
    public NoiseHeightMap(NoiseSettings noiseSettings) {
        // TODO WHY do we double the range?!
        this(null, noiseSettings.getRange() * 2, noiseSettings.getScale() / 5, noiseSettings.getRoughness() + 1, new Random().nextLong());
    }

    public NoiseHeightMap(NoiseSettings noiseSettings, long seedOffset) {
        // TODO WHY do we double the range?!
        this(null, noiseSettings.getRange() * 2, noiseSettings.getScale() / 5, noiseSettings.getRoughness() + 1, seedOffset);
    }

    public NoiseHeightMap(String name, NoiseSettings noiseSettings) {
        // TODO WHY do we double the range?!
        this(name, noiseSettings.getRange() * 2, noiseSettings.getScale() / 5, noiseSettings.getRoughness() + 1, new Random().nextLong());
    }

    public NoiseHeightMap(String name, NoiseSettings noiseSettings, long seedOffset) {
        // TODO WHY do we double the range?!
        this(name, noiseSettings.getRange() * 2, noiseSettings.getScale() / 5, noiseSettings.getRoughness() + 1, seedOffset);
    }

    public NoiseHeightMap(double height, double scale, int octaves) {
        this(null, height, scale, octaves, new Random().nextLong());
    }

    public NoiseHeightMap(double height, double scale, int octaves, long seedOffset) {
        this(null, height, scale, octaves, seedOffset);
    }

    public NoiseHeightMap(String name, double height, double scale, int octaves) {
        this(name, height, scale, octaves, new Random().nextLong());
    }

    public NoiseHeightMap(String name, double height, double scale, int octaves, long seedOffset) {
        setName(name);
        if (octaves > 10) {
            throw new IllegalArgumentException("More than 10 octaves not supported");
        }
        this.dHeight = height;
        this.scale = scale;
        this.octaves = octaves;
        this.seedOffset = seedOffset;
        perlinNoise = new PerlinNoise(seedOffset);
    }

    public double getHeight() {
        return dHeight;
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
    public void setSeed(long seed) {
        if (seed != this.seed) {
            super.setSeed(seed);
            perlinNoise.setSeed(seed + seedOffset);
        }
    }

    @Override
    public double getHeight(int x, int y) {
        return getValue(x, y);
    }

    @Override
    public double getHeight(float x, float y) {
        return getValue(x, y);
    }

    @Override
    public int getColour(int x, int y) {
        int value = (int) MathUtils.clamp(0L, Math.round(getHeight(x, y)), 255L);
        return (value << 16) | (value << 8) | value;
    }

    public double getValue(double x) {
        if (octaves == 1) {
            return (perlinNoise.getPerlinNoise(x / LARGE_BLOBS / scale) + 0.5) * dHeight;
        } else {
            double noise = 0;
            for (int i = 0; i < octaves; i++) {
                noise += perlinNoise.getPerlinNoise(x / LARGE_BLOBS / scale * FACTORS[i]);
            }
            noise /= octaves;
            return (noise + 0.5) * dHeight;
        }
    }

    public double getValue(double x, double y) {
        if (octaves == 1) {
            return (perlinNoise.getPerlinNoise(x / LARGE_BLOBS / scale, y / LARGE_BLOBS / scale) + 0.5) * dHeight;
        } else {
            double noise = 0;
            for (int i = 0; i < octaves; i++) {
                noise += perlinNoise.getPerlinNoise(x / LARGE_BLOBS / scale * FACTORS[i], y / LARGE_BLOBS / scale * FACTORS[i]);
            }
            noise /= octaves;
            return (noise + 0.5) * dHeight;
        }
    }

    public double getValue(double x, double y, double z) {
        if (octaves == 1) {
            return (perlinNoise.getPerlinNoise(x / LARGE_BLOBS / scale, y / LARGE_BLOBS / scale, z / LARGE_BLOBS / scale) + 0.5) * dHeight;
        } else {
            double noise = 0;
            for (int i = 0; i < octaves; i++) {
                noise += perlinNoise.getPerlinNoise(x / LARGE_BLOBS / scale * FACTORS[i], y / LARGE_BLOBS / scale * FACTORS[i], z / LARGE_BLOBS / scale * FACTORS[i]);
            }
            noise /= octaves;
            return (noise + 0.5) * dHeight;
        }
    }

    @Override
    public NoiseHeightMap clone() {
        NoiseHeightMap clone = new NoiseHeightMap(name, dHeight, scale, octaves, seedOffset);
        clone.setSeed(getSeed());
        return clone;
    }

    @Override
    public Icon getIcon() {
        return ICON_NOISE_HEIGHTMAP;
    }

    @Override
    public double[] getRange() {
        return new double[] {0.0, dHeight};
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (version == 0) {
            dHeight = height;
            height = 0.0f;
            version = 1;
        }
    }

    private final PerlinNoise perlinNoise;
    @Deprecated
    private float height;
    private double dHeight;
    private final double scale;
    private final int octaves;
    private final long seedOffset;
    private int version = 1;

    private static final int[] FACTORS = {1, 2, 4, 8, 16, 32, 64, 128, 256, 512};
    private static final long serialVersionUID = 1L;
    private static final Icon ICON_NOISE_HEIGHTMAP = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/noise.png");
}