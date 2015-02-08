/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.themes;

import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import org.pepsoft.worldpainter.ColourScheme;
import org.pepsoft.worldpainter.Terrain;

/**
 *
 * @author pepijn
 */
public class TerrainListCellRenderer extends DefaultListCellRenderer {
    public TerrainListCellRenderer(ColourScheme colourScheme) {
        this(colourScheme, " ");
    }

    public TerrainListCellRenderer(ColourScheme colourScheme, String nullLabel) {
        this.nullLabel = nullLabel;
        helper = new TerrainCellRendererHelper(colourScheme);
    }
    
    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof Terrain) {
            helper.configure(this, (Terrain) value);
        } else if (value == null) {
            setText(nullLabel);
        }
        return this;
    }
    
    private final String nullLabel;
    private final TerrainCellRendererHelper helper;
    
    private static final long serialVersionUID = 1L;
}