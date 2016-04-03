/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.tools;

import org.pepsoft.minecraft.Constants;
import org.pepsoft.util.FileUtils;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.biomeschemes.AutoBiomeScheme;
import org.pepsoft.worldpainter.colourschemes.DynMapColourScheme;
import org.pepsoft.worldpainter.heightMaps.*;
import org.pepsoft.worldpainter.heightMaps.gui.HeightMapPropertiesPanel;
import org.pepsoft.worldpainter.heightMaps.gui.HeightMapTileProvider;
import org.pepsoft.worldpainter.heightMaps.gui.HeightMapTreeCellRenderer;
import org.pepsoft.worldpainter.heightMaps.gui.HeightMapTreeModel;
import org.pepsoft.worldpainter.layers.Biome;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.themes.SimpleTheme;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 *
 * @author pepijn
 */
public class HeightMapEditor extends javax.swing.JFrame implements HeightMapPropertiesPanel.HeightMapListener {
    /**
     * Creates new form HeightMapEditor
     */
    public HeightMapEditor() throws IOException {
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
        TreeCellRenderer cellRenderer = new HeightMapTreeCellRenderer();
        jTree1.setCellRenderer(cellRenderer);
        ToolTipManager.sharedInstance().registerComponent(jTree1);
        heightMapPropertiesPanel1.setListener(this);
        createHeightMap();
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

    private void createHeightMap() throws IOException {
//        HeightMap heightMap = TileFactoryFactory.createFancyTileFactory(new Random().nextLong(), Terrain.GRASS, Constants.DEFAULT_MAX_HEIGHT_2, 62, 58, false, 20f, 1.0).getHeightMap();
        File bitmapFile = new File("/home/pepijn/Pictures/WorldPainter/test-image-8-bit-grayscale.png");
        BufferedImage bitmap = ImageIO.read(bitmapFile);
        BitmapHeightMap bitmapHeightMap = BitmapHeightMap.build().withImage(bitmap).withSmoothScaling(false).withRepeat(true).now();
        TransformingHeightMap scaledHeightMap = TransformingHeightMap.build().withHeightMap(bitmapHeightMap).withScale(300).now();
        NoiseHeightMap angleMap = new NoiseHeightMap("Angle", (float) (Math.PI * 2), 2.5f, 1);
        NoiseHeightMap distanceMap = new NoiseHeightMap("Distance", 25f, 2.5f, 1);
        heightMap = new DisplacementHeightMap(scaledHeightMap, angleMap, distanceMap);
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
                JMenu insertMenu = new JMenu("Insert");
                JMenuItem menuItem = new JMenuItem("Product");
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
                    TransformingHeightMap transformingHeightMap = new TransformingHeightMap(heightMap.getName(), heightMap, 100, 100, 0, 0, 0);
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
                            BitmapHeightMap bitmapHeightMap = new BitmapHeightMap(file.getName(), image, 0, file, false, false);
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
                    TransformingHeightMap transformingHeightMap = new TransformingHeightMap(null, new ConstantHeightMap(1.0f), 100, 100, 0, 0, 0);
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
                    HeightMapEditor.this.heightMap = newHeightMap;
                    installHeightMap(true);
                } else {
                    parent.replace(oldHeightMap, newHeightMap);
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
                tiledImageViewer1.setTileProvider(new HeightMapTileProvider(heightMap));
                tiledImageViewer1.setGridColour(Color.GRAY);
                break;
            case TERRAIN:
                long seed = new Random().nextLong();
                TileFactory tileFactory = new HeightMapTileFactory(seed, heightMap, Constants.DEFAULT_MAX_HEIGHT_2, false, SimpleTheme.createDefault(Terrain.GRASS, Constants.DEFAULT_MAX_HEIGHT_2, 62));
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
                tiledImageViewer1.setTileProvider(new WPTileProvider(tileProvider, new DynMapColourScheme("default", true), new AutoBiomeScheme(null), null, Collections.singleton((Layer) Biome.INSTANCE), false, 10, TileRenderer.LightOrigin.NORTHWEST, false, null));
                tiledImageViewer1.setGridColour(Color.BLACK);
                break;
        }
        if (updateTreeModel) {
            treeModel = new HeightMapTreeModel(heightMap);
            jTree1.setModel(treeModel);
        }
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
    @SuppressWarnings("unchecked")
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
        tiledImageViewer1 = new org.pepsoft.util.swing.TiledImageViewer();
        jSplitPane2 = new javax.swing.JSplitPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTree1 = new javax.swing.JTree();
        heightMapPropertiesPanel1 = new org.pepsoft.worldpainter.heightMaps.gui.HeightMapPropertiesPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
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

        tiledImageViewer1.setPaintGrid(true);
        jSplitPane1.setRightComponent(tiledImageViewer1);

        jSplitPane2.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        jTree1.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                jTree1ValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(jTree1);

        jSplitPane2.setLeftComponent(jScrollPane1);
        jSplitPane2.setRightComponent(heightMapPropertiesPanel1);

        jSplitPane1.setLeftComponent(jSplitPane2);

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
        heightMap.setSeed(new Random().nextLong());
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
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> {
            try {
                new HeightMapEditor().setVisible(true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private final Map<Point, Tile> tileCache = Collections.synchronizedMap(new HashMap<>());
    private HeightMap heightMap;
    private HeightMapTreeModel treeModel;
    private ViewMode viewMode = ViewMode.HEIGHT_MAP;

    private static final Tile RENDERING = new Tile(0, 0, 0, false) {};

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JCheckBox checkBoxShowGrid;
    private org.pepsoft.worldpainter.heightMaps.gui.HeightMapPropertiesPanel heightMapPropertiesPanel1;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JSplitPane jSplitPane2;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JTree jTree1;
    private javax.swing.JRadioButton radioButtonViewAsHeightMap;
    private javax.swing.JRadioButton radioButtonViewAsTerrain;
    private org.pepsoft.util.swing.TiledImageViewer tiledImageViewer1;
    // End of variables declaration//GEN-END:variables

    enum ViewMode {HEIGHT_MAP, TERRAIN}
}