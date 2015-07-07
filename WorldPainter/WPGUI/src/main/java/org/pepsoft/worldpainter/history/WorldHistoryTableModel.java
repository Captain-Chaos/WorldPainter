/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.history;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import org.pepsoft.worldpainter.World2;

/**
 *
 * @author Pepijn Schmitz
 */
public class WorldHistoryTableModel implements TableModel {
    public WorldHistoryTableModel(World2 world) {
        history = world.getHistory();
        filter();
    }

    public boolean isIncludeLoadsSaves() {
        return includeLoadsSaves;
    }

    public void setIncludeLoadsSaves(boolean includeLoadsSaves) {
        this.includeLoadsSaves = includeLoadsSaves;
        filter();
    }

    public boolean isIncludeExportsMerges() {
        return includeExportsMerges;
    }

    public void setIncludeExportsMerges(boolean includeExportsMerges) {
        this.includeExportsMerges = includeExportsMerges;
        filter();
    }
    
    @Override
    public int getRowCount() {
        return filteredHistory.size();
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
        HistoryEntry entry = filteredHistory.get(rowIndex);
        switch (columnIndex) {
            case COLUMN_DATE:
                return dateFormatter.format(new Date(entry.timestamp));
            case COLUMN_USER:
                return entry.userId;
            case COLUMN_ACTION:
                return entry.getText();
            default:
                throw new IndexOutOfBoundsException("columnIndex");
        }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public void addTableModelListener(TableModelListener l) {
        listeners.add(l);
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
        listeners.remove(l);
    }
    
    private void filter() {
        filteredHistory = history.stream()
            .filter((entry) -> includeLoadsSaves || ((entry.key != HistoryEntry.WORLD_LOADED) && (entry.key != HistoryEntry.WORLD_SAVED)))
            .filter((entry) -> includeExportsMerges || ((entry.key != HistoryEntry.WORLD_EXPORTED_FULL) && (entry.key != HistoryEntry.WORLD_EXPORTED_PARTIAL) && (entry.key != HistoryEntry.WORLD_MERGED_FULL) && (entry.key != HistoryEntry.WORLD_MERGED_PARTIAL)))
            .collect(Collectors.toList());
        TableModelEvent event = new TableModelEvent(this);
        for (TableModelListener listener: listeners) {
            listener.tableChanged(event);
        }
    }
    
    private final List<HistoryEntry> history;
    private final List<TableModelListener> listeners = new ArrayList<>();
    private final DateFormat dateFormatter = DateFormat.getDateInstance();
    private List<HistoryEntry> filteredHistory;
    private boolean includeLoadsSaves, includeExportsMerges = true;
    
    private static final String[] COLUMN_NAMES = {"Date", "User", "Action"};
    private static final Class[] COLUMN_TYPES = {String.class, String.class, String.class};
    private static final int COLUMN_DATE   = 0;
    private static final int COLUMN_USER   = 1;
    private static final int COLUMN_ACTION = 2;
}