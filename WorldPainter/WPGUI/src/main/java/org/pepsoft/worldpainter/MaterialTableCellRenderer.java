package org.pepsoft.worldpainter;

import org.pepsoft.minecraft.Material;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

import static org.pepsoft.worldpainter.Platform.Capability.NAME_BASED;

public class MaterialTableCellRenderer extends DefaultTableCellRenderer {
    public MaterialTableCellRenderer(Platform platform) {
        nameBased = platform.capabilities.contains(NAME_BASED);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (value instanceof Material) {
            if (! isSelected) {
                setForeground(Color.BLUE);
            }
            if (nameBased) {
                setText("<html><u>" + value + "</u></html>");
            } else {
                setText("<html><u>" + ((Material) value).toLegacyString() + "</u></html>");
            }
        }
        return this;
    }

    private final boolean nameBased;
}