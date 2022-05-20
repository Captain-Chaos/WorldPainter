/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.layers.exporters;

import org.pepsoft.minecraft.Material;
import org.pepsoft.util.PerlinNoise;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.exporting.*;
import org.pepsoft.worldpainter.layers.DeciduousForest;
import org.pepsoft.worldpainter.layers.GardenCategory;
import org.pepsoft.worldpainter.layers.PineForest;
import org.pepsoft.worldpainter.layers.TreeLayer;
import org.pepsoft.worldpainter.layers.trees.TreeType;

import javax.vecmath.Point3i;
import java.awt.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static java.util.Collections.singleton;
import static org.pepsoft.minecraft.Constants.MC_LAVA;
import static org.pepsoft.minecraft.Constants.MC_WATER;
import static org.pepsoft.minecraft.Material.*;
import static org.pepsoft.worldpainter.Constants.SMALL_BLOBS;
import static org.pepsoft.worldpainter.exporting.SecondPassLayerExporter.Stage.ADD_FEATURES;

/**
 *
 * @author pepijn
 */
public class TreesExporter<T extends TreeLayer> extends AbstractLayerExporter<T> implements SecondPassLayerExporter, IncidentalLayerExporter {
    public TreesExporter(Dimension dimension, Platform platform, ExporterSettings settings, T layer) {
        super(dimension, platform, (settings != null) ? settings : new TreeLayerSettings<>(layer), layer);
    }

    @Override
    public Set<Stage> getStages() {
        return singleton(ADD_FEATURES);
    }

    @SuppressWarnings("unchecked") // Responsibility of caller
    @Override
    public List<Fixup> addFeatures(Rectangle area, Rectangle exportedArea, MinecraftWorld minecraftWorld) {
        final TreeLayerSettings<T> settings = (TreeLayerSettings<T>) super.settings;
        final int minimumLevel = settings.getMinimumLevel();
        final int treeChance = settings.getTreeChance();
        final int maxWaterDepth = settings.getMaxWaterDepth();
        final int layerStrengthCap = settings.getLayerStrengthCap();
        final int maxZ = dimension.getMaxHeight() - 1;
        for (int chunkX = area.x; chunkX < area.x + area.width; chunkX += 16) {
            for (int chunkY = area.y; chunkY < area.y + area.height; chunkY += 16) {
                // Set the seed and randomizer according to the chunk coordinates to make sure the chunk is always
                // rendered the same, no matter how often it is rendered
                final long seed = dimension.getSeed() + (chunkX >> 4) * 65537L + (chunkY >> 4) * 4099L + layer.hashCode();
                Random random = new Random(seed);
                for (int x = chunkX; x < chunkX + 16; x++) {
                    for (int y = chunkY; y < chunkY + 16; y++) {
                        final int height = dimension.getIntHeightAt(x, y);
                        if ((height == Integer.MIN_VALUE) || (height >= maxZ)) {
                            // height == Integer.MIN_VALUE means there is no tile there
                            continue;
                        }
                        final int strength = Math.max(minimumLevel, dimension.getLayerValueAt(layer, x, y));
                        final int cappedStrength = Math.min(strength, layerStrengthCap);
                        if ((strength > 0) && (random.nextInt(treeChance) <= (cappedStrength * cappedStrength))) {
                            final int waterDepth = dimension.getWaterLevelAt(x, y) - height;
                            if (waterDepth > maxWaterDepth) {
                                continue;
                            }
                            // Don't build trees on air, or in lava or water, or where there is already a solid block (from another layer)
                            final Material blockTypeUnderTree = minecraftWorld.getMaterialAt(x, y, height);
                            final Material blockTypeAtTree = minecraftWorld.getMaterialAt(x, y, height + 1);
                            if ((blockTypeUnderTree == AIR)
                                    || (blockTypeUnderTree.isNamed(MC_WATER))
                                    || (blockTypeAtTree.isNamed(MC_LAVA))
                                    || (! blockTypeAtTree.veryInsubstantial)) {
                                continue;
                            }
                            // Don't build trees directly next to each other, or
                            // where there are structures blocking the location
                            // or extremely near
                            if (room(dimension, x, y, minecraftWorld)) {
                                // Plant a tree
                                renderTree(layer, x, y, height, strength, minecraftWorld, dimension, new Random(seed + x * 65537L + y), seed);
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked") // Responsibility of creator
    @Override
    public Fixup apply(Point3i location, int intensity, Rectangle exportedArea, MinecraftWorld minecraftWorld) {
        final TreeLayerSettings<T> settings = (TreeLayerSettings<T>) super.settings;
        final int treeChance = settings.getTreeChance();
        final int maxWaterDepth = settings.getMaxWaterDepth();
        final int layerStrengthCap = settings.getLayerStrengthCap();
        // Set the seed and randomizer according to the location coordinates to make sure the location is always
        // rendered the same, no matter how often it is rendered
        final long seed = dimension.getSeed() + location.x * 65537L + location.y * 4099L + location.z + layer.hashCode();
        final Random random = new Random(seed);
        final int strength = intensity * 15 / 100;
        final int cappedStrength = Math.min(strength, layerStrengthCap);
        if ((strength > 0) && (random.nextInt(treeChance) <= (cappedStrength * cappedStrength))) {
            final int waterDepth = dimension.getWaterLevelAt(location.x, location.y) - (location.z - 1);
            if (waterDepth > maxWaterDepth) {
                return null;
            }
            // Don't build trees on air, or in lava or water, or where there is already a solid block (from another layer)
            final Material blockTypeUnderTree = minecraftWorld.getMaterialAt(location.x, location.y, location.z - 1);
            final Material blockTypeAtTree = minecraftWorld.getMaterialAt(location.x, location.y, location.z);
            if ((blockTypeUnderTree == AIR)
                    || (blockTypeUnderTree.isNamed(MC_WATER))
                    || (blockTypeAtTree.isNamed(MC_LAVA))
                    || (! blockTypeAtTree.veryInsubstantial)) {
                return null;
            }
            // Don't build trees directly next to each other, or
            // where there are structures blocking the location
            // or extremely near
            if (room(dimension, location.x, location.y, minecraftWorld)) {
                // Plant a tree
                renderTree(layer, location.x, location.y, location.z - 1, strength, minecraftWorld, dimension, random, seed);
            }
        }
        return null;
    }

    private boolean room(Dimension dimension, int x, int y, MinecraftWorld minecraftWorld) {
        return room(dimension, x, y, -1, -1, minecraftWorld)
            && room(dimension, x, y, -1,  0, minecraftWorld)
            && room(dimension, x, y, -1,  1, minecraftWorld)
            && room(dimension, x, y,  0, -1, minecraftWorld)
            && room(dimension, x, y,  0,  0, minecraftWorld)
            && room(dimension, x, y,  0,  1, minecraftWorld)
            && room(dimension, x, y,  1, -1, minecraftWorld)
            && room(dimension, x, y,  1,  0, minecraftWorld)
            && room(dimension, x, y,  1,  1, minecraftWorld);
    }
    
    private boolean room(Dimension dimension, int x, int y, int dx, int dy, MinecraftWorld minecraftWorld) {
        final int height = dimension.getIntHeightAt(x + dx, y + dy);
        return (height >= dimension.getMinHeight())
            && (height < (dimension.getMaxHeight() - 1))
            && (! minecraftWorld.getMaterialAt(x + dx, y + dy, height + 1).simpleName.endsWith("_log"))
            && (! minecraftWorld.getMaterialAt(x + dx, y + dy, height + 1).simpleName.endsWith("_bark"))
            && (! minecraftWorld.getMaterialAt(x + dx, y + dy, height + 1).simpleName.endsWith("_wood"))
            && (dimension.getLayerValueAt(GardenCategory.INSTANCE, x + dx, y + dy) == GardenCategory.CATEGORY_UNOCCUPIED);
    }

    private void renderTree(TreeLayer layer, int x, int y, int height, int strength, MinecraftWorld minecraftWorld, Dimension dimension, Random random, long seed) {
        TreeType treeRenderer = layer.pickTree(random);
        treeRenderer.renderTree(x, y, height, strength, minecraftWorld, dimension, random);
        renderMushrooms(x, y, height, strength, minecraftWorld, random, seed);
    }

    @SuppressWarnings("unchecked") // Responsibility of caller
    private void renderMushrooms(int blockInWorldX, int blockInWorldY, int height, int strength, MinecraftWorld minecraftWorld, Random random, long seed) {
        if (height > (minecraftWorld.getMaxHeight() - 2)) {
            return;
        }
        final TreeLayerSettings<T> settings = (TreeLayerSettings<T>) super.settings;
        final int mushroomIncidence = settings.getMushroomIncidence();
        final float mushroomChance = settings.getMushroomChance();
        PerlinNoise perlinNoise = perlinNoiseRef.get();
        if (perlinNoise == null) {
            perlinNoise = new PerlinNoise(seed);
            perlinNoiseRef.set(perlinNoise);
        }
        if (perlinNoise.getSeed() != (seed + SEED_OFFSET)) {
            perlinNoise.setSeed(seed + SEED_OFFSET);
        }
        final int size = Math.min(2 + strength / 3, 5) + random.nextInt(3);
        final int r = Math.min(size / 2, 3);
        if (r > 0) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    if ((dx != 0) || (dy != 0)) {
                        final int rnd = random.nextInt(mushroomIncidence);
                        final int x = blockInWorldX + dx, y = blockInWorldY + dy;
                        if ((rnd == 0) && (minecraftWorld.getMaterialAt(x, y, height) != AIR) && (minecraftWorld.getMaterialAt(x, y, height + 1) == AIR)) {
                            final float chance = perlinNoise.getPerlinNoise(x / SMALL_BLOBS, y / SMALL_BLOBS, height / SMALL_BLOBS);
                            if (chance > mushroomChance) {
                                minecraftWorld.setMaterialAt(x, y, height + 1, BROWN_MUSHROOM);
                            } else if (chance < -mushroomChance) {
                                minecraftWorld.setMaterialAt(x, y, height + 1, RED_MUSHROOM);
                            }
                        }
                    }
                }
            }
        }
    }

    private final ThreadLocal<PerlinNoise> perlinNoiseRef = new ThreadLocal<>();

    private static final long SEED_OFFSET = 61380672;
    
    public static class TreeLayerSettings<T extends TreeLayer> implements ExporterSettings {
        public TreeLayerSettings(T layer) {
            this.layer = layer;
            treeChance = layer.getDefaultTreeChance();
            maxWaterDepth = layer.getDefaultMaxWaterDepth();
            mushroomChance = layer.getDefaultMushroomChance();
            mushroomIncidence = layer.getDefaultMushroomIncidence();
            layerStrengthCap = layer.getDefaultLayerStrengthCap();
        }

        @Override
        public boolean isApplyEverywhere() {
            return minimumLevel > 0;
        }

        public int getMinimumLevel() {
            return minimumLevel;
        }

        public void setMinimumLevel(int minimumLevel) {
            this.minimumLevel = minimumLevel;
        }

        public int getMaxWaterDepth() {
            return maxWaterDepth;
        }

        public void setMaxWaterDepth(int maxWaterDepth) {
            this.maxWaterDepth = maxWaterDepth;
        }

        public int getTreeChance() {
            return treeChance;
        }

        public void setTreeChance(int treeChance) {
            this.treeChance = treeChance;
        }

        public int getMushroomIncidence() {
            return mushroomIncidence;
        }

        public void setMushroomIncidence(int mushroomIncidence) {
            this.mushroomIncidence = mushroomIncidence;
        }

        public float getMushroomChance() {
            return mushroomChance;
        }

        public void setMushroomChance(float mushroomChance) {
            this.mushroomChance = mushroomChance;
        }

        public int getLayerStrengthCap() {
            return layerStrengthCap;
        }

        public void setLayerStrengthCap(int layerStrengthCap) {
            this.layerStrengthCap = layerStrengthCap;
        }

        @Override
        public T getLayer() {
            return layer;
        }

        @Override
        public ExporterSettings clone() {
            TreeLayerSettings<T> clone = new TreeLayerSettings<>(layer);
            clone.minimumLevel = minimumLevel;
            return clone;
        }
        
        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            
            // Legacy
            if (treeChance == 0) {
                treeChance = layer.getDefaultTreeChance();
                maxWaterDepth = layer.getDefaultMaxWaterDepth();
                mushroomChance = layer.getDefaultMushroomChance();
                mushroomIncidence = layer.getDefaultMushroomIncidence();
                layerStrengthCap = layer.getDefaultLayerStrengthCap();
            }
        }
        
        private final T layer;
        private int minimumLevel, maxWaterDepth, treeChance, mushroomIncidence, layerStrengthCap;
        private float mushroomChance;
        
        private static final long serialVersionUID = 1L;
    }
    
    // Legacy
    
    @Deprecated
    public static class DeciduousSettings implements ExporterSettings {
        @Override
        public boolean isApplyEverywhere() {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public DeciduousForest getLayer() {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public ExporterSettings clone() {
            throw new UnsupportedOperationException("Not supported");
        }
        
        private Object readResolve() throws ObjectStreamException {
            TreeLayerSettings<DeciduousForest> settings = new TreeLayerSettings<>(DeciduousForest.INSTANCE);
            settings.setMinimumLevel(minimumLevel);
            return settings;
        }
        
        private int minimumLevel;
        
        private static final long serialVersionUID = 2011060801L;
    }
    
    @Deprecated
    public static class PineSettings implements ExporterSettings {
        @Override
        public boolean isApplyEverywhere() {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public PineForest getLayer() {
            throw new UnsupportedOperationException("Not supported");
        }

        @Override
        public PineSettings clone() {
            throw new UnsupportedOperationException("Not supported");
        }
        
        private Object readResolve() throws ObjectStreamException {
            TreeLayerSettings<PineForest> settings = new TreeLayerSettings<>(PineForest.INSTANCE);
            settings.setMinimumLevel(minimumLevel);
            return settings;
        }
        
        private int minimumLevel;
        
        private static final long serialVersionUID = 2011071601L;
    }
}