/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * TileSelector.java
 *
 * Created on Apr 7, 2012, 5:58:41 PM
 */
package org.pepsoft.worldpainter;

import org.pepsoft.worldpainter.TileRenderer.LightOrigin;
import org.pepsoft.worldpainter.biomeschemes.CustomBiomeManager;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.renderers.VoidRenderer;
import org.pepsoft.worldpainter.tools.BiomesTileProvider;
import org.pepsoft.worldpainter.tools.WPTileSelectionViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.pepsoft.worldpainter.Constants.*;
import static org.pepsoft.worldpainter.DefaultPlugin.JAVA_ANVIL;
import static org.pepsoft.worldpainter.DefaultPlugin.JAVA_MCREGION;
import static org.pepsoft.worldpainter.Dimension.Role.*;
import static org.pepsoft.worldpainter.Generator.DEFAULT;
import static org.pepsoft.worldpainter.Generator.LARGE_BIOMES;
import static org.pepsoft.worldpainter.WPTileProvider.Effect.FADE_TO_FIFTY_PERCENT;

/**
 *
 * @author pepijn
 */
public class TileSelector extends javax.swing.JPanel {
    /** Creates new form TileSelector */
    public TileSelector() {
        initComponents();
        jPanel1.setBackground(new Color(VoidRenderer.getColour()));
        viewer.setZoom(viewer.getZoom() - 2);
        viewer.addMouseWheelListener(e -> {
            int rotation = e.getWheelRotation();
            int zoom = viewer.getZoom();
            if (rotation < 0) {
                zoom = Math.min(zoom + -rotation, 0);
            } else {
                zoom = Math.max(zoom - rotation, -4);
            }
            viewer.setZoom(zoom);
        });
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON1) {
                    return;
                }
                Point tileLocation = getTileLocation(e.getX(), e.getY());
                if (viewer.isSelectedTile(tileLocation)) {
                    viewer.removeSelectedTile(tileLocation);
                } else if (allowNonExistentTileSelection
                        || ((dimension != null) && dimension.isTilePresent(tileLocation.x, tileLocation.y))
                        || (allowBackgroundTileSelection && (backgroundDimension != null) && backgroundDimension.isTilePresent(tileLocation.x >> backgroundZoom, tileLocation.y >> backgroundZoom))) {
                    viewer.addSelectedTile(tileLocation);
                }
                viewer.setSelectedRectangleCorner1(null);
                viewer.setSelectedRectangleCorner2(null);
                setControlStates();
                notifyListeners();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON1) {
                    return;
                }
                selecting = true;
                selectionCorner1 = getTileLocation(e.getX(), e.getY());
                selectionCorner2 = null;
                viewer.setSelectedRectangleCorner1(null);
                viewer.setSelectedRectangleCorner2(null);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON1) {
                    return;
                }
                final boolean deselect = e.isControlDown() || e.isMetaDown();
                if ((selectionCorner1 != null) && (selectionCorner2 != null)) {
                    final int tileX1 = Math.min(selectionCorner1.x, selectionCorner2.x);
                    final int tileX2 = Math.max(selectionCorner1.x, selectionCorner2.x);
                    final int tileY1 = Math.min(selectionCorner1.y, selectionCorner2.y);
                    final int tileY2 = Math.max(selectionCorner1.y, selectionCorner2.y);
                    for (int x = tileX1; x <= tileX2; x++) {
                        for (int y = tileY1; y <= tileY2; y++) {
                            final Point tileLocation = new Point(x, y);
                            if (deselect && viewer.isSelectedTile(tileLocation)) {
                                viewer.removeSelectedTile(tileLocation);
                            } else if ((! deselect)
                                    && (allowNonExistentTileSelection
                                        || ((dimension != null) && dimension.isTilePresent(tileLocation.x, tileLocation.y))
                                        || (allowBackgroundTileSelection && (backgroundDimension != null) && backgroundDimension.isTilePresent(tileLocation.x >> backgroundZoom, tileLocation.y >> backgroundZoom)))
                                    && (! viewer.isSelectedTile(tileLocation))) {
                                viewer.addSelectedTile(tileLocation);
                            }
                        }
                    }
                    setControlStates();
                    notifyListeners();
                }
                viewer.setSelectedRectangleCorner1(null);
                viewer.setSelectedRectangleCorner2(null);
                selecting = false;
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                viewer.setHighlightedTileLocation(getTileLocation(e.getX(), e.getY()));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                viewer.setHighlightedTileLocation(null);
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                viewer.setHighlightedTileLocation(getTileLocation(e.getX(), e.getY()));
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                viewer.setHighlightedTileLocation(getTileLocation(e.getX(), e.getY()));
                if (selecting) {
                    selectionCorner2 = getTileLocation(e.getX(), e.getY());
                    viewer.setSelectedRectangleCorner1(selectionCorner1);
                    viewer.setSelectedRectangleCorner2(selectionCorner2);
                }
            }
            
            private Point getTileLocation(int x, int y) {
                Point coords = viewer.viewToWorld(x, y);
                return new Point(coords.x >> TILE_SIZE_BITS, coords.y >> TILE_SIZE_BITS);
            }
            
            private boolean selecting;
            private Point selectionCorner1, selectionCorner2;
        };
        viewer.addMouseListener(mouseAdapter);
        viewer.addMouseMotionListener(mouseAdapter);
        jPanel1.add(viewer, BorderLayout.CENTER);
        
        setControlStates();
        
        ActionMap actionMap = getActionMap();
        actionMap.put("zoomIn", new AbstractAction("zoomIn") {
            @Override
            public void actionPerformed(ActionEvent e) {
                int zoom = viewer.getZoom();
                if (zoom < 0) {
                    viewer.setZoom(zoom + 1);
                }
            }
            
            private static final long serialVersionUID = 1L;
        });
        actionMap.put("zoomOut", new AbstractAction("zoomOut") {
            @Override
            public void actionPerformed(ActionEvent e) {
                int zoom = viewer.getZoom();
                if (zoom > -4) {
                    viewer.setZoom(zoom - 1);
                }
            }
            
            private static final long serialVersionUID = 1L;
        });
        
        InputMap inputMap = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, 0), "zoomOut");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0), "zoomOut");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, 0), "zoomIn");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, KeyEvent.SHIFT_DOWN_MASK), "zoomIn");
    }

    public ColourScheme getColourScheme() {
        return colourScheme;
    }

    public void setColourScheme(ColourScheme colourScheme) {
        this.colourScheme = colourScheme;
    }

    public boolean isContourLines() {
        return contourLines;
    }

    public void setContourLines(boolean contourLines) {
        this.contourLines = contourLines;
    }

    public int getContourSeparation() {
        return contourSeparation;
    }

    public void setContourSeparation(int contourSeparation) {
        this.contourSeparation = contourSeparation;
    }

    public LightOrigin getLightOrigin() {
        return lightOrigin;
    }

    public void setLightOrigin(LightOrigin lightOrigin) {
        this.lightOrigin = lightOrigin;
    }

    public Point getCurrentLocation() {
        return viewer.getViewLocation();
    }

    public Dimension getDimension() {
        return dimension;
    }

    public void setDimension(Dimension dimension) {
        selectableTileCoords.clear();
        this.dimension = dimension;
        if (dimension != null) {
            if ((dimension.getAnchor().dim == DIM_NORMAL) && ((dimension.getBorder() == null) || (! dimension.getBorder().isEndless()))) {
                World2 world = dimension.getWorld();
                if (world != null) {
                    int biomeAlgorithm = -1;
                    Platform platform = world.getPlatform();
                    if (platform == JAVA_MCREGION) {
                        biomeAlgorithm = BIOME_ALGORITHM_1_1;
                    } else if (platform == JAVA_ANVIL) { // TODO add support for newer platforms
                        if (dimension.getGenerator().getType() == DEFAULT) {
                            biomeAlgorithm = BIOME_ALGORITHM_1_7_DEFAULT;
                        } else if (dimension.getGenerator().getType() == LARGE_BIOMES) {
                            biomeAlgorithm = BIOME_ALGORITHM_1_7_LARGE;
                        }
                    }
                    if (biomeAlgorithm != -1) {
                        viewer.setTileProvider(-2, new BiomesTileProvider(biomeAlgorithm, dimension.getMinecraftSeed(), colourScheme, 0, true));
                    }
                }
            }

            WPTileProvider tileProvider = new WPTileProvider(dimension, colourScheme, customBiomeManager, hiddenLayers, contourLines, contourSeparation, lightOrigin);
//            tileProvider.setZoom(zoom);
            viewer.setTileProvider(tileProvider);

            final Dimension.Anchor anchor = dimension.getAnchor();
            if (anchor.role == DETAIL) {
                backgroundDimension = dimension.getWorld().getDimension(new Dimension.Anchor(anchor.dim, MASTER, anchor.invert, 0));
                backgroundZoom = 4;
            } else if (anchor.role == CAVE_FLOOR) {
                backgroundDimension = dimension.getWorld().getDimension(new Dimension.Anchor(anchor.dim, DETAIL, anchor.invert, 0));
                backgroundZoom = 0;
            } else {
                backgroundDimension = null;
                backgroundZoom = 0;
            }
            if (backgroundDimension != null) {
                WPTileProvider backgroundProvider = new WPTileProvider(backgroundDimension, colourScheme, customBiomeManager, hiddenLayers, contourLines, contourSeparation, lightOrigin, false, FADE_TO_FIFTY_PERCENT, true);
                viewer.setTileProvider(-1, backgroundProvider);
                viewer.setTileProviderZoom(backgroundProvider, backgroundZoom);
            }
            calculateSelectableTiles();

            if (dimension.getBorder() != null) {
                viewer.setTileProvider(-2, new WPBorderTileProvider(dimension, colourScheme));
            }

            viewer.setMarkerCoords((dimension.getAnchor().dim == DIM_NORMAL) ? dimension.getWorld().getSpawnPoint() : null);
            buttonSpawn.setEnabled(true);
//            moveToCentre();
        } else {
            viewer.removeAllTileProviders();
            viewer.setMarkerCoords(null);
            buttonSpawn.setEnabled(false);
        }
        viewer.clearSelectedTiles();
        setControlStates();
    }

    public void refresh() {
        if ((dimension != null) && (dimension.getAnchor().dim == DIM_NORMAL)) {
            viewer.setMarkerCoords(dimension.getWorld().getSpawnPoint());
        }
        viewer.refresh();
        calculateSelectableTiles();
    }

    public Set<Layer> getHiddenLayers() {
        return hiddenLayers;
    }

    public void setHiddenLayers(Set<Layer> hiddenLayers) {
        this.hiddenLayers = hiddenLayers;
    }

    public CustomBiomeManager getCustomBiomeManager() {
        return customBiomeManager;
    }

    public void setCustomBiomeManager(CustomBiomeManager customBiomeManager) {
        this.customBiomeManager = customBiomeManager;
    }
    
    public Set<Point> getSelectedTiles() {
        return viewer.getSelectedTiles();
    }
    
    public void setSelectedTiles(Set<Point> selectedTiles) {
        viewer.setSelectedTiles(selectedTiles);
        setControlStates();
        notifyListeners();
    }

    public boolean isAllowNonExistentTileSelection() {
        return allowNonExistentTileSelection;
    }

    public void setAllowNonExistentTileSelection(boolean allowNonExistentTileSelection) {
        if (allowNonExistentTileSelection != this.allowNonExistentTileSelection) {
            this.allowNonExistentTileSelection = allowNonExistentTileSelection;
            if (! allowNonExistentTileSelection) {
                clearSelection();
            }
        }
    }

    public boolean isAllowBackgroundTileSelection() {
        return allowBackgroundTileSelection;
    }

    public void setAllowBackgroundTileSelection(boolean allowBackgroundTileSelection) {
        if (allowBackgroundTileSelection != this.allowBackgroundTileSelection) {
            this.allowBackgroundTileSelection = allowBackgroundTileSelection;
            calculateSelectableTiles();
            if (! allowBackgroundTileSelection) {
                clearSelection();
            }
        }
    }

    public void selectAllTiles() {
        boolean selectionChanged = false;
        for (Point tileCoords: selectableTileCoords) {
            if (! viewer.isSelectedTile(tileCoords)) {
                viewer.addSelectedTile(tileCoords);
                selectionChanged = true;
            }
        }
        if (selectionChanged) {
            setControlStates();
            notifyListeners();
        }
    }
    
    public void clearSelection() {
        viewer.clearSelectedTiles();
        setControlStates();
        notifyListeners();
    }
    
    public void invertSelection() {
        final Set<Point> allTiles = selectableTileCoords;
        for (Point tileCoords: allTiles) {
            if (! viewer.isSelectedTile(tileCoords)) {
                viewer.addSelectedTile(tileCoords);
            } else {
                viewer.removeSelectedTile(tileCoords);
            }
        }
        // Deselect all tiles where no tiles exist:
        new HashSet<>(viewer.getSelectedTiles()).stream().filter(tileCoords -> ! allTiles.contains(tileCoords)).forEach(viewer::removeSelectedTile);
        setControlStates();
        notifyListeners();
    }
    
    public void moveToSpawn() {
        viewer.moveToMarker();
    }

    public void moveToCentre() {
        viewer.moveToOrigin();
    }
    
    public void addListener(Listener listener) {
        listeners.add(listener);
    }
    
    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    private void calculateSelectableTiles() {
        selectableTileCoords.clear();
        selectableTileCoords.addAll(dimension.getTileCoords());
        if (allowBackgroundTileSelection && (backgroundDimension != null)) {
            final int scale = 1 << backgroundZoom;
            for (Point tileCoords: backgroundDimension.getTileCoords()) {
                final int scaledTileX = tileCoords.x << backgroundZoom, scaledTileY = tileCoords.y << backgroundZoom;
                for (int dx = 0; dx < scale; dx++) {
                    for (int dy = 0; dy < scale; dy++) {
                        selectableTileCoords.add(new Point(scaledTileX + dx, scaledTileY + dy));
                    }
                }
            }
        }
    }

    private void setControlStates() {
        Set<Point> selectedTiles = viewer.getSelectedTiles();
        boolean allowSelectAll, allowInvertSelection, allowClearSelection;
        if (dimension == null) {
            allowSelectAll = allowInvertSelection = false;
        } else if (selectedTiles.isEmpty()) {
            allowSelectAll = allowInvertSelection = true;
        } else {
            int existingTileCount = dimension.getTileCount(), selectedExistingTileCount = 0;
            for (Point selectedTile: selectedTiles) {
                if (selectableTileCoords.contains(selectedTile)) {
                    selectedExistingTileCount++;
                }
            }
            allowSelectAll = selectedExistingTileCount < existingTileCount;
            allowInvertSelection = true;
        }
        allowClearSelection = ! selectedTiles.isEmpty();
        buttonSelectAll.setEnabled(allowSelectAll);
        buttonInvertSelection.setEnabled(allowInvertSelection);
        buttonClearSelection.setEnabled(allowClearSelection);
    }
    
    private void notifyListeners() {
        for (Listener listener: listeners) {
            listener.selectionChanged(this, viewer.getSelectedTiles());
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

        jPanel1 = new javax.swing.JPanel();
        buttonSpawn = new javax.swing.JButton();
        buttonOrigin = new javax.swing.JButton();
        buttonSelectAll = new javax.swing.JButton();
        buttonInvertSelection = new javax.swing.JButton();
        buttonClearSelection = new javax.swing.JButton();

        jPanel1.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        jPanel1.setLayout(new java.awt.BorderLayout());

        buttonSpawn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/spawn_red.png"))); // NOI18N
        buttonSpawn.setEnabled(false);
        buttonSpawn.addActionListener(this::buttonSpawnActionPerformed);

        buttonOrigin.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/arrow_in.png"))); // NOI18N
        buttonOrigin.addActionListener(this::buttonOriginActionPerformed);

        buttonSelectAll.setText("Select all tiles");
        buttonSelectAll.setEnabled(false);
        buttonSelectAll.addActionListener(this::buttonSelectAllActionPerformed);

        buttonInvertSelection.setText("Invert selection");
        buttonInvertSelection.setEnabled(false);
        buttonInvertSelection.addActionListener(this::buttonInvertSelectionActionPerformed);

        buttonClearSelection.setText("Clear selection");
        buttonClearSelection.setEnabled(false);
        buttonClearSelection.addActionListener(this::buttonClearSelectionActionPerformed);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(buttonSpawn)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonOrigin)
                .addGap(18, 18, Short.MAX_VALUE)
                .addComponent(buttonSelectAll)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonInvertSelection)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonClearSelection))
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 264, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonSpawn)
                    .addComponent(buttonOrigin)
                    .addComponent(buttonSelectAll)
                    .addComponent(buttonInvertSelection)
                    .addComponent(buttonClearSelection)))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void buttonSpawnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSpawnActionPerformed
        moveToSpawn();
    }//GEN-LAST:event_buttonSpawnActionPerformed

    private void buttonOriginActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonOriginActionPerformed
        moveToCentre();
    }//GEN-LAST:event_buttonOriginActionPerformed

    private void buttonSelectAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSelectAllActionPerformed
        selectAllTiles();
    }//GEN-LAST:event_buttonSelectAllActionPerformed

    private void buttonInvertSelectionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonInvertSelectionActionPerformed
        invertSelection();
    }//GEN-LAST:event_buttonInvertSelectionActionPerformed

    private void buttonClearSelectionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonClearSelectionActionPerformed
        clearSelection();
    }//GEN-LAST:event_buttonClearSelectionActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonClearSelection;
    private javax.swing.JButton buttonInvertSelection;
    private javax.swing.JButton buttonOrigin;
    private javax.swing.JButton buttonSelectAll;
    private javax.swing.JButton buttonSpawn;
    private javax.swing.JPanel jPanel1;
    // End of variables declaration//GEN-END:variables

    private final WPTileSelectionViewer viewer = new WPTileSelectionViewer(false, true);
    private final List<Listener> listeners = new ArrayList<>();
    private final Set<Point> selectableTileCoords = new HashSet<>();
    private Dimension dimension, backgroundDimension;
    private ColourScheme colourScheme;
    private Set<Layer> hiddenLayers;
    private int contourSeparation = 10, backgroundZoom;
    private boolean contourLines, allowNonExistentTileSelection = false, allowBackgroundTileSelection = true;
    private TileRenderer.LightOrigin lightOrigin;
    private CustomBiomeManager customBiomeManager;

    private static final long serialVersionUID = 1L;
    
    public interface Listener {
        void selectionChanged(TileSelector tileSelector, Set<Point> newSelection);
    }
}