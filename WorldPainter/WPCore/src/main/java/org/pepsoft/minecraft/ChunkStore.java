package org.pepsoft.minecraft;

/**
 * A provider of Minecraft chunk storage.
 *
 * <p>Created by Pepijn on 14-12-2016.
 */
public interface ChunkStore extends ChunkProvider {
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
}