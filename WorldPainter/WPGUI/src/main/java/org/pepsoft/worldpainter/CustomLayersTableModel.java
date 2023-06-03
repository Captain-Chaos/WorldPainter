/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

import org.pepsoft.worldpainter.exporting.FirstPassLayerExporter;
import org.pepsoft.worldpainter.exporting.SecondPassLayerExporter;
import org.pepsoft.worldpainter.layers.CombinedLayer;
import org.pepsoft.worldpainter.layers.CustomLayer;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author pepijn
 */
public class CustomLayersTableModel implements TableModel {
    /**
     * This assumes that no exporter is both first pass <em>and</em> second pass, which is currently the case for
     * {@link CombinedLayer}s, so those should be excluded
     */
    public CustomLayersTableModel(Collection<CustomLayer> allCustomLayers) {
        customLayers = new ArrayList<>(allCustomLayers);

        // Sort the list, with first pass layers first
        customLayers.sort((layer1, layer2) -> {
            boolean layer1FirstPass = FirstPassLayerExporter.class.isAssignableFrom(layer1.getExporterType());
            boolean layer2FirstPass = FirstPassLayerExporter.class.isAssignableFrom(layer2.getExporterType());
            if (layer1FirstPass && (! layer2FirstPass)) {
                return -1;
            } else if ((! layer1FirstPass) && layer2FirstPass) {
                return 1;
            } else {
                return layer1.compareTo(layer2);
            }
        });

        // Insert first and second pass headers
        if (! customLayers.isEmpty()) {
            customLayers.add(0, FIRST_PASS_HEADER);
            for (int i = customLayers.size() - 1; i > 1; i--) {
                if (! SecondPassLayerExporter.class.isAssignableFrom(customLayers.get(i).getExporterType())) {
                    customLayers.add(i + 1, SECOND_PASS_HEADER);
                    break;
                }
            }
        }
    }

    /**
     * Swap two layers.
     * 
     * @param rowIndex1 The index of the first layer.
     * @param rowIndex2 The index of the second layer.
     */
    public void swap(int rowIndex1, int rowIndex2) {
        if (isHeaderRow(rowIndex1) || isHeaderRow(rowIndex2)) {
            throw new IllegalArgumentException("Cannot swap with header rows");
        }
        CustomLayer layer = customLayers.get(rowIndex1);
        customLayers.set(rowIndex1, customLayers.get(rowIndex2));
        customLayers.set(rowIndex2, layer);
        orderPristine = false;
        TableModelEvent event = new TableModelEvent(this, Math.min(rowIndex1, rowIndex2), Math.max(rowIndex1, rowIndex2));
        for (TableModelListener listener: listeners) {
            listener.tableChanged(event);
        }
    }

    public void setExport(int rowIndex, boolean export) {
        final CustomLayer layer = customLayers.get(rowIndex);
        if (isHeader(layer)) {
            throw new IllegalArgumentException("Cannot set export property of header rows");
        }
        if (export != layer.isExport()) {
            layer.setExport(export);
            final TableModelEvent event = new TableModelEvent(this, rowIndex, rowIndex, COLUMN_EXPORT);
            for (TableModelListener listener: listeners) {
                listener.tableChanged(event);
            }
        }
    }

    public boolean isPristine() {
        return orderPristine && exportsPristine;
    }

    public boolean isHeaderRow(int rowIndex) {
        return isHeader(customLayers.get(rowIndex));
    }

    public boolean isHeader(CustomLayer layer) {
        return (layer == FIRST_PASS_HEADER) || (layer == SECOND_PASS_HEADER);
    }

    /**
     * Save the current order to the indices of the custom layers.
     */
    public void save() {
        if (! orderPristine) {
            int index = 0;
            for (CustomLayer layer : customLayers) {
                if (! isHeader(layer)) {
                    layer.setIndex(index++);
                }
            }
        }
        // The exports have already been set directly on the layers. TODO: fix
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
        return (columnIndex == COLUMN_EXPORT) && (! isHeaderRow(rowIndex));
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        CustomLayer layer = customLayers.get(rowIndex);
        switch (columnIndex) {
            case COLUMN_LAYER:
                return layer;
            case COLUMN_TYPE:
                return isHeader(layer) ? null : layer.getType();
            case COLUMN_PALETTE:
                return isHeader(layer) ? null : layer.getPalette();
            case COLUMN_EXPORT:
                return isHeader(layer) ? null : layer.isExport();
            default:
                throw new IndexOutOfBoundsException("columnIndex " + columnIndex);
        }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if ((columnIndex != COLUMN_EXPORT) || isHeaderRow(rowIndex)) {
            throw new IllegalArgumentException();
        }
        customLayers.get(rowIndex).setExport((Boolean) aValue);
        exportsPristine = false;
    }

    @Override
    public void addTableModelListener(TableModelListener l) {
        listeners.add(l);
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
        listeners.remove(l);
    }
    
    private final List<CustomLayer> customLayers;
    private final List<TableModelListener> listeners = new ArrayList<>();
    private boolean orderPristine = true, exportsPristine = true;

    public static final int COLUMN_LAYER   = 0;
    public static final int COLUMN_PALETTE = 1;
    public static final int COLUMN_TYPE    = 2;
    public static final int COLUMN_EXPORT  = 3;

    private static final String[]   COLUMN_NAMES = {"Layer",           "Palette",    "Type",       "Export"};
    private static final Class<?>[] COLUMN_TYPES = {CustomLayer.class, String.class, String.class, Boolean.class};

    public static final CustomLayer FIRST_PASS_HEADER = new CustomLayer("First export pass", null, null, -1, -1) {};
    public static final CustomLayer SECOND_PASS_HEADER = new CustomLayer("Second export pass", null, null, -1, -1) {};
}