/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.exporting;

import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.Dimension;

import java.awt.*;
import java.io.File;

import static org.pepsoft.worldpainter.Constants.*;

/**
 *
 * @author pepijn
 */
public class JavaMinecraftWorld extends CachingMinecraftWorld {
    public JavaMinecraftWorld(File worldDir, int dimension, int maxHeight, Platform platform, boolean readOnly, int cacheSize) {
        super(worldDir, dimension, maxHeight, platform, readOnly, cacheSize);
        switch (dimension) {
            case DIM_NORMAL:
                dimensionDir = worldDir;
                break;
            case DIM_NETHER:
                dimensionDir = new File(worldDir, "DIM-1");
                break;
            case DIM_END:
                dimensionDir = new File(worldDir, "DIM1");
                break;
            default:
                throw new IllegalArgumentException("Dimension " + dimension + " not supported");
        }
        if (! worldDir.isDirectory()) {
            throw new IllegalArgumentException(worldDir + " does not exist or is not a directory");
        }
    }
    
    public JavaMinecraftWorld(File worldDir, Dimension dimension, Platform platform) {
        this(worldDir, dimension, platform, false, -1);
    }

    public JavaMinecraftWorld(File worldDir, Dimension dimension, Platform platform, boolean readOnly, int cacheSize) {
        super(worldDir, dimension.getDim(), dimension.getMaxHeight(), platform, readOnly, cacheSize);
        if ((dimension.getWorld().getTilesToExport() != null) && dimension.getWorld().getDimensionsToExport().contains(dimension.getDim())) {
            lowestX = Integer.MAX_VALUE;
            highestX = Integer.MIN_VALUE;
            lowestZ = Integer.MAX_VALUE;
            highestZ = Integer.MIN_VALUE;
            for (Point tile: dimension.getWorld().getTilesToExport()) {
                int chunkX = tile.x << 3;
                int chunkZ = tile.y << 3;
                if (chunkX < lowestX) {
                    lowestX = chunkX;
                }
                if (chunkX + 7 > highestX) {
                    highestX = chunkX + 7;
                }
                if (chunkZ < lowestZ) {
                    lowestZ = chunkZ;
                }
                if (chunkX + 7 > highestZ) {
                    highestZ = chunkZ + 7;
                }
            }
        } else {
            Point northEastChunk = new Point((dimension.getHighestX() + 1) * TILE_SIZE - 1, dimension.getLowestY() *  TILE_SIZE);
            Point southWestChunk = new Point(dimension.getLowestX() * TILE_SIZE, (dimension.getHighestY() + 1) * TILE_SIZE - 1);
            lowestX = southWestChunk.x >> 4;
            highestX = northEastChunk.x >> 4;
            lowestZ = northEastChunk.y >> 4;
            highestZ = southWestChunk.y >> 4;
        }
        switch (dimension.getDim()) {
            case DIM_NORMAL:
                dimensionDir = worldDir;
                break;
            case DIM_NETHER:
                dimensionDir = new File(worldDir, "DIM-1");
                break;
            case DIM_END:
                dimensionDir = new File(worldDir, "DIM1");
                break;
            default:
                throw new IllegalArgumentException("Dimension " + dimension.getDim() + " not supported");
        }
        if (! worldDir.isDirectory()) {
            throw new IllegalArgumentException(worldDir + " does not exist or is not a directory");
        }
    }

    public int getHighestX() {
        return highestX;
    }

    public int getHighestZ() {
        return highestZ;
    }

    public int getLowestX() {
        return lowestX;
    }

    public int getLowestZ() {
        return lowestZ;
    }

    private final File dimensionDir;
    private int lowestX, highestX, lowestZ, highestZ;
}