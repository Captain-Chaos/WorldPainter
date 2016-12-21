package org.pepsoft.worldpainter.exporting;

import org.pepsoft.minecraft.Chunk;
import org.pepsoft.minecraft.ChunkStore;

import java.io.File;
import java.util.LinkedList;

/**
 * Created by Pepijn on 14-12-2016.
 */
public class MCPEChunkStore implements ChunkStore {
    public MCPEChunkStore(File worldDir, int dimension) {
        this.worldDir = worldDir;
        this.dimension = dimension;
    }

    @Override
    public void saveChunk(Chunk chunk) {

    }

    @Override
    public synchronized void flush() {

    }

    @Override
    public boolean isChunkPresent(int x, int z) {
        return false;
    }

    @Override
    public synchronized Chunk getChunk(int x, int z) {
        return null;
    }

    @Override
    public synchronized Chunk getChunkForEditing(int x, int z) {
        return null;
    }

    private final File worldDir;
    private final int dimension;
    private final LinkedList<Chunk> chunks = new LinkedList<>();
}