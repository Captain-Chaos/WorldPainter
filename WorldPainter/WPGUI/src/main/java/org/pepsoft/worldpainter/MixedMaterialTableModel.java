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
public final class MixedMaterialTableModel implements TableModel, Cloneable {
    public MixedMaterialTableModel(MixedMaterial material) {
        rows = material.getRows().clone();
        mode = material.getMode();
        if (mode == Mode.LAYERED) {
            COLUMN_NAMES[COLUMN_OCCURRENCE] = "Thickness";
        }
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
                    for (Row row : rows) {
                        total += row.occurrence;
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

    /**
     * Adjusts the occurrences, if necessary, so that they total one thousand
     * while keeping their proportions the same.
     */
    public void normalise() {
        int total = 0;
        for (Row row: rows) {
            total += row.occurrence;
        }
        if (total != 1000) {
            float ratio = 1000f / total;
            total = 0;
            for (int i = 0; i < rows.length; i++) {
                if (i < (rows.length - 1)) {
                    rows[i] = new Row(rows[i].material, Math.max((int) (rows[i].occurrence * ratio + 0.5f), 1), rows[i].scale);
                } else {
                    rows[i] = new Row(rows[i].material, Math.max(1000 - total, 1), rows[i].scale);
                }
                total += rows[i].occurrence;
            }
            while (total > 1000) {
                // This can happen if one or more rows have had to be rounded up
                // because they would have otherwise been zero. This crude
                // algorithm keeps stealing from the highest row (where the
                // relative effect will be the smallest) until the total is 1000
                int highestRowOccurrence = -1, highestRowIndex = -1;
                for (int i = 0; i < rows.length; i++) {
                    if (rows[i].occurrence > highestRowOccurrence) {
                        highestRowOccurrence = rows[i].occurrence;
                        highestRowIndex = i;
                    }
                }
                rows[highestRowIndex] = new Row(rows[highestRowIndex].material, rows[highestRowIndex].occurrence - 1, rows[highestRowIndex].scale);
                total--;
            }
            fireEvent(new TableModelEvent(this, 0, rows.length - 1, COLUMN_OCCURRENCE));
        }
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
        return (columnIndex != COLUMN_MATERIAL) && ((rows.length > 1) || (columnIndex != COLUMN_OCCURRENCE) || (mode == Mode.LAYERED));
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Row row = rows[rowIndex];
        switch (columnIndex) {
            case COLUMN_MATERIAL:
                return row.material;
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
        Row row = rows[rowIndex];
        switch (columnIndex) {
            case COLUMN_MATERIAL:
                row = new Row((Material) aValue, row.occurrence, row.scale);
                break;
            case COLUMN_OCCURRENCE:
                int oldOccurrence = row.occurrence;
                int maxValue = 1001 - rows.length;
                int occurrence = Math.min((Integer) aValue, maxValue);
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
    
    public static final int COLUMN_MATERIAL   = 0;
    public static final int COLUMN_OCCURRENCE = 1;
    public static final int COLUMN_SCALE      = 2;
    
    private static final String[] COLUMN_NAMES =   {"Material",     "Occurrence (in ‰)", "Scale (in %)"};
    private static final Class<?>[] COLUMN_TYPES = {Material.class, Integer.class,         Integer.class};
}