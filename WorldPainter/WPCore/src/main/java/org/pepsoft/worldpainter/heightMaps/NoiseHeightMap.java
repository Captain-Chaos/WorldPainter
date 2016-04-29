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
import java.awt.*;
import java.util.Random;

import static org.pepsoft.worldpainter.Constants.LARGE_BLOBS;

/**
 *
 * @author pepijn
 */
public final class NoiseHeightMap extends AbstractHeightMap {
    public NoiseHeightMap(NoiseSettings noiseSettings) {
        this(null, noiseSettings.getRange() * 2, noiseSettings.getScale() / 5, noiseSettings.getRoughness() + 1, new Random().nextLong());
    }

    public NoiseHeightMap(NoiseSettings noiseSettings, long seedOffset) {
        this(null, noiseSettings.getRange() * 2, noiseSettings.getScale() / 5, noiseSettings.getRoughness() + 1, seedOffset);
    }

    public NoiseHeightMap(String name, NoiseSettings noiseSettings) {
        this(name, noiseSettings.getRange() * 2, noiseSettings.getScale() / 5, noiseSettings.getRoughness() + 1, new Random().nextLong());
    }

    public NoiseHeightMap(String name, NoiseSettings noiseSettings, long seedOffset) {
        this(name, noiseSettings.getRange() * 2, noiseSettings.getScale() / 5, noiseSettings.getRoughness() + 1, seedOffset);
    }

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
        setName(name);
        if (octaves > 10) {
            throw new IllegalArgumentException("More than 10 octaves not supported");
        }
        this.range = range;
        this.scale = scale;
        this.octaves = octaves;
        this.seedOffset = seedOffset;
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

    public void setOctaves(int octaves) {
        this.octaves = octaves;
    }

    public void setRange(float range) {
        this.range = range;
    }

    public void setScale(double scale) {
        this.scale = scale;
    }

    public void setSeedOffset(long seedOffset) {
        if (seedOffset != this.seedOffset) {
            this.seedOffset = seedOffset;
            perlinNoise.setSeed(seed + seedOffset);
        }
    }

    public void setNoiseSettings(NoiseSettings noiseSettings) {
        range = noiseSettings.getRange() * 2;
        scale = noiseSettings.getScale() / 5;
        octaves = noiseSettings.getRoughness() + 1;
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
    public float getHeight(int x, int y) {
        return getValue((double) x, (double) y);
    }

    @Override
    public float getHeight(float x, float y) {
        return getValue(x, y);
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

    @Override
    public Icon getIcon() {
        return ICON_NOISE_HEIGHTMAP;
    }

    private PerlinNoise perlinNoise;
    private float range;
    private double scale;
    private int octaves;
    private long seedOffset;

    private static final int[] FACTORS = {1, 2, 4, 8, 16, 32, 64, 128, 256, 512};
    private static final long serialVersionUID = 1L;
    private static final Icon ICON_NOISE_HEIGHTMAP = IconUtils.loadIcon("org/pepsoft/worldpainter/icons/noise.png");
}