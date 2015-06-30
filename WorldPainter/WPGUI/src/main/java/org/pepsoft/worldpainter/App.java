/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter;

import com.jidesoft.docking.*;
import com.jidesoft.swing.JideLabel;
import org.jetbrains.annotations.NonNls;
import org.pepsoft.minecraft.Direction;
import org.pepsoft.minecraft.Material;
import org.pepsoft.util.*;
import org.pepsoft.util.ProgressReceiver.OperationCancelled;
import org.pepsoft.util.swing.ProgressDialog;
import org.pepsoft.util.swing.ProgressTask;
import org.pepsoft.util.swing.TiledImageViewerContainer;
import org.pepsoft.util.undo.UndoManager;
import org.pepsoft.worldpainter.biomeschemes.AutoBiomeScheme;
import org.pepsoft.worldpainter.biomeschemes.BiomeSchemeManager;
import org.pepsoft.worldpainter.biomeschemes.CustomBiome;
import org.pepsoft.worldpainter.biomeschemes.CustomBiomeManager;
import org.pepsoft.worldpainter.biomeschemes.CustomBiomeManager.CustomBiomeListener;
import org.pepsoft.worldpainter.brushes.BitmapBrush;
import org.pepsoft.worldpainter.brushes.Brush;
import org.pepsoft.worldpainter.brushes.RotatedBrush;
import org.pepsoft.worldpainter.brushes.SymmetricBrush;
import org.pepsoft.worldpainter.colourschemes.DynMapColourScheme;
import org.pepsoft.worldpainter.gardenofeden.GardenOfEdenOperation;
import org.pepsoft.worldpainter.importing.MapImportDialog;
import org.pepsoft.worldpainter.layers.*;
import org.pepsoft.worldpainter.layers.exporters.ExporterSettings;
import org.pepsoft.worldpainter.layers.exporters.ResourcesExporter.ResourcesExporterSettings;
import org.pepsoft.worldpainter.layers.groundcover.GroundCoverLayer;
import org.pepsoft.worldpainter.layers.plants.PlantLayer;
import org.pepsoft.worldpainter.layers.pockets.UndergroundPocketsDialog;
import org.pepsoft.worldpainter.layers.pockets.UndergroundPocketsLayer;
import org.pepsoft.worldpainter.layers.renderers.VoidRenderer;
import org.pepsoft.worldpainter.layers.tunnel.TunnelLayer;
import org.pepsoft.worldpainter.layers.tunnel.TunnelLayerDialog;
import org.pepsoft.worldpainter.objects.AbstractObject;
import org.pepsoft.worldpainter.operations.*;
import org.pepsoft.worldpainter.painting.DiscreteLayerPaint;
import org.pepsoft.worldpainter.painting.LayerPaint;
import org.pepsoft.worldpainter.painting.Paint;
import org.pepsoft.worldpainter.painting.PaintFactory;
import org.pepsoft.worldpainter.panels.BrushOptions;
import org.pepsoft.worldpainter.panels.BrushOptions.Listener;
import org.pepsoft.worldpainter.threedeeview.ThreeDeeFrame;
import org.pepsoft.worldpainter.tools.BiomesViewerFrame;
import org.pepsoft.worldpainter.tools.RespawnPlayerDialog;
import org.pepsoft.worldpainter.util.BackupUtil;
import org.pepsoft.worldpainter.util.BetterAction;
import org.pepsoft.worldpainter.util.LayoutUtils;
import org.pepsoft.worldpainter.vo.AttributeKeyVO;
import org.pepsoft.worldpainter.vo.EventVO;
import org.pepsoft.worldpainter.vo.UsageVO;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.Box;
import javax.swing.Timer;
import javax.swing.border.BevelBorder;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.WritableRaster;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipException;

import static com.jidesoft.docking.DockContext.DOCK_SIDE_EAST;
import static com.jidesoft.docking.DockContext.DOCK_SIDE_WEST;
import static com.jidesoft.docking.DockableFrame.*;
import static java.awt.event.KeyEvent.*;
import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.worldpainter.Constants.*;

//import javax.swing.JSeparator;

/**
 *
 * @author pepijn
 */
public final class App extends JFrame implements RadiusControl,
        BiomesViewerFrame.SeedListener, Listener, CustomBiomeListener,
        PaletteManager.ButtonProvider, DockableHolder {
    private App() {
        super((mode == Mode.WORLDPAINTER) ? "WorldPainter" : "MinecraftMapEditor"); // NOI18N
        setIconImage(ICON);

        colourSchemes = new ColourScheme[] {
            new DynMapColourScheme("default", true),
            new DynMapColourScheme("flames", true),
            new DynMapColourScheme("ovocean", true),
            new DynMapColourScheme("sk89q", true),
            new DynMapColourScheme("dokudark", true),
            new DynMapColourScheme("dokuhigh", true),
            new DynMapColourScheme("dokulight", true),
            new DynMapColourScheme("misa", true),
            new DynMapColourScheme("sphax", true)
        };
        defaultColourScheme = colourSchemes[0];
        Configuration config = Configuration.getInstance();
        selectedColourScheme = colourSchemes[config.getColourschemeIndex()];
        operations = OperationManager.getInstance().getOperations();
        setMaxRadius(config.getMaximumBrushSize());

        loadCustomBrushes();
        
        brushOptions = new BrushOptions();
        brushOptions.setListener(this);

        if (SystemUtils.isMac()) {
            installMacCustomisations();
        }

        initComponents();
        
        hiddenLayers.add(Biome.INSTANCE);
        view.addHiddenLayer(Biome.INSTANCE);
        
        // Initialize various things
        customBiomeManager.addListener(this);
        
        int biomeCount = autoBiomeScheme.getBiomeCount();
        for (int i = 0; i < biomeCount; i++) {
            if (autoBiomeScheme.isBiomePresent(i)) {
                biomeNames[i] = autoBiomeScheme.getBiomeName(i) + " (ID " + i + ")";
            }
        }
        
        String sizeStr = System.getProperty("org.pepsoft.worldpainter.size");
        if (sizeStr != null) {
            String[] dims = sizeStr.split("x");
            int width = Integer.parseInt(dims[0]);
            int height = Integer.parseInt(dims[1]);
            setSize(width, height);
            setLocationRelativeTo(null);
        } else if (config.getWindowBounds() != null) {
            setBounds(config.getWindowBounds());
        } else {
            setSize(1024, 896);
            setLocationRelativeTo(null);
        }
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        // For some look and feels the preferred size of labels isn't set until
        // they are displayed
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                fixLabelSizes();
                maybePing();

                // Show mini map here because we only know our location now
//                JDialog miniMapDialog = new JDialog(App.this, "Mini Map");
//                final MiniMap miniMap = new MiniMap();
//                miniMap.setView(view);
//                miniMapDialog.getContentPane().add(miniMap, BorderLayout.CENTER);
//                miniMapDialog.setSize(300, 300);
//                miniMapDialog.setLocation(getX() + getWidth() - 300, getY());
//                miniMapDialog.setAlwaysOnTop(true);
//                miniMapDialog.addWindowListener(new WindowAdapter() {
//                    @Override
//                    public void windowClosing(WindowEvent e) {
//                        miniMap.setView(null);
//                    }
//                });
//                miniMapDialog.setVisible(true);
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                if (getExtendedState() != Frame.MAXIMIZED_BOTH) {
                    Configuration config = Configuration.getInstance();
                    if (config != null) {
                        config.setWindowBounds(getBounds());
                    }
                }
            }

            @Override
            public void componentResized(ComponentEvent e) {
                if (getExtendedState() != Frame.MAXIMIZED_BOTH) {
                    Configuration config = Configuration.getInstance();
                    if (config != null) {
                        config.setWindowBounds(getBounds());
                    }
                }
            }
        });
        WindowAdapter windowAdapter = new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                Configuration config = Configuration.getInstance();
                if (config != null) {
                    config.setMaximised(getExtendedState() == Frame.MAXIMIZED_BOTH);
                }
                exit();
            }
        };
        addWindowListener(windowAdapter);

        ActionMap actionMap = rootPane.getActionMap();
        actionMap.put("rotateLightLeft", ACTION_ROTATE_LIGHT_LEFT);
        actionMap.put("rotateLightRight", ACTION_ROTATE_LIGHT_RIGHT);
        actionMap.put("intensity10", ACTION_INTENSITY_10_PERCENT);
        actionMap.put("intensity20", ACTION_INTENSITY_20_PERCENT);
        actionMap.put("intensity30", ACTION_INTENSITY_30_PERCENT);
        actionMap.put("intensity40", ACTION_INTENSITY_40_PERCENT);
        actionMap.put("intensity50", ACTION_INTENSITY_50_PERCENT);
        actionMap.put("intensity60", ACTION_INTENSITY_60_PERCENT);
        actionMap.put("intensity70", ACTION_INTENSITY_70_PERCENT);
        actionMap.put("intensity80", ACTION_INTENSITY_80_PERCENT);
        actionMap.put("intensity90", ACTION_INTENSITY_90_PERCENT);
        actionMap.put("intensity100", ACTION_INTENSITY_100_PERCENT);

        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        inputMap.put(ACTION_ROTATE_LIGHT_LEFT.getAcceleratorKey(), "rotateLightLeft");
        inputMap.put(ACTION_ROTATE_LIGHT_RIGHT.getAcceleratorKey(), "rotateLightRight");
        inputMap.put(ACTION_INTENSITY_10_PERCENT.getAcceleratorKey(), "intensity10");
        inputMap.put(ACTION_INTENSITY_20_PERCENT.getAcceleratorKey(), "intensity20");
        inputMap.put(ACTION_INTENSITY_30_PERCENT.getAcceleratorKey(), "intensity30");
        inputMap.put(ACTION_INTENSITY_40_PERCENT.getAcceleratorKey(), "intensity40");
        inputMap.put(ACTION_INTENSITY_50_PERCENT.getAcceleratorKey(), "intensity50");
        inputMap.put(ACTION_INTENSITY_60_PERCENT.getAcceleratorKey(), "intensity60");
        inputMap.put(ACTION_INTENSITY_70_PERCENT.getAcceleratorKey(), "intensity70");
        inputMap.put(ACTION_INTENSITY_80_PERCENT.getAcceleratorKey(), "intensity80");
        inputMap.put(ACTION_INTENSITY_90_PERCENT.getAcceleratorKey(), "intensity90");
        inputMap.put(ACTION_INTENSITY_100_PERCENT.getAcceleratorKey(), "intensity100");
        inputMap.put(KeyStroke.getKeyStroke(VK_NUMPAD1, 0), "intensity10");
        inputMap.put(KeyStroke.getKeyStroke(VK_NUMPAD2, 0), "intensity20");
        inputMap.put(KeyStroke.getKeyStroke(VK_NUMPAD3, 0), "intensity30");
        inputMap.put(KeyStroke.getKeyStroke(VK_NUMPAD4, 0), "intensity40");
        inputMap.put(KeyStroke.getKeyStroke(VK_NUMPAD5, 0), "intensity50");
        inputMap.put(KeyStroke.getKeyStroke(VK_NUMPAD6, 0), "intensity60");
        inputMap.put(KeyStroke.getKeyStroke(VK_NUMPAD7, 0), "intensity70");
        inputMap.put(KeyStroke.getKeyStroke(VK_NUMPAD8, 0), "intensity80");
        inputMap.put(KeyStroke.getKeyStroke(VK_NUMPAD9, 0), "intensity90");
        inputMap.put(KeyStroke.getKeyStroke(VK_NUMPAD0, 0), "intensity100");

        // Log some information about the graphics environment
        GraphicsDevice graphicsDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        DisplayMode displayMode = graphicsDevice.getDisplayMode();
        ImageCapabilities imageCapabilities = graphicsDevice.getDefaultConfiguration().getImageCapabilities();
        logger.info("Default graphics device, ID string: " + graphicsDevice.getIDstring() + ", available accelerated memory: " + graphicsDevice.getAvailableAcceleratedMemory() + ", display mode: " + displayMode.getWidth() + "x" + displayMode.getHeight() + ", bit depth: " + ((displayMode.getBitDepth() == DisplayMode.BIT_DEPTH_MULTI) ? "multi" : displayMode.getBitDepth()) + ", refresh rate: " + ((displayMode.getRefreshRate() == DisplayMode.REFRESH_RATE_UNKNOWN) ? "unknown" : displayMode.getRefreshRate()) + ", reported dpi: " + Toolkit.getDefaultToolkit().getScreenResolution() + ", accelerated: " + (imageCapabilities.isAccelerated() ? "yes" : "no") + ", true volatile: " + (imageCapabilities.isTrueVolatile() ? "yes" : "no"));
    }

    public World2 getWorld() {
        return world;
    }

    /**
     * This setter <em>must</em> be called in two steps when loading a new
     * world: first setting the value to <code>null</code>, and then to the new
     * World, except when re-loading the same World. Otherwise it will throw
     * an {@link IllegalStateException}.
     *
     * <p>This is for historical reasons: it was introduced to make sure that
     * the custom materials, which are registered when loaded, are not then
     * immediately cleared when invoking this method.
     *
     * @param world The world to set, or <code>null</code>.
     */
    public void setWorld(World2 world) {
        if ((this.world != null) && ((world != null) && (this.world != world))) {
            throw new IllegalStateException(world + " != " + this.world);
        }
        if (world != null) {
            if (world == this.world) {
                // Reloading the same world; no need to do anything other than
                // to reload the dimension
                if (dimension != null) {
                    setDimension(world.getDimension(dimension.getDim()));
                }
            } else {
                this.world = world;

                loadCustomTerrains();

                extendedBlockIdsMenuItem.setSelected(world.isExtendedBlockIds());

                // Load the layout *before* setting the dimension, because otherwise
                // the subsequent changes (due to the loading of custom layers for
                // instance) may cause layout/display bugs
                Configuration config = Configuration.getInstance();
                if ((config.getJideLayoutData() != null) && config.getJideLayoutData().containsKey(world.getName())) {
                    dockingManager.loadLayoutFrom(new ByteArrayInputStream(config.getJideLayoutData().get(world.getName())));
                }

                setDimension(world.getDimension(DIM_NORMAL));

                if (config.isDefaultViewDistanceEnabled() != view.isDrawViewDistance()) {
                    ACTION_VIEW_DISTANCE.actionPerformed(null);
                }
                if (config.isDefaultWalkingDistanceEnabled() != view.isDrawWalkingDistance()) {
                    ACTION_WALKING_DISTANCE.actionPerformed(null);
                }
                view.setLightOrigin(config.getDefaultLightOrigin());

                brushOptions.setMaxHeight(world.getMaxHeight());

                if (config.isEasyMode()) {
                    boolean imported = world.getImportedFrom() != null;
                    ACTION_EXPORT_WORLD.setEnabled(!imported);
                    ACTION_MERGE_WORLD.setEnabled(imported);
                } else {
                    ACTION_EXPORT_WORLD.setEnabled(true);
                    ACTION_MERGE_WORLD.setEnabled(true);
                }
            }
        } else if (this.world != null) {
            for (Dimension dimension: this.world.getDimensions()) {
                dimension.unregister();
            }
            this.world = null;

            setDimension(null);

            undoManagers.clear();

            ACTION_EXPORT_WORLD.setEnabled(false);
            ACTION_MERGE_WORLD.setEnabled(false);

            // Unload all custom terrain types
            clearCustomTerrains();

            // Unload all custom materials
            MixedMaterialManager.getInstance().clear();
        }
    }

    public Dimension getDimension() {
        return dimension;
    }

    public void setDimension(final Dimension dimension) {
        Configuration config = Configuration.getInstance();
        if (this.dimension != null) {
            Point viewPosition = view.getViewCentreInWorldCoords();
            if (viewPosition != null) {
                this.dimension.setLastViewPosition(viewPosition);
                // Keep the view position of the opposite dimension, if any,
                // in sync
                if (world != null) {
                    Dimension oppositeDimension = null;
                    switch (this.dimension.getDim()) {
                        case DIM_NORMAL:
                            oppositeDimension = world.getDimension(DIM_NORMAL_CEILING);
                            break;
                        case DIM_NORMAL_CEILING:
                            oppositeDimension = world.getDimension(DIM_NORMAL);
                            break;
                        case DIM_NETHER:
                            oppositeDimension = world.getDimension(DIM_NETHER_CEILING);
                            break;
                        case DIM_NETHER_CEILING:
                            oppositeDimension = world.getDimension(DIM_NETHER);
                            break;
                        case DIM_END:
                            oppositeDimension = world.getDimension(DIM_END_CEILING);
                            break;
                        case DIM_END_CEILING:
                            oppositeDimension = world.getDimension(DIM_END);
                            break;
                    }
                    if (oppositeDimension != null) {
                        oppositeDimension.setLastViewPosition(viewPosition);
                    }
                }
            }

            this.dimension.unregister();
            currentUndoManager.unregisterActions();
            currentUndoManager = null;

            // Remove the existing custom object layers and save the list of
            // custom layers to the dimension to preserve layers which aren't
            // currently in use
            if (! paletteManager.isEmpty()) {
                List<CustomLayer> customLayers = new ArrayList<>();
                boolean visibleLayersChanged = false;
                for (Palette palette: paletteManager.clear()) {
                    List<CustomLayer> paletteLayers = palette.getLayers();
                    customLayers.addAll(paletteLayers);
                    for (Layer layer: paletteLayers) {
                        if (hiddenLayers.contains(layer)) {
                            hiddenLayers.remove(layer);
                            visibleLayersChanged = true;
                        }
                        if (layer.equals(soloLayer)) {
                            soloLayer = null;
                            visibleLayersChanged = true;
                        }
                    }
                    dockingManager.removeFrame(palette.getDockableFrame().getKey());
                }
                if (visibleLayersChanged) {
                    updateLayerVisibility();
                }
                layerSoloCheckBoxes.clear();
                this.dimension.setCustomLayers(customLayers);
            } else {
                this.dimension.setCustomLayers(Collections.EMPTY_LIST);
            }
            layersWithNoButton.clear();

            saveCustomBiomes();
        }
        this.dimension = dimension;
        if (dimension != null) {
            setTitle("WorldPainter - " + world.getName() + " - " + dimension.getName()); // NOI18N
            viewSurfaceMenuItem.setSelected(dimension.getDim() == DIM_NORMAL);
            viewSurfaceCeilingMenuItem.setSelected(dimension.getDim() == DIM_NORMAL_CEILING);
            viewNetherMenuItem.setSelected(dimension.getDim() == DIM_NETHER);
            viewNetherCeilingMenuItem.setSelected(dimension.getDim() == DIM_NETHER_CEILING);
            viewEndMenuItem.setSelected(dimension.getDim() == DIM_END);
            viewEndCeilingMenuItem.setSelected(dimension.getDim() == DIM_END_CEILING);

            // Legacy: if this is an older world with an overlay enabled, ask
            // the user if we should fix the coordinates (ask because they might
            // have fixed the problem manually in 1.9.0 or 1.9.1, in which we
            // neglected to do it automatically)
            if (dimension.isFixOverlayCoords()) {
                Toolkit.getDefaultToolkit().beep();
                if (JOptionPane.showConfirmDialog(this,
                        "This world was created in an older version of WorldPainter\n" +
                        "in which the overlay offsets were not stored correctly.\n" +
                        "Do you want WorldPainter to fix the offsets now?\n" +
                        "\n" +
                        "If you already manually fixed the offsets using version 1.9.0\n"
                        + "or 1.9.1 of WorldPainter, say no. If you're unsure, say yes.", "Fix Overlay Offset?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.NO_OPTION) {
                    dimension.setOverlayOffsetX(dimension.getOverlayOffsetX() + dimension.getLowestX() << TILE_SIZE_BITS);
                    dimension.setOverlayOffsetY(dimension.getOverlayOffsetY() + dimension.getLowestY() << TILE_SIZE_BITS);
                }
                dimension.setFixOverlayCoords(false);
            }
            
            view.setDimension(dimension);
            view.moveTo(dimension.getLastViewPosition());
            
            setDimensionControlStates();
            if ((! "true".equals(System.getProperty("org.pepsoft.worldpainter.disableUndo"))) && config.isUndoEnabled()) {
                currentUndoManager = undoManagers.get(dimension.getDim());
                if (currentUndoManager == null) {
                    currentUndoManager = new UndoManager(ACTION_UNDO, ACTION_REDO, Math.max(config.getUndoLevels() + 1, 2));
                    undoManagers.put(dimension.getDim(), currentUndoManager);
                    currentUndoManager.setStopAtClasses(PropertyChangeListener.class, Tile.Listener.class, Biome.class, BetterAction.class);
                    dimension.register(currentUndoManager);
                } else {
                    currentUndoManager.registerActions(ACTION_UNDO, ACTION_REDO);
                }
                dimension.armSavePoint();
            } else {
                // Still install an undo manager, because some operations depend
                // on one level of undo being available
                currentUndoManager = new UndoManager(2);
                currentUndoManager.setStopAtClasses(PropertyChangeListener.class, Tile.Listener.class, Biome.class, BetterAction.class);
                dimension.register(currentUndoManager);
                ACTION_UNDO.setEnabled(false);
                ACTION_REDO.setEnabled(false);
            }
            if (threeDeeFrame != null) {
                threeDeeFrame.setDimension(dimension);
            }
                
            // Add the custom object layers from the world
            boolean missingTerrainWarningGiven = false;
            for (CustomLayer customLayer: dimension.getCustomLayers()) {
                if (customLayer.isHide()) {
                    layersWithNoButton.add(customLayer);
                } else {
                    registerCustomLayer(customLayer, false);
                }
                if (customLayer instanceof CombinedLayer) {
                    if (((CombinedLayer) customLayer).isMissingTerrainWarning()) {
                        if (! missingTerrainWarningGiven) {
                            JOptionPane.showMessageDialog(this, "The world contains one or more Combined Layer(s) referring to a Custom Terrain\nwhich is not present in this world. The terrain has been reset.", "Missing Custom Terrain", JOptionPane.WARNING_MESSAGE);
                            missingTerrainWarningGiven = true;
                        }
                        ((CombinedLayer) customLayer).resetMissingTerrainWarning();
                    }
                }
            }
            
            // Set action states
            ACTION_GRID.setSelected(view.isPaintGrid());
            ACTION_CONTOURS.setSelected(view.isDrawContours());
            ACTION_OVERLAY.setSelected(view.isDrawOverlay());
            
            // Load custom biomes. But first remove any that are now regular
            // biomes
            List<CustomBiome> customBiomes = dimension.getCustomBiomes();
            if (customBiomes != null) {
                for (Iterator<CustomBiome> i = customBiomes.iterator(); i.hasNext(); ) {
                    CustomBiome customBiome = i.next();
                    if (autoBiomeScheme.isBiomePresent(customBiome.getId())) {
                        i.remove();
                    }
                }
                if (customBiomes.isEmpty()) {
                    customBiomes = null;
                }
            }
            customBiomeManager.setCustomBiomes(customBiomes);
        } else {
            view.setDimension(null);
            setTitle("WorldPainter"); // NOI18N

            // Clear action states
            ACTION_GRID.setSelected(false);
            ACTION_CONTOURS.setSelected(false);
            ACTION_OVERLAY.setSelected(false);
            
            // Close the 3D view
            if (threeDeeFrame != null) {
                threeDeeFrame.dispose();
                threeDeeFrame = null;
            }

            // Clear status bar
            locationLabel.setText(strings.getString("location-"));
            heightLabel.setText(" ");
            waterLabel.setText(" ");
            biomeLabel.setText(" ");
            materialLabel.setText(" ");
            
            // Deselect any current operation
            if (activeOperation != null) {
                deselectTool();
            }
            
            customBiomeManager.setCustomBiomes(null);
        }
    }

    public void updateStatusBar(int x, int y) {
        if (dimension == null) {
            return;
        }
        locationLabel.setText(MessageFormat.format(strings.getString("location.0.1"), x, y));
        int height = dimension.getIntHeightAt(x, y);
        if (height == -1) {
            // Not on a tile
            heightLabel.setText(" ");
            waterLabel.setText(" ");
            if (dimension.isBorderTile(x >> TILE_SIZE_BITS, y >> TILE_SIZE_BITS)) {
                materialLabel.setText("Border");
            } else {
                materialLabel.setText("Minecraft Generated");
            }
            biomeLabel.setText(" ");
            return;
        }
        if (devMode) {
            heightLabel.setText(MessageFormat.format("Height: {0} ({1}) of {2}", dimension.getHeightAt(x, y), height, dimension.getMaxHeight() - 1));
        } else {
            heightLabel.setText(MessageFormat.format(strings.getString("height.0.of.1"), height, dimension.getMaxHeight() - 1));
        }
        if ((activeOperation instanceof PaintOperation) && (paint instanceof LayerPaint)) {
            Layer layer = ((LayerPaint) paint).getLayer();
            switch (layer.getDataSize()) {
                case BIT:
                case BIT_PER_CHUNK:
                    waterLabel.setText(MessageFormat.format(strings.getString("layer.0.on.off"), layer.getName(), (dimension.getBitLayerValueAt(layer, x, y) ? 1 : 0)));
                    break;
                case NIBBLE:
                    int value = dimension.getLayerValueAt(layer, x, y);
                    int strength = (value > 0) ? ((value - 1) * 100  / 14 + 1): 0;
                    if ((strength == 51) || (strength == 101)) {
                        strength--;
                    }
                    waterLabel.setText(MessageFormat.format(strings.getString("layer.0.level.1"), layer.getName(), strength));
                    break;
                case BYTE:
                    value = dimension.getLayerValueAt(layer, x, y);
                    strength = (value > 0) ? ((value - 1) * 100  / 254 + 1): 0;
                    waterLabel.setText(MessageFormat.format(strings.getString("layer.0.level.1"), layer.getName(), strength));
                    break;
                default:
                    // Do nothing
                    break;
            }
        } else if (activeOperation instanceof GardenOfEdenOperation) {
            switch(dimension.getLayerValueAt(GardenCategory.INSTANCE, x, y)) {
                case GardenCategory.CATEGORY_BUILDING:
                    waterLabel.setText(strings.getString("structure.building"));
                    break;
                case GardenCategory.CATEGORY_FIELD:
                    waterLabel.setText(strings.getString("structure.field"));
                    break;
                case GardenCategory.CATEGORY_ROAD:
                    waterLabel.setText(strings.getString("structure.road"));
                    break;
                case GardenCategory.CATEGORY_STREET_FURNITURE:
                    waterLabel.setText(strings.getString("structure.street.furniture"));
                    break;
                case GardenCategory.CATEGORY_WATER:
                    waterLabel.setText(strings.getString("structure.water"));
                    break;
                default:
                    waterLabel.setText(null);
                    break;
            }
        } else {
            int waterLevel = dimension.getWaterLevelAt(x, y);
            if (waterLevel > height) {
                waterLabel.setText(MessageFormat.format(strings.getString("fluid.level.1.depth.2"), dimension.getBitLayerValueAt(FloodWithLava.INSTANCE, x, y) ? 1 : 0, waterLevel, waterLevel - height));
            } else {
                waterLabel.setText(" ");
            }
        }
        Terrain terrain = dimension.getTerrainAt(x, y);
        if (terrain.isCustom()) {
            int index = terrain.getCustomTerrainIndex();
            materialLabel.setText(MessageFormat.format(strings.getString("material.custom.1.0"), Terrain.getCustomMaterial(index), index + 1));
        } else {
            materialLabel.setText(MessageFormat.format(strings.getString("material.0"), terrain.getName()));
        }
        // TODO: apparently this was sometimes invoked at or soon after startup,
        // with biomeNames being null, causing a NPE. How is this possible?
        if (dimension.getDim() == 0) {
            int biome = dimension.getLayerValueAt(Biome.INSTANCE, x, y);
            // TODO: is this too slow?
            if (biome == 255) {
                biome = dimension.getAutoBiome(x, y);
                if (biome != -1) {
                    if (biomeNames[biome] == null) {
                        biomeLabel.setText("Auto biome: biome " + biome);
                    } else {
                        biomeLabel.setText("Auto biome: " + biomeNames[biome]);
                    }
                }
            } else if (biome != -1) {
                if (biomeNames[biome] == null) {
                    biomeLabel.setText(MessageFormat.format(strings.getString("biome.0"), biome));
                } else {
                    biomeLabel.setText(MessageFormat.format(strings.getString("biome.0"), biomeNames[biome]));
                }
            }
        } else {
            biomeLabel.setText(strings.getString("biome-"));
        }
    }

    public Operation getActiveOperation() {
        return activeOperation;
    }

    public Brush getBrush() {
        return brush;
    }

    public Set<Layer> getHiddenLayers() {
        return Collections.unmodifiableSet(hiddenLayers);
    }

    public float getLevel() {
        return level;
    }

    public int getRadius() {
        return radius;
    }

    public Brush getToolBrush() {
        return toolBrush;
    }

    public float getToolLevel() {
        return toolLevel;
    }

    public int getZoom() {
        return zoom;
    }

    public final int getMaxRadius() {
        return maxRadius;
    }

    public final void setMaxRadius(int maxRadius) {
        this.maxRadius = maxRadius;
        if (radius > maxRadius) {
            radius = maxRadius;
            if (activeOperation instanceof RadiusOperation) {
                ((RadiusOperation) activeOperation).setRadius(radius);
            }
            view.setRadius(radius);
            radiusLabel.setText(MessageFormat.format(strings.getString("radius.0"), radius));
        }
    }

    public void open(File file, boolean askForConfirmation) {
        if (askForConfirmation && (world != null) && world.isDirty()) {
            int action = JOptionPane.showConfirmDialog(this, strings.getString("there.are.unsaved.changes"));
            if (action == JOptionPane.YES_OPTION) {
                if (! saveAs()) {
                    // User cancelled the save
                    return;
                }
            } else if (action != JOptionPane.NO_OPTION) {
                // User closed the confirmation dialog without making a choice
                return;
            }
        }
        open(file);
    }
    
    public void open(final File file) {
        logger.info("Loading world " + file.getAbsolutePath());
        setWorld(null); // Free up memory of the world and the undo buffer
        final World2 newWorld = ProgressDialog.executeTask(this, new ProgressTask<World2>() {
            @Override
            public String getName() {
                return strings.getString("loading.world");
            }

            @Override
            public World2 execute(ProgressReceiver progressReceiver) throws OperationCancelled {
                try {
                    try (WPCustomObjectInputStream in = new WPCustomObjectInputStream(new GZIPInputStream(new FileInputStream(file)), PluginManager.getPluginClassLoader(), AbstractObject.class)) {
                        Object object = in.readObject();
                        if (object instanceof World2) {
                            return (World2) object;
                        } else {
                            return migrate(object);
                        }
                    }
                } catch (ZipException e) {
                    logger.log(java.util.logging.Level.SEVERE, "ZipException while loading " + file, e);
                    reportDamagedFile();
                    return null;
                } catch (StreamCorruptedException e) {
                    logger.log(java.util.logging.Level.SEVERE, "StreamCorruptedException while loading " + file, e);
                    reportDamagedFile();
                    return null;
                } catch (EOFException e) {
                    logger.log(java.util.logging.Level.SEVERE, "EOFException while loading " + file, e);
                    reportDamagedFile();
                    return null;
                } catch (InvalidClassException e) {
                    logger.log(java.util.logging.Level.SEVERE, "InvalidClassException while loading " + file, e);
                    reportMissingPlugins();
                    return null;
                } catch (IOException e) {
                    if (e.getMessage().equals("Not in GZIP format")) {
                        logger.log(java.util.logging.Level.SEVERE, "IOException while loading " + file, e);
                        reportDamagedFile();
                        return null;
                    } else {
                        throw new RuntimeException("I/O error while loading world", e);
                    }
                } catch (ClassNotFoundException e) {
                    logger.log(java.util.logging.Level.SEVERE, "ClassNotFoundException while loading " + file, e);
                    reportMissingPlugins();
                    return null;
                } catch (IllegalArgumentException e) {
                    logger.log(java.util.logging.Level.SEVERE, "IllegalArgumentException while loading " + file, e);
                    reportWorldPainterTooOld();
                    return null;
                }
            }
            
            private void reportDamagedFile() {
                try {
                    SwingUtilities.invokeAndWait(() -> JOptionPane.showMessageDialog(App.this, strings.getString("the.file.is.damaged"), strings.getString("file.damaged"), JOptionPane.ERROR_MESSAGE));
                } catch (InterruptedException e) {
                    throw new RuntimeException("Thread interrupted while reporting damaged file " + file, e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException("Invocation target exception while reporting damaged file " + file, e);
                }
            }

            private void reportMissingPlugins() {
                try {
                    SwingUtilities.invokeAndWait(() -> JOptionPane.showMessageDialog(App.this, strings.getString("you.don.t.have.the.right.plugins.installed"), strings.getString("missing.plugin.s"), JOptionPane.ERROR_MESSAGE));
                } catch (InterruptedException e) {
                    throw new RuntimeException("Thread interrupted while reporting damaged file " + file, e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException("Invocation target exception while reporting damaged file " + file, e);
                }
            }

            private void reportWorldPainterTooOld() {
                try {
                    SwingUtilities.invokeAndWait(() -> JOptionPane.showMessageDialog(App.this, "This world was created with a newer version of WorldPainter.\nPlease upgrade WorldPainter to the latest version to load it.", "WorldPainter Too Old", JOptionPane.ERROR_MESSAGE));
                } catch (InterruptedException e) {
                    throw new RuntimeException("Thread interrupted while reporting damaged file " + file, e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException("Invocation target exception while reporting damaged file " + file, e);
                }
            }
        }, false);
        if (newWorld == null) {
            // The file was damaged
            return;
        }
        if (! isBackupFile(file)) {
            lastSelectedFile = file;
        } else {
            lastSelectedFile = null;
        }

        // Log an event
        Configuration config = Configuration.getInstance();
        EventVO event = new EventVO(EVENT_KEY_ACTION_OPEN_WORLD).addTimestamp();
        event.setAttribute(ATTRIBUTE_KEY_MAX_HEIGHT, newWorld.getMaxHeight());
        Dimension loadedDimension = newWorld.getDimension(0);
        event.setAttribute(ATTRIBUTE_KEY_TILES, loadedDimension.getTiles().size());
        logLayers(loadedDimension, event, "");
        loadedDimension = newWorld.getDimension(1);
        if (loadedDimension != null) {
            event.setAttribute(ATTRIBUTE_KEY_NETHER_TILES, loadedDimension.getTiles().size());
            logLayers(loadedDimension, event, "nether.");
        }
        loadedDimension = newWorld.getDimension(2);
        if (loadedDimension != null) {
            event.setAttribute(ATTRIBUTE_KEY_END_TILES, loadedDimension.getTiles().size());
            logLayers(loadedDimension, event, "end.");
        }
        if (newWorld.getImportedFrom() != null) {
            event.setAttribute(ATTRIBUTE_KEY_IMPORTED_WORLD, true);
        }
        config.logEvent(event);
        
        Set<World2.Warning> warnings = newWorld.getWarnings();
        if ((warnings != null) && (! warnings.isEmpty())) {
            for (World2.Warning warning: warnings) {
                switch (warning) {
                    case AUTO_BIOMES_DISABLED:
                        if (JOptionPane.showOptionDialog(this, "Automatic Biomes were previously enabled for this world but have been disabled.\nPress More Info for more information, including how to reenable it.", "Automatic Biomes Disabled", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, new Object[] {"More Info", "OK"}, "OK") == 0) {
                            try {
                                DesktopUtils.open(new URL("http://www.worldpainter.net/trac/wiki/NewAutomaticBiomes"));
                            } catch (MalformedURLException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        break;
                    case AUTO_BIOMES_ENABLED:
                        if (JOptionPane.showOptionDialog(this, "Automatic Biomes were previously disabled for this world but have been enabled.\nPress More Info for more information, including how to disable it.", "Automatic Biomes Enabled", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, new Object[] {"More Info", "OK"}, "OK") == 0) {
                            try {
                                DesktopUtils.open(new URL("http://www.worldpainter.net/trac/wiki/NewAutomaticBiomes"));
                            } catch (MalformedURLException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        break;
                }
            }
        }
        
        if (newWorld.isAskToConvertToAnvil() && (newWorld.getMaxHeight() == DEFAULT_MAX_HEIGHT_1) && (newWorld.getImportedFrom() == null)) {
            if (JOptionPane.showConfirmDialog(this, strings.getString("this.world.is.128.blocks.high"), strings.getString("convert.world.height"), JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                ChangeHeightDialog.resizeWorld(newWorld, HeightTransform.IDENTITY, DEFAULT_MAX_HEIGHT_2, this);
                // Force the version to "Anvil" if it was previously exported
                // with the old format
                if (newWorld.getVersion() != 0) {
                    newWorld.setVersion(SUPPORTED_VERSION_2);
                }
                
                // Log event
                config.logEvent(new EventVO(EVENT_KEY_ACTION_MIGRATE_HEIGHT).addTimestamp());
            }
            // Don't ask again, no matter what the user answered
            newWorld.setAskToConvertToAnvil(false);
        }
        
        if (newWorld.isAskToRotate() && (newWorld.getUpIs() == Direction.WEST) && (newWorld.getImportedFrom() == null)) {
            if (JOptionPane.showConfirmDialog(this, strings.getString("this.world.was.created.when.north.was.to.the.right"), strings.getString("rotate.world"), JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                ProgressDialog.executeTask(this, new ProgressTask<java.lang.Void>() {
                    @Override
                    public String getName() {
                        return strings.getString("rotating.world");
                    }

                    @Override
                    public java.lang.Void execute(ProgressReceiver progressReceiver) throws OperationCancelled {
                        newWorld.transform(CoordinateTransform.ROTATE_CLOCKWISE_270_DEGREES, progressReceiver);
                        return null;
                    }
                }, false);
                
                // Log event
                config.logEvent(new EventVO(EVENT_KEY_ACTION_MIGRATE_ROTATION).addTimestamp());
            }
            // Don't ask again, no matter what the user answered
            newWorld.setAskToRotate(false);
        }

        // Make sure the world name is always the same as the file name, to
        // avoid confusion, unless the only difference is illegal filename
        // characters changed into underscores. Do this here as well as when
        // saving, because the file might have been renamed
        String name = isBackupFile(file) ? getOriginalFile(file).getName() : file.getName();
        int p = name.lastIndexOf('.');
        if (p != -1) {
            name = name.substring(0, p);
        }
        String worldName = newWorld.getName();
        if (worldName.length() != name.length()) {
            newWorld.setName(name);
        } else {
            for (int i = 0; i < name.length(); i++) {
                if ((name.charAt(i) != '_') && (name.charAt(i) != worldName.charAt(i))) {
                    newWorld.setName(name);
                    break;
                }
            }
        }
        newWorld.setDirty(false);
        setWorld(newWorld);

        if (newWorld.getImportedFrom() != null) {
            enableImportedWorldOperation();
        } else {
            disableImportedWorldOperation();
        }
        setDimensionControlStates();
    }

    public static Mode getMode() {
        return mode;
    }

    public static void setMode(Mode mode) {
        App.mode = mode;
    }

    // RadiusControl

    @Override
    public void increaseRadius(int amount) {
        int oldRadius = radius;
        if (radius == 0) {
            radius = 1;
        } else {
            double factor = Math.pow(1.1, amount);
            radius = (int) (radius * factor);
            if (radius == oldRadius) {
                radius++;
            }
            if (radius > maxRadius) {
                radius = maxRadius;
            }
            if (radius == oldRadius) {
                return;
            }
        }
        if (activeOperation instanceof RadiusOperation) {
            ((RadiusOperation) activeOperation).setRadius(radius);
        }
        view.setRadius(radius);
        radiusLabel.setText(MessageFormat.format(strings.getString("radius.0"), radius));
    }

    @Override
    public void increaseRadiusByOne() {
        if (radius < maxRadius) {
            radius++;
            if (activeOperation instanceof RadiusOperation) {
                ((RadiusOperation) activeOperation).setRadius(radius);
            }
            view.setRadius(radius);
            radiusLabel.setText(MessageFormat.format(strings.getString("radius.0"), radius));
        }
    }
    
    @Override
    public void decreaseRadius(int amount) {
        if (radius > 0) {
            int oldRadius = radius;
            double factor = Math.pow(0.9, amount);
            radius = (int) (radius * factor);
            if (radius == oldRadius) {
                radius--;
            }
            if (radius < 0) {
                radius = 0;
            }
            if (radius == oldRadius) {
                return;
            }
            if (activeOperation instanceof RadiusOperation) {
                ((RadiusOperation) activeOperation).setRadius(radius);
            }
            view.setRadius(radius);
            radiusLabel.setText(MessageFormat.format(strings.getString("radius.0"), radius));
        }
    }

    @Override
    public void decreaseRadiusByOne() {
        if (radius > 0) {
            radius--;
            if (activeOperation instanceof RadiusOperation) {
                ((RadiusOperation) activeOperation).setRadius(radius);
            }
            view.setRadius(radius);
            radiusLabel.setText(MessageFormat.format(strings.getString("radius.0"), radius));
        }
    }
    
    // SeedListener
    
    @Override
    public void setSeed(long seed, Generator generator) {
        if (world != null) {
            world.setGenerator(generator);
            Dimension dim0 = world.getDimension(DIM_NORMAL);
            if (dim0 != null) {
                dim0.setMinecraftSeed(seed);
            }
        }
    }
    
    public static App getInstance() {
        if (instance == null) {
            instance = new App();
        }
        return instance;
    }

    public static App getInstanceIfExists() {
        return instance;
    }
    
    /**
     * Offer to save the current world, but only if is dirty.
     * 
     * @return <code>true</code> if there are no unsaved changes, the user saved
     *     the changes, or the user indicated that unsaved changes may be
     *     discarded (in other words, a destructive operation may proceed),
     *     <code>false</code> if there were unsaved changes and the user did not
     *     save them or indicate that they may be discarded (in other words, a
     *     destructive operation should be cancelled).
     */
    public boolean saveIfNecessary() {
        if ((world != null) && world.isDirty()) {
            int action = JOptionPane.showConfirmDialog(this, (lastSelectedFile != null) ? (MessageFormat.format(strings.getString("there.are.unsaved.changes.do.you.want.to.save.the.world.to.0"), lastSelectedFile.getName())) : strings.getString("there.are.unsaved.changes"));
            if (action == JOptionPane.YES_OPTION) {
                if (! save()) {
                    // The file was not saved for some reason
                    return false;
                }
            } else if (action != JOptionPane.NO_OPTION) {
                // User closed the confirmation dialog without making a choice
                return false;
            }
        }
        return true;
    }
    
    public boolean editCustomMaterial(int customMaterialIndex) {
        MixedMaterial material = Terrain.getCustomMaterial(customMaterialIndex);
        CustomMaterialDialog dialog;
        if (material == null) {
            material = MixedMaterial.create(BLK_DIRT);
            dialog = new CustomMaterialDialog(App.this, material, world.isExtendedBlockIds(), selectedColourScheme);
        } else {
            dialog = new CustomMaterialDialog(App.this, material, world.isExtendedBlockIds(), selectedColourScheme);
        }
        dialog.setVisible(true);
        if (! dialog.isCancelled()) {
            material = MixedMaterialManager.getInstance().register(material);
            Terrain.setCustomMaterial(customMaterialIndex, material);
            customMaterialButtons[customMaterialIndex].setIcon(new ImageIcon(material.getIcon(selectedColourScheme)));
            customMaterialButtons[customMaterialIndex].setToolTipText(MessageFormat.format(strings.getString("customMaterial.0.right.click.to.change"), material));
            view.refreshTiles();
            return true;
        }
        return false;
    }

    public void deselectTool() {
        toolButtonGroup.clearSelection();
    }

    public void deselectPaint() {
        paintButtonGroup.clearSelection();
        paint = PaintFactory.NULL_PAINT;
        paintChanged();
    }

    /**
     * Gets all currently loaded layers, including custom layers and including
     * hidden ones (from the panel or the view), regardless of whether they are
     * used on the map.
     */
    public Set<Layer> getAllLayers() {
        Set<Layer> allLayers = new HashSet<>(layers);
        allLayers.add(Populate.INSTANCE);
        if (readOnlyToggleButton.isEnabled()) {
            allLayers.add(ReadOnly.INSTANCE);
        }
        allLayers.addAll(getCustomLayers());
        return allLayers;
    }
    
    /**
     * Gets all currently loaded custom layers, including hidden ones (from the
     * panel or the view), regardless of whether they are used on the map.
     */
    public Set<CustomLayer> getCustomLayers() {
        Set<CustomLayer> customLayers = new HashSet<>();
        customLayers.addAll(paletteManager.getLayers());
        customLayers.addAll(layersWithNoButton);
        return customLayers;
    }

    /**
     * Gets all currently loaded custom layers, including hidden ones (from the
     * panel or the view), regardless of whether they are used on the map, by
     * palette (which will be <code>null</code> for hidden layers). For the
     * visible layers the collections will be in the order they are displayed on
     * the palette.
     */
    public Map<String, Collection<CustomLayer>> getCustomLayersByPalette() {
        Map<String, Collection<CustomLayer>> customLayers = paletteManager.getLayersByPalette();
        if (! layersWithNoButton.isEmpty()) {
            customLayers.put(null, layersWithNoButton);
        }
        return customLayers;
    }

    public ColourScheme getColourScheme() {
        return selectedColourScheme;
    }

    public void setFilter(Filter filter) {
        if (activeOperation instanceof PaintOperation) {
            this.filter = filter;
        } else {
            toolFilter = filter;
        }
        if (activeOperation instanceof RadiusOperation) {
            ((RadiusOperation) activeOperation).setFilter(filter);
        }
    }

    public CustomBiomeManager getCustomBiomeManager() {
        return customBiomeManager;
    }

    public void showCustomTerrainButtonPopup(final int customMaterialIndex) {
        final JToggleButton button = customMaterialButtons[customMaterialIndex];
        JPopupMenu popupMenu = new JPopupMenu();
        final MixedMaterial material = Terrain.getCustomMaterial(customMaterialIndex);
//        JLabel label = new JLabel(MessageFormat.format(strings.getString("current.material.0"), (material != null) ? material : "none"));
//        popupMenu.add(label);

        MixedMaterial[] customMaterials = MixedMaterialManager.getInstance().getMaterials();
        if (customMaterials.length > 0) {
            JMenu existingMaterialsMenu = new JMenu("Select existing material");
            Set<MixedMaterial> customTerrainMaterials = new HashSet<>();
            for (int i = 0; i < Terrain.CUSTOM_TERRAIN_COUNT; i++) {
                if (Terrain.getCustomTerrain(i).isConfigured()) {
                    customTerrainMaterials.add(Terrain.getCustomMaterial(i));
                }
            }
            for (final MixedMaterial customMaterial: customMaterials) {
                if (customTerrainMaterials.contains(customMaterial)) {
                    continue;
                }
                JMenuItem menuItem = new JMenuItem(customMaterial.getName());
                menuItem.setIcon(new ImageIcon(customMaterial.getIcon(selectedColourScheme)));
                menuItem.addActionListener(e -> {
                    Terrain.setCustomMaterial(customMaterialIndex, customMaterial);
                    button.setIcon(new ImageIcon(customMaterial.getIcon(selectedColourScheme)));
                    button.setToolTipText(MessageFormat.format(strings.getString("customMaterial.0.right.click.to.change"), customMaterial));
                    view.refreshTiles();
                });
                existingMaterialsMenu.add(menuItem);
            }
            if (existingMaterialsMenu.getMenuComponentCount() > 0) {
                popupMenu.add(existingMaterialsMenu);
            }
        }

        JMenuItem menuItem = new JMenuItem(((material != null) ? "Edit custom material" : strings.getString("select.custom.material")) + "...");
        menuItem.addActionListener(e -> {
            if (editCustomMaterial(customMaterialIndex)) {
                button.setSelected(true);
            }
        });
        popupMenu.add(menuItem);

        menuItem = new JMenuItem("Import from file...");
        menuItem.addActionListener(e -> {
            if (importCustomMaterial(customMaterialIndex)) {
                button.setSelected(true);
            }
        });
        popupMenu.add(menuItem);

        if (material != null) {
            menuItem = new JMenuItem("Export to file...");
            menuItem.addActionListener(e -> exportCustomMaterial(customMaterialIndex));
            popupMenu.add(menuItem);
        }

        popupMenu.show(button, button.getWidth(), 0);
    }

    // BrushOptions.Listener

    @Override
    public void filterChanged(Filter newFilter) {
        setFilter(newFilter);
    }

    // CustomBiomeListener
    
    @Override
    public void customBiomeAdded(CustomBiome customBiome) {
        biomeNames[customBiome.getId()] = customBiome.getName() + " (ID " + customBiome.getId() + ")";
    }

    @Override
    public void customBiomeChanged(CustomBiome customBiome) {
        biomeNames[customBiome.getId()] = customBiome.getName() + " (ID " + customBiome.getId() + ")";
    }

    @Override
    public void customBiomeRemoved(CustomBiome customBiome) {
        biomeNames[customBiome.getId()] = null;
    }
    
    // DockableHolder

    @Override
    public DockingManager getDockingManager() {
        return dockingManager;
    }
    
    void exit() {
        if (saveIfNecessary()) {
            System.exit(0);
        }
    }

    ColourScheme getColourScheme(int index) {
        return colourSchemes[index];
    }

    private void loadCustomBrushes() {
        customBrushes = new TreeMap<>();
        File brushesDir = new File(Configuration.getConfigDir(), "brushes");
        if (brushesDir.isDirectory()) {
            loadCustomBrushes(CUSTOM_BRUSHES_DEFAULT_TITLE, brushesDir);
        }
    }
    
    private void loadCustomBrushes(String category, File brushesDir) {
        File[] files = brushesDir.listFiles(new java.io.FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (pathname.isDirectory()) {
                    return true;
                }
                String name = pathname.getName();
                for (String extension : extensions) {
                    if (name.toLowerCase().endsWith(extension)) {
                        return true;
                    }
                }
                return false;
            }

            private final String[] extensions = ImageIO.getReaderFileSuffixes();
        });
        List<Brush> brushes = new ArrayList<>();
        for (File file: files) {
            if (file.isDirectory()) {
                loadCustomBrushes(file.getName(), file);
            } else {
                try {
                    brushes.add(new BitmapBrush(file));
                } catch (RuntimeException e) {
                    logger.log(Level.SEVERE, "There was an error loading custom brush image file " + file.getName() + "; skipping file", e);
                }
            }
        }
        if (! brushes.isEmpty()) {
            customBrushes.put(category, brushes);
        }
    }
    
    private void maybePing() {
        Configuration config = Configuration.getInstance();
        if (config.getPingAllowed() == null) {
            int rc = JOptionPane.showConfirmDialog(this, strings.getString("may.we.have.your.permission"), strings.getString("usage.statistics.permission"), JOptionPane.YES_NO_OPTION);
            if (rc == JOptionPane.YES_OPTION) {
                config.setPingAllowed(Boolean.TRUE);
            } else if (rc == JOptionPane.NO_OPTION) {
                config.setPingAllowed(Boolean.FALSE);
            } else {
                // User closed the dialog without making a choice. Ask again
                // next time.
                return;
            }
        }
        if (config.getPingAllowed()) {
            ping();
        }
    }
    
    private void ping() {
        final UsageVO usageVO = new UsageVO();
        Configuration config = Configuration.getInstance();
        usageVO.setInstall(config.getUuid());
        usageVO.setLaunchCount(config.getLaunchCount());
        List<EventVO> eventLog = config.getEventLog();
        if ((eventLog != null) && (! eventLog.isEmpty())) {
            usageVO.setEvents(eventLog);
        }
        usageVO.setWPVersion(Version.VERSION);
        Main.privateContext.submitUsageData(usageVO);
    }

    private void newWorld() {
        if (! saveIfNecessary()) {
            return;
        }
        final NewWorldDialog dialog = new NewWorldDialog(this, strings.getString("generated.world"), World2.DEFAULT_OCEAN_SEED, DIM_NORMAL, Configuration.getInstance().getDefaultMaxHeight());
        dialog.setVisible(true);
        if (! dialog.isCancelled()) {
            setWorld(null); // Free up memory of the world and the undo buffer
            if (! dialog.checkMemoryRequirements(this)) {
                return;
            }
            World2 newWorld = ProgressDialog.executeTask(this, new ProgressTask<World2>() {
                @Override
                public String getName() {
                    return strings.getString("creating.new.world");
                }

                @Override
                public World2 execute(ProgressReceiver progressReceiver) throws OperationCancelled {
                    return dialog.getSelectedWorld(progressReceiver);
                }
            });
            if (newWorld == null) {
                // Operation cancelled by user
                return;
            }

            // Log an event
            Configuration config = Configuration.getInstance();
            if (config != null) {
                EventVO event = new EventVO(EVENT_KEY_ACTION_NEW_WORLD).addTimestamp();
                event.setAttribute(ATTRIBUTE_KEY_MAX_HEIGHT, newWorld.getMaxHeight());
                event.setAttribute(ATTRIBUTE_KEY_TILES, newWorld.getDimension(0).getTiles().size());
                config.logEvent(event);
            }
            
            setWorld(newWorld);
            lastSelectedFile = null;
            disableImportedWorldOperation();
            setDimensionControlStates();
        }
    }

    private void open() {
        if (! saveIfNecessary()) {
            return;
        }
        File dir;
        Configuration config = Configuration.getInstance();
        if (lastSelectedFile != null) {
            dir = lastSelectedFile.getParentFile();
        } else if ((config != null) && (config.getWorldDirectory() != null)) {
            dir = config.getWorldDirectory();
        } else {
            dir = DesktopUtils.getDocumentsFolder();
        }
        File selectedFile = FileUtils.openFile(this, "Select a WorldPainter world", dir,
                new FileFilter() {
                    @Override
                    public boolean accept(File f) {
                        return f.isDirectory()
                                || f.getName().toLowerCase().endsWith(".world");
                    }

                    @Override
                    public String getDescription() {
                        return strings.getString("worldpainter.files.world");
                    }
                });
        if (selectedFile != null) {
            if (! selectedFile.isFile()) {
                if (logger.isLoggable(Level.FINE)) {
                    try {
                        logger.fine("Path not a file according to File.isFile(): \"" + selectedFile + "\" (directory: " + selectedFile.isDirectory() + "; length: " + selectedFile.length() + "; absolutePath: \"" + selectedFile.getAbsolutePath() + "\"; canonicalPath: \"" + selectedFile.getCanonicalPath() + "\")");
                    } catch (IOException e) {
                        logger.fine("Path not a file according to File.isFile(): \"" + selectedFile + "\" (directory: " + selectedFile.isDirectory() + "; length: " + selectedFile.length() + "; absolutePath: \"" + selectedFile.getAbsolutePath() + "\")");
                        logger.log(Level.WARNING, "I/O error while trying to report canonical path of file: \"" + selectedFile + "\"", e);
                    }
                }
                JOptionPane.showMessageDialog(this, "The specified path does not exist or is not a file", "File Does Not Exist", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (! selectedFile.canRead()) {
                JOptionPane.showMessageDialog(this, "WorldPainter is not authorised to read the selected file", "Access Denied", JOptionPane.ERROR_MESSAGE);
                return;
            }
            open(selectedFile);
            if (config != null) {
                config.setWorldDirectory(selectedFile.getParentFile());
            }
        }
    }
    
    /**
     * If the world was loaded from an existing file, and/or was previously
     * saved, save the world to the same file, without asking for confirmation.
     * Otherwise do the same thing as {@link #saveAs()}. Shows a progress
     * indicator while saving.
     * 
     * @return <code>true</code> if the file was saved.
     */
    private boolean save() {
        if (lastSelectedFile == null) {
            return saveAs();
        } else {
            return save(lastSelectedFile);
        }
    }
    
    /**
     * Ask for a filename and save the world with that name. If a file exists
     * with the name, ask for confirmation to overwrite it. Shows a progress
     * indicator while saving, and a confirmation when it is saved.
     * 
     * @return <code>true</code> if the file was saved.
     */
    private boolean saveAs() {
        Configuration config = Configuration.getInstance();
        File file = lastSelectedFile;
        if (file == null) {
            if ((config != null) && (config.getWorldDirectory() != null)) {
                file = new File(config.getWorldDirectory(), FileUtils.sanitiseName(world.getName().trim() + ".world"));
            } else {
                file = new File(DesktopUtils.getDocumentsFolder(), FileUtils.sanitiseName(world.getName().trim() + ".world"));
            }
        }
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(file);
        fileChooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory()
                    || f.getName().toLowerCase().endsWith(".world");
            }

            @Override
            public String getDescription() {
                return strings.getString("worldpainter.files.world");
            }
        });
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (fileChooser.showSaveDialog(App.this) == JFileChooser.APPROVE_OPTION) {
            file = fileChooser.getSelectedFile();
            if (! file.getName().toLowerCase().endsWith(".world")) {
                file = new File(file.getParentFile(), file.getName() + ".world");
            }
            if (file.exists() && (JOptionPane.showConfirmDialog(App.this, strings.getString("do.you.want.to.overwrite.the.file"), strings.getString("file.exists"), JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)) {
                return false;
            }

            if (save(file)) {
                JOptionPane.showMessageDialog(App.this, strings.getString("file.saved"), strings.getString("success"), JOptionPane.INFORMATION_MESSAGE);
                return true;
            }
        }
        return false;
    }
    
    /**
     * Save the world to the specified file, overwriting it if it already exists
     * without asking for confirmation. Shows a progress indicator while saving.
     * 
     * @param file The file to which to save the world.
     */
    private boolean save(File file) {
        // Check for write access to directory
        if (! file.getParentFile().isDirectory()) {
            JOptionPane.showMessageDialog(this, strings.getString("the.selected.path.does.not.exist"), strings.getString("non.existant.path"), JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (! file.getParentFile().canWrite()) {
            JOptionPane.showMessageDialog(this, strings.getString("you.do.not.have.write.access"), strings.getString("access.denied"), JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        // Normalise the filename
        String name = file.getName();
        name = name.trim();
        if (name.isEmpty()) {
            name = strings.getString("generated.world") + ".world"; // NOI18N
        } else {
            name = FileUtils.sanitiseName(name);
        }
        final File normalisedFile = new File(file.getParentFile(), name);
        
        // Make sure the world name is always the same as the file name, to
        // avoid confusion (unless the only difference is illegal filename
        // characters changed into underscores
        int p = name.lastIndexOf('.');
        if (p != -1) {
            name = name.substring(0, p).trim();
        }
        String worldName = world.getName();
        if (worldName.length() != name.length()) {
            world.setName(name);
            setTitle("WorldPainter - " + name + " - " + dimension.getName()); // NOI18N
        } else {
            for (int i = 0; i < name.length(); i++) {
                if ((name.charAt(i) != '_') && (name.charAt(i) != worldName.charAt(i))) {
                    world.setName(name);
                    setTitle("WorldPainter - " + name + " - " + dimension.getName()); // NOI18N
                    break;
                }
            }
        }

        logger.info("Saving world " + world.getName() + " to "+ file.getAbsolutePath());

        saveCustomMaterials();
        
        saveCustomBiomes();
        
        // Remove the existing custom object layers and save the list of
        // custom layers to the dimension to preserve layers which aren't
        // currently in use
        if (! paletteManager.isEmpty()) {
            List<CustomLayer> customLayers = new ArrayList<>();
            for (Palette palette: paletteManager.getPalettes()) {
                customLayers.addAll(palette.getLayers());
            }
            dimension.setCustomLayers(customLayers);
        } else {
            dimension.setCustomLayers(Collections.EMPTY_LIST);
        }

        if (dimension != null) {
            Point viewPosition = view.getViewCentreInWorldCoords();
            if (viewPosition != null) {
                this.dimension.setLastViewPosition(viewPosition);
            }
        }

        ProgressDialog.executeTask(this, new ProgressTask<java.lang.Void>() {
            @Override
            public String getName() {
                return strings.getString("saving.world");
            }

            @Override
            public java.lang.Void execute(ProgressReceiver progressReceiver) throws OperationCancelled {
                try {
                    Configuration config = Configuration.getInstance();
                    if ((config.getWorldFileBackups() > 0) && normalisedFile.isFile()) {
                        progressReceiver.setMessage(strings.getString("creating.backup.s"));
                        for (int i = config.getWorldFileBackups(); i > 0; i--) {
                            File nextBackupFile = (i > 1) ? BackupUtil.getBackupFile(normalisedFile, i - 1) : normalisedFile;
                            if (nextBackupFile.isFile()) {
                                File backupFile = BackupUtil.getBackupFile(normalisedFile, i);
                                if (backupFile.isFile()) {
                                    if (! backupFile.delete()) {
                                        throw new RuntimeException("Could not delete old backup file " + backupFile);
                                    }
                                }
                                if (! nextBackupFile.renameTo(backupFile)) {
                                    throw new RuntimeException("Could not move " + nextBackupFile + " to " + backupFile);
                                }
                            }
                        }
                        progressReceiver.setMessage(null);
                    }

                    try (ObjectOutputStream out = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(normalisedFile)))) {
                        out.writeObject(world);
                    }
                    
                    Map<String, byte[]> layoutData = config.getJideLayoutData();
                    if (layoutData == null) {
                        layoutData = new HashMap<>();
                    }
                    layoutData.put(world.getName(), dockingManager.getLayoutRawData());
                    config.setJideLayoutData(layoutData);
                    
                    return null;
                } catch (IOException e) {
                    throw new RuntimeException("I/O error saving file (message: " + e.getMessage() + ")", e);
                }
            }
        }, false);

        // Log an event
        Configuration config = Configuration.getInstance();
        if (config != null) {
            EventVO event = new EventVO(EVENT_KEY_ACTION_SAVE_WORLD).addTimestamp();
            event.setAttribute(ATTRIBUTE_KEY_MAX_HEIGHT, world.getMaxHeight());
            Dimension loadedDimension = world.getDimension(0);
            event.setAttribute(ATTRIBUTE_KEY_TILES, loadedDimension.getTiles().size());
            logLayers(loadedDimension, event, "");
            loadedDimension = world.getDimension(1);
            if (loadedDimension != null) {
                event.setAttribute(ATTRIBUTE_KEY_NETHER_TILES, loadedDimension.getTiles().size());
                logLayers(loadedDimension, event, "nether.");
            }
            loadedDimension = world.getDimension(2);
            if (loadedDimension != null) {
                event.setAttribute(ATTRIBUTE_KEY_END_TILES, loadedDimension.getTiles().size());
                logLayers(loadedDimension, event, "end.");
            }
            if (world.getImportedFrom() != null) {
                event.setAttribute(ATTRIBUTE_KEY_IMPORTED_WORLD, true);
            }
            config.logEvent(event);
        }

        if (currentUndoManager != null) {
            currentUndoManager.armSavePoint();
        }
        world.setDirty(false);
        lastSelectedFile = file;

        Configuration.getInstance().setWorldDirectory(file.getParentFile());
        
        return true;
    }
    
    private boolean isBackupFile(File file) {
        String filename = file.getName();
        if (filename.toLowerCase().endsWith(".world")) {
            filename = filename.substring(0, filename.length() - 6);
        }
        int p = filename.length() - 1;
        while ((p > 0) && Character.isDigit(filename.charAt(p))) {
            p--;
        }
        // At this point p points to the dot in front of the backup number, if
        // there is one
        return (p > 0) && (p < (filename.length() - 1)) && (filename.charAt(p) == '.');
    }
    
    private File getOriginalFile(File backupFile) {
        String extension = "";
        String filename = backupFile.getName();
        if (filename.toLowerCase().endsWith(".world")) {
            extension = filename.substring(filename.length() - 6);
            filename = filename.substring(0, filename.length() - 6);
        }
        int p = filename.length() - 1;
        while ((p > 0) && Character.isDigit(filename.charAt(p))) {
            p--;
        }
        // At this point p points to the dot in front of the backup number
        return new File(backupFile.getParentFile(), filename.substring(0, p) + extension);
    }
    
    private void addRemoveTiles() {
        TileEditor tileEditor = new TileEditor(this, dimension, selectedColourScheme, autoBiomeScheme, customBiomeManager, hiddenLayers, false, 10, view.getLightOrigin());
        tileEditor.setVisible(true);
    }
    
    private void importWorld() {
        if (! saveIfNecessary()) {
            return;
        }
        if (! Configuration.getInstance().isImportWarningDisplayed()) {
            JOptionPane.showMessageDialog(this, strings.getString("the.import.functionality.only.imports.the.i.landscape"), strings.getString("information"), JOptionPane.INFORMATION_MESSAGE);
        }
        MapImportDialog dialog = new MapImportDialog(this);
        dialog.setVisible(true);
        if (! dialog.isCancelled()) {
            setWorld(dialog.getImportedWorld());
            lastSelectedFile = null;
            Configuration config = Configuration.getInstance();
            config.setImportWarningDisplayed(true);
            enableImportedWorldOperation();
            setDimensionControlStates();
        }
    }
    
    private void importHeightMap() {
        if (! saveIfNecessary()) {
            return;
        }
        ImportHeightMapDialog dialog = new ImportHeightMapDialog(this, selectedColourScheme);
        dialog.setVisible(true);
        if (! dialog.isCancelled()) {
            setWorld(null);
            World2 importedWorld = dialog.getImportedWorld();
            setWorld(importedWorld);
            lastSelectedFile = null;
        }
    }
    
    private void merge() {
        if ((world.getImportedFrom() != null) && (! world.isAllowMerging())) {
            JOptionPane.showMessageDialog(this, strings.getString("this.world.was.imported.before.the.great.coordinate.shift"), strings.getString("merge.not.allowed"), JOptionPane.ERROR_MESSAGE);
            return;
        }
        if ((world.getImportedFrom() == null) && (JOptionPane.showConfirmDialog(this, strings.getString("this.world.was.not.imported"), strings.getString("not.imported"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION)) {
            return;
        }
        if ((world.getDimensions().length > 1) && (JOptionPane.showConfirmDialog(this, strings.getString("merging.the.nether.or.end.is.not.yet.supported"), strings.getString("merging.nether.not.supported"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION)) {
            return;
        }
        Configuration config = Configuration.getInstance();
        if (((config == null) || (! config.isMergeWarningDisplayed())) && (JOptionPane.showConfirmDialog(this, strings.getString("this.is.experimental.and.unfinished.functionality"), strings.getString("experimental.functionality"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION)) {
            return;
        }
        MergeWorldDialog dialog = new MergeWorldDialog(this, world, autoBiomeScheme, selectedColourScheme, customBiomeManager, hiddenLayers, false, 10, view.getLightOrigin());
        view.setInhibitUpdates(true);
        try {
            dialog.setVisible(true);
        } finally {
            view.setInhibitUpdates(false);
        }
    }

    private void updateZoomLabel() {
        double factor = Math.pow(2.0, zoom);
        int zoomPercentage = (int) (100 * factor);
        zoomLabel.setText(MessageFormat.format(strings.getString("zoom.0"), zoomPercentage));
        glassPane.setScale((float) factor);
    }

    private void initComponents() {
        view = new WorldPainter(selectedColourScheme, autoBiomeScheme, customBiomeManager);
        view.setBackground(new Color(VoidRenderer.getColour()));
        view.setRadius(radius);
        view.setBrushShape(brush.getBrushShape());
        final Cursor cursor = Toolkit.getDefaultToolkit().createCustomCursor(IconUtils.loadImage("org/pepsoft/worldpainter/cursor.png"), new Point(15, 15), "Custom Crosshair");
        view.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setCursor(cursor);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setCursor(null);
            }
        });
        TiledImageViewerContainer viewContainer = new TiledImageViewerContainer(view);

        glassPane = new GlassPane();
        JRootPane privateRootPane = new JRootPane();
        privateRootPane.setContentPane(viewContainer);
        privateRootPane.setGlassPane(glassPane);
        glassPane.setVisible(true);

        // Set up docking framework
        JPanel contentContainer = new JPanel(new BorderLayout());
        getContentPane().add(contentContainer, BorderLayout.CENTER);
        dockingManager = new DefaultDockingManager(this, contentContainer);
        if (SystemUtils.isLinux()) {
            // On Linux, at least in the GTK look and feel, the default
            // (whatever it is) doesn't work; nothing is displayed
            dockingManager.setOutlineMode(DockingManager.MIX_OUTLINE_MODE);
        }
        dockingManager.setGroupAllowedOnSidePane(false);
        dockingManager.setTabbedPaneCustomizer(tabbedPane -> tabbedPane.setTabPlacement(JTabbedPane.LEFT));
        Workspace workspace = dockingManager.getWorkspace();
        workspace.setLayout(new BorderLayout());
        workspace.add(privateRootPane, BorderLayout.CENTER);

        setJMenuBar(createMenuBar());
        
        getContentPane().add(createToolBar(), BorderLayout.NORTH);

        getContentPane().add(createStatusBar(), BorderLayout.SOUTH);

        final ScrollController scrollController = new ScrollController();
        scrollController.install();

        mapDragControl = new MapDragControl() {
            @Override
            public boolean isMapDraggingInhibited() {
                return mapDraggingInhibited;
            }

            @Override
            public void setMapDraggingInhibited(boolean mapDraggingInhibited) {
                if (mapDraggingInhibited != this.mapDraggingInhibited) {
                    this.mapDraggingInhibited = mapDraggingInhibited;
                    if (mapDraggingInhibited) {
                        scrollController.uninstall();
                    } else {
                        scrollController.install();
                    }
                }
            }
            
            private boolean mapDraggingInhibited;
        };
        
        dockingManager.addFrame(createDockableFrame(createToolPanel(), "Tools", DOCK_SIDE_WEST, 1));

        dockingManager.addFrame(createDockableFrame(createLayerPanel(), "Layers", DOCK_SIDE_WEST, 3));

        dockingManager.addFrame(createDockableFrame(createTerrainPanel(), "Terrain", DOCK_SIDE_WEST, 3));

        dockingManager.addFrame(createDockableFrame(createCustomTerrainPanel(), "customTerrain", "Custom Terrain", DOCK_SIDE_WEST, 3));

        biomesPanel = createDockableFrame(createBiomesPanel(), "Biomes", DOCK_SIDE_WEST, 3);
        dockingManager.addFrame(biomesPanel);

        dockingManager.addFrame(createDockableFrame(createAnnotationsPanel(), "Annotations", DOCK_SIDE_WEST, 3));

        dockingManager.addFrame(createDockableFrame(createBrushPanel(), "Brushes", DOCK_SIDE_EAST, 1));
        
        if (customBrushes.containsKey(CUSTOM_BRUSHES_DEFAULT_TITLE)) {
            dockingManager.addFrame(createDockableFrame(createCustomBrushPanel(CUSTOM_BRUSHES_DEFAULT_TITLE, customBrushes.get(CUSTOM_BRUSHES_DEFAULT_TITLE)), "customBrushesDefault", "Custom Brushes", DOCK_SIDE_EAST, 1));
        }
        for (Map.Entry<String, List<Brush>> entry: customBrushes.entrySet()) {
            if (entry.getKey().equals(CUSTOM_BRUSHES_DEFAULT_TITLE)) {
                continue;
            }
            dockingManager.addFrame(createDockableFrame(createCustomBrushPanel(entry.getKey(), entry.getValue()), "customBrushes." + entry.getKey(), entry.getKey(), DOCK_SIDE_EAST, 1));
        }
        
        dockingManager.addFrame(createDockableFrame(createBrushSettingsPanel(), "brushSettings", "Brush Settings", DOCK_SIDE_EAST, 2));

        Configuration config = Configuration.getInstance();
        if (config.getDefaultJideLayoutData() != null) {
            dockingManager.loadLayoutFrom(new ByteArrayInputStream(config.getDefaultJideLayoutData()));
        } else {
            dockingManager.resetToDefault();
        }

        MouseAdapter viewListener = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                Point worldCoords = view.viewToWorld(e.getX(), e.getY());
                updateStatusBar(worldCoords.x, worldCoords.y);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                locationLabel.setText(strings.getString("location-"));
                heightLabel.setText(" ");
                waterLabel.setText(" ");
                App.this.biomeLabel.setText(" ");
                materialLabel.setText(" ");
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (e.isControlDown()) {
                    int oldZoom = zoom;
                    if (e.getWheelRotation() < 0) {
                        zoom = Math.min(zoom - e.getWheelRotation(), 6);
                    } else {
                        zoom = Math.max(zoom - e.getWheelRotation(), -4);
                    }
                    if (zoom != oldZoom) {
                        view.setZoom(zoom, e.getX(), e.getY());
                        updateZoomLabel();
                        ACTION_ZOOM_IN.setEnabled(zoom < 6);
                        ACTION_ZOOM_OUT.setEnabled(zoom > -4);
                        ACTION_ZOOM_RESET.setEnabled(zoom != 0);
                    }
                } else if (e.isAltDown() || e.isAltGraphDown()) {
                    if (e.getWheelRotation() < 0) {
                        ACTION_ROTATE_BRUSH_LEFT.actionPerformed(new ActionEvent(e.getSource(), e.getID(), e.paramString()));
                    } else {
                        ACTION_ROTATE_BRUSH_RIGHT.actionPerformed(new ActionEvent(e.getSource(), e.getID(), e.paramString()));
                    }
                } else if (activeOperation instanceof RadiusOperation) {
                    if (e.isShiftDown()) {
                        if (e.getWheelRotation() < 0) {
                            decreaseRadiusByOne();
                        } else {
                            increaseRadiusByOne();
                        }
                    } else {
                        if (e.getWheelRotation() < 0) {
                            decreaseRadius(-e.getWheelRotation());
                        } else {
                            increaseRadius(e.getWheelRotation());
                        }
                    }
                }
            }
        };
        view.addMouseMotionListener(viewListener);
        view.addMouseListener(viewListener);
        view.addMouseWheelListener(viewListener);

        JRootPane rootPane = getRootPane();
        ActionMap actionMap = rootPane.getActionMap();
        actionMap.put(ACTION_NAME_INCREASE_RADIUS, new BetterAction(ACTION_NAME_INCREASE_RADIUS, strings.getString("increase.radius")) {
            @Override
            public void performAction(ActionEvent e) {
                increaseRadius(1);
            }

            private static final long serialVersionUID = 2011090601L;
        });
        actionMap.put(ACTION_NAME_INCREASE_RADIUS_BY_ONE, new BetterAction(ACTION_NAME_INCREASE_RADIUS_BY_ONE, "Increase brush radius by one") {
            @Override
            public void performAction(ActionEvent e) {
                increaseRadiusByOne();
            }

            private static final long serialVersionUID = 2011090601L;
        });
        actionMap.put(ACTION_NAME_DECREASE_RADIUS, new BetterAction(ACTION_NAME_DECREASE_RADIUS, strings.getString("decrease.radius")) {
            @Override
            public void performAction(ActionEvent e) {
                decreaseRadius(1);
            }

            private static final long serialVersionUID = 2011090601L;
        });
        actionMap.put(ACTION_NAME_DECREASE_RADIUS_BY_ONE, new BetterAction(ACTION_NAME_DECREASE_RADIUS_BY_ONE, "Decrease brush radius by one") {
            @Override
            public void performAction(ActionEvent e) {
                decreaseRadiusByOne();
            }

            private static final long serialVersionUID = 2011090601L;
        });
        actionMap.put(ACTION_NAME_REDO, ACTION_REDO);
        actionMap.put(ACTION_NAME_ZOOM_IN, ACTION_ZOOM_IN);
        actionMap.put(ACTION_NAME_ZOOM_OUT, ACTION_ZOOM_OUT);
        actionMap.put(ACTION_ZOOM_RESET.getName(), ACTION_ZOOM_RESET);
        actionMap.put(ACTION_ROTATE_BRUSH_LEFT.getName(), ACTION_ROTATE_BRUSH_LEFT);
        actionMap.put(ACTION_ROTATE_BRUSH_RIGHT.getName(), ACTION_ROTATE_BRUSH_RIGHT);
        actionMap.put(ACTION_ROTATE_BRUSH_RESET.getName(), ACTION_ROTATE_BRUSH_RESET);
        actionMap.put(ACTION_ROTATE_BRUSH_RIGHT_30_DEGREES.getName(), ACTION_ROTATE_BRUSH_RIGHT_30_DEGREES);
        actionMap.put(ACTION_ROTATE_BRUSH_RIGHT_45_DEGREES.getName(), ACTION_ROTATE_BRUSH_RIGHT_45_DEGREES);
        actionMap.put(ACTION_ROTATE_BRUSH_RIGHT_90_DEGREES.getName(), ACTION_ROTATE_BRUSH_RIGHT_90_DEGREES);

        int platformCommandMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        inputMap.put(KeyStroke.getKeyStroke(VK_SUBTRACT, 0),                                     ACTION_NAME_DECREASE_RADIUS);
        inputMap.put(KeyStroke.getKeyStroke(VK_MINUS,    0),                                     ACTION_NAME_DECREASE_RADIUS);
        inputMap.put(KeyStroke.getKeyStroke(VK_ADD,      0),                                     ACTION_NAME_INCREASE_RADIUS);
        inputMap.put(KeyStroke.getKeyStroke(VK_EQUALS,   SHIFT_DOWN_MASK),                       ACTION_NAME_INCREASE_RADIUS);
        inputMap.put(KeyStroke.getKeyStroke(VK_SUBTRACT, SHIFT_DOWN_MASK),                       ACTION_NAME_DECREASE_RADIUS_BY_ONE);
        inputMap.put(KeyStroke.getKeyStroke(VK_MINUS,    SHIFT_DOWN_MASK),                       ACTION_NAME_DECREASE_RADIUS_BY_ONE);
        inputMap.put(KeyStroke.getKeyStroke(VK_ADD,      SHIFT_DOWN_MASK),                       ACTION_NAME_INCREASE_RADIUS_BY_ONE);
        inputMap.put(KeyStroke.getKeyStroke(VK_Z,        platformCommandMask | SHIFT_DOWN_MASK), ACTION_NAME_REDO);
        inputMap.put(KeyStroke.getKeyStroke(VK_MINUS,    platformCommandMask),                   ACTION_NAME_ZOOM_OUT);
        inputMap.put(KeyStroke.getKeyStroke(VK_EQUALS,   platformCommandMask | SHIFT_DOWN_MASK), ACTION_NAME_ZOOM_IN);
        inputMap.put(KeyStroke.getKeyStroke(VK_NUMPAD0,  platformCommandMask),                   ACTION_ZOOM_RESET.getName());
        inputMap.put(KeyStroke.getKeyStroke(VK_SUBTRACT, ALT_DOWN_MASK),                         ACTION_ROTATE_BRUSH_LEFT.getName());
        inputMap.put(KeyStroke.getKeyStroke(VK_MINUS,    ALT_DOWN_MASK),                         ACTION_ROTATE_BRUSH_LEFT.getName());
        inputMap.put(KeyStroke.getKeyStroke(VK_ADD,      ALT_DOWN_MASK),                         ACTION_ROTATE_BRUSH_RIGHT.getName());
        inputMap.put(KeyStroke.getKeyStroke(VK_EQUALS,   ALT_DOWN_MASK | SHIFT_DOWN_MASK),       ACTION_ROTATE_BRUSH_RIGHT.getName());
        inputMap.put(KeyStroke.getKeyStroke(VK_0,        ALT_DOWN_MASK),                         ACTION_ROTATE_BRUSH_RESET.getName());
        inputMap.put(KeyStroke.getKeyStroke(VK_NUMPAD0,  ALT_DOWN_MASK),                         ACTION_ROTATE_BRUSH_RESET.getName());
        inputMap.put(KeyStroke.getKeyStroke(VK_3,        ALT_DOWN_MASK),                         ACTION_ROTATE_BRUSH_RIGHT_30_DEGREES.getName());
        inputMap.put(KeyStroke.getKeyStroke(VK_NUMPAD3,  ALT_DOWN_MASK),                         ACTION_ROTATE_BRUSH_RIGHT_30_DEGREES.getName());
        inputMap.put(KeyStroke.getKeyStroke(VK_4,        ALT_DOWN_MASK),                         ACTION_ROTATE_BRUSH_RIGHT_45_DEGREES.getName());
        inputMap.put(KeyStroke.getKeyStroke(VK_NUMPAD4,  ALT_DOWN_MASK),                         ACTION_ROTATE_BRUSH_RIGHT_45_DEGREES.getName());
        inputMap.put(KeyStroke.getKeyStroke(VK_9,        ALT_DOWN_MASK),                         ACTION_ROTATE_BRUSH_RIGHT_90_DEGREES.getName());
        inputMap.put(KeyStroke.getKeyStroke(VK_NUMPAD9,  ALT_DOWN_MASK),                         ACTION_ROTATE_BRUSH_RIGHT_90_DEGREES.getName());

        programmaticChange = true;
        try {
            selectBrushButton(brush);
            levelSlider.setValue((int) (level * 100));
            brushRotationSlider.setValue(brushRotation);
        } finally {
            programmaticChange = false;
        }
    }
    
    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel();
        statusBar.setLayout(new FlowLayout(FlowLayout.LEADING));
        locationLabel = new JLabel(MessageFormat.format(strings.getString("location.0.1"), -99999, -99999));
        locationLabel.setBorder(new BevelBorder(BevelBorder.LOWERED));
        statusBar.add(locationLabel);
        heightLabel = new JLabel(MessageFormat.format(strings.getString("height.0.of.1"), 9999, 9999));
        heightLabel.setBorder(new BevelBorder(BevelBorder.LOWERED));
        statusBar.add(heightLabel);
        materialLabel = new JLabel(MessageFormat.format(strings.getString("material.0"), Material.MOSSY_COBBLESTONE.toString()));
        materialLabel.setBorder(new BevelBorder(BevelBorder.LOWERED));
        statusBar.add(materialLabel);
        waterLabel = new JLabel(MessageFormat.format(strings.getString("fluid.level.1.depth.2"), 9999, 9999));
        waterLabel.setBorder(new BevelBorder(BevelBorder.LOWERED));
        statusBar.add(waterLabel);
        biomeLabel = new JLabel("Auto biome: Mega Spruce Taiga Hills (ID 161)");
        biomeLabel.setBorder(new BevelBorder(BevelBorder.LOWERED));
        statusBar.add(biomeLabel);
        radiusLabel = new JLabel(MessageFormat.format(strings.getString("radius.0"), 999));
        radiusLabel.setToolTipText(strings.getString("scroll.the.mouse.wheel"));
        radiusLabel.setBorder(new BevelBorder(BevelBorder.LOWERED));
        statusBar.add(radiusLabel);
        zoomLabel = new JLabel(MessageFormat.format(strings.getString("zoom.0"), 3200));
        zoomLabel.setBorder(new BevelBorder(BevelBorder.LOWERED));
        statusBar.add(zoomLabel);
        final JProgressBar memoryBar = new JProgressBar();
        memoryBar.setBorder(new BevelBorder(BevelBorder.LOWERED));
        java.awt.Dimension preferredSize = memoryBar.getPreferredSize();
        preferredSize.width = 100;
        memoryBar.setPreferredSize(preferredSize);
        memoryBar.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("Forcing garbage collect");
                }
                System.gc();
            }
        });
        statusBar.add(memoryBar);
        new Timer(2500, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                long free = runtime.freeMemory();
                long total = runtime.totalMemory();
                long max = runtime.maxMemory();
                long inUse = total - free;
                memoryBar.setValue((int) (inUse * 100 / max));
                int inUseMB = (int) (inUse / ONE_MEGABYTE);
                int maxMB = (int) (max / ONE_MEGABYTE);
                memoryBar.setToolTipText(MessageFormat.format(strings.getString("memory.usage.0.mb.of.1.mb"), inUseMB, maxMB));
            }
            
            private final Runtime runtime = Runtime.getRuntime();
        }).start();
        return statusBar;
    }

    private JPanel createToolPanel() {
        JPanel toolPanel = new JPanel();
        toolPanel.setLayout(new GridLayout(0, 4));
        // TODO: use function keys as accelerators?
        toolPanel.add(createButtonForOperation(new SprayPaint(view, this, mapDragControl), "spraypaint", 'r'));
        toolPanel.add(createButtonForOperation(new Pencil(view, this, mapDragControl), "pencil", 'p'));
        toolPanel.add(createButtonForOperation(new Fill(view), "fill", 'l'));
        toolPanel.add(createButtonForOperation(new Text(view), "text", 'x'));

        toolPanel.add(createButtonForOperation(new Flood(view, false), "flood", 'f'));
        toolPanel.add(createButtonForOperation(new Flood(view, true), "flood_with_lava"));
        toolPanel.add(createButtonForOperation(new Sponge(view, this, mapDragControl), "sponge"));
        toolPanel.add(Box.createGlue());

        toolPanel.add(createButtonForOperation(new Height(view, this, mapDragControl), "height", 'h'));
        toolPanel.add(createButtonForOperation(new Flatten(view, this, mapDragControl), "flatten", 'a'));
        toolPanel.add(createButtonForOperation(new Smooth(view, this, mapDragControl), "smooth", 's'));
        toolPanel.add(createButtonForOperation(new RaiseMountain(view, this, mapDragControl), "mountain", 'm'));

//        toolPanel.add(createButtonForOperation(new Erode(view, this, mapDragControl), "erode", 'm'));
        toolPanel.add(createButtonForOperation(new SetSpawnPoint(view), "spawn"));
        JButton button = new JButton(loadIcon("globals"));
        button.setMargin(new Insets(2, 2, 2, 2));
        button.addActionListener(e -> showGlobalOperations());
        button.setToolTipText(strings.getString("global.operations.fill.or.clear.the.world.with.a.terrain.biome.or.layer"));
        toolPanel.add(button);
        toolPanel.add(createButtonForOperation(new RaiseRotatedPyramid(view), "pyramid"));
        toolPanel.add(createButtonForOperation(new RaisePyramid(view), "pyramid"));

        for (Operation operation: operations) {
            operation.setView(view);
            toolPanel.add(createButtonForOperation(operation, operation.getName().replaceAll("\\s", "").toLowerCase()));
        }

        return toolPanel;
    }
    
    private JPanel createLayerPanel() {
        final JPanel layerPanel = new JPanel();
        layerPanel.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(1, 1, 1, 1);

        JideLabel label = new JideLabel("Show");
        label.setOrientation(SwingConstants.VERTICAL);
        label.setClockwise(false);
        label.setMinimumSize(label.getPreferredSize());
        constraints.anchor = GridBagConstraints.SOUTH;
        layerPanel.add(label, constraints);
        label = new JideLabel("Solo");
        label.setOrientation(SwingConstants.VERTICAL);
        label.setClockwise(false);
        label.setMinimumSize(label.getPreferredSize());
        layerPanel.add(label, constraints);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.weightx = 1.0;
        layerPanel.add(new JLabel(), constraints);
        
        Configuration config = Configuration.getInstance();
        constraints.anchor = GridBagConstraints.FIRST_LINE_START;
        constraints.weightx = 0.0;
        for (Layer layer: layers) {
            LayoutUtils.addRowOfComponents(layerPanel, constraints, createLayerButton(layer, layer.getMnemonic(), true));
        }
        if (! config.isEasyMode()) {
            LayoutUtils.addRowOfComponents(layerPanel, constraints, createLayerButton(Populate.INSTANCE, 'p', true));
        }
        LayoutUtils.addRowOfComponents(layerPanel, constraints, createLayerButton(ReadOnly.INSTANCE, 'o', true));
        disableImportedWorldOperation();

        final JPopupMenu customLayerMenu = createCustomLayerMenu(null);

        final JButton addLayerButton = new JButton(loadIcon("plus"));
        addLayerButton.setToolTipText(strings.getString("add.a.custom.layer"));
        addLayerButton.setMargin(new Insets(2, 2, 2, 2));
        addLayerButton.addActionListener(e -> customLayerMenu.show(layerPanel, addLayerButton.getX() + addLayerButton.getWidth(), addLayerButton.getY()));
        JPanel spacer = new JPanel();
        constraints.gridwidth = 1;
        constraints.weightx = 0.0;
        layerPanel.add(spacer, constraints);
        spacer = new JPanel();
        layerPanel.add(spacer, constraints);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.weightx = 1.0;
        layerPanel.add(addLayerButton, constraints);

        return layerPanel;
    }

    private JPanel createBiomesPanel() {
        final JPanel biomesPanel = new JPanel();
        biomesPanel.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(1, 1, 1, 1);

        Configuration config = Configuration.getInstance();
        constraints.anchor = GridBagConstraints.FIRST_LINE_START;
        constraints.weightx = 0.0;
        biomesCheckBox = new JCheckBox("Show:");
        biomesCheckBox.setHorizontalTextPosition(SwingConstants.LEADING);
        biomesCheckBox.setToolTipText("Uncheck to hide biomes from view (it will still be exported)");
        biomesCheckBox.addActionListener(e -> {
            if (biomesCheckBox.isSelected()) {
                hiddenLayers.remove(Biome.INSTANCE);
            } else {
                hiddenLayers.add(Biome.INSTANCE);
            }
            updateLayerVisibility();
        });
        if (! config.isEasyMode()) {
            constraints.gridwidth = 1;
            constraints.weightx = 0.0;
            biomesPanel.add(biomesCheckBox, constraints);
        }
        
        biomesSoloCheckBox = new JCheckBox("Solo:");
        biomesSoloCheckBox.setHorizontalTextPosition(SwingConstants.LEADING);
        biomesSoloCheckBox.setToolTipText("<html>Check to show <em>only</em> the biomes (the other layers are still exported)</html>");
        biomesSoloCheckBox.addActionListener(e -> {
            if (biomesSoloCheckBox.isSelected()) {
                layerSoloCheckBoxes.values().stream().filter(otherSoloCheckBox -> otherSoloCheckBox != biomesSoloCheckBox).forEach(otherSoloCheckBox -> otherSoloCheckBox.setSelected(false));
                soloLayer = Biome.INSTANCE;
            } else {
                soloLayer = null;
            }
            updateLayerVisibility();
        });
        layerSoloCheckBoxes.put(Biome.INSTANCE, biomesSoloCheckBox);
        if (! config.isEasyMode()) {
            constraints.gridwidth = GridBagConstraints.REMAINDER;
            biomesPanel.add(biomesSoloCheckBox, constraints);
        }

        biomesPanel.add(new BiomesPanel(defaultColourScheme, customBiomeManager, biomeId -> {
            paintUpdater = () -> {
                paint = PaintFactory.createDiscreteLayerPaint(Biome.INSTANCE, biomeId);
                paintChanged();
            };
            paintUpdater.updatePaint();
        }, paintButtonGroup), constraints);

        return biomesPanel;
    }

    private JPanel createAnnotationsPanel() {
        final JPanel layerPanel = new JPanel();
        layerPanel.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(1, 1, 1, 1);

        Configuration config = Configuration.getInstance();
        constraints.anchor = GridBagConstraints.FIRST_LINE_START;
        constraints.weightx = 0.0;
        annotationsCheckBox = new JCheckBox("Show:");
        annotationsCheckBox.setHorizontalTextPosition(SwingConstants.LEADING);
        annotationsCheckBox.setSelected(true);
        annotationsCheckBox.setToolTipText("Uncheck to hide annotations from view");
        annotationsCheckBox.addActionListener(e -> {
            if (annotationsCheckBox.isSelected()) {
                hiddenLayers.remove(Annotations.INSTANCE);
            } else {
                hiddenLayers.add(Annotations.INSTANCE);
            }
            updateLayerVisibility();
        });
        if (! config.isEasyMode()) {
            constraints.gridwidth = 1;
            constraints.weightx = 0.0;
            layerPanel.add(annotationsCheckBox, constraints);
        }
        
        annotationsSoloCheckBox = new JCheckBox("Solo:");
        annotationsSoloCheckBox.setHorizontalTextPosition(SwingConstants.LEADING);
        annotationsSoloCheckBox.setToolTipText("<html>Check to show <em>only</em> the annotations (the other layers are still exported)</html>");
        annotationsSoloCheckBox.addActionListener(e -> {
            if (annotationsSoloCheckBox.isSelected()) {
                layerSoloCheckBoxes.values().stream().filter(otherSoloCheckBox -> otherSoloCheckBox != annotationsSoloCheckBox).forEach(otherSoloCheckBox -> otherSoloCheckBox.setSelected(false));
                soloLayer = Annotations.INSTANCE;
            } else {
                soloLayer = null;
            }
            updateLayerVisibility();
        });
        layerSoloCheckBoxes.put(Annotations.INSTANCE, annotationsSoloCheckBox);
        if (! config.isEasyMode()) {
            constraints.gridwidth = GridBagConstraints.REMAINDER;
            layerPanel.add(annotationsSoloCheckBox, constraints);
        }

        JPanel colourGrid = new JPanel(new GridLayout(0, 4));
        for (int i = 1; i < 16; i++) {
            final int selectedColour = i;
            JToggleButton button = new JToggleButton(createColourIcon(defaultColourScheme.getColour(BLK_WOOL, i - ((i < 8) ? 1 : 0))));
            button.setMargin(new Insets(2, 2, 2, 2));
            if (i == 1) {
                button.setSelected(true);
            }
            paintButtonGroup.add(button);
            button.addActionListener(e -> {
                paintUpdater = () -> {
                    paint = PaintFactory.createDiscreteLayerPaint(Annotations.INSTANCE, selectedColour);
                    paintChanged();
                };
                paintUpdater.updatePaint();
            });
            colourGrid.add(button);
        }
        layerPanel.add(colourGrid, constraints);

        return layerPanel;
    }
    
    private JPopupMenu createCustomLayerMenu(final String paletteName) {
        JPopupMenu customLayerMenu = new JPopupMenu();
        JMenuItem menuItem = new JMenuItem(strings.getString("add.a.custom.object.layer") + "...");
        menuItem.addActionListener(e -> {
            EditLayerDialog<Bo2Layer> dialog = new EditLayerDialog(App.this, Bo2Layer.class);
            dialog.setVisible(true);
            if (! dialog.isCancelled()) {
                Bo2Layer layer = dialog.getLayer();
                if (paletteName != null) {
                    layer.setPalette(paletteName);
                }
                registerCustomLayer(layer, true);
            }
        });
        customLayerMenu.add(menuItem);
        
        menuItem = new JMenuItem(strings.getString("add.a.custom.ground.cover.layer") + "...");
        menuItem.addActionListener(e -> {
            EditLayerDialog<GroundCoverLayer> dialog = new EditLayerDialog(App.this, GroundCoverLayer.class);
            dialog.setVisible(true);
            if (! dialog.isCancelled()) {
                GroundCoverLayer layer = dialog.getLayer();
                if (paletteName != null) {
                    layer.setPalette(paletteName);
                }
                registerCustomLayer(layer, true);
            }
        });
        customLayerMenu.add(menuItem);
        
        menuItem = new JMenuItem(strings.getString("add.a.custom.underground.pockets.layer") + "...");
        menuItem.addActionListener(e -> {
            UndergroundPocketsDialog dialog = new UndergroundPocketsDialog(App.this, MixedMaterial.create(Material.IRON_BLOCK), selectedColourScheme, world.getMaxHeight(), world.isExtendedBlockIds());
            dialog.setVisible(true);
            if (! dialog.isCancelled()) {
                UndergroundPocketsLayer layer = dialog.getSelectedLayer();
                if (paletteName != null) {
                    layer.setPalette(paletteName);
                }
                registerCustomLayer(layer, true);
            }
        });
        customLayerMenu.add(menuItem);
        
        menuItem = new JMenuItem("Add a custom cave/tunnel layer...");
        menuItem.addActionListener(e -> {
            final TunnelLayer layer = new TunnelLayer("Tunnels", 0x000000);
            final int baseHeight, waterLevel;
            final TileFactory tileFactory = dimension.getTileFactory();
            if (tileFactory instanceof HeightMapTileFactory) {
                baseHeight = (int) ((HeightMapTileFactory) tileFactory).getBaseHeight();
                waterLevel = ((HeightMapTileFactory) tileFactory).getWaterHeight();
                layer.setFloodWithLava(((HeightMapTileFactory) tileFactory).isFloodWithLava());
            } else {
                baseHeight = 58;
                waterLevel = 62;
            }
            TunnelLayerDialog dialog = new TunnelLayerDialog(App.this, layer, world.isExtendedBlockIds(), selectedColourScheme, dimension.getMaxHeight(), baseHeight, waterLevel);
            dialog.setVisible(true);
            if (! dialog.isCancelled()) {
                if (paletteName != null) {
                    layer.setPalette(paletteName);
                }
                registerCustomLayer(layer, true);
            }
        });
        customLayerMenu.add(menuItem);
        
        menuItem = new JMenuItem("Add a custom plants layer...");
        menuItem.addActionListener(e -> {
            EditLayerDialog<PlantLayer> dialog = new EditLayerDialog<>(App.this, PlantLayer.class);
            dialog.setVisible(true);
            if (! dialog.isCancelled()) {
                PlantLayer layer = dialog.getLayer();
                if (paletteName != null) {
                    layer.setPalette(paletteName);
                }
                registerCustomLayer(layer, true);
            }
        });
        customLayerMenu.add(menuItem);
        
        menuItem = new JMenuItem("Add a combined layer...");
        menuItem.addActionListener(e -> {
            EditLayerDialog<CombinedLayer> dialog = new EditLayerDialog<>(App.this, CombinedLayer.class);
            dialog.setVisible(true);
            if (! dialog.isCancelled()) {
                // TODO: get saved layer
                CombinedLayer layer = dialog.getLayer();
                if (paletteName != null) {
                    layer.setPalette(paletteName);
                }
                registerCustomLayer(layer, true);
            }
        });
        customLayerMenu.add(menuItem);
        
        menuItem = new JMenuItem("Import custom layer(s) from file...");
        menuItem.addActionListener(e -> importLayers(paletteName));
        customLayerMenu.add(menuItem);
        
        return customLayerMenu;
    }

    private JPanel createTerrainPanel() {
        JPanel terrainPanel = new JPanel();
        terrainPanel.setLayout(new GridLayout(0, 4));
        terrainPanel.add(createTerrainButton(Terrain.GRASS));
        terrainPanel.add(createTerrainButton(Terrain.PERMADIRT));
        terrainPanel.add(createTerrainButton(Terrain.SAND));
        terrainPanel.add(createTerrainButton(Terrain.DESERT));
        
        terrainPanel.add(createTerrainButton(Terrain.BARE_GRASS));
        terrainPanel.add(createTerrainButton(Terrain.STONE));
        terrainPanel.add(createTerrainButton(Terrain.ROCK));
        terrainPanel.add(createTerrainButton(Terrain.SANDSTONE));

        terrainPanel.add(createTerrainButton(Terrain.STONE_MIX));
        terrainPanel.add(createTerrainButton(Terrain.GRANITE));
        terrainPanel.add(createTerrainButton(Terrain.DIORITE));
        terrainPanel.add(createTerrainButton(Terrain.ANDESITE));

        terrainPanel.add(createTerrainButton(Terrain.PODZOL));
        terrainPanel.add(createTerrainButton(Terrain.COBBLESTONE));
        terrainPanel.add(createTerrainButton(Terrain.MOSSY_COBBLESTONE));
        terrainPanel.add(createTerrainButton(Terrain.GRAVEL));
        
        terrainPanel.add(createTerrainButton(Terrain.OBSIDIAN));
        terrainPanel.add(createTerrainButton(Terrain.WATER));
        terrainPanel.add(createTerrainButton(Terrain.LAVA));
        terrainPanel.add(createTerrainButton(Terrain.DEEP_SNOW));
        
        terrainPanel.add(createTerrainButton(Terrain.NETHERRACK));
        terrainPanel.add(createTerrainButton(Terrain.SOUL_SAND));
        terrainPanel.add(createTerrainButton(Terrain.NETHERLIKE));
        terrainPanel.add(createTerrainButton(Terrain.MYCELIUM));

        terrainPanel.add(createTerrainButton(Terrain.END_STONE));
        terrainPanel.add(createTerrainButton(Terrain.BEDROCK));
        terrainPanel.add(createTerrainButton(Terrain.CLAY));
        terrainPanel.add(createTerrainButton(Terrain.BEACHES));
        
        terrainPanel.add(createTerrainButton(Terrain.RED_SAND));
        terrainPanel.add(createTerrainButton(Terrain.RED_SANDSTONE));
        terrainPanel.add(createTerrainButton(Terrain.RED_DESERT));
        terrainPanel.add(createTerrainButton(Terrain.MESA));

        terrainPanel.add(createTerrainButton(Terrain.WHITE_STAINED_CLAY));
        terrainPanel.add(createTerrainButton(Terrain.ORANGE_STAINED_CLAY));
        terrainPanel.add(createTerrainButton(Terrain.MAGENTA_STAINED_CLAY));
        terrainPanel.add(createTerrainButton(Terrain.LIGHT_BLUE_STAINED_CLAY));

        terrainPanel.add(createTerrainButton(Terrain.YELLOW_STAINED_CLAY));
        terrainPanel.add(createTerrainButton(Terrain.LIME_STAINED_CLAY));
        terrainPanel.add(createTerrainButton(Terrain.PINK_STAINED_CLAY));
        terrainPanel.add(createTerrainButton(Terrain.GREY_STAINED_CLAY));

        terrainPanel.add(createTerrainButton(Terrain.LIGHT_GREY_STAINED_CLAY));
        terrainPanel.add(createTerrainButton(Terrain.CYAN_STAINED_CLAY));
        terrainPanel.add(createTerrainButton(Terrain.PURPLE_STAINED_CLAY));
        terrainPanel.add(createTerrainButton(Terrain.BLUE_STAINED_CLAY));

        terrainPanel.add(createTerrainButton(Terrain.BROWN_STAINED_CLAY));
        terrainPanel.add(createTerrainButton(Terrain.GREEN_STAINED_CLAY));
        terrainPanel.add(createTerrainButton(Terrain.RED_STAINED_CLAY));
        terrainPanel.add(createTerrainButton(Terrain.BLACK_STAINED_CLAY));

        terrainPanel.add(createTerrainButton(Terrain.HARDENED_CLAY));

        return terrainPanel;
    }
    
    private JPanel createCustomTerrainPanel() {
        JPanel customTerrainPanel = new JPanel();
        customTerrainPanel.setLayout(new GridLayout(0, 4));
    
        for (int i = 0; i < Terrain.CUSTOM_TERRAIN_COUNT; i++) {
            customMaterialButtons[i] = createTerrainButton(Terrain.getCustomTerrain(i));
            customMaterialButtons[i].setIcon(ICON_UNKNOWN_PATTERN);
            customMaterialButtons[i].setToolTipText(strings.getString("not.set.click.to.set"));
            addMaterialSelectionTo(customMaterialButtons[i], i);
            customTerrainPanel.add(customMaterialButtons[i]);
        }

        return customTerrainPanel;
    }
    
    private JPanel createBrushPanel() {
        JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new GridBagLayout());
        JPanel brushPanel = new JPanel(new GridLayout(0, 3));
        brushPanel.add(createBrushButton(SymmetricBrush.SPIKE_CIRCLE));
        brushPanel.add(createBrushButton(SymmetricBrush.SPIKE_SQUARE));
        brushPanel.add(createBrushButton(new BitmapBrush(App.class.getResourceAsStream("resources/brush_noise.png"), strings.getString("noise"))));

        brushPanel.add(createBrushButton(SymmetricBrush.LINEAR_CIRCLE));
        brushPanel.add(createBrushButton(SymmetricBrush.LINEAR_SQUARE));
        brushPanel.add(createBrushButton(new BitmapBrush(App.class.getResourceAsStream("resources/brush_cracked_earth.png"), strings.getString("cracks"))));
        
        brushPanel.add(createBrushButton(SymmetricBrush.COSINE_CIRCLE));
        brushPanel.add(createBrushButton(SymmetricBrush.COSINE_SQUARE));
        brushPanel.add(createBrushButton(SymmetricBrush.CONSTANT_CIRCLE));

        brushPanel.add(createBrushButton(SymmetricBrush.PLATEAU_CIRCLE));
        brushPanel.add(createBrushButton(SymmetricBrush.PLATEAU_SQUARE));
        brushPanel.add(createBrushButton(SymmetricBrush.CONSTANT_SQUARE));
        
        brushPanel.add(createBrushButton(SymmetricBrush.DOME_CIRCLE));
        brushPanel.add(createBrushButton(SymmetricBrush.DOME_SQUARE));
        
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.FIRST_LINE_START;
        constraints.weightx = 1.0;
        constraints.insets = new Insets(1, 1, 1, 1);
        optionsPanel.add(brushPanel, constraints);
        
        return optionsPanel;
    }

    private JPanel createCustomBrushPanel(String title, List<Brush> customBrushes) {
        JPanel customBrushesPanel = new JPanel();
        customBrushesPanel.setLayout(new GridBagLayout());
        JPanel customBrushPanel = new JPanel(new GridLayout(0, 3));
        for (Brush customBrush: customBrushes) {
            customBrushPanel.add(createBrushButton(customBrush));
        }
        
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.FIRST_LINE_START;
        constraints.weightx = 1.0;
        constraints.insets = new Insets(1, 1, 1, 1);
        customBrushesPanel.add(customBrushPanel, constraints);
        
        return customBrushesPanel;
    }
    
    private JPanel createBrushSettingsPanel() {
        JPanel brushSettingsPanel = new JPanel();
        brushSettingsPanel.setLayout(new GridBagLayout());
        
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.FIRST_LINE_START;
        constraints.weightx = 1.0;
        constraints.insets = new Insets(1, 1, 1, 1);

        levelSlider = new JSlider(2, 100);
        levelSlider.setMajorTickSpacing(49);
        levelSlider.setMinorTickSpacing(7);
        levelSlider.setPaintTicks(true);
        levelSlider.setSnapToTicks(true);
        levelSlider.setPaintLabels(false);
        levelSlider.addChangeListener(e -> {
            int value = levelSlider.getValue();
            levelLabel.setText("Intensity: " + ((value < 52) ? (value - 1) : value) + " %");
            if ((! programmaticChange) && (! levelSlider.getValueIsAdjusting())) {
                float newLevel = value / 100.0f;
                if (activeOperation instanceof PaintOperation) {
                    level = newLevel;
                    ((MouseOrTabletOperation) activeOperation).setLevel(level);
                } else {
                    toolLevel = newLevel;
                    if (activeOperation instanceof MouseOrTabletOperation) {
                        ((MouseOrTabletOperation) activeOperation).setLevel(toolLevel);
                    }
                }
                brush.setLevel(newLevel);
            }
        });
        
        brushRotationSlider = new JSlider(-180, 180);
        brushRotationSlider.setMajorTickSpacing(45);
        brushRotationSlider.setMinorTickSpacing(15);
        brushRotationSlider.setPaintTicks(true);
        brushRotationSlider.setSnapToTicks(true);
        brushRotationSlider.setPaintLabels(false);
        brushRotationSlider.addChangeListener(e -> {
            int value = brushRotationSlider.getValue();
            brushRotationLabel.setText("Rotation: " + ((value < 0) ? (((value - 7) / 15) * 15) : (((value + 7) / 15) * 15)) + "°");
            if ((! programmaticChange) && (! brushRotationSlider.getValueIsAdjusting())) {
                if (activeOperation instanceof PaintOperation) {
                    brushRotation = value;
                } else {
                    toolBrushRotation = value;
                }
                updateBrushRotation();
            }
        });
        
        constraints.insets = new Insets(3, 1, 1, 1);
        brushRotationLabel = new JLabel("Rotation: 0°");
        brushSettingsPanel.add(brushRotationLabel, constraints);
        
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(1, 1, 1, 1);
        // The preferred width of the slider is way too much. Make it smaller, and
        // then fill the available width created by the buttons
        java.awt.Dimension preferredSize = brushRotationSlider.getPreferredSize();
        preferredSize.width = 1;
        brushRotationSlider.setPreferredSize(preferredSize);
        brushSettingsPanel.add(brushRotationSlider, constraints);

        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(3, 1, 1, 1);
        levelLabel = new JLabel("Intensity: 50 %");
        brushSettingsPanel.add(levelLabel, constraints);
        
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(1, 1, 1, 1);
        // The preferred width of the slider is way too much. Make it smaller, and
        // then fill the available width created by the buttons
        preferredSize = levelSlider.getPreferredSize();
        preferredSize.width = 1;
        levelSlider.setPreferredSize(preferredSize);
        brushSettingsPanel.add(levelSlider, constraints);
        
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(3, 1, 1, 1);
        brushSettingsPanel.add(new JLabel("Options"), constraints);
        
        constraints.insets = new Insets(1, 1, 1, 1);
        brushSettingsPanel.add(brushOptions, constraints);
        
        return brushSettingsPanel;
    }

    private void updateBrushRotation() {
        int desiredBrushRotation = (activeOperation instanceof PaintOperation) ? brushRotation : toolBrushRotation;
        if (desiredBrushRotation != previousBrushRotation) {
            long start = System.currentTimeMillis();
            if (desiredBrushRotation == 0) {
                for (Map.Entry<Brush, JToggleButton> entry: brushButtons.entrySet()) {
                    Brush brush = entry.getKey();
                    JToggleButton button = entry.getValue();
                    button.setIcon(createBrushIcon(brush, 0));
                    if (button.isSelected() && (activeOperation instanceof RadiusOperation)) {
                        ((RadiusOperation) activeOperation).setBrush(brush);
                    }
                }
            } else {
                for (Map.Entry<Brush, JToggleButton> entry: brushButtons.entrySet()) {
                    Brush brush = entry.getKey();
                    JToggleButton button = entry.getValue();
                    button.setIcon(createBrushIcon(brush, desiredBrushRotation));
                    if (button.isSelected() && (activeOperation instanceof RadiusOperation)) {
                        Brush rotatedBrush = RotatedBrush.rotate(brush, desiredBrushRotation);
                        ((RadiusOperation) activeOperation).setBrush(rotatedBrush);
                    }
                }
            }
            view.setBrushRotation(desiredBrushRotation);
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Updating brush rotation took " + (System.currentTimeMillis() - start) + " ms");
            }
            previousBrushRotation = desiredBrushRotation;
        }
    }

    private void registerCustomLayer(final CustomLayer layer, boolean activate) {
        // Add to palette, creating it if necessary
        Palette palette = paletteManager.register(layer);

        // Show the palette if it is not showing yet
        if (palette != null) {
            dockingManager.addFrame(palette.getDockableFrame());
            dockingManager.dockFrame(palette.getDockableFrame().getKey(), DockContext.DOCK_SIDE_WEST, 3);
            if (activate) {
                dockingManager.activateFrame(palette.getDockableFrame().getKey());
            }
        } else {
            validate();
        }

        if (activate) {
            paletteManager.activate(layer);
        }
    }

    private void unregisterCustomLayer(final CustomLayer layer) {
        // Remove from palette
        Palette palette = paletteManager.unregister(layer);

        // Remove tracked GUI components
        layerSoloCheckBoxes.remove(layer);

        // If the palette is now empty, remove it too
        if (palette.isEmpty()) {
            paletteManager.delete(palette);
            dockingManager.removeFrame(palette.getDockableFrame().getKey());
        }
    }

    // PaletteManager.ButtonProvider
    
    @Override
    public List<Component> createCustomLayerButton(final CustomLayer layer) {
        final List<Component> buttonComponents = createLayerButton(layer, '\0', true);
        final JToggleButton button = (JToggleButton) buttonComponents.get(2);
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopup(e);
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopup(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopup(e);
                }
            }

            private void showPopup(MouseEvent e) {
                JPopupMenu popup = new JPopupMenu();
                JMenuItem menuItem = new JMenuItem(strings.getString("edit") + "...");
                menuItem.addActionListener(e1 -> edit());
                popup.add(menuItem);
                menuItem = new JMenuItem(strings.getString("remove") + "...");
                menuItem.addActionListener(e1 -> remove());
                popup.add(menuItem);
                menuItem = new JMenuItem("Export to file...");
                menuItem.addActionListener(e1 -> exportLayer(layer));
                popup.add(menuItem);

                JMenu paletteMenu = new JMenu("Move to palette");

                for (final Palette palette : paletteManager.getPalettes()) {
                    menuItem = new JMenuItem(palette.getName());
                    menuItem.addActionListener(e1 -> moveLayerToPalette(layer, palette));
                    if (palette.contains(layer)) {
                        menuItem.setEnabled(false);
                    }
                    paletteMenu.add(menuItem);
                }

                menuItem = new JMenuItem("New palette...");
                menuItem.addActionListener(e1 -> createNewLayerPalette(layer));
                paletteMenu.add(menuItem);

                popup.add(paletteMenu);

                List<Action> actions = layer.getActions();
                if (actions != null) {
                    for (Action action : actions) {
                        action.putValue(CustomLayer.KEY_DIMENSION, dimension);
                        popup.add(new JMenuItem(action));
                    }
                }

                popup.show(button, e.getX(), e.getY());
            }

            private void edit() {
                int previousColour = layer.getColour();
                WorldPainterDialog dialog;
                if ((layer instanceof Bo2Layer) || (layer instanceof GroundCoverLayer) || (layer instanceof CombinedLayer) || (layer instanceof PlantLayer)) {
                    dialog = new EditLayerDialog<Layer>(App.this, layer);
                } else if (layer instanceof UndergroundPocketsLayer) {
                    dialog = new UndergroundPocketsDialog(App.this, (UndergroundPocketsLayer) layer, selectedColourScheme, dimension.getMaxHeight(), world.isExtendedBlockIds());
                } else if (layer instanceof TunnelLayer) {
                    final int baseHeight, waterLevel;
                    final TileFactory tileFactory = dimension.getTileFactory();
                    if (tileFactory instanceof HeightMapTileFactory) {
                        baseHeight = (int) ((HeightMapTileFactory) tileFactory).getBaseHeight();
                        waterLevel = ((HeightMapTileFactory) tileFactory).getWaterHeight();
                    } else {
                        baseHeight = 58;
                        waterLevel = 62;
                    }
                    dialog = new TunnelLayerDialog(App.this, (TunnelLayer) layer, world.isExtendedBlockIds(), selectedColourScheme, dimension.getMaxHeight(), baseHeight, waterLevel);
                } else {
                    throw new RuntimeException("Don't know how to edit " + layer.getName());
                }
                dialog.setVisible(true);
                if (!dialog.isCancelled()) {
                    button.setText(layer.getName());
                    button.setToolTipText(layer.getName() + ": " + layer.getDescription());
                    int newColour = layer.getColour();
                    boolean viewRefreshed = false;
                    if (newColour != previousColour) {
                        button.setIcon(new ImageIcon(layer.getIcon()));
                        view.refreshTilesForLayer(layer, false);
                        viewRefreshed = true;
                    }
                    dimension.setDirty(true);
                    if (layer instanceof CombinedLayer) {
                        updateHiddenLayers();
                    }
                    if ((layer instanceof TunnelLayer) && (!viewRefreshed)) {
                        view.refreshTilesForLayer(layer, false);
                        viewRefreshed = true;
                    }
                }
            }

            private void remove() {
                if (JOptionPane.showConfirmDialog(App.this, MessageFormat.format(strings.getString("are.you.sure.you.want.to.remove.the.0.layer"), layer.getName()), MessageFormat.format(strings.getString("confirm.0.removal"), layer.getName()), JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    if ((activeOperation instanceof PaintOperation) && (paint instanceof LayerPaint) && (((LayerPaint) paint).getLayer() == layer)) {
                        deselectPaint();
                    }
                    dimension.setEventsInhibited(true);
                    try {
                        dimension.clearLayerData(layer);
                    } finally {
                        dimension.setEventsInhibited(false);
                    }
                    unregisterCustomLayer(layer);

                    boolean visibleLayersChanged = false;
                    if (hiddenLayers.contains(layer)) {
                        hiddenLayers.remove(layer);
                        visibleLayersChanged = true;
                    }
                    if (layer.equals(soloLayer)) {
                        soloLayer = null;
                        visibleLayersChanged = true;
                    }
                    if (layer instanceof LayerContainer) {
                        boolean layersUnhidden = false;
                        for (Layer subLayer: ((LayerContainer) layer).getLayers()) {
                            if ((subLayer instanceof CustomLayer) && ((CustomLayer) subLayer).isHide()) {
                                ((CustomLayer) subLayer).setHide(false);
                                layersUnhidden = true;
                            }
                        }
                        if (layersUnhidden) {
                            updateHiddenLayers();
                            visibleLayersChanged = false;
                        }
                    }
                    if (visibleLayersChanged) {
                        updateLayerVisibility();
                    }

                    App.this.validate(); // Doesn't happen automatically for some reason; Swing bug?
                }
            }
        });
        return buttonComponents;
    }

    @Override
    public List<Component> createPopupMenuButton(String paletteName) {
        final JPopupMenu customLayerMenu = createCustomLayerMenu(paletteName);
        
        final JButton addLayerButton = new JButton(loadIcon("plus"));
        final List<Component> addLayerButtonPanel = new ArrayList<>(3);
        addLayerButton.setToolTipText(strings.getString("add.a.custom.layer"));
        addLayerButton.setMargin(new Insets(2, 2, 2, 2));
        addLayerButton.addActionListener(e -> customLayerMenu.show(addLayerButton, addLayerButton.getWidth(), 0));
        JPanel spacer = new JPanel();
        addLayerButtonPanel.add(spacer);
        spacer = new JPanel();
        addLayerButtonPanel.add(spacer);
        addLayerButtonPanel.add(addLayerButton);

        return addLayerButtonPanel;
    }

    private void updateHiddenLayers() {
        // Hide newly hidden layers
        paletteManager.getLayers().stream().filter(CustomLayer::isHide).forEach(layer -> {
            if ((activeOperation instanceof PaintOperation) && (paint instanceof LayerPaint) && (((LayerPaint) paint).getLayer().equals(layer))) {
                deselectPaint();
            }
            unregisterCustomLayer(layer);
            hiddenLayers.remove(layer);
            if (layer.equals(soloLayer)) {
                soloLayer = null;
            }
            layersWithNoButton.add(layer);
        });
        // Show newly unhidden layers
        for (Iterator<CustomLayer> i = layersWithNoButton.iterator(); i.hasNext(); ) {
            CustomLayer layer = i.next();
            if (! layer.isHide()) {
                i.remove();
                registerCustomLayer(layer, false);
            }
        }
        updateLayerVisibility();
    }
    
    private void createNewLayerPalette(CustomLayer layer) {
        String name;
        if ((name = JOptionPane.showInputDialog(this, "Enter a unique name for the new palette:", "New Palette", JOptionPane.QUESTION_MESSAGE)) != null) {
            if (paletteManager.getPalette(name) != null) {
                JOptionPane.showMessageDialog(this, "There is already a palette with that name!", "Duplicate Name", JOptionPane.ERROR_MESSAGE);
                return;
            }
            Palette destPalette = paletteManager.create(name);
            dockingManager.addFrame(destPalette.getDockableFrame());
            dockingManager.dockFrame(destPalette.getDockableFrame().getKey(), DockContext.DOCK_SIDE_WEST, 3);
            moveLayerToPalette(layer, destPalette);
            dockingManager.activateFrame(destPalette.getDockableFrame().getKey());
        }
    }

    private void moveLayerToPalette(CustomLayer layer, Palette destPalette) {
        Palette srcPalette = paletteManager.move(layer, destPalette);
        if (srcPalette.isEmpty()) {
            dockingManager.removeFrame(srcPalette.getDockableFrame().getKey());
            paletteManager.delete(srcPalette);
        }
        validate();
    }

    private JMenuBar createMenuBar() {
        JMenuItem menuItem = new JMenuItem(ACTION_NEW_WORLD);
        menuItem.setMnemonic('n');
        JMenu menu = new JMenu(strings.getString("file"));
        menu.setMnemonic('i');
        menu.add(menuItem);

        menuItem = new JMenuItem(ACTION_OPEN_WORLD);
        menuItem.setMnemonic('o');
        menu.add(menuItem);

        final Configuration config = Configuration.getInstance();
        if (! config.isEasyMode()) {
            menuItem = new JMenuItem(ACTION_IMPORT_MAP);
            menuItem.setMnemonic('m');
            menuItem.setText("Minecraft map...");
            JMenu subMenu = new JMenu(strings.getString("import"));
            subMenu.setMnemonic('i');
            subMenu.add(menuItem);

            menuItem = new JMenuItem(strings.getString("height.map") + "...");
            menuItem.addActionListener(event -> importHeightMap());
            menuItem.setMnemonic('h');
            menuItem.setAccelerator(KeyStroke.getKeyStroke(VK_M, PLATFORM_COMMAND_MASK));
            subMenu.add(menuItem);

            menuItem = new JMenuItem(ACTION_IMPORT_LAYER);
            menuItem.setMnemonic('l');
            menuItem.setText("Custom layer(s)...");
            subMenu.add(menuItem);

//            menuItem = new JMenuItem("Existing Minecraft map into current world...");
//            menuItem.addActionListener(new ActionListener() {
//                @Override
//                public void actionPerformed(ActionEvent e) {
//                    importMapIntoWorld();
//                }
//            });
//            subMenu.add(menuItem);

            menu.add(subMenu);
        } else {
            menuItem = new JMenuItem(ACTION_IMPORT_MAP);
            menuItem.setMnemonic('m');
            menu.add(menuItem);
        }

        menu.addSeparator();

        menuItem = new JMenuItem(ACTION_SAVE_WORLD);
        menuItem.setMnemonic('s');
        menu.add(menuItem);

        menuItem = new JMenuItem(ACTION_SAVE_WORLD_AS);
        menuItem.setMnemonic('a');
        menu.add(menuItem);

        menuItem = new JMenuItem(ACTION_EXPORT_WORLD);
        menuItem.setMnemonic('m');
        if (config.isEasyMode()) {
            menu.add(menuItem);
        } else {
            JMenu exportMenu = new JMenu(strings.getString("export"));
            exportMenu.setMnemonic('e');
            exportMenu.add(menuItem);

            menuItem = new JMenuItem(strings.getString("export.as.image.file") + "...");
            menuItem.addActionListener(event -> exportImage());
            menuItem.setMnemonic('i');
            exportMenu.add(menuItem);

            menuItem = new JMenuItem(strings.getString("export.as.height.map") + "...");
            menuItem.addActionListener(event -> exportHeightMap());
            menuItem.setMnemonic('h');
            exportMenu.add(menuItem);

            menu.add(exportMenu);
        }

        menuItem = new JMenuItem(ACTION_MERGE_WORLD);
        menuItem.setMnemonic('m');
        menu.add(menuItem);

        if (! hideExit) {
            menu.addSeparator();

            menuItem = new JMenuItem(ACTION_EXIT);
            menuItem.setMnemonic('x');
            menu.add(menuItem);
        }
        
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(menu);

        menuItem = new JMenuItem(ACTION_UNDO);
        menuItem.setMnemonic('u');
        menu = new JMenu(strings.getString("edit"));
        menu.setMnemonic('e');
        menu.add(menuItem);

        menuItem = new JMenuItem(ACTION_REDO);
        menuItem.setMnemonic('r');
        menu.add(menuItem);

        menu.addSeparator();

        extendedBlockIdsMenuItem = new JCheckBoxMenuItem("Extended block IDs");
        extendedBlockIdsMenuItem.setToolTipText("Allow block IDs from 0 to 4095 (inclusive) as used by some mods");
        extendedBlockIdsMenuItem.setMnemonic('e');
        extendedBlockIdsMenuItem.addActionListener(e -> {
            if (world != null) {
                world.setExtendedBlockIds(extendedBlockIdsMenuItem.isSelected());
            }
        });
        menu.add(extendedBlockIdsMenuItem);
        
        menuItem = new JMenuItem(ACTION_DIMENSION_PROPERTIES);
        menuItem.setMnemonic('p');
        menu.add(menuItem);

        if (! config.isEasyMode()) {
            menuItem = new JMenuItem(ACTION_CHANGE_HEIGHT);
            menuItem.setMnemonic('h');
            menu.add(menuItem);

            menuItem = new JMenuItem(ACTION_ROTATE_WORLD);
            menuItem.setMnemonic('o');
            menu.add(menuItem);

            menuItem = new JMenuItem(ACTION_SHIFT_WORLD);
            menuItem.setMnemonic('s');
            menu.add(menuItem);

            menu.addSeparator();
        }

        menuItem = new JMenuItem(strings.getString("global.operations") + "...");
        menuItem.addActionListener(event -> showGlobalOperations());
        menuItem.setMnemonic('g');
        menuItem.setAccelerator(KeyStroke.getKeyStroke(VK_G, PLATFORM_COMMAND_MASK));
        menu.add(menuItem);
        
        menu.addSeparator();

        menuItem = new JMenuItem(ACTION_EDIT_TILES);
        menuItem.setMnemonic('t');
        menu.add(menuItem);

        addSurfaceCeilingMenuItem = new JMenuItem("Add Ceiling to Surface...");
        addSurfaceCeilingMenuItem.addActionListener(e -> addSurfaceCeiling());
        menu.add(addSurfaceCeilingMenuItem);

        removeSurfaceCeilingMenuItem = new JMenuItem("Remove Ceiling from Surface...");
        removeSurfaceCeilingMenuItem.addActionListener(e -> removeSurfaceCeiling());
        menu.add(removeSurfaceCeilingMenuItem);

        addNetherMenuItem = new JMenuItem(strings.getString("add.nether") + "...");
        addNetherMenuItem.addActionListener(e -> addNether());
        addNetherMenuItem.setMnemonic('n');
        menu.add(addNetherMenuItem);

        removeNetherMenuItem = new JMenuItem("Remove Nether...");
        removeNetherMenuItem.addActionListener(e -> removeNether());
        menu.add(removeNetherMenuItem);

        addNetherCeilingMenuItem = new JMenuItem("Add Ceiling to Nether...");
        addNetherCeilingMenuItem.addActionListener(e -> addNetherCeiling());
        menu.add(addNetherCeilingMenuItem);

        removeNetherCeilingMenuItem = new JMenuItem("Remove Ceiling from Nether...");
        removeNetherCeilingMenuItem.addActionListener(e -> removeNetherCeiling());
        menu.add(removeNetherCeilingMenuItem);

        addEndMenuItem = new JMenuItem(strings.getString("add.end") + "...");
        addEndMenuItem.addActionListener(e -> addEnd());
        addEndMenuItem.setMnemonic('d');
        menu.add(addEndMenuItem);
        
        removeEndMenuItem = new JMenuItem("Remove End...");
        removeEndMenuItem.addActionListener(e -> removeEnd());
        menu.add(removeEndMenuItem);

        addEndCeilingMenuItem = new JMenuItem("Add Ceiling to End...");
        addEndCeilingMenuItem.addActionListener(e -> addEndCeiling());
        menu.add(addEndCeilingMenuItem);

        removeEndCeilingMenuItem = new JMenuItem("Remove Ceiling from End...");
        removeEndCeilingMenuItem.addActionListener(e -> removeEndCeiling());
        menu.add(removeEndCeilingMenuItem);

//        final JMenuItem easyModeItem = new JCheckBoxMenuItem("Advanced mode");
//        if (! config.isEasyMode()) {
//            easyModeItem.setSelected(true);
//        }
//        easyModeItem.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(final ActionEvent e) {
//                Configuration config = Configuration.getInstance();
//                if (config.isEasyMode()) {
//                    if (JOptionPane.showConfirmDialog(App.this, "Are you sure you want to switch to Advanced Mode?", "Please Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
//                        config.setEasyMode(false);
//                        try {
//                            config.save();
//                            easyModeItem.setSelected(true);
//                            JOptionPane.showMessageDialog(App.this, "Advanced Mode has been activated. You must\nrestart WorldPainter for the change to take effect!", "Restart Required", JOptionPane.INFORMATION_MESSAGE);
//                        } catch (IOException exception) {
//                            ErrorDialog errorDialog = new ErrorDialog(App.this);
//                            errorDialog.setException(exception);
//                            errorDialog.setVisible(true);
//                        }
//                    } else {
//                        easyModeItem.setSelected(false);
//                    }
//                } else {
//                    if (JOptionPane.showConfirmDialog(App.this, "Are you sure you want to switch off Advanced Mode?", "Please Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
//                        config.setEasyMode(true);
//                        try {
//                            config.save();
//                            easyModeItem.setSelected(false);
//                            JOptionPane.showMessageDialog(App.this, "Advanced Mode has been deactivated. You must\nrestart WorldPainter for the change to take effect!", "Restart Required", JOptionPane.INFORMATION_MESSAGE);
//                        } catch (IOException exception) {
//                            ErrorDialog errorDialog = new ErrorDialog(App.this);
//                            errorDialog.setException(exception);
//                            errorDialog.setVisible(true);
//                        }
//                    } else {
//                        easyModeItem.setSelected(true);
//                    }
//                }
//            }
//        });
//        menu.add(easyModeItem);

        if ((! config.isEasyMode()) && (! hidePreferences)) {
            menu.addSeparator();

            menuItem = new JMenuItem(strings.getString("preferences") + "...");
            menuItem.addActionListener(e -> {
                PreferencesDialog dialog = new PreferencesDialog(App.this, selectedColourScheme);
                dialog.setVisible(true);
                if (! dialog.isCancelled()) {
                    setMaxRadius(Configuration.getInstance().getMaximumBrushSize());
            }
            });
            menuItem.setMnemonic('f');
            menu.add(menuItem);
        }
        menuBar.add(menu);

        menuItem = new JMenuItem(ACTION_ZOOM_IN);
        menuItem.setMnemonic('i');
        menu = new JMenu(strings.getString("view"));
        menu.setMnemonic('v');
        menu.add(menuItem);

        menuItem = new JMenuItem(ACTION_ZOOM_OUT);
        menuItem.setMnemonic('o');
        menu.add(menuItem);

        menuItem = new JMenuItem(ACTION_ZOOM_RESET);
        menuItem.setMnemonic('r');
        menu.add(menuItem);
        
        menu.addSeparator();
        
        viewSurfaceMenuItem = new JCheckBoxMenuItem(strings.getString("view.surface"), true);
        viewSurfaceMenuItem.addActionListener(e -> viewDimension(DIM_NORMAL));
        viewSurfaceMenuItem.setMnemonic('s');
        viewSurfaceMenuItem.setAccelerator(KeyStroke.getKeyStroke(VK_U, PLATFORM_COMMAND_MASK));
        viewSurfaceMenuItem.setEnabled(false);
        menu.add(viewSurfaceMenuItem);

        viewSurfaceCeilingMenuItem = new JCheckBoxMenuItem("View Surface Ceiling", false);
        viewSurfaceCeilingMenuItem.addActionListener(e -> viewDimension(DIM_NORMAL_CEILING));
        viewSurfaceCeilingMenuItem.setEnabled(false);
        menu.add(viewSurfaceCeilingMenuItem);

        viewNetherMenuItem = new JCheckBoxMenuItem(strings.getString("view.nether"), false);
        viewNetherMenuItem.addActionListener(e -> viewDimension(DIM_NETHER));
        viewNetherMenuItem.setMnemonic('n');
        viewNetherMenuItem.setAccelerator(KeyStroke.getKeyStroke(VK_H, PLATFORM_COMMAND_MASK));
        viewNetherMenuItem.setEnabled(false);
        menu.add(viewNetherMenuItem);

        viewNetherCeilingMenuItem = new JCheckBoxMenuItem("View Nether Ceiling", false);
        viewNetherCeilingMenuItem.addActionListener(e -> viewDimension(DIM_NETHER_CEILING));
        viewNetherCeilingMenuItem.setEnabled(false);
        menu.add(viewNetherCeilingMenuItem);

        viewEndMenuItem = new JCheckBoxMenuItem(strings.getString("view.end"), false);
        viewEndMenuItem.addActionListener(e -> viewDimension(DIM_END));
        viewEndMenuItem.setMnemonic('e');
        viewEndMenuItem.setAccelerator(KeyStroke.getKeyStroke(VK_D, PLATFORM_COMMAND_MASK));
        viewEndMenuItem.setEnabled(false);
        menu.add(viewEndMenuItem);

        viewEndCeilingMenuItem = new JCheckBoxMenuItem("View End Ceiling", false);
        viewEndCeilingMenuItem.addActionListener(e -> viewDimension(DIM_END_CEILING));
        viewEndCeilingMenuItem.setEnabled(false);
        menu.add(viewEndCeilingMenuItem);

        menu.add(ACTION_SWITCH_TO_FROM_CEILING);

        menu.addSeparator();
        
        JMenu colourSchemeMenu = new JMenu(strings.getString("change.colour.scheme"));
        String[] colourSchemeNames = {strings.getString("default"), "Flames", "Ovocean", "Sk89q", "DokuDark", "DokuHigh", "DokuLight", "Misa", "Sphax"};
        Set<String> deprecatedColourSchemes = new HashSet<>(Arrays.asList("Flames", "Ovocean", "Sk89q"));
        final int schemeCount = colourSchemeNames.length;
        final JCheckBoxMenuItem[] schemeMenuItems = new JCheckBoxMenuItem[schemeCount];
        for (int i = 0; i < colourSchemeNames.length; i++) {
            final int index = i;
            schemeMenuItems[index] = new JCheckBoxMenuItem(colourSchemeNames[index]);
            if (config.getColourschemeIndex() == index) {
                schemeMenuItems[index].setSelected(true);
            }
            schemeMenuItems[index].addActionListener(e -> {
                for (int i1 = 0; i1 < schemeCount; i1++) {
                    if ((i1 != index) && schemeMenuItems[i1].isSelected()) {
                        schemeMenuItems[i1].setSelected(false);
                    }
                }
                selectedColourScheme = colourSchemes[index];
                view.setColourScheme(selectedColourScheme);
                config.setColourschemeIndex(index);
            });
            if (! deprecatedColourSchemes.contains(colourSchemeNames[i])) {
                colourSchemeMenu.add(schemeMenuItems[index]);
            }
        }
        colourSchemeMenu.addSeparator();
        colourSchemeMenu.add(new JLabel("Deprecated:"));
        for (int i = 0; i < colourSchemeNames.length; i++) {
            if (deprecatedColourSchemes.contains(colourSchemeNames[i])) {
                colourSchemeMenu.add(schemeMenuItems[i]);
            }
        }
        menu.add(colourSchemeMenu);

        menuItem = new JMenuItem(strings.getString("configure.view") + "...");
        menuItem.addActionListener(e -> {
            ConfigureViewDialog dialog = new ConfigureViewDialog(App.this, dimension, view);
            dialog.setVisible(true);
            ACTION_GRID.setSelected(view.isPaintGrid());
            ACTION_CONTOURS.setSelected(view.isDrawContours());
            ACTION_OVERLAY.setSelected(view.isDrawOverlay());
        });
        menuItem.setMnemonic('c');
        menuItem.setAccelerator(KeyStroke.getKeyStroke(VK_V, PLATFORM_COMMAND_MASK));
        menu.add(menuItem);
        
        menu.addSeparator();

        menuItem = new JMenuItem(ACTION_RESET_DOCKS);
        menuItem.setMnemonic('d');
        menu.add(menuItem);

        ACTION_LOAD_LAYOUT.setEnabled(config.getDefaultJideLayoutData() != null);
        menuItem = new JMenuItem(ACTION_LOAD_LAYOUT);
        menuItem.setMnemonic('d');
        menu.add(menuItem);

        menuItem = new JMenuItem(ACTION_SAVE_LAYOUT);
        menuItem.setMnemonic('d');
        menu.add(menuItem);

        menu.addSeparator();
        
        menuItem = new JMenuItem(strings.getString("show.3d.view") + "...");
        menuItem.addActionListener(e -> {
            Point focusPoint = view.getViewCentreInWorldCoords();
            if (threeDeeFrame != null) {
                threeDeeFrame.requestFocus();
                threeDeeFrame.moveTo(focusPoint);
            } else {
                logger.info("Opening 3D view");
                threeDeeFrame = new ThreeDeeFrame(dimension, view.getColourScheme(), customBiomeManager, focusPoint);
                threeDeeFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                threeDeeFrame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        // TODO not sure how this can be null, but at least
                        // one error has been reported by a user where it
                        // was
                        if (threeDeeFrame != null) {
                            threeDeeFrame.dispose();
                            threeDeeFrame = null;
                        }
                    }
                });
                threeDeeFrame.setLocationRelativeTo(App.this);
                threeDeeFrame.setVisible(true);
            }
        });
        menuItem.setMnemonic('3');
        menuItem.setAccelerator(KeyStroke.getKeyStroke(VK_3, PLATFORM_COMMAND_MASK));
        menu.add(menuItem);
        
        menuBar.add(menu);

        if (! config.isEasyMode()) {
            menuItem = new JMenuItem(strings.getString("respawn.player") + "...");
            menuItem.addActionListener(e -> {
                RespawnPlayerDialog dialog = new RespawnPlayerDialog(App.this);
                dialog.setVisible(true);
            });
            menuItem.setMnemonic('r');
            menu = new JMenu(strings.getString("tools"));
            menu.setMnemonic('t');
            menu.add(menuItem);

            menuItem = new JMenuItem(strings.getString("open.custom.brushes.folder"));
            menuItem.addActionListener(e -> {
                File brushesDir = new File(Configuration.getConfigDir(), "brushes");
                if (! brushesDir.exists()) {
                    if (! brushesDir.mkdirs()) {
                        Toolkit.getDefaultToolkit().beep();
                        return;
                    }
                }
                DesktopUtils.open(brushesDir);
            });
            menuItem.setMnemonic('c');
            menu.add(menuItem);

            menuItem = new JMenuItem(strings.getString("open.plugins.folder"));
            menuItem.addActionListener(e -> {
                File pluginsDir = new File(Configuration.getConfigDir(), "plugins");
                if (! pluginsDir.exists()) {
                    if (! pluginsDir.mkdirs()) {
                        Toolkit.getDefaultToolkit().beep();
                        return;
                    }
                }
                DesktopUtils.open(pluginsDir);
            });
            menuItem.setMnemonic('p');
            menu.add(menuItem);

            menuItem = new JMenuItem(strings.getString("biomes.viewer") + "...");
            menuItem.addActionListener(event -> {
                if (biomesViewerFrame != null) {
                    biomesViewerFrame.requestFocus();
                } else {
                    BiomeScheme viewerScheme = null;
                    boolean askedFor17 = false;
                    if ((dimension != null) && (dimension.getDim() == DIM_NORMAL) && (dimension.getMaxHeight() == DEFAULT_MAX_HEIGHT_2)) {
                        if (world.getGenerator() == Generator.LARGE_BIOMES) {
                            viewerScheme = BiomeSchemeManager.getBiomeScheme(BIOME_ALGORITHM_1_7_LARGE, null, false);
                            if (viewerScheme == null) {
                                askedFor17 = true;
                                viewerScheme = BiomeSchemeManager.getBiomeScheme(BIOME_ALGORITHM_1_7_LARGE, App.this, true);
                            }
                        } else {
                            viewerScheme = BiomeSchemeManager.getBiomeScheme(BIOME_ALGORITHM_1_7_DEFAULT, null, false);
                            if (viewerScheme == null) {
                                askedFor17 = true;
                                viewerScheme = BiomeSchemeManager.getBiomeScheme(BIOME_ALGORITHM_1_7_DEFAULT, App.this, true);
                            }
                        }
                    }
                    if (viewerScheme == null) {
                        viewerScheme = BiomeSchemeManager.getBiomeScheme(BIOME_ALGORITHM_1_7_DEFAULT, null, false);
                    }
                    if (viewerScheme == null) {
                        viewerScheme = BiomeSchemeManager.getBiomeScheme(BIOME_ALGORITHM_1_2_AND_1_3_DEFAULT, null, false);
                    }
                    if (viewerScheme == null) {
                        viewerScheme = BiomeSchemeManager.getBiomeScheme(BIOME_ALGORITHM_1_1, null, false);
                    }
                    if ((viewerScheme == null) && (! askedFor17)) {
                        askedFor17 = true;
                        viewerScheme = BiomeSchemeManager.getBiomeScheme(BIOME_ALGORITHM_1_7_DEFAULT, App.this, true);
                    }
                    if (viewerScheme == null) {
                        viewerScheme = BiomeSchemeManager.getBiomeScheme(BIOME_ALGORITHM_1_2_AND_1_3_DEFAULT, App.this, true);
                    }
                    if (viewerScheme == null) {
                        viewerScheme = BiomeSchemeManager.getBiomeScheme(BIOME_ALGORITHM_1_1, App.this, true);
                    }
                    if (viewerScheme == null) {
                        JOptionPane.showMessageDialog(App.this, strings.getString("you.must.supply.an.original.minecraft.jar"), strings.getString("no.minecraft.jar.supplied"), JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    logger.info("Opening biomes viewer");
                    biomesViewerFrame = new BiomesViewerFrame(dimension.getMinecraftSeed(), world.getSpawnPoint(), viewerScheme, colourSchemes[0], App.this);
                    biomesViewerFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                    biomesViewerFrame.addWindowListener(new WindowAdapter() {
                        @Override
                        public void windowClosing(WindowEvent e) {
                            // TODO not sure how this can be null, but at least
                            // one error has been reported by a user where it
                            // was
                            if (biomesViewerFrame != null) {
                                biomesViewerFrame.destroy();
                                biomesViewerFrame.dispose();
                                biomesViewerFrame = null;
                            }
                        }
                    });
                    biomesViewerFrame.setLocationRelativeTo(App.this);
                    biomesViewerFrame.setVisible(true);
                }
            });
            menuItem.setMnemonic('b');
            menu.add(menuItem);

    //        menuItem = new JMenuItem("Manage plugins...");
    //        menuItem.addActionListener(new ActionListener() {
    //            @Override
    //            public void actionPerformed(ActionEvent e) {
    //                StringBuilder url = new StringBuilder("http://bo.worldpainter.net:8081/wp/plugins/overview.jsp");
    //                url.append("?uuid=").append(Configuration.getInstance().getUuid().toString());
    //                boolean first = true;
    //                for (Plugin plugin: PluginManager.getInstance().getAllPlugins()) {
    //                    if (plugin.getName().equals("Default")) {
    //                        continue;
    //                    }
    //                    if (first) {
    //                        url.append("&plugins=");
    //                        first = false;
    //                    } else {
    //                        url.append(',');
    //                    }
    //                    url.append(plugin.getName().replaceAll("\\s", "").toLowerCase());
    //                }
    //                SimpleBrowser browser = new SimpleBrowser(App.this, true, "Manage Plugins", url.toString());
    //                browser.setVisible(true);
    //            }
    //        });
    //        menuItem.setMnemonic('p');
    //        menu.add(menuItem);
            menuBar.add(menu);
        }
        
        menuItem = new JMenuItem(ACTION_OPEN_DOCUMENTATION);
        menuItem.setMnemonic('d');
        menu = new JMenu(strings.getString("help"));
//        menu.setMnemonic('h');
        menu.add(menuItem);

        if (! hideAbout) {
            menu.addSeparator();

            menuItem = new JMenuItem(strings.getString("about"));
            menuItem.setMnemonic('a');
            menuItem.addActionListener(e -> {
                AboutDialog dialog = new AboutDialog(App.this, world, view, currentUndoManager);
                dialog.setVisible(true);
            });
            menu.add(menuItem);
        }
        menuBar.add(menu);
        
        addStatisticsTo(menuBar, "menu", config);
        
        return menuBar;
    }

    private void addSurfaceCeiling() {
        final NewWorldDialog dialog = new NewWorldDialog(this, world.getName(), dimension.getSeed() + 1, DIM_NORMAL_CEILING, world.getMaxHeight(), world.getDimension(DIM_NORMAL).getTileCoords());
        dialog.setVisible(true);
        if (! dialog.isCancelled()) {
            if (! dialog.checkMemoryRequirements(this)) {
                return;
            }
            Dimension surfaceCeiling = ProgressDialog.executeTask(this, new ProgressTask<Dimension>() {
                @Override
                public String getName() {
                    return "Creating Surface Ceiling";
                }

                @Override
                public Dimension execute(ProgressReceiver progressReceiver) throws OperationCancelled {
                    return dialog.getSelectedDimension(progressReceiver);
                }
            });
            if (surfaceCeiling == null) {
                // Cancelled by user
                return;
            }
            world.addDimension(surfaceCeiling);
            setDimension(surfaceCeiling);
            setDimensionControlStates();
        }
    }

    private void removeSurfaceCeiling() {
        if (JOptionPane.showConfirmDialog(this, "Are you sure you want to completely remove the Surface ceiling?\nThis action cannot be undone!", "Confirm Surface Ceiling Deletion", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            world.removeDimension(DIM_NORMAL_CEILING);
            if ((dimension != null) && (dimension.getDim() == DIM_NORMAL_CEILING)) {
                viewDimension(DIM_NORMAL);
            } else {
                setDimensionControlStates();
                if (dimension.getDim() == DIM_NORMAL) {
                    view.refreshTiles();
                }
            }
            JOptionPane.showMessageDialog(this, "The Surface ceiling was successfully deleted", "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void addNetherCeiling() {
        final NewWorldDialog dialog = new NewWorldDialog(this, world.getName(), dimension.getSeed() + 1, DIM_NETHER_CEILING, world.getMaxHeight(), world.getDimension(DIM_NETHER).getTileCoords());
        dialog.setVisible(true);
        if (! dialog.isCancelled()) {
            if (! dialog.checkMemoryRequirements(this)) {
                return;
            }
            Dimension netherCeiling = ProgressDialog.executeTask(this, new ProgressTask<Dimension>() {
                @Override
                public String getName() {
                    return "Creating Nether Ceiling";
                }

                @Override
                public Dimension execute(ProgressReceiver progressReceiver) throws OperationCancelled {
                    return dialog.getSelectedDimension(progressReceiver);
                }
            });
            if (netherCeiling == null) {
                // Cancelled by user
                return;
            }
            world.addDimension(netherCeiling);
            setDimension(netherCeiling);
            setDimensionControlStates();
        }
    }

    private void removeNetherCeiling() {
        if (JOptionPane.showConfirmDialog(this, "Are you sure you want to completely remove the Nether ceiling?\nThis action cannot be undone!", "Confirm Nether Ceiling Deletion", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            world.removeDimension(DIM_NETHER_CEILING);
            if ((dimension != null) && (dimension.getDim() == DIM_NETHER_CEILING)) {
                viewDimension(DIM_NETHER);
            } else {
                setDimensionControlStates();
                if (dimension.getDim() == DIM_NETHER) {
                    view.refreshTiles();
                }
            }
            JOptionPane.showMessageDialog(this, "The Nether ceiling was successfully deleted", "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void addEndCeiling() {
        final NewWorldDialog dialog = new NewWorldDialog(this, world.getName(), dimension.getSeed() + 1, DIM_END_CEILING, world.getMaxHeight(), world.getDimension(DIM_END).getTileCoords());
        dialog.setVisible(true);
        if (! dialog.isCancelled()) {
            if (! dialog.checkMemoryRequirements(this)) {
                return;
            }
            Dimension endCeiling = ProgressDialog.executeTask(this, new ProgressTask<Dimension>() {
                @Override
                public String getName() {
                    return "Creating End Ceiling";
                }

                @Override
                public Dimension execute(ProgressReceiver progressReceiver) throws OperationCancelled {
                    return dialog.getSelectedDimension(progressReceiver);
                }
            });
            if (endCeiling == null) {
                // Cancelled by user
                return;
            }
            world.addDimension(endCeiling);
            setDimension(endCeiling);
            setDimensionControlStates();
        }
    }

    private void removeEndCeiling() {
        if (JOptionPane.showConfirmDialog(this, "Are you sure you want to completely remove the End ceiling?\nThis action cannot be undone!", "Confirm End Ceiling Deletion", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            world.removeDimension(DIM_END_CEILING);
            if ((dimension != null) && (dimension.getDim() == DIM_END_CEILING)) {
                viewDimension(DIM_END);
            } else {
                setDimensionControlStates();
                if (dimension.getDim() == DIM_END) {
                    view.refreshTiles();
                }
            }
            JOptionPane.showMessageDialog(this, "The End ceiling was successfully deleted", "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }

//    private void importMapIntoWorld() {
//        JFileChooser fileChooser = new JFileChooser(Configuration.getInstance().getExportDirectory());
//        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
//        fileChooser.setFileFilter(new FileFilter() {
//            @Override
//            public boolean accept(File f) {
//                return f.isDirectory() || f.equals("level.dat");
//            }
//
//            @Override
//            public String getDescription() {
//                return "Minecraft map level files (level.dat)";
//            }
//        });
//        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
//            File levelDatFile = fileChooser.getSelectedFile();
//            view.addTileProvider(new TileProvider() {
//                @Override
//                public int getTileSize() {
//                    return 0;
//                }
//
//                @Override
//                public BufferedImage getTile(int x, int y) {
//                    return null;
//                }
//
//                @Override
//                public int getTilePriority(int x, int y) {
//                    return 0;
//                }
//
//                @Override
//                public Rectangle getExtent() {
//                    return null;
//                }
//
//                @Override
//                public void addTileListener(TileListener tileListener) {
//
//                }
//
//                @Override
//                public void removeTileListener(TileListener tileListener) {
//
//                }
//
//                @Override
//                public boolean isZoomSupported() {
//                    return false;
//                }
//
//                @Override
//                public int getZoom() {
//                    return 0;
//                }
//
//                @Override
//                public void setZoom(int zoom) {
//
//                }
//            });
//        }
//    }

    private JToolBar createToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.add(ACTION_NEW_WORLD);
        toolBar.add(ACTION_OPEN_WORLD);
        toolBar.add(ACTION_SAVE_WORLD);
        toolBar.add(ACTION_EXPORT_WORLD);
        toolBar.addSeparator();
        toolBar.add(ACTION_UNDO);
        toolBar.add(ACTION_REDO);
        toolBar.addSeparator();
        toolBar.add(ACTION_ZOOM_OUT);
        toolBar.add(ACTION_ZOOM_RESET);
        toolBar.add(ACTION_ZOOM_IN);
        toolBar.addSeparator();
        toolBar.add(ACTION_DIMENSION_PROPERTIES);
        if (! Configuration.getInstance().isEasyMode()) {
            toolBar.add(ACTION_CHANGE_HEIGHT);
            toolBar.add(ACTION_ROTATE_WORLD);
            toolBar.add(ACTION_SHIFT_WORLD);
        }
        toolBar.add(ACTION_EDIT_TILES);
        toolBar.addSeparator();
        toolBar.add(ACTION_MOVE_TO_SPAWN);
        toolBar.add(ACTION_MOVE_TO_ORIGIN);
        toolBar.addSeparator();
        JToggleButton button = new JToggleButton(ACTION_GRID);
        button.setHideActionText(true);
        toolBar.add(button);
        button = new JToggleButton(ACTION_CONTOURS);
        button.setHideActionText(true);
        toolBar.add(button);
        button = new JToggleButton(ACTION_OVERLAY);
        button.setHideActionText(true);
        toolBar.add(button);
        button = new JToggleButton(ACTION_VIEW_DISTANCE);
        button.setHideActionText(true);
        toolBar.add(button);
        button = new JToggleButton(ACTION_WALKING_DISTANCE);
        button.setHideActionText(true);
        toolBar.add(button);
        toolBar.add(ACTION_ROTATE_LIGHT_LEFT);
        toolBar.add(ACTION_ROTATE_LIGHT_RIGHT);
        return toolBar;
    }
    
    private void addStatisticsTo(MenuElement menuElement, @NonNls final String key, final EventLogger eventLogger) {
        if ((menuElement instanceof JMenuItem) && (! (menuElement instanceof JMenu))) {
            JMenuItem menuItem = (JMenuItem) menuElement;
            if (((! (menuItem.getAction() instanceof BetterAction)) || (! ((BetterAction) menuItem.getAction()).isLogEvent()))
                    && (! menuItem.getText().equals("Existing Minecraft map..."))
                    && (! menuItem.getText().equals("Merge World..."))){
                menuItem.addActionListener(e -> eventLogger.logEvent(new EventVO(key).addTimestamp()));
            }
        }
        for (MenuElement subElement: menuElement.getSubElements()) {
            if (subElement instanceof JPopupMenu) {
                addStatisticsTo(subElement, key, eventLogger);
            } else if (subElement instanceof JMenuItem) {
                addStatisticsTo(subElement, key + "." + ((JMenuItem) subElement).getText().replaceAll("[ \\t\\n\\x0B\\f\\r\\.]", ""), eventLogger);
            }
        }
    }
    
    private void addNether() {
        final NewWorldDialog dialog = new NewWorldDialog(this, world.getName(), dimension.getSeed() + 1, DIM_NETHER, world.getMaxHeight());
        dialog.setVisible(true);
        if (! dialog.isCancelled()) {
            if (! dialog.checkMemoryRequirements(this)) {
                return;
            }
            Dimension nether = ProgressDialog.executeTask(this, new ProgressTask<Dimension>() {
                @Override
                public String getName() {
                    return "Creating Nether";
                }

                @Override
                public Dimension execute(ProgressReceiver progressReceiver) throws OperationCancelled {
                    return dialog.getSelectedDimension(progressReceiver);
                }
            });
            if (nether == null) {
                // Cancelled by user
                return;
            }
            world.addDimension(nether);
            setDimension(nether);
            setDimensionControlStates();
            DimensionPropertiesDialog propertiesDialog = new DimensionPropertiesDialog(this, nether, selectedColourScheme);
            propertiesDialog.setVisible(true);
        }
    }

    private void removeNether() {
        if (JOptionPane.showConfirmDialog(this, "Are you sure you want to completely remove the Nether dimension?\nThis action cannot be undone!", "Confirm Nether Deletion", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            world.removeDimension(DIM_NETHER);
            if (world.getDimension(DIM_NETHER_CEILING) != null) {
                world.removeDimension(DIM_NETHER_CEILING);
            }
            if ((dimension != null) && ((dimension.getDim() == DIM_NETHER) || (dimension.getDim() == DIM_NETHER_CEILING))) {
                viewDimension(DIM_NORMAL);
            } else {
                setDimensionControlStates();
            }
            JOptionPane.showMessageDialog(this, "The Nether dimension was successfully deleted", "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void addEnd() {
        final NewWorldDialog dialog = new NewWorldDialog(this, world.getName(), dimension.getSeed() + 1, DIM_END, world.getMaxHeight());
        dialog.setVisible(true);
        if (! dialog.isCancelled()) {
            if (! dialog.checkMemoryRequirements(this)) {
                return;
            }
            Dimension end = ProgressDialog.executeTask(this, new ProgressTask<Dimension>() {
                @Override
                public String getName() {
                    return "Creating End";
                }

                @Override
                public Dimension execute(ProgressReceiver progressReceiver) throws OperationCancelled {
                    return dialog.getSelectedDimension(progressReceiver);
                }
            });
            if (end == null) {
                // Cancelled by user
                return;
            }
            world.addDimension(end);
            setDimension(end);
            setDimensionControlStates();
        }
    }

    private void removeEnd() {
        if (JOptionPane.showConfirmDialog(this, "Are you sure you want to completely remove the End dimension?\nThis action cannot be undone!", "Confirm End Deletion", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            world.removeDimension(DIM_END);
            if (world.getDimension(DIM_END_CEILING) != null) {
                world.removeDimension(DIM_END_CEILING);
            }
            if ((dimension != null) && ((dimension.getDim() == DIM_END) || (dimension.getDim() == DIM_END_CEILING))) {
                viewDimension(DIM_NORMAL);
            } else {
                setDimensionControlStates();
            }
            JOptionPane.showMessageDialog(this, "The End dimension was successfully deleted", "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void viewDimension(int dim) {
        if (dim != dimension.getDim()) {
            setDimension(world.getDimension(dim));
            setDimensionControlStates();
        }
    }
    
    private void fixLabelSizes() {
        locationLabel.setMinimumSize(locationLabel.getSize());
        locationLabel.setPreferredSize(locationLabel.getSize());
        locationLabel.setMaximumSize(locationLabel.getSize());
        heightLabel.setMinimumSize(heightLabel.getSize());
        heightLabel.setPreferredSize(heightLabel.getSize());
        heightLabel.setMaximumSize(heightLabel.getSize());
        materialLabel.setMinimumSize(materialLabel.getSize());
        materialLabel.setPreferredSize(materialLabel.getSize());
        materialLabel.setMaximumSize(materialLabel.getSize());
        waterLabel.setMinimumSize(waterLabel.getSize());
        waterLabel.setPreferredSize(waterLabel.getSize());
        waterLabel.setMaximumSize(waterLabel.getSize());
        biomeLabel.setMinimumSize(biomeLabel.getSize());
        biomeLabel.setPreferredSize(biomeLabel.getSize());
        biomeLabel.setMaximumSize(biomeLabel.getSize());
        radiusLabel.setMinimumSize(radiusLabel.getSize());
        radiusLabel.setPreferredSize(radiusLabel.getSize());
        radiusLabel.setMaximumSize(radiusLabel.getSize());
        zoomLabel.setMinimumSize(zoomLabel.getSize());
        zoomLabel.setPreferredSize(zoomLabel.getSize());
        zoomLabel.setMaximumSize(zoomLabel.getSize());

        locationLabel.setText(strings.getString("location-"));
        heightLabel.setText(" ");
        waterLabel.setText(" ");
        biomeLabel.setText(" ");
        materialLabel.setText(" ");
        radiusLabel.setText(MessageFormat.format(strings.getString("radius.0"), radius));
        zoomLabel.setText(MessageFormat.format(strings.getString("zoom.0"), 100));
    }

    private JToggleButton createButtonForOperation(Operation operation, @NonNls String iconName) {
        return createButtonForOperation(operation, iconName, (char) 0);
    }

    private JToggleButton createButtonForOperation(final Operation operation, @NonNls String iconName, char mnemonic) {
        Icon icon;
        if (iconName != null) {
            icon = loadIcon(operation, iconName);
        } else {
            icon = null;
        }
        JToggleButton button = new JToggleButton();
        if (operation instanceof SetSpawnPoint) {
            setSpawnPointToggleButton = button;
        }
        button.setMargin(new Insets(2, 2, 2, 2));
        if (icon != null) {
            button.setIcon(icon);
        }
        if (operation.getName().equalsIgnoreCase(operation.getDescription())) {
            button.setToolTipText(operation.getName());
        } else {
            button.setToolTipText(operation.getName() + ": " + operation.getDescription());
        }
        if (mnemonic != 0) {
            button.setMnemonic(mnemonic);
        }
        button.addItemListener(event -> {
            if (event.getStateChange() == ItemEvent.DESELECTED) {
                if (operation instanceof RadiusOperation) {
                    view.setDrawRadius(false);
                }
                operation.setActive(false);
                activeOperation = null;
            } else {
                if (operation instanceof PaintOperation) {
                    programmaticChange = true;
                    try {
                        if (operation instanceof MouseOrTabletOperation) {
                            ((MouseOrTabletOperation) operation).setLevel(level);
                            if (operation instanceof RadiusOperation) {
                                ((RadiusOperation) operation).setBrush(brushRotation == 0 ? brush : RotatedBrush.rotate(brush, brushRotation));
                                ((RadiusOperation) operation).setFilter(filter);
                                selectBrushButton(brush);
                                view.setBrushShape(brush.getBrushShape());
                                view.setBrushRotation(brushRotation);
                            }
                        }
                        levelSlider.setValue((int) (level * 100));
                        brushRotationSlider.setValue(brushRotation);
                    } finally {
                        programmaticChange = false;
                    }
                    brushOptions.setFilter(filter);
                    ((PaintOperation) operation).setPaint(paint);
                } else {
                    programmaticChange = true;
                    try {
                        if (operation instanceof MouseOrTabletOperation) {
                            ((MouseOrTabletOperation) operation).setLevel(toolLevel);
                            if (operation instanceof RadiusOperation) {
                                ((RadiusOperation) operation).setBrush(toolBrushRotation == 0 ? toolBrush : RotatedBrush.rotate(toolBrush, toolBrushRotation));
                                ((RadiusOperation) operation).setFilter(toolFilter);
                                selectBrushButton(toolBrush);
                                view.setBrushShape(toolBrush.getBrushShape());
                                view.setBrushRotation(toolBrushRotation);
                            }
                        }
                        levelSlider.setValue((int) (toolLevel * 100));
                        brushRotationSlider.setValue(toolBrushRotation);
                    } finally {
                        programmaticChange = false;
                    }
                    brushOptions.setFilter(toolFilter);
                }
                if (operation instanceof RadiusOperation) {
                    view.setDrawRadius(true);
                    view.setRadius(radius);
                    ((RadiusOperation) operation).setRadius(radius);
                }
                activeOperation = operation;
                updateLayerVisibility();
                updateBrushRotation();
                operation.setActive(true);
            }
        });
        toolButtonGroup.add(button);
        return button;
    }

    private List<Component> createLayerButton(final Layer layer, char mnemonic, boolean checkboxEnabled) {
        boolean readOnlyOperation = layer.equals(ReadOnly.INSTANCE);
        final JToggleButton button = new JToggleButton();
        if (readOnlyOperation) {
            readOnlyToggleButton = button;
        }
        button.setMargin(new Insets(2, 2, 2, 2));
        button.setIcon(new ImageIcon(layer.getIcon()));
        button.setToolTipText(layer.getName() + ": " + layer.getDescription());
        // TODO: make this work again, but with Ctrl + Alt or something
//        if (mnemonic != 0) {
//            button.setMnemonic(mnemonic);
//        }
        button.addItemListener(event -> {
            if (event.getStateChange() == ItemEvent.SELECTED) {
                paintUpdater = () -> {
                    paint = PaintFactory.createLayerPaint(layer);
                    paintChanged();
                };
                paintUpdater.updatePaint();
            }
        });
        paintButtonGroup.add(button);
        final JCheckBox checkBox = new JCheckBox();
        if (readOnlyOperation) {
            readOnlyCheckBox = checkBox;
        }
        checkBox.setToolTipText(strings.getString("whether.or.not.to.display.this.layer"));
        checkBox.setSelected(true);
        if (checkboxEnabled) {
            checkBox.addActionListener(e -> {
                if (checkBox.isSelected()) {
                    hiddenLayers.remove(layer);
                } else {
                    hiddenLayers.add(layer);
                }
                updateLayerVisibility();
            });
        } else {
            checkBox.setEnabled(false);
        }

        final JCheckBox soloCheckBox = new JCheckBox();
        if (readOnlyOperation) {
            readOnlySoloCheckBox = soloCheckBox;
        }
        layerSoloCheckBoxes.put(layer, soloCheckBox);
        soloCheckBox.setToolTipText("<html>Check to show <em>only</em> this layer (the other layers are still exported)</html>");
        if (checkboxEnabled) {
            soloCheckBox.addActionListener(e -> {
                if (soloCheckBox.isSelected()) {
                    layerSoloCheckBoxes.values().stream().filter(otherSoloCheckBox -> otherSoloCheckBox != soloCheckBox).forEach(otherSoloCheckBox -> otherSoloCheckBox.setSelected(false));
                    soloLayer = layer;
                } else {
                    soloLayer = null;
                }
                updateLayerVisibility();
            });
        } else {
            soloCheckBox.setEnabled(false);
        }

        button.setText(layer.getName());
        return Arrays.asList((Component) checkBox, soloCheckBox, button);
    }

    private JToggleButton createTerrainButton(final Terrain terrain) {
        final JToggleButton button = new JToggleButton();
        button.setMargin(new Insets(2, 2, 2, 2));
        button.setIcon(new ImageIcon(terrain.getIcon(defaultColourScheme)));
        button.setToolTipText(terrain.getName() + ": " + terrain.getDescription());
        button.addItemListener(event -> {
            if (event.getStateChange() == ItemEvent.SELECTED) {
                paintUpdater = () -> {
                    paint = PaintFactory.createTerrainPaint(terrain);
                    paintChanged();
                };
                paintUpdater.updatePaint();
            }
        });
        paintButtonGroup.add(button);
        return button;
    }

    private void paintChanged() {
        if (activeOperation instanceof PaintOperation) {
            ((PaintOperation) activeOperation).setPaint(paint);
            updateLayerVisibility();
        }
    }

    /**
     * Configure the view to show the correct layers
     */
    private void updateLayerVisibility() {
        // Get the currently hidden layers
        Set<Layer> viewHiddenLayers = view.getHiddenLayers();
        
        // Determine which layers should be hidden
        Set<Layer> targetHiddenLayers = new HashSet<>();
        // The FloodWithLava layer should *always* be hidden
        targetHiddenLayers.add(FloodWithLava.INSTANCE);
        if (soloLayer != null) {
            // Only the solo layer and the active layer (if there is one and it
            // is different than the solo layer) should be visible
            targetHiddenLayers.addAll((dimension != null) ? dimension.getAllLayers(true) : new HashSet<>(layers));
            targetHiddenLayers.remove(soloLayer);
        } else {
            // The layers marked as hidden should be invisible, except the
            // currently active one, if any
            targetHiddenLayers.addAll(hiddenLayers);
        }
        // The currently active layer, if any, should always be visible
        if ((activeOperation instanceof PaintOperation) && (paint instanceof LayerPaint)) {
            targetHiddenLayers.remove(((LayerPaint) paint).getLayer());
        }
        
        // Hide the selected layers
        targetHiddenLayers.stream().filter(hiddenLayer -> !viewHiddenLayers.contains(hiddenLayer)).forEach(view::addHiddenLayer);
        viewHiddenLayers.stream().filter(hiddenLayer -> !targetHiddenLayers.contains(hiddenLayer)).forEach(view::removeHiddenLayer);
        
        // Configure the glass pane to show the right icons
        glassPane.setHiddenLayers(hiddenLayers);
        glassPane.setSoloLayer(soloLayer);
    }
    
    private void selectBrushButton(Brush brush) {
        brushButtons.get(brush).setSelected(true);
    }

    private JComponent createBrushButton(final Brush brush) {
        final JToggleButton button = new JToggleButton(createBrushIcon(brush, 0));
        button.setMargin(new Insets(2, 2, 2, 2));
        button.setToolTipText(brush.getName());
        button.addItemListener(e -> {
            if ((! programmaticChange) && (e.getStateChange() == ItemEvent.SELECTED)) {
                int effectiveRotation;
                if (activeOperation instanceof PaintOperation) {
                    App.this.brush = brush;
                    effectiveRotation = brushRotation;
                } else {
                    toolBrush = brush;
                    effectiveRotation = toolBrushRotation;
                }
                if (activeOperation instanceof RadiusOperation) {
                    ((RadiusOperation) activeOperation).setBrush((effectiveRotation == 0) ? brush : RotatedBrush.rotate(brush, effectiveRotation));
                }
                view.setBrushShape(brush.getBrushShape());
            }
        });
        brushButtonGroup.add(button);
        brushButtons.put(brush, button);
        return button;
    }
    
    private Icon createBrushIcon(Brush brush, int degrees) {
        brush = brush.clone();
        brush.setRadius(15);
        if (degrees != 0) {
            brush = RotatedBrush.rotate(brush, degrees);
        }
        return new ImageIcon(createBrushImage(brush));
    }

    private BufferedImage createBrushImage(Brush brush) {
        BufferedImage image = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().createCompatibleImage(32, 32, Transparency.TRANSLUCENT);
        for (int dx = -15; dx <= 15; dx++) {
            for (int dy = -15; dy <= 15; dy++) {
                float strength = brush.getFullStrength(dx, dy);
                int alpha = (int) (strength * 255f + 0.5f);
                image.setRGB(dx + 15, dy + 15, alpha << 24);
            }
        }
        return image;
    }

    private static Icon loadIcon(@NonNls String name) {
        return IconUtils.loadIcon("org/pepsoft/worldpainter/icons/" + name + ".png");
    }
    
    private static Icon loadIcon(Object plugin, String name) {
        return IconUtils.loadIcon(plugin.getClass().getClassLoader(), "org/pepsoft/worldpainter/icons/" + name + ".png");
    }
    
    private void enableImportedWorldOperation() {
        if (! alwaysEnableReadOnly) {
            readOnlyCheckBox.setEnabled(true);
            readOnlyToggleButton.setEnabled(true);
            readOnlySoloCheckBox.setEnabled(true);
        }
    }

    private void disableImportedWorldOperation() {
        if (! alwaysEnableReadOnly) {
            if ((activeOperation instanceof PaintOperation) && (paint instanceof LayerPaint) && ((LayerPaint) paint).getLayer().equals(Biome.INSTANCE)) {
                deselectPaint();
            }
            readOnlyCheckBox.setEnabled(false);
            readOnlyToggleButton.setEnabled(false);
            readOnlySoloCheckBox.setEnabled(false);
            if (readOnlySoloCheckBox.isSelected()) {
                readOnlySoloCheckBox.setSelected(false);
                soloLayer = null;
                updateLayerVisibility();
            }
        }
    }
    
    private World2 migrate(Object object) {
        if (object instanceof World) {
            World oldWorld = (World) object;
            World2 newWorld = new World2(oldWorld.getMinecraftSeed(), oldWorld.getTileFactory(), 128);
            newWorld.setCreateGoodiesChest(oldWorld.isCreateGoodiesChest());
            newWorld.setImportedFrom(oldWorld.getImportedFrom());
            newWorld.setName(oldWorld.getName());
            newWorld.setSpawnPoint(oldWorld.getSpawnPoint());
            Dimension dim0 = newWorld.getDimension(0);
            Generator generator = Generator.DEFAULT;
            TileFactory tileFactory = dim0.getTileFactory();
            if ((tileFactory instanceof HeightMapTileFactory)
                    && (((HeightMapTileFactory) tileFactory).getWaterHeight() < 32)
                    && (((HeightMapTileFactory) tileFactory).getBaseHeight() < 32)) {
                // Low level
                generator = Generator.FLAT;
            }
            newWorld.setGenerator(generator);
            newWorld.setAskToConvertToAnvil(true);
            newWorld.setUpIs(Direction.WEST);
            newWorld.setAskToRotate(true);
            newWorld.setAllowMerging(false);
            dim0.setEventsInhibited(true);
            try {
                dim0.setBedrockWall(oldWorld.isBedrockWall());
                dim0.setBorder((oldWorld.getBorder() != null) ? Dimension.Border.valueOf(oldWorld.getBorder().name()) : null);
                dim0.setDarkLevel(oldWorld.isDarkLevel());
                for (Map.Entry<Layer, ExporterSettings> entry: oldWorld.getAllLayerSettings().entrySet()) {
                    dim0.setLayerSettings(entry.getKey(), entry.getValue());
                }
                dim0.setMinecraftSeed(oldWorld.getMinecraftSeed());
                dim0.setPopulate(oldWorld.isPopulate());
                dim0.setContoursEnabled(false);
                Terrain subsurfaceMaterial = oldWorld.getSubsurfaceMaterial();
                ResourcesExporterSettings resourcesSettings = (ResourcesExporterSettings) dim0.getLayerSettings(Resources.INSTANCE);
                if (subsurfaceMaterial == Terrain.RESOURCES) {
                    dim0.setSubsurfaceMaterial(Terrain.STONE);
                } else {
                    dim0.setSubsurfaceMaterial(subsurfaceMaterial);
                    resourcesSettings.setMinimumLevel(0);
                }
                
                // Load legacy settings
                resourcesSettings.setChance(BLK_GOLD_ORE,         1);
                resourcesSettings.setChance(BLK_IRON_ORE,         5);
                resourcesSettings.setChance(BLK_COAL,             9);
                resourcesSettings.setChance(BLK_LAPIS_LAZULI_ORE, 1);
                resourcesSettings.setChance(BLK_DIAMOND_ORE,      1);
                resourcesSettings.setChance(BLK_REDSTONE_ORE,     6);
                resourcesSettings.setChance(BLK_WATER,            1);
                resourcesSettings.setChance(BLK_LAVA,             1);
                resourcesSettings.setChance(BLK_DIRT,             9);
                resourcesSettings.setChance(BLK_GRAVEL,           9);
                resourcesSettings.setChance(BLK_EMERALD_ORE,      0);
                resourcesSettings.setMaxLevel(BLK_GOLD_ORE,         Terrain.GOLD_LEVEL);
                resourcesSettings.setMaxLevel(BLK_IRON_ORE,         Terrain.IRON_LEVEL);
                resourcesSettings.setMaxLevel(BLK_COAL,             Terrain.COAL_LEVEL);
                resourcesSettings.setMaxLevel(BLK_LAPIS_LAZULI_ORE, Terrain.LAPIS_LAZULI_LEVEL);
                resourcesSettings.setMaxLevel(BLK_DIAMOND_ORE,      Terrain.DIAMOND_LEVEL);
                resourcesSettings.setMaxLevel(BLK_REDSTONE_ORE,     Terrain.REDSTONE_LEVEL);
                resourcesSettings.setMaxLevel(BLK_WATER,            Terrain.WATER_LEVEL);
                resourcesSettings.setMaxLevel(BLK_LAVA,             Terrain.LAVA_LEVEL);
                resourcesSettings.setMaxLevel(BLK_DIRT,             Terrain.DIRT_LEVEL);
                resourcesSettings.setMaxLevel(BLK_GRAVEL,           Terrain.GRAVEL_LEVEL);
                resourcesSettings.setMaxLevel(BLK_EMERALD_ORE,      Terrain.GOLD_LEVEL);

                oldWorld.getTiles().forEach(dim0::addTile);
            } finally {
                dim0.setEventsInhibited(false);
            }
            newWorld.setDirty(false);

            // Log event
            Configuration config = Configuration.getInstance();
            if (config != null) {
                config.logEvent(new EventVO(EVENT_KEY_ACTION_MIGRATE_WORLD).addTimestamp());
            }

            return newWorld;
        } else {
            throw new IllegalArgumentException("Save file format not supported");
        }
    }
    
    private void setDimensionControlStates() {
        boolean imported = (world != null) && (world.getImportedFrom() != null);
        boolean nether = (world != null) && (world.getDimension(DIM_NETHER) != null);
        boolean end = (world != null) && (world.getDimension(DIM_END) != null);
        boolean surfaceCeiling = (world != null) && (world.getDimension(DIM_NORMAL_CEILING) != null);
        boolean netherCeiling = (world != null) && (world.getDimension(DIM_NETHER_CEILING) != null);
        boolean endCeiling = (world != null) && (world.getDimension(DIM_END_CEILING) != null);
        addNetherMenuItem.setEnabled((! imported) && (! nether));
        removeNetherMenuItem.setEnabled(nether);
        addEndMenuItem.setEnabled((! imported) && (! end));
        removeEndMenuItem.setEnabled(end);
        viewSurfaceMenuItem.setEnabled(nether || end || surfaceCeiling);
        viewSurfaceCeilingMenuItem.setEnabled(surfaceCeiling);
        viewNetherMenuItem.setEnabled(nether);
        viewNetherCeilingMenuItem.setEnabled(netherCeiling);
        viewEndMenuItem.setEnabled(end);
        viewEndCeilingMenuItem.setEnabled(endCeiling);
        addSurfaceCeilingMenuItem.setEnabled((! imported) && (! surfaceCeiling));
        removeSurfaceCeilingMenuItem.setEnabled(surfaceCeiling);
        addNetherCeilingMenuItem.setEnabled(nether && (! netherCeiling));
        removeNetherCeilingMenuItem.setEnabled(netherCeiling);
        addEndCeilingMenuItem.setEnabled(end && (! endCeiling));
        removeEndCeilingMenuItem.setEnabled(endCeiling);
        if (dimension != null) {
            switch (dimension.getDim()) {
                case DIM_NORMAL:
                    setSpawnPointToggleButton.setEnabled(true);
                    ACTION_MOVE_TO_SPAWN.setEnabled(true);
                    biomesPanel.setEnabled(true);
                    biomesCheckBox.setEnabled(true);
                    biomesSoloCheckBox.setEnabled(true);
                    break;
                case DIM_NORMAL_CEILING:
                    if ((paint instanceof DiscreteLayerPaint) && ((DiscreteLayerPaint) paint).getLayer().equals(Biome.INSTANCE)) {
                        // TODO: select another paint & panel
                    }
                    setSpawnPointToggleButton.setEnabled(true);
                    ACTION_MOVE_TO_SPAWN.setEnabled(true);
                    biomesPanel.setEnabled(false);
                    biomesCheckBox.setEnabled(false);
                    biomesSoloCheckBox.setEnabled(false);
                    break;
                default:
                    if (activeOperation instanceof SetSpawnPoint) {
                        deselectTool();
                    } else if ((paint instanceof DiscreteLayerPaint) && ((DiscreteLayerPaint) paint).getLayer().equals(Biome.INSTANCE)) {
                        // TODO: select another paint & panel
                    }
                    setSpawnPointToggleButton.setEnabled(false);
                    ACTION_MOVE_TO_SPAWN.setEnabled(false);
                    biomesPanel.setEnabled(false);
                    biomesCheckBox.setEnabled(false);
                    biomesSoloCheckBox.setEnabled(false);
                    break;
            }
        }
    }

    private void addMaterialSelectionTo(final JToggleButton button, final int customMaterialIndex) {
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopup();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopup();
                }
            }

            private void showPopup() {
                showCustomTerrainButtonPopup(customMaterialIndex);
            }
        });
    }

    private boolean importCustomMaterial(int customMaterialIndex) {
        MixedMaterial customMaterial = MixedMaterialHelper.load(this);
        if (customMaterial != null) {
            customMaterial = MixedMaterialManager.getInstance().register(customMaterial);
            Terrain.setCustomMaterial(customMaterialIndex, customMaterial);
            customMaterialButtons[customMaterialIndex].setIcon(new ImageIcon(customMaterial.getIcon(selectedColourScheme)));
            customMaterialButtons[customMaterialIndex].setToolTipText(MessageFormat.format(strings.getString("customMaterial.0.right.click.to.change"), customMaterial));
            view.refreshTiles();
            return true;
        } else {
            return false;
        }
    }

    private void exportCustomMaterial(int customMaterialIndex) {
        MixedMaterialHelper.save(this, Terrain.getCustomMaterial(customMaterialIndex));
    }

    private void loadCustomTerrains() {
        for (int i = 0; i < Terrain.CUSTOM_TERRAIN_COUNT; i++) {
            MixedMaterial material = world.getMixedMaterial(i);
            Terrain.setCustomMaterial(i, material);
            if (material != null) {
                customMaterialButtons[i].setIcon(new ImageIcon(material.getIcon(selectedColourScheme)));
                customMaterialButtons[i].setToolTipText(MessageFormat.format(strings.getString("customMaterial.0.right.click.to.change"), material));
            }
        }
    }

    private void saveCustomMaterials() {
        for (int i = 0; i < Terrain.CUSTOM_TERRAIN_COUNT; i++) {
            world.setMixedMaterial(i, Terrain.getCustomMaterial(i));
        }
    }

    private void clearCustomTerrains() {
        for (int i = 0; i < Terrain.CUSTOM_TERRAIN_COUNT; i++) {
            if (Terrain.getCustomMaterial(i) != null) {
                Terrain.setCustomMaterial(i, null);
                customMaterialButtons[i].setIcon(ICON_UNKNOWN_PATTERN);
                customMaterialButtons[i].setToolTipText(strings.getString("not.set.click.to.set"));
                if (customMaterialButtons[i].isSelected()) {
                    toolButtonGroup.clearSelection();
                }
            }
        }
    }

    private void saveCustomBiomes() {
        if (dimension != null) {
            dimension.setCustomBiomes(customBiomeManager.getCustomBiomes());
        }
    }
    
    private void installMacCustomisations() {
        hideExit = MacUtils.installQuitHandler(() -> {
            exit();
            // If we get here the user cancelled closing the program
            return false;
        });
        hideAbout = MacUtils.installAboutHandler(() -> {
            AboutDialog dialog = new AboutDialog(App.this, world, view, currentUndoManager);
            dialog.setVisible(true);
        });
        MacUtils.installOpenFilesHandler(files -> {
            if (files.size() > 0) {
                open(files.get(0), true);
            }
        });
        hidePreferences = MacUtils.installPreferencesHandler(() -> {
            PreferencesDialog dialog = new PreferencesDialog(App.this, selectedColourScheme);
            dialog.setVisible(true);
            if (! dialog.isCancelled()) {
                setMaxRadius(Configuration.getInstance().getMaximumBrushSize());
            }
        });
    }
    
    private void showGlobalOperations() {
        Set<Layer> allLayers = getAllLayers();
        List<Integer> allBiomes = new ArrayList<>();
        final int biomeCount = autoBiomeScheme.getBiomeCount();
        for (int biome = 0; biome < biomeCount; biome++) {
            if (autoBiomeScheme.isBiomePresent(biome)) {
                allBiomes.add(biome);
            }
        }
        if (customBiomeManager.getCustomBiomes() != null) {
            allBiomes.addAll(customBiomeManager.getCustomBiomes().stream().map(CustomBiome::getId).collect(Collectors.toList()));
        }
        FillDialog dialog = new FillDialog(App.this, dimension, allLayers.toArray(new Layer[allLayers.size()]), selectedColourScheme, allBiomes.toArray(new Integer[allBiomes.size()]), customBiomeManager, view);
        dialog.setVisible(true);
    }

    private void exportImage() {
        JFileChooser fileChooser = new JFileChooser();
        final Set<String> extensions = new HashSet<>(Arrays.asList(ImageIO.getReaderFileSuffixes()));
        StringBuilder sb = new StringBuilder(strings.getString("supported.image.formats"));
        sb.append(" (");
        boolean first = true;
        for (String extension: extensions) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append("*.");
            sb.append(extension);
        }
        sb.append(')');
        final String description = sb.toString();
        fileChooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                if (f.isDirectory()) {
                    return true;
                }
                String filename = f.getName();
                int p = filename.lastIndexOf('.');
                if (p != -1) {
                    String extension = filename.substring(p + 1).toLowerCase();
                    return extensions.contains(extension);
                } else {
                    return false;
                }
            }

            @Override
            public String getDescription() {
                return description;
            }
        });
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        String defaultname = world.getName().replaceAll("\\s", "").toLowerCase() + ((dimension.getDim() == DIM_NORMAL) ? "" : ("_" + dimension.getName().toLowerCase())) + ".png"; // NOI18N
        fileChooser.setSelectedFile(new File(defaultname));
        if (fileChooser.showSaveDialog(App.this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            final String type;
            int p = selectedFile.getName().lastIndexOf('.');
            if (p != -1) {
                type = selectedFile.getName().substring(p + 1).toUpperCase();
            } else {
                type = "PNG"; // NOI18N
                selectedFile = new File(selectedFile.getParentFile(), selectedFile.getName() + ".png");
            }
            if (selectedFile.exists()) {
                if (JOptionPane.showConfirmDialog(App.this, strings.getString("the.file.already.exists"), strings.getString("overwrite.file"), JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            final File file = selectedFile;
            if (! ProgressDialog.executeTask(App.this, new ProgressTask<Boolean>() {
                        @Override
                        public String getName() {
                            return strings.getString("exporting.image");
                        }

                        @Override
                        public Boolean execute(ProgressReceiver progressReceiver) throws OperationCancelled {
                            // Leave the progress receiver indeterminate, since
                            // by *far* the most time goes into actually writing
                            // the file, and we can't report progress for that
                            try {
                                return ImageIO.write(view.getImage(), type, file);
                            } catch (IOException e) {
                                throw new RuntimeException("I/O error while exporting image", e);
                            }
                        }
                    }, false)) {
                JOptionPane.showMessageDialog(App.this, MessageFormat.format(strings.getString("format.0.not.supported"), type));
            }
        }
    }
    
    private void exportHeightMap() {
        JFileChooser fileChooser = new JFileChooser();
        final Set<String> extensions = new HashSet<>(Arrays.asList(ImageIO.getReaderFileSuffixes()));
        StringBuilder sb = new StringBuilder(strings.getString("supported.image.formats"));
        sb.append(" (");
        boolean first = true;
        for (String extension: extensions) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append("*.");
            sb.append(extension);
        }
        sb.append(')');
        final String description = sb.toString();
        fileChooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                if (f.isDirectory()) {
                    return true;
                }
                String filename = f.getName();
                int p = filename.lastIndexOf('.');
                if (p != -1) {
                    String extension = filename.substring(p + 1).toLowerCase();
                    return extensions.contains(extension);
                } else {
                    return false;
                }
            }

            @Override
            public String getDescription() {
                return description;
            }
        });
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        String defaultname = world.getName().replaceAll("\\s", "").toLowerCase() + ((dimension.getDim() == DIM_NORMAL) ? "" : ("_" + dimension.getName().toLowerCase())) + "_heightmap.png"; // NOI18N
        fileChooser.setSelectedFile(new File(defaultname));
        if (fileChooser.showSaveDialog(App.this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            final String type;
            int p = selectedFile.getName().lastIndexOf('.');
            if (p != -1) {
                type = selectedFile.getName().substring(p + 1).toUpperCase();
            } else {
                type = "PNG"; // NOI18N
                selectedFile = new File(selectedFile.getParentFile(), selectedFile.getName() + ".png");
            }
            if (selectedFile.exists()) {
                if (JOptionPane.showConfirmDialog(App.this, strings.getString("the.file.already.exists"), strings.getString("overwrite.file"), JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            final File file = selectedFile;
            if (! ProgressDialog.executeTask(App.this, new ProgressTask<Boolean>() {
                        @Override
                        public String getName() {
                            return strings.getString("exporting.height.map");
                        }

                        @Override
                        public Boolean execute(ProgressReceiver progressReceiver) throws OperationCancelled {
                            // Leave the progress receiver indeterminate, since
                            // by *far* the most time goes into actually writing
                            // the file, and we can't report progress for that
                            try {
                                BufferedImage image = new BufferedImage(dimension.getWidth() * TILE_SIZE, dimension.getHeight() * TILE_SIZE, (dimension.getMaxHeight() <= 256) ? BufferedImage.TYPE_BYTE_GRAY : BufferedImage.TYPE_USHORT_GRAY);
                                WritableRaster raster = image.getRaster();
                                for (Tile tile: dimension.getTiles()) {
                                    int tileOffsetX = (tile.getX() - dimension.getLowestX()) * TILE_SIZE;
                                    int tileOffsetY = (tile.getY() - dimension.getLowestY()) * TILE_SIZE;
                                    for (int dx = 0; dx < TILE_SIZE; dx++) {
                                        for (int dy = 0; dy < TILE_SIZE; dy++) {
                                            int height = tile.getIntHeight(dx, dy);
                                            raster.setSample(tileOffsetX + dx, tileOffsetY + dy, 0, height);
                                        }
                                    }
                                }
                                return ImageIO.write(image, type, file);
                            } catch (IOException e) {
                                throw new RuntimeException("I/O error while exporting image", e);
                            }
                        }
                    }, false)) {
                JOptionPane.showMessageDialog(App.this, MessageFormat.format(strings.getString("format.0.not.supported"), type));
            }
        }
    }
    
    private void importLayers(String paletteName) {
        Configuration config = Configuration.getInstance();
        File layerDirectory = config.getLayerDirectory();
        if ((layerDirectory == null) || (! layerDirectory.isDirectory())) {
            layerDirectory = DesktopUtils.getDocumentsFolder();
        }
        JFileChooser fileChooser = new JFileChooser(layerDirectory);
        fileChooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".layer");
            }

            @Override
            public String getDescription() {
                return "WorldPainter Custom Layers (*.layer)";
            }
        });
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setMultiSelectionEnabled(true);
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File[] selectedFiles = fileChooser.getSelectedFiles();
            boolean updateCustomTerrainButtons = false;
            for (File selectedFile: selectedFiles) {
                try {
                    try (ObjectInputStream in = new ObjectInputStream(new GZIPInputStream(new BufferedInputStream(new FileInputStream(selectedFile))))) {
                        CustomLayer layer = (CustomLayer) in.readObject();
                        for (Layer existingLayer : getCustomLayers()) {
                            if (layer.equals(existingLayer)) {
                                JOptionPane.showMessageDialog(this, "That layer is already present in the dimension.\nThe layer has not been added.", "Layer Already Present", JOptionPane.WARNING_MESSAGE);
                                return;
                            }
                        }
                        if (paletteName != null) {
                            layer.setPalette(paletteName);
                        }
                        registerCustomLayer(layer, true);
                        if (layer instanceof CombinedLayer) {
                            CombinedLayer combinedLayer = (CombinedLayer) layer;
                            addLayersFromCombinedLayer(combinedLayer);
                            if (combinedLayer.isMissingTerrainWarning()) {
                                JOptionPane.showMessageDialog(this, "The layer contained a Custom Terrain which is not present in this world. The terrain has been reset.", "Missing Custom Terrain", JOptionPane.WARNING_MESSAGE);
                                combinedLayer.resetMissingTerrainWarning();
                            } else if ((combinedLayer.getTerrain() != null) && combinedLayer.getTerrain().isCustom()) {
                                updateCustomTerrainButtons = true;
                            }
                        }
                    }
                } catch (FileNotFoundException e) {
                    logger.log(Level.SEVERE, "File not found while loading file " + selectedFile, e);
                    JOptionPane.showMessageDialog(this, "The specified path does not exist or is not a file", "Nonexistent File", JOptionPane.ERROR_MESSAGE);
                    return;
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "I/O error while loading file " + selectedFile, e);
                    JOptionPane.showMessageDialog(this, "I/O error occurred while reading the specified file,\nor is not a (valid) WorldPainter layer file", "I/O Error Or Invalid File", JOptionPane.ERROR_MESSAGE);
                    return;
                } catch (ClassNotFoundException e) {
                    logger.log(Level.SEVERE, "Class not found exception while loading file " + selectedFile, e);
                    JOptionPane.showMessageDialog(this, "The specified file is not a (valid) WorldPainter layer file", "Invalid File", JOptionPane.ERROR_MESSAGE);
                    return;
                } catch (ClassCastException e) {
                    logger.log(Level.SEVERE, "Class cast exception while loading file " + selectedFile, e);
                    JOptionPane.showMessageDialog(this, "The specified file is not a (valid) WorldPainter layer file", "Invalid File", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            if (updateCustomTerrainButtons) {
                updateCustomTerrainButtons();
            }
        }        
    }
    
    private void updateCustomTerrainButtons() {
        for (int i = 0; i < Terrain.CUSTOM_TERRAIN_COUNT; i++) {
            if (Terrain.getCustomMaterial(i) != null) {
                MixedMaterial material = Terrain.getCustomMaterial(i);
                customMaterialButtons[i].setIcon(new ImageIcon(material.getIcon(selectedColourScheme)));
                customMaterialButtons[i].setToolTipText(MessageFormat.format(strings.getString("customMaterial.0.right.click.to.change"), material));
            } else {
                customMaterialButtons[i].setIcon(ICON_UNKNOWN_PATTERN);
                customMaterialButtons[i].setToolTipText(strings.getString("not.set.click.to.set"));
            }
        }
    }
    
    private void addLayersFromCombinedLayer(CombinedLayer combinedLayer) {
        combinedLayer.getLayers().stream().filter(layer -> (layer instanceof CustomLayer) && (!paletteManager.contains(layer)) && (!layersWithNoButton.contains(layer))).forEach(layer -> {
            CustomLayer customLayer = (CustomLayer) layer;
            if (customLayer.isHide()) {
                layersWithNoButton.add(customLayer);
            } else {
                registerCustomLayer((CustomLayer) customLayer, false);
            }
            if (layer instanceof CombinedLayer) {
                addLayersFromCombinedLayer((CombinedLayer) customLayer);
            }
        });
    }
    
    private void exportLayer(CustomLayer layer) {
        Configuration config = Configuration.getInstance();
        File layerDirectory = config.getLayerDirectory();
        if ((layerDirectory == null) || (! layerDirectory.isDirectory())) {
            layerDirectory = DesktopUtils.getDocumentsFolder();
        }
        JFileChooser fileChooser = new JFileChooser(layerDirectory);
        fileChooser.setSelectedFile(new File(layerDirectory, FileUtils.sanitiseName(layer.getName()) + ".layer"));
        fileChooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".layer");
            }

            @Override
            public String getDescription() {
                return "WorldPainter Custom Layers (*.layer)";
            }
        });
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            if (! selectedFile.getName().toLowerCase().endsWith(".layer")) {
                selectedFile = new File(selectedFile.getPath() + ".layer");
            }
            if (selectedFile.isFile() && (JOptionPane.showConfirmDialog(this, "The file " + selectedFile.getName() + " already exists.\nDo you want to overwrite it?", "Overwrite File", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION)) {
                return;
            }
            try {
                try (ObjectOutputStream out = new ObjectOutputStream(new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(selectedFile))))) {
                    out.writeObject(layer);
                }
            } catch (IOException e) {
                throw new RuntimeException("I/O error while trying to write " + selectedFile, e);
            }
            config.setLayerDirectory(selectedFile.getParentFile());
            JOptionPane.showMessageDialog(this, "Layer " + layer.getName() + " exported successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void logLayers(Dimension dimension, EventVO event, @NonNls String prefix) {
        StringBuilder sb = new StringBuilder();
        for (Layer layer: dimension.getAllLayers(false)) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(layer.getName());
        }
        if (sb.length() > 0) {
            event.setAttribute(new AttributeKeyVO<>(prefix + "layers"), sb.toString());
        }
    }

    private Icon createColourIcon(int colour) {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                image.setRGB(x, y, colour);
            }
        }
        return new ImageIcon(image);
    }

    static DockableFrame createDockableFrame(Component component, String title, int side, int index) {
        String id = Character.toLowerCase(title.charAt(0)) + title.substring(1);
        return createDockableFrame(component, id, title, side, index);
    }
    
    static DockableFrame createDockableFrame(Component component, String id, String title, int side, int index) {
        DockableFrame dockableFrame = new DockableFrame(id);
        
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(component, constraints);
        constraints.weighty = 1.0;
        panel.add(new JPanel(), constraints);
        dockableFrame.add(panel, BorderLayout.CENTER);
        
        // Use title everywhere
        dockableFrame.setTitle(title);
        dockableFrame.setSideTitle(title);
        dockableFrame.setTabTitle(title);
        dockableFrame.setToolTipText(title);

        // Try to find an icon to use for the tab
        if (component instanceof Container) {
            Icon icon = findIcon((Container) component);
            if (icon != null) {
                if (((icon.getIconHeight() > 16) || (icon.getIconWidth() > 16))
                        && (icon instanceof ImageIcon)
                        && (((ImageIcon) icon).getImage() instanceof BufferedImage)) {
                    float s;
                    if (icon.getIconWidth() > icon.getIconHeight()) {
                        // Wide icon
                        s = 16f / icon.getIconWidth();
                    } else {
                        // Tall (or square) icon
                        s = 16f / icon.getIconHeight();
                    }
                    BufferedImageOp op = new AffineTransformOp(AffineTransform.getScaleInstance(s, s), AffineTransformOp.TYPE_BICUBIC);
                    BufferedImage iconImage = op.filter((BufferedImage) ((ImageIcon) icon).getImage(), null);
                    icon = new ImageIcon(iconImage);
                }
                dockableFrame.setFrameIcon(icon);
            }
        }

        // Use preferred size of component as much as possible
        final java.awt.Dimension preferredSize = component.getPreferredSize();
        dockableFrame.setAutohideWidth(preferredSize.width);
        dockableFrame.setDockedWidth(preferredSize.width);
        dockableFrame.setDockedHeight(preferredSize.height);
        dockableFrame.setUndockedBounds(new Rectangle(-1, -1, preferredSize.width, preferredSize.height));

        // Make hidable, but don't display hide button, so incidental panels can
        // be hidden on the fly
        dockableFrame.setHidable(true);
        dockableFrame.setAvailableButtons(BUTTON_FLOATING | BUTTON_AUTOHIDE | BUTTON_HIDE_AUTOHIDE);
        dockableFrame.setShowContextMenu(false); // Disable the context menu because it contains the Close option with no way to hide it

        // Initial location of panel
        dockableFrame.setInitMode(DockContext.STATE_FRAMEDOCKED);
        dockableFrame.setInitSide(side);
        dockableFrame.setInitIndex(index);

        // Other flags
        dockableFrame.setAutohideWhenActive(true);
        dockableFrame.setMaximizable(false);
        return dockableFrame;
    }

    static Icon findIcon(Container container) {
        for (Component component: container.getComponents()) {
            if ((component instanceof AbstractButton) && (((AbstractButton) component).getIcon() != null)) {
                return ((AbstractButton) component).getIcon();
            } else if (component instanceof Container) {
                Icon icon = findIcon((Container) component);
                if (icon != null) {
                    return icon;
                }
            }
        }
        return null;
    }

    public final IntensityAction ACTION_INTENSITY_10_PERCENT  = new IntensityAction(  2, VK_1); // 9 so that it will round to level 1 for nibble sized layers
    public final IntensityAction ACTION_INTENSITY_20_PERCENT  = new IntensityAction( 16, VK_2);
    public final IntensityAction ACTION_INTENSITY_30_PERCENT  = new IntensityAction( 23, VK_3);
    public final IntensityAction ACTION_INTENSITY_40_PERCENT  = new IntensityAction( 37, VK_4);
    public final IntensityAction ACTION_INTENSITY_50_PERCENT  = new IntensityAction( 51, VK_5);
    public final IntensityAction ACTION_INTENSITY_60_PERCENT  = new IntensityAction( 58, VK_6);
    public final IntensityAction ACTION_INTENSITY_70_PERCENT  = new IntensityAction( 65, VK_7);
    public final IntensityAction ACTION_INTENSITY_80_PERCENT  = new IntensityAction( 79, VK_8);
    public final IntensityAction ACTION_INTENSITY_90_PERCENT  = new IntensityAction( 86, VK_9);
    public final IntensityAction ACTION_INTENSITY_100_PERCENT = new IntensityAction(100, VK_0);
    
    private final BetterAction ACTION_NEW_WORLD = new BetterAction("newWorld", strings.getString("new.world") + "...", ICON_NEW_WORLD, false) {
        {
            setAcceleratorKey(KeyStroke.getKeyStroke(VK_N, PLATFORM_COMMAND_MASK));
            setShortDescription(strings.getString("create.a.new.world"));
        }
        
        @Override
        public void performAction(ActionEvent e) {
            newWorld();
        }

        private static final long serialVersionUID = 1L;
    };

    private final BetterAction ACTION_OPEN_WORLD = new BetterAction("openWorld", strings.getString("open.world") + "...", ICON_OPEN_WORLD, false) {
        {
            setAcceleratorKey(KeyStroke.getKeyStroke(VK_O, PLATFORM_COMMAND_MASK));
            setShortDescription(strings.getString("open.an.existing.worldpainter.world"));
        }
        
        @Override
        public void performAction(ActionEvent e) {
            open();
        }

        private static final long serialVersionUID = 1L;
    };
    
    private final BetterAction ACTION_SAVE_WORLD = new BetterAction("saveWorld", strings.getString("save.world") + "...", ICON_SAVE_WORLD, false) {
        {
            setAcceleratorKey(KeyStroke.getKeyStroke(VK_S, PLATFORM_COMMAND_MASK));
            setShortDescription(strings.getString("save.the.world.as.a.worldpainter.file.to.the.previously.used.file"));
        }
        
        @Override
        public void performAction(ActionEvent e) {
            save();
        }

        private static final long serialVersionUID = 1L;
    };

    private final BetterAction ACTION_SAVE_WORLD_AS = new BetterAction("saveWorldAs", strings.getString("save.world.as") + "...", ICON_SAVE_WORLD, false) {
        {
            setAcceleratorKey(KeyStroke.getKeyStroke(VK_S, PLATFORM_COMMAND_MASK | SHIFT_DOWN_MASK));
            setShortDescription(strings.getString("save.the.world.as.a.worldpainter.file"));
        }
        
        @Override
        public void performAction(ActionEvent e) {
            saveAs();
        }

        private static final long serialVersionUID = 1L;
    };
    
    private final BetterAction ACTION_EXPORT_WORLD = new BetterAction("exportAsMinecraftMap", strings.getString("export.as.minecraft.map") + "...", ICON_EXPORT_WORLD, false) {
        {
            setAcceleratorKey(KeyStroke.getKeyStroke(VK_E, PLATFORM_COMMAND_MASK));
            setShortDescription(strings.getString("export.the.world.to.a.minecraft.map"));
        }
        
        @Override
        public void performAction(ActionEvent e) {
            if (world.getImportedFrom() != null) {
                Toolkit.getDefaultToolkit().beep();
                if (JOptionPane.showConfirmDialog(App.this, strings.getString("this.is.an.imported.world"), strings.getString("imported"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            ExportWorldDialog dialog = new ExportWorldDialog(App.this, world, autoBiomeScheme, selectedColourScheme, customBiomeManager, hiddenLayers, false, 10, view.getLightOrigin(), view);
            dialog.setVisible(true);
        }

        private static final long serialVersionUID = 1L;
    };
    
    private final BetterAction ACTION_IMPORT_MAP = new BetterAction("importMinecraftMap", strings.getString("existing.minecraft.map") + "...", false) {
        {
            setAcceleratorKey(KeyStroke.getKeyStroke(VK_I, PLATFORM_COMMAND_MASK));
            setShortDescription("Import the landscape of an existing Minecraft map. Use Merge to merge your changes.");
        }

        @Override
        public void performAction(ActionEvent e) {
            importWorld();
        }

        private static final long serialVersionUID = 1L;
    };

    private final BetterAction ACTION_MERGE_WORLD = new BetterAction("mergeWorld", strings.getString("merge.world") + "...", false) {
        {
            setAcceleratorKey(KeyStroke.getKeyStroke(VK_R, PLATFORM_COMMAND_MASK));
            setShortDescription("Merge the changes in a previously Imported world back to the original Minecraft map.");
        }

        @Override
        public void performAction(ActionEvent e) {
            merge();
        }

        private static final long serialVersionUID = 1L;
    };

    private final BetterAction ACTION_EXIT = new BetterAction("exit", strings.getString("exit") + "...", ICON_EXIT) {
        {
            setShortDescription(strings.getString("shut.down.worldpainter"));
        }
        
        @Override
        public void performAction(ActionEvent e) {
            exit();
        }

        private static final long serialVersionUID = 1L;
    };

    private final BetterAction ACTION_ZOOM_IN = new BetterAction("zoomIn", strings.getString("zoom.in"), ICON_ZOOM_IN) {
        {
            setAcceleratorKey(KeyStroke.getKeyStroke(VK_ADD, PLATFORM_COMMAND_MASK));
            setShortDescription(strings.getString("zoom.in"));
        }
        
        @Override
        public void performAction(ActionEvent e) {
//            Point location = view.getViewCentreInWorldCoords();
            zoom++;
            view.setZoom(zoom);
            updateZoomLabel();
//            view.moveTo(location);
            if (zoom == 6) {
                setEnabled(false);
            }
            ACTION_ZOOM_OUT.setEnabled(true);
            ACTION_ZOOM_RESET.setEnabled(zoom != 0);
        }

        private static final long serialVersionUID = 1L;
    };

    private final BetterAction ACTION_ZOOM_RESET = new BetterAction("resetZoom", strings.getString("reset.zoom"), ICON_ZOOM_RESET) {
        {
            setAcceleratorKey(KeyStroke.getKeyStroke(VK_0, PLATFORM_COMMAND_MASK));
            setShortDescription(strings.getString("reset.the.zoom.level.to.1.1"));
            setEnabled(false);
        }
        
        @Override
        public void performAction(ActionEvent e) {
//            int oldZoom = zoom;
            zoom = 0;
            view.setZoom(zoom);
            updateZoomLabel();
//            Point mousePosition = view.getMousePosition();
//            if (mousePosition != null) {
//                // TODO: this algorithm causes flashing, because the scrollpane already
//                // repaints itself before it is scrolled. See if we can do something
//                // about that
//                double scale = Math.pow(2.0, zoom - oldZoom);
//                final int horizontalDisplacement = (int) (mousePosition.x * scale) - mousePosition.x;
//                final int verticalDisplacement = (int) (mousePosition.y * scale) - mousePosition.y;
//                // Schedule this for later execution, after the scrollpane
//                // has revalidated its contents and resized the scrollbars:
//                SwingUtilities.invokeLater(new Runnable() {
//                    @Override
//                    public void run() {
//                        // TODO reintroduce scrolling
////                        scroll(scrollPane.getHorizontalScrollBar(), horizontalDisplacement);
////                        scroll(scrollPane.getVerticalScrollBar(), verticalDisplacement);
//                        view.refreshBrush();
//                    }
//                });
//            }
            ACTION_ZOOM_IN.setEnabled(true);
            ACTION_ZOOM_OUT.setEnabled(true);
            setEnabled(false);
        }

        private static final long serialVersionUID = 1L;
    };
    
    private final BetterAction ACTION_ZOOM_OUT = new BetterAction("zoomOut", strings.getString("zoom.out"), ICON_ZOOM_OUT) {
        {
            setAcceleratorKey(KeyStroke.getKeyStroke(VK_SUBTRACT, PLATFORM_COMMAND_MASK));
            setShortDescription(strings.getString("zoom.out"));
        }
        
        @Override
        public void performAction(ActionEvent e) {
//            Point location = view.getViewCentreInWorldCoords();
            zoom--;
            view.setZoom(zoom);
            updateZoomLabel();
//            view.moveTo(location);
            if (zoom == -4) {
                setEnabled(false);
            }
            ACTION_ZOOM_IN.setEnabled(true);
            ACTION_ZOOM_RESET.setEnabled(zoom != 0);
        }

        private static final long serialVersionUID = 1L;
    };

    private final BetterAction ACTION_GRID = new BetterAction("grid", strings.getString("grid"), ICON_GRID) {
        {
            setShortDescription(strings.getString("enable.or.disable.the.grid"));
            setSelected(false);
        }
        
        @Override
        public void performAction(ActionEvent e) {
            view.setPaintGrid(!view.isPaintGrid());
            dimension.setGridEnabled(view.isPaintGrid());
            setSelected(view.isPaintGrid());
        }

        private static final long serialVersionUID = 1L;
    };
    
    private final BetterAction ACTION_CONTOURS = new BetterAction("contours", strings.getString("contours"), ICON_CONTOURS) {
        {
            setShortDescription(strings.getString("enable.or.disable.height.contours"));
            setSelected(false);
        }
        
        @Override
        public void performAction(ActionEvent e) {
            view.setDrawContours(!view.isDrawContours());
            dimension.setContoursEnabled(view.isDrawContours());
            setSelected(view.isDrawContours());
        }

        private static final long serialVersionUID = 1L;
    };
    
    private final BetterAction ACTION_OVERLAY = new BetterAction("overlay", strings.getString("overlay"), ICON_OVERLAY) {
        {
            setShortDescription(strings.getString("enable.or.disable.image.overlay"));
        }

        @Override
        public void performAction(ActionEvent e) {
            if (view.isDrawOverlay()) {
                // An overlay is showing; disable it
                view.setDrawOverlay(false);
                dimension.setOverlayEnabled(false);
                setSelected(false);
            } else if ((dimension.getOverlay() != null) && dimension.getOverlay().isFile()) {
                // No overlay is being shown, but there is one configured and it can be found, so enable it
                view.setDrawOverlay(true); // This will cause the overlay configured in the dimension to be loaded
                dimension.setOverlayEnabled(true);
                setSelected(true);
            } else {
                // Otherwise show the configure view dialog so the user can (re)configure an overlay
                ConfigureViewDialog dialog = new ConfigureViewDialog(App.this, dimension, view, true);
                dialog.setVisible(true);
                setSelected(view.isDrawOverlay());
                ACTION_GRID.setSelected(view.isPaintGrid());
                ACTION_CONTOURS.setSelected(view.isDrawContours());
            }
        }
        private static final long serialVersionUID = 1L;
    };
    
    private final BetterAction ACTION_UNDO = new BetterAction("undo", strings.getString("undo"), ICON_UNDO) {
        {
            setAcceleratorKey(KeyStroke.getKeyStroke(VK_Z, PLATFORM_COMMAND_MASK));
            setShortDescription(strings.getString("undo.the.most.recent.action"));
        }
        
        @Override
        public void performAction(ActionEvent e) {
            if ((currentUndoManager != null) && currentUndoManager.undo()) {
                currentUndoManager.armSavePoint();
            } else {
                Toolkit.getDefaultToolkit().beep();
            }
        }

        private static final long serialVersionUID = 1L;
    };
    
    private final BetterAction ACTION_REDO = new BetterAction("redo", strings.getString("redo"), ICON_REDO) {
        {
            setAcceleratorKey(KeyStroke.getKeyStroke(VK_Y, PLATFORM_COMMAND_MASK));
            setShortDescription(strings.getString("redo.the.most.recent.action"));
        }
        
        @Override
        public void performAction(ActionEvent e) {
            if ((currentUndoManager != null) && currentUndoManager.redo()) {
                currentUndoManager.armSavePoint();
            } else {
                Toolkit.getDefaultToolkit().beep();
            }
        }

        private static final long serialVersionUID = 1L;
    };
    
    private final BetterAction ACTION_EDIT_TILES = new BetterAction("editTiles", strings.getString("add.remove.tiles") + "...", ICON_EDIT_TILES) {
        {
            setAcceleratorKey(KeyStroke.getKeyStroke(VK_T, PLATFORM_COMMAND_MASK));
            setShortDescription(strings.getString("add.or.remove.tiles"));
        }
        
        @Override
        public void performAction(ActionEvent e) {
            addRemoveTiles();
        }

        private static final long serialVersionUID = 1L;
    };
    
    private final BetterAction ACTION_CHANGE_HEIGHT = new BetterAction("changeHeight", strings.getString("change.height") + "...", ICON_CHANGE_HEIGHT) {
        {
            setShortDescription(strings.getString("raise.or.lower.the.entire.map"));
        }
        
        @Override
        public void performAction(ActionEvent e) {
            ChangeHeightDialog dialog = new ChangeHeightDialog(App.this, world);
            dialog.setVisible(true);
            if (threeDeeFrame != null) {
                threeDeeFrame.refresh();
            }
        }

        private static final long serialVersionUID = 1L;
    };
    
    private final BetterAction ACTION_ROTATE_WORLD = new BetterAction("rotate", strings.getString("rotate") + "...", ICON_ROTATE_WORLD) {
        {
            setShortDescription(strings.getString("rotate.the.entire.map.by.quarter.turns"));
        }
        
        @Override
        public void performAction(ActionEvent e) {
            if ((world.getImportedFrom() != null) && (JOptionPane.showConfirmDialog(App.this, strings.getString("this.world.was.imported.from.an.existing.map"), strings.getString("imported"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION)) {
                return;
            }
            RotateWorldDialog dialog = new RotateWorldDialog(App.this, world, dimension.getDim());
            dialog.setVisible(true);
            if (! dialog.isCancelled()) {
                currentUndoManager.armSavePoint();
            }
        }

        private static final long serialVersionUID = 1L;
    };
    
    private final BetterAction ACTION_SHIFT_WORLD = new BetterAction("shift", "Shift...", ICON_SHIFT_WORLD) {
        {
            setShortDescription("Shift the entire map horizontally by whole 128-block tiles");
        }

        @Override
        public void performAction(ActionEvent e) {
            if ((world.getImportedFrom() != null) && (JOptionPane.showConfirmDialog(App.this, "This world was imported from an existing map!\nIf you shift it you will no longer be able to merge it properly.\nAre you sure you want to shift the world?", strings.getString("imported"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION)) {
                return;
            }
            ShiftWorldDialog dialog = new ShiftWorldDialog(App.this, world, dimension.getDim());
            dialog.setVisible(true);
            if (! dialog.isCancelled()) {
                currentUndoManager.armSavePoint();
            }
        }

        private static final long serialVersionUID = 1L;
    };

    private final BetterAction ACTION_DIMENSION_PROPERTIES = new BetterAction("dimensionProperties", strings.getString("dimension.properties") + "...", ICON_DIMENSION_PROPERTIES) {
        {
            setAcceleratorKey(KeyStroke.getKeyStroke(VK_P, PLATFORM_COMMAND_MASK));
            setShortDescription(strings.getString("edit.the.properties.of.this.dimension"));
        }
        
        @Override
        public void performAction(ActionEvent e) {
            boolean previousCoverSteepTerrain = dimension.isCoverSteepTerrain();
            int previousTopLayerMinDepth = dimension.getTopLayerMinDepth();
            int previousTopLayerVariation = dimension.getTopLayerVariation();
            Dimension.Border previousBorder = dimension.getBorder();
            int previousBorderSize = dimension.getBorderSize();
            long previousMinecraftSeed = dimension.getMinecraftSeed();
            int previousCeilingHeight = dimension.getCeilingHeight();
            boolean previousBedrockWall = dimension.isBedrockWall();
            DimensionPropertiesDialog dialog = new DimensionPropertiesDialog(App.this, dimension, selectedColourScheme);
            dialog.setVisible(true);
            if ((dimension.isCoverSteepTerrain() != previousCoverSteepTerrain)
                    || (dimension.getTopLayerMinDepth() != previousTopLayerMinDepth)
                    || (dimension.getTopLayerVariation() != previousTopLayerVariation)) {
                if (threeDeeFrame != null) {
                    threeDeeFrame.refresh();
                }
            }
            if ((dimension.getBorder() != previousBorder)
                    || ((dimension.getBorder() != null) && (dimension.getBorderSize() != previousBorderSize))
                    || (dimension.getMinecraftSeed() != previousMinecraftSeed)
                    || (dimension.getCeilingHeight() != previousCeilingHeight)
                    || (dimension.isBedrockWall() != previousBedrockWall)) {
                view.refreshTiles();
            }
        }

        private static final long serialVersionUID = 1L;
    };
    
    private final BetterAction ACTION_VIEW_DISTANCE = new BetterAction("viewDistance", strings.getString("view.distance"), ICON_VIEW_DISTANCE) {
        {
            setShortDescription(strings.getString("enable.or.disable.showing.the.maximum.far.view.distance"));
            setSelected(false);
        }
        
        @Override
        public void performAction(ActionEvent e) {
            view.setDrawViewDistance(!view.isDrawViewDistance());
            setSelected(view.isDrawViewDistance());
        }

        private static final long serialVersionUID = 1L;
    };

    private final BetterAction ACTION_WALKING_DISTANCE = new BetterAction("walkingDistances", strings.getString("walking.distances"), ICON_WALKING_DISTANCE) {
        {
            setShortDescription(strings.getString("enable.or.disable.showing.the.walking.distances"));
            setSelected(false);
        }
        
        @Override
        public void performAction(ActionEvent e) {
            view.setDrawWalkingDistance(!view.isDrawWalkingDistance());
            setSelected(view.isDrawWalkingDistance());
        }

        private static final long serialVersionUID = 1L;
    };

    private final BetterAction ACTION_ROTATE_LIGHT_RIGHT = new BetterAction("rotateLightClockwise", strings.getString("rotate.light.clockwise"), ICON_ROTATE_LIGHT_RIGHT) {
        {
            setAcceleratorKey(KeyStroke.getKeyStroke(VK_R, 0));
            setShortDescription(strings.getString("rotate.the.direction.the.light.comes.from.clockwise"));
        }
        
        @Override
        public void performAction(ActionEvent e) {
            view.rotateLightRight();
        }

        private static final long serialVersionUID = 1L;
    };
    
    private final BetterAction ACTION_ROTATE_LIGHT_LEFT = new BetterAction("rotateLightAnticlockwise", strings.getString("rotate.light.anticlockwise"), ICON_ROTATE_LIGHT_LEFT) {
        {
            setAcceleratorKey(KeyStroke.getKeyStroke(VK_L, 0));
            setShortDescription(strings.getString("rotate.the.direction.the.light.comes.from.anticlockwise"));
        }
        
        @Override
        public void performAction(ActionEvent e) {
            view.rotateLightLeft();
        }

        private static final long serialVersionUID = 1L;
    };
    
    private final BetterAction ACTION_MOVE_TO_SPAWN = new BetterAction("moveToSpawn", strings.getString("move.to.spawn"), ICON_MOVE_TO_SPAWN) {
        {
            setShortDescription(strings.getString("move.the.view.to.the.spawn.point"));
        }
        
        @Override
        public void performAction(ActionEvent e) {
            view.moveToSpawn();
        }

        private static final long serialVersionUID = 1L;
    };
    
    private final BetterAction ACTION_MOVE_TO_ORIGIN = new BetterAction("moveToOrigin", strings.getString("move.to.origin"), ICON_MOVE_TO_ORIGIN) {
        {
            setShortDescription(strings.getString("move.the.view.to.the.origin.coordinates.0.0"));
        }
        
        @Override
        public void performAction(ActionEvent e) {
            view.moveToOrigin();
        }

        private static final long serialVersionUID = 1L;
    };
    
    private final BetterAction ACTION_OPEN_DOCUMENTATION = new BetterAction("browseDocumentation", strings.getString("browse.documentation")) {
        {
            setAcceleratorKey(KeyStroke.getKeyStroke(VK_F1, 0));
        }
        
        @Override
        public void performAction(ActionEvent event) {
            try {
                DesktopUtils.open(new URL("http://www.worldpainter.net/trac/wiki/Documentation/"));
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        private static final long serialVersionUID = 1L;
    };
    
    private final BetterAction ACTION_IMPORT_LAYER = new BetterAction("importLayer", "Import custom layer(s)") {
        @Override
        protected void performAction(ActionEvent e) {
            importLayers(null);
        }

        private static final long serialVersionUID = 1L;
    };
    
    private final BetterAction ACTION_ROTATE_BRUSH_LEFT = new BetterAction("rotateBrushLeft", "Rotate brush counterclockwise fifteen degrees") {
        @Override
        protected void performAction(ActionEvent e) {
            int rotation = brushRotationSlider.getValue() - 15;
            if (rotation < -180) {
                rotation += 360;
            }
            brushRotationSlider.setValue(rotation);
            if (activeOperation instanceof PaintOperation) {
                brushRotation = rotation;
            } else {
                toolBrushRotation = rotation;
            }
            updateBrushRotation();
        }
    };
    
    private final BetterAction ACTION_ROTATE_BRUSH_RIGHT = new BetterAction("rotateBrushRight", "Rotate brush clockwise fifteen degrees") {
        @Override
        protected void performAction(ActionEvent e) {
            int rotation = brushRotationSlider.getValue() + 15;
            if (rotation > 180) {
                rotation -= 360;
            }
            brushRotationSlider.setValue(rotation);
            if (activeOperation instanceof PaintOperation) {
                brushRotation = rotation;
            } else {
                toolBrushRotation = rotation;
            }
            updateBrushRotation();
        }
    };
    
    private final BetterAction ACTION_ROTATE_BRUSH_RESET = new BetterAction("rotateBrushReset", "Reset brush rotation to zero degrees") {
        @Override
        protected void performAction(ActionEvent e) {
            if (brushRotationSlider.getValue() != 0) {
                brushRotationSlider.setValue(0);
                if (activeOperation instanceof PaintOperation) {
                    brushRotation = 0;
                } else {
                    toolBrushRotation = 0;
                }
                updateBrushRotation();
            }
        }
    };

    private final BetterAction ACTION_ROTATE_BRUSH_RIGHT_30_DEGREES = new BetterAction("rotateBrushRight30Degrees", "Rotate brush clockwise 30 degrees") {
        @Override
        protected void performAction(ActionEvent e) {
            int rotation = brushRotationSlider.getValue() + 30;
            if (rotation > 180) {
                rotation -= 360;
            }
            brushRotationSlider.setValue(rotation);
            if (activeOperation instanceof PaintOperation) {
                brushRotation = rotation;
            } else {
                toolBrushRotation = rotation;
            }
            updateBrushRotation();
        }
    };

    private final BetterAction ACTION_ROTATE_BRUSH_RIGHT_45_DEGREES = new BetterAction("rotateBrushRight45Degrees", "Rotate brush clockwise 45 degrees") {
        @Override
        protected void performAction(ActionEvent e) {
            int rotation = brushRotationSlider.getValue() + 45;
            if (rotation > 180) {
                rotation -= 360;
            }
            brushRotationSlider.setValue(rotation);
            if (activeOperation instanceof PaintOperation) {
                brushRotation = rotation;
            } else {
                toolBrushRotation = rotation;
            }
            updateBrushRotation();
        }
    };
    
    private final BetterAction ACTION_ROTATE_BRUSH_RIGHT_90_DEGREES = new BetterAction("rotateBrushRight90Degrees", "Rotate brush clockwise 90 degrees") {
        @Override
        protected void performAction(ActionEvent e) {
            int rotation = brushRotationSlider.getValue() + 90;
            if (rotation > 180) {
                rotation -= 360;
            }
            brushRotationSlider.setValue(rotation);
            if (activeOperation instanceof PaintOperation) {
                brushRotation = rotation;
            } else {
                toolBrushRotation = rotation;
            }
            updateBrushRotation();
        }
    };
    
    private final BetterAction ACTION_RESET_DOCKS = new BetterAction("resetDockLayout", "Reset workspace layout") {
        @Override
        protected void performAction(ActionEvent e) {
            dockingManager.resetToDefault();
            Configuration config = Configuration.getInstance();
            config.setDefaultJideLayoutData(null);
            ACTION_LOAD_LAYOUT.setEnabled(false);
        }
    };

    private final BetterAction ACTION_LOAD_LAYOUT = new BetterAction("loadDockLayout", "Load workspace layout") {
        @Override
        protected void performAction(ActionEvent e) {
            Configuration config = Configuration.getInstance();
            if (config.getDefaultJideLayoutData() != null) {
                dockingManager.loadLayoutFrom(new ByteArrayInputStream(config.getDefaultJideLayoutData()));
            }
        }
    };

    private final BetterAction ACTION_SAVE_LAYOUT = new BetterAction("resetDocks", "Save workspace layout") {
        @Override
        protected void performAction(ActionEvent e) {
            Configuration config = Configuration.getInstance();
            config.setDefaultJideLayoutData(dockingManager.getLayoutRawData());
            ACTION_LOAD_LAYOUT.setEnabled(true);
            JOptionPane.showMessageDialog(App.this, "Workspace layout saved", "Workspace layout saved", JOptionPane.INFORMATION_MESSAGE);
        }
    };

    private final BetterAction ACTION_SWITCH_TO_FROM_CEILING = new BetterAction("switchCeiling", "Switch to/from Ceiling") {
        {
            setAcceleratorKey(KeyStroke.getKeyStroke(VK_C, PLATFORM_COMMAND_MASK));
        }

        @Override
        public void performAction(ActionEvent e) {
            if ((dimension != null) && (world != null)) {
                Dimension oppositeDimension = null;
                switch (dimension.getDim()) {
                    case DIM_NORMAL:
                        oppositeDimension = world.getDimension(DIM_NORMAL_CEILING);
                        break;
                    case DIM_NORMAL_CEILING:
                        oppositeDimension = world.getDimension(DIM_NORMAL);
                        break;
                    case DIM_NETHER:
                        oppositeDimension = world.getDimension(DIM_NETHER_CEILING);
                        break;
                    case DIM_NETHER_CEILING:
                        oppositeDimension = world.getDimension(DIM_NETHER);
                        break;
                    case DIM_END:
                        oppositeDimension = world.getDimension(DIM_END_CEILING);
                        break;
                    case DIM_END_CEILING:
                        oppositeDimension = world.getDimension(DIM_END);
                        break;
                }
                if (oppositeDimension != null) {
                    setDimension(oppositeDimension);
                    return;
                }
            }
            Toolkit.getDefaultToolkit().beep();
        }

        private static final long serialVersionUID = 1L;
    };

    private World2 world;
    private Dimension dimension;
    private WorldPainter view;
    private Operation activeOperation;
    private File lastSelectedFile;
    private JLabel heightLabel, locationLabel, waterLabel, materialLabel, radiusLabel, zoomLabel, biomeLabel, levelLabel, brushRotationLabel;
    private int radius = 50;
    private final ButtonGroup toolButtonGroup = new ButtonGroup(), brushButtonGroup = new ButtonGroup(), paintButtonGroup = new ButtonGroup();
    private Brush brush = SymmetricBrush.PLATEAU_CIRCLE, toolBrush = SymmetricBrush.COSINE_CIRCLE;
    private final Map<Brush, JToggleButton> brushButtons = new HashMap<>();
    private boolean programmaticChange;
    private UndoManager currentUndoManager;
    private final Map<Integer, UndoManager> undoManagers = new HashMap<>();
    private JSlider levelSlider, brushRotationSlider;
    private float level = 0.51f, toolLevel = 0.51f;
    private Set<Layer> hiddenLayers = new HashSet<>();
    private int zoom = 0, maxRadius = DEFAULT_MAX_RADIUS, brushRotation = 0, toolBrushRotation = 0, previousBrushRotation = 0;
    private GlassPane glassPane;
    private JCheckBox readOnlyCheckBox, biomesCheckBox, annotationsCheckBox, readOnlySoloCheckBox, biomesSoloCheckBox, annotationsSoloCheckBox;
    private JToggleButton readOnlyToggleButton, setSpawnPointToggleButton;
    private JMenuItem addNetherMenuItem, removeNetherMenuItem, addEndMenuItem, removeEndMenuItem, addSurfaceCeilingMenuItem, removeSurfaceCeilingMenuItem, addNetherCeilingMenuItem, removeNetherCeilingMenuItem, addEndCeilingMenuItem, removeEndCeilingMenuItem;
    private JCheckBoxMenuItem viewSurfaceMenuItem, viewNetherMenuItem, viewEndMenuItem, extendedBlockIdsMenuItem, viewSurfaceCeilingMenuItem, viewNetherCeilingMenuItem, viewEndCeilingMenuItem;
    private final JToggleButton[] customMaterialButtons = new JToggleButton[Terrain.CUSTOM_TERRAIN_COUNT];
    private final ColourScheme[] colourSchemes;
    private final ColourScheme defaultColourScheme;
    private ColourScheme selectedColourScheme;
    private final String[] biomeNames = new String[256];
    private SortedMap<String, List<Brush>> customBrushes;
    private final List<Layer> layers = LayerManager.getInstance().getLayers();
    private final List<Operation> operations;
    private ThreeDeeFrame threeDeeFrame;
    private BiomesViewerFrame biomesViewerFrame;
    private MapDragControl mapDragControl;
    private DockableFrame biomesPanel;
    private final boolean devMode = "true".equalsIgnoreCase(System.getProperty("org.pepsoft.worldpainter.devMode")); // NOI18N
    private final boolean alwaysEnableReadOnly = "true".equalsIgnoreCase(System.getProperty("org.pepsoft.worldpainter.alwaysEnableReadOnly")); // NOI18N
    private final BiomeScheme autoBiomeScheme = new AutoBiomeScheme(null);
//    private JScrollPane scrollPane = new JScrollPane();
    private Filter filter, toolFilter;
    private final BrushOptions brushOptions;
    private final CustomBiomeManager customBiomeManager = new CustomBiomeManager();
    private final Set<CustomLayer> layersWithNoButton = new HashSet<>();
    private final Map<Layer, JCheckBox> layerSoloCheckBoxes = new HashMap<>();
    private Layer soloLayer;
    private final PaletteManager paletteManager = new PaletteManager(this);
    private DockingManager dockingManager;
    private boolean hideAbout, hidePreferences, hideExit;
    private Paint paint = PaintFactory.NULL_PAINT;
    private PaintUpdater paintUpdater = () -> {
        // Do nothing
    };

    public static final Image ICON = IconUtils.loadImage("org/pepsoft/worldpainter/icons/shovel-icon.png");
    
    public static final int DEFAULT_MAX_RADIUS = 300;

    private static App instance;
    private static Mode mode = Mode.WORLDPAINTER;

    private static final String ACTION_NAME_INCREASE_RADIUS        = "increaseRadius"; // NOI18N
    private static final String ACTION_NAME_INCREASE_RADIUS_BY_ONE = "increaseRadiusByOne"; // NOI18N
    private static final String ACTION_NAME_DECREASE_RADIUS        = "decreaseRadius"; // NOI18N
    private static final String ACTION_NAME_DECREASE_RADIUS_BY_ONE = "decreaseRadiusByOne"; // NOI18N
    private static final String ACTION_NAME_REDO                   = "redo"; // NOI18N
    private static final String ACTION_NAME_ZOOM_IN                = "zoomIn"; // NOI18N
    private static final String ACTION_NAME_ZOOM_OUT               = "zoomPut"; // NOI18N
    
    private static final long ONE_MEGABYTE = 1024 * 1024;
    
    private static final Logger logger = Logger.getLogger(App.class.getName());

    private static final Icon ICON_NEW_WORLD            = IconUtils.loadIcon("org/pepsoft/worldpainter/icons/page_white.png");
    private static final Icon ICON_OPEN_WORLD           = IconUtils.loadIcon("org/pepsoft/worldpainter/icons/folder_page_white.png");
    private static final Icon ICON_SAVE_WORLD           = IconUtils.loadIcon("org/pepsoft/worldpainter/icons/disk.png");
    private static final Icon ICON_EXPORT_WORLD         = IconUtils.loadIcon("org/pepsoft/worldpainter/icons/map_go.png");
    private static final Icon ICON_EXIT                 = IconUtils.loadIcon("org/pepsoft/worldpainter/icons/door_in.png");
    private static final Icon ICON_ZOOM_IN              = IconUtils.loadIcon("org/pepsoft/worldpainter/icons/magnifier_zoom_in.png");
    private static final Icon ICON_ZOOM_RESET           = IconUtils.loadIcon("org/pepsoft/worldpainter/icons/magnifier.png");
    private static final Icon ICON_ZOOM_OUT             = IconUtils.loadIcon("org/pepsoft/worldpainter/icons/magnifier_zoom_out.png");
    private static final Icon ICON_GRID                 = IconUtils.loadIcon("org/pepsoft/worldpainter/icons/grid.png");
    private static final Icon ICON_CONTOURS             = IconUtils.loadIcon("org/pepsoft/worldpainter/icons/contours.png");
    private static final Icon ICON_OVERLAY              = IconUtils.loadIcon("org/pepsoft/worldpainter/icons/photo.png");
    private static final Icon ICON_UNDO                 = IconUtils.loadIcon("org/pepsoft/worldpainter/icons/arrow_undo.png");
    private static final Icon ICON_REDO                 = IconUtils.loadIcon("org/pepsoft/worldpainter/icons/arrow_redo.png");
    private static final Icon ICON_EDIT_TILES           = IconUtils.loadIcon("org/pepsoft/worldpainter/icons/plugin.png");
    private static final Icon ICON_CHANGE_HEIGHT        = IconUtils.loadIcon("org/pepsoft/worldpainter/icons/arrow_up_down.png");
    private static final Icon ICON_ROTATE_WORLD         = IconUtils.loadIcon("org/pepsoft/worldpainter/icons/arrow_rotate_anticlockwise.png");
    private static final Icon ICON_DIMENSION_PROPERTIES = IconUtils.loadIcon("org/pepsoft/worldpainter/icons/application_form.png");
    private static final Icon ICON_VIEW_DISTANCE        = IconUtils.loadIcon("org/pepsoft/worldpainter/icons/eye.png");
    private static final Icon ICON_WALKING_DISTANCE     = IconUtils.loadIcon("org/pepsoft/worldpainter/icons/user_go.png");
    private static final Icon ICON_ROTATE_LIGHT_RIGHT   = IconUtils.loadIcon("org/pepsoft/worldpainter/icons/arrow_rotate_lightbulb_clockwise.png");
    private static final Icon ICON_ROTATE_LIGHT_LEFT    = IconUtils.loadIcon("org/pepsoft/worldpainter/icons/arrow_rotate_lightbulb_anticlockwise.png");
    private static final Icon ICON_MOVE_TO_SPAWN        = IconUtils.loadIcon("org/pepsoft/worldpainter/icons/spawn_red.png");
    private static final Icon ICON_MOVE_TO_ORIGIN       = IconUtils.loadIcon("org/pepsoft/worldpainter/icons/arrow_in.png");
    private static final Icon ICON_UNKNOWN_PATTERN      = IconUtils.loadIcon("org/pepsoft/worldpainter/icons/unknown_pattern.png");
    private static final Icon ICON_SHIFT_WORLD          = IconUtils.loadIcon("org/pepsoft/worldpainter/icons/arrow_cross.png");
    
    private static final int PLATFORM_COMMAND_MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

    private static final String CUSTOM_BRUSHES_DEFAULT_TITLE = "Custom Brushes";
    
    private static final ResourceBundle strings = ResourceBundle.getBundle("org.pepsoft.worldpainter.resources.strings"); // NOI18N
    private static final long serialVersionUID = 1L;
    
    public class IntensityAction extends BetterAction {
        public IntensityAction(int percentage, int keyCode) {
            super("intensity" + percentage, MessageFormat.format(strings.getString("set.intensity.to.0"), percentage));
            this.percentage = percentage;
            setAcceleratorKey(KeyStroke.getKeyStroke(keyCode, 0));
        }

        @Override
        public void performAction(ActionEvent e) {
            levelSlider.setValue(percentage);
        }
        
        private final int percentage;
        
        private static final long serialVersionUID = 1L;
    }
    
    class ScrollController extends MouseAdapter implements KeyEventDispatcher {
        ScrollController() {
            timer.setRepeats(false);
        }
        
        void install() {
            view.addMouseListener(this);
            view.addMouseMotionListener(this);
            KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
        }

        void uninstall() {
            if (keyDragging || mouseDragging) {
                glassPane.setCursor(previousCursor);
            }
            mouseDragging = false;
            keyDragging = false;
            KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this);
            view.removeMouseMotionListener(this);
            view.removeMouseListener(this);
        }
        
        // MouseListener / MouseMotionListener
        
        @Override
        public void mousePressed(MouseEvent e) {
            if ((e.getButton() == MouseEvent.BUTTON2) && (! mouseDragging)) {
                if (! keyDragging) {
                    Point viewLocOnScreen = view.getLocationOnScreen();
                    e.translatePoint(viewLocOnScreen.x, viewLocOnScreen.y);
                    previousLocation = e.getPoint();

                    previousCursor = glassPane.getCursor();
                    glassPane.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                }
                
                mouseDragging = true;
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            if (mouseDragging || keyDragging) {
                Point viewLocOnScreen = view.getLocationOnScreen();
                e.translatePoint(viewLocOnScreen.x, viewLocOnScreen.y);
                Point location = e.getPoint();
                if (previousLocation != null) {
                    // No idea how previousLocation could be null (it
                    // implies that the mouse pressed event was never
                    // received or handled), but we have a report from the
                    // wild that it happened, so check for it
                    int dx = location.x - previousLocation.x;
                    int dy = location.y - previousLocation.y;
                    view.moveBy(-dx, -dy);
                }
                previousLocation = location;
            }
        }
        
        @Override
        public void mouseDragged(MouseEvent e) {
            if (mouseDragging || keyDragging) {
                Point viewLocOnScreen = view.getLocationOnScreen();
                e.translatePoint(viewLocOnScreen.x, viewLocOnScreen.y);
                Point location = e.getPoint();
                if (previousLocation != null) {
                    // No idea how previousLocation could be null (it
                    // implies that the mouse pressed event was never
                    // received or handled), but we have a report from the
                    // wild that it happened, so check for it
                    int dx = location.x - previousLocation.x;
                    int dy = location.y - previousLocation.y;
                    view.moveBy(-dx, -dy);
                }
                previousLocation = location;
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if ((e.getButton() == MouseEvent.BUTTON2) && mouseDragging) {
                mouseDragging = false;
                if (! keyDragging) {
                    glassPane.setCursor(previousCursor);
                }
            }
        }

        // KeyEventDispatcher
        
        @Override
        public boolean dispatchKeyEvent(KeyEvent e) {
            if ((e.getKeyCode() == KeyEvent.VK_SPACE) && App.this.isFocused()) {
                if (e.getID() == KeyEvent.KEY_PRESSED) {
                    if ((e.getWhen() - lastReleased) < KEY_REPEAT_GUARD_TIME) {
                        timer.stop();
                        return true;
                    } else if (! keyDragging) {
                        Point mouseLocOnScreen = MouseInfo.getPointerInfo().getLocation();
                        Point scrollPaneLocOnScreen = view.getLocationOnScreen();
                        Rectangle viewBoundsOnScreen = view.getBounds();
                        viewBoundsOnScreen.translate(scrollPaneLocOnScreen.x, scrollPaneLocOnScreen.y);
                        if (! viewBoundsOnScreen.contains(mouseLocOnScreen)) {
                            // The mouse cursor is not over the view
                            return false;
                        }

                        if (! mouseDragging) {
                            previousLocation = mouseLocOnScreen;

                            previousCursor = glassPane.getCursor();
                            glassPane.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                        }

                        keyDragging = true;
                        return true;
                    }
                } else if ((e.getID() == KeyEvent.KEY_RELEASED) && keyDragging) {
                    lastReleased = e.getWhen();
                    timer.start();
                    return true;
                }
            }
            return false;
        }

        private Point previousLocation;
        private boolean mouseDragging, keyDragging;
        private Cursor previousCursor;
        private long lastReleased;
        private final Timer timer = new Timer(KEY_REPEAT_GUARD_TIME, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    keyDragging = false;
                    if (! mouseDragging) {
                        glassPane.setCursor(previousCursor);
                    }
                }
            });
        
        /**
         * The number of milliseconds between key press and release events below
         * which they will be considered automatic repeats
         */
        private static final int KEY_REPEAT_GUARD_TIME = 10;
    }

    interface PaintUpdater {
        void updatePaint();
    }

    public enum Mode {WORLDPAINTER, MINECRAFTMAPEDITOR}
}