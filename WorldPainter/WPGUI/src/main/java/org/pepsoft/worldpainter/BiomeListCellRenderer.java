/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

import org.pepsoft.worldpainter.biomeschemes.BiomeHelper;
import org.pepsoft.worldpainter.biomeschemes.CustomBiomeManager;

import javax.swing.*;
import java.awt.*;

import static org.pepsoft.worldpainter.Platform.Capability.NAMED_BIOMES;

/**
 *
 * @author pepijn
 */
public class BiomeListCellRenderer extends DefaultListCellRenderer {
    public BiomeListCellRenderer(ColourScheme colourScheme, CustomBiomeManager customBiomeManager, Platform platform) {
        this(colourScheme, customBiomeManager, " ", platform);
    }
    
    public BiomeListCellRenderer(ColourScheme colourScheme, CustomBiomeManager customBiomeManager, String nullLabel, Platform platform) {
        this.nullLabel = nullLabel;
        this.showIds = ! platform.capabilities.contains(NAMED_BIOMES);
        biomeHelper = new BiomeHelper(colourScheme, customBiomeManager, platform);
    }
    
    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value == null) {
            setText(nullLabel);
        } else if (value instanceof Integer) {
            int biome = (Integer) value;
            if (biome == -1) {
                setText(nullLabel);
            } else {
                setText(showIds ? value + " " + biomeHelper.getBiomeNameWithoutId(biome) : biomeHelper.getBiomeNameWithoutId(biome));
                setIcon(biomeHelper.getBiomeIcon(biome));
            }
        }
        return this;
    }
 
    private final BiomeHelper biomeHelper;
    private final String nullLabel;
    private final boolean showIds;
    
    private static final long serialVersionUID = 1L;
}