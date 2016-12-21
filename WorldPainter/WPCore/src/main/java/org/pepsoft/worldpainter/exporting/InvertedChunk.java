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

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A chunk wrapper which inverts the wrapped chunk vertically. Does not support
 * conversion to NBT.
 *
 * Created by pepijn on 21-04-15.
 */
public class InvertedChunk implements Chunk {
    public InvertedChunk(Chunk chunk, int delta) {
        this.chunk = chunk;
        maxHeight = chunk.getMaxHeight();
        maxY = maxHeight - delta - 1;
    }

    @Override
    public int getBlockLightLevel(int x, int y, int z) {
        if (y > maxY) {
            return 0;
        } else {
            return chunk.getBlockLightLevel(x, maxY - y, z);
        }
    }

    @Override
    public void setBlockLightLevel(int x, int y, int z, int blockLightLevel) {
        if (y <= maxY) {
            chunk.setBlockLightLevel(x, maxY - y, z, blockLightLevel);
        }
    }

    @Override
    public int getBlockType(int x, int y, int z) {
        if (y > maxY) {
            return Constants.BLK_AIR;
        } else {
            return chunk.getBlockType(x, maxY - y, z);
        }
    }

    @Override
    public void setBlockType(int x, int y, int z, int blockType) {
        if (y <= maxY) {
            chunk.setBlockType(x, maxY - y, z, blockType);
        }
    }

    @Override
    public int getDataValue(int x, int y, int z) {
        if (y > maxY) {
            return 0;
        } else {
            return chunk.getDataValue(x, maxY - y, z);
        }
    }

    @Override
    public void setDataValue(int x, int y, int z, int dataValue) {
        if (y <= maxY) {
            chunk.setDataValue(x, maxY - y, z, dataValue);
        }
    }

    @Override
    public int getHeight(int x, int z) {
        return maxY - chunk.getHeight(x, z);
    }

    @Override
    public void setHeight(int x, int z, int height) {
        chunk.setHeight(x, z, maxY - height);
    }

    @Override
    public int getSkyLightLevel(int x, int y, int z) {
        if (y > maxY) {
            return 15;
        } else {
            return chunk.getSkyLightLevel(x, maxY - y, z);
        }
    }

    @Override
    public void setSkyLightLevel(int x, int y, int z, int skyLightLevel) {
        if (y <= maxY) {
            chunk.setSkyLightLevel(x, maxY - y, z, skyLightLevel);
        }
    }

    @Override
    public int getxPos() {
        return chunk.getxPos();
    }

    @Override
    public int getzPos() {
        return chunk.getzPos();
    }

    @Override
    public Point getCoords() {
        return chunk.getCoords();
    }

    @Override
    public boolean isTerrainPopulated() {
        return chunk.isTerrainPopulated();
    }

    @Override
    public void setTerrainPopulated(boolean terrainPopulated) {
        chunk.setTerrainPopulated(terrainPopulated);
    }

    @Override
    public Material getMaterial(int x, int y, int z) {
        if (y > maxY) {
            return Material.AIR;
        } else {
            return chunk.getMaterial(x, maxY - y, z).invert();
        }
    }

    @Override
    public void setMaterial(int x, int y, int z, Material material) {
        if (y <= maxY) {
            chunk.setMaterial(x, maxY - y, z, material.invert());
        }
    }

    @Override
    public List<Entity> getEntities() {
        List<Entity> chunkEntities = chunk.getEntities();
        if (chunkEntities != null) {
            List<Entity> entities = new ArrayList<>(chunkEntities.size());
            for (Entity chunkEntity: chunkEntities) {
                Entity entity = (Entity) chunkEntity.clone();
                double[] pos = entity.getPos();
                pos[1] = maxY - pos[1];
                entity.setPos(pos);
                entities.add(entity);
            }
            return entities;
        } else {
            return null;
        }
    }

    @Override
    public List<TileEntity> getTileEntities() {
        List<TileEntity> chunkTileEntities = chunk.getTileEntities();
        if (chunkTileEntities != null) {
            List<TileEntity> tileEntities = new ArrayList<>(chunkTileEntities.size());
            for (TileEntity chunkTileEntity: chunkTileEntities) {
                TileEntity tileEntity = (TileEntity) chunkTileEntity.clone();
                int adjustedY = maxY - tileEntity.getY();
                if ((adjustedY >= 0) && (adjustedY < maxHeight)) {
                    tileEntity.setY(adjustedY);
                    tileEntities.add(tileEntity);
                }
            }
            return tileEntities;
        } else {
            return null;
        }
    }

    @Override
    public int getMaxHeight() {
        return maxHeight;
    }

    @Override
    public boolean isBiomesAvailable() {
        return chunk.isBiomesAvailable();
    }

    @Override
    public int getBiome(int x, int z) {
        return chunk.getBiome(x, z);
    }

    @Override
    public void setBiome(int x, int z, int biome) {
        chunk.setBiome(x, z, biome);
    }

    @Override
    public boolean isReadOnly() {
        return chunk.isReadOnly();
    }

    @Override
    public boolean isLightPopulated() {
        return chunk.isLightPopulated();
    }

    @Override
    public void setLightPopulated(boolean lightPopulated) {
        chunk.setLightPopulated(lightPopulated);
    }

    @Override
    public long getInhabitedTime() {
        return chunk.getInhabitedTime();
    }

    @Override
    public void setInhabitedTime(long inhabitedTime) {
        chunk.setInhabitedTime(inhabitedTime);
    }

    @Override
    public int getHighestNonAirBlock(int x, int z) {
        for (int y = 0; y <= maxY; y++) {
            if (chunk.getBlockType(x, y, z) != Constants.BLK_AIR) {
                return maxY - y;
            }
        }
        return -1;
    }

    @Override
    public int getHighestNonAirBlock() {
        for (int y = 0; y <= maxY; y++) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    if (chunk.getBlockType(x, y, z) != Constants.BLK_AIR) {
                        return maxY - y;
                    }
                }
            }
        }
        return -1;
    }

    private final Chunk chunk;
    private final int maxHeight, maxY;
}