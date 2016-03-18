package org.pepsoft.worldpainter.dynmap;

import org.dynmap.DynmapWorld;
import org.dynmap.utils.MapChunkCache;
import org.dynmap.utils.MapIterator;
import org.dynmap.utils.VisibilityLimit;
import org.pepsoft.worldpainter.objects.WPObject;

/**
 * Implementation of {@link MapChunkCache} used by {@link WPObjectDynmapWorld}.
 *
 * <p>Created by Pepijn Schmitz on 09-06-15.
 */
class WPObjectMapChunkCache extends MapChunkCache {
    WPObjectMapChunkCache(WPObjectDynmapWorld world, WPObject object) {
        this.world = world;
        this.object = object;
    }

    @Override
    public boolean setChunkDataTypes(boolean blockdata, boolean biome, boolean highestblocky, boolean rawbiome) {
        return (! biome) && (! rawbiome);
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
        return false;
    }

    @Override
    public MapIterator getIterator(int x, int y, int z) {
        return new WPObjectMapIterator(object, x, y, z);
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
        return world;
    }

    private final WPObjectDynmapWorld world;
    private final WPObject object;
}