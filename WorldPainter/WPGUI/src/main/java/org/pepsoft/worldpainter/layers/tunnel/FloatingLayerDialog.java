/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers.tunnel;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.Dimension.Anchor;
import org.pepsoft.worldpainter.biomeschemes.CustomBiomeManager;
import org.pepsoft.worldpainter.heightMaps.ConstantHeightMap;
import org.pepsoft.worldpainter.heightMaps.NoiseHeightMap;
import org.pepsoft.worldpainter.layers.Biome;
import org.pepsoft.worldpainter.layers.CustomLayer;
import org.pepsoft.worldpainter.layers.LayerTableCellRenderer;
import org.pepsoft.worldpainter.layers.tunnel.TunnelLayer.Mode;
import org.pepsoft.worldpainter.themes.JSpinnerTableCellEditor;
import org.pepsoft.worldpainter.themes.SimpleTheme;
import org.pepsoft.worldpainter.themes.TerrainListCellRenderer;
import org.pepsoft.worldpainter.util.BiomeUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

import static java.util.Collections.singletonMap;
import static org.pepsoft.util.AwtUtils.doLaterOnEventThread;
import static org.pepsoft.util.CollectionUtils.nullAnd;
import static org.pepsoft.worldpainter.Dimension.Role.CAVE_FLOOR;
import static org.pepsoft.worldpainter.Terrain.GRASS;
import static org.pepsoft.worldpainter.biomeschemes.Minecraft1_7Biomes.BIOME_PLAINS;
import static org.pepsoft.worldpainter.layers.tunnel.TunnelLayer.LayerMode.FLOATING;
import static org.pepsoft.worldpainter.layers.tunnel.TunnelLayersTableModel.*;
import static org.pepsoft.worldpainter.themes.Filter.EVERYWHERE;

/**
 *
 * @author SchmitzP
 */
@SuppressWarnings({"unused", "FieldCanBeLocal"}) // Managed by NetBeans
public class FloatingLayerDialog extends TunnelLayerDialog {
    public FloatingLayerDialog(Window parent, Platform platform, TunnelLayer layer, Dimension dimension, boolean extendedBlockIds, ColourScheme colourScheme, CustomBiomeManager customBiomeManager, int minHeight, int maxHeight, int baseHeight, int waterLevel) {
        super(parent, platform, layer, dimension, extendedBlockIds, colourScheme, customBiomeManager, minHeight, maxHeight, baseHeight, waterLevel, false);

        programmaticChange = true;
        try {
            initComponents();

            ((SpinnerNumberModel) spinnerFloorLevel.getModel()).setMinimum(minHeight);
            ((SpinnerNumberModel) spinnerFloorLevel.getModel()).setMaximum(maxHeight - 1);
            comboBoxTerrain.setRenderer(new TerrainListCellRenderer(colourScheme));
            comboBoxTerrain.setModel(new DefaultComboBoxModel<>(Terrain.getConfiguredValues()));
            comboBoxBiome1.setRenderer(new BiomeListCellRenderer(colourScheme, null, platform));
            comboBoxBiome1.setModel(new DefaultComboBoxModel<>(nullAnd(BiomeUtils.getAllBiomes(platform, null)).toArray(new Integer[0])));

            tableRoofLayers.getSelectionModel().addListSelectionListener(this);
            mixedMaterialChooserBottom.setPlatform(platform);
            mixedMaterialChooserBottom.setExtendedBlockIds(extendedBlockIds);
            mixedMaterialChooserBottom.setColourScheme(colourScheme);
            labelPreview.setPreferredSize(new java.awt.Dimension(128, 0));
            ((SpinnerNumberModel) spinnerFloorLevel.getModel()).setMinimum(minHeight);
            ((SpinnerNumberModel) spinnerFloorLevel.getModel()).setMaximum(maxHeight - 1);
            ((SpinnerNumberModel) spinnerBottomLevel.getModel()).setMinimum(minHeight);
            ((SpinnerNumberModel) spinnerBottomLevel.getModel()).setMaximum(maxHeight - 1);
            ((SpinnerNumberModel) spinnerBottomMin.getModel()).setMinimum(minHeight);
            ((SpinnerNumberModel) spinnerBottomMin.getModel()).setMaximum(maxHeight - 1);
            ((SpinnerNumberModel) spinnerBottomMax.getModel()).setMinimum(minHeight);
            ((SpinnerNumberModel) spinnerBottomMax.getModel()).setMaximum(maxHeight - 1);
        } finally {
            programmaticChange = false;
        }

        loadSettings();

        getRootPane().setDefaultButton(buttonOK);

        noiseSettingsEditorBottom.addChangeListener(this);

        // TODO [FLOATING] remove
        jTabbedPane1.setEnabledAt(1, false);
        
        scaleToUI();
        pack();
        scaleWindowToUI();
        setLocationRelativeTo(parent);

        // TODO this causes the preview only to become bigger, never smaller:
//        addComponentListener(new ComponentAdapter() {
//            @Override
//            public void componentResized(ComponentEvent e) {
//                updatePreview();
//            }
//        });
        doLaterOnEventThread(this::updatePreview);
    }

    @Override
    protected void ok() {
        if (tableRoofLayers.isEditing()) {
            tableRoofLayers.getCellEditor().stopCellEditing();
        }
        if (layer.getFloorDimensionId() == null) {
            final Dimension floorDimension = createFloorDimension();
            layer.setFloorDimensionId(floorDimension.getAnchor().id);
            final Configuration config = Configuration.getInstance();
            if (! config.isMessageDisplayedCountAtLeast(PAINT_TUNNEL_LAYER_KEY, 3)) {
                doLaterOnEventThread(() -> JOptionPane.showMessageDialog(App.getInstance(),
                        "Use the paint tools to paint the Floating Dimension in the desired shape.\n" +
                        "Then right-click on the [" + layer.getName() + "] button on the [" + layer.getPalette() + "] panel\n" +
                        "and select \"Edit floating dimension\" to paint on, and vertically shape, the dimension floor."));
                config.setMessageDisplayed(PAINT_TUNNEL_LAYER_KEY);
            }
        }
        saveSettingsTo(layer, true);
        dismiss();
    }

    protected void generatePreview() {
        final TunnelLayer layer = new TunnelLayer("tmp", FLOATING, null, platform);
        saveSettingsTo(layer, false);
        final Insets insets = labelPreview.getInsets();
        final int width = labelPreview.getWidth() - insets.left - insets.right;
        final int height = labelPreview.getHeight() - insets.top - insets.bottom;
        if ((width > 0) && (height > 0)) {
            final int baseHeight = (int) spinnerFloorLevel.getValue();
            final BufferedImage preview = FloatingLayerExporter.generatePreview(dimension, layer, width, height, baseHeight + (int) spinnerFloodLevel1.getValue(), minHeight, baseHeight, (int) spinnerRange.getValue());
            labelPreview.setIcon(new ImageIcon(preview));
        } else {
            labelPreview.setIcon(null);
        }
    }
    
    private void loadSettings() {
        programmaticChange = true;
        try {
            spinnerFloorLevel.setValue(layer.getFloorLevel());

            // The "roof" settings on TunnelLayers double for the bottom of floating layers:
            spinnerBottomLevel.setValue(layer.getRoofLevel());
            spinnerBottomMin.setValue(Math.max(layer.getRoofMin(), minHeight));
            spinnerBottomMax.setValue(Math.min(layer.getRoofMax(), maxHeight - 1));
            mixedMaterialChooserBottom.setMaterial(layer.getRoofMaterial());
            switch (layer.getRoofMode()) {
                case FIXED_HEIGHT:
                    radioButtonBottomFixedLevel.setSelected(true);
                    break;
                case FIXED_HEIGHT_ABOVE_FLOOR:
                    radioButtonBottomFixedHeight.setSelected(true);
                    break;
            }
            NoiseSettings bottomNoise = layer.getRoofNoise();
            if (bottomNoise == null) {
                bottomNoise = new NoiseSettings();
            }
            noiseSettingsEditorBottom.setNoiseSettings(bottomNoise);
            spinnerEdgeWidth.setValue(layer.getRoofWallDepth());
            textFieldName.setText(layer.getName());
            paintPicker1.setPaint(layer.getPaint());
            paintPicker1.setOpacity(layer.getOpacity());

            List<TunnelLayer.LayerSettings> roofLayers = layer.getRoofLayers();
            roofLayersTableModel = new TunnelLayersTableModel(roofLayers, minHeight, maxHeight);
            tableRoofLayers.setModel(roofLayersTableModel);
            tableRoofLayers.getColumnModel().getColumn(COLUMN_NAME).setCellRenderer(new LayerTableCellRenderer());
            SpinnerModel spinnerModel = new SpinnerNumberModel(50, 0, 100, 1);
            tableRoofLayers.getColumnModel().getColumn(COLUMN_INTENSITY).setCellEditor(new JSpinnerTableCellEditor(spinnerModel));
            tableRoofLayers.getColumnModel().getColumn(COLUMN_VARIATION).setCellRenderer(new NoiseSettingsTableCellRenderer());
            spinnerModel = new SpinnerNumberModel(minHeight, minHeight, maxHeight - 1, 1);
            tableRoofLayers.getColumnModel().getColumn(COLUMN_MIN_LEVEL).setCellEditor(new JSpinnerTableCellEditor(spinnerModel));
            spinnerModel = new SpinnerNumberModel(maxHeight - 1, minHeight, maxHeight - 1, 1);
            tableRoofLayers.getColumnModel().getColumn(COLUMN_MAX_LEVEL).setCellEditor(new JSpinnerTableCellEditor(spinnerModel));

            if (layer.getFloorDimensionId() == null) {
                // This is a new layer
                spinnerFloorLevel.setValue(124);
                spinnerRange.setValue(20);
                spinnerScale.setValue(100);
                comboBoxTerrain.setSelectedItem(GRASS);
                comboBoxBiome1.setSelectedItem(BIOME_PLAINS);
                spinnerFloodLevel1.setValue(4);
                checkBoxFloodWithLava1.setSelected(false);
            } else {
                spinnerFloorLevel.setEnabled(false);
                spinnerRange.setEnabled(false);
                spinnerScale.setEnabled(false);
                comboBoxTerrain.setEnabled(false);
                comboBoxBiome1.setEnabled(false);
                spinnerFloodLevel1.setEnabled(false);
                checkBoxFloodWithLava1.setEnabled(false);
            }
        } finally {
            programmaticChange = false;
        }
        
        setControlStates();
    }

    private void saveSettingsTo(TunnelLayer layer, boolean registerMaterials) {
        layer.setFloorLevel((Integer) spinnerFloorLevel.getValue());
        layer.setFloorMode(Mode.CUSTOM_DIMENSION);
        // The "roof" settings double for the bottom of floating layers:
        layer.setRoofLevel((Integer) spinnerBottomLevel.getValue());
        layer.setRoofMin(((Integer) spinnerBottomMin.getValue() <= minHeight) ? Integer.MIN_VALUE : ((Integer) spinnerBottomMin.getValue()));
        layer.setRoofMax(((Integer) spinnerBottomMax.getValue() >= (maxHeight - 1)) ? Integer.MAX_VALUE : ((Integer) spinnerBottomMax.getValue()));
        MixedMaterial bottomMaterial = mixedMaterialChooserBottom.getMaterial();
        if ((bottomMaterial != null) && registerMaterials) {
            // Make sure the material is registered, in case it's new
            bottomMaterial = MixedMaterialManager.getInstance().register(bottomMaterial);
        }
        layer.setRoofMaterial(bottomMaterial);
        if (radioButtonBottomFixedLevel.isSelected()) {
            layer.setRoofMode(Mode.FIXED_HEIGHT);
        } else {
            layer.setRoofMode(Mode.FIXED_HEIGHT_ABOVE_FLOOR);
        }
        final NoiseSettings bottomNoiseSettings = noiseSettingsEditorBottom.getNoiseSettings();
        if (bottomNoiseSettings.getRange() == 0) {
            layer.setRoofNoise(null);
        } else {
            layer.setRoofNoise(bottomNoiseSettings);
        }
        layer.setRoofWallDepth((Integer) spinnerEdgeWidth.getValue());

        layer.setName(textFieldName.getText().trim());
        layer.setPaint(paintPicker1.getPaint());
        layer.setOpacity(paintPicker1.getOpacity());
        
        List<TunnelLayer.LayerSettings> roofLayers = roofLayersTableModel.getLayers();
        layer.setRoofLayers(((roofLayers != null) && (! roofLayers.isEmpty())) ? roofLayers : null);

        layer.setFloodWithLava(checkBoxFloodWithLava1.isSelected());
    }
    
    protected void setControlStates() {
        spinnerBottomMin.setEnabled(! radioButtonBottomFixedLevel.isSelected());
        spinnerBottomMax.setEnabled(! radioButtonBottomFixedLevel.isSelected());

        int selectedRoofRowCount = tableRoofLayers.getSelectedRowCount();
        buttonRemoveRoofLayer.setEnabled(selectedRoofRowCount > 0);
        buttonEditRoofLayer.setEnabled((selectedRoofRowCount == 1) && (roofLayersTableModel.getLayer(tableRoofLayers.getSelectedRow()) instanceof CustomLayer));
    }

    private void removeRoofLayers() {
        if (tableRoofLayers.isEditing()) {
            tableRoofLayers.getCellEditor().stopCellEditing();
        }
        int[] selectedRows = tableRoofLayers.getSelectedRows();
        for (int i = selectedRows.length - 1; i >= 0; i--) {
            roofLayersTableModel.removeLayer(selectedRows[i]);
        }
    }

    private void editRoofLayer() {
        editLayer(tableRoofLayers, roofLayersTableModel);
    }

    private void addRoofLayer() {
        addLayer(buttonAddRoofLayer, roofLayersTableModel);
    }

    private void newRoofLayer() {
        newLayer(buttonNewRoofLayer, roofLayersTableModel);
    }

    private void editRoofLayerVariation() {
        editLayerVariation(tableRoofLayers, roofLayersTableModel);
    }

    private Dimension createFloorDimension() {
        final int minHeight = dimension.getMinHeight();
        final int maxHeight = dimension.getMaxHeight();
        final int dim = dimension.getAnchor().dim;
        final boolean invert = dimension.getAnchor().invert;
        final World2 world = dimension.getWorld();
        final int id = findNextId(world, dim, invert);
        layer.setFloorDimensionId(id);
        final long seed = dimension.getSeed() + id;

        final int floorLevel = (int) spinnerFloorLevel.getValue();
        final int range = (int) spinnerRange.getValue();
        final float scale = ((int) spinnerScale.getValue()) / 100.0f;
        final int waterLevel = floorLevel + (int) spinnerFloodLevel1.getValue();
        final boolean floodWithLava = checkBoxFloodWithLava1.isSelected();
        final Integer biome = (Integer) comboBoxBiome1.getSelectedItem();

        HeightMap heightMap;
        heightMap = new ConstantHeightMap(floorLevel);
        if (range != 0) {
            // Adjust the scale to correspond to what users are used to from the NewWorldDialog:
            // TODO check heights (too high?)
            heightMap = heightMap.plus(new NoiseHeightMap(range, scale, 1, FLOOR_DIMENSION_SEED_OFFSET));
        }
        final SimpleTheme theme = SimpleTheme.createSingleTerrain((Terrain) comboBoxTerrain.getSelectedItem(), minHeight, maxHeight, waterLevel);
        if (biome != null) {
            theme.setLayerMap(singletonMap(EVERYWHERE, Biome.INSTANCE));
            theme.setDiscreteValues(singletonMap(Biome.INSTANCE, biome));
        }
        final TileFactory tileFactory = new HeightMapTileFactory(seed, heightMap, minHeight, maxHeight, floodWithLava, theme);
        final Dimension floorDimension = new Dimension(world, null, seed, tileFactory, new Anchor(dim, CAVE_FLOOR, invert, id));
        world.addDimension(floorDimension);
        layer.updateFloorDimension(dimension, textFieldName.getText() + " Floor");

        // Also update the layer with the same settings, so that e.g. the preview works approximately right
        layer.setFloorLevel(floorLevel);
        layer.setFloodLevel(waterLevel);
        layer.setFloodWithLava(floodWithLava);
        layer.setBiome((biome != null) ? biome : -1);

        return floorDimension;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings({"Convert2Lambda", "Anonymous2MethodRef", "DataFlowIssue"}) // Managed by NetBeans
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup3 = new javax.swing.ButtonGroup();
        buttonGroup4 = new javax.swing.ButtonGroup();
        buttonCancel = new javax.swing.JButton();
        buttonOK = new javax.swing.JButton();
        buttonReset = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        textFieldName = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        spinnerBottomLevel = new javax.swing.JSpinner();
        jLabel12 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        spinnerBottomMin = new javax.swing.JSpinner();
        spinnerBottomMax = new javax.swing.JSpinner();
        jLabel13 = new javax.swing.JLabel();
        noiseSettingsEditorBottom = new org.pepsoft.worldpainter.NoiseSettingsEditor();
        jLabel8 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        spinnerEdgeWidth = new javax.swing.JSpinner();
        jLabel6 = new javax.swing.JLabel();
        radioButtonBottomFixedLevel = new javax.swing.JRadioButton();
        jLabel10 = new javax.swing.JLabel();
        labelPreview = new javax.swing.JLabel();
        radioButtonBottomFixedHeight = new javax.swing.JRadioButton();
        mixedMaterialChooserBottom = new MixedMaterialChooser(true);
        jLabel25 = new javax.swing.JLabel();
        comboBoxBiome1 = new javax.swing.JComboBox<>();
        jLabel3 = new javax.swing.JLabel();
        jLabel27 = new javax.swing.JLabel();
        spinnerFloorLevel = new javax.swing.JSpinner();
        spinnerFloodLevel1 = new javax.swing.JSpinner();
        checkBoxFloodWithLava1 = new javax.swing.JCheckBox();
        jLabel2 = new javax.swing.JLabel();
        comboBoxTerrain = new javax.swing.JComboBox<>();
        jLabel28 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        radioButtonSheerEdge = new javax.swing.JRadioButton();
        radioButtonLinearEdge = new javax.swing.JRadioButton();
        jLabel14 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        radioButtonSmoothEdge = new javax.swing.JRadioButton();
        jLabel19 = new javax.swing.JLabel();
        radioButtonRoundedEdge = new javax.swing.JRadioButton();
        jLabel20 = new javax.swing.JLabel();
        jLabel21 = new javax.swing.JLabel();
        spinnerRange = new javax.swing.JSpinner();
        jLabel22 = new javax.swing.JLabel();
        spinnerScale = new javax.swing.JSpinner();
        jLabel5 = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        jLabel24 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        tableRoofLayers = new javax.swing.JTable();
        buttonNewRoofLayer = new javax.swing.JButton();
        buttonAddRoofLayer = new javax.swing.JButton();
        buttonEditRoofLayer = new javax.swing.JButton();
        buttonRemoveRoofLayer = new javax.swing.JButton();
        paintPicker1 = new org.pepsoft.worldpainter.layers.renderers.PaintPicker();
        jLabel23 = new javax.swing.JLabel();
        jLabel26 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("[PREVIEW] Configure Floating Dimension");

        buttonCancel.setText("Cancel");
        buttonCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCancelActionPerformed(evt);
            }
        });

        buttonOK.setText("OK");
        buttonOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonOKActionPerformed(evt);
            }
        });

        buttonReset.setText("Reset");
        buttonReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonResetActionPerformed(evt);
            }
        });

        jLabel1.setText("Create a floating dimension with the following properties:");

        jLabel4.setText("Name:");

        textFieldName.setColumns(20);
        textFieldName.setText("jTextField1");

        jLabel11.setText("Paint");

        spinnerBottomLevel.setModel(new javax.swing.SpinnerNumberModel(8, 0, 255, 1));
        spinnerBottomLevel.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerBottomLevelStateChanged(evt);
            }
        });

        jLabel12.setText("Edges:");

        jLabel9.setText("Variation:");

        jLabel17.setText(", max:");

        spinnerBottomMin.setModel(new javax.swing.SpinnerNumberModel(0, 0, 255, 1));
        spinnerBottomMin.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerBottomMinStateChanged(evt);
            }
        });

        spinnerBottomMax.setModel(new javax.swing.SpinnerNumberModel(255, 0, 255, 1));
        spinnerBottomMax.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerBottomMaxStateChanged(evt);
            }
        });

        jLabel13.setText("Width:");

        jLabel8.setText("Level:");

        jLabel16.setText("Absolute min:");

        spinnerEdgeWidth.setModel(new javax.swing.SpinnerNumberModel(8, 0, 255, 1));
        spinnerEdgeWidth.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerEdgeWidthStateChanged(evt);
            }
        });

        jLabel6.setText("Bottom:");

        buttonGroup3.add(radioButtonBottomFixedLevel);
        radioButtonBottomFixedLevel.setText("fixed level");
        radioButtonBottomFixedLevel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonBottomFixedLevelActionPerformed(evt);
            }
        });

        jLabel10.setText("Material:");

        labelPreview.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        buttonGroup3.add(radioButtonBottomFixedHeight);
        radioButtonBottomFixedHeight.setSelected(true);
        radioButtonBottomFixedHeight.setText("fixed depth below floor");
        radioButtonBottomFixedHeight.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonBottomFixedHeightActionPerformed(evt);
            }
        });

        mixedMaterialChooserBottom.setEnabled(false);

        jLabel25.setText("Biome:");

        jLabel3.setText("Level:");

        jLabel27.setText("Relative water level:");

        spinnerFloorLevel.setModel(new javax.swing.SpinnerNumberModel(128, -64, 319, 1));
        spinnerFloorLevel.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerFloorLevelStateChanged(evt);
            }
        });

        spinnerFloodLevel1.setModel(new javax.swing.SpinnerNumberModel(4, -384, 384, 1));
        spinnerFloodLevel1.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerFloodLevel1StateChanged(evt);
            }
        });

        checkBoxFloodWithLava1.setText("Lava instead of water:");
        checkBoxFloodWithLava1.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        checkBoxFloodWithLava1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxFloodWithLava1ActionPerformed(evt);
            }
        });

        jLabel2.setText("Surface material:");

        jLabel28.setText("Floor defaults:");

        jLabel7.setText("<html>These are the default settings. They can only be set when creating the<br>layer. Once created, edit the floor dimension to change height and shape.</html>");

        jLabel18.setText("Shape:");

        buttonGroup4.add(radioButtonSheerEdge);
        radioButtonSheerEdge.setText("sheer");
        radioButtonSheerEdge.setEnabled(false);
        radioButtonSheerEdge.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonSheerEdgeActionPerformed(evt);
            }
        });

        buttonGroup4.add(radioButtonLinearEdge);
        radioButtonLinearEdge.setSelected(true);
        radioButtonLinearEdge.setText("linear");
        radioButtonLinearEdge.setEnabled(false);
        radioButtonLinearEdge.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonLinearEdgeActionPerformed(evt);
            }
        });

        jLabel14.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/edge_sheer.png"))); // NOI18N

        jLabel15.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/edge_linear.png"))); // NOI18N

        buttonGroup4.add(radioButtonSmoothEdge);
        radioButtonSmoothEdge.setText("smooth");
        radioButtonSmoothEdge.setEnabled(false);
        radioButtonSmoothEdge.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonSmoothEdgeActionPerformed(evt);
            }
        });

        jLabel19.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/edge_smooth.png"))); // NOI18N

        buttonGroup4.add(radioButtonRoundedEdge);
        radioButtonRoundedEdge.setText("rounded");
        radioButtonRoundedEdge.setEnabled(false);
        radioButtonRoundedEdge.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonRoundedEdgeActionPerformed(evt);
            }
        });

        jLabel20.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/edge_rounded.png"))); // NOI18N

        jLabel21.setText("Hill height:");

        spinnerRange.setModel(new javax.swing.SpinnerNumberModel(20, 1, 255, 1));
        spinnerRange.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerRangeStateChanged(evt);
            }
        });

        jLabel22.setText("Horizontal hill size:");

        spinnerScale.setModel(new javax.swing.SpinnerNumberModel(100, 1, 999, 1));
        spinnerScale.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerScaleStateChanged(evt);
            }
        });

        jLabel5.setText("%");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel12)
                    .addComponent(jLabel6)
                    .addComponent(jLabel28)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel13)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerEdgeWidth, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(radioButtonBottomFixedHeight)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(radioButtonBottomFixedLevel))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel8)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerBottomLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel16)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerBottomMin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, 0)
                                .addComponent(jLabel17)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerBottomMax, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel9)
                                    .addComponent(jLabel10))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(mixedMaterialChooserBottom, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(noiseSettingsEditorBottom, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerFloorLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(comboBoxTerrain, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel25)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(comboBoxBiome1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel27)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerFloodLevel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(checkBoxFloodWithLava1))
                            .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel18)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(radioButtonSheerEdge)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel14)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(radioButtonLinearEdge)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel15)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(radioButtonSmoothEdge)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel19)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(radioButtonRoundedEdge)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel20))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel21)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerRange, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel22)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerScale, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, 0)
                                .addComponent(jLabel5)))))
                .addGap(18, 18, 18)
                .addComponent(labelPreview, javax.swing.GroupLayout.DEFAULT_SIZE, 136, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel28)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel3)
                            .addComponent(spinnerFloorLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel21)
                            .addComponent(spinnerRange, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel22)
                            .addComponent(spinnerScale, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel5))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel27)
                            .addComponent(spinnerFloodLevel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(checkBoxFloodWithLava1))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel2)
                            .addComponent(comboBoxTerrain, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel25)
                            .addComponent(comboBoxBiome1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(radioButtonBottomFixedLevel)
                            .addComponent(radioButtonBottomFixedHeight))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel8)
                            .addComponent(spinnerBottomLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel16)
                            .addComponent(spinnerBottomMin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel17)
                            .addComponent(spinnerBottomMax, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel9)
                            .addComponent(noiseSettingsEditorBottom, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel10)
                            .addComponent(mixedMaterialChooserBottom, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addComponent(jLabel12)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel20)
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(radioButtonSheerEdge)
                                .addComponent(radioButtonLinearEdge)
                                .addComponent(radioButtonSmoothEdge)
                                .addComponent(radioButtonRoundedEdge)
                                .addComponent(jLabel14)
                                .addComponent(jLabel15)
                                .addComponent(jLabel19)
                                .addComponent(jLabel18)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel13)
                            .addComponent(spinnerEdgeWidth, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(labelPreview, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        jTabbedPane1.addTab("Floating Dimension", jPanel1);

        jLabel24.setText("<html>You can add custom layers here which will be rendered on the bottom of the floating dimension:<br> <strong>Note:</strong> these layers will be inverted! This includes Custom Objects.<br> A later release will make this optional.</html>");

        tableRoofLayers.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        tableRoofLayers.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tableRoofLayersMouseClicked(evt);
            }
        });
        jScrollPane2.setViewportView(tableRoofLayers);

        buttonNewRoofLayer.setText("Create New");
        buttonNewRoofLayer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonNewRoofLayerActionPerformed(evt);
            }
        });

        buttonAddRoofLayer.setText("Copy Existing");
        buttonAddRoofLayer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonAddRoofLayerActionPerformed(evt);
            }
        });

        buttonEditRoofLayer.setText("Edit");
        buttonEditRoofLayer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonEditRoofLayerActionPerformed(evt);
            }
        });

        buttonRemoveRoofLayer.setText("Remove");
        buttonRemoveRoofLayer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonRemoveRoofLayerActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel24, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 469, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(buttonAddRoofLayer, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(buttonNewRoofLayer, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(buttonEditRoofLayer, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(buttonRemoveRoofLayer, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel24, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(buttonNewRoofLayer)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonAddRoofLayer)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonEditRoofLayer)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonRemoveRoofLayer)))
                .addContainerGap())
        );

        jTabbedPane1.addTab("Bottom Layers", jPanel3);

        jLabel23.setFont(jLabel23.getFont().deriveFont((jLabel23.getFont().getStyle() | java.awt.Font.ITALIC) | java.awt.Font.BOLD, jLabel23.getFont().getSize()+6));
        jLabel23.setText("LIMITED PREVIEW ");

        jLabel26.setText("<html>This is a limited preview of the floating dimension functionality. It is not finished and<br>may not work correctly. Functionality may change. Please post feedback on the subreddit.</html>");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(buttonReset)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(buttonOK)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonCancel)
                        .addGap(11, 11, 11))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jTabbedPane1)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel1)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(jLabel4)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(textFieldName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(jLabel11)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(paintPicker1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGap(0, 0, Short.MAX_VALUE)))
                        .addContainerGap())
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel23)
                            .addComponent(jLabel26, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel23)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel26, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 475, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(textFieldName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel11)
                    .addComponent(paintPicker1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonCancel)
                    .addComponent(buttonOK)
                    .addComponent(buttonReset))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void buttonOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonOKActionPerformed
        ok();
    }//GEN-LAST:event_buttonOKActionPerformed

    private void buttonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCancelActionPerformed
        cancel();
    }//GEN-LAST:event_buttonCancelActionPerformed

    private void buttonResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonResetActionPerformed
        loadSettings();
        updatePreview();
    }//GEN-LAST:event_buttonResetActionPerformed

    private void buttonRemoveRoofLayerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonRemoveRoofLayerActionPerformed
        removeRoofLayers();
    }//GEN-LAST:event_buttonRemoveRoofLayerActionPerformed

    private void buttonEditRoofLayerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonEditRoofLayerActionPerformed
        editRoofLayer();
    }//GEN-LAST:event_buttonEditRoofLayerActionPerformed

    private void buttonAddRoofLayerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonAddRoofLayerActionPerformed
        addRoofLayer();
    }//GEN-LAST:event_buttonAddRoofLayerActionPerformed

    private void buttonNewRoofLayerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonNewRoofLayerActionPerformed
        newRoofLayer();
    }//GEN-LAST:event_buttonNewRoofLayerActionPerformed

    private void tableRoofLayersMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableRoofLayersMouseClicked
        if ((! evt.isPopupTrigger()) && (evt.getClickCount() == 2)) {
            int column = tableRoofLayers.columnAtPoint(evt.getPoint());
            if (column == COLUMN_NAME) {
                editRoofLayer();
            } else if (column == COLUMN_VARIATION) {
                editRoofLayerVariation();
            }
        }
    }//GEN-LAST:event_tableRoofLayersMouseClicked

    private void radioButtonBottomFixedHeightActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonBottomFixedHeightActionPerformed
        updatePreview();
        setControlStates();
    }//GEN-LAST:event_radioButtonBottomFixedHeightActionPerformed

    private void radioButtonBottomFixedLevelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonBottomFixedLevelActionPerformed
        updatePreview();
        setControlStates();
    }//GEN-LAST:event_radioButtonBottomFixedLevelActionPerformed

    private void spinnerEdgeWidthStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerEdgeWidthStateChanged
        if (! programmaticChange) {
            updatePreview();
        }
    }//GEN-LAST:event_spinnerEdgeWidthStateChanged

    private void spinnerBottomMaxStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerBottomMaxStateChanged
        if (! programmaticChange) {
            if ((Integer) spinnerBottomMax.getValue() < (Integer) spinnerBottomMin.getValue()) {
                spinnerBottomMin.setValue(spinnerBottomMax.getValue());
            }
            updatePreview();
        }
    }//GEN-LAST:event_spinnerBottomMaxStateChanged

    private void spinnerBottomMinStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerBottomMinStateChanged
        if (! programmaticChange) {
            if ((Integer) spinnerBottomMax.getValue() < (Integer) spinnerBottomMin.getValue()) {
                spinnerBottomMax.setValue(spinnerBottomMin.getValue());
            }
            updatePreview();
        }
    }//GEN-LAST:event_spinnerBottomMinStateChanged

    private void spinnerBottomLevelStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerBottomLevelStateChanged
        if (! programmaticChange) {
            updatePreview();
        }
    }//GEN-LAST:event_spinnerBottomLevelStateChanged

    private void radioButtonSheerEdgeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonSheerEdgeActionPerformed
        if (! programmaticChange) {
            updatePreview();
        }
    }//GEN-LAST:event_radioButtonSheerEdgeActionPerformed

    private void radioButtonLinearEdgeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonLinearEdgeActionPerformed
        if (! programmaticChange) {
            updatePreview();
        }
    }//GEN-LAST:event_radioButtonLinearEdgeActionPerformed

    private void radioButtonSmoothEdgeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonSmoothEdgeActionPerformed
        if (! programmaticChange) {
            updatePreview();
        }
    }//GEN-LAST:event_radioButtonSmoothEdgeActionPerformed

    private void radioButtonRoundedEdgeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonRoundedEdgeActionPerformed
        if (! programmaticChange) {
            updatePreview();
        }
    }//GEN-LAST:event_radioButtonRoundedEdgeActionPerformed

    private void spinnerFloorLevelStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerFloorLevelStateChanged
        if (! programmaticChange) {
            updatePreview();
        }
    }//GEN-LAST:event_spinnerFloorLevelStateChanged

    private void spinnerFloodLevel1StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerFloodLevel1StateChanged
        if (! programmaticChange) {
            updatePreview();
        }
    }//GEN-LAST:event_spinnerFloodLevel1StateChanged

    private void checkBoxFloodWithLava1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxFloodWithLava1ActionPerformed
        if (! programmaticChange) {
            updatePreview();
        }
    }//GEN-LAST:event_checkBoxFloodWithLava1ActionPerformed

    private void spinnerRangeStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerRangeStateChanged
        if (! programmaticChange) {
            updatePreview();
        }
    }//GEN-LAST:event_spinnerRangeStateChanged

    private void spinnerScaleStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerScaleStateChanged
        if (! programmaticChange) {
            updatePreview();
        }
    }//GEN-LAST:event_spinnerScaleStateChanged

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonAddRoofLayer;
    private javax.swing.JButton buttonCancel;
    private javax.swing.JButton buttonEditRoofLayer;
    private javax.swing.ButtonGroup buttonGroup3;
    private javax.swing.ButtonGroup buttonGroup4;
    private javax.swing.JButton buttonNewRoofLayer;
    private javax.swing.JButton buttonOK;
    private javax.swing.JButton buttonRemoveRoofLayer;
    private javax.swing.JButton buttonReset;
    private javax.swing.JCheckBox checkBoxFloodWithLava1;
    private javax.swing.JComboBox<Integer> comboBoxBiome1;
    private javax.swing.JComboBox<Terrain> comboBoxTerrain;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JLabel labelPreview;
    private org.pepsoft.worldpainter.MixedMaterialChooser mixedMaterialChooserBottom;
    private org.pepsoft.worldpainter.NoiseSettingsEditor noiseSettingsEditorBottom;
    private org.pepsoft.worldpainter.layers.renderers.PaintPicker paintPicker1;
    private javax.swing.JRadioButton radioButtonBottomFixedHeight;
    private javax.swing.JRadioButton radioButtonBottomFixedLevel;
    private javax.swing.JRadioButton radioButtonLinearEdge;
    private javax.swing.JRadioButton radioButtonRoundedEdge;
    private javax.swing.JRadioButton radioButtonSheerEdge;
    private javax.swing.JRadioButton radioButtonSmoothEdge;
    private javax.swing.JSpinner spinnerBottomLevel;
    private javax.swing.JSpinner spinnerBottomMax;
    private javax.swing.JSpinner spinnerBottomMin;
    private javax.swing.JSpinner spinnerEdgeWidth;
    private javax.swing.JSpinner spinnerFloodLevel1;
    private javax.swing.JSpinner spinnerFloorLevel;
    private javax.swing.JSpinner spinnerRange;
    private javax.swing.JSpinner spinnerScale;
    private javax.swing.JTable tableRoofLayers;
    private javax.swing.JTextField textFieldName;
    // End of variables declaration//GEN-END:variables

    private static final long FLOOR_DIMENSION_SEED_OFFSET = 27981L;
    private static final long serialVersionUID = 1L;
}