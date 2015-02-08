/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.util;

/**
 *
 * @author pepijn
 */
public final class RandomField {
    public RandomField(int bits, double scale, long seed) {
        this.bits = bits;
        this.scale = scale;
        noiseGenerators = new PerlinNoise[bits];
        for (int i = 0; i < bits; i++) {
            noiseGenerators[i] = new PerlinNoise(seed + i);
        }
    }
    
    public int getValue(int x) {
        return getValue(x, 0, 0);
    }

    public int getValue(int x, int y) {
        return getValue(x, y, 0);
    }

    public int getValue(int x, int y, int z) {
        final double dX = x / scale, dY = y / scale, dZ = z / scale;
        switch (bits) {
            case 1:
                return noiseGenerators[0].getPerlinNoise(dX, dY, dZ) > 0 ? 1 : 0;
            case 2:
                return (noiseGenerators[0].getPerlinNoise(dX, dY, dZ) > 0 ? 2 : 0)
                     | (noiseGenerators[1].getPerlinNoise(dX, dY, dZ) > 0 ? 1 : 0);
            case 3:
                return (noiseGenerators[0].getPerlinNoise(dX, dY, dZ) > 0 ? 4 : 0)
                     | (noiseGenerators[1].getPerlinNoise(dX, dY, dZ) > 0 ? 2 : 0)
                     | (noiseGenerators[2].getPerlinNoise(dX, dY, dZ) > 0 ? 1 : 0);
            case 4:
                return (noiseGenerators[0].getPerlinNoise(dX, dY, dZ) > 0 ? 8 : 0)
                     | (noiseGenerators[1].getPerlinNoise(dX, dY, dZ) > 0 ? 4 : 0)
                     | (noiseGenerators[2].getPerlinNoise(dX, dY, dZ) > 0 ? 2 : 0)
                     | (noiseGenerators[3].getPerlinNoise(dX, dY, dZ) > 0 ? 1 : 0);
            default:
                int value = 0;
                for (int i = 0; i < bits; i++) {
                    value = (value << 1) | (noiseGenerators[i].getPerlinNoise(dX, dY, dZ) > 0 ? 1 : 0);
                }
                return value;
        }
    }
    
    // Properties
    
    public int getBits() {
        return bits;
    }

    public double getScale() {
        return scale;
    }

    public long getSeed() {
        return noiseGenerators[0].getSeed();
    }

    public void setSeed(long seed) {
        for (int i = 0; i < bits; i++) {
            noiseGenerators[i].setSeed(seed + i);
        }
    }
    
    private final int bits;
    private final double scale;
    private final PerlinNoise[] noiseGenerators;
}