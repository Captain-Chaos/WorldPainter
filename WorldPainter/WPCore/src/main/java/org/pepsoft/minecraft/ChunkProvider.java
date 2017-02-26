/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft;

/**
 * A provider of Minecraft chunks.
 *
 * <p>This API's coordinate system is the Minecraft coordinate system (W <- x -> E, N <- z -> S).
 *
 * @author pepijn
 */
public interface ChunkProvider extends AutoCloseable {
    /**
     * Determine whether a chunk is present.
     *
     * @param x The X coordinate to check.
     * @param z The Z coordinate to check.
     * @return <code>true</code> if the chunk provider contains a chunk at the
     * specified coordinates.
     */
    boolean isChunkPresent(int x, int z);

    /**
     * Retrieve a chunk. If the chunk does not exist, <code>null</code> is
     * returned. The returned chunk <em>may</em> be read-only.
     *
     * @param x The X coordinate of the chunk to retrieve.
     * @param z The Z coordinate of the chunk to retrieve.
     * @return The specified chunk, or <code>null</code> if there is no chunk at
     * the specified coordinates.
     */
    Chunk getChunk(int x, int z);

    /**
     * Retrieve a chunk for editing. If the chunk does not exist,
     * <code>null</code> <em>may</em> be returned, or the provider <em>may</em>
     * create a new, empty chunk. The returned chunk is guaranteed not to be
     * read-only.
     *
     * @param x The X coordinate of the chunk to retrieve.
     * @param z The Z coordinate of the chunk to retrieve.
     * @return The specified chunk, or <code>null</code> if there is no chunk at
     * the specified coordinates, and this chunk provider does not create new
     * chunks.
     */
    Chunk getChunkForEditing(int x, int z);

    /**
     * Close the chunk provider, flushing any changes and closing any system
     * resources.
     */
    @Override
    void close();
}