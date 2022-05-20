/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.layers.exporters;

import com.google.common.collect.ImmutableSet;
import org.pepsoft.minecraft.Chunk;
import org.pepsoft.util.PerlinNoise;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.exporting.Fixup;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;
import org.pepsoft.worldpainter.exporting.SecondPassLayerExporter;
import org.pepsoft.worldpainter.layers.Caverns;
import org.pepsoft.worldpainter.layers.Void;

import java.awt.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.pepsoft.minecraft.Material.AIR;
import static org.pepsoft.worldpainter.Constants.*;
import static org.pepsoft.worldpainter.exporting.SecondPassLayerExporter.Stage.ADD_FEATURES;
import static org.pepsoft.worldpainter.exporting.SecondPassLayerExporter.Stage.CARVE;

/**
 *
 * @author pepijn
 */
public class CavernsExporter extends AbstractCavesExporter<Caverns> implements SecondPassLayerExporter {
    public CavernsExporter(Dimension dimension, Platform platform, ExporterSettings settings) {
        super(dimension, platform, (settings instanceof CavernsSettings) ? ((CavernsSettings) settings) : new CavernsSettings(), Caverns.INSTANCE);
    }
    
    @Override
    public Set<Stage> getStages() {
        return ImmutableSet.of(CARVE, ADD_FEATURES);
    }

    @Override
    public List<Fixup> carve(Rectangle area, Rectangle exportedArea, MinecraftWorld minecraftWorld) {
        final CavernsSettings settings = (CavernsSettings) super.settings;
        final boolean surfaceBreaking = settings.isSurfaceBreaking();
        final boolean glassCeiling = settings.isGlassCeiling();
        final int minimumLevel = settings.getCavernsEverywhereLevel();
        final int minY = settings.getMinimumLevel(), minHeight = dimension.getMinHeight();
        final int maxY = Math.min(settings.getMaximumLevel(), dimension.getMaxHeight() - 1), extremeY = Integer.MAX_VALUE - Math.max(-minY, 0);
        final boolean fallThrough = (minY == minHeight) && dimension.isBottomless();
        final int minYAdjusted = Math.max(minY, minHeight + 1);
        final long seed = dimension.getSeed();
        if ((seed + SEED_OFFSET) != perlinNoise.getSeed()) {
            perlinNoise.setSeed(seed + SEED_OFFSET);
        }
        visitChunksForLayerInAreaForEditing(minecraftWorld, layer, area, dimension, (tile, chunkX, chunkZ, chunkSupplier) -> {
            final Chunk chunk = chunkSupplier.get();
            final int xOffset = (chunkX & 7) << 4;
            final int zOffset = (chunkZ & 7) << 4;
            setupForColumn(seed, tile, maxY, (settings.getWaterLevel() > minHeight) ? settings.getWaterLevel() : minHeight - 1, glassCeiling,
                    surfaceBreaking, settings.isLeaveWater(), settings.isFloodWithLava()); // TODO shouldn't we at least reset the flags for every column?
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    final int localX = xOffset + x, localY = zOffset + z;
                    final int worldX = tile.getX() * TILE_SIZE + localX, worldY = tile.getY() * TILE_SIZE + localY;
                    if (tile.getBitLayerValue(Void.INSTANCE, localX, localY)) {
                        continue;
                    }
                    final int cavernsValue = Math.max(minimumLevel, tile.getLayerValue(Caverns.INSTANCE, localX, localY));
                    if (cavernsValue > 0) {
                        final int terrainheight = Math.min(tile.getIntHeight(localX, localY), maxY);
//                        if ((x == 0) && (z == 0)) {
//                            System.out.println("terrainHeight: " + terrainheight);
//                        }
                        for (int y = terrainheight; fallThrough ? (y >= minHeight) : (y >= minYAdjusted); y--) {
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
                                                    (fallThrough ? extremeY : (y - 1)) - minY),
                                            10)),
                                    1.0f - cavernsValue / 15.0f);
                            if (fallThrough && (y < minHeight + 5)) {
                                // Widen the caverns towards the bottom
                                bias -= (minHeight + 5 - y) * 0.05f;
                            }
                            final float cavernLikelyhood = perlinNoise.getPerlinNoise(worldX / MEDIUM_BLOBS, worldY / MEDIUM_BLOBS, y / SMALL_BLOBS) + 0.5f - bias;
//                            if ((x == 0) && (z == 0)) {
//                                System.out.println(y + ": bias: " + bias + ", cavernLikelyHood: " + cavernLikelyhood);
//                            }
                            processBlock(chunk, x, y, z, cavernLikelyhood > CAVERN_CHANCE);
                        }
                    }
                    if (glassCeiling) {
                        chunk.setHeight(x, z, 1);
                    }
                    resetColumn();
                }
            }
            return true;
        });
        return null;
    }

    @Override
    public List<Fixup> addFeatures(Rectangle area, Rectangle exportedArea, MinecraftWorld minecraftWorld) {
        final CavernsSettings settings = (CavernsSettings) super.settings;
        final boolean surfaceBreaking = settings.isSurfaceBreaking();
        final int minimumLevel = settings.getCavernsEverywhereLevel();
        final int minY = settings.getMinimumLevel(), minHeight = dimension.getMinHeight();
        final int maxY = Math.min(settings.getMaximumLevel(), dimension.getMaxHeight() - 1), extremeY = Integer.MAX_VALUE - Math.max(-minY, 0);
        final boolean fallThrough = (minY == minHeight) && dimension.isBottomless();
        final int minYAdjusted = Math.max(minY, minHeight + 1);
        final long seed = dimension.getSeed();
        if ((seed + SEED_OFFSET) != perlinNoise.getSeed()) {
            perlinNoise.setSeed(seed + SEED_OFFSET);
        }
        final Random random = new Random(seed + SEED_OFFSET + 1);
        visitChunksForLayerInAreaForEditing(minecraftWorld, layer, area, dimension, (tile, chunkX, chunkZ, chunkSupplier) -> {
            final int xOffsetInTile = (chunkX & 7) << 4;
            final int yOffsetInTile = (chunkZ & 7) << 4;
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    final int xInTile = xOffsetInTile + x, yInTile = yOffsetInTile + z;
                    final int worldX = tile.getX() * TILE_SIZE + xInTile, worldY = tile.getY() * TILE_SIZE + yInTile;
                    if (tile.getBitLayerValue(Void.INSTANCE, xInTile, yInTile)) {
                        continue;
                    }
                    final int cavernsValue = Math.max(minimumLevel, tile.getLayerValue(Caverns.INSTANCE, xInTile, yInTile));
                    if (cavernsValue > 0) {
                        final int terrainheight = Math.min(tile.getIntHeight(xInTile, yInTile), maxY);
                        for (int y = terrainheight; fallThrough ? (y >= minHeight) : (y >= minYAdjusted); y--) {
                            float bias = CAVERN_CHANCE
                                    * Math.max(
                                    0.1f * (10 - Math.min(
                                            Math.min(
                                                    surfaceBreaking ? Integer.MAX_VALUE : (terrainheight - Math.max(dimension.getTopLayerDepth(worldX, worldY, terrainheight), 3) - y),
                                                    (fallThrough ? extremeY : (y - 1)) - minY),
                                            10)),
                                    1.0f - cavernsValue / 15.0f);
                            if (fallThrough && (y < minHeight + 5)) {
                                // Widen the caverns towards the bottom
                                bias -= (minHeight + 5 - y) * 0.05f;
                            }
                            final float cavernLikelyhood = perlinNoise.getPerlinNoise(worldX / MEDIUM_BLOBS, worldY / MEDIUM_BLOBS, y / SMALL_BLOBS) + 0.5f - bias;
                            if (cavernLikelyhood > CAVERN_CHANCE) {
                                decorateBlock(minecraftWorld, random, worldX, worldY, y);
                            }
                        }
                    }
                }
            }
            return true;
        });
        return null;
    }

    private final PerlinNoise perlinNoise = new PerlinNoise(0);
//    private int lowestValueCavern = Integer.MAX_VALUE;
    
    private static final float CAVERN_CHANCE = 0.5f;
    private static final long SEED_OFFSET = 37;

    public static class CavernsSettings implements CaveSettings {
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

        public CaveDecorationSettings getCaveDecorationSettings() {
            return decorationSettings;
        }

        public void setCaveDecorationSettings(CaveDecorationSettings decorationSettings) {
            if (decorationSettings == null) {
                throw new NullPointerException();
            }
            this.decorationSettings = decorationSettings;
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
            if (! this.decorationSettings.equals(other.decorationSettings)) {
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
            hash = 29 * hash + this.decorationSettings.hashCode();
            return hash;
        }

        @Override
        public CavernsSettings clone() {
            try {
                final CavernsSettings clone = (CavernsSettings) super.clone();
                clone.decorationSettings = decorationSettings.clone();
                return clone;
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
            if (decorationSettings == null) {
                decorationSettings = new CaveDecorationSettings();
            }
        }

        private int waterLevel, cavernsEverywhereLevel;
        private boolean floodWithLava, glassCeiling, surfaceBreaking, leaveWater = true;
        private int minimumLevel = 0, maximumLevel = Integer.MAX_VALUE;
        private CaveDecorationSettings decorationSettings = new CaveDecorationSettings();
        
        private static final long serialVersionUID = 2011060801L;
    }
}