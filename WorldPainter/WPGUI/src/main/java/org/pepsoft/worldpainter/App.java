/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter;

import com.jidesoft.docking.*;
import com.jidesoft.swing.JideLabel;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;
import org.pepsoft.minecraft.Direction;
import org.pepsoft.minecraft.Material;
import org.pepsoft.minecraft.SeededGenerator;
import org.pepsoft.util.*;
import org.pepsoft.util.ProgressReceiver.OperationCancelled;
import org.pepsoft.util.swing.*;
import org.pepsoft.util.undo.UndoManager;
import org.pepsoft.worldpainter.Dimension.Anchor;
import org.pepsoft.worldpainter.biomeschemes.BiomeHelper;
import org.pepsoft.worldpainter.biomeschemes.BiomeSchemeManager;
import org.pepsoft.worldpainter.biomeschemes.CustomBiome;
import org.pepsoft.worldpainter.biomeschemes.CustomBiomeManager;
import org.pepsoft.worldpainter.biomeschemes.CustomBiomeManager.CustomBiomeListener;
import org.pepsoft.worldpainter.brushes.BitmapBrush;
import org.pepsoft.worldpainter.brushes.Brush;
import org.pepsoft.worldpainter.brushes.RotatedBrush;
import org.pepsoft.worldpainter.brushes.SymmetricBrush;
import org.pepsoft.worldpainter.dnd.WPTransferHandler;
import org.pepsoft.worldpainter.dynmap.DynmapColourScheme;
import org.pepsoft.worldpainter.exporting.HeightMapExporter;
import org.pepsoft.worldpainter.gardenofeden.GardenOfEdenOperation;
import org.pepsoft.worldpainter.history.HistoryEntry;
import org.pepsoft.worldpainter.history.WorldHistoryDialog;
import org.pepsoft.worldpainter.importing.CustomItemsTreeModel;
import org.pepsoft.worldpainter.importing.ImportCustomItemsDialog;
import org.pepsoft.worldpainter.importing.ImportMaskDialog;
import org.pepsoft.worldpainter.importing.MapImportDialog;
import org.pepsoft.worldpainter.layers.*;
import org.pepsoft.worldpainter.layers.exporters.AnnotationsExporter;
import org.pepsoft.worldpainter.layers.plants.PlantLayerEditor;
import org.pepsoft.worldpainter.layers.renderers.VoidRenderer;
import org.pepsoft.worldpainter.layers.tunnel.TunnelLayer;
import org.pepsoft.worldpainter.operations.*;
import org.pepsoft.worldpainter.painting.LayerPaint;
import org.pepsoft.worldpainter.painting.Paint;
import org.pepsoft.worldpainter.painting.PaintFactory;
import org.pepsoft.worldpainter.painting.TerrainPaint;
import org.pepsoft.worldpainter.palettes.Palette;
import org.pepsoft.worldpainter.panels.BrushOptions;
import org.pepsoft.worldpainter.panels.DefaultFilter;
import org.pepsoft.worldpainter.panels.InfoPanel;
import org.pepsoft.worldpainter.plugins.PlatformManager;
import org.pepsoft.worldpainter.ramps.ColourGradient;
import org.pepsoft.worldpainter.ramps.DefaultColourRamp;
import org.pepsoft.worldpainter.selection.*;
import org.pepsoft.worldpainter.threedeeview.ThreeDeeFrame;
import org.pepsoft.worldpainter.tools.BiomesViewerFrame;
import org.pepsoft.worldpainter.tools.Eyedropper.PaintType;
import org.pepsoft.worldpainter.tools.Eyedropper.SelectionListener;
import org.pepsoft.worldpainter.tools.RespawnPlayerDialog;
import org.pepsoft.worldpainter.tools.scripts.ScriptRunner;
import org.pepsoft.worldpainter.util.*;
import org.pepsoft.worldpainter.util.BetterAction;
import org.pepsoft.worldpainter.util.FileFilter;
import org.pepsoft.worldpainter.util.FileUtils;
import org.pepsoft.worldpainter.vo.AttributeKeyVO;
import org.pepsoft.worldpainter.vo.EventVO;
import org.pepsoft.worldpainter.vo.UsageVO;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.Box;
import javax.swing.Timer;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.io.*;
import java.lang.Void;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;

import static com.jidesoft.docking.DockContext.DOCK_SIDE_EAST;
import static com.jidesoft.docking.DockContext.DOCK_SIDE_WEST;
import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static java.awt.GridBagConstraints.HORIZONTAL;
import static java.awt.event.ComponentEvent.COMPONENT_RESIZED;
import static java.awt.event.KeyEvent.*;
import static java.lang.Math.round;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toSet;
import static javax.swing.JOptionPane.*;
import static javax.swing.KeyStroke.getKeyStroke;
import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.util.AwtUtils.doLaterOnEventThread;
import static org.pepsoft.util.AwtUtils.doOnEventThread;
import static org.pepsoft.util.DesktopUtils.PLATFORM_COMMAND_MASK;
import static org.pepsoft.util.GUIUtils.getUIScale;
import static org.pepsoft.util.IconUtils.*;
import static org.pepsoft.util.MathUtils.mod;
import static org.pepsoft.util.swing.MessageUtils.*;
import static org.pepsoft.util.swing.ProgressDialog.NOT_CANCELABLE;
import static org.pepsoft.util.swing.ProgressDialog.NO_FOCUS_STEALING;
import static org.pepsoft.worldpainter.App.TerrainMode.SHOW_TERRAIN;
import static org.pepsoft.worldpainter.Configuration.LookAndFeel.DARK_METAL;
import static org.pepsoft.worldpainter.Constants.*;
import static org.pepsoft.worldpainter.DefaultPlugin.JAVA_MCREGION;
import static org.pepsoft.worldpainter.Dimension.Anchor.*;
import static org.pepsoft.worldpainter.Dimension.Role.*;
import static org.pepsoft.worldpainter.Generator.LARGE_BIOMES;
import static org.pepsoft.worldpainter.Platform.Capability.*;
import static org.pepsoft.worldpainter.Terrain.*;
import static org.pepsoft.worldpainter.TileRenderer.FLUIDS_AS_LAYER;
import static org.pepsoft.worldpainter.TileRenderer.TERRAIN_AS_LAYER;
import static org.pepsoft.worldpainter.WPTileProvider.Effect.FADE_TO_FIFTY_PERCENT;
import static org.pepsoft.worldpainter.WPTileProvider.Effect.FADE_TO_TWENTYFIVE_PERCENT;
import static org.pepsoft.worldpainter.World2.*;
import static org.pepsoft.worldpainter.exporting.HeightMapExporter.Format.INTEGER_HIGH_RESOLUTION;
import static org.pepsoft.worldpainter.exporting.HeightMapExporter.Format.INTEGER_LOW_RESOLUTION;
import static org.pepsoft.worldpainter.painting.PaintFactory.*;
import static org.pepsoft.worldpainter.ramps.ColourGradient.Transition.LINEAR;
import static org.pepsoft.worldpainter.util.BiomeUtils.getAllBiomes;
import static org.pepsoft.worldpainter.util.BiomeUtils.getBiomeScheme;

/**
 *
 * @author pepijn
 */
@SuppressWarnings("MagicConstant")
public final class App extends JFrame implements BrushControl,
        BiomesViewerFrame.SeedListener, BrushOptions.Listener, CustomBiomeListener,
        DockableHolder, PropertyChangeListener, Dimension.Listener, Tile.Listener, MapDragControl {
    private App() {
        super((mode == Mode.WORLDPAINTER) ? "WorldPainter" : "MinecraftMapEditor"); // NOI18N

        if (MainFrame.getMainFrame() != null) {
            throw new IllegalArgumentException("Already instantiated");
        }

        setIconImage(ICON);

        colourSchemes = new ColourScheme[] {
            DynmapColourScheme.loadDynMapColourScheme("default", 0),
            null,
            null,
            null,
            DynmapColourScheme.loadDynMapColourScheme("dokudark", 0),
            DynmapColourScheme.loadDynMapColourScheme("dokuhigh", 0),
            DynmapColourScheme.loadDynMapColourScheme("dokulight", 0),
            DynmapColourScheme.loadDynMapColourScheme("misa", 0),
            DynmapColourScheme.loadDynMapColourScheme("sphax", 0)
        };
        defaultColourScheme = colourSchemes[0];
        Configuration config = Configuration.getInstance();
        darkMode = (! "true".equalsIgnoreCase(System.getProperty("org.pepsoft.worldpainter.safeMode"))) && (config.getLookAndFeel() == DARK_METAL);
        final String customColourSchemeLocation = System.getProperty("org.pepsoft.worldpainter.colourSchemeFile");
        if (customColourSchemeLocation != null) {
            throw new UnsupportedOperationException("The org.pepsoft.worldpainter.colourSchemeFile advanced setting is no longer supported (colour schemes are once again available from the View menu)");
        } else {
            selectedColourScheme = (colourSchemes[config.getColourschemeIndex()] != null) ? colourSchemes[config.getColourschemeIndex()] : defaultColourScheme;
        }
        operations = OperationManager.getInstance().getOperations();
        setMaxRadius(config.getMaximumBrushSize());

        loadCustomBrushes();
        
        brushOptions = new BrushOptions();
        brushOptions.setColourScheme(selectedColourScheme);
        brushOptions.setCustomBiomeManager(customBiomeManager);
        brushOptions.setListener(this);
        brushOptions.setSelectionState(selectionState);

        if (SystemUtils.isMac()) {
            installMacCustomisations();
        }

        MainFrame.setMainFrame(this);
        initComponents();

        getRootPane().putClientProperty(KEY_HELP_KEY, "Main");

        hiddenLayers.add(Biome.INSTANCE);
        view.setHiddenLayers(singleton(Biome.INSTANCE));
        
        // Initialize various things
        customBiomeManager.addListener(this);
        
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
            setSize((int) (1024 * getUIScale()), (int) (896 * getUIScale()));
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
                autosaveTimer = new Timer(config.getAutosaveDelay(), event -> maybeAutosave());
                autosaveTimer.setDelay(config.getAutosaveDelay() / 2);
                if (config.isAutosaveEnabled() && (! config.isAutosaveInhibited())) {
                    autosaveTimer.start();
                }
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

        // Log some information about the graphics environment
        GraphicsDevice graphicsDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        DisplayMode displayMode = graphicsDevice.getDisplayMode();
        ImageCapabilities imageCapabilities = graphicsDevice.getDefaultConfiguration().getImageCapabilities();
        int availableAcceleratedMemory = graphicsDevice.getAvailableAcceleratedMemory();
        logger.info("Default graphics device, ID string: {}, available accelerated memory: {}, display mode: {}x{}, bit depth: {}, refresh rate: {}, reported dpi: {}, accelerated: {}, true volatile: {}",
                graphicsDevice.getIDstring(),
                (availableAcceleratedMemory == -1) ? "unknown" : (0x00000000ffffffffL & availableAcceleratedMemory) + " bytes",
                displayMode.getWidth(),
                displayMode.getHeight(),
                (displayMode.getBitDepth() == DisplayMode.BIT_DEPTH_MULTI) ? "multi" : displayMode.getBitDepth(),
                (displayMode.getRefreshRate() == DisplayMode.REFRESH_RATE_UNKNOWN) ? "unknown" : displayMode.getRefreshRate(),
                Toolkit.getDefaultToolkit().getScreenResolution(),
                imageCapabilities.isAccelerated() ? "yes" : "no",
                imageCapabilities.isTrueVolatile() ? "yes" : "no"
        );
        if (getUIScale() != 1.0) {
            logger.info("High resolution display support enabled. Scale: {}", getUIScale());
        }

        mapSelectionController = new MapSelectionController(this, view);

        PlantLayerEditor.loadIconsInBackground();

        setTransferHandler(new WPTransferHandler(this));
    }

    public World2 getWorld() {
        return world;
    }

    /**
     * Unload the currently loaded World. This must always be done before
     * loading a different World using {@link #setWorld(World2, boolean)}.
     *
     * <p>This is for historical reasons: it was introduced to make sure that
     * the custom materials, which are registered when loaded, are not then
     * immediately cleared when invoking this method.
     */
    public void clearWorld() {
        if (world != null) {
            for (Dimension dimension: world.getDimensions()) {
                dimension.unregisterUndoManager();
            }
            world.removePropertyChangeListener(this);
            world = null;
            lastSavedState = lastAutosavedState = -1;

            setDimension(null);

            if (currentUndoManager != null) {
                currentUndoManager.unregisterActions();
            }
            undoManagers.clear();

            ACTION_EXPORT_WORLD.setEnabled(false);
            ACTION_MERGE_WORLD.setEnabled(false);

            // Unload all custom terrain types
            clearCustomTerrains();

            // Unload all custom materials
            MixedMaterialManager.getInstance().clear();
        }
    }

    /**
     * This setter may only be used to re-load the same World (by passing in the
     * current World instance), or after the current World has been unloaded by
     * invoking {@link #clearWorld()}. Otherwise it will throw an {@link
     * IllegalStateException}.
     *
     * <p>This is for historical reasons: it was introduced to make sure that
     * the custom materials, which are registered when loaded, are not then
     * immediately cleared when invoking this method.
     *
     * @param world The world to set.
     * @param markClean Whether the world is clean (loaded or created from a
     *                  persistent source so that it can be recreated and does
     *                  not need to be saved if unchanged) or dirty (needs to be
     *                  saved even if unchanged by the user).
     */
    public void setWorld(World2 world, boolean markClean) {
        if (world == null) {
            throw new NullPointerException();
        } else if ((this.world != null) && (this.world != world)) {
            throw new IllegalStateException(world + " != " + this.world);
        }
        if (world == this.world) {
            // Reloading the same world; no need to do anything other than
            // to reload the dimension
            if (dimension != null) {
                setDimension(world.getDimension(dimension.getAnchor()));
            }
        } else {
            this.world = world;
            long now = System.currentTimeMillis();
            if (markClean) {
                lastSavedState = lastAutosavedState = world.getChangeNo();
            } else {
                lastSavedState = lastAutosavedState = -1;
            }
            lastSaveTimestamp = now;
            lastChangeTimestamp = now;
            world.addPropertyChangeListener(this);

            loadCustomTerrains();

            extendedBlockIdsMenuItem.setSelected(world.isExtendedBlockIds());

            final Dimension surfaceDimension = world.getDimension(NORMAL_DETAIL);
            final Dimension masterDimension = world.getDimension(NORMAL_MASTER);
            if ((masterDimension != null) && (surfaceDimension.getTileCount() == 0)) {
                setDimension(masterDimension);

                doLaterOnEventThread(() -> JOptionPane.showMessageDialog(App.this,
                        "You are now editing the Master Dimension. This will be exported\n" +
                                "at sixteen times the horizontal size.\n" +
                                "\n" +
                                "To add details at 1:1 scale, switch to the Surface Dimension by\n" +
                                "pressing " + COMMAND_KEY_NAME + "+M or using the View menu and then add tiles by\n" +
                                "pressing " + COMMAND_KEY_NAME + "+T or using the Edit menu.", "Editing Master Dimension", INFORMATION_MESSAGE));
            } else {
                setDimension(surfaceDimension);
            }

            final Configuration config = Configuration.getInstance();
            if (config.isDefaultViewDistanceEnabled() != view.isDrawViewDistance()) {
                ACTION_VIEW_DISTANCE.actionPerformed(null);
            }
            if (config.isDefaultWalkingDistanceEnabled() != view.isDrawWalkingDistance()) {
                ACTION_WALKING_DISTANCE.actionPerformed(null);
            }
            view.setLightOrigin(config.getDefaultLightOrigin());

            if (config.isEasyMode()) {
                boolean imported = world.getImportedFrom() != null;
                ACTION_EXPORT_WORLD.setEnabled(!imported);
                ACTION_MERGE_WORLD.setEnabled(imported);
            } else {
                ACTION_EXPORT_WORLD.setEnabled(true);
                ACTION_MERGE_WORLD.setEnabled(true);
            }

            if (! PlatformManager.getInstance().getAllPlatforms().contains(world.getPlatform())) {
                beepAndShowWarning(this, "This world is set to a map format (\"" + world.getPlatform().displayName + "\") that is unknown and unsupported.\n" +
                        "It cannot be Exported without first changing the format.\n" +
                        "It is most likely supported by a plugin that is not installed or could not be loaded.", "Unknown Map Format");
            }
        }
    }

    public Dimension getDimension() {
        return dimension;
    }

    public void setDimension(final Dimension dimension) {
        Configuration config = Configuration.getInstance();
        if (this.dimension != null) {
            this.dimension.removePropertyChangeListener(this);
            this.dimension.removeDimensionListener(this);
            for (Tile tile: this.dimension.getTiles()) {
                tile.removeListener(this);
            }
            Point viewPosition = view.getViewCentreInWorldCoords();
            this.dimension.setLastViewPosition(viewPosition);
            // Keep the view position of related dimensions, if any, in sync
            if (world != null) {
                final Anchor anchor = this.dimension.getAnchor();
                final Dimension oppositeDimension = world.getDimension(new Anchor(anchor.dim, anchor.role, ! anchor.invert, anchor.id));
                if (oppositeDimension != null) {
                    oppositeDimension.setLastViewPosition(viewPosition);
                }
                switch (anchor.role) {
                    case DETAIL:
                        final Dimension masterDimension = world.getDimension(new Anchor(anchor.dim, MASTER, anchor.invert, 0));
                        if (masterDimension != null) {
                            masterDimension.setLastViewPosition(new Point(viewPosition.x >> 4, viewPosition.y >> 4));
                        }
                        break;
                    case MASTER:
                        Dimension detailDimension = world.getDimension(new Anchor(anchor.dim, DETAIL, anchor.invert, 0));
                        if (detailDimension != null) {
                            detailDimension.setLastViewPosition(new Point(viewPosition.x << 4, viewPosition.y << 4));
                        }
                        break;
                    case CAVE_FLOOR:
                    case FLOATING_FLOOR:
                        // Keep the surface dimension in sync with the last edited cave floor
                        detailDimension = world.getDimension(new Anchor(anchor.dim, DETAIL, anchor.invert, 0));
                        detailDimension.setLastViewPosition(viewPosition);
                        break;
                }
            }

            // No idea how currentUndoManager could be null here, but it has been observed in the wild:
            if (currentUndoManager != null) {
                currentUndoManager.unregisterActions();
                currentUndoManager = null;
            }

            Map<String, byte[]> layoutData = config.getJideLayoutData();
            if (layoutData == null) {
                layoutData = new HashMap<>();
            }
            layoutData.put(this.dimension.getId().toString(), dockingManager.getLayoutRawData());
            config.setJideLayoutData(layoutData);

            // Remove the existing custom object layers and save the list of custom layers to the dimension to preserve
            // layers which aren't currently in use
            saveCustomLayers();
            if (! customLayerController.paletteManager.isEmpty()) {
                boolean visibleLayersChanged = false;
                final Set<String> hiddenPalettes = new HashSet<>();
                String soloedPalette = null;
                for (Palette palette: customLayerController.paletteManager.clear()) {
                    final String paletteName = palette.getName();
                    if (! palette.isShow()) {
                        hiddenPalettes.add(paletteName);
                    }
                    if (palette.isSolo()) {
                        soloedPalette = paletteName;
                    }
                    palette.removePropertyChangeListener(this);
                    List<CustomLayer> paletteLayers = palette.getLayers();
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
                this.dimension.setHiddenPalettes(hiddenPalettes.isEmpty() ? null : hiddenPalettes);
                this.dimension.setSoloedPalette(soloedPalette);

                if ((paint instanceof LayerPaint) && (((LayerPaint) paint).getLayer() instanceof CustomLayer)) {
                    // Don't leave a CustomLayer selected as paint, since they are associated with the dimension
                    deselectPaint();
                }
            }
            layersWithNoButton.clear();

            saveCustomBiomes();

            cancelPaintSelection();
            removeGlassPaneComponents();

            view.setDimension(null);
        }
        this.dimension = dimension;
        if (dimension != null) {
            setTitle("WorldPainter - " + world.getName() + " - " + dimension.getName()); // NOI18N
            final Anchor anchor = dimension.getAnchor();
            viewSurfaceMenuItem.setSelected(anchor.dim == DIM_NORMAL);
            viewNetherMenuItem.setSelected(anchor.dim == DIM_NETHER);
            viewEndMenuItem.setSelected(anchor.dim == DIM_END);
            ACTION_EDIT_TILES.setEnabled((anchor.role != CAVE_FLOOR) && (anchor.role != FLOATING_FLOOR));

            // Legacy: if this is an older world with an overlay enabled, warn the user that it may be incorrectly
            // located (we used to offer to fix this, but this should be exceedingly rare).
            if (dimension.isFixOverlayCoords()) {
                beepAndShowWarning(this, "This world was created in an older version of WorldPainter\n" +
                        "in which the overlay offsets were not stored correctly.\n" +
                        "You may need to fix the position of your overlay.", "Check Overlay Positioning");
                dimension.setFixOverlayCoords(false);
            }

            view.setDimension(dimension, false);
            outsideDimensionLabel = "Minecraft Generated";
            if (anchor.equals(NORMAL_DETAIL)) {
                backgroundDimension = world.getDimension(new Anchor(DIM_NORMAL, MASTER, false, 0));
                showBackgroundStatus = backgroundDimension != null;
                backgroundZoom = 4;
                view.setBackgroundDimension(backgroundDimension, backgroundZoom, FADE_TO_FIFTY_PERCENT);
            } else if ((anchor.role == CAVE_FLOOR) || (anchor.role == FLOATING_FLOOR)) {
                backgroundDimension = world.getDimension(new Anchor(anchor.dim, DETAIL, anchor.invert, 0));
                showBackgroundStatus = false;
                backgroundZoom = 0;
                outsideDimensionLabel = (anchor.role == CAVE_FLOOR) ? "Outside Cave/Tunnel" : "Outside Floating Dimension";
                view.setBackgroundDimension(backgroundDimension, backgroundZoom, FADE_TO_TWENTYFIVE_PERCENT);
            } else {
                backgroundDimension = null;
                showBackgroundStatus = false;
                view.setBackgroundDimension(null, -1, null);
            }
            view.moveTo(dimension.getLastViewPosition());

            configureForPlatform();
            currentUndoManager = undoManagers.get(anchor);
            if (currentUndoManager == null) {
                if ((! "true".equals(System.getProperty("org.pepsoft.worldpainter.disableUndo"))) && config.isUndoEnabled()) {
                    currentUndoManager = new UndoManager(ACTION_UNDO, ACTION_REDO, Math.max(config.getUndoLevels() + 1, 2));
                } else {
                    // Still install an undo manager, because some operations depend on one level of undo being
                    // available
                    currentUndoManager = new UndoManager(2);
                    ACTION_UNDO.setEnabled(false);
                    ACTION_REDO.setEnabled(false);
                }
                currentUndoManager.setStopAtClasses(PropertyChangeListener.class, Tile.Listener.class, Biome.class, BetterAction.class);
                undoManagers.put(anchor, currentUndoManager);
                dimension.registerUndoManager(currentUndoManager);
            } else if ((! "true".equals(System.getProperty("org.pepsoft.worldpainter.disableUndo"))) && config.isUndoEnabled()) {
                currentUndoManager.registerActions(ACTION_UNDO, ACTION_REDO);
            }
            dimension.armSavePoint();
            if (threeDeeFrame != null) {
                threeDeeFrame.setDimension(dimension);
            }
                
            // Add the custom object layers from the world
            StringBuilder warnings = new StringBuilder();
            for (CustomLayer customLayer: dimension.getCustomLayers()) {
                if (customLayer.isHide()) {
                    layersWithNoButton.add(customLayer);
                } else {
                    customLayerController.registerCustomLayer(customLayer, false);
                }
                if (customLayer instanceof CombinedLayer) {
                    if (! ((CombinedLayer) customLayer).restoreCustomTerrain()) {
                        if (warnings.length() == 0) {
                            warnings.append("The Custom Terrain for one or more Combined Layer could not be restored:\n\n");
                        }
                        warnings.append(customLayer.getName()).append('\n');
                    } else {
                        // Check for a custom terrain type and if necessary make sure it has a button
                        Terrain terrain = ((CombinedLayer) customLayer).getTerrain();
                        if ((terrain != null) && terrain.isCustom() && (customMaterialButtons[terrain.getCustomTerrainIndex()] == null)) {
                            addButtonForNewCustomTerrain(terrain.getCustomTerrainIndex(), Terrain.getCustomMaterial(terrain.getCustomTerrainIndex()), false);
                        }
                    }
                }
            }
            if (warnings.length() > 0) {
                warnings.append("\nThe Custom Terrain has been removed from the layer(s).");
                showMessageDialog(this, warnings.toString(), "Custom Terrain(s) Not Restored", ERROR_MESSAGE);
            }

            // Restore palette states
            if (dimension.getSoloedPalette() != null) {
                customLayerController.paletteManager.getPalette(dimension.getSoloedPalette()).setSolo(true);
            }
            if (dimension.getHiddenPalettes() != null) {
                dimension.getHiddenPalettes().forEach(name -> {
                    final Palette palette = customLayerController.paletteManager.getPalette(name);
                    // It's not clear how this can be null, but that has been observed in the wild. TODO find out why
                    //  and fix the underlying cause!
                    if (palette != null) {
                        palette.setShow(false);
                    } else {
                        logger.error("dimension.hiddenPalettes contains non existent palette name {}", name);
                    }
                });
            }

            // Set action states
            ACTION_GRID.setSelected(view.isPaintGrid());
            ACTION_CONTOURS.setSelected(view.isDrawContours());
            ACTION_OVERLAYS.setSelected(dimension.isOverlaysEnabled());

            // TODO: make this work correctly with undo/redo, and make "inside selection" ineffective when there is no selection, to avoid confusion
            // Set operation states
//            if (dimension.containsOneOf(SelectionChunk.INSTANCE, SelectionBlock.INSTANCE)) {
//                selectionState.setValue(true);
//            } else {
//                if (activeOperation instanceof CopySelectionOperation) {
//                    deselectTool();
//                }
//                selectionState.setValue(false);
//            }
            
            // Load custom biomes. But first remove any that are now regular biomes
            List<CustomBiome> customBiomes = dimension.getCustomBiomes();
            if (customBiomes != null) {
                final BiomeScheme biomeScheme = getBiomeScheme(world.getPlatform());
                customBiomes.removeIf(customBiome -> biomeScheme.isBiomePresent(customBiome.getId()));
                if (customBiomes.isEmpty()) {
                    customBiomes = null;
                }
            }
            programmaticChange = true;
            try {
                if (customBiomes != null) {
                    customBiomeManager.setCustomBiomes(customBiomes);
                } else {
                    customBiomeManager.clearCustomBiomes();
                }
            } finally {
                programmaticChange = false;
            }

            brushOptions.setMinHeight(dimension.getMinHeight());
            brushOptions.setMaxHeight(dimension.getMaxHeight());

            dimension.addPropertyChangeListener(this);
            dimension.addDimensionListener(this);
            for (Tile tile: dimension.getTiles()) {
                tile.addListener(this);
            }

            updateZoomLabel();
            updateRadiusLabel();

            final Map<String, byte[]> layoutData = config.getJideLayoutData();
            final String key = dimension.getId().toString();
            if ((layoutData != null) && layoutData.containsKey(key)) {
                dockingManager.loadLayoutFrom(new ByteArrayInputStream(layoutData.get(key)));
                // This works around a bug in JIDE that otherwise causes painting to be shifted if this resulted in
                // resized docks:
                view.componentResized(new ComponentEvent(view, COMPONENT_RESIZED));
            }

            if (! refreshTerrainMode()) {
                view.refreshTiles();
            }

            if (dimension.getTileCount() == 0) {
                doLaterOnEventThread(this::addRemoveTiles);
            }
        } else {
            view.setDimension(null);
            view.setBackgroundDimension(null, -1, null);
            setTitle("WorldPainter"); // NOI18N

            // Clear action states
            ACTION_GRID.setSelected(false);
            ACTION_CONTOURS.setSelected(false);
            ACTION_OVERLAYS.setSelected(false);
            
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

            // TODO: make this work correctly with undo/redo, and make "inside selection" ineffective when there is no selection, to avoid confusion
            // Disable copy selection operation
//            selectionState.setValue(false);
            
            programmaticChange = true;
            try {
                customBiomeManager.clearCustomBiomes();
            } finally {
                programmaticChange = false;
            }

            backgroundDimension = null;
        }
    }

    public void updateStatusBar(int x, int y) {
        Dimension dimension = this.dimension;
        if (dimension == null) {
            setTextIfDifferent(locationLabel, " ");
            setTextIfDifferent(heightLabel, " ");
            setTextIfDifferent(waterLabel, " ");
            setTextIfDifferent(materialLabel, " ");
            setTextIfDifferent(biomeLabel, " ");
            return;
        }
        final float scale = dimension.getScale();
        setTextIfDifferent(locationLabel, MessageFormat.format(strings.getString("location.0.1"), INT_NUMBER_FORMAT.format(round(x * scale)), INT_NUMBER_FORMAT.format(round(y * scale))));
        Tile tile = dimension.getTile(x >> TILE_SIZE_BITS, y >> TILE_SIZE_BITS);
        int xInTile = x & TILE_SIZE_MASK, yInTile = y & TILE_SIZE_MASK;
        if (showBackgroundStatus
                && ((tile == null) || tile.getBitLayerValue(NotPresent.INSTANCE, xInTile, yInTile) || tile.getBitLayerValue(NotPresentBlock.INSTANCE, xInTile, yInTile))
                && backgroundDimension.isTilePresent(x >> (TILE_SIZE_BITS + backgroundZoom), y >> (TILE_SIZE_BITS + backgroundZoom))) {
            dimension = backgroundDimension;
            x = x >> backgroundZoom;
            y = y >> backgroundZoom;
            xInTile = x & TILE_SIZE_MASK;
            yInTile = y & TILE_SIZE_MASK;
            tile = dimension.getTile(x >> TILE_SIZE_BITS, y >> TILE_SIZE_BITS);
        }
        if (tile == null) {
            // Not on a tile
            setTextIfDifferent(heightLabel, " ");
            setTextIfDifferent(slopeLabel, " ");
            setTextIfDifferent(waterLabel, " ");
            if (dimension.isBorderTile(x >> TILE_SIZE_BITS, y >> TILE_SIZE_BITS)) {
                setTextIfDifferent(materialLabel, "Border");
            } else {
                setTextIfDifferent(materialLabel, outsideDimensionLabel);
            }
            setTextIfDifferent(biomeLabel, " ");
            return;
        }
        if (tile.getBitLayerValue(NotPresent.INSTANCE, xInTile, yInTile) || tile.getBitLayerValue(NotPresentBlock.INSTANCE, xInTile, yInTile)) {
            // Marked as not present
            setTextIfDifferent(heightLabel, " ");
            setTextIfDifferent(slopeLabel, " ");
            setTextIfDifferent(waterLabel, " ");
            setTextIfDifferent(materialLabel, outsideDimensionLabel);
            setTextIfDifferent(biomeLabel, " ");
            return;
        }
        final int height = tile.getIntHeight(xInTile, yInTile);
        setTextIfDifferent(heightLabel, MessageFormat.format(strings.getString("height.0.of.1"), height, (height >= 0) ? dimension.getMaxHeight() - 1 : dimension.getMinHeight()));
        setTextIfDifferent(slopeLabel, MessageFormat.format("Slope: {0}°", (int) round(Math.atan(dimension.getSlope(x, y)) * 180 / Math.PI)));
        if ((activeOperation instanceof PaintOperation) && (paint instanceof LayerPaint)) {
            final Layer layer = ((LayerPaint) paint).getLayer();
            final Layer.DataSize dataSize = layer.getDataSize();
            switch (dataSize) {
                case BIT:
                case BIT_PER_CHUNK:
                    setTextIfDifferent(waterLabel, MessageFormat.format(strings.getString("layer.0.on.off"), layer.getName(), (tile.getBitLayerValue(layer, xInTile, yInTile) ? 1 : 0)));
                    break;
                case NIBBLE:
                case BYTE:
                    if ((! layer.equals(Annotations.INSTANCE)) && (! layer.equals(Biome.INSTANCE))) {
                        setTextIfDifferent(waterLabel, MessageFormat.format(strings.getString("layer.0.level.1"), layer.getName(), dataSize.toString(tile.getLayerValue(layer, xInTile, yInTile))));
                    } else {
                        setTextIfDifferent(waterLabel, " ");
                    }
                    break;
                default:
                    // Do nothing
                    break;
            }
        } else if (activeOperation instanceof GardenOfEdenOperation) {
            setTextIfDifferent(waterLabel, strings.getString("structure") + ": " + GardenCategory.getLabel(strings, tile.getLayerValue(GardenCategory.INSTANCE, xInTile, yInTile)));
        } else {
            int waterLevel = tile.getWaterLevel(xInTile, yInTile);
            if (waterLevel > height) {
                setTextIfDifferent(waterLabel, MessageFormat.format(strings.getString("fluid.level.1.depth.2"), tile.getBitLayerValue(FloodWithLava.INSTANCE, xInTile, yInTile) ? 1 : 0, waterLevel, waterLevel - height));
            } else {
                setTextIfDifferent(waterLabel, " ");
            }
        }
        Terrain terrain = tile.getTerrain(xInTile, yInTile);
        if (terrain.isCustom()) {
            int index = terrain.getCustomTerrainIndex();
            setTextIfDifferent(materialLabel, MessageFormat.format(strings.getString("material.custom.1.0"), Terrain.getCustomMaterial(index), index + 1));
        } else {
            setTextIfDifferent(materialLabel, MessageFormat.format(strings.getString("material.0"), terrain.getName()));
        }
        // TODO: apparently this was sometimes invoked at or soon after startup, with biomeHelper being null, causing a
        //  NPE. How is this possible?
        if (biomeHelper != null) {
            int biome = tile.getLayerValue(Biome.INSTANCE, xInTile, yInTile);
            // TODO: is this too slow?
            if (biome == 255) {
                biome = dimension.getAutoBiome(x, y);
                if (biome != -1) {
                    setTextIfDifferent(biomeLabel, "Auto biome: " + biomeHelper.getBiomeName(biome));
                }
            } else if (biome != -1) {
                setTextIfDifferent(biomeLabel, MessageFormat.format(strings.getString("biome.0"), biomeHelper.getBiomeName(biome)));
            }
        }
    }

    private void updateRadiusLabel() {
        if ((dimension != null) && (activeOperation instanceof BrushOperation)) {
            final float scale = dimension.getScale();
            setTextIfDifferent(radiusLabel, MessageFormat.format(strings.getString("radius.0"), round(radius * scale)));
        } else {
            setTextIfDifferent(radiusLabel, " ");
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

    public Brush getToolBrush() {
        return toolBrush;
    }

    public float getToolLevel() {
        return toolLevel;
    }

    public int getZoom() {
        return view.getZoom();
    }

    public int getMaxRadius() {
        return maxRadius;
    }

    public void setMaxRadius(int maxRadius) {
        this.maxRadius = maxRadius;
        if (radius > maxRadius) {
            radius = maxRadius;
            if (activeOperation instanceof BrushOperation) {
                ((BrushOperation) activeOperation).setRadius(radius);
            }
            view.setRadius(radius);
            updateRadiusLabel();
        }
    }

    public void open(File file, boolean askForConfirmation) {
        if (askForConfirmation && (world != null) && (world.getChangeNo() != lastSavedState)) {
            int action = showConfirmDialog(this, strings.getString("there.are.unsaved.changes"));
            if (action == YES_OPTION) {
                if (! saveAs()) {
                    // User cancelled the save
                    return;
                }
            } else if (action != NO_OPTION) {
                // User closed the confirmation dialog without making a choice
                return;
            }
        }
        open(file);
    }
    
    public void open(final File file) {
        logger.info("Loading world " + file.getAbsolutePath());
        clearWorld(); // Free up memory of the world and the undo buffer
        final boolean loadedFromAutosave = isAutosaveFile(file);
        final World2 newWorld = ProgressDialog.executeTask(this, new LoadWorldTask(this, file), NOT_CANCELABLE);
        if (newWorld == null) {
            // The file was damaged
            return;
        }
        if (loadedFromAutosave) {
            newWorld.addHistoryEntry(HistoryEntry.WORLD_RECOVERED_FROM_AUTOSAVE);
        } else {
            newWorld.addHistoryEntry(HistoryEntry.WORLD_LOADED, file);
        }
        final boolean loadedFromBackup = (! loadedFromAutosave) && isBackupFile(file);
        if ((! loadedFromBackup) && (! loadedFromAutosave)) {
            lastSelectedFile = file;
        } else {
            lastSelectedFile = null;
        }

        // Log an event
        final Configuration config = Configuration.getInstance();
        final EventVO event = new EventVO(EVENT_KEY_ACTION_OPEN_WORLD).addTimestamp();
        event.setAttribute(ATTRIBUTE_KEY_MAX_HEIGHT, newWorld.getMaxHeight());
        Dimension loadedDimension = newWorld.getDimension(NORMAL_DETAIL);
        event.setAttribute(ATTRIBUTE_KEY_TILES, loadedDimension.getTileCount());
        logLayers(loadedDimension, event, "");
        loadedDimension = newWorld.getDimension(NETHER_DETAIL);
        if (loadedDimension != null) {
            event.setAttribute(ATTRIBUTE_KEY_NETHER_TILES, loadedDimension.getTileCount());
            logLayers(loadedDimension, event, "nether.");
        }
        loadedDimension = newWorld.getDimension(END_DETAIL);
        if (loadedDimension != null) {
            event.setAttribute(ATTRIBUTE_KEY_END_TILES, loadedDimension.getTileCount());
            logLayers(loadedDimension, event, "end.");
        }
        if (newWorld.getImportedFrom() != null) {
            event.setAttribute(ATTRIBUTE_KEY_IMPORTED_WORLD, true);
        }
        config.logEvent(event);

        if (Version.isSnapshot()
                && (newWorld.getMetadata() != null)
                && newWorld.getMetadata().containsKey(METADATA_KEY_WP_VERSION)
                && (! ((String) newWorld.getMetadata().get(METADATA_KEY_WP_VERSION)).contains("SNAPSHOT"))) {
            beepAndShowWarning(this, "You are running a snapshot version of WorldPainter.\n" +
                    "This file was last saved by a regular version of WorldPainter.\n" +
                    "If you save the file with this version, you may no longer be able to open it\n" +
                    "using a regular version of WorldPainter!", "Loading Non-snapshot World");
        }

        Set<Warning> warnings = newWorld.getWarnings();
        if ((warnings != null) && (! warnings.isEmpty())) {
            DesktopUtils.beep();
            for (Warning warning: warnings) {
                switch (warning) {
                    case AUTO_BIOMES_DISABLED:
                        if (showOptionDialog(this, "Automatic Biomes were previously enabled for this world but have been disabled.\nPress More Info for more information, including how to reenable it.", "Automatic Biomes Disabled", DEFAULT_OPTION, WARNING_MESSAGE, null, new Object[] {"More Info", "OK"}, "OK") == 0) {
                            try {
                                DesktopUtils.open(new URL("https://www.worldpainter.net/doc/legacy/newautomaticbiomes"));
                            } catch (MalformedURLException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        break;
                    case AUTO_BIOMES_ENABLED:
                        if (showOptionDialog(this, "Automatic Biomes were previously disabled for this world but have been enabled.\nPress More Info for more information, including how to disable it.", "Automatic Biomes Enabled", DEFAULT_OPTION, WARNING_MESSAGE, null, new Object[] {"More Info", "OK"}, "OK") == 0) {
                            try {
                                DesktopUtils.open(new URL("https://www.worldpainter.net/doc/legacy/newautomaticbiomes"));
                            } catch (MalformedURLException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        break;
                    case MISSING_CUSTOM_TERRAINS:
                        showWarning(this, "One or more Custom Terrain Types were missing. This can happen in rare\n" +
                                "circumstances; for example by using Undo after removing a Custom Terrain\n" +
                                "Type. The missing Custom Terrain Type(s) have been replaced with Magenta\n" +
                                "Wool and will have to be reconfigured from the Custom Terrain panel.", "Missing Custom Terrain Types");
                        break;
                    case SUPERFLAT_SETTINGS_RESET:
                        showWarning(this, "The Superflat preset from this world could not be parsed.\n" +
                                "It has been reset to default values.", "Superflat Preset Reset");
                        break;
                    case GAME_TYPE_RESET:
                        showWarning(this, "The Game Mode from this world was lost.\n" +
                                "It has been reset to Surbial.", "Game Mode Reset");
                        break;
                }
            }
        }

        if (newWorld.isAskToConvertToAnvil() && (newWorld.getMaxHeight() == DEFAULT_MAX_HEIGHT_MCREGION) && (newWorld.getImportedFrom() == null)) {
            if (showConfirmDialog(this, strings.getString("this.world.is.128.blocks.high"), strings.getString("convert.world.height"), YES_NO_OPTION) == YES_OPTION) {
                ProgressDialog.executeTask(this, new ProgressTask<Void>() {
                    @Override
                    public String getName() {
                        return "Changing world height";
                    }

                    @Override
                    public Void execute(ProgressReceiver progressReceiver) throws OperationCancelled {
                        WorldUtils.resizeWorld(newWorld, HeightTransform.IDENTITY, 0, DEFAULT_MAX_HEIGHT_ANVIL, true, progressReceiver);
                        return null;
                    }
                }, NOT_CANCELABLE);
                newWorld.addHistoryEntry(HistoryEntry.WORLD_MAX_HEIGHT_CHANGED, DEFAULT_MAX_HEIGHT_ANVIL);
                // Force the version to "Anvil" if it was previously exported
                // with the old format
                if (newWorld.getPlatform() != null) {
                    newWorld.setPlatform(DefaultPlugin.JAVA_ANVIL);
                }
                
                // Log event
                config.logEvent(new EventVO(EVENT_KEY_ACTION_MIGRATE_HEIGHT).addTimestamp());
            }
            // Don't ask again, no matter what the user answered
            newWorld.setAskToConvertToAnvil(false);
        }
        
        if (newWorld.isAskToRotate() && (newWorld.getUpIs() == Direction.WEST) && (newWorld.getImportedFrom() == null)) {
            if (showConfirmDialog(this, strings.getString("this.world.was.created.when.north.was.to.the.right"), strings.getString("rotate.world"), YES_NO_OPTION) == YES_OPTION) {
                ProgressDialog.executeTask(this, new ProgressTask<java.lang.Void>() {
                    @Override
                    public String getName() {
                        return strings.getString("rotating.world");
                    }

                    @Override
                    public java.lang.Void execute(ProgressReceiver progressReceiver) throws OperationCancelled {
                        newWorld.transform(CoordinateTransform.ROTATE_CLOCKWISE_270_DEGREES, progressReceiver);
                        for (Dimension dimension: newWorld.getDimensions()) {
                            final Anchor anchor = dimension.getAnchor();
                            if (((anchor.role == DETAIL) || (anchor.role == MASTER)) && (! anchor.invert)) {
                                newWorld.addHistoryEntry(HistoryEntry.WORLD_DIMENSION_ROTATED, dimension.getName(), 270);
                            }
                        }
                        return null;
                    }
                }, NOT_CANCELABLE);
                
                // Log event
                config.logEvent(new EventVO(EVENT_KEY_ACTION_MIGRATE_ROTATION).addTimestamp());
            }
            // Don't ask again, no matter what the user answered
            newWorld.setAskToRotate(false);
        }

        if (! loadedFromAutosave) {
            // Make sure the world name is always the same as the file name, to
            // avoid confusion, unless the only difference is illegal filename
            // characters changed into underscores. Do this here as well as when
            // saving, because the file might have been renamed
            File originalFile = loadedFromBackup ? getOriginalFile(file) : file;
            String name = originalFile.getName();
            if (name.toLowerCase().endsWith(".world")) {
                name = name.substring(0, name.length() - 6);
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

            addRecentlyUsedWorld(originalFile);
        }
        setWorld(newWorld, ! (loadedFromBackup || loadedFromAutosave));
    }

    /**
     * Stop autosaving temporarily (until {@link #resumeAutosave()} is called).
     * Can be called multiple times; autosaving will not be resumed until
     * {@link #resumeAutosave()} has been called as many times as this method
     * was. Must be called on the event thread.
     */
    public void pauseAutosave() {
        pauseAutosave++;
    }

    /**
     * Resume autosaving (if it is enabled and not otherwise inhibited, only
     * after calling the method as many times as {@link #pauseAutosave()} was,
     * and only after the configured guard time). Must be called on the event
     * thread.
     */
    public void resumeAutosave() {
        autosaveInhibitedUntil = System.currentTimeMillis() + Configuration.getInstance().getAutosaveDelay();
        pauseAutosave = Math.max(pauseAutosave - 1, 0);
    }


    public void selectPaint(String paintId) {
        if (paintId == null) {
            throw new NullPointerException();
        } else {
            for (Enumeration<AbstractButton> e = paintButtonGroup.getElements(); e.hasMoreElements(); ) {
                final AbstractButton button = e.nextElement();
                final String buttonPaintId = (String) button.getClientProperty(KEY_PAINT_ID);
                if ((buttonPaintId != null) && buttonPaintId.equals(paintId)) {
                    // Make sure that the dock the button is on is showing:
                    Component parent = button.getParent();
                    while (parent != null) {
                        if (parent instanceof DockableFrame) {
                            if (! parent.isShowing()) {
                                dockingManager.showFrame(((DockableFrame) parent).getKey());
                            }
                            break;
                        }
                        parent = parent.getParent();
                    }
                    // Make sure that the button itself is selected:
                    if (! button.isSelected()) {
                        button.setSelected(true);
                    }
                    return;
                }
            }
            // If we reach here it might be a non-custom biome, which don't all have buttons so we delegate to the
            // biomes panel
            if (paintId.startsWith("Layer/Biome/")) {
                biomesPanel.selectBiome(Integer.parseInt(paintId.substring(12)));
                if (! biomesPanelFrame.isShowing()) {
                    dockingManager.showFrame("biomes");
                }
                return;
            }
        }
        throw new IllegalArgumentException("No button found for paint " + paint);
    }

    boolean changeWorldHeight(Window parent) {
        if ((world == null) || (dimension == null)) {
            DesktopUtils.beep();
            return false;
        }
        ChangeHeightDialog dialog = new ChangeHeightDialog(parent, world);
        dialog.setVisible(true);
        if (! dialog.isCancelled()) {
            view.refreshTiles();
            if (threeDeeFrame != null) {
                threeDeeFrame.refresh(false);
            }
            brushOptions.setMinHeight(dimension.getMinHeight());
            brushOptions.setMaxHeight(dimension.getMaxHeight());
            return true;
        } else {
            return false;
        }
    }

    void shiftWorld(Window parent) {
        if ((world == null) || (dimension == null)) {
            DesktopUtils.beep();
            return;
        } else if ((world.getImportedFrom() != null) && (showConfirmDialog(parent, "This world was imported from an existing map!\nIf you shift it you will no longer be able to merge it properly.\nAre you sure you want to shift the world?", strings.getString("imported"), YES_NO_OPTION, WARNING_MESSAGE) != YES_OPTION)) {
            return;
        }
        view.setInhibitUpdates(true);
        try {
            ShiftWorldDialog dialog = new ShiftWorldDialog(parent, world, dimension.getAnchor());
            dialog.setVisible(true);
            if (! dialog.isCancelled()) {
                currentUndoManager.armSavePoint();
                if (threeDeeFrame != null) {
                    threeDeeFrame.refresh(true);
                }
            }
        } finally {
            view.setInhibitUpdates(false);
        }
    }

    void rotateWorld(Window parent) {
        if ((world == null) || (dimension == null)) {
            DesktopUtils.beep();
            return;
        } else if ((world.getImportedFrom() != null) && (showConfirmDialog(parent, strings.getString("this.world.was.imported.from.an.existing.map"), strings.getString("imported"), YES_NO_OPTION, WARNING_MESSAGE) != YES_OPTION)) {
            return;
        }
        view.setInhibitUpdates(true);
        try {
            RotateWorldDialog dialog = new RotateWorldDialog(parent, world, dimension.getAnchor());
            dialog.setVisible(true);
            if (! dialog.isCancelled()) {
                currentUndoManager.armSavePoint();
                if (threeDeeFrame != null) {
                    threeDeeFrame.refresh(true);
                }
            }
        } finally {
            view.setInhibitUpdates(false);
        }
    }
    
    void scaleWorld(Window parent) {
        if ((world == null) || (dimension == null)) {
            DesktopUtils.beep();
            return;
        } else if ((world.getImportedFrom() != null) && (showConfirmDialog(parent, "This world was imported from an existing map!\nIf you scale it you will no longer be able to merge it properly.\nAre you sure you want to scale the world?", strings.getString("imported"), YES_NO_OPTION, WARNING_MESSAGE) != YES_OPTION)) {
            return;
        }
        view.setInhibitUpdates(true);
        try {
            ScaleWorldDialog dialog = new ScaleWorldDialog(parent, world, dimension.getAnchor());
            dialog.setVisible(true);
            if (! dialog.isCancelled()) {
                currentUndoManager.armSavePoint();
                if (threeDeeFrame != null) {
                    threeDeeFrame.refresh(true);
                }
            }
        } finally {
            view.setInhibitUpdates(false);
        }
    }

    private void addRecentlyUsedWorld(File file) {
        // For some reason (Java bug? Java 8 bug?) the files passed in are
        // sometimes Win32ShellFolder2 instances, instead of Files, which causes
        // problems when serializing/deserializing, so make sure they are Files:
        file = file.getAbsoluteFile();
        Configuration config = Configuration.getInstance();
        List<File> recentFiles = config.getRecentFiles();
        if (recentFiles == null) {
            recentFiles = new ArrayList<>();
            config.setRecentFiles(recentFiles);
        }
        recentFiles.remove(file);
        recentFiles.add(0, file);
        while (recentFiles.size() > MAX_RECENT_FILES) {
            recentFiles.remove(recentFiles.size() - 1);
        }
        updateRecentMenu();
    }

    private void updateRecentMenu() {
        recentMenu.removeAll();
        for (Iterator<File> i = Configuration.getInstance().getRecentFiles().iterator(); i.hasNext(); ) {
            File recentFile = i.next();
            if (recentFile.isFile() && recentFile.canRead()) {
                JMenuItem menuItem = new JMenuItem(recentFile.getName());
                menuItem.addActionListener(e -> {
                    if (recentFile.isFile()) {
                        open(recentFile, true);
                    } else {
                        JOptionPane.showMessageDialog(App.this, "The file " + recentFile.getName() + " no longer exists\nin " + recentFile.getParent(), "File Removed", JOptionPane.ERROR_MESSAGE);
                    }
                });
                recentMenu.add(menuItem);
            } else {
                i.remove();
            }
        }
        recentMenu.setEnabled(recentMenu.getMenuComponentCount() > 0);
    }

    public static Mode getMode() {
        return mode;
    }

    public static void setMode(Mode mode) {
        App.mode = mode;
    }

    void reset3DViewAlwaysOnTop() {
        if (threeDeeFrame != null) {
            threeDeeFrame.resetAlwaysOnTop();
        }
    }

    // PropertyChangeListener

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (programmaticChange) {
            return;
        }
        programmaticChange = true;
        try {
            if (evt.getSource() == world) {
                lastChangeTimestamp = System.currentTimeMillis();
                if (evt.getPropertyName().equals("platform")) {
                    doOnEventThread(this::configureForPlatform);
                }
            }
        } finally {
            programmaticChange = false;
        }
    }

    // Dimension.Listener

    @Override
    public void tilesAdded(Dimension dimension, Set<Tile> tiles) {
        for (Tile tile: tiles) {
            tile.addListener(this);
        }
        lastChangeTimestamp = System.currentTimeMillis();
    }

    @Override
    public void tilesRemoved(Dimension dimension, Set<Tile> tiles) {
        for (Tile tile: tiles) {
            tile.removeListener(this);
        }
        lastChangeTimestamp = System.currentTimeMillis();
    }

    @Override public void overlayAdded(Dimension dimension, int index, Overlay overlay) {}
    @Override public void overlayRemoved(Dimension dimension, int index, Overlay overlay) {}

    // Tile.Listener

    @Override
    public void heightMapChanged(Tile tile) {
        lastChangeTimestamp = System.currentTimeMillis();
    }

    @Override
    public void terrainChanged(Tile tile) {
        lastChangeTimestamp = System.currentTimeMillis();
    }

    @Override
    public void waterLevelChanged(Tile tile) {
        lastChangeTimestamp = System.currentTimeMillis();
    }

    @Override
    public void layerDataChanged(Tile tile, Set<Layer> changedLayers) {
        lastChangeTimestamp = System.currentTimeMillis();
    }

    @Override
    public void allBitLayerDataChanged(Tile tile) {
        lastChangeTimestamp = System.currentTimeMillis();
    }

    @Override
    public void allNonBitlayerDataChanged(Tile tile) {
        lastChangeTimestamp = System.currentTimeMillis();
    }

    @Override
    public void seedsChanged(Tile tile) {
        lastChangeTimestamp = System.currentTimeMillis();
    }

    // RadiusControl

    @Override
    public int getRadius() {
        return radius;
    }

    @Override
    public void increaseRadius(int steps) {
        if (radius == 0) {
            setRadius(1);
        } else {
            final double factor = Math.pow(1.1, steps);
            int newRadius = (int) (radius * factor);
            if (newRadius == radius) {
                newRadius++;
            }
            setRadius(newRadius);
        }
    }

    @Override
    public void increaseRadiusByOne() {
        setRadius(radius + 1);
    }
    
    @Override
    public void decreaseRadius(int steps) {
        final double factor = Math.pow(0.9, steps);
        int newRadius = (int) (radius * factor);
        if (newRadius == radius) {
            newRadius--;
        }
        setRadius(newRadius);
    }

    @Override
    public void decreaseRadiusByOne() {
        setRadius(radius - 1);
    }

    @Override
    public void setRadius(int radius) {
        if (radius < 0) {
            radius = 0;
        } else if (radius > maxRadius) {
            radius = maxRadius;
        }
        if (radius == this.radius) {
            return;
        }
        this.radius = radius;
        if (activeOperation instanceof BrushOperation) {
            ((BrushOperation) activeOperation).setRadius(radius);
        }
        view.setRadius(radius);
        updateRadiusLabel();
    }

    @Override
    public int getRotation() {
        return (activeOperation instanceof PaintOperation) ? brushRotation : toolBrushRotation;
    }

    @Override
    public void setRotation(int rotation) {
        if ((rotation < -180) || (rotation > 180)) {
            rotation = mod(rotation + 180, 360) - 180;
        }
        brushRotationSlider.setValue(rotation);
        if (activeOperation instanceof PaintOperation) {
            brushRotation = rotation;
        } else {
            toolBrushRotation = rotation;
        }
        updateBrushRotation();
    }

    // SeedListener
    
    @Override
    public void setSeed(long seed, Generator generator) {
        if (world != null) {
            for (Dimension dimension: world.getDimensions()) {
                dimension.setMinecraftSeed(seed);
                if (dimension.getGenerator() instanceof SeededGenerator) {
                    ((SeededGenerator) dimension.getGenerator()).setSeed(seed);
                }
            }
        }
    }

    // MapDragControl

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

    public static App getInstance() {
        if (getInstanceIfExists() == null) {
            new App();
        }
        return getInstanceIfExists();
    }

    public static App getInstanceIfExists() {
        return (App) MainFrame.getMainFrame();
    }

    /**
     * Offer to save the current world, but only if is dirty.
     * 
     * @return {@code true} if there are no unsaved changes, the user saved the changes, or the user indicated that
     * unsaved changes may be discarded (in other words, a destructive operation may proceed), {@code false} if there
     * were unsaved changes and the user did not save them or indicate that they may be discarded (in other words, a
     * destructive operation should be cancelled).
     */
    public boolean saveIfNecessary() {
        if (logger.isDebugEnabled()) {
            logger.debug("Last saved state: {}", lastSavedState);
        }
        pauseAutosave();
        try {
            if ((world != null) && (world.getChangeNo() != lastSavedState)) {
                int action = showConfirmDialog(this, (lastSelectedFile != null) ? (MessageFormat.format(strings.getString("there.are.unsaved.changes.do.you.want.to.save.the.world.to.0"), lastSelectedFile.getName())) : strings.getString("there.are.unsaved.changes"));
                if (action == YES_OPTION) {
                    if (! save()) {
                        // The file was not saved for some reason
                        return false;
                    }
                } else if (action != NO_OPTION) {
                    // User closed the confirmation dialog without making a choice
                    return false;
                }
            }
            // If we get here then either the world didn't need saving; it *was* saved; or the user indicated it didn't
            // need saving. In all cases any autosave file should no longer exist, so make sure to rotate it away if
            // necessary
            try {
                rotateAutosaveFile();
            } catch (RuntimeException | Error e) {
                logger.error("An exception occurred while trying to rotate the autosave", e);
                beepAndShowWarning(this, "An error occurred while trying to clear the autosave.\nWorldPainter may try to load the autosave on the next start.\nIf this keeps happening, please report it to the author.", "Clearing Autosave Failed");
            }
            return true;
        } finally {
            resumeAutosave();
        }
    }

    public boolean editCustomMaterial(int customMaterialIndex) {
        MixedMaterial material = Terrain.getCustomMaterial(customMaterialIndex);
        CustomMaterialDialog dialog;
        if (material == null) {
            material = MixedMaterial.create(world.getPlatform(), Material.DIRT);
            dialog = new CustomMaterialDialog(App.this, world.getPlatform(), material, world.isExtendedBlockIds(), selectedColourScheme);
        } else {
            dialog = new CustomMaterialDialog(App.this, world.getPlatform(), material, world.isExtendedBlockIds(), selectedColourScheme);
        }
        dialog.setVisible(true);
        if (! dialog.isCancelled()) {
            material = MixedMaterialManager.getInstance().register(material);
            Terrain.setCustomMaterial(customMaterialIndex, material);
            customMaterialButtons[customMaterialIndex].setIcon(new ImageIcon(material.getIcon(selectedColourScheme)));
            customMaterialButtons[customMaterialIndex].setToolTipText(MessageFormat.format(strings.getString("customMaterial.0.right.click.to.change"), material));
            view.refreshTiles();
            if (threeDeeFrame != null) {
                threeDeeFrame.refresh(false);
            }
            return true;
        }
        return false;
    }

    public void deselectTool() {
        if (activeOperation != null) {
            activeOperation.interrupt();
        }
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
    public List<Layer> getAllLayers() {
        final List<Layer> allLayers = new ArrayList<>(layers);
        allLayers.add(Populate.INSTANCE);
        if (layerControls.get(ReadOnly.INSTANCE).isEnabled()) {
            allLayers.add(ReadOnly.INSTANCE);
        }
        allLayers.addAll(getCustomLayers());
        return allLayers;
    }

    /**
     * Gets all currently loaded custom layers, including hidden ones (from the panel or the view), regardless of
     * whether they are used on the map.
     */
    public List<CustomLayer> getCustomLayers() {
        return customLayerController.getCustomLayers();
    }

    /**
     * Gets all currently loaded custom layers, including hidden ones (from the panel or the view), regardless of
     * whether they are used on the map, by palette (which will be {@code null} for hidden layers). For the visible
     * layers the collections will be in the order they are displayed on the palette.
     */
    public Map<String, Collection<CustomLayer>> getCustomLayersByPalette() {
        return customLayerController.getCustomLayersByPalette();
    }

    public ColourScheme getColourScheme() {
        return selectedColourScheme;
    }

    public void setFilter(Filter filter) {
        if (programmaticChange) {
            return;
        }
        if (activeOperation instanceof PaintOperation) {
            this.filter = filter;
        } else {
            toolFilter = filter;
        }
        if (activeOperation instanceof FilteredOperation) {
            ((FilteredOperation) activeOperation).setFilter(filter);
        }
    }

    public CustomBiomeManager getCustomBiomeManager() {
        return customBiomeManager;
    }

    public void showCustomTerrainButtonPopup(final AWTEvent event, final int customMaterialIndex) {
        final JToggleButton button = (customMaterialIndex >= 0) ? customMaterialButtons[customMaterialIndex] : null;
        // This is sometimes invoked when the source is not showing. No idea why, but it has been observed in the wild.
        // TODO: find out why and solve the underlying issue
        if ((button != null) ? (! button.isShowing()) : ((event.getSource() instanceof Component) && (! ((Component) event.getSource()).isShowing()))) {
            DesktopUtils.beep();
            logger.warn("Event source {} not showing; not opening popup", event.getSource());
            return;
        }
        JPopupMenu popupMenu = new BetterJPopupMenu();
        final MixedMaterial material = (customMaterialIndex >= 0) ? Terrain.getCustomMaterial(customMaterialIndex) : null;
//        JLabel label = new JLabel(MessageFormat.format(strings.getString("current.material.0"), (material != null) ? material : "none"));
//        popupMenu.add(label);

        JMenuItem menuItem;
        if (button == null) {
            menuItem = new JMenuItem(strings.getString("select.custom.material") + "...");
            menuItem.addActionListener(e -> {
                MixedMaterial newMaterial = MixedMaterial.create(world.getPlatform(), Material.DIRT);
                CustomMaterialDialog dialog = new CustomMaterialDialog(App.this, world.getPlatform(), newMaterial, world.isExtendedBlockIds(), selectedColourScheme);
                dialog.setVisible(true);
                if (! dialog.isCancelled()) {
                    newMaterial = MixedMaterialManager.getInstance().register(newMaterial);
                    int index = findNextCustomTerrainIndex();
                    addButtonForNewCustomTerrain(index, newMaterial, true);
                }
            });
            popupMenu.add(menuItem);
        }

        MixedMaterial[] customMaterials = MixedMaterialManager.getInstance().getMaterials();
        if (customMaterials.length > 0) {
            JMenu existingMaterialsMenu = new JMenu("Select existing material");
            Set<MixedMaterial> customTerrainMaterials = new HashSet<>();
            for (int i = 0; i < CUSTOM_TERRAIN_COUNT; i++) {
                if (getCustomTerrain(i).isConfigured()) {
                    customTerrainMaterials.add(Terrain.getCustomMaterial(i));
                }
            }
            for (final MixedMaterial customMaterial: customMaterials) {
                if (customTerrainMaterials.contains(customMaterial)) {
                    continue;
                }
                menuItem = new JMenuItem(customMaterial.getName());
                menuItem.setIcon(new ImageIcon(customMaterial.getIcon(selectedColourScheme)));
                menuItem.addActionListener(e -> {
                    if (button != null) {
                        Terrain.setCustomMaterial(customMaterialIndex, customMaterial);
                        button.setIcon(new ImageIcon(customMaterial.getIcon(selectedColourScheme)));
                        button.setToolTipText(MessageFormat.format(strings.getString("customMaterial.0.right.click.to.change"), customMaterial));
                        view.refreshTiles();
                    } else {
                        addButtonForNewCustomTerrain(findNextCustomTerrainIndex(), customMaterial, true);
                    }
                });
                existingMaterialsMenu.add(menuItem);
            }
            if (existingMaterialsMenu.getMenuComponentCount() > 0) {
                popupMenu.add(existingMaterialsMenu);
            }
        }

        if (button != null) {
            menuItem = new JMenuItem(((material != null) ? "Edit custom material" : strings.getString("select.custom.material")) + "...");
            menuItem.addActionListener(e -> {
                if (editCustomMaterial(customMaterialIndex)) {
                    button.setSelected(true);
                    paintUpdater = () -> {
                        paint = PaintFactory.createTerrainPaint(getCustomTerrain(customMaterialIndex));
                        paintChanged();
                    };
                    paintUpdater.updatePaint();
                }
            });
            popupMenu.add(menuItem);
        }

        menuItem = new JMenuItem("Import from file...");
        menuItem.addActionListener(e -> {
            if (button != null) {
                if (importCustomMaterial(customMaterialIndex)) {
                    button.setSelected(true);
                    paintUpdater = () -> {
                        paint = PaintFactory.createTerrainPaint(getCustomTerrain(customMaterialIndex));
                        paintChanged();
                    };
                    paintUpdater.updatePaint();
                }
            } else {
                MixedMaterial customMaterial = MixedMaterialHelper.load(this);
                if (customMaterial != null) {
                    addButtonForNewCustomTerrain(findNextCustomTerrainIndex(), customMaterial, true);
                }
            }
        });
        popupMenu.add(menuItem);

        menuItem = new JMenuItem("Import from another world...");
        menuItem.addActionListener(e -> importCustomItemsFromWorld(CustomItemsTreeModel.ItemType.TERRAIN, null));
        popupMenu.add(menuItem);

        if (button != null) {
            if (material != null) {
                menuItem = new JMenuItem("Remove...");
                menuItem.addActionListener(e -> removeCustomMaterial(customMaterialIndex));
                popupMenu.add(menuItem);

                menuItem = new JMenuItem("Export to file...");
                menuItem.addActionListener(e -> exportCustomMaterial(customMaterialIndex));
                popupMenu.add(menuItem);
            }

            popupMenu.show(button, button.getWidth(), 0);
        } else {
            Component invoker = (Component) event.getSource();
            popupMenu.show(invoker, invoker.getWidth(), 0);
        }
    }

    public void showHelp(Component component) {
        String helpKey = null;
        do {
            if ((component instanceof AbstractButton) && (((AbstractButton) component).getAction() != null) && (((AbstractButton) component).getAction().getValue(KEY_HELP_KEY) != null)) {
                helpKey = (String) ((AbstractButton) component).getAction().getValue(KEY_HELP_KEY);
            } else if (component instanceof JComponent) {
                helpKey = (String) ((JComponent) component).getClientProperty(KEY_HELP_KEY);
            } else if (component instanceof RootPaneContainer) {
                helpKey = (String) ((RootPaneContainer) component).getRootPane().getClientProperty(KEY_HELP_KEY);
            }
            component = component.getParent();
        } while ((helpKey == null) && (component != null));
        if (helpKey == null) {
            throw new IllegalArgumentException("No help key found in hierarchy");
        }
        try {
            DesktopUtils.open(new URL(HELP_ROOT_URL + encodeForURL(helpKey.toLowerCase())));
        } catch (MalformedURLException e) {
            throw new RuntimeException("Malformed help URL: " + HELP_ROOT_URL + encodeForURL(helpKey), e);
        }
    }

    public void selectPaintOnMap(Set<PaintType> paintTypes, SelectionListener selectionListener) {
        mapSelectionController.selectPaintOnMap(paintTypes, selectionListener);
    }

    public void cancelPaintSelection() {
        mapSelectionController.cancelPaintSelection(true, false);
        eyedropperToggleButton.setSelected(false);
    }

    // BrushOptions.Listener

    @Override
    public void filterChanged(Filter newFilter) {
        setFilter(newFilter);
    }

    // CustomBiomeListener
    
    @Override
    public void customBiomeAdded(CustomBiome customBiome) {
        if ((! programmaticChange) && (dimension != null)) {
            // It's possible that the biome ID already exists in the world, for instance if the user removed a biome and
            // then performed an undo, so repaint it
            view.refreshTiles();
        }
    }

    @Override
    public void customBiomeChanged(CustomBiome customBiome) {
        if ((! programmaticChange) && (dimension != null)) {
            view.refreshTiles();
        }
    }

    @Override
    public void customBiomeRemoved(CustomBiome customBiome) {
        if ((! programmaticChange) && (dimension != null)) {
            ProgressDialog.executeTask(this, new ProgressTask<Void>() {
                @Override
                public String getName() {
                    return "Removing custom biome " + customBiome.getName();
                }

                @Override
                public Void execute(ProgressReceiver progressReceiver) {
                    dimension.armSavePoint();
                    int customBiomeId = customBiome.getId();
                    boolean biomesChanged = false;
                    for (Tile tile: dimension.getTiles()) {
                        if (tile.hasLayer(Biome.INSTANCE)) {
                            tile.inhibitEvents();
                            try {
                                boolean allCustomOrAuto = true;
                                for (int x = 0; x < TILE_SIZE; x++) {
                                    for (int y = 0; y < TILE_SIZE; y++) {
                                        int layerValue = tile.getLayerValue(Biome.INSTANCE, x, y);
                                        if (layerValue == customBiomeId) {
                                            tile.setLayerValue(Biome.INSTANCE, x, y, 255);
                                            biomesChanged = true;
                                        } else if (layerValue != 255) {
                                            allCustomOrAuto = false;
                                        }
                                    }
                                }
                                if (allCustomOrAuto) {
                                    // This tile was completely filled with the custom biome and/or automatic biome.
                                    // Since we're replacing it with automatic biome, which is the default layer value,
                                    // we can delete the layer data to conserve space
                                    tile.clearLayerData(Biome.INSTANCE);
                                }
                            } finally {
                                tile.releaseEvents();
                            }
                        }
                    }
                    if (biomesChanged) {
                        dimension.armSavePoint();
                    }
                    return null;
                }
            }, NOT_CANCELABLE);
        }
    }
    
    // DockableHolder

    @Override
    public DockingManager getDockingManager() {
        return dockingManager;
    }

    // JFrame

    @Override
    public void setTitle(String title) {
        StringBuilder sb = new StringBuilder();
        sb.append(title);
        if (Version.isSnapshot()) {
            sb.append(" [SNAPSHOT]");
        }
        if (Configuration.getInstance().isSafeMode()) {
            sb.append(" [SAFE MODE]");
        }
        super.setTitle(sb.toString());
    }

    void exit() {
        if (saveIfNecessary()) {
            System.exit(0);
        }
    }

    void pushGlassPaneComponent(Component component) {
        glassPane.pushPanelComponent(component);
    }

    Component popGlassPaneComponent() {
        return glassPane.popPanelComponent();
    }

    void removeGlassPaneComponents() {
        glassPane.removePanelComponents();
    }

    private String encodeForURL(String str) {
        String[] parts = str.split("/");
        try {
            for (int i = 0; i < parts.length; i++) {
                parts[i] = URLEncoder.encode(parts[i], "UTF-8");
            }
        } catch (UnsupportedEncodingException e) {
            throw new InternalError("VM does not support mandatory encoding UTF-8");
        }
        return String.join("/", parts);
    }

    private int findNextCustomTerrainIndex() {
        for (int i = 0; i < CUSTOM_TERRAIN_COUNT; i++) {
            if (! Terrain.isCustomMaterialConfigured(i)) {
                return i;
            }
        }
        return -1;
    }

    void addButtonForNewCustomTerrain(int index, MixedMaterial customMaterial, boolean select) {
        Terrain.setCustomMaterial(index, customMaterial);

        if (customTerrainPanel == null) {
            dockingManager.addFrame(new DockableFrameBuilder(createCustomTerrainPanel(), "Custom Terrain", DOCK_SIDE_WEST, 3).withId("customTerrain").scrollable().build());
        }

        JToggleButton newButton = createTerrainButton(Terrain.getCustomTerrain(index));
        customMaterialButtons[index] = newButton;
        newButton.setToolTipText(MessageFormat.format(strings.getString("customMaterial.0.right.click.to.change"), customMaterial));
        addMaterialSelectionTo(newButton, index);
        customTerrainPanel.add(newButton, customTerrainPanel.getComponentCount() - 1);
        customTerrainPanel.validate();
        if (Terrain.getConfiguredCustomMaterialCount() == CUSTOM_TERRAIN_COUNT) {
            ACTION_SHOW_CUSTOM_TERRAIN_POPUP.setEnabled(false);
        }

        if (select) {
            newButton.setSelected(true);
            paintUpdater = () -> {
                paint = PaintFactory.createTerrainPaint(getCustomTerrain(index));
                paintChanged();
            };
            paintUpdater.updatePaint();
            dockingManager.activateFrame("customTerrain");
        }
    }

    boolean performUndo() {
        if ((currentUndoManager != null) && currentUndoManager.undo()) {
            currentUndoManager.armSavePoint();
            return true;
        } else {
            return false;
        }
    }

    boolean performRedo() {
        if ((currentUndoManager != null) && currentUndoManager.redo()) {
            currentUndoManager.armSavePoint();
            return true;
        } else {
            return false;
        }
    }

    /**
     * {@link JLabel#setText(String)} does not check whether the new value is
     * different. On the other hand {@link JLabel#getText()} is a very simple
     * method which just returns a field. So if the text will frequently not
     * have changed, it is cheaper to check with {@code getText()} whether
     * the text is different and only invoke {@code setText()} if it is,
     * which is what this method does.
     *
     * @param label The label on which to set the {@code text} property.
     * @param text The text to set.
     */
    private void setTextIfDifferent(JLabel label, String text) {
        if (! label.getText().equals(text)) {
            label.setText(text);
        }
    }

    private void loadCustomBrushes() {
        customBrushes = new TreeMap<>();
        if (! Configuration.getInstance().isSafeMode()) {
            final File brushesDir = new File(Configuration.getConfigDir(), "brushes");
            if (brushesDir.isDirectory()) {
                loadCustomBrushes(CUSTOM_BRUSHES_DEFAULT_TITLE, brushesDir);
                final Configuration config = Configuration.getInstance();
                if ((customBrushes.size() == 1)
                        && customBrushes.containsKey(CUSTOM_BRUSHES_DEFAULT_TITLE)
                        && (customBrushes.get(CUSTOM_BRUSHES_DEFAULT_TITLE).brushes.size() >= 25)
                        && (! config.isMessageDisplayed(BRUSH_FOLDER_TIP_KEY))) {
                    StartupMessages.addMessage("Tip: you have many Custom Brushes in one folder.\n" +
                            "You can distribute your Custom Brushes over\n" +
                            "multiple palettes by moving them to\n" +
                            "separate subfolders");
                    config.setMessageDisplayed(BRUSH_FOLDER_TIP_KEY);
                }
            }
        } else {
            logger.info("[SAFE MODE] Not loading custom brushes");
        }
    }
    
    private void loadCustomBrushes(String category, File brushesDir) {
        final File[] files = brushesDir.listFiles(new java.io.FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (pathname.isDirectory()) {
                    return true;
                }
                String name = pathname.getName();
                for (String extension: extensions) {
                    if (name.toLowerCase().endsWith(extension)) {
                        return true;
                    }
                }
                return false;
            }

            private final String[] extensions = ImageIO.getReaderFileSuffixes();
        });
        if ((files == null) || (files.length == 0)) {
            // No idea how files could be null since this is only invoked from loadCustomBrushes(), which checks that
            // the path is an existing directory, but it has been observed in the wild
            return;
        }
        final List<Brush> brushes = new ArrayList<>();
        BufferedImage icon = null;
        for (File file: files) {
            if (file.isDirectory()) {
                loadCustomBrushes(file.getName(), file);
            } else if (file.isFile()) {
                if (file.getName().equalsIgnoreCase("icon.png")) {
                    try {
                        icon = ImageIO.read(file);
                    } catch (Exception e) {
                        logger.error("There was an error loading the brush group icon file icon.png; skipping icon file", e);
                    }
                } else {
                    try {
                        brushes.add(new BitmapBrush(file));
                    } catch (RuntimeException e) {
                        logger.error("There was an error loading custom brush image file " + file.getName() + "; skipping file", e);
                    }
                }
            } else {
                logger.warn("Skipping file " + file + "; it is neither a file nor a directory");
            }
        }
        if (! brushes.isEmpty()) {
            customBrushes.put(category, new BrushGroup(category, icon, brushes));
        }
    }
    
    private void maybePing() {
        Configuration config = Configuration.getInstance();
        if (config.getPingAllowed() == null) {
            int rc = showConfirmDialog(this, strings.getString("may.we.have.your.permission"), strings.getString("usage.statistics.permission"), YES_NO_OPTION);
            if (rc == YES_OPTION) {
                config.setPingAllowed(Boolean.TRUE);
            } else if (rc == NO_OPTION) {
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
        Main.privateContext.submitUsageData(usageVO, false);
    }

    private void newWorld() {
        if (! saveIfNecessary()) {
            return;
        }
        Configuration config = Configuration.getInstance();
        final NewWorldDialog dialog = new NewWorldDialog(this, selectedColourScheme, strings.getString("generated.world"), DEFAULT_OCEAN_SEED, config.getDefaultPlatform(), NORMAL_DETAIL, config.getDefaultPlatform().minZ, config.getDefaultMaxHeight(), null);
        dialog.setVisible(true);
        if (! dialog.isCancelled()) {
            clearWorld(); // Free up memory of the world and the undo buffers
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
            EventVO event = new EventVO(EVENT_KEY_ACTION_NEW_WORLD).addTimestamp();
            event.setAttribute(ATTRIBUTE_KEY_MAX_HEIGHT, newWorld.getMaxHeight());
            event.setAttribute(ATTRIBUTE_KEY_TILES, newWorld.getDimension(NORMAL_DETAIL).getTileCount());
            config.logEvent(event);

            setWorld(newWorld, true);
            lastSelectedFile = null;
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
        File selectedFile = FileUtils.selectFileForOpen(this, "Select a WorldPainter world", dir,
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

                    @Override
                    public String getExtensions() {
                        return "*.world";
                    }
                });
        if (selectedFile != null) {
            if (! selectedFile.isFile()) {
                if (logger.isDebugEnabled()) {
                    try {
                        logger.debug("Path not a file according to File.isFile(): \"" + selectedFile + "\" (directory: " + selectedFile.isDirectory() + "; length: " + selectedFile.length() + "; absolutePath: \"" + selectedFile.getAbsolutePath() + "\"; canonicalPath: \"" + selectedFile.getCanonicalPath() + "\")");
                    } catch (IOException e) {
                        logger.debug("Path not a file according to File.isFile(): \"" + selectedFile + "\" (directory: " + selectedFile.isDirectory() + "; length: " + selectedFile.length() + "; absolutePath: \"" + selectedFile.getAbsolutePath() + "\")");
                        logger.warn("I/O error while trying to report canonical path of file: \"" + selectedFile + "\"", e);
                    }
                }
                showMessageDialog(this, "The specified path does not exist or is not a file", "File Does Not Exist", ERROR_MESSAGE);
                return;
            }
            if (! selectedFile.canRead()) {
                showMessageDialog(this, "WorldPainter is not authorised to read the selected file", "Access Denied", ERROR_MESSAGE);
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
     * @return {@code true} if the file was saved.
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
     * @return {@code true} if the file was saved.
     */
    private boolean saveAs() {
        if (world == null) {
            DesktopUtils.beep();
            return false;
        }
        pauseAutosave();
        try {
            Configuration config = Configuration.getInstance();
            File file = lastSelectedFile;
            if (file == null) {
                if ((config != null) && (config.getWorldDirectory() != null)) {
                    file = new File(config.getWorldDirectory(), org.pepsoft.util.FileUtils.sanitiseName(world.getName().trim() + ".world"));
                } else {
                    file = new File(DesktopUtils.getDocumentsFolder(), org.pepsoft.util.FileUtils.sanitiseName(world.getName().trim() + ".world"));
                }
            }
            file = FileUtils.selectFileForSave(App.this, "Save as a WorldPainter world", file, new FileFilter() {
                @Override
                public boolean accept(File f) {
                    return f.isDirectory()
                            || f.getName().toLowerCase().endsWith(".world");
                }

                @Override
                public String getDescription() {
                    return strings.getString("worldpainter.files.world");
                }

                @Override
                public String getExtensions() {
                    return "*.world";
                }
            });
            if (file != null) {
                if (!file.getName().toLowerCase().endsWith(".world")) {
                    file = new File(file.getParentFile(), file.getName() + ".world");
                }
                if (file.exists() && (showConfirmDialog(App.this, strings.getString("do.you.want.to.overwrite.the.file"), strings.getString("file.exists"), YES_NO_OPTION) != YES_OPTION)) {
                    return false;
                }
                if (save(file)) {
                    showInfo(App.this, strings.getString("file.saved"), strings.getString("success"));
                    return true;
                }
            }
            return false;
        } finally {
            resumeAutosave();
        }
    }
    
    /**
     * Save the world to the specified file, overwriting it if it already exists
     * without asking for confirmation. Shows a progress indicator while saving.
     * 
     * @param file The file to which to save the world.
     */
    private boolean save(File file) {
        if (world == null) {
            DesktopUtils.beep();
            return false;
        }
        pauseAutosave();
        try {
            // Check for write access to directory
            if (! file.getParentFile().isDirectory()) {
                showMessageDialog(this, strings.getString("the.selected.path.does.not.exist"), strings.getString("non.existant.path"), ERROR_MESSAGE);
                return false;
            }
            if (! file.getParentFile().canWrite()) {
                showMessageDialog(this, strings.getString("you.do.not.have.write.access"), strings.getString("access.denied"), ERROR_MESSAGE);
                return false;
            }

            // Normalise the filename
            String name = file.getName();
            name = name.trim();
            if (name.isEmpty()) {
                name = strings.getString("generated.world") + ".world"; // NOI18N
            } else {
                name = org.pepsoft.util.FileUtils.sanitiseName(name);
            }
            final File normalisedFile = new File(file.getParentFile(), name);

            // Fail early if the temp save file already exists
            final File tempFile = new File (normalisedFile.getParentFile(), normalisedFile.getName() + ".tmp");
            if (tempFile.exists()) {
                logger.error("Temporary save file {} already exists", tempFile);
                beepAndShowError(this, "A previous save attempt has failed\nand left temporary save file " + tempFile.getName() + " behind.\nPlease remove or rename that file and try again.", "Temporary Save File Exists");
                return false;
            }

            // Make sure the world name is always the same as the file name, to avoid confusion (unless the only
            // difference is illegal filename characters changed into underscores
            final int p = name.lastIndexOf('.');
            if (p != -1) {
                name = name.substring(0, p).trim();
            }
            final String worldName = world.getName();
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

            logger.info("Saving world " + world.getName() + " to " + file.getAbsolutePath());

            saveCustomMaterials();
            saveCustomBiomes();
            saveCustomLayers();

            if (dimension != null) {
                this.dimension.setLastViewPosition(view.getViewCentreInWorldCoords());
            }

            final Configuration config = Configuration.getInstance();
            ProgressDialog.executeTask(this, new ProgressTask<java.lang.Void>() {
                @Override
                public String getName() {
                    return strings.getString("saving.world");
                }

                @Override
                public java.lang.Void execute(ProgressReceiver progressReceiver) throws OperationCancelled {
                    if (tempFile.exists()) {
                        throw new RuntimeException("Temporary file " + tempFile.getName() + " already exists; delete it and try again");
                    }
                    try {
                        // Save the world to a temporary file first, to ensure that there is enough space
                        world.addHistoryEntry(HistoryEntry.WORLD_SAVED, normalisedFile);
                        final WorldIO worldIO = new WorldIO(world);
                        worldIO.save(new FileOutputStream(tempFile));

                        // If that succeeded, move the existing file out of the way by rotating (if enabled) or deleting
                        // it
                        if (normalisedFile.isFile()) {
                            if (config.getWorldFileBackups() > 0) {
                                progressReceiver.setMessage(strings.getString("creating.backup.s"));
                                for (int i = config.getWorldFileBackups(); i > 0; i--) {
                                    final File nextBackupFile = (i > 1) ? BackupUtils.getBackupFile(normalisedFile, i - 1) : normalisedFile;
                                    if (nextBackupFile.isFile()) {
                                        final File backupFile = BackupUtils.getBackupFile(normalisedFile, i);
                                        if (backupFile.isFile()) {
                                            if (! backupFile.delete()) {
                                                throw new IOException("Could not delete old backup file " + backupFile.getName());
                                            }
                                        }
                                        if (! nextBackupFile.renameTo(backupFile)) {
                                            throw new IOException("Could not move " + nextBackupFile.getName() + " to " + backupFile.getName());
                                        }
                                    }
                                }
                                progressReceiver.setMessage(null);
                            } else if (! normalisedFile.delete()) {
                                throw new IOException("Could not delete existing file " + normalisedFile.getName() + "; world is saved as " + tempFile.getName());
                            }
                        }

                        // Finally, move the temporary file to the final location
                        if (! tempFile.renameTo(normalisedFile)) {
                            throw new IOException("Could not move " + tempFile.getName() + " to " + normalisedFile.getName() + "; world is saved as " + tempFile.getName());
                        }

                        Map<String, byte[]> layoutData = config.getJideLayoutData();
                        if (layoutData == null) {
                            layoutData = new HashMap<>();
                        }
                        layoutData.put(dimension.getId().toString(), dockingManager.getLayoutRawData());
                        config.setJideLayoutData(layoutData);

                        return null;
                    } catch (IOException e) {
                        throw new RuntimeException("I/O error saving file (message: " + e.getMessage() + ")", e);
                    }
                }
            }, NOT_CANCELABLE);

            // Log an event
            final EventVO event = new EventVO(EVENT_KEY_ACTION_SAVE_WORLD).addTimestamp();
            event.setAttribute(ATTRIBUTE_KEY_MAX_HEIGHT, world.getMaxHeight());
            Dimension loadedDimension = world.getDimension(NORMAL_DETAIL);
            event.setAttribute(ATTRIBUTE_KEY_TILES, loadedDimension.getTileCount());
            logLayers(loadedDimension, event, "");
            loadedDimension = world.getDimension(NETHER_DETAIL);
            if (loadedDimension != null) {
                event.setAttribute(ATTRIBUTE_KEY_NETHER_TILES, loadedDimension.getTileCount());
                logLayers(loadedDimension, event, "nether.");
            }
            loadedDimension = world.getDimension(END_DETAIL);
            if (loadedDimension != null) {
                event.setAttribute(ATTRIBUTE_KEY_END_TILES, loadedDimension.getTileCount());
                logLayers(loadedDimension, event, "end.");
            }
            if (world.getImportedFrom() != null) {
                event.setAttribute(ATTRIBUTE_KEY_IMPORTED_WORLD, true);
            }
            config.logEvent(event);

            if (currentUndoManager != null) {
                currentUndoManager.armSavePoint();
            }
            lastSaveTimestamp = lastChangeTimestamp = System.currentTimeMillis();
            lastAutosavedState = lastSavedState = world.getChangeNo();
            try {
                rotateAutosaveFile();
            } catch (RuntimeException | Error e) {
                logger.error("An exception occurred while trying to rotate the autosave", e);
                beepAndShowWarning(this, "An error occurred while trying to clear the autosave.\nWorldPainter may try to load the autosave on the next start.\nIf this keeps happening, please report it to the author.", "Clearing Autosave Failed");
            }
            lastSelectedFile = file;
            addRecentlyUsedWorld(file);

            config.setWorldDirectory(file.getParentFile());

            return true;
        } finally {
            resumeAutosave();
        }
    }

    private void rotateAutosaveFile() {
        if (Configuration.getInstance().isAutosaveInhibited()) {
            return;
        }
        try {
            org.pepsoft.util.FileUtils.rotateFile(getAutosaveFile(), "autosave.{0}.world", 0, 3);
        } catch (IOException e) {
            throw new RuntimeException("I/O error while rotating autosave file", e);
        }
    }

    /**
     * Evaluate whether an autosave should be performed given the current change
     * number and the time since the last autosave, and if so, perform one.
     */
    private void maybeAutosave() {
        Configuration config = Configuration.getInstance();
        long now = System.currentTimeMillis();
        if (config.isAutosaveEnabled() && (! config.isAutosaveInhibited()) && (world != null) && (pauseAutosave == 0) && (now >= autosaveInhibitedUntil)) {
            if (world.getChangeNo() != lastAutosavedState) {
                // Autosave is enabled and not paused, a world is loaded, and it has changed since it was last (auto)saved
                long timeSinceLastChange = now - lastChangeTimestamp;
                if (timeSinceLastChange > config.getAutosaveDelay()) {
                    // The last change was longer than the autosave delay ago
                    long timeSinceLastSave = now - lastSaveTimestamp;
                    if (timeSinceLastSave > config.getAutosaveInterval()) {
                        // The last save was longer than the autosave interval ago.
                        // Do the autosave
                        autosave();
                    } else if (logger.isDebugEnabled()) {
                        logger.debug("[AUTOSAVE] World changed, but waiting for autosave interval to expire (since last save: " + timeSinceLastSave + " ms");
                    }
                } else if (logger.isDebugEnabled()) {
                    logger.debug("[AUTOSAVE] World changed, but waiting for guard time to expire (time since last change: " + timeSinceLastChange + " ms)");
                }
            } else {
                logger.debug("[AUTOSAVE] World not changed since last save");
            }
        } else if (logger.isDebugEnabled()) {
            if (! config.isAutosaveEnabled()) {
                logger.debug("[AUTOSAVE] Autosave disabled in configuration");
            } else if (config.isAutosaveInhibited()) {
                logger.debug("[AUTOSAVE] Autosave inhibited (e.g. due to another instance of WorldPainter running)");
            } else if (world == null) {
                logger.debug("[AUTOSAVE] No world loaded");
            } else if (pauseAutosave != 0) {
                logger.debug("[AUTOSAVE] Autosave paused");
            } else {
                logger.debug("[AUTOSAVE] Autosave temporarily inhibited");
            }
        }
    }

    private void autosave() {
        try {
            if (activeOperation != null) {
                activeOperation.interrupt();
            }

            if (logger.isDebugEnabled()) {
                logger.debug("[AUTOSAVE] Autosaving");
            }
            saveCustomMaterials();
            saveCustomBiomes();
            saveCustomLayers();

            if (dimension != null) {
                this.dimension.setLastViewPosition(view.getViewCentreInWorldCoords());
            }

            ProgressDialog.executeTask(this, new ProgressTask<java.lang.Void>() {
                @Override
                public String getName() {
                    return "Autosaving";
                }

                @Override
                public java.lang.Void execute(ProgressReceiver progressReceiver) {
                    try {
                        rotateAutosaveFile();
                        WorldIO worldIO = new WorldIO(world);
                        worldIO.save(new FileOutputStream(getAutosaveFile()));
                    } catch (IOException e) {
                        throw new RuntimeException("I/O error autosaving world (message: " + e.getMessage() + ")", e);
                    }
                    return null;
                }
            }, NOT_CANCELABLE, NO_FOCUS_STEALING); // TODO make cancelable. Close the output stream? That oughta do it. But also move the previous autosave file back...

            lastSaveTimestamp = lastChangeTimestamp = System.currentTimeMillis();
            lastAutosavedState = world.getChangeNo();

            // Log an event
            final EventVO event = new EventVO(EVENT_KEY_ACTION_AUTOSAVE_WORLD).addTimestamp().setTransient();
            Configuration.getInstance().logEvent(event);
        } catch (RuntimeException | Error e) {
            logger.error("An exception occurred while trying to autosave world", e);
            beepAndShowWarning(this, "An error occurred while trying to autosave the world.\n" +
                    "One possibility is that the disk is full; please make space.\n" +
                    "It has not been autosaved. If this keeps happening,\n" +
                    "please report it to the author.", "Autosave Failed");
        }
    }

    private boolean isAutosaveFile(File file) {
        return file.equals(getAutosaveFile());
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
        TileEditor tileEditor = new TileEditor(this, dimension, selectedColourScheme, customBiomeManager, hiddenLayers, false, 10, view.getLightOrigin());
        tileEditor.moveTo(view.getViewLocation());
        tileEditor.setVisible(() -> {
            if (tileEditor.isTilesChanged()) {
                view.refreshTiles();
            }
        });
    }
    
    private void importWorld() {
        if (! saveIfNecessary()) {
            return;
        }
        if (! Configuration.getInstance().isMessageDisplayed(IMPORT_WARNING_KEY)) {
            showInfo(this, strings.getString("the.import.functionality.only.imports.the.i.landscape"), strings.getString("information"));
        }
        MapImportDialog dialog = new MapImportDialog(this);
        dialog.setVisible(true);
        if (! dialog.isCancelled()) {
            World2 importedWorld = dialog.getImportedWorld();
            setWorld(importedWorld, true);
            lastSelectedFile = null;
            Configuration config = Configuration.getInstance();
            config.setMessageDisplayed(IMPORT_WARNING_KEY);
        }
    }
    
    public void importHeightMap(File preselectedFile) {
        if (! saveIfNecessary()) {
            return;
        }
        ImportHeightMapDialog dialog = new ImportHeightMapDialog(this, selectedColourScheme, view.isDrawContours(), view.getContourSeparation(), view.getLightOrigin(), preselectedFile);
        dialog.setVisible(true);
        if (! dialog.isCancelled()) {
            clearWorld();
            World2 importedWorld = dialog.getImportedWorld();
            if (importedWorld != null) {
                setWorld(importedWorld, true);
                lastSelectedFile = null;
            }
        }
    }

    public void importHeightMapIntoCurrentDimension(File preselectedFile) {
        if (dimension == null) {
            DesktopUtils.beep();
            return;
        }
        ImportHeightMapDialog dialog = new ImportHeightMapDialog(this, dimension, selectedColourScheme, customBiomeManager, view.isDrawContours(), view.getContourSeparation(), view.getLightOrigin(), preselectedFile);
        dialog.setVisible(true);
        if (! dialog.isCancelled()) {
            view.refreshTiles();
        }
    }

    public void importMask(File preselectedFile) {
        if (dimension == null) {
            DesktopUtils.beep();
            return;
        }
        final List<Layer> allLayers = new ArrayList<>();
        allLayers.add(Biome.INSTANCE);
        allLayers.add(Annotations.INSTANCE);
        allLayers.addAll(getAllLayers());
        final ImportMaskDialog dialog = new ImportMaskDialog(this, dimension, selectedColourScheme, allLayers, customBiomeManager, preselectedFile);
        dialog.setVisible(true);
    }
    
    private void merge() {
        pauseAutosave();
        try {
            if ((world.getImportedFrom() != null) && (! world.isAllowMerging())) {
                showMessageDialog(this, strings.getString("this.world.was.imported.before.the.great.coordinate.shift"), strings.getString("merge.not.allowed"), ERROR_MESSAGE);
                return;
            }
            if ((world.getImportedFrom() == null) && (world.getMergedWith() == null) && (showConfirmDialog(this, strings.getString("this.world.was.not.imported"), strings.getString("not.imported"), YES_NO_OPTION, WARNING_MESSAGE) != YES_OPTION)) {
                return;
            }
            Configuration config = Configuration.getInstance();
            if (((config == null) || (! config.isMessageDisplayed(MERGE_WARNING_KEY))) && (showConfirmDialog(this, strings.getString("this.is.experimental.and.unfinished.functionality"), strings.getString("experimental.functionality"), YES_NO_OPTION, WARNING_MESSAGE) != YES_OPTION)) {
                return;
            }
            saveCustomBiomes();
            saveCustomLayers();
            MergeWorldDialog dialog = new MergeWorldDialog(this, world, selectedColourScheme, customBiomeManager, hiddenLayers, false, 10, view.getLightOrigin(), view);
            dialog.setVisible(true);
        } finally {
            resumeAutosave();
        }
    }

    private void updateZoomLabel() {
        double factor = Math.pow(2.0, view.getZoom());
        final float scale = (dimension != null) ? dimension.getScale() : 1.0f;
        int zoomPercentage = (int) (100 * factor / scale);
        zoomLabel.setText(MessageFormat.format(strings.getString("zoom.0"), zoomPercentage));
        glassPane.setScale((float) factor / scale);
    }

    private void initComponents() {
        view = new WorldPainter(selectedColourScheme, customBiomeManager);
        Configuration config = Configuration.getInstance();
        if (config.getBackgroundColour() == -1) {
            view.setBackground(new Color(VoidRenderer.getColour()));
        } else {
            view.setBackground(new Color(config.getBackgroundColour()));
        }
        view.setDrawBorders(config.isShowBorders());
        view.setDrawBiomes(config.isShowBiomes());
        view.setViewDistance(config.getViewDistance());
        view.setBackgroundImageMode(config.getBackgroundImageMode());
        if (config.getBackgroundImage() != null) {
            if (! config.isSafeMode()) {
                new Thread("Background Image Loader") {
                    @Override
                    public void run() {
                        try {
                            BufferedImage image = ImageIO.read(config.getBackgroundImage());
                            SwingUtilities.invokeLater(() -> view.setBackgroundImage(image));
                        } catch (IOException e) {
                            logger.error("I/O error loading background image; disabling background image", e);
                        }
                    }
                }.start();
            } else {
                logger.info("[SAFE MODE] Not loading background image");
            }
        }
        view.setRadius(radius);
        view.setBrushShape(brush.getBrushShape());

        glassPane = new GlassPane();
        final BufferedImage cursorImage = IconUtils.loadUnscaledImage("org/pepsoft/worldpainter/cursor.png");
        final java.awt.Dimension bestCursorSize = Toolkit.getDefaultToolkit().getBestCursorSize(Math.round(32 * getUIScale()), Math.round(32 * getUIScale()));
        if ((bestCursorSize.width != 0) && (bestCursorSize.height == bestCursorSize.width)) {
            int hotspot = 15;
            if (bestCursorSize.width != 32) {
                hotspot = round(15 * ((float) bestCursorSize.width / 32));
            }
            final Cursor cursor = Toolkit.getDefaultToolkit().createCustomCursor(cursorImage, new Point(hotspot, hotspot), "Custom Crosshair");
            glassPane.setCursor(cursor);
        }
        JRootPane privateRootPane = new JRootPane();
        privateRootPane.putClientProperty(KEY_HELP_KEY, "Editor");
        privateRootPane.setContentPane(view);
        privateRootPane.setGlassPane(glassPane);
        glassPane.setVisible(true);
        TiledImageViewerContainer viewContainer = new TiledImageViewerContainer(privateRootPane);

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
        // Stop JIDE from swallowing the Esc key
        dockingManager.getMainContainer().unregisterKeyboardAction(getKeyStroke(VK_ESCAPE, 0));
        Workspace workspace = dockingManager.getWorkspace();
        workspace.setLayout(new BorderLayout());
        workspace.add(viewContainer, BorderLayout.CENTER);

        setJMenuBar(createMenuBar());
        
        getContentPane().add(createToolBar(), BorderLayout.NORTH);

        getContentPane().add(createStatusBar(), BorderLayout.SOUTH);

        scrollController.install();

        dockingManager.addFrame(new DockableFrameBuilder(createToolPanel(), "Tools", DOCK_SIDE_WEST, 1).build());

        dockingManager.addFrame(new DockableFrameBuilder(createToolSettingsPanel(), "Tool Settings", DOCK_SIDE_WEST, 2).expand().scrollable().build());

        dockingManager.addFrame(new DockableFrameBuilder(createLayerPanel(), "Layers", DOCK_SIDE_WEST, 3).build());

        dockingManager.addFrame(new DockableFrameBuilder(createTerrainPanel(), "Terrain", DOCK_SIDE_WEST, 3).build());

        biomesPanelFrame = new DockableFrameBuilder(createBiomesPanelContainer(), "Biomes", DOCK_SIDE_WEST, 3).scrollable().build();
        dockingManager.addFrame(biomesPanelFrame);

        dockingManager.addFrame(new DockableFrameBuilder(createAnnotationsPanel(), "Annotations", DOCK_SIDE_WEST, 3).build());

        dockingManager.addFrame(new DockableFrameBuilder(createBrushPanel(), "Brushes", DOCK_SIDE_EAST, 1).build());

        if (customBrushes.containsKey(CUSTOM_BRUSHES_DEFAULT_TITLE)) {
            dockingManager.addFrame(new DockableFrameBuilder(createCustomBrushPanel(CUSTOM_BRUSHES_DEFAULT_TITLE, customBrushes.get(CUSTOM_BRUSHES_DEFAULT_TITLE)), "Custom Brushes", DOCK_SIDE_EAST, 1).withId("customBrushesDefault").scrollable().build());
        }
        for (Map.Entry<String, BrushGroup> entry: customBrushes.entrySet()) {
            if (entry.getKey().equals(CUSTOM_BRUSHES_DEFAULT_TITLE)) {
                continue;
            }
            dockingManager.addFrame(new DockableFrameBuilder(createCustomBrushPanel(entry.getKey(), entry.getValue()), entry.getKey(), DOCK_SIDE_EAST, 1).withId("customBrushes." + entry.getKey()).scrollable().build());
        }
        
        dockingManager.addFrame(new DockableFrameBuilder(createBrushSettingsPanel(), "Brush Settings", DOCK_SIDE_EAST, 2).withId("brushSettings").build());

        infoPanel = createInfoPanel();
        dockingManager.addFrame(new DockableFrameBuilder(infoPanel, "Info", DOCK_SIDE_EAST, 2).withId("infoPanel").expand().withIcon(loadScaledIcon("information")).build());

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
                    int oldZoom = view.getZoom(), zoom;
                    if (e.getWheelRotation() < 0) {
                        zoom = Math.min(oldZoom - e.getWheelRotation(), 6);
                    } else {
                        zoom = Math.max(oldZoom - e.getWheelRotation(), -4);
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
                } else if (activeOperation instanceof BrushOperation) {
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

        if (config.getShowCalloutCount() > 0) {
            BufferedImage callout = loadCallout("callout_1");
            view.addOverlay("callout_1", 0, dockingManager.getFrame("tools"), callout);
            callout = loadCallout("callout_2");
            view.addOverlay("callout_2", -callout.getWidth(), dockingManager.getFrame("brushes"), callout);
            callout = loadCallout("callout_3");
            view.addOverlay("callout_3", 0, dockingManager.getFrame("layers"), callout);
            config.setShowCalloutCount(config.getShowCalloutCount() - 1);
        }

        JRootPane rootPane = getRootPane();
        ActionMap actionMap = rootPane.getActionMap();
        actionMap.put(ACTION_NAME_INCREASE_RADIUS, new BetterAction(ACTION_NAME_INCREASE_RADIUS, strings.getString("increase.radius")) {
            @Override
            public void performAction(ActionEvent e) {
                increaseRadius(1);
            }

            @Serial
            private static final long serialVersionUID = 2011090601L;
        });
        actionMap.put(ACTION_NAME_INCREASE_RADIUS_BY_ONE, new BetterAction(ACTION_NAME_INCREASE_RADIUS_BY_ONE, "Increase brush radius by one") {
            @Override
            public void performAction(ActionEvent e) {
                increaseRadiusByOne();
            }

            @Serial
            private static final long serialVersionUID = 2011090601L;
        });
        actionMap.put(ACTION_NAME_DECREASE_RADIUS, new BetterAction(ACTION_NAME_DECREASE_RADIUS, strings.getString("decrease.radius")) {
            @Override
            public void performAction(ActionEvent e) {
                decreaseRadius(1);
            }

            @Serial
            private static final long serialVersionUID = 2011090601L;
        });
        actionMap.put(ACTION_NAME_DECREASE_RADIUS_BY_ONE, new BetterAction(ACTION_NAME_DECREASE_RADIUS_BY_ONE, "Decrease brush radius by one") {
            @Override
            public void performAction(ActionEvent e) {
                decreaseRadiusByOne();
            }

            @Serial
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
        actionMap.put(ACTION_ESCAPE.getName(), ACTION_ESCAPE);

        int platformCommandMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        inputMap.put(getKeyStroke(VK_SUBTRACT, 0),                                     ACTION_NAME_DECREASE_RADIUS);
        inputMap.put(getKeyStroke(VK_MINUS,    0),                                     ACTION_NAME_DECREASE_RADIUS);
        inputMap.put(getKeyStroke(VK_ADD,      0),                                     ACTION_NAME_INCREASE_RADIUS);
        inputMap.put(getKeyStroke(VK_EQUALS,   SHIFT_DOWN_MASK),                       ACTION_NAME_INCREASE_RADIUS);
        inputMap.put(getKeyStroke(VK_SUBTRACT, SHIFT_DOWN_MASK),                       ACTION_NAME_DECREASE_RADIUS_BY_ONE);
        inputMap.put(getKeyStroke(VK_MINUS,    SHIFT_DOWN_MASK),                       ACTION_NAME_DECREASE_RADIUS_BY_ONE);
        inputMap.put(getKeyStroke(VK_ADD,      SHIFT_DOWN_MASK),                       ACTION_NAME_INCREASE_RADIUS_BY_ONE);
        inputMap.put(getKeyStroke(VK_Z,        platformCommandMask | SHIFT_DOWN_MASK), ACTION_NAME_REDO);
        inputMap.put(getKeyStroke(VK_MINUS,    platformCommandMask),                   ACTION_NAME_ZOOM_OUT);
        inputMap.put(getKeyStroke(VK_EQUALS,   platformCommandMask | SHIFT_DOWN_MASK), ACTION_NAME_ZOOM_IN);
        inputMap.put(getKeyStroke(VK_NUMPAD0,  platformCommandMask),                   ACTION_ZOOM_RESET.getName());
        inputMap.put(getKeyStroke(VK_SUBTRACT, ALT_DOWN_MASK),                         ACTION_ROTATE_BRUSH_LEFT.getName());
        inputMap.put(getKeyStroke(VK_MINUS,    ALT_DOWN_MASK),                         ACTION_ROTATE_BRUSH_LEFT.getName());
        inputMap.put(getKeyStroke(VK_ADD,      ALT_DOWN_MASK),                         ACTION_ROTATE_BRUSH_RIGHT.getName());
        inputMap.put(getKeyStroke(VK_EQUALS,   ALT_DOWN_MASK | SHIFT_DOWN_MASK),       ACTION_ROTATE_BRUSH_RIGHT.getName());
        inputMap.put(getKeyStroke(VK_0,        ALT_DOWN_MASK),                         ACTION_ROTATE_BRUSH_RESET.getName());
        inputMap.put(getKeyStroke(VK_NUMPAD0,  ALT_DOWN_MASK),                         ACTION_ROTATE_BRUSH_RESET.getName());
        inputMap.put(getKeyStroke(VK_3,        ALT_DOWN_MASK),                         ACTION_ROTATE_BRUSH_RIGHT_30_DEGREES.getName());
        inputMap.put(getKeyStroke(VK_NUMPAD3,  ALT_DOWN_MASK),                         ACTION_ROTATE_BRUSH_RIGHT_30_DEGREES.getName());
        inputMap.put(getKeyStroke(VK_4,        ALT_DOWN_MASK),                         ACTION_ROTATE_BRUSH_RIGHT_45_DEGREES.getName());
        inputMap.put(getKeyStroke(VK_NUMPAD4,  ALT_DOWN_MASK),                         ACTION_ROTATE_BRUSH_RIGHT_45_DEGREES.getName());
        inputMap.put(getKeyStroke(VK_9,        ALT_DOWN_MASK),                         ACTION_ROTATE_BRUSH_RIGHT_90_DEGREES.getName());
        inputMap.put(getKeyStroke(VK_NUMPAD9,  ALT_DOWN_MASK),                         ACTION_ROTATE_BRUSH_RIGHT_90_DEGREES.getName());
        inputMap.put(ACTION_ROTATE_LIGHT_LEFT.getAcceleratorKey(),                     "rotateLightLeft");
        inputMap.put(ACTION_ROTATE_LIGHT_RIGHT.getAcceleratorKey(),                    "rotateLightRight");
        inputMap.put(ACTION_INTENSITY_10_PERCENT.getAcceleratorKey(),                  "intensity10");
        inputMap.put(ACTION_INTENSITY_20_PERCENT.getAcceleratorKey(),                  "intensity20");
        inputMap.put(ACTION_INTENSITY_30_PERCENT.getAcceleratorKey(),                  "intensity30");
        inputMap.put(ACTION_INTENSITY_40_PERCENT.getAcceleratorKey(),                  "intensity40");
        inputMap.put(ACTION_INTENSITY_50_PERCENT.getAcceleratorKey(),                  "intensity50");
        inputMap.put(ACTION_INTENSITY_60_PERCENT.getAcceleratorKey(),                  "intensity60");
        inputMap.put(ACTION_INTENSITY_70_PERCENT.getAcceleratorKey(),                  "intensity70");
        inputMap.put(ACTION_INTENSITY_80_PERCENT.getAcceleratorKey(),                  "intensity80");
        inputMap.put(ACTION_INTENSITY_90_PERCENT.getAcceleratorKey(),                  "intensity90");
        inputMap.put(ACTION_INTENSITY_100_PERCENT.getAcceleratorKey(),                 "intensity100");
        inputMap.put(getKeyStroke(VK_NUMPAD1, 0),                                      "intensity10");
        inputMap.put(getKeyStroke(VK_NUMPAD2, 0),                                      "intensity20");
        inputMap.put(getKeyStroke(VK_NUMPAD3, 0),                                      "intensity30");
        inputMap.put(getKeyStroke(VK_NUMPAD4, 0),                                      "intensity40");
        inputMap.put(getKeyStroke(VK_NUMPAD5, 0),                                      "intensity50");
        inputMap.put(getKeyStroke(VK_NUMPAD6, 0),                                      "intensity60");
        inputMap.put(getKeyStroke(VK_NUMPAD7, 0),                                      "intensity70");
        inputMap.put(getKeyStroke(VK_NUMPAD8, 0),                                      "intensity80");
        inputMap.put(getKeyStroke(VK_NUMPAD9, 0),                                      "intensity90");
        inputMap.put(getKeyStroke(VK_NUMPAD0, 0),                                      "intensity100");
        inputMap.put(ACTION_ESCAPE.getAcceleratorKey(),                        ACTION_ESCAPE.getName());

        programmaticChange = true;
        try {
            selectBrushButton(brush);
            levelSlider.setValue((int) (level * 100));
            brushRotationSlider.setValue(brushRotation);
        } finally {
            programmaticChange = false;
        }
    }

    private InfoPanel createInfoPanel() {
        return new InfoPanel(view, customBiomeManager);
    }

    private BufferedImage loadCallout(String key) {
        try {
            BufferedImage callout = ImageIO.read(App.class.getResourceAsStream("/org/pepsoft/worldpainter/" + key + ".png"));
            callouts.put(key, callout);
            return callout;
        } catch (IOException e) {
            throw new RuntimeException("I/O error loading callout image from classpath", e);
        }
    }

    /**
     * Close the callout with the specified key, if it is currently showing.
     * Does nothing if it is not showing.
     *
     * @param key The key of the callout to close.
     * @return {@code true} if the callout was showing and was closed;
     * {@code false} otherwise.
     */
    private boolean closeCallout(String key) {
        if (callouts.containsKey(key)) {
            view.removeOverlay(key);
            callouts.remove(key);
            return true;
        } else {
            return false;
        }
    }

    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel();
        statusBar.setLayout(new FlowLayout(FlowLayout.LEADING));
        StringBuilder warnings = new StringBuilder();
        Configuration config = Configuration.getInstance();
        if (config.isAutosaveEnabled() && config.isAutosaveInhibited()) {
            warnings.append("Autosave disabled");
        }
        if (config.isSafeMode()) {
            if (warnings.length() > 0) {
                warnings.append(": ");
            }
            warnings.append("Safe mode");
        }
        if (warnings.length() > 0) {
            JLabel warningsLabel = new JLabel(warnings.toString(), IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/error.png"), SwingConstants.LEADING);
            warningsLabel.setBorder(new BevelBorder(BevelBorder.LOWERED));
            statusBar.add(warningsLabel);
        }
        locationLabel = new JLabel(MessageFormat.format(strings.getString("location.0.1"), "-99,999", "-99,999"));
        locationLabel.setBorder(new BevelBorder(BevelBorder.LOWERED));
        statusBar.add(locationLabel);
        heightLabel = new JLabel(MessageFormat.format(strings.getString("height.0.of.1"), "-9,999", "9,999"));
        heightLabel.setBorder(new BevelBorder(BevelBorder.LOWERED));
        statusBar.add(heightLabel);
        slopeLabel = new JLabel("Slope: 90°");
        slopeLabel.setBorder(new BevelBorder(BevelBorder.LOWERED));
        statusBar.add(slopeLabel);
        materialLabel = new JLabel(MessageFormat.format(strings.getString("material.0"), Material.MOSSY_COBBLESTONE.toString()));
        materialLabel.setBorder(new BevelBorder(BevelBorder.LOWERED));
        statusBar.add(materialLabel);
        waterLabel = new JLabel(MessageFormat.format(strings.getString("fluid.level.1.depth.2"), 0, "-9,999", "9,999"));
        waterLabel.setBorder(new BevelBorder(BevelBorder.LOWERED));
        statusBar.add(waterLabel);
        biomeLabel = new JLabel("Auto biome: Mega Spruce Taiga Hills (ID 161)");
        biomeLabel.setBorder(new BevelBorder(BevelBorder.LOWERED));
        statusBar.add(biomeLabel);
        radiusLabel = new JLabel(MessageFormat.format(strings.getString("radius.0"), 15984));
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
                if (logger.isDebugEnabled()) {
                    logger.debug("Forcing garbage collect");
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
        toolPanel.add(createButtonForOperation(new SprayPaint(view), 'r'));
        toolPanel.add(createButtonForOperation(new Pencil(view), 'p'));
        toolPanel.add(createButtonForOperation(new Fill(view), 'l'));
        toolPanel.add(createButtonForOperation(new Text(view), 'x'));

        toolPanel.add(createButtonForOperation(new Flood(view, false), 'f'));
        toolPanel.add(createButtonForOperation(new Flood(view, true)));
        toolPanel.add(createButtonForOperation(new Sponge(view)));
        eyedropperToggleButton = new JToggleButton(loadScaledIcon("eyedropper"));
        eyedropperToggleButton.setMnemonic('y');
        eyedropperToggleButton.setMargin(App.BUTTON_INSETS);
        eyedropperToggleButton.addActionListener(e -> {
            if (mapSelectionController.isSelectionActive()) {
                // The user clicked on the eyedropper while it was already selected
                cancelPaintSelection();
                return;
            } else if (dimension == null) {
                eyedropperToggleButton.setSelected(false);
                DesktopUtils.beep();
                return;
            }
            mapSelectionController.selectPaintOnMap(null, new SelectionListener() {
                @Override
                public void terrainSelected(Terrain terrain) {
                    eyedropperToggleButton.setSelected(false);
                    App.getInstance().selectPaint(createTerrainPaintId(terrain));
                }

                @Override
                public void layerSelected(Layer layer, int value) {
                    eyedropperToggleButton.setSelected(false);
                    if (layer.discrete) {
                        App.getInstance().selectPaint(createDiscreteLayerPaintId(layer, value));
                    } else {
                        App.getInstance().selectPaint(createLayerPaintId(layer));
                    }
                }

                @Override public void selectionCancelled(boolean byUser) {
                    eyedropperToggleButton.setSelected(false);
                }
            });
        });
        eyedropperToggleButton.setToolTipText("Eyedropper: Select a paint from the map (Alt+y)");
        eyedropperToggleButton.putClientProperty(KEY_HELP_KEY, "Operation/Eyedropper");
        toolPanel.add(eyedropperToggleButton);

        toolPanel.add(createButtonForOperation(new Height(view), 'h'));
        toolPanel.add(createButtonForOperation(new Flatten(view), 'a'));
        toolPanel.add(createButtonForOperation(new Smooth(view), 's'));
        toolPanel.add(createButtonForOperation(new RaiseMountain(view), 'm'));

//        toolPanel.add(createButtonForOperation(new Erode(view, this, mapDragControl), 'm'));
        toolPanel.add(createButtonForOperation(new SetSpawnPoint(view)));
        final JButton button = new JButton(loadScaledIcon("globals"));
        button.setMargin(App.BUTTON_INSETS);
        button.addActionListener(e -> showGlobalOperations());
        button.setToolTipText(strings.getString("global.operations.fill.or.clear.the.world.with.a.terrain.biome.or.layer"));
        button.putClientProperty(KEY_HELP_KEY, "Operation/GlobalOperations");
        toolPanel.add(button);
        toolPanel.add(createButtonForOperation(new RaiseRotatedPyramid(view)));
        toolPanel.add(createButtonForOperation(new RaisePyramid(view)));

        final AbstractButton copySelectionButton = createButtonForOperation(new CopySelectionOperation(view));
        copySelectionButton.setEnabled(selectionState.getValue());
        toolPanel.add(createButtonForOperation(new EditSelectionOperation(view, selectionState)));
        toolPanel.add(copySelectionButton);
        final JButton clearSelectionButton = new JButton(loadScaledIcon("clear_selection"));
        clearSelectionButton.setEnabled(selectionState.getValue());
        clearSelectionButton.setMargin(App.BUTTON_INSETS);
        clearSelectionButton.addActionListener(e -> {
            if (dimension == null) {
                DesktopUtils.beep();
                return;
            }
            if (dimension.containsOneOf(SelectionChunk.INSTANCE, SelectionBlock.INSTANCE)) {
                dimension.setEventsInhibited(true);
                try {
                    new SelectionHelper(dimension).clearSelection();
                    dimension.armSavePoint();
                } finally {
                    dimension.setEventsInhibited(false);
                }
            } else {
                DesktopUtils.beep();
            }
            if (activeOperation instanceof CopySelectionOperation) {
                deselectTool();
            }
            // TODO: make this work correctly with undo/redo, and make "inside selection" ineffective when there is no selection, to avoid confusion
//            selectionState.setValue(false);
        });
        clearSelectionButton.setToolTipText("Clear the selection");
        selectionState.addObserver((o, selectionMayBePresent) -> {
            copySelectionButton.setEnabled((boolean) selectionMayBePresent);
            clearSelectionButton.setEnabled((boolean) selectionMayBePresent);
        });
        clearSelectionButton.putClientProperty(KEY_HELP_KEY, "Operation/ClearSelection");
        toolPanel.add(clearSelectionButton);

        while ((toolPanel.getComponentCount() % 4) != 0) {
            toolPanel.add(Box.createGlue());
        }
        for (Operation operation: operations) {
            operation.setView(view);
            toolPanel.add(createButtonForOperation(operation));
        }

        return toolPanel;
    }

    private JPanel createToolSettingsPanel() {
        toolSettingsPanel = new JPanel(new BorderLayout());
        return toolSettingsPanel;
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
        constraints.anchor = GridBagConstraints.WEST;
        constraints.weightx = 0.0;
        List<Component> terrainComponents = createTerrainDropDown();
        terrainSoloCheckBox = (JCheckBox) terrainComponents.get(1);
        terrainModeComboBox = (JComboBox<TerrainMode>) terrainComponents.get(2);
        LayoutUtils.addRowOfComponents(layerPanel, constraints, terrainComponents);
        LayoutUtils.addRowOfComponents(layerPanel, constraints, createLayerButton(FLUIDS_AS_LAYER, (char) 0, false, false));
        for (Layer layer: layers) {
            LayoutUtils.addRowOfComponents(layerPanel, constraints, createLayerButton(layer, layer.getMnemonic()));
        }
        if (! config.isEasyMode()) {
            LayoutUtils.addRowOfComponents(layerPanel, constraints, createLayerButton(Populate.INSTANCE, 'p'));
        }
        LayoutUtils.addRowOfComponents(layerPanel, constraints, createLayerButton(ReadOnly.INSTANCE, 'o'));

        final JButton addLayerButton = new JButton(loadScaledIcon("plus"));
        addLayerButton.setToolTipText(strings.getString("add.a.custom.layer"));
        addLayerButton.setMargin(App.BUTTON_INSETS);
        addLayerButton.addActionListener(e -> {
            if (dimension == null) {
                DesktopUtils.beep();
                return;
            }
            // This is sometimes invoked while layerPanel is not showing, which results in an error. TODO: find out why
            //  and fix underlying cause
            if (layerPanel.isShowing()) {
                final JPopupMenu customLayerMenu = customLayerController.createCustomLayerMenu(null);
                customLayerMenu.show(layerPanel, addLayerButton.getX() + addLayerButton.getWidth(), addLayerButton.getY());
            }
        });
        JPanel spacer = new JPanel();
        constraints.gridwidth = 1;
        constraints.weightx = 0.0;
        layerPanel.add(spacer, constraints);
        spacer = new JPanel();
        layerPanel.add(spacer, constraints);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.weightx = 1.0;
        layerPanel.add(addLayerButton, constraints);

        layerPanel.putClientProperty(KEY_ICON, ICON_LAYERS);

        return layerPanel;
    }

    private List<Component> createTerrainDropDown() {
        final JComboBox<TerrainMode> comboBox = new JComboBox<>(TerrainMode.values());
        final Icon terrainIcon = new ImageIcon(TERRAIN_AS_LAYER.getIcon());
        final String terrainName = TERRAIN_AS_LAYER.getName();
        final Icon defaultColourRampIcon = new ImageIcon(new DefaultColourRamp(-64, 62, 320).getPreview((int) (80 * getUIScale()), (int) (16 * getUIScale()), -64, 319));
        final Icon defaultGreyscaleRampIcon = new ImageIcon(new ColourGradient(0.0f, 0x000000, 1.0f, 0xffffff, LINEAR).getPreview((int) (80 * getUIScale()), (int) (16 * getUIScale()), 0.0f, 1.0f));
        comboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                switch ((TerrainMode) value) {
                    case SHOW_TERRAIN:
                        setIcon(terrainIcon);
                        setText(terrainName);
                        break;
                    case HIDE_TERRAIN:
                        setIcon(ICON_NO_TERRAIN);
                        setText("Hide Terrain");
                        break;
                    case DEFAULT_COLOUR_RAMP:
                        setIcon(defaultColourRampIcon);
                        setText(null);
                        break;
                    case DEFAULT_GREYSCALE_RAMP:
                        setIcon(defaultGreyscaleRampIcon);
                        setText(null);
                        break;
                }
                return this;
            }
        });
        comboBox.addActionListener(e -> {
            terrainMode = (TerrainMode) comboBox.getSelectedItem();
            switch (terrainMode) {
                case SHOW_TERRAIN:
                    hiddenLayers.remove(TERRAIN_AS_LAYER);
                    break;
                case HIDE_TERRAIN:
                    hiddenLayers.add(TERRAIN_AS_LAYER);
                    view.setColourRamp(null);
                    break;
                case DEFAULT_COLOUR_RAMP:
                    hiddenLayers.add(TERRAIN_AS_LAYER);
                    if (dimension != null) {
                        setDefaultColourRamp();
                    }
                    break;
                case DEFAULT_GREYSCALE_RAMP:
                    hiddenLayers.add(TERRAIN_AS_LAYER);
                    if (dimension != null) {
                        setDefaultGreyscaleRamp();
                    }
                    break;
            }
            updateLayerVisibility();
        });
        return createLayerRow(TERRAIN_AS_LAYER, false, true, comboBox);
    }

    private boolean refreshTerrainMode() {
        switch (terrainMode) {
            case DEFAULT_COLOUR_RAMP:
                return setDefaultColourRamp();
            case DEFAULT_GREYSCALE_RAMP:
                return setDefaultGreyscaleRamp();
        }
        return false;
    }

    private boolean setDefaultColourRamp() {
        final int waterLevel = (dimension.getTileFactory() instanceof HeightMapTileFactory) ? ((HeightMapTileFactory) dimension.getTileFactory()).getWaterHeight() : 62;
        return view.setColourRamp(new DefaultColourRamp(dimension.getMinHeight(), waterLevel, dimension.getMaxHeight()));
    }

    private boolean setDefaultGreyscaleRamp() {
        return view.setColourRamp(new ColourGradient(dimension.getMinHeight(), 0x000000, dimension.getMaxHeight(), 0xffffff, LINEAR));
    }

    private JPanel createBiomesPanelContainer() {
        final JPanel biomesPanelContainer = new JPanel();
        biomesPanelContainer.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(1, 1, 1, 1);

        Configuration config = Configuration.getInstance();
        constraints.anchor = GridBagConstraints.FIRST_LINE_START;
        constraints.weightx = 0.0;
        JCheckBox checkBox = new JCheckBox("Show:");
        checkBox.setHorizontalTextPosition(SwingConstants.LEADING);
        checkBox.setToolTipText("Uncheck to hide biomes from view (it will still be exported)");
        checkBox.addActionListener(e -> {
            if (checkBox.isSelected()) {
                hiddenLayers.remove(Biome.INSTANCE);
            } else {
                hiddenLayers.add(Biome.INSTANCE);
            }
            updateLayerVisibility();
        });
        if (! config.isEasyMode()) {
            constraints.gridwidth = 1;
            constraints.weightx = 0.0;
            biomesPanelContainer.add(checkBox, constraints);
        }

        JCheckBox soloCheckBox = new JCheckBox("Solo:");
        soloCheckBox.setHorizontalTextPosition(SwingConstants.LEADING);
        soloCheckBox.setToolTipText("<html>Check to show <em>only</em> the biomes (the other layers are still exported)</html>");
        soloCheckBox.addActionListener(new SoloCheckboxHandler(soloCheckBox, Biome.INSTANCE));
        layerSoloCheckBoxes.put(Biome.INSTANCE, soloCheckBox);
        if (! config.isEasyMode()) {
            constraints.gridwidth = GridBagConstraints.REMAINDER;
            biomesPanelContainer.add(soloCheckBox, constraints);
        }

        biomesPanel = new BiomesPanel(customBiomeManager, biomeId -> {
            paintUpdater = () -> {
                paint = createDiscreteLayerPaint(Biome.INSTANCE, biomeId);
                paintChanged();
            };
            paintUpdater.updatePaint();
        }, paintButtonGroup);
        constraints.weightx = 1.0;
        constraints.fill = HORIZONTAL;
        biomesPanelContainer.add(biomesPanel, constraints);

        layerControls.put(Biome.INSTANCE, new LayerControls(Biome.INSTANCE, checkBox, soloCheckBox, null));

        biomesPanelContainer.putClientProperty(KEY_ICON, ICON_BIOMES);

        return biomesPanelContainer;
    }

    private JPanel createAnnotationsPanel() {
        final JPanel layerPanel = new JPanel();
        layerPanel.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(1, 1, 1, 1);

        Configuration config = Configuration.getInstance();
        constraints.anchor = GridBagConstraints.FIRST_LINE_START;
        constraints.weightx = 0.0;
        JCheckBox checkBox = new JCheckBox("Show:");
        checkBox.setHorizontalTextPosition(SwingConstants.LEADING);
        checkBox.setSelected(true);
        checkBox.setToolTipText("Uncheck to hide annotations from view");
        checkBox.addActionListener(e -> {
            if (checkBox.isSelected()) {
                hiddenLayers.remove(Annotations.INSTANCE);
            } else {
                hiddenLayers.add(Annotations.INSTANCE);
            }
            updateLayerVisibility();
        });
        if (! config.isEasyMode()) {
            constraints.gridwidth = 1;
            constraints.weightx = 0.0;
            layerPanel.add(checkBox, constraints);
        }

        JCheckBox soloCheckBox = new JCheckBox("Solo:");
        soloCheckBox.setHorizontalTextPosition(SwingConstants.LEADING);
        soloCheckBox.setToolTipText("<html>Check to show <em>only</em> the annotations (the other layers are still exported)</html>");
        soloCheckBox.addActionListener(new SoloCheckboxHandler(soloCheckBox, Annotations.INSTANCE));
        layerSoloCheckBoxes.put(Annotations.INSTANCE, soloCheckBox);
        if (! config.isEasyMode()) {
            constraints.gridwidth = GridBagConstraints.REMAINDER;
            layerPanel.add(soloCheckBox, constraints);
        }

        JPanel colourGrid = new JPanel(new GridLayout(0, 4));
        for (int i = 1; i < 16; i++) {
            final int layerValue = i;
            JToggleButton button = new JToggleButton(createScaledColourIcon(Annotations.getColour(layerValue, selectedColourScheme)));
            button.setToolTipText(Annotations.getColourName(layerValue));
            button.setMargin(App.BUTTON_INSETS);
            if (layerValue == 1) {
                button.setSelected(true);
            }
            paintButtonGroup.add(button);
            button.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    paintUpdater = () -> {
                        paint = createDiscreteLayerPaint(Annotations.INSTANCE, layerValue);
                        paintChanged();
                    };
                    paintUpdater.updatePaint();
                }
            });
            button.putClientProperty(KEY_PAINT_ID, createDiscreteLayerPaintId(Annotations.INSTANCE, layerValue));
            colourGrid.add(button);
        }
        layerPanel.add(colourGrid, constraints);

        layerControls.put(Annotations.INSTANCE, new LayerControls(Annotations.INSTANCE, checkBox, soloCheckBox));

        layerPanel.putClientProperty(KEY_ICON, ICON_ANNOTATIONS);

        return layerPanel;
    }

    @Nullable
    Function<Layer, Boolean> getLayerFilterForCurrentDimension() {
        if (dimension != null) {
            if (dimension.getAnchor().role == CAVE_FLOOR) {
                return TunnelLayer::isLayerSupportedForCaveFloorDimension;
            } else if (dimension.getAnchor().role == FLOATING_FLOOR) {
                return TunnelLayer::isLayerSupportedForFloatingFloorDimension;
            }
        }
        return null;
    }

    private JPanel createTerrainPanel() {
        JPanel terrainPanel = new JPanel();
        terrainPanel.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(1, 1, 1, 1);

        Configuration config = Configuration.getInstance();
        constraints.anchor = GridBagConstraints.FIRST_LINE_START;
        constraints.weightx = 0.0;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        if (! config.isEasyMode()) {
            final JCheckBox checkBoxSoloTerrain = new RemoteJCheckBox(terrainSoloCheckBox, "Solo:");
            checkBoxSoloTerrain.setHorizontalTextPosition(SwingConstants.LEADING);
            checkBoxSoloTerrain.setToolTipText("<html>Check to show <em>only</em> the biomes (the other layers are still exported)</html>");
            terrainPanel.add(checkBoxSoloTerrain, constraints);
        }

        JPanel buttonPanel = new JPanel(new GridLayout(0, 5));
        // Surface
        buttonPanel.add(createTerrainButton(GRASS));
        buttonPanel.add(createTerrainButton(PERMADIRT));
        buttonPanel.add(createTerrainButton(SAND));
        buttonPanel.add(createTerrainButton(GRASS_PATH));
        buttonPanel.add(createTerrainButton(PODZOL));

        buttonPanel.add(createTerrainButton(BARE_GRASS));
        buttonPanel.add(createTerrainButton(MOSS));
        buttonPanel.add(createTerrainButton(MUD));
        buttonPanel.add(createTerrainButton(GRAVEL));
        buttonPanel.add(createTerrainButton(CLAY));

        buttonPanel.add(createTerrainButton(STONE_MIX));
        buttonPanel.add(createTerrainButton(GRANITE));
        buttonPanel.add(createTerrainButton(DIORITE));
        buttonPanel.add(createTerrainButton(ANDESITE));
        buttonPanel.add(createTerrainButton(CALCITE));

        buttonPanel.add(createTerrainButton(COBBLESTONE));
        buttonPanel.add(createTerrainButton(MOSSY_COBBLESTONE));
        buttonPanel.add(createTerrainButton(STONE));
        buttonPanel.add(createTerrainButton(DEEPSLATE));
        buttonPanel.add(createTerrainButton(ROCK));

        buttonPanel.add(createTerrainButton(TUFF));
        buttonPanel.add(createTerrainButton(BEDROCK));
        buttonPanel.add(createTerrainButton(MYCELIUM));
        buttonPanel.add(createTerrainButton(PALE_MOSS));
        buttonPanel.add(Box.createGlue());

        buttonPanel.add(createTerrainButton(OBSIDIAN));
        buttonPanel.add(createTerrainButton(MAGMA));
        buttonPanel.add(createTerrainButton(LAVA));
        buttonPanel.add(Box.createGlue());
        buttonPanel.add(Box.createGlue());

        buttonPanel.add(createTerrainButton(DESERT));
        buttonPanel.add(createTerrainButton(SANDSTONE));
        buttonPanel.add(createTerrainButton(RED_SAND));
        buttonPanel.add(createTerrainButton(RED_SANDSTONE));
        buttonPanel.add(createTerrainButton(RED_DESERT));

        buttonPanel.add(createTerrainButton(WHITE_STAINED_CLAY));
        buttonPanel.add(createTerrainButton(ORANGE_STAINED_CLAY));
        buttonPanel.add(createTerrainButton(MAGENTA_STAINED_CLAY));
        buttonPanel.add(createTerrainButton(LIGHT_BLUE_STAINED_CLAY));
        buttonPanel.add(createTerrainButton(YELLOW_STAINED_CLAY));

        buttonPanel.add(createTerrainButton(LIME_STAINED_CLAY));
        buttonPanel.add(createTerrainButton(PINK_STAINED_CLAY));
        buttonPanel.add(createTerrainButton(GREY_STAINED_CLAY));
        buttonPanel.add(createTerrainButton(LIGHT_GREY_STAINED_CLAY));
        buttonPanel.add(createTerrainButton(CYAN_STAINED_CLAY));

        buttonPanel.add(createTerrainButton(PURPLE_STAINED_CLAY));
        buttonPanel.add(createTerrainButton(BLUE_STAINED_CLAY));
        buttonPanel.add(createTerrainButton(BROWN_STAINED_CLAY));
        buttonPanel.add(createTerrainButton(GREEN_STAINED_CLAY));
        buttonPanel.add(createTerrainButton(RED_STAINED_CLAY));

        buttonPanel.add(createTerrainButton(BLACK_STAINED_CLAY));
        buttonPanel.add(createTerrainButton(HARDENED_CLAY));
        buttonPanel.add(createTerrainButton(MESA));
        buttonPanel.add(Box.createGlue());
        buttonPanel.add(Box.createGlue());

        buttonPanel.add(createTerrainButton(BEACHES));
        buttonPanel.add(createTerrainButton(WATER));
        buttonPanel.add(createTerrainButton(DEEP_SNOW));
        buttonPanel.add(Box.createGlue());
        buttonPanel.add(Box.createGlue());

        // Nether
        buttonPanel.add(createTerrainButton(NETHERRACK));
        buttonPanel.add(createTerrainButton(BASALT));
        buttonPanel.add(createTerrainButton(BLACKSTONE));
        buttonPanel.add(createTerrainButton(NETHERLIKE));
        buttonPanel.add(Box.createGlue());

        buttonPanel.add(createTerrainButton(SOUL_SAND));
        buttonPanel.add(createTerrainButton(SOUL_SOIL));
        buttonPanel.add(createTerrainButton(WARPED_NYLIUM));
        buttonPanel.add(createTerrainButton(CRIMSON_NYLIUM));
        buttonPanel.add(Box.createGlue());

        // End
        buttonPanel.add(createTerrainButton(END_STONE));
        buttonPanel.add(Box.createGlue());
        buttonPanel.add(Box.createGlue());
        buttonPanel.add(Box.createGlue());
        buttonPanel.add(Box.createGlue());

        // Custom terrains
        JButton addCustomTerrainButton = new JButton(ACTION_SHOW_CUSTOM_TERRAIN_POPUP);
        addCustomTerrainButton.setMargin(App.BUTTON_INSETS);
        buttonPanel.add(addCustomTerrainButton);
        terrainPanel.add(buttonPanel, constraints);

        return terrainPanel;
    }
    
    private JPanel createCustomTerrainPanel() {
        customTerrainPanel = new JPanel();
        customTerrainPanel.setLayout(new GridLayout(0, 4));

        JButton addCustomTerrainButton = new JButton(ACTION_SHOW_CUSTOM_TERRAIN_POPUP);
        addCustomTerrainButton.setMargin(App.BUTTON_INSETS);
        customTerrainPanel.add(addCustomTerrainButton);

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

        optionsPanel.putClientProperty(KEY_ICON, createBrushThumbnail(SymmetricBrush.COSINE_CIRCLE, Math.round(16 * getUIScale())));

        return optionsPanel;
    }

    private JPanel createCustomBrushPanel(String title, BrushGroup brushGroup) {
        JPanel customBrushesPanel = new JPanel();
        customBrushesPanel.setLayout(new GridBagLayout());
        JPanel customBrushPanel = new JPanel(new GridLayout(0, 3));
        for (Brush customBrush: brushGroup.brushes) {
            customBrushPanel.add(createBrushButton(customBrush));
        }
        
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.FIRST_LINE_START;
        constraints.weightx = 1.0;
        constraints.insets = new Insets(1, 1, 1, 1);
        customBrushesPanel.add(customBrushPanel, constraints);

        if (brushGroup.icon != null) {
            customBrushesPanel.putClientProperty(KEY_ICON, new ImageIcon(scaleIcon(brushGroup.icon, 16)));
        } else {
            customBrushesPanel.putClientProperty(KEY_ICON, createScaledLetterIcon(title.charAt(0), darkMode ? WHITE : BLACK));
        }

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
        
        constraints.fill = HORIZONTAL;
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
        
        constraints.fill = HORIZONTAL;
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

        brushSettingsPanel.putClientProperty(KEY_ICON, ICON_SETTINGS);

        return brushSettingsPanel;
    }

    private void updateBrushRotation() {
        final int desiredBrushRotation = (activeOperation instanceof PaintOperation) ? brushRotation : toolBrushRotation;
        if (desiredBrushRotation != previousBrushRotation) {
//            long start = System.currentTimeMillis();
            if (activeOperation instanceof BrushOperation) {
                final Brush brush = (activeOperation instanceof PaintOperation) ? this.brush : this.toolBrush;
                if (desiredBrushRotation == 0) {
                    ((BrushOperation) activeOperation).setBrush(brush);
                } else {
                    Brush rotatedBrush = RotatedBrush.rotate(brush, desiredBrushRotation);
                    ((BrushOperation) activeOperation).setBrush(rotatedBrush);
                }
                updateBrushRotation(brush, brushButtons.get(brush));
            }
            view.setBrushRotation(desiredBrushRotation);
//            if (logger.isDebugEnabled()) {
//                logger.debug("Updating brush rotation took " + (System.currentTimeMillis() - start) + " ms");
//            }
            previousBrushRotation = desiredBrushRotation;
        }
    }

    /**
     * Update the image of a single brush button
     */
    private void updateBrushRotation(Brush brush, JToggleButton button) {
        if (! (brush instanceof BitmapBrush)) {
            return;
        }
        final int desiredBrushRotation = (activeOperation instanceof PaintOperation) ? brushRotation : toolBrushRotation;
        final Icon thumbnail = (Icon) button.getClientProperty(KEY_THUMBNAIL);
        if (thumbnail != null) {
            if ((desiredBrushRotation == 0) || (! button.isSelected())){
                button.setIcon(thumbnail);
            } else {
                button.setIcon(IconUtils.rotateIcon(thumbnail, desiredBrushRotation));
            }
        }
    }

    public void editCustomLayer(CustomLayer layer, Runnable callback) {
        customLayerController.editCustomLayer(layer, callback);
    }

    public void deleteCustomLayer(CustomLayer layer) {
        customLayerController.deleteCustomLayer(layer);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(createFileMenu());
        menuBar.add(createEditMenu());
        menuBar.add(createViewMenu());
        Configuration config = Configuration.getInstance();
        if (! config.isEasyMode()) {
            menuBar.add(createToolsMenu());
        }
        menuBar.add(createHelpMenu());
        addStatisticsTo(menuBar, "menu", config);
        return menuBar;
    }
    
    private JMenu createFileMenu() {
        JMenuItem menuItem = new JMenuItem(ACTION_NEW_WORLD);
        menuItem.setMnemonic('n');
        JMenu menu = new JMenu(strings.getString("file"));
        menu.setMnemonic('i');
        menu.add(menuItem);

        menuItem = new JMenuItem(ACTION_OPEN_WORLD);
        menuItem.setMnemonic('o');
        menu.add(menuItem);

        final Configuration config = Configuration.getInstance();
        recentMenu = new JMenu("Recently used Worlds");
        if ((config.getRecentFiles() != null) && (! config.getRecentFiles().isEmpty())) {
            updateRecentMenu();
        } else {
            recentMenu.setEnabled(false);
        }
        menu.add(recentMenu);

        if (! config.isEasyMode()) {
            menuItem = new JMenuItem(ACTION_IMPORT_MAP);
            menuItem.setMnemonic('m');
            menuItem.setText("From Minecraft map...");
            JMenu subMenu = new JMenu(strings.getString("import"));
            subMenu.setMnemonic('i');
            subMenu.add(menuItem);

            menuItem = new JMenuItem(strings.getString("height.map") + "...");
            menuItem.addActionListener(event -> importHeightMap(null));
            menuItem.setMnemonic('h');
            subMenu.add(menuItem);

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
            menuItem.addActionListener(event -> exportHeightMap(INTEGER_LOW_RESOLUTION));
            menuItem.setMnemonic('h');
            exportMenu.add(menuItem);

            menuItem = new JMenuItem("Export as 1:256 (high resolution) integer height map...");
            menuItem.addActionListener(event -> exportHeightMap(INTEGER_HIGH_RESOLUTION));
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
        menu.putClientProperty(KEY_HELP_KEY, "Menu/File");
        return menu;
    }
    
    private JMenu createEditMenu() {
        JMenuItem menuItem = new JMenuItem(ACTION_UNDO);
        menuItem.setMnemonic('u');
        JMenu menu = new JMenu(strings.getString("edit"));
        menu.setMnemonic('e');
        menu.add(menuItem);

        menuItem = new JMenuItem(ACTION_REDO);
        menuItem.setMnemonic('r');
        menu.add(menuItem);

        menu.addSeparator();

        menuItem = new JMenuItem("Change Map Format...");
        menuItem.addActionListener(e -> changeWorldHeight(this));
        menu.add(menuItem);

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

        JMenu dimensionsMenu = new JMenu("Dimensions");

        addMasterMenuItem = new JMenuItem("Add master dimension...");
        addMasterMenuItem.addActionListener(e -> addMaster());
        dimensionsMenu.add(addMasterMenuItem);

        removeMasterMenuItem = new JMenuItem("Remove master dimension...");
        removeMasterMenuItem.addActionListener(e -> removeMaster());
        dimensionsMenu.add(removeMasterMenuItem);

        addCeilingMenuItem = new JMenuItem("Add ceiling dimension...");
        addCeilingMenuItem.addActionListener(e -> addCeiling());
        dimensionsMenu.add(addCeilingMenuItem);

        removeCeilingMenuItem = new JMenuItem("Remove ceiling dimension...");
        removeCeilingMenuItem.addActionListener(e -> removeCeiling());
        dimensionsMenu.add(removeCeilingMenuItem);

        addNetherMenuItem = new JMenuItem(strings.getString("add.nether") + "...");
        addNetherMenuItem.addActionListener(e -> addNether());
        addNetherMenuItem.setMnemonic('n');
        dimensionsMenu.add(addNetherMenuItem);

        removeNetherMenuItem = new JMenuItem("Remove Nether...");
        removeNetherMenuItem.addActionListener(e -> removeNether());
        dimensionsMenu.add(removeNetherMenuItem);

        addEndMenuItem = new JMenuItem(strings.getString("add.end") + "...");
        addEndMenuItem.addActionListener(e -> addEnd());
        addEndMenuItem.setMnemonic('d');
        dimensionsMenu.add(addEndMenuItem);

        removeEndMenuItem = new JMenuItem("Remove End...");
        removeEndMenuItem.addActionListener(e -> removeEnd());
        dimensionsMenu.add(removeEndMenuItem);
        menu.add(dimensionsMenu);

        JMenu importMenu = new JMenu("Import");
        menuItem = new JMenuItem("Custom items from existing world...");
        menuItem.setMnemonic('i');
        menuItem.addActionListener(e -> importCustomItemsFromWorld(CustomItemsTreeModel.ItemType.ALL, getLayerFilterForCurrentDimension()));
        importMenu.add(menuItem);

        menuItem = new JMenuItem(ACTION_IMPORT_LAYER);
        menuItem.setMnemonic('l');
        menuItem.setText("Custom Layer(s) from file(s)...");
        importMenu.add(menuItem);

        menuItem = new JMenuItem("Custom Terrain(s) from file(s)...");
        menuItem.setMnemonic('t');
        menuItem.addActionListener(e -> importCustomMaterials());
        importMenu.add(menuItem);

        menuItem = new JMenuItem("Height map into current dimension...");
        menuItem.addActionListener(e -> importHeightMapIntoCurrentDimension(null));
        importMenu.add(menuItem);

        menuItem = new JMenuItem("Mask as terrain or layer...");
        menuItem.addActionListener(e -> importMask(null));
        importMenu.add(menuItem);

//        menuItem = new JMenuItem("Existing Minecraft map into current world...");
//        menuItem.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                importMapIntoWorld();
//            }
//        });
//        importMenu.add(menuItem);

        menu.add(importMenu);

        Configuration config = Configuration.getInstance();
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

            menuItem = new JMenuItem(ACTION_SCALE_WORLD);
            menuItem.setMnemonic('c');
            menu.add(menuItem);
        }

        menuItem = new JMenuItem(strings.getString("global.operations") + "...");
        menuItem.addActionListener(event -> showGlobalOperations());
        menuItem.setMnemonic('g');
        menuItem.setAccelerator(getKeyStroke(VK_G, PLATFORM_COMMAND_MASK));
        menu.add(menuItem);
        
        menuItem = new JMenuItem(ACTION_EDIT_TILES);
        menuItem.setMnemonic('t');
        menu.add(menuItem);

        menuItem = new JMenuItem("Delete unused layers...");
        menuItem.addActionListener(e -> customLayerController.deleteUnusedLayers());
        menu.add(menuItem);

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
            menuItem.addActionListener(e -> openPreferences());
            menuItem.setMnemonic('f');
            menu.add(menuItem);
        }

        menu.putClientProperty(KEY_HELP_KEY, "Menu/Edit");
        return menu;
    }

    private void openPreferences() {
        final Configuration config = Configuration.getInstance();
        final PreferencesDialog dialog = new PreferencesDialog(App.this, selectedColourScheme);
        dialog.setVisible(true);
        if (! dialog.isCancelled()) {
            setMaxRadius(config.getMaximumBrushSize());
            if (! config.isAutosaveInhibited()) {
                if (config.isAutosaveEnabled()) {
                    autosaveTimer.setInitialDelay(config.getAutosaveDelay());
                    autosaveTimer.setDelay(config.getAutosaveDelay() / 2);
                    if (! autosaveTimer.isRunning()) {
                        autosaveTimer.start();
                    }
                } else {
                    autosaveTimer.stop();
                    try {
                        rotateAutosaveFile();
                    } catch (RuntimeException | Error e2) {
                        logger.error("An exception occurred while trying to rotate the autosave", e2);
                        beepAndShowWarning(this, "An error occurred while trying to clear the autosave.\nWorldPainter may try to load the autosave on the next start.\nIf this keeps happening, please report it to the author.", "Clearing Autosave Failed");
                    }
                }
            }
        }
    }

    private JMenu createViewMenu() {
        JMenuItem menuItem = new JMenuItem(ACTION_ZOOM_IN);
        menuItem.setMnemonic('i');
        JMenu menu = new JMenu(strings.getString("view"));
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
        viewSurfaceMenuItem.addActionListener(e -> viewDimension(NORMAL_DETAIL));
        viewSurfaceMenuItem.setMnemonic('s');
        viewSurfaceMenuItem.setAccelerator(getKeyStroke(VK_U, PLATFORM_COMMAND_MASK));
        menu.add(viewSurfaceMenuItem);

        viewNetherMenuItem = new JCheckBoxMenuItem(strings.getString("view.nether"), false);
        viewNetherMenuItem.addActionListener(e -> viewDimension(NETHER_DETAIL));
        viewNetherMenuItem.setMnemonic('n');
        viewNetherMenuItem.setAccelerator(getKeyStroke(VK_H, PLATFORM_COMMAND_MASK));
        viewNetherMenuItem.setEnabled(false);
        menu.add(viewNetherMenuItem);

        viewEndMenuItem = new JCheckBoxMenuItem(strings.getString("view.end"), false);
        viewEndMenuItem.addActionListener(e -> viewDimension(END_DETAIL));
        viewEndMenuItem.setMnemonic('e');
        viewEndMenuItem.setAccelerator(getKeyStroke(VK_D, PLATFORM_COMMAND_MASK));
        viewEndMenuItem.setEnabled(false);
        menu.add(viewEndMenuItem);

        menu.add(ACTION_SWITCH_TO_FROM_MASTER);
        menu.add(ACTION_SWITCH_TO_FROM_CEILING);

        menu.addSeparator();
        
        final JMenu colourSchemeMenu = new JMenu(strings.getString("change.colour.scheme"));
        // This array must correspond 1:1 with App.colourSchemes as initialised in the constructor:
        final String[] colourSchemeNames = {strings.getString("default"), null, null, null, "DokuDark", "DokuHigh", "DokuLight", "Misa", "Sphax"};
        final ButtonGroup schemesButtonGroup = new ButtonGroup();
        final Configuration config = Configuration.getInstance();
        for (int i = 0; i < colourSchemeNames.length; i++) {
            final int index = i;
            if (colourSchemes[index] == null) {
                // Skip deprecated schemes
                continue;
            }
            final JCheckBoxMenuItem schemeMenuItem = new JCheckBoxMenuItem(colourSchemeNames[index]);
            schemesButtonGroup.add(schemeMenuItem);
            if (config.getColourschemeIndex() == index) {
                schemeMenuItem.setSelected(true);
            }
            schemeMenuItem.addActionListener(e -> {
                selectedColourScheme = colourSchemes[index];
                view.setColourScheme(selectedColourScheme);
                config.setColourschemeIndex(index);
            });
            colourSchemeMenu.add(schemeMenuItem);
        }
        menu.add(colourSchemeMenu);

        menuItem = new JMenuItem(strings.getString("configure.view") + "...");
        menuItem.addActionListener(e -> {
            if (dimension == null) {
                DesktopUtils.beep();
                return;
            }
            ConfigureViewDialog dialog = new ConfigureViewDialog(App.this, dimension, view);
            dialog.setVisible(true);
            ACTION_GRID.setSelected(view.isPaintGrid());
            ACTION_CONTOURS.setSelected(view.isDrawContours());
            ACTION_OVERLAYS.setSelected(dimension.isOverlaysEnabled());
        });
        menuItem.setMnemonic('c');
        menuItem.setAccelerator(getKeyStroke(VK_V, PLATFORM_COMMAND_MASK));
        menu.add(menuItem);
        
        menu.addSeparator();

        JMenu workspaceLayoutMenu = new JMenu("Workspace layout");

        menuItem = new JMenuItem(ACTION_RESET_DOCKS);
        menuItem.setMnemonic('r');
        workspaceLayoutMenu.add(menuItem);

        menuItem = new JMenuItem(ACTION_RESET_ALL_DOCKS);
        menuItem.setMnemonic('a');
        workspaceLayoutMenu.add(menuItem);

        ACTION_LOAD_LAYOUT.setEnabled(config.getDefaultJideLayoutData() != null);
        menuItem = new JMenuItem(ACTION_LOAD_LAYOUT);
        menuItem.setMnemonic('l');
        workspaceLayoutMenu.add(menuItem);

        menuItem = new JMenuItem(ACTION_SAVE_LAYOUT);
        menuItem.setMnemonic('s');
        workspaceLayoutMenu.add(menuItem);
        menu.add(workspaceLayoutMenu);

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
                threeDeeFrame.setHiddenLayers(hiddenLayers);
                threeDeeFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                threeDeeFrame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        // TODO not sure how this can be null, but at least one error has been reported by a user where
                        //  it was
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
        menuItem.setAccelerator(getKeyStroke(VK_3, PLATFORM_COMMAND_MASK));
        menu.add(menuItem);

        menu.addSeparator();

        menuItem = new JMenuItem("View world history...");
        menuItem.addActionListener(e -> {
            if (world != null) {
                WorldHistoryDialog dialog = new WorldHistoryDialog(this, world);
                dialog.setVisible(true);
            }
        });
        menu.add(menuItem);

        menu.putClientProperty(KEY_HELP_KEY, "Menu/View");
        return menu;
    }

    private JMenu createToolsMenu() {
        JMenuItem menuItem = new JMenuItem(strings.getString("respawn.player") + "...");
        menuItem.addActionListener(e -> {
            RespawnPlayerDialog dialog = new RespawnPlayerDialog(App.this);
            dialog.setVisible(true);
        });
        menuItem.setMnemonic('r');
        JMenu menu = new JMenu(strings.getString("tools"));
        menu.setMnemonic('t');
        menu.add(menuItem);

        menuItem = new JMenuItem(strings.getString("open.custom.brushes.folder"));
        menuItem.addActionListener(e -> {
            File brushesDir = new File(Configuration.getConfigDir(), "brushes");
            if (! brushesDir.exists()) {
                if (! brushesDir.mkdirs()) {
                    DesktopUtils.beep();
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
                    DesktopUtils.beep();
                    return;
                }
            }
            DesktopUtils.open(pluginsDir);
        });
        menuItem.setMnemonic('p');
        menu.add(menuItem);

        menuItem = new JMenuItem("Open custom materials folder");
        menuItem.addActionListener(e -> {
            File customMaterialsDir = new File(Configuration.getConfigDir(), "materials");
            if (! customMaterialsDir.exists()) {
                if (! customMaterialsDir.mkdirs()) {
                    DesktopUtils.beep();
                    return;
                }
            }
            DesktopUtils.open(customMaterialsDir);
        });
        menuItem.setMnemonic('m');
        menu.add(menuItem);

        menuItem = new JMenuItem(strings.getString("biomes.viewer") + "...");
        menuItem.setEnabled(false);
        if (Configuration.getInstance().isSafeMode()) {
            menuItem.setToolTipText("Biomes viewer not available in safe mode");
        } else {
            menuItem.setToolTipText("Biomes subsystem initialising...");
            // Enable the menu item after the biome scheme manager has been initialised, if it turns out there are supported
            // biome algorithms, but without blocking the GUI
            final JMenuItem biomesViewerMenuItem = menuItem;
            new Thread("Biomes Viewer Menu Item Initialiser") {
                @Override
                public void run() {
                    if (BiomeSchemeManager.getAvailableBiomeAlgorithms().isEmpty()) {
                        logger.info("No supported Minecraft installation found; disabling biomes preview and Biomes Viewer");
                        doLaterOnEventThread(() -> biomesViewerMenuItem.setToolTipText("No supported Minecraft installation found"));
                    } else {
                        doLaterOnEventThread(() -> {
                            biomesViewerMenuItem.setToolTipText(null);
                            biomesViewerMenuItem.setEnabled(true);
                        });
                    }
                }
            }.start();
            menuItem.addActionListener(event -> {
                if (biomesViewerFrame != null) {
                    biomesViewerFrame.requestFocus();
                } else {
                    int preferredAlgorithm = -1;
                    if ((dimension != null) && (dimension.getAnchor().dim == DIM_NORMAL) && (dimension.getMaxHeight() == DEFAULT_MAX_HEIGHT_ANVIL)) {
                        if (dimension.getGenerator().getType() == LARGE_BIOMES) {
                            preferredAlgorithm = BIOME_ALGORITHM_1_7_LARGE;
                        } else {
                            preferredAlgorithm = BIOME_ALGORITHM_1_7_DEFAULT;
                        }
                    }
                    logger.info("Opening biomes viewer");
                    biomesViewerFrame = new BiomesViewerFrame(dimension.getMinecraftSeed(), world.getSpawnPoint(), preferredAlgorithm, selectedColourScheme, App.this);
                    biomesViewerFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                    biomesViewerFrame.addWindowListener(new WindowAdapter() {
                        @Override
                        public void windowClosing(WindowEvent e) {
                            // TODO not sure how this can be null, but at least one error has been reported by a user
                            //  where it was
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
        }
        menuItem.setMnemonic('b');
        menu.add(menuItem);

        menuItem = new JMenuItem("Run script...");
        menuItem.addActionListener(e -> {
            try {
                new ScriptRunner(this, world, dimension, undoManagers.values()).setVisible(true);
            } catch (UnsupportedClassVersionError | NoClassDefFoundError exc) {
                logger.error("Could not open ScriptRunner", exc);
                beepAndShowError(App.this, "JavaScript support requires Java 11 or later.\n" +
                        "Please install a newer version of Java and try again.\n" +
                        "See www.worldpainter.net for links.", "Newer Java Required");
            }
        });
        menuItem.setMnemonic('s');
        menu.add(menuItem);
        menu.putClientProperty(KEY_HELP_KEY, "Menu/Tools");
        return menu;
    }

    private JMenu createHelpMenu() {
        JMenuItem menuItem = new JMenuItem(ACTION_OPEN_DOCUMENTATION);
        menuItem.setMnemonic('d');
        JMenu menu = new JMenu(strings.getString("help"));
//        menu.setMnemonic('h');
        menu.add(menuItem);

//        menu.add(ACTION_SHOW_HELP_PICKER);

        menuItem = new JMenuItem("Frequently Asked Questions");
        menuItem.setMnemonic('f');
        menuItem.addActionListener(e -> {
            try {
                DesktopUtils.open(new URL("https://www.worldpainter.net/doc/faq"));
            } catch (MalformedURLException ex) {
                throw new RuntimeException(ex);
            }
        });
        menu.add(menuItem);

        menuItem = new JMenuItem("Troubleshooting");
        menuItem.setMnemonic('t');
        menuItem.addActionListener(e -> {
            try {
                DesktopUtils.open(new URL("https://www.worldpainter.net/trac/wiki/Troubleshooting"));
            } catch (MalformedURLException ex) {
                throw new RuntimeException(ex);
            }
        });
        menu.add(menuItem);

        menu.addSeparator();

        menuItem = new JMenuItem("Donate");
        menuItem.setMnemonic('d');
        menuItem.addActionListener(e -> {
            try {
                DesktopUtils.open(new URL("https://www.worldpainter.net/donate/paypal"));
            } catch (MalformedURLException ex) {
                throw new RuntimeException(ex);
            }
        });
        menu.add(menuItem);

        menuItem = new JMenuItem("Merch store");
        menuItem.setMnemonic('m');
        menuItem.addActionListener(e -> {
            try {
                DesktopUtils.open(new URL("https://www.worldpainter.store/"));
            } catch (MalformedURLException ex) {
                throw new RuntimeException(ex);
            }
        });
        menu.add(menuItem);

        if (! hideAbout) {
            menuItem = new JMenuItem(strings.getString("about"));
            menuItem.setMnemonic('a');
            menuItem.addActionListener(e -> {
                AboutDialog dialog = new AboutDialog(App.this, world, view, currentUndoManager);
                dialog.setVisible(true);
            });
            menu.add(menuItem);
        }
        menu.putClientProperty(KEY_HELP_KEY, "Menu/Help");
        return menu;
    }

    private void addCeiling() {
        final Anchor currentAnchor = dimension.getAnchor();
        if (currentAnchor.invert) {
            throw new IllegalStateException("Current dimension is already a ceiling");
        } else if (currentAnchor.id != 0) {
            throw new UnsupportedOperationException("Layers other than 0 not yet supported");
        }
        final Anchor newAnchor = new Anchor(currentAnchor.dim, currentAnchor.role, true, currentAnchor.id);
        if (world.isDimensionPresent(newAnchor)) {
            throw new IllegalStateException("Ceiling dimension already exists");
        }
        final NewWorldDialog dialog = new NewWorldDialog(this, selectedColourScheme, world.getName(), dimension.getSeed() + newAnchor.hashCode(), world.getPlatform(), newAnchor, dimension.getMinHeight(), dimension.getMaxHeight(), dimension, dimension.getTileCoords());
        dialog.setVisible(true);
        if (! dialog.isCancelled()) {
            if (! dialog.checkMemoryRequirements(this)) {
                return;
            }
            Dimension ceiling = ProgressDialog.executeTask(this, new ProgressTask<Dimension>() {
                @Override
                public String getName() {
                    return "Creating ceiling dimension";
                }

                @Override
                public Dimension execute(ProgressReceiver progressReceiver) throws OperationCancelled {
                    return dialog.getSelectedDimension(world, progressReceiver);
                }
            });
            if (ceiling == null) {
                // Cancelled by user
                return;
            }
            world.addDimension(ceiling);
            setDimension(ceiling);
        }
    }

    private void removeCeiling() {
        final Anchor currentAnchor = dimension.getAnchor();
        if (currentAnchor.id != 0) {
            throw new UnsupportedOperationException("Layers other than 0 not yet supported");
        }
        final Anchor ceilingAnchor = new Anchor(currentAnchor.dim, currentAnchor.role, true, currentAnchor.id);
        if (! world.isDimensionPresent(ceilingAnchor)) {
            throw new IllegalStateException("There is no ceiling dimension");
        }
        final Dimension ceiling = world.getDimension(ceilingAnchor);
        if (showConfirmDialog(this, "Are you sure you want to completely remove the " + ceiling.getName() + " dimension?\nThis action cannot be undone!", "Confirm " + ceiling.getName() + " Deletion", YES_NO_OPTION) == YES_OPTION) {
            world.removeDimension(ceilingAnchor);
            if ((dimension != null) && (dimension.getAnchor().equals(ceilingAnchor))) {
                viewDimension(new Anchor(ceilingAnchor.dim, ceilingAnchor.role, false, ceilingAnchor.id));
            } else {
                configureForPlatform();
                if (dimension.getAnchor().dim == ceilingAnchor.dim) {
                    view.refreshTiles();
                }
            }
            showInfo(this, "The " + ceiling.getName() + " was successfully deleted", "Success");
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
            toolBar.add(ACTION_SCALE_WORLD);
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
        button = new JToggleButton(ACTION_OVERLAYS);
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
//        toolBar.add(Box.createHorizontalGlue());
//        toolBar.add(ACTION_SHOW_HELP_PICKER);
        return toolBar;
    }
    
    private void addStatisticsTo(MenuElement menuElement, @NonNls final String key, final EventLogger eventLogger) {
        if ((menuElement instanceof JMenuItem) && (! (menuElement instanceof JMenu))) {
            JMenuItem menuItem = (JMenuItem) menuElement;
            if (((! (menuItem.getAction() instanceof BetterAction)) || (! ((BetterAction) menuItem.getAction()).isLogEvent()))
                    && (! menuItem.getText().equals("Existing Minecraft map..."))
                    && (! menuItem.getText().equals("Merge World..."))) {
                if (key.startsWith("menu.File.RecentlyusedWorlds.")) {
                    menuItem.addActionListener(e -> eventLogger.logEvent(new EventVO("menu.File.RecentlyusedWorlds").addTimestamp()));
                } else {
                    menuItem.addActionListener(e -> eventLogger.logEvent(new EventVO(key).addTimestamp()));
                }
            }
        }
        for (MenuElement subElement: menuElement.getSubElements()) {
            if (subElement instanceof JPopupMenu) {
                addStatisticsTo(subElement, key, eventLogger);
            } else if (subElement instanceof JMenuItem) {
                addStatisticsTo(subElement, key + "." + ((JMenuItem) subElement).getText().replaceAll("[ \\t\\n\\x0B\\f\\r.]", ""), eventLogger);
            }
        }
    }
    
    private void addNether() {
        final Dimension surface = world.getDimension(NORMAL_DETAIL);
        final NewWorldDialog dialog = new NewWorldDialog(this, selectedColourScheme, world.getName(), surface.getSeed() + 1, world.getPlatform(), NETHER_DETAIL, Math.max(world.getMinHeight(), 0), Math.min(world.getMaxHeight(), DEFAULT_MAX_HEIGHT_NETHER), surface);
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
                    return dialog.getSelectedDimension(world, progressReceiver);
                }
            });
            if (nether == null) {
                // Cancelled by user
                return;
            }
            world.addDimension(nether);
            setDimension(nether);
            DimensionPropertiesDialog propertiesDialog = new DimensionPropertiesDialog(this, nether, selectedColourScheme, customBiomeManager);
            propertiesDialog.setVisible(true);
        }
    }

    private void removeNether() {
        if (showConfirmDialog(this, "Are you sure you want to completely remove the Nether dimension?\nThis action cannot be undone!", "Confirm Nether Deletion", YES_NO_OPTION) == YES_OPTION) {
            world.removeDimension(NETHER_DETAIL);
            if (world.isDimensionPresent(NETHER_DETAIL_CEILING)) {
                world.removeDimension(NETHER_DETAIL_CEILING);
            }
            if ((dimension != null) && (dimension.getAnchor().dim == DIM_NETHER)) {
                viewDimension(NORMAL_DETAIL);
            } else {
                configureForPlatform();
            }
            showInfo(this, "The Nether dimension was successfully deleted", "Success");
        }
    }
    
    private void addEnd() {
        final Dimension surface = world.getDimension(NORMAL_DETAIL);
        final NewWorldDialog dialog = new NewWorldDialog(this, selectedColourScheme, world.getName(), surface.getSeed() + 2, world.getPlatform(), END_DETAIL, Math.max(world.getMinHeight(), 0), Math.min(world.getMaxHeight(), DEFAULT_MAX_HEIGHT_END), surface);
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
                    return dialog.getSelectedDimension(world, progressReceiver);
                }
            });
            if (end == null) {
                // Cancelled by user
                return;
            }
            world.addDimension(end);
            setDimension(end);
        }
    }

    private void removeEnd() {
        if (showConfirmDialog(this, "Are you sure you want to completely remove the End dimension?\nThis action cannot be undone!", "Confirm End Deletion", YES_NO_OPTION) == YES_OPTION) {
            world.removeDimension(END_DETAIL);
            if (world.isDimensionPresent(END_DETAIL_CEILING)) {
                world.removeDimension(END_DETAIL_CEILING);
            }
            if ((dimension != null) && (dimension.getAnchor().dim == DIM_END)) {
                viewDimension(NORMAL_DETAIL);
            } else {
                configureForPlatform();
            }
            showInfo(this, "The End dimension was successfully deleted", "Success");
        }
    }

    private void addMaster() {
        final Anchor currentAnchor = dimension.getAnchor();
        if (currentAnchor.role == MASTER) {
            throw new IllegalStateException("Current dimension is already a master");
        } else if (currentAnchor.id != 0) {
            throw new UnsupportedOperationException("Layers other than 0 not yet supported");
        }
        final Anchor newAnchor = new Anchor(currentAnchor.dim, MASTER, false, currentAnchor.id);
        if (world.isDimensionPresent(newAnchor)) {
            throw new IllegalStateException("Master dimension already exists");
        }
        final NewWorldDialog dialog = new NewWorldDialog(this, selectedColourScheme, world.getName(), dimension.getSeed(), world.getPlatform(), newAnchor, dimension.getMinHeight(), dimension.getMaxHeight(), dimension);
        dialog.setVisible(true);
        if (! dialog.isCancelled()) {
            if (! dialog.checkMemoryRequirements(this)) {
                return;
            }
            Dimension master = ProgressDialog.executeTask(this, new ProgressTask<Dimension>() {
                @Override
                public String getName() {
                    return "Creating master dimension";
                }

                @Override
                public Dimension execute(ProgressReceiver progressReceiver) throws OperationCancelled {
                    return dialog.getSelectedDimension(world, progressReceiver);
                }
            });
            if (master == null) {
                // Cancelled by user
                return;
            }
            world.addDimension(master);
            setDimension(master);
        }
    }

    private void removeMaster() {
        final Anchor currentAnchor = dimension.getAnchor();
        if (currentAnchor.id != 0) {
            throw new UnsupportedOperationException("Layers other than 0 not yet supported");
        }
        final Anchor masterAnchor = new Anchor(currentAnchor.dim, MASTER, false, currentAnchor.id);
        if (! world.isDimensionPresent(masterAnchor)) {
            throw new IllegalStateException("There is no master dimension");
        }
        final Dimension master = world.getDimension(masterAnchor);
        if (showConfirmDialog(this, "Are you sure you want to completely remove the " + master.getName() + " dimension?\nThis action cannot be undone!", "Confirm " + master.getName() + " Deletion", YES_NO_OPTION) == YES_OPTION) {
            world.removeDimension(masterAnchor);
            if ((dimension != null) && (dimension.getAnchor().equals(masterAnchor))) {
                viewDimension(new Anchor(masterAnchor.dim, DETAIL, false, masterAnchor.id));
            } else {
                if (backgroundDimension == master) {
                    showBackgroundStatus = false;
                    view.setBackgroundDimension(null, 0, null);
                    backgroundDimension = null;
                }
                configureForPlatform();
            }
            showInfo(this, "The " + master.getName() + " was successfully deleted", "Success");
        }
    }

    private void viewDimension(Anchor anchor) {
        if (! anchor.equals(dimension.getAnchor())) {
            setDimension(world.getDimension(anchor));
        }
    }
    
    private void fixLabelSizes() {
        locationLabel.setMinimumSize(locationLabel.getSize());
        locationLabel.setPreferredSize(locationLabel.getSize());
        locationLabel.setMaximumSize(locationLabel.getSize());
        heightLabel.setMinimumSize(heightLabel.getSize());
        heightLabel.setPreferredSize(heightLabel.getSize());
        heightLabel.setMaximumSize(heightLabel.getSize());
        slopeLabel.setMinimumSize(slopeLabel.getSize());
        slopeLabel.setPreferredSize(slopeLabel.getSize());
        slopeLabel.setMaximumSize(slopeLabel.getSize());
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
        slopeLabel.setText(" ");
        waterLabel.setText(" ");
        biomeLabel.setText(" ");
        materialLabel.setText(" ");
        radiusLabel.setText(MessageFormat.format(strings.getString("radius.0"), radius));
        updateZoomLabel();
    }

    private AbstractButton createButtonForOperation(final Operation operation) {
        return createButtonForOperation(operation, (char) 0);
    }

    private AbstractButton createButtonForOperation(final Operation operation, char mnemonic) {
        BufferedImage icon = operation.getIcon();
        AbstractButton button = (operation instanceof GlobalOperation) ? new JButton() : new JToggleButton();
        if (operation instanceof SetSpawnPoint) {
            setSpawnPointToggleButton = (JToggleButton) button;
        }
        button.setMargin(App.BUTTON_INSETS);
        if (icon != null) {
            button.setIcon(new ImageIcon(icon));
        }
        final StringBuilder tooltip = new StringBuilder();
        tooltip.append(operation.getName());
        final String description = operation.getDescription();
        if ((description != null) && (! operation.getName().equalsIgnoreCase(description))) {
            tooltip.append(": ").append(description);
        }
        if (mnemonic != 0) {
            button.setMnemonic(mnemonic);
            tooltip.append(" (Alt+").append(mnemonic).append(')');
        }
        button.setToolTipText(tooltip.toString());
        if (operation instanceof GlobalOperation) {
            button.addActionListener(event ->  {
                cancelPaintSelection();
                if (operation instanceof BrushOperation) {
                    ((BrushOperation) operation).setBrush(brushRotation == 0 ? brush : RotatedBrush.rotate(brush, brushRotation));
                }
                if (operation instanceof PaintOperation) {
                    ((PaintOperation) operation).setPaint(paint);
                }
                if (closeCallout("callout_1")) {
                    // If the user picked an operation which doesn't need a brush, close the "select brush" callout too
                    if (! (operation instanceof BrushOperation)) {
                        closeCallout("callout_2");
                    }
                    // If the user picked an operation which doesn't use paint, close the "select paint" callout too
                    if (! (operation instanceof PaintOperation)) {
                        closeCallout("callout_3");
                    }
                }
                ((GlobalOperation) operation).invoke();
            });
        } else {
            button.addItemListener(event -> {
                boolean refreshOptionsPanel = false;
                if (event.getStateChange() == ItemEvent.DESELECTED) {
                    if (operation instanceof BrushOperation) {
                        view.setDrawBrush(false);
                    }
                    try {
                        operation.setActive(false);
                    } catch (PropertyVetoException e) {
                        logger.error("Property veto exception while deactivating operation " + operation, e);
                    }
                    activeOperation = null;
                    if (toolSettingsPanel.getComponentCount() > 0) {
                        toolSettingsPanel.removeAll();
                        refreshOptionsPanel = true;
                    }
                } else {
                    mapSelectionController.cancelPaintSelection(true, false);
                    if (operation instanceof PaintOperation) {
                        programmaticChange = true;
                        try {
                            if (operation instanceof MouseOrTabletOperation) {
                                ((MouseOrTabletOperation) operation).setLevel(level);
                                if (operation instanceof FilteredOperation) {
                                    ((FilteredOperation) operation).setFilter(filter);
                                }
                                if (operation instanceof BrushOperation) {
                                    ((BrushOperation) operation).setBrush(brushRotation == 0 ? brush : RotatedBrush.rotate(brush, brushRotation));
                                    selectBrushButton(brush);
                                    view.setBrushShape(brush.getBrushShape());
                                    view.setBrushRotation(brushRotation);
                                }
                            }
                            levelSlider.setValue((int) (level * 100));
                            brushRotationSlider.setValue(brushRotation);
                            if (filter instanceof DefaultFilter) {
                                brushOptions.setFilter((DefaultFilter) filter);
                            } else {
                                brushOptions.setFilter(null);
                            }
                        } finally {
                            programmaticChange = false;
                        }
                        ((PaintOperation) operation).setPaint(paint);
                    } else {
                        programmaticChange = true;
                        try {
                            if (operation instanceof MouseOrTabletOperation) {
                                ((MouseOrTabletOperation) operation).setLevel(toolLevel);
                                if (operation instanceof FilteredOperation) {
                                    ((FilteredOperation) operation).setFilter(toolFilter);
                                }
                                if (operation instanceof BrushOperation) {
                                    ((BrushOperation) operation).setBrush(toolBrushRotation == 0 ? toolBrush : RotatedBrush.rotate(toolBrush, toolBrushRotation));
                                    selectBrushButton(toolBrush);
                                    view.setBrushShape(toolBrush.getBrushShape());
                                    view.setBrushRotation(toolBrushRotation);
                                }
                            }
                            levelSlider.setValue((int) (toolLevel * 100));
                            brushRotationSlider.setValue(toolBrushRotation);
                            if (toolFilter instanceof DefaultFilter) {
                                brushOptions.setFilter((DefaultFilter) toolFilter);
                            } else {
                                brushOptions.setFilter(null);
                            }
                        } finally {
                            programmaticChange = false;
                        }
                    }
                    if (operation instanceof BrushOperation) {
                        view.setDrawBrush(true);
                        view.setRadius(radius);
                        ((BrushOperation) operation).setRadius(radius);
                    }
                    activeOperation = operation;
                    updateLayerVisibility();
                    updateBrushRotation();
                    try {
                        operation.setActive(true);
                    } catch (PropertyVetoException e) {
                        deselectTool();
                        DesktopUtils.beep();
                        return;
                    }
                    if (closeCallout("callout_1")) {
                        // If the user picked an operation which doesn't need a
                        // brush, close the "select brush" callout too
                        if (! (operation instanceof BrushOperation)) {
                            closeCallout("callout_2");
                        }
                        // If the user picked an operation which doesn't use paint,
                        // close the "select paint" callout too
                        if (! (operation instanceof PaintOperation)) {
                            closeCallout("callout_3");
                        }
                    }
                    JPanel optionsPanel = operation.getOptionsPanel();
                    if (optionsPanel != null) {
                        toolSettingsPanel.add(optionsPanel, BorderLayout.CENTER);
                        refreshOptionsPanel = true;
                    }
                }
                if (refreshOptionsPanel) {
                    toolSettingsPanel.revalidate();
                    toolSettingsPanel.repaint();
                }
            });
            toolButtonGroup.add(button);
        }
        button.putClientProperty(KEY_HELP_KEY, "Operation/" + operation.getClass().getSimpleName());
        return button;
    }

    List<Component> createLayerButton(final Layer layer, final char mnemonic) {
        return createLayerButton(layer, mnemonic, true, true);
    }

    /**
     * Notifies the App that a layer has been removed. The caller is responsible for actually removing the button from
     * the UI.
     */
    void layerRemoved(final Layer layer) {
        final String paintId = createLayerPaintId(layer);
        for (Enumeration<AbstractButton> e = paintButtonGroup.getElements(); e.hasMoreElements(); ) {
            final AbstractButton button = e.nextElement();
            if (paintId.equals(button.getClientProperty(KEY_PAINT_ID))) {
                if (button.isSelected()) {
                    deselectPaint();
                }
                paintButtonGroup.remove(button);
                return;
            }
        }
    }

    private List<Component> createLayerButton(final Layer layer, final char mnemonic, final boolean createSoloCheckbox, final boolean createButton) {
        if (createButton) {
            final JToggleButton button = new JToggleButton();
            button.setMargin(new Insets(2, 2, 2, 2));
            if (layer.getIcon() != null) {
                button.setIcon(new ImageIcon(layer.getIcon()));
            }
            button.setToolTipText(layer.getName() + ": " + layer.getDescription());
            // TODO: make this work again, but with Ctrl + Alt or something
//            if (mnemonic != 0) {
//                button.setMnemonic(mnemonic);
//            }
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
            button.setText(layer.getName());
            button.putClientProperty(KEY_HELP_KEY, "Layer/" + layer.getId());
            button.putClientProperty(KEY_PAINT_ID, createLayerPaintId(layer));
            return createLayerRow(layer, true, createSoloCheckbox, button);
        } else {
            JLabel label = new JLabel(layer.getName(), new ImageIcon(layer.getIcon()), JLabel.LEADING);
            label.putClientProperty(KEY_HELP_KEY, "Layer/" + layer.getId());
            return createLayerRow(layer, true, createSoloCheckbox, label);
        }
    }

    private List<Component> createLayerRow(final Layer layer, final boolean createShowCheckbox, final boolean createSoloCheckbox, final JComponent layerComponent) {
        List<Component> components = new ArrayList<>(3);

        final JCheckBox checkBox;
        if (createShowCheckbox) {
            checkBox = new JCheckBox();
            checkBox.setToolTipText(strings.getString("whether.or.not.to.display.this.layer"));
            checkBox.setSelected(true);
            checkBox.addChangeListener(e -> {
                if (checkBox.isSelected()) {
                    hiddenLayers.remove(layer);
                } else {
                    hiddenLayers.add(layer);
                }
                updateLayerVisibility();
            });
            components.add(checkBox);
        } else {
            checkBox = null;
            components.add(Box.createGlue());
        }

        final JCheckBox soloCheckBox;
        if (createSoloCheckbox) {
            soloCheckBox = new JCheckBox();
            layerSoloCheckBoxes.put(layer, soloCheckBox);
            soloCheckBox.setToolTipText("<html>Check to show <em>only</em> this layer (the other layers are still exported)</html>");
            soloCheckBox.addActionListener(new SoloCheckboxHandler(soloCheckBox, layer));
            components.add(soloCheckBox);
        } else {
            soloCheckBox = null;
            components.add(Box.createGlue());
        }

        components.add(layerComponent);
        layerControls.put(layer, new LayerControls(layer, checkBox, soloCheckBox, layerComponent));

        return components;
    }

    private JToggleButton createTerrainButton(final Terrain terrain) {
        final JToggleButton button = new JToggleButton();
        button.putClientProperty(KEY_PAINT_ID, createTerrainPaintId(terrain));
        button.setMargin(App.SMALLER_BUTTON_INSETS);
        button.setIcon(new ImageIcon(terrain.getScaledIcon(18, selectedColourScheme)));
        button.setToolTipText(terrain.getName() + ": " + terrain.getDescription());
        button.addItemListener(event -> {
            if (event.getStateChange() == ItemEvent.SELECTED) {
                if (terrain.isConfigured()) {
                    paintUpdater = () -> {
                        paint = PaintFactory.createTerrainPaint(terrain);
                        paintChanged();
                    };
                    paintUpdater.updatePaint();
                }
            }
        });
        if (terrain.isCustom()) {
            button.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (!terrain.isConfigured()) {
                        showCustomTerrainButtonPopup(e, terrain.getCustomTerrainIndex());
                    }
                }
            });
        }
        paintButtonGroup.add(button);
        return button;
    }

    private void paintChanged() {
        if (activeOperation instanceof PaintOperation) {
            ((PaintOperation) activeOperation).setPaint(paint);
        }
        updateLayerVisibility();
        closeCallout("callout_3");
        cancelPaintSelection();
    }

    /**
     * Configure the view to show the correct layers
     */
    void updateLayerVisibility() {
        // Determine which layers should be hidden
        Set<Layer> targetHiddenLayers = new HashSet<>();
        // The FloodWithLava layer should *always* be hidden
        targetHiddenLayers.add(FloodWithLava.INSTANCE);
        if (soloLayer != null) {
            // Only the solo layer and the active layer (if there is one and it is different than the solo layer) should
            // be visible
            targetHiddenLayers.addAll((dimension != null) ? dimension.getAllLayers(true) : new HashSet<>(layers));
            // Put in the currently hidden layers as well, as some of them might be synthetic and not returned by the
            // dimension
            targetHiddenLayers.addAll(hiddenLayers);
            // Don't hide the solo layer, except when that is the terrain. "Soloing" the terrain has the slightly
            // different meaning of hiding all the layers
            if (soloLayer != TERRAIN_AS_LAYER) {
                targetHiddenLayers.remove(soloLayer);
            }
        } else {
            Palette soloPalette = null;
            for (Palette palette: customLayerController.paletteManager.getPalettes()) {
                if (palette.isSolo()) {
                    soloPalette = palette;
                    break;
                }
            }
            if (soloPalette != null) {
                // Only the layers on the solo palette and the active layer (if there is one and it is different than
                // the solo layer) should be visible
                targetHiddenLayers.addAll((dimension != null) ? dimension.getAllLayers(true) : new HashSet<>(layers));
                soloPalette.getLayers().forEach(targetHiddenLayers::remove);
                // Put in the currently hidden layers as well, as some of them might be on the solo palette and we still
                // want to hide those
                targetHiddenLayers.addAll(hiddenLayers);
            } else {
                // The layers marked as hidden should be invisible, except the currently active one, if any
                targetHiddenLayers.addAll(hiddenLayers);
                for (Palette palette: customLayerController.paletteManager.getPalettes()) {
                    if (! palette.isShow()) {
                        targetHiddenLayers.addAll(palette.getLayers());
                    }
                }
            }
        }
        // The currently active layer, if any, should always be visible
        if (activeOperation instanceof PaintOperation) {
            if (paint instanceof LayerPaint) {
                targetHiddenLayers.remove(((LayerPaint) paint).getLayer());
            } else if (paint instanceof TerrainPaint) {
                targetHiddenLayers.remove(TERRAIN_AS_LAYER);
            }
        } else if ((activeOperation instanceof Flood) || (activeOperation instanceof FloodWithLava)) {
            targetHiddenLayers.remove(FLUIDS_AS_LAYER);
        }
        // The Selection layers should *never* be hidden
        targetHiddenLayers.remove(SelectionBlock.INSTANCE);
        targetHiddenLayers.remove(SelectionChunk.INSTANCE);

        // Hide the selected layers
        view.setHiddenLayers(targetHiddenLayers);
        if (threeDeeFrame != null) {
            threeDeeFrame.setHiddenLayers(targetHiddenLayers);
        }

        // Configure the glass pane to show the right icons
        glassPane.setHiddenLayers(hiddenLayers);
        glassPane.setSoloLayer(soloLayer);
    }
    
    private void selectBrushButton(Brush selectedBrush) {
        if (! brushButtons.get(selectedBrush).isSelected()) {
            for (Map.Entry<Brush, JToggleButton> entry: brushButtons.entrySet()) {
                JToggleButton button = entry.getValue();
                if (button.isSelected()) {
                    button.setSelected(false);
                    updateBrushRotation(entry.getKey(), button);
                    break;
                }
            }
            brushButtons.get(selectedBrush).setSelected(true);
        }
    }

    private JComponent createBrushButton(final Brush brush) {
        final JToggleButton button = new LazyLoadingIconToggleButton((int) (32 * getUIScale()), () -> setBrushThumbnail(brush));
        button.setMargin(new Insets(2, 2, 2, 2));
        button.setToolTipText(brush.getName());
        button.addItemListener(e -> {
            if (! programmaticChange) {
                updateBrushRotation(brush, button);
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    int effectiveRotation;
                    if (activeOperation instanceof PaintOperation) {
                        App.this.brush = brush;
                        effectiveRotation = brushRotation;
                    } else {
                        toolBrush = brush;
                        effectiveRotation = toolBrushRotation;
                    }
                    if (activeOperation instanceof BrushOperation) {
                        ((BrushOperation) activeOperation).setBrush((effectiveRotation == 0) ? brush : RotatedBrush.rotate(brush, effectiveRotation));
                    }
                    view.setBrushShape(brush.getBrushShape());
                }
            }
        });
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                closeCallout("callout_2");
            }
        });
        brushButtonGroup.add(button);
        brushButtons.put(brush, button);
        return button;
    }

    private Icon setBrushThumbnail(Brush brush) {
        final Icon thumbnail = createBrushThumbnail(brush.clone(), round(32 * getUIScale()));
        final JToggleButton button = brushButtons.get(brush);
        button.putClientProperty(KEY_THUMBNAIL, thumbnail);
        updateBrushRotation(brush, button);
        return thumbnail;
    }
    
    private Icon createBrushThumbnail(Brush brush, int size) {
        final int radius = size / 2;
        brush.setRadius(radius - 1);
        final BufferedImage image = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().createCompatibleImage(size, size, Transparency.TRANSLUCENT);
        for (int dx = -radius + 1; dx < radius; dx++) {
            for (int dy = -radius + 1; dy < radius; dy++) {
                final float strength = brush.getFullStrength(dx, dy);
                final int alpha = round(strength * 255f);
                image.setRGB(dx + radius - 1, dy + radius - 1, (alpha << 24) | (darkMode ? 0xd0d0d0 : 0x000000));
            }
        }
        return new ImageIcon(image);
    }

    private static Icon loadScaledIcon(@NonNls String name) {
        return IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/" + name + ".png");
    }

    private void configureForPlatform() {
        final Platform platform = world.getPlatform();
        final boolean nether = world.isDimensionPresent(NETHER_DETAIL);
        final boolean end = world.isDimensionPresent(END_DETAIL);
        final Anchor anchor = dimension.getAnchor();
        biomeHelper = new BiomeHelper(selectedColourScheme, customBiomeManager, platform);
        setEnabled(addNetherMenuItem, platform.supportedDimensions.contains(DIM_NETHER) && (! nether));
        setEnabled(removeNetherMenuItem, nether);
        setEnabled(addEndMenuItem, platform.supportedDimensions.contains(DIM_END) && (! end));
        setEnabled(removeEndMenuItem, end);
        setEnabled(viewNetherMenuItem, nether);
        setEnabled(viewEndMenuItem, end);
        setEnabled(ACTION_SWITCH_TO_FROM_MASTER, world.isDimensionPresent(new Anchor(anchor.dim, (anchor.role == MASTER) ? DETAIL : MASTER, false, 0)));
        setEnabled(ACTION_SWITCH_TO_FROM_CEILING, world.isDimensionPresent(new Anchor(anchor.dim, DETAIL, true, 0)));
        setEnabled(addMasterMenuItem, (anchor.role == DETAIL) && (anchor.id == 0) && (! world.isDimensionPresent(new Anchor(anchor.dim, MASTER, false, 0))));
        setEnabled(removeMasterMenuItem, (anchor.role == MASTER) || (world.isDimensionPresent(new Anchor(anchor.dim, MASTER, false, 0))));
        setEnabled(addCeilingMenuItem, (anchor.role == DETAIL) && (anchor.id == 0) && (! anchor.invert) && (! world.isDimensionPresent(new Anchor(anchor.dim, anchor.role, true, 0))));
        setEnabled(removeCeilingMenuItem, anchor.invert || (world.isDimensionPresent(new Anchor(anchor.dim, anchor.role, true, 0))));
        final boolean biomesSupported = (! anchor.invert) && platform.capabilities.contains(BIOMES) || platform.capabilities.contains(BIOMES_3D) || platform.capabilities.contains(NAMED_BIOMES);
        setEnabled(Biome.INSTANCE, biomesSupported, "Biomes not supported by format " + platform);
        setEnabled(biomesPanelFrame, biomesSupported);
        // TODO deselect biomes panel if it was selected
        if ((anchor.dim == DIM_NORMAL) && (anchor.role != MASTER)) {
            setEnabled(setSpawnPointToggleButton, platform.capabilities.contains(SET_SPAWN_POINT));
            setEnabled(ACTION_MOVE_TO_SPAWN, platform.capabilities.contains(SET_SPAWN_POINT));
        } else {
            if (activeOperation instanceof SetSpawnPoint) {
                deselectTool();
            }
            setEnabled(setSpawnPointToggleButton, false);
            setEnabled(ACTION_MOVE_TO_SPAWN, false);
        }
        final boolean caveFloor = anchor.role == CAVE_FLOOR, floatingFloor = anchor.role == FLOATING_FLOOR;
        setEnabled(Populate.INSTANCE, (! caveFloor) && (! floatingFloor) && (! anchor.invert) && platform.capabilities.contains(POPULATE), "Automatic population not supported or not applicable");
        biomesPanel.loadBiomes(platform, selectedColourScheme);
        setEnabled(extendedBlockIdsMenuItem, (! platform.capabilities.contains(NAME_BASED)) && (platform != JAVA_MCREGION));
        brushOptions.setPlatform(platform);
        infoPanel.setPlatform(platform);
        // TODO actually why not support these:
        setEnabled(Caves.INSTANCE, (! caveFloor) && (! floatingFloor), "Caves not supported in Custom Cave/Tunnel floor dimensions");
        setEnabled(Caverns.INSTANCE, (! caveFloor) && (! floatingFloor), "Caverns not supported in Custom Cave/Tunnel floor dimensions");
        setEnabled(Chasms.INSTANCE, (! caveFloor) && (! floatingFloor), "Chasms not supported in Custom Cave/Tunnel floor dimensions");
        setEnabled(ReadOnly.INSTANCE, anchor.equals(NORMAL_DETAIL), "Read Only layer not applicable");
    }

    private void setEnabled(Layer layer, boolean enabled, String toolTipText) {
        final LayerControls layerControls = this.layerControls.get(layer);
        if (enabled && (! layerControls.isEnabled())) {
            layerControls.setEnabled(true);
        } else if ((! enabled) && layerControls.isEnabled()) {
            if ((paint instanceof LayerPaint) && (((LayerPaint) paint).getLayer().equals(layer))) {
                deselectPaint();
            }
            layerControls.disable(toolTipText);
        }
    }

    private void setEnabled(Component component, boolean enabled) {
        if (enabled != component.isEnabled()) {
            component.setEnabled(enabled);
        }
    }

    private void setEnabled(Action action, boolean enabled) {
        if (enabled != action.isEnabled()) {
            action.setEnabled(enabled);
        }
    }

    private void addMaterialSelectionTo(final JToggleButton button, final int customMaterialIndex) {
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
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

            private void showPopup(AWTEvent event) {
                showCustomTerrainButtonPopup(event, customMaterialIndex);
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
            return true;
        } else {
            return false;
        }
    }

    private boolean importCustomMaterials() {
        if (dimension == null) {
            DesktopUtils.beep();
            return false;
        } else if (getConfiguredCustomMaterialCount() == CUSTOM_TERRAIN_COUNT) {
            showMessageDialog(this, "All Custom Terrain slots are already in use.", "No Free Slots", ERROR_MESSAGE);
            return false;
        }
        MixedMaterial[] customMaterials = MixedMaterialHelper.loadMultiple(this);
        if (customMaterials != null) {
            if (getConfiguredCustomMaterialCount() + customMaterials.length > CUSTOM_TERRAIN_COUNT) {
                showWarning(this, "Not enough unused Custom Terrain slots available;\nselect " + (CUSTOM_TERRAIN_COUNT - getConfiguredCustomMaterialCount()) + " Custom Terrains or fewer.", "Too Many Custom Terrains");
                return false;
            }
            int nextIndex = 0;
            for (MixedMaterial customMaterial: customMaterials) {
                while (Terrain.getCustomMaterial(nextIndex) != null) {
                    nextIndex++;
                }
                customMaterial = MixedMaterialManager.getInstance().register(customMaterial);
                addButtonForNewCustomTerrain(nextIndex, customMaterial, false);
            }
            view.refreshTiles();
            return true;
        } else {
            return false;
        }
    }

    private void exportCustomMaterial(int customMaterialIndex) {
        MixedMaterialHelper.save(this, Terrain.getCustomMaterial(customMaterialIndex));
    }

    private void removeCustomMaterial(int index) {
        Terrain customTerrain = Terrain.getCustomTerrain(index);
        MixedMaterial mixedMaterial = Terrain.getCustomMaterial(index);
        String name = mixedMaterial.getName();

        // Check whether the terrain is present on the map
        Set<Terrain> allTerrains = ProgressDialog.executeTask(this, "Checking whether terrain is in use", () -> world.getDimensions().stream()
                .parallel()
                .flatMap(dim -> dim.getAllTerrains().parallelStream())
                .collect(toSet()), NOT_CANCELABLE);
        if (allTerrains.contains(customTerrain)) {
            Toolkit.getDefaultToolkit().beep();
            JOptionPane.showMessageDialog(this, "Custom terrain \"" + name + "\" is still in use on the world.\nUse the Global Operations tool to replace it.", "Terrain In Use", ERROR_MESSAGE);
            return;
        }

        // Check whether the terrain is used in a layer
        final Set<CustomLayer> allLayers = new HashSet<>(customLayerController.getCustomLayers());
        for (Dimension dimension: world.getDimensions()) {
            if (dimension != this.dimension) {
                allLayers.addAll(dimension.getCustomLayers());
            }
        }
        for (CustomLayer layer: allLayers) {
            if ((layer instanceof CombinedLayer) && (((CombinedLayer) layer).getTerrain() == customTerrain)) {
                Toolkit.getDefaultToolkit().beep();
                JOptionPane.showMessageDialog(this, "Custom terrain \"" + name + "\" is still in use in Combined Layer \"" + layer.getName() + "\".\nRemove it from that layer, or delete the layer.", "Terrain In Use", ERROR_MESSAGE);
                return;
            }
        }

        if (JOptionPane.showConfirmDialog(this, "Are you sure you want to delete custom terrain \"" + name + "\"?\nThis operation cannot be undone.", "Confirm Deletion", YES_NO_OPTION) == YES_OPTION) {
            // Clear all the undo managers, because we don't know which of them may have a version in their history
            // which is still using the terrain
            undoManagers.values().forEach(UndoManager::clear);
            MixedMaterialManager.getInstance().clear(mixedMaterial);
            Terrain.setCustomMaterial(index, null);
            if (customMaterialButtons[index].isSelected()) {
                deselectPaint();
            }
            customTerrainPanel.remove(customMaterialButtons[index]);
            customMaterialButtons[index] = null;
            if (Terrain.getConfiguredCustomMaterialCount() == 0) {
                dockingManager.removeFrame("customTerrain");
                customTerrainPanel = null;
            } else {
                customTerrainPanel.validate();
            }
            showInfo(this, "Custom terrain \"" + name + "\" was successfully deleted.", "Custom Terrain Deleted");
        }
    }

    private void clearCustomTerrains() {
        for (int index = 0; index < CUSTOM_TERRAIN_COUNT; index++) {
            if (Terrain.getCustomMaterial(index) != null) {
                Terrain.setCustomMaterial(index, null);
            }
            if (customMaterialButtons[index] != null) {
                if (customMaterialButtons[index].isSelected()) {
                    deselectPaint();
                }
                customMaterialButtons[index] = null;
            }
        }
        if (customTerrainPanel != null) {
            while (customTerrainPanel.getComponentCount() > 1) {
                customTerrainPanel.remove(0);
            }
            dockingManager.removeFrame("customTerrain");
            customTerrainPanel = null;
        }
    }

    private void loadCustomTerrains() {
        boolean customTerrainsChanged = false;
        for (int i = 0; i < CUSTOM_TERRAIN_COUNT; i++) {
            MixedMaterial material = world.getMixedMaterial(i);
            Terrain.setCustomMaterial(i, material);
            if (material != null) {
                // TODO: this doesn't preserve the original order of the buttons
                //  is that a problem?
                addButtonForNewCustomTerrain(i, material, false);
                customTerrainsChanged = true;
            }
        }
        if (customTerrainsChanged) {
            customTerrainPanel.validate();
        }
    }

    private void saveCustomLayers() {
        if (dimension != null) {
            if (! customLayerController.paletteManager.isEmpty()) {
                final List<CustomLayer> customLayers = new ArrayList<>();
                for (Palette palette: customLayerController.paletteManager.getPalettes()) {
                    customLayers.addAll(palette.getLayers());
                }
                dimension.setCustomLayers(customLayers);
            } else {
                dimension.setCustomLayers(emptyList());
            }
        }
    }

    private void saveCustomMaterials() {
        for (int i = 0; i < CUSTOM_TERRAIN_COUNT; i++) {
            world.setMixedMaterial(i, Terrain.getCustomMaterial(i));
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
        hidePreferences = MacUtils.installPreferencesHandler(this::openPreferences);
    }
    
    private void showGlobalOperations() {
        if ((world == null) || (dimension == null)) {
            DesktopUtils.beep();
            return;
        }
        cancelPaintSelection();
        final List<Layer> allLayers = getAllLayers();
        final List<Integer> allBiomes = getAllBiomes(world.getPlatform(), customBiomeManager);
        final FillDialog dialog = new FillDialog(App.this, dimension, allLayers.toArray(new Layer[allLayers.size()]), selectedColourScheme, allBiomes.toArray(new Integer[allBiomes.size()]), customBiomeManager, view, selectionState);
        dialog.setVisible();
    }

    private void exportImage() {
        if (dimension == null) {
            DesktopUtils.beep();
            return;
        }
        if (! imageFitsInJavaArray(dimension)) {
            beepAndShowError(this, "The dimension is too large to export to an image.\nThe area (width x height) may not be more than " + INT_NUMBER_FORMAT.format(Integer.MAX_VALUE), "Dimension Too Large");
            return;
        }
        final Set<String> extensions = new HashSet<>(asList(ImageIO.getReaderFileSuffixes()));
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
        String defaultname = world.getName().replaceAll("\\s", "").toLowerCase() + ((dimension.getAnchor().dim == DIM_NORMAL) ? "" : ("_" + dimension.getName().toLowerCase())) + ".png"; // NOI18N
        File selectedFile = FileUtils.selectFileForSave(App.this, "Export as image file", new File(defaultname), new FileFilter() {
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

            @Override
            public String getExtensions() {
                return String.join(";", extensions);
            }
        });
        if (selectedFile != null) {
            final String type;
            int p = selectedFile.getName().lastIndexOf('.');
            if (p != -1) {
                type = selectedFile.getName().substring(p + 1).toUpperCase();
            } else {
                type = "PNG"; // NOI18N
                selectedFile = new File(selectedFile.getParentFile(), selectedFile.getName() + ".png");
            }
            if (selectedFile.exists()) {
                if (showConfirmDialog(App.this, strings.getString("the.file.already.exists"), strings.getString("overwrite.file"), YES_NO_OPTION) != YES_OPTION) {
                    return;
                }
            }
            final File file = selectedFile;
            //noinspection ConstantConditions // Can't happen for non-cancelable task
            if (! ProgressDialog.executeTask(App.this, new ProgressTask<Boolean>() {
                        @Override
                        public String getName() {
                            return strings.getString("exporting.image");
                        }

                        @Override
                        public Boolean execute(ProgressReceiver progressReceiver) {
                            // Leave the progress receiver indeterminate, since by *far* the most time goes into
                            // actually writing the file, and we can't report progress for that
                            try {
                                return ImageIO.write(view.getImage(), type, file);
                            } catch (IOException e) {
                                throw new RuntimeException("I/O error while exporting image", e);
                            }
                        }
                    }, NOT_CANCELABLE)) {
                showMessageDialog(App.this, MessageFormat.format(strings.getString("format.0.not.supported"), type));
            }
        }
    }
    
    private void exportHeightMap(HeightMapExporter.Format format) {
        if (dimension == null) {
            DesktopUtils.beep();
            return;
        }
        if (! imageFitsInJavaArray(dimension)) {
            beepAndShowError(this, "The dimension is too large to export to a height map.\nThe area (width x height) may not be more than " + INT_NUMBER_FORMAT.format(Integer.MAX_VALUE), "Dimension Too Large");
            return;
        }
        final HeightMapExporter heightMapExporter = new HeightMapExporter(dimension, format);
        final List<String> extensions = heightMapExporter.getSupportedFileExtensions();
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
        Configuration config = Configuration.getInstance();
        File dir = config.getHeightMapsDirectory();
        if ((dir == null) || (! dir.isDirectory())) {
            dir = DesktopUtils.getPicturesFolder();
        }
        File defaultFile = new File(dir, heightMapExporter.getDefaultFilename());
        File selectedFile = FileUtils.selectFileForSave(App.this, (format == INTEGER_HIGH_RESOLUTION) ? "Export as high resolution height map image file" : "Export as height map image file", defaultFile, new FileFilter() {
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

            @Override
            public String getExtensions() {
                return String.join(";", extensions);
            }
        });
        if (selectedFile != null) {
            final String type;
            int p = selectedFile.getName().lastIndexOf('.');
            if (p != -1) {
                type = selectedFile.getName().substring(p + 1).toUpperCase();
            } else {
                beepAndShowError(App.this, "No filename extension specified.", "Missing Extension");
                return;
            }
            if (selectedFile.exists()) {
                if (showConfirmDialog(App.this, strings.getString("the.file.already.exists"), strings.getString("overwrite.file"), YES_NO_OPTION) != YES_OPTION) {
                    return;
                }
            }
            if (! ImageIO.getImageWritersBySuffix(type).hasNext()) {
                beepAndShowError(this, "Filename extension " + type + " is not a supported image type.", "Unsupported Format");
                return;
            }
            config.setHeightMapsDirectory(selectedFile.getParentFile());
            final File file = selectedFile;
            //noinspection ConstantConditions // Can't happen for non-cancelable task
            if (ProgressDialog.executeTask(App.this, new ProgressTask<Boolean>() {
                        @Override
                        public String getName() {
                            return strings.getString("exporting.height.map");
                        }

                        @Override
                        public Boolean execute(ProgressReceiver progressReceiver) {
                            return heightMapExporter.exportToFile(file);
                        }
                    }, NOT_CANCELABLE)) {
                MessageUtils.showInfo(App.this, "Dimension exported to " + selectedFile.getName() + "\n" + heightMapExporter.getFormatDescription(), "Export Succeeded");
            } else {
                beepAndShowError(App.this, MessageFormat.format(strings.getString("format.0.not.supported"), type), "Unsupported Format");
            }
        }
    }

    private boolean imageFitsInJavaArray(Dimension dimension) {
        final long areaInTiles = (long) dimension.getWidth() * dimension.getHeight();
        return (areaInTiles >= 0L) && (areaInTiles <= 131071L);
    }

    void importLayers(String paletteName, Function<Layer, Boolean> filter) {
        if (dimension == null) {
            DesktopUtils.beep();
            return;
        }
        final Configuration config = Configuration.getInstance();
        File layerDirectory = config.getLayerDirectory();
        if ((layerDirectory == null) || (! layerDirectory.isDirectory())) {
            layerDirectory = DesktopUtils.getDocumentsFolder();
        }
        final File[] selectedFiles = FileUtils.selectFilesForOpen(this, "Select WorldPainter layer file(s)", layerDirectory, new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".layer");
            }

            @Override
            public String getDescription() {
                return "WorldPainter Custom Layers (*.layer)";
            }

            @Override
            public String getExtensions() {
                return "*.layer";
            }
        });
        if (selectedFiles != null) {
            boolean updateCustomTerrainButtons = false;
            for (File selectedFile: selectedFiles) {
                try {
                    try (ObjectInputStream in = new ObjectInputStream(new GZIPInputStream(new BufferedInputStream(new FileInputStream(selectedFile))))) {
                        final CustomLayer layer = (CustomLayer) in.readObject();
                        for (Layer existingLayer: customLayerController.getCustomLayers()) {
                            if (layer.equals(existingLayer)) {
                                beepAndShowError(this, "That layer is already present in the dimension.\nThe layer has not been added.", "Layer Already Present");
                                return;
                            }
                        }
                        if ((filter != null) && (! filter.apply(layer))) {
                            beepAndShowError(this, "That layer or layer type is not supported for the current dimension.\nThe layer has not been added.", "Inapplicable Layer Type");
                            return;
                        }
                        if (! layer.isExportableToFile()) {
                            beepAndShowError(this, "That layer is not importable.\nThe layer has not been added.", "Unimportable Layer");
                            return;
                        }
                        if (paletteName != null) {
                            layer.setPalette(paletteName);
                        }
                        updateCustomTerrainButtons = customLayerController.importCustomLayer(layer) || updateCustomTerrainButtons;
                        config.setLayerDirectory(selectedFile.getParentFile());
                    }
                } catch (FileNotFoundException e) {
                    logger.error("File not found while loading file " + selectedFile, e);
                    beepAndShowError(this, "The specified path does not exist or is not a file", "Nonexistent File");
                    return;
                } catch (IOException e) {
                    logger.error("I/O error while loading file " + selectedFile, e);
                    beepAndShowError(this, "I/O error occurred while reading the specified file,\nor is not a (valid) WorldPainter layer file", "I/O Error Or Invalid File");
                    return;
                } catch (ClassNotFoundException e) {
                    logger.error("Class not found exception while loading file " + selectedFile, e);
                    beepAndShowError(this, "The specified file is not a (valid) WorldPainter layer file", "Invalid File");
                    return;
                } catch (ClassCastException e) {
                    logger.error("Class cast exception while loading file " + selectedFile, e);
                    beepAndShowError(this, "The specified file is not a (valid) WorldPainter layer file", "Invalid File");
                    return;
                }
            }
            if (updateCustomTerrainButtons) {
                updateCustomTerrainButtons();
            }
        }
    }

    private void updateCustomTerrainButtons() {
        for (int i = 0; i < CUSTOM_TERRAIN_COUNT; i++) {
            if (customMaterialButtons[i] != null) {
                MixedMaterial material = Terrain.getCustomMaterial(i);
                customMaterialButtons[i].setIcon(new ImageIcon(material.getIcon(selectedColourScheme)));
                customMaterialButtons[i].setToolTipText(MessageFormat.format(strings.getString("customMaterial.0.right.click.to.change"), material));
            }
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

    void importCustomItemsFromWorld(CustomItemsTreeModel.ItemType itemType, Function<Layer, Boolean> filter) {
        final File dir;
        final Configuration config = Configuration.getInstance();
        if (lastSelectedFile != null) {
            dir = lastSelectedFile.getParentFile();
        } else if ((config != null) && (config.getWorldDirectory() != null)) {
            dir = config.getWorldDirectory();
        } else {
            dir = DesktopUtils.getDocumentsFolder();
        }
        final File selectedFile = FileUtils.selectFileForOpen(this, "Select a WorldPainter world", dir,
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

                    @Override
                    public String getExtensions() {
                        return "*.world";
                    }
                });
        if (selectedFile != null) {
            if (! selectedFile.isFile()) {
                if (logger.isDebugEnabled()) {
                    try {
                        logger.debug("Path not a file according to File.isFile(): \"" + selectedFile + "\" (directory: " + selectedFile.isDirectory() + "; length: " + selectedFile.length() + "; absolutePath: \"" + selectedFile.getAbsolutePath() + "\"; canonicalPath: \"" + selectedFile.getCanonicalPath() + "\")");
                    } catch (IOException e) {
                        logger.debug("Path not a file according to File.isFile(): \"" + selectedFile + "\" (directory: " + selectedFile.isDirectory() + "; length: " + selectedFile.length() + "; absolutePath: \"" + selectedFile.getAbsolutePath() + "\")");
                        logger.warn("I/O error while trying to report canonical path of file: \"" + selectedFile + "\"", e);
                    }
                }
                showMessageDialog(this, "The specified path does not exist or is not a file", "File Does Not Exist", ERROR_MESSAGE);
                return;
            }
            if (! selectedFile.canRead()) {
                showMessageDialog(this, "WorldPainter is not authorised to read the selected file", "Access Denied", ERROR_MESSAGE);
                return;
            }
            final World2 selectedWorld = ProgressDialog.executeTask(this, new LoadWorldTask(this, selectedFile), NOT_CANCELABLE);
            if (selectedWorld == null) {
                // The file was damaged
                return;
            }
            if (CustomItemsTreeModel.hasCustomItems(selectedWorld, itemType)) {
                final ImportCustomItemsDialog dialog = new ImportCustomItemsDialog(this, selectedWorld, selectedColourScheme, itemType);
                dialog.setVisible(true);
                if (! dialog.isCancelled()) {
                    final StringBuilder errors = new StringBuilder();
                    List<CustomLayer> existingCustomLayers = null;
                    boolean refreshView = false, showError = false, updateCustomTerrainButtons = false;
                    for (Object selectedItem: dialog.getSelectedItems()) {
                        if (selectedItem instanceof CustomLayer) {
                            if (existingCustomLayers == null) {
                                existingCustomLayers = customLayerController.getCustomLayers();
                            }
                            if (existingCustomLayers.contains(selectedItem)) {
                                errors.append("Layer \"" + ((CustomLayer) selectedItem).getName() + "\" already exists\n");
                            } else if ((filter != null) && (! filter.apply((CustomLayer) selectedItem))) {
                                errors.append("Layer \"" + ((CustomLayer) selectedItem).getName() + "\" or layer type not supported for current dimension\n");
                                showError = true;
                            } else {
                                updateCustomTerrainButtons = customLayerController.importCustomLayer((CustomLayer) selectedItem) || updateCustomTerrainButtons;
                            }
                        } else if (selectedItem instanceof MixedMaterial) {
                            MixedMaterial customMaterial = (MixedMaterial) selectedItem;
                            final int index = findNextCustomTerrainIndex();
                            if (index == -1) {
                                errors.append("No free slots for Custom Terrain \"" + customMaterial.getName() + "\"\n");
                                showError = true;
                                continue;
                            }
                            customMaterial = MixedMaterialManager.getInstance().register(customMaterial);
                            addButtonForNewCustomTerrain(index, customMaterial, false);
                        } else if (selectedItem instanceof CustomBiome) {
                            final CustomBiome customBiome = (CustomBiome) selectedItem;
                            if (! customBiomeManager.addCustomBiome(null, customBiome)) {
                                errors.append("ID already in use for Custom Biome " + customBiome.getId() + " (\"" + customBiome + "\")\n");
                                showError = true;
                            } else {
                                refreshView = true;
                            }
                        } else {
                            throw new InternalError("Unsupported custom item type " + selectedItem.getClass() + " encountered");
                        }
                    }
                    if (refreshView) {
                        view.refreshTiles();
                    }
                    if (updateCustomTerrainButtons) {
                        updateCustomTerrainButtons();
                    }
                    if (errors.length() > 0) {
                        JOptionPane.showMessageDialog(App.this, "Not all items have been imported:\n\n" + errors, "Not All Items Imported", showError ? JOptionPane.ERROR_MESSAGE : JOptionPane.WARNING_MESSAGE);
                    }
                }
            } else {
                final String what;
                switch (itemType) {
                    case ALL:
                        what = "layers, terrains or biomes";
                        break;
                    case BIOME:
                        what = "biomes";
                        break;
                    case LAYER:
                        what = "layers";
                        break;
                    case TERRAIN:
                        what = "terrains";
                        break;
                    default:
                        throw new InternalError();
                }
                showWarning(this, "The selected world has no, or no importable, custom " + what + ".", "No Custom Items To Import");
            }
        }
    }

    private void showHelpPicker() {
        Component glassPane = getGlassPane();
        MouseListener mouseListener = new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                glassPane.setVisible(false);
                glassPane.removeMouseListener(this);
                glassPane.setCursor(null);
                showHelp(SwingUtilities.getDeepestComponentAt(getRootPane(), e.getX(), e.getY()));
            }
        };
        glassPane.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        glassPane.addMouseListener(mouseListener);
        glassPane.setVisible(true);
    }

    private void escapePressed() {
        if (mapSelectionController.isSelectionActive()) {
            cancelPaintSelection();
        } else {
            exitDimension();
        }
    }

    private void exitDimension() {
        if ((dimension != null) && ((dimension.getAnchor().role == CAVE_FLOOR) || (dimension.getAnchor().role == FLOATING_FLOOR))) {
            final Anchor anchor = dimension.getAnchor();
            setDimension(world.getDimension(new Anchor(anchor.dim, DETAIL, anchor.invert, 0)));
        } else {
            DesktopUtils.beep();
        }
    }

    static File getAutosaveFile() {
        return new File(Configuration.getConfigDir(), "autosave.world");
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
            setAcceleratorKey(getKeyStroke(VK_N, PLATFORM_COMMAND_MASK));
            setShortDescription(strings.getString("create.a.new.world"));
        }
        
        @Override
        public void performAction(ActionEvent e) {
            newWorld();
        }
    };

    private final BetterAction ACTION_OPEN_WORLD = new BetterAction("openWorld", strings.getString("open.world") + "...", ICON_OPEN_WORLD, false) {
        {
            setAcceleratorKey(getKeyStroke(VK_O, PLATFORM_COMMAND_MASK));
            setShortDescription(strings.getString("open.an.existing.worldpainter.world"));
        }
        
        @Override
        public void performAction(ActionEvent e) {
            open();
        }
    };
    
    private final BetterAction ACTION_SAVE_WORLD = new BetterAction("saveWorld", strings.getString("save.world") + "...", ICON_SAVE_WORLD, false) {
        {
            setAcceleratorKey(getKeyStroke(VK_S, PLATFORM_COMMAND_MASK));
            setShortDescription(strings.getString("save.the.world.as.a.worldpainter.file.to.the.previously.used.file"));
        }
        
        @Override
        public void performAction(ActionEvent e) {
            save();
        }
    };

    private final BetterAction ACTION_SAVE_WORLD_AS = new BetterAction("saveWorldAs", strings.getString("save.world.as") + "...", ICON_SAVE_WORLD, false) {
        {
            setAcceleratorKey(getKeyStroke(VK_S, PLATFORM_COMMAND_MASK | SHIFT_DOWN_MASK));
            setShortDescription(strings.getString("save.the.world.as.a.worldpainter.file"));
        }
        
        @Override
        public void performAction(ActionEvent e) {
            saveAs();
        }
    };
    
    private final BetterAction ACTION_EXPORT_WORLD = new BetterAction("exportAsMinecraftMap", strings.getString("export.as.minecraft.map") + "...", ICON_EXPORT_WORLD, false) {
        {
            setAcceleratorKey(getKeyStroke(VK_E, PLATFORM_COMMAND_MASK));
            setShortDescription(strings.getString("export.the.world.to.a.minecraft.map"));
        }
        
        @Override
        public void performAction(ActionEvent e) {
            if (world == null) {
                DesktopUtils.beep();
                return;
            }
            pauseAutosave();
            try {
                if (world.getImportedFrom() != null) {
                    DesktopUtils.beep();
                    if (showConfirmDialog(App.this, strings.getString("this.is.an.imported.world"), strings.getString("imported"), YES_NO_OPTION, WARNING_MESSAGE) != YES_OPTION) {
                        return;
                    }
                }
                saveCustomBiomes();
                saveCustomLayers();
                ExportWorldDialog dialog = new ExportWorldDialog(App.this, world, selectedColourScheme, customBiomeManager, hiddenLayers, false, 10, view.getLightOrigin(), view);
                dialog.setVisible(() -> {
                    view.refreshTiles();
                    if (threeDeeFrame != null) {
                        threeDeeFrame.refresh(false);
                    }
                });
            } finally {
                resumeAutosave();
            }
        }
    };
    
    private final BetterAction ACTION_IMPORT_MAP = new BetterAction("importMinecraftMap", strings.getString("existing.minecraft.map") + "...", false) {
        {
            setAcceleratorKey(getKeyStroke(VK_I, PLATFORM_COMMAND_MASK));
            setShortDescription("Import the landscape of an existing Minecraft map. Use Merge to merge your changes.");
        }

        @Override
        public void performAction(ActionEvent e) {
            importWorld();
        }
    };

    private final BetterAction ACTION_MERGE_WORLD = new BetterAction("mergeWorld", strings.getString("merge.world") + "...", false) {
        {
            setAcceleratorKey(getKeyStroke(VK_R, PLATFORM_COMMAND_MASK));
            setShortDescription("Merge the changes in a previously Imported world back to the original Minecraft map.");
        }

        @Override
        public void performAction(ActionEvent e) {
            if ((world == null) || (dimension == null)) {
                DesktopUtils.beep();
                return;
            }
            merge();
        }
    };

    private final BetterAction ACTION_EXIT = new BetterAction("exit", strings.getString("exit") + "...", ICON_EXIT) {
        {
            setShortDescription(strings.getString("shut.down.worldpainter"));
        }
        
        @Override
        public void performAction(ActionEvent e) {
            exit();
        }
    };

    private final BetterAction ACTION_ZOOM_IN = new BetterAction("zoomIn", strings.getString("zoom.in"), ICON_ZOOM_IN) {
        {
            setAcceleratorKey(getKeyStroke(VK_ADD, PLATFORM_COMMAND_MASK));
            setShortDescription(strings.getString("zoom.in"));
        }
        
        @Override
        public void performAction(ActionEvent e) {
//            Point location = view.getViewCentreInWorldCoords();
            int zoom = view.getZoom() + 1;
            view.setZoom(zoom);
            updateZoomLabel();
//            view.moveTo(location);
            if (zoom == 6) {
                setEnabled(false);
            }
            ACTION_ZOOM_OUT.setEnabled(true);
            ACTION_ZOOM_RESET.setEnabled(zoom != 0);
        }
    };

    private final BetterAction ACTION_ZOOM_RESET = new BetterAction("resetZoom", strings.getString("reset.zoom"), ICON_ZOOM_RESET) {
        {
            setAcceleratorKey(getKeyStroke(VK_0, PLATFORM_COMMAND_MASK));
            setShortDescription(strings.getString("reset.the.zoom.level.to.1.1"));
            setEnabled(false);
        }
        
        @Override
        public void performAction(ActionEvent e) {
//            int oldZoom = zoom;
            view.resetZoom();
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
    };
    
    private final BetterAction ACTION_ZOOM_OUT = new BetterAction("zoomOut", strings.getString("zoom.out"), ICON_ZOOM_OUT) {
        {
            setAcceleratorKey(getKeyStroke(VK_SUBTRACT, PLATFORM_COMMAND_MASK));
            setShortDescription(strings.getString("zoom.out"));
        }
        
        @Override
        public void performAction(ActionEvent e) {
//            Point location = view.getViewCentreInWorldCoords();
            int zoom = view.getZoom() - 1;
            view.setZoom(zoom);
            updateZoomLabel();
//            view.moveTo(location);
            if (zoom == -4) {
                setEnabled(false);
            }
            ACTION_ZOOM_IN.setEnabled(true);
            ACTION_ZOOM_RESET.setEnabled(zoom != 0);
        }
    };

    private final BetterAction ACTION_GRID = new BetterAction("grid", strings.getString("grid"), ICON_GRID) {
        {
            setShortDescription(strings.getString("enable.or.disable.the.grid"));
            setSelected(false);
        }
        
        @Override
        public void performAction(ActionEvent e) {
            if (dimension == null) {
                DesktopUtils.beep();
                return;
            }
            view.setPaintGrid(!view.isPaintGrid());
            dimension.setGridEnabled(view.isPaintGrid());
            setSelected(view.isPaintGrid());
        }
    };
    
    private final BetterAction ACTION_CONTOURS = new BetterAction("contours", strings.getString("contours"), ICON_CONTOURS) {
        {
            setShortDescription(strings.getString("enable.or.disable.height.contours"));
            setSelected(false);
        }
        
        @Override
        public void performAction(ActionEvent e) {
            if (dimension == null) {
                DesktopUtils.beep();
                return;
            }
            view.setDrawContours(!view.isDrawContours());
            dimension.setContoursEnabled(view.isDrawContours());
            setSelected(view.isDrawContours());
        }
    };
    
    private final BetterAction ACTION_OVERLAYS = new BetterAction("overlay", strings.getString("overlay"), ICON_OVERLAY) {
        {
            setShortDescription(strings.getString("enable.or.disable.image.overlay"));
        }

        @Override
        public void performAction(ActionEvent e) {
            if (dimension == null) {
                DesktopUtils.beep();
                return;
            }
            if (dimension.isOverlaysEnabled()) {
                // Overlays are showing; disable them
                dimension.setOverlaysEnabled(false);
                setSelected(false);
            } else if ((! dimension.getOverlays().isEmpty()) && dimension.getOverlays().stream().anyMatch(Overlay::isEnabled)) {
                // No overlay is being shown, but there is at least one configured and enable, so show overlays
                dimension.setOverlaysEnabled(true);
                setSelected(true);
            } else if (dimension.getOverlays().size() == 1) {
                // No overlay is being shown, but there is exactly one configured but disabled. Enable it and show
                // overlays
                dimension.getOverlays().get(0).setEnabled(true);
                dimension.setOverlaysEnabled(true);
                setSelected(true);
            } else {
                // Otherwise show the configure view dialog so the user can (re)configure the overlays
                ConfigureViewDialog dialog = new ConfigureViewDialog(App.this, dimension, view, true);
                dialog.setVisible(true);
                setSelected(dimension.isOverlaysEnabled());
                ACTION_GRID.setSelected(view.isPaintGrid());
                ACTION_CONTOURS.setSelected(view.isDrawContours());
            }
        }    };
    
    private final BetterAction ACTION_UNDO = new BetterAction("undo", strings.getString("undo"), ICON_UNDO) {
        {
            setAcceleratorKey(getKeyStroke(VK_Z, PLATFORM_COMMAND_MASK));
            setShortDescription(strings.getString("undo.the.most.recent.action"));
        }
        
        @Override
        public void performAction(ActionEvent e) {
            if (! performUndo()) {
                DesktopUtils.beep();
            }
        }
    };
    
    private final BetterAction ACTION_REDO = new BetterAction("redo", strings.getString("redo"), ICON_REDO) {
        {
            setAcceleratorKey(getKeyStroke(VK_Y, PLATFORM_COMMAND_MASK));
            setShortDescription(strings.getString("redo.the.most.recent.action"));
        }
        
        @Override
        public void performAction(ActionEvent e) {
            if (! performRedo()) {
                DesktopUtils.beep();
            }
        }
    };
    
    private final BetterAction ACTION_EDIT_TILES = new BetterAction("editTiles", strings.getString("add.remove.tiles") + "...", ICON_EDIT_TILES) {
        {
            setAcceleratorKey(getKeyStroke(VK_T, PLATFORM_COMMAND_MASK));
            setShortDescription(strings.getString("add.or.remove.tiles"));
        }
        
        @Override
        public void performAction(ActionEvent e) {
            if (dimension == null) {
                DesktopUtils.beep();
                return;
            }
            addRemoveTiles();
        }
    };
    
    private final BetterAction ACTION_CHANGE_HEIGHT = new BetterAction("changeHeight", strings.getString("change.height") + "...", ICON_CHANGE_HEIGHT) {
        {
            setShortDescription(strings.getString("raise.or.lower.the.entire.map"));
        }
        
        @Override
        public void performAction(ActionEvent e) {
            changeWorldHeight(App.this);
        }
    };

    private final BetterAction ACTION_ROTATE_WORLD = new BetterAction("rotate", strings.getString("rotate") + "...", ICON_ROTATE_WORLD) {
        {
            setShortDescription(strings.getString("rotate.the.entire.map.by.quarter.turns"));
        }
        
        @Override
        public void performAction(ActionEvent e) {
            rotateWorld(App.this);
        }
    };

    private final BetterAction ACTION_SHIFT_WORLD = new BetterAction("shift", "Shift...", ICON_SHIFT_WORLD) {
        {
            setShortDescription("Shift the entire map horizontally by whole 128-block tiles");
        }

        @Override
        public void performAction(ActionEvent e) {
            shiftWorld(App.this);
        }
    };

    private final BetterAction ACTION_SCALE_WORLD = new BetterAction("scale", "Scale...", ICON_SCALE_WORLD) {
        {
            setShortDescription("Scale the entire map up or down by an arbitrary amount");
        }

        @Override
        public void performAction(ActionEvent e) {
            scaleWorld(App.this);
        }
    };

    private final BetterAction ACTION_DIMENSION_PROPERTIES = new BetterAction("dimensionProperties", strings.getString("dimension.properties") + "...", ICON_DIMENSION_PROPERTIES) {
        {
            setAcceleratorKey(getKeyStroke(VK_P, PLATFORM_COMMAND_MASK));
            setShortDescription(strings.getString("edit.the.properties.of.this.dimension"));
        }
        
        @Override
        public void performAction(ActionEvent e) {
            if (dimension == null) {
                DesktopUtils.beep();
                return;
            }
            final boolean previousCoverSteepTerrain = dimension.isCoverSteepTerrain();
            final int previousTopLayerMinDepth = dimension.getTopLayerMinDepth();
            final int previousTopLayerVariation = dimension.getTopLayerVariation();
            final Dimension.Border previousBorder = dimension.getBorder();
            final int previousBorderSize = dimension.getBorderSize();
            final long previousMinecraftSeed = dimension.getMinecraftSeed();
            final int previousCeilingHeight = dimension.getCeilingHeight();
            final Dimension.WallType previousWallType = dimension.getWallType();
            final BorderSettings previousBorderSettings = (dimension.getWorld() != null) ? dimension.getWorld().getBorderSettings().clone() : null;
            final Dimension.LayerAnchor previousTopLayerAnchor = dimension.getTopLayerAnchor();
            final boolean previousAnnotationsExport = (dimension.getLayerSettings(Annotations.INSTANCE) != null) && ((AnnotationsExporter.AnnotationsSettings) dimension.getLayerSettings(Annotations.INSTANCE)).isExport();
            final DimensionPropertiesDialog dialog = new DimensionPropertiesDialog(App.this, dimension, selectedColourScheme, customBiomeManager);
            dialog.setVisible(true);
            if (! dialog.isCancelled()) {
                if (threeDeeFrame != null) {
                    final boolean newAnnotationsExport = (dimension.getLayerSettings(Annotations.INSTANCE) != null) && ((AnnotationsExporter.AnnotationsSettings) dimension.getLayerSettings(Annotations.INSTANCE)).isExport();
                    if ((dimension.isCoverSteepTerrain() != previousCoverSteepTerrain)
                            || (dimension.getTopLayerMinDepth() != previousTopLayerMinDepth)
                            || (dimension.getTopLayerVariation() != previousTopLayerVariation)
                            || (dimension.getTopLayerAnchor() != previousTopLayerAnchor)
                            || (newAnnotationsExport != previousAnnotationsExport)) {
                        threeDeeFrame.refresh(false);
                    }
                }
                if ((dimension.getBorder() != previousBorder)
                        || ((dimension.getBorder() != null) && (dimension.getBorderSize() != previousBorderSize))
                        || (dimension.getMinecraftSeed() != previousMinecraftSeed)
                        || (dimension.getCeilingHeight() != previousCeilingHeight)
                        || (dimension.getWallType() != previousWallType)
                        || (dimension.getTopLayerAnchor() != previousTopLayerAnchor)) {
                    view.refreshTiles();
                }
                if ((previousBorderSettings != null) && (!previousBorderSettings.equals(dimension.getWorld().getBorderSettings()))) {
                    view.repaint();
                }
            }
        }
    };
    
    private final BetterAction ACTION_VIEW_DISTANCE = new BetterAction("viewDistance", strings.getString("view.distance"), ICON_VIEW_DISTANCE) {
        {
            setShortDescription(strings.getString("enable.or.disable.showing.the.maximum.far.view.distance"));
            setSelected(false);
        }
        
        @Override
        public void performAction(ActionEvent e) {
            view.setDrawViewDistance(! view.isDrawViewDistance());
            setSelected(view.isDrawViewDistance());
        }
    };

    private final BetterAction ACTION_WALKING_DISTANCE = new BetterAction("walkingDistances", strings.getString("walking.distances"), ICON_WALKING_DISTANCE) {
        {
            setShortDescription(strings.getString("enable.or.disable.showing.the.walking.distances"));
            setSelected(false);
        }
        
        @Override
        public void performAction(ActionEvent e) {
            view.setDrawWalkingDistance(! view.isDrawWalkingDistance());
            setSelected(view.isDrawWalkingDistance());
        }
    };

    private final BetterAction ACTION_ROTATE_LIGHT_RIGHT = new BetterAction("rotateLightClockwise", strings.getString("rotate.light.clockwise"), ICON_ROTATE_LIGHT_RIGHT) {
        {
            setAcceleratorKey(getKeyStroke(VK_R, 0));
            setShortDescription(strings.getString("rotate.the.direction.the.light.comes.from.clockwise"));
        }
        
        @Override
        public void performAction(ActionEvent e) {
            view.rotateLightRight();
        }
    };
    
    private final BetterAction ACTION_ROTATE_LIGHT_LEFT = new BetterAction("rotateLightAnticlockwise", strings.getString("rotate.light.anticlockwise"), ICON_ROTATE_LIGHT_LEFT) {
        {
            setAcceleratorKey(getKeyStroke(VK_L, 0));
            setShortDescription(strings.getString("rotate.the.direction.the.light.comes.from.anticlockwise"));
        }
        
        @Override
        public void performAction(ActionEvent e) {
            view.rotateLightLeft();
        }
    };
    
    private final BetterAction ACTION_MOVE_TO_SPAWN = new BetterAction("moveToSpawn", strings.getString("move.to.spawn"), ICON_MOVE_TO_SPAWN) {
        {
            setShortDescription(strings.getString("move.the.view.to.the.spawn.point"));
        }
        
        @Override
        public void performAction(ActionEvent e) {
            view.moveToSpawn();
        }
    };
    
    private final BetterAction ACTION_MOVE_TO_ORIGIN = new BetterAction("moveToOrigin", strings.getString("move.to.origin"), ICON_MOVE_TO_ORIGIN) {
        {
            setShortDescription(strings.getString("move.the.view.to.the.origin.coordinates.0.0"));
        }
        
        @Override
        public void performAction(ActionEvent e) {
            view.moveToOrigin();
        }
    };
    
    private final BetterAction ACTION_OPEN_DOCUMENTATION = new BetterAction("browseDocumentation", strings.getString("browse.documentation")) {
        {
            setAcceleratorKey(getKeyStroke(VK_F1, 0));
        }
        
        @Override
        public void performAction(ActionEvent event) {
            try {
                DesktopUtils.open(new URL("https://www.worldpainter.net/doc/"));
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
    };
    
    private final BetterAction ACTION_IMPORT_LAYER = new BetterAction("importLayer", "Import custom layer(s)") {
        @Override
        protected void performAction(ActionEvent e) {
            importLayers(null, getLayerFilterForCurrentDimension());
        }
    };
    
    private final BetterAction ACTION_ROTATE_BRUSH_LEFT = new BetterAction("rotateBrushLeft", "Rotate brush counterclockwise fifteen degrees") {
        @Override
        protected void performAction(ActionEvent e) {
            setRotation(brushRotationSlider.getValue() - 15);
        }
    };
    
    private final BetterAction ACTION_ROTATE_BRUSH_RIGHT = new BetterAction("rotateBrushRight", "Rotate brush clockwise fifteen degrees") {
        @Override
        protected void performAction(ActionEvent e) {
            setRotation(brushRotationSlider.getValue() + 15);
        }
    };
    
    private final BetterAction ACTION_ROTATE_BRUSH_RESET = new BetterAction("rotateBrushReset", "Reset brush rotation to zero degrees") {
        @Override
        protected void performAction(ActionEvent e) {
            setRotation(0);
        }
    };

    private final BetterAction ACTION_ROTATE_BRUSH_RIGHT_30_DEGREES = new BetterAction("rotateBrushRight30Degrees", "Rotate brush clockwise 30 degrees") {
        @Override
        protected void performAction(ActionEvent e) {
            setRotation(brushRotationSlider.getValue() + 30);
        }
    };

    private final BetterAction ACTION_ROTATE_BRUSH_RIGHT_45_DEGREES = new BetterAction("rotateBrushRight45Degrees", "Rotate brush clockwise 45 degrees") {
        @Override
        protected void performAction(ActionEvent e) {
            setRotation(brushRotationSlider.getValue() + 45);
        }
    };
    
    private final BetterAction ACTION_ROTATE_BRUSH_RIGHT_90_DEGREES = new BetterAction("rotateBrushRight90Degrees", "Rotate brush clockwise 90 degrees") {
        @Override
        protected void performAction(ActionEvent e) {
            setRotation(brushRotationSlider.getValue() + 90);
        }
    };
    
    private final BetterAction ACTION_RESET_DOCKS = new BetterAction("resetDockLayout", "Reset current and default") {
        @Override
        protected void performAction(ActionEvent e) {
            DesktopUtils.beep();
            if (JOptionPane.showConfirmDialog(App.this, "Are you sure you want to reset the workspace?", "Confirm Workspace Reset", YES_NO_OPTION) == YES_OPTION) {
                dockingManager.resetToDefault();
                Configuration config = Configuration.getInstance();
                config.setDefaultJideLayoutData(null);
                ACTION_LOAD_LAYOUT.setEnabled(false);
            }
        }
    };

    private final BetterAction ACTION_RESET_ALL_DOCKS = new BetterAction("resetAllDockLayout", "Reset current, default and all saved worlds") {
        @Override
        protected void performAction(ActionEvent e) {
            DesktopUtils.beep();
            if (JOptionPane.showConfirmDialog(App.this, "Are you sure you want to reset the workspace for all worlds?", "Confirm Workspace Reset", YES_NO_OPTION) == YES_OPTION) {
                dockingManager.resetToDefault();
                Configuration config = Configuration.getInstance();
                config.setDefaultJideLayoutData(null);
                config.setJideLayoutData(null);
                ACTION_LOAD_LAYOUT.setEnabled(false);
            }
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
            showInfo(App.this, "Workspace layout saved", "Workspace layout saved");
        }
    };

    private final BetterAction ACTION_SWITCH_TO_FROM_CEILING = new BetterAction("switchCeiling", "Switch to/from Ceiling") {
        {
            setAcceleratorKey(getKeyStroke(VK_C, PLATFORM_COMMAND_MASK));
        }

        @Override
        public void performAction(ActionEvent e) {
            if ((dimension != null) && (world != null)) {
                final Anchor anchor = dimension.getAnchor();
                final Dimension oppositeDimension = world.getDimension(new Anchor(anchor.dim, DETAIL, ! anchor.invert, 0));
                if ((oppositeDimension != dimension) && (oppositeDimension != null)) {
                    setDimension(oppositeDimension);
                    return;
                }
            }
            DesktopUtils.beep();
        }
    };

    private final BetterAction ACTION_SWITCH_TO_FROM_MASTER = new BetterAction("switchMaster", "Switch to/from Master") {
        {
            setAcceleratorKey(getKeyStroke(VK_M, PLATFORM_COMMAND_MASK));
        }

        @Override
        public void performAction(ActionEvent e) {
            if ((dimension != null) && (world != null)) {
                final Anchor anchor = dimension.getAnchor();
                final Dimension oppositeDimension = world.getDimension(new Anchor(anchor.dim, (anchor.role == MASTER) ? DETAIL : MASTER, false, 0));
                if ((oppositeDimension != dimension) && (oppositeDimension != null)) {
                    setDimension(oppositeDimension);
                    return;
                }
            }
            DesktopUtils.beep();
        }
    };

    private final BetterAction ACTION_SHOW_CUSTOM_TERRAIN_POPUP = new BetterAction("showCustomTerrainMenu", null, loadScaledIcon("plus")) {
        {
            setShortDescription("Add a new Custom Terrain");
        }

        @Override
        protected void performAction(ActionEvent e) {
            if (dimension == null) {
                DesktopUtils.beep();
                return;
            }
            showCustomTerrainButtonPopup(e, -1);
        }
    };

    private final BetterAction ACTION_SHOW_HELP_PICKER = new BetterAction("showHelpPicker", "Help for control", loadScaledIcon("information")) {
        {
            setShortDescription("Show help information for a specific control");
        }

        @Override
        protected void performAction(ActionEvent e) {
            showHelpPicker();
        }
    };

    private final BetterAction ACTION_ESCAPE = new BetterAction("exitDimension", "Leave the current operation") {
        {
            setAcceleratorKey(getKeyStroke(VK_ESCAPE, 0));
        }

        @Override
        protected void performAction(ActionEvent e) {
            escapePressed();
        }
    };

    final Map<Layer, LayerControls> layerControls = new HashMap<>();
    final Map<Layer, JCheckBox> layerSoloCheckBoxes = new HashMap<>();
    final Set<CustomLayer> layersWithNoButton = new HashSet<>();
    final JToggleButton[] customMaterialButtons = new JToggleButton[CUSTOM_TERRAIN_COUNT];

    WorldPainter view;
    DockingManager dockingManager;
    Paint paint = PaintFactory.NULL_PAINT;
    Set<Layer> hiddenLayers = new HashSet<>();
    Layer soloLayer;
    GlassPane glassPane;

    private final ButtonGroup toolButtonGroup = new ButtonGroup(), brushButtonGroup = new ButtonGroup(), paintButtonGroup = new ButtonGroup();
    private final Map<Brush, JToggleButton> brushButtons = new HashMap<>();
    private final Map<Anchor, UndoManager> undoManagers = new HashMap<>();
    private final ColourScheme[] colourSchemes;
    private final ColourScheme defaultColourScheme;
    private final List<Layer> layers = LayerManager.getInstance().getLayers();
    private final List<Operation> operations;
    private final BrushOptions brushOptions;
    private final CustomBiomeManager customBiomeManager = new CustomBiomeManager();
    private final Map<String, BufferedImage> callouts = new HashMap<>();
    private final ObservableBoolean selectionState = new ObservableBoolean(true);
    private final boolean darkMode;
    private final MapSelectionController mapSelectionController;
    private final CustomLayerController customLayerController = new CustomLayerController(this);
    private final ScrollController scrollController = new ScrollController(this);

    private World2 world;
    private long lastSavedState = -1, lastAutosavedState = -1, lastSaveTimestamp = -1;
    private volatile long lastChangeTimestamp = -1;
    private Dimension dimension, backgroundDimension;
    private boolean showBackgroundStatus;
    private int backgroundZoom;
    private Operation activeOperation;
    private File lastSelectedFile;
    private JLabel heightLabel, slopeLabel, locationLabel, waterLabel, materialLabel, radiusLabel, zoomLabel, biomeLabel, levelLabel, brushRotationLabel;
    private int radius = 50;
    private Brush brush = SymmetricBrush.PLATEAU_CIRCLE, toolBrush = SymmetricBrush.COSINE_CIRCLE;
    private boolean programmaticChange;
    private UndoManager currentUndoManager;
    private JSlider levelSlider, brushRotationSlider;
    private float level = 0.51f, toolLevel = 0.51f;
    private int maxRadius = DEFAULT_MAX_RADIUS, brushRotation = 0, toolBrushRotation = 0, previousBrushRotation = 0;
    private JComboBox<TerrainMode> terrainModeComboBox;
    private JCheckBox terrainSoloCheckBox;
    private JToggleButton setSpawnPointToggleButton, eyedropperToggleButton;
    private JMenuItem addNetherMenuItem, removeNetherMenuItem, addEndMenuItem, removeEndMenuItem, addCeilingMenuItem, removeCeilingMenuItem, addMasterMenuItem, removeMasterMenuItem;
    private JCheckBoxMenuItem viewSurfaceMenuItem, viewNetherMenuItem, viewEndMenuItem, extendedBlockIdsMenuItem;
    private ColourScheme selectedColourScheme;
    private BiomeHelper biomeHelper;
    private SortedMap<String, BrushGroup> customBrushes;
    private ThreeDeeFrame threeDeeFrame;
    private BiomesViewerFrame biomesViewerFrame;
    private BiomesPanel biomesPanel;
    private DockableFrame biomesPanelFrame;
    private Filter filter, toolFilter;
    private boolean hideAbout, hidePreferences, hideExit;
    private PaintUpdater paintUpdater = () -> {
        // Do nothing
    };
    private JMenu recentMenu;
    private JPanel toolSettingsPanel, customTerrainPanel;
    private Timer autosaveTimer;
    private int pauseAutosave;
    private long autosaveInhibitedUntil;
    private InfoPanel infoPanel;
    private String outsideDimensionLabel;
    private TerrainMode terrainMode = SHOW_TERRAIN;

    public static final Image ICON = IconUtils.loadScaledImage("org/pepsoft/worldpainter/icons/shovel-icon.png");
    
    public static final int DEFAULT_MAX_RADIUS = 300;

    public static final String KEY_HELP_KEY = "org.pepsoft.worldpainter.helpKey";
    public static final String KEY_ICON = "org.pepsoft.worldpainter.icon";
    public static final String KEY_THUMBNAIL = "org.pepsoft.worldpainter.thumbnail";
    public static final String KEY_PAINT_ID = "org.pepsoft.worldpainter.paint.id";

    public static final Insets BUTTON_INSETS = new Insets(3, 5, 3, 5) {
        @Override
        public void set(int top, int left, int bottom, int right) {
            throw new UnsupportedOperationException();
        }
    };
    public static final Insets SMALLER_BUTTON_INSETS = new Insets(2, 4, 2, 4) {
        @Override
        public void set(int top, int left, int bottom, int right) {
            throw new UnsupportedOperationException();
        }
    };

    public static final NumberFormat INT_NUMBER_FORMAT = NumberFormat.getIntegerInstance();
    public static final NumberFormat FLOAT_NUMBER_FORMAT = NumberFormat.getNumberInstance();

    static final String COMMAND_KEY_NAME = (PLATFORM_COMMAND_MASK == META_DOWN_MASK) ? "⌘" : "Ctrl";

    private static Mode mode = Mode.WORLDPAINTER;

    private static final String ACTION_NAME_INCREASE_RADIUS        = "increaseRadius"; // NOI18N
    private static final String ACTION_NAME_INCREASE_RADIUS_BY_ONE = "increaseRadiusByOne"; // NOI18N
    private static final String ACTION_NAME_DECREASE_RADIUS        = "decreaseRadius"; // NOI18N
    private static final String ACTION_NAME_DECREASE_RADIUS_BY_ONE = "decreaseRadiusByOne"; // NOI18N
    private static final String ACTION_NAME_REDO                   = "redo"; // NOI18N
    private static final String ACTION_NAME_ZOOM_IN                = "zoomIn"; // NOI18N
    private static final String ACTION_NAME_ZOOM_OUT               = "zoomOut"; // NOI18N
    
    private static final long ONE_MEGABYTE = 1024 * 1024;
    
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(App.class);

    private static final Icon ICON_NEW_WORLD            = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/page_white.png");
    private static final Icon ICON_OPEN_WORLD           = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/folder_page_white.png");
    private static final Icon ICON_SAVE_WORLD           = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/disk.png");
    private static final Icon ICON_EXPORT_WORLD         = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/map_go.png");
    private static final Icon ICON_EXIT                 = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/door_in.png");
    private static final Icon ICON_ZOOM_IN              = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/magnifier_zoom_in.png");
    private static final Icon ICON_ZOOM_RESET           = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/magnifier.png");
    private static final Icon ICON_ZOOM_OUT             = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/magnifier_zoom_out.png");
    private static final Icon ICON_GRID                 = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/grid.png");
    private static final Icon ICON_CONTOURS             = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/contours.png");
    private static final Icon ICON_OVERLAY              = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/photo.png");
    private static final Icon ICON_UNDO                 = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/arrow_undo.png");
    private static final Icon ICON_REDO                 = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/arrow_redo.png");
    private static final Icon ICON_EDIT_TILES           = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/plugin.png");
    private static final Icon ICON_CHANGE_HEIGHT        = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/arrow_up_down.png");
    private static final Icon ICON_ROTATE_WORLD         = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/arrow_rotate_anticlockwise.png");
    private static final Icon ICON_DIMENSION_PROPERTIES = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/application_form.png");
    private static final Icon ICON_VIEW_DISTANCE        = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/eye.png");
    private static final Icon ICON_WALKING_DISTANCE     = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/user_go.png");
    private static final Icon ICON_ROTATE_LIGHT_RIGHT   = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/arrow_rotate_lightbulb_clockwise.png");
    private static final Icon ICON_ROTATE_LIGHT_LEFT    = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/arrow_rotate_lightbulb_anticlockwise.png");
    private static final Icon ICON_MOVE_TO_SPAWN        = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/spawn_red.png");
    private static final Icon ICON_MOVE_TO_ORIGIN       = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/arrow_in.png");
    private static final Icon ICON_SHIFT_WORLD          = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/arrow_cross.png");
    private static final Icon ICON_SCALE_WORLD          = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/arrow_out.png");
    private static final Icon ICON_SETTINGS             = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/wrench.png");
    private static final Icon ICON_ANNOTATIONS          = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/annotations.png");
    private static final Icon ICON_BIOMES               = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/deciduous_trees_pattern.png");
    private static final Icon ICON_LAYERS               = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/layers.png");
    private static final Icon ICON_NO_TERRAIN           = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/edit_selection.png");

    private static final String CUSTOM_BRUSHES_DEFAULT_TITLE = "Custom Brushes";

    private static final int MAX_RECENT_FILES = 10;
    
    private static final String HELP_ROOT_URL = "https://www.worldpainter.net/help/";

    private static final ResourceBundle strings = ResourceBundle.getBundle("org.pepsoft.worldpainter.resources.strings"); // NOI18N

    private static final String IMPORT_WARNING_KEY = "org.pepsoft.worldpainter.importWarning";
    private static final String BRUSH_FOLDER_TIP_KEY = "org.pepsoft.worldpainter.brushFolderTip";

    static final String MERGE_WARNING_KEY = "org.pepsoft.worldpainter.mergeWarning";

    @Serial
    private static final long serialVersionUID = 1L;
    
    public class IntensityAction extends BetterAction {
        public IntensityAction(int percentage, int keyCode) {
            super("intensity" + percentage, MessageFormat.format(strings.getString("set.intensity.to.0"), percentage));
            this.percentage = percentage;
            setAcceleratorKey(getKeyStroke(keyCode, 0));
        }

        @Override
        public void performAction(ActionEvent e) {
            levelSlider.setValue(percentage);
        }
        
        private final int percentage;
            }

    interface PaintUpdater {
        void updatePaint();
    }

    class SoloCheckboxHandler implements ActionListener {
        public SoloCheckboxHandler(JCheckBox checkBox, Layer layer) {
            this.checkBox = checkBox;
            this.layer = layer;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (checkBox.isSelected()) {
                layerSoloCheckBoxes.values().stream().filter(otherSoloCheckBox -> otherSoloCheckBox != checkBox).forEach(otherSoloCheckBox -> otherSoloCheckBox.setSelected(false));
                soloLayer = layer;
            } else {
                soloLayer = null;
            }
            updateLayerVisibility();
        }

        private final JCheckBox checkBox;
        private final Layer layer;
    }

    class LayerControls {
        LayerControls(Layer layer, JCheckBox checkBox, JCheckBox soloCheckBox) {
            this(layer, checkBox, soloCheckBox, null);
        }

        LayerControls(Layer layer, JCheckBox checkBox, JCheckBox soloCheckBox, JComponent control) {
            this.layer = layer;
            this.checkBox = checkBox;
            this.soloCheckBox = soloCheckBox;
            this.control = control;
            defaultCheckBoxToolTip = (checkBox != null) ? checkBox.getToolTipText() : null;
            defaultSoloCheckBoxToolTip = (soloCheckBox != null) ? soloCheckBox.getToolTipText() : null;
            defaultButtonToolTip = (control != null) ? control.getToolTipText() : null;
        }

        /**
         * Indicate whether the controls for the layer are currently enabled.
         *
         * @return {@code true} if the controls for the layer are currently
         * enabled.
         */
        boolean isEnabled() {
            return (checkBox == null) || checkBox.isEnabled();
        }

        /**
         * Enable or disable the controls for the layer.
         *
         * @param enabled Whether the controls should be enabled.
         */
        void setEnabled(boolean enabled) {
            doOnEventThread(() -> {
                if (! enabled) {
                    if ((paint instanceof LayerPaint) && ((LayerPaint) paint).getLayer().equals(layer)) {
                        deselectPaint();
                    }
                    if ((soloCheckBox != null) && (soloCheckBox.isSelected())) {
                        soloCheckBox.setSelected(false);
                        soloLayer = null;
                        updateLayerVisibility();
                    }
                }
                if (checkBox != null) {
                    checkBox.setEnabled(enabled);
                    if (enabled) {
                        checkBox.setToolTipText(defaultCheckBoxToolTip);
                    }
                }
                if (soloCheckBox != null) {
                    soloCheckBox.setEnabled(enabled);
                    if (enabled) {
                        soloCheckBox.setToolTipText(defaultSoloCheckBoxToolTip);
                    }
                }
                if (control != null) {
                    control.setEnabled(enabled);
                    if (enabled) {
                        control.setToolTipText(defaultButtonToolTip);
                    }
                }
            });
        }

        /**
         * Disable the controls, and set a temporary tooltip text. When the
         * controls are reenabled the previous tooltip text will automatically
         * be restored.
         *
         * @param toolTipText The temporary tooltip text.
         */
        void disable(String toolTipText) {
            if (isEnabled()) {
                setEnabled(false);
                if (checkBox != null) {
                    checkBox.setToolTipText(toolTipText);
                }
                if (soloCheckBox != null) {
                    soloCheckBox.setToolTipText(toolTipText);
                }
                if (control != null) {
                    control.setToolTipText(toolTipText);
                }
            }
        }

        /**
         * Indicate whether the "solo" checkbox for the layer is currently
         * checked.
         *
         * @return {@code true} if the "solo" checkbox for the layer is
         * currently checked.
         */
        boolean isSolo() {
            return (soloCheckBox != null) && soloCheckBox.isSelected();
        }

        /**
         * Set or reset the "solo" checkbox for the layer.
         *
         * @param solo {@code true} to set the "solo" checkbox for the layer.
         */
        void setSolo(boolean solo) {
            if (soloCheckBox != null) {
                soloCheckBox.setSelected(solo);
            } else {
                throw new IllegalArgumentException("Layer " + layer + " has no solo control");
            }
        }

        protected final Layer layer;
        protected final JCheckBox checkBox;
        protected final JCheckBox soloCheckBox;
        protected final JComponent control;
        protected final String defaultCheckBoxToolTip, defaultSoloCheckBoxToolTip, defaultButtonToolTip;
    }

    public enum Mode { WORLDPAINTER, MINECRAFTMAPEDITOR }

    class BrushGroup {
        BrushGroup(String name, BufferedImage icon, List<Brush> brushes) {
            this.name = name;
            this.icon = icon;
            this.brushes = brushes;
        }

        final String name;
        final BufferedImage icon;
        final List<Brush> brushes;
    }

    public enum TerrainMode { SHOW_TERRAIN, HIDE_TERRAIN, DEFAULT_COLOUR_RAMP, DEFAULT_GREYSCALE_RAMP }
}