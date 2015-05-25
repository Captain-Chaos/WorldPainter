/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.brushes;

import org.pepsoft.util.PerlinNoise;
import static org.pepsoft.worldpainter.Constants.*;

/**
 *
 * @author pepijn
 */
public abstract class AbstractBrush implements Brush, Cloneable {
    public AbstractBrush(String name) {
        this.name = name;
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public float getStrength(int centerX, int centerY, int x, int y) {
        return getStrength(x - centerX, y - centerY);
    }
    
    @Override
    public float getFullStrength(int centerX, int centerY, int x, int y) {
        return getFullStrength(x - centerX, y - centerY);
    }
    
    @Override
    public float getNoisyStrength(long seed, int centerX, int centerY, int x, int y) {
        return getNoisyStrength(seed, x, y, getStrength(centerX, centerY, x, y));
    }
    
    @Override
    public float getNoisyFullStrength(long seed, int centerX, int centerY, int x, int y) {
        return getNoisyStrength(seed, x, y, getFullStrength(centerX, centerY, x, y));
    }

    @Override
    public int getEffectiveRadius() {
        return getRadius();
    }

    @Override
    public AbstractBrush clone() {
        try {
            AbstractBrush clone = (AbstractBrush) super.clone();
            clone.perlinNoise = (PerlinNoise) perlinNoise.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
    
    private float getNoisyStrength(long seed, int x, int y, float strength) {
        float allowableNoiseRange = (0.5f - Math.abs(strength - 0.5f)) / 5;
//        System.out.print("strength: " + strength + ", allowableNoiseRange: " + allowableNoiseRange);
        if (perlinNoise.getSeed() != (seed + SEED_OFFSET)) {
            perlinNoise.setSeed(seed + SEED_OFFSET);
        }
        float noise = perlinNoise.getPerlinNoise(x / MEDIUM_BLOBS, y / MEDIUM_BLOBS);
//        System.out.print(", noise: " + noise + ", noise / NOISE_RANGE: " + (noise / NOISE_RANGE));
        strength = strength + noise * allowableNoiseRange * strength;
//        System.out.println(" -> strength: " + strength);
        if (strength < 0.0) {
            return 0.0f;
        } else if (strength > 1.0) {
            return 1.0f;
        } else {
            return strength;
        }
    }
    
    private final String name;
    private PerlinNoise perlinNoise = new PerlinNoise(0);
    
    private static final long SEED_OFFSET = 67;
}