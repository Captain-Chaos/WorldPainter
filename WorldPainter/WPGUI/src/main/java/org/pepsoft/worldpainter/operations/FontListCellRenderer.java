/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.operations;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author pepijn
 */
public class FontListCellRenderer extends DefaultListCellRenderer {
    public FontListCellRenderer(int size) {
        this.size = size;
    }
    
    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value != null) {
            Font font = fontCache.get(value);
            if (font == null) {
                font = new Font((String) value, Font.PLAIN, size);
                fontCache.put((String) value, font);
            }
            setFont(font);
        }
        return this;
    }
    
    private final Map<String, Font> fontCache = new HashMap<>();
    private final int size;
}