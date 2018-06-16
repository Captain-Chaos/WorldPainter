/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers.pockets;

import org.pepsoft.minecraft.Chunk;
import org.pepsoft.util.MathUtils;
import org.pepsoft.util.PerlinNoise;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.exporting.AbstractLayerExporter;
import org.pepsoft.worldpainter.exporting.FirstPassLayerExporter;
import org.pepsoft.worldpainter.layers.exporters.ExporterSettings;

import java.awt.image.BufferedImage;

import static org.pepsoft.worldpainter.Constants.TILE_SIZE_BITS;
import static org.pepsoft.worldpainter.Constants.TINY_BLOBS;

/**
 *
 * @author pepijn
 */
public class UndergroundPocketsLayerExporter extends AbstractLayerExporter<UndergroundPocketsLayer> implements FirstPassLayerExporter {
    public UndergroundPocketsLayerExporter(UndergroundPocketsLayer layer) {
        super(layer);
    }

    @Override
    public void setSettings(ExporterSettings settings) {
        super.setSettings(settings);
        init();
    }
    
    @Override
    public void render(Dimension dimension, Tile tile, Chunk chunk, Platform platform) {
        final MixedMaterial material = layer.getMaterial();
        final Terrain terrain = layer.getTerrain();
        final boolean useMaterial = material != null;
        final int minLevel = layer.getMinLevel();
        final int maxLevel = layer.getMaxLevel();
        final boolean coverSteepTerrain = dimension.isCoverSteepTerrain();
        
        final int xOffset = (chunk.getxPos() & 7) << 4;
        final int zOffset = (chunk.getzPos() & 7) << 4;
        final long seed = dimension.getSeed();
        if (noiseGenerator.getSeed() != seed + seedOffset) {
            noiseGenerator.setSeed(seed + seedOffset);
        }
        // Coordinates in chunk
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                // Coordinates in tile
                final int localX = xOffset + x, localY = zOffset + z;
                if (tile.getBitLayerValue(org.pepsoft.worldpainter.layers.Void.INSTANCE, localX, localY)) {
                    continue;
                }
                final int value = tile.getLayerValue(layer, localX, localY);
                if (value > 0) {
                    final float biasedThreshold = biasedThresholds[value - 1];
                    final int terrainheight = tile.getIntHeight(localX, localY);
                    // Coordinates in world
                    final int worldX = tile.getX() << TILE_SIZE_BITS | localX, worldY = tile.getY() << TILE_SIZE_BITS | localY;
                    final int minY = Math.max(1, minLevel);
                    int maxY = Math.min(terrainheight - dimension.getTopLayerDepth(worldX, worldY, terrainheight), maxLevel);
                    if (coverSteepTerrain) {
                        maxY = Math.min(maxY,
                            Math.min(Math.min(dimension.getIntHeightAt(worldX - 1, worldY, Integer.MAX_VALUE),
                            dimension.getIntHeightAt(worldX + 1, worldY, Integer.MAX_VALUE)),
                            Math.min(dimension.getIntHeightAt(worldX, worldY - 1, Integer.MAX_VALUE),
                            dimension.getIntHeightAt(worldX, worldY + 1, Integer.MAX_VALUE))));
                    }
                    if (biasedThreshold <= -0.5f) {
                        // Special case: replace every block
                        for (int y = maxY; y >= minY; y--) {
                            if (useMaterial) {
                                chunk.setMaterial(x, y, z, material.getMaterial(seed, worldX, worldY, y));
                            } else {
                                chunk.setMaterial(x, y, z, terrain.getMaterial(platform, seed, worldX, worldY, y, terrainheight));
                            }
                        }
                    } else {
                        for (int y = maxY; y >= minY; y--) {
                            double dx = worldX / scale, dy = worldY / scale, dz = y / scale;
                            if (noiseGenerator.getPerlinNoise(dx, dy, dz) >= biasedThreshold) {
                                if (useMaterial) {
                                    chunk.setMaterial(x, y, z, material.getMaterial(seed, worldX, worldY, y));
                                } else {
                                    chunk.setMaterial(x, y, z, terrain.getMaterial(platform, seed, worldX, worldY, y, terrainheight));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Create a black and white preview of the layer.
     */
    public BufferedImage createPreview(int width, int height) {
        init();
        final BufferedImage preview = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        // For threshold <= -0.5 the image would be all-black, which it is by
        // default anyway
        if (threshold > -0.5f) {
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < height; z++) {
                    double dx = x / scale, dz = z / scale;
                    if (noiseGenerator.getPerlinNoise(dx, 0.0, dz) < threshold) {
                        preview.setRGB(x, height - z - 1, 0xffffff);
                    }
                }
            }
        }
//        drawLabels(preview, height);
        return preview;
    }
    
    /**
     * Create a coloured preview of the layer.
     */
    public BufferedImage createPreview(int width, int height, ColourScheme colourScheme, Terrain subsurfaceMaterial) {
        init();
        final BufferedImage preview = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        final MixedMaterial material = layer.getMaterial();
        final Terrain terrain = layer.getTerrain();
        final boolean useMaterial = material != null;
        if (threshold <= -0.5f) {
            // Special case: replace every block
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < height; z++) {
                    if (useMaterial) {
                        preview.setRGB(x, height - z - 1, colourScheme.getColour(material.getMaterial(0L, x, 0, z)));
                    } else {
                        preview.setRGB(x, height - z - 1, colourScheme.getColour(terrain.getMaterial(0L, x, 0, z, height - 1)));
                    }
                }
            }
        } else {
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < height; z++) {
                    double dx = x / scale, dz = z / scale;
                    if (noiseGenerator.getPerlinNoise(dx, 0.0, dz) >= threshold) {
                        if (useMaterial) {
                            preview.setRGB(x, height - z - 1, colourScheme.getColour(material.getMaterial(0L, x, 0, z)));
                        } else {
                            preview.setRGB(x, height - z - 1, colourScheme.getColour(terrain.getMaterial(0L, x, 0, z, height - 1)));
                        }
                    } else {
                        preview.setRGB(x, height - z - 1, colourScheme.getColour(subsurfaceMaterial.getMaterial(0L, x, 0, z, height - 1)));
                    }
                }
            }
        }
//        drawLabels(preview, height);
        return preview;
    }
    
    private void init() {
        seedOffset = (layer.getMaterial() != null) ? layer.getMaterial().hashCode() : layer.getTerrain().hashCode();
        for (int i = 1; i <= 15; i++) {
            biasedThresholds[i - 1] = PerlinNoise.getLevelForPromillage(MathUtils.clamp(0f, (float) (layer.getFrequency() * Math.pow(0.9, 8 - i)), 1000f));
        }
        threshold = biasedThresholds[8];
        scale = TINY_BLOBS * (layer.getScale() / 100.0);
    }

//    private void drawLabels(final BufferedImage preview, final int height) {
//        Font font = new Font(Font.DIALOG, Font.PLAIN, 8);
//        Graphics2D g2 = preview.createGraphics();
//        try {
//            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//            g2.setColor(Color.WHITE);
//            g2.setFont(font);
//            LineMetrics lineMetrics = font.getLineMetrics("000", g2.getFontRenderContext());
//            int offset = (int) (lineMetrics.getAscent() / 2 + 0.5f);
//            for (int z = 16; z < height; z += 16) {
//                String text = Integer.toString(z);
//                for (int dx = -1; dx <= 1; dx++) {
//                    for (int dy = -1; dy <= 1; dy++) {
//                        g2.drawLine(0, height - z - 1 + dy, 3 + dx, height - z - 1 + dy);
//                        g2.drawString(text, 5 + dx, height - z - 1 + offset + dy);
//                    }
//                }
//            }
//            g2.setColor(Color.BLACK);
//            g2.setFont(font);
//            for (int z = 16; z < height; z += 16) {
//                g2.drawLine(0, height - z - 1, 3, height - z - 1);
//                g2.drawString(Integer.toString(z), 5, height - z - 1 + offset);
//            }
//        } finally {
//            g2.dispose();
//        }
//    }
    
    private final PerlinNoise noiseGenerator = new PerlinNoise(0);
    /**
     * The thresholds for all possible layer values
     */
    private final float[] biasedThresholds = new float[15];
    private long seedOffset;
    /**
     * The threshold for value 8 (50% intensity)
     */
    private float threshold;
    private double scale;
}
