/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.themes;

import java.awt.Component;
import javax.swing.AbstractCellEditor;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerModel;
import javax.swing.table.TableCellEditor;

/**
 *
 * @author pepijn
 */
public class JSpinnerTableCellEditor extends AbstractCellEditor implements TableCellEditor {
    public JSpinnerTableCellEditor(SpinnerModel spinnerModel) {
        spinner.setModel(spinnerModel);
    }
    
    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        spinner.setValue(value);
        return spinner;
    }
    
    @Override
    public Object getCellEditorValue() {
        return spinner.getValue();
    }
    
    private final JSpinner spinner = new JSpinner();
    
    private static final long serialVersionUID = 1L;
}