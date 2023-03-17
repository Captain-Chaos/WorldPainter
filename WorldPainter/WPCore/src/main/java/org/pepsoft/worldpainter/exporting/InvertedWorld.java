/*
 * WorldPainter, a graphical and interactive map generator for Minecraft.
 * Copyright © 2011-2015  pepsoft.org, The Netherlands
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.pepsoft.worldpainter.exporting;

import org.pepsoft.minecraft.*;
import org.pepsoft.worldpainter.Platform;

/**
 * A Minecraft world wrapper which inverts the wrapped world vertically. Does
 * not support adding new chunks.
 *
 * Created by pepijn on 21-04-15.
 */
public class InvertedWorld implements MinecraftWorld {
    public InvertedWorld(MinecraftWorld world, int delta, Platform platform) {
        this.world = world;
        this.delta = delta;
        this.platform = platform;
        minHeight = world.getMinHeight();
        maxHeight = world.getMaxHeight();
        maxZ = maxHeight + minHeight - delta - 1;
    }

    @Override
    public int getBlockTypeAt(int x, int y, int height) {
        if (((maxZ - height) < minHeight) || ((maxZ - height) >= maxHeight)) {
            return Constants.BLK_AIR;
        } else {
            return world.getBlockTypeAt(x, y, maxZ - height);
        }
    }

    @Override
    public int getDataAt(int x, int y, int height) {
        if (((maxZ - height) < minHeight) || ((maxZ - height) >= maxHeight)) {
            return 0;
        } else {
            return world.getDataAt(x, y, maxZ - height);
        }
    }

    @Override
    public Material getMaterialAt(int x, int y, int height) {
        if (((maxZ - height) < minHeight) || ((maxZ - height) >= maxHeight)) {
            return Material.AIR;
        } else {
            return world.getMaterialAt(x, y, maxZ - height).invert(platform);
        }
    }

    @Override
    public void setBlockTypeAt(int x, int y, int height, int blockType) {
        if (((maxZ - height) >= minHeight) && ((maxZ - height) < maxHeight)) {
            world.setBlockTypeAt(x, y, maxZ - height, blockType);
        }
    }

    @Override
    public void setDataAt(int x, int y, int height, int data) {
        if (((maxZ - height) >= minHeight) && ((maxZ - height) < maxHeight)) {
            world.setDataAt(x, y, maxZ - height, data);
        }
    }

    @Override
    public void setMaterialAt(int x, int y, int height, Material material) {
        if (((maxZ - height) >= minHeight) && ((maxZ - height) < maxHeight)) {
            world.setMaterialAt(x, y, maxZ - height, material.invert(platform));
        }
    }

    @Override
    public int getMinHeight() {
        return minHeight;
    }

    @Override
    public int getMaxHeight() {
        return maxHeight;
    }

    @Override
    public void addEntity(double x, double y, double height, Entity entity) {
        Entity worldEntity = entity.clone();
        double[] pos = worldEntity.getPos();
        pos[1] = maxZ - pos[1];
        worldEntity.setPos(pos);
        world.addEntity(x, y, maxZ - height, worldEntity);
    }

    @Override
    public void addTileEntity(int x, int y, int height, TileEntity tileEntity) {
        if (height <= maxZ) {
            TileEntity worldEntity = (TileEntity) tileEntity.clone();
            worldEntity.setY(maxZ - worldEntity.getY());
            world.addTileEntity(x, y, maxZ - height, worldEntity);
        }
    }

    @Override
    public int getBlockLightLevel(int x, int y, int height) {
        if (((maxZ - height) < minHeight) || ((maxZ - height) >= maxHeight)) {
            return 0;
        } else {
            return world.getBlockLightLevel(x, y, maxZ - height);
        }
    }

    @Override
    public void setBlockLightLevel(int x, int y, int height, int blockLightLevel) {
        if (((maxZ - height) >= minHeight) && ((maxZ - height) < maxHeight)) {
            world.setBlockLightLevel(x, y, maxZ - height, blockLightLevel);
        }
    }

    @Override
    public int getSkyLightLevel(int x, int y, int height) {
        if (((maxZ - height) < minHeight) || ((maxZ - height) >= maxHeight)) {
            return 15;
        } else {
            return world.getSkyLightLevel(x, y, maxZ - height);
        }
    }

    @Override
    public void setSkyLightLevel(int x, int y, int height, int skyLightLevel) {
        if (((maxZ - height) >= minHeight) && ((maxZ - height) < maxHeight)) {
            world.setSkyLightLevel(x, y, maxZ - height, skyLightLevel);
        }
    }

    @Override
    public boolean isChunkPresent(int x, int y) {
        return world.isChunkPresent(x, y);
    }

    @Override
    public void addChunk(Chunk chunk) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public int getHighestNonAirBlock(int x, int y) { // TODOMC118 does this work for minHeight < 0?
        int worldHighestNonAirBlock = world.getHighestNonAirBlock(x, y);
        if (worldHighestNonAirBlock >= 0) {
            for (int z = minHeight; z < worldHighestNonAirBlock; z++) {
                if (world.getMaterialAt(x, y, z) != Material.AIR) {
                    return maxZ - z;
                }
            }
            return maxZ - worldHighestNonAirBlock;
        } else {
            return Integer.MIN_VALUE;
        }
    }

    @Override
    public Chunk getChunk(int x, int z) {
        Chunk chunk = world.getChunk(x, z);
        return (chunk != null) ? new InvertedChunk(chunk, delta, platform) : null;
    }

    @Override
    public Chunk getChunkForEditing(int x, int z) {
        Chunk chunk = world.getChunkForEditing(x, z);
        return (chunk != null) ? new InvertedChunk(chunk, delta, platform) : null;
    }

    /**
     * Does nothing. (In particular: does <em>not</em> close the underlying
     * world).
     */
    @Override
    public void close() {
        // Do nothing
    }

    private final MinecraftWorld world;
    private final int minHeight, maxHeight, maxZ, delta;
    private final Platform platform;
}