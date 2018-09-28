/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.MixedMaterial.Mode;
import org.pepsoft.worldpainter.MixedMaterial.Row;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author pepijn
 */
public final class MixedMaterialTableModel implements TableModel, Cloneable {
    public MixedMaterialTableModel(MixedMaterial material) {
        rows = material.getRows().clone();
        mode = material.getMode();
    }
    
    public MixedMaterialTableModel() {
        rows = new Row[] {new Row(Material.DIRT, 3, 1.0f)};
        mode = Mode.NOISE;
    }

    public void addMaterial(Row row) {
        rows = Arrays.copyOf(rows, rows.length + 1);
        rows[rows.length - 1] = row;
        
        TableModelEvent event = new TableModelEvent(this, 0, 0, COLUMN_COUNT);
        fireEvent(event);
        event = new TableModelEvent(this, rows.length - 1, rows.length - 1, TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT);
        fireEvent(event);
    }

    public void removeMaterial(int rowIndex) {
        if (rows.length == 1) {
            throw new IllegalArgumentException("Can't remove last row");
        }
        Row[] oldRows = rows;
        rows = new Row[rows.length - 1];
        System.arraycopy(oldRows, 0, rows, 0, rowIndex);
        System.arraycopy(oldRows, rowIndex + 1, rows, rowIndex, rows.length - rowIndex);

        TableModelEvent event = new TableModelEvent(this);
        fireEvent(event);
    }

    public Row[] getRows() {
        return rows;
    }

    public void setMode(Mode mode) {
        if (mode != this.mode) {
            this.mode = mode;
            TableModelEvent event = new TableModelEvent(this, TableModelEvent.HEADER_ROW);
            fireEvent(event);
        }
    }

    public Mode getMode() {
        return mode;
    }

    public int getAverageCount() {
        return (int) (Arrays.stream(rows).mapToInt(row -> row.occurrence).average().orElse(0.0) + 0.5);
    }

    // TableModel
    
    @Override
    public int getRowCount() {
        return rows.length;
    }

    @Override
    public int getColumnCount() {
        return (mode == Mode.BLOBS) ? COLUMN_NAMES.length : (COLUMN_NAMES.length - 1);
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
        return (rows.length > 1) || (columnIndex != COLUMN_COUNT) || (mode == Mode.LAYERED);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Row row = rows[rowIndex];
        switch (columnIndex) {
            case COLUMN_BLOCK_ID:
                return row.material.blockType;
            case COLUMN_DATA_VALUE:
                return row.material.data;
            case COLUMN_COUNT:
                return row.occurrence;
            case COLUMN_SCALE:
                return (int) (row.scale * 100 + 0.5f);
            default:
                throw new IndexOutOfBoundsException("columnIndex " + columnIndex);
        }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if ((rows.length == 1) && (columnIndex == COLUMN_COUNT) && (mode != Mode.LAYERED)) {
            throw new IllegalArgumentException("Uneditable cell");
        }
        Row row = rows[rowIndex];
        switch (columnIndex) {
            case COLUMN_BLOCK_ID:
                row = new Row(Material.get((Integer) aValue, row.material.data), row.occurrence, row.scale);
                break;
            case COLUMN_DATA_VALUE:
                row = new Row(Material.get(row.material.blockType, (Integer) aValue), row.occurrence, row.scale);
                break;
            case COLUMN_COUNT:
                row = new Row(row.material, (Integer) aValue, row.scale);
                break;
            case COLUMN_SCALE:
                row = new Row(row.material, row.occurrence, (Integer) aValue / 100f);
                break;
            default:
                throw new IndexOutOfBoundsException("columnIndex " + columnIndex);
        }
        rows[rowIndex] = row;

        TableModelEvent event = new TableModelEvent(this, rowIndex, rowIndex, columnIndex);
        fireEvent(event);
    }

    @Override
    public void addTableModelListener(TableModelListener l) {
        listeners.add(l);
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
        listeners.remove(l);
    }

    // Cloneable

    @Override
    public MixedMaterialTableModel clone() {
        MixedMaterialTableModel clone = new MixedMaterialTableModel();
        clone.mode = mode;
        clone.rows = rows.clone();
        return clone;
    }

    private void fireEvent(TableModelEvent event) {
        for (TableModelListener listener: listeners) {
            listener.tableChanged(event);
        }
    }

    private final List<TableModelListener> listeners = new ArrayList<>();
    private Row[] rows;
    private Mode mode;

    public static final int COLUMN_BLOCK_ID   = 0;
    public static final int COLUMN_DATA_VALUE = 1;
    public static final int COLUMN_COUNT      = 2;
    public static final int COLUMN_SCALE      = 3;

    private static final String[] COLUMN_NAMES =   {"Block ID",    "Data Value",  "Count",       "Scale (in %)"};
    private static final Class<?>[] COLUMN_TYPES = {Integer.class, Integer.class, Integer.class, Integer.class};
}