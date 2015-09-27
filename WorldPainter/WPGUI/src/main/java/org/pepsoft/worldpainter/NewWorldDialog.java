/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * NewWorldDialog.java
 *
 * Created on Mar 29, 2011, 10:09:56 AM
 */

package org.pepsoft.worldpainter;

import org.pepsoft.minecraft.Material;
import org.pepsoft.util.MathUtils;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.worldpainter.Dimension.Border;
import org.pepsoft.worldpainter.biomeschemes.AutoBiomeScheme;
import org.pepsoft.worldpainter.biomeschemes.Minecraft1_2BiomeScheme;
import org.pepsoft.worldpainter.history.HistoryEntry;
import org.pepsoft.worldpainter.layers.Biome;
import org.pepsoft.worldpainter.layers.Caverns;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.Resources;
import org.pepsoft.worldpainter.layers.exporters.CavernsExporter.CavernsSettings;
import org.pepsoft.worldpainter.layers.exporters.ExporterSettings;
import org.pepsoft.worldpainter.layers.exporters.ResourcesExporter;
import org.pepsoft.worldpainter.themes.SimpleTheme;
import org.pepsoft.worldpainter.themes.TerrainListCellRenderer;

import javax.swing.*;
import javax.swing.JSpinner.DefaultEditor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.worldpainter.Constants.*;
import static org.pepsoft.worldpainter.Terrain.*;
import org.pepsoft.worldpainter.themes.impl.simple.EditSimpleThemeDialog;

/**
 *
 * @author pepijn
 */
public class NewWorldDialog extends javax.swing.JDialog {
    /** Creates new form NewWorldDialog */
    public NewWorldDialog(App app, String name, long seed, int dim, int defaultMaxHeight) {
        this(app, name, seed, dim, defaultMaxHeight, null);
    }
    
    /** Creates new form NewWorldDialog */
    public NewWorldDialog(App app, String name, long seed, int dim, int defaultMaxHeight, Set<Point> tiles) {
        super(app, true);
        this.app = app;
        this.dim = dim;
        this.tiles = tiles;
        
        initComponents();

        comboBoxSurfaceMaterial.setModel(new DefaultComboBoxModel(Terrain.PICK_LIST));
        comboBoxSurfaceMaterial.setRenderer(new TerrainListCellRenderer(app.getColourScheme()));

        comboBoxMaxHeight.setSelectedItem(Integer.toString(defaultMaxHeight));
        
        Configuration config = Configuration.getInstance();
        if (dim == DIM_NORMAL) {
            if (! config.isHilly()) {
                radioButtonFlat.setSelected(true);
                spinnerRange.setEnabled(false);
                spinnerScale.setEnabled(false);
            }
            spinnerWidth.setValue(config.getDefaultWidth() * 128);
            spinnerLength.setValue(config.getDefaultHeight() * 128);
            spinnerTerrainLevel.setValue(config.getLevel());
            spinnerWaterLevel.setValue(config.getWaterLevel());
            spinnerRange.setValue((int) (config.getDefaultRange() + 0.5f));
            spinnerScale.setValue((int) (config.getDefaultScale() * 100 + 0.5));
            checkBoxLava.setSelected(config.isLava());
            comboBoxSurfaceMaterial.setSelectedItem(config.getSurface());
            checkBoxBeaches.setSelected(config.isBeaches());
            checkBoxCircular.setSelected(config.isDefaultCircularWorld());
        }

        // Fix spinner sizes
        ((DefaultEditor) spinnerWidth.getEditor()).getTextField().setColumns(4);
        ((DefaultEditor) spinnerLength.getEditor()).getTextField().setColumns(4);

        if (dim == DIM_NORMAL_CEILING) {
            setTitle("Add Surface Ceiling");
            fieldName.setEnabled(false);
            comboBoxSurfaceMaterial.setSelectedItem(STONE_MIX);
            spinnerTerrainLevel.setValue(58);
            spinnerWaterLevel.setValue(0);
            checkBoxBeaches.setSelected(false);
            comboBoxMaxHeight.setEnabled(false);
        } else if (dim == DIM_NETHER) {
            setTitle("Add Nether");
            fieldName.setEnabled(false);
            comboBoxSurfaceMaterial.setSelectedItem(NETHERLIKE);
            int lavaLevel = defaultMaxHeight * 3 / 4;
            spinnerTerrainLevel.setValue(lavaLevel - 4);
            spinnerWaterLevel.setValue(lavaLevel);
            checkBoxLava.setSelected(true);
            checkBoxBeaches.setSelected(false);
            comboBoxMaxHeight.setEnabled(false);
        } else if (dim == DIM_NETHER_CEILING) {
            setTitle("Add Nether Ceiling");
            fieldName.setEnabled(false);
            comboBoxSurfaceMaterial.setSelectedItem(NETHERLIKE);
            spinnerTerrainLevel.setValue(58);
            spinnerWaterLevel.setValue(0);
            checkBoxBeaches.setSelected(false);
            comboBoxMaxHeight.setEnabled(false);
        } else if (dim == DIM_END) {
            setTitle("Add End");
            fieldName.setEnabled(false);
            comboBoxSurfaceMaterial.setSelectedItem(END_STONE);
            spinnerTerrainLevel.setValue(32);
            spinnerWaterLevel.setValue(0);
            checkBoxBeaches.setSelected(false);
            comboBoxMaxHeight.setEnabled(false);
        } else if (dim == DIM_END_CEILING) {
            setTitle("Add End Ceiling");
            fieldName.setEnabled(false);
            comboBoxSurfaceMaterial.setSelectedItem(END_STONE);
            spinnerTerrainLevel.setValue(58);
            spinnerWaterLevel.setValue(0);
            checkBoxBeaches.setSelected(false);
            comboBoxMaxHeight.setEnabled(false);
        }
        
        if (tiles != null) {
            int lowestX = Integer.MAX_VALUE, highestX = Integer.MIN_VALUE;
            int lowestY = Integer.MAX_VALUE, highestY = Integer.MIN_VALUE;
            for (Point tileCoords: tiles) {
                if (tileCoords.x < lowestX) {
                    lowestX = tileCoords.x;
                }
                if (tileCoords.x > highestX) {
                    highestX = tileCoords.x;
                }
                if (tileCoords.y < lowestY) {
                    lowestY = tileCoords.y;
                }
                if (tileCoords.y > highestY) {
                    highestY = tileCoords.y;
                }
            }
            int width = highestX - lowestX + 1;
            int height = highestY - lowestY + 1;
            spinnerWidth.setValue(width * TILE_SIZE);
            spinnerLength.setValue(height * TILE_SIZE);
            spinnerWidth.setEnabled(false);
            spinnerLength.setEnabled(false);
            checkBoxCircular.setEnabled(false);
        }
        
        TileFactory defaultTileFactory = config.getDefaultTerrainAndLayerSettings().getTileFactory();
        if ((defaultTileFactory instanceof HeightMapTileFactory) && (((HeightMapTileFactory) defaultTileFactory).getTheme() instanceof SimpleTheme)) {
            theme = (SimpleTheme) ((HeightMapTileFactory) defaultTileFactory).getTheme().clone();
        } else {
            theme = SimpleTheme.createDefault((Terrain) comboBoxSurfaceMaterial.getSelectedItem(), Integer.parseInt((String) comboBoxMaxHeight.getSelectedItem()), (Integer) spinnerWaterLevel.getValue());
        }
        
        pack();
        setLocationRelativeTo(app);
        fieldSeed.setText(Long.toString(seed));
        if (seed == World2.DEFAULT_OCEAN_SEED) {
            worldpainterSeed = new Random().nextLong();
        } else if (seed == World2.DEFAULT_LAND_SEED) {
            radioButtonLandSeed.setSelected(true);
            worldpainterSeed = new Random().nextLong();
        } else {
            radioButtonCustomSeed.setSelected(true);
            buttonRandomSeed.setEnabled(true);
            worldpainterSeed = seed;
        }
        fieldName.setText(name);
        fieldName.selectAll();
        fieldName.requestFocusInWindow();
        labelWarning.setVisible(false);
        checkBoxExtendedBlockIds.setSelected(config.isDefaultExtendedBlockIds());
        
        ActionMap actionMap = rootPane.getActionMap();
        actionMap.put("cancel", new AbstractAction("cancel") {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
            
            private static final long serialVersionUID = 1L;
        });

        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");

        rootPane.setDefaultButton(buttonCreate);
        
        updatePreview();
        
        setControlStates();
        updateWalkingTimes();
    }

    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Try to guestimate whether there is enough memory to create a world of the
     * configured size. If not, ask the user whether they want to continue at
     * their own risk.
     * 
     * @param parent The parent to use for the dialog, if necessary.
     * @return <code>true</code> if there is enough memory, or the user
     *     indicated they want to continue at their own risk.
     */
    public boolean checkMemoryRequirements(Window parent) {
        Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        long maxMemory = runtime.maxMemory();
        long freeMemory = runtime.freeMemory();
        long totalMemory = runtime.totalMemory();
        long memoryInUse = totalMemory - freeMemory;
        long availableMemory = maxMemory - memoryInUse;
        // Allow room for export
        // This has been disabled because it was causing too many false
        // negatives. The consequence is that some worlds may be created without
        // warning which may not be able to be exported due to lack of memory.
        // TODO: find a better way to prevent that situation
//        availableMemory -= 250000000L;
        // Convert to KB
        availableMemory /= 1024;
        // Guestimate data size
        long tileCount;
        if (checkBoxCircular.isSelected()) {
            int r = ((Integer) spinnerWidth.getValue()) / TILE_SIZE;
            tileCount = (int) (Math.PI * r * r);
        } else {
            tileCount = (tiles != null) ? tiles.size() : (long) (((Integer) spinnerWidth.getValue()) / TILE_SIZE) * (((Integer) spinnerLength.getValue()) / TILE_SIZE);
        }
        long totalEstimatedDataSize = tileCount * ESTIMATED_TILE_DATA_SIZE;
        if (totalEstimatedDataSize > availableMemory) {
            return JOptionPane.showConfirmDialog(parent, "There may not be enough memory to create a world of that size!\nIt may fail to be created, or cause errors later on.\nPlease consider creating a smaller world, or installing more memory.\nDo you want to continue?", "Large World", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
        }
        return true;
    }
    
    public World2 getSelectedWorld(ProgressReceiver progressReceiver) throws ProgressReceiver.OperationCancelled {
        final String name = fieldName.getText().trim();
        final Dimension dimension = getSelectedDimension(progressReceiver);
        if (dimension == null) {
            // Operation cancelled by user
            return null;
        }
        final World2 world = new World2(dimension.getMaxHeight());
        world.addHistoryEntry(HistoryEntry.WORLD_CREATED);
        final boolean minecraft11Only = dimension.getMaxHeight() != DEFAULT_MAX_HEIGHT_2;
        world.setName(name);

        // Export settings
        final Configuration config = Configuration.getInstance();
        world.setCreateGoodiesChest(config.isDefaultCreateGoodiesChest());
        Generator generator = config.getDefaultGenerator();
        if (minecraft11Only && (generator == Generator.LARGE_BIOMES)) {
            generator = Generator.DEFAULT;
        } else if ((! minecraft11Only) && ((dimension.getMinecraftSeed() == World2.DEFAULT_OCEAN_SEED) || (dimension.getMinecraftSeed() == World2.DEFAULT_LAND_SEED)) && (generator == Generator.DEFAULT)) {
            generator = Generator.LARGE_BIOMES;
        }
        world.setGenerator(generator);
        if (generator == Generator.FLAT) {
            world.setGeneratorOptions(config.getDefaultGeneratorOptions());
        }
        world.setMapFeatures(config.isDefaultMapFeatures());
        world.setGameType(config.getDefaultGameType());
        world.setAllowCheats(config.isDefaultAllowCheats());

        world.addDimension(dimension);
        if (! minecraft11Only) {
            world.setExtendedBlockIds(checkBoxExtendedBlockIds.isSelected());
        }
        if (tiles != null) {
            int lowestX = Integer.MAX_VALUE, highestX = Integer.MIN_VALUE;
            int lowestY = Integer.MAX_VALUE, highestY = Integer.MIN_VALUE;
            for (Point tileCoords: tiles) {
                if (tileCoords.x < lowestX) {
                    lowestX = tileCoords.x;
                }
                if (tileCoords.x > highestX) {
                    highestX = tileCoords.x;
                }
                if (tileCoords.y < lowestY) {
                    lowestY = tileCoords.y;
                }
                if (tileCoords.y > highestY) {
                    highestY = tileCoords.y;
                }
            }
            final int middleX = Math.round((lowestX + highestX) / 2f);
            final int middleY = Math.round((lowestY + highestY) / 2f);
            Point mostCenteredTileCoords = null;
            float mostCenteredTileDistance = Float.MAX_VALUE;
            for (Point tileCoords: tiles) {
                float distance = (float) Math.sqrt((tileCoords.x - middleX) * (tileCoords.x - middleX) + (tileCoords.y - middleY) * (tileCoords.y - middleY));
                if (distance < mostCenteredTileDistance) {
                    mostCenteredTileCoords = tileCoords;
                    mostCenteredTileDistance = distance;
                }
            }
            if (mostCenteredTileCoords != null) {
                world.setSpawnPoint(new Point(mostCenteredTileCoords.x * TILE_SIZE + TILE_SIZE / 2, mostCenteredTileCoords.y * TILE_SIZE + TILE_SIZE / 2));
                if (dimension.getDim() == DIM_NORMAL) {
                    dimension.setLastViewPosition(world.getSpawnPoint());
                }
            }
        }

        if ("true".equals(System.getProperty("org.pepsoft.worldpainter.fancyworlds"))) {
            world.setMixedMaterial(0, new MixedMaterial("Dirt/Gravel", new MixedMaterial.Row[] {new MixedMaterial.Row(Material.DIRT, 750, 1.0f), new MixedMaterial.Row(Material.GRAVEL, 250, 1.0f)}, Minecraft1_2BiomeScheme.BIOME_PLAINS, null, 1.0f));
            world.setMixedMaterial(1, new MixedMaterial("Stone/Gravel", new MixedMaterial.Row[] {new MixedMaterial.Row(Material.STONE, 750, 1.0f), new MixedMaterial.Row(Material.GRAVEL, 250, 1.0f)}, Minecraft1_2BiomeScheme.BIOME_PLAINS, null, 1.0f));
        }
        
        world.setDirty(false);
        return world;
    }
    
    public Dimension getSelectedDimension(final ProgressReceiver progressReceiver) throws ProgressReceiver.OperationCancelled {
        long minecraftSeed;
        try {
            minecraftSeed = Long.parseLong(fieldSeed.getText());
        } catch (NumberFormatException e) {
            minecraftSeed = fieldSeed.getText().hashCode();
        }
        if (radioButtonCustomSeed.isSelected()) {
            worldpainterSeed = minecraftSeed;
        }
        int waterHeight = (Integer) spinnerWaterLevel.getValue();

        final TileFactory tileFactory = createTileFactory(worldpainterSeed);
        
        int maxHeight = Integer.parseInt((String) comboBoxMaxHeight.getSelectedItem());
        final Dimension dimension = new Dimension(minecraftSeed, tileFactory, dim, maxHeight);
        dimension.setEventsInhibited(true);
        try {
            ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            final AtomicBoolean cancelled = new AtomicBoolean();
            if (tiles != null) {
                logger.info("Creating new dimension with " + tiles.size() + " preselected tiles");
                final int[] tileCount = new int[1];
                final int totalTiles = tiles.size();
                for (final Point tileCoords: tiles) {
                    executorService.execute(() -> {
                        if (cancelled.get()) {
                            // Operation cancelled by user
                            return;
                        }
                        Tile tile = tileFactory.createTile(tileCoords.x, tileCoords.y);
                        dimension.addTile(tile);
                        if (progressReceiver != null) {
                            synchronized (tileCount) {
                                tileCount[0]++;
                                try {
                                    progressReceiver.setProgress((float) tileCount[0] / totalTiles);
                                } catch (ProgressReceiver.OperationCancelled e) {
                                    cancelled.set(true);
                                }
                            }
                        }
                    });
                }
            } else if (checkBoxCircular.isSelected()) {
                final int radius = (Integer) spinnerWidth.getValue() / 2;
                int diameter = radius * 2;
                logger.info("Creating new circular dimension with diameter " + diameter + " blocks");
                int tileRadius = (radius + 127) / 128;
                final int[] tileCount = new int[1];
                final int approximateTotalTiles = (int) Math.ceil(Math.PI * tileRadius * tileRadius);
                for (int x = -tileRadius; x < tileRadius; x++) {
                    for (int y = -tileRadius; y < tileRadius; y++) {
                        if (org.pepsoft.worldpainter.util.MathUtils.getSmallestDistanceFromOrigin(x, y) < radius) {
                            // At least one corner is inside the circle; include
                            // the tile. Note that this is always correct in
                            // this case only because the centre of the circle
                            // is always at a tile intersection so the circle
                            // can never "bulge" into a tile without any of the
                            // the tile's corners being inside the circle
                            final int tileX = x, tileY = y;
                            executorService.execute(() -> {
                                if (cancelled.get()) {
                                    // Operation cancelled by user
                                    return;
                                }
                                Tile tile = tileFactory.createTile(tileX, tileY);
                                dimension.addTile(tile);
                                if (org.pepsoft.worldpainter.util.MathUtils.getLargestDistanceFromOrigin(tileX, tileY) >= radius) {
                                    // The tile is not completely inside the circle,
                                    // so use the Void layer to create the shape of
                                    // the edge
                                    for (int xx = 0; xx < TILE_SIZE; xx++) {
                                        for (int yy = 0; yy < TILE_SIZE; yy++) {
                                            float distance = MathUtils.getDistance(tileX * TILE_SIZE + xx + 0.5f, tileY * TILE_SIZE + yy + 0.5f);
                                            if (distance > radius) {
                                                tile.setBitLayerValue(org.pepsoft.worldpainter.layers.Void.INSTANCE, xx, yy, true);
                                            }
                                        }
                                    }
                                }
                                if (progressReceiver != null) {
                                    synchronized (tileCount) {
                                        tileCount[0]++;
                                        try {
                                            System.out.println("Progress: " + tileCount[0] + " of " + approximateTotalTiles + " (" + ((float) tileCount[0] / approximateTotalTiles) + ")");
                                            progressReceiver.setProgress((float) tileCount[0] / approximateTotalTiles);
                                        } catch (ProgressReceiver.OperationCancelled e) {
                                            cancelled.set(true);
                                        }
                                    }
                                }
                            });
                        }
                    }
                }
                
                // Assume the user will want a void border by default; override
                // the preferences
                dimension.setBorder(Border.VOID);
                dimension.setBorderSize(2);
            } else {
                int width = ((Integer) spinnerWidth.getValue()) / 128;
                int height = ((Integer) spinnerLength.getValue()) / 128;
                logger.info("Creating new dimension of size " + width + "x" + height + " for a total of " + width * height + " tiles");
                final int[] tileCount = new int[1];
                final int totalTiles = width * height;
                int startX = -width / 2;
                int startY = -height / 2;
                for (int x = startX; x < startX + width; x++) {
                    for (int y = startY; y < startY + height; y++) {
                        final int tileX = x, tileY = y;
                        executorService.execute(() -> {
                            if (cancelled.get()) {
                                // Operation cancelled by user
                                return;
                            }
                            Tile tile = tileFactory.createTile(tileX, tileY);
                            dimension.addTile(tile);
                            if (progressReceiver != null) {
                                synchronized (tileCount) {
                                    tileCount[0]++;
                                    try {
                                        progressReceiver.setProgress((float) tileCount[0] / totalTiles);
                                    } catch (ProgressReceiver.OperationCancelled e) {
                                        cancelled.set(true);
                                    }
                                }
                            }
                        });
                    }
                }
            }
            
            // Wait for all tiles to be created
            executorService.shutdown();
            try {
                executorService.awaitTermination(1, TimeUnit.DAYS);
            } catch (InterruptedException e) {
                throw new RuntimeException("Thread interrupted", e);
            }
            
            if (cancelled.get()) {
                // The operation was cancelled by the user
                return null;
            }

            if (dim == DIM_NORMAL_CEILING) {
                ResourcesExporter.ResourcesExporterSettings resourcesSettings = new ResourcesExporter.ResourcesExporterSettings(maxHeight);
                // Invert min and max levels:
                int maxZ = maxHeight - 1;
                for (int blockType: resourcesSettings.getBlockTypes()) {
                    int low = resourcesSettings.getMinLevel(blockType);
                    int high = resourcesSettings.getMaxLevel(blockType);
                    resourcesSettings.setMinLevel(blockType, maxZ - high);
                    resourcesSettings.setMaxLevel(blockType, maxZ - low);
                }
                // Remove lava and water:
                resourcesSettings.setChance(BLK_WATER, 0);
                resourcesSettings.setChance(BLK_LAVA, 0);
                dimension.setLayerSettings(Resources.INSTANCE, resourcesSettings);
            } else if (dim == DIM_NETHER) {
                dimension.setSubsurfaceMaterial(NETHERLIKE);

                CavernsSettings cavernsSettings = new CavernsSettings();
                cavernsSettings.setCavernsEverywhereLevel(16);
                cavernsSettings.setSurfaceBreaking(true);
                cavernsSettings.setFloodWithLava(true);
                cavernsSettings.setWaterLevel(16);
                dimension.setLayerSettings(Caverns.INSTANCE, cavernsSettings);
            } else if (dim == DIM_NETHER_CEILING) {
                dimension.setSubsurfaceMaterial(NETHERLIKE);
            } else if ((dim == DIM_END) || (dim == DIM_END_CEILING)) {
                dimension.setSubsurfaceMaterial(END_STONE);
            }

            Configuration config = Configuration.getInstance();
            Dimension defaults = config.getDefaultTerrainAndLayerSettings();
            if (dim == DIM_NORMAL) {
                if (! checkBoxCircular.isSelected()) {
                    dimension.setBorder(defaults.getBorder());
                    dimension.setBorderSize(defaults.getBorderSize());
                }
                dimension.setBedrockWall(defaults.isBedrockWall());
                dimension.setSubsurfaceMaterial(defaults.getSubsurfaceMaterial());
                dimension.setPopulate(defaults.isPopulate());
                dimension.setTopLayerMinDepth(defaults.getTopLayerMinDepth());
                dimension.setTopLayerVariation(defaults.getTopLayerVariation());
                dimension.setBottomless(defaults.isBottomless());
                for (Map.Entry<Layer, ExporterSettings> entry: defaults.getAllLayerSettings().entrySet()) {
                    dimension.setLayerSettings(entry.getKey(), entry.getValue().clone());
                }
            }
            dimension.setBorderLevel(waterHeight);
            
            dimension.setGridEnabled(config.isDefaultGridEnabled());
            dimension.setGridSize(config.getDefaultGridSize());
            dimension.setContoursEnabled(config.isDefaultContoursEnabled());
            dimension.setContourSeparation(config.getDefaultContourSeparation());
        } finally {
            dimension.setEventsInhibited(false);
        }

        dimension.setDirty(false);
        return dimension;
    }

    private void create() {
        cancelled = false;
        dispose();
    }

    private void cancel() {
        dispose();
    }
    
    private void setControlStates() {
        boolean surfaceDimension = dim == DIM_NORMAL;
        boolean minecraft11Only = Integer.parseInt((String) comboBoxMaxHeight.getSelectedItem()) != DEFAULT_MAX_HEIGHT_2;
        checkBoxExtendedBlockIds.setEnabled(! minecraft11Only);
        boolean hilly = radioButtonHilly.isSelected();
        spinnerRange.setEnabled(hilly);
        spinnerScale.setEnabled(hilly);
        spinnerLength.setEnabled((tiles == null) && (! checkBoxCircular.isSelected()));
        boolean seedLocked = tiles != null;
        radioButtonOceanSeed.setEnabled(surfaceDimension && (! seedLocked));
        radioButtonLandSeed.setEnabled(surfaceDimension && (! seedLocked));
        radioButtonCustomSeed.setEnabled(surfaceDimension && (! seedLocked));
        buttonRandomSeed.setEnabled(surfaceDimension && radioButtonCustomSeed.isSelected() && (! seedLocked));
        fieldSeed.setEnabled(surfaceDimension && radioButtonCustomSeed.isSelected() && (! seedLocked));
        boolean advancedTerrain = radioButtonAdvancedTerrain.isSelected();
        comboBoxSurfaceMaterial.setEnabled(! advancedTerrain);
        labelAdvancedTerrain.setEnabled(advancedTerrain);
        labelAdvancedTerrain.setCursor(advancedTerrain ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : null);
    }

    private void updatePreview() {
        long tmpSeed;
        if (radioButtonCustomSeed.isSelected()) {
            try {
                tmpSeed = Long.parseLong(fieldSeed.getText());
            } catch (NumberFormatException e) {
                tmpSeed = fieldSeed.getText().hashCode();
            }
        } else {
            tmpSeed = worldpainterSeed;
        }
        final TileFactory tileFactory = createTileFactory(tmpSeed);
        TileProvider tileProvider = new TileProvider() {
            @Override
            public Rectangle getExtent() {
                return null; // Tile factories are endless
            }
            
            @Override
            public boolean isTilePresent(int x, int y) {
                return true; // Tile factories are endless and have no holes
            }
            
            @Override
            public Tile getTile(int x, int y) {
                Point coords = new Point(x, y);
                synchronized (cache) {
                    Tile tile = cache.get(coords);
                    if (tile == null) {
                        tile = tileFactory.createTile(x, y);
                        cache.put(coords, tile);
                    }
                    return tile;
                }
            }
            
            private final Map<Point, Tile> cache = new HashMap<>();
        };
        Configuration config = Configuration.getInstance();
        tiledImageViewer1.setTileProvider(new WPTileProvider(tileProvider, app.getColourScheme(config.getColourschemeIndex()), autoBiomeScheme, app.getCustomBiomeManager(), Collections.singleton((Layer) Biome.INSTANCE), config.isDefaultContoursEnabled(), config.getDefaultContourSeparation(), config.getDefaultLightOrigin(), false, null));
    }
    
    private TileFactory createTileFactory(long seed) {
        Terrain terrain = (Terrain) comboBoxSurfaceMaterial.getSelectedItem();
        int baseHeight = (Integer) spinnerTerrainLevel.getValue();
        int waterHeight = (Integer) spinnerWaterLevel.getValue();
        float range = ((Number) spinnerRange.getValue()).floatValue();
        double scale = ((Integer) spinnerScale.getValue()) / 100.0;
        boolean floodWithLava = checkBoxLava.isSelected();
        boolean beaches = checkBoxBeaches.isSelected();
        int maxHeight = Integer.parseInt((String) comboBoxMaxHeight.getSelectedItem());
        
        HeightMapTileFactory tileFactory;
        if ("true".equals(System.getProperty("org.pepsoft.worldpainter.fancyworlds"))) {
            tileFactory = TileFactoryFactory.createFancyTileFactory(seed, terrain, maxHeight, baseHeight, waterHeight, floodWithLava, range, scale);
        } else {
    //        HeightMapTileFactory tileFactory = new ExperimentalTileFactory(maxHeight);
            if (radioButtonHilly.isSelected()) {
                tileFactory = TileFactoryFactory.createNoiseTileFactory(seed, terrain, maxHeight, baseHeight, waterHeight, floodWithLava, beaches, range, scale);
            } else {
                tileFactory = TileFactoryFactory.createFlatTileFactory(seed, terrain, maxHeight, baseHeight, waterHeight, floodWithLava, beaches);
            }
            if (radioButtonAdvancedTerrain.isSelected()) {
                tileFactory.setTheme(theme);
            }
            Configuration config = Configuration.getInstance();
            Dimension defaults = config.getDefaultTerrainAndLayerSettings();
            if ((dim == DIM_NORMAL)
                    && (defaults.getTileFactory() instanceof HeightMapTileFactory)
                    && (((HeightMapTileFactory) defaults.getTileFactory()).getTheme() instanceof SimpleTheme)
                    && (((SimpleTheme) ((HeightMapTileFactory) defaults.getTileFactory()).getTheme()).getTerrainRanges() != null)) {
                HeightMapTileFactory defaultTileFactory = (HeightMapTileFactory) defaults.getTileFactory();
                SimpleTheme defaultTheme = (SimpleTheme) defaultTileFactory.getTheme();
                if (radioButtonSimpleTerrain.isSelected()) {
                    SortedMap<Integer, Terrain> terrainRanges = new TreeMap<>(defaultTheme.getTerrainRanges());
                    int surfaceLevel = terrainRanges.headMap(waterHeight + 3).lastKey();
                    terrainRanges.put(surfaceLevel, terrain);
                    SimpleTheme theme = (SimpleTheme) tileFactory.getTheme();
                    theme.setTerrainRanges(terrainRanges);
                    theme.setRandomise(defaultTheme.isRandomise());
                }
            } else if ((dim != DIM_NORMAL) && radioButtonSimpleTerrain.isSelected()) {
                // Override the default terrain map:
                SortedMap<Integer, Terrain> terrainMap = new TreeMap<>();
                terrainMap.put(-1, terrain);
                SimpleTheme theme = (SimpleTheme) tileFactory.getTheme();
                theme.setTerrainRanges(terrainMap);
            }
        }
        
        return tileFactory;
    }

    private void updateWalkingTimes() {
        int width = (Integer) spinnerWidth.getValue();
        if (checkBoxCircular.isSelected()) {
            labelWalkingTimes.setText(blocksToWalkingTime(width));
        } else {
            String westEastTime = blocksToWalkingTime(width);
            int length = (Integer) spinnerLength.getValue();
            String northSouthTime = blocksToWalkingTime(length);
            if (westEastTime.equals(northSouthTime)) {
                labelWalkingTimes.setText(westEastTime);
            } else {
                labelWalkingTimes.setText("West to east: " + westEastTime + ", north to south: " + northSouthTime);
            }
        }
    }
    
    private String blocksToWalkingTime(int blocks) {
        int mins = (int) (blocks / 256f + 0.5f);
        if (mins < 60) {
            return mins + " min.";
        } else {
            int hours = mins / 60;
            mins -= 60 * hours;
            if (mins == 0) {
                return hours + "hr.";
            } else {
                return hours + "hr., " + mins + " min.";
            }
        }
    }

    private void editTheme() {
        EditSimpleThemeDialog dialog = new EditSimpleThemeDialog(this, theme);
        dialog.setVisible(true);
        if (! dialog.isCancelled()) {
            theme = dialog.getTheme();
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
        buttonCancel = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        spinnerWaterLevel = new javax.swing.JSpinner();
        jLabel20 = new javax.swing.JLabel();
        radioButtonFlat = new javax.swing.JRadioButton();
        jLabel5 = new javax.swing.JLabel();
        comboBoxMaxHeight = new javax.swing.JComboBox();
        spinnerLength = new javax.swing.JSpinner();
        jLabel6 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        fieldSeed = new javax.swing.JTextField();
        spinnerRange = new javax.swing.JSpinner();
        checkBoxCircular = new javax.swing.JCheckBox();
        jLabel19 = new javax.swing.JLabel();
        spinnerTerrainLevel = new javax.swing.JSpinner();
        jLabel8 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        checkBoxBeaches = new javax.swing.JCheckBox();
        jLabel2 = new javax.swing.JLabel();
        spinnerScale = new javax.swing.JSpinner();
        jLabel7 = new javax.swing.JLabel();
        radioButtonHilly = new javax.swing.JRadioButton();
        jLabel3 = new javax.swing.JLabel();
        buttonRandomSeed = new javax.swing.JButton();
        comboBoxSurfaceMaterial = new javax.swing.JComboBox();
        jLabel10 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        fieldName = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        checkBoxLava = new javax.swing.JCheckBox();
        jLabel4 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        spinnerWidth = new javax.swing.JSpinner();
        radioButtonOceanSeed = new javax.swing.JRadioButton();
        radioButtonLandSeed = new javax.swing.JRadioButton();
        radioButtonCustomSeed = new javax.swing.JRadioButton();
        labelWarning = new javax.swing.JLabel();
        checkBoxExtendedBlockIds = new javax.swing.JCheckBox();
        jLabel12 = new javax.swing.JLabel();
        labelWalkingTimes = new javax.swing.JLabel();
        radioButtonSimpleTerrain = new javax.swing.JRadioButton();
        radioButtonAdvancedTerrain = new javax.swing.JRadioButton();
        labelAdvancedTerrain = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        tiledImageViewer1 = new org.pepsoft.util.swing.TiledImageViewer();
        buttonCreate = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Create New World");

        buttonCancel.setText("Cancel");
        buttonCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCancelActionPerformed(evt);
            }
        });

        spinnerWaterLevel.setModel(new javax.swing.SpinnerNumberModel(62, 0, 127, 1));
        spinnerWaterLevel.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerWaterLevelStateChanged(evt);
            }
        });

        jLabel20.setText("%");

        buttonGroup1.add(radioButtonFlat);
        radioButtonFlat.setText("Flat");
        radioButtonFlat.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonFlatActionPerformed(evt);
            }
        });

        jLabel5.setText("Level:");

        comboBoxMaxHeight.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "32", "64", "128", "256", "512", "1024", "2048" }));
        comboBoxMaxHeight.setSelectedIndex(3);
        comboBoxMaxHeight.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxMaxHeightActionPerformed(evt);
            }
        });

        spinnerLength.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(640), Integer.valueOf(1), null, Integer.valueOf(128)));
        spinnerLength.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerLengthStateChanged(evt);
            }
        });

        jLabel6.setText("Surface material:");

        jLabel9.setText("Water level:");

        fieldSeed.setText("202961");
        fieldSeed.setEnabled(false);
        fieldSeed.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                fieldSeedFocusLost(evt);
            }
        });

        spinnerRange.setModel(new javax.swing.SpinnerNumberModel(20, 1, 255, 1));
        spinnerRange.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerRangeStateChanged(evt);
            }
        });

        checkBoxCircular.setText("Circular world");
        checkBoxCircular.setToolTipText("<html>This will create a cirular world. The dimension indicates the diameter of the circle, and<br>\nthe origin (0,0) will be the centre. The Void layer will be used to create the circular edge of the world.</html>");
        checkBoxCircular.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxCircularActionPerformed(evt);
            }
        });

        jLabel19.setText("Horizontal hill size:");

        spinnerTerrainLevel.setModel(new javax.swing.SpinnerNumberModel(58, 1, 127, 1));
        spinnerTerrainLevel.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerTerrainLevelStateChanged(evt);
            }
        });

        jLabel8.setText("Name:");

        jLabel11.setText("blocks");

        checkBoxBeaches.setSelected(true);
        checkBoxBeaches.setText("Beaches:");
        checkBoxBeaches.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        checkBoxBeaches.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxBeachesActionPerformed(evt);
            }
        });

        jLabel2.setText("x");

        spinnerScale.setModel(new javax.swing.SpinnerNumberModel(100, 1, 999, 1));
        spinnerScale.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerScaleStateChanged(evt);
            }
        });

        jLabel7.setText("Minecraft seed:");

        buttonGroup1.add(radioButtonHilly);
        radioButtonHilly.setSelected(true);
        radioButtonHilly.setText("Hilly");
        radioButtonHilly.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonHillyActionPerformed(evt);
            }
        });

        jLabel3.setText("blocks (in multiples of 128)");

        buttonRandomSeed.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/arrow_rotate_clockwise.png"))); // NOI18N
        buttonRandomSeed.setToolTipText("Choose a random seed");
        buttonRandomSeed.setEnabled(false);
        buttonRandomSeed.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonRandomSeedActionPerformed(evt);
            }
        });

        comboBoxSurfaceMaterial.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "GRASS" }));
        comboBoxSurfaceMaterial.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxSurfaceMaterialActionPerformed(evt);
            }
        });

        jLabel10.setText("Height:");

        jLabel17.setText("(Minecraft default: 62)");

        fieldName.setText("Generated World");

        jLabel1.setText("Dimensions:");

        checkBoxLava.setText("Lava instead of water:");
        checkBoxLava.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        checkBoxLava.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxLavaActionPerformed(evt);
            }
        });

        jLabel4.setText("Topography:");

        jLabel18.setText("Hill height:");

        spinnerWidth.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(640), Integer.valueOf(1), null, Integer.valueOf(128)));
        spinnerWidth.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerWidthStateChanged(evt);
            }
        });

        buttonGroup2.add(radioButtonOceanSeed);
        radioButtonOceanSeed.setSelected(true);
        radioButtonOceanSeed.setText("Ocean");
        radioButtonOceanSeed.setToolTipText("A seed with a huge ocean around the origin");
        radioButtonOceanSeed.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonOceanSeedActionPerformed(evt);
            }
        });

        buttonGroup2.add(radioButtonLandSeed);
        radioButtonLandSeed.setText("Land");
        radioButtonLandSeed.setToolTipText("A seed with a large continent around the origin");
        radioButtonLandSeed.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonLandSeedActionPerformed(evt);
            }
        });

        buttonGroup2.add(radioButtonCustomSeed);
        radioButtonCustomSeed.setText(" ");
        radioButtonCustomSeed.setToolTipText("Set your own custom Minecraft seed");
        radioButtonCustomSeed.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonCustomSeedActionPerformed(evt);
            }
        });

        labelWarning.setFont(labelWarning.getFont().deriveFont(labelWarning.getFont().getStyle() | java.awt.Font.BOLD));
        labelWarning.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/error.png"))); // NOI18N
        labelWarning.setText("Only Minecraft 1.1, with mods!");

        checkBoxExtendedBlockIds.setText("Extended block IDs:");
        checkBoxExtendedBlockIds.setToolTipText("Wether to support block IDs higher than 255 but lower than 4096, as used by various mods");
        checkBoxExtendedBlockIds.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);

        jLabel12.setText("Edge to edge walking time:");

        labelWalkingTimes.setText("...");

        buttonGroup3.add(radioButtonSimpleTerrain);
        radioButtonSimpleTerrain.setSelected(true);
        radioButtonSimpleTerrain.setText("Simple:");
        radioButtonSimpleTerrain.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonSimpleTerrainActionPerformed(evt);
            }
        });

        buttonGroup3.add(radioButtonAdvancedTerrain);
        radioButtonAdvancedTerrain.setText("Advanced:");
        radioButtonAdvancedTerrain.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonAdvancedTerrainActionPerformed(evt);
            }
        });

        labelAdvancedTerrain.setForeground(new java.awt.Color(0, 51, 255));
        labelAdvancedTerrain.setText("<html><u>configure default terrain and layers</u></html>");
        labelAdvancedTerrain.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        labelAdvancedTerrain.setEnabled(false);
        labelAdvancedTerrain.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                labelAdvancedTerrainMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(fieldName)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addComponent(radioButtonOceanSeed)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(radioButtonLandSeed)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(radioButtonCustomSeed)
                        .addGap(0, 0, 0)
                        .addComponent(fieldSeed)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonRandomSeed))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel8)
                            .addComponent(jLabel1)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(spinnerWidth, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerLength, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, 0)
                                .addComponent(jLabel3))
                            .addComponent(jLabel4)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(jLabel10)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(comboBoxMaxHeight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, 0)
                                .addComponent(jLabel11)
                                .addGap(18, 18, 18)
                                .addComponent(labelWarning))
                            .addComponent(radioButtonFlat)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(jLabel5)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerTerrainLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(checkBoxLava)
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addComponent(jLabel9)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(spinnerWaterLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jLabel17))
                                    .addComponent(checkBoxBeaches)))
                            .addComponent(jLabel7)
                            .addComponent(checkBoxCircular)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(radioButtonHilly)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel18)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerRange, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel19)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerScale, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, 0)
                                .addComponent(jLabel20))
                            .addComponent(radioButtonSimpleTerrain)
                            .addComponent(jLabel6))
                        .addContainerGap())
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(labelWalkingTimes)
                            .addComponent(jLabel12)
                            .addComponent(checkBoxExtendedBlockIds)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(radioButtonAdvancedTerrain)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(comboBoxSurfaceMaterial, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(labelAdvancedTerrain, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addGap(0, 0, Short.MAX_VALUE))))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(jLabel8)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(fieldName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(spinnerWidth, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2)
                    .addComponent(spinnerLength, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(checkBoxCircular)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(comboBoxMaxHeight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel11)
                    .addComponent(labelWarning))
                .addGap(18, 18, 18)
                .addComponent(jLabel12)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(labelWalkingTimes)
                .addGap(18, 18, 18)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(radioButtonHilly)
                    .addComponent(jLabel18)
                    .addComponent(spinnerRange, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel19)
                    .addComponent(spinnerScale, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel20))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(radioButtonFlat)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(spinnerTerrainLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel9)
                    .addComponent(spinnerWaterLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel17))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(checkBoxLava)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(checkBoxBeaches)
                .addGap(18, 18, 18)
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(comboBoxSurfaceMaterial, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(radioButtonSimpleTerrain))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(radioButtonAdvancedTerrain)
                    .addComponent(labelAdvancedTerrain, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(checkBoxExtendedBlockIds)
                .addGap(18, 18, 18)
                .addComponent(jLabel7)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(buttonRandomSeed)
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(radioButtonOceanSeed)
                        .addComponent(radioButtonLandSeed)
                        .addComponent(radioButtonCustomSeed)
                        .addComponent(fieldSeed, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(0, 0, Short.MAX_VALUE))
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        javax.swing.GroupLayout tiledImageViewer1Layout = new javax.swing.GroupLayout(tiledImageViewer1);
        tiledImageViewer1.setLayout(tiledImageViewer1Layout);
        tiledImageViewer1Layout.setHorizontalGroup(
            tiledImageViewer1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 244, Short.MAX_VALUE)
        );
        tiledImageViewer1Layout.setVerticalGroup(
            tiledImageViewer1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(tiledImageViewer1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(tiledImageViewer1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        buttonCreate.setText("Create");
        buttonCreate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCreateActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(buttonCreate)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonCancel))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonCancel)
                    .addComponent(buttonCreate))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void buttonCreateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCreateActionPerformed
        create();
    }//GEN-LAST:event_buttonCreateActionPerformed

    private void buttonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCancelActionPerformed
        cancel();
    }//GEN-LAST:event_buttonCancelActionPerformed

    private void radioButtonFlatActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonFlatActionPerformed
        if (radioButtonFlat.isSelected()) {
            int terrainLevel = (Integer) spinnerTerrainLevel.getValue();
            int waterLevel = (Integer) spinnerWaterLevel.getValue();
            int maxHeight = Integer.parseInt((String) comboBoxMaxHeight.getSelectedItem());
            int minimumSpawnHeight = ((maxHeight == 128) || (maxHeight == 256)) ? 63 : (maxHeight / 2 - 1);
            if ((terrainLevel < (minimumSpawnHeight + 1)) && (waterLevel < minimumSpawnHeight)) {
                savedTerrainLevel = terrainLevel;
                terrainLevel = minimumSpawnHeight + 1; // Add one to avoid beaches everywhere
                spinnerTerrainLevel.setValue(terrainLevel);
            } else {
                savedTerrainLevel = 0;
            }
        }
        setControlStates();
        updatePreview();
    }//GEN-LAST:event_radioButtonFlatActionPerformed

    private void radioButtonHillyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonHillyActionPerformed
        if (savedTerrainLevel != 0) {
            spinnerTerrainLevel.setValue(savedTerrainLevel);
            savedTerrainLevel = 0;
        }
        setControlStates();
        updatePreview();
    }//GEN-LAST:event_radioButtonHillyActionPerformed

    private void comboBoxMaxHeightActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxMaxHeightActionPerformed
        int maxHeight = Integer.parseInt((String) comboBoxMaxHeight.getSelectedItem());
        int exp = (int) (Math.log(maxHeight) / Math.log(2));
        if (exp != previousExp) {
//            double factor = Math.pow(2, exp - previousExp);
            previousExp = exp;
            
            int terrainLevel = (Integer) spinnerTerrainLevel.getValue();
            int waterLevel = (Integer) spinnerWaterLevel.getValue();
            int diff = waterLevel - terrainLevel;
//            int newWaterLevel = (int) (waterLevel * factor);
            int newWaterLevel = (maxHeight == 256) ? 62 : (maxHeight / 2 - 2);
            int newTerrainLevel = newWaterLevel - diff;
            if (newTerrainLevel < 1) {
                newTerrainLevel = 1;
            } else if (newTerrainLevel >= maxHeight) {
                newTerrainLevel = maxHeight - 1;
            }
            if (newWaterLevel < 0) {
                newWaterLevel = 0;
            } else if (newWaterLevel >= maxHeight) {
                newWaterLevel = maxHeight - 1;
            }
            spinnerTerrainLevel.setValue(newTerrainLevel);
            spinnerWaterLevel.setValue(newWaterLevel);
            ((SpinnerNumberModel) spinnerTerrainLevel.getModel()).setMaximum(maxHeight - 1);
            ((SpinnerNumberModel) spinnerWaterLevel.getModel()).setMaximum(maxHeight - 1);
            
            if (exp == 7) {
                labelWarning.setText("Only Minecraft 1.1!");
                labelWarning.setVisible(true);
            } else if (exp != 8) {
                labelWarning.setText("Only Minecraft 1.1, with mods!");
                labelWarning.setVisible(true);
            } else {
                labelWarning.setVisible(false);
            }
            
            if (exp != 8) {
                checkBoxExtendedBlockIds.setSelected(false);
            }
            
            int range = (Integer) spinnerRange.getValue();
            if (range >= maxHeight) {
                spinnerRange.setValue(maxHeight - 1);
            }
            ((SpinnerNumberModel) spinnerRange.getModel()).setMaximum(maxHeight - 1);
            
            setControlStates();
            updatePreview();
        }
    }//GEN-LAST:event_comboBoxMaxHeightActionPerformed

    private void spinnerWidthStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerWidthStateChanged
        int value = (Integer) spinnerWidth.getValue();
        if (! checkBoxCircular.isSelected()) {
            value = Math.round(value / 128f) * 128;
            if (value < 128) {
                value = 128;
            }
            spinnerWidth.setValue(value);
        } else {
            if ((value % 2) != 0) {
                spinnerWidth.setValue(value + 1);
            }
        }
        updateWalkingTimes();
    }//GEN-LAST:event_spinnerWidthStateChanged

    private void spinnerLengthStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerLengthStateChanged
        int value = (Integer) spinnerLength.getValue();
        if (! checkBoxCircular.isSelected()) {
            value = Math.round(value / 128f) * 128;
            if (value < 128) {
                value = 128;
            }
            spinnerLength.setValue(value);
        } else {
            if ((value % 2) != 0) {
                spinnerLength.setValue(value + 1);
            }
        }
        updateWalkingTimes();
    }//GEN-LAST:event_spinnerLengthStateChanged

    private void checkBoxCircularActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxCircularActionPerformed
        if (checkBoxCircular.isSelected()) {
            ((SpinnerNumberModel) spinnerWidth.getModel()).setStepSize(2);
            jLabel3.setText("blocks (even number)");
        } else {
            ((SpinnerNumberModel) spinnerWidth.getModel()).setStepSize(128);
            spinnerWidth.setValue(Math.max(Math.round((Integer) spinnerWidth.getValue() / 128f) * 128, 128));
            jLabel3.setText("blocks (in multiples of 128)");
        }
        setControlStates();
        updateWalkingTimes();
    }//GEN-LAST:event_checkBoxCircularActionPerformed

    private void buttonRandomSeedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonRandomSeedActionPerformed
        fieldSeed.setText(Long.toString(new Random().nextLong()));
        updatePreview();
    }//GEN-LAST:event_buttonRandomSeedActionPerformed

    private void spinnerRangeStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerRangeStateChanged
        updatePreview();
    }//GEN-LAST:event_spinnerRangeStateChanged

    private void spinnerScaleStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerScaleStateChanged
        updatePreview();
    }//GEN-LAST:event_spinnerScaleStateChanged

    private void spinnerTerrainLevelStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerTerrainLevelStateChanged
        updatePreview();
    }//GEN-LAST:event_spinnerTerrainLevelStateChanged

    private void spinnerWaterLevelStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerWaterLevelStateChanged
        updatePreview();
    }//GEN-LAST:event_spinnerWaterLevelStateChanged

    private void checkBoxLavaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxLavaActionPerformed
        updatePreview();
    }//GEN-LAST:event_checkBoxLavaActionPerformed

    private void checkBoxBeachesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxBeachesActionPerformed
        updatePreview();
    }//GEN-LAST:event_checkBoxBeachesActionPerformed

    private void comboBoxSurfaceMaterialActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxSurfaceMaterialActionPerformed
        updatePreview();
    }//GEN-LAST:event_comboBoxSurfaceMaterialActionPerformed

    private void fieldSeedFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_fieldSeedFocusLost
        updatePreview();
    }//GEN-LAST:event_fieldSeedFocusLost

    private void radioButtonOceanSeedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonOceanSeedActionPerformed
        if (radioButtonOceanSeed.isSelected()) {
            fieldSeed.setText(Long.toString(World2.DEFAULT_OCEAN_SEED));
            updatePreview();
            setControlStates();
        }
    }//GEN-LAST:event_radioButtonOceanSeedActionPerformed

    private void radioButtonLandSeedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonLandSeedActionPerformed
        if (radioButtonLandSeed.isSelected()) {
            fieldSeed.setText(Long.toString(World2.DEFAULT_LAND_SEED));
            updatePreview();
            setControlStates();
        }
    }//GEN-LAST:event_radioButtonLandSeedActionPerformed

    private void radioButtonCustomSeedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonCustomSeedActionPerformed
        if (radioButtonCustomSeed.isSelected()) {
            updatePreview();
            setControlStates();
        }
    }//GEN-LAST:event_radioButtonCustomSeedActionPerformed

    private void labelAdvancedTerrainMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_labelAdvancedTerrainMouseClicked
        if (radioButtonAdvancedTerrain.isSelected()) {
            editTheme();
        }
    }//GEN-LAST:event_labelAdvancedTerrainMouseClicked

    private void radioButtonSimpleTerrainActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonSimpleTerrainActionPerformed
        setControlStates();
    }//GEN-LAST:event_radioButtonSimpleTerrainActionPerformed

    private void radioButtonAdvancedTerrainActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonAdvancedTerrainActionPerformed
        setControlStates();
    }//GEN-LAST:event_radioButtonAdvancedTerrainActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonCancel;
    private javax.swing.JButton buttonCreate;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.ButtonGroup buttonGroup3;
    private javax.swing.JButton buttonRandomSeed;
    private javax.swing.JCheckBox checkBoxBeaches;
    private javax.swing.JCheckBox checkBoxCircular;
    private javax.swing.JCheckBox checkBoxExtendedBlockIds;
    private javax.swing.JCheckBox checkBoxLava;
    private javax.swing.JComboBox comboBoxMaxHeight;
    private javax.swing.JComboBox comboBoxSurfaceMaterial;
    private javax.swing.JTextField fieldName;
    private javax.swing.JTextField fieldSeed;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JLabel labelAdvancedTerrain;
    private javax.swing.JLabel labelWalkingTimes;
    private javax.swing.JLabel labelWarning;
    private javax.swing.JRadioButton radioButtonAdvancedTerrain;
    private javax.swing.JRadioButton radioButtonCustomSeed;
    private javax.swing.JRadioButton radioButtonFlat;
    private javax.swing.JRadioButton radioButtonHilly;
    private javax.swing.JRadioButton radioButtonLandSeed;
    private javax.swing.JRadioButton radioButtonOceanSeed;
    private javax.swing.JRadioButton radioButtonSimpleTerrain;
    private javax.swing.JSpinner spinnerLength;
    private javax.swing.JSpinner spinnerRange;
    private javax.swing.JSpinner spinnerScale;
    private javax.swing.JSpinner spinnerTerrainLevel;
    private javax.swing.JSpinner spinnerWaterLevel;
    private javax.swing.JSpinner spinnerWidth;
    private org.pepsoft.util.swing.TiledImageViewer tiledImageViewer1;
    // End of variables declaration//GEN-END:variables

    private final App app;
    private final Set<Point> tiles;
    private final AutoBiomeScheme autoBiomeScheme = new AutoBiomeScheme(null);
    private boolean cancelled = true;
    private int previousExp = 7, dim, savedTerrainLevel;
    private long worldpainterSeed;
    private SimpleTheme theme;

    static final int ESTIMATED_TILE_DATA_SIZE = 81; // in KB
    
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(NewWorldDialog.class);
    private static final long serialVersionUID = 1L;
}