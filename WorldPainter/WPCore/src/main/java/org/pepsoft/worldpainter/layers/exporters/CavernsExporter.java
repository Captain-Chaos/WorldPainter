/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.layers.exporters;

import org.pepsoft.minecraft.Chunk;
import org.pepsoft.util.PerlinNoise;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.exporting.FirstPassLayerExporter;
import org.pepsoft.worldpainter.layers.Caverns;

import java.io.IOException;
import java.io.ObjectInputStream;

import static org.pepsoft.minecraft.Material.AIR;
import static org.pepsoft.worldpainter.Constants.*;

/**
 *
 * @author pepijn
 */
public class CavernsExporter extends AbstractCavesExporter<Caverns> implements FirstPassLayerExporter {
    public CavernsExporter() {
        super(Caverns.INSTANCE, new CavernsSettings());
    }
    
    @Override
    public void render(Dimension dimension, Tile tile, Chunk chunk, Platform platform) {
        final CavernsSettings settings = (CavernsSettings) getSettings();
        final boolean surfaceBreaking = settings.isSurfaceBreaking();
        final boolean glassCeiling = settings.isGlassCeiling();
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
        setupForColumn(seed, tile, maxY, (settings.getWaterLevel() > 0) ? settings.getWaterLevel() : -1, glassCeiling,
                surfaceBreaking, settings.isLeaveWater(), settings.isFloodWithLava());
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
                    for (int y = terrainheight; fallThrough ? (y >= 0) : (y >= minYAdjusted); y--) {
                        if (chunk.getMaterial(x, y, z) == AIR) {
                            // There is already a void here; assume that things
                            // like removing water, etc. have already been done
                            // by whatever made the void
                            emptyBlockEncountered();
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
                        processBlock(chunk, x, y, z, cavernLikelyhood > CAVERN_CHANCE);
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