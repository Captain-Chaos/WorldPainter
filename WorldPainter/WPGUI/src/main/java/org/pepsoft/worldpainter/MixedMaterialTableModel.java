/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import org.pepsoft.minecraft.Material;
import org.pepsoft.util.MathUtils;
import org.pepsoft.worldpainter.MixedMaterial.Mode;
import org.pepsoft.worldpainter.MixedMaterial.Row;

/**
 *
 * @author pepijn
 */
public class MixedMaterialTableModel implements TableModel {
    public MixedMaterialTableModel(MixedMaterial material) {
        rows = Arrays.copyOf(material.getRows(), material.getRows().length);
        mode = material.getMode();
    }
    
    public MixedMaterialTableModel() {
        rows = new Row[] {new Row(Material.DIRT, 1000, 1.0f)};
        mode = Mode.NOISE;
    }

    public void addMaterial(Row row) {
        rows = Arrays.copyOf(rows, rows.length + 1);
        rows[rows.length - 1] = row;
        
        if (mode != Mode.LAYERED) {
            int remaining = 1000 - row.occurrence;
            float factor = (float) remaining / 1000;
            for (int i = 0; i < rows.length - 1; i++) {
                if (i < rows.length - 2) {
                    int newOccurrence = MathUtils.clamp(1, (int) (rows[i].occurrence * factor + 0.5f), 999);
                    rows[i] = new Row(rows[i].material, newOccurrence, rows[i].scale);
                    remaining -= newOccurrence;
                } else {
                    rows[i] = new Row(rows[i].material, remaining, rows[i].scale);
                }
            }
        }

        TableModelEvent event = new TableModelEvent(this, 0, 0, COLUMN_OCCURRENCE);
        fireEvent(event);
        event = new TableModelEvent(this, rows.length - 1, rows.length - 1, TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT);
        fireEvent(event);
    }

    public void removeMaterial(int rowIndex) {
        if (rows.length == 1) {
            throw new IllegalArgumentException("Can't remove last row");
        }
        Row oldRow = rows[rowIndex];
        Row[] oldRows = rows;
        rows = new Row[rows.length - 1];
        System.arraycopy(oldRows, 0, rows, 0, rowIndex);
        System.arraycopy(oldRows, rowIndex + 1, rows, rowIndex, rows.length - rowIndex);

        if (mode != Mode.LAYERED) {
            float factor = (float) 1000 / (1000 - oldRow.occurrence);
            int remaining = 1000;
            for (int i = 0; i < rows.length; i++) {
                if (i < rows.length - 1) {
                    int newOccurrence = MathUtils.clamp(1, (int) (rows[i].occurrence * factor + 0.5f), 999);
                    rows[i] = new Row(rows[i].material, newOccurrence, rows[i].scale);
                    remaining -= newOccurrence;
                } else {
                    rows[i] = new Row(rows[i].material, remaining, rows[i].scale);
                }
            }
        }

        TableModelEvent event = new TableModelEvent(this);
        fireEvent(event);
    }

    public Row[] getRows() {
        return rows;
    }

    public void setMode(Mode mode) {
        if (mode != this.mode) {
            Mode previousMode = this.mode;
            this.mode = mode;
            if (mode == Mode.LAYERED) {
                for (int i = 0; i < rows.length; i++) {
                    rows[i] = new Row(rows[i].material, Math.max(rows[i].occurrence / 100, 3), rows[i].scale);
                }
                COLUMN_NAMES[COLUMN_OCCURRENCE] = "Thickness";
            } else {
                if (previousMode == Mode.LAYERED) {
                    int total = 0;
                    for (int i = 0; i < rows.length; i++) {
                        total += rows[i].occurrence;
                    }
                    int remaining = 1000;
                    for (int i = 0; i < rows.length; i++) {
                        if (i < rows.length - 1) {
                            int newOccurrence = rows[i].occurrence * 1000 / total;
                            rows[i] = new Row(rows[i].material, newOccurrence, rows[i].scale);
                            remaining -= newOccurrence;
                        } else {
                            rows[i] = new Row(rows[i].material, remaining, rows[i].scale);
                        }
                    }
                }
                COLUMN_NAMES[COLUMN_OCCURRENCE] = "Occurrence (in ‰)";
            }
            TableModelEvent event = new TableModelEvent(this, TableModelEvent.HEADER_ROW);
            fireEvent(event);
        }
    }
    
    public Mode getMode() {
        return mode;
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
        return (rows.length > 1) || (columnIndex != COLUMN_OCCURRENCE) || (mode == Mode.LAYERED);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Row row = rows[rowIndex];
        switch (columnIndex) {
            case COLUMN_BLOCK_ID:
                return row.material.blockType;
            case COLUMN_DATA_VALUE:
                return row.material.data;
            case COLUMN_OCCURRENCE:
                return row.occurrence;
            case COLUMN_SCALE:
                return (int) (row.scale * 100 + 0.5f);
            default:
                throw new IndexOutOfBoundsException("columnIndex " + columnIndex);
        }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if ((rows.length == 1) && (columnIndex == COLUMN_OCCURRENCE) && (mode != Mode.LAYERED)) {
            throw new IllegalArgumentException("Uneditable cell");
        }
        int occurrence = 0, oldOccurrence = 0;
        Row row = rows[rowIndex];
        switch (columnIndex) {
            case COLUMN_BLOCK_ID:
                row = new Row(Material.get((Integer) aValue, row.material.data), row.occurrence, row.scale);
                break;
            case COLUMN_DATA_VALUE:
                row = new Row(Material.get(row.material.blockType, (Integer) aValue), row.occurrence, row.scale);
                break;
            case COLUMN_OCCURRENCE:
                oldOccurrence = row.occurrence;
                int maxValue = 1001 - rows.length;
                occurrence = Math.min((Integer) aValue, maxValue);
                if (occurrence == oldOccurrence) {
                    return;
                }
                row = new Row(row.material, occurrence, row.scale);
                break;
            case COLUMN_SCALE:
                row = new Row(row.material, row.occurrence, (Integer) aValue / 100f);
                break;
            default:
                throw new IndexOutOfBoundsException("columnIndex " + columnIndex);
        }
        rows[rowIndex] = row;
        
        if ((mode != Mode.LAYERED) &&  (columnIndex == COLUMN_OCCURRENCE)) {
            int remaining = 1000 - occurrence;
            float factor = (float) remaining / (1000 - oldOccurrence);
            for (int i = 0; i < rows.length; i++) {
                if (i == rowIndex) {
                    continue;
                }
                if ((rowIndex == rows.length - 1) ? (i < rows.length - 2) : (i < rows.length - 1)) {
                    int newOccurrence = MathUtils.clamp(1, (int) (rows[i].occurrence * factor + 0.5f), 999);
                    rows[i] = new Row(rows[i].material, newOccurrence, rows[i].scale);
                    remaining -= newOccurrence;
                } else {
                    rows[i] = new Row(rows[i].material, remaining, rows[i].scale);
                }
            }

            TableModelEvent event = new TableModelEvent(this, 0, rows.length - 1, columnIndex);
            fireEvent(event);
        } else {
            TableModelEvent event = new TableModelEvent(this, rowIndex, rowIndex, columnIndex);
            fireEvent(event);
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

    private void fireEvent(TableModelEvent event) {
        for (TableModelListener listener: listeners) {
            listener.tableChanged(event);
        }
    }
    
    private final List<TableModelListener> listeners = new ArrayList<TableModelListener>();
    private Row[] rows;
    private Mode mode;
    
    public static final int COLUMN_BLOCK_ID   = 0;
    public static final int COLUMN_DATA_VALUE = 1;
    public static final int COLUMN_OCCURRENCE = 2;
    public static final int COLUMN_SCALE      = 3;
    
    private static final String[] COLUMN_NAMES =   {"Block ID",    "Data Value",  "Occurrence (in ‰)", "Scale (in %)"};
    private static final Class<?>[] COLUMN_TYPES = {Integer.class, Integer.class, Integer.class,       Integer.class};
}