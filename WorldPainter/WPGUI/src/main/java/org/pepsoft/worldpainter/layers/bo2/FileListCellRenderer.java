/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers.bo2;

import java.awt.Color;
import java.awt.Component;
import java.io.File;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

/**
 *
 * @author pepijn
 */
public class FileListCellRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof File) {
            if (! ((File) value).exists()) {
                setForeground(Color.RED);
            } else {
                setForeground(list.getForeground());
            }
        }
        return this;
    }
    
    private static final long serialVersionUID = 1L;
}