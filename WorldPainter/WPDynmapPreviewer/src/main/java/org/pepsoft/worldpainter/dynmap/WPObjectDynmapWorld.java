package org.pepsoft.worldpainter.dynmap;

import org.dynmap.DynmapChunk;
import org.dynmap.DynmapLocation;
import org.dynmap.DynmapWorld;
import org.dynmap.utils.MapChunkCache;
import org.pepsoft.util.Box;
import org.pepsoft.worldpainter.objects.WPObject;

import javax.vecmath.Point3i;
import java.util.List;

/**
 * A {@link DynmapWorld} implementation which wraps a {@link WPObject} for use
 * with the dynmap API.
 *
 * <p>Created by Pepijn Schmitz on 08-06-15.
 */
public class WPObjectDynmapWorld extends DynmapWorld {
    public WPObjectDynmapWorld(WPObject object) {
        super(object.getName(), object.getDimensions().z, 0);
        this.object = object;
        chunkCache = new WPObjectMapChunkCache(this, object);
        Point3i offset = object.getOffset();
        xOffset = offset.x;
        yOffset = offset.y;
        Point3i dimensions = object.getDimensions();
        bounds = new Box(xOffset, dimensions.x + xOffset, yOffset, dimensions.y + yOffset, 0, dimensions.z);
    }

    @Override
    public boolean isNether() {
        return false;
    }

    @Override
    public DynmapLocation getSpawnLocation() {
        return null;
    }

    @Override
    public long getTime() {
        return 10000;
    }

    @Override
    public boolean hasStorm() {
        return false;
    }

    @Override
    public boolean isThundering() {
        return false;
    }

    @Override
    public boolean isLoaded() {
        return true;
    }

    @Override
    public void setWorldUnloaded() {
        // Do nothing
    }

    @Override
    public int getLightLevel(int x, int y, int z) {
        if (bounds.contains(x, z, y) && object.getMask(x - xOffset, z - yOffset, y)) {
            return object.getMaterial(x - xOffset, z - yOffset, y).block.blockLight;
        } else {
            return 0;
        }
    }

    @Override
    public int getHighestBlockYAt(int x, int z) {
        if (bounds.containsXY(x, z)) {
            for (int height = object.getDimensions().z - 1; height >= 0; height--) {
                if (object.getMask(x - xOffset, z - yOffset, height)) {
                    return height;
                }
            }
            return -1;
        } else {
            return -1;
        }
    }

    @Override
    public boolean canGetSkyLightLevel() {
        return false;
    }

    @Override
    public int getSkyLightLevel(int x, int y, int z) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getEnvironment() {
        return "normal";
    }

    @Override
    public MapChunkCache getChunkCache(List<DynmapChunk> chunks) {
        return chunkCache;
    }

    private final WPObject object;
    private final WPObjectMapChunkCache chunkCache;
    private final Box bounds;
    private final int xOffset, yOffset;
}