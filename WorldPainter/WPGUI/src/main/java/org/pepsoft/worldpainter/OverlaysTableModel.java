package org.pepsoft.worldpainter;

import org.jetbrains.annotations.Nls;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.util.ArrayList;
import java.util.List;

import static javax.swing.event.TableModelEvent.*;

public class OverlaysTableModel implements TableModel {
    public OverlaysTableModel(Dimension dimension) {
        this.dimension = dimension;
    }

    public int addOverlay(Overlay overlay) {
        final int rowIndex = dimension.addOverlay(overlay);
        final TableModelEvent event = new TableModelEvent(this, rowIndex, rowIndex, ALL_COLUMNS, INSERT);
        for (TableModelListener listener: listeners) {
            listener.tableChanged(event);
        }
        return rowIndex;
    }

    public Overlay getOverlay(int rowIndex) {
        return dimension.getOverlays().get(rowIndex);
    }

    public void removeOverlay(int rowIndex) {
        dimension.removeOverlay(rowIndex);
        final TableModelEvent event = new TableModelEvent(this, rowIndex, rowIndex, ALL_COLUMNS, DELETE);
        for (TableModelListener listener: listeners) {
            listener.tableChanged(event);
        }
    }

    public void overlayChanged(int rowIndex) {
        final TableModelEvent event = new TableModelEvent(this, rowIndex);
        for (TableModelListener listener: listeners) {
            listener.tableChanged(event);
        }
    }

    @Override
    public int getRowCount() {
        return dimension.getOverlays().size();
    }

    @Override
    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    @Nls
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
        return columnIndex == COLUMN_ENABLED;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        final Overlay overlay = dimension.getOverlays().get(rowIndex);
        switch (columnIndex) {
            case COLUMN_ENABLED:
                return overlay.isEnabled();
            case COLUMN_IMAGE:
                return overlay.getFile().getName();
            default:
                throw new IndexOutOfBoundsException("columnIndex " + columnIndex);
        }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (columnIndex == COLUMN_ENABLED) {
            dimension.getOverlays().get(rowIndex).setEnabled((boolean) aValue);
        } else {
            throw new IllegalArgumentException("columnIndex " + columnIndex);
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

    private final Dimension dimension;
    private final List<TableModelListener> listeners = new ArrayList<>();

    private static final int COLUMN_ENABLED = 0;
    private static final int COLUMN_IMAGE   = 1;

    private static final String[] COLUMN_NAMES = { "Enabled", "Image" };
    private static final Class<?>[] COLUMN_TYPES = { Boolean.class, String.class };
}