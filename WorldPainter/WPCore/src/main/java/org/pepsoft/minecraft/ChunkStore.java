package org.pepsoft.minecraft;

/**
 * Created by Pepijn on 14-12-2016.
 */
public interface ChunkStore extends ChunkProvider {
    void saveChunk(Chunk chunk);
    void flush();
}