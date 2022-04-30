package org.pepsoft.worldpainter;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

import static java.awt.Color.GRAY;

public class NoiseSettingsTableCellRenderer extends DefaultTableCellRenderer {
    public NoiseSettingsTableCellRenderer() {
        setHorizontalAlignment(TRAILING);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (value instanceof NoiseSettings) {
            if (! isSelected) {
                setForeground(table.getForeground());
            }
            NoiseSettings noiseSettings = (NoiseSettings) value;
            setText(noiseSettings.getRange() + ", " + Math.round(noiseSettings.getScale() * 100) + ", " + noiseSettings.getRoughness());
        } else {
            if (! isSelected) {
                setForeground(GRAY);
            }
            setText("<html><i>no variation</i></html>");
        }
        return this;
    }
}