/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers.renderers;

import org.pepsoft.util.ColourUtils;
import org.pepsoft.worldpainter.BiomeScheme;
import org.pepsoft.worldpainter.ColourScheme;
import org.pepsoft.worldpainter.biomeschemes.CustomBiome;
import org.pepsoft.worldpainter.biomeschemes.CustomBiomeManager;
import org.pepsoft.worldpainter.biomeschemes.CustomBiomeManager.CustomBiomeListener;
import org.pepsoft.worldpainter.biomeschemes.StaticBiomeInfo;

import java.awt.image.BufferedImage;
import java.util.List;

import static java.awt.image.BufferedImage.TYPE_INT_RGB;

/**
 *
 * @author pepijn
 */
public class BiomeRenderer implements ByteLayerRenderer, CustomBiomeListener {
    public BiomeRenderer(CustomBiomeManager customBiomeManager, ColourScheme colourScheme) {
        patterns = new BufferedImage[255];
        for (int i = 0; i < 255; i++) {
            if (BIOME_INFO.isBiomePresent(i)) {
                patterns[i] = createPattern(i, colourScheme);
            }
        }
        if (customBiomeManager != null) {
            final List<CustomBiome> customBiomes = customBiomeManager.getCustomBiomes();
            for (CustomBiome customBiome: customBiomes) {
                final int id = customBiome.getId();
                if (patterns[id] == null) {
                    final BufferedImage pattern = customBiome.getPattern();
                    if (pattern != null) {
                        patterns[id] = pattern;
                    }
                } else {
                    patterns[id] = createPattern(customBiome.getColour());
                }
            }
            customBiomeManager.addListener(this);
        }
    }
    
    @Override
    public int getPixelColour(int x, int y, int underlyingColour, int value) {
        if ((value == 255) || (patterns[value] == null)) {
            return underlyingColour;
        } else {
            return ColourUtils.mix(underlyingColour, patterns[value].getRGB(x & 0xf, y & 0xf));
        }
    }

    // CustomBiomeListener
    
    @Override
    public void customBiomeAdded(CustomBiome customBiome) {
        customBiomeChanged(customBiome);
    }

    @Override
    public void customBiomeChanged(CustomBiome customBiome) {
        final int id = customBiome.getId();
        if (! BIOME_INFO.isBiomePresent(id)) {
            final BufferedImage pattern = customBiome.getPattern();
            if (pattern != null) {
                patterns[id] = pattern;
            } else {
                patterns[id] = createPattern(customBiome.getColour());
            }
        }
    }

    @Override
    public void customBiomeRemoved(CustomBiome customBiome) {
        final int id = customBiome.getId();
        if (! BIOME_INFO.isBiomePresent(id)) {
            patterns[id] = null;
        }
    }

    private BufferedImage createPattern(int biomeId, ColourScheme colourScheme) {
        final boolean[][] pattern = BIOME_INFO.getPattern(biomeId);
        final int colour = BIOME_INFO.getColour(biomeId, colourScheme);
        final BufferedImage image = new BufferedImage(16, 16, TYPE_INT_RGB);
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                if ((pattern != null) && pattern[x][y]) {
                    image.setRGB(x, y, BLACK);
                } else {
                    image.setRGB(x, y, colour);
                }
            }
        }
        return image;
    }

    private BufferedImage createPattern(int colour) {
        final BufferedImage image = new BufferedImage(16, 16, TYPE_INT_RGB);
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                image.setRGB(x, y, colour);
            }
        }
        return image;
    }

    private final BufferedImage[] patterns;

    private static final int BLACK = 0;
    private static final BiomeScheme BIOME_INFO = StaticBiomeInfo.INSTANCE;
}