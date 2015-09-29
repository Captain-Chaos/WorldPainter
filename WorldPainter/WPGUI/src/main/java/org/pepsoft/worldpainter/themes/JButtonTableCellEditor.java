/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.themes;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

/**
 *
 * @author pepijn
 */
public class JButtonTableCellEditor extends AbstractCellEditor implements ActionListener, TableCellEditor {
    public JButtonTableCellEditor(ButtonPressListener listener) {
        this.listener = listener;
        button.addActionListener(this);
    }
    
    public Object getCellEditorValue() {
        return button;
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        source = table;
        button.setText(((JButton) value).getText());
        this.row = row;
        this.column = column;
        return button;
    }

    // ActionListener
    
    public void actionPerformed(ActionEvent e) {
        if (listener != null) {
            listener.buttonPressed(source, row, column);
        }
    }

    private final JButton button = new JButton();
    private final ButtonPressListener listener;
    private int row, column;
    private JTable source;
    
    private static final long serialVersionUID = 1L;
}