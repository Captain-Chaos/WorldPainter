/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.themes.impl.fancy;

import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.themes.Filter;
import org.pepsoft.worldpainter.themes.HeightFilter;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author SchmitzP
 */
public class LayerMapTableModel implements TableModel {
    public LayerMapTableModel(int minHeight, int maxHeight, Map<Filter, Layer> layerMap) {
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        filters = new ArrayList<>(layerMap.size());
        layers = new ArrayList<>(layerMap.size());
        for (Map.Entry<Filter, Layer> entry: layerMap.entrySet()) {
            filters.add((HeightFilter) entry.getKey());
            layers.add(entry.getValue());
        }
    }

    @Override
    public int getRowCount() {
        return layers.size();
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
        return true;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case COLUMN_LAYER:
                return layers.get(rowIndex);
            case COLUMN_FROM:
                return filters.get(rowIndex).getStartHeight();
            case COLUMN_TO:
                return filters.get(rowIndex).getStopHeight();
            case COLUMN_FEATHER:
                return filters.get(rowIndex).isFeather();
            default:
                throw new IndexOutOfBoundsException("columnIndex " + columnIndex);
        }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case COLUMN_LAYER:
                layers.set(rowIndex, (Layer) aValue);
                break;
            case COLUMN_FROM:
                HeightFilter oldFilter = filters.get(rowIndex);
                filters.set(rowIndex, new HeightFilter(minHeight, maxHeight, (Integer) aValue, oldFilter.getStopHeight(), oldFilter.isFeather()));
                break;
            case COLUMN_TO:
                oldFilter = filters.get(rowIndex);
                filters.set(rowIndex, new HeightFilter(minHeight, maxHeight, oldFilter.getStartHeight(), (Integer) aValue, oldFilter.isFeather()));
                break;
            case COLUMN_FEATHER:
                oldFilter = filters.get(rowIndex);
                filters.set(rowIndex, new HeightFilter(minHeight, maxHeight, oldFilter.getStartHeight(), oldFilter.getStopHeight(), (Boolean) aValue));
                break;
            default:
                throw new IndexOutOfBoundsException("columnIndex " + columnIndex);
        }
        TableModelEvent event = new TableModelEvent(this, rowIndex, rowIndex, columnIndex);
        for (TableModelListener listener: listeners) {
            listener.tableChanged(event);
        }
    }

    @Override
    public void addTableModelListener(TableModelListener l) {
        listeners.add(l);
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
        listeners.add(l);
    }
    
    private final int minHeight, maxHeight;
    private final List<HeightFilter> filters;
    private final List<Layer> layers;
    private final List<TableModelListener> listeners = new ArrayList<>();
    
    private static final String[] COLUMN_NAMES = {"Layer",     "From",        "To",          "Feather"};
    private static final Class[] COLUMN_TYPES =  {Layer.class, Integer.class, Integer.class, Boolean.class};
    
    private static final int COLUMN_LAYER   = 0;
    private static final int COLUMN_FROM    = 1;
    private static final int COLUMN_TO      = 2;
    private static final int COLUMN_FEATHER = 3;
}