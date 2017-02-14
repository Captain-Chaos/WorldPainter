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

/**
 * A Minecraft world wrapper which inverts the wrapped world vertically. Does
 * not support adding new chunks.
 *
 * Created by pepijn on 21-04-15.
 */
public class InvertedWorld implements MinecraftWorld {
    public InvertedWorld(MinecraftWorld world, int delta) {
        this.world = world;
        this.delta = delta;
        maxHeight = world.getMaxHeight();
        maxZ = maxHeight - delta - 1;
    }

    @Override
    public int getBlockTypeAt(int x, int y, int height) {
        if (height > maxZ) {
            return Constants.BLK_AIR;
        } else {
            return world.getBlockTypeAt(x, y, maxZ - height);
        }
    }

    @Override
    public int getDataAt(int x, int y, int height) {
        if (height > maxZ) {
            return 0;
        } else {
            return world.getDataAt(x, y, maxZ - height);
        }
    }

    @Override
    public Material getMaterialAt(int x, int y, int height) {
        if (height > maxZ) {
            return Material.AIR;
        } else {
            return world.getMaterialAt(x, y, maxZ - height).invert();
        }
    }

    @Override
    public void setBlockTypeAt(int x, int y, int height, int blockType) {
        if (height <= maxZ) {
            world.setBlockTypeAt(x, y, maxZ - height, blockType);
        }
    }

    @Override
    public void setDataAt(int x, int y, int height, int data) {
        if (height <= maxZ) {
            world.setDataAt(x, y, maxZ - height, data);
        }
    }

    @Override
    public void setMaterialAt(int x, int y, int height, Material material) {
        if (height <= maxZ) {
            world.setMaterialAt(x, y, maxZ - height, material.invert());
        }
    }

    @Override
    public int getMaxHeight() {
        return maxHeight;
    }

    @Override
    public void addEntity(int x, int y, int height, Entity entity) {
        Entity worldEntity = (Entity) entity.clone();
        double[] pos = worldEntity.getPos();
        pos[1] = maxZ - pos[1];
        worldEntity.setPos(pos);
        world.addEntity(x, y, maxZ - height, worldEntity);
    }

    @Override
    public void addEntity(double x, double y, double height, Entity entity) {
        Entity worldEntity = (Entity) entity.clone();
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
        if (height > maxZ) {
            return 0;
        } else {
            return world.getBlockLightLevel(x, y, maxZ - height);
        }
    }

    @Override
    public void setBlockLightLevel(int x, int y, int height, int blockLightLevel) {
        if (height <= maxZ) {
            world.setBlockLightLevel(x, y, maxZ - height, blockLightLevel);
        }
    }

    @Override
    public int getSkyLightLevel(int x, int y, int height) {
        if (height > maxZ) {
            return 15;
        } else {
            return world.getSkyLightLevel(x, y, maxZ - height);
        }
    }

    @Override
    public void setSkyLightLevel(int x, int y, int height, int skyLightLevel) {
        if (height <= maxZ) {
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
    public int getHighestNonAirBlock(int x, int y) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public Chunk getChunk(int x, int z) {
        Chunk chunk = world.getChunk(x, z);
        return (chunk != null) ? new InvertedChunk(chunk, delta) : null;
    }

    @Override
    public Chunk getChunkForEditing(int x, int z) {
        Chunk chunk = world.getChunkForEditing(x, z);
        return (chunk != null) ? new InvertedChunk(chunk, delta) : null;
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
    private final int maxHeight, maxZ, delta;
}