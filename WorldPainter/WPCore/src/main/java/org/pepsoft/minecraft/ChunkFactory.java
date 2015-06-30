/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft;

/**
 *
 * @author pepijn
 */
public interface ChunkFactory {
    /**
     * Create the chunk at the specified location, if present. No lighting need
     * be performed.
     *
     * @param x The X coordinate in the Minecraft coordinate space of the chunk
     *     to generate.
     * @param z The Z coordinate in the Minecraft coordinate space of the chunk
     *     to generate.
     * @return The generated chunk, in a data structure along with some
     *     statistics about it, or <code>null</code> if no chunk is present at
     *     the specified coordinates.
     */
    ChunkCreationResult createChunk(int x, int z);
    
    /**
     * Get the height of the chunks this chunk factory will create.
     * 
     * @return The height of the chunks this factory will create.
     */
    int getMaxHeight();

    class ChunkCreationResult {
        public Chunk chunk;
        public final Stats stats = new Stats();
    }
    
    class Stats {
        public long surfaceArea, landArea, waterArea, size, time;
    }
}