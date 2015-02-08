/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers.combined;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import org.pepsoft.worldpainter.layers.CombinedLayer;
import org.pepsoft.worldpainter.layers.CustomLayer;
import org.pepsoft.worldpainter.layers.Layer;

/**
 *
 * @author pepijn
 */
public class CombinedLayerTableModel implements TableModel {
    public CombinedLayerTableModel(List<Layer> layers, Map<Layer, Float> factors) {
        rows = new ArrayList<Row>(layers.size());
        for (Layer layer: layers) {
            rows.add(new Row(layer, (int) (factors.get(layer) * 100 + 0.5f), ((layer instanceof CustomLayer) ? ((CustomLayer) layer).isHide() : false)));
        }
    }
    
    void addRow(Row row) {
        rows.add(row);
        TableModelEvent event = new TableModelEvent(this, rows.size() - 1, rows.size() - 1, TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT);
        for (TableModelListener listener: listeners) {
            listener.tableChanged(event);
        }
    }
    
    void deleteRow(int rowIndex) {
        rows.remove(rowIndex);
        TableModelEvent event = new TableModelEvent(this, rowIndex, rowIndex, TableModelEvent.ALL_COLUMNS, TableModelEvent.DELETE);
        for (TableModelListener listener: listeners) {
            listener.tableChanged(event);
        }
    }

    void saveSettings(CombinedLayer layer) {
        List<Layer> layers = new ArrayList<Layer>(rows.size());
        Map<Layer, Float> factors = new HashMap<Layer, Float>();
        for (Row row: rows) {
            layers.add(row.layer);
            if (row.layer instanceof CustomLayer) {
                ((CustomLayer) row.layer).setHide(row.hide);
            }
            factors.put(row.layer, row.factor / 100f);
        }
        layer.setLayers(layers);
        layer.setFactors(factors);
    }

    boolean contains(Layer layer) {
        for (Row row: rows) {
            if (row.layer.equals(layer)) {
                return true;
            }
        }
        return false;
    }

    // TableModel
    
    @Override
    public int getRowCount() {
        return rows.size();
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
        Row row = rows.get(rowIndex);
        switch (columnIndex) {
            case COLUMN_LAYER:
                return row.layer;
            case COLUMN_FACTOR:
                return row.factor;
            case COLUMN_HIDE:
                return row.hide;
            default:
                throw new IndexOutOfBoundsException("columnIndex " + columnIndex);
        }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        Row row = rows.get(rowIndex);
        switch (columnIndex) {
            case COLUMN_LAYER:
                row.layer = (Layer) aValue;
                break;
            case COLUMN_FACTOR:
                row.factor = (Integer) aValue;
                break;
            case COLUMN_HIDE:
                row.hide = (Boolean) aValue;
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
    
    private final List<Row> rows;
    private final List<TableModelListener> listeners = new ArrayList<TableModelListener>();
    
    public static final int COLUMN_LAYER  = 0;
    public static final int COLUMN_FACTOR = 1;
    public static final int COLUMN_HIDE   = 2;
    
    private static final String[] COLUMN_NAMES   = {"Layer",      "Factor",      "Hidden"};
    private static final Class<?>[] COLUMN_TYPES = {String.class, Integer.class, Boolean.class};
    
    static class Row {
        Row(Layer layer, int factor, boolean hide) {
            this.layer = layer;
            this.factor = factor;
            this.hide = hide;
        }
        
        Layer layer;
        int factor;
        boolean hide;
    }
}