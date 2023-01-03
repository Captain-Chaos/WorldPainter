/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.layers.plants;

import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.exporting.Fixup;
import org.pepsoft.worldpainter.exporting.IncidentalLayerExporter;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;
import org.pepsoft.worldpainter.exporting.SecondPassLayerExporter;
import org.pepsoft.worldpainter.layers.bo2.Bo2ObjectProvider;
import org.pepsoft.worldpainter.layers.exporters.WPObjectExporter;

import javax.vecmath.Point3i;
import java.awt.*;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static java.util.Collections.singleton;
import static org.pepsoft.minecraft.Constants.MC_FARMLAND;
import static org.pepsoft.minecraft.Constants.MC_LAVA;
import static org.pepsoft.minecraft.Material.*;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE_BITS;
import static org.pepsoft.worldpainter.exporting.SecondPassLayerExporter.Stage.ADD_FEATURES;
import static org.pepsoft.worldpainter.layers.plants.Category.*;

/**
 *
 * @author pepijn
 */
public class PlantLayerExporter extends WPObjectExporter<PlantLayer> implements SecondPassLayerExporter, IncidentalLayerExporter {
    public PlantLayerExporter(Dimension dimension, Platform platform, PlantLayer layer) {
        super(dimension, platform, null, layer);
        final long total = layer.getConfiguredPlants().values().stream().mapToLong(setting -> setting.occurrence).sum();
        if (total <= 0L) {
            throw new IllegalArgumentException("No plants configured on PlantLayer \"" + layer + "\"");
        } else if (total > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Total occurrence of plants configured on PlantLayer \"" + layer + "\" higher than " + Integer.MAX_VALUE);
        }
    }

    @Override
    public Set<Stage> getStages() {
        return singleton(ADD_FEATURES);
    }

    @Override
    public List<Fixup> addFeatures(Rectangle area, Rectangle exportedArea, MinecraftWorld minecraftWorld) {
        final long seed = dimension.getSeed();
        final int tileX1 = exportedArea.x >> TILE_SIZE_BITS, tileX2 = (exportedArea.x + exportedArea.width - 1) >> TILE_SIZE_BITS;
        final int tileY1 = exportedArea.y >> TILE_SIZE_BITS, tileY2 = (exportedArea.y + exportedArea.height - 1) >> TILE_SIZE_BITS;
        final int minHeight = minecraftWorld.getMinHeight(), maxY = minecraftWorld.getMaxHeight() - 1;
        final boolean generateTilledDirt = layer.isGenerateFarmland();
        final boolean blockRulesEnforced = ! "false".equalsIgnoreCase(System.getProperty("org.pepsoft.worldpainter.enforceBlockRules"));
        final boolean onlyOnValidBlocks = layer.isOnlyOnValidBlocks();
        final Bo2ObjectProvider objectProvider = layer.getObjectProvider(platform);
        for (int tileX = tileX1; tileX <= tileX2; tileX++) {
            for (int tileY = tileY1; tileY <= tileY2; tileY++) {
                final Tile tile = dimension.getTile(tileX, tileY);
                if ((tile == null) || (! tile.hasLayer(layer))) {
                    // Tile doesn't exist, or it doesn't have the layer
                    continue;
                }
                final long tileSeed = (long) tileX << 32 ^ tileY ^ seed;
                objectProvider.setSeed(tileSeed);
                for (int x = 0; x < TILE_SIZE; x++) {
                    for (int y = 0; y < TILE_SIZE; y++) {
                        if (tile.getBitLayerValue(layer, x, y)) {
                            // Possibly place a plant
                            final int height = tile.getIntHeight(x, y);
                            if ((height >= minHeight) && (height < maxY)) {
                                final int worldX = (tileX << TILE_SIZE_BITS) | x, worldY = (tileY << TILE_SIZE_BITS) | y;
                                Plant plant = (Plant) objectProvider.getObject();
                                Category category = plant.isValidFoundation(minecraftWorld, worldX, worldY, height, onlyOnValidBlocks);
                                if (category == null) {
                                    // The plant disagrees that it can be planted here
                                    if (! blockRulesEnforced) {
                                        renderObject(minecraftWorld, dimension, platform, plant, worldX, worldY, height + 1, false);
                                    }
                                    continue;
                                } else if (category != FLOATING_PLANTS) {
                                    // There might already be a plant here, placed by a Terrain or Bo2Layer. Assume the
                                    // PlantLayer should take priority and remove the existing plant
                                    for (int dz = height + 1; dz <= minecraftWorld.getHighestNonAirBlock(worldX, worldY); dz++) {
                                        if (minecraftWorld.getMaterialAt(worldX, worldY, dz).vegetation) {
                                            clearBlock(minecraftWorld, worldX, worldY, dz);
                                        } else {
                                            break;
                                        }
                                    }
                                }
                                if (category == FLOATING_PLANTS) {
                                    possiblyRenderFloatingPlant(minecraftWorld, dimension, plant, worldX, worldY, height + 1);
                                } else if ((category == WATER_PLANTS) || (category == HANGING_WATER_PLANTS) || (category == DRIPLEAF)) {
                                    if (plant.getMaterial(0, 0, 0).hasProperty(WATERLOGGED)) {
                                        // Take this as a signal that the plant can stick out of the water and does not
                                        // have to be constrained to the water surface
                                        renderObject(minecraftWorld, dimension, platform, plant, worldX, worldY, height + 1, false);
                                    } else {
                                        int waterLevel = tile.getWaterLevel(x, y);
                                        if (waterLevel > height) {
                                            // Constrain the height to ensure the plant does not stick out of the water:
                                            if (height + plant.getDimensions().z > waterLevel) {
                                                int newHeight = waterLevel - height;
                                                if (newHeight < 1) {
                                                    continue;
                                                } else {
                                                    plant = plant.realise(newHeight, platform);
                                                    // Some plants can't shrink down to fit
                                                    if (plant.getDimensions().z > newHeight) {
                                                        continue;
                                                    }
                                                }
                                            }
                                            renderObject(minecraftWorld, dimension, platform, plant, worldX, worldY, height + 1, false);
                                        }
                                    }
                                } else if (category == CROPS) {
                                    if (minecraftWorld.getMaterialAt(worldX, worldY, height).isNamed(MC_FARMLAND)) {
                                        renderObject(minecraftWorld, dimension, platform, plant, worldX, worldY, height + 1, false);
                                    } else if (generateTilledDirt) {
                                        minecraftWorld.setMaterialAt(worldX, worldY, height, TILLED_DIRT);
                                        renderObject(minecraftWorld, dimension, platform, plant, worldX, worldY, height + 1, false);
                                    }
                                } else {
                                    // TODO shrink the plant to fit if necessary
                                    renderObject(minecraftWorld, dimension, platform, plant, worldX, worldY, height + 1, false);
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public Fixup apply(Point3i location, int intensity, Rectangle exportedArea, MinecraftWorld minecraftWorld) {
        final Random random = incidentalRandomRef.get();
        final long seed = dimension.getSeed() + location.x + location.y * 4099L + location.z * 65537L + layer.hashCode();
        random.setSeed(seed);
        if ((intensity >= 100) || ((intensity > 0) && (random.nextInt(100) < intensity))) {
            // Place plant
            final Bo2ObjectProvider objectProvider = layer.getObjectProvider(platform);
            objectProvider.setSeed(seed + 1);
            final Plant plant = (Plant) objectProvider.getObject();
            Category category = plant.isValidFoundation(minecraftWorld, location.x, location.y, location.z - 1, layer.isOnlyOnValidBlocks());
            if ((category != null)
                    && (location.z < (minecraftWorld.getMaxHeight() - 1))) {
                if (category == FLOATING_PLANTS) {
                    possiblyRenderFloatingPlant(minecraftWorld, dimension, plant, location.x, location.y, location.z + 1);
                } else if (category == CROPS) {
                    if (minecraftWorld.getMaterialAt(location.x, location.y, location.z - 1).isNamed(MC_FARMLAND)) {
                        renderObject(minecraftWorld, dimension, platform, plant, location.x, location.y, location.z, false);
                    } else if (layer.isGenerateFarmland()) {
                        minecraftWorld.setMaterialAt(location.x, location.y, location.z - 1, TILLED_DIRT);
                        renderObject(minecraftWorld, dimension, platform, plant, location.x, location.y, location.z, false);
                    }
                } else {
                    // TODO shrink the plant to fit if necessary
                    renderObject(minecraftWorld, dimension, platform, plant, location.x, location.y, location.z, false);
                }
            }
        }
        return null;
    }
    
    private void possiblyRenderFloatingPlant(MinecraftWorld world, Dimension dimension, Plant plant, int x, int y, int z) {
        final int maxHeight = world.getMaxHeight();
        Material existingMaterial;
        do {
            z++;
            existingMaterial = world.getMaterialAt(x, y, z);
        } while ((z < maxHeight) && existingMaterial.containsWater());
        if ((z < maxHeight)
                && existingMaterial.veryInsubstantial
                && (! existingMaterial.isNamed(MC_LAVA))) {
            renderObject(world, dimension, platform, plant, x, y, z, false);
        }
    }

    private static void clearBlock(MinecraftWorld minecraftWorld, int x, int y, int z) {
        if (minecraftWorld.getMaterialAt(x, y, z).containsWater()) {
            minecraftWorld.setMaterialAt(x, y, z, STATIONARY_WATER);
        } else {
            minecraftWorld.setMaterialAt(x, y, z, AIR);
        }
    }

    private final ThreadLocal<Random> incidentalRandomRef = new ThreadLocal<Random>() {
        @Override
        protected Random initialValue() {
            return new Random();
        }
    };

    private static final Material TILLED_DIRT = FARMLAND.withProperty(MOISTURE, 4);
}