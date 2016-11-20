/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * DimensionPropertiesEditor.java
 *
 * Created on 8-jun-2011, 20:56:18
 */
package org.pepsoft.worldpainter;

import org.pepsoft.worldpainter.layers.*;
import org.pepsoft.worldpainter.layers.exporters.AnnotationsExporter.AnnotationsSettings;
import org.pepsoft.worldpainter.layers.exporters.CavernsExporter.CavernsSettings;
import org.pepsoft.worldpainter.layers.exporters.ChasmsExporter.ChasmsSettings;
import org.pepsoft.worldpainter.layers.exporters.FrostExporter.FrostSettings;
import org.pepsoft.worldpainter.layers.exporters.ResourcesExporter.ResourcesExporterSettings;
import org.pepsoft.worldpainter.layers.exporters.TreesExporter.TreeLayerSettings;
import org.pepsoft.worldpainter.themes.SimpleTheme;
import org.pepsoft.worldpainter.themes.TerrainListCellRenderer;

import javax.swing.*;
import javax.swing.JSpinner.NumberEditor;
import javax.swing.event.ListSelectionEvent;
import java.util.*;

import static org.pepsoft.minecraft.Constants.*;

/**
 *
 * @author pepijn
 */
public class DimensionPropertiesEditor extends javax.swing.JPanel {
    /** Creates new form DimensionPropertiesEditor */
    public DimensionPropertiesEditor() {
        initComponents();

        if ((Configuration.getInstance() != null) && Configuration.getInstance().isEasyMode()) {
            jLabel4.setVisible(false);
            radioButtonNoBorder.setVisible(false);
            radioButtonVoidBorder.setVisible(false);
            radioButtonWaterBorder.setVisible(false);
            jLabel5.setVisible(false);
            spinnerBorderLevel.setVisible(false);
            jLabel44.setVisible(false);
            radioButtonLavaBorder.setVisible(false);
            jLabel8.setVisible(false);
            spinnerBorderSize.setVisible(false);
            jLabel9.setVisible(false);
            checkBoxBedrockWall.setVisible(false);
            jLabel7.setVisible(false);
            spinnerMinecraftSeed.setVisible(false);

            checkBoxPopulate.setVisible(false);
            jLabel47.setVisible(false);
        }

        spinnerQuartzChance.setEditor(new NumberEditor(spinnerQuartzChance, "0"));
        spinnerGoldChance.setEditor(new NumberEditor(spinnerGoldChance, "0"));
        spinnerGoldMaxLevel.setEditor(new NumberEditor(spinnerGoldMaxLevel, "0"));
        spinnerIronChance.setEditor(new NumberEditor(spinnerIronChance, "0"));
        spinnerIronMaxLevel.setEditor(new NumberEditor(spinnerIronMaxLevel, "0"));
        spinnerCoalChance.setEditor(new NumberEditor(spinnerCoalChance, "0"));
        spinnerCoalMaxLevel.setEditor(new NumberEditor(spinnerCoalMaxLevel, "0"));
        spinnerLapisChance.setEditor(new NumberEditor(spinnerLapisChance, "0"));
        spinnerLapisMaxLevel.setEditor(new NumberEditor(spinnerLapisMaxLevel, "0"));
        spinnerDiamondChance.setEditor(new NumberEditor(spinnerDiamondChance, "0"));
        spinnerDiamondMaxLevel.setEditor(new NumberEditor(spinnerDiamondMaxLevel, "0"));
        spinnerRedstoneChance.setEditor(new NumberEditor(spinnerRedstoneChance, "0"));
        spinnerRedstoneMaxLevel.setEditor(new NumberEditor(spinnerRedstoneMaxLevel, "0"));
        spinnerWaterChance.setEditor(new NumberEditor(spinnerWaterChance, "0"));
        spinnerWaterMaxLevel.setEditor(new NumberEditor(spinnerWaterMaxLevel, "0"));
        spinnerLavaChance.setEditor(new NumberEditor(spinnerLavaChance, "0"));
        spinnerDirtChance.setEditor(new NumberEditor(spinnerDirtChance, "0"));
        spinnerLavaMaxLevel.setEditor(new NumberEditor(spinnerLavaMaxLevel, "0"));
        spinnerDirtMaxLevel.setEditor(new NumberEditor(spinnerDirtMaxLevel, "0"));
        spinnerGravelChance.setEditor(new NumberEditor(spinnerGravelChance, "0"));
        spinnerGravelMaxLevel.setEditor(new NumberEditor(spinnerGravelMaxLevel, "0"));
        spinnerEmeraldChance.setEditor(new NumberEditor(spinnerEmeraldChance, "0"));
        spinnerEmeraldMaxLevel.setEditor(new NumberEditor(spinnerEmeraldMaxLevel, "0"));
        spinnerGoldMinLevel.setEditor(new NumberEditor(spinnerGoldMinLevel, "0"));
        spinnerIronMinLevel.setEditor(new NumberEditor(spinnerIronMinLevel, "0"));
        spinnerCoalMinLevel.setEditor(new NumberEditor(spinnerCoalMinLevel, "0"));
        spinnerLapisMinLevel.setEditor(new NumberEditor(spinnerLapisMinLevel, "0"));
        spinnerDiamondMinLevel.setEditor(new NumberEditor(spinnerDiamondMinLevel, "0"));
        spinnerEmeraldMinLevel.setEditor(new NumberEditor(spinnerEmeraldMinLevel, "0"));
        spinnerWaterMinLevel.setEditor(new NumberEditor(spinnerWaterMinLevel, "0"));
        spinnerLavaMinLevel.setEditor(new NumberEditor(spinnerLavaMinLevel, "0"));
        spinnerDirtMinLevel.setEditor(new NumberEditor(spinnerDirtMinLevel, "0"));
        spinnerGravelMinLevel.setEditor(new NumberEditor(spinnerGravelMinLevel, "0"));
        spinnerRedstoneMinLevel.setEditor(new NumberEditor(spinnerRedstoneMinLevel, "0"));
        spinnerQuartzMinLevel.setEditor(new NumberEditor(spinnerQuartzMinLevel, "0"));
        spinnerQuartzMaxLevel.setEditor(new NumberEditor(spinnerQuartzMaxLevel, "0"));
        spinnerCavernsMinLevel.setEditor(new NumberEditor(spinnerCavernsMinLevel, "0"));
        spinnerCavernsMaxLevel.setEditor(new NumberEditor(spinnerCavernsMaxLevel, "0"));
        spinnerChasmsMinLevel.setEditor(new NumberEditor(spinnerChasmsMinLevel, "0"));
        spinnerChasmsMaxLevel.setEditor(new NumberEditor(spinnerChasmsMaxLevel, "0"));
        
        addListeners(spinnerGoldMinLevel,  spinnerGoldMaxLevel);
        addListeners(spinnerIronMinLevel,  spinnerIronMaxLevel);
        addListeners(spinnerCoalMinLevel,  spinnerCoalMaxLevel);
        addListeners(spinnerLapisMinLevel,  spinnerLapisMaxLevel);
        addListeners(spinnerDiamondMinLevel, spinnerDiamondMaxLevel);
        addListeners(spinnerEmeraldMinLevel, spinnerEmeraldMaxLevel);
        addListeners(spinnerWaterMinLevel, spinnerWaterMaxLevel);
        addListeners(spinnerLavaMinLevel, spinnerLavaMaxLevel);
        addListeners(spinnerDirtMinLevel, spinnerDirtMaxLevel);
        addListeners(spinnerGravelMinLevel, spinnerGravelMaxLevel);
        addListeners(spinnerRedstoneMinLevel, spinnerRedstoneMaxLevel);
        addListeners(spinnerQuartzMinLevel, spinnerQuartzMaxLevel);
        addListeners(spinnerCavernsMinLevel, spinnerCavernsMaxLevel);
        addListeners(spinnerChasmsMinLevel, spinnerChasmsMaxLevel);

        tableCustomLayers.setDefaultRenderer(CustomLayer.class, new CustomLayersTableCellRenderer());
        tableCustomLayers.getSelectionModel().addListSelectionListener((ListSelectionEvent e) -> setControlStates());
    }

    public void setColourScheme(ColourScheme colourScheme) {
        comboBoxSubsurfaceMaterial.setRenderer(new TerrainListCellRenderer(colourScheme));
        themeEditor.setColourScheme(colourScheme);
    }
    
    public ColourScheme getColourScheme() {
        return themeEditor.getColourScheme();
    }
    
    public void setMode(Mode mode) {
        if (this.mode != null) {
            throw new IllegalStateException("Mode already set");
        }
        this.mode = mode;
        switch (mode) {
            case EXPORT:
                jTabbedPane1.remove(1);
                break;
            case DEFAULT_SETTINGS:
                spinnerMinecraftSeed.setEnabled(false);
                jTabbedPane1.remove(5);
                break;
            case EDITOR:
                jTabbedPane1.remove(5);
                break;
            default:
                throw new IllegalArgumentException("mode " + mode);
        }
    }
    
    public Dimension getDimension() {
        return dimension;
    }

    public void setDimension(Dimension dimension) {
        this.dimension = dimension;
        if (dimension != null) {
            loadSettings();
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        comboBoxSubsurfaceMaterial.setEnabled(enabled);
        checkBoxCavernsEverywhere.setEnabled(enabled);
        checkBoxChasmsEverywhere.setEnabled(enabled);
        checkBoxDeciduousEverywhere.setEnabled(enabled);
        checkBoxFrostEverywhere.setEnabled(enabled);
        checkBoxFloodCaverns.setEnabled(enabled);
        checkBoxCavernsBreakSurface.setEnabled(enabled);
        checkBoxChasmsBreakSurface.setEnabled(enabled);
        checkBoxPineEverywhere.setEnabled(enabled);
        checkBoxJungleEverywhere.setEnabled(enabled);
        checkBoxSwamplandEverywhere.setEnabled(enabled);
        jCheckBox8.setEnabled(enabled);
        checkBoxSmoothSnow.setEnabled(enabled);
        jTabbedPane1.setEnabled(enabled);
        spinnerQuartzChance.setEnabled(enabled);
        spinnerGoldChance.setEnabled(enabled);
        spinnerGoldMaxLevel.setEnabled(enabled);
        spinnerIronChance.setEnabled(enabled);
        spinnerIronMaxLevel.setEnabled(enabled);
        spinnerCoalChance.setEnabled(enabled);
        spinnerCoalMaxLevel.setEnabled(enabled);
        spinnerLapisChance.setEnabled(enabled);
        spinnerLapisMaxLevel.setEnabled(enabled);
        spinnerDiamondChance.setEnabled(enabled);
        spinnerDiamondMaxLevel.setEnabled(enabled);
        spinnerRedstoneChance.setEnabled(enabled);
        spinnerRedstoneMaxLevel.setEnabled(enabled);
        spinnerWaterChance.setEnabled(enabled);
        spinnerWaterMaxLevel.setEnabled(enabled);
        spinnerLavaChance.setEnabled(enabled);
        spinnerDirtChance.setEnabled(enabled);
        spinnerLavaMaxLevel.setEnabled(enabled);
        spinnerDirtMaxLevel.setEnabled(enabled);
        spinnerGravelChance.setEnabled(enabled);
        spinnerGravelMaxLevel.setEnabled(enabled);
        spinnerEmeraldChance.setEnabled(enabled);
        spinnerEmeraldMaxLevel.setEnabled(enabled);
        spinnerGoldMinLevel.setEnabled(enabled);
        spinnerIronMinLevel.setEnabled(enabled);
        spinnerCoalMinLevel.setEnabled(enabled);
        spinnerLapisMinLevel.setEnabled(enabled);
        spinnerDiamondMinLevel.setEnabled(enabled);
        spinnerEmeraldMinLevel.setEnabled(enabled);
        spinnerWaterMinLevel.setEnabled(enabled);
        spinnerLavaMinLevel.setEnabled(enabled);
        spinnerDirtMinLevel.setEnabled(enabled);
        spinnerGravelMinLevel.setEnabled(enabled);
        spinnerRedstoneMinLevel.setEnabled(enabled);
        spinnerQuartzMinLevel.setEnabled(enabled);
        spinnerQuartzMaxLevel.setEnabled(enabled);
        themeEditor.setEnabled(enabled);
        spinnerMinSurfaceDepth.setEnabled(enabled);
        spinnerMaxSurfaceDepth.setEnabled(enabled);
        checkBoxBottomless.setEnabled(enabled);
        spinnerCavernsMinLevel.setEnabled(enabled);
        spinnerCavernsMaxLevel.setEnabled(enabled);
        spinnerChasmsMinLevel.setEnabled(enabled);
        spinnerChasmsMaxLevel.setEnabled(enabled);
        checkBoxCoverSteepTerrain.setEnabled(enabled);
        checkBoxExportAnnotations.setEnabled(enabled);
        checkBoxSnowUnderTrees.setEnabled(enabled);
        setControlStates();
    }
    
    public boolean saveSettings() {
        int maxHeight = dimension.getMaxHeight() - 1;
        
        // terrain ranges
        if ((mode != Mode.EXPORT) && (! themeEditor.save())) {
            jTabbedPane1.setSelectedIndex(1);
            return false;
        }
        
        // general
        int topLayerMinDepth = (Integer) spinnerMinSurfaceDepth.getValue();
        dimension.setTopLayerMinDepth(topLayerMinDepth);
        dimension.setTopLayerVariation((Integer) spinnerMaxSurfaceDepth.getValue() - topLayerMinDepth);
        dimension.setSubsurfaceMaterial((Terrain) comboBoxSubsurfaceMaterial.getSelectedItem());
        dimension.setBorder(getSelectedBorder());
        dimension.setBorderLevel((Integer) spinnerBorderLevel.getValue());
        dimension.setBorderSize((Integer) spinnerBorderSize.getValue() / 128);
        dimension.setBedrockWall(checkBoxBedrockWall.isSelected());
        long previousSeed = dimension.getMinecraftSeed();
        long newSeed = ((Number) spinnerMinecraftSeed.getValue()).longValue();
        if (newSeed != previousSeed) {
            dimension.setMinecraftSeed(newSeed);
        }
        dimension.setBottomless(checkBoxBottomless.isSelected());
        dimension.setCoverSteepTerrain(checkBoxCoverSteepTerrain.isSelected());
        dimension.setCeilingHeight((Integer) spinnerCeilingHeight.getValue());

        // Minecraft world border
        World2 world = dimension.getWorld();
        if (world != null) {
            World2.BorderSettings borderSettings = world.getBorderSettings();
            borderSettings.setCentreX((Integer) spinnerMcBorderCentreX.getValue());
            borderSettings.setCentreY((Integer) spinnerMcBorderCentreY.getValue());
            borderSettings.setSize((Integer) spinnerMcBorderSize.getValue());
//            borderSettings.setSafeZone((Integer) spinnerMcBorderBuffer.getValue());
//            borderSettings.setDamagePerBlock((Float) spinnerMcBorderDamage.getValue());
//            borderSettings.setWarningTime((Integer) spinnerMcBorderWarningTime.getValue());
//            borderSettings.setWarningBlocks((Integer) spinnerMcBorderWarningDistance.getValue());
        }
        
        // caverns
        CavernsSettings cavernsSettings = (CavernsSettings) dimension.getLayerSettings(Caverns.INSTANCE);
        if (cavernsSettings == null) {
            cavernsSettings = new CavernsSettings();
        }
        if (checkBoxCavernsEverywhere.isSelected()) {
            int cavernsEverywhereLevel = sliderCavernsEverywhereLevel.getValue();
            cavernsSettings.setCavernsEverywhereLevel(cavernsEverywhereLevel);
        } else {
            cavernsSettings.setCavernsEverywhereLevel(0);
        }
        if (checkBoxFloodCaverns.isSelected()) {
            cavernsSettings.setWaterLevel((Integer) spinnerCavernsFloodLevel.getValue());
        } else {
            cavernsSettings.setWaterLevel(0);
        }
        cavernsSettings.setFloodWithLava(checkBoxCavernsFloodWithLava.isSelected());
        cavernsSettings.setSurfaceBreaking(checkBoxCavernsBreakSurface.isSelected());
        cavernsSettings.setLeaveWater(! checkBoxCavernsRemoveWater.isSelected());
        cavernsSettings.setMinimumLevel((Integer) spinnerCavernsMinLevel.getValue());
        int cavernsMaxLevel = (Integer) spinnerCavernsMaxLevel.getValue();
        cavernsSettings.setMaximumLevel((cavernsMaxLevel >= maxHeight) ? Integer.MAX_VALUE : cavernsMaxLevel);
        dimension.setLayerSettings(Caverns.INSTANCE, cavernsSettings);
        
        // chasms
        ChasmsSettings chasmsSettings = (ChasmsSettings) dimension.getLayerSettings(Chasms.INSTANCE);
        if (chasmsSettings == null) {
            chasmsSettings = new ChasmsSettings();
        }
        if (checkBoxChasmsEverywhere.isSelected()) {
            int chasmsEverywhereLevel = sliderChasmsEverywhereLevel.getValue();
            chasmsSettings.setChasmsEverywhereLevel(chasmsEverywhereLevel);
        } else {
            chasmsSettings.setChasmsEverywhereLevel(0);
        }
        chasmsSettings.setSurfaceBreaking(checkBoxChasmsBreakSurface.isSelected());
        // Other settings copied from Caverns layer
        if (checkBoxFloodCaverns.isSelected()) {
            chasmsSettings.setWaterLevel((Integer) spinnerCavernsFloodLevel.getValue());
        } else {
            chasmsSettings.setWaterLevel(0);
        }
        chasmsSettings.setFloodWithLava(checkBoxCavernsFloodWithLava.isSelected());
        chasmsSettings.setLeaveWater(! checkBoxCavernsRemoveWater.isSelected());
        chasmsSettings.setMinimumLevel((Integer) spinnerChasmsMinLevel.getValue());
        chasmsSettings.setMaximumLevel((Integer) spinnerChasmsMaxLevel.getValue());
        dimension.setLayerSettings(Chasms.INSTANCE, chasmsSettings);
        
        // populate
        dimension.setPopulate(checkBoxPopulate.isSelected());
        
        // deciduous
        TreeLayerSettings<DeciduousForest> deciduousSettings = (TreeLayerSettings<DeciduousForest>) dimension.getLayerSettings(DeciduousForest.INSTANCE);
        if (deciduousSettings == null) {
            deciduousSettings = new TreeLayerSettings<>(DeciduousForest.INSTANCE);
        }
        if (checkBoxDeciduousEverywhere.isSelected()) {
            int minimumLevel = sliderDeciduousLevel.getValue();
            deciduousSettings.setMinimumLevel(minimumLevel);
        } else {
            deciduousSettings.setMinimumLevel(0);
        }
        dimension.setLayerSettings(DeciduousForest.INSTANCE, deciduousSettings);
        
        // pine
        TreeLayerSettings<PineForest> pineSettings = (TreeLayerSettings<PineForest>) dimension.getLayerSettings(PineForest.INSTANCE);
        if (pineSettings == null) {
            pineSettings = new TreeLayerSettings<>(PineForest.INSTANCE);
        }
        if (checkBoxPineEverywhere.isSelected()) {
            int minimumLevel = sliderPineLevel.getValue();
            pineSettings.setMinimumLevel(minimumLevel);
        } else {
            pineSettings.setMinimumLevel(0);
        }
        dimension.setLayerSettings(PineForest.INSTANCE, pineSettings);
        
        // jungle
        TreeLayerSettings<Jungle> jungleSettings = (TreeLayerSettings<Jungle>) dimension.getLayerSettings(Jungle.INSTANCE);
        if (jungleSettings == null) {
            jungleSettings = new TreeLayerSettings<>(Jungle.INSTANCE);
        }
        if (checkBoxJungleEverywhere.isSelected()) {
            int minimumLevel = sliderJungleLevel.getValue();
            jungleSettings.setMinimumLevel(minimumLevel);
        } else {
            jungleSettings.setMinimumLevel(0);
        }
        dimension.setLayerSettings(Jungle.INSTANCE, jungleSettings);
        
        // swampland
        TreeLayerSettings<SwampLand> swampLandSettings = (TreeLayerSettings<SwampLand>) dimension.getLayerSettings(SwampLand.INSTANCE);
        if (swampLandSettings == null) {
            swampLandSettings = new TreeLayerSettings<>(SwampLand.INSTANCE);
        }
        if (checkBoxSwamplandEverywhere.isSelected()) {
            int minimumLevel = jSlider6.getValue();
            swampLandSettings.setMinimumLevel(minimumLevel);
        } else {
            swampLandSettings.setMinimumLevel(0);
        }
        dimension.setLayerSettings(SwampLand.INSTANCE, swampLandSettings);
        
        // frost
        FrostSettings frostSettings = (FrostSettings) dimension.getLayerSettings(Frost.INSTANCE);
        if (frostSettings == null) {
            frostSettings = new FrostSettings();
        }
        frostSettings.setFrostEverywhere(checkBoxFrostEverywhere.isSelected());
        if ((! checkBoxSmoothSnow.isSelected()) || (frostSettings.getMode() != 3)) {
            frostSettings.setMode(checkBoxSmoothSnow.isSelected() ? 2 : 0);
        }
        frostSettings.setSnowUnderTrees(checkBoxSnowUnderTrees.isSelected());
        dimension.setLayerSettings(Frost.INSTANCE, frostSettings);
        
        // resources
        ResourcesExporterSettings resourcesSettings = (ResourcesExporterSettings) dimension.getLayerSettings(Resources.INSTANCE);
        if (resourcesSettings == null) {
            resourcesSettings = new ResourcesExporterSettings(dimension.getMaxHeight());
        }
        if (jCheckBox8.isSelected()) {
            int minimumLevel = jSlider4.getValue();
            resourcesSettings.setMinimumLevel(minimumLevel);
        } else {
            resourcesSettings.setMinimumLevel(0);
        }
        resourcesSettings.setChance(BLK_GOLD_ORE, (Integer) spinnerGoldChance.getValue());
        resourcesSettings.setMinLevel(BLK_GOLD_ORE, (Integer) spinnerGoldMinLevel.getValue());
        resourcesSettings.setMaxLevel(BLK_GOLD_ORE, (Integer) spinnerGoldMaxLevel.getValue());
        resourcesSettings.setChance(BLK_IRON_ORE, (Integer) spinnerIronChance.getValue());
        resourcesSettings.setMinLevel(BLK_IRON_ORE, (Integer) spinnerIronMinLevel.getValue());
        resourcesSettings.setMaxLevel(BLK_IRON_ORE, (Integer) spinnerIronMaxLevel.getValue());
        resourcesSettings.setChance(BLK_COAL, (Integer) spinnerCoalChance.getValue());
        resourcesSettings.setMinLevel(BLK_COAL, (Integer) spinnerCoalMinLevel.getValue());
        resourcesSettings.setMaxLevel(BLK_COAL, (Integer) spinnerCoalMaxLevel.getValue());
        resourcesSettings.setChance(BLK_LAPIS_LAZULI_ORE, (Integer) spinnerLapisChance.getValue());
        resourcesSettings.setMinLevel(BLK_LAPIS_LAZULI_ORE, (Integer) spinnerLapisMinLevel.getValue());
        resourcesSettings.setMaxLevel(BLK_LAPIS_LAZULI_ORE, (Integer) spinnerLapisMaxLevel.getValue());
        resourcesSettings.setChance(BLK_DIAMOND_ORE, (Integer) spinnerDiamondChance.getValue());
        resourcesSettings.setMinLevel(BLK_DIAMOND_ORE, (Integer) spinnerDiamondMinLevel.getValue());
        resourcesSettings.setMaxLevel(BLK_DIAMOND_ORE, (Integer) spinnerDiamondMaxLevel.getValue());
        resourcesSettings.setChance(BLK_REDSTONE_ORE, (Integer) spinnerRedstoneChance.getValue());
        resourcesSettings.setMinLevel(BLK_REDSTONE_ORE, (Integer) spinnerRedstoneMinLevel.getValue());
        resourcesSettings.setMaxLevel(BLK_REDSTONE_ORE, (Integer) spinnerRedstoneMaxLevel.getValue());
        resourcesSettings.setChance(BLK_WATER, (Integer) spinnerWaterChance.getValue());
        resourcesSettings.setMinLevel(BLK_WATER, (Integer) spinnerWaterMinLevel.getValue());
        resourcesSettings.setMaxLevel(BLK_WATER, (Integer) spinnerWaterMaxLevel.getValue());
        resourcesSettings.setChance(BLK_LAVA, (Integer) spinnerLavaChance.getValue());
        resourcesSettings.setMinLevel(BLK_LAVA, (Integer) spinnerLavaMinLevel.getValue());
        resourcesSettings.setMaxLevel(BLK_LAVA, (Integer) spinnerLavaMaxLevel.getValue());
        resourcesSettings.setChance(BLK_DIRT, (Integer) spinnerDirtChance.getValue());
        resourcesSettings.setMinLevel(BLK_DIRT, (Integer) spinnerDirtMinLevel.getValue());
        resourcesSettings.setMaxLevel(BLK_DIRT, (Integer) spinnerDirtMaxLevel.getValue());
        resourcesSettings.setChance(BLK_GRAVEL, (Integer) spinnerGravelChance.getValue());
        resourcesSettings.setMinLevel(BLK_GRAVEL, (Integer) spinnerGravelMinLevel.getValue());
        resourcesSettings.setMaxLevel(BLK_GRAVEL, (Integer) spinnerGravelMaxLevel.getValue());
        resourcesSettings.setChance(BLK_EMERALD_ORE, (Integer) spinnerEmeraldChance.getValue());
        resourcesSettings.setMinLevel(BLK_EMERALD_ORE, (Integer) spinnerEmeraldMinLevel.getValue());
        resourcesSettings.setMaxLevel(BLK_EMERALD_ORE, (Integer) spinnerEmeraldMaxLevel.getValue());
        resourcesSettings.setChance(BLK_QUARTZ_ORE, (Integer) spinnerQuartzChance.getValue());
        resourcesSettings.setMinLevel(BLK_QUARTZ_ORE, (Integer) spinnerQuartzMinLevel.getValue());
        resourcesSettings.setMaxLevel(BLK_QUARTZ_ORE, (Integer) spinnerQuartzMaxLevel.getValue());
        dimension.setLayerSettings(Resources.INSTANCE, resourcesSettings);
        
        // annotations
        AnnotationsSettings annotationsSettings = (AnnotationsSettings) dimension.getLayerSettings(Annotations.INSTANCE);
        if (annotationsSettings == null) {
            annotationsSettings = new AnnotationsSettings();
        }
        annotationsSettings.setExport(checkBoxExportAnnotations.isSelected());
        dimension.setLayerSettings(Annotations.INSTANCE, annotationsSettings);
        
        // custom layers
        if ((mode == Mode.EXPORT) && (! customLayersTableModel.isPristine())) {
            customLayersTableModel.save();
            dimension.setDirty(true);
        }
        
        return true;
    }

    public void addBorderListener(BorderListener borderListener) {
        borderListeners.add(borderListener);
    }

    public void removeBorderListener(BorderListener borderListener) {
        borderListeners.remove(borderListener);
    }

    public boolean isPopulateSelected() {
        return checkBoxPopulate.isSelected();
    }

    private Dimension.Border getSelectedBorder() {
        if (radioButtonLavaBorder.isSelected()) {
            return radioButtonEndlessBorder.isSelected() ? Dimension.Border.ENDLESS_LAVA : Dimension.Border.LAVA;
        } else if (radioButtonNoBorder.isSelected()) {
            return null;
        } else if (radioButtonVoidBorder.isSelected()) {
            return radioButtonEndlessBorder.isSelected() ? Dimension.Border.ENDLESS_VOID : Dimension.Border.VOID;
        } else {
            return radioButtonEndlessBorder.isSelected() ? Dimension.Border.ENDLESS_WATER : Dimension.Border.WATER;
        }
    }

    private void loadSettings() {
        int maxHeight = dimension.getMaxHeight() - 1;
        
        // general
        ((SpinnerNumberModel) spinnerMinSurfaceDepth.getModel()).setMaximum(maxHeight);
        spinnerMinSurfaceDepth.setValue(dimension.getTopLayerMinDepth());
        ((SpinnerNumberModel) spinnerMaxSurfaceDepth.getModel()).setMaximum(maxHeight);
        spinnerMaxSurfaceDepth.setValue(dimension.getTopLayerMinDepth() + dimension.getTopLayerVariation());
        if (dimension.getBorder() != null) {
            switch (dimension.getBorder()) {
                case LAVA:
                    radioButtonLavaBorder.setSelected(true);
                    radioButtonFixedBorder.setSelected(true);
                    break;
                case WATER:
                    radioButtonWaterBorder.setSelected(true);
                    radioButtonFixedBorder.setSelected(true);
                    break;
                case VOID:
                    radioButtonVoidBorder.setSelected(true);
                    radioButtonFixedBorder.setSelected(true);
                    break;
                case ENDLESS_LAVA:
                    radioButtonLavaBorder.setSelected(true);
                    radioButtonEndlessBorder.setSelected(true);
                    break;
                case ENDLESS_WATER:
                    radioButtonWaterBorder.setSelected(true);
                    radioButtonEndlessBorder.setSelected(true);
                    break;
                case ENDLESS_VOID:
                    radioButtonVoidBorder.setSelected(true);
                    radioButtonEndlessBorder.setSelected(true);
                    break;
                default:
                    throw new InternalError();
            }
        } else {
            radioButtonNoBorder.setSelected(true);
        }
        ((SpinnerNumberModel) spinnerBorderLevel.getModel()).setMaximum(maxHeight);
        spinnerBorderLevel.setValue(dimension.getBorderLevel());
        spinnerBorderSize.setValue(dimension.getBorderSize() * 128);
        checkBoxBedrockWall.setSelected(dimension.isBedrockWall());
        spinnerMinecraftSeed.setValue(dimension.getMinecraftSeed());
        checkBoxBottomless.setSelected(dimension.isBottomless());
        checkBoxCoverSteepTerrain.setSelected(dimension.isCoverSteepTerrain());
        ((SpinnerNumberModel) spinnerCeilingHeight.getModel()).setMaximum(maxHeight + 1);
        spinnerCeilingHeight.setValue(dimension.getCeilingHeight());

        // Minecraft world border
        World2 world = dimension.getWorld();
        if (world != null) {
            World2.BorderSettings borderSettings = world.getBorderSettings();
            spinnerMcBorderCentreX.setValue(borderSettings.getCentreX());
            spinnerMcBorderCentreY.setValue(borderSettings.getCentreY());
            spinnerMcBorderSize.setValue(borderSettings.getSize());
//            spinnerMcBorderBuffer.setValue(borderSettings.getSafeZone());
//            spinnerMcBorderDamage.setValue(borderSettings.getDamagePerBlock());
//            spinnerMcBorderWarningTime.setValue(borderSettings.getWarningTime());
//            spinnerMcBorderWarningDistance.setValue(borderSettings.getWarningBlocks());
        } else {
            spinnerMcBorderCentreX.setEnabled(false);
            spinnerMcBorderCentreY.setEnabled(false);
            spinnerMcBorderSize.setEnabled(false);
//            spinnerMcBorderBuffer.setEnabled(false);
//            spinnerMcBorderDamage.setEnabled(false);
//            spinnerMcBorderWarningTime.setEnabled(false);
//            spinnerMcBorderWarningDistance.setEnabled(false);
        }
        
        List<Terrain> materialList = new ArrayList<>(Arrays.asList(Terrain.VALUES));
        for (Iterator<Terrain> i = materialList.iterator(); i.hasNext(); ) {
            Terrain terrain = i.next();
            if ((terrain.isCustom() && (! terrain.isConfigured())) || (terrain == Terrain.GRASS) || (terrain == Terrain.DESERT) || (terrain == Terrain.RED_DESERT)) {
                i.remove();
            }
        }
        comboBoxSubsurfaceMaterial.setModel(new DefaultComboBoxModel(materialList.toArray()));
        comboBoxSubsurfaceMaterial.setSelectedItem(dimension.getSubsurfaceMaterial());

        // caverns
        CavernsSettings cavernsSettings = (CavernsSettings) dimension.getLayerSettings(Caverns.INSTANCE);
        if (cavernsSettings == null) {
            cavernsSettings = new CavernsSettings();
        }
        if (cavernsSettings.getCavernsEverywhereLevel() > 0) {
            checkBoxCavernsEverywhere.setSelected(true);
            sliderCavernsEverywhereLevel.setValue(cavernsSettings.getCavernsEverywhereLevel());
        } else {
            checkBoxCavernsEverywhere.setSelected(false);
            sliderCavernsEverywhereLevel.setValue(8);
        }
        ((SpinnerNumberModel) spinnerCavernsFloodLevel.getModel()).setMaximum(maxHeight);
        if (cavernsSettings.getWaterLevel() > 0) {
            checkBoxFloodCaverns.setSelected(true);
            spinnerCavernsFloodLevel.setValue(cavernsSettings.getWaterLevel());
        } else {
            checkBoxFloodCaverns.setSelected(false);
            spinnerCavernsFloodLevel.setValue(8);
        }
        checkBoxCavernsFloodWithLava.setSelected(cavernsSettings.isFloodWithLava());
        checkBoxCavernsBreakSurface.setSelected(cavernsSettings.isSurfaceBreaking());
        checkBoxCavernsRemoveWater.setSelected(! cavernsSettings.isLeaveWater());
        ((SpinnerNumberModel) spinnerCavernsMinLevel.getModel()).setMaximum(maxHeight);
        spinnerCavernsMinLevel.setValue(cavernsSettings.getMinimumLevel());
        ((SpinnerNumberModel) spinnerCavernsMaxLevel.getModel()).setMaximum(maxHeight);
        spinnerCavernsMaxLevel.setValue(Math.min(cavernsSettings.getMaximumLevel(), maxHeight));
        
        // chasms
        ChasmsSettings chasmsSettings = (ChasmsSettings) dimension.getLayerSettings(Chasms.INSTANCE);
        if (chasmsSettings == null) {
            chasmsSettings = new ChasmsSettings();
        }
        if (chasmsSettings.getChasmsEverywhereLevel() > 0) {
            checkBoxChasmsEverywhere.setSelected(true);
            sliderChasmsEverywhereLevel.setValue(chasmsSettings.getChasmsEverywhereLevel());
        } else {
            checkBoxChasmsEverywhere.setSelected(false);
            sliderChasmsEverywhereLevel.setValue(8);
        }
        checkBoxChasmsBreakSurface.setSelected(chasmsSettings.isSurfaceBreaking());
        ((SpinnerNumberModel) spinnerChasmsMinLevel.getModel()).setMaximum(maxHeight);
        spinnerChasmsMinLevel.setValue(chasmsSettings.getMinimumLevel());
        ((SpinnerNumberModel) spinnerChasmsMaxLevel.getModel()).setMaximum(maxHeight);
        spinnerChasmsMaxLevel.setValue(Math.min(chasmsSettings.getMaximumLevel(), maxHeight));
        
        // populate
        checkBoxPopulate.setSelected(dimension.isPopulate());
        
        // deciduous
        TreeLayerSettings<DeciduousForest> deciduousSettings = (TreeLayerSettings<DeciduousForest>) dimension.getLayerSettings(DeciduousForest.INSTANCE);
        if (deciduousSettings == null) {
            deciduousSettings = new TreeLayerSettings<>(DeciduousForest.INSTANCE);
        }
        if (deciduousSettings.getMinimumLevel() > 0) {
            checkBoxDeciduousEverywhere.setSelected(true);
            sliderDeciduousLevel.setValue(deciduousSettings.getMinimumLevel());
        } else {
            checkBoxDeciduousEverywhere.setSelected(false);
            sliderDeciduousLevel.setValue(8);
        }
        
        // pine
        TreeLayerSettings<PineForest> pineSettings = (TreeLayerSettings<PineForest>) dimension.getLayerSettings(PineForest.INSTANCE);
        if (pineSettings == null) {
            pineSettings = new TreeLayerSettings<>(PineForest.INSTANCE);
        }
        if (pineSettings.getMinimumLevel() > 0) {
            checkBoxPineEverywhere.setSelected(true);
            sliderPineLevel.setValue(pineSettings.getMinimumLevel());
        } else {
            checkBoxPineEverywhere.setSelected(false);
            sliderPineLevel.setValue(8);
        }
        
        // jungle
        TreeLayerSettings<Jungle> jungleSettings = (TreeLayerSettings<Jungle>) dimension.getLayerSettings(Jungle.INSTANCE);
        if (jungleSettings == null) {
            jungleSettings = new TreeLayerSettings<>(Jungle.INSTANCE);
        }
        if (jungleSettings.getMinimumLevel() > 0) {
            checkBoxJungleEverywhere.setSelected(true);
            sliderJungleLevel.setValue(jungleSettings.getMinimumLevel());
        } else {
            checkBoxJungleEverywhere.setSelected(false);
            sliderJungleLevel.setValue(8);
        }
        
        // swamp
        TreeLayerSettings<SwampLand> swampLandSettings = (TreeLayerSettings<SwampLand>) dimension.getLayerSettings(SwampLand.INSTANCE);
        if (swampLandSettings == null) {
            swampLandSettings = new TreeLayerSettings<>(SwampLand.INSTANCE);
        }
        if (swampLandSettings.getMinimumLevel() > 0) {
            checkBoxSwamplandEverywhere.setSelected(true);
            jSlider6.setValue(swampLandSettings.getMinimumLevel());
        } else {
            checkBoxSwamplandEverywhere.setSelected(false);
            jSlider6.setValue(8);
        }
        
        // frost
        FrostSettings frostSettings = (FrostSettings) dimension.getLayerSettings(Frost.INSTANCE);
        if (frostSettings == null) {
            frostSettings = new FrostSettings();
        }
        checkBoxFrostEverywhere.setSelected(frostSettings.isFrostEverywhere());
        checkBoxSmoothSnow.setSelected((frostSettings.getMode() == 2) || (frostSettings.getMode() == 3));
        checkBoxSnowUnderTrees.setSelected(frostSettings.isSnowUnderTrees());
        
        // resources
        ResourcesExporterSettings resourcesSettings = (ResourcesExporterSettings) dimension.getLayerSettings(Resources.INSTANCE);
        if (resourcesSettings == null) {
            resourcesSettings = new ResourcesExporterSettings(dimension.getMaxHeight());
            resourcesSettings.setMinimumLevel(0);
        }
        jCheckBox8.setSelected(resourcesSettings.isApplyEverywhere());
        if (resourcesSettings.isApplyEverywhere()) {
            jSlider4.setValue(resourcesSettings.getMinimumLevel());
        } else {
            jSlider4.setValue(8);
        }
        spinnerGoldChance.setValue(resourcesSettings.getChance(BLK_GOLD_ORE));
        ((SpinnerNumberModel) spinnerGoldMinLevel.getModel()).setMaximum(maxHeight);
        spinnerGoldMinLevel.setValue(clamp(resourcesSettings.getMinLevel(BLK_GOLD_ORE), maxHeight));
        ((SpinnerNumberModel) spinnerGoldMaxLevel.getModel()).setMaximum(maxHeight);
        spinnerGoldMaxLevel.setValue(clamp(resourcesSettings.getMaxLevel(BLK_GOLD_ORE), maxHeight));
        spinnerIronChance.setValue(resourcesSettings.getChance(BLK_IRON_ORE));
        ((SpinnerNumberModel) spinnerIronMinLevel.getModel()).setMaximum(maxHeight);
        spinnerIronMinLevel.setValue(clamp(resourcesSettings.getMinLevel(BLK_IRON_ORE), maxHeight));
        ((SpinnerNumberModel) spinnerIronMaxLevel.getModel()).setMaximum(maxHeight);
        spinnerIronMaxLevel.setValue(clamp(resourcesSettings.getMaxLevel(BLK_IRON_ORE), maxHeight));
        spinnerCoalChance.setValue(resourcesSettings.getChance(BLK_COAL));
        ((SpinnerNumberModel) spinnerCoalMinLevel.getModel()).setMaximum(maxHeight);
        spinnerCoalMinLevel.setValue(clamp(resourcesSettings.getMinLevel(BLK_COAL), maxHeight));
        ((SpinnerNumberModel) spinnerCoalMaxLevel.getModel()).setMaximum(maxHeight);
        spinnerCoalMaxLevel.setValue(clamp(resourcesSettings.getMaxLevel(BLK_COAL), maxHeight));
        spinnerLapisChance.setValue(resourcesSettings.getChance(BLK_LAPIS_LAZULI_ORE));
        ((SpinnerNumberModel) spinnerLapisMinLevel.getModel()).setMaximum(maxHeight);
        spinnerLapisMinLevel.setValue(clamp(resourcesSettings.getMinLevel(BLK_LAPIS_LAZULI_ORE), maxHeight));
        ((SpinnerNumberModel) spinnerLapisMaxLevel.getModel()).setMaximum(maxHeight);
        spinnerLapisMaxLevel.setValue(clamp(resourcesSettings.getMaxLevel(BLK_LAPIS_LAZULI_ORE), maxHeight));
        spinnerDiamondChance.setValue(resourcesSettings.getChance(BLK_DIAMOND_ORE));
        ((SpinnerNumberModel) spinnerDiamondMinLevel.getModel()).setMaximum(maxHeight);
        spinnerDiamondMinLevel.setValue(clamp(resourcesSettings.getMinLevel(BLK_DIAMOND_ORE), maxHeight));
        ((SpinnerNumberModel) spinnerDiamondMaxLevel.getModel()).setMaximum(maxHeight);
        spinnerDiamondMaxLevel.setValue(clamp(resourcesSettings.getMaxLevel(BLK_DIAMOND_ORE), maxHeight));
        spinnerRedstoneChance.setValue(resourcesSettings.getChance(BLK_REDSTONE_ORE));
        ((SpinnerNumberModel) spinnerRedstoneMinLevel.getModel()).setMaximum(maxHeight);
        spinnerRedstoneMinLevel.setValue(clamp(resourcesSettings.getMinLevel(BLK_REDSTONE_ORE), maxHeight));
        ((SpinnerNumberModel) spinnerRedstoneMaxLevel.getModel()).setMaximum(maxHeight);
        spinnerRedstoneMaxLevel.setValue(clamp(resourcesSettings.getMaxLevel(BLK_REDSTONE_ORE), maxHeight));
        spinnerWaterChance.setValue(resourcesSettings.getChance(BLK_WATER));
        ((SpinnerNumberModel) spinnerWaterMinLevel.getModel()).setMaximum(maxHeight);
        spinnerWaterMinLevel.setValue(clamp(resourcesSettings.getMinLevel(BLK_WATER), maxHeight));
        ((SpinnerNumberModel) spinnerWaterMaxLevel.getModel()).setMaximum(maxHeight);
        spinnerWaterMaxLevel.setValue(clamp(resourcesSettings.getMaxLevel(BLK_WATER), maxHeight));
        spinnerLavaChance.setValue(resourcesSettings.getChance(BLK_LAVA));
        ((SpinnerNumberModel) spinnerLavaMinLevel.getModel()).setMaximum(maxHeight);
        spinnerLavaMinLevel.setValue(clamp(resourcesSettings.getMinLevel(BLK_LAVA), maxHeight));
        ((SpinnerNumberModel) spinnerLavaMaxLevel.getModel()).setMaximum(maxHeight);
        spinnerLavaMaxLevel.setValue(clamp(resourcesSettings.getMaxLevel(BLK_LAVA), maxHeight));
        spinnerDirtChance.setValue(resourcesSettings.getChance(BLK_DIRT));
        ((SpinnerNumberModel) spinnerDirtMinLevel.getModel()).setMaximum(maxHeight);
        spinnerDirtMinLevel.setValue(clamp(resourcesSettings.getMinLevel(BLK_DIRT), maxHeight));
        ((SpinnerNumberModel) spinnerDirtMaxLevel.getModel()).setMaximum(maxHeight);
        spinnerDirtMaxLevel.setValue(clamp(resourcesSettings.getMaxLevel(BLK_DIRT), maxHeight));
        spinnerGravelChance.setValue(resourcesSettings.getChance(BLK_GRAVEL));
        ((SpinnerNumberModel) spinnerGravelMinLevel.getModel()).setMaximum(maxHeight);
        spinnerGravelMinLevel.setValue(clamp(resourcesSettings.getMinLevel(BLK_GRAVEL), maxHeight));
        ((SpinnerNumberModel) spinnerGravelMaxLevel.getModel()).setMaximum(maxHeight);
        spinnerGravelMaxLevel.setValue(clamp(resourcesSettings.getMaxLevel(BLK_GRAVEL), maxHeight));
        spinnerEmeraldChance.setValue(resourcesSettings.getChance(BLK_EMERALD_ORE));
        ((SpinnerNumberModel) spinnerEmeraldMinLevel.getModel()).setMaximum(maxHeight);
        spinnerEmeraldMinLevel.setValue(clamp(resourcesSettings.getMinLevel(BLK_EMERALD_ORE), maxHeight));
        ((SpinnerNumberModel) spinnerEmeraldMaxLevel.getModel()).setMaximum(maxHeight);
        spinnerEmeraldMaxLevel.setValue(clamp(resourcesSettings.getMaxLevel(BLK_EMERALD_ORE), maxHeight));
        spinnerQuartzChance.setValue(resourcesSettings.getChance(BLK_QUARTZ_ORE));
        ((SpinnerNumberModel) spinnerQuartzMinLevel.getModel()).setMaximum(maxHeight);
        spinnerQuartzMinLevel.setValue(clamp(resourcesSettings.getMinLevel(BLK_QUARTZ_ORE), maxHeight));
        ((SpinnerNumberModel) spinnerQuartzMaxLevel.getModel()).setMaximum(maxHeight);
        spinnerQuartzMaxLevel.setValue(clamp(resourcesSettings.getMaxLevel(BLK_QUARTZ_ORE), maxHeight));
        
        // terrain ranges
        if (mode != Mode.EXPORT) {
            if ((dimension.getTileFactory() instanceof HeightMapTileFactory)
                    && (((HeightMapTileFactory) dimension.getTileFactory()).getTheme() instanceof SimpleTheme)
                    && (((SimpleTheme) ((HeightMapTileFactory) dimension.getTileFactory()).getTheme()).getTerrainRanges() != null)) {
                themeEditor.setTheme((SimpleTheme) ((HeightMapTileFactory) dimension.getTileFactory()).getTheme());
            } else {
                jTabbedPane1.setEnabledAt(1, false);
            }
        }
        
        // annotations
        AnnotationsSettings annotationsSettings = (AnnotationsSettings) dimension.getLayerSettings(Annotations.INSTANCE);
        if (annotationsSettings == null) {
            annotationsSettings = new AnnotationsSettings();
        }
        checkBoxExportAnnotations.setSelected(annotationsSettings.isExport());
        
        // custom layers
        if (mode == Mode.EXPORT) {
            Set<CustomLayer> customLayers = App.getInstance().getCustomLayers();
//            if (! customLayers.isEmpty()) {
                customLayersTableModel = new CustomLayersTableModel(customLayers);
                tableCustomLayers.setModel(customLayersTableModel);
//            } else {
//                jTabbedPane1.setEnabledAt(5, false);
//            }
        }
        
        setControlStates();
    }
    
    private int clamp(int value, int maxValue) {
        if (value < 0) {
            return 0;
        } else if (value > maxValue) {
            return maxValue;
        } else {
            return value;
        }
    }
    
    private void setControlStates() {
        boolean enabled = isEnabled();
        boolean dim0 = (dimension != null) && (dimension.getDim() == Constants.DIM_NORMAL);
        boolean ceiling = (dimension != null) && (dimension.getDim() < 0);
        radioButtonLavaBorder.setEnabled(enabled  && (! ceiling));
        radioButtonNoBorder.setEnabled(enabled  && (! ceiling));
        radioButtonVoidBorder.setEnabled(enabled  && (! ceiling));
        radioButtonWaterBorder.setEnabled(enabled  && (! ceiling));
        spinnerBorderLevel.setEnabled(enabled && (! ceiling) && (radioButtonLavaBorder.isSelected() || radioButtonWaterBorder.isSelected()));
        radioButtonFixedBorder.setEnabled(enabled && (! ceiling) && (! radioButtonNoBorder.isSelected()));
        radioButtonEndlessBorder.setEnabled(enabled && dim0 && (! ceiling) && (! radioButtonNoBorder.isSelected()));
        spinnerBorderSize.setEnabled(enabled && (! ceiling) && (! radioButtonNoBorder.isSelected()) && radioButtonFixedBorder.isSelected());
        checkBoxBedrockWall.setEnabled(enabled  && (! ceiling) && (radioButtonNoBorder.isSelected() || radioButtonFixedBorder.isSelected()));
        sliderCavernsEverywhereLevel.setEnabled(enabled && checkBoxCavernsEverywhere.isSelected());
        sliderChasmsEverywhereLevel.setEnabled(enabled && checkBoxChasmsEverywhere.isSelected());
        spinnerCavernsFloodLevel.setEnabled(enabled && checkBoxFloodCaverns.isSelected());
        checkBoxCavernsFloodWithLava.setEnabled(enabled && checkBoxFloodCaverns.isSelected());
        sliderDeciduousLevel.setEnabled(enabled && checkBoxDeciduousEverywhere.isSelected());
        sliderPineLevel.setEnabled(enabled && checkBoxPineEverywhere.isSelected());
        sliderJungleLevel.setEnabled(enabled && checkBoxJungleEverywhere.isSelected());
        jSlider6.setEnabled(enabled && checkBoxSwamplandEverywhere.isSelected());
        jSlider4.setEnabled(enabled && jCheckBox8.isSelected());
        spinnerMinecraftSeed.setEnabled((mode != Mode.DEFAULT_SETTINGS) && enabled && dim0);
        checkBoxPopulate.setEnabled(enabled && dim0);
        checkBoxCavernsRemoveWater.setEnabled(enabled && (checkBoxCavernsBreakSurface.isSelected() || checkBoxChasmsBreakSurface.isSelected()));
        spinnerCeilingHeight.setEnabled(enabled && ceiling);
        int selectedRow = tableCustomLayers.getSelectedRow();
        buttonCustomLayerUp.setEnabled(enabled && (selectedRow != -1) && (! customLayersTableModel.isHeaderRow(selectedRow)) && (selectedRow > 0) && (! customLayersTableModel.isHeaderRow(selectedRow - 1)));
        buttonCustomLayerDown.setEnabled(enabled && (selectedRow != -1) && (! customLayersTableModel.isHeaderRow(selectedRow)) && (selectedRow < (tableCustomLayers.getRowCount() - 1)) && (! customLayersTableModel.isHeaderRow(selectedRow + 1)));
    }

    private void addListeners(final JSpinner minSpinner, final JSpinner maxSpinner) {
        minSpinner.addChangeListener(e -> {
            int newMinValue = (Integer) minSpinner.getValue();
            int currentMaxValue = (Integer) maxSpinner.getValue();
            if (newMinValue > currentMaxValue) {
                maxSpinner.setValue(newMinValue);
            }
        });
        maxSpinner.addChangeListener(e -> {
            int newMaxValue = (Integer) maxSpinner.getValue();
            int currentMinValue = (Integer) minSpinner.getValue();
            if (newMaxValue < currentMinValue) {
                minSpinner.setValue(newMaxValue);
            }
        });
    }

    private void notifyBorderListeners() {
        Dimension.Border newBorder = getSelectedBorder();
        for (BorderListener borderListener: borderListeners) {
            borderListener.borderChanged(newBorder);
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        buttonGroup2 = new javax.swing.ButtonGroup();
        buttonGroup3 = new javax.swing.ButtonGroup();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        radioButtonWaterBorder = new javax.swing.JRadioButton();
        radioButtonNoBorder = new javax.swing.JRadioButton();
        radioButtonVoidBorder = new javax.swing.JRadioButton();
        jLabel4 = new javax.swing.JLabel();
        radioButtonLavaBorder = new javax.swing.JRadioButton();
        jLabel5 = new javax.swing.JLabel();
        spinnerBorderLevel = new javax.swing.JSpinner();
        checkBoxBedrockWall = new javax.swing.JCheckBox();
        jLabel6 = new javax.swing.JLabel();
        comboBoxSubsurfaceMaterial = new javax.swing.JComboBox();
        jLabel7 = new javax.swing.JLabel();
        spinnerMinecraftSeed = new javax.swing.JSpinner();
        jLabel8 = new javax.swing.JLabel();
        spinnerBorderSize = new javax.swing.JSpinner();
        jLabel9 = new javax.swing.JLabel();
        jLabel44 = new javax.swing.JLabel();
        jLabel65 = new javax.swing.JLabel();
        spinnerMinSurfaceDepth = new javax.swing.JSpinner();
        jLabel66 = new javax.swing.JLabel();
        spinnerMaxSurfaceDepth = new javax.swing.JSpinner();
        checkBoxBottomless = new javax.swing.JCheckBox();
        jLabel67 = new javax.swing.JLabel();
        checkBoxCoverSteepTerrain = new javax.swing.JCheckBox();
        jLabel78 = new javax.swing.JLabel();
        spinnerCeilingHeight = new javax.swing.JSpinner();
        jPanel6 = new javax.swing.JPanel();
        jLabel79 = new javax.swing.JLabel();
        spinnerMcBorderCentreX = new javax.swing.JSpinner();
        jLabel80 = new javax.swing.JLabel();
        spinnerMcBorderCentreY = new javax.swing.JSpinner();
        jLabel81 = new javax.swing.JLabel();
        spinnerMcBorderSize = new javax.swing.JSpinner();
        jLabel85 = new javax.swing.JLabel();
        jLabel86 = new javax.swing.JLabel();
        jLabel90 = new javax.swing.JLabel();
        radioButtonFixedBorder = new javax.swing.JRadioButton();
        radioButtonEndlessBorder = new javax.swing.JRadioButton();
        jPanel5 = new javax.swing.JPanel();
        themeEditor = new org.pepsoft.worldpainter.themes.impl.simple.SimpleThemeEditor();
        jLabel45 = new javax.swing.JLabel();
        jLabel46 = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        checkBoxCavernsEverywhere = new javax.swing.JCheckBox();
        sliderCavernsEverywhereLevel = new javax.swing.JSlider();
        sliderChasmsEverywhereLevel = new javax.swing.JSlider();
        checkBoxChasmsEverywhere = new javax.swing.JCheckBox();
        jSeparator1 = new javax.swing.JSeparator();
        checkBoxFloodCaverns = new javax.swing.JCheckBox();
        jLabel1 = new javax.swing.JLabel();
        spinnerCavernsFloodLevel = new javax.swing.JSpinner();
        checkBoxCavernsFloodWithLava = new javax.swing.JCheckBox();
        jLabel2 = new javax.swing.JLabel();
        checkBoxCavernsBreakSurface = new javax.swing.JCheckBox();
        checkBoxCavernsRemoveWater = new javax.swing.JCheckBox();
        checkBoxChasmsBreakSurface = new javax.swing.JCheckBox();
        jLabel70 = new javax.swing.JLabel();
        jLabel71 = new javax.swing.JLabel();
        spinnerCavernsMinLevel = new javax.swing.JSpinner();
        spinnerCavernsMaxLevel = new javax.swing.JSpinner();
        jLabel73 = new javax.swing.JLabel();
        spinnerChasmsMinLevel = new javax.swing.JSpinner();
        jLabel72 = new javax.swing.JLabel();
        spinnerChasmsMaxLevel = new javax.swing.JSpinner();
        jPanel4 = new javax.swing.JPanel();
        jCheckBox8 = new javax.swing.JCheckBox();
        jSlider4 = new javax.swing.JSlider();
        jSeparator2 = new javax.swing.JSeparator();
        jLabel10 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        spinnerGoldChance = new javax.swing.JSpinner();
        jLabel13 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        spinnerGoldMaxLevel = new javax.swing.JSpinner();
        jLabel15 = new javax.swing.JLabel();
        spinnerIronChance = new javax.swing.JSpinner();
        spinnerIronMaxLevel = new javax.swing.JSpinner();
        jLabel16 = new javax.swing.JLabel();
        spinnerCoalChance = new javax.swing.JSpinner();
        spinnerCoalMaxLevel = new javax.swing.JSpinner();
        jLabel17 = new javax.swing.JLabel();
        spinnerLapisChance = new javax.swing.JSpinner();
        spinnerLapisMaxLevel = new javax.swing.JSpinner();
        jLabel18 = new javax.swing.JLabel();
        spinnerDiamondChance = new javax.swing.JSpinner();
        spinnerDiamondMaxLevel = new javax.swing.JSpinner();
        jLabel19 = new javax.swing.JLabel();
        spinnerRedstoneChance = new javax.swing.JSpinner();
        spinnerRedstoneMaxLevel = new javax.swing.JSpinner();
        jLabel20 = new javax.swing.JLabel();
        spinnerWaterChance = new javax.swing.JSpinner();
        spinnerWaterMaxLevel = new javax.swing.JSpinner();
        jLabel21 = new javax.swing.JLabel();
        spinnerLavaChance = new javax.swing.JSpinner();
        jLabel22 = new javax.swing.JLabel();
        jLabel23 = new javax.swing.JLabel();
        jLabel24 = new javax.swing.JLabel();
        jLabel25 = new javax.swing.JLabel();
        jLabel26 = new javax.swing.JLabel();
        jLabel27 = new javax.swing.JLabel();
        jLabel28 = new javax.swing.JLabel();
        jLabel29 = new javax.swing.JLabel();
        jLabel30 = new javax.swing.JLabel();
        spinnerDirtChance = new javax.swing.JSpinner();
        jLabel31 = new javax.swing.JLabel();
        spinnerLavaMaxLevel = new javax.swing.JSpinner();
        spinnerDirtMaxLevel = new javax.swing.JSpinner();
        jLabel32 = new javax.swing.JLabel();
        spinnerGravelChance = new javax.swing.JSpinner();
        jLabel33 = new javax.swing.JLabel();
        spinnerGravelMaxLevel = new javax.swing.JSpinner();
        jLabel34 = new javax.swing.JLabel();
        jLabel35 = new javax.swing.JLabel();
        jLabel36 = new javax.swing.JLabel();
        jLabel37 = new javax.swing.JLabel();
        jLabel38 = new javax.swing.JLabel();
        jLabel39 = new javax.swing.JLabel();
        jLabel40 = new javax.swing.JLabel();
        jLabel41 = new javax.swing.JLabel();
        jLabel42 = new javax.swing.JLabel();
        jLabel43 = new javax.swing.JLabel();
        jSeparator3 = new javax.swing.JSeparator();
        jLabel3 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jLabel51 = new javax.swing.JLabel();
        spinnerEmeraldChance = new javax.swing.JSpinner();
        jLabel52 = new javax.swing.JLabel();
        spinnerEmeraldMaxLevel = new javax.swing.JSpinner();
        jLabel53 = new javax.swing.JLabel();
        spinnerGoldMinLevel = new javax.swing.JSpinner();
        spinnerIronMinLevel = new javax.swing.JSpinner();
        spinnerCoalMinLevel = new javax.swing.JSpinner();
        spinnerLapisMinLevel = new javax.swing.JSpinner();
        spinnerDiamondMinLevel = new javax.swing.JSpinner();
        spinnerEmeraldMinLevel = new javax.swing.JSpinner();
        spinnerWaterMinLevel = new javax.swing.JSpinner();
        spinnerLavaMinLevel = new javax.swing.JSpinner();
        spinnerDirtMinLevel = new javax.swing.JSpinner();
        spinnerGravelMinLevel = new javax.swing.JSpinner();
        spinnerRedstoneMinLevel = new javax.swing.JSpinner();
        jLabel54 = new javax.swing.JLabel();
        jLabel55 = new javax.swing.JLabel();
        jLabel56 = new javax.swing.JLabel();
        jLabel57 = new javax.swing.JLabel();
        jLabel58 = new javax.swing.JLabel();
        jLabel59 = new javax.swing.JLabel();
        jLabel60 = new javax.swing.JLabel();
        jLabel61 = new javax.swing.JLabel();
        jLabel62 = new javax.swing.JLabel();
        jLabel63 = new javax.swing.JLabel();
        jLabel64 = new javax.swing.JLabel();
        jLabel74 = new javax.swing.JLabel();
        spinnerQuartzChance = new javax.swing.JSpinner();
        jLabel75 = new javax.swing.JLabel();
        spinnerQuartzMinLevel = new javax.swing.JSpinner();
        jLabel77 = new javax.swing.JLabel();
        spinnerQuartzMaxLevel = new javax.swing.JSpinner();
        jLabel76 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        checkBoxPopulate = new javax.swing.JCheckBox();
        checkBoxDeciduousEverywhere = new javax.swing.JCheckBox();
        sliderDeciduousLevel = new javax.swing.JSlider();
        checkBoxFrostEverywhere = new javax.swing.JCheckBox();
        checkBoxPineEverywhere = new javax.swing.JCheckBox();
        sliderPineLevel = new javax.swing.JSlider();
        checkBoxSmoothSnow = new javax.swing.JCheckBox();
        jLabel47 = new javax.swing.JLabel();
        jLabel48 = new javax.swing.JLabel();
        jLabel49 = new javax.swing.JLabel();
        jLabel50 = new javax.swing.JLabel();
        checkBoxJungleEverywhere = new javax.swing.JCheckBox();
        jLabel68 = new javax.swing.JLabel();
        sliderJungleLevel = new javax.swing.JSlider();
        checkBoxSwamplandEverywhere = new javax.swing.JCheckBox();
        jLabel69 = new javax.swing.JLabel();
        jSlider6 = new javax.swing.JSlider();
        checkBoxSnowUnderTrees = new javax.swing.JCheckBox();
        checkBoxExportAnnotations = new javax.swing.JCheckBox();
        jPanel7 = new javax.swing.JPanel();
        jLabel82 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tableCustomLayers = new javax.swing.JTable();
        buttonCustomLayerUp = new javax.swing.JButton();
        buttonCustomLayerDown = new javax.swing.JButton();

        buttonGroup1.add(radioButtonWaterBorder);
        radioButtonWaterBorder.setText("Water");
        radioButtonWaterBorder.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonWaterBorderActionPerformed(evt);
            }
        });

        buttonGroup1.add(radioButtonNoBorder);
        radioButtonNoBorder.setText("No border");
        radioButtonNoBorder.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonNoBorderActionPerformed(evt);
            }
        });

        buttonGroup1.add(radioButtonVoidBorder);
        radioButtonVoidBorder.setText("Void");
        radioButtonVoidBorder.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonVoidBorderActionPerformed(evt);
            }
        });

        jLabel4.setText("Border:");

        buttonGroup1.add(radioButtonLavaBorder);
        radioButtonLavaBorder.setText("Lava");
        radioButtonLavaBorder.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonLavaBorderActionPerformed(evt);
            }
        });

        jLabel5.setText("Water or lava level:");

        spinnerBorderLevel.setModel(new javax.swing.SpinnerNumberModel(62, 0, 127, 1));
        spinnerBorderLevel.setEnabled(false);

        checkBoxBedrockWall.setText("Bedrock wall");

        jLabel6.setText("Underground material:");

        comboBoxSubsurfaceMaterial.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        comboBoxSubsurfaceMaterial.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxSubsurfaceMaterialActionPerformed(evt);
            }
        });

        jLabel7.setText("Minecraft seed:");

        spinnerMinecraftSeed.setModel(new javax.swing.SpinnerNumberModel(Long.valueOf(-9223372036854775808L), null, null, Long.valueOf(1L)));
        spinnerMinecraftSeed.setEditor(new javax.swing.JSpinner.NumberEditor(spinnerMinecraftSeed, "0"));

        jLabel8.setLabelFor(spinnerBorderSize);
        jLabel8.setText("Border size:");

        spinnerBorderSize.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(256), Integer.valueOf(128), null, Integer.valueOf(128)));
        spinnerBorderSize.setEnabled(false);
        spinnerBorderSize.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerBorderSizeStateChanged(evt);
            }
        });

        jLabel9.setText("blocks (in multiples of 128)");

        jLabel44.setText("(Minecraft default: 62)");

        jLabel65.setText("Top layer minimum depth:");

        spinnerMinSurfaceDepth.setModel(new javax.swing.SpinnerNumberModel(3, 1, 255, 1));
        spinnerMinSurfaceDepth.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerMinSurfaceDepthStateChanged(evt);
            }
        });

        jLabel66.setText(", maximum depth:");

        spinnerMaxSurfaceDepth.setModel(new javax.swing.SpinnerNumberModel(7, 1, 255, 1));
        spinnerMaxSurfaceDepth.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerMaxSurfaceDepthStateChanged(evt);
            }
        });

        checkBoxBottomless.setText("Bottomless world");
        checkBoxBottomless.setToolTipText("<html>Generate a bottomless map:\n<ul><li>No bedrock at the bottom of the map\n<li>Caverns and chasms are open to the void</html>");

        jLabel67.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/information.png"))); // NOI18N
        jLabel67.setText(" ");
        jLabel67.setToolTipText("<html>Generate a bottomless map:\n<ul><li>No bedrock at the bottom of the map\n<li>Caverns and chasms are open to the void</html>");

        checkBoxCoverSteepTerrain.setText("keep steep terrain covered");
        checkBoxCoverSteepTerrain.setToolTipText("<html>Enable this to extend the top layer<br>\ndownwards on steep terrain such as cliffs <br>\nso that the underground material is never exposed.</html>");

        jLabel78.setText("Ceiling height:");

        spinnerCeilingHeight.setModel(new javax.swing.SpinnerNumberModel(256, 1, 256, 1));

        jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder("Minecraft World Border"));

        jLabel79.setText("Centre:");

        spinnerMcBorderCentreX.setModel(new javax.swing.SpinnerNumberModel(0, -99999, 99999, 1));

        jLabel80.setText(", ");

        spinnerMcBorderCentreY.setModel(new javax.swing.SpinnerNumberModel(0, -99999, 99999, 1));

        jLabel81.setText("Size:");

        spinnerMcBorderSize.setModel(new javax.swing.SpinnerNumberModel(0, 0, 60000000, 1));

        jLabel85.setText(" blocks");

        jLabel86.setText(" blocks");

        jLabel90.setText("(Applies to all dimensions)");

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(jLabel79)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spinnerMcBorderCentreX, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, 0)
                        .addComponent(jLabel80)
                        .addGap(0, 0, 0)
                        .addComponent(spinnerMcBorderCentreY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, 0)
                        .addComponent(jLabel85))
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(jLabel81)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spinnerMcBorderSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, 0)
                        .addComponent(jLabel86))
                    .addComponent(jLabel90))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel90)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel79)
                    .addComponent(spinnerMcBorderCentreX, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel80)
                    .addComponent(spinnerMcBorderCentreY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel85))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel81)
                    .addComponent(spinnerMcBorderSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel86))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        buttonGroup3.add(radioButtonFixedBorder);
        radioButtonFixedBorder.setSelected(true);
        radioButtonFixedBorder.setText("fixed:");
        radioButtonFixedBorder.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonFixedBorderActionPerformed(evt);
            }
        });

        buttonGroup3.add(radioButtonEndlessBorder);
        radioButtonEndlessBorder.setText("endless");
        radioButtonEndlessBorder.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonEndlessBorderActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(comboBoxSubsurfaceMaterial, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel65)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spinnerMinSurfaceDepth, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, 0)
                        .addComponent(jLabel66)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spinnerMaxSurfaceDepth, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(checkBoxCoverSteepTerrain))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(checkBoxBottomless)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel67)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel78)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spinnerCeilingHeight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel7)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerMinecraftSeed, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jLabel4)
                            .addComponent(radioButtonNoBorder)
                            .addComponent(radioButtonVoidBorder)
                            .addComponent(radioButtonLavaBorder)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(radioButtonWaterBorder)
                                    .addComponent(jLabel8))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(radioButtonEndlessBorder)
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addComponent(radioButtonFixedBorder)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addComponent(spinnerBorderSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGap(0, 0, 0)
                                                .addComponent(jLabel9))
                                            .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addComponent(jLabel5)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(spinnerBorderLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                            .addComponent(jLabel44))))))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(checkBoxBedrockWall))
                .addContainerGap(114, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel65)
                    .addComponent(spinnerMinSurfaceDepth, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel66)
                    .addComponent(spinnerMaxSurfaceDepth, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(checkBoxCoverSteepTerrain))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(comboBoxSubsurfaceMaterial, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(checkBoxBottomless)
                    .addComponent(jLabel67, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel78)
                    .addComponent(spinnerCeilingHeight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(19, 19, 19)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(radioButtonNoBorder)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(radioButtonVoidBorder)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(radioButtonWaterBorder)
                            .addComponent(jLabel5)
                            .addComponent(spinnerBorderLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(radioButtonLavaBorder)
                            .addComponent(jLabel44))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel8)
                            .addComponent(spinnerBorderSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel9)
                            .addComponent(radioButtonFixedBorder)))
                    .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(radioButtonEndlessBorder)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(checkBoxBedrockWall)
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(spinnerMinecraftSeed, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("General", jPanel1);

        jLabel45.setText("These are the default terrain types and layers used by the Mountain tool, and when resetting the");

        jLabel46.setText("terrain to the default by right-clicking with a terrain type selected:");

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(themeEditor, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(jLabel46, javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jLabel45, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel45)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel46)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(themeEditor, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Default Terrain and Layers", jPanel5);

        checkBoxCavernsEverywhere.setText("Caverns everywhere");
        checkBoxCavernsEverywhere.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxCavernsEverywhereActionPerformed(evt);
            }
        });

        sliderCavernsEverywhereLevel.setMajorTickSpacing(1);
        sliderCavernsEverywhereLevel.setMaximum(15);
        sliderCavernsEverywhereLevel.setMinimum(1);
        sliderCavernsEverywhereLevel.setPaintTicks(true);
        sliderCavernsEverywhereLevel.setSnapToTicks(true);
        sliderCavernsEverywhereLevel.setValue(8);

        sliderChasmsEverywhereLevel.setMajorTickSpacing(1);
        sliderChasmsEverywhereLevel.setMaximum(15);
        sliderChasmsEverywhereLevel.setMinimum(1);
        sliderChasmsEverywhereLevel.setPaintTicks(true);
        sliderChasmsEverywhereLevel.setSnapToTicks(true);
        sliderChasmsEverywhereLevel.setValue(8);

        checkBoxChasmsEverywhere.setText("Chasms everywhere");
        checkBoxChasmsEverywhere.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxChasmsEverywhereActionPerformed(evt);
            }
        });

        checkBoxFloodCaverns.setText("Flood the caverns and chasms");
        checkBoxFloodCaverns.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxFloodCavernsActionPerformed(evt);
            }
        });

        jLabel1.setText("Level:");

        spinnerCavernsFloodLevel.setModel(new javax.swing.SpinnerNumberModel(16, 1, 127, 1));

        checkBoxCavernsFloodWithLava.setText("Lava instead of water:");
        checkBoxCavernsFloodWithLava.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);

        jLabel2.setText("Settings for the Caverns and Chasms layers. These apply also to hand-painted Caverns and Chasms:");

        checkBoxCavernsBreakSurface.setText("Caverns break the surface");
        checkBoxCavernsBreakSurface.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxCavernsBreakSurfaceActionPerformed(evt);
            }
        });

        checkBoxCavernsRemoveWater.setSelected(true);
        checkBoxCavernsRemoveWater.setText("Remove water and lava above openings");
        checkBoxCavernsRemoveWater.setEnabled(false);

        checkBoxChasmsBreakSurface.setText("Chasms break the surface");
        checkBoxChasmsBreakSurface.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxChasmsBreakSurfaceActionPerformed(evt);
            }
        });

        jLabel70.setText("Caverns min. level:");

        jLabel71.setText("Caverns max. level:");

        spinnerCavernsMinLevel.setModel(new javax.swing.SpinnerNumberModel(0, 0, 255, 1));

        spinnerCavernsMaxLevel.setModel(new javax.swing.SpinnerNumberModel(255, 0, 255, 1));

        jLabel73.setText("Chasms min. level:");

        spinnerChasmsMinLevel.setModel(new javax.swing.SpinnerNumberModel(0, 0, 255, 1));

        jLabel72.setText("Chasms max. level:");

        spinnerChasmsMaxLevel.setModel(new javax.swing.SpinnerNumberModel(255, 0, 255, 1));

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(checkBoxCavernsEverywhere)
                    .addComponent(jLabel2)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(checkBoxCavernsRemoveWater)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(sliderCavernsEverywhereLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel3Layout.createSequentialGroup()
                                        .addGap(12, 12, 12)
                                        .addComponent(sliderChasmsEverywhereLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addComponent(checkBoxChasmsEverywhere)))
                            .addComponent(checkBoxCavernsFloodWithLava)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerCavernsFloodLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 442, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(checkBoxFloodCaverns)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(jLabel70)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerCavernsMinLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(checkBoxCavernsBreakSurface)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(jLabel71)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerCavernsMaxLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(jLabel72)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerChasmsMaxLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(checkBoxChasmsBreakSurface)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(jLabel73)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerChasmsMinLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(checkBoxCavernsEverywhere)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(sliderCavernsEverywhereLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(checkBoxChasmsEverywhere)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(sliderChasmsEverywhereLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(18, 18, 18)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addGap(18, 18, 18)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel70)
                    .addComponent(spinnerCavernsMinLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel73)
                    .addComponent(spinnerChasmsMinLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel71)
                    .addComponent(spinnerCavernsMaxLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel72)
                    .addComponent(spinnerChasmsMaxLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(checkBoxFloodCaverns)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(spinnerCavernsFloodLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(checkBoxCavernsFloodWithLava)
                .addGap(18, 18, 18)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(checkBoxCavernsBreakSurface)
                    .addComponent(checkBoxChasmsBreakSurface))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(checkBoxCavernsRemoveWater)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Caverns and Chasms", new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/caverns.png")), jPanel3); // NOI18N

        jCheckBox8.setText("Resources everywhere");
        jCheckBox8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox8ActionPerformed(evt);
            }
        });

        jSlider4.setMajorTickSpacing(1);
        jSlider4.setMaximum(15);
        jSlider4.setMinimum(1);
        jSlider4.setPaintTicks(true);
        jSlider4.setSnapToTicks(true);
        jSlider4.setValue(8);

        jLabel10.setText("Settings for the Resources layer at 50% intensity. These also apply to hand-painted Resources:");

        jLabel12.setText("Gold:");

        spinnerGoldChance.setModel(new javax.swing.SpinnerNumberModel(0, 0, 1000, 1));

        jLabel13.setText("Occurrence:");

        jLabel14.setText("Levels:");

        spinnerGoldMaxLevel.setModel(new javax.swing.SpinnerNumberModel(1, 0, 127, 1));

        jLabel15.setText("Iron:");

        spinnerIronChance.setModel(new javax.swing.SpinnerNumberModel(0, 0, 1000, 1));

        spinnerIronMaxLevel.setModel(new javax.swing.SpinnerNumberModel(1, 0, 127, 1));

        jLabel16.setText("Coal:");

        spinnerCoalChance.setModel(new javax.swing.SpinnerNumberModel(0, 0, 1000, 1));

        spinnerCoalMaxLevel.setModel(new javax.swing.SpinnerNumberModel(1, 0, 127, 1));

        jLabel17.setText("Lapis Lazuli:");

        spinnerLapisChance.setModel(new javax.swing.SpinnerNumberModel(0, 0, 1000, 1));

        spinnerLapisMaxLevel.setModel(new javax.swing.SpinnerNumberModel(1, 0, 127, 1));

        jLabel18.setText("Diamond:");

        spinnerDiamondChance.setModel(new javax.swing.SpinnerNumberModel(0, 0, 1000, 1));

        spinnerDiamondMaxLevel.setModel(new javax.swing.SpinnerNumberModel(1, 0, 127, 1));

        jLabel19.setText("Redstone:");

        spinnerRedstoneChance.setModel(new javax.swing.SpinnerNumberModel(0, 0, 1000, 1));

        spinnerRedstoneMaxLevel.setModel(new javax.swing.SpinnerNumberModel(1, 0, 127, 1));

        jLabel20.setText("Water:");

        spinnerWaterChance.setModel(new javax.swing.SpinnerNumberModel(0, 0, 1000, 1));

        spinnerWaterMaxLevel.setModel(new javax.swing.SpinnerNumberModel(1, 0, 127, 1));

        jLabel21.setText("Lava:");

        spinnerLavaChance.setModel(new javax.swing.SpinnerNumberModel(0, 0, 1000, 1));

        jLabel22.setText("‰");

        jLabel23.setText("‰");

        jLabel24.setText("‰");

        jLabel25.setText("‰");

        jLabel26.setText("‰");

        jLabel27.setText("‰");

        jLabel28.setText("‰");

        jLabel29.setText("‰");

        jLabel30.setText("Dirt:");

        spinnerDirtChance.setModel(new javax.swing.SpinnerNumberModel(0, 0, 1000, 1));

        jLabel31.setText("‰");

        spinnerLavaMaxLevel.setModel(new javax.swing.SpinnerNumberModel(1, 0, 127, 1));

        spinnerDirtMaxLevel.setModel(new javax.swing.SpinnerNumberModel(1, 0, 127, 1));

        jLabel32.setText("Gravel:");

        spinnerGravelChance.setModel(new javax.swing.SpinnerNumberModel(0, 0, 1000, 1));

        jLabel33.setText("‰");

        spinnerGravelMaxLevel.setModel(new javax.swing.SpinnerNumberModel(1, 0, 127, 1));

        jLabel34.setText("blocks");

        jLabel35.setText("blocks");

        jLabel36.setText("blocks");

        jLabel37.setText("blocks");

        jLabel38.setText("blocks");

        jLabel39.setText("blocks");

        jLabel40.setText("blocks");

        jLabel41.setText("blocks");

        jLabel42.setText("blocks");

        jLabel43.setText("blocks");

        jSeparator3.setOrientation(javax.swing.SwingConstants.VERTICAL);

        jLabel3.setText("Occurrence:");

        jLabel11.setText("Levels:");

        jLabel51.setText("Emerald:");

        spinnerEmeraldChance.setModel(new javax.swing.SpinnerNumberModel(0, 0, 1000, 1));

        jLabel52.setText("‰");

        spinnerEmeraldMaxLevel.setModel(new javax.swing.SpinnerNumberModel(1, 0, 127, 1));

        jLabel53.setText("blocks");

        spinnerGoldMinLevel.setModel(new javax.swing.SpinnerNumberModel(0, 0, 127, 1));

        spinnerIronMinLevel.setModel(new javax.swing.SpinnerNumberModel(0, 0, 127, 1));

        spinnerCoalMinLevel.setModel(new javax.swing.SpinnerNumberModel(0, 0, 127, 1));

        spinnerLapisMinLevel.setModel(new javax.swing.SpinnerNumberModel(0, 0, 127, 1));

        spinnerDiamondMinLevel.setModel(new javax.swing.SpinnerNumberModel(0, 0, 127, 1));

        spinnerEmeraldMinLevel.setModel(new javax.swing.SpinnerNumberModel(0, 0, 127, 1));

        spinnerWaterMinLevel.setModel(new javax.swing.SpinnerNumberModel(0, 0, 127, 1));

        spinnerLavaMinLevel.setModel(new javax.swing.SpinnerNumberModel(0, 0, 127, 1));

        spinnerDirtMinLevel.setModel(new javax.swing.SpinnerNumberModel(0, 0, 127, 1));

        spinnerGravelMinLevel.setModel(new javax.swing.SpinnerNumberModel(0, 0, 127, 1));

        spinnerRedstoneMinLevel.setModel(new javax.swing.SpinnerNumberModel(0, 0, 127, 1));

        jLabel54.setText("-");

        jLabel55.setText("-");

        jLabel56.setText("-");

        jLabel57.setText("-");

        jLabel58.setText("-");

        jLabel59.setText("-");

        jLabel60.setText("-");

        jLabel61.setText("-");

        jLabel62.setText("-");

        jLabel63.setText("-");

        jLabel64.setText("-");

        jLabel74.setText("Quartz:");

        spinnerQuartzChance.setModel(new javax.swing.SpinnerNumberModel(0, 0, 1000, 1));

        jLabel75.setText("‰");

        spinnerQuartzMinLevel.setModel(new javax.swing.SpinnerNumberModel(0, 0, 127, 1));

        jLabel77.setText("-");

        spinnerQuartzMaxLevel.setModel(new javax.swing.SpinnerNumberModel(1, 0, 127, 1));

        jLabel76.setText("blocks");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(jSlider4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jCheckBox8)
                    .addComponent(jLabel10)
                    .addComponent(jSeparator2)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel4Layout.createSequentialGroup()
                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel12)
                                    .addComponent(jLabel15)
                                    .addComponent(jLabel16)
                                    .addComponent(jLabel17)
                                    .addComponent(jLabel18))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel13)
                                    .addGroup(jPanel4Layout.createSequentialGroup()
                                        .addComponent(spinnerGoldChance, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(0, 0, 0)
                                        .addComponent(jLabel22))
                                    .addGroup(jPanel4Layout.createSequentialGroup()
                                        .addComponent(spinnerIronChance, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(0, 0, 0)
                                        .addComponent(jLabel23))
                                    .addGroup(jPanel4Layout.createSequentialGroup()
                                        .addComponent(spinnerCoalChance, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(0, 0, 0)
                                        .addComponent(jLabel24))
                                    .addGroup(jPanel4Layout.createSequentialGroup()
                                        .addComponent(spinnerLapisChance, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(0, 0, 0)
                                        .addComponent(jLabel25))
                                    .addGroup(jPanel4Layout.createSequentialGroup()
                                        .addComponent(spinnerDiamondChance, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(0, 0, 0)
                                        .addComponent(jLabel26))
                                    .addGroup(jPanel4Layout.createSequentialGroup()
                                        .addComponent(spinnerEmeraldChance, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(0, 0, 0)
                                        .addComponent(jLabel52))))
                            .addComponent(jLabel51))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel14)
                            .addGroup(jPanel4Layout.createSequentialGroup()
                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel4Layout.createSequentialGroup()
                                        .addComponent(spinnerDiamondMinLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(0, 0, 0)
                                        .addComponent(jLabel58)
                                        .addGap(0, 0, 0)
                                        .addComponent(spinnerDiamondMaxLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(0, 0, 0)
                                        .addComponent(jLabel38))
                                    .addGroup(jPanel4Layout.createSequentialGroup()
                                        .addComponent(spinnerLapisMinLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(0, 0, 0)
                                        .addComponent(jLabel57)
                                        .addGap(0, 0, 0)
                                        .addComponent(spinnerLapisMaxLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(0, 0, 0)
                                        .addComponent(jLabel37))
                                    .addGroup(jPanel4Layout.createSequentialGroup()
                                        .addComponent(spinnerCoalMinLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(0, 0, 0)
                                        .addComponent(jLabel56)
                                        .addGap(0, 0, 0)
                                        .addComponent(spinnerCoalMaxLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(0, 0, 0)
                                        .addComponent(jLabel36))
                                    .addGroup(jPanel4Layout.createSequentialGroup()
                                        .addComponent(spinnerIronMinLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(0, 0, 0)
                                        .addComponent(jLabel55)
                                        .addGap(0, 0, 0)
                                        .addComponent(spinnerIronMaxLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(0, 0, 0)
                                        .addComponent(jLabel35))
                                    .addGroup(jPanel4Layout.createSequentialGroup()
                                        .addComponent(spinnerGoldMinLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(0, 0, 0)
                                        .addComponent(jLabel54)
                                        .addGap(0, 0, 0)
                                        .addComponent(spinnerGoldMaxLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(0, 0, 0)
                                        .addComponent(jLabel34))
                                    .addGroup(jPanel4Layout.createSequentialGroup()
                                        .addComponent(spinnerEmeraldMinLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(0, 0, 0)
                                        .addComponent(jLabel59)
                                        .addGap(0, 0, 0)
                                        .addComponent(spinnerEmeraldMaxLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(0, 0, 0)
                                        .addComponent(jLabel53)))
                                .addGap(18, 18, 18)
                                .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, 6, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel20)
                                    .addComponent(jLabel21)
                                    .addComponent(jLabel30)
                                    .addComponent(jLabel32)
                                    .addComponent(jLabel19)
                                    .addComponent(jLabel74))))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel4Layout.createSequentialGroup()
                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel4Layout.createSequentialGroup()
                                        .addComponent(spinnerWaterChance, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(0, 0, 0)
                                        .addComponent(jLabel28))
                                    .addGroup(jPanel4Layout.createSequentialGroup()
                                        .addComponent(spinnerLavaChance, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(0, 0, 0)
                                        .addComponent(jLabel29))
                                    .addGroup(jPanel4Layout.createSequentialGroup()
                                        .addComponent(spinnerDirtChance, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(0, 0, 0)
                                        .addComponent(jLabel31))
                                    .addGroup(jPanel4Layout.createSequentialGroup()
                                        .addComponent(spinnerGravelChance, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(0, 0, 0)
                                        .addComponent(jLabel33))
                                    .addComponent(jLabel3))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel11)
                                    .addGroup(jPanel4Layout.createSequentialGroup()
                                        .addComponent(spinnerGravelMinLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(0, 0, 0)
                                        .addComponent(jLabel63)
                                        .addGap(0, 0, 0)
                                        .addComponent(spinnerGravelMaxLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(0, 0, 0)
                                        .addComponent(jLabel43))
                                    .addGroup(jPanel4Layout.createSequentialGroup()
                                        .addComponent(spinnerDirtMinLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(0, 0, 0)
                                        .addComponent(jLabel62)
                                        .addGap(0, 0, 0)
                                        .addComponent(spinnerDirtMaxLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(0, 0, 0)
                                        .addComponent(jLabel42))
                                    .addGroup(jPanel4Layout.createSequentialGroup()
                                        .addComponent(spinnerLavaMinLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(0, 0, 0)
                                        .addComponent(jLabel61)
                                        .addGap(0, 0, 0)
                                        .addComponent(spinnerLavaMaxLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(0, 0, 0)
                                        .addComponent(jLabel41))
                                    .addGroup(jPanel4Layout.createSequentialGroup()
                                        .addComponent(spinnerWaterMinLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(0, 0, 0)
                                        .addComponent(jLabel60)
                                        .addGap(0, 0, 0)
                                        .addComponent(spinnerWaterMaxLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(0, 0, 0)
                                        .addComponent(jLabel40))))
                            .addGroup(jPanel4Layout.createSequentialGroup()
                                .addComponent(spinnerRedstoneChance, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, 0)
                                .addComponent(jLabel27)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerRedstoneMinLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, 0)
                                .addComponent(jLabel64)
                                .addGap(0, 0, 0)
                                .addComponent(spinnerRedstoneMaxLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, 0)
                                .addComponent(jLabel39))
                            .addGroup(jPanel4Layout.createSequentialGroup()
                                .addComponent(spinnerQuartzChance, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, 0)
                                .addComponent(jLabel75)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerQuartzMinLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, 0)
                                .addComponent(jLabel77)
                                .addGap(0, 0, 0)
                                .addComponent(spinnerQuartzMaxLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, 0)
                                .addComponent(jLabel76)))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jCheckBox8)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSlider4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel10)
                .addGap(18, 18, 18)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel13)
                            .addComponent(jLabel14)
                            .addComponent(jLabel3)
                            .addComponent(jLabel11))
                        .addGap(6, 6, 6)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel4Layout.createSequentialGroup()
                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel12)
                                    .addComponent(spinnerGoldChance, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(spinnerGoldMaxLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel22)
                                    .addComponent(jLabel34)
                                    .addComponent(spinnerGoldMinLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel54))
                                .addGap(6, 6, 6)
                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel15)
                                    .addComponent(spinnerIronChance, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(spinnerIronMaxLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel23)
                                    .addComponent(jLabel35)
                                    .addComponent(spinnerIronMinLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel55))
                                .addGap(6, 6, 6)
                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel16)
                                    .addComponent(spinnerCoalChance, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(spinnerCoalMaxLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel24)
                                    .addComponent(jLabel36)
                                    .addComponent(spinnerCoalMinLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel56))
                                .addGap(6, 6, 6)
                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel17)
                                    .addComponent(spinnerLapisChance, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(spinnerLapisMaxLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel25)
                                    .addComponent(jLabel37)
                                    .addComponent(spinnerLapisMinLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel57))
                                .addGap(6, 6, 6)
                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel18)
                                    .addComponent(spinnerDiamondChance, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(spinnerDiamondMaxLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel26)
                                    .addComponent(jLabel38)
                                    .addComponent(spinnerDiamondMinLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel58)))
                            .addGroup(jPanel4Layout.createSequentialGroup()
                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel20)
                                    .addComponent(spinnerWaterChance, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(spinnerWaterMaxLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel28)
                                    .addComponent(jLabel40)
                                    .addComponent(spinnerWaterMinLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel60))
                                .addGap(6, 6, 6)
                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel21)
                                    .addComponent(spinnerLavaChance, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel29)
                                    .addComponent(spinnerLavaMaxLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel41)
                                    .addComponent(spinnerLavaMinLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel61))
                                .addGap(6, 6, 6)
                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel30)
                                    .addComponent(spinnerDirtChance, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel31)
                                    .addComponent(spinnerDirtMaxLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel42)
                                    .addComponent(spinnerDirtMinLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel62))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel32)
                                    .addComponent(spinnerGravelChance, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel33)
                                    .addComponent(spinnerGravelMaxLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel43)
                                    .addComponent(spinnerGravelMinLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel63))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel19)
                                    .addComponent(spinnerRedstoneMaxLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel27)
                                    .addComponent(jLabel39)
                                    .addComponent(spinnerRedstoneChance, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(spinnerRedstoneMinLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel64))))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel51)
                                .addComponent(spinnerEmeraldChance, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel52)
                                .addComponent(spinnerEmeraldMaxLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel53)
                                .addComponent(spinnerEmeraldMinLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel59))
                            .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(spinnerQuartzChance, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel74)
                                .addComponent(jLabel75)
                                .addComponent(spinnerQuartzMinLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel77)
                                .addComponent(spinnerQuartzMaxLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel76))))
                    .addComponent(jSeparator3, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
        );

        jTabbedPane1.addTab("Resources", new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/resources.png")), jPanel4); // NOI18N

        checkBoxPopulate.setText("Allow Minecraft to populate the entire terrain");
        checkBoxPopulate.setToolTipText("<html>This will mark the entire terrain as unpopulated, causing Minecraft to generate trees,<br/>\nwater and lava pools, pockets of dirt, gravel, coal, ore, etc. (but not caverns) as the<br/>\nchunks are loaded. This will slow down the initial loading of each chunk, and you have<br/>\nno control over where trees, snow, etc. appear.</html>");
        checkBoxPopulate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxPopulateActionPerformed(evt);
            }
        });

        checkBoxDeciduousEverywhere.setText("Deciduous forest everywhere");
        checkBoxDeciduousEverywhere.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxDeciduousEverywhereActionPerformed(evt);
            }
        });

        sliderDeciduousLevel.setMajorTickSpacing(1);
        sliderDeciduousLevel.setMaximum(15);
        sliderDeciduousLevel.setMinimum(1);
        sliderDeciduousLevel.setPaintTicks(true);
        sliderDeciduousLevel.setSnapToTicks(true);
        sliderDeciduousLevel.setValue(8);

        checkBoxFrostEverywhere.setText("Frost everywhere");

        checkBoxPineEverywhere.setText("Pine forest everywhere");
        checkBoxPineEverywhere.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxPineEverywhereActionPerformed(evt);
            }
        });

        sliderPineLevel.setMajorTickSpacing(1);
        sliderPineLevel.setMaximum(15);
        sliderPineLevel.setMinimum(1);
        sliderPineLevel.setPaintTicks(true);
        sliderPineLevel.setSnapToTicks(true);
        sliderPineLevel.setValue(8);

        checkBoxSmoothSnow.setText("Smooth snow (also applies to hand-painted Frost layer)");

        jLabel47.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/populate.png"))); // NOI18N

        jLabel48.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/deciduousforest.png"))); // NOI18N

        jLabel49.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/pineforest.png"))); // NOI18N

        jLabel50.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/frost.png"))); // NOI18N

        checkBoxJungleEverywhere.setText("Jungle everywhere");
        checkBoxJungleEverywhere.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxJungleEverywhereActionPerformed(evt);
            }
        });

        jLabel68.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/jungle.png"))); // NOI18N

        sliderJungleLevel.setMajorTickSpacing(1);
        sliderJungleLevel.setMaximum(15);
        sliderJungleLevel.setMinimum(1);
        sliderJungleLevel.setPaintTicks(true);
        sliderJungleLevel.setSnapToTicks(true);
        sliderJungleLevel.setValue(8);

        checkBoxSwamplandEverywhere.setText("Swampland everywhere");
        checkBoxSwamplandEverywhere.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxSwamplandEverywhereActionPerformed(evt);
            }
        });

        jLabel69.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/swampland.png"))); // NOI18N

        jSlider6.setMajorTickSpacing(1);
        jSlider6.setMaximum(15);
        jSlider6.setMinimum(1);
        jSlider6.setPaintTicks(true);
        jSlider6.setSnapToTicks(true);
        jSlider6.setValue(8);

        checkBoxSnowUnderTrees.setText("Frost under trees (also applies to hand-painted Frost layer)");

        checkBoxExportAnnotations.setText("Export the annotations (as coloured wool)");
        checkBoxExportAnnotations.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxExportAnnotationsActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(checkBoxPopulate)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel47))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(checkBoxFrostEverywhere)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel50))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(checkBoxSmoothSnow)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(sliderDeciduousLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(sliderPineLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(67, 67, 67)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addComponent(checkBoxJungleEverywhere)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jLabel68))
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addComponent(checkBoxSwamplandEverywhere)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jLabel69))
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addGap(12, 12, 12)
                                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(jSlider6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(sliderJungleLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                            .addComponent(checkBoxSnowUnderTrees)))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(checkBoxDeciduousEverywhere)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(checkBoxPineEverywhere)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel49)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel48))
                    .addComponent(checkBoxExportAnnotations))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(checkBoxPopulate)
                            .addComponent(jLabel47))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(checkBoxDeciduousEverywhere)
                                    .addComponent(jLabel48))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(sliderDeciduousLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(checkBoxJungleEverywhere)
                                    .addComponent(jLabel68))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(sliderJungleLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGap(18, 18, 18)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(checkBoxPineEverywhere)
                                    .addComponent(jLabel49)))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGap(18, 18, 18)
                                .addComponent(jLabel69))))
                    .addComponent(checkBoxSwamplandEverywhere, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sliderPineLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jSlider6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(checkBoxFrostEverywhere)
                    .addComponent(jLabel50))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(checkBoxSmoothSnow)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(checkBoxSnowUnderTrees)
                .addGap(18, 18, 18)
                .addComponent(checkBoxExportAnnotations)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Other Layers", jPanel2);

        jLabel82.setText("<html>On this page you can configure the export order of your custom layers,<br>as well as prevent certain layers from being exported.</html>");

        tableCustomLayers.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane1.setViewportView(tableCustomLayers);

        buttonCustomLayerUp.setText("Up");
        buttonCustomLayerUp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCustomLayerUpActionPerformed(evt);
            }
        });

        buttonCustomLayerDown.setText("Down");
        buttonCustomLayerDown.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCustomLayerDownActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addComponent(jLabel82, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 618, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(buttonCustomLayerDown, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(buttonCustomLayerUp, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap())
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel82, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addComponent(buttonCustomLayerUp)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonCustomLayerDown)
                        .addGap(0, 224, Short.MAX_VALUE)))
                .addContainerGap())
        );

        jTabbedPane1.addTab("Custom Layers", jPanel7);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(jTabbedPane1))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 362, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void radioButtonWaterBorderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonWaterBorderActionPerformed
        setControlStates();
        if ((Integer) spinnerBorderLevel.getValue() == (dimension.getMaxHeight() - 1)) {
            spinnerBorderLevel.setValue(dimension.getBorderLevel());
        }
        notifyBorderListeners();
}//GEN-LAST:event_radioButtonWaterBorderActionPerformed

    private void radioButtonNoBorderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonNoBorderActionPerformed
        setControlStates();
        notifyBorderListeners();
}//GEN-LAST:event_radioButtonNoBorderActionPerformed

    private void checkBoxPopulateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxPopulateActionPerformed
        if (checkBoxPopulate.isSelected() && jCheckBox8.isSelected()) {
            jCheckBox8.setSelected(false);
            setControlStates();
            JOptionPane.showMessageDialog(this, "\"Resources everywhere\" disabled on the Resources tab,\nto avoid duplicate resources. You may enable it again manually.", "Resources Everywhere Disabled", JOptionPane.INFORMATION_MESSAGE);
        }
}//GEN-LAST:event_checkBoxPopulateActionPerformed

    private void radioButtonVoidBorderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonVoidBorderActionPerformed
        setControlStates();
        notifyBorderListeners();
}//GEN-LAST:event_radioButtonVoidBorderActionPerformed

    private void radioButtonLavaBorderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonLavaBorderActionPerformed
        setControlStates();
        if ((Integer) spinnerBorderLevel.getValue() == (dimension.getMaxHeight() - 1)) {
            spinnerBorderLevel.setValue(dimension.getBorderLevel());
        }
        notifyBorderListeners();
}//GEN-LAST:event_radioButtonLavaBorderActionPerformed

    private void comboBoxSubsurfaceMaterialActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxSubsurfaceMaterialActionPerformed
        //        if (comboBoxSubsurfaceMaterial.getSelectedItem() == Terrain.RESOURCES) {
        //            // TODO: do something?
        //        }
}//GEN-LAST:event_comboBoxSubsurfaceMaterialActionPerformed

    private void checkBoxDeciduousEverywhereActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxDeciduousEverywhereActionPerformed
        setControlStates();
    }//GEN-LAST:event_checkBoxDeciduousEverywhereActionPerformed

    private void checkBoxPineEverywhereActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxPineEverywhereActionPerformed
        setControlStates();
    }//GEN-LAST:event_checkBoxPineEverywhereActionPerformed

    private void spinnerBorderSizeStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerBorderSizeStateChanged
        int value = (Integer) spinnerBorderSize.getValue();
        value = Math.round(value / 128f) * 128;
        if (value < 128) {
            value = 128;
        }
        spinnerBorderSize.setValue(value);
    }//GEN-LAST:event_spinnerBorderSizeStateChanged

    private void jCheckBox8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox8ActionPerformed
        setControlStates();
    }//GEN-LAST:event_jCheckBox8ActionPerformed

    private void spinnerMinSurfaceDepthStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerMinSurfaceDepthStateChanged
        int topLevelMinDepth = (Integer) spinnerMinSurfaceDepth.getValue();
        int topLevelMaxDepth = (Integer) spinnerMaxSurfaceDepth.getValue();
        if (topLevelMinDepth > topLevelMaxDepth) {
            spinnerMaxSurfaceDepth.setValue(topLevelMinDepth);
        }
    }//GEN-LAST:event_spinnerMinSurfaceDepthStateChanged

    private void spinnerMaxSurfaceDepthStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerMaxSurfaceDepthStateChanged
        int topLevelMinDepth = (Integer) spinnerMinSurfaceDepth.getValue();
        int topLevelMaxDepth = (Integer) spinnerMaxSurfaceDepth.getValue();
        if (topLevelMaxDepth < topLevelMinDepth) {
            spinnerMinSurfaceDepth.setValue(topLevelMaxDepth);
        }
    }//GEN-LAST:event_spinnerMaxSurfaceDepthStateChanged

    private void checkBoxCavernsBreakSurfaceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxCavernsBreakSurfaceActionPerformed
        setControlStates();
    }//GEN-LAST:event_checkBoxCavernsBreakSurfaceActionPerformed

    private void checkBoxFloodCavernsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxFloodCavernsActionPerformed
        setControlStates();
    }//GEN-LAST:event_checkBoxFloodCavernsActionPerformed

    private void checkBoxCavernsEverywhereActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxCavernsEverywhereActionPerformed
        setControlStates();
    }//GEN-LAST:event_checkBoxCavernsEverywhereActionPerformed

    private void checkBoxChasmsEverywhereActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxChasmsEverywhereActionPerformed
        setControlStates();
    }//GEN-LAST:event_checkBoxChasmsEverywhereActionPerformed

    private void checkBoxChasmsBreakSurfaceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxChasmsBreakSurfaceActionPerformed
        setControlStates();
    }//GEN-LAST:event_checkBoxChasmsBreakSurfaceActionPerformed

    private void checkBoxJungleEverywhereActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxJungleEverywhereActionPerformed
        setControlStates();
    }//GEN-LAST:event_checkBoxJungleEverywhereActionPerformed

    private void checkBoxSwamplandEverywhereActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxSwamplandEverywhereActionPerformed
        setControlStates();
    }//GEN-LAST:event_checkBoxSwamplandEverywhereActionPerformed

    private void checkBoxExportAnnotationsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxExportAnnotationsActionPerformed
        setControlStates();
    }//GEN-LAST:event_checkBoxExportAnnotationsActionPerformed

    private void buttonCustomLayerUpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCustomLayerUpActionPerformed
        int rowIndex = tableCustomLayers.getSelectedRow();
        customLayersTableModel.swap(rowIndex - 1, rowIndex);
        tableCustomLayers.getSelectionModel().setSelectionInterval(rowIndex - 1, rowIndex - 1);
        tableCustomLayers.scrollRectToVisible(tableCustomLayers.getCellRect(rowIndex - 1, 0, true));
        setControlStates();
    }//GEN-LAST:event_buttonCustomLayerUpActionPerformed

    private void buttonCustomLayerDownActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCustomLayerDownActionPerformed
        int rowIndex = tableCustomLayers.getSelectedRow();
        customLayersTableModel.swap(rowIndex, rowIndex + 1);
        tableCustomLayers.getSelectionModel().setSelectionInterval(rowIndex + 1, rowIndex + 1);
        tableCustomLayers.scrollRectToVisible(tableCustomLayers.getCellRect(rowIndex + 1, 0, true));
        setControlStates();
    }//GEN-LAST:event_buttonCustomLayerDownActionPerformed

    private void radioButtonFixedBorderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonFixedBorderActionPerformed
        setControlStates();
        notifyBorderListeners();
    }//GEN-LAST:event_radioButtonFixedBorderActionPerformed

    private void radioButtonEndlessBorderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonEndlessBorderActionPerformed
        setControlStates();
        notifyBorderListeners();
    }//GEN-LAST:event_radioButtonEndlessBorderActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonCustomLayerDown;
    private javax.swing.JButton buttonCustomLayerUp;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.ButtonGroup buttonGroup3;
    private javax.swing.JCheckBox checkBoxBedrockWall;
    private javax.swing.JCheckBox checkBoxBottomless;
    private javax.swing.JCheckBox checkBoxCavernsBreakSurface;
    private javax.swing.JCheckBox checkBoxCavernsEverywhere;
    private javax.swing.JCheckBox checkBoxCavernsFloodWithLava;
    private javax.swing.JCheckBox checkBoxCavernsRemoveWater;
    private javax.swing.JCheckBox checkBoxChasmsBreakSurface;
    private javax.swing.JCheckBox checkBoxChasmsEverywhere;
    private javax.swing.JCheckBox checkBoxCoverSteepTerrain;
    private javax.swing.JCheckBox checkBoxDeciduousEverywhere;
    private javax.swing.JCheckBox checkBoxExportAnnotations;
    private javax.swing.JCheckBox checkBoxFloodCaverns;
    private javax.swing.JCheckBox checkBoxFrostEverywhere;
    private javax.swing.JCheckBox checkBoxJungleEverywhere;
    private javax.swing.JCheckBox checkBoxPineEverywhere;
    private javax.swing.JCheckBox checkBoxPopulate;
    private javax.swing.JCheckBox checkBoxSmoothSnow;
    private javax.swing.JCheckBox checkBoxSnowUnderTrees;
    private javax.swing.JCheckBox checkBoxSwamplandEverywhere;
    private javax.swing.JComboBox comboBoxSubsurfaceMaterial;
    private javax.swing.JCheckBox jCheckBox8;
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
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel31;
    private javax.swing.JLabel jLabel32;
    private javax.swing.JLabel jLabel33;
    private javax.swing.JLabel jLabel34;
    private javax.swing.JLabel jLabel35;
    private javax.swing.JLabel jLabel36;
    private javax.swing.JLabel jLabel37;
    private javax.swing.JLabel jLabel38;
    private javax.swing.JLabel jLabel39;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel40;
    private javax.swing.JLabel jLabel41;
    private javax.swing.JLabel jLabel42;
    private javax.swing.JLabel jLabel43;
    private javax.swing.JLabel jLabel44;
    private javax.swing.JLabel jLabel45;
    private javax.swing.JLabel jLabel46;
    private javax.swing.JLabel jLabel47;
    private javax.swing.JLabel jLabel48;
    private javax.swing.JLabel jLabel49;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel50;
    private javax.swing.JLabel jLabel51;
    private javax.swing.JLabel jLabel52;
    private javax.swing.JLabel jLabel53;
    private javax.swing.JLabel jLabel54;
    private javax.swing.JLabel jLabel55;
    private javax.swing.JLabel jLabel56;
    private javax.swing.JLabel jLabel57;
    private javax.swing.JLabel jLabel58;
    private javax.swing.JLabel jLabel59;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel60;
    private javax.swing.JLabel jLabel61;
    private javax.swing.JLabel jLabel62;
    private javax.swing.JLabel jLabel63;
    private javax.swing.JLabel jLabel64;
    private javax.swing.JLabel jLabel65;
    private javax.swing.JLabel jLabel66;
    private javax.swing.JLabel jLabel67;
    private javax.swing.JLabel jLabel68;
    private javax.swing.JLabel jLabel69;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel70;
    private javax.swing.JLabel jLabel71;
    private javax.swing.JLabel jLabel72;
    private javax.swing.JLabel jLabel73;
    private javax.swing.JLabel jLabel74;
    private javax.swing.JLabel jLabel75;
    private javax.swing.JLabel jLabel76;
    private javax.swing.JLabel jLabel77;
    private javax.swing.JLabel jLabel78;
    private javax.swing.JLabel jLabel79;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel80;
    private javax.swing.JLabel jLabel81;
    private javax.swing.JLabel jLabel82;
    private javax.swing.JLabel jLabel85;
    private javax.swing.JLabel jLabel86;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JLabel jLabel90;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSlider jSlider4;
    private javax.swing.JSlider jSlider6;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JRadioButton radioButtonEndlessBorder;
    private javax.swing.JRadioButton radioButtonFixedBorder;
    private javax.swing.JRadioButton radioButtonLavaBorder;
    private javax.swing.JRadioButton radioButtonNoBorder;
    private javax.swing.JRadioButton radioButtonVoidBorder;
    private javax.swing.JRadioButton radioButtonWaterBorder;
    private javax.swing.JSlider sliderCavernsEverywhereLevel;
    private javax.swing.JSlider sliderChasmsEverywhereLevel;
    private javax.swing.JSlider sliderDeciduousLevel;
    private javax.swing.JSlider sliderJungleLevel;
    private javax.swing.JSlider sliderPineLevel;
    private javax.swing.JSpinner spinnerBorderLevel;
    private javax.swing.JSpinner spinnerBorderSize;
    private javax.swing.JSpinner spinnerCavernsFloodLevel;
    private javax.swing.JSpinner spinnerCavernsMaxLevel;
    private javax.swing.JSpinner spinnerCavernsMinLevel;
    private javax.swing.JSpinner spinnerCeilingHeight;
    private javax.swing.JSpinner spinnerChasmsMaxLevel;
    private javax.swing.JSpinner spinnerChasmsMinLevel;
    private javax.swing.JSpinner spinnerCoalChance;
    private javax.swing.JSpinner spinnerCoalMaxLevel;
    private javax.swing.JSpinner spinnerCoalMinLevel;
    private javax.swing.JSpinner spinnerDiamondChance;
    private javax.swing.JSpinner spinnerDiamondMaxLevel;
    private javax.swing.JSpinner spinnerDiamondMinLevel;
    private javax.swing.JSpinner spinnerDirtChance;
    private javax.swing.JSpinner spinnerDirtMaxLevel;
    private javax.swing.JSpinner spinnerDirtMinLevel;
    private javax.swing.JSpinner spinnerEmeraldChance;
    private javax.swing.JSpinner spinnerEmeraldMaxLevel;
    private javax.swing.JSpinner spinnerEmeraldMinLevel;
    private javax.swing.JSpinner spinnerGoldChance;
    private javax.swing.JSpinner spinnerGoldMaxLevel;
    private javax.swing.JSpinner spinnerGoldMinLevel;
    private javax.swing.JSpinner spinnerGravelChance;
    private javax.swing.JSpinner spinnerGravelMaxLevel;
    private javax.swing.JSpinner spinnerGravelMinLevel;
    private javax.swing.JSpinner spinnerIronChance;
    private javax.swing.JSpinner spinnerIronMaxLevel;
    private javax.swing.JSpinner spinnerIronMinLevel;
    private javax.swing.JSpinner spinnerLapisChance;
    private javax.swing.JSpinner spinnerLapisMaxLevel;
    private javax.swing.JSpinner spinnerLapisMinLevel;
    private javax.swing.JSpinner spinnerLavaChance;
    private javax.swing.JSpinner spinnerLavaMaxLevel;
    private javax.swing.JSpinner spinnerLavaMinLevel;
    private javax.swing.JSpinner spinnerMaxSurfaceDepth;
    private javax.swing.JSpinner spinnerMcBorderCentreX;
    private javax.swing.JSpinner spinnerMcBorderCentreY;
    private javax.swing.JSpinner spinnerMcBorderSize;
    private javax.swing.JSpinner spinnerMinSurfaceDepth;
    private javax.swing.JSpinner spinnerMinecraftSeed;
    private javax.swing.JSpinner spinnerQuartzChance;
    private javax.swing.JSpinner spinnerQuartzMaxLevel;
    private javax.swing.JSpinner spinnerQuartzMinLevel;
    private javax.swing.JSpinner spinnerRedstoneChance;
    private javax.swing.JSpinner spinnerRedstoneMaxLevel;
    private javax.swing.JSpinner spinnerRedstoneMinLevel;
    private javax.swing.JSpinner spinnerWaterChance;
    private javax.swing.JSpinner spinnerWaterMaxLevel;
    private javax.swing.JSpinner spinnerWaterMinLevel;
    private javax.swing.JTable tableCustomLayers;
    private org.pepsoft.worldpainter.themes.impl.simple.SimpleThemeEditor themeEditor;
    // End of variables declaration//GEN-END:variables

    private Dimension dimension;
    private CustomLayersTableModel customLayersTableModel;
    private Mode mode;
    private List<BorderListener> borderListeners = new ArrayList<>();
    
    private static final long serialVersionUID = 1L;
    
    public enum Mode {EXPORT, DEFAULT_SETTINGS, EDITOR}

    public interface BorderListener {
        void borderChanged(Dimension.Border newBorder);
    }
}