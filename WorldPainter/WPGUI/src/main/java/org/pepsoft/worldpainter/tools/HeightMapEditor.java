/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.tools;

import org.pepsoft.util.FileUtils;
import org.pepsoft.worldpainter.MouseAdapter;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.heightMaps.*;
import org.pepsoft.worldpainter.heightMaps.gui.HeightMapPropertiesPanel;
import org.pepsoft.worldpainter.heightMaps.gui.HeightMapTileProvider;
import org.pepsoft.worldpainter.heightMaps.gui.HeightMapTreeCellRenderer;
import org.pepsoft.worldpainter.heightMaps.gui.HeightMapTreeModel;
import org.pepsoft.worldpainter.layers.Biome;
import org.pepsoft.worldpainter.themes.SimpleTheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.pepsoft.minecraft.Constants.DEFAULT_MAX_HEIGHT_ANVIL;
import static org.pepsoft.minecraft.Constants.DEFAULT_WATER_LEVEL;
import static org.pepsoft.util.GUIUtils.scaleToUI;
import static org.pepsoft.worldpainter.Terrain.GRASS;

/**
 *
 * @author pepijn
 */
public class HeightMapEditor extends javax.swing.JFrame implements HeightMapPropertiesPanel.HeightMapListener {
    /**
     * Creates new form HeightMapEditor
     */
    public HeightMapEditor(HeightMap heightMap) throws IOException {
        rootHeightMap = heightMap;
        initComponents();
        tiledImageViewer1.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                int oldZoom = zoom;
                if (e.getWheelRotation() < 0) {
                    zoom = Math.min(zoom - e.getWheelRotation(), 6);
                } else {
                    zoom = Math.max(zoom - e.getWheelRotation(), -4);
                }
                if (zoom != oldZoom) {
                    tiledImageViewer1.setZoom(zoom, e.getX(), e.getY());
                }
            }

            private int zoom = 0;
        });
        cellRenderer = new HeightMapTreeCellRenderer();
        jTree1.setCellRenderer(cellRenderer);
        ToolTipManager.sharedInstance().registerComponent(jTree1);
        heightMapPropertiesPanel1.setListener(this);
        simpleThemeEditor1.setTheme(theme);
        simpleThemeEditor1.setChangeListener(editor -> themeChanged());
        createHeightMap();
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                jSplitPane3.setDividerLocation(1.0);
            }
        });
        scaleToUI(this);
    }

    // HeightMapListener

    @Override
    public void heightMapChanged(HeightMap heightMap, String propertyName) {
        if (! propertyName.equals("name")) {
            synchronized (tileCache) {
                tileCache.clear();
                tileCache.notifyAll();
            }
            tiledImageViewer1.refresh(true);
        }
    }

    private void themeChanged() {
        simpleThemeEditor1.save();
        if (radioButtonViewAsTerrain.isSelected()) {
            installHeightMap(false);
        }
    }

    private void createHeightMap() {
        focusOn(rootHeightMap);
        installHeightMap(true);
        jTree1.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                if (event.isPopupTrigger()) {
                    showPopupMenu(event);
                }
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                if (event.isPopupTrigger()) {
                    showPopupMenu(event);
                }
            }

            private void showPopupMenu(MouseEvent event) {
                TreePath path = jTree1.getPathForLocation(event.getX(), event.getY());
                if (path == null) {
                    return;
                }
                jTree1.setSelectionPath(path);
                HeightMap heightMap = (HeightMap) path.getLastPathComponent();
                TreePath parentPath = path.getParentPath();
                DelegatingHeightMap parent;
                if (parentPath != null) {
                    parent = (DelegatingHeightMap) parentPath.getLastPathComponent();
                } else {
                    parent = null;
                }
                JPopupMenu menu = new JPopupMenu();
                JMenuItem menuItem = new JMenuItem("Focus Here");
                menuItem.addActionListener(actionEvent -> {
                    focusOn(heightMap);
                    installHeightMap(false);
                });
                menu.add(menuItem);

                JMenu insertMenu = new JMenu("Insert");
                menuItem = new JMenuItem("Product");
                menuItem.addActionListener(actionEvent -> {
                    ProductHeightMap productHeightMap = new ProductHeightMap(heightMap, new ConstantHeightMap(1.0f));
                    replace(parent, heightMap, productHeightMap);
                });
                insertMenu.add(menuItem);
                menuItem = new JMenuItem("Sum");
                menuItem.addActionListener(actionEvent -> {
                    SumHeightMap sumHeightMap = new SumHeightMap(heightMap, new ConstantHeightMap(0.0f));
                    replace(parent, heightMap, sumHeightMap);
                });
                insertMenu.add(menuItem);
                menuItem = new JMenuItem("Maximum");
                menuItem.addActionListener(actionEvent -> {
                    MaximisingHeightMap maximisingHeightMap = new MaximisingHeightMap(heightMap, new ConstantHeightMap(0.0f));
                    replace(parent, heightMap, maximisingHeightMap);
                });
                insertMenu.add(menuItem);
                menuItem = new JMenuItem("Slope");
                menuItem.addActionListener(actionEvent -> {
                    SlopeHeightMap slopeHeightMap = new SlopeHeightMap(heightMap);
                    replace(parent, heightMap, slopeHeightMap);
                });
                insertMenu.add(menuItem);
                menuItem = new JMenuItem("Displacement");
                menuItem.addActionListener(actionEvent -> {
                    DisplacementHeightMap displacementHeightMap = new DisplacementHeightMap(heightMap, new ConstantHeightMap(0.0f), new ConstantHeightMap(0.0f));
                    replace(parent, heightMap, displacementHeightMap);
                });
                insertMenu.add(menuItem);
                menuItem = new JMenuItem("Transformation");
                menuItem.addActionListener(actionEvent -> {
                    TransformingHeightMap transformingHeightMap = new TransformingHeightMap(heightMap.getName(), heightMap, 1.0f, 1.0f, 0, 0, 0.0f);
                    replace(parent, heightMap, transformingHeightMap);
                });
                insertMenu.add(menuItem);
                menuItem = new JMenuItem("Shelves");
                menuItem.addActionListener(actionEvent -> {
                    ShelvingHeightMap shelvingHeightMap = new ShelvingHeightMap(heightMap);
                    replace(parent, heightMap, shelvingHeightMap);
                });
                insertMenu.add(menuItem);
                menu.add(insertMenu);
                JMenu replaceMenu = new JMenu("Replace");
                menuItem = new JMenuItem("Mandelbrot");
                menuItem.addActionListener(actionEvent -> {
                    MandelbrotHeightMap mandelbrotHeightMap = new MandelbrotHeightMap();
                    replace(parent, heightMap, mandelbrotHeightMap);
                });
                replaceMenu.add(menuItem);
                menuItem = new JMenuItem("Nine Patch");
                menuItem.addActionListener(actionEvent -> {
                    NinePatchHeightMap ninePatchHeightMap = new NinePatchHeightMap(100, 25, 1.0f);
                    replace(parent, heightMap, ninePatchHeightMap);
                });
                replaceMenu.add(menuItem);
                menuItem = new JMenuItem("Constant");
                menuItem.addActionListener(actionEvent -> {
                    ConstantHeightMap constantHeightMap = new ConstantHeightMap(1.0f);
                    replace(parent, heightMap, constantHeightMap);
                });
                replaceMenu.add(menuItem);
                menuItem = new JMenuItem("Bitmap");
                menuItem.addActionListener(actionEvent -> {
                    File myHeightMapDir = Configuration.getInstance().getHeightMapsDirectory();
                    final Set<String> extensions = new HashSet<>(Arrays.asList(ImageIO.getReaderFileSuffixes()));
                    StringBuilder sb = new StringBuilder("Supported image formats (");
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
                    File file = FileUtils.selectFileForOpen(HeightMapEditor.this, "Select a height map image file", myHeightMapDir, new FileFilter() {
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
                    if (file != null) {
                        try {
                            BufferedImage image = ImageIO.read(file);
                            BitmapHeightMap bitmapHeightMap = BitmapHeightMap.build().withName(file.getName()).withImage(image).withFile(file).now();
                            replace(parent, heightMap, bitmapHeightMap);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
                replaceMenu.add(menuItem);
                menuItem = new JMenuItem("Noise");
                menuItem.addActionListener(actionEvent -> {
                    NoiseHeightMap noiseHeightMap = new NoiseHeightMap(1f, 1.0, 1);
                    replace(parent, heightMap, noiseHeightMap);
                });
                replaceMenu.add(menuItem);
                menuItem = new JMenuItem("Bands");
                menuItem.addActionListener(actionEvent -> {
                    BandedHeightMap bandedHeightMap = new BandedHeightMap();
                    replace(parent, heightMap, bandedHeightMap);
                });
                replaceMenu.add(menuItem);
                replaceMenu.addSeparator();
                menuItem = new JMenuItem("Product");
                menuItem.addActionListener(actionEvent -> {
                    ProductHeightMap productHeightMap = new ProductHeightMap(new ConstantHeightMap(1.0f), new ConstantHeightMap(1.0f));
                    replace(parent, heightMap, productHeightMap);
                });
                replaceMenu.add(menuItem);
                menuItem = new JMenuItem("Sum");
                menuItem.addActionListener(actionEvent -> {
                    SumHeightMap sumHeightMap = new SumHeightMap(new ConstantHeightMap(0.5f), new ConstantHeightMap(0.5f));
                    replace(parent, heightMap, sumHeightMap);
                });
                replaceMenu.add(menuItem);
                menuItem = new JMenuItem("Maximum");
                menuItem.addActionListener(actionEvent -> {
                    MaximisingHeightMap maximisingHeightMap = new MaximisingHeightMap(new ConstantHeightMap(1.0f), new ConstantHeightMap(1.0f));
                    replace(parent, heightMap, maximisingHeightMap);
                });
                replaceMenu.add(menuItem);
                menuItem = new JMenuItem("Slope");
                menuItem.addActionListener(actionEvent -> {
                    SlopeHeightMap slopeHeightMap = new SlopeHeightMap(new ConstantHeightMap(1.0f));
                    replace(parent, heightMap, slopeHeightMap);
                });
                replaceMenu.add(menuItem);
                menuItem = new JMenuItem("Displacement");
                menuItem.addActionListener(actionEvent -> {
                    DisplacementHeightMap displacementHeightMap = new DisplacementHeightMap(new ConstantHeightMap(1.0f), new ConstantHeightMap(0.0f), new ConstantHeightMap(0.0f));
                    replace(parent, heightMap, displacementHeightMap);
                });
                replaceMenu.add(menuItem);
                menuItem = new JMenuItem("Transformation");
                menuItem.addActionListener(actionEvent -> {
                    TransformingHeightMap transformingHeightMap = new TransformingHeightMap(null, new ConstantHeightMap(1.0f), 1.0f, 1.0f, 0, 0, 0.0f);
                    replace(parent, heightMap, transformingHeightMap);
                });
                replaceMenu.add(menuItem);
                menuItem = new JMenuItem("Shelves");
                menuItem.addActionListener(actionEvent -> {
                    ShelvingHeightMap shelvingHeightMap = new ShelvingHeightMap(new ConstantHeightMap(1.0f));
                    replace(parent, heightMap, shelvingHeightMap);
                });
                replaceMenu.add(menuItem);
                menu.add(replaceMenu);
                if (heightMap instanceof DelegatingHeightMap) {
                    menuItem = new JMenuItem("Delete");
                    menuItem.addActionListener(e -> {
                        replace(parent, heightMap, ((DelegatingHeightMap) heightMap).getHeightMap(0));
                        treeModel.notifyListeners();
                    });
                    menu.add(menuItem);
                }
                menu.show(jTree1, event.getX(), event.getY());
            }

            private void replace(DelegatingHeightMap parent, HeightMap oldHeightMap, HeightMap newHeightMap) {
                if (parent == null) {
                    HeightMapEditor.this.rootHeightMap = newHeightMap;
                    focusOn(newHeightMap);
                    installHeightMap(true);
                } else {
                    parent.replace(oldHeightMap, newHeightMap);
                    if (oldHeightMap == HeightMapEditor.this.focusHeightMap) {
                        focusOn(newHeightMap);
                    }
                    treeModel.notifyListeners();
                    synchronized (tileCache) {
                        tileCache.clear();
                        tileCache.notifyAll();
                    }
                    tiledImageViewer1.refresh(true);
                }
            }
        });
    }

    private void installHeightMap(boolean updateTreeModel) {
        switch (viewMode) {
            case HEIGHT_MAP:
                tiledImageViewer1.setTileProvider(new HeightMapTileProvider(focusHeightMap));
                tiledImageViewer1.setGridColour(Color.GRAY);
                break;
            case TERRAIN:
                TileFactory tileFactory = new HeightMapTileFactory(seed, focusHeightMap, 0, DEFAULT_MAX_HEIGHT_ANVIL, false, theme);
                synchronized (tileCache) {
                    tileCache.clear();
                }
                final org.pepsoft.worldpainter.TileProvider tileProvider = new org.pepsoft.worldpainter.TileProvider() {
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
                        Tile tile;
                        synchronized (tileCache) {
                            tile = tileCache.get(coords);
                            if (tile == RENDERING) {
                                do {
                                    try {
                                        tileCache.wait();
                                        tile = tileCache.get(coords);
                                    } catch (InterruptedException e) {
                                        throw new RuntimeException("Thread interrupted while waiting for tile to be rendered");
                                    }
                                } while (tileCache.get(coords) == RENDERING);
                            }
                            if (tile == null) {
                                tileCache.put(coords, RENDERING);
                            }
                        }
                        if (tile == null) {
                            tile = tileFactory.createTile(x, y);
                            synchronized (tileCache) {
                                tileCache.put(coords, tile);
                                tileCache.notifyAll();
                            }
                        }
                        return tile;
                    }
                };
                tiledImageViewer1.setTileProvider(new WPTileProvider(tileProvider, ColourScheme.DEFAULT, null, Collections.singleton(Biome.INSTANCE), false, 10, TileRenderer.LightOrigin.NORTHWEST));
                tiledImageViewer1.setGridColour(Color.BLACK);
                break;
        }
        if (updateTreeModel) {
            treeModel = new HeightMapTreeModel(rootHeightMap);
            jTree1.setModel(treeModel);
        }
    }

    private void focusOn(HeightMap heightMap) {
        focusHeightMap = heightMap;
        cellRenderer.setFocusHeightMap(focusHeightMap);
        jTree1.repaint();
    }

    private void switchView() {
        switch (viewMode) {
            case TERRAIN:
                if (radioButtonViewAsHeightMap.isSelected()) {
                    viewMode = ViewMode.HEIGHT_MAP;
                    installHeightMap(false);
                }
                break;
            case HEIGHT_MAP:
                if (radioButtonViewAsTerrain.isSelected()) {
                    viewMode = ViewMode.TERRAIN;
                    installHeightMap(false);
                }
                break;
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        jToolBar1 = new javax.swing.JToolBar();
        jButton1 = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        radioButtonViewAsHeightMap = new javax.swing.JRadioButton();
        radioButtonViewAsTerrain = new javax.swing.JRadioButton();
        checkBoxShowGrid = new javax.swing.JCheckBox();
        jSplitPane1 = new javax.swing.JSplitPane();
        jSplitPane2 = new javax.swing.JSplitPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTree1 = new javax.swing.JTree();
        heightMapPropertiesPanel1 = new org.pepsoft.worldpainter.heightMaps.gui.HeightMapPropertiesPanel();
        jSplitPane3 = new javax.swing.JSplitPane();
        tiledImageViewer1 = new org.pepsoft.util.swing.TiledImageViewer();
        simpleThemeEditor1 = new org.pepsoft.worldpainter.themes.impl.simple.SimpleThemeEditor();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Height Map Editor");

        jToolBar1.setRollover(true);

        jButton1.setText("New Seed");
        jButton1.setFocusable(false);
        jButton1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton1.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jToolBar1.add(jButton1);

        jLabel1.setText("View as:");
        jToolBar1.add(jLabel1);

        buttonGroup1.add(radioButtonViewAsHeightMap);
        radioButtonViewAsHeightMap.setSelected(true);
        radioButtonViewAsHeightMap.setText("height map");
        radioButtonViewAsHeightMap.setFocusable(false);
        radioButtonViewAsHeightMap.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        radioButtonViewAsHeightMap.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonViewAsHeightMapActionPerformed(evt);
            }
        });
        jToolBar1.add(radioButtonViewAsHeightMap);

        buttonGroup1.add(radioButtonViewAsTerrain);
        radioButtonViewAsTerrain.setText("terrain");
        radioButtonViewAsTerrain.setFocusable(false);
        radioButtonViewAsTerrain.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        radioButtonViewAsTerrain.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonViewAsTerrainActionPerformed(evt);
            }
        });
        jToolBar1.add(radioButtonViewAsTerrain);

        checkBoxShowGrid.setSelected(true);
        checkBoxShowGrid.setText("Show grid:");
        checkBoxShowGrid.setFocusable(false);
        checkBoxShowGrid.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        checkBoxShowGrid.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxShowGridActionPerformed(evt);
            }
        });
        jToolBar1.add(checkBoxShowGrid);

        getContentPane().add(jToolBar1, java.awt.BorderLayout.NORTH);

        jSplitPane1.setContinuousLayout(true);

        jSplitPane2.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jSplitPane2.setResizeWeight(0.1);
        jSplitPane2.setContinuousLayout(true);

        jTree1.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                jTree1ValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(jTree1);

        jSplitPane2.setTopComponent(jScrollPane1);
        jSplitPane2.setBottomComponent(heightMapPropertiesPanel1);

        jSplitPane1.setLeftComponent(jSplitPane2);

        jSplitPane3.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jSplitPane3.setResizeWeight(0.9);
        jSplitPane3.setContinuousLayout(true);
        jSplitPane3.setOneTouchExpandable(true);

        tiledImageViewer1.setPaintGrid(true);
        jSplitPane3.setTopComponent(tiledImageViewer1);
        jSplitPane3.setBottomComponent(simpleThemeEditor1);

        jSplitPane1.setRightComponent(jSplitPane3);

        getContentPane().add(jSplitPane1, java.awt.BorderLayout.CENTER);

        setSize(new java.awt.Dimension(800, 600));
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void jTree1ValueChanged(javax.swing.event.TreeSelectionEvent evt) {//GEN-FIRST:event_jTree1ValueChanged
        if (jTree1.getSelectionCount() == 1) {
            HeightMap heightMap = (HeightMap) jTree1.getSelectionPath().getLastPathComponent();
            heightMapPropertiesPanel1.setHeightMap(heightMap);
        }
    }//GEN-LAST:event_jTree1ValueChanged

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        seed = new Random().nextLong();
        rootHeightMap.setSeed(seed);
        theme.setSeed(seed);
        synchronized (tileCache) {
            tileCache.clear();
            tileCache.notifyAll();
        }
        tiledImageViewer1.refresh(true);
    }//GEN-LAST:event_jButton1ActionPerformed

    private void radioButtonViewAsHeightMapActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonViewAsHeightMapActionPerformed
        switchView();
    }//GEN-LAST:event_radioButtonViewAsHeightMapActionPerformed

    private void radioButtonViewAsTerrainActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonViewAsTerrainActionPerformed
        switchView();
    }//GEN-LAST:event_radioButtonViewAsTerrainActionPerformed

    private void checkBoxShowGridActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxShowGridActionPerformed
        tiledImageViewer1.setPaintGrid(checkBoxShowGrid.isSelected());
    }//GEN-LAST:event_checkBoxShowGridActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        // Load or initialise configuration
        Configuration config = null;
        try {
            config = Configuration.load(); // This will migrate the configuration directory if necessary
        } catch (IOException | Error | RuntimeException | ClassNotFoundException e) {
            configError(e);
        }
        if (config == null) {
            if (! logger.isDebugEnabled()) {
                // If debug logging is on, the Configuration constructor will
                // already log this
                logger.info("Creating new configuration");
            }
            config = new Configuration();
        }
        Configuration.setInstance(config);
        logger.info("Installation ID: " + config.getUuid());

        HeightMap rootHeightMap = TileFactoryFactory.createFancyTileFactory(new Random().nextLong(), GRASS, 0, DEFAULT_MAX_HEIGHT_ANVIL, DEFAULT_WATER_LEVEL, 58, false, 20f, 1.0).getHeightMap();
//        File bitmapFile = new File("/home/pepijn/Pictures/WorldPainter/test-image-8-bit-grayscale.png");
//        BufferedImage bitmap = ImageIO.read(bitmapFile);
//        BitmapHeightMap bitmapHeightMap = BitmapHeightMap.build().withImage(bitmap).withSmoothScaling(false).withRepeat(true).now();
//        TransformingHeightMap scaledHeightMap = TransformingHeightMap.build().withHeightMap(bitmapHeightMap).withScale(300).now();
//        NoiseHeightMap angleMap = new NoiseHeightMap("Angle", (float) (Math.PI * 2), 2.5f, 1);
//        NoiseHeightMap distanceMap = new NoiseHeightMap("Distance", 25f, 2.5f, 1);
//        rootHeightMap = new DisplacementHeightMap(scaledHeightMap, angleMap, distanceMap);

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> {
            try {
                final HeightMapEditor heightMapEditor = new HeightMapEditor(rootHeightMap);
                heightMapEditor.setDefaultCloseOperation(EXIT_ON_CLOSE);
                heightMapEditor.setVisible(true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private static void configError(Throwable e) {
        logger.error("Exception while loading config file", e);
        JOptionPane.showMessageDialog(null, "Could not read configuration file! Resetting configuration.\n\nException type: " + e.getClass().getSimpleName() + "\nMessage: " + e.getMessage(), "Configuration Error", JOptionPane.ERROR_MESSAGE);
    }

    private HeightMap rootHeightMap, focusHeightMap;
    private final Map<Point, Tile> tileCache = Collections.synchronizedMap(new HashMap<>());
    private HeightMapTreeModel treeModel;
    private ViewMode viewMode = ViewMode.HEIGHT_MAP;
    private SimpleTheme theme = SimpleTheme.createDefault(Terrain.GRASS, 0, DEFAULT_MAX_HEIGHT_ANVIL, DEFAULT_WATER_LEVEL);
    private long seed = new Random().nextLong();
    private HeightMapTreeCellRenderer cellRenderer;

    private static final Tile RENDERING = new Tile(0, 0, 0, 0, false) {};
    private static final Logger logger = LoggerFactory.getLogger(HeightMapEditor.class);

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JCheckBox checkBoxShowGrid;
    private org.pepsoft.worldpainter.heightMaps.gui.HeightMapPropertiesPanel heightMapPropertiesPanel1;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JSplitPane jSplitPane2;
    private javax.swing.JSplitPane jSplitPane3;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JTree jTree1;
    private javax.swing.JRadioButton radioButtonViewAsHeightMap;
    private javax.swing.JRadioButton radioButtonViewAsTerrain;
    private org.pepsoft.worldpainter.themes.impl.simple.SimpleThemeEditor simpleThemeEditor1;
    private org.pepsoft.util.swing.TiledImageViewer tiledImageViewer1;
    // End of variables declaration//GEN-END:variables

    enum ViewMode {HEIGHT_MAP, TERRAIN}
}