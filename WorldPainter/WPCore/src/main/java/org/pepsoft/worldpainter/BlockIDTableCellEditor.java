/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;

import static org.pepsoft.minecraft.Block.BLOCK_TYPE_NAMES;

/**
 *
 * @author pepijn
 */
public class BlockIDTableCellEditor extends AbstractCellEditor implements TableCellEditor {
    public BlockIDTableCellEditor(boolean extendedBlockIds) {
        BLOCK_TYPES = new String[extendedBlockIds ? 4096 : 256];
        for (int i = 0; i < BLOCK_TYPES.length; i++) {
            if ((i >= BLOCK_TYPE_NAMES.length) || (BLOCK_TYPE_NAMES[i] == null)) {
                BLOCK_TYPES[i] = Integer.toString(i);
            } else {
                BLOCK_TYPES[i] = i + " " + BLOCK_TYPE_NAMES[i];
            }
        }
        comboBox = new JComboBox(BLOCK_TYPES);
    }
    
    @Override
    public Object getCellEditorValue() {
        return comboBox.getSelectedIndex();
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        comboBox.setSelectedIndex((Integer) value);
        return comboBox;
    }
    
    private final JComboBox comboBox;
    private final String[] BLOCK_TYPES;

    private static final long serialVersionUID = 1L;
}