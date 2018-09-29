/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.threedeeview;

import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.biomeschemes.CustomBiomeManager;
import org.pepsoft.worldpainter.layers.*;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE;

/**
 *
 * @author pepijn
 */
// TODO: adapt for new dynamic maximum level height
public class Tile3DRenderer {
    public Tile3DRenderer(Dimension dimension, ColourScheme colourScheme, BiomeScheme biomeScheme, CustomBiomeManager customBiomeManager, int rotation) {
        this.dimension = dimension;
        maxHeight = dimension.getMaxHeight();
        this.colourScheme = colourScheme;
        this.rotation = rotation;
        tileRenderer = new TileRenderer(dimension, colourScheme, biomeScheme, customBiomeManager, 0);
        tileRenderer.addHiddenLayers(DEFAULT_HIDDEN_LAYERS);
        tileRenderer.setContourLines(false);
    }
    
    public BufferedImage render(Tile tile) {
//        System.out.println("Rendering tile " + tile);
        tileRenderer.renderTile(tile, tileImgBuffer, 0, 0);
//        Terrain subSurfaceMaterial = dimension.getSubsurfaceMaterial();
        final long seed = dimension.getSeed();
        final boolean coverSteepTerrain = dimension.isCoverSteepTerrain(), topLayersRelativeToTerrain = dimension.getTopLayerAnchor() == Dimension.LayerAnchor.TERRAIN;
        final int tileOffsetX = tile.getX() * TILE_SIZE, tileOffsetY = tile.getY() * TILE_SIZE;
        int currentColour = -1;
        final int imgWidth = TILE_SIZE * 2;
        final int imgHeight = TILE_SIZE + maxHeight - 1;
        final int maxZ = maxHeight - 1;
        final BufferedImage img = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().createCompatibleImage(imgWidth, imgHeight, Transparency.TRANSLUCENT);
        final Graphics2D g2 = img.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            for (int x = 0; x < TILE_SIZE; x++) {
                for (int y = 0; y < TILE_SIZE; y++) {
                    // Coordinates of the block in the world
                    final int xInTile, yInTile;
                    switch (rotation) {
                        case 0:
                            xInTile = x;
                            yInTile = y;
                            break;
                        case 1:
                            xInTile = y;
                            yInTile = TILE_SIZE - 1 - x;
                            break;
                        case 2:
                            xInTile = TILE_SIZE - 1 - x;
                            yInTile = TILE_SIZE - 1 - y;
                            break;
                        case 3:
                            xInTile = TILE_SIZE - 1 - y;
                            yInTile = x;
                            break;
                        default:
                            throw new IllegalArgumentException();
                    }
                    if (tile.getBitLayerValue(org.pepsoft.worldpainter.layers.Void.INSTANCE, xInTile, yInTile)
                            || tile.getBitLayerValue(NotPresent.INSTANCE, xInTile, yInTile)) {
                        continue;
                    }
                    final int blockX = tileOffsetX + xInTile, blockY = tileOffsetY + yInTile;
                    final int terrainHeight = tile.getIntHeight(xInTile, yInTile);
                    final int fluidLevel = tile.getWaterLevel(xInTile, yInTile);
                    final boolean floodWithLava;
                    if (fluidLevel > terrainHeight) {
                        floodWithLava = tile.getBitLayerValue(FloodWithLava.INSTANCE, xInTile, yInTile);
                    } else {
                        floodWithLava = false;
                    }
                    // Image coordinates
                    final float imgX = TILE_SIZE + x - y - 0.5f, imgY = (x + y) / 2f + maxHeight - 0.5f;
//                    System.out.println(blockX + ", " + blockY + " -> " + blockXTranslated + ", " + blockYTranslated + " -> " + imgX + ", " + imgY);
                    int subsurfaceHeight = Math.max(terrainHeight - dimension.getTopLayerDepth(blockX, blockY, terrainHeight), 0);
                    if (coverSteepTerrain) {
                        subsurfaceHeight = Math.min(subsurfaceHeight,
                            Math.min(Math.min(dimension.getIntHeightAt(blockX - 1, blockY, Integer.MAX_VALUE),
                            dimension.getIntHeightAt(blockX + 1, blockY, Integer.MAX_VALUE)),
                            Math.min(dimension.getIntHeightAt(blockX, blockY - 1, Integer.MAX_VALUE),
                            dimension.getIntHeightAt(blockX, blockY + 1, Integer.MAX_VALUE))));
                    }
                    int colour = colourScheme.getColour(BLK_STONE);
                    if (colour != currentColour) {
                        g2.setColor(new Color(colour));
                        currentColour = colour;
                    }
                    if (subsurfaceHeight > 0) {
                        g2.fill(new Rectangle2D.Float(imgX, imgY - subsurfaceHeight, 2, subsurfaceHeight));
                    }
//                    for (int z = 0; z <= subsurfaceHeight; z++) {
//                        colour = colourScheme.getColour(subSurfaceMaterial.getMaterial(seed, blockX, blockY, z, terrainHeight));
//                        if (colour != currentColour) {
//    //                                        g2.setColor(new Color(ColourUtils.multiply(colour, brightenAmount)));
//                            g2.setColor(new Color(colour));
//                            currentColour = colour;
//                        }
//                        g2.draw(new Line2D.Float(imgX, imgY - z, imgX + 1, imgY - z));
//                    }
                    // Do this per block because they might have different
                    // colours
                    final Terrain terrain = tile.getTerrain(xInTile, yInTile);
                    final MixedMaterial mixedMaterial;
                    final int topLayerOffset;
                    if (terrain.isCustom()) {
                        mixedMaterial = Terrain.getCustomMaterial(terrain.getCustomTerrainIndex());
                        if (topLayersRelativeToTerrain && (mixedMaterial.getMode() == MixedMaterial.Mode.LAYERED)) {
                            topLayerOffset = -(terrainHeight - mixedMaterial.getPatternHeight() + 1);
                        } else {
                            topLayerOffset = 0;
                        }
                    } else {
                        mixedMaterial = null;
                        topLayerOffset = 0;
                    }
                    if ((mixedMaterial != null) && (mixedMaterial.getColour() != null)) {
                        // A custom terrain with a configured colour; use that
                        // colour throughout
                        colour = mixedMaterial.getColour();
                        if (colour != currentColour) {
                            g2.setColor(new Color(colour));
                            currentColour = colour;
                        }
                        g2.fill(new Rectangle2D.Float(imgX, imgY - terrainHeight + 1, 2, terrainHeight - subsurfaceHeight - 1));
                    } else {
                        Material nextMaterial = terrain.getMaterial(seed, blockX, blockY, subsurfaceHeight + 1, terrainHeight);
                        for (int z = subsurfaceHeight + 1; z <= terrainHeight - 1; z++) {
                            Material material = nextMaterial;
                            if (z < maxZ) {
                                nextMaterial = terrain.getMaterial(seed, blockX, blockY, z + 1, terrainHeight);
                                if (! nextMaterial.veryInsubstantial) {
                                    // Block above is solid
                                    if ((material == Material.GRASS_BLOCK) || (material == Material.MYCELIUM) || (material == Material.FARMLAND)) {
                                        material = Material.DIRT;
                                    }
                                }
                            }
                            if (topLayerOffset != 0) {
                                // This means the terrain is a custom terrain with a
                                // layered material
                                colour = colourScheme.getColour(mixedMaterial.getMaterial(seed, blockX, blockY, z + topLayerOffset));
                            } else {
                                colour = colourScheme.getColour(material);
                            }
                            if (colour != currentColour) {
//                                g2.setColor(new Color(ColourUtils.multiply(colour, brightenAmount)));
                                g2.setColor(new Color(colour));
                                currentColour = colour;
                            }
                            g2.draw(new Line2D.Float(imgX, imgY - z, imgX + 1, imgY - z));
                        }
                    }
                    colour = tileImgBuffer.getRGB(xInTile, yInTile);
                    if (colour != currentColour) {
                        g2.setColor(new Color(colour));
                        currentColour = colour;
                    }
                    g2.draw(new Line2D.Float(imgX, imgY - terrainHeight, imgX + 1, imgY - terrainHeight));
                    if (fluidLevel > terrainHeight) {
                        colour = colourScheme.getColour(floodWithLava ? BLK_LAVA : BLK_WATER);
    //                                    currentColour = 0x80000000 | ColourUtils.multiply(colour, brightenAmount);
                        currentColour = 0x80000000 | colour;
                        g2.setColor(new Color(currentColour, true));
                        boolean ice = (! floodWithLava) && tile.getBitLayerValue(Frost.INSTANCE, xInTile, yInTile);
                        for (int z = terrainHeight + 1; z <= fluidLevel; z++) {
                            if ((z == fluidLevel) && ice) {
                                colour = colourScheme.getColour(BLK_ICE);
                                g2.setColor(new Color(colour));
                                currentColour = colour;
                            }
                            g2.draw(new Line2D.Float(imgX, imgY - z, imgX + 1, imgY - z));
                        }
                    }
                }
            }
        } finally {
            g2.dispose();
        }
        return img;
    }
 
    private final Dimension dimension;
    private final ColourScheme colourScheme;
    private final TileRenderer tileRenderer;
    private final int maxHeight, rotation;

    private final BufferedImage tileImgBuffer = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_RGB);

    static final Set<Layer> DEFAULT_HIDDEN_LAYERS = new HashSet<>(Arrays.asList(Biome.INSTANCE, Caverns.INSTANCE, ReadOnly.INSTANCE, Resources.INSTANCE));
}