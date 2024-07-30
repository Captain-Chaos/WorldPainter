/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft;

import org.pepsoft.worldpainter.gardenofeden.Garden;
import org.pepsoft.worldpainter.layers.Layer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

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
     *     statistics about it, or {@code null} if no chunk is present at
     *     the specified coordinates.
     */
    ChunkCreationResult createChunk(int x, int z);

    /**
     * Get the depth of the chunks this factory will create.
     *
     * @return The depth of the chunks this factory will create.
     */
    int getMinHeight();

    /**
     * Get the height of the chunks this chunk factory will create.
     *
     * @return The height of the chunks this factory will create.
     */
    int getMaxHeight();

    class ChunkCreationResult {
        public Chunk chunk;
        public final Stats stats = new Stats();
        public String errors, warnings;
    }

    class Stats {
        public long surfaceArea, landArea, waterArea, size, time;
        /**
         * Total time spent on each stage in nanoseconds. A stage can be:
         *
         * <ul>
         *     <li>A {@link Layer}
         *     <li>A {@link Stage}
         * </ul>
         */
        public final Map<Object, AtomicLong> timings = new ConcurrentHashMap<>();

    }
    
    enum Stage {
        /**
         * Creating the chunk and generating terrain and water or lava (excluding border and wall chunks).
         */
        TERRAIN_GENERATION("Terrain", "Generating terrain, water and lava"),

        /**
         * Post-processing the generated chunks (including border and wall chunks).
         */
        POST_PROCESSING("Post processing", "Post-processing all chunks"),

        /**
         * Creating border or wall chunks (including layers but excluding post-processing).
         */
        BORDER_CHUNKS("Border chunks", "Creating border chunks"),

        /**
         * Exporting the {@link Garden} seeds.
         */
        SEEDS("Seeds", "Exporting seeds"),

        /**
         * Calculating and propagating block properties such as lighting and leaf distances.
         */
        BLOCK_PROPERTIES( "Block properties", "Calculating light and/or leaf distances"),

        /**
         * Saving the generated chunks to disk.
         */
        DISK_WRITING ("Saving", "Saving chunks to disk"),

        /**
         * Applying region-straddling layers along region boundaries, not differentiated by layer.
         */
        FIXUPS("Fixups", "Fixing up region boundaries");

        private final String name, description;

        Stage(String name, String description) {
            this.name = name;
            this.description = description;
        }
    }
}