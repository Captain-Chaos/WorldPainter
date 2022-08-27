package org.pepsoft.worldpainter.dynmap;

import org.dynmap.DynmapChunk;
import org.dynmap.DynmapLocation;
import org.dynmap.DynmapWorld;
import org.dynmap.renderer.DynmapBlockState;
import org.dynmap.utils.MapChunkCache;
import org.pepsoft.minecraft.Material;
import org.pepsoft.util.Box;
import org.pepsoft.worldpainter.objects.WPObject;

import javax.vecmath.Point3i;
import java.util.List;

import static org.dynmap.renderer.DynmapBlockState.AIR;
import static org.pepsoft.minecraft.Material.WOOL_MAGENTA;
import static org.pepsoft.worldpainter.dynmap.DynmapBlockStateHelper.getDynmapBlockState;

/**
 * A {@link DynmapWorld} implementation which wraps a {@link WPObject} for use
 * with the dynmap API.
 *
 * <p>Created by Pepijn Schmitz on 08-06-15.
 */
class WPObjectDynmapWorld extends DynmapWorld {
    WPObjectDynmapWorld(WPObject object) {
        super(object.getName(), object.getDimensions().z, 0);
        this.object = object;
        chunkCache = new WPObjectMapChunkCache(this);
        Point3i offset = object.getOffset();
        xOffset = offset.x;
        yOffset = offset.y;
        Point3i dimensions = object.getDimensions();
        bounds = new Box(xOffset, dimensions.x + xOffset, yOffset, dimensions.y + yOffset, 0, dimensions.z);
        blockStates = new DynmapBlockState[dimensions.x][dimensions.y][dimensions.z];
        lightLevels = new int[dimensions.x][dimensions.y][dimensions.z];
        heights = new int[dimensions.x][dimensions.y];
        for (int x = 0; x < dimensions.x; x++) {
            for (int y = 0; y < dimensions.y; y++) {
                for (int z = 0; z < dimensions.z; z++) {
                    if (object.getMask(x, y, z)) {
                        final Material material = object.getMaterial(x, y, z);
                        if (material == Material.AIR) {
                            blockStates[x][y][z] = AIR;
                        } else {
                            final DynmapBlockState blockState = getDynmapBlockState(material);
                            blockStates[x][y][z] = (blockState != null) ? blockState : MISSING_BLOCK_STATE;
                            lightLevels[x][y][z] = material.blockLight;
                        }
                    } else {
                        blockStates[x][y][z] = AIR;
                    }
                }
                int height = -1;
                for (int z = object.getDimensions().z - 1; z >= 0; z--) {
                    if (object.getMask(x, y, z)) {
                        height = z;
                        break;
                    }
                }
                heights[x][y] = height;
            }
        }
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
        if (bounds.contains(x, z, y)) {
            return lightLevels[x - xOffset][z][y - yOffset];
        } else {
            return 0;
        }
    }

    @Override
    public int getHighestBlockYAt(int x, int z) {
        if (bounds.containsXY(x, z)) {
            return heights[x - xOffset][z - yOffset];
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

    final WPObject object;
    final Box bounds;
    final int xOffset, yOffset;
    final DynmapBlockState[][][] blockStates;

    private final WPObjectMapChunkCache chunkCache;
    private final int[][][] lightLevels;
    private final int[][] heights;

    private static final DynmapBlockState MISSING_BLOCK_STATE = getDynmapBlockState(WOOL_MAGENTA);
}