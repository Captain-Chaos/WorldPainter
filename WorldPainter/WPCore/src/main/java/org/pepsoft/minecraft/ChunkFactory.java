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
         *     <li>{@link #TERRAIN_GENERATION}
         *
         *     <li>{@link #SEEDS}
         *     <li>{@link #BORDER_CHUNKS}
         *     <li>{@link #POST_PROCESSING}
         *     <li>{@link #BLOCK_PROPERTIES}
         *     <li>{@link #DISK_WRITING}
         * </ul>
         */
        public final Map<Object, AtomicLong> timings = new ConcurrentHashMap<>();

        /**
         * Creating the chunk and generating terrain and water or lava (excluding border and wall chunks).
         */
        public static final Object TERRAIN_GENERATION = "Terrain";

        /**
         * Post-processing the generated chunks (including border and wall chunks).
         */
        public static final Object POST_PROCESSING = "Post processing";

        /**
         * Creating border or wall chunks (including layers but excluding post-processing).
         */
        public static final Object BORDER_CHUNKS = "Border chunks";

        /**
         * Exporting the {@link Garden} seeds.
         */
        public static final Object SEEDS = "Seeds";

        /**
         * Calculating and propagating block properties such as lighting and leaf distances.
         */
        public static final Object BLOCK_PROPERTIES = "Block properties";

        /**
         * Saving the generated chunks to disk.
         */
        public static final Object DISK_WRITING = "Saving";

        /**
         * Applying region-straddling layers along region boundaries, not differentiated by layer.
         */
        public static final Object FIXUPS = "Fixups";
    }
}