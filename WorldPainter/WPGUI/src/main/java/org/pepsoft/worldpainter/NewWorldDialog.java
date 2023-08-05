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

import org.pepsoft.minecraft.MapGenerator;
import org.pepsoft.minecraft.Material;
import org.pepsoft.minecraft.SeededGenerator;
import org.pepsoft.util.IconUtils;
import org.pepsoft.util.MathUtils;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.util.mdc.MDCThreadPoolExecutor;
import org.pepsoft.worldpainter.Dimension.Anchor;
import org.pepsoft.worldpainter.Dimension.Border;
import org.pepsoft.worldpainter.biomeschemes.Minecraft1_2BiomeScheme;
import org.pepsoft.worldpainter.history.HistoryEntry;
import org.pepsoft.worldpainter.layers.*;
import org.pepsoft.worldpainter.layers.exporters.CavernsExporter.CavernsSettings;
import org.pepsoft.worldpainter.layers.exporters.ExporterSettings;
import org.pepsoft.worldpainter.layers.exporters.ResourcesExporter.ResourcesExporterSettings;
import org.pepsoft.worldpainter.plugins.PlatformManager;
import org.pepsoft.worldpainter.themes.SimpleTheme;
import org.pepsoft.worldpainter.themes.TerrainListCellRenderer;
import org.pepsoft.worldpainter.themes.impl.simple.EditSimpleThemeDialog;

import javax.swing.*;
import javax.swing.JSpinner.DefaultEditor;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.primitives.Ints.asList;
import static org.pepsoft.minecraft.Constants.DEFAULT_MAX_HEIGHT_END;
import static org.pepsoft.minecraft.Constants.DEFAULT_MAX_HEIGHT_NETHER;
import static org.pepsoft.util.swing.MessageUtils.showInfo;
import static org.pepsoft.util.swing.SpinnerUtils.setMaximum;
import static org.pepsoft.util.swing.SpinnerUtils.setMinimum;
import static org.pepsoft.worldpainter.App.INT_NUMBER_FORMAT;
import static org.pepsoft.worldpainter.Constants.*;
import static org.pepsoft.worldpainter.DefaultPlugin.ATTRIBUTE_MC_VERSION;
import static org.pepsoft.worldpainter.DefaultPlugin.JAVA_MCREGION;
import static org.pepsoft.worldpainter.Dimension.Anchor.NORMAL_DETAIL;
import static org.pepsoft.worldpainter.Dimension.Role.DETAIL;
import static org.pepsoft.worldpainter.Dimension.Role.MASTER;
import static org.pepsoft.worldpainter.HeightTransform.IDENTITY;
import static org.pepsoft.worldpainter.Platform.Capability.*;
import static org.pepsoft.worldpainter.Terrain.*;
import static org.pepsoft.worldpainter.util.MinecraftUtil.blocksToWalkingTime;

/**
 *
 * @author pepijn
 */
@SuppressWarnings({"unused", "FieldCanBeLocal"}) // Managed by NetBeans
public class NewWorldDialog extends WorldPainterDialog {
    /** Creates new form NewWorldDialog */
    public NewWorldDialog(App app, ColourScheme colourScheme, String name, long seed, Platform platform, Anchor anchor, int defaultMinHeight, int defaultMaxHeight, Dimension baseDimension) {
        this(app, colourScheme, name, seed, platform, anchor, defaultMinHeight, defaultMaxHeight, baseDimension, null);
    }
    
    /** Creates new form NewWorldDialog */
    public NewWorldDialog(App app, ColourScheme colourScheme, String name, long seed, Platform platform, Anchor anchor, int defaultMinHeight, int defaultMaxHeight, Dimension baseDimension, Set<Point> tiles) {
        super(app);
        this.app = app;
        this.colourScheme = colourScheme;
        this.platform = platform;
        this.anchor = anchor;
        this.tiles = tiles;
        this.baseDimension = baseDimension;
        
        initComponents();

        List<Platform> allPlatforms = PlatformManager.getInstance().getAllPlatforms();
        comboBoxTarget.setModel(new DefaultComboBoxModel(allPlatforms.toArray()));
        comboBoxTarget.setSelectedItem(platform);
        comboBoxTarget.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Platform) {
                    setText(((Platform) value).displayName);
                }
                return this;
            }
        });

        comboBoxSurfaceMaterial.setModel(new DefaultComboBoxModel(Terrain.PICK_LIST));
        comboBoxSurfaceMaterial.setRenderer(new TerrainListCellRenderer(app.getColourScheme()));

        initPlatform();
        comboBoxMaxHeight.setSelectedItem(defaultMaxHeight);
        comboBoxMinHeight.setSelectedItem(defaultMinHeight);
        processBuildLimits(true);

        scale = 1.0f;
        Configuration config = Configuration.getInstance();
        if ((anchor.dim == DIM_NORMAL) && (! anchor.invert)) {
            if (! config.isHilly()) {
                radioButtonFlat.setSelected(true);
                spinnerRange.setEnabled(false);
                spinnerScale.setEnabled(false);
            }
            spinnerWidth.setValue(config.getDefaultWidth() * 128);
            spinnerLength.setValue(config.getDefaultHeight() * 128);
            spinnerTerrainLevel.setValue(config.getLevel());
            spinnerWaterLevel.setValue(config.getWaterLevel());
            spinnerRange.setValue(Math.round(config.getDefaultRange()));
            spinnerScale.setValue((int) Math.round(config.getDefaultScale() * 100));
            checkBoxLava.setSelected(config.isLava());
            comboBoxSurfaceMaterial.setSelectedItem(config.getSurface());
            checkBoxBeaches.setSelected(config.isBeaches());
            checkBoxCircular.setSelected(config.isDefaultCircularWorld());
        }

        // Fix spinner sizes
        ((DefaultEditor) spinnerWidth.getEditor()).getTextField().setColumns(4);
        ((DefaultEditor) spinnerLength.getEditor()).getTextField().setColumns(4);

        if (! anchor.equals(NORMAL_DETAIL)) {
            setTitle("Add " + anchor.getDefaultName());
        }
        if (anchor.role == MASTER) {
            fieldName.setEnabled(false);
            comboBoxTarget.setEnabled(false);
            comboBoxMinHeight.setEnabled(false);
            comboBoxMaxHeight.setEnabled(false);
            scale = 16.0f;
            checkBoxMasterDimension.setSelected(true);
        } else if ((anchor.dim == DIM_NORMAL) && anchor.invert) {
            fieldName.setEnabled(false);
            comboBoxTarget.setEnabled(false);
            comboBoxSurfaceMaterial.setSelectedItem(STONE_MIX);
            spinnerTerrainLevel.setValue(58);
            spinnerWaterLevel.setValue(0);
            checkBoxBeaches.setSelected(false);
            comboBoxMinHeight.setEnabled(false);
            comboBoxMaxHeight.setEnabled(false);
        } else if (anchor.dim == DIM_NETHER) {
            if (! anchor.invert) {
                fieldName.setEnabled(false);
                comboBoxTarget.setEnabled(false);
                comboBoxSurfaceMaterial.setSelectedItem(NETHERLIKE);
                int lavaLevel = defaultMaxHeight * 3 / 4;
                spinnerTerrainLevel.setValue(lavaLevel - 4);
                spinnerWaterLevel.setValue(lavaLevel);
                checkBoxLava.setSelected(true);
            } else {
                fieldName.setEnabled(false);
                comboBoxTarget.setEnabled(false);
                comboBoxSurfaceMaterial.setSelectedItem(NETHERLIKE);
                spinnerTerrainLevel.setValue(58);
                spinnerWaterLevel.setValue(0);
            }
            checkBoxBeaches.setSelected(false);
            comboBoxMinHeight.setEnabled(false);
            comboBoxMaxHeight.setEnabled(false);
        } else if (anchor.dim == DIM_END) {
            if (! anchor.invert) {
                fieldName.setEnabled(false);
                comboBoxTarget.setEnabled(false);
                comboBoxSurfaceMaterial.setSelectedItem(END_STONE);
                spinnerTerrainLevel.setValue(32);
            } else {
                fieldName.setEnabled(false);
                comboBoxTarget.setEnabled(false);
                comboBoxSurfaceMaterial.setSelectedItem(END_STONE);
                spinnerTerrainLevel.setValue(58);
            }
            spinnerWaterLevel.setValue(0);
            checkBoxBeaches.setSelected(false);
            comboBoxMinHeight.setEnabled(false);
            comboBoxMaxHeight.setEnabled(false);
        }
        checkBoxMasterDimension.setEnabled((anchor.role == DETAIL) && (! anchor.invert));
        
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
        updateWalkingTimes();
        
        TileFactory defaultTileFactory = config.getDefaultTerrainAndLayerSettings().getTileFactory();
        if ((defaultTileFactory instanceof HeightMapTileFactory) && (((HeightMapTileFactory) defaultTileFactory).getTheme() instanceof SimpleTheme)) {
            theme = (SimpleTheme) ((HeightMapTileFactory) defaultTileFactory).getTheme().clone();
        } else {
            theme = SimpleTheme.createDefault((Terrain) comboBoxSurfaceMaterial.getSelectedItem(), (Integer) comboBoxMinHeight.getSelectedItem(), (Integer) comboBoxMaxHeight.getSelectedItem(), (Integer) spinnerWaterLevel.getValue());
        }

        scaleToUI();
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
        labelWarning.setIcon(null);
        labelWarning.setText(" ");
        checkBoxExtendedBlockIds.setSelected(config.isDefaultExtendedBlockIds());
        
        rootPane.setDefaultButton(buttonCreate);

        programmaticChange = false;
        setControlStates();
        updatePreview();
    }

    /**
     * Try to guestimate whether there is enough memory to create a world of the
     * configured size. If not, ask the user whether they want to continue at
     * their own risk.
     * 
     * @param parent The parent to use for the dialog, if necessary.
     * @return {@code true} if there is enough memory, or the user
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
        final World2 world = new World2(platform, (Integer) comboBoxMinHeight.getSelectedItem(), (Integer) comboBoxMaxHeight.getSelectedItem());
        final Dimension dimension = getSelectedDimension(world, progressReceiver);
        if (dimension == null) {
            // Operation cancelled by user
            return null;
        }
        world.addHistoryEntry(HistoryEntry.WORLD_CREATED);
        world.setName(name);

        // Export settings
        final Configuration config = Configuration.getInstance();
        world.setCreateGoodiesChest(config.isDefaultCreateGoodiesChest());
        world.setMapFeatures(config.isDefaultMapFeatures());
        world.setGameType(config.getDefaultGameType());
        world.setAllowCheats(config.isDefaultAllowCheats());

        world.addDimension(dimension);
        if ((! platform.capabilities.contains(NAME_BASED)) && (platform != JAVA_MCREGION)) {
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
                if (dimension.getAnchor().dim == DIM_NORMAL) {
                    dimension.setLastViewPosition(world.getSpawnPoint());
                }
            }
        }

        if ("true".equals(System.getProperty("org.pepsoft.worldpainter.fancyworlds"))) {
            world.setMixedMaterial(0, new MixedMaterial("Dirt/Gravel", new MixedMaterial.Row[] {new MixedMaterial.Row(Material.DIRT, 750, 1.0f), new MixedMaterial.Row(Material.GRAVEL, 250, 1.0f)}, Minecraft1_2BiomeScheme.BIOME_PLAINS, null, 1.0f));
            world.setMixedMaterial(1, new MixedMaterial("Stone/Gravel", new MixedMaterial.Row[] {new MixedMaterial.Row(Material.STONE, 750, 1.0f), new MixedMaterial.Row(Material.GRAVEL, 250, 1.0f)}, Minecraft1_2BiomeScheme.BIOME_PLAINS, null, 1.0f));
        }

        if (dimension.getAnchor().role == MASTER) {
            world.addDimension(createDimension(world, new Anchor(anchor.dim, DETAIL, anchor.invert, 0)));
        }

        return world;
    }
    
    public Dimension getSelectedDimension(World2 world, final ProgressReceiver progressReceiver) throws ProgressReceiver.OperationCancelled {
        final Anchor anchor;
        if (checkBoxMasterDimension.isSelected()) {
            anchor = new Anchor(this.anchor.dim, MASTER, this.anchor.invert, 0);
        } else {
            anchor = this.anchor;
        }
        final Dimension dimension = createDimension(world, anchor);
        dimension.setEventsInhibited(true);
        try {
            ExecutorService executorService = MDCThreadPoolExecutor.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            final AtomicBoolean cancelled = new AtomicBoolean();
            final TileFactory tileFactory = dimension.getTileFactory();
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
                        Tile tile = createTile(tileFactory, anchor, tileCoords.x, tileCoords.y);
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
                final int diameter = radius * 2;
                logger.info("Creating new circular dimension with diameter " + diameter + " blocks");
                final int tileRadius = (radius + 127) / 128;
                final int[] tileCount = new int[1];
                final int approximateTotalTiles = (int) Math.ceil(Math.PI * tileRadius * tileRadius);
                for (int x = -tileRadius; x < tileRadius; x++) {
                    for (int y = -tileRadius; y < tileRadius; y++) {
                        if (org.pepsoft.worldpainter.util.MathUtils.getSmallestDistanceFromOrigin(x, y) < radius) {
                            // At least one corner is inside the circle; include the tile. Note that this is always
                            // correct in this case only because the centre of the circle is always at a tile
                            // intersection so the circle can never "bulge" into a tile without any of the the tile's
                            // corners being inside the circle
                            final int tileX = x, tileY = y;
                            executorService.execute(() -> {
                                if (cancelled.get()) {
                                    // Operation cancelled by user
                                    return;
                                }
                                Tile tile = createTile(tileFactory, anchor, tileX, tileY);
                                dimension.addTile(tile);
                                if (org.pepsoft.worldpainter.util.MathUtils.getLargestDistanceFromOrigin(tileX, tileY) >= radius) {
                                    // The tile is not completely inside the circle, so use the Void layer to create the
                                    // shape of the edge
                                    final int tileXOffset = tileX << TILE_SIZE_BITS, tileYOffset = tileY << TILE_SIZE_BITS;
                                    for (int chunkXInTile = 0; chunkXInTile < 8; chunkXInTile++) {
                                        for (int chunkYInTile = 0; chunkYInTile < 8; chunkYInTile++) {
                                            int voidColumnsInChunk = 0;
                                            final int chunkXOffsetInTile = chunkXInTile << 4, chunkYOffsetInTile = chunkYInTile << 4;
                                            for (int xx = 0; xx < 16; xx++) {
                                                for (int yy = 0; yy < 16; yy++) {
                                                    final float distance = MathUtils.getDistance((tileXOffset | chunkXOffsetInTile | xx) + 0.5f, (tileYOffset | chunkYOffsetInTile | yy) + 0.5f);
                                                    if (distance > radius) {
                                                        tile.setBitLayerValue(org.pepsoft.worldpainter.layers.Void.INSTANCE, chunkXOffsetInTile | xx, chunkYOffsetInTile | yy, true);
                                                        voidColumnsInChunk++;
                                                    }
                                                }
                                            }
                                            if (voidColumnsInChunk == 256) {
                                                // Entire chunk is empty
                                                tile.setBitLayerValue(NotPresent.INSTANCE, chunkXOffsetInTile, chunkYOffsetInTile, true);
                                            }
                                        }
                                    }
                                }
                                if (progressReceiver != null) {
                                    synchronized (tileCount) {
                                        tileCount[0]++;
                                        try {
//                                            System.out.println("Progress: " + tileCount[0] + " of " + approximateTotalTiles + " (" + ((float) tileCount[0] / approximateTotalTiles) + ")");
                                            progressReceiver.setProgress(Math.min((float) tileCount[0] / approximateTotalTiles, 1.0f));
                                        } catch (ProgressReceiver.OperationCancelled e) {
                                            cancelled.set(true);
                                        }
                                    }
                                }
                            });
                        }
                    }
                }
                
                // Assume the user will want an endless void border by default;
                // override the preferences
                dimension.setBorder(Border.ENDLESS_VOID);
            } else {
                final int width = ((Integer) spinnerWidth.getValue()) / 128;
                final int height = ((Integer) spinnerLength.getValue()) / 128;
                logger.info("Creating new dimension of size " + width + "x" + height + " for a total of " + width * height + " tiles");
                final int[] tileCount = new int[1];
                final int totalTiles = width * height;
                final int startX = -width / 2;
                final int startY = -height / 2;
                for (int x = startX; x < startX + width; x++) {
                    for (int y = startY; y < startY + height; y++) {
                        final int tileX = x, tileY = y;
                        executorService.execute(() -> {
                            if (cancelled.get()) {
                                // Operation cancelled by user
                                return;
                            }
                            final Tile tile = createTile(tileFactory, anchor, tileX, tileY);
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
        } finally {
            dimension.setEventsInhibited(false);
        }
        return dimension;
    }

    private Tile createTile(TileFactory tileFactory, Anchor anchor, int tileX, int tileY) {
        final Tile tile = tileFactory.createTile(tileX, tileY);
        if ((anchor.role == MASTER) && (baseDimension != null)) {
            for (int xInMasterTile = 0; xInMasterTile < TILE_SIZE; xInMasterTile++) {
                for (int yInMasterTile = 0; yInMasterTile < TILE_SIZE; yInMasterTile++) {
                    final int masterX = (tileX << TILE_SIZE_BITS) | xInMasterTile, masterY = (tileY << TILE_SIZE_BITS) | yInMasterTile;
                    final int detailX = masterX << 4, detailY = masterY << 4;
                    final int detailTileX = detailX >> TILE_SIZE_BITS, detailTileY = detailY >> TILE_SIZE_BITS;
                    final Tile detailTile = baseDimension.getTile(detailTileX, detailTileY);
                    if (detailTile != null) {
                        final int xInDetailTile = detailX & TILE_SIZE_MASK, yInDetailTile = detailY & TILE_SIZE_MASK;
                        if (! detailTile.getBitLayerValue(NotPresent.INSTANCE, xInDetailTile, yInDetailTile)) {
                            // The chunk that corresponds to the block on the master dimension exists in the detail
                            // dimension. Scan it to determine the average terrain height and most prevalent water
                            // height and terrain type. We are doing this just so the master dimension visually
                            // resemble the already existing terrain, so it's not necessary to copy layers, etc.
                            final Map<Integer, Integer> waterHeights = new HashMap<>();
                            final Map<Terrain, Integer> terrainTypes = new HashMap<>();
                            float terrainHeightTotal = 0.0f;
                            for (int xInChunk = 0; xInChunk < 16; xInChunk++) {
                                for (int yInChunk = 0; yInChunk < 16; yInChunk++) {
                                    final int myX = xInDetailTile + xInChunk, myY = yInDetailTile + yInChunk;
                                    terrainHeightTotal += detailTile.getHeight(myX, myY);
                                    final int waterHeight = detailTile.getWaterLevel(myX, myY);
                                    final int waterHeightTotal = waterHeights.computeIfAbsent(waterHeight, k -> 0);
                                    waterHeights.put(waterHeight, waterHeightTotal + 1);
                                    final Terrain terrain = detailTile.getTerrain(myX, myY);
                                    final int terrainTotal = terrainTypes.computeIfAbsent(terrain, k -> 0);
                                    terrainTypes.put(terrain, terrainTotal + 1);
                                }
                            }
                            final float averageTerrainHeight = terrainHeightTotal / 256;
                            int mostPrevalentWaterHeight = Integer.MIN_VALUE, mostPrevalentWaterHeightCount = 0;
                            for (Map.Entry<Integer, Integer> entry: waterHeights.entrySet()) {
                                if (entry.getValue() > mostPrevalentWaterHeightCount) {
                                    mostPrevalentWaterHeight = entry.getKey();
                                    mostPrevalentWaterHeightCount = entry.getValue();
                                }
                            }
                            Terrain mostPrevalentTerrain = null;
                            int mostPrevalentTerrainCount = 0;
                            for (Map.Entry<Terrain, Integer> entry: terrainTypes.entrySet()) {
                                if (entry.getValue() > mostPrevalentTerrainCount) {
                                    mostPrevalentTerrain = entry.getKey();
                                    mostPrevalentTerrainCount = entry.getValue();
                                }
                            }
                            tile.setHeight(xInMasterTile, yInMasterTile, averageTerrainHeight);
                            tile.setWaterLevel(xInMasterTile, yInMasterTile, mostPrevalentWaterHeight);
                            tile.setTerrain(xInMasterTile, yInMasterTile, mostPrevalentTerrain);
                        }
                    }
                }
            }
        }
        return tile;
    }

    private Dimension createDimension(World2 world, Anchor anchor) {
        long minecraftSeed;
        try {
            minecraftSeed = Long.parseLong(fieldSeed.getText());
        } catch (NumberFormatException e) {
            minecraftSeed = fieldSeed.getText().hashCode();
        }
        if (radioButtonCustomSeed.isSelected()) {
            worldpainterSeed = minecraftSeed;
        }
        final int waterHeight = (Integer) spinnerWaterLevel.getValue();

        final TileFactory tileFactory = createTileFactory(worldpainterSeed, anchor.role);

        final Dimension dimension;
        dimension = new Dimension(world, anchor.getDefaultName(), minecraftSeed, tileFactory, anchor);
        final ResourcesExporterSettings resourcesSettings = (ResourcesExporterSettings) dimension.getLayerSettings(Resources.INSTANCE);
        if (anchor.invert) {
            // Ceiling dimension; invert min and max levels:
            final int maxZ = dimension.getMaxHeight() + dimension.getMinHeight() - 1;
            for (Material material: resourcesSettings.getMaterials()) {
                final int oldMinLevel = resourcesSettings.getMinLevel(material);
                resourcesSettings.setMinLevel(material, maxZ - resourcesSettings.getMaxLevel(material));
                resourcesSettings.setMaxLevel(material, maxZ - oldMinLevel);
            }
        }
        if (anchor.dim == DIM_NETHER) {
            if (! anchor.invert) {
                dimension.setSubsurfaceMaterial(NETHERLIKE);

                final CavernsSettings cavernsSettings = new CavernsSettings();
                cavernsSettings.setCavernsEverywhereLevel(16);
                cavernsSettings.setSurfaceBreaking(true);
                cavernsSettings.setFloodWithLava(true);
                cavernsSettings.setWaterLevel(16);
                cavernsSettings.setCaveDecorationSettings(null);
                dimension.setLayerSettings(Caverns.INSTANCE, cavernsSettings);
            } else {
                dimension.setSubsurfaceMaterial(NETHERLIKE);
            }
        } else if (anchor.dim == DIM_END) {
            dimension.setSubsurfaceMaterial(END_STONE);
        }

        final Configuration config = Configuration.getInstance();
        final Dimension defaults = config.getDefaultTerrainAndLayerSettings();
        if ((anchor.dim == DIM_NORMAL) && (! anchor.invert)) {
            if (! checkBoxCircular.isSelected()) {
                dimension.setBorder(defaults.getBorder());
                dimension.setBorderSize(defaults.getBorderSize());
            }
            dimension.setWallType(defaults.getWallType());
            dimension.setRoofType(defaults.getRoofType());
            dimension.setSubsurfaceMaterial(defaults.getSubsurfaceMaterial());
            dimension.setPopulate(defaults.isPopulate());
            dimension.setTopLayerMinDepth(defaults.getTopLayerMinDepth());
            dimension.setTopLayerVariation(defaults.getTopLayerVariation());
            dimension.setBottomless(defaults.isBottomless());
            for (Map.Entry<Layer, ExporterSettings> entry: defaults.getAllLayerSettings().entrySet()) {
                dimension.setLayerSettings(entry.getKey(), entry.getValue().clone());
            }
            MapGenerator generator = config.getDefaultGenerator();
            if (generator instanceof SeededGenerator) {
                ((SeededGenerator) generator).setSeed(dimension.getMinecraftSeed());
            }
            dimension.setGenerator(generator);
        }
        if (anchor.role == MASTER) {
            dimension.setScale(scale);
        }
        if (baseDimension != null) {
            resourcesSettings.setMinimumLevel(((ResourcesExporterSettings) baseDimension.getLayerSettings(Resources.INSTANCE)).getMinimumLevel());
        } else {
            resourcesSettings.setMinimumLevel(config.getDefaultResourcesMinimumLevel());
        }
        dimension.setBorderLevel(waterHeight);
        dimension.setCoverSteepTerrain(defaults.isCoverSteepTerrain());

        dimension.setGridEnabled(config.isDefaultGridEnabled());
        dimension.setGridSize(config.getDefaultGridSize());
        dimension.setContoursEnabled(config.isDefaultContoursEnabled());
        dimension.setContourSeparation(config.getDefaultContourSeparation());

        dimension.setExportSettings(config.getDefaultExportSettings());

        return dimension;
    }

    private void setControlStates() {
        boolean surfaceDimension = (anchor.dim == DIM_NORMAL) && (! anchor.invert);
        checkBoxExtendedBlockIds.setEnabled(platform.capabilities.contains(BLOCK_BASED) && (! platform.capabilities.contains(NAME_BASED)) && (platform != JAVA_MCREGION));
        boolean hilly = radioButtonHilly.isSelected();
        spinnerRange.setEnabled(hilly);
        spinnerScale.setEnabled(hilly);
        spinnerLength.setEnabled((tiles == null) && (! checkBoxCircular.isSelected()));
        boolean seedLocked = (tiles != null) || (! platform.capabilities.contains(SEED)) || anchor.invert || (anchor.role != DETAIL);
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
        if (programmaticChange) {
            return;
        }

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
        final TileFactory tileFactory = createTileFactory(tmpSeed, DETAIL);
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
        tiledImageViewer1.setTileProvider(new WPTileProvider(tileProvider, colourScheme, app.getCustomBiomeManager(), Collections.singleton(Biome.INSTANCE), config.isDefaultContoursEnabled(), config.getDefaultContourSeparation(), config.getDefaultLightOrigin(), null));
    }
    
    private TileFactory createTileFactory(long seed, Dimension.Role role) {
        final Terrain terrain = (Terrain) comboBoxSurfaceMaterial.getSelectedItem();
        final int baseHeight = (Integer) spinnerTerrainLevel.getValue();
        final int waterHeight = (Integer) spinnerWaterLevel.getValue();
        final float range = ((Number) spinnerRange.getValue()).floatValue();
        final double scale = ((Integer) spinnerScale.getValue()) / 100.0;
        final boolean floodWithLava = checkBoxLava.isSelected();
        final boolean beaches = checkBoxBeaches.isSelected();
        final int minHeight = (Integer) comboBoxMinHeight.getSelectedItem();
        final int maxHeight = (Integer) comboBoxMaxHeight.getSelectedItem();

        final HeightMapTileFactory tileFactory;
        final double additionalScale = (role == MASTER) ? 16.0 : 1.0;
        if ("true".equals(System.getProperty("org.pepsoft.worldpainter.fancyworlds"))) {
            tileFactory = TileFactoryFactory.createFancyTileFactory(seed, terrain, minHeight, maxHeight, baseHeight, waterHeight, floodWithLava, range, scale / additionalScale);
        } else {
    //        HeightMapTileFactory tileFactory = new ExperimentalTileFactory(maxHeight);
            if (radioButtonHilly.isSelected()) {
                tileFactory = TileFactoryFactory.createNoiseTileFactory(seed, terrain, minHeight, maxHeight, baseHeight, waterHeight, floodWithLava, beaches, range, scale / additionalScale);
            } else {
                tileFactory = TileFactoryFactory.createFlatTileFactory(seed, terrain, minHeight, maxHeight, baseHeight, waterHeight, floodWithLava, beaches);
            }
            if (radioButtonAdvancedTerrain.isSelected()) {
                theme.setWaterHeight((Integer) spinnerWaterLevel.getValue());
                theme.setBeaches(checkBoxBeaches.isSelected());
                tileFactory.setTheme(theme);
            }
            Configuration config = Configuration.getInstance();
            Dimension defaults = config.getDefaultTerrainAndLayerSettings();
            if ((anchor.dim == DIM_NORMAL) && (! anchor.invert)
                    && (defaults.getTileFactory() instanceof HeightMapTileFactory)
                    && (((HeightMapTileFactory) defaults.getTileFactory()).getTheme() instanceof SimpleTheme)
                    && (((SimpleTheme) ((HeightMapTileFactory) defaults.getTileFactory()).getTheme()).getTerrainRanges() != null)
                    && radioButtonSimpleTerrain.isSelected()) {
                final HeightMapTileFactory defaultTileFactory = (HeightMapTileFactory) defaults.getTileFactory();
                final SimpleTheme defaultTheme = (SimpleTheme) defaultTileFactory.getTheme().clone();
                defaultTheme.setMinMaxHeight(minHeight, maxHeight, IDENTITY);
                final SortedMap<Integer, Terrain> terrainRanges = new TreeMap<>(defaultTheme.getTerrainRanges());
                final int surfaceLevel = terrainRanges.headMap(waterHeight + 3).lastKey();
                terrainRanges.put(surfaceLevel, terrain);
                final SimpleTheme theme = (SimpleTheme) tileFactory.getTheme();
                theme.setTerrainRanges(terrainRanges);
                theme.setRandomise(defaultTheme.isRandomise());
            } else if (((anchor.dim != DIM_NORMAL) || anchor.invert) && radioButtonSimpleTerrain.isSelected()) {
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
        final int width = Math.round((int) spinnerWidth.getValue() * scale);
        final int length = Math.round((int) spinnerLength.getValue() * scale);
        if (checkBoxCircular.isSelected()) {
            labelWalkingTimes.setText(blocksToWalkingTime(width));
        } else {
            String westEastTime = blocksToWalkingTime(width);
            String northSouthTime = blocksToWalkingTime(length);
            if (westEastTime.equals(northSouthTime)) {
                labelWalkingTimes.setText(westEastTime);
            } else {
                labelWalkingTimes.setText("West to east: " + westEastTime + ", north to south: " + northSouthTime);
            }
        }
        if (scale == 1.0f) {
            labelScaledDimensions.setText(" ");
        } else if (checkBoxCircular.isSelected()) {
            labelScaledDimensions.setText("(Scaled: " + INT_NUMBER_FORMAT.format(width) + ")");
        } else {
            labelScaledDimensions.setText("(Scaled: " + INT_NUMBER_FORMAT.format(width) + " x " + INT_NUMBER_FORMAT.format(length) + ")");
        }
    }
    
    private void editTheme() {
        theme.setWaterHeight((Integer) spinnerWaterLevel.getValue());
        theme.setBeaches(checkBoxBeaches.isSelected());
        EditSimpleThemeDialog dialog = new EditSimpleThemeDialog(this, theme);
        dialog.setVisible(true);
        if (! dialog.isCancelled()) {
            theme = dialog.getTheme();
            spinnerWaterLevel.setValue(theme.getWaterHeight());
            checkBoxBeaches.setSelected(theme.isBeaches());
            setControlStates();
            updatePreview();
        }
    }

    private void initPlatform() {
        final int maxWidth = (int) Math.min((((long) platform.maxX - platform.minX) / TILE_SIZE) * TILE_SIZE, Integer.MAX_VALUE);
        final int maxLength = (int) Math.min((((long) platform.maxY - platform.minY) / TILE_SIZE) * TILE_SIZE, Integer.MAX_VALUE);
        SpinnerNumberModel model = (SpinnerNumberModel) spinnerWidth.getModel();
        model.setMaximum(maxWidth);
        if ((Integer) model.getValue() > maxWidth) {
            model.setValue(maxWidth);
        }
        model = (SpinnerNumberModel) spinnerLength.getModel();
        model.setMaximum(maxLength);
        if ((Integer) model.getValue() > maxLength) {
            model.setValue(maxLength);
        }
        updateWalkingTimes();

        final int minMinHeight, maxMaxHeight;
        switch (anchor.dim) {
            case DIM_NETHER:
                minMinHeight = Math.max(platform.minMinHeight, 0);
                maxMaxHeight = Math.min(platform.maxMaxHeight, DEFAULT_MAX_HEIGHT_NETHER);
                break;
            case DIM_END:
                minMinHeight = Math.max(platform.minMinHeight, 0);
                maxMaxHeight = Math.min(platform.maxMaxHeight, DEFAULT_MAX_HEIGHT_END);
                break;
            default:
                minMinHeight = platform.minMinHeight;
                maxMaxHeight = platform.maxMaxHeight;
                break;
        }
        final List<Integer> maxHeights = new ArrayList<>(asList(platform.maxHeights));
        maxHeights.removeIf(height -> height.compareTo(maxMaxHeight) > 0);
        comboBoxMaxHeight.setModel(new DefaultComboBoxModel<>(maxHeights.toArray(new Integer[maxHeights.size()])));
        comboBoxMaxHeight.setEnabled((anchor.role == DETAIL) && (! anchor.invert) && (maxHeights.size() > 1));
        final List<Integer> minHeights = new ArrayList<>(asList(platform.minHeights));
        minHeights.removeIf(height -> height.compareTo(minMinHeight) < 0);
        comboBoxMinHeight.setModel(new DefaultComboBoxModel<>(minHeights.toArray(new Integer[minHeights.size()])));
        comboBoxMinHeight.setEnabled((anchor.role == DETAIL) && (! anchor.invert) && (minHeights.size() > 1));
    }

    private void setPlatform(Platform platform) {
        if (programmaticChange) {
            return;
        }

        final Platform previousPlatform = this.platform;
        final Integer previousMinHeight = (Integer) comboBoxMinHeight.getSelectedItem(), previousMaxHeight = (Integer) comboBoxMaxHeight.getSelectedItem();
        this.platform = platform;
        initPlatform();

        final int defaultMaxHeight, previousDefaultMaxHeight, defaultMinHeight, previousDefaultMinHeight;
        switch (anchor.dim) {
            case DIM_NETHER:
                defaultMinHeight = previousDefaultMinHeight = 0;
                defaultMaxHeight = previousDefaultMaxHeight = DEFAULT_MAX_HEIGHT_NETHER;
                break;
            case DIM_END:
                defaultMinHeight = previousDefaultMinHeight = 0;
                defaultMaxHeight = previousDefaultMaxHeight = DEFAULT_MAX_HEIGHT_END;
                break;
            default:
                defaultMinHeight = platform.minZ;
                defaultMaxHeight = platform.standardMaxHeight;
                previousDefaultMinHeight = (previousPlatform != null) ? previousPlatform.minZ : Integer.MIN_VALUE;
                previousDefaultMaxHeight = (previousPlatform != null) ? previousPlatform.standardMaxHeight : Integer.MIN_VALUE;
                break;
        }
        final boolean maxHeightAtDefault = (previousMaxHeight == null) || (previousMaxHeight == previousDefaultMaxHeight);
        if (maxHeightAtDefault) {
            comboBoxMaxHeight.setSelectedItem(defaultMaxHeight);
        } else if (previousMaxHeight < platform.minMaxHeight){
            comboBoxMaxHeight.setSelectedItem(platform.minMaxHeight);
        } else if (previousMaxHeight > platform.maxMaxHeight) {
            comboBoxMaxHeight.setSelectedItem(platform.maxMaxHeight);
        } else {
            comboBoxMaxHeight.setSelectedItem(previousMaxHeight);
        }
        final boolean minHeightAtDefault = (previousMinHeight == null) || (previousMinHeight == previousDefaultMinHeight);
        if (minHeightAtDefault) {
            comboBoxMinHeight.setSelectedItem(defaultMinHeight);
        } else if (previousMinHeight < platform.minMinHeight){
            comboBoxMinHeight.setSelectedItem(platform.minMinHeight);
        } else if (previousMinHeight > platform.maxMinHeight) {
            comboBoxMinHeight.setSelectedItem(platform.maxMinHeight);
        } else {
            comboBoxMinHeight.setSelectedItem(previousMinHeight);
        }
        processBuildLimits(true);

        setControlStates();
    }

    private void processBuildLimits(boolean force) {
        final int minHeight = (Integer) comboBoxMinHeight.getSelectedItem(), maxHeight = (Integer) comboBoxMaxHeight.getSelectedItem();
        if (force || (minHeight != previousMinHeight) || (maxHeight != previousMaxHeight)) {
            previousMinHeight = minHeight;
            previousMaxHeight = maxHeight;

            int terrainLevel = (Integer) spinnerTerrainLevel.getValue();
            int waterLevel = (Integer) spinnerWaterLevel.getValue();
            int newWaterLevel = Math.min(waterLevel, maxHeight - 1);
            int newTerrainLevel = Math.min(terrainLevel, maxHeight - 1);
            spinnerTerrainLevel.setValue(newTerrainLevel);
            spinnerWaterLevel.setValue(newWaterLevel);
            setMinimum(spinnerTerrainLevel, minHeight);
            setMaximum(spinnerTerrainLevel, maxHeight - 1);
            setMinimum(spinnerWaterLevel, minHeight);
            setMaximum(spinnerWaterLevel, maxHeight - 1);

            if ((platform == JAVA_MCREGION) && (maxHeight != 128)) {
                labelWarning.setIcon(ICON_WARNING);
                labelWarning.setText("Only with mods!");
            } else if ((platform.getAttribute(ATTRIBUTE_MC_VERSION).isAtLeast(V_1_17)) && ((maxHeight - minHeight) > 384)) {
                labelWarning.setIcon(ICON_WARNING);
                labelWarning.setText("May impact performance");
            } else {
                labelWarning.setIcon(null);
                labelWarning.setText(" ");
            }

            int range = (Integer) spinnerRange.getValue();
            if (range >= maxHeight - minHeight) {
                spinnerRange.setValue(maxHeight - 1);
            }
            ((SpinnerNumberModel) spinnerRange.getModel()).setMaximum(maxHeight - 1 - minHeight);

            setControlStates();
            updatePreview();
        }
    }

    public static void showMasterDimensionInfo(Window parent) {
        showInfo(parent, "<html>" +
                "<h1>Master Dimensions</h1>" +
                "<p>A Master Dimension is a WorldPainter dimension type (similar to a Ceiling Dimension). It is Exported<br>" +
                "at 1:16 scale wherever the regular Surface Dimension does not exist. Because the Master Dimension is<br>" +
                "only scaled at Export, operations such as saving, loading and editing it are <em>much</em> faster than<br>" +
                "creating a regular Surface Dimension of that size. Note that exporting it takes just as long though." +
                "<p><p>Use a Master Dimension to quickly create large featureless areas of your world (oceans, continents,<br>" +
                "deserts, etc.). When you add tiles to the surface Dimension, they will be initialised to the contents<br>" +
                "of the Master Dimension in that location. This allows you to quickly sketch out the rough shapes of<br>" +
                "your world in the Master Dimension, and then fill in the details where necessary in the surface Dimension." +
                "<p><p>Master Dimensions can use all the features of regular surface Dimensions (terrain types, layers, etc.)<br>" +
                "so large forests, cave systems, etc. are also easily created with it." +
                "</html>", "What Is A Master Dimension");
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        buttonGroup2 = new javax.swing.ButtonGroup();
        buttonGroup3 = new javax.swing.ButtonGroup();
        jCheckBox1 = new javax.swing.JCheckBox();
        jLabel14 = new javax.swing.JLabel();
        buttonCancel = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        spinnerWaterLevel = new javax.swing.JSpinner();
        jLabel20 = new javax.swing.JLabel();
        radioButtonFlat = new javax.swing.JRadioButton();
        jLabel5 = new javax.swing.JLabel();
        comboBoxMaxHeight = new javax.swing.JComboBox<>();
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
        jLabel13 = new javax.swing.JLabel();
        comboBoxTarget = new javax.swing.JComboBox<>();
        checkBoxMasterDimension = new javax.swing.JCheckBox();
        buttonMasterInfo = new javax.swing.JButton();
        labelScaledDimensions = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        comboBoxMinHeight = new javax.swing.JComboBox<>();
        jLabel16 = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        tiledImageViewer1 = new org.pepsoft.util.swing.TiledImageViewer();
        buttonCreate = new javax.swing.JButton();

        jCheckBox1.setText("jCheckBox1");

        jLabel14.setText("jLabel14");

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

        comboBoxMaxHeight.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxMaxHeightActionPerformed(evt);
            }
        });

        spinnerLength.setModel(new javax.swing.SpinnerNumberModel(640, 1, null, 128));
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

        comboBoxSurfaceMaterial.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxSurfaceMaterialActionPerformed(evt);
            }
        });

        jLabel10.setText(", upper:");

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

        spinnerWidth.setModel(new javax.swing.SpinnerNumberModel(640, 1, null, 128));
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
        labelWarning.setText("Only with mods!");

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

        jLabel13.setText("Map format:");

        comboBoxTarget.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxTargetActionPerformed(evt);
            }
        });

        checkBoxMasterDimension.setText("create as master dimension (1:16 scale)");
        checkBoxMasterDimension.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxMasterDimensionActionPerformed(evt);
            }
        });

        buttonMasterInfo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/information.png"))); // NOI18N
        buttonMasterInfo.setMargin(new java.awt.Insets(0, 0, 0, 0));
        buttonMasterInfo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonMasterInfoActionPerformed(evt);
            }
        });

        labelScaledDimensions.setText("(Scaled: 99999 x 99999)");

        jLabel15.setText("Build limits:");

        comboBoxMinHeight.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxMinHeightActionPerformed(evt);
            }
        });

        jLabel16.setText("lower:");

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
                            .addComponent(jLabel6)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(jLabel15)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(labelWarning)
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addComponent(jLabel16)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(comboBoxMinHeight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(0, 0, 0)
                                        .addComponent(jLabel10)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(comboBoxMaxHeight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(0, 0, 0)
                                        .addComponent(jLabel11)))))
                        .addContainerGap(14, Short.MAX_VALUE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(labelScaledDimensions)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(checkBoxMasterDimension)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(buttonMasterInfo))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(jLabel13)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(comboBoxTarget, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
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
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel13)
                    .addComponent(comboBoxTarget, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(checkBoxMasterDimension)
                    .addComponent(buttonMasterInfo))
                .addGap(18, 18, 18)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(spinnerWidth, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2)
                    .addComponent(spinnerLength, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(labelScaledDimensions)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(checkBoxCircular)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(comboBoxMaxHeight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel11)
                    .addComponent(jLabel15)
                    .addComponent(comboBoxMinHeight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel16))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(labelWarning)
                .addGap(6, 6, 6)
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
            .addGap(0, 242, Short.MAX_VALUE)
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
        ok();
    }//GEN-LAST:event_buttonCreateActionPerformed

    private void buttonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCancelActionPerformed
        cancel();
    }//GEN-LAST:event_buttonCancelActionPerformed

    private void radioButtonFlatActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonFlatActionPerformed
        if (radioButtonFlat.isSelected()) {
            int terrainLevel = (Integer) spinnerTerrainLevel.getValue();
            int waterLevel = (Integer) spinnerWaterLevel.getValue();
            int maxHeight = (Integer) comboBoxMaxHeight.getSelectedItem();
            int minimumSpawnHeight = ((maxHeight == 128) || (maxHeight == 256) || (maxHeight == 320)) ? 63 : (maxHeight / 2 - 1);
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
        processBuildLimits(false);
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
        updatePreview();
    }//GEN-LAST:event_radioButtonSimpleTerrainActionPerformed

    private void radioButtonAdvancedTerrainActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonAdvancedTerrainActionPerformed
        setControlStates();
        updatePreview();
    }//GEN-LAST:event_radioButtonAdvancedTerrainActionPerformed

    private void comboBoxTargetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxTargetActionPerformed
        setPlatform((Platform) comboBoxTarget.getSelectedItem());
    }//GEN-LAST:event_comboBoxTargetActionPerformed

    private void buttonMasterInfoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonMasterInfoActionPerformed
        showMasterDimensionInfo(this);
    }//GEN-LAST:event_buttonMasterInfoActionPerformed

    private void checkBoxMasterDimensionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxMasterDimensionActionPerformed
        if (checkBoxMasterDimension.isSelected()) {
            scale = 16.0f;
        } else {
            scale = 1.0f;
        }
        updateWalkingTimes();
        updatePreview();
    }//GEN-LAST:event_checkBoxMasterDimensionActionPerformed

    private void comboBoxMinHeightActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxMinHeightActionPerformed
        processBuildLimits(false);
    }//GEN-LAST:event_comboBoxMinHeightActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonCancel;
    private javax.swing.JButton buttonCreate;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.ButtonGroup buttonGroup3;
    private javax.swing.JButton buttonMasterInfo;
    private javax.swing.JButton buttonRandomSeed;
    private javax.swing.JCheckBox checkBoxBeaches;
    private javax.swing.JCheckBox checkBoxCircular;
    private javax.swing.JCheckBox checkBoxExtendedBlockIds;
    private javax.swing.JCheckBox checkBoxLava;
    private javax.swing.JCheckBox checkBoxMasterDimension;
    private javax.swing.JComboBox<Integer> comboBoxMaxHeight;
    private javax.swing.JComboBox<Integer> comboBoxMinHeight;
    private javax.swing.JComboBox comboBoxSurfaceMaterial;
    private javax.swing.JComboBox<Platform> comboBoxTarget;
    private javax.swing.JTextField fieldName;
    private javax.swing.JTextField fieldSeed;
    private javax.swing.JCheckBox jCheckBox1;
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
    private javax.swing.JLabel labelScaledDimensions;
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
    private final ColourScheme colourScheme;
    private final Dimension baseDimension;
    private final Anchor anchor;
    private Platform platform;
    private int previousMinHeight = Integer.MIN_VALUE, previousMaxHeight = Integer.MIN_VALUE, savedTerrainLevel;
    private long worldpainterSeed;
    private SimpleTheme theme;
    private boolean programmaticChange = true;
    private float scale;

    static final int ESTIMATED_TILE_DATA_SIZE = 81; // in KB

    private static final Icon ICON_WARNING = IconUtils.loadScaledIcon("/org/pepsoft/worldpainter/icons/error.png");

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(NewWorldDialog.class);
    private static final long serialVersionUID = 1L;
}