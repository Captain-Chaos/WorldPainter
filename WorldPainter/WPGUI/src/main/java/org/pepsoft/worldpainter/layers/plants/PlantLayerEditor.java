/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.layers.plants;

import org.pepsoft.minecraft.Material;
import org.pepsoft.util.DesktopUtils;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.biomeschemes.BiomeSchemeManager;
import org.pepsoft.worldpainter.exporting.ExportSettings;
import org.pepsoft.worldpainter.layers.AbstractLayerEditor;
import org.pepsoft.worldpainter.layers.exporters.ExporterSettings;
import org.pepsoft.worldpainter.platforms.JavaExportSettings;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static java.lang.Math.max;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.pepsoft.util.swing.MessageUtils.showInfo;
import static org.pepsoft.worldpainter.Platform.Capability.NAME_BASED;
import static org.pepsoft.worldpainter.layers.plants.Category.*;
import static org.pepsoft.worldpainter.layers.plants.Plants.ALL_PLANTS;
import static org.pepsoft.worldpainter.util.I18nHelper.m;

/**
 *
 * @author Pepijn Schmitz
 */
@SuppressWarnings({"unused", "FieldCanBeLocal"}) // Managed by NetBeans
public class PlantLayerEditor extends AbstractLayerEditor<PlantLayer> {
    /**
     * Creates new form PlantLayerEditor
     */
    public PlantLayerEditor() {
        initComponents();

        fieldName.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                settingsChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                settingsChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                settingsChanged();
            }
        });
    }

    // LayerEditor

    @Override
    public List<Component> getAdditionalButtons() {
        return singletonList(buttonClear);
    }

    @Override
    public PlantLayer createLayer() {
        return new PlantLayer("Plants", "A custom collection of plants", Color.GREEN);
    }

    @Override
    public void setLayer(PlantLayer layer) {
        super.setLayer(layer);
        reset();
    }

    @Override
    public void setContext(LayerEditorContext context) {
        super.setContext(context);
        initPlantControls();
    }

    @Override
    public void commit() {
        if (! isCommitAvailable()) {
            throw new IllegalStateException("Settings invalid or incomplete");
        }
        saveSettings(layer);
    }

    @Override
    public void reset() {
        fieldName.setText(layer.getName());
        paintPicker1.setPaint(layer.getPaint());
        paintPicker1.setOpacity(layer.getOpacity());
        checkBoxGenerateTilledDirt.setSelected(layer.isGenerateFarmland());
        checkBoxOnlyValidBlocks.setSelected(layer.isOnlyOnValidBlocks());
        final Platform platform = context.getDimension().getWorld().getPlatform();
        for (int i = 0; i < ALL_PLANTS.length; i++) {
            PlantLayer.PlantSettings settings = layer.getSettings(i);
            if (! isCompatibleWithPlatform(ALL_PLANTS[i], platform)) {
                // Force incompatible plants to zero, since the user can't edit them to do that
                spinners[i].setValue(0);
                if ((settings != null) && (settings.occurrence > 0)) {
                    // The plant was previously selected, so make that clear by making the label struck through
                    // instead of disabled
                    plantLabels[i].setEnabled(true);
                    plantLabels[i].setText("<html><s>" + ALL_PLANTS[i].getName() + "</s></html>");
                } else {
                    plantLabels[i].setEnabled(false);
                    plantLabels[i].setText(ALL_PLANTS[i].getName());
                }
                plantLabels[i].setToolTipText("This plant is not compatible with the current map format");
            } else if (settings != null) {
                spinners[i].setValue((int) settings.occurrence);
                plantLabels[i].setEnabled(true);
                plantLabels[i].setText(ALL_PLANTS[i].getName());
                plantLabels[i].setToolTipText(null);
                if (growthFromSpinners[i] != null) {
                    growthFromSpinners[i].setValue(settings.growthFrom);
                    growthToSpinners[i].setValue(settings.growthTo);
                }
            } else {
                spinners[i].setValue(0);
                if (growthFromSpinners[i] != null) {
                    growthFromSpinners[i].setValue(max(ALL_PLANTS[i].getDefaultGrowth() / 2, 1));
                    growthToSpinners[i].setValue(ALL_PLANTS[i].getDefaultGrowth());
                }
                plantLabels[i].setEnabled(true);
                plantLabels[i].setText(ALL_PLANTS[i].getName());
                plantLabels[i].setToolTipText(null);
            }
        }
        updatePercentages();
        setControlStates();
    }

    @Override
    public ExporterSettings getSettings() {
        if (! isCommitAvailable()) {
            throw new IllegalStateException("Settings invalid or incomplete");
        }
        final PlantLayer previewLayer = saveSettings(null);
        return new ExporterSettings() {
            @Override
            public boolean isApplyEverywhere() {
                return false;
            }

            @Override
            public PlantLayer getLayer() {
                return previewLayer;
            }

            @Override
            public ExporterSettings clone() {
                throw new UnsupportedOperationException("Not supported");
            }
        };
    }

    @Override
    public boolean isCommitAvailable() {
        return (! fieldName.getText().trim().isEmpty()) && (totalOccurrence > 0L) && (totalOccurrence <= Integer.MAX_VALUE);
    }

    private void initPlantControls() {
        JPanel panel = new JPanel(new GridBagLayout());
        panelPlantControls.add(panel);
        addCategory(panel, PLANTS_AND_FLOWERS);
        addFiller(panel);
        panel = new JPanel(new GridBagLayout());
        panelPlantControls.add(panel);
        addCategory(panel, SAPLINGS);
        addCategory(panel, CROPS);
        addCategory(panel, "Various", MUSHROOMS, CACTUS, SUGAR_CANE, FLOATING_PLANTS, END);
        addFiller(panel);
        panel = new JPanel(new GridBagLayout());
        panelPlantControls.add(panel);
        addCategory(panel, "Water plants", WATER_PLANTS, DRIPLEAF);
        addCategory(panel, NETHER);
        addCategory(panel, "Hanging Plants", "For ceilings and cave/tunnel roofs", HANGING_DRY_PLANTS, HANGING_WATER_PLANTS);
        addFiller(panel);
        panelPlantControls.setPreferredSize(panelPlantControls.getMinimumSize());
    }

    private void addCategory(JPanel panel, Category category) {
        addCategory(panel, m(category), category);
    }

    private void addCategory(JPanel panel, String title, Category... categories) {
        addCategory(panel, title, null, categories);
    }

    private void addCategory(JPanel panel, String title, String subTitle, Category... categories) {
        final boolean newColumn = panel.getComponentCount() == 0;
        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridwidth = newColumn ? 3 : GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.BASELINE_LEADING;
        constraints.insets = new Insets(4, 0, 4, 0);
        panel.add(new JLabel("<html><b>" + title + "</b></html>"), constraints);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        if (newColumn) {
            panel.add(new JLabel("Growth:"), constraints);
        }
        if (subTitle != null) {
            panel.add(new JLabel(subTitle), constraints);
        }
        final Platform platform = context.getDimension().getWorld().getPlatform();
        for (Category category: categories) {
            for (int i = 0; i < ALL_PLANTS.length; i++) {
                final Plant plant = ALL_PLANTS[i];
                if (plant.getCategories()[0] == category) {
                    addPlantRow(panel, plant, i, isCompatibleWithPlatform(plant, platform));
                }
            }
        }
    }

    private boolean isCompatibleWithPlatform(Plant plant, Platform platform) {
        if (! platform.capabilities.contains(NAME_BASED)) {
            for (Material material: plant.getAllMaterials()) {
                if (material.blockType == -1) {
                    return false;
                }
            }
        }
        return true;
    }

    private void addPlantRow(final JPanel panel, final Plant plant, final int index, final boolean enabled) {
        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.BASELINE_LEADING;
        constraints.insets = new Insets(1, 0, 1, 4);
        synchronized (icons) {
            final BufferedImage icon = icons.get(plant.getIconNames()[0]);
            if (icon != null) {
                plantLabels[index] = new JLabel(plant.getName(), new ImageIcon(icon), JLabel.TRAILING);
            } else {
                plantLabels[index] = new JLabel(plant.getName());
            }
        }
        panel.add(plantLabels[index], constraints);

        SpinnerModel spinnerModel = new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1);
        spinners[index] = new JSpinner(spinnerModel);
        ((JSpinner.NumberEditor) spinners[index].getEditor()).getTextField().setColumns(3);
        spinners[index].addChangeListener(percentageListener);
        panel.add(spinners[index], constraints);

        percentageLabels[index] = new JLabel("100%");
        panel.add(percentageLabels[index], constraints);

        if (! enabled) {
            spinners[index].setEnabled(false);
            spinners[index].setToolTipText("This plant is not compatible with the current map format");
        }

        if (plant.getMaxGrowth() > 1) {
            spinnerModel = new SpinnerNumberModel(max(plant.getDefaultGrowth() / 2, 1), 1, plant.getMaxGrowth(), 1);
            growthFromSpinners[index] = new JSpinner(spinnerModel);
            growthFromSpinners[index].addChangeListener(e -> {
                int newValue = (Integer) growthFromSpinners[index].getValue();
                if ((Integer) growthToSpinners[index].getValue() < newValue) {
                    growthToSpinners[index].setValue(newValue);
                }
                settingsChanged();
            });
            panel.add(growthFromSpinners[index], constraints);

            final JLabel dashLabel = new JLabel("-");
            panel.add(dashLabel);

            constraints.gridwidth = GridBagConstraints.REMAINDER;
            spinnerModel = new SpinnerNumberModel(plant.getDefaultGrowth(), 1, plant.getMaxGrowth(), 1);
            growthToSpinners[index] = new JSpinner(spinnerModel);
            growthToSpinners[index].addChangeListener(e -> {
                final int newValue = (Integer) growthToSpinners[index].getValue();
                if ((Integer) growthFromSpinners[index].getValue() > newValue) {
                    growthFromSpinners[index].setValue(newValue);
                }
                settingsChanged();
            });
            panel.add(growthToSpinners[index], constraints);

            if (! enabled) {
                growthFromSpinners[index].setEnabled(false);
                dashLabel.setEnabled(false);
                growthToSpinners[index].setEnabled(false);
            }
        } else {
            constraints.gridwidth = GridBagConstraints.REMAINDER;
            panel.add(new JLabel(), constraints);
        }
    }

    private void addFiller(final JPanel panel) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.weighty = 1.0;
        panel.add(Box.createGlue(), constraints);
    }

    public static void loadIconsInBackground() {
        new Thread("Plant Icon Loader") {
            @Override
            public void run() {
                synchronized (icons) {
                    File resourcesJar = BiomeSchemeManager.getLatestMinecraftJar();
                    if (resourcesJar == null) {
                        logger.warn("Could not find Minecraft jar for loading plant icons");
                        return;
                    } else {
                        logger.info("Loading plant icons from {}", resourcesJar);
                    }
                    try (JarFile jarFile = new JarFile(resourcesJar)) {
                        for (Plant plant: ALL_PLANTS) {
                            // Find an icon
                            final String[] iconNames = plant.getIconNames();
                            for (String iconName: iconNames) {
                                final BufferedImage icon = findIcon(jarFile, iconName);
                                if (icon != null) {
                                    // Then store it under the first icon name (even if it wasn't actually found under
                                    // that name) because we use the first icon name for the icon lookups
                                    icons.put(iconNames[0], icon);
                                    break;
                                }
                            }
                        }
                    } catch (IOException e) {
                        logger.error("I/O error while trying to load plant icons; not loading icons", e);
                    }
                }
            }
        }.start();
    }

    private static BufferedImage findIcon(JarFile jarFile, String name) {
        try {
            JarEntry entry = jarFile.getJarEntry("assets/minecraft/textures/" + name);
            if (entry != null) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Loading plant icon " + name + " from " + jarFile.getName());
                }
                try (InputStream in = jarFile.getInputStream(entry)) {
                    BufferedImage icon = ImageIO.read(in);
                    if (icon.getHeight() > icon.getWidth()) {
                        // Assume this is an animation strip; take the top square of it
                        icon = icon.getSubimage(0, 0, icon.getWidth(), icon.getWidth());
                    }
                    return icon;
                }
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Could not find plant icon " + name + " in Minecraft jar " + jarFile.getName());
                }
                return null;
            }
        } catch (IOException e) {
            logger.error("I/O error while trying to load plant icon " + name + "; continuing without icon", e);
            return null;
        }
    }

    private void updatePercentages() {
        totalOccurrence = 0;
        for (JSpinner spinner: spinners) {
            totalOccurrence += (Integer) spinner.getValue();
        }
        if (normalFont == null) {
            normalFont = plantLabels[0].getFont().deriveFont(Font.PLAIN);
            boldFont = normalFont.deriveFont(Font.BOLD);
        }
        cropsSelected = false;
        for (int i = 0; i < spinners.length; i++) {
            int value = (Integer) spinners[i].getValue();
            if ((value == 0) && percentageLabels[i].isEnabled()) {
                percentageLabels[i].setEnabled(false);
                plantLabels[i].setFont(normalFont);
                percentageLabels[i].setText("   %");
                if ((growthFromSpinners[i] != null) && growthFromSpinners[i].isEnabled()) {
                    growthFromSpinners[i].setEnabled(false);
                    growthToSpinners[i].setEnabled(false);
                }
            } else if (value > 0) {
                if (! percentageLabels[i].isEnabled()) {
                    percentageLabels[i].setEnabled(true);
                    plantLabels[i].setFont(boldFont);
                }
                if (asList(ALL_PLANTS[i].getCategories()).contains(CROPS)) {
                    cropsSelected = true;
                }
                int percentage = (int) (value * 100 / totalOccurrence);
                if (percentage < 10) {
                    percentageLabels[i].setText("  " + percentage + "%");
                } else if (percentage < 100) {
                    percentageLabels[i].setText(" " + percentage + "%");
                } else {
                    percentageLabels[i].setText(percentage + "%");
                }
                if ((growthFromSpinners[i] != null) && (! growthFromSpinners[i].isEnabled())) {
                    growthFromSpinners[i].setEnabled(true);
                    growthToSpinners[i].setEnabled(true);
                }
            }
        }
        settingsChanged();
    }

    private void settingsChanged() {
        setControlStates();
        context.settingsChanged();
    }

    private void setControlStates() {
        checkBoxGenerateTilledDirt.setEnabled(cropsSelected);
    }
    
    private PlantLayer saveSettings(PlantLayer layer) {
        if (layer == null) {
            layer = createLayer();
        }
        layer.setName(fieldName.getText().trim());
        layer.setPaint(paintPicker1.getPaint());
        layer.setOpacity(paintPicker1.getOpacity());
        layer.setGenerateFarmland(checkBoxGenerateTilledDirt.isSelected());
        layer.setOnlyOnValidBlocks(checkBoxOnlyValidBlocks.isSelected());
        for (int i = 0; i < ALL_PLANTS.length; i++) {
            PlantLayer.PlantSettings settings = new PlantLayer.PlantSettings();
            settings.occurrence = (short) ((int) ((Integer) spinners[i].getValue()));
            if (growthFromSpinners[i] != null) {
                settings.growthFrom = (Integer) growthFromSpinners[i].getValue();
                settings.growthTo = (Integer) growthToSpinners[i].getValue();
            } else {
                settings.growthFrom = 1;
                settings.growthTo = 1;
            }
            layer.setSettings(i, settings);
        }
        return layer;
    }
    
    private void clear() {
        for (int i = 0; i < ALL_PLANTS.length; i++) {
            spinners[i].setValue(0);
            if (growthFromSpinners[i] != null) {
                growthFromSpinners[i].setValue(max(ALL_PLANTS[i].getDefaultGrowth() / 2, 1));
                growthToSpinners[i].setValue(ALL_PLANTS[i].getDefaultGrowth());
            }
        }
        updatePercentages();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings({"Convert2Lambda", "Anonymous2MethodRef"}) // Managed by NetBeans
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonClear = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        panelPlantControls = new javax.swing.JPanel();
        checkBoxGenerateTilledDirt = new javax.swing.JCheckBox();
        fieldName = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        checkBoxOnlyValidBlocks = new javax.swing.JCheckBox();
        paintPicker1 = new org.pepsoft.worldpainter.layers.renderers.PaintPicker();

        buttonClear.setText("Clear");
        buttonClear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonClearActionPerformed(evt);
            }
        });

        jLabel1.setText("Name:");

        panelPlantControls.setLayout(new javax.swing.BoxLayout(panelPlantControls, javax.swing.BoxLayout.LINE_AXIS));

        checkBoxGenerateTilledDirt.setSelected(true);
        checkBoxGenerateTilledDirt.setText("turn grass and dirt beneath crops to tilled dirt");
        checkBoxGenerateTilledDirt.setEnabled(false);

        fieldName.setColumns(20);
        fieldName.setText("jTextField1");

        jLabel2.setText("Paint:");

        checkBoxOnlyValidBlocks.setSelected(true);
        checkBoxOnlyValidBlocks.setText("only place on valid blocks");
        checkBoxOnlyValidBlocks.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxOnlyValidBlocksActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(panelPlantControls, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(checkBoxGenerateTilledDirt)
                        .addGap(18, 18, 18)
                        .addComponent(checkBoxOnlyValidBlocks))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(fieldName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(paintPicker1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(0, 0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(fieldName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2)
                    .addComponent(paintPicker1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(checkBoxGenerateTilledDirt)
                    .addComponent(checkBoxOnlyValidBlocks))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelPlantControls, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void buttonClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonClearActionPerformed
        clear();
    }//GEN-LAST:event_buttonClearActionPerformed

    private void checkBoxOnlyValidBlocksActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxOnlyValidBlocksActionPerformed
        if (! checkBoxOnlyValidBlocks.isSelected()) {
            final ExportSettings exportSettings = context.getDimension().getExportSettings();
            if ((exportSettings == null) || ((exportSettings instanceof JavaExportSettings) && ((JavaExportSettings) exportSettings).isRemovePlants())) {
                DesktopUtils.beep();
                showInfo(SwingUtilities.windowForComponent(this), "You must also turn off \"Plants: remove from invalid blocks\"\n" +
                        "on the Post Processing tab of the Export screen! Otherwise\n" +
                        "plants on invalid blocks will be removed during post-\n" +
                        "processing.", "Reminder: Turn Off Remove Plants");
            }
        }
    }//GEN-LAST:event_checkBoxOnlyValidBlocksActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonClear;
    private javax.swing.JCheckBox checkBoxGenerateTilledDirt;
    private javax.swing.JCheckBox checkBoxOnlyValidBlocks;
    private javax.swing.JTextField fieldName;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private org.pepsoft.worldpainter.layers.renderers.PaintPicker paintPicker1;
    private javax.swing.JPanel panelPlantControls;
    // End of variables declaration//GEN-END:variables

    private final JSpinner[] spinners = new JSpinner[ALL_PLANTS.length];
    private final JLabel[] plantLabels = new JLabel[ALL_PLANTS.length], percentageLabels = new JLabel[ALL_PLANTS.length];
    private final JSpinner[] growthFromSpinners = new JSpinner[ALL_PLANTS.length], growthToSpinners = new JSpinner[ALL_PLANTS.length];
    private long totalOccurrence;
    private boolean cropsSelected, initialised;
    private Font normalFont, boldFont;

    private final ChangeListener percentageListener = e -> updatePercentages();

    private static final Map<String, BufferedImage> icons = new HashMap<>();
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PlantLayerEditor.class);
}