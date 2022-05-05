package org.pepsoft.worldpainter;

import org.pepsoft.minecraft.Material;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

import static org.pepsoft.worldpainter.DefaultPlugin.*;

public class MaterialTableCellRenderer extends DefaultTableCellRenderer {
    public MaterialTableCellRenderer(Platform platform) {
        this.platform = platform;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (value instanceof Material) {
            if (! isSelected) {
                setForeground(Color.BLUE);
            }
            if ((platform == JAVA_ANVIL_1_15) || (platform == JAVA_ANVIL_1_17) || (platform == JAVA_ANVIL_1_18) /* TODO make dynamic */) {
                setText("<html><u>" + value + "</u></html>");
            } else {
                setText("<html><u>" + ((Material) value).toLegacyString() + "</u></html>");
            }
        }
        return this;
    }

    private final Platform platform;
}