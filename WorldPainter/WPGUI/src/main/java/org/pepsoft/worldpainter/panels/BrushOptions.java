/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.panels;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner.NumberEditor;
import javax.swing.SpinnerNumberModel;
import org.pepsoft.worldpainter.App;
import org.pepsoft.worldpainter.BiomeScheme;
import org.pepsoft.worldpainter.ColourScheme;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.biomeschemes.AutoBiomeScheme;
import org.pepsoft.worldpainter.biomeschemes.BiomeHelper;
import org.pepsoft.worldpainter.biomeschemes.CustomBiome;
import org.pepsoft.worldpainter.biomeschemes.CustomBiomeManager;
import org.pepsoft.worldpainter.layers.Biome;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.LayerManager;
import org.pepsoft.worldpainter.operations.Filter;
import org.pepsoft.worldpainter.panels.FilterImpl.LevelType;

/**
 *
 * @author pepijn
 */
public class BrushOptions extends javax.swing.JPanel {
    /**
     * Creates new form BrushOptions
     */
    public BrushOptions() {
        initComponents();
        
        // Eliminate thousands separators to make spinners smaller:
        spinnerAbove.setEditor(new NumberEditor(spinnerAbove, "0"));
        spinnerBelow.setEditor(new NumberEditor(spinnerBelow, "0"));
    }

    public Filter getFilter() {
        if (checkBoxAbove.isSelected() || checkBoxBelow.isSelected() || checkBoxReplace.isSelected() || checkBoxExceptOn.isSelected() || checkBoxAboveSlope.isSelected() || checkBoxBelowSlope.isSelected()) {
            return new FilterImpl(App.getInstance().getDimension(),
                    checkBoxAbove.isSelected() ? (Integer) spinnerAbove.getValue() : -1,
                    checkBoxBelow.isSelected() ? (Integer) spinnerBelow.getValue() : -1,
                    checkBoxFeather.isSelected(),
                    checkBoxReplace.isSelected() ? onlyOn : (checkBoxExceptOn.isSelected() ? exceptOn : null),
                    checkBoxExceptOn.isSelected(),
                    (checkBoxAboveSlope.isSelected() || checkBoxBelowSlope.isSelected()) ? (Integer) spinnerSlope.getValue() : -1,
                    checkBoxAboveSlope.isSelected());
        } else {
            return null;
        }
    }
    
    public void setFilter(Filter filter) {
        if (filter == null) {
            checkBoxAbove.setSelected(false);
            checkBoxBelow.setSelected(false);
            checkBoxReplace.setSelected(false);
            checkBoxExceptOn.setSelected(false);
            checkBoxAboveSlope.setSelected(false);
            checkBoxBelowSlope.setSelected(false);
        } else {
            FilterImpl myFilter = (FilterImpl) filter;
            checkBoxAbove.setSelected(myFilter.levelType == LevelType.ABOVE || myFilter.levelType == LevelType.BETWEEN || myFilter.levelType == LevelType.OUTSIDE);
            if (myFilter.aboveLevel >= 0) {
                spinnerAbove.setValue(myFilter.aboveLevel);
            }
            checkBoxBelow.setSelected(myFilter.levelType == LevelType.BELOW || myFilter.levelType == LevelType.BETWEEN || myFilter.levelType == LevelType.OUTSIDE);
            if (myFilter.belowLevel >= 0) {
                spinnerBelow.setValue(myFilter.belowLevel);
            }
            checkBoxAboveSlope.setSelected(myFilter.checkSlope && myFilter.slopeIsAbove);
            checkBoxBelowSlope.setSelected(myFilter.checkSlope && (! myFilter.slopeIsAbove));
            if (myFilter.degrees >= 0) {
                spinnerSlope.setValue(myFilter.degrees);
            }
            checkBoxReplace.setSelected((myFilter.objectType != null) && (! myFilter.replaceIsExcept));
            checkBoxExceptOn.setSelected((myFilter.objectType != null) && myFilter.replaceIsExcept);
            if (myFilter.objectType != null) {
                final App app = App.getInstance();
                switch (myFilter.objectType) {
                    case BIOME:
                        int biome = myFilter.biome;
                        BiomeHelper biomeHelper = new BiomeHelper(autoBiomeScheme, app.getColourScheme(), app.getCustomBiomeManager());
                        if (myFilter.replaceIsExcept) {
                            exceptOn = biome;
                            buttonExceptOn.setText(biomeHelper.getBiomeName(biome));
                            buttonExceptOn.setIcon(biomeHelper.getBiomeIcon(biome));
                        } else {
                            onlyOn = biome;
                            buttonReplace.setText(biomeHelper.getBiomeName(biome));
                            buttonReplace.setIcon(biomeHelper.getBiomeIcon(biome));
                        }
                        break;
                    case BIT_LAYER:
                    case INT_LAYER:
                        Layer layer = myFilter.layer;
                        if (myFilter.replaceIsExcept) {
                            exceptOn = layer;
                            buttonExceptOn.setText(layer.getName());
                            buttonExceptOn.setIcon(new ImageIcon(layer.getIcon()));
                        } else {
                            onlyOn = layer;
                            buttonReplace.setText(layer.getName());
                            buttonReplace.setIcon(new ImageIcon(layer.getIcon()));
                        }
                        break;
                    case TERRAIN:
                        Terrain terrain = myFilter.terrain;
                        ColourScheme colourScheme = app.getColourScheme();
                        if (myFilter.replaceIsExcept) {
                            exceptOn = terrain;
                            buttonExceptOn.setText(terrain.getName());
                            buttonExceptOn.setIcon(new ImageIcon(terrain.getIcon(colourScheme)));
                        } else {
                            onlyOn = terrain;
                            buttonReplace.setText(terrain.getName());
                            buttonReplace.setIcon(new ImageIcon(terrain.getIcon(colourScheme)));
                        }
                        break;
                    case WATER:
                        if (myFilter.replaceIsExcept) {
                            exceptOn = WATER;
                            buttonExceptOn.setText("Water");
                            buttonExceptOn.setIcon(null);
                        } else {
                            onlyOn = WATER;
                            buttonReplace.setText("Water");
                            buttonReplace.setIcon(null);
                        }
                        break;
                    case LAND:
                        if (myFilter.replaceIsExcept) {
                            exceptOn = LAND;
                            buttonExceptOn.setText("Land");
                            buttonExceptOn.setIcon(null);
                        } else {
                            onlyOn = LAND;
                            buttonReplace.setText("Land");
                            buttonReplace.setIcon(null);
                        }
                        break;
                }
            }
        }
        setControlStates();
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

//    * also add to global operations tool
    
    private void setControlStates() {
        spinnerAbove.setEnabled(checkBoxAbove.isSelected());
        spinnerBelow.setEnabled(checkBoxBelow.isSelected());
        spinnerSlope.setEnabled(checkBoxAboveSlope.isSelected() || checkBoxBelowSlope.isSelected());
        buttonReplace.setEnabled(checkBoxReplace.isSelected());
        buttonExceptOn.setEnabled(checkBoxExceptOn.isSelected());
        checkBoxFeather.setEnabled(checkBoxAbove.isSelected() || checkBoxBelow.isSelected());
    }
    
    private JPopupMenu createReplaceMenu() {
        return createObjectSelectionMenu(new ObjectSelectionListener() {
            @Override
            public void objectSelected(Object object, String name, Icon icon) {
                onlyOn = object;
                buttonReplace.setText(name);
                buttonReplace.setIcon(icon);
                filterChanged();
            }
        });
    }
    
    private JPopupMenu createExceptOnMenu() {
        return createObjectSelectionMenu(new ObjectSelectionListener() {
            @Override
            public void objectSelected(Object object, String name, Icon icon) {
                exceptOn = object;
                buttonExceptOn.setText(name);
                buttonExceptOn.setIcon(icon);
                filterChanged();
            }
        });
    }
    
    private JPopupMenu createObjectSelectionMenu(final ObjectSelectionListener listener) {
        JMenuItem waterItem = new JMenuItem("Water");
        waterItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                listener.objectSelected(WATER, "Water", null);
            }
        });
        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.add(waterItem);

        JMenuItem landItem = new JMenuItem("Land");
        landItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                listener.objectSelected(LAND, "Land", null);
            }
        });
        popupMenu.add(landItem);
        
        JMenu terrainMenu = new JMenu("Terrain");
        JMenu customTerrainMenu = new JMenu("Custom");
        JMenu stainedClayTerrainMenu = new JMenu("Stained Clay");
        App app = App.getInstance();
        ColourScheme colourScheme = app.getColourScheme();
        for (Terrain terrain: Terrain.getConfiguredValues()) {
            final Terrain selectedTerrain = terrain;
            final String name = terrain.getName();
            final Icon icon = new ImageIcon(terrain.getIcon(colourScheme));
            JMenuItem menuItem = new JMenuItem(name, icon);
            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    listener.objectSelected(selectedTerrain, name, icon);
                }
            });
            if (terrain.isCustom()) {
                customTerrainMenu.add(menuItem);
            } else if (terrain.getName().endsWith(" Clay") && (terrain != Terrain.HARDENED_CLAY)) {
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
        
        JMenu layerMenu = new JMenu("Layer");
        for (Layer layer: LayerManager.getInstance().getLayers()) {
            if (layer.equals(Biome.INSTANCE)) {
                continue;
            }
            final Layer selectedLayer = layer;
            final String name = layer.getName();
            final Icon icon = new ImageIcon(layer.getIcon());
            JMenuItem menuItem = new JMenuItem(name, icon);
            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    listener.objectSelected(selectedLayer, name, icon);
                }
            });
            layerMenu.add(menuItem);
        }
        for (Layer layer: app.getCustomLayers()) {
            final Layer selectedLayer = layer;
            final String name = layer.getName();
            final Icon icon = new ImageIcon(layer.getIcon());
            JMenuItem menuItem = new JMenuItem(name, icon);
            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    listener.objectSelected(selectedLayer, name, icon);
                }
            });
            layerMenu.add(menuItem);
        }
        popupMenu.add(layerMenu);
        
        final JMenu biomeMenu = new JMenu("Biome");
        final CustomBiomeManager customBiomeManager = app.getCustomBiomeManager();
        final BiomeHelper biomeHelper = new BiomeHelper(autoBiomeScheme, colourScheme, customBiomeManager);
        List<CustomBiome> customBiomes = customBiomeManager.getCustomBiomes();
        if ((customBiomes != null) && (! customBiomes.isEmpty())) {
            JMenu customBiomeMenu = new JMenu("Custom");
            for (CustomBiome customBiome: customBiomes) {
                final int selectedBiome = customBiome.getId();
                final String name = biomeHelper.getBiomeName(selectedBiome);
                final Icon icon = biomeHelper.getBiomeIcon(selectedBiome);
                final JMenuItem menuItem = new JMenuItem(name, icon);
                menuItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        listener.objectSelected(selectedBiome, name, icon);
                    }
                });
                customBiomeMenu.add(menuItem);
            }
            biomeMenu.add(customBiomeMenu);
        }
        for (int i = 0; i < autoBiomeScheme.getBiomeCount(); i++) {
            if (autoBiomeScheme.isBiomePresent(i) && (! (autoBiomeScheme.getBiomeName(i).endsWith("+") || autoBiomeScheme.getBiomeName(i).endsWith(" F") || autoBiomeScheme.getBiomeName(i).endsWith(" M") || autoBiomeScheme.getBiomeName(i).endsWith(" F M") || autoBiomeScheme.getBiomeName(i).endsWith(" Edge") || autoBiomeScheme.getBiomeName(i).endsWith(" Shore")))) {
                final int selectedBiome = i;
                final String name = biomeHelper.getBiomeName(i);
                final Icon icon = biomeHelper.getBiomeIcon(i);
                final JMenuItem menuItem = new JMenuItem(name, icon);
                menuItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        listener.objectSelected(selectedBiome, name, icon);
                    }
                });
                biomeMenu.add(menuItem);
            }
        }
        JMenu autoBiomeSubMenu = new JMenu("Auto Biomes");
        JMenuItem autoBiomesMenuItem = new JMenuItem("All Auto Biomes");
        autoBiomesMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                listener.objectSelected(AUTO_BIOMES, "All Auto Biomes", null);
            }
        });
        autoBiomeSubMenu.add(autoBiomesMenuItem);
        for (int autoBiome: Dimension.POSSIBLE_AUTO_BIOMES) {
            final int selectedBiome = -autoBiome;
            final String name = biomeHelper.getBiomeName(autoBiome);
            final Icon icon = biomeHelper.getBiomeIcon(autoBiome);
            final JMenuItem menuItem = new JMenuItem(name, icon);
            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    listener.objectSelected(selectedBiome, name, icon);
                }
            });
            autoBiomeSubMenu.add(menuItem);
        }
        biomeMenu.add(autoBiomeSubMenu);
        JMenu biomeSubMenu = new JMenu("Variants");
        for (int i = 0; i < autoBiomeScheme.getBiomeCount(); i++) {
            if (autoBiomeScheme.isBiomePresent(i) && (autoBiomeScheme.getBiomeName(i).endsWith("+") || autoBiomeScheme.getBiomeName(i).endsWith(" F") || autoBiomeScheme.getBiomeName(i).endsWith(" M") || autoBiomeScheme.getBiomeName(i).endsWith(" F M") || autoBiomeScheme.getBiomeName(i).endsWith(" Edge") || autoBiomeScheme.getBiomeName(i).endsWith(" Shore"))) {
                final int selectedBiome = i;
                final String name = biomeHelper.getBiomeName(i);
                final Icon icon = biomeHelper.getBiomeIcon(i);
                final JMenuItem menuItem = new JMenuItem(name, icon);
                menuItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        listener.objectSelected(selectedBiome, name, icon);
                    }
                });
                biomeSubMenu.add(menuItem);
            }
        }
        biomeMenu.add(biomeSubMenu);
        popupMenu.add(biomeMenu);
        return popupMenu;
    }
    
    private void showReplaceMenu() {
        JPopupMenu menu = createReplaceMenu();
        menu.show(this, buttonReplace.getX() + buttonReplace.getWidth(), buttonReplace.getY());
    }
    
    private void showExceptOnMenu() {
        JPopupMenu menu = createExceptOnMenu();
        menu.show(this, buttonExceptOn.getX() + buttonExceptOn.getWidth(), buttonExceptOn.getY());
    }
    
    private void filterChanged() {
        if (listener != null) {
            listener.filterChanged(getFilter());
        }
    }
    
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

            autoBiomeScheme = new AutoBiomeScheme(null);
            
            initialised = true;
        }
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
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

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(checkBoxAbove)
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
                                .addComponent(jLabel1)))))
                .addGap(0, 0, 0))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(checkBoxAbove)
                .addGap(0, 0, 0)
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
        if (checkBoxReplace.isSelected()) {
            checkBoxExceptOn.setSelected(false);
        }
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
        if (checkBoxExceptOn.isSelected()) {
            checkBoxReplace.setSelected(false);
        }
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

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonExceptOn;
    private javax.swing.JButton buttonReplace;
    private javax.swing.JCheckBox checkBoxAbove;
    private javax.swing.JCheckBox checkBoxAboveSlope;
    private javax.swing.JCheckBox checkBoxBelow;
    private javax.swing.JCheckBox checkBoxBelowSlope;
    private javax.swing.JCheckBox checkBoxExceptOn;
    private javax.swing.JCheckBox checkBoxFeather;
    private javax.swing.JCheckBox checkBoxReplace;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JSpinner spinnerAbove;
    private javax.swing.JSpinner spinnerBelow;
    private javax.swing.JSpinner spinnerSlope;
    // End of variables declaration//GEN-END:variables

    private Object onlyOn, exceptOn;
    private BiomeScheme autoBiomeScheme;
    private Listener listener;
    private boolean initialised;
    
    public static final String LAND = "Land";
    public static final String WATER = "Water";
    public static final String AUTO_BIOMES = "Automatic Biomes";
    
    private static final long serialVersionUID = 1L;
    
    public static interface Listener {
        void filterChanged(Filter newFilter);
    }
    
    public static interface ObjectSelectionListener {
        void objectSelected(Object object, String name, Icon icon);
    }
}