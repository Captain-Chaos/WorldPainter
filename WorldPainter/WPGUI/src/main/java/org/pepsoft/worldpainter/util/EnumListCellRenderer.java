package org.pepsoft.worldpainter.util;

import javax.swing.*;
import java.awt.*;

public class EnumListCellRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof Enum) {
            setText(I18nHelper.m((Enum) value));
        }
        return this;
    }
}
