/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.biomeschemes;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.pepsoft.worldpainter.BiomeScheme;
import org.pepsoft.worldpainter.ColourScheme;
import org.pepsoft.worldpainter.operations.BiomePaint;

/**
 * A helper class for determining biome names and icons
 * 
 * @author pepijn
 */
public class BiomeHelper {
    public BiomeHelper(BiomeScheme biomeScheme, ColourScheme colourScheme, CustomBiomeManager customBiomeManager) {
        this.biomeScheme = (biomeScheme != null) ? biomeScheme : new AutoBiomeScheme(null);
        this.colourScheme = colourScheme;
        this.customBiomeManager = customBiomeManager;
    }
    
    public String getBiomeName(int biomeID) {
        if (biomeID == 255) {
            return "Auto";
        } else if (names[biomeID] == null) {
            if (biomeScheme.isBiomePresent(biomeID)) {
                names[biomeID] = biomeScheme.getBiomeName(biomeID);
            } else if (customBiomeManager.getCustomBiomes() != null) {
                for (CustomBiome customBiome: customBiomeManager.getCustomBiomes()) {
                    if (customBiome.getId() == biomeID) {
                        names[biomeID] = customBiome.getName();
                        break;
                    }
                }
            }
            if (names[biomeID] == null) {
                names[biomeID] = "Biome " + biomeID;
            }
        }
        return names[biomeID];
    }
    
    public Icon getBiomeIcon(int biomeID) {
        if (icons[biomeID] == null) {
            if (biomeScheme.isBiomePresent(biomeID)) {
                icons[biomeID] = new ImageIcon(BiomeSchemeManager.createImage(biomeScheme, biomeID, colourScheme));
            } else if (customBiomeManager.getCustomBiomes() != null) {
                for (CustomBiome customBiome: customBiomeManager.getCustomBiomes()) {
                    if (customBiome.getId() == biomeID) {
                        icons[biomeID] = new ImageIcon(BiomePaint.createIcon(customBiome.getColour()));
                        break;
                    }
                }
            }
        }
        return icons[biomeID];
    }
 
    private final BiomeScheme biomeScheme;
    private final CustomBiomeManager customBiomeManager;
    private final ColourScheme colourScheme;
    private final String[] names = new String[256];
    private final Icon[] icons = new Icon[256];
}