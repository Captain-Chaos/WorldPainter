package org.pepsoft.worldpainter.layers.tunnel;

import org.pepsoft.minecraft.Chunk;
import org.pepsoft.minecraft.Material;
import org.pepsoft.util.PerlinNoise;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.MixedMaterial;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.exporting.*;
import org.pepsoft.worldpainter.layers.Biome;
import org.pepsoft.worldpainter.layers.FloodWithLava;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.Void;
import org.pepsoft.worldpainter.util.BiomeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;

import static org.pepsoft.minecraft.Constants.DEFAULT_WATER_LEVEL;
import static org.pepsoft.minecraft.Material.*;
import static org.pepsoft.util.MathUtils.clamp;
import static org.pepsoft.worldpainter.Constants.*;
import static org.pepsoft.worldpainter.Platform.Capability.*;
import static org.pepsoft.worldpainter.biomeschemes.Minecraft1_7Biomes.*;
import static org.pepsoft.worldpainter.exporting.SecondPassLayerExporter.Stage.ADD_FEATURES;
import static org.pepsoft.worldpainter.exporting.SecondPassLayerExporter.Stage.CARVE;
import static org.pepsoft.worldpainter.layers.tunnel.TunnelLayer.LayerMode.CAVE;
import static org.pepsoft.worldpainter.layers.tunnel.TunnelLayer.Mode.CUSTOM_DIMENSION;

public class TunnelLayerExporter extends AbstractTunnelLayerExporter implements SecondPassLayerExporter {
    public TunnelLayerExporter(Dimension dimension, Platform platform, TunnelLayer layer, TunnelLayerHelper helper) {
        super(dimension, platform, layer, helper);
    }

    @Override
    public Set<Stage> getStages() {
        return EnumSet.of(CARVE, ADD_FEATURES);
    }

    @Override
    public List<Fixup> carve(Rectangle area, Rectangle exportedArea, MinecraftWorld world) {
        final List<Fixup> fixups = new ArrayList<>();
        final int floodLevel = layer.getFloodLevel(), biome = (layer.getTunnelBiome() != null) ? layer.getTunnelBiome() : -1, defaultBiome;
        final Dimension.Anchor anchor = dimension.getAnchor();
        switch (anchor.dim) {
            case DIM_NORMAL:
                defaultBiome = BIOME_PLAINS;
                break;
            case DIM_NETHER:
                defaultBiome = BIOME_HELL;
                break;
            case DIM_END:
                defaultBiome = BIOME_SKY;
                break;
            default:
                throw new InternalError();
        }
        final boolean removeWater = layer.isRemoveWater(), floodWithLava = layer.isFloodWithLava();
        final Dimension floorDimension = ((layer.getFloorMode() == CUSTOM_DIMENSION) && (layer.getFloorDimensionId() != null))
                ? dimension.getWorld().getDimension(new Dimension.Anchor(anchor.dim, Dimension.Role.CAVE_FLOOR, anchor.invert, layer.getFloorDimensionId()))
                : null;
        final boolean customDimension = floorDimension != null;
        final boolean set3DBiomes = (platform.capabilities.contains(BIOMES_3D) || platform.capabilities.contains(NAMED_BIOMES)) && (customDimension || (layer.getTunnelBiome() != null));
        final BiomeUtils biomeUtils = new BiomeUtils(customDimension ? floorDimension : dimension);
        final MixedMaterial floorMaterial = (! customDimension) ? layer.getFloorMaterial() : null, wallMaterial = layer.getWallMaterial(), roofMaterial = layer.getRoofMaterial();
        // TODO: increase efficiency and correctness by doing this _after_ the carving and then only for blocks which
        //  adjoin a solid block
        if ((floorMaterial != null) || (wallMaterial != null) || (roofMaterial != null)) {
            // First pass:  place floor, wall and roof materials
            visitChunksForLayerInAreaForEditing(world, layer, area, dimension, (tile, chunkX, chunkZ, chunkSupplier) ->
                whereTunnelIsRealisedDo(tile, chunkX, chunkZ, chunkSupplier, (chunk, x, y, xInTile, yInTile, terrainHeight, actualFloorLevel, floorLedgeHeight, actualRoofLevel, roofLedgeHeight) -> {
                    if (dimension.getBitLayerValueAt(Void.INSTANCE, x, y)) {
                        return true;
                    }
                    int waterLevel = tile.getWaterLevel(xInTile, yInTile);
                    boolean flooded = waterLevel > terrainHeight;
                    final int startZ = Math.min(removeWater ? Math.max(terrainHeight, waterLevel) : terrainHeight, actualRoofLevel);
                    for (int z = startZ; z > actualFloorLevel; z--) {
                        if ((floorLedgeHeight == 0) && (floorMaterial != null)) {
                            setIfSolid(world, chunk, x, y, z - 1, minZ, maxZ, floorMaterial.getMaterial(MATERIAL_SEED, x, y, z - 1), flooded, terrainHeight, waterLevel, removeWater);
                        }
                        if (wallMaterial != null) {
                            if (floorLedgeHeight > 0) {
                                setIfSolid(world, chunk, x, y, z - 1, minZ, maxZ, wallMaterial.getMaterial(MATERIAL_SEED, x, y, z - 1), flooded, terrainHeight, waterLevel, removeWater);
                            }
                            if (roofLedgeHeight > 0) {
                                setIfSolid(world, chunk, x, y, z + 1, minZ, maxZ, wallMaterial.getMaterial(MATERIAL_SEED, x, y, z + 1), flooded, terrainHeight, waterLevel, removeWater);
                            }
                        }
                        if ((roofLedgeHeight == 0) && (roofMaterial != null)) {
                            setIfSolid(world, chunk, x, y, z + 1, minZ, maxZ, roofMaterial.getMaterial(MATERIAL_SEED, x, y, z + 1), flooded, terrainHeight, waterLevel, removeWater);
                        }
                    }
                    if (wallMaterial != null) {
                        terrainHeight = dimension.getIntHeightAt(x - 1, y);
                        waterLevel = dimension.getWaterLevelAt(x - 1, y);
                        flooded = waterLevel > terrainHeight;
                        for (int z = Math.min(removeWater ? Math.max(terrainHeight, waterLevel) : terrainHeight, actualRoofLevel); z > actualFloorLevel; z--) {
                            setIfSolid(world, chunk, x - 1, y, z, minZ, maxZ, wallMaterial.getMaterial(MATERIAL_SEED, x - 1, y, z), flooded, terrainHeight, waterLevel, removeWater);
                        }
                        terrainHeight = dimension.getIntHeightAt(x, y - 1);
                        waterLevel = dimension.getWaterLevelAt(x, y - 1);
                        flooded = waterLevel > terrainHeight;
                        for (int z = Math.min(removeWater ? Math.max(terrainHeight, waterLevel) : terrainHeight, actualRoofLevel); z > actualFloorLevel; z--) {
                            setIfSolid(world, chunk, x, y - 1, z, minZ, maxZ, wallMaterial.getMaterial(MATERIAL_SEED, x, y - 1, z), flooded, terrainHeight, waterLevel, removeWater);
                        }
                        terrainHeight = dimension.getIntHeightAt(x + 1, y);
                        waterLevel = dimension.getWaterLevelAt(x + 1, y);
                        flooded = waterLevel > terrainHeight;
                        for (int z = Math.min(removeWater ? Math.max(terrainHeight, waterLevel) : terrainHeight, actualRoofLevel); z > actualFloorLevel; z--) {
                            setIfSolid(world, chunk, x + 1, y, z, minZ, maxZ, wallMaterial.getMaterial(MATERIAL_SEED, x + 1, y, z), flooded, terrainHeight, waterLevel, removeWater);
                        }
                        terrainHeight = dimension.getIntHeightAt(x, y + 1);
                        waterLevel = dimension.getWaterLevelAt(x, y + 1);
                        flooded = waterLevel > terrainHeight;
                        for (int z = Math.min(removeWater ? Math.max(terrainHeight, waterLevel) : terrainHeight, actualRoofLevel); z > actualFloorLevel; z--) {
                            setIfSolid(world, chunk, x, y + 1, z, minZ, maxZ, wallMaterial.getMaterial(MATERIAL_SEED, x, y + 1, z), flooded, terrainHeight, waterLevel, removeWater);
                        }
                    }
                    return true;
                }));
        }

        // First/second pass: excavate interior
        final long seed = dimension.getSeed();
        final Material singleFillMaterial;
        final MixedMaterial complexFillMaterial;
        switch (layer.getFillMode()) {
            case CAVE_AIR:
                if (platform.capabilities.contains(NAME_BASED)) {
                    singleFillMaterial = CAVE_AIR;
                } else {
                    // This is to cover the case where someone has created the layer on a platform that does support
                    // cave_air and then changed the platform to one which doesn't
                    logger.warn("Platform does not support cave_air; using air instead");
                    singleFillMaterial = AIR;
                }
                complexFillMaterial = null;
                break;
            case AIR:
                singleFillMaterial = AIR;
                complexFillMaterial = null;
                break;
            case LIGHT:
                singleFillMaterial = LIGHT.withProperty(LEVEL, 8);
                complexFillMaterial = null;
                break;
            case MIXED_MATERIAL:
                singleFillMaterial = null;
                complexFillMaterial = layer.getFillMaterial();
                break;
            default:
                throw new IllegalArgumentException("Unsupported fill mode: " + layer.getFillMode());
        }
        visitChunksForLayerInAreaForEditing(world, layer, area, dimension, (tile, chunkX, chunkZ, chunkSupplier) ->
            whereTunnelIsRealisedDo(tile, chunkX, chunkZ, chunkSupplier, (chunk, x, y, xInTile, yInTile, terrainHeight, actualFloorLevel, floorLedgeHeight, actualRoofLevel, roofLedgeHeight) -> {
                if (dimension.getBitLayerValueAt(Void.INSTANCE, x, y)) {
                    return true;
                }
                final int waterLevel = tile.getWaterLevel(xInTile, yInTile);
                final int myFloodLevel;
                final boolean myFloodWithLava;
                final int myBiome;
                if (customDimension) {
                    myFloodLevel = floorDimension.getWaterLevelAt(x, y);
                    myFloodWithLava = floorDimension.getBitLayerValueAt(FloodWithLava.INSTANCE, x, y);
                    if (set3DBiomes) {
                        final int layerValue = floorDimension.getLayerValueAt(Biome.INSTANCE, x, y);
                        if (layerValue == 255) {
                            myBiome = floorDimension.getAutoBiome(x, y, defaultBiome);
                        } else {
                            myBiome = layerValue;
                        }
                    } else {
                        myBiome = -1;
                    }
                } else {
                    myFloodLevel = floodLevel;
                    myFloodWithLava = floodWithLava;
                    myBiome = biome;
                }
                for (int z = Math.min(removeWater ? Math.max(terrainHeight, waterLevel) : terrainHeight, actualRoofLevel); z > actualFloorLevel; z--) {
                    if (removeWater || (z <= terrainHeight) || (z > waterLevel)) {
                        if (z <= myFloodLevel) {
                            // TODO should this be moved to the ADD_FEATURES stage?
                            chunk.setMaterial(x & 0xf, z, y & 0xf, myFloodWithLava ? Material.LAVA : Material.WATER);
                        } else {
                            chunk.setMaterial(x & 0xf, z, y & 0xf, (singleFillMaterial != null) ? singleFillMaterial : complexFillMaterial.getMaterial(seed, x, y, z));
                        }
                        // Since the biomes are stored in 4x4x4 blocks this way of doing it results in doing it 63 too
                        // many times, but it's simplest, and ensures that any of those blocks touched is changed:
                        if (set3DBiomes) {
                            biomeUtils.set3DBiome(chunk, (x & 0xf) >> 2, z >> 2, (y & 0xf) >> 2, myBiome);
                        }
                    }
                }
                if (actualFloorLevel == minHeight) {
                    // Bottomless world, and cave extends all the way to the bottom. Remove the floor block, as that is
                    // probably what the user wants
                    if (myFloodLevel > minHeight) {
                        chunk.setMaterial(x & 0xf, minHeight, y & 0xf, myFloodWithLava ? Material.STATIONARY_LAVA : Material.STATIONARY_WATER);
                    } else {
                        chunk.setMaterial(x & 0xf, minHeight, y & 0xf, (singleFillMaterial != null) ? singleFillMaterial : complexFillMaterial.getMaterial(seed, x, y, minHeight));
                    }
                }
                return true;
            }));

        // Carve floor dimension
        if (customDimension) {
            // TODO add support for combined layers
            final List<Layer> floorLayers = new ArrayList<>(floorDimension.getAllLayers(false));
            floorLayers.addAll(floorDimension.getMinimumLayers());
            Collections.sort(floorLayers);
            final List<FirstPassLayerExporter> firstPassExporters = new ArrayList<>();
            final List<SecondPassLayerExporter> secondPassExporters = new ArrayList<>();
            floorLayers.stream()
                    .filter(layer -> ! SKIP_LAYERS.contains(layer))
                    .sorted()
                    .forEach(layer -> {
                        final Class<? extends LayerExporter> exporterType = layer.getExporterType();
                        if (exporterType != null) {
                            if (FirstPassLayerExporter.class.isAssignableFrom(exporterType) || SecondPassLayerExporter.class.isAssignableFrom(exporterType)) {
                                final LayerExporter exporter = layer.getExporter(floorDimension, platform, floorDimension.getLayerSettings(layer));
                                if (exporter instanceof FirstPassLayerExporter) {
                                    firstPassExporters.add((FirstPassLayerExporter) exporter);
                                }
                                if (exporter instanceof SecondPassLayerExporter) {
                                    if (((SecondPassLayerExporter) exporter).getStages().contains(CARVE)) {
                                        secondPassExporters.add((SecondPassLayerExporter) exporter);
                                    }
                                }
                            } else {
                                logger.debug("Skipping layer {} for stage CARVE while processing TunnelLayer floor dimension", layer.getName());
                            }
                        } else if (! (layer instanceof Biome)) {
                            throw new UnsupportedOperationException("Layer " + layer.getName() + " of type " + layer.getClass().getSimpleName() + " not yet supported for cave floor dimensions");
                        }
                    });
            final WorldPainterChunkFactory chunkFactory = new WorldPainterChunkFactory(floorDimension, null, platform, maxHeight);
            visitChunksForLayerInAreaForEditing(world, layer, area, dimension, (tile, chunkX, chunkZ, chunkSupplier) -> {
                final Tile floorTile = floorDimension.getTile(tile.getX(), tile.getY());
                whereTunnelIsRealisedDo(tile, chunkX, chunkZ, chunkSupplier, (chunk, x, y, xInTile, yInTile, terrainHeight, actualFloorLevel, floorLedgeHeight, actualRoofLevel, roofLedgeHeight) -> {
                    if (dimension.getBitLayerValueAt(Void.INSTANCE, x, y)) {
                        // TODO apply void
                    } else {
                        chunkFactory.applyTopLayer(floorTile, chunk, xInTile, yInTile, minHeight, true);
                    }
                    return true;
                });
                // TODO this will actually go outside the TunnelLayer boundaries:
                if (! firstPassExporters.isEmpty()) {
                    final Chunk chunk = chunkSupplier.get();
                    for (FirstPassLayerExporter exporter: firstPassExporters) {
                        exporter.render(floorTile, chunk);
                    }
                }
                return true;
            });
            for (SecondPassLayerExporter exporter: secondPassExporters) {
                final List<Fixup> layerFixups = exporter.carve(area, exportedArea, world);
                if (layerFixups != null) {
                    fixups.addAll(layerFixups);
                }
            }
        }

        return fixups.isEmpty() ? null : fixups;
    }

    public static BufferedImage generatePreview(TunnelLayer layer, int width, int height, int waterLevel, int minHeight, int baseHeight, int heightDifference) {
        if (layer.getLayerMode() != CAVE) {
            throw new IllegalArgumentException("Layer must be in mode CAVE");
        }
        final TunnelLayer.Mode floorMode = layer.getFloorMode();
        final int tunnelExtent = width - 34, floodLevel = layer.getFloodLevel();
        final boolean removeWater = layer.isRemoveWater(), floodWithLava = layer.isFloodWithLava();
        final PerlinNoise noise = new PerlinNoise(0);
        final BufferedImage preview = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        final TunnelLayerHelper helper = layer.getHelper(null);
        for (int x = 0; x < width; x++) {
            // Clear the sky
            final int terrainHeight = clamp(minHeight, (int) (Math.sin((x / (double) width * 1.5 + 1.25) * Math.PI) * heightDifference / 2 + heightDifference / 2 + baseHeight + noise.getPerlinNoise(x / 20.0) * 32 + noise.getPerlinNoise(x / 10.0) * 16 + noise.getPerlinNoise(x / 5.0) * 8), baseHeight + heightDifference - 1);
            for (int z = height - 1 + minHeight; z > terrainHeight; z--) {
                preview.setRGB(x, height - 1 - z + minHeight, (z <= waterLevel) ? 0x0000ff : 0xffffff);
            }

            if (x <= tunnelExtent) {
                // Draw the tunnel
                if (floorMode == CUSTOM_DIMENSION) {
                    int actualFloorLevel = helper.calculateFloorLevel(x, 0, terrainHeight, minHeight + 1, height - 1);
                    int actualRoofLevel = helper.calculateRoofLevel(x, 0, terrainHeight, minHeight + 1, height - 1, actualFloorLevel);
                    final int roofLedgeHeight = helper.calculateTopLedgeHeight(tunnelExtent - x);
                    actualRoofLevel = Math.min(actualRoofLevel - roofLedgeHeight, Math.max(terrainHeight, DEFAULT_WATER_LEVEL));
                    int tint = 0x7f;
                    for (int z = actualRoofLevel; z >= minHeight; z--) {
                        if (z <= floodLevel) {
                            if (floodWithLava) {
                                preview.setRGB(x, height - 1 - z + minHeight, 0xff8000);
                            } else {
                                preview.setRGB(x, height - 1 - z + minHeight, 0x0000ff);
                            }
                        } else {
                            if (z > terrainHeight) {
                                if (removeWater) {
                                    preview.setRGB(x, height - 1 - z + minHeight, (tint << 16) | (tint << 8) | 0xff);
                                }
                            } else {
                                preview.setRGB(x, height - 1 - z + minHeight, (tint << 16) | (tint << 8) | tint);
                            }
                        }
                        if (tint > 0) {
                            tint = Math.max(tint - 2, 0);
                        }
                    }
                } else {
                    int actualFloorLevel = helper.calculateFloorLevel(x, 0, terrainHeight, minHeight + 1, height - 1);
                    int actualRoofLevel = helper.calculateRoofLevel(x, 0, terrainHeight, minHeight + 1, height - 1, actualFloorLevel);
                    if (actualRoofLevel > actualFloorLevel) {
                        final float distanceToWall = tunnelExtent - x;
                        final int floorLedgeHeight = helper.calculateBottomLedgeHeight(distanceToWall);
                        final int roofLedgeHeight = helper.calculateTopLedgeHeight(distanceToWall);
                        actualFloorLevel += floorLedgeHeight;
                        actualRoofLevel = Math.min(actualRoofLevel - roofLedgeHeight, Math.max(terrainHeight, DEFAULT_WATER_LEVEL));
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
        }
        // Add height markers
        final Graphics2D g2 = preview.createGraphics();
        try {
            g2.setColor(Color.GRAY);
            g2.setFont(HEIGHT_MARKER_FONT);
            for (int y = (minHeight / 20) * 20; y < (height + minHeight); y += 20) {
                g2.drawLine(width - 10, height + minHeight - y, width - 1, height + minHeight - y);
                g2.drawString(Integer.toString(y), width - 30, height + minHeight - y + 4);
            }
        } finally {
            g2.dispose();
        }
        return preview;
    }

    private static final long MATERIAL_SEED = 0x688b2af137c77e0cL;

    private static final Logger logger = LoggerFactory.getLogger(TunnelLayerExporter.class);
}