/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.biomeschemes;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.pepsoft.util.MathUtils;

/**
 *
 * @author pepijn
 */
public class Maps2MinecraftBiomeScheme extends AbstractMinecraft1_1BiomeScheme {
    public Maps2MinecraftBiomeScheme(File imageFile, int zoomFactor) throws IOException {
        if (zoomFactor < 2) {
            throw new IllegalArgumentException("zoomFactor " + zoomFactor + " < 2");
        }
        image = ImageIO.read(imageFile);
        imageWidth = image.getWidth();
        imageHeight = image.getHeight();
        this.zoomFactor = MathUtils.pow(2, zoomFactor) * 16;
    }

    public boolean isCustom() {
        return false;
    }
    
    @Override
    public void setSeed(long seed) {
        // Do nothing
    }

    @Override
    public void getBiomes(int x, int y, int width, int height, int[] buffer) {
        for (int dx = 0; dx < width; dx++) {
            for (int dy = 0; dy < height; dy++) {
                buffer[dx + dy * width] = getBiome(x + dx, y + dy);
            }
        }
    }
    
    private int getBiome(int x, int y) {
        int pixelX = (Math.abs(x) / zoomFactor) % imageWidth, pixelY = (Math.abs(y) / zoomFactor) % imageHeight;
        int colour = image.getRGB(pixelX, pixelY) & 0xFFFFFF; // Strip alpha
        switch (colour) {
            case 0x207905:
                return BIOME_FOREST;
            case 0x0400FF:
                return BIOME_OCEAN;
            case 0xEDE8FF:
                return BIOME_ICE_MOUNTAINS;
            case 0x95BFE7:
                return BIOME_FROZEN_OCEAN;
            case 0x0FD59F:
                return BIOME_TAIGA;
            case 0x0E3D00:
                return BIOME_SWAMPLAND;
            case 0x929D8E:
                return BIOME_EXTREME_HILLS; // "Mountains", according to mod
            case 0xFCFF00:
                return BIOME_DESERT;
            case 0xF000FF:
                return BIOME_MUSHROOM_ISLAND;
            case 0x06FF00:
                return BIOME_PLAINS;
            case 0xCFFFDB:
                // Fall through
            default:
                return BIOME_ICE_PLAINS; // "Tundra", according to mod
        }
    }
    
    private final BufferedImage image;
    private final int zoomFactor, imageWidth, imageHeight;
}