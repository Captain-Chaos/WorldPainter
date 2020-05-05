package org.pepsoft.worldpainter.superflat;

import org.pepsoft.minecraft.SuperflatPreset.Layer;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toCollection;
import static javax.swing.event.TableModelEvent.*;

public class SuperflatPresetLayersTableModel implements TableModel {
    public SuperflatPresetLayersTableModel(List<Layer> layers) {
        this.layers = layers.stream().map(layer -> new Layer(layer.getMaterialName(), layer.getThickness())).collect(toCollection(ArrayList::new));
    }

    public void addLayer(Layer layer) {
        layers.add(layer);
        TableModelEvent event = new TableModelEvent(this, layers.size() - 1, layers.size() - 1, ALL_COLUMNS, INSERT);
        for (TableModelListener listener: listeners) {
            listener.tableChanged(event);
        }
    }

    public void deleteLayer(int rowIndex) {
        layers.remove(rowIndex);
        TableModelEvent event = new TableModelEvent(this, layers.size() - 1, layers.size() - 1, ALL_COLUMNS, DELETE);
        for (TableModelListener listener: listeners) {
            listener.tableChanged(event);
        }
    }

    public List<Layer> getLayers() {
        return layers;
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
            case COLUMN_MATERIAL:
                return layers.get(rowIndex).getMaterialName();
            case COLUMN_THICKNESS:
                return layers.get(rowIndex).getThickness();
            default:
                throw new IndexOutOfBoundsException("columnIndex " + columnIndex);
        }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case COLUMN_MATERIAL:
                layers.get(rowIndex).setMaterialName((String) aValue);
                break;
            case COLUMN_THICKNESS:
                layers.get(rowIndex).setThickness((Integer) aValue);
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

    private final List<Layer> layers;
    private final List<TableModelListener> listeners = new ArrayList<>();

    private static final String[] COLUMN_NAMES    = {"Material",   "Thickness"};
    private static final Class<?>[] COLUMN_TYPES  = {String.class, Integer.class};
    private static final int COLUMN_MATERIAL  = 0;
    private static final int COLUMN_THICKNESS = 1;
}