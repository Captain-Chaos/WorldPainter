package org.pepsoft.minecraft;

import org.pepsoft.minecraft.mcpe.MCPEChunk;
import org.pepsoft.minecraft.mcpe.MCPEConstants;
import org.pepsoft.worldpainter.GameType;
import org.pepsoft.worldpainter.Generator;
import org.pepsoft.worldpainter.exporting.JavaChunkStore;
import org.pepsoft.worldpainter.exporting.MCPEChunkStore;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.pepsoft.worldpainter.Constants.*;
import static org.pepsoft.worldpainter.GameType.*;
import static org.pepsoft.worldpainter.Generator.*;

/**
 * A Minecraft platform.
 *
 * <p>Created by Pepijn on 11-12-2016.
 */
public enum Platform {
    JAVA_MCREGION("Java/MCRegion") {
        @Override
        public Chunk createChunk(int x, int z, int maxHeight) {
            return new ChunkImpl(x, z, maxHeight);
        }

        @Override
        public boolean supportsBiomes() {
            return false;
        }

        @Override
        public int getStandardMaxHeight() {
            return Constants.DEFAULT_MAX_HEIGHT_1;
        }

        @Override
        public boolean supportsNonStandardHeights() {
            return true;
        }

        @Override
        public List<GameType> getGameTypes() {
            return Arrays.asList(SURVIVAL, CREATIVE);
        }

        @Override
        public List<Generator> getGenerators() {
            return Arrays.asList(DEFAULT, FLAT);
        }

        @Override
        public ChunkStore getChunkStore(File worldDir, int dimension) {
            File regionDir;
            switch (dimension) {
                case DIM_NORMAL:
                    regionDir = new File(worldDir, "regions");
                    break;
                case DIM_NETHER:
                    regionDir = new File(worldDir, "DIM-1/regions");
                    break;
                case DIM_END:
                    regionDir = new File(worldDir, "DIM1/regions");
                    break;
                default:
                    throw new IllegalArgumentException("Dimension " + dimension + " not supported");
            }
            return new JavaChunkStore(JAVA_MCREGION, regionDir, false, null, getStandardMaxHeight());
        }
    },

    JAVA_ANVIL("Java/Anvil") {
        @Override
        public Chunk createChunk(int x, int z, int maxHeight) {
            return new ChunkImpl2(x, z, maxHeight);
        }

        @Override
        public boolean supportsBiomes() {
            return true;
        }

        @Override
        public int getStandardMaxHeight() {
            return Constants.DEFAULT_MAX_HEIGHT_2;
        }

        @Override
        public boolean supportsNonStandardHeights() {
            return false;
        }

        @Override
        public List<GameType> getGameTypes() {
            return Arrays.asList(SURVIVAL, CREATIVE, ADVENTURE, HARDCORE);
        }

        @Override
        public List<Generator> getGenerators() {
            return Arrays.asList(DEFAULT, FLAT, LARGE_BIOMES);
        }

        @Override
        public ChunkStore getChunkStore(File worldDir, int dimension) {
            File regionDir;
            switch (dimension) {
                case DIM_NORMAL:
                    regionDir = new File(worldDir, "regions");
                    break;
                case DIM_NETHER:
                    regionDir = new File(worldDir, "DIM-1/regions");
                    break;
                case DIM_END:
                    regionDir = new File(worldDir, "DIM1/regions");
                    break;
                default:
                    throw new IllegalArgumentException("Dimension " + dimension + " not supported");
            }
            return new JavaChunkStore(JAVA_ANVIL, regionDir, false, null, getStandardMaxHeight());
        }
    },

    MCPE("MCPE") {
        @Override
        public Chunk createChunk(int x, int z, int maxHeight) {
            if (maxHeight != MCPEConstants.MAX_HEIGHT) {
                throw new IllegalArgumentException("maxHeight " + maxHeight);
            }
            return new MCPEChunk(x, z);
        }

        @Override
        public boolean supportsBiomes() {
            return true;
        }

        @Override
        public int getStandardMaxHeight() {
            return MCPEConstants.MAX_HEIGHT;
        }

        @Override
        public boolean supportsNonStandardHeights() {
            return false;
        }

        @Override
        public List<GameType> getGameTypes() {
            return Arrays.asList(SURVIVAL, CREATIVE);
        }

        @Override
        public List<Generator> getGenerators() {
            return Arrays.asList(DEFAULT, FLAT);
        }

        @Override
        public ChunkStore getChunkStore(File worldDir, int dimension) {
            return new MCPEChunkStore(worldDir, dimension);
        }
    };

    Platform(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Create a new, empty chunk.
     *
     * @param x The X coordinate (in chunks) of the chunk to create.
     * @param z The Z coordinate (in chunks) of the chunk to create.
     * @param maxHeight The height (in blocks) of the chunk to create.
     * @return The newly created chunk.
     */
    public abstract Chunk createChunk(int x, int z, int maxHeight);

    /**
     * Indicates whether this platform supports storing biome IDs.
     *
     * @return <code>true</code> iff the platform supports storing biome IDs.
     */
    public abstract boolean supportsBiomes();

    /**
     * Indicates whether this platform supports heights other than
     * {@link #getStandardMaxHeight()}.
     *
     * @return <code>true</code> iff the platform supports heights other than
     *     {@link #getStandardMaxHeight()}.
     */
    public abstract boolean supportsNonStandardHeights();

    /**
     * Get the default height of maps for this platform. If
     * {@link #supportsNonStandardHeights()} returns <code>false</code> this is
     * the <em>only</em> height supported.
     *
     * @return The default height of maps for this platform.
     */
    public abstract int getStandardMaxHeight();

    /**
     * Get the list of game types supported by this platform.
     *
     * @return The list of game types supported by this platform.
     */
    public abstract List<GameType> getGameTypes();

    /**
     * Get the list of generators supported by this platform.
     *
     * @return The list of generators supported by this platform.
     */
    public abstract List<Generator> getGenerators();

    /**
     * Obtain a {@link ChunkStore} which will save chunks in the format of the
     * platform, for a specific map base directory and dimension number.
     *
     * @param worldDir The map base directory for which to provide a chunk
     *                 store.
     * @param dimension The dimension number for which to provide a chunk store.
     * @return A chunk store which will write chunks in the appropriate format
     *     for the specified dimension under the specified base directory.
     */
    public abstract ChunkStore getChunkStore(File worldDir, int dimension);

    /**
     * Get the human-friendly display name of this platform.
     *
     * @return The human-friendly display name of this platform.
     */
    public String getDisplayName() {
        return displayName;
    }

    private final String displayName;
}