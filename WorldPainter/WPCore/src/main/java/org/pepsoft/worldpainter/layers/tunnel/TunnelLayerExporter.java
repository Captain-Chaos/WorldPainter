/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers.tunnel;

import org.pepsoft.minecraft.Chunk;
import org.pepsoft.minecraft.Material;
import org.pepsoft.util.MathUtils;
import org.pepsoft.util.PerlinNoise;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.MixedMaterial;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.exporting.*;
import org.pepsoft.worldpainter.heightMaps.NoiseHeightMap;
import org.pepsoft.worldpainter.layers.Layer;

import javax.vecmath.Point3i;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.pepsoft.worldpainter.Constants.TILE_SIZE_MASK;
import static org.pepsoft.worldpainter.Platform.Capability.BIOMES_3D;
import static org.pepsoft.worldpainter.Platform.Capability.NAMED_BIOMES;
import static org.pepsoft.worldpainter.biomeschemes.Minecraft1_18Biomes.MODERN_IDS;

/**
 *
 * @author SchmitzP
 */
public class TunnelLayerExporter extends AbstractLayerExporter<TunnelLayer> implements SecondPassLayerExporter {
    public TunnelLayerExporter(TunnelLayer layer) {
        super(layer);
        if (layer.getFloorNoise() != null) {
            floorNoise = new NoiseHeightMap(layer.getFloorNoise(), FLOOR_NOISE_SEED_OFFSET);
            floorNoiseOffset = layer.getFloorNoise().getRange();
        } else {
            floorNoise = null;
            floorNoiseOffset = 0;
        }
        if (layer.getRoofNoise() != null) {
            roofNoise = new NoiseHeightMap(layer.getRoofNoise(), ROOF_NOISE_SEED_OFFSET);
            roofNoiseOffset = layer.getRoofNoise().getRange();
        } else {
            roofNoise = null;
            roofNoiseOffset = 0;
        }
//        if (layer.getWallNoise() != null) {
//            wallNoise = new PerlinNoise(layer.getWallNoise().getSeed());
//        } else {
//            wallNoise = null;
//        }
    }

    @Override
    public List<Fixup> render(Dimension dimension, Rectangle area, Rectangle exportedArea, MinecraftWorld world, Platform platform) {
        final int floodLevel = layer.getFloodLevel(), biome = (layer.getTunnelBiome() != null) ? layer.getTunnelBiome() : -1;
        final int minHeight = dimension.getMinHeight(), minZ = minHeight + (dimension.isBottomless() ? 0 : 1), maxZ = dimension.getMaxHeight() - 1;
        final boolean removeWater = layer.isRemoveWater(), floodWithLava = layer.isFloodWithLava();
        final boolean set3DBiomes = platform.capabilities.contains(BIOMES_3D) && (layer.getTunnelBiome() != null);
        final boolean setNamedBiomes = platform.capabilities.contains(NAMED_BIOMES) && (layer.getTunnelBiome() != null);
        final MixedMaterial floorMaterial = layer.getFloorMaterial(), wallMaterial = layer.getWallMaterial(), roofMaterial = layer.getRoofMaterial();
        final String[] customBiomeNames = new String[256];
        if (setNamedBiomes && (dimension.getCustomBiomes() != null)) {
            dimension.getCustomBiomes().forEach(customBiome -> customBiomeNames[customBiome.getId()] = customBiome.getName());
        }
        if (floorNoise != null) {
            floorNoise.setSeed(dimension.getSeed());
        }
        if (roofNoise != null) {
            roofNoise.setSeed(dimension.getSeed());
        }
        if ((floorMaterial != null) || (wallMaterial != null) || (roofMaterial != null)) {
            // First pass:  place floor, wall and roof materials
            visitChunksForLayerInAreaForEditing(world, layer, area, dimension, (tile, chunkX, chunkZ, chunkSupplier) ->
                whereTunnelIsRealisedDo(dimension, tile, chunkX, chunkZ, chunkSupplier, (chunk, x, y, xInTile, yInTile, terrainHeight, actualFloorLevel, floorLedgeHeight, actualRoofLevel, roofLedgeHeight) -> {
                    int waterLevel = tile.getWaterLevel(xInTile, yInTile);
                    boolean flooded = waterLevel > terrainHeight;
                    final int startZ = Math.min(removeWater ? Math.max(terrainHeight, waterLevel) : terrainHeight, actualRoofLevel);
                    for (int z = startZ; z > actualFloorLevel; z--) {
                        if ((floorLedgeHeight == 0) && (floorMaterial != null)) {
                            setIfSolid(world, chunk, x, y, z - 1, minZ, maxZ, floorMaterial, flooded, terrainHeight, waterLevel, removeWater);
                        }
                        if (wallMaterial != null) {
                            if (floorLedgeHeight > 0) {
                                setIfSolid(world, chunk, x, y, z - 1, minZ, maxZ, wallMaterial, flooded, terrainHeight, waterLevel, removeWater);
                            }
                            if (roofLedgeHeight > 0) {
                                setIfSolid(world, chunk, x, y, z + 1, minZ, maxZ, wallMaterial, flooded, terrainHeight, waterLevel, removeWater);
                            }
                        }
                        if ((roofLedgeHeight == 0) && (roofMaterial != null)) {
                            setIfSolid(world, chunk, x, y, z + 1, minZ, maxZ, roofMaterial, flooded, terrainHeight, waterLevel, removeWater);
                        }
                    }
                    if (wallMaterial != null) {
                        terrainHeight = dimension.getIntHeightAt(x - 1, y);
                        waterLevel = dimension.getWaterLevelAt(x - 1, y);
                        flooded = waterLevel > terrainHeight;
                        for (int z = Math.min(removeWater ? Math.max(terrainHeight, waterLevel) : terrainHeight, actualRoofLevel); z > actualFloorLevel; z--) {
                            setIfSolid(world, chunk, x - 1, y, z, minZ, maxZ, wallMaterial, flooded, terrainHeight, waterLevel, removeWater);
                        }
                        terrainHeight = dimension.getIntHeightAt(x, y - 1);
                        waterLevel = dimension.getWaterLevelAt(x, y - 1);
                        flooded = waterLevel > terrainHeight;
                        for (int z = Math.min(removeWater ? Math.max(terrainHeight, waterLevel) : terrainHeight, actualRoofLevel); z > actualFloorLevel; z--) {
                            setIfSolid(world, chunk, x, y - 1, z, minZ, maxZ, wallMaterial, flooded, terrainHeight, waterLevel, removeWater);
                        }
                        terrainHeight = dimension.getIntHeightAt(x + 1, y);
                        waterLevel = dimension.getWaterLevelAt(x + 1, y);
                        flooded = waterLevel > terrainHeight;
                        for (int z = Math.min(removeWater ? Math.max(terrainHeight, waterLevel) : terrainHeight, actualRoofLevel); z > actualFloorLevel; z--) {
                            setIfSolid(world, chunk, x + 1, y, z, minZ, maxZ, wallMaterial, flooded, terrainHeight, waterLevel, removeWater);
                        }
                        terrainHeight = dimension.getIntHeightAt(x, y + 1);
                        waterLevel = dimension.getWaterLevelAt(x, y + 1);
                        flooded = waterLevel > terrainHeight;
                        for (int z = Math.min(removeWater ? Math.max(terrainHeight, waterLevel) : terrainHeight, actualRoofLevel); z > actualFloorLevel; z--) {
                            setIfSolid(world, chunk, x, y + 1, z, minZ, maxZ, wallMaterial, flooded, terrainHeight, waterLevel, removeWater);
                        }
                    }
                    return true;
                }));
        }

        // First/second pass: excavate interior
        visitChunksForLayerInAreaForEditing(world, layer, area, dimension, (tile, chunkX, chunkZ, chunkSupplier) ->
            whereTunnelIsRealisedDo(dimension, tile, chunkX, chunkZ, chunkSupplier, (chunk, x, y, xInTile, yInTile, terrainHeight, actualFloorLevel, floorLedgeHeight, actualRoofLevel, roofLedgeHeight) -> {
                final int waterLevel = tile.getWaterLevel(xInTile, yInTile);
                for (int z1 = Math.min(removeWater ? Math.max(terrainHeight, waterLevel) : terrainHeight, actualRoofLevel); z1 > actualFloorLevel; z1--) {
                    if (removeWater || (z1 <= terrainHeight) || (z1 > waterLevel)) {
                        if (z1 <= floodLevel) {
                            chunk.setMaterial(x & 0xf, z1, y & 0xf, floodWithLava ? Material.LAVA : Material.WATER);
                        } else {
                            chunk.setMaterial(x & 0xf, z1, y & 0xf, Material.AIR);
                        }
                        // Since the biomes are stored in 4x4x4 blocks this way of doing it results in doing it
                        // 63 too many times, but it's simplest, and ensures that any of those blocks touched is
                        // changed:
                        if (set3DBiomes) {
                            chunk.set3DBiome((x & 0xf) >> 2, z1 >> 2, (y & 0xf) >> 2, biome);
                        }
                        if (setNamedBiomes) {
                            chunk.setNamedBiome((x & 0xf) >> 2, z1 >> 2, (y & 0xf) >> 2, MODERN_IDS[biome] != null ? MODERN_IDS[biome] : customBiomeNames[biome]);
                        }
                    }
                }
                if (actualFloorLevel == minHeight) {
                    // Bottomless world, and cave extends all the way to
                    // the bottom. Remove the floor block, as that is
                    // probably what the user wants
                    if (floodLevel > minHeight) {
                        chunk.setMaterial(x & 0xf, minHeight, y & 0xf, floodWithLava ? Material.STATIONARY_LAVA : Material.STATIONARY_WATER);
                    } else {
                        chunk.setMaterial(x & 0xf, minHeight, y & 0xf, Material.AIR);
                    }
                }
                return true;
            }));

        // Second/third pass: render floor layers
        // TODO make TunnelFloorDimension much more intelligent, so that "distance to edge" can work and the normal
        //  exporting can be used rather than the incidental exporting
        List<Fixup> fixups = new ArrayList<>();
        final Map<Layer, TunnelLayer.LayerSettings> floorLayers = layer.getFloorLayers();
        if ((floorLayers != null) && (! floorLayers.isEmpty())) {
            final IncidentalLayerExporter[] floorExporters = new IncidentalLayerExporter[floorLayers.size()];
            final TunnelLayer.LayerSettings[] floorLayerSettings = new TunnelLayer.LayerSettings[floorLayers.size()];
            final NoiseHeightMap[] floorLayerNoise = new NoiseHeightMap[floorLayers.size()];
            int index = 0;
            for (Layer floorLayer: floorLayers.keySet()) {
                floorExporters[index] = (IncidentalLayerExporter) floorLayer.getExporter();
                TunnelLayer.LayerSettings layerSettings = floorLayers.get(floorLayer);
                floorLayerSettings[index] = layerSettings;
                if (layerSettings.getVariation() != null) {
                    floorLayerNoise[index] = new NoiseHeightMap(layerSettings.getVariation(), index);
                    floorLayerNoise[index].setSeed(dimension.getSeed());
                }
                index++;
            }
            final TunnelFloorDimension floorDimension = new TunnelFloorDimension(dimension, layer);
            visitChunksForLayerInAreaForEditing(world, layer, area, dimension, (tile, chunkX, chunkZ, chunkSupplier) ->
                whereTunnelIsRealisedDo(dimension, tile, chunkX, chunkZ, chunkSupplier, (chunk, x, y, xInTile, yInTile, terrainHeight, actualFloorLevel, floorLedgeHeight, actualRoofLevel, roofLedgeHeight) -> {
                    final int z = actualFloorLevel + 1;
                    final Point3i location = new Point3i(x, y, z);
                    for (int i = 0; i < floorExporters.length; i++) {
                        if ((z >= floorLayerSettings[i].getMinLevel()) && (z <= floorLayerSettings[i].getMaxLevel())) {
                            final int intensity = floorLayerNoise[i] != null
                                    ? MathUtils.clamp(0, (int) (floorLayerSettings[i].getIntensity() + floorLayerNoise[i].getValue(x, y, z) + 0.5f), 100)
                                    : floorLayerSettings[i].getIntensity();
                            if (intensity > 0) {
                                Fixup fixup = floorExporters[i].apply(floorDimension, location, intensity, exportedArea, world, platform);
                                if (fixup != null) {
                                    fixups.add(fixup);
                                }
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

    public BufferedImage generatePreview(int width, int height, int waterLevel, int minHeight, int baseHeight, int heightDifference) {
        final TunnelLayer.Mode floorMode = layer.getFloorMode(), roofMode = layer.getRoofMode();
        final int floorWallDepth = layer.getFloorWallDepth(), roofWallDepth = layer.getRoofWallDepth(),
                floorLevel = layer.getFloorLevel(), roofLevel = layer.getRoofLevel(), tunnelExtent = width - 24,
                floorMin = layer.getFloorMin(), floorMax = layer.getFloorMax(), roofMin = layer.getRoofMin(), roofMax = layer.getRoofMax(),
                floodLevel = layer.getFloodLevel();
        final boolean removeWater = layer.isRemoveWater(), floodWithLava = layer.isFloodWithLava();
        final PerlinNoise noise = new PerlinNoise(0);
        final BufferedImage preview = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            // Clear the sky
            final int terrainHeight = MathUtils.clamp(minHeight, (int) (Math.sin((x / (double) width * 1.5 + 1.25) * Math.PI) * heightDifference / 2 + heightDifference / 2 + baseHeight + noise.getPerlinNoise(x / 20.0) * 32 + noise.getPerlinNoise(x / 10.0) * 16 + noise.getPerlinNoise(x / 5.0) * 8), baseHeight + heightDifference - 1);
            for (int z = height - 1 + minHeight; z > terrainHeight; z--) {
                preview.setRGB(x, height - 1 - z + minHeight, (z <= waterLevel) ? 0x0000ff : 0xffffff);
            }

            if (x <= tunnelExtent) {
                // Draw the tunnel
                int actualFloorLevel = calculateLevel(floorMode, floorLevel, terrainHeight, floorMin, floorMax, minHeight + 1, height - 1, (floorNoise != null) ? ((int) floorNoise.getHeight(x, 0) - floorNoiseOffset) : 0);
                int actualRoofLevel = calculateLevel(roofMode, roofLevel, terrainHeight, roofMin, roofMax, minHeight + 1, height - 1, (roofNoise != null) ? ((int) roofNoise.getHeight(x, 0) - roofNoiseOffset) : 0);
                if (actualRoofLevel > actualFloorLevel) {
                    final float distanceToWall = tunnelExtent - x;
                    final int floorLedgeHeight = calculateLedgeHeight(floorWallDepth, distanceToWall);
                    final int roofLedgeHeight = calculateLedgeHeight(roofWallDepth, distanceToWall);
                    actualFloorLevel += floorLedgeHeight;
                    actualRoofLevel = Math.min(actualRoofLevel - roofLedgeHeight, Math.max(terrainHeight, 62));
                    for (int z = actualFloorLevel + 1; z <= actualRoofLevel; z++) {
                        if (z <= floodLevel) {
                            if (floodWithLava) {
                                preview.setRGB(x, height - 1 - z + minHeight, 0xff8000);
                            } else {
                                preview.setRGB(x, height - 1 - z + minHeight, 0x0000ff);
                            }
                        } else {
                            if (z > terrainHeight) {
                                if (removeWater) {
                                    preview.setRGB(x, height - 1 - z + minHeight, 0x7f7fff);
                                }
                            } else {
                                preview.setRGB(x, height - 1 - z + minHeight, 0x7f7f7f);
                            }
                        }
                    }
                }
            }
        }
        return preview;
    }

    static int calculateLevel(TunnelLayer.Mode mode, int level, int terrainHeight, int minLevel, int maxLevel, int minZ, int maxZ, int offset) {
        switch (mode) {
            case CONSTANT_DEPTH:
                return MathUtils.clamp(minZ, MathUtils.clamp(minLevel, terrainHeight - level, maxLevel) + offset, maxZ);
            case FIXED_HEIGHT:
                return MathUtils.clamp(minZ, level + offset, maxZ);
            case INVERTED_DEPTH:
                return MathUtils.clamp(minZ, MathUtils.clamp(minLevel, level - (terrainHeight - level), maxLevel) + offset, maxZ);
            default:
                throw new InternalError();
        }
    }

    static int calculateLedgeHeight(int wallDepth, float distanceToWall) {
        if (distanceToWall > wallDepth) {
            return 0;
        } else {
            final float a = wallDepth - distanceToWall;
            return (int) (wallDepth - Math.sqrt(wallDepth * wallDepth - a * a) + 0.5);
        }
    }

    @FunctionalInterface interface ColumnVisitor {
        boolean visitColumn(Chunk chunk, int x, int y, int xInTile, int yInTile, int terrainHeight, int actualFloorLevel, int floorLedgeHeight, int actualRoofLevel, int roofLedgeHeight);
    }

    private boolean whereTunnelIsRealisedDo(Dimension dimension, Tile tile, int chunkX, int chunkZ, Supplier<Chunk> chunkSupplier, ColumnVisitor visitor) {
        final TunnelLayer.Mode floorMode = layer.getFloorMode(), roofMode = layer.getRoofMode();
        final int floorWallDepth = layer.getFloorWallDepth(), roofWallDepth = layer.getRoofWallDepth(),
                floorLevel = layer.getFloorLevel(), roofLevel = layer.getRoofLevel(),
                maxWallDepth = Math.max(floorWallDepth, roofWallDepth) + 1,
                floorMin = layer.getFloorMin(), floorMax = layer.getFloorMax(), roofMin = layer.getRoofMin(),
                roofMax = layer.getRoofMax();
        final int minHeight = dimension.getMinHeight(), minZ = minHeight + (dimension.isBottomless() ? 0 : 1), maxZ = dimension.getMaxHeight() - 1;
        Chunk chunk = null;
        for (int xInChunk = 0; xInChunk < 16; xInChunk++) {
            for (int zInChunk = 0; zInChunk < 16; zInChunk++) {
                final int x = (chunkX << 4) | xInChunk, y = (chunkZ << 4) | zInChunk;
                final int xInTile = x & TILE_SIZE_MASK, yInTile = y & TILE_SIZE_MASK;
                if (tile.getBitLayerValue(layer, xInTile, yInTile)) {
                    int terrainHeight = tile.getIntHeight(xInTile, yInTile);
                    int actualFloorLevel = calculateLevel(floorMode, floorLevel, terrainHeight, floorMin, floorMax, minZ, maxZ, (floorNoise != null) ? ((int) floorNoise.getHeight(x, y) - floorNoiseOffset) : 0);
                    int actualRoofLevel = calculateLevel(roofMode, roofLevel, terrainHeight, roofMin, roofMax, minZ, maxZ, (roofNoise != null) ? ((int) roofNoise.getHeight(x, y) - roofNoiseOffset) : 0);
                    if (actualRoofLevel <= actualFloorLevel) {
                        continue;
                    }
                    final float distanceToWall = dimension.getDistanceToEdge(layer, x, y, maxWallDepth) - 1;
                    final int floorLedgeHeight = calculateLedgeHeight(floorWallDepth, distanceToWall);
                    final int roofLedgeHeight = calculateLedgeHeight(roofWallDepth, distanceToWall);
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
     * Set a block on the specified world or chunk, if the existing block at that location is solid and the coordinates
     * match a set of conditions.
     *
     * <p>If the specified coordinates lie on the specified {@link Chunk}, block will be set directly on it; otherwise
     * it will be set via the {@link MinecraftWorld}.
     */
    private void setIfSolid(MinecraftWorld world, Chunk chunk, int x, int y, int z, int minZ, int maxZ, MixedMaterial material, boolean flooded, int terrainHeight, int waterLevel, boolean removeWater) {
        if ((z >= minZ) && (z <= maxZ)) {
            if (removeWater || (! flooded) || (z <= terrainHeight) || (z > waterLevel)) {
                if (((x >> 4) == chunk.getxPos()) && ((y >> 4) == chunk.getzPos())) {
                    final Material existingBlock = chunk.getMaterial(x & 0xf, z, y & 0xf);
                    if ((existingBlock != Material.AIR) && (!existingBlock.insubstantial)) {
                        // The coordinates are within bounds and the existing block is solid
                        chunk.setMaterial(x & 0xf, z, y & 0xf, material.getMaterial(MATERIAL_SEED, x, y, z));
                    }
                } else {
                    final Material existingBlock = world.getMaterialAt(x, y, z);
                    if ((existingBlock != Material.AIR) && (! existingBlock.insubstantial)) {
                        // The coordinates are within bounds and the existing block is solid
                        world.setMaterialAt(x, y, z, material.getMaterial(MATERIAL_SEED, x, y, z));
                    }
                }
            }
        }
    }

    private final NoiseHeightMap floorNoise, roofNoise;
    private final int floorNoiseOffset, roofNoiseOffset;
//    private final PerlinNoise wallNoise;

    static final long FLOOR_NOISE_SEED_OFFSET = 177766561L;
    static final long ROOF_NOISE_SEED_OFFSET = 184818453L;

    private static final long MATERIAL_SEED = 0x688b2af137c77e0cL;
}