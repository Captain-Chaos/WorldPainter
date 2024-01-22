/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers.tunnel;

import org.pepsoft.minecraft.Chunk;
import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.exporting.*;
import org.pepsoft.worldpainter.heightMaps.NoiseHeightMap;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.exporters.AbstractCavesExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.vecmath.Point3i;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static java.awt.Font.PLAIN;
import static org.pepsoft.util.MathUtils.clamp;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE_MASK;
import static org.pepsoft.worldpainter.exporting.SecondPassLayerExporter.Stage.ADD_FEATURES;
import static org.pepsoft.worldpainter.layers.tunnel.TunnelLayer.Mode.CUSTOM_DIMENSION;

/**
 * Abstract base class for custom tunnel or floating layer exporters.
 *
 * @author SchmitzP
 */
public abstract class AbstractTunnelLayerExporter extends AbstractCavesExporter<TunnelLayer> {
    public AbstractTunnelLayerExporter(Dimension dimension, Platform platform, TunnelLayer layer, TunnelLayerHelper helper) {
        super(dimension, platform, null, layer);
        this.helper = helper;
    }

    public List<Fixup> addFeatures(Rectangle area, Rectangle exportedArea, MinecraftWorld world) {
        final Dimension floorDimension = ((layer.getFloorMode() == CUSTOM_DIMENSION) && (layer.getFloorDimensionId() != null))
                ? dimension.getWorld().getDimension(new Dimension.Anchor(dimension.getAnchor().dim, Dimension.Role.CAVE_FLOOR, dimension.getAnchor().invert, layer.getFloorDimensionId()))
                : null;
        final List<Fixup> fixups = new ArrayList<>();

        // Render floor dimension
        if (floorDimension != null) {
            // TODO add support for combined layers
            // TODO add support for first pass exporters
            final List<Layer> floorLayers = new ArrayList<>(floorDimension.getAllLayers(false));
            floorLayers.addAll(floorDimension.getMinimumLayers());
            floorLayers.stream()
                    .sorted()
                    .filter(layer -> (layer.getExporterType() != null) && SecondPassLayerExporter.class.isAssignableFrom(layer.getExporterType()))
                    .map(layer -> (SecondPassLayerExporter) layer.getExporter(floorDimension, platform, floorDimension.getLayerSettings(layer)))
                    .filter(exporter -> exporter.getStages().contains(ADD_FEATURES))
                    .forEach(exporter -> {
                final List<Fixup> layerFixups = exporter.addFeatures(area, exportedArea, world);
                if (layerFixups != null) {
                    fixups.addAll(layerFixups);
                }
            });
        } else {

            // Or render floor layers
            final List<TunnelLayer.LayerSettings> floorLayers = layer.getFloorLayers();
            if ((floorLayers != null) && (! floorLayers.isEmpty())) {
                final IncidentalLayerExporter[] floorExporters = new IncidentalLayerExporter[floorLayers.size()];
                final TunnelLayer.LayerSettings[] floorLayerSettings = new TunnelLayer.LayerSettings[floorLayers.size()];
                final NoiseHeightMap[] floorLayerNoise = new NoiseHeightMap[floorLayers.size()];
                int index = 0;
                for (TunnelLayer.LayerSettings layerSettings: floorLayers) {
                    floorExporters[index] = (IncidentalLayerExporter) layerSettings.getLayer().getExporter(new TunnelFloorDimension(dimension, layer, helper), platform, null);
                    floorLayerSettings[index] = layerSettings;
                    if (layerSettings.getVariation() != null) {
                        floorLayerNoise[index] = new NoiseHeightMap(layerSettings.getVariation(), index);
                        floorLayerNoise[index].setSeed(dimension.getSeed());
                    }
                    index++;
                }
                visitChunksForLayerInAreaForEditing(world, layer, area, dimension, (tile, chunkX, chunkZ, chunkSupplier) ->
                        whereTunnelIsRealisedDo(tile, chunkX, chunkZ, chunkSupplier, (chunk, x, y, xInTile, yInTile, terrainHeight, actualFloorLevel, floorLedgeHeight, actualRoofLevel, roofLedgeHeight) -> {
                            final int z = actualFloorLevel + 1;
                            final Point3i location = new Point3i(x, y, z);
                            for (int i = 0; i < floorExporters.length; i++) {
                                if ((z >= floorLayerSettings[i].getMinLevel()) && (z <= floorLayerSettings[i].getMaxLevel())) {
                                    final int intensity = floorLayerNoise[i] != null
                                            ? (int) clamp(0L, Math.round(floorLayerSettings[i].getIntensity() + floorLayerNoise[i].getValue(x, y, z) - floorLayerNoise[i].getHeight() / 2), 100L)
                                            : floorLayerSettings[i].getIntensity();
                                    if (intensity > 0) {
                                        Fixup fixup = floorExporters[i].apply(location, intensity, exportedArea, world);
                                        if (fixup != null) {
                                            fixups.add(fixup);
                                        }
                                    }
                                }
                            }
                            return true;
                        }));
            }
        }

        // Render roof layers
        final List<TunnelLayer.LayerSettings> roofLayers = layer.getRoofLayers();
        if ((roofLayers != null) && (! roofLayers.isEmpty())) {
            final IncidentalLayerExporter[] roofExporters = new IncidentalLayerExporter[roofLayers.size()];
            final TunnelLayer.LayerSettings[] roofLayerSettings = new TunnelLayer.LayerSettings[roofLayers.size()];
            final NoiseHeightMap[] roofLayerNoise = new NoiseHeightMap[roofLayers.size()];
            int index = 0;
            for (TunnelLayer.LayerSettings layerSettings: roofLayers) {
                roofExporters[index] = (IncidentalLayerExporter) layerSettings.getLayer().getExporter(new TunnelRoofDimension(dimension, layer, helper), platform, null);
                roofLayerSettings[index] = layerSettings;
                if (layerSettings.getVariation() != null) {
                    roofLayerNoise[index] = new NoiseHeightMap(layerSettings.getVariation(), index);
                    roofLayerNoise[index].setSeed(dimension.getSeed());
                }
                index++;
            }
            final MinecraftWorld invertedWorld = new InvertedWorld(world, 0, platform);
            visitChunksForLayerInAreaForEditing(world, layer, area, dimension, (tile, chunkX, chunkZ, chunkSupplier) ->
                    whereTunnelIsRealisedDo(tile, chunkX, chunkZ, chunkSupplier, (chunk, x, y, xInTile, yInTile, terrainHeight, actualFloorLevel, floorLedgeHeight, actualRoofLevel, roofLedgeHeight) -> {
                        final int z = actualRoofLevel;
                        final Point3i location = new Point3i(x, y, maxHeight + minHeight - 1 - z);
                        for (int i = 0; i < roofExporters.length; i++) {
                            if ((z >= roofLayerSettings[i].getMinLevel()) && (z <= roofLayerSettings[i].getMaxLevel())) {
                                final int intensity = roofLayerNoise[i] != null
                                        ? (int) clamp(0L, Math.round(roofLayerSettings[i].getIntensity() + roofLayerNoise[i].getValue(x, y, z) - roofLayerNoise[i].getHeight() / 2), 100L)
                                        : roofLayerSettings[i].getIntensity();
                                if (intensity > 0) {
                                    roofExporters[i].apply(location, intensity, exportedArea, invertedWorld);
                                    // TODO support inverted fixups
                                }
                            }
                        }
                        return true;
                    }));
        }

        return fixups.isEmpty() ? null : fixups;
    }

    //    private void excavateDisc(final MinecraftWorld world, final int x, final int y, final int z, int r, final Material materialAbove, final Material materialBesides, final Material materialBelow) {
//        GeometryUtil.visitFilledCircle(r, new PlaneVisitor() {
//            @Override
//            public boolean visit(int dx, int dy, float d) {
//                world.setMaterialAt(x + dx, y + dy, z, Material.AIR);
//                if (materialAbove != null) {
//                    setIfSolid(world, x + dx, y + dy, z - 1, 0, Integer.MAX_VALUE, materialAbove);
//                }
//                if (materialBesides != null) {
//                    setIfSolid(world, x + dx - 1, y + dy, z, 0, Integer.MAX_VALUE, materialBesides);
//                    setIfSolid(world, x + dx, y + dy - 1, z, 0, Integer.MAX_VALUE, materialBesides);
//                    setIfSolid(world, x + dx + 1, y + dy, z, 0, Integer.MAX_VALUE, materialBesides);
//                    setIfSolid(world, x + dx, y + dy + 1, z, 0, Integer.MAX_VALUE, materialBesides);
//                }
//                if (materialBelow != null) {
//                    setIfSolid(world, x + dx, y + dy, z + 1, 0, Integer.MAX_VALUE, materialBelow);
//                }
//                return true;
//            }
//        });
//    }

    @FunctionalInterface interface ColumnVisitor {
        boolean visitColumn(Chunk chunk, int x, int y, int xInTile, int yInTile, int terrainHeight, int actualFloorLevel, int floorLedgeHeight, int actualRoofLevel, int roofLedgeHeight);
    }

    protected final boolean whereTunnelIsRealisedDo(Tile tile, int chunkX, int chunkZ, Supplier<Chunk> chunkSupplier, ColumnVisitor visitor) {
        Chunk chunk = null;
        for (int xInChunk = 0; xInChunk < 16; xInChunk++) {
            for (int zInChunk = 0; zInChunk < 16; zInChunk++) {
                final int x = (chunkX << 4) | xInChunk, y = (chunkZ << 4) | zInChunk;
                final int xInTile = x & TILE_SIZE_MASK, yInTile = y & TILE_SIZE_MASK;
                if (tile.getBitLayerValue(layer, xInTile, yInTile)) {
                    int terrainHeight = tile.getIntHeight(xInTile, yInTile);
                    int actualFloorLevel = helper.calculateFloorLevel(x, y, terrainHeight, minZ, maxZ);
                    int actualRoofLevel = helper.calculateRoofLevel(x, y, terrainHeight, minZ, maxZ, actualFloorLevel);
                    if (actualRoofLevel <= actualFloorLevel) {
                        continue;
                    }
                    final int floorLedgeHeight = helper.calculateBottomLedgeHeight(x, y);
                    final int roofLedgeHeight = helper.calculateTopLedgeHeight(x, y);
                    actualFloorLevel += floorLedgeHeight;
                    actualRoofLevel -= roofLedgeHeight;
                    if (actualRoofLevel <= actualFloorLevel) {
                        continue;
                    }
                    if (chunk == null) {
                        chunk = chunkSupplier.get();
                    }
                    if (! visitor.visitColumn(chunk, x, y, xInTile, yInTile, terrainHeight, actualFloorLevel, floorLedgeHeight, actualRoofLevel, roofLedgeHeight)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Set a block on the specified world or chunk, if the location is valid and the existing block at that location is
     * substantial.
     *
     * <p>If the specified coordinates lie on the specified {@link Chunk}, block will be set directly on it; otherwise
     * it will be set via the {@link MinecraftWorld}.
     */
    protected final void setIfSolid(MinecraftWorld world, Chunk chunk, int x, int y, int z, int minZ, int maxZ, Material material, boolean flooded, int terrainHeight, int waterLevel, boolean removeWater) {
        if ((z >= minZ) && (z <= maxZ)) {
            if (removeWater || (! flooded) || (z <= terrainHeight) || (z > waterLevel)) {
                if (((x >> 4) == chunk.getxPos()) && ((y >> 4) == chunk.getzPos())) {
                    final Material existingBlock = chunk.getMaterial(x & 0xf, z, y & 0xf);
                    if (((! existingBlock.empty)) && (! existingBlock.insubstantial)) {
                        // The coordinates are within bounds and the existing block is solid
                        chunk.setMaterial(x & 0xf, z, y & 0xf, material);
                    }
                } else {
                    final Material existingBlock = world.getMaterialAt(x, y, z);
                    if (((! existingBlock.empty)) && (! existingBlock.insubstantial)) {
                        // The coordinates are within bounds and the existing block is solid
                        world.setMaterialAt(x, y, z, material);
                    }
                }
            }
        }
    }

    protected final TunnelLayerHelper helper;

    protected static final Font HEIGHT_MARKER_FONT = new Font("SansSerif", PLAIN, 10);
    private static final Logger logger = LoggerFactory.getLogger(AbstractTunnelLayerExporter.class);
}