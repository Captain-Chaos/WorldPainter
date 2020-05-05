package org.pepsoft.worldpainter.superflat;

import org.pepsoft.minecraft.SuperflatPreset.Structure;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.sort;
import static java.util.stream.Collectors.joining;

public class SuperflatPresetStructuresTableModel implements TableModel {
    @SuppressWarnings("unchecked") // Guaranteed by code
    public SuperflatPresetStructuresTableModel(Map<Structure, Map<String, String>> structures) {
        allStructures = Structure.values();
        sort(allStructures);
        params = (Map<String, String>[]) new Map[allStructures.length];
        enabled = new boolean[allStructures.length];
        for (int i = 0; i < allStructures.length; i++) {
            if (structures.containsKey(allStructures[i])) {
                params[i] = new HashMap<>(structures.get(allStructures[i]));
                enabled[i] = true;
            } else {
                params[i] = new HashMap<>();
                enabled[i] = false;
            }
        }
    }

    public Map<Structure, Map<String, String>> getStructures() {
        Map<Structure, Map<String, String>> structures = new HashMap<>();
        for (int i = 0; i < allStructures.length; i++) {
            if (enabled[i]) {
                structures.put(allStructures[i], params[i]);
            }
        }
        return structures;
    }

    // TableModel

    @Override
    public int getRowCount() {
        return params.length;
    }

    @Override
    public int getColumnCount() {
        return 3;
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
        return columnIndex != COLUMN_NAME;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case COLUMN_NAME:
                return allStructures[rowIndex].name().toLowerCase();
            case COLUMN_ENABLED:
                return enabled[rowIndex];
            case COLUMN_PARAMS:
                return params[rowIndex].isEmpty()
                        ? ""
                        : params[rowIndex].entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue()).collect(joining(" "));
            default:
                throw new IndexOutOfBoundsException("columnIndex " + columnIndex);
        }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case COLUMN_NAME:
                throw new IllegalArgumentException("columnIndex " + columnIndex);
            case COLUMN_ENABLED:
                enabled[rowIndex] = (Boolean) aValue;
                break;
            case COLUMN_PARAMS:
                params[rowIndex] = new HashMap<>();
                String text = (String) aValue;
                if (! text.trim().isEmpty()) {
                    for (String param: text.trim().split(" ")) {
                        int p = param.indexOf('=');
                        params[rowIndex].put(param.substring(0, p), param.substring(p + 1));
                    }
                }
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

    public static final int COLUMN_NAME    = 0;
    public static final int COLUMN_ENABLED = 1;
    public static final int COLUMN_PARAMS  = 2;

    public static final String[] COLUMN_NAMES   = {"Structure",  "Enabled",     "Parameters"};
    public static final Class<?>[] COLUMN_TYPES = {String.class, Boolean.class, String.class};

    private final Structure[] allStructures;
    private final Map<String, String>[] params;
    private final boolean[] enabled;
    private final List<TableModelListener> listeners = new ArrayList<>();
}