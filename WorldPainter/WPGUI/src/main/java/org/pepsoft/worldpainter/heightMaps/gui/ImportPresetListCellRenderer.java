package org.pepsoft.worldpainter.heightMaps.gui;

import org.pepsoft.worldpainter.heightMaps.ImportPreset;

import javax.swing.*;
import java.awt.*;

public class ImportPresetListCellRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value == null) {
            setText("-");
        } else {
            setText(((ImportPreset) value).getDescription());
        }
        return this;
    }
}