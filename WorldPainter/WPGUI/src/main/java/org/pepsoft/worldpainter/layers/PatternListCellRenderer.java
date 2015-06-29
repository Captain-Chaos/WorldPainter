/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.layers;

import java.awt.Component;
import java.util.HashMap;
import java.util.Map;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JList;
import org.pepsoft.worldpainter.layers.LayerPreviewCreator.Pattern;

/**
 *
 * @author Pepijn Schmitz
 */
public class PatternListCellRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof Pattern) {
            Pattern pattern = (Pattern) value;
            Icon icon = iconCache.get(pattern);
            if (icon == null) {
                icon = new ImageIcon(pattern.getIcon());
                iconCache.put(pattern, icon);
            }
            setIcon(icon);
            setText(pattern.getDescription());
        }
        return this;
    }
    
    private final Map<Pattern, Icon> iconCache = new HashMap<>();
}