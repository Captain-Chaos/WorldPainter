/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import org.pepsoft.worldpainter.layers.CustomLayer;

/**
 *
 * @author pepijn
 */
public class CustomLayersTableModel implements TableModel {
    public CustomLayersTableModel(Dimension dimension, Set<CustomLayer> allCustomLayers) {
        this.dimension = dimension;
        customLayers = new ArrayList<>(allCustomLayers);
        Collections.sort(customLayers);
    }

    /**
     * Swap two layers.
     * 
     * @param rowIndex1 The index of the first layer.
     * @param rowIndex2 The index of the second layer.
     */
    public void swap(int rowIndex1, int rowIndex2) {
        CustomLayer layer = customLayers.get(rowIndex1);
        customLayers.set(rowIndex1, customLayers.get(rowIndex2));
        customLayers.set(rowIndex2, layer);
        pristine = false;
        TableModelEvent event = new TableModelEvent(this, Math.min(rowIndex1, rowIndex2), Math.max(rowIndex1, rowIndex2));
        for (TableModelListener listener: listeners) {
            listener.tableChanged(event);
        }
    }

    public boolean isPristine() {
        return pristine;
    }

    /**
     * Save the current order to the indices of the custom layers.
     */
    public void save() {
        int index = 0;
        for (CustomLayer layer: customLayers) {
            layer.setIndex(index++);
        }
    }
    
    // TableModel
    
    @Override
    public int getRowCount() {
        return customLayers.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return COLUMN_NAMES[columnIndex];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return COLUMN_TYPES[columnIndex];
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == COLUMN_EXPORT;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case COLUMN_LAYER:
                return customLayers.get(rowIndex);
            case COLUMN_EXPORT:
                return customLayers.get(rowIndex).isExport();
            default:
                throw new IndexOutOfBoundsException("columnIndex " + columnIndex);
        }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (columnIndex != COLUMN_EXPORT) {
            throw new IllegalArgumentException("columnIndex " + columnIndex);
        }
        customLayers.get(rowIndex).setExport((Boolean) aValue);
    }

    @Override
    public void addTableModelListener(TableModelListener l) {
        listeners.add(l);
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
        listeners.remove(l);
    }
    
    private final Dimension dimension;
    private final List<CustomLayer> customLayers;
    private final List<TableModelListener> listeners = new ArrayList<>();
    private boolean pristine = true;
 
    private static final String[]   COLUMN_NAMES = {"Layer",      "Export"};
    private static final Class<?>[] COLUMN_TYPES = {String.class, Boolean.class};
    private static final int COLUMN_LAYER  = 0;
    private static final int COLUMN_EXPORT = 1;
}