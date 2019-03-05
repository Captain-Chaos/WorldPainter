package org.pepsoft.worldpainter;

import javax.swing.*;
import java.awt.*;

public class PlatformListCellRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof Platform) {
            setText(((Platform) value).displayName);
        }
        return this;
    }
}
