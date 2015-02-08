/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter;

import org.pepsoft.minecraft.HeightMapChunkFactory;
import org.pepsoft.util.PerlinNoise;
import static org.pepsoft.worldpainter.Constants.*;

/**
 *
 * @author pepijn
 */
public class NoiseChunkFactory extends HeightMapChunkFactory {
    public NoiseChunkFactory(int maxHeight, int version) {
        super(maxHeight, version);
    }
    
    @Override
    protected int getHeight(int x, int z) {
        return (int) Math.round(perlinNoise.getPerlinNoise(x / LARGE_BLOBS, z / LARGE_BLOBS) * RANGE + 36);
    }

    private final PerlinNoise perlinNoise = new PerlinNoise(System.currentTimeMillis());

    public static final double RANGE = 20;
}