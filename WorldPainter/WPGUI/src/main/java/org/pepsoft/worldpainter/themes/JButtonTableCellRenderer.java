/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.themes;

import java.awt.Component;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/**
 *
 * @author pepijn
 */
public class JButtonTableCellRenderer extends JButton implements TableCellRenderer {
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component renderer;
        if (value != null) {
            setText(((JButton) value).getText());
            renderer = this;
        } else {
            renderer = emptyCellRenderer;
        }
        if (isSelected) {
            renderer.setBackground(table.getSelectionBackground());
        } else {
            renderer.setBackground(table.getBackground());
        }
        return renderer;
    }
    
    private static final long serialVersionUID = 1L;
    
    private final JPanel emptyCellRenderer = new JPanel();
}