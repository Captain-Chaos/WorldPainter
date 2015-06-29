/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers.renderers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.pepsoft.util.ColourUtils;
import org.pepsoft.worldpainter.BiomeScheme;
import org.pepsoft.worldpainter.ColourScheme;
import org.pepsoft.worldpainter.biomeschemes.CustomBiome;
import org.pepsoft.worldpainter.biomeschemes.CustomBiomeManager;
import org.pepsoft.worldpainter.biomeschemes.CustomBiomeManager.CustomBiomeListener;

/**
 *
 * @author pepijn
 */
public class BiomeRenderer implements ByteLayerRenderer, ColourSchemeRenderer, CustomBiomeListener {
    public BiomeRenderer(BiomeScheme biomeScheme, CustomBiomeManager customBiomeManager) {
        if (biomeScheme == null) {
            throw new NullPointerException();
        }
        this.biomeScheme = biomeScheme;
        int count = biomeScheme.getBiomeCount();
        patterns = new boolean[count][][];
        for (int i = 0; i < count; i++) {
            patterns[i] = biomeScheme.getPattern(i);
        }
        if (customBiomeManager != null) {
            List<CustomBiome> customBiomes = customBiomeManager.getCustomBiomes();
            if (customBiomes != null) {
                for (CustomBiome customBiome: customBiomes) {
                    customColours.put(customBiome.getId(), customBiome.getColour());
                }
            }
            resetColours();
            customBiomeManager.addListener(this);
        } else {
            resetColours();
        }
    }
    
    public ColourScheme getColourScheme() {
        return colourScheme;
    }
    
    @Override
    public void setColourScheme(ColourScheme colourScheme) {
        this.colourScheme = colourScheme;
        resetColours();
    }
    
    public BiomeScheme getBiomeScheme() {
        return biomeScheme;
    }

    @Override
    public int getPixelColour(int x, int y, int underlyingColour, int value) {
        if (value == 255) {
            return underlyingColour;
        } else if ((patterns != null) && (value < patterns.length) && (patterns[value] != null) && patterns[value][x & 0xF][y & 0xF]) {
            return ColourUtils.mix(underlyingColour, BLACK);
        } else {
            return ColourUtils.mix(underlyingColour, colours[value]);
        }
    }

    // CustomBiomeListener
    
    @Override
    public void customBiomeAdded(CustomBiome customBiome) {
        customColours.put(customBiome.getId(), customBiome.getColour());
        resetColours();
    }

    @Override
    public void customBiomeChanged(CustomBiome customBiome) {
        customColours.put(customBiome.getId(), customBiome.getColour());
        resetColours();
    }

    @Override
    public void customBiomeRemoved(CustomBiome customBiome) {
        customColours.remove(customBiome.getId());
        resetColours();
    }
    
    private void resetColours() {
        for (int i = 0; i < 256; i++) {
            if (biomeScheme.isBiomePresent(i) && (colourScheme != null)) {
                colours[i] = biomeScheme.getColour(i, colourScheme);
            } else if (customColours.containsKey(i)) {
                colours[i] = customColours.get(i);
            } else {
                colours[i] = ((((i & 0x02) == 0x02)
                        ? (((i & 0x01) == 0x01) ? 255 : 192)
                        : (((i & 0x01) == 0x01) ? 128 :   0)) << 16)
                    | ((((i & 0x08) == 0x08)
                        ? (((i & 0x04) == 0x04) ? 255 : 192)
                        : (((i & 0x04) == 0x04) ? 128 :   0)) << 8)
                    | (((i & 0x20) == 0x20)
                        ? (((i & 0x10) == 0x10) ? 255 : 192)
                        : (((i & 0x10) == 0x10) ? 128 :   0));
            }
        }
        
    }

    private final int[] colours = new int[256];
    private final Map<Integer, Integer> customColours = new HashMap<>();
    private final BiomeScheme biomeScheme;
    private final boolean[][][] patterns;
    private ColourScheme colourScheme;
    
    private static final int BLACK = 0;
}