package org.pepsoft.worldpainter;

import org.pepsoft.minecraft.SuperflatPreset;
import org.pepsoft.minecraft.SuperflatPreset.Layer;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class SuperflatPresetTableModel implements TableModel {
    public SuperflatPresetTableModel(SuperflatPreset superflatPreset) {
        this.superflatPreset = superflatPreset;
        layers = superflatPreset.getLayers().stream().map(layer -> new Layer(layer.getMaterialName(), layer.getThickness())).collect(toList());
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
        return false;
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

    }

    @Override
    public void addTableModelListener(TableModelListener l) {
        listeners.add(l);
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
        listeners.remove(l);
    }

    private final SuperflatPreset superflatPreset;
    private final List<Layer> layers;
    private final List<TableModelListener> listeners = new ArrayList<>();

    private static final String[] COLUMN_NAMES = {"Material",   "Thickness"};
    private static final Class[] COLUMN_TYPES  = {String.class, Integer.class};
    private static final int COLUMN_MATERIAL  = 0;
    private static final int COLUMN_THICKNESS = 1;
}