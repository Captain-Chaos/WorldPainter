/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers.renderers;

import org.pepsoft.util.ColourUtils;
import org.pepsoft.worldpainter.ColourScheme;
import org.pepsoft.worldpainter.biomeschemes.CustomBiome;
import org.pepsoft.worldpainter.biomeschemes.CustomBiomeManager;
import org.pepsoft.worldpainter.biomeschemes.CustomBiomeManager.CustomBiomeListener;
import org.pepsoft.worldpainter.biomeschemes.StaticBiomeInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author pepijn
 */
public class BiomeRenderer implements ByteLayerRenderer, ColourSchemeRenderer, CustomBiomeListener {
    public BiomeRenderer(CustomBiomeManager customBiomeManager) {
        int count = StaticBiomeInfo.INSTANCE.getBiomeCount();
        patterns = new boolean[count][][];
        for (int i = 0; i < count; i++) {
            patterns[i] = StaticBiomeInfo.INSTANCE.getPattern(i);
        }
        if (customBiomeManager != null) {
            List<CustomBiome> customBiomes = customBiomeManager.getCustomBiomes();
            if (customBiomes != null) {
                for (CustomBiome customBiome: customBiomes) {
                    customColours.put(customBiome.getId(), customBiome.getColour());
                    custom[customBiome.getId()] = true;
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
    
    @Override
    public int getPixelColour(int x, int y, int underlyingColour, int value) {
        if (value == 255) {
            return underlyingColour;
        } else if ((! custom[value]) && (patterns != null) && (value < patterns.length) && (patterns[value] != null) && patterns[value][x & 0xF][y & 0xF]) {
            return ColourUtils.mix(underlyingColour, BLACK);
        } else {
            return ColourUtils.mix(underlyingColour, colours[value]);
        }
    }

    // CustomBiomeListener
    
    @Override
    public void customBiomeAdded(CustomBiome customBiome) {
        customColours.put(customBiome.getId(), customBiome.getColour());
        custom[customBiome.getId()] = true;
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
        custom[customBiome.getId()] = false;
        resetColours();
    }
    
    private void resetColours() {
        for (int i = 0; i < 256; i++) {
            if (custom[i]) {
                colours[i] = customColours.get(i);
            } else if (StaticBiomeInfo.INSTANCE.isBiomePresent(i) && (colourScheme != null)) {
                colours[i] = StaticBiomeInfo.INSTANCE.getColour(i, colourScheme);
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
    private final boolean[] custom = new boolean[256];
    private final Map<Integer, Integer> customColours = new HashMap<>();
    private final boolean[][][] patterns;
    private ColourScheme colourScheme;
    
    private static final int BLACK = 0;
}