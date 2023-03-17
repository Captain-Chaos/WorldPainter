package org.pepsoft.worldpainter.plugins;

import org.pepsoft.minecraft.Chunk;
import org.pepsoft.minecraft.ChunkStore;
import org.pepsoft.worldpainter.Constants;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.exporting.PostProcessor;

import java.io.File;

/**
 * A {@link PlatformProvider} for Minecraft-like
 * {@link Platform.Capability#BLOCK_BASED block based} platforms based on 16x16 chunks.
 */
public interface BlockBasedPlatformProvider extends PlatformProvider {
    /**
     * Determine which dimensions are present in the map specified by a
     * particular {@link Platform} and directory.
     *
     * @param platform The platform for which to determine the dimensions.
     * @param worldDir The map base directory for which to determine the
     *                 dimensions.
     * @return An array of dimension numbers corresponding to the constants
     * {@link Constants#DIM_NORMAL}, {@link Constants#DIM_NETHER} and
     * {@link Constants#DIM_END} for dimensions which correspond to vanilla
     * Minecraft dimensions.
     */
    int[] getDimensions(Platform platform, File worldDir);

//    /**
//     * Create a new, empty chunk for a platform supported by this provider with a lower build limit of zero.
//     *
//     * <p>The default implementation delegates to {@link #createChunk(Platform, int, int, int, int)} with a
//     * {@code minHeight} of zero.
//     *
//     * <p>Implementations MUST implement at least on of {@link #createChunk(Platform, int, int, int, int)} or
//     * {@link #createChunk(Platform, int, int, int)}.
//     *
//     * @param platform The platform for which to create a chunk.
//     * @param x The X coordinate (in chunks) of the chunk to create.
//     * @param z The Z coordinate (in chunks) of the chunk to create.
//     * @param maxHeight The height (in blocks) of the chunk to create.
//     * @return The newly created chunk.
//     */
//    default Chunk createChunk(Platform platform, int x, int z, int maxHeight) {
//        return createChunk(platform, x, z, 0, maxHeight);
//    }

    /**
     * Create a new, empty chunk for a platform supported by this provider.
     *
     * <p>The default implementation delegates to {@link #createChunk(Platform, int, int, int)} if {@code minHeight} is
     * zero, or throws an {@link IllegalArgumentException} otherwise.
     *
     * <p>Implementations MUST implement at least on of {@link #createChunk(Platform, int, int, int, int)} or
     * {@link #createChunk(Platform, int, int, int)}.
     *
     * @param platform The platform for which to create a chunk.
     * @param x The X coordinate (in chunks) of the chunk to create.
     * @param z The Z coordinate (in chunks) of the chunk to create.
     * @param minHeight The depth (in blocks) ot the chunk to create.
     * @param maxHeight The height (in blocks) of the chunk to create.
     * @return The newly created chunk.
     */
    default Chunk createChunk(Platform platform, int x, int z, int minHeight, int maxHeight) {
//        if (minHeight == 0) {
//            return createChunk(platform, x, z, maxHeight);
//        } else {
            throw new IllegalArgumentException("minHeight " + minHeight);
//        }
    }

    /**
     * Obtain a {@link ChunkStore} which will save chunks in the format of the
     * platform, for a platform supported by this provider and for a specific
     * map base directory and dimension number.
     *
     * @param platform The platform for which to provide a chunk store.
     * @param worldDir The map base directory for which to provide a chunk
     *                 store.
     * @param dimension The dimension number for which to provide a chunk store.
     * @return A chunk store which will write chunks in the appropriate format
     *     for the specified dimension under the specified base directory.
     */
    ChunkStore getChunkStore(Platform platform, File worldDir, int dimension);

    /**
     * Obtain a {@link PostProcessor} for a platform supported by this provider.
     *
     * @param platform The platform for which to provide a post processor.
     * @return A post processor for the specified platform.
     */
    PostProcessor getPostProcessor(Platform platform);
}