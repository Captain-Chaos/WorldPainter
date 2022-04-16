/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.tools;

import org.pepsoft.minecraft.Constants;
import org.pepsoft.minecraft.Java117Level;
import org.pepsoft.minecraft.SeededGenerator;
import org.pepsoft.util.DesktopUtils;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.util.ProgressReceiver.OperationCancelled;
import org.pepsoft.util.swing.ProgressDialog;
import org.pepsoft.util.swing.ProgressTask;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.biomeschemes.*;
import org.pepsoft.worldpainter.util.MinecraftUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.pepsoft.util.GUIUtils.scaleToUI;
import static org.pepsoft.worldpainter.Constants.*;
import static org.pepsoft.worldpainter.Generator.DEFAULT;
import static org.pepsoft.worldpainter.Generator.LARGE_BIOMES;

/**
 *
 * @author pepijn
 */
public class BiomesViewerFrame extends JFrame {
    public BiomesViewerFrame(long seed, int preferredBiomeAlgorithm, ColourScheme colourScheme, SeedListener seedListener) throws HeadlessException {
        this(seed, null, preferredBiomeAlgorithm, colourScheme, seedListener);
    }
    
    public BiomesViewerFrame(long seed, final Point marker, int preferredBiomeAlgorithm, ColourScheme colourScheme, SeedListener seedListener) throws HeadlessException {
        super("WorldPainter - Biomes Viewer");
        this.colourScheme = colourScheme;
        this.seedListener = seedListener;
        standAloneMode = App.getInstanceIfExists() == null;
        imageViewer = new BiomesViewer(standAloneMode, true);
        if (marker != null) {
            imageViewer.setMarkerCoords(marker);
            imageViewer.moveToMarker();
        }
        imageViewer.addMouseWheelListener(e -> {
            int rotation = e.getWheelRotation();
            int zoom = imageViewer.getZoom();
            if (rotation < 0) {
                zoom = Math.min(zoom + -rotation, 0);
            } else {
                zoom = Math.max(zoom - rotation, -4);
            }
            imageViewer.setZoom(zoom);
        });
        
        if (! standAloneMode) {
            Controller controller = new Controller();
            imageViewer.addMouseListener(controller);
            imageViewer.addMouseMotionListener(controller);
        }
        
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        getContentPane().add(imageViewer, BorderLayout.CENTER);

        JToolBar toolBar = new JToolBar();
        toolBar.add(new JLabel("Biome scheme:"));
        List<Integer> availableAlgorithms = BiomeSchemeManager.getAvailableBiomeAlgorithms();
        //noinspection unchecked // NetBeans visual designer
        schemeChooser = new JComboBox(availableAlgorithms.toArray());
        schemeChooser.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Integer) {
                    switch ((Integer) value) {
                        case BIOME_ALGORITHM_1_1:
                            setText("Minecraft 1.1");
                            break;
                        case BIOME_ALGORITHM_1_2_AND_1_3_DEFAULT:
                            setText("Minecraft 1.2 - 1.6 Default");
                            break;
                        case BIOME_ALGORITHM_1_3_LARGE:
                            setText("Minecraft 1.3 - 1.6 Large");
                            break;
                        case BIOME_ALGORITHM_1_7_DEFAULT:
                            setText("Minecraft 1.7 - 1.12 Default");
                            break;
                        case BIOME_ALGORITHM_1_7_LARGE:
                            setText("Minecraft 1.7 - 1.12 Large");
                            break;
                    }
                }
                return this;
            }
        });
        seedSpinner = new JSpinner(new SpinnerNumberModel(Long.valueOf(seed), Long.valueOf(Long.MIN_VALUE), Long.valueOf(Long.MAX_VALUE), Long.valueOf(1L)));
        if (availableAlgorithms.contains(preferredBiomeAlgorithm)) {
            schemeChooser.setSelectedItem(preferredBiomeAlgorithm);
        } else {
            schemeChooser.setSelectedIndex(0);
        }
        BiomeScheme biomeScheme = BiomeSchemeManager.getNewBiomeScheme((Integer) schemeChooser.getSelectedItem());
        if (biomeScheme != null) {
            this.biomeScheme = biomeScheme;
            this.biomeScheme.setSeed(seed);
            imageViewer.setTileProvider(new BiomesTileProvider(BiomesViewerFrame.this.biomeScheme, BiomesViewerFrame.this.colourScheme, imageViewer.getZoom(), false));
        }
        schemeChooser.addItemListener(e -> {
            BiomeScheme biomeScheme1 = BiomeSchemeManager.getNewBiomeScheme((Integer) schemeChooser.getSelectedItem());
            if (biomeScheme1 != null) {
                BiomesViewerFrame.this.biomeScheme = biomeScheme1;
                BiomesViewerFrame.this.biomeScheme.setSeed(((Number) seedSpinner.getValue()).longValue());
                imageViewer.setTileProvider(new BiomesTileProvider(BiomesViewerFrame.this.biomeScheme, BiomesViewerFrame.this.colourScheme, imageViewer.getZoom(), false));
            }
        });
        toolBar.add(schemeChooser);
        toolBar.add(Box.createHorizontalStrut(5));
        toolBar.add(new JLabel("Seed:"));
        seedSpinner.setEditor(new JSpinner.NumberEditor(seedSpinner, "0"));
        seedSpinner.addChangeListener(e -> {
            BiomesViewerFrame.this.biomeScheme.setSeed(((Number) seedSpinner.getValue()).longValue());
            imageViewer.setTileProvider(new BiomesTileProvider(BiomesViewerFrame.this.biomeScheme, BiomesViewerFrame.this.colourScheme, imageViewer.getZoom(), false));
        });
        toolBar.add(seedSpinner);
        toolBar.add(Box.createHorizontalGlue());
        getContentPane().add(toolBar, BorderLayout.NORTH);
        
        toolBar = new JToolBar();
        JButton button = new JButton("-");
        button.addActionListener(e -> {
            int zoom = imageViewer.getZoom();
            zoom = Math.max(zoom - 1, -4);
            imageViewer.setZoom(zoom);
        });
        toolBar.add(button);
        
        button = new JButton("+");
        button.addActionListener(e -> {
            int zoom = imageViewer.getZoom();
            zoom = Math.min(zoom + 1, 0);
            imageViewer.setZoom(zoom);
        });
        toolBar.add(button);
        
        toolBar.add(Box.createHorizontalStrut(5));
        createWorldButton = new JButton("Create world");
        if (! standAloneMode) {
            createWorldButton.setToolTipText("Create a new WorldPainter world from the selected tiles");
            createWorldButton.addActionListener(e -> createWorld());
        }
        createWorldButton.setEnabled(false);
        toolBar.add(createWorldButton);
        
        toolBar.add(Box.createHorizontalStrut(5));
        button = new JButton("Reset");
        button.addActionListener(e -> {
            imageViewer.setZoom(-2);
            if (marker != null) {
                imageViewer.moveToMarker();
            } else {
                imageViewer.moveToOrigin();
            }
        });
        toolBar.add(button);
        
        if (seedListener != null) {
            toolBar.add(Box.createHorizontalStrut(5));
            button = new JButton("Copy seed to world");
            button.setToolTipText("Copy the current seed to the world currently being edited");
            button.addActionListener(e -> BiomesViewerFrame.this.seedListener.setSeed(((Number) seedSpinner.getValue()).longValue(), ((schemeChooser.getSelectedIndex() == 1) || (schemeChooser.getSelectedIndex() == 3)) ? Generator.LARGE_BIOMES : Generator.DEFAULT));
            toolBar.add(button);
        }
        
        toolBar.add(Box.createHorizontalStrut(5));
        button = new JButton("Play here in Survival mode");
        button.setToolTipText("Create a Survival in Minecraft with this seed and at this location");
        button.addActionListener(event -> playHere(false));
        toolBar.add(button);

        toolBar.add(Box.createHorizontalStrut(5));
        button = new JButton("Play here in Creative mode");
        button.setToolTipText("Create a Creative in Minecraft with this seed and at this location");
        button.addActionListener(event -> playHere(true));
        toolBar.add(button);

        toolBar.add(Box.createHorizontalGlue());
        getContentPane().add(toolBar, BorderLayout.SOUTH);

        scaleToUI(this);
        setSize(800, 600);
    }

    public void destroy() {
        imageViewer.removeAllTileProviders();
    }

    private void playHere(boolean creativeMode) {
        String name = JOptionPane.showInputDialog(BiomesViewerFrame.this, "Type a name for the map:", "Map Name", JOptionPane.QUESTION_MESSAGE);
        if ((name == null) || (name.trim().length() == 0)) {
            return;
        }
        name = name.trim();
        File savesDir;
        boolean minecraftDirUsed = false;
        File minecraftDir = MinecraftUtil.findMinecraftDir();
        if (minecraftDir != null) {
            savesDir = new File(minecraftDir, "saves");
            minecraftDirUsed = true;
        } else {
            savesDir = DesktopUtils.getDocumentsFolder();
        }
        File worldDir = new File(savesDir, name);
        int ordinal = 1;
        while (worldDir.isDirectory()) {
            worldDir = new File(savesDir, name + ordinal);
            ordinal++;
        }
        if (! worldDir.mkdirs()) {
            throw new RuntimeException("Could not create " + worldDir);
        }
        BiomeScheme biomeScheme1 = BiomesViewerFrame.this.biomeScheme;
        final Platform platform = (biomeScheme1 instanceof Minecraft1_1BiomeScheme) ? DefaultPlugin.JAVA_MCREGION : DefaultPlugin.JAVA_ANVIL;
        Java117Level level = new Java117Level(platform.standardMaxHeight, platform);
        final long seed = ((Number) seedSpinner.getValue()).longValue();
        if (! (biomeScheme1 instanceof Minecraft1_1BiomeScheme)) {
            final Generator generatorType = ((biomeScheme1 instanceof Minecraft1_3LargeBiomeScheme) || (biomeScheme1 instanceof Minecraft1_7LargeBiomeScheme) || (biomeScheme1 instanceof Minecraft1_8LargeBiomeScheme) || (biomeScheme1 instanceof Minecraft1_12LargeBiomeScheme)) ? Generator.LARGE_BIOMES : Generator.DEFAULT;
            level.setGenerator(DIM_NORMAL, new SeededGenerator(generatorType, seed));
        }
        if (creativeMode) {
            level.setGameType(Constants.GAME_TYPE_CREATIVE);
        } else {
            level.setGameType(Constants.GAME_TYPE_SURVIVAL);
        }
        level.setMapFeatures(true);
        level.setName(name);
        level.setSeed(seed);
        Point worldCoords = imageViewer.getViewLocation();
        level.setSpawnX(worldCoords.x);
        level.setSpawnZ(worldCoords.y);
        level.setSpawnY(64);
        try {
            level.save(worldDir);
        } catch (IOException e) {
            throw new RuntimeException("I/O error writing level.dat file", e);
        }
        if (minecraftDirUsed) {
            JOptionPane.showMessageDialog(BiomesViewerFrame.this, "Map saved! You can find it in Minecraft under Singleplayer.");
        } else {
            JOptionPane.showMessageDialog(BiomesViewerFrame.this, "Map saved as " + worldDir + ".\nMove it to your Minecraft saves directory to play.");
        }
    }

    private void createWorld() {
        App app = App.getInstanceIfExists();
        if (! app.saveIfNecessary()) {
            return;
        }
        final NewWorldDialog dialog = new NewWorldDialog(
            app,
            app.getColourScheme(),
            "Generated World",
            ((Number) seedSpinner.getValue()).longValue(),
            ((Integer) schemeChooser.getSelectedItem() == BIOME_ALGORITHM_1_1) ? DefaultPlugin.JAVA_MCREGION : DefaultPlugin.JAVA_ANVIL,
            DIM_NORMAL,
            Configuration.getInstance().getDefaultMaxHeight(),
            imageViewer.getSelectedTiles());
        dialog.setVisible(true);
        if (! dialog.isCancelled()) {
            app.clearWorld();
            if (! dialog.checkMemoryRequirements(this)) {
                return;
            }
            World2 newWorld = ProgressDialog.executeTask(this, new ProgressTask<World2>() {
                @Override
                public String getName() {
                    return "Creating new world";
                }
                
                @Override
                public World2 execute(ProgressReceiver progressReceiver) throws OperationCancelled {
                    return dialog.getSelectedWorld(progressReceiver);
                }
            });
            if (newWorld != null) {
                final Dimension dimension = newWorld.getDimension(DIM_NORMAL);
                switch ((Integer) schemeChooser.getSelectedItem()) {
                    case BIOME_ALGORITHM_1_1:
                    case BIOME_ALGORITHM_1_2_AND_1_3_DEFAULT:
                    case BIOME_ALGORITHM_1_7_DEFAULT:
                        dimension.setGenerator(new SeededGenerator(DEFAULT, dimension.getMinecraftSeed()));
                        break;
                    case BIOME_ALGORITHM_1_3_LARGE:
                    case BIOME_ALGORITHM_1_7_LARGE:
                        dimension.setGenerator(new SeededGenerator(LARGE_BIOMES, dimension.getMinecraftSeed()));
                        break;
                }
                app.setWorld(newWorld, true);
            }
        }
    }
    
    private void setControlStates() {
        createWorldButton.setEnabled((! standAloneMode) && (! imageViewer.getSelectedTiles().isEmpty()));
    }

    private final WPTileSelectionViewer imageViewer;
    private final SeedListener seedListener;
    private final JButton createWorldButton;
    private final JSpinner seedSpinner;
    private final JComboBox schemeChooser;
    private final ColourScheme colourScheme;
    private final boolean standAloneMode;
    private BiomeScheme biomeScheme;
    
    private static final long serialVersionUID = 1L;
    
    public interface SeedListener {
        void setSeed(long seed, Generator generator);
    }
    
    class Controller implements MouseListener, MouseMotionListener {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getButton() != MouseEvent.BUTTON1) {
                return;
            }
            Point tileLocation = getTileLocation(e.getX(), e.getY());
            if (imageViewer.isSelectedTile(tileLocation)) {
                imageViewer.removeSelectedTile(tileLocation);
            } else {
                imageViewer.addSelectedTile(tileLocation);
            }
            imageViewer.setSelectedRectangleCorner1(null);
            imageViewer.setSelectedRectangleCorner2(null);
            setControlStates();
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (e.getButton() != MouseEvent.BUTTON1) {
                return;
            }
            selecting = true;
            selectionCorner1 = getTileLocation(e.getX(), e.getY());
            selectionCorner2 = null;
            imageViewer.setSelectedRectangleCorner1(null);
            imageViewer.setSelectedRectangleCorner2(null);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.getButton() != MouseEvent.BUTTON1) {
                return;
            }
            if ((selectionCorner1 != null) && (selectionCorner2 != null)) {
                int tileX1 = Math.min(selectionCorner1.x, selectionCorner2.x);
                int tileX2 = Math.max(selectionCorner1.x, selectionCorner2.x);
                int tileY1 = Math.min(selectionCorner1.y, selectionCorner2.y);
                int tileY2 = Math.max(selectionCorner1.y, selectionCorner2.y);
                for (int x = tileX1; x <= tileX2; x++) {
                    for (int y = tileY1; y <= tileY2; y++) {
                        Point tileLocation = new Point(x, y);
                        if (imageViewer.isSelectedTile(tileLocation)) {
                            imageViewer.removeSelectedTile(tileLocation);
                        } else {
                            imageViewer.addSelectedTile(tileLocation);
                        }
                    }
                }
                setControlStates();
            }
            imageViewer.setSelectedRectangleCorner1(null);
            imageViewer.setSelectedRectangleCorner2(null);
            selecting = false;
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            imageViewer.setHighlightedTileLocation(getTileLocation(e.getX(), e.getY()));
        }

        @Override
        public void mouseExited(MouseEvent e) {
            imageViewer.setHighlightedTileLocation(null);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            imageViewer.setHighlightedTileLocation(getTileLocation(e.getX(), e.getY()));
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            imageViewer.setHighlightedTileLocation(getTileLocation(e.getX(), e.getY()));
            if (selecting) {
                selectionCorner2 = getTileLocation(e.getX(), e.getY());
                imageViewer.setSelectedRectangleCorner1(selectionCorner1);
                imageViewer.setSelectedRectangleCorner2(selectionCorner2);
            }
        }

        private Point getTileLocation(int x, int y) {
            Point coords = imageViewer.viewToWorld(x, y);
            return new Point(coords.x >> TILE_SIZE_BITS, coords.y >> TILE_SIZE_BITS);
        }

        private boolean selecting;
        private Point selectionCorner1, selectionCorner2;
    }
}