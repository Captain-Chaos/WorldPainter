/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.biomeschemes;

import org.pepsoft.util.IconUtils;
import org.pepsoft.worldpainter.BiomeScheme;
import org.pepsoft.worldpainter.ColourScheme;
import org.pepsoft.worldpainter.Platform;

import javax.swing.*;

import static org.pepsoft.worldpainter.Platform.Capability.NAMED_BIOMES;
import static org.pepsoft.worldpainter.util.BiomeUtils.getBiomeScheme;

/**
 * A helper class for determining biome names and icons
 * 
 * @author pepijn
 */
public class BiomeHelper {
    public BiomeHelper(ColourScheme colourScheme, CustomBiomeManager customBiomeManager, Platform platform) {
        this.colourScheme = colourScheme;
        this.customBiomeManager = customBiomeManager;
        showIds = ! platform.capabilities.contains(NAMED_BIOMES);
        biomeScheme = getBiomeScheme(platform);
    }

    public String getBiomeName(int biomeID) {
        if (biomeID == 255) {
            return "Auto";
        } else  {
            return getBiomeNames(biomeID)[1];
        }
    }

    public String getBiomeNameWithoutId(int biomeID) {
        if (biomeID == 255) {
            return "Auto";
        } else  {
            return getBiomeNames(biomeID)[0];
        }
    }

    private String[] getBiomeNames(int biomeID) {
        if (names[biomeID][0] == null) {
            if (biomeScheme.isBiomePresent(biomeID)) {
                names[biomeID][0] = biomeScheme.getBiomeName(biomeID);
                names[biomeID][1] = showIds ? names[biomeID][0] + " (" + biomeID + ")" : names[biomeID][0];
            } else if (customBiomeManager.getCustomBiomes() != null) {
                for (CustomBiome customBiome: customBiomeManager.getCustomBiomes()) {
                    if (customBiome.getId() == biomeID) {
                        names[biomeID][0] = customBiome.getName();
                        names[biomeID][1] = showIds ? names[biomeID][0] + " (" + biomeID + ")" : names[biomeID][0];
                        break;
                    }
                }
            }
            if (names[biomeID][0] == null) {
                names[biomeID][0] = "Biome " + biomeID;
                names[biomeID][1] = names[biomeID][0];
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
                        icons[biomeID] = IconUtils.createScaledColourIcon(customBiome.getColour());
                        break;
                    }
                }
            }
        }
        return icons[biomeID];
    }
 
    private final CustomBiomeManager customBiomeManager;
    private final ColourScheme colourScheme;
    private final BiomeScheme biomeScheme;
    private final boolean showIds;
    private final String[][] names = new String[255][2];
    private final Icon[] icons = new Icon[256];
}