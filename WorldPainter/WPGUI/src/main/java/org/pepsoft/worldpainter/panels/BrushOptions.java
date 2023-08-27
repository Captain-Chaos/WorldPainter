/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.panels;

import org.pepsoft.util.ObservableBoolean;
import org.pepsoft.util.swing.BetterJPopupMenu;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.biomeschemes.BiomeHelper;
import org.pepsoft.worldpainter.biomeschemes.CustomBiome;
import org.pepsoft.worldpainter.biomeschemes.CustomBiomeManager;
import org.pepsoft.worldpainter.layers.*;
import org.pepsoft.worldpainter.operations.Filter;
import org.pepsoft.worldpainter.panels.DefaultFilter.LayerValue;
import org.pepsoft.worldpainter.panels.DefaultFilter.LevelType;
import org.pepsoft.worldpainter.tools.Eyedropper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.JSpinner.NumberEditor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

import static java.awt.RenderingHints.KEY_INTERPOLATION;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static java.lang.Math.abs;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.pepsoft.util.GUIUtils.getUIScale;
import static org.pepsoft.util.IconUtils.createScaledColourIcon;
import static org.pepsoft.util.IconUtils.loadScaledIcon;
import static org.pepsoft.worldpainter.Platform.Capability.NAMED_BIOMES;
import static org.pepsoft.worldpainter.Terrain.STAINED_TERRACOTTAS;
import static org.pepsoft.worldpainter.util.BiomeUtils.getBiomeScheme;

/**
 *
 * @author pepijn
 */
@SuppressWarnings("unused") // Managed by NetBeans
public class BrushOptions extends javax.swing.JPanel implements Observer {
    /**
     * Creates new form BrushOptions
     */
    public BrushOptions() {
        initComponents();
        
        // Eliminate thousands separators to make spinners smaller:
        spinnerAbove.setEditor(new NumberEditor(spinnerAbove, "0"));
        spinnerBelow.setEditor(new NumberEditor(spinnerBelow, "0"));
    }

    public ColourScheme getColourScheme() {
        return colourScheme;
    }

    public void setColourScheme(ColourScheme colourScheme) {
        this.colourScheme = colourScheme;
    }

    public CustomBiomeManager getCustomBiomeManager() {
        return customBiomeManager;
    }

    public void setCustomBiomeManager(CustomBiomeManager customBiomeManager) {
        this.customBiomeManager = customBiomeManager;
    }

    public void setSelectionState(ObservableBoolean selectionState) {
        if (this.selectionState != null) {
            throw new IllegalStateException("selectionState already set");
        }
        this.selectionState = selectionState;
        selectionState.addObserver(this);
        checkBoxInSelection.setEnabled(selectionState.getValue());
        checkBoxOutsideSelection.setEnabled(selectionState.getValue());
    }

    public DefaultFilter getFilter() {
        if (checkBoxInSelection.isSelected() || checkBoxOutsideSelection.isSelected() || checkBoxAbove.isSelected()
                || checkBoxBelow.isSelected() || checkBoxReplace.isSelected() || (onlyOn != null)
                || checkBoxExceptOn.isSelected() || (exceptOn != null) || checkBoxAboveSlope.isSelected()
                || checkBoxBelowSlope.isSelected()) {
            return new DefaultFilter(App.getInstance().getDimension(),
                    checkBoxInSelection.isSelected(),
                    checkBoxOutsideSelection.isSelected(),
                    checkBoxAbove.isSelected() ? (Integer) spinnerAbove.getValue() : Integer.MIN_VALUE,
                    checkBoxBelow.isSelected() ? (Integer) spinnerBelow.getValue() : Integer.MIN_VALUE,
                    checkBoxFeather.isSelected(),
                    checkBoxReplace.isSelected(),
                    onlyOn,
                    checkBoxExceptOn.isSelected(),
                    exceptOn,
                    (checkBoxAboveSlope.isSelected() || checkBoxBelowSlope.isSelected()) ? (Integer) spinnerSlope.getValue() : -1,
                    checkBoxAboveSlope.isSelected());
        } else {
            return null;
        }
    }
    
    public void setFilter(DefaultFilter filter) {
        if (filter == null) {
            checkBoxInSelection.setSelected(false);
            checkBoxOutsideSelection.setSelected(false);
            checkBoxAbove.setSelected(false);
            checkBoxBelow.setSelected(false);
            checkBoxReplace.setSelected(false);
            checkBoxExceptOn.setSelected(false);
            checkBoxAboveSlope.setSelected(false);
            checkBoxBelowSlope.setSelected(false);
            onlyOn = exceptOn = null;
        } else {
            checkBoxInSelection.setSelected(filter.inSelection);
            checkBoxOutsideSelection.setSelected(filter.outsideSelection);
            checkBoxAbove.setSelected(filter.levelType == LevelType.ABOVE || filter.levelType == LevelType.BETWEEN || filter.levelType == LevelType.OUTSIDE);
            if (filter.aboveLevel != Integer.MIN_VALUE) {
                spinnerAbove.setValue(filter.aboveLevel);
            }
            checkBoxBelow.setSelected(filter.levelType == LevelType.BELOW || filter.levelType == LevelType.BETWEEN || filter.levelType == LevelType.OUTSIDE);
            if (filter.belowLevel != Integer.MIN_VALUE) {
                spinnerBelow.setValue(filter.belowLevel);
            }
            checkBoxAboveSlope.setSelected(filter.checkSlope && filter.slopeIsAbove);
            checkBoxBelowSlope.setSelected(filter.checkSlope && (! filter.slopeIsAbove));
            if (filter.degrees >= 0) {
                spinnerSlope.setValue(filter.degrees);
            }
            checkBoxReplace.setSelected(filter.onlyOn);
            checkBoxExceptOn.setSelected(filter.exceptOn);
            final App app = App.getInstance();
            if (filter.onlyOnFilter instanceof OnlyOnTerrainOrLayerFilter) {
                onlyOn = toPaint((OnlyOnTerrainOrLayerFilter) filter.onlyOnFilter);
            } else if (filter.onlyOnFilter instanceof CombinedFilter) {
                onlyOn = ((CombinedFilter) filter.onlyOnFilter).getFilters().stream()
                        .map(f -> toPaint((OnlyOnTerrainOrLayerFilter) f))
                        .collect(Collectors.toCollection(ArrayList::new));
            } else if (filter.onlyOnFilter != null) {
                throw new UnsupportedOperationException("Unsupported onlyOn filter type " + filter.onlyOnFilter.getClass());
            }
            if (filter.exceptOnFilter instanceof ExceptOnTerrainOrLayerFilter) {
                exceptOn = toPaint((ExceptOnTerrainOrLayerFilter) filter.exceptOnFilter);
            } else if (filter.exceptOnFilter instanceof CombinedFilter) {
                exceptOn = ((CombinedFilter) filter.exceptOnFilter).getFilters().stream()
                        .map(f -> toPaint((ExceptOnTerrainOrLayerFilter) f))
                        .collect(Collectors.toCollection(ArrayList::new));
            } else if (filter.exceptOnFilter != null) {
                throw new UnsupportedOperationException("Unsupported exceptOn filter type " + filter.exceptOnFilter.getClass());
            }
        }
        installPaint(onlyOn, buttonReplace, checkBoxReplace);
        installPaint(exceptOn, buttonExceptOn, checkBoxExceptOn);
        setControlStates();
    }

    public void setMinHeight(int minHeight) {
        boolean updateFilter = false;
        ((SpinnerNumberModel) spinnerAbove.getModel()).setMinimum(minHeight);
        if ((Integer) spinnerAbove.getValue() < minHeight) {
            spinnerAbove.setValue(minHeight);
            updateFilter = true;
        }
        ((SpinnerNumberModel) spinnerBelow.getModel()).setMinimum(minHeight);
        if ((Integer) spinnerBelow.getValue() < minHeight) {
            spinnerBelow.setValue(minHeight);
            updateFilter = true;
        }
        if (updateFilter) {
            filterChanged();
        }
    }

    public void setMaxHeight(int maxHeight) {
        boolean updateFilter = false;
        ((SpinnerNumberModel) spinnerAbove.getModel()).setMaximum(maxHeight - 1);
        if ((Integer) spinnerAbove.getValue() >= maxHeight) {
            spinnerAbove.setValue(maxHeight - 1);
            updateFilter = true;
        }
        ((SpinnerNumberModel) spinnerBelow.getModel()).setMaximum(maxHeight - 1);
        if ((Integer) spinnerBelow.getValue() >= maxHeight) {
            spinnerBelow.setValue(maxHeight - 1);
            updateFilter = true;
        }
        if (updateFilter) {
            filterChanged();
        }
    }

    public Listener getListener() {
        return listener;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
    }

    public MapSelectionListener getMapSelectionListener() {
        return mapSelectionListener;
    }

    public void setMapSelectionListener(MapSelectionListener mapSelectionListener) {
        this.mapSelectionListener = mapSelectionListener;
    }
    
    public void setOnlyOn(Terrain terrain) {
        onlyOn = terrain;
        installPaint(onlyOn, buttonReplace, checkBoxReplace);
    }

    public void setOnlyOn(Layer layer, int value) {
        onlyOn = toPaint(layer, value);
        installPaint(onlyOn, buttonReplace, checkBoxReplace);
    }

    public void addOnlyOn(Terrain terrain) {
        onlyOn = addToSelection(onlyOn, terrain);
        installPaint(onlyOn, buttonReplace, checkBoxReplace);
    }

    public void addOnlyOn(Layer layer, int value) {
        onlyOn = addToSelection(onlyOn, toPaint(layer, value));
        installPaint(onlyOn, buttonReplace, checkBoxReplace);
    }

    public void setExceptOn(Terrain terrain) {
        exceptOn = terrain;
        installPaint(exceptOn, buttonExceptOn, checkBoxExceptOn);
    }

    public void setExceptOn(Layer layer, int value) {
        exceptOn = toPaint(layer, value);
        installPaint(exceptOn, buttonExceptOn, checkBoxExceptOn);
    }

    public void addExceptOn(Terrain terrain) {
        exceptOn = addToSelection(exceptOn, terrain);
        installPaint(exceptOn, buttonExceptOn, checkBoxExceptOn);
    }

    public void addExceptOn(Layer layer, int value) {
        exceptOn = addToSelection(exceptOn, toPaint(layer, value));
        installPaint(exceptOn, buttonExceptOn, checkBoxExceptOn);
    }

    // Observer

    @Override
    public void update(Observable o, Object selectionMayBePresent) {
        checkBoxInSelection.setEnabled((boolean) selectionMayBePresent);
        checkBoxOutsideSelection.setEnabled((boolean) selectionMayBePresent);
    }

    @SuppressWarnings("unchecked") // Guaranteed by code
    private Object addToSelection(Object currentSelection, Object item) {
        final List<Object> selection;
        if (currentSelection instanceof List) {
            selection = (List<Object>) currentSelection;
        } else {
            selection = new ArrayList<>(singleton(currentSelection));
        }
        selection.add(item);
        return selection;
    }

    @SuppressWarnings("unchecked") // Guaranteed by code
    private void installPaint(Object paint, AbstractButton button, JComponent... additionalComponents) {
        final String text, tooltip;
        final Icon icon;
        if (paint == null) {
            text = "...";
            icon = null;
            tooltip = null;
        } else if (paint instanceof List) {
            final List<Object> list = (List<Object>) paint;
            final String symbol = (button == buttonReplace) ? " + " : " / ";
            if (list.size() > 2) {
                text = shorten(getText(list.get(0))) + symbol + (list.size() - 1) + " more";
            } else {
                text = shorten(getText(list.get(0)) + symbol + shorten(getText(list.get(1))));
            }
            final List<ImageIcon> icons = list.stream()
                    .map(item -> (ImageIcon) getIcon(item))
                    .filter(Objects::nonNull)
                    .limit(2)
                    .collect(toList());
            if (icons.isEmpty()) {
                icon = null;
            } else if (icons.size() == 1) {
                icon = icons.get(0);
            } else {
                final int iconSize = Math.round(16 * getUIScale()), subSize = Math.round(12 * getUIScale());
                final BufferedImage image = new BufferedImage(iconSize, iconSize, TYPE_INT_ARGB);
                final Graphics2D g2 = image.createGraphics();
                try {
                    g2.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BICUBIC);
                    g2.drawImage(icons.get(0).getImage(), 0, 0, subSize, subSize, null);
                    g2.drawImage(icons.get(1).getImage(), (iconSize - subSize), (iconSize - subSize), subSize, subSize, null);
                } finally {
                    g2.dispose();
                }
                icon = new ImageIcon(image);
            }
            tooltip = "<html>" + ((List<Object>) paint).stream().map(this::getText).collect(joining("<br>")) + "</html>";
        } else {
            text = getText(paint);
            icon = getIcon(paint);
            tooltip = null;
        }
        button.setText(text);
        button.setIcon(icon);
        button.setToolTipText(tooltip);
        for (JComponent additionalComponent: additionalComponents) {
            additionalComponent.setToolTipText(tooltip);
        }
        filterChanged();
    }

    private String shorten(String text) {
        return (text.length() > 15) ? (text.substring(0, 12) + "...") : text;
    }

    private String getText(Object paint) {
        if (paint instanceof String) {
            switch ((String) paint) {
                case TerrainOrLayerFilter.WATER:
                    return "Water";
                case TerrainOrLayerFilter.LAVA:
                    return "Lava";
                case TerrainOrLayerFilter.LAND:
                    return "Land";
                case TerrainOrLayerFilter.AUTO_BIOMES:
                    return "All Auto Biomes";
                default:
                    throw new UnsupportedOperationException("Paint of type \"" + paint + "\" not supported");
            }
        } else if (paint instanceof Terrain) {
            return ((Terrain) paint).getName();
        } else if (paint instanceof LayerValue) {
            final LayerValue layerValue = (LayerValue) paint;
            if (layerValue.layer instanceof Biome) {
                return new BiomeHelper(colourScheme, customBiomeManager, platform).getBiomeName(abs(layerValue.value));
            } else if (layerValue.layer instanceof Annotations) {
                if (layerValue.value == -1) {
                    return "All Annotations";
                } else {
                    return Annotations.getColourName(layerValue.value) + " Annotations";
                }
            } else {
                throw new UnsupportedOperationException("Paint of layer type " + layerValue.layer.getClass() + " and value " + layerValue.value + " not supported");
            }
        } else if (paint instanceof Layer) {
            return ((Layer) paint).getName();
        } else {
            throw new UnsupportedOperationException("Paint of type " + paint.getClass() + " not supported");
        }
    }

    private Icon getIcon(Object paint) {
        if (paint instanceof String) {
            switch ((String) paint) {
                case TerrainOrLayerFilter.WATER:
                    return ICON_WATER;
                case TerrainOrLayerFilter.LAVA:
                    return ICON_LAVA;
                case TerrainOrLayerFilter.LAND:
                case TerrainOrLayerFilter.AUTO_BIOMES:
                    return null;
                default:
                    throw new UnsupportedOperationException("Paint of type \"" + paint + "\" not supported");
            }
        } else if (paint instanceof Terrain) {
            return new ImageIcon(((Terrain) paint).getScaledIcon(16, colourScheme));
        } else if (paint instanceof LayerValue) {
            final LayerValue layerValue = (LayerValue) paint;
            if (layerValue.layer instanceof Biome) {
                return new BiomeHelper(colourScheme, customBiomeManager, platform).getBiomeIcon(abs(layerValue.value));
            } else if (layerValue.layer instanceof Annotations) {
                if (layerValue.value == -1) {
                    return null;
                } else {
                    return createScaledColourIcon(Annotations.getColour(layerValue.value, colourScheme));
                }
            } else {
                throw new UnsupportedOperationException("Paint of layer type " + layerValue.layer.getClass() + " and value " + layerValue.value + " not supported");
            }
        } else if (paint instanceof Layer) {
            return new ImageIcon(((Layer) paint).getIcon());
        } else {
            throw new UnsupportedOperationException("Paint of type " + paint.getClass() + " not supported");
        }
    }

    private Object toPaint(Layer layer, int value) {
        if (layer instanceof Biome) {
            return new LayerValue(Biome.INSTANCE, value);
        } else if (layer instanceof Annotations) {
            return new LayerValue(Annotations.INSTANCE, value);
        } else if (layer.discrete) {
            throw new UnsupportedOperationException("Discrete layers not supported");
        } else {
            return layer;
        }
    }

    private Object toPaint(TerrainOrLayerFilter filter) {
        switch (filter.objectType) {
            case BIOME:
                if (filter.value == 255) {
                    return TerrainOrLayerFilter.AUTO_BIOMES;
                } else {
                    return new LayerValue(Biome.INSTANCE, filter.value);
                }
            case AUTO_BIOME:
                return new LayerValue(Biome.INSTANCE, -filter.value);
            case BIT_LAYER:
            case INT_LAYER_ANY:
                return filter.layer;
            case INT_LAYER_EQUAL:
                return new LayerValue(filter.layer, filter.value);
            case TERRAIN:
                return filter.terrain;
            case WATER:
                return TerrainOrLayerFilter.WATER;
            case LAND:
                return TerrainOrLayerFilter.LAND;
            case LAVA:
                return TerrainOrLayerFilter.LAVA;
            case ANNOTATION:
                return new LayerValue(Annotations.INSTANCE, filter.value);
            case ANNOTATION_ANY:
                return new LayerValue(Annotations.INSTANCE);
            default:
                throw new UnsupportedOperationException("Unsupported onlyOn filter type " + filter.objectType);
        }
    }

    private void setControlStates() {
        spinnerAbove.setEnabled(checkBoxAbove.isSelected());
        spinnerBelow.setEnabled(checkBoxBelow.isSelected());
        spinnerSlope.setEnabled(checkBoxAboveSlope.isSelected() || checkBoxBelowSlope.isSelected());
        buttonReplace.setEnabled(checkBoxReplace.isSelected());
        buttonExceptOn.setEnabled(checkBoxExceptOn.isSelected());
        checkBoxFeather.setEnabled(checkBoxAbove.isSelected() || checkBoxBelow.isSelected());
    }

    private JPopupMenu createReplaceMenu() {
        final JMenu menu = createObjectSelectionMenu(MENU_ONLY_ON, (object, name, icon) -> {
            onlyOn = object;
            installPaint(onlyOn, buttonReplace, checkBoxReplace);
        }, false, onlyOn);
        final JPopupMenu popupMenu = new BetterJPopupMenu();
        Arrays.stream(menu.getMenuComponents()).forEach(popupMenu::add);
        return popupMenu;
    }

    private JPopupMenu createExceptOnMenu() {
        final JMenu menu = createObjectSelectionMenu(MENU_EXCEPT_ON, (object, name, icon) -> {
            exceptOn = object;
            installPaint(exceptOn, buttonExceptOn, checkBoxExceptOn);
        }, false, exceptOn);
        final JPopupMenu popupMenu = new BetterJPopupMenu();
        Arrays.stream(menu.getMenuComponents()).forEach(popupMenu::add);
        return popupMenu;
    }
    
    @SuppressWarnings("unchecked") // Guaranteed by code
    private JMenu createObjectSelectionMenu(final String descriptor, final ObjectSelectionListener listener, final boolean addAnother, final Object currentSelection) {
        final JMenuItem waterItem = new JMenuItem("Water", ICON_WATER);
        waterItem.addActionListener(e -> listener.objectSelected(TerrainOrLayerFilter.WATER, "Water", null));
        JMenu popupMenu = new JMenu();
        popupMenu.add(waterItem);

        final JMenuItem lavaItem = new JMenuItem("Lava", ICON_LAVA);
        lavaItem.addActionListener(e -> listener.objectSelected(TerrainOrLayerFilter.LAVA, "Lava", null));
        popupMenu.add(lavaItem);

        final JMenuItem landItem = new JMenuItem("Land");
        landItem.addActionListener(e -> listener.objectSelected(TerrainOrLayerFilter.LAND, "Land", null));
        popupMenu.add(landItem);

        final JMenuItem eyedropperItem = new JMenuItem("Select on Map", ICON_EYEDROPPER);
        eyedropperItem.setToolTipText("Select a paint from the map");
        final App app = App.getInstance();
        final ColourScheme colourScheme = app.getColourScheme();
        if (mapSelectionListener != null) {
            eyedropperItem.addActionListener(e -> mapSelectionListener.mapSelectionRequested(descriptor, addAnother));
        } else {
            eyedropperItem.addActionListener(e -> App.getInstance().selectPaintOnMap(null, new Eyedropper.SelectionListener() {
                @Override
                public void terrainSelected(Terrain terrain) {
                    switch (descriptor) {
                        case MENU_ONLY_ON:
                            if (addAnother) {
                                addOnlyOn(terrain);
                            } else {
                                setOnlyOn(terrain);
                            }
                            break;
                        case MENU_EXCEPT_ON:
                            if (addAnother) {
                                addExceptOn(terrain);
                            } else {
                                setExceptOn(terrain);
                            }
                            break;
                    }
                }

                @Override
                public void layerSelected(Layer layer, int value) {
                    switch (descriptor) {
                        case MENU_ONLY_ON:
                            if (addAnother) {
                                addOnlyOn(layer, value);
                            } else {
                                setOnlyOn(layer, value);
                            }
                            break;
                        case MENU_EXCEPT_ON:
                            if (addAnother) {
                                addExceptOn(layer, value);
                            } else {
                                setExceptOn(layer, value);
                            }
                            break;
                    }
                }

                @Override public void selectionCancelled(boolean byUser) {}
            }));
        }
        popupMenu.add(eyedropperItem);

        final JMenu terrainMenu = new JMenu("Terrain");
        final JMenu customTerrainMenu = new JMenu("Custom");
        final JMenu stainedClayTerrainMenu = new JMenu("Stained Terracotta");
        for (Terrain terrain: Terrain.getConfiguredValues()) {
            final Terrain selectedTerrain = terrain;
            final String name = terrain.getName();
            final Icon icon = new ImageIcon(terrain.getScaledIcon(16, colourScheme));
            final JMenuItem menuItem = new JMenuItem(name, icon);
            menuItem.addActionListener(e -> listener.objectSelected(selectedTerrain, name, icon));
            if (terrain.isCustom()) {
                customTerrainMenu.add(menuItem);
            } else if (STAINED_TERRACOTTAS.contains(terrain)) {
                stainedClayTerrainMenu.add(menuItem);
            } else {
                terrainMenu.add(menuItem);
            }
        }
        terrainMenu.add(stainedClayTerrainMenu);
        if (customTerrainMenu.getMenuComponentCount() > 0) {
            terrainMenu.add(customTerrainMenu);
        }
        popupMenu.add(terrainMenu);
        
        final JMenu layerMenu = new JMenu("Layer");
        LayerManager.getInstance().getLayers().stream()
            .filter(layer -> ! layer.equals(Biome.INSTANCE))
            .forEach(layer -> {
                JMenuItem menuItem = new JMenuItem(layer.getName(), new ImageIcon(layer.getIcon()));
                menuItem.addActionListener(e -> listener.objectSelected(layer, layer.getName(), new ImageIcon(layer.getIcon())));
                layerMenu.add(menuItem);
            });
        final List<CustomLayer> customLayers = app.getCustomLayers();
        if (customLayers.size() > 15) {
            // If there are fifteen or more custom layers, split them by palette and move them to separate submenus to
            // try and conserve screen space
            app.getCustomLayersByPalette().entrySet().stream()
                .map((entry) -> {
                    String palette = entry.getKey();
                    JMenu paletteMenu = new JMenu(palette != null ? palette : "Hidden Layers");
                    entry.getValue().forEach(layer -> {
                        JMenuItem menuItem = new JMenuItem(layer.getName(), new ImageIcon(layer.getIcon()));
                        menuItem.addActionListener(e -> listener.objectSelected(layer, layer.getName(), new ImageIcon(layer.getIcon())));
                        paletteMenu.add(menuItem);
                    });
                    return paletteMenu;
                }).forEach(layerMenu::add);
        } else {
            customLayers.forEach(layer -> {
                JMenuItem menuItem = new JMenuItem(layer.getName(), new ImageIcon(layer.getIcon()));
                menuItem.addActionListener(e -> listener.objectSelected(layer, layer.getName(), new ImageIcon(layer.getIcon())));
                layerMenu.add(menuItem);
            });
        }
        popupMenu.add(layerMenu);

        final JMenu biomeMenu = new JMenu("Biome");
        final CustomBiomeManager customBiomeManager = app.getCustomBiomeManager();
        final BiomeHelper biomeHelper = new BiomeHelper(colourScheme, customBiomeManager, platform);
        List<CustomBiome> customBiomes = customBiomeManager.getCustomBiomes();
        if (! customBiomes.isEmpty()) {
            JMenu customBiomeMenu = new JMenu("Custom");
            for (CustomBiome customBiome: customBiomes) {
                final int selectedBiome = customBiome.getId();
                final String name = biomeHelper.getBiomeName(selectedBiome);
                final Icon icon = biomeHelper.getBiomeIcon(selectedBiome);
                final JMenuItem menuItem = new JMenuItem(name, icon);
                menuItem.addActionListener(e -> listener.objectSelected(new LayerValue(Biome.INSTANCE, selectedBiome), name, icon));
                customBiomeMenu.add(menuItem);
            }
            biomeMenu.add(customBiomeMenu);
        }
        final BiomeScheme biomeScheme = getBiomeScheme(platform);
        if (platform.capabilities.contains(NAMED_BIOMES)) {
            final Map<String, Integer> biomes = new TreeMap<>();
            for (int i = 0; i < biomeScheme.getBiomeCount(); i++) {
                if (biomeScheme.isBiomePresent(i)) {
                    biomes.put(biomeHelper.getBiomeName(i), i);
                }
            }
            biomes.forEach((name, id) -> {
                final Icon icon = biomeHelper.getBiomeIcon(id);
                final JMenuItem menuItem = new JMenuItem(name, icon);
                menuItem.addActionListener(e -> listener.objectSelected(new LayerValue(Biome.INSTANCE, id), name, icon));
                biomeMenu.add(menuItem);
            });
        } else {
            for (int i = 0; i < biomeScheme.getBiomeCount(); i++) {
                if (biomeScheme.isBiomePresent(i)) {
                    final int selectedBiome = i;
                    final String name = biomeHelper.getBiomeName(i);
                    final Icon icon = biomeHelper.getBiomeIcon(i);
                    final JMenuItem menuItem = new JMenuItem(name, icon);
                    menuItem.addActionListener(e -> listener.objectSelected(new LayerValue(Biome.INSTANCE, selectedBiome), name, icon));
                    biomeMenu.add(menuItem);
                }
            }
        }
        final JMenu autoBiomeSubMenu = new JMenu("Auto Biomes");
        final JMenuItem autoBiomesMenuItem = new JMenuItem("All Auto Biomes");
        autoBiomesMenuItem.addActionListener(e -> listener.objectSelected(TerrainOrLayerFilter.AUTO_BIOMES, "All Auto Biomes", null));
        autoBiomeSubMenu.add(autoBiomesMenuItem);
        for (int autoBiome: Dimension.POSSIBLE_AUTO_BIOMES) {
            final int selectedBiome = -autoBiome;
            final String name = biomeHelper.getBiomeName(autoBiome);
            final Icon icon = biomeHelper.getBiomeIcon(autoBiome);
            final JMenuItem menuItem = new JMenuItem(name, icon);
            menuItem.addActionListener(e -> listener.objectSelected(new LayerValue(Biome.INSTANCE, selectedBiome), name, icon));
            autoBiomeSubMenu.add(menuItem);
        }
        biomeMenu.add(autoBiomeSubMenu);
        popupMenu.add(biomeMenu);

        final JMenu annotationsMenu = new JMenu("Annotations");
        JMenuItem menuItem = new JMenuItem("All Annotations");
        menuItem.addActionListener(e -> listener.objectSelected(new LayerValue(Annotations.INSTANCE), "All Annotations", null));
        annotationsMenu.add(menuItem);
        for (int i = 1; i < 16; i++) {
            final int layerValue = i;
            final Icon icon  = createScaledColourIcon(Annotations.getColour(layerValue, colourScheme));
            final String colourName = Annotations.getColourName(layerValue);
            menuItem = new JMenuItem(colourName, icon);
            menuItem.addActionListener(e -> listener.objectSelected(new LayerValue(Annotations.INSTANCE, layerValue), colourName + " Annotations", icon));
            annotationsMenu.add(menuItem);
        }
        popupMenu.add(annotationsMenu);

        if ((! addAnother) && (currentSelection != null)) {
            final JMenu subMenu = createObjectSelectionMenu(descriptor, ((object, name, icon) -> {
                final List<Object> selection;
                if (currentSelection instanceof List) {
                    selection = (List<Object>) currentSelection;
                } else {
                    selection = new ArrayList<>();
                    selection.add(currentSelection);
                }
                selection.add(object);
                listener.objectSelected(selection, "Multiple", ICON_PLUS);
            }), true, currentSelection);
            subMenu.setText("Add Another");
            subMenu.setIcon(ICON_PLUS);
            popupMenu.add(subMenu);
        }

        popupMenu = breakUpLongMenus(popupMenu, 25);

        return popupMenu;
    }

    /**
     * Recursively break up any long menus by moving any excess items to
     * submenus called "More".
     *
     * @param menu The menu hierarchy to break up.
     * @param maxLength The maximum number of items in each submenu.
     * @return The broken up menu hierarchy.
     */
    private JMenu breakUpLongMenus(JMenu menu, int maxLength) {
        if (menu.getMenuComponentCount() > maxLength) {
            JMenu replacementMenu = new JMenu(menu.getText());
            replacementMenu.setToolTipText(menu.getToolTipText());

            // First gather all submenus, which go on the first page, breaking
            // them up on the go if necessary
            for (Component menuItem: menu.getMenuComponents()) {
                if (menuItem instanceof JMenu) {
                    menu.remove(menuItem);
                    replacementMenu.add(breakUpLongMenus((JMenu) menuItem, maxLength));
                }
            }

            // Then gather the menu items which will still fit on the first page
            // and insert them before the submenus
            int index = 0;
            while ((replacementMenu.getMenuComponentCount() < (maxLength - 1)) && (menu.getMenuComponentCount() > 0)) {
                Component menuItem = menu.getMenuComponent(0);
                menu.remove(0);
                replacementMenu.add(menuItem, index++);
            }

            // Lastly, if there are still items left, create a "more" submenu
            // from the rest of the menu items and insert it between the menu
            // items and the submenus
            if (menu.getMenuComponentCount() > 0) {
                menu.setText("More");
                menu.setToolTipText(null);
                replacementMenu.add(breakUpLongMenus(menu, maxLength), index);
            }

            return replacementMenu;
        } else {
            for (int i = 0; i < menu.getMenuComponentCount(); i++) {
                if (menu.getMenuComponent(i) instanceof JMenu) {
                    JMenu subMenu = (JMenu) menu.getMenuComponent(i);
                    menu.remove(i);
                    menu.add(breakUpLongMenus(subMenu, maxLength), i);
                }
            }
            return menu;
        }
    }

    private void showReplaceMenu() {
        App.getInstance().cancelPaintSelection();
        JPopupMenu menu = createReplaceMenu();
        menu.show(this, buttonReplace.getX() + buttonReplace.getWidth(), buttonReplace.getY());
    }
    
    private void showExceptOnMenu() {
        App.getInstance().cancelPaintSelection();
        JPopupMenu menu = createExceptOnMenu();
        menu.show(this, buttonExceptOn.getX() + buttonExceptOn.getWidth(), buttonExceptOn.getY());
    }
    
    private void filterChanged() {
        if (listener != null) {
            Filter filter = getFilter();
            if (logger.isTraceEnabled()) {
                logger.trace("Reporting new filter " + filter + " to listener " + listener);
            }
            listener.filterChanged(filter);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings({"Convert2Lambda", "Anonymous2MethodRef"}) // Managed by NetBeans
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        checkBoxAbove = new javax.swing.JCheckBox();
        spinnerAbove = new javax.swing.JSpinner();
        checkBoxBelow = new javax.swing.JCheckBox();
        spinnerBelow = new javax.swing.JSpinner();
        checkBoxReplace = new javax.swing.JCheckBox();
        buttonReplace = new javax.swing.JButton();
        checkBoxFeather = new javax.swing.JCheckBox();
        checkBoxExceptOn = new javax.swing.JCheckBox();
        buttonExceptOn = new javax.swing.JButton();
        checkBoxAboveSlope = new javax.swing.JCheckBox();
        checkBoxBelowSlope = new javax.swing.JCheckBox();
        spinnerSlope = new javax.swing.JSpinner();
        jLabel1 = new javax.swing.JLabel();
        checkBoxInSelection = new javax.swing.JCheckBox();
        checkBoxOutsideSelection = new javax.swing.JCheckBox();

        checkBoxAbove.setFont(checkBoxAbove.getFont().deriveFont(checkBoxAbove.getFont().getSize()-1f));
        checkBoxAbove.setText("at or above");
        checkBoxAbove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxAboveActionPerformed(evt);
            }
        });

        spinnerAbove.setFont(spinnerAbove.getFont().deriveFont(spinnerAbove.getFont().getSize()-1f));
        spinnerAbove.setModel(new javax.swing.SpinnerNumberModel(0, 0, 255, 1));
        spinnerAbove.setEnabled(false);
        spinnerAbove.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerAboveStateChanged(evt);
            }
        });

        checkBoxBelow.setFont(checkBoxBelow.getFont().deriveFont(checkBoxBelow.getFont().getSize()-1f));
        checkBoxBelow.setText("at or below");
        checkBoxBelow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxBelowActionPerformed(evt);
            }
        });

        spinnerBelow.setFont(spinnerBelow.getFont().deriveFont(spinnerBelow.getFont().getSize()-1f));
        spinnerBelow.setModel(new javax.swing.SpinnerNumberModel(255, 0, 255, 1));
        spinnerBelow.setEnabled(false);
        spinnerBelow.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerBelowStateChanged(evt);
            }
        });

        checkBoxReplace.setFont(checkBoxReplace.getFont().deriveFont(checkBoxReplace.getFont().getSize()-1f));
        checkBoxReplace.setText("only on");
        checkBoxReplace.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxReplaceActionPerformed(evt);
            }
        });

        buttonReplace.setFont(buttonReplace.getFont().deriveFont(buttonReplace.getFont().getSize()-1f));
        buttonReplace.setText("...");
        buttonReplace.setEnabled(false);
        buttonReplace.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonReplaceActionPerformed(evt);
            }
        });

        checkBoxFeather.setFont(checkBoxFeather.getFont().deriveFont(checkBoxFeather.getFont().getSize()-1f));
        checkBoxFeather.setText("feather");
        checkBoxFeather.setEnabled(false);
        checkBoxFeather.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxFeatherActionPerformed(evt);
            }
        });

        checkBoxExceptOn.setFont(checkBoxExceptOn.getFont().deriveFont(checkBoxExceptOn.getFont().getSize()-1f));
        checkBoxExceptOn.setText("except on");
        checkBoxExceptOn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxExceptOnActionPerformed(evt);
            }
        });

        buttonExceptOn.setFont(buttonExceptOn.getFont().deriveFont(buttonExceptOn.getFont().getSize()-1f));
        buttonExceptOn.setText("...");
        buttonExceptOn.setEnabled(false);
        buttonExceptOn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonExceptOnActionPerformed(evt);
            }
        });

        checkBoxAboveSlope.setFont(checkBoxAboveSlope.getFont().deriveFont(checkBoxAboveSlope.getFont().getSize()-1f));
        checkBoxAboveSlope.setText("above");
        checkBoxAboveSlope.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxAboveSlopeActionPerformed(evt);
            }
        });

        checkBoxBelowSlope.setFont(checkBoxBelowSlope.getFont().deriveFont(checkBoxBelowSlope.getFont().getSize()-1f));
        checkBoxBelowSlope.setText("below");
        checkBoxBelowSlope.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxBelowSlopeActionPerformed(evt);
            }
        });

        spinnerSlope.setFont(spinnerSlope.getFont().deriveFont(spinnerSlope.getFont().getSize()-1f));
        spinnerSlope.setModel(new javax.swing.SpinnerNumberModel(45, 0, 89, 1));
        spinnerSlope.setEnabled(false);
        spinnerSlope.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerSlopeStateChanged(evt);
            }
        });

        jLabel1.setFont(jLabel1.getFont().deriveFont(jLabel1.getFont().getSize()-1f));
        jLabel1.setText("degrees");

        checkBoxInSelection.setFont(checkBoxInSelection.getFont().deriveFont(checkBoxInSelection.getFont().getSize()-1f));
        checkBoxInSelection.setText("inside selection");
        checkBoxInSelection.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxInSelectionActionPerformed(evt);
            }
        });

        checkBoxOutsideSelection.setFont(checkBoxOutsideSelection.getFont().deriveFont(checkBoxOutsideSelection.getFont().getSize()-1f));
        checkBoxOutsideSelection.setText("outside selection");
        checkBoxOutsideSelection.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxOutsideSelectionActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(checkBoxInSelection)
            .addComponent(checkBoxBelow)
            .addComponent(checkBoxReplace)
            .addComponent(checkBoxExceptOn)
            .addGroup(layout.createSequentialGroup()
                .addComponent(checkBoxAboveSlope)
                .addGap(0, 0, 0)
                .addComponent(checkBoxBelowSlope))
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(spinnerAbove, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(spinnerBelow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(checkBoxFeather)
                    .addComponent(buttonReplace)
                    .addComponent(buttonExceptOn)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(spinnerSlope, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, 0)
                        .addComponent(jLabel1))))
            .addComponent(checkBoxAbove)
            .addComponent(checkBoxOutsideSelection)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(checkBoxInSelection)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(checkBoxOutsideSelection)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(checkBoxAbove)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(spinnerAbove, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(checkBoxBelow)
                .addGap(0, 0, 0)
                .addComponent(spinnerBelow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(checkBoxFeather)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(checkBoxReplace)
                .addGap(0, 0, 0)
                .addComponent(buttonReplace)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(checkBoxExceptOn)
                .addGap(0, 0, 0)
                .addComponent(buttonExceptOn)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(checkBoxAboveSlope)
                    .addComponent(checkBoxBelowSlope))
                .addGap(0, 0, 0)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(spinnerSlope, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1)))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void initialiseIfNecessary() {
        if (! initialised) {
            // Prevent the intensity being changed when somebody tries to type a
            // value:
            Action nullAction = new AbstractAction("Do nothing") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // Do nothing
                }

                private static final long serialVersionUID = 1L;
            };
            getActionMap().put("doNothing", nullAction);
            InputMap inputMap = getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
            App app = App.getInstance();
            inputMap.put(app.ACTION_INTENSITY_10_PERCENT.getAcceleratorKey(), "doNothing");
            inputMap.put(app.ACTION_INTENSITY_20_PERCENT.getAcceleratorKey(), "doNothing");
            inputMap.put(app.ACTION_INTENSITY_30_PERCENT.getAcceleratorKey(), "doNothing");
            inputMap.put(app.ACTION_INTENSITY_40_PERCENT.getAcceleratorKey(), "doNothing");
            inputMap.put(app.ACTION_INTENSITY_50_PERCENT.getAcceleratorKey(), "doNothing");
            inputMap.put(app.ACTION_INTENSITY_60_PERCENT.getAcceleratorKey(), "doNothing");
            inputMap.put(app.ACTION_INTENSITY_70_PERCENT.getAcceleratorKey(), "doNothing");
            inputMap.put(app.ACTION_INTENSITY_80_PERCENT.getAcceleratorKey(), "doNothing");
            inputMap.put(app.ACTION_INTENSITY_90_PERCENT.getAcceleratorKey(), "doNothing");
            inputMap.put(app.ACTION_INTENSITY_100_PERCENT.getAcceleratorKey(), "doNothing");

            initialised = true;
        }
    }

    private void checkBoxAboveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxAboveActionPerformed
        initialiseIfNecessary();
        setControlStates();
        filterChanged();
    }//GEN-LAST:event_checkBoxAboveActionPerformed

    private void checkBoxBelowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxBelowActionPerformed
        initialiseIfNecessary();
        setControlStates();
        filterChanged();
    }//GEN-LAST:event_checkBoxBelowActionPerformed

    private void checkBoxReplaceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxReplaceActionPerformed
        initialiseIfNecessary();
        setControlStates();
        if (checkBoxReplace.isSelected() && (onlyOn == null)) {
            showReplaceMenu();
        } else {
            filterChanged();
        }
    }//GEN-LAST:event_checkBoxReplaceActionPerformed

    private void buttonReplaceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonReplaceActionPerformed
        showReplaceMenu();
    }//GEN-LAST:event_buttonReplaceActionPerformed

    private void spinnerAboveStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerAboveStateChanged
        filterChanged();
    }//GEN-LAST:event_spinnerAboveStateChanged

    private void spinnerBelowStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerBelowStateChanged
        filterChanged();
    }//GEN-LAST:event_spinnerBelowStateChanged

    private void checkBoxFeatherActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxFeatherActionPerformed
        filterChanged();
    }//GEN-LAST:event_checkBoxFeatherActionPerformed

    private void checkBoxExceptOnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxExceptOnActionPerformed
        initialiseIfNecessary();
        setControlStates();
        if (checkBoxExceptOn.isSelected() && (exceptOn == null)) {
            showExceptOnMenu();
        } else {
            filterChanged();
        }
    }//GEN-LAST:event_checkBoxExceptOnActionPerformed

    private void buttonExceptOnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonExceptOnActionPerformed
        showExceptOnMenu();
    }//GEN-LAST:event_buttonExceptOnActionPerformed

    private void checkBoxAboveSlopeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxAboveSlopeActionPerformed
        initialiseIfNecessary();
        if (checkBoxAboveSlope.isSelected()) {
            checkBoxBelowSlope.setSelected(false);
        }
        setControlStates();
        filterChanged();
    }//GEN-LAST:event_checkBoxAboveSlopeActionPerformed

    private void checkBoxBelowSlopeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxBelowSlopeActionPerformed
        initialiseIfNecessary();
        if (checkBoxBelowSlope.isSelected()) {
            checkBoxAboveSlope.setSelected(false);
        }
        setControlStates();
        filterChanged();
    }//GEN-LAST:event_checkBoxBelowSlopeActionPerformed

    private void spinnerSlopeStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerSlopeStateChanged
        filterChanged();
    }//GEN-LAST:event_spinnerSlopeStateChanged

    private void checkBoxInSelectionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxInSelectionActionPerformed
        initialiseIfNecessary();
        if (checkBoxInSelection.isSelected() && checkBoxOutsideSelection.isSelected()) {
            checkBoxOutsideSelection.setSelected(false);
        }
        filterChanged();
    }//GEN-LAST:event_checkBoxInSelectionActionPerformed

    private void checkBoxOutsideSelectionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxOutsideSelectionActionPerformed
        initialiseIfNecessary();
        if (checkBoxOutsideSelection.isSelected() && checkBoxInSelection.isSelected()) {
            checkBoxInSelection.setSelected(false);
        }
        filterChanged();
    }//GEN-LAST:event_checkBoxOutsideSelectionActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonExceptOn;
    private javax.swing.JButton buttonReplace;
    private javax.swing.JCheckBox checkBoxAbove;
    private javax.swing.JCheckBox checkBoxAboveSlope;
    private javax.swing.JCheckBox checkBoxBelow;
    private javax.swing.JCheckBox checkBoxBelowSlope;
    private javax.swing.JCheckBox checkBoxExceptOn;
    private javax.swing.JCheckBox checkBoxFeather;
    private javax.swing.JCheckBox checkBoxInSelection;
    private javax.swing.JCheckBox checkBoxOutsideSelection;
    private javax.swing.JCheckBox checkBoxReplace;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JSpinner spinnerAbove;
    private javax.swing.JSpinner spinnerBelow;
    private javax.swing.JSpinner spinnerSlope;
    // End of variables declaration//GEN-END:variables

    private ColourScheme colourScheme;
    private CustomBiomeManager customBiomeManager;
    private Object onlyOn, exceptOn;
    private Listener listener;
    private boolean initialised;
    private ObservableBoolean selectionState;
    private Platform platform;
    private MapSelectionListener mapSelectionListener;

    public static final String MENU_ONLY_ON = "onlyOn";
    public static final String MENU_EXCEPT_ON = "exceptOn";
    
    private static final Icon ICON_WATER = loadScaledIcon("org/pepsoft/worldpainter/icons/flood.png");
    private static final Icon ICON_LAVA = loadScaledIcon("org/pepsoft/worldpainter/icons/flood_with_lava.png");
    private static final Icon ICON_EYEDROPPER = loadScaledIcon("org/pepsoft/worldpainter/icons/eyedropper.png");
    private static final Icon ICON_PLUS = loadScaledIcon("org/pepsoft/worldpainter/icons/plus.png");
    private static final Logger logger = LoggerFactory.getLogger(BrushOptions.class);
    private static final long serialVersionUID = 1L;
    
    public interface Listener {
        void filterChanged(Filter newFilter);
    }
    
    public interface ObjectSelectionListener {
        void objectSelected(Object object, String name, Icon icon);
    }
    
    public interface MapSelectionListener {
        /**
         * The user has requested to select a value from the map.
         * 
         * @param descriptor The descriptor associated with the menu from which the request is being made.
         * @param addAnother Whether the request is being made from the "Add Another" submenu.
         */
        void mapSelectionRequested(String descriptor, boolean addAnother);
    }
}