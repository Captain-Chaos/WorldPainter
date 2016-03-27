/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.tools;

import org.pepsoft.util.FileUtils;
import org.pepsoft.util.swing.TileProvider;
import org.pepsoft.worldpainter.Configuration;
import org.pepsoft.worldpainter.HeightMap;
import org.pepsoft.worldpainter.MouseAdapter;
import org.pepsoft.worldpainter.heightMaps.*;
import org.pepsoft.worldpainter.heightMaps.gui.HeightMapPropertiesPanel;
import org.pepsoft.worldpainter.heightMaps.gui.HeightMapTileProvider;
import org.pepsoft.worldpainter.heightMaps.gui.HeightMapTreeCellRenderer;
import org.pepsoft.worldpainter.heightMaps.gui.HeightMapTreeModel;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

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
        TreeCellRenderer cellRenderer = new HeightMapTreeCellRenderer();
        jTree1.setCellRenderer(cellRenderer);
        heightMapPropertiesPanel1.setListener(this);
        createHeightMap();
    }

    // HeightMapListener

    @Override
    public void heightMapChanged(HeightMap heightMap, String propertyName) {
        if (! propertyName.equals("name")) {
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
        installHeightMap();
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
                    TransformingHeightMap transformingHeightMap = new TransformingHeightMap(heightMap.getName(), heightMap, 100, 0, 0);
                    replace(parent, heightMap, transformingHeightMap);
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
                    TransformingHeightMap transformingHeightMap = new TransformingHeightMap(null, new ConstantHeightMap(1.0f), 100, 0, 0);
                    replace(parent, heightMap, transformingHeightMap);
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
                    installHeightMap();
                } else {
                    parent.replace(oldHeightMap, newHeightMap);
                    treeModel.notifyListeners();
                    tiledImageViewer1.refresh(true);
                }
            }
        });
    }

    private void installHeightMap() {
        TileProvider tileProvider = new HeightMapTileProvider(heightMap);
        tiledImageViewer1.setTileProvider(tileProvider);
        treeModel = new HeightMapTreeModel(heightMap);
        jTree1.setModel(treeModel);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jToolBar1 = new javax.swing.JToolBar();
        jButton1 = new javax.swing.JButton();
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
        jButton1.addActionListener(this::jButton1ActionPerformed);
        jToolBar1.add(jButton1);

        getContentPane().add(jToolBar1, java.awt.BorderLayout.NORTH);

        jSplitPane1.setRightComponent(tiledImageViewer1);

        jSplitPane2.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        jTree1.addTreeSelectionListener(this::jTree1ValueChanged);
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
        tiledImageViewer1.refresh(true);
    }//GEN-LAST:event_jButton1ActionPerformed

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

    private HeightMap heightMap;
    private HeightMapTreeModel treeModel;

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private org.pepsoft.worldpainter.heightMaps.gui.HeightMapPropertiesPanel heightMapPropertiesPanel1;
    private javax.swing.JButton jButton1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JSplitPane jSplitPane2;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JTree jTree1;
    private org.pepsoft.util.swing.TiledImageViewer tiledImageViewer1;
    // End of variables declaration//GEN-END:variables
}
