/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.layers.tunnel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import org.pepsoft.worldpainter.NoiseSettings;
import org.pepsoft.worldpainter.layers.Layer;

/**
 *
 * @author SchmitzP
 */
public class TunnelFloorLayersTableModel implements TableModel {
    public TunnelFloorLayersTableModel(List<Layer> layers, Map<Layer, TunnelLayer.LayerSettings> settings) {
        this.layers = layers;
        this.settings = settings;
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
    public String getColumnName(int index) {
        return COLUMN_NAMES[index];
    }

    @Override
    public Class<?> getColumnClass(int index) {
        return COLUMN_TYPES[index];
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    @Override
    public Object getValueAt(int row, int column) {
        Layer layer = layers.get(row);
        TunnelLayer.LayerSettings layerSettings = settings.get(layer);
        switch (column) {
            case COLUMN_NAME:
                return layer.getName();
            case COLUMN_INTENSITY:
                return layerSettings.getIntensity();
            case COLUMN_VARIATION:
                return layerSettings.getVariation();
            case COLUMN_MIN_LEVEL:
                return layerSettings.getMinLevel();
            case COLUMN_MAX_LEVEL:
                return layerSettings.getMaxLevel();
            default:
                throw new IndexOutOfBoundsException("column: " + column);
        }
    }

    @Override
    public void setValueAt(Object value, int row, int column) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public void addTableModelListener(TableModelListener listener) {
        if (listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeTableModelListener(TableModelListener listener) {
        listeners.remove(listener);
    }
    
    private final List<Layer> layers;
    private final Map<Layer, TunnelLayer.LayerSettings> settings;
    private final List<TableModelListener> listeners = new ArrayList<>();
    
    public static final int COLUMN_NAME      = 0;
    public static final int COLUMN_INTENSITY = 1;
    public static final int COLUMN_VARIATION = 2;
    public static final int COLUMN_MIN_LEVEL = 3;
    public static final int COLUMN_MAX_LEVEL = 4;
    
    private static final String[] COLUMN_NAMES = {"Layer",      "Intensity",   "Variation",         "Minimum level", "Maximum level"};
    private static final Class[] COLUMN_TYPES =  {String.class, Integer.class, NoiseSettings.class, Integer.class,   Integer.class};
}