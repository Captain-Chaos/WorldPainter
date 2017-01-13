/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.importing;

import org.pepsoft.util.IconUtils;
import org.pepsoft.worldpainter.ColourScheme;
import org.pepsoft.worldpainter.MixedMaterial;
import org.pepsoft.worldpainter.biomeschemes.CustomBiome;
import org.pepsoft.worldpainter.layers.CustomLayer;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 *
 * @author Pepijn Schmitz
 */
public class CustomItemsTreeCellRenderer extends DefaultTreeCellRenderer {
    public CustomItemsTreeCellRenderer(ColourScheme colourScheme) {
        this.colourScheme = colourScheme;
    }
    
    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        if ((value != null) && (! (value instanceof String))) {
            Icon icon = icons.get(value);
            String label = labels.get(value);
            if (icon == null) {
                if (value instanceof CustomLayer) {
                    icon = new ImageIcon(((CustomLayer) value).getIcon());
                    label = ((CustomLayer) value).getName();
                } else if (value instanceof MixedMaterial) {
                    icon = new ImageIcon(((MixedMaterial) value).getIcon(colourScheme));
                    label = ((MixedMaterial) value).getName();
                } else if (value instanceof CustomBiome) {
                    icon = IconUtils.createScaledColourIcon(((CustomBiome) value).getColour());
                    label = ((CustomBiome) value).getName();
                }
                icons.put(value, icon);
                labels.put(value, label);
            }
            setIcon(icon);
            setText(label);
        }
        return this;
    }
 
    private final ColourScheme colourScheme;
    private final Map<Object, Icon> icons = new IdentityHashMap<>();
    private final Map<Object, String> labels = new IdentityHashMap<>();
}