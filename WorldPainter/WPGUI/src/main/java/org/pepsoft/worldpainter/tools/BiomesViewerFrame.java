/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.tools;

import java.awt.BorderLayout;
import java.awt.HeadlessException;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.io.IOException;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.JToolBar;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.pepsoft.minecraft.Constants;
import org.pepsoft.minecraft.Level;
import org.pepsoft.util.DesktopUtils;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.util.ProgressReceiver.OperationCancelled;
import org.pepsoft.util.swing.ProgressDialog;
import org.pepsoft.util.swing.ProgressTask;
import org.pepsoft.worldpainter.App;
import org.pepsoft.worldpainter.BiomeScheme;
import org.pepsoft.worldpainter.ColourScheme;
import org.pepsoft.worldpainter.Configuration;
import org.pepsoft.worldpainter.NewWorldDialog;
import org.pepsoft.worldpainter.World2;
import org.pepsoft.worldpainter.biomeschemes.BiomeSchemeManager;
import org.pepsoft.worldpainter.biomeschemes.Minecraft1_1BiomeScheme;
import org.pepsoft.worldpainter.biomeschemes.Minecraft1_2BiomeScheme;
import org.pepsoft.worldpainter.util.MinecraftUtil;
import static org.pepsoft.worldpainter.Constants.*;
import org.pepsoft.worldpainter.Generator;
import org.pepsoft.worldpainter.biomeschemes.Minecraft1_3LargeBiomeScheme;
import org.pepsoft.worldpainter.biomeschemes.Minecraft1_6BiomeScheme;
import org.pepsoft.worldpainter.biomeschemes.Minecraft1_6LargeBiomeScheme;
import org.pepsoft.worldpainter.biomeschemes.Minecraft1_7BiomeScheme;
import org.pepsoft.worldpainter.biomeschemes.Minecraft1_7LargeBiomeScheme;
import org.pepsoft.worldpainter.biomeschemes.Minecraft1_8BiomeScheme;
import org.pepsoft.worldpainter.biomeschemes.Minecraft1_8LargeBiomeScheme;

/**
 *
 * @author pepijn
 */
public class BiomesViewerFrame extends JFrame {
    public BiomesViewerFrame(long seed, BiomeScheme biomeScheme, ColourScheme colourScheme, SeedListener seedListener) throws HeadlessException {
        this(seed, null, biomeScheme, colourScheme, seedListener);
    }
    
    public BiomesViewerFrame(long seed, final Point marker, BiomeScheme biomeScheme, ColourScheme colourScheme, SeedListener seedListener) throws HeadlessException {
        super("WorldPainter - Biomes Viewer");
        if (! ((biomeScheme instanceof Minecraft1_1BiomeScheme)
                || (biomeScheme instanceof Minecraft1_2BiomeScheme)
                || (biomeScheme instanceof Minecraft1_3LargeBiomeScheme)
                || (biomeScheme instanceof Minecraft1_6BiomeScheme)
                || (biomeScheme instanceof Minecraft1_6LargeBiomeScheme)
                || (biomeScheme instanceof Minecraft1_7BiomeScheme)
                || (biomeScheme instanceof Minecraft1_7LargeBiomeScheme)
                || (biomeScheme instanceof Minecraft1_8BiomeScheme)
                || (biomeScheme instanceof Minecraft1_8LargeBiomeScheme))) {
            throw new IllegalArgumentException("A Minecraft 1.8 or 1.1 - 1.7 biome scheme must be selected");
        }
        this.biomeScheme = biomeScheme;
        this.colourScheme = colourScheme;
        this.seedListener = seedListener;
        standAloneMode = App.getInstanceIfExists() == null;
        biomeScheme.setSeed(seed);
        final BiomesTileProvider tileProvider = new BiomesTileProvider(biomeScheme, colourScheme);
        imageViewer = new BiomesViewer(standAloneMode, Math.max(Runtime.getRuntime().availableProcessors() - 1, 1), true);
        if (marker != null) {
            imageViewer.setMarkerCoords(marker);
            imageViewer.moveToMarker();
        }
        imageViewer.setTileProvider(tileProvider);
        imageViewer.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                int rotation = e.getWheelRotation();
                int zoom = imageViewer.getZoom();
                if (rotation < 0) {
                    zoom = Math.min(zoom + -rotation, 0);
                } else {
                    zoom = Math.max(zoom - rotation, -4);
                }
                imageViewer.setZoom(zoom);
            }
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
        schemeChooser = new JComboBox(new Object[] {"Minecraft 1.7 or 1.8 Default", "Minecraft 1.7 or 1.8 Large Biomes", "Minecraft 1.6 Default (or 1.2 - 1.5)", "Minecraft 1.6 Large Biomes (or 1.3 - 1.5)", "Minecraft 1.1"});
        seedSpinner = new JSpinner(new SpinnerNumberModel(Long.valueOf(seed), Long.valueOf(Long.MIN_VALUE), Long.valueOf(Long.MAX_VALUE), Long.valueOf(1L)));
        if ((biomeScheme instanceof Minecraft1_7LargeBiomeScheme) || (biomeScheme instanceof Minecraft1_8LargeBiomeScheme)) {
            schemeChooser.setSelectedIndex(1);
        } else if (biomeScheme instanceof Minecraft1_6BiomeScheme) {
            schemeChooser.setSelectedIndex(2);
        } else if (biomeScheme instanceof Minecraft1_6LargeBiomeScheme) {
            schemeChooser.setSelectedIndex(3);
        } else if (biomeScheme instanceof Minecraft1_1BiomeScheme) {
            schemeChooser.setSelectedIndex(4);
        }
        schemeChooser.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                int selectedIndex = schemeChooser.getSelectedIndex();
                BiomeScheme biomeScheme = null;
                switch (selectedIndex) {
                    case 0:
                        if (! (BiomesViewerFrame.this.biomeScheme instanceof Minecraft1_7BiomeScheme)) {
                            biomeScheme = BiomeSchemeManager.getBiomeScheme(BiomeSchemeManager.BIOME_ALGORITHM_1_7_DEFAULT, BiomesViewerFrame.this);
                        }
                        break;
                    case 1:
                        if (! (BiomesViewerFrame.this.biomeScheme instanceof Minecraft1_7LargeBiomeScheme)) {
                            biomeScheme = BiomeSchemeManager.getBiomeScheme(BiomeSchemeManager.BIOME_ALGORITHM_1_7_LARGE, BiomesViewerFrame.this);
                        }
                        break;
                    case 2:
                        if (! (BiomesViewerFrame.this.biomeScheme instanceof Minecraft1_2BiomeScheme)) {
                            biomeScheme = BiomeSchemeManager.getBiomeScheme(BiomeSchemeManager.BIOME_ALGORITHM_1_2_AND_1_3_DEFAULT, BiomesViewerFrame.this);
                        }
                        break;
                    case 3:
                        if (! (BiomesViewerFrame.this.biomeScheme instanceof Minecraft1_3LargeBiomeScheme)) {
                            biomeScheme = BiomeSchemeManager.getBiomeScheme(BiomeSchemeManager.BIOME_ALGORITHM_1_3_LARGE, BiomesViewerFrame.this);
                        }
                        break;
                    case 4:
                        if (! (BiomesViewerFrame.this.biomeScheme instanceof Minecraft1_1BiomeScheme)) {
                            biomeScheme = BiomeSchemeManager.getBiomeScheme(BiomeSchemeManager.BIOME_ALGORITHM_1_1, BiomesViewerFrame.this);
                        }
                        break;
                }
                if (biomeScheme != null) {
                    BiomesViewerFrame.this.biomeScheme = biomeScheme;
                    BiomesViewerFrame.this.biomeScheme.setSeed(((Number) seedSpinner.getValue()).longValue());
                    imageViewer.setTileProvider(new BiomesTileProvider(BiomesViewerFrame.this.biomeScheme, BiomesViewerFrame.this.colourScheme, imageViewer.getZoom(), false));
                }
            }
        });
        toolBar.add(schemeChooser);
        toolBar.add(Box.createHorizontalStrut(5));
        toolBar.add(new JLabel("Seed:"));
        seedSpinner.setEditor(new JSpinner.NumberEditor(seedSpinner, "0"));
        seedSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                BiomesViewerFrame.this.biomeScheme.setSeed(((Number) seedSpinner.getValue()).longValue());
                imageViewer.setTileProvider(new BiomesTileProvider(BiomesViewerFrame.this.biomeScheme, BiomesViewerFrame.this.colourScheme, imageViewer.getZoom(), false));
            }
        });
        toolBar.add(seedSpinner);
        getContentPane().add(toolBar, BorderLayout.NORTH);
        
        toolBar = new JToolBar();
        JButton button = new JButton("-");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int zoom = imageViewer.getZoom();
                zoom = Math.max(zoom - 1, -4);
                imageViewer.setZoom(zoom);
            }
        });
        toolBar.add(button);
        
        button = new JButton("+");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int zoom = imageViewer.getZoom();
                zoom = Math.min(zoom + 1, 0);
                imageViewer.setZoom(zoom);
            }
        });
        toolBar.add(button);
        
        toolBar.add(Box.createHorizontalStrut(5));
        createWorldButton = new JButton("Create world");
        if (! standAloneMode) {
            createWorldButton.setToolTipText("Create a new WorldPainter world from the selected tiles");
            createWorldButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    createWorld();
                }
            });
        }
        createWorldButton.setEnabled(false);
        toolBar.add(createWorldButton);
        
        toolBar.add(Box.createHorizontalStrut(5));
        button = new JButton("Reset");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                imageViewer.reset();
                if (marker != null) {
                    imageViewer.moveToMarker();
                }
            }
        });
        toolBar.add(button);
        
        if (seedListener != null) {
            toolBar.add(Box.createHorizontalStrut(5));
            button = new JButton("Copy seed to world");
            button.setToolTipText("Copy the current seed to the world currently being edited");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    BiomesViewerFrame.this.seedListener.setSeed(((Number) seedSpinner.getValue()).longValue(), ((schemeChooser.getSelectedIndex() == 1) || (schemeChooser.getSelectedIndex() == 3)) ? Generator.LARGE_BIOMES : Generator.DEFAULT);
                }
            });
            toolBar.add(button);
        }
        
        toolBar.add(Box.createHorizontalStrut(5));
        button = new JButton("Play here");
        button.setToolTipText("Create a map in Minecraft with this seed and at this location");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
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
                BiomeScheme biomeScheme = BiomesViewerFrame.this.biomeScheme;
                Level level = new Level(Constants.DEFAULT_MAX_HEIGHT_1, (biomeScheme instanceof Minecraft1_1BiomeScheme) ? Constants.SUPPORTED_VERSION_1 : Constants.SUPPORTED_VERSION_2);
                if (! (biomeScheme instanceof Minecraft1_1BiomeScheme)) {
                    level.setGenerator(((biomeScheme instanceof Minecraft1_3LargeBiomeScheme) || (biomeScheme instanceof Minecraft1_7LargeBiomeScheme) || (biomeScheme instanceof Minecraft1_8LargeBiomeScheme)) ? Generator.LARGE_BIOMES : Generator.DEFAULT);
                }
                level.setGameType(Constants.GAME_TYPE_SURVIVAL);
                level.setMapFeatures(true);
                level.setName(name);
                level.setSeed(((Number) seedSpinner.getValue()).longValue());
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
        });
        toolBar.add(button);
        
        toolBar.add(Box.createHorizontalGlue());
        getContentPane().add(toolBar, BorderLayout.SOUTH);

        setSize(800, 600);
    }

    public void destroy() {
        imageViewer.removeAllTileProviders();
    }
    
    private void createWorld() {
        App app = App.getInstanceIfExists();
        if (! app.saveIfNecessary()) {
            return;
        }
        final NewWorldDialog dialog = new NewWorldDialog(
            app,
            "Generated World",
            ((Number) seedSpinner.getValue()).longValue(),
            DIM_NORMAL,
            Configuration.getInstance().getDefaultMaxHeight(),
            imageViewer.getSelectedTiles());
        dialog.setVisible(true);
        if (! dialog.isCancelled()) {
            app.setWorld(null);
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
                newWorld.setGenerator(((schemeChooser.getSelectedIndex() == 1) || (schemeChooser.getSelectedIndex() == 3)) ? Generator.LARGE_BIOMES : Generator.DEFAULT);
                app.setWorld(newWorld);
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
    
    public static interface SeedListener {
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