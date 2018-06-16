package org.pepsoft.worldpainter.dynmap;

import org.dynmap.DynmapWorld;
import org.dynmap.utils.MapChunkCache;
import org.dynmap.utils.MapIterator;
import org.dynmap.utils.VisibilityLimit;
import org.pepsoft.minecraft.Chunk;
import org.pepsoft.minecraft.MC12AnvilChunk;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;

/**
 * Implementation of {@link MapChunkCache} used by {@link WPDynmapWorld}.
 *
 * <p>Created by Pepijn Schmitz on 05-06-15.
 */
class WPMapChunkCache extends MapChunkCache {
    WPMapChunkCache(DynmapWorld dmWorld, MinecraftWorld mcWorld) {
        this.dmWorld = dmWorld;
        this.mcWorld = mcWorld;
    }

    @Override
    public boolean setChunkDataTypes(boolean blockdata, boolean biome, boolean highestblocky, boolean rawbiome) {
        return !rawbiome;
    }

    @Override
    public int loadChunks(int maxToLoad) {
        return 0;
    }

    @Override
    public boolean isDoneLoading() {
        return true;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public void unloadChunks() {
        // Do nothing
    }

    @Override
    public boolean isEmptySection(int sx, int sy, int sz) {
        Chunk chunk = mcWorld.getChunk(sx, sz);
        if (chunk instanceof MC12AnvilChunk) {
            return ! ((MC12AnvilChunk) chunk).isSectionPresent(sy);
        } else {
            return chunk == null;
        }
    }

    @Override
    public MapIterator getIterator(int x, int y, int z) {
        return new WPMapIterator(mcWorld, x, y, z);
    }

    @Override
    public void setHiddenFillStyle(HiddenChunkStyle style) {
        // Do nothing
    }

    @Override
    public void setVisibleRange(VisibilityLimit limit) {
        // Do nothing
    }

    @Override
    public void setHiddenRange(VisibilityLimit limit) {
        // Do nothing
    }

    @Override
    public DynmapWorld getWorld() {
        return dmWorld;
    }

    private final DynmapWorld dmWorld;
    private final MinecraftWorld mcWorld;
}