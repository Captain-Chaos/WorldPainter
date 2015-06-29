/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.operations;

import java.awt.Component;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.HashMap;
import java.util.Map;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

/**
 *
 * @author pepijn
 */
public class FontListCellRenderer extends DefaultListCellRenderer {
    public FontListCellRenderer(int size) {
        // Dirty hack to find default font size:
        for (String fontFamilyName: GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            fontCache.put(fontFamilyName, new Font(fontFamilyName, Font.PLAIN, size));
        }
    }
    
    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (fontCache.containsKey(value)) {
            setFont(fontCache.get(value));
        }
        return this;
    }
    
    private final Map<String, Font> fontCache = new HashMap<>();
}