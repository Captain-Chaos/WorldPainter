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

import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.minecraft.Material.*;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE_BITS;
import static org.pepsoft.worldpainter.layers.plants.Category.*;

/**
 *
 * @author pepijn
 */
public class PlantLayerExporter extends WPObjectExporter<PlantLayer> implements SecondPassLayerExporter, IncidentalLayerExporter {
    public PlantLayerExporter(PlantLayer layer) {
        super(layer);
    }

    @Override
    public List<Fixup> render(Dimension dimension, Rectangle area, Rectangle exportedArea, MinecraftWorld minecraftWorld, Platform platform) {
        final long seed = dimension.getSeed();
        final int tileX1 = exportedArea.x >> TILE_SIZE_BITS, tileX2 = (exportedArea.x + exportedArea.width - 1) >> TILE_SIZE_BITS;
        final int tileY1 = exportedArea.y >> TILE_SIZE_BITS, tileY2 = (exportedArea.y + exportedArea.height - 1) >> TILE_SIZE_BITS;
        final int maxY = minecraftWorld.getMaxHeight() - 1;
        final boolean generateTilledDirt = layer.isGenerateFarmland();
        final boolean blockRulesEnforced = ! "false".equalsIgnoreCase(System.getProperty("org.pepsoft.worldpainter.enforceBlockRules"));
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
                            if (height < maxY) {
                                final int worldX = (tileX << TILE_SIZE_BITS) | x, worldY = (tileY << TILE_SIZE_BITS) | y;
                                Plant plant = (Plant) objectProvider.getObject();
                                Category category = plant.isValidFoundation(minecraftWorld, worldX, worldY, height);
                                if (category == null) {
                                    // The plant disagrees that it can be planted here
                                    if (! blockRulesEnforced) {
                                        renderObject(minecraftWorld, dimension, plant, worldX, worldY, height + 1, false);
                                    }
                                    continue;
                                }
                                if (category == FLOATING_PLANTS) {
                                    possiblyRenderFloatingPlant(minecraftWorld, dimension, plant, worldX, worldY, height + 1);
                                } else if (category == WATER_PLANTS) {
                                    int waterLevel = tile.getWaterLevel(x, y);
                                    if (waterLevel > height) {
                                        // Constrain the height to ensure the plant does not stick out of the water:
                                        if ((height + plant.getDimensions().z > waterLevel) && (! plant.getName().equals("Big Dripleaf")) /* TODO make this dynamic */) {
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
                                        renderObject(minecraftWorld, dimension, plant, worldX, worldY, height + 1, false);
                                    }
                                } else if (category == CROPS) {
                                    if (minecraftWorld.getMaterialAt(worldX, worldY, height).isNamed(MC_FARMLAND)) {
                                        renderObject(minecraftWorld, dimension, plant, worldX, worldY, height + 1, false);
                                    } else if (generateTilledDirt) {
                                        minecraftWorld.setMaterialAt(worldX, worldY, height, TILLED_DIRT);
                                        renderObject(minecraftWorld, dimension, plant, worldX, worldY, height + 1, false);
                                    }
                                } else {
                                    renderObject(minecraftWorld, dimension, plant, worldX, worldY, height + 1, false);
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
    public Fixup apply(Dimension dimension, Point3i location, int intensity, Rectangle exportedArea, MinecraftWorld minecraftWorld, Platform platform) {
        final Random random = incidentalRandomRef.get();
        final long seed = dimension.getSeed() ^ ((long) location.x << 40) ^ ((long) location.y << 20) ^ (location.z);
        random.setSeed(seed);
        if ((intensity >= 100) || ((intensity > 0) && (random.nextInt(100) < intensity))) {
            // Place plant
            final Bo2ObjectProvider objectProvider = layer.getObjectProvider(platform);
            objectProvider.setSeed(seed);
            final Plant plant = (Plant) objectProvider.getObject();
            final Material existingMaterial = minecraftWorld.getMaterialAt(location.x, location.y, location.z);
            Category category = plant.isValidFoundation(minecraftWorld, location.x, location.y, location.z - 1);
            if ((location.z < (minecraftWorld.getMaxHeight() - 1))
                    && ((category == FLOATING_PLANTS) ? existingMaterial.isNamed(MC_WATER) : (existingMaterial == AIR))) {
                // TODOMC13 support water plants
                if (category == FLOATING_PLANTS) {
                    possiblyRenderFloatingPlant(minecraftWorld, dimension, plant, location.x, location.y, location.z + 1);
                } else if (category == CROPS) {
                    if (minecraftWorld.getMaterialAt(location.x, location.y, location.z - 1).isNamed(MC_FARMLAND)) {
                        renderObject(minecraftWorld, dimension, plant, location.x, location.y, location.z, false);
                    } else if (layer.isGenerateFarmland()) {
                        minecraftWorld.setMaterialAt(location.x, location.y, location.z - 1, TILLED_DIRT);
                        renderObject(minecraftWorld, dimension, plant, location.x, location.y, location.z, false);
                    }
                } else if (category != null) {
                    renderObject(minecraftWorld, dimension, plant, location.x, location.y, location.z, false);
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
        } while ((z < maxHeight) && existingMaterial.isNamed(MC_WATER));
        if ((z < maxHeight)
                && existingMaterial.veryInsubstantial
                && (! existingMaterial.isNamed(MC_LAVA))) {
            renderObject(world, dimension, plant, x, y, z, false);
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