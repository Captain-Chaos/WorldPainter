/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.layers.exporters;

import org.pepsoft.minecraft.Chunk;
import org.pepsoft.util.PerlinNoise;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.exporting.AbstractLayerExporter;
import org.pepsoft.worldpainter.exporting.FirstPassLayerExporter;
import org.pepsoft.worldpainter.layers.Caverns;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Random;

import static org.pepsoft.minecraft.Block.BLOCKS;
import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.worldpainter.Constants.*;

/**
 *
 * @author pepijn
 */
public class CavernsExporter extends AbstractLayerExporter<Caverns> implements FirstPassLayerExporter {
    public CavernsExporter() {
        super(Caverns.INSTANCE, new CavernsSettings());
    }
    
    @Override
    public void render(Dimension dimension, Tile tile, Chunk chunk) {
        final CavernsSettings settings = (CavernsSettings) getSettings();
        final boolean surfaceBreaking = settings.isSurfaceBreaking();
        final boolean floodWithLava = settings.isFloodWithLava();
        final boolean glassCeiling = settings.isGlassCeiling();
        final boolean leaveWater = settings.isLeaveWater();
        final int waterLevel = (settings.getWaterLevel() > 0) ? settings.getWaterLevel() : -1; // To avoid rendering a layer of water when fallThrough is true
        final int minimumLevel = settings.getCavernsEverywhereLevel();
        final int minY = settings.getMinimumLevel();
        final int maxY = Math.min(settings.getMaximumLevel(), dimension.getMaxHeight() - 1);
        final boolean fallThrough = (minY == 0) && dimension.isBottomless();
        final int minYAdjusted = Math.max(minY, 1);
        final long seed = dimension.getSeed();
        if ((seed + SEED_OFFSET) != perlinNoise.getSeed()) {
            perlinNoise.setSeed(seed + SEED_OFFSET);
        }
        final int xOffset = (chunk.getxPos() & 7) << 4;
        final int zOffset = (chunk.getzPos() & 7) << 4;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                final int localX = xOffset + x, localY = zOffset + z;
                final int worldX = tile.getX() * TILE_SIZE + localX, worldY = tile.getY() * TILE_SIZE + localY;
                if (tile.getBitLayerValue(org.pepsoft.worldpainter.layers.Void.INSTANCE, localX, localY)) {
                    continue;
                }
                final int cavernsValue = Math.max(minimumLevel, tile.getLayerValue(Caverns.INSTANCE, localX, localY));
                if (cavernsValue > 0) {
                    final int terrainheight = Math.min(tile.getIntHeight(localX, localY), maxY);
//                    if ((x == 0) && (z == 0)) {
//                        System.out.println("terrainHeight: " + terrainheight);
//                    }
                    boolean breachedCeiling = false, previousBlockInCavern = false;
                    for (int y = terrainheight; fallThrough ? (y >= 0) : (y >= minYAdjusted); y--) {
                        final int existingBlockType = chunk.getBlockType(x, y, z);
                        if (existingBlockType == BLK_AIR) {
                            // There is already a void here; assume that things
                            // like removing water, etc. have already been done
                            // by whatever made the void
                            breachedCeiling = true;
                            previousBlockInCavern = true;
                            continue;
                        }
                        float bias = CAVERN_CHANCE
                            * Math.max(
                                0.1f * (10 - Math.min(
                                    Math.min(
                                        surfaceBreaking ? Integer.MAX_VALUE : (terrainheight - Math.max(dimension.getTopLayerDepth(worldX, worldY, terrainheight), 3) - y),
                                        (fallThrough ? Integer.MAX_VALUE : (y - 1)) - minY),
                                    10)),
                                1.0f - cavernsValue / 15.0f);
                        if (fallThrough && (y < 5)) {
                            // Widen the caverns towards the bottom
                            bias -= (5 - y) * 0.05f;
                        }
                        final float cavernLikelyhood = perlinNoise.getPerlinNoise(worldX / MEDIUM_BLOBS, worldY / MEDIUM_BLOBS, y / SMALL_BLOBS) + 0.5f - bias;
//                        if ((x == 0) && (z == 0)) {
//                            System.out.println(y + ": bias: " + bias + ", cavernLikelyHood: " + cavernLikelyhood);
//                        }
                        if (cavernLikelyhood > CAVERN_CHANCE) {
                            // In a cavern
                            if ((! breachedCeiling) && (y < maxY)) {
//                                System.out.println("Cavern for value: " + cavernsValue);
//                                if (cavernsValue < lowestValueCavern) {
//                                    lowestValueCavern = cavernsValue;
//                                    System.out.println("Lowest cavern value with caverns: " + lowestValueCavern);
//                                }
                                if (glassCeiling) {
                                    for (int yy = y + 1; yy <= terrainheight; yy++) {
                                        chunk.setBlockType(x, yy, z, BLK_GLASS);
                                    }
                                }
                                if (surfaceBreaking) {
                                    final int blockAbove = chunk.getBlockType(x, y + 1, z);
                                    if (leaveWater) {
                                        if (blockAbove == BLK_STATIONARY_WATER) {
                                            chunk.setBlockType(x, y + 1, z, BLK_WATER);
                                        } else if (blockAbove == BLK_STATIONARY_LAVA) {
                                            chunk.setBlockType(x, y + 1, z, BLK_LAVA);
                                        }                                        
                                    } else {
                                        if ((blockAbove == BLK_WATER) || (blockAbove == BLK_STATIONARY_WATER)) {
                                            for (int yy = y + 1; yy <= maxY; yy++) {
                                                final int blockType = chunk.getBlockType(x, yy, z);
                                                if ((blockType == BLK_WATER) || (blockType == BLK_STATIONARY_WATER)) {
                                                    chunk.setBlockType(x, yy, z, BLK_AIR);
                                                    chunk.setDataValue(x, yy, z, 0);
                                                    // Set the surrounding water, if
                                                    // any, to non-stationary, so that
                                                    // it will flow into the cavern
                                                    if ((x > 0) && (chunk.getBlockType(x - 1, yy, z) == BLK_STATIONARY_WATER)) {
                                                        chunk.setBlockType(x - 1, yy, z, BLK_WATER);
                                                    }
                                                    if ((x < 15) && (chunk.getBlockType(x + 1, yy, z) == BLK_STATIONARY_WATER)) {
                                                        chunk.setBlockType(x + 1, yy, z, BLK_WATER);
                                                    }
                                                    if ((z > 0) && (chunk.getBlockType(x, yy, z - 1) == BLK_STATIONARY_WATER)) {
                                                        chunk.setBlockType(x, yy, z - 1, BLK_WATER);
                                                    }
                                                    if ((z < 15) && (chunk.getBlockType(x, yy, z + 1) == BLK_STATIONARY_WATER)) {
                                                        chunk.setBlockType(x, yy, z + 1, BLK_WATER);
                                                    }
                                                } else {
                                                    break;
                                                }
                                            }
                                        } else if ((blockAbove == BLK_LAVA) || (blockAbove == BLK_STATIONARY_LAVA)) {
                                            for (int yy = y + 1; yy <= maxY; yy++) {
                                                final int blockType = chunk.getBlockType(x, yy, z);
                                                if ((blockType == BLK_LAVA) || (blockType == BLK_STATIONARY_LAVA)) {
                                                    chunk.setBlockType(x, yy, z, BLK_AIR);
                                                    chunk.setDataValue(x, yy, z, 0);
                                                    // Set the surrounding water, if
                                                    // any, to non-stationary, so that
                                                    // it will flow into the cavern
                                                    if ((x > 0) && (chunk.getBlockType(x - 1, yy, z) == BLK_STATIONARY_LAVA)) {
                                                        chunk.setBlockType(x - 1, yy, z, BLK_LAVA);
                                                    }
                                                    if ((x < 15) && (chunk.getBlockType(x + 1, yy, z) == BLK_STATIONARY_LAVA)) {
                                                        chunk.setBlockType(x + 1, yy, z, BLK_LAVA);
                                                    }
                                                    if ((z > 0) && (chunk.getBlockType(x, yy, z - 1) == BLK_STATIONARY_LAVA)) {
                                                        chunk.setBlockType(x, yy, z - 1, BLK_LAVA);
                                                    }
                                                    if ((z < 15) && (chunk.getBlockType(x, yy, z + 1) == BLK_STATIONARY_LAVA)) {
                                                        chunk.setBlockType(x, yy, z + 1, BLK_LAVA);
                                                    }
                                                } else {
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            breachedCeiling = true;
                            if ((! previousBlockInCavern) && (y < maxY)) {
                                final int blockAbove = chunk.getBlockType(x, y + 1, z);
                                // Note that the post processor will take care
                                // of supporting sand with sandstone, if that is
                                // not disabled
                                if (blockAbove == BLK_GRAVEL) {
                                    // Support gravel with stone
                                    chunk.setBlockType(x, y + 1, z, BLK_STONE);
                                }
                            }
//                            System.out.println("Cavern at " + x + ", " + y + ", " + z);
                            if (y > waterLevel) {
                                chunk.setBlockType(x, y, z, BLK_AIR);
                                previousBlockInCavern = true;
                            } else {
                                if (floodWithLava) {
                                    chunk.setBlockType(x, y, z, BLK_STATIONARY_LAVA);
                                } else {
                                    chunk.setBlockType(x, y, z, BLK_STATIONARY_WATER);
                                }
                                previousBlockInCavern = false;
                            }
                        } else if (previousBlockInCavern
                                && (y >= waterLevel)
                                && (! BLOCKS[existingBlockType].veryInsubstantial)) {
//                            Material material = subsurfaceMaterial.getMaterial(seed, worldX, worldY, 1);
//                            if (material == Material.AIR) {
                                final int rnd = new Random(seed + (worldX * 65537) + (worldY * 4099)).nextInt(MUSHROOM_CHANCE);
                                if (rnd == 0) {
                                    chunk.setBlockType(x, y + 1, z, BLK_BROWN_MUSHROOM);
//                                    System.out.println("Cave shroom @ " + worldX + ", " + worldY + "!");
                                }
//                            } else {
//                                chunk.setMaterial(x, y + 1, z, material);
//                            }
//                            chunk.setMaterial(x, y, z, subsurfaceMaterial.getMaterial(seed, worldX, worldY, 0));
                            previousBlockInCavern = false;
                        }
                    }
                }
                if (glassCeiling) {
                    chunk.setHeight(x, z, 1);
                }
            }
        }
    }

    private final PerlinNoise perlinNoise = new PerlinNoise(0);
//    private int lowestValueCavern = Integer.MAX_VALUE;
    
    private static final float CAVERN_CHANCE = 0.5f;
    private static final long SEED_OFFSET = 37;
    private static final int MUSHROOM_CHANCE = 250;
    
    public static class CavernsSettings implements ExporterSettings {
        @Override
        public boolean isApplyEverywhere() {
            return cavernsEverywhereLevel > 0;
        }

        @Override
        public Caverns getLayer() {
            return Caverns.INSTANCE;
        }
        
        public boolean isFloodWithLava() {
            return floodWithLava;
        }

        public void setFloodWithLava(boolean floodWithLava) {
            this.floodWithLava = floodWithLava;
        }

        public boolean isGlassCeiling() {
            return glassCeiling;
        }

        public void setGlassCeiling(boolean glassCeiling) {
            this.glassCeiling = glassCeiling;
        }

        public boolean isSurfaceBreaking() {
            return surfaceBreaking;
        }

        public void setSurfaceBreaking(boolean surfaceBreaking) {
            this.surfaceBreaking = surfaceBreaking;
        }

        public int getWaterLevel() {
            return waterLevel;
        }

        public void setWaterLevel(int waterLevel) {
            this.waterLevel = waterLevel;
        }

        public int getCavernsEverywhereLevel() {
            return cavernsEverywhereLevel;
        }

        public void setCavernsEverywhereLevel(int cavernsEverywhereLevel) {
            this.cavernsEverywhereLevel = cavernsEverywhereLevel;
        }

        public boolean isLeaveWater() {
            return leaveWater;
        }

        public void setLeaveWater(boolean leaveWater) {
            this.leaveWater = leaveWater;
        }

        public int getMinimumLevel() {
            return minimumLevel;
        }

        public void setMinimumLevel(int minimumLevel) {
            this.minimumLevel = minimumLevel;
        }

        public int getMaximumLevel() {
            return maximumLevel;
        }

        public void setMaximumLevel(int maximumLevel) {
            this.maximumLevel = maximumLevel;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final CavernsSettings other = (CavernsSettings) obj;
            if (this.waterLevel != other.waterLevel) {
                return false;
            }
            if (this.cavernsEverywhereLevel != other.cavernsEverywhereLevel) {
                return false;
            }
            if (this.floodWithLava != other.floodWithLava) {
                return false;
            }
            if (this.glassCeiling != other.glassCeiling) {
                return false;
            }
            if (this.surfaceBreaking != other.surfaceBreaking) {
                return false;
            }
            if (this.leaveWater != other.leaveWater) {
                return false;
            }
            if (this.minimumLevel != other.minimumLevel) {
                return false;
            }
            if (this.maximumLevel != other.maximumLevel) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 29 * hash + this.waterLevel;
            hash = 29 * hash + this.cavernsEverywhereLevel;
            hash = 29 * hash + (this.floodWithLava ? 1 : 0);
            hash = 29 * hash + (this.glassCeiling ? 1 : 0);
            hash = 29 * hash + (this.surfaceBreaking ? 1 : 0);
            hash = 29 * hash + (this.leaveWater ? 1 : 0);
            hash = 29 * hash + this.minimumLevel;
            hash = 29 * hash + this.maximumLevel;
            return hash;
        }

        @Override
        public CavernsSettings clone() {
            try {
                return (CavernsSettings) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }
        
        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            
            // Legacy
            if (maximumLevel == 0) {
                maximumLevel = Integer.MAX_VALUE;
            }
        }
        
        private int waterLevel, cavernsEverywhereLevel;
        private boolean floodWithLava, glassCeiling, surfaceBreaking, leaveWater = true;
        private int minimumLevel = 0, maximumLevel = Integer.MAX_VALUE;
        
        private static final long serialVersionUID = 2011060801L;
    }
}