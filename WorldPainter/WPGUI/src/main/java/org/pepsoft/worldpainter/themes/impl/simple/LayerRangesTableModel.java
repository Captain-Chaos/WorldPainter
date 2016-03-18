/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.themes.impl.simple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.themes.Filter;
import org.pepsoft.worldpainter.themes.HeightFilter;

/**
 *
 * @author pepijn
 */
public class LayerRangesTableModel implements TableModel {
    public LayerRangesTableModel(int maxHeight, Map<Filter, Layer> layerMap) {
        this.maxHeight = maxHeight;
        if (layerMap != null) {
            layerMap.forEach((key, value) -> {
                filters.add(key);
                layers.add(value);
            });
        }
    }

    public Map<Filter, Layer> getLayerMap() {
        if (! layers.isEmpty()) {
            Map<Filter, Layer> layerMap = new HashMap<>();
            Iterator<Filter> filterIterator = filters.iterator();
            for (Layer layer : layers) {
                Filter filter = filterIterator.next();
                layerMap.put(filter, layer);
            }
            return layerMap;
        } else {
            return null;
        }
    }

    public void addRow(Filter filter, Layer layer) {
        filters.add(filter);
        layers.add(layer);
        int index = filters.size() - 1;
        TableModelEvent event = new TableModelEvent(this, index, index, TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT);
        listeners.forEach(listener -> listener.tableChanged(event));
    }

    public void deleteRow(int row) {
        layers.remove(row);
        filters.remove(row);
        TableModelEvent event = new TableModelEvent(this, row, row, TableModelEvent.ALL_COLUMNS, TableModelEvent.DELETE);
        listeners.forEach(listener -> listener.tableChanged(event));
    }
    
    // TableModel
    
    @Override
    public int getRowCount() {
        return filters.size();
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
        return columnIndex != COLUMN_LAYER;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case COLUMN_FROM:
                return ((HeightFilter) filters.get(rowIndex)).getStartHeight();
            case COLUMN_TO:
                return ((HeightFilter) filters.get(rowIndex)).getStopHeight();
            case COLUMN_FEATHER:
                return ((HeightFilter) filters.get(rowIndex)).isFeather();
            case COLUMN_LAYER:
                return layers.get(rowIndex);
            case COLUMN_ACTIONS:
                return deleteButton;
            default:
                throw new IndexOutOfBoundsException("columnIndex " + columnIndex);
        }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case COLUMN_FROM:
                HeightFilter old = (HeightFilter) filters.get(rowIndex);
                filters.set(rowIndex, new HeightFilter(maxHeight, (int) aValue, old.getStopHeight(), old.isFeather()));
                break;
            case COLUMN_TO:
                old = (HeightFilter) filters.get(rowIndex);
                filters.set(rowIndex, new HeightFilter(maxHeight, old.getStartHeight(), (int) aValue, old.isFeather()));
                break;
            case COLUMN_FEATHER:
                old = (HeightFilter) filters.get(rowIndex);
                filters.set(rowIndex, new HeightFilter(maxHeight, old.getStartHeight(), old.getStopHeight(), (boolean) aValue));
                break;
            case COLUMN_LAYER:
                throw new IllegalArgumentException("columnIndex " + columnIndex + " not editable");
            case COLUMN_ACTIONS:
                // Do nothing
                break;
            default:
                throw new IndexOutOfBoundsException("columnIndex " + columnIndex);
        }
    }

    @Override
    public void addTableModelListener(TableModelListener l) {
        listeners.add(l);
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
        listeners.remove(l);
    }
    
    private final List<Filter> filters = new ArrayList<>();
    private final List<Layer> layers = new ArrayList<>();
    private final int maxHeight;
    private final List<TableModelListener> listeners = new ArrayList<>();
    private final JButton deleteButton = new JButton("Delete");
    
    private static final String[] COLUMN_NAMES = {"From",        "To",          "Feather",     "Layer",     "Actions"};
    private static final Class[]  COLUMN_TYPES = {Integer.class, Integer.class, Boolean.class, Layer.class, JButton.class};
    private static final int COLUMN_FROM    = 0;
    private static final int COLUMN_TO      = 1;
    private static final int COLUMN_FEATHER = 2;
    private static final int COLUMN_LAYER   = 3;
    private static final int COLUMN_ACTIONS = 4;
}