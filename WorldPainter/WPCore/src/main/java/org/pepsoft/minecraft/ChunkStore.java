package org.pepsoft.minecraft;

import org.pepsoft.worldpainter.exception.WPRuntimeException;

import java.util.Set;

/**
 * A provider of Minecraft chunk storage.
 *
 * <p>Created by Pepijn on 14-12-2016.
 */
public interface ChunkStore extends ChunkProvider {
    /**
     * Return a total count of all the chunks currently in the store.
     *
     * @return The count of all the chunks currently in the store.
     */
    int getChunkCount();

    /**
     * Returns the coordinates of all the chunks that currently exist in the
     * store. This is intended to be more efficient and faster than actually
     * visiting all the chunks for use cases where only the existence and/or
     * coordinates are needed.
     *
     * @return A set of the coordinates of all the chunks that currently exist
     * in the store.
     */
    Set<MinecraftCoords> getChunkCoords();

    /**
     * Visit all known chunks.
     *
     * <p><strong>Note</strong> that the order is undefined, allowing the provider to use as efficient an implementation
     * as is possible. The provided chunks <em>may</em> be read-only.
     *
     * <p><strong>Also note</strong> that to improve performance the process may be parallelised. In other words the
     * visitor may be invoked concurrently for different chunks and must therefore be thread-safe. It also means that if
     * the visitor returns false, the iteration may not stop immediately.
     *
     * @param visitor The visitor to invoke for each chunk.
     * @return {@code true} if all chunks were visited; {@code false} if not all chunks may have been visited because
     * the visitor returned {@code false}.
     */
    boolean visitChunks(ChunkVisitor visitor);

    /**
     * Visit all known chunks for editing. Note that the order is undefined, allowing the provider to use as efficient
     * an implementation as is possible. The provided chunks are guaranteed not to be read-only, and are saved to the
     * chunk store after the visitor returns true (regardless of whether the visitor modified the chunk).
     *
     * @param visitor The visitor to invoke for each chunk.
     * @return {@code true} if all chunks were visited; {@code false} if not all chunks may have been visited because
     * the visitor returned {@code false}.
     */
    boolean visitChunksForEditing(ChunkVisitor visitor);

    /**
     * Save a chunk to the store. The chunk is only guaranteerd to have been
     * written to disk if this operation was performed inside a
     * {@link #doInTransaction(Runnable)}, or after {@link #flush()} has been
     * called.
     *
     * @param chunk The chunk to save.
     */
    void saveChunk(Chunk chunk);

    /**
     * Run a task, for example saving more than one chunk, in one transaction
     * against the chunk store, which may improve performance.
     *
     * @param task The task to execute in one chunk store transaction.
     */
    void doInTransaction(Runnable task);

    /**
     * Make sure all previously saved chunks are written to disk. Not necessary
     * for chunks which have been saved inside a
     * {@link #doInTransaction(Runnable)}.
     */
    void flush();

    /**
     * A visitor of chunks.
     */
    @FunctionalInterface interface ChunkVisitor {
        /**
         * Visit a chunk.
         *
         * <p>For convenience, the visitor may throw checked exceptions. They will be wrapped in a
         * {@link WPRuntimeException} if this happens.
         *
         * @param chunk The chunk to be visited.
         * @return {@code true} if more chunks should be visited, or {@code false} if no more chunks need to be visited.
         */
        boolean visitChunk(Chunk chunk) throws Exception;

        /**
         * This is called when a chunk is skipped due to a loading error, to
         * give the visitor the opportunity to record the error or abort the
         * process. When this is called {@link #visitChunk(Chunk)} is
         * <em>not</em> called for that chunk!
         *
         * <p>The default implementation just returns {@code true} so that the
         * process continues.
         *
         * @param coords The coordinates of the problematic chunk, or
         * {@code null} if they are not known or do not apply.
         * @param message A message describing the problem with the chunk
         * @return {@code true} if more chunks should be visited, or
         * {@code false} if no more chunks should be visited.
         */
        default boolean chunkError(MinecraftCoords coords, String message) {
            return true;
        }
    }
}