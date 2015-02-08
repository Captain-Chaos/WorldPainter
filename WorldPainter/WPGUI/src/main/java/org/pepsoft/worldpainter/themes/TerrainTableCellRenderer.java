/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.themes;

import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import org.pepsoft.worldpainter.ColourScheme;
import org.pepsoft.worldpainter.Terrain;

/**
 *
 * @author pepijn
 */
public class TerrainTableCellRenderer extends DefaultTableCellRenderer {
    public TerrainTableCellRenderer(ColourScheme colourScheme) {
        helper = new TerrainCellRendererHelper(colourScheme);
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        helper.configure(this, (Terrain) value);
        return this;
    }
    
    private final TerrainCellRendererHelper helper;
    
    private static final long serialVersionUID = 1L;
}