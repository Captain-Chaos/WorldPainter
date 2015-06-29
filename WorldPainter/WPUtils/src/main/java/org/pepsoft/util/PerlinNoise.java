/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.util;

import com.kenperlin.ImprovedNoise;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.Serializable;

/**
 *
 * @author pepijn
 */
public final class PerlinNoise implements Serializable, Cloneable {
    public PerlinNoise(long seed) {
        this.seed = seed;
        improvedNoise = new ImprovedNoise(seed);
    }

    public long getSeed() {
        return seed;
    }

    public void setSeed(long seed) {
        if (seed != this.seed) {
            this.seed = seed;
            improvedNoise = new ImprovedNoise(seed);
        }
    }

    /**
     * Generates one dimensional noise. The input value is normalised to be
     * between 0 (inclusive) and 256 (exclusive), so the input should be
     * constrained to be between those values for best results (otherwise the
     * pattern will start to repeat).
     *
     * @param x The point for which to determine the noise value.
     * @return A noise value between -0.5 and 0.5.
     */
    public float getPerlinNoise(double x) {
        return (float) (improvedNoise.noise(x, 0, 0) * FACTOR);
    }

    /**
     * Generates two dimensional noise. The input values are normalised to be
     * between 0 (inclusive) and 256 (exclusive), so the input should be
     * constrained to be between those values for best results (otherwise the
     * pattern will start to repeat).
     *
     * @param x The X coordinate of the point for which to determine the noise
     *     value.
     * @param y The Y coordinate of the point for which to determine the noise
     *     value.
     * @return A noise value between -0.5 and 0.5.
     */
    public float getPerlinNoise(double x, double y) {
        return (float) (improvedNoise.noise(x, y, 0) * FACTOR);
    }

    /**
     * Generates three dimensional noise. The input values are normalised to be
     * between 0 (inclusive) and 256 (exclusive), so the input should be
     * constrained to be between those values for best results (otherwise the
     * pattern will start to repeat).
     *
     * @param x The X coordinate of the point for which to determine the noise
     *     value.
     * @param y The Y coordinate of the point for which to determine the noise
     *     value.
     * @param z The Z coordinate of the point for which to determine the noise
     *     value.
     * @return A noise value between -0.5 and 0.5.
     */
    public float getPerlinNoise(double x, double y, double z) {
        return (float) (improvedNoise.noise(x, y, z) * FACTOR);
    }
    
    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public static float getLevelForPromillage(int promillage) {
        return getLevelForPromillage((float) promillage);
    }
    
    public static float getLevelForPromillage(float promillage) {
        if ((promillage < 0f) || (promillage > 1000f)) {
            throw new IllegalArgumentException();
        }
        promillage *= 10;
        if (promillage == (int) promillage) {
            return LEVEL_FOR_PROMILLAGE[(int) promillage];
        } else {
            float level1 = LEVEL_FOR_PROMILLAGE[(int) promillage];
            return level1 + (LEVEL_FOR_PROMILLAGE[((int) promillage) + 1] - level1) * (promillage - (int) promillage);
        }
    }
    
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        // Legacy maps
        if (improvedNoise == null) {
            improvedNoise = new ImprovedNoise(seed);
        }
    }

    private long seed;
    private ImprovedNoise improvedNoise;

    private static final double FACTOR = 0.5;
    private static final long serialVersionUID = 2011040701L;

    private static final float[] LEVEL_FOR_PROMILLAGE = new float[10001];
    
    static {
        // Initialise the array from a file, because it is too large to
        // initialise with a static initialiser. It generates a "code too large"
        // error.
        try {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(PerlinNoise.class.getResourceAsStream("noiselevels.txt")))) {
                int index = 0;
                String line;
                while ((line = in.readLine()) != null) {
                    for (String token : line.split("[ ,]+")) {
                        LEVEL_FOR_PROMILLAGE[index++] = Float.parseFloat(token);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("I/O error while trying to load noise levels from classpath", e);
        }
    }
}