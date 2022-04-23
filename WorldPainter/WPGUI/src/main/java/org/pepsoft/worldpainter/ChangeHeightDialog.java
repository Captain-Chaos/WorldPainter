/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * ChangeHeightDialog.java
 *
 * Created on 30-jan-2012, 17:54:20
 */
package org.pepsoft.worldpainter;

import org.pepsoft.minecraft.Material;
import org.pepsoft.minecraft.SuperflatGenerator;
import org.pepsoft.minecraft.SuperflatPreset;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.util.ProgressReceiver.OperationCancelled;
import org.pepsoft.util.swing.ProgressDialog;
import org.pepsoft.util.swing.ProgressTask;
import org.pepsoft.worldpainter.layers.CustomLayer;
import org.pepsoft.worldpainter.layers.Resources;
import org.pepsoft.worldpainter.layers.exporters.ExporterSettings;
import org.pepsoft.worldpainter.layers.exporters.ResourcesExporter.ResourcesExporterSettings;
import org.pepsoft.worldpainter.plugins.PlatformManager;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import static java.util.Arrays.stream;
import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.util.MathUtils.clamp;
import static org.pepsoft.util.swing.ProgressDialog.NOT_CANCELABLE;
import static org.pepsoft.worldpainter.Constants.*;
import static org.pepsoft.worldpainter.DefaultPlugin.*;
import static org.pepsoft.worldpainter.history.HistoryEntry.*;

/**
 *
 * @author pepijn
 */
@SuppressWarnings({"Convert2Lambda", "Anonymous2MethodRef", "unused", "FieldCanBeLocal"})
public class ChangeHeightDialog extends WorldPainterDialog {
    /** Creates new form ChangeHeightDialog */
    @SuppressWarnings("OptionalGetWithoutIsPresent") // Expected
    public ChangeHeightDialog(Window parent, World2 world) {
        super(parent);
        this.world = world;
        lowestHeight = stream(world.getDimensions()).mapToInt(Dimension::getLowestIntHeight).min().getAsInt();
        highestHeight = stream(world.getDimensions()).mapToInt(Dimension::getHightestIntHeight).max().getAsInt();

        initComponents();
        labelOldExtents.setText(lowestHeight + " - " + highestHeight);
        comboBoxPlatform.setModel(new DefaultComboBoxModel<>(PlatformManager.getInstance().getAllPlatforms().toArray(new Platform[0])));
        final Platform platform = world.getPlatform();

        labelCurrentMinHeight.setText(Integer.toString(platform.minZ));
        final int maxHeight = world.getMaxHeight();
        labelCurrentMaxHeight.setText(Integer.toString(maxHeight));
        comboBoxNewMaxHeight.setSelectedItem(maxHeight);
        
        getRootPane().setDefaultButton(buttonOK);

        scaleToUI();
        pack();
        setLocationRelativeTo(parent);

        setPlatform(platform);
    }

    private void setPlatform(Platform platform) {
        comboBoxPlatform.setSelectedItem(platform);
        comboBoxNewMinHeight.setModel((platform.minZ != 0) ? new DefaultComboBoxModel<>(new Integer[] {platform.minZ, 0}) : new DefaultComboBoxModel<>(new Integer[] {0}));
        comboBoxNewMaxHeight.setModel(new DefaultComboBoxModel<>(stream(platform.maxHeights).boxed().toArray(Integer[]::new)));
        comboBoxNewMinHeight.setSelectedItem(platform.minZ);
        comboBoxNewMaxHeight.setSelectedItem(platform.standardMaxHeight);
        updateLabels();
        setControlStates();
    }

    private void updateLabels() {
        final HeightTransform transform = getTransform();
        final int newLowestHeight = transform.transformHeight(lowestHeight), newHighestHeight = transform.transformHeight(highestHeight);
        final int newMinHeight = (int) comboBoxNewMinHeight.getSelectedItem(), newMaxHeight = (int) comboBoxNewMaxHeight.getSelectedItem();
        boolean activateWarning = false;
        final StringBuilder label = new StringBuilder("<html>");
        if (newLowestHeight < newMinHeight) {
            label.append("<b><color=red>" + newLowestHeight + "</color></b>");
            activateWarning = true;
        } else {
            label.append(newLowestHeight);
        }
        label.append(" - ");
        if (newHighestHeight > newMaxHeight) {
            label.append("<b><color=red>" + newHighestHeight + "</color></b>");
            activateWarning = true;
        } else {
            label.append(newHighestHeight);
        }
        label.append("</html>");
        labelNewExtents.setText(label.toString());
        labelCutOffWarning.setVisible(activateWarning);
    }

    private void setControlStates() {
        final Platform oldPlatform = world.getPlatform(), newPlatform = (Platform) comboBoxPlatform.getSelectedItem();
        final int newMinHeight = (Integer) comboBoxNewMinHeight.getSelectedItem();
        final int oldMaxHeight = world.getMaxHeight(), newMaxHeight = (Integer) comboBoxNewMaxHeight.getSelectedItem();
        final boolean translate = checkBoxTranslate.isSelected(), scale = checkBoxScale.isSelected();
        buttonOK.setEnabled((oldPlatform != newPlatform) || (oldMaxHeight != newMaxHeight) || (translate && ((Integer) spinnerTranslateAmount.getValue() != 0)) || (scale && ((Integer) spinnerScaleAmount.getValue() != 100)));
        spinnerTranslateAmount.setEnabled(translate);
        spinnerScaleAmount.setEnabled(scale);
        if ((newPlatform == DefaultPlugin.JAVA_MCREGION) && (newMaxHeight != DEFAULT_MAX_HEIGHT_MCREGION)) {
            labelWarning.setText("Only with mods!");
            labelWarning.setVisible(true);
        } else if ((newPlatform == JAVA_ANVIL_1_17) && (newMaxHeight > 320)) {
            labelWarning.setText("May impact performance");
            labelWarning.setVisible(true);
        } else {
            labelWarning.setVisible(false);
        }
        checkBoxAdjustLayers.setEnabled((newMinHeight != oldPlatform.minZ) || (newMaxHeight != oldMaxHeight) || translate || scale);
    }
    
    private void doResize() {
        // TODO warn about platform incompatibility?
        final Platform oldPlatform = world.getPlatform(), newPlatform = (Platform) comboBoxPlatform.getSelectedItem();
        final int oldMaxHeight = world.getMaxHeight(), oldMinHeight = world.getPlatform().minZ;
        final int newMaxHeight = (Integer) comboBoxNewMaxHeight.getSelectedItem(), newMinHeight = (Integer) comboBoxNewMinHeight.getSelectedItem();
        if (((newPlatform != oldPlatform) || (newMinHeight != oldMinHeight) || (newMaxHeight != oldMaxHeight)) && (world.getImportedFrom() != null) && (JOptionPane.showConfirmDialog(this, "<html>This world was imported from an existing map!<br>Are you <i>sure</i> you want to retarget it?<br>You will not be able to merge it back to the existing map any more!</html>", "Import from Existing Map", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION)) {
            return;
        }
        changePlatform(newPlatform, checkBoxAdjustLayers.isSelected());
        resizeWorld(world, getTransform(), newMinHeight, newMaxHeight, checkBoxAdjustLayers.isSelected(), this);
        if (newMinHeight != oldMinHeight) {
            world.addHistoryEntry(WORLD_MIN_HEIGHT_CHANGED, newMinHeight);
        }
        if (newMaxHeight != oldMaxHeight) {
            world.addHistoryEntry(WORLD_MAX_HEIGHT_CHANGED, newMaxHeight);
        }
        if (checkBoxTranslate.isSelected()) {
            for (Dimension dimension: world.getDimensions()) {
                world.addHistoryEntry(WORLD_DIMENSION_SHIFTED_VERTICALLY, dimension.getName(), (Integer) spinnerTranslateAmount.getValue());
            }
        }
    }

    private void changePlatform(Platform newPlatform, boolean transformLayers) {
        final Platform oldPlatform = world.getPlatform();
        if (newPlatform != oldPlatform) {
            world.setPlatform(newPlatform);
            if (transformLayers) {
                for (Dimension dim: world.getDimensions()) {
                    if (dim.getGenerator() instanceof SuperflatGenerator) {
                        // Patch some block names TODO are there more (that are commonly used in Superflat presets)?
                        SuperflatPreset settings = ((SuperflatGenerator) dim.getGenerator()).getSettings();
                        if (((oldPlatform == JAVA_MCREGION) || (oldPlatform == JAVA_ANVIL)) && (newPlatform != JAVA_MCREGION) && (newPlatform != JAVA_ANVIL)) {
                            for (SuperflatPreset.Layer layer: settings.getLayers()) {
                                switch (layer.getMaterialName()) {
                                    case "minecraft:grass":
                                        layer.setMaterialName(MC_GRASS_BLOCK);
                                        break;
                                    case "minecraft:snow_layer":
                                        layer.setMaterialName(MC_SNOW);
                                        break;
                                }
                            }
                        } else if ((oldPlatform != JAVA_MCREGION) && (oldPlatform != JAVA_ANVIL) && ((newPlatform == JAVA_MCREGION) || (newPlatform == JAVA_ANVIL))) {
                            for (SuperflatPreset.Layer layer: settings.getLayers()) {
                                switch (layer.getMaterialName()) {
                                    case MC_GRASS_BLOCK:
                                        layer.setMaterialName("minecraft:grass");
                                        break;
                                    case MC_SNOW:
                                        layer.setMaterialName("minecraft:snow_layer");
                                        break;
                                }
                            }

                        }
                    }
                }
            }
            world.addHistoryEntry(WORLD_RETARGETED, oldPlatform.displayName, newPlatform.displayName);
        }
    }

    private HeightTransform getTransform() {
        boolean scale = checkBoxScale.isSelected();
        int scaleAmount = (Integer) spinnerScaleAmount.getValue();
        boolean translate = checkBoxTranslate.isSelected();
        int translateAmount = (Integer) spinnerTranslateAmount.getValue();
        return HeightTransform.get(scale ? scaleAmount : 100, translate ? translateAmount : 0);
    }

    static void resizeWorld(final World2 world, final HeightTransform transform, final int newMinHeight, final int newMaxHeight, final boolean transformLayers, final Window parent) {
        int tileCount = 0;
        for (Dimension dim: world.getDimensions()) {
            dim.setEventsInhibited(true);
            tileCount += dim.getTiles().size();
        }
        final int finalTileCount = tileCount;
        
        try {
            ProgressDialog.executeTask(parent, new ProgressTask<World2>() {
                @Override
                public String getName() {
                    return "Changing world height";
                }

                @Override
                public World2 execute(ProgressReceiver progressReceiver) throws OperationCancelled {
                    int tileNo = 0;
                    for (Dimension dim: world.getDimensions()) {
                        final int oldMinheight = dim.getMinHeight(), oldMaxHeight = dim.getMaxHeight();
                        final int dimNewMinHeight, dimNewMaxHeight;
                        switch (dim.getDim()) {
                            case DIM_NETHER:
                            case DIM_NETHER_CEILING:
                                dimNewMinHeight = Math.max(newMinHeight, 0);
                                dimNewMaxHeight = Math.min(newMaxHeight, DEFAULT_MAX_HEIGHT_NETHER);
                                break;
                            case DIM_END:
                            case DIM_END_CEILING:
                                dimNewMinHeight = Math.max(newMinHeight, 0);
                                dimNewMaxHeight = Math.min(newMaxHeight, DEFAULT_MAX_HEIGHT_END);
                                break;
                            default:
                                dimNewMinHeight = newMinHeight;
                                dimNewMaxHeight = newMaxHeight;
                                break;
                        }
                        if ((dimNewMinHeight == oldMinheight) && (dimNewMaxHeight == oldMaxHeight) && transform.isIdentity()) {
                            // Dimension heights don't need to change
                            continue;
                        }
                        dim.clearUndo();
                        dim.getTiles().forEach(org.pepsoft.worldpainter.Tile::inhibitEvents);
                        try {
                            for (Tile tile: dim.getTiles()) {
                                tile.setMinMaxHeight(dimNewMinHeight, dimNewMaxHeight, transform);
                                tileNo++;
                                progressReceiver.setProgress((float) tileNo / finalTileCount);
                            }
                            dim.setMinHeight(dimNewMinHeight);
                            dim.setMaxHeight(dimNewMaxHeight);
                            TileFactory tileFactory = dim.getTileFactory();
                            if (tileFactory instanceof HeightMapTileFactory) {
                                HeightMapTileFactory heightMapTileFactory = (HeightMapTileFactory) tileFactory;
                                heightMapTileFactory.setMinMaxHeight(dimNewMinHeight, dimNewMaxHeight, transform);
                            }
                            if (transformLayers) {
                                ResourcesExporterSettings resourcesSettings = (ResourcesExporterSettings) dim.getLayerSettings(Resources.INSTANCE);
                                if (resourcesSettings != null) {
                                    // TODO move this to ResourcesExporterSettings, which requires also communicating oldMinHeight and oldMaxHeight somehow
                                    for (Material material: resourcesSettings.getMaterials()) {
                                        int maxLevel = resourcesSettings.getMaxLevel(material);
                                        if (maxLevel == (oldMaxHeight - 1)) {
                                            maxLevel = dimNewMaxHeight - 1;
                                        } else if (maxLevel > 1) {
                                            maxLevel = clamp(dimNewMinHeight, transform.transformHeight(maxLevel), dimNewMaxHeight - 1);
                                        }
                                        // TODO: do the same for minLevels? Or do we WANT those to stay put?
                                        resourcesSettings.setMaxLevel(material, maxLevel);
                                    }
                                }
                                for (ExporterSettings exporterSettings: dim.getAllLayerSettings().values()) {
                                    if (! (exporterSettings instanceof ResourcesExporterSettings)) {
                                        exporterSettings.setMinMaxHeight(dimNewMinHeight, dimNewMaxHeight, transform);
                                    }
                                }
                                for (CustomLayer customLayer: dim.getCustomLayers()) {
                                    customLayer.setMinMaxHeight(dimNewMinHeight, dimNewMaxHeight, transform);
                                }
                                if (dim.getGenerator() instanceof SuperflatGenerator) {
                                    if (dimNewMinHeight != oldMinheight) {
                                        final int raiseBy = oldMinheight - dimNewMinHeight;
                                        final SuperflatPreset settings = ((SuperflatGenerator) dim.getGenerator()).getSettings();
                                        final List<SuperflatPreset.Layer> layers = settings.getLayers();
                                        final int currentHeight = layers.stream().mapToInt(SuperflatPreset.Layer::getThickness).sum();
                                        if (raiseBy > 0) {
                                            // Insert deepslate to raise the Superflat terrain up. Skip the lowest layer
                                            // if that is bedrock, so that bedrock remains at the bottom. If the lowest
                                            // or second-lowest layer is already deepslate: expand that
                                            if (layers.get(0).getMaterialName().equals(MC_BEDROCK)) {
                                                if (layers.get(1).getMaterialName().equals(MC_DEEPSLATE)) {
                                                    layers.get(1).setThickness(layers.get(1).getThickness() + raiseBy);
                                                } else {
                                                    layers.add(1, new SuperflatPreset.Layer(MC_DEEPSLATE, raiseBy));
                                                }
                                            } else if (layers.get(0).getMaterialName().equals(MC_DEEPSLATE)) {
                                                layers.get(0).setThickness(layers.get(0).getThickness() + raiseBy);
                                            } else {
                                                layers.add(0, new SuperflatPreset.Layer(MC_DEEPSLATE, raiseBy));
                                            }
                                        } else {
                                            int lowerBy = -raiseBy;
                                            // Keep reducing the thickness of layers, starting with the bottom one, until we
                                            // have shaved off enough, or we run out of layers
                                            for (SuperflatPreset.Layer layer: layers) {
                                                final int amount = Math.min(lowerBy, layer.getThickness() - 1);
                                                if (amount > 0) {
                                                    layer.setThickness(layer.getThickness() - amount);
                                                    lowerBy -= amount;
                                                    if (lowerBy == 0) {
                                                        break;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            dim.clearUndo();
                            dim.armSavePoint();
                        } finally {
                            dim.getTiles().forEach(org.pepsoft.worldpainter.Tile::releaseEvents);
                        }
                    }
                    world.setMaxHeight(newMaxHeight);
                    return world;
                }
            }, NOT_CANCELABLE);
        } finally {
            for (Dimension dim: world.getDimensions()) {
                dim.setEventsInhibited(false);
            }
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        labelCurrentMaxHeight = new javax.swing.JLabel();
        comboBoxNewMaxHeight = new javax.swing.JComboBox<>();
        buttonCancel = new javax.swing.JButton();
        buttonOK = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        spinnerTranslateAmount = new javax.swing.JSpinner();
        label = new javax.swing.JLabel();
        spinnerScaleAmount = new javax.swing.JSpinner();
        jLabel7 = new javax.swing.JLabel();
        checkBoxScale = new javax.swing.JCheckBox();
        checkBoxTranslate = new javax.swing.JCheckBox();
        jLabel6 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        labelWarning = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        comboBoxPlatform = new javax.swing.JComboBox<>();
        labelCurrentMinHeight = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        comboBoxNewMinHeight = new javax.swing.JComboBox<>();
        jLabel10 = new javax.swing.JLabel();
        checkBoxAdjustLayers = new javax.swing.JCheckBox();
        labelCutOffWarning = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        labelOldExtents = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        labelNewExtents = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Change Map Format");

        jLabel1.setText("Current height limits:");

        jLabel2.setText("New height limits:");

        labelCurrentMaxHeight.setText("jLabel3");

        comboBoxNewMaxHeight.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxNewMaxHeightActionPerformed(evt);
            }
        });

        buttonCancel.setText("Cancel");
        buttonCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCancelActionPerformed(evt);
            }
        });

        buttonOK.setText("OK");
        buttonOK.setEnabled(false);
        buttonOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonOKActionPerformed(evt);
            }
        });

        jLabel5.setText("Terrain and water levels:");

        spinnerTranslateAmount.setModel(new javax.swing.SpinnerNumberModel(0, -127, 127, 1));
        spinnerTranslateAmount.setEnabled(false);
        spinnerTranslateAmount.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerTranslateAmountStateChanged(evt);
            }
        });

        label.setText("blocks");

        spinnerScaleAmount.setModel(new javax.swing.SpinnerNumberModel(100, 1, 9999, 1));
        spinnerScaleAmount.setEnabled(false);
        spinnerScaleAmount.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerScaleAmountStateChanged(evt);
            }
        });

        jLabel7.setText("%");

        checkBoxScale.setText("Scale");
        checkBoxScale.setToolTipText("<html>Scale the levels by the specified percentage;<br>\nlevels that are (still) too low or high will be cut off.</html>");
        checkBoxScale.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                checkBoxScaleStateChanged(evt);
            }
        });
        checkBoxScale.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxScaleActionPerformed(evt);
            }
        });

        checkBoxTranslate.setText("Shift");
        checkBoxTranslate.setToolTipText("<html>Shift the levels up or down by the specified number of blocks;<br>\nnegative means down; levels which are (still) too low or high will be cut off.</html>");
        checkBoxTranslate.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                checkBoxTranslateStateChanged(evt);
            }
        });
        checkBoxTranslate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxTranslateActionPerformed(evt);
            }
        });

        jLabel6.setText("<html><b>Note:</b> this operation cannot be undone!</html>");

        jLabel8.setText("(If both are enabled scale");

        labelWarning.setFont(labelWarning.getFont().deriveFont(labelWarning.getFont().getStyle() | java.awt.Font.BOLD));
        labelWarning.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/error.png"))); // NOI18N
        labelWarning.setText("May impact performance");

        jLabel3.setText("Map format:");

        comboBoxPlatform.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxPlatformActionPerformed(evt);
            }
        });

        labelCurrentMinHeight.setText("jLabel4");

        jLabel9.setText("Minimum");

        comboBoxNewMinHeight.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxNewMinHeightActionPerformed(evt);
            }
        });

        jLabel10.setText("Maximum");

        checkBoxAdjustLayers.setSelected(true);
        checkBoxAdjustLayers.setText("Also apply to theme and layer settings");

        labelCutOffWarning.setFont(labelCutOffWarning.getFont().deriveFont(labelCutOffWarning.getFont().getStyle() | java.awt.Font.BOLD));
        labelCutOffWarning.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/error.png"))); // NOI18N
        labelCutOffWarning.setText("Top and/or bottom cut off!");

        jLabel11.setText("will be applied first, then shift.)");

        jLabel4.setText("Current height range in use:");

        labelOldExtents.setText("-999 - -999");

        jLabel12.setText("New height range in use:");

        labelNewExtents.setText("<html><b>-999 - 999</b></html>");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(labelCutOffWarning)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(buttonOK)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonCancel))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(checkBoxAdjustLayers)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel1)
                                    .addComponent(jLabel2))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel9)
                                    .addComponent(labelCurrentMinHeight)
                                    .addComponent(comboBoxNewMinHeight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(comboBoxNewMaxHeight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(labelWarning))
                                    .addComponent(labelCurrentMaxHeight)
                                    .addComponent(jLabel10)))
                            .addComponent(jLabel5)
                            .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(comboBoxPlatform, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(checkBoxTranslate)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(spinnerScaleAmount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(0, 0, 0)
                                        .addComponent(jLabel7))
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(spinnerTranslateAmount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(0, 0, 0)
                                        .addComponent(label)))
                                .addGap(18, 18, 18)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel11)
                                    .addComponent(jLabel8)))
                            .addComponent(checkBoxScale)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel4)
                                    .addComponent(jLabel12))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(labelNewExtents, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(labelOldExtents))))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(comboBoxPlatform, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(jLabel10))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(labelCurrentMaxHeight)
                    .addComponent(labelCurrentMinHeight))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(comboBoxNewMaxHeight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(labelWarning)
                    .addComponent(comboBoxNewMinHeight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(checkBoxScale)
                    .addComponent(spinnerScaleAmount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel7)
                    .addComponent(jLabel8))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(checkBoxTranslate)
                    .addComponent(spinnerTranslateAmount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(label)
                    .addComponent(jLabel11))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(labelOldExtents))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel12)
                    .addComponent(labelNewExtents, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(checkBoxAdjustLayers)
                .addGap(18, 18, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonCancel)
                    .addComponent(buttonOK)
                    .addComponent(labelCutOffWarning))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void comboBoxNewMaxHeightActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxNewMaxHeightActionPerformed
        updateLabels();
        setControlStates();
    }//GEN-LAST:event_comboBoxNewMaxHeightActionPerformed

    private void buttonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCancelActionPerformed
        cancel();
    }//GEN-LAST:event_buttonCancelActionPerformed

    private void checkBoxScaleStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_checkBoxScaleStateChanged
        setControlStates();
    }//GEN-LAST:event_checkBoxScaleStateChanged

    private void checkBoxTranslateStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_checkBoxTranslateStateChanged
        setControlStates();
    }//GEN-LAST:event_checkBoxTranslateStateChanged

    private void buttonOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonOKActionPerformed
        doResize();
        ok();
    }//GEN-LAST:event_buttonOKActionPerformed

    private void spinnerScaleAmountStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerScaleAmountStateChanged
        updateLabels();
        setControlStates();
    }//GEN-LAST:event_spinnerScaleAmountStateChanged

    private void spinnerTranslateAmountStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerTranslateAmountStateChanged
        updateLabels();
        setControlStates();
    }//GEN-LAST:event_spinnerTranslateAmountStateChanged

    private void comboBoxPlatformActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxPlatformActionPerformed
        setPlatform((Platform) comboBoxPlatform.getSelectedItem());
    }//GEN-LAST:event_comboBoxPlatformActionPerformed

    private void comboBoxNewMinHeightActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxNewMinHeightActionPerformed
        updateLabels();
        setControlStates();
    }//GEN-LAST:event_comboBoxNewMinHeightActionPerformed

    private void checkBoxScaleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxScaleActionPerformed
        updateLabels();
    }//GEN-LAST:event_checkBoxScaleActionPerformed

    private void checkBoxTranslateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxTranslateActionPerformed
        updateLabels();
    }//GEN-LAST:event_checkBoxTranslateActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonCancel;
    private javax.swing.JButton buttonOK;
    private javax.swing.JCheckBox checkBoxAdjustLayers;
    private javax.swing.JCheckBox checkBoxScale;
    private javax.swing.JCheckBox checkBoxTranslate;
    private javax.swing.JComboBox<Integer> comboBoxNewMaxHeight;
    private javax.swing.JComboBox<Integer> comboBoxNewMinHeight;
    private javax.swing.JComboBox<Platform> comboBoxPlatform;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JLabel label;
    private javax.swing.JLabel labelCurrentMaxHeight;
    private javax.swing.JLabel labelCurrentMinHeight;
    private javax.swing.JLabel labelCutOffWarning;
    private javax.swing.JLabel labelNewExtents;
    private javax.swing.JLabel labelOldExtents;
    private javax.swing.JLabel labelWarning;
    private javax.swing.JSpinner spinnerScaleAmount;
    private javax.swing.JSpinner spinnerTranslateAmount;
    // End of variables declaration//GEN-END:variables

    private final World2 world;
    private final int lowestHeight, highestHeight;

    private static final long serialVersionUID = 1L;
}