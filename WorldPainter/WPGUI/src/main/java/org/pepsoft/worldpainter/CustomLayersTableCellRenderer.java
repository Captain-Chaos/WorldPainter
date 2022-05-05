package org.pepsoft.worldpainter;

import org.pepsoft.worldpainter.layers.CustomLayer;
import org.pepsoft.worldpainter.layers.LayerTableCellRenderer;

import javax.swing.*;
import java.awt.*;

import static java.awt.Font.BOLD;
import static java.awt.Font.ITALIC;

/**
 * Created by pepijn on 6-2-16.
 */
public class CustomLayersTableCellRenderer extends LayerTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (((CustomLayersTableModel) table.getModel()).isHeader((CustomLayer) value)) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setIcon(null);
            if (headerFont == null) {
                headerFont = getFont().deriveFont(BOLD | ITALIC);
            }
            setFont(headerFont);
            return this;
        } else {
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }

    private Font headerFont;
}
