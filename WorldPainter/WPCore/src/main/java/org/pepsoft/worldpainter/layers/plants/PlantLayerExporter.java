/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.layers.plants;

import org.pepsoft.minecraft.Constants;
import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.Dimension;
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

import static org.pepsoft.minecraft.Block.BLOCKS;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE_BITS;

/**
 *
 * @author pepijn
 */
public class PlantLayerExporter extends WPObjectExporter<PlantLayer> implements SecondPassLayerExporter, IncidentalLayerExporter {
    public PlantLayerExporter(PlantLayer layer) {
        super(layer);
    }

    @Override
    public List<Fixup> render(Dimension dimension, Rectangle area, Rectangle exportedArea, MinecraftWorld minecraftWorld) {
        final long seed = dimension.getSeed();
        final int tileX1 = exportedArea.x >> TILE_SIZE_BITS, tileX2 = (exportedArea.x + exportedArea.width - 1) >> TILE_SIZE_BITS;
        final int tileY1 = exportedArea.y >> TILE_SIZE_BITS, tileY2 = (exportedArea.y + exportedArea.height - 1) >> TILE_SIZE_BITS;
        final int maxY = minecraftWorld.getMaxHeight() - 1;
        final boolean generateTilledDirt = layer.isGenerateTilledDirt();
        final boolean blockRulesEnforced = ! "false".equalsIgnoreCase(System.getProperty("org.pepsoft.worldpainter.enforceBlockRules"));
        final Bo2ObjectProvider objectProvider = layer.getObjectProvider();
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
                                final Plant plant = (Plant) objectProvider.getObject();
                                if (plant.getCategory() == Plant.Category.WATER_PLANTS) {
                                    if ((! blockRulesEnforced) || plant.isValidFoundation(minecraftWorld, worldX, worldY, height)) {
                                        possiblyRenderWaterPlant(minecraftWorld, dimension, plant, worldX, worldY, height + 1);
                                    }
                                } else {
                                    if (tile.getIntHeight(x, y) >= tile.getWaterLevel(x, y)) {
                                        if (! blockRulesEnforced) {
                                            renderObject(minecraftWorld, dimension, plant, worldX, worldY, height + 1, false);
                                            if (generateTilledDirt
                                                    && (plant.getCategory() == Plant.Category.CROPS)
                                                    && ((minecraftWorld.getBlockTypeAt(worldX, worldY, height) == Constants.BLK_GRASS)
                                                    || (minecraftWorld.getBlockTypeAt(worldX, worldY, height) == Constants.BLK_DIRT))) {
                                                minecraftWorld.setMaterialAt(worldX, worldY, height, TILLED_DIRT);
                                            }
                                        } else {
                                            if (plant.isValidFoundation(minecraftWorld, worldX, worldY, height)) {
                                                renderObject(minecraftWorld, dimension, plant, worldX, worldY, height + 1, false);
                                            } else if (generateTilledDirt
                                                    && (plant.getCategory() == Plant.Category.CROPS)
                                                    && ((minecraftWorld.getBlockTypeAt(worldX, worldY, height) == Constants.BLK_GRASS)
                                                    || (minecraftWorld.getBlockTypeAt(worldX, worldY, height) == Constants.BLK_DIRT))) {
                                                minecraftWorld.setMaterialAt(worldX, worldY, height, TILLED_DIRT);
                                                renderObject(minecraftWorld, dimension, plant, worldX, worldY, height + 1, false);
                                            }
                                        }
                                    }
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
    public Fixup apply(Dimension dimension, Point3i location, int intensity, Rectangle exportedArea, MinecraftWorld minecraftWorld) {
        final Random random = incidentalRandomRef.get();
        final long seed = dimension.getSeed() ^ ((long) location.x << 40) ^ ((long) location.y << 20) ^ (location.z);
        random.setSeed(seed);
        if ((intensity >= 100) || ((intensity > 0) && (random.nextInt(100) < intensity))) {
            // Place plant
            final Bo2ObjectProvider objectProvider = layer.getObjectProvider();
            objectProvider.setSeed(seed);
            final Plant plant = (Plant) objectProvider.getObject();
            final int existingBlockType = minecraftWorld.getBlockTypeAt(location.x, location.y, location.z);
            if ((location.z < (minecraftWorld.getMaxHeight() - 1))
                    && ((plant.getCategory() == Plant.Category.WATER_PLANTS)
                        ? ((existingBlockType == Constants.BLK_WATER) || (existingBlockType == Constants.BLK_STATIONARY_WATER))
                        : (existingBlockType == Constants.BLK_AIR))) {
                if (plant.isValidFoundation(minecraftWorld, location.x, location.y, location.z - 1)) {
                    if (plant.getCategory() == Plant.Category.WATER_PLANTS) {
                        possiblyRenderWaterPlant(minecraftWorld, dimension, plant, location.x, location.y, location.z + 1);
                    } else {
                        renderObject(minecraftWorld, dimension, plant, location.x, location.y, location.z, false);
                    }
                } else if (layer.isGenerateTilledDirt()
                        && (plant.getCategory() == Plant.Category.CROPS)
                        && ((minecraftWorld.getBlockTypeAt(location.x, location.y, location.z - 1) == Constants.BLK_GRASS)
                            || (minecraftWorld.getBlockTypeAt(location.x, location.y, location.z - 1) == Constants.BLK_DIRT))) {
                    minecraftWorld.setMaterialAt(location.x, location.y, location.z - 1, TILLED_DIRT);
                    renderObject(minecraftWorld, dimension, plant, location.x, location.y, location.z, false);
                }
            }
        }
        return null;
    }
    
    private void possiblyRenderWaterPlant(MinecraftWorld world, Dimension dimension, Plant plant, int x, int y, int z) {
        final int maxHeight = world.getMaxHeight();
        int existingBlockType;
        do {
            z++;
            existingBlockType = world.getBlockTypeAt(x, y, z);
        } while ((z < maxHeight) && ((existingBlockType == Constants.BLK_STATIONARY_WATER) || (existingBlockType == Constants.BLK_WATER)));
        if ((z < maxHeight)
                && BLOCKS[existingBlockType].veryInsubstantial
                && (! ((existingBlockType == Constants.BLK_LAVA) || (existingBlockType == Constants.BLK_STATIONARY_LAVA)))) {
            renderObject(world, dimension, plant, x, y, z, false);
        }
    }

    private final ThreadLocal<Random> incidentalRandomRef = new ThreadLocal<Random>() {
        @Override
        protected Random initialValue() {
            return new Random();
        }
    };

    private static final Material TILLED_DIRT = Material.get(Constants.BLK_TILLED_DIRT, 4);
}