/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter;

import javax.swing.*;
import java.awt.*;

/**
 *
 * @author Pepijn Schmitz
 */
public class MixedMaterialListCellRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value == null) {
            setText("none");
        } else if (value instanceof MixedMaterial) {
            setIcon(new ImageIcon(((MixedMaterial) value).getIcon(colourScheme)));
        }
        return this;
    }

    public ColourScheme getColourScheme() {
        return colourScheme;
    }

    public void setColourScheme(ColourScheme colourScheme) {
        this.colourScheme = colourScheme;
    }

    private ColourScheme colourScheme;
}
