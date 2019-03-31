package org.pepsoft.minecraft;

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
    long getChunkCount();

    /**
     * Visit all known chunks. Note that the order is undefined, allowing the
     * provider to use as efficient an implementation as is possible. The
     * provided chunks <em>may</em> be read-only.
     *
     * @param visitor The visitor to invoke for each chunk.
     * @return {@code true} if all chunks were visited; {@code false} if not all
     * chunks may have been visited because the visitor returned {@code false}.
     */
    boolean visitChunks(ChunkVisitor visitor);

    /**
     * Visit all known chunks for editing. Note that the order is undefined,
     * allowing the provider to use as efficient an implementation as is
     * possible. The provided chunks are guaranteed not to be read-only.
     *
     * @param visitor The visitor to invoke for each chunk.
     * @return {@code true} if all chunks were visited; {@code false} if not all
     * chunks may have been visited because the visitor returned {@code false}.
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
         * @param chunk The chunk to be visited.
         * @return {@code true} if more chunks should be visited, or
         * {@code false} if no more chunks need to be visited.
         */
        boolean visitChunk(Chunk chunk);
    }
}