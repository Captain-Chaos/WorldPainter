/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.layers.tunnel;

import org.pepsoft.util.MathUtils;
import org.pepsoft.worldpainter.NoiseSettings;
import org.pepsoft.worldpainter.layers.Layer;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author SchmitzP
 */
public class TunnelLayersTableModel implements TableModel {
    public TunnelLayersTableModel(List<TunnelLayer.LayerSettings> settings, int minHeight, int maxHeight) {
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        if (settings != null) {
            for (TunnelLayer.LayerSettings layerSettings: settings) {
                // Clone the custom layers and all layer settings, so we can still cancel out of any changes if the user
                // presses Cancel
                final TunnelLayer.LayerSettings clonedSettings = layerSettings.clone();
                layers.add(clonedSettings.getLayer());
                this.settings.put(clonedSettings.getLayer(), clonedSettings);
            }
        }
    }

    public void addLayer(Layer layer) {
        if (! layers.contains(layer)) {
            layers.add(layer);
            final TunnelLayer.LayerSettings layerSettings = new TunnelLayer.LayerSettings();
            layerSettings.setLayer(layer);
            settings.put(layer, layerSettings);
            int row = layers.size() - 1;
            TableModelEvent event = new TableModelEvent(this, row, row, TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT);
            for (TableModelListener listener: listeners) {
                listener.tableChanged(event);
            }
        }
    }

    public Layer getLayer(int row) {
        return layers.get(row);
    }

    public void removeLayer(int row) {
        Layer layer = layers.remove(row);
        settings.remove(layer);
        TableModelEvent event = new TableModelEvent(this, row, row, TableModelEvent.ALL_COLUMNS, TableModelEvent.DELETE);
        for (TableModelListener listener: listeners) {
            listener.tableChanged(event);
        }
    }

    public void layerChanged(int row) {
        TableModelEvent event = new TableModelEvent(this, row);
        for (TableModelListener listener: listeners) {
            listener.tableChanged(event);
        }
    }

    public List<TunnelLayer.LayerSettings> getLayers() {
        final List<TunnelLayer.LayerSettings> layerSettings = new ArrayList<>(layers.size());
        for (Layer layer: layers) {
            layerSettings.add(settings.get(layer));
        }
        return layerSettings;
    }
    
    // TableModel
    
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
        return (column != COLUMN_NAME);
    }

    @Override
    public Object getValueAt(int row, int column) {
        Layer layer = layers.get(row);
        switch (column) {
            case COLUMN_NAME:
                return layer;
            case COLUMN_INTENSITY:
                TunnelLayer.LayerSettings layerSettings = settings.get(layer);
                return layerSettings.getIntensity();
            case COLUMN_VARIATION:
                layerSettings = settings.get(layer);
                return layerSettings.getVariation();
            case COLUMN_MIN_LEVEL:
                layerSettings = settings.get(layer);
                return layerSettings.getMinLevel();
            case COLUMN_MAX_LEVEL:
                layerSettings = settings.get(layer);
                return layerSettings.getMaxLevel();
            default:
                throw new IndexOutOfBoundsException("column: " + column);
        }
    }

    @Override
    public void setValueAt(Object value, int row, int column) {
        Layer layer = layers.get(row);
        TunnelLayer.LayerSettings layerSettings = settings.get(layer);
        switch (column) {
            case COLUMN_INTENSITY:
                layerSettings.setIntensity(MathUtils.clamp(0, (Integer) value, 100));
                break;
            case COLUMN_VARIATION:
                layerSettings.setVariation((NoiseSettings) value);
                break;
            case COLUMN_MIN_LEVEL:
                layerSettings.setMinLevel(MathUtils.clamp(minHeight, (Integer) value, layerSettings.getMaxLevel()));
                break;
            case COLUMN_MAX_LEVEL:
                layerSettings.setMaxLevel(MathUtils.clamp(layerSettings.getMinLevel(), (Integer) value, maxHeight - 1));
                break;
            default:
                throw new IndexOutOfBoundsException("column: " + column);
        }
    }

    @Override
    public void addTableModelListener(TableModelListener listener) {
        if (! listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeTableModelListener(TableModelListener listener) {
        listeners.remove(listener);
    }
    
    private final List<Layer> layers = new ArrayList<>();
    private final Map<Layer, TunnelLayer.LayerSettings> settings = new HashMap<>();
    private final List<TableModelListener> listeners = new ArrayList<>();
    private final int minHeight, maxHeight;
    
    public static final int COLUMN_NAME      = 0;
    public static final int COLUMN_INTENSITY = 1;
    public static final int COLUMN_VARIATION = 2;
    public static final int COLUMN_MIN_LEVEL = 3;
    public static final int COLUMN_MAX_LEVEL = 4;
    
    private static final String[] COLUMN_NAMES = {"Layer",      "Base intensity (%)", "Variation (%-points)", "Minimum level", "Maximum level"};
    private static final Class[] COLUMN_TYPES =  {JLabel.class, Integer.class,        NoiseSettings.class,    Integer.class,   Integer.class};
}