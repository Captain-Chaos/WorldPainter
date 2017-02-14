/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft;

/**
 *
 * @author pepijn
 */
public interface ChunkProvider extends AutoCloseable {
    boolean isChunkPresent(int x, int z);
    Chunk getChunk(int x, int z);
    Chunk getChunkForEditing(int x, int z);
    @Override void close();
}