/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.panels;

import org.pepsoft.util.IconUtils;
import org.pepsoft.worldpainter.biomeschemes.AutoBiomeScheme;
import org.pepsoft.worldpainter.biomeschemes.BiomeHelper;
import org.pepsoft.worldpainter.biomeschemes.CustomBiomeManager;
import org.pepsoft.worldpainter.biomeschemes.Minecraft1_7Biomes;
import org.pepsoft.worldpainter.layers.*;
import org.pepsoft.worldpainter.selection.SelectionBlock;
import org.pepsoft.worldpainter.selection.SelectionChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.WorldPainter;

import static org.pepsoft.worldpainter.Constants.*;

/**
 *
 * @author pepijn
 */
public class InfoPanel extends javax.swing.JPanel implements MouseMotionListener {
    /**
     * Creates new form InfoPanel
     */
    public InfoPanel(WorldPainter view, CustomBiomeManager customBiomeManager) {
        this.view = view;
        tableModel = new LayerTableModel();
        biomeHelper = new BiomeHelper(new AutoBiomeScheme(null), view.getColourScheme(), customBiomeManager);
        heightFormatter = NumberFormat.getInstance();
        heightFormatter.setMaximumFractionDigits(3);

        initComponents();

        jTable1.setModel(tableModel);
        jTable1.setDefaultRenderer(Layer.class, new LayerTableCellRenderer());
        jTable1.setDefaultRenderer(LayerTableModel.InfoRow.class, new InfoRowTableCellRenderer(biomeHelper));
        jTable1.getColumnModel().getColumn(1).setPreferredWidth(25);

        addHierarchyListener(e -> {
            if (isShowing()) {
                activate();
            } else {
                deactivate();
            }
        });
    }

    void updateInfo() {
        setTextIfDifferent(labelCoords, worldCoords.x + "," + worldCoords.y);
        Dimension dim = view.getDimension();
        if (dim == null) {
            clearFields();
            return;
        }
        Tile tile = dim.getTile(worldCoords.x >> TILE_SIZE_BITS, worldCoords.y >> TILE_SIZE_BITS);
        if (tile == null) {
            clearFields();
            return;
        }
        final int x = worldCoords.x & TILE_SIZE_MASK, y = worldCoords.y & TILE_SIZE_MASK;
        if (tile.getBitLayerValue(NotPresent.INSTANCE, x, y)) {
            clearFields();
            return;
        }
        fieldsClear = false;
        float height = tile.getHeight(x, y);
        setTextIfDifferent(labelHeight, heightFormatter.format(height));
        int intHeight = (int) (height + 0.5f);
        int waterLevel = tile.getWaterLevel(x, y);
        setTextIfDifferent(labelWaterLevel, Integer.toString(waterLevel));
        if (waterLevel > intHeight) {
            setTextIfDifferent(labelWaterDepth, Integer.toString(waterLevel - intHeight));
        } else {
            setTextIfDifferent(labelWaterDepth, null);
        }
        float slope;
        if ((x > 0) && (x < TILE_SIZE - 1) && (y > 0) && (y < TILE_SIZE - 1)) {
            slope = tile.getSlope(x, y);
        } else {
            slope = dim.getSlope(worldCoords.x, worldCoords.y);
        }
        setTextIfDifferent(labelSlope, ((int) (Math.atan(slope) * 180 / Math.PI + 0.5)) + "°");
        Terrain terrain = tile.getTerrain(x, y);
        if (terrain != currentTerrain) {
            labelTerrain.setText(terrain.getName());
            labelTerrain.setIcon(new ImageIcon(terrain.getIcon(view.getColourScheme())));
            currentTerrain = terrain;
        }
        int biome = tile.getLayerValue(Biome.INSTANCE, x, y);
        boolean automaticBiome = false;
        if (biome == 255) {
            automaticBiome = true;
            biome = dim.getAutoBiome(tile, x, y);
        }
        if (biome < 0) {
            biome = Minecraft1_7Biomes.BIOME_PLAINS;
        }
        if ((automaticBiome != currentAutomaticBiome) || (biome != currentBiome)) {
            labelBiome.setText(biomeHelper.getBiomeName(biome) + " (" + biome + ")");
            labelBiome.setIcon(biomeHelper.getBiomeIcon(biome));
            checkBoxAutomaticBiome.setSelected(automaticBiome);
            currentAutomaticBiome = automaticBiome;
            currentBiome = biome;
        }
        Map<Layer, Integer> layerValues = tile.getLayersAt(x, y);
        if (layerValues != null) {
            checkBoxInSelection.setSelected(layerValues.containsKey(SelectionChunk.INSTANCE) || layerValues.containsKey(SelectionBlock.INSTANCE));
            layerValues.keySet().removeAll(HIDDEN_LAYERS);
            if (! layerValues.isEmpty()) {
                tableModel.update(layerValues);
            } else {
                tableModel.clear();
            }
        } else {
            checkBoxInSelection.setSelected(false);
            tableModel.clear();
        }
    }

    private void activate() {
        if (! active) {
            if (logger.isDebugEnabled()) {
                logger.debug("Activating info panel");
            }
            //        updateTimer = new Timer(150, e -> updateInfo());
            //        updateTimer.setRepeats(false);
            view.addMouseMotionListener(this);
            active = true;
        }
    }

    private void deactivate() {
        if (active) {
            if (logger.isDebugEnabled()) {
                logger.debug("Deactivating info panel");
            }
            view.removeMouseMotionListener(this);
            active = false;
        }
    }

    private void clearFields() {
        if (! fieldsClear) {
            labelHeight.setText(null);
            labelWaterLevel.setText(null);
            labelWaterDepth.setText(null);
            labelSlope.setText(null);
            tableModel.clear();
            labelTerrain.setIcon(ICON_BLANK);
            labelTerrain.setText(null);
            currentTerrain = null;
            labelBiome.setIcon(ICON_BLANK);
            labelBiome.setText(null);
            checkBoxAutomaticBiome.setSelected(false);
            currentAutomaticBiome = false;
            currentBiome = -2;
            fieldsClear = true;
        }
    }
    
    /**
     * {@link JLabel#setText(String)} does not check whether the new value is
     * different. On the other hand {@link JLabel#getText()} is a very simple
     * method which just returns a field. So if the text will frequently not
     * have changed, it is cheaper to check with <code>getText()</code> whether
     * the text is different and only invoke <code>setText()</code> if it is,
     * which is what this method does.
     *
     * @param label The label on which to set the <code>text</code> property.
     * @param text The text to set.
     */
    private void setTextIfDifferent(JLabel label, String text) {
        if ((text == null) ? (label.getText() != null) : (! text.equals(label.getText()))) {
            label.setText(text);
        }
    }

    // MouseMotionListener

    @Override
    public void mouseMoved(MouseEvent e) {
        worldCoords = view.viewToWorld(e.getX(), e.getY());
        updateInfo();
//        if (updateTimer.isRunning()) {
//            updateTimer.restart();
//        } else {
//            updateTimer.start();
//        }
    }

    @Override public void mouseDragged(MouseEvent e) {}

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        labelSlope = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        labelCoords = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        labelHeight = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        labelFluidType = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        labelWaterLevel = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        labelWaterDepth = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jLabel3 = new javax.swing.JLabel();
        labelTerrain = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        labelBiome = new javax.swing.JLabel();
        checkBoxAutomaticBiome = new javax.swing.JCheckBox();
        jLabel9 = new javax.swing.JLabel();
        checkBoxInSelection = new javax.swing.JCheckBox();
        jLabel4 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();

        jLabel1.setText("Slope:");

        jLabel2.setText("Layers:");

        labelSlope.setText(" ");

        jLabel5.setText("Coordinates:");

        labelCoords.setText(" ");

        jLabel7.setText("Height:");

        labelHeight.setText(" ");

        jLabel10.setText("m");

        labelFluidType.setText("Fluid");

        jLabel12.setText("level:");

        jLabel13.setText("depth:");

        labelWaterLevel.setText(" ");

        jLabel15.setText("m");

        labelWaterDepth.setText(" ");

        jLabel17.setText("m");

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane2.setViewportView(jTable1);

        jLabel3.setText("Terrain:");

        labelTerrain.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/transparent.png"))); // NOI18N
        labelTerrain.setText(" ");

        jLabel6.setText("Biome:");

        labelBiome.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/transparent.png"))); // NOI18N
        labelBiome.setText(" ");

        checkBoxAutomaticBiome.setText(" ");
        checkBoxAutomaticBiome.setEnabled(false);

        jLabel9.setText(" ");

        checkBoxInSelection.setText(" ");
        checkBoxInSelection.setEnabled(false);
        checkBoxInSelection.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);

        jLabel4.setText("In selection:");

        jLabel8.setText("automatic");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(labelFluidType)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel13)
                            .addComponent(jLabel12)))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel5)
                            .addComponent(jLabel1)
                            .addComponent(jLabel7)
                            .addComponent(jLabel3))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(labelCoords)
                            .addComponent(labelSlope)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(labelHeight)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel10))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(labelWaterLevel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel15))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(labelWaterDepth)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel17))))
                    .addComponent(jLabel2)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel9))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(checkBoxAutomaticBiome)
                        .addGap(0, 0, 0)
                        .addComponent(jLabel8))
                    .addComponent(labelTerrain)
                    .addComponent(labelBiome)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addGap(0, 0, 0)
                        .addComponent(checkBoxInSelection)))
                .addGap(0, 53, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(labelCoords))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(labelHeight)
                    .addComponent(jLabel10))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(labelSlope))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(labelFluidType)
                    .addComponent(jLabel12)
                    .addComponent(labelWaterLevel)
                    .addComponent(jLabel15))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel13)
                    .addComponent(labelWaterDepth)
                    .addComponent(jLabel17))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(labelTerrain)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel9)
                    .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(labelBiome)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(checkBoxAutomaticBiome)
                    .addComponent(jLabel8))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(checkBoxInSelection)
                    .addComponent(jLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 119, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox checkBoxAutomaticBiome;
    private javax.swing.JCheckBox checkBoxInSelection;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTable jTable1;
    private javax.swing.JLabel labelBiome;
    private javax.swing.JLabel labelCoords;
    private javax.swing.JLabel labelFluidType;
    private javax.swing.JLabel labelHeight;
    private javax.swing.JLabel labelSlope;
    private javax.swing.JLabel labelTerrain;
    private javax.swing.JLabel labelWaterDepth;
    private javax.swing.JLabel labelWaterLevel;
    // End of variables declaration//GEN-END:variables

    private final WorldPainter view;
    private final LayerTableModel tableModel;
//    private final Timer updateTimer;
    private final BiomeHelper biomeHelper;
    private final NumberFormat heightFormatter;
    private Point worldCoords;
    private boolean fieldsClear = true, currentAutomaticBiome, active;
    private Terrain currentTerrain;
    private int currentBiome;

    private static final Set<Layer> HIDDEN_LAYERS = new HashSet<>(Arrays.asList(Biome.INSTANCE, SelectionChunk.INSTANCE,
            SelectionBlock.INSTANCE, FloodWithLava.INSTANCE, NotPresent.INSTANCE));
    private static final Icon ICON_BLANK = IconUtils.loadIcon("org/pepsoft/worldpainter/icons/transparent.png");
    private static final Logger logger = LoggerFactory.getLogger(InfoPanel.class);

    static class LayerTableModel implements TableModel {
        boolean update(Map<Layer, Integer> intensities) {
            boolean changed = false;
            if (rows.isEmpty()) {
                int index = 0;
                for (Map.Entry<Layer, Integer> entry: intensities.entrySet()) {
                    Layer layer = entry.getKey();
                    rows.add(new InfoRow(layer, entry.getValue()));
                    layerIndices.put(layer, index++);
                    TableModelEvent event = new TableModelEvent(this);
                    for (TableModelListener listener: listeners) {
                        listener.tableChanged(event);
                    }
                    changed = true;
                }
            } else if (! intensities.isEmpty()) {
                Set<Layer> oldLayers = new HashSet<>(layerIndices.keySet());
                for (Map.Entry<Layer, Integer> entry: intensities.entrySet()) {
                    Layer layer = entry.getKey();
                    int intensity = entry.getValue();
                    if (layerIndices.containsKey(layer)) {
                        // Layer is already on the list
                        int index = layerIndices.get(layer);
                        InfoRow row = rows.get(index);
                        if (intensity != row.intensity) {
                            row.intensity = intensity;
                            TableModelEvent event = new TableModelEvent(this, index, index, 1);
                            for (TableModelListener listener: listeners) {
                                listener.tableChanged(event);
                            }
                        }
                    } else {
                        // Layer is not on the list; add it (to the end, to
                        // limit the amount of repainting needed
                        int index = rows.size();
                        rows.add(new InfoRow(layer, intensity));
                        layerIndices.put(layer, index);
                        TableModelEvent event = new TableModelEvent(this, index, index, TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT);
                        for (TableModelListener listener: listeners) {
                            listener.tableChanged(event);
                        }
                        changed = true;
                    }
                }
                oldLayers.removeAll(intensities.keySet());
                for (Layer oldLayer: oldLayers) {
                    // Layer removed
                    int index = layerIndices.remove(oldLayer);
                    for (Map.Entry<Layer, Integer> entry: layerIndices.entrySet()) {
                        if (entry.getValue() > index) {
                            entry.setValue(entry.getValue() - 1);
                        }
                    }
                    rows.remove(index);
                    TableModelEvent event = new TableModelEvent(this, index, index, TableModelEvent.ALL_COLUMNS, TableModelEvent.DELETE);
                    for (TableModelListener listener: listeners) {
                        listener.tableChanged(event);
                    }
                    changed = true;
                }
            }
            return changed;
        }

        boolean clear() {
            if (! rows.isEmpty()) {
                rows.clear();
                layerIndices.clear();
                TableModelEvent event = new TableModelEvent(this);
                for (TableModelListener listener: listeners) {
                    listener.tableChanged(event);
                }
                return true;
            } else {
                return false;
            }
        }

        // TableModel

        @Override
        public int getRowCount() {
            return rows.size();
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
            switch (columnIndex) {
                case 0:
                    return Layer.class;
                case 1:
                    return InfoRow.class;
                default:
                    throw new IndexOutOfBoundsException(Integer.toString(columnIndex));
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            InfoRow row = rows.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return row.layer;
                case 1:
                    return row;
                default:
                    throw new IndexOutOfBoundsException(Integer.toString(columnIndex));
            }
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addTableModelListener(TableModelListener l) {
            listeners.add(l);
        }

        @Override
        public void removeTableModelListener(TableModelListener l) {
            listeners.remove(l);
        }

        private final List<InfoRow> rows = new ArrayList<>();
        private final Map<Layer, Integer> layerIndices = new HashMap<>();
        private final List<TableModelListener> listeners = new ArrayList<>();

        private static final String[] COLUMN_NAMES = {"Layer", "Intensity"};

        static class InfoRow {
            InfoRow(Layer layer, int intensity) {
                this.layer = layer;
                this.intensity = intensity;
            }

            Layer layer;
            int intensity;
        }
    }

    static class InfoRowTableCellRenderer extends DefaultTableCellRenderer {
        InfoRowTableCellRenderer(BiomeHelper biomeHelper) {
            this.biomeHelper = biomeHelper;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            LayerTableModel.InfoRow infoRow = (LayerTableModel.InfoRow) value;
            setText(getIntensity(infoRow.layer, infoRow.intensity));
            return this;
        }

        private String getIntensity(Layer layer, int intensity) {
            if (layer instanceof Biome) {
                return biomeHelper.getBiomeName(intensity);
            } else if (layer instanceof Annotations) {
                return org.pepsoft.minecraft.Constants.COLOUR_NAMES[intensity - ((intensity < 8) ? 1 : 0)];
            } else if (layer instanceof GardenCategory) {
                return GardenCategory.getLabel(intensity);
            } else {
                switch (layer.getDataSize()) {
                    case BIT:
                    case BIT_PER_CHUNK:
                        return intensity == 0 ? "off" : "on";
                    case NIBBLE:
                        int strength = (intensity > 0) ? ((intensity - 1) * 100  / 14 + 1): 0;
                        if ((strength == 51) || (strength == 101)) {
                            strength--;
                        }
                        return strength + "%";
                    case BYTE:
                        return (intensity * 100 / 255) + "%";
                    case NONE:
                        return "N/A";
                    default:
                        throw new UnsupportedOperationException();
                }
            }
        }

        private final BiomeHelper biomeHelper;
    }
}