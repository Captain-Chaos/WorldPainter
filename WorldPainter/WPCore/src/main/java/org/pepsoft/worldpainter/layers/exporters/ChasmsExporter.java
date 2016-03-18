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
import org.pepsoft.worldpainter.layers.Chasms;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Random;

import static org.pepsoft.minecraft.Block.BLOCKS;
import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.worldpainter.Constants.MEDIUM_BLOBS;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE;

/**
 *
 * @author pepijn
 */
public class ChasmsExporter extends AbstractLayerExporter<Chasms> implements FirstPassLayerExporter {
    public ChasmsExporter() {
        super(Chasms.INSTANCE, new ChasmsSettings());
    }

    @Override
    public void render(Dimension dimension, Tile tile, Chunk chunk) {
        final ChasmsSettings settings = (ChasmsSettings) getSettings();
        final boolean surfaceBreaking = settings.isSurfaceBreaking();
        final boolean floodWithLava = settings.isFloodWithLava();
        final boolean glassCeiling = settings.isGlassCeiling();
        final boolean leaveWater = settings.isLeaveWater();
        final int waterLevel = (settings.getWaterLevel() > 0) ? settings.getWaterLevel() : -1; // To avoid rendering a layer of water when fallThrough is true
        final int minimumLevel = settings.getChasmsEverywhereLevel();
        final long seed = dimension.getSeed();
        if ((seed + SEED_OFFSET) != perlinNoise.getSeed()) {
            perlinNoise.setSeed(seed + SEED_OFFSET);
            perlinNoise2.setSeed(seed + SEED_OFFSET2);
            perlinNoise3.setSeed(seed + SEED_OFFSET3);
            perlinNoise4.setSeed(seed + SEED_OFFSET4);
            perlinNoise5.setSeed(seed + SEED_OFFSET5);
            perlinNoise6.setSeed(seed + SEED_OFFSET6);
        }
        final int xOffset = (chunk.getxPos() & 7) << 4;
        final int zOffset = (chunk.getzPos() & 7) << 4;
        final int minY = settings.getMinimumLevel();
        final boolean fallThrough = (minY == 0) && dimension.isBottomless();
        final int minYAdjusted = Math.max(minY, 1);
        final int maxY = Math.min(settings.getMaximumLevel(), dimension.getMaxHeight() - 1);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                final int localX = xOffset + x, localY = zOffset + z;
                if (tile.getBitLayerValue(org.pepsoft.worldpainter.layers.Void.INSTANCE, localX, localY)) {
                    continue;
                }
                final int chasmsValue = Math.max(minimumLevel, tile.getLayerValue(Chasms.INSTANCE, localX, localY));
                if (chasmsValue > 0) {
                    final int worldX = tile.getX() * TILE_SIZE + localX, worldY = tile.getY() * TILE_SIZE + localY;
//                    final float px = worldX / MEDIUM_BLOBS, py = worldY / MEDIUM_BLOBS;
//                    final float px = worldX / SMALL_BLOBS, py = worldY / SMALL_BLOBS; // [2] ravine?
                    final float px = worldX / MEDIUM_BLOBS, py = worldY / MEDIUM_BLOBS; // [3] tunnellike
                    // Maximising it shouldn't be necessary, but there are
                    // worlds in the wild with heights above the maximum:
                    final int terrainheight = Math.min(tile.getIntHeight(localX, localY), maxY);
//                    if ((x == 0) && (z == 0)) {
//                        System.out.println("terrainHeight: " + terrainheight);
//                    }
                    boolean breachedCeiling = false, previousBlockInCavern = false;
                    for (int y = terrainheight; fallThrough ? (y >= 0) : (y >= minYAdjusted); y--) {
                        int existingBlockType = chunk.getBlockType(x, y, z);
                        if (existingBlockType == BLK_AIR) {
                            // There is already a void here; assume that things
                            // like removing water, etc. have already been done
                            // by whatever made the void
                            breachedCeiling = true;
                            previousBlockInCavern = true;
                            continue;
                        }
                        float bias = CHASM_CHANCE
                                * Math.max(
                                0.1f * (10 - Math.min(
                                        Math.min(
                                            surfaceBreaking ? Integer.MAX_VALUE : (terrainheight - Math.max(dimension.getTopLayerDepth(worldX, worldY, terrainheight), 3) - y),
                                            (fallThrough ? Integer.MAX_VALUE : (y - 1)) - minY),
                                        10)),
                                1.0f - chasmsValue / 15.0f);
//                                0.5f - chasmsValue / 15.0f); // TODO: higher than 50% has no effect
                        if (fallThrough && (y < 5)) {
                            // Widen the caverns towards the bottom
                            bias -= (5 - y) * 0.05f;
                        }
//                        final float pz = y / SMALL_BLOBS;
//                        final float pz = y / MEDIUM_BLOBS; // [2] ravine?
                        final float pz = (y + 15) / MEDIUM_BLOBS; // [3] tunnellike
//                        float cavernLikelyhood = Math.min(perlinNoise.getPerlinNoise(px, py, pz), perlinNoise2.getPerlinNoise(px, py, pz)) + 0.5f - bias;
//                        double cavernLikelyhood = Math.min(Math.sin(perlinNoise.getPerlinNoise(px, py, pz) * 10), Math.cos(perlinNoise2.getPerlinNoise(px, py, pz) * 10)) / 2 + 0.5f - bias; // [2] ravine?
                        final double cavernLikelyhood = Math.min(perlinNoise3.getPerlinNoise(px, py, pz) - bias, Math.min(Math.sin(perlinNoise.getPerlinNoise(px, py, pz) * 25), Math.cos(perlinNoise2.getPerlinNoise(px, py, pz) * 25)) / 2) + 0.5f; // [3] tunnellike
                        final double cavernLikelyhood2 = Math.min(perlinNoise6.getPerlinNoise(px, py, pz) - bias, Math.min(Math.sin(perlinNoise4.getPerlinNoise(px, py, pz) * 25), Math.cos(perlinNoise5.getPerlinNoise(px, py, pz) * 25)) / 2) + 0.5f; // [3] tunnellike
//                        double cavernLikelyhood = Math.sin(perlinNoise.getPerlinNoise(px, py, pz) * 5);
//                        double cavernLikelyhood2 = Math.sin(perlinNoise2.getPerlinNoise(px, py) * 5);
//                        if ((x == 0) && (z == 0)) {
//                            System.out.println(y + ": bias: " + bias + ", cavernLikelyHood: " + cavernLikelyhood);
//                        }
//                        if (cavernLikelyhood > CHASM_CHANCE) {
                        if ((cavernLikelyhood > CHASM_CHANCE) || (cavernLikelyhood2 > CHASM_CHANCE)) {
//                        if ((cavernLikelyhood > CHASM_CHANCE) && (cavernLikelyhood2 > CHASM_CHANCE)) {
                            // In a cavern
                            if ((! breachedCeiling) && (y < maxY)) {
//                                System.out.println("Cavern for value: " + chasmsValue);
//                                if (chasmsValue < lowestValueCavern) {
//                                    lowestValueCavern = chasmsValue;
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
                                int rnd = new Random(seed + (worldX * 65537) + (worldY * 4099)).nextInt(MUSHROOM_CHANCE);
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
    private final PerlinNoise perlinNoise2 = new PerlinNoise(0);
    private final PerlinNoise perlinNoise3 = new PerlinNoise(0);
    private final PerlinNoise perlinNoise4 = new PerlinNoise(0);
    private final PerlinNoise perlinNoise5 = new PerlinNoise(0);
    private final PerlinNoise perlinNoise6 = new PerlinNoise(0);
//    private int lowestValueCavern = Integer.MAX_VALUE;

    private static final float CHASM_CHANCE = 0.5f;
    //    private static final float CHASM_CHANCE = 0.95f;
    private static final long SEED_OFFSET = 37;
    private static final long SEED_OFFSET2 = 41;
    private static final long SEED_OFFSET3 = 43;
    private static final long SEED_OFFSET4 = 47;
    private static final long SEED_OFFSET5 = 49;
    private static final long SEED_OFFSET6 = 51;
    private static final int MUSHROOM_CHANCE = 250;

    public static class ChasmsSettings implements ExporterSettings {
        @Override
        public boolean isApplyEverywhere() {
            return chasmsEverywhereLevel > 0;
        }

        @Override
        public Chasms getLayer() {
            return Chasms.INSTANCE;
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

        public int getChasmsEverywhereLevel() {
            return chasmsEverywhereLevel;
        }

        public void setChasmsEverywhereLevel(int chasmsEverywhereLevel) {
            this.chasmsEverywhereLevel = chasmsEverywhereLevel;
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
            final ChasmsSettings other = (ChasmsSettings) obj;
            if (this.waterLevel != other.waterLevel) {
                return false;
            }
            if (this.chasmsEverywhereLevel != other.chasmsEverywhereLevel) {
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
            hash = 29 * hash + this.chasmsEverywhereLevel;
            hash = 29 * hash + (this.floodWithLava ? 1 : 0);
            hash = 29 * hash + (this.glassCeiling ? 1 : 0);
            hash = 29 * hash + (this.surfaceBreaking ? 1 : 0);
            hash = 29 * hash + (this.leaveWater ? 1 : 0);
            hash = 29 * hash + this.minimumLevel;
            hash = 29 * hash + this.maximumLevel;
            return hash;
        }

        @Override
        public ChasmsSettings clone() {
            try {
                return (ChasmsSettings) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }
        
        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            
            // Legacy settings
            if (maximumLevel == 0) {
                maximumLevel = Integer.MAX_VALUE;
            }
        }

        private int waterLevel, chasmsEverywhereLevel;
        private boolean floodWithLava, glassCeiling, surfaceBreaking, leaveWater = true;
        private int minimumLevel = 0, maximumLevel = Integer.MAX_VALUE;

        private static final long serialVersionUID = 1L;
    }
}