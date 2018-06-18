/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers.groundcover;


import org.pepsoft.minecraft.Chunk;
import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.exporting.*;
import org.pepsoft.worldpainter.heightMaps.NoiseHeightMap;

import javax.vecmath.Point3i;
import java.awt.*;

import static org.pepsoft.minecraft.Material.AIR;
import static org.pepsoft.minecraft.Material.LAYERS;

/**
 * Algorithm:
 * 
 * The layers are rendered in order from the lowest thickness to the highest
 * thickness (which implies that layers digging into the ground are rendered
 * first).
 * 
 * 1. If the layer has a negative thickness (it digs down into the ground), the
 *    blocks is always placed, except if the existing block is air
 * 2. If the layer has positive thickness, and:
 *    a. it is insubstantial (including water), the block is only placed if the
 *       existing block is insubstantial (excluding water)
 *    b. it is substantial, the block is always placed
 * 
 * @author pepijn
 */
public class GroundCoverLayerExporter extends AbstractLayerExporter<GroundCoverLayer> implements FirstPassLayerExporter, IncidentalLayerExporter {
    public GroundCoverLayerExporter(GroundCoverLayer layer) {
        super(layer);
        NoiseSettings noiseSettings = layer.getNoiseSettings();
        if (noiseSettings != null) {
            noiseHeightMap = new NoiseHeightMap(noiseSettings, NOISE_SEED_OFFSET);
            noiseOffset = noiseSettings.getRange();
        } else {
            noiseHeightMap = null;
            noiseOffset = 0;
        }
    }
    
    @Override
    public void render(Dimension dimension, Tile tile, Chunk chunk, Platform platform) {
        if (noiseHeightMap != null) {
            noiseHeightMap.setSeed(dimension.getSeed());
        }
        final int xOffset = (chunk.getxPos() & 7) << 4;
        final int zOffset = (chunk.getzPos() & 7) << 4;
        final int minY = dimension.isBottomless() ? 0 : 1;
        final int maxY = dimension.getMaxHeight() - 1;
        final MixedMaterial mixedMaterial = layer.getMaterial();
        final int thickness = layer.getThickness(), edgeThickness = Math.abs(thickness) - 2;
        final GroundCoverLayer.EdgeShape edgeShape = layer.getEdgeShape();
        final boolean taperedEdge = (edgeShape != GroundCoverLayer.EdgeShape.SHEER) && (Math.abs(thickness) > 1);
        final int edgeWidth = layer.getEdgeWidth(), edgeWidthPlusOne = edgeWidth + 1, edgeWidthMinusOne = edgeWidth - 1;
        final double edgeFactor = edgeThickness / 2.0, edgeOffset = 1.5 + edgeFactor;
        final long seed = dimension.getSeed();
        final boolean smooth = layer.isSmooth() || (mixedMaterial.getSingleMaterial() == Material.SNOW_EIGHT_LAYERS);
        final GroundCoverLayer.LayerAnchor layeredMaterialAnchor;
        final int patternHeight = mixedMaterial.getPatternHeight();
        if (mixedMaterial.getMode() == MixedMaterial.Mode.LAYERED) {
            layeredMaterialAnchor = layer.getLayerAnchor();
        } else {
            layeredMaterialAnchor = GroundCoverLayer.LayerAnchor.BEDROCK;
        }
        for (int x = 0; x < 16; x++) {
            final int localX = xOffset + x;
            final int worldX = (chunk.getxPos() << 4) + x;
            for (int z = 0; z < 16; z++) {
                final int localY = zOffset + z;
                if (tile.getBitLayerValue(layer, localX, localY)) {
                    final int terrainheight = tile.getIntHeight(localX, localY);
                    final Material blockBelow = chunk.getMaterial(x, terrainheight, z);
                    if ((blockBelow != AIR)
                            && (! blockBelow.insubstantial)) {
                        int effectiveThickness = Math.abs(thickness);
                        final int worldY = (chunk.getzPos() << 4) + z;
                        if (taperedEdge) {
                            float distanceToEdge = dimension.getDistanceToEdge(layer, worldX, worldY, edgeWidthPlusOne);
                            if (distanceToEdge < edgeWidthPlusOne) {
                                final double normalisedDistance = (distanceToEdge - 1) / edgeWidthMinusOne;
                                switch (edgeShape) {
                                    case LINEAR:
                                        effectiveThickness = (int) (1.5 + normalisedDistance * edgeThickness);
                                        break;
                                    case SMOOTH:
                                        effectiveThickness = (int) (edgeOffset + -Math.cos(normalisedDistance * Math.PI) * edgeFactor);
                                        break;
                                    case ROUNDED:
                                        double reversedNormalisedDistance = 1 - (distanceToEdge - 0.5) / edgeWidth;
                                        effectiveThickness = (int) (1.5 + Math.sqrt(1 - reversedNormalisedDistance * reversedNormalisedDistance) * edgeThickness);
                                        break;
                                }
                            }
                        }
                        if (noiseHeightMap != null) {
                            effectiveThickness += noiseHeightMap.getHeight(worldX, worldY) - noiseOffset;
                        }
                        if (thickness > 0) {
                            int yOffset;
                            switch (layeredMaterialAnchor) {
                                case BEDROCK:
                                    yOffset = 0;
                                    break;
                                case TERRAIN:
                                    yOffset = -(terrainheight + 1);
                                    break;
                                case TOP_OF_LAYER:
                                    yOffset = -(terrainheight + effectiveThickness - patternHeight + 1);
                                    break;
                                default:
                                    throw new InternalError();
                            }
                            if (smooth) {
                                float fEffectiveThickness = Math.abs(thickness);
                                if (taperedEdge) {
                                    float distanceToEdge = dimension.getDistanceToEdge(layer, worldX, worldY, edgeWidthPlusOne);
                                    if (distanceToEdge < edgeWidth) {
                                        final double normalisedDistance = distanceToEdge / edgeWidthPlusOne;
                                        switch (edgeShape) {
                                            case LINEAR:
                                                fEffectiveThickness = (float) (normalisedDistance * thickness);
                                                break;
                                            case SMOOTH:
                                                fEffectiveThickness = (float) ((-Math.cos(normalisedDistance * Math.PI) + 1) * thickness / 2);
//                                                System.out.printf("distanceToEdge: %f, normalisedDistance: %f, effectiveThickness: %f%n", distanceToEdge, normalisedDistance, fEffectiveThickness);
                                                break;
                                            case ROUNDED:
                                                // TODO is this right?
                                                double reversedNormalisedDistance = 1 - (distanceToEdge - 0.5) / edgeWidth;
                                                fEffectiveThickness = (float) (Math.sqrt(1 - reversedNormalisedDistance * reversedNormalisedDistance) * thickness);
                                                break;
                                        }
                                    }
                                }
                                if (noiseHeightMap != null) {
                                    fEffectiveThickness += noiseHeightMap.getHeight(worldX, worldY) - noiseOffset;
                                }
                                // Layer height in eights of a block
                                int layerHeight = Math.max(Math.round((dimension.getHeightAt(worldX, worldY) + fEffectiveThickness - dimension.getIntHeightAt(worldX, worldY)) / 0.125f), 1);
                                // TODO is this necessary or desired for smooth layers?
//                                if (layerHeight > 0) {
//                                    layerHeight = Math.max(Math.min(layerHeight, dimension.getBitLayerCount(layer, worldX, worldY, 1) - 2), 0);
//                                }
                                for (int dy = 0; layerHeight > 0; dy++, layerHeight -= 8) {
//                                    System.out.printf("dy: %d, layerHeight: %d; ", dy, layerHeight);
                                    final int y = terrainheight + dy + 1;
                                    if (y > maxY) {
                                        break;
                                    }
                                    final Material existingMaterial = chunk.getMaterial(x, y, z);
                                    final Material material = mixedMaterial.getMaterial(seed, worldX, worldY, y + yOffset);
                                    if ((material != AIR)
                                            && ((! material.veryInsubstantial)
                                            || (existingMaterial == AIR)
                                            || existingMaterial.insubstantial)) {
                                        if (layerHeight < 8) {
                                            // Top layer, smooth enabled
                                            chunk.setMaterial(x, y, z, material.withProperty(LAYERS, layerHeight));
                                        } else {
                                            // Place a full block
                                            chunk.setMaterial(x, y, z, material == Material.SNOW_EIGHT_LAYERS ? Material.SNOW_BLOCK : material);
                                        }
                                    }
                                }
//                                System.out.println();
                            } else {
                                for (int dy = 0; dy < effectiveThickness; dy++) {
                                    final int y = terrainheight + dy + 1;
                                    if (y > maxY) {
                                        break;
                                    }
                                    final Material existingMaterial = chunk.getMaterial(x, y, z);
                                    final Material material = mixedMaterial.getMaterial(seed, worldX, worldY, y + yOffset);
                                    if ((material != AIR)
                                            && ((! material.veryInsubstantial)
                                            || (existingMaterial == AIR)
                                            || existingMaterial.insubstantial)) {
                                        chunk.setMaterial(x, y, z, material);
                                    }
                                }
                            }
                        } else {
                            int yOffset;
                            switch (layeredMaterialAnchor) {
                                case BEDROCK:
                                    yOffset = 0;
                                    break;
                                case TERRAIN:
                                    yOffset = -(terrainheight - effectiveThickness + 1);
                                    break;
                                case TOP_OF_LAYER:
                                    yOffset = -(terrainheight - patternHeight + 1);
                                    break;
                                default:
                                    throw new InternalError();
                            }
                            for (int dy = 0; dy < effectiveThickness; dy++) {
                                final int y = terrainheight - dy;
                                if (y < minY) {
                                    break;
                                }
                                Material existingMaterial = chunk.getMaterial(x, y, z);
                                if (existingMaterial != AIR) {
                                    chunk.setMaterial(x, y, z, mixedMaterial.getMaterial(seed, worldX, worldY, y + yOffset));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // TODO: add smooth layer support here as well; will require refactoring the incidental layer framework
    @Override
    public Fixup apply(Dimension dimension, Point3i location, int intensity, Rectangle exportedArea, MinecraftWorld minecraftWorld, Platform platform) {
        if (intensity > 0) {
            final Material blockBelow = minecraftWorld.getMaterialAt(location.x, location.y, location.z - 1);
            if ((blockBelow != AIR)
                    && (! blockBelow.insubstantial)) {
                final int thickness = layer.getThickness();
                final MixedMaterial mixedMaterial = layer.getMaterial();
                final long seed = dimension.getSeed();
                int effectiveThickness = Math.abs(thickness);
                if (noiseHeightMap != null) {
                    noiseHeightMap.setSeed(seed);
                    effectiveThickness += noiseHeightMap.getHeight(location.x, location.y) - noiseOffset;
                }
                if (thickness > 0) {
                    final int maxZ = dimension.getMaxHeight() - 1;
                    for (int dz = 0; dz < effectiveThickness; dz++) {
                        final int z = location.z + dz;
                        if (z > maxZ) {
                            break;
                        }
                        final Material existingMaterial = minecraftWorld.getMaterialAt(location.x, location.y, z);
                        final Material material = mixedMaterial.getMaterial(seed, location.x, location.y, z);
                        if ((material != AIR)
                                && ((! material.veryInsubstantial)
                                    || (existingMaterial == AIR)
                                    || existingMaterial.insubstantial)) {
                            minecraftWorld.setMaterialAt(location.x, location.y, z, material);
                        }
                    }
                } else {
                    final int minZ = dimension.isBottomless() ? 0 : 1;
                    for (int dz = 0; dz < effectiveThickness; dz++) {
                        final int z = location.z - 1 - dz;
                        if (z < minZ) {
                            break;
                        }
                        Material existingMaterial = minecraftWorld.getMaterialAt(location.x, location.y, z);
                        if (existingMaterial != AIR) {
                            minecraftWorld.setMaterialAt(location.x, location.y, z, mixedMaterial.getMaterial(seed, location.x, location.y, z));
                        }
                    }
                }
            }
        }
        return null;
    }
    
    private final NoiseHeightMap noiseHeightMap;
    private final int noiseOffset;
    
    private static final long NOISE_SEED_OFFSET = 135101785L;
}