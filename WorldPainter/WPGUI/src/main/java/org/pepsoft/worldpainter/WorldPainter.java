/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter;

import com.google.common.collect.Sets;
import org.pepsoft.minecraft.MapGenerator;
import org.pepsoft.simplerpc.Message;
import org.pepsoft.simplerpc.RPCClient;
import org.pepsoft.util.MemoryUtils;
import org.pepsoft.worldpainter.Configuration.OverlayType;
import org.pepsoft.worldpainter.Dimension.Anchor;
import org.pepsoft.worldpainter.TileRenderer.LightOrigin;
import org.pepsoft.worldpainter.biomeschemes.CustomBiomeManager;
import org.pepsoft.worldpainter.brushes.BrushShape;
import org.pepsoft.worldpainter.layers.Biome;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.ramps.ColourRamp;
import org.pepsoft.worldpainter.tools.BiomesTileProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.Timer;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.*;

import static java.awt.BasicStroke.CAP_SQUARE;
import static java.awt.BasicStroke.JOIN_MITER;
import static java.util.Collections.singleton;
import static java.util.Collections.unmodifiableSet;
import static org.pepsoft.simplerpc.Constants.DEFAULT_PORT;
import static org.pepsoft.util.AwtUtils.doLaterOnEventThread;
import static org.pepsoft.util.AwtUtils.doOnEventThread;
import static org.pepsoft.worldpainter.Configuration.OverlayType.SCALE_ON_LOAD;
import static org.pepsoft.worldpainter.Constants.*;
import static org.pepsoft.worldpainter.DefaultPlugin.JAVA_ANVIL;
import static org.pepsoft.worldpainter.DefaultPlugin.JAVA_MCREGION;
import static org.pepsoft.worldpainter.Dimension.Anchor.NORMAL_DETAIL;
import static org.pepsoft.worldpainter.Generator.DEFAULT;
import static org.pepsoft.worldpainter.Generator.LARGE_BIOMES;
import static org.pepsoft.worldpainter.TileRenderer.FLUIDS_AS_LAYER;
import static org.pepsoft.worldpainter.TileRenderer.TERRAIN_AS_LAYER;
import static org.pepsoft.worldpainter.WPTileProvider.Effect.FADE_TO_FIFTY_PERCENT;

/**
 *
 * @author pepijn
 */
public class WorldPainter extends WorldPainterView implements MouseMotionListener, PropertyChangeListener, Dimension.Listener {
    public WorldPainter(ColourScheme colourScheme, CustomBiomeManager customBiomeManager) {
        super(false, false);
        this.colourScheme = colourScheme;
        this.customBiomeManager = customBiomeManager;
        setOpaque(true);
        addMouseMotionListener(this);
        enableInputMethods(false);
        startWPLink();
    }

    public WorldPainter(Dimension dimension, ColourScheme colourScheme, CustomBiomeManager customBiomeManager) {
        this(colourScheme, customBiomeManager);
        setDimension(dimension);
    }

    @Override
    public final Dimension getDimension() {
        return dimension;
    }

    @Override
    public final void setDimension(Dimension dimension) {
        setDimension(dimension, true);
    }

    final void setDimension(Dimension dimension, boolean refreshTiles) {
        Dimension oldDimension = this.dimension;
        if (oldDimension != null) {
            oldDimension.removePropertyChangeListener(this);
            if (oldDimension.getAnchor().dim == DIM_NORMAL) {
                oldDimension.getWorld().removePropertyChangeListener("spawnPoint", this);
            }
            oldDimension.removeDimensionListener(this);
            for (Overlay overlay: oldDimension.getOverlays()) {
                overlay.removePropertyChangeListener(this);
            }
        }
        this.dimension = dimension;
        if (dimension != null) {
            drawContours = dimension.isContoursEnabled();
            contourSeparation = dimension.getContourSeparation();
            dimension.addPropertyChangeListener(this);
            if (dimension.getAnchor().dim == DIM_NORMAL) {
                dimension.getWorld().addPropertyChangeListener("spawnPoint", this);
            }
            dimension.addDimensionListener(this);
            for (Overlay overlay: dimension.getOverlays()) {
                overlay.addPropertyChangeListener(this);
            }

            setGridSize(dimension.getGridSize());
            setPaintGrid(dimension.isGridEnabled());
            setLabelScale((int) dimension.getScale());
            
            overlayType = Configuration.getInstance().getOverlayType();
            drawOverlays = dimension.isOverlaysEnabled();
            setMarkerCoords((dimension.getAnchor().dim == DIM_NORMAL) ? dimension.getWorld().getSpawnPoint() : null);
        } else {
            drawOverlays = false;
            setMarkerCoords(null);
        }
        firePropertyChange("dimension", oldDimension, dimension);
        if (refreshTiles) {
            refreshTiles();
        }
    }

    public ColourScheme getColourScheme() {
        return colourScheme;
    }

    public void setColourScheme(ColourScheme colourScheme) {
        this.colourScheme = colourScheme;
        refreshTiles();
    }

    public boolean isDrawBrush() {
        return drawBrush;
    }

    public void setDrawBrush(boolean drawBrush) {
        if (drawBrush != this.drawBrush) {
            this.drawBrush = drawBrush;
            firePropertyChange("drawBrush", !drawBrush, drawBrush);
            repaintWorld(getBrushBounds());
        }
    }

    public boolean isDrawViewDistance() {
        return drawViewDistance;
    }

    public void setDrawViewDistance(boolean drawViewDistance) {
        if (drawViewDistance != this.drawViewDistance) {
            this.drawViewDistance = drawViewDistance;
            firePropertyChange("drawViewDistance", !drawViewDistance, drawViewDistance);
            final int scaledRadius = (dimension != null) ? (int) Math.ceil(viewDistance / dimension.getScale()) : viewDistance;
            repaintWorld(mouseX - scaledRadius, mouseY - scaledRadius, (2 * scaledRadius) + 1, (2 * scaledRadius) + 1);
        }
    }

    public int getViewDistance() {
        return viewDistance;
    }

    public void setViewDistance(int viewDistance) {
        if (viewDistance != this.viewDistance) {
            final int oldViewDistance = this.viewDistance;
            this.viewDistance = viewDistance;
            firePropertyChange("viewDistance", oldViewDistance, viewDistance);
            if (drawViewDistance) {
                final int largestDistance = Math.max(oldViewDistance, viewDistance);
                final int scaledRadius = (dimension != null) ? (int) Math.ceil(largestDistance / dimension.getScale()) : largestDistance;
                repaintWorld(mouseX - scaledRadius, mouseY - scaledRadius, (2 * scaledRadius) + 1, (2 * scaledRadius) + 1);
            }
        }
    }

    public boolean isDrawWalkingDistance() {
        return drawWalkingDistance;
    }

    public void setDrawWalkingDistance(boolean drawWalkingDistance) {
        if (drawWalkingDistance != this.drawWalkingDistance) {
            this.drawWalkingDistance = drawWalkingDistance;
            firePropertyChange("drawWalkingDistance", !drawWalkingDistance, drawWalkingDistance);
            final int scaledRadius = (dimension != null) ? (int) Math.ceil(DAY_NIGHT_WALK_DISTANCE_RADIUS / dimension.getScale()) : DAY_NIGHT_WALK_DISTANCE_RADIUS;
            repaintWorld(mouseX - scaledRadius, mouseY - scaledRadius, (2 * scaledRadius) + 1, (2 * scaledRadius) + 1);
        }
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        int oldRadius = this.radius;
        int oldEffectiveRadius = this.effectiveRadius;
        this.radius = radius;
        if ((brushShape == BrushShape.CIRCLE) || ((brushRotation % 90) == 0)) {
            effectiveRadius = radius;
        } else {
            double a = brushRotation / 180.0 * Math.PI;
            effectiveRadius = (int) Math.ceil(Math.abs(Math.sin(a)) * radius + Math.abs(Math.cos(a)) * radius);
        }
        firePropertyChange("radius", oldRadius, radius);
        if (drawBrush && (brushShape != BrushShape.CUSTOM)) {
            int largestRadius = Math.max(oldEffectiveRadius, effectiveRadius);
            int diameter = largestRadius * 2 + 1;
            repaintWorld(mouseX - largestRadius, mouseY - largestRadius, diameter, diameter);
        }
    }

    public BrushShape getBrushShape() {
        return brushShape;
    }

    public void setBrushShape(BrushShape brushShape) {
        if (brushShape != this.brushShape) {
            BrushShape oldBrushShape = this.brushShape;
            Rectangle oldBounds = getBrushBounds();
            this.brushShape = brushShape;
            if ((brushShape == BrushShape.CIRCLE) || (brushShape == BrushShape.CUSTOM) || ((brushRotation % 90) == 0)) {
                effectiveRadius = radius;
            } else {
                double a = brushRotation / 180.0 * Math.PI;
                effectiveRadius = (int) Math.ceil(Math.abs(Math.sin(a)) * radius + Math.abs(Math.cos(a)) * radius);
            }
            firePropertyChange("brushShape", oldBrushShape, brushShape);
            if (drawBrush) {
                repaintWorld(getBrushBounds().union(oldBounds));
            }
        }
    }

    public Shape getCustomBrushShape() {
        return customBrushShape;
    }

    public void setCustomBrushShape(Shape customBrushShape) {
        final Shape oldCustomBrushShape = this.customBrushShape;
        final Rectangle oldBrushBounds = getBrushBounds();
        this.customBrushShape = customBrushShape;
        if ((drawBrush) && (brushShape == BrushShape.CUSTOM)) {
            repaintWorld(getBrushBounds().union(oldBrushBounds));
        }
        firePropertyChange("customBrushShape", oldCustomBrushShape, customBrushShape);
    }

    public int getContourSeparation() {
        return contourSeparation;
    }

    public void setContourSeparation(int contourSeparation) {
        if (contourSeparation != this.contourSeparation) {
            int oldContourSeparation = this.contourSeparation;
            this.contourSeparation = contourSeparation;
            refreshTiles();
            firePropertyChange("contourSeparation", oldContourSeparation, contourSeparation);
        }
    }

    public boolean isDrawContours() {
        return drawContours;
    }

    public void setDrawContours(boolean drawContours) {
        if (drawContours != this.drawContours) {
            this.drawContours = drawContours;
            refreshTiles();
            firePropertyChange("drawContours", ! drawContours, drawContours);
        }
    }

    public ColourRamp getColourRamp() {
        return colourRamp;
    }

    /**
     * Set or remove the colour ramp.
     *
     * @param colourRamp The colour ramp to set. May be {@code null} to remove the colour ramp.
     * @return A boolean which indicates whether the tiles have been refreshed as a result of changing the colour ramp.
     */
    public boolean setColourRamp(ColourRamp colourRamp) {
        if (! Objects.equals(colourRamp, this.colourRamp)) {
            final ColourRamp oldColourRamp = this.colourRamp;
            this.colourRamp = colourRamp;
            if ((hiddenLayers != null) && hiddenLayers.contains(TERRAIN_AS_LAYER)) {
                refreshTiles();
            }
            firePropertyChange("colourRamp", oldColourRamp, colourRamp);
        }
        return (hiddenLayers != null) && hiddenLayers.contains(TERRAIN_AS_LAYER);
    }

    public void refreshBrush() {
        Point mousePos = getMousePosition();
        if (mousePos != null) {
            mouseMoved(new MouseEvent(this, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, mousePos.x, mousePos.y, 0, false));
        }
    }

    public void setHiddenLayers(Set<Layer> hiddenLayers) {
        final Set<Layer> oldHiddenLayers = new HashSet<>(this.hiddenLayers);
        this.hiddenLayers.clear();
        if (hiddenLayers != null) {
            this.hiddenLayers.addAll(hiddenLayers);
        }
        final Set<Layer> difference = Sets.symmetricDifference(oldHiddenLayers, this.hiddenLayers);
        if (! difference.isEmpty()) {
            if (dimension != null) {
                tileProvider.setHiddenLayers(this.hiddenLayers);
                if ((difference.contains(TERRAIN_AS_LAYER)) || (difference.contains(FLUIDS_AS_LAYER))) {
                    refreshTiles();
                } else if (!difference.isEmpty()) {
                    refreshTilesForLayers(difference, true);
                }
            }
            firePropertyChange("hiddenLayers", oldHiddenLayers, hiddenLayers);
        }
    }

    public Set<Layer> getHiddenLayers() {
        return unmodifiableSet(hiddenLayers);
    }

    public void refreshTiles() {
        if (dimension != null) {
            int biomeAlgorithm = -1;
            long minecraftSeed = -1;
            final Anchor anchor = dimension.getAnchor();
            if (drawBiomes
                    && (anchor.equals(NORMAL_DETAIL))
                    && ((dimension.getBorder() == null) || (! dimension.getBorder().isEndless()))) {
                final World2 world = dimension.getWorld();
                if (world != null) {
                    final Platform platform = world.getPlatform();
                    if (platform == JAVA_MCREGION) {
                        biomeAlgorithm = BIOME_ALGORITHM_1_1;
                        minecraftSeed = dimension.getMinecraftSeed();
                    } else if (platform == JAVA_ANVIL) { // TODO add support for newer platforms
                        minecraftSeed = dimension.getMinecraftSeed();
                        final MapGenerator generator = dimension.getGenerator();
                        if (generator.getType() == DEFAULT) {
                            biomeAlgorithm = BIOME_ALGORITHM_1_7_DEFAULT;
                        } else if (generator.getType() == LARGE_BIOMES) {
                            biomeAlgorithm = BIOME_ALGORITHM_1_7_LARGE;
                        }
                    }
                }
            }
            if (biomeAlgorithm != -1) {
                final BiomesTileProvider biomesTileProvider = new BiomesTileProvider(biomeAlgorithm, minecraftSeed, colourScheme, 0, true);
                setTileProvider(LAYER_BIOMES, biomesTileProvider);
            } else {
                removeTileProvider(LAYER_BIOMES);
            }

            tileProvider = new WPTileProvider(dimension, colourScheme, customBiomeManager, hiddenLayers, drawContours, contourSeparation, lightOrigin, true, null, backgroundDimension == null, colourRamp);
            setTileProvider(LAYER_DETAILS, tileProvider);

            if (backgroundDimension != null) {
                backgroundTileProvider = new WPTileProvider(backgroundDimension, colourScheme, customBiomeManager, hiddenLayers, false, contourSeparation, lightOrigin, false, FADE_TO_FIFTY_PERCENT, true, colourRamp);
                setTileProvider(LAYER_BACKGROUND, backgroundTileProvider);
                setTileProviderZoom(backgroundTileProvider, backgroundDimensionZoom);
            } else {
                removeTileProvider(LAYER_BACKGROUND);
            }

            if (drawBorders && (dimension.getBorder() != null)) {
                setTileProvider(LAYER_BORDER, new WPBorderTileProvider(dimension, colourScheme));
            } else {
                removeTileProvider(LAYER_BORDER);
            }
        } else {
            if (getTileProviderCount() > 0) {
                removeAllTileProviders();
            }
            tileProvider = null;
        }
    }

    public void refreshTilesForLayer(Layer layer, boolean evenIfHidden) {
        refreshTilesForLayers(singleton(layer), evenIfHidden);
    }

    public void refreshTilesForLayers(Set<Layer> layers, boolean evenIfHidden) {
        if ((hiddenLayers.containsAll(layers) && (! evenIfHidden)) || (dimension == null)) {
            return;
        }
        final long start = System.currentTimeMillis();
        Set<Point> coords = new HashSet<>();
        if (getZoom() < 0) {
            final int shift = -getZoom();
            for (Tile tile: dimension.getTiles()) {
                for (Layer layer: layers) {
                    if (tile.hasLayer(layer)) {
                        coords.add(new Point(tile.getX() >> shift, tile.getY() >> shift));
                        break;
                    }
                }
            }
        } else {
            for (Tile tile: dimension.getTiles()) {
                for (Layer layer: layers) {
                    if (tile.hasLayer(layer)) {
                        coords.add(new Point(tile.getX(), tile.getY()));
                        break;
                    }
                }
            }
        }
        if (! coords.isEmpty()) {
            refresh(tileProvider, coords);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Refreshing {} tiles for layers {} took {} ms", coords.size(), layers, System.currentTimeMillis() - start);
        }
    }
    
    @Override
    public void updateStatusBar(int x, int y) {
        App.getInstance().updateStatusBar(x, y);
    }

    public BufferedImage getImage() {
        if (dimension == null) {
            return null;
        }
        TileRenderer tileRenderer = new TileRenderer(dimension, colourScheme, customBiomeManager, 0, true, colourRamp);
        tileRenderer.setContourLines(drawContours);
        tileRenderer.setContourSeparation(contourSeparation);
        tileRenderer.setHiddenLayers(hiddenLayers);
        tileRenderer.setLightOrigin(lightOrigin);
        int xOffset = dimension.getLowestX(), yOffset = dimension.getLowestY();
        BufferedImage image = new BufferedImage(dimension.getWidth() << TILE_SIZE_BITS, dimension.getHeight() << TILE_SIZE_BITS, BufferedImage.TYPE_INT_ARGB);
        for (Tile tile: dimension.getTiles()) {
            tileRenderer.renderTile(tile, image, (tile.getX() - xOffset) << TILE_SIZE_BITS, (tile.getY() - yOffset) << TILE_SIZE_BITS);
        }
        return image;
    }

    public void rotateLightLeft() {
        lightOrigin = lightOrigin.left();
        refreshTiles();
    }

    public void rotateLightRight() {
        lightOrigin = lightOrigin.right();
        refreshTiles();
    }
    
    public LightOrigin getLightOrigin() {
        return lightOrigin;
    }
    
    public void setLightOrigin(LightOrigin lightOrigin) {
        if (lightOrigin == null) {
            throw new NullPointerException();
        }
        if (lightOrigin != this.lightOrigin) {
            this.lightOrigin = lightOrigin;
            refreshTiles();
        }
    }
    
    public void moveToSpawn() {
        if ((dimension != null) && (dimension.getAnchor().dim == DIM_NORMAL)) {
            moveToMarker();
        }
    }

    public Point getViewCentreInWorldCoords() {
        return new Point(getViewX(), getViewY());
    }

    public int getBrushRotation() {
        return brushRotation;
    }
    
    public void setBrushRotation(int brushRotation) {
        int oldBrushRotation = this.brushRotation;
        int oldEffectiveRadius = effectiveRadius;
        this.brushRotation = brushRotation;
        if ((brushShape == BrushShape.CIRCLE) || ((brushRotation % 90) == 0)) {
            effectiveRadius = radius;
        } else {
            double a = brushRotation / 180.0 * Math.PI;
            effectiveRadius = (int) Math.ceil(Math.abs(Math.sin(a)) * radius + Math.abs(Math.cos(a)) * radius);
        }
        firePropertyChange("brushRotation", oldBrushRotation, brushRotation);
        if (drawBrush && (brushShape != BrushShape.CIRCLE)) {
            int largestRadius = Math.max(oldEffectiveRadius, effectiveRadius);
            int diameter = largestRadius * 2 + 1;
            repaintWorld(mouseX - largestRadius, mouseY - largestRadius, diameter, diameter);
        }
    }
    
    public void minecraftSeedChanged(Dimension dimension, long newSeed) {
        if ((! isInhibitUpdates()) && (! hiddenLayers.contains(Biome.INSTANCE))) {
            refreshTiles();
        }
    }

    public boolean isDrawMinecraftBorder() {
        return drawMinecraftBorder;
    }

    public void setDrawMinecraftBorder(boolean drawMinecraftBorder) {
        if (drawMinecraftBorder != this.drawMinecraftBorder) {
            this.drawMinecraftBorder = drawMinecraftBorder;
            firePropertyChange("drawMinecraftBorder", ! drawMinecraftBorder, drawMinecraftBorder);
            repaint();
        }
    }

    public boolean isDrawBorders() {
        return drawBorders;
    }

    public void setDrawBorders(boolean drawBorders) {
        if (drawBorders != this.drawBorders) {
            this.drawBorders = drawBorders;
            firePropertyChange("drawBorders", ! drawBorders, drawBorders);
            refreshTiles();
        }
    }

    public boolean isDrawBiomes() {
        return drawBiomes;
    }

    public void setDrawBiomes(boolean drawBiomes) {
        if (drawBiomes != this.drawBiomes) {
            this.drawBiomes = drawBiomes;
            if ((dimension != null) && (dimension.getAnchor().dim == DIM_NORMAL)) {
                refreshTiles();
            }
            firePropertyChange("drawBiomes", ! drawBiomes, drawBiomes);
        }
    }

    @Override
    public Point getMousePosition() throws HeadlessException {
        Point translation = new Point(0, 0);
        Component component = this;
        while (component != null) {
            Point mousePosition = (component == this) ? super.getMousePosition() : component.getMousePosition();
            if (mousePosition != null) {
                mousePosition.translate(-translation.x, -translation.y);
                return mousePosition;
            } else {
                translation.translate(component.getX(), component.getY());
                component = component.getParent();
            }
        }
        return null;
    }

    public Dimension getBackgroundDimension() {
        return backgroundDimension;
    }

    public void setBackgroundDimension(Dimension backgroundDimension, int zoomLevel, WPTileProvider.Effect effect) {
        this.backgroundDimension = backgroundDimension;
        backgroundDimensionZoom = zoomLevel;
        refreshTiles();
    }

    // MouseMotionListener

    @Override
    public void mouseDragged(MouseEvent e) {
        int oldMouseX = mouseX;
        int oldMouseY = mouseY;
        Point mouseInWorld = viewToWorld(e.getPoint());
        mouseX = mouseInWorld.x;
        mouseY = mouseInWorld.y;
        if ((mouseX == oldMouseX) && (mouseY == oldMouseY)) {
            return;
        }
        Rectangle repaintArea = null; // The repaint area in world coordinates relative to the mouse position
        if (drawBrush) {
            if (brushShape != BrushShape.CUSTOM) {
                repaintArea = new Rectangle(-effectiveRadius, -effectiveRadius, effectiveRadius * 2 + 1, effectiveRadius * 2 + 1);
            } else {
                repaintArea = customBrushShape.getBounds();
            }
        }
        if (dimension != null) {
            if (drawViewDistance) {
                final int scaledRadius = (int) Math.ceil(viewDistance / dimension.getScale());
                Rectangle viewDistanceArea = new Rectangle(-scaledRadius, -scaledRadius, scaledRadius * 2, scaledRadius * 2);
                if (repaintArea != null) {
                    repaintArea = repaintArea.union(viewDistanceArea);
                } else {
                    repaintArea = viewDistanceArea;
                }
            }
            if (drawWalkingDistance) {
                final int scaledRadius = (int) Math.ceil(viewDistance / dimension.getScale());
                Rectangle walkingDistanceArea = new Rectangle(-scaledRadius, -scaledRadius, scaledRadius * 2, scaledRadius * 2);
                if (repaintArea != null) {
                    repaintArea = repaintArea.union(walkingDistanceArea);
                } else {
                    repaintArea = walkingDistanceArea;
                }
            }
        }
        if (repaintArea != null) {
            Rectangle oldRectangle = new Rectangle(oldMouseX + repaintArea.x, oldMouseY + repaintArea.y, repaintArea.width, repaintArea.height);
            Rectangle newRectangle = new Rectangle(mouseX + repaintArea.x, mouseY + repaintArea.y, repaintArea.width, repaintArea.height);
            if (oldRectangle.intersects(newRectangle)) {
                repaintWorld(oldRectangle.union(newRectangle));
            } else {
                // Two separate repaints to avoid having to repaint a huge area
                // just because the cursor jumps a large distance for some
                // reason
                repaintWorld(oldRectangle);
                SwingUtilities.invokeLater(() -> repaintWorld(newRectangle));
            }
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseDragged(e);
    }

    // PropertyChangeListener

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ((evt.getSource() == dimension.getWorld()) && evt.getPropertyName().equals("spawnPoint")) {
            setMarkerCoords((Point) evt.getNewValue());
        } else if (evt.getSource() == dimension) {
            if ("overlaysEnabled".equals(evt.getPropertyName())) {
                drawOverlays = ((Boolean) evt.getNewValue());
                repaint();
            }
        } else if (evt.getSource() instanceof Overlay) {
            if (evt.getPropertyName().equals("image")) {
                // Do nothing
            } else if ((evt.getPropertyName().equals("scale")) && (Configuration.getInstance().getOverlayType() == SCALE_ON_LOAD)) {
                // The overlay type is set to scale on load, so since the scale has changed the image has to be reloaded
                ((Overlay) evt.getSource()).setImage(null);
                if (drawOverlays) {
                    // Since reloading an image is slow, wait with triggering it in case the value is still adjusting
                    scheduleRepaint(250);
                }
            } else if (drawOverlays) {
                repaint();
            }
        }
    }

    // Dimension.Listener

    @Override public void tilesAdded(Dimension dimension, Set<Tile> tiles) {}
    @Override public void tilesRemoved(Dimension dimension, Set<Tile> tiles) {}

    @Override
    public void overlayAdded(Dimension dimension, int index, Overlay overlay) {
        overlay.addPropertyChangeListener(this);
        if (drawOverlays) {
            repaint();
        }
    }

    @Override
    public void overlayRemoved(Dimension dimension, int index, Overlay overlay) {
        overlay.removePropertyChangeListener(this);
        if (drawOverlays) {
            repaint();
        }
    }

    long getOverlayImageSize() {
        long total = 0L;
        if (dimension != null) {
            for (Overlay overlay: dimension.getOverlays()) {
                total += (overlay.getImage() != null) ? MemoryUtils.getSize(overlay.getImage(), Collections.emptySet()) : 0L;
            }
        }
        return total;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        // Paint the tiles, grid and markers:
        super.paintComponent(g);

        if (dimension != null) {
            // Paint anything else:
            final Graphics2D g2 = (Graphics2D) g;
            final Color savedColour = g2.getColor();
            final Object savedAAValue = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
//            final Object savedInterpolationValue = g2.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
            final Stroke savedStroke = g2.getStroke();
            final AffineTransform savedTransform = g2.getTransform();
            final Font savedFont = g2.getFont();
            try {
                if (drawMinecraftBorder && (dimension.getWorld() != null)) {
                    drawMinecraftBorderIfNecessary(g2, dimension.getWorld().getBorderSettings());
                }

                // Switch to world coordinate system
                final float scale = transformGraphics(g2);
                if (drawOverlays) {
                    drawOverlays(g2);
                }
                if (drawBrush || drawViewDistance || drawWalkingDistance) {
                    g2.setColor(Color.BLACK);
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    drawBrushEtc(g2, scale, false);
                    g2.setColor(Color.WHITE);
                    drawBrushEtc(g2, scale, true);
                }
            } finally {
                g2.setColor(savedColour);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, savedAAValue);
//                if (savedInterpolationValue != null) {
//                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, savedInterpolationValue);
//                }
                g2.setStroke(savedStroke);
                g2.setTransform(savedTransform);
                g2.setFont(savedFont);
            }
        }
    }

    private void drawBrushEtc(Graphics2D g2, float scale, boolean dashed) {
        final float onePixel = 1 / scale;
        if (! dashed) {
            g2.setStroke(new BasicStroke(onePixel));
        }
        if (drawBrush) {
            if (dashed) {
                g2.setStroke(new BasicStroke(onePixel, CAP_SQUARE, JOIN_MITER, 10.0f, new float[] { 4 * onePixel, 6 * onePixel }, 0));
            }
            final int diameter = radius * 2 + 1;
            switch (brushShape) {
                case CIRCLE:
                    g2.drawOval(mouseX - radius, mouseY - radius, diameter, diameter);
                    break;
                case SQUARE:
                    if (brushRotation % 90 == 0) {
                        g2.drawRect(mouseX - radius, mouseY - radius, diameter, diameter);
                    } else {
                        AffineTransform existingTransform = g2.getTransform();
                        try {
                            if (scale > 1.0f) {
                                g2.rotate(brushRotation / 180.0 * Math.PI, mouseX + 0.5, mouseY + 0.5);
                            } else {
                                g2.rotate(brushRotation / 180.0 * Math.PI, mouseX, mouseY);
                            }
                            g2.drawRect(mouseX - radius, mouseY - radius, diameter, diameter);
                        } finally {
                            g2.setTransform(existingTransform);
                        }
                    }
                    break;
                case BITMAP:
                    final int arrowSize = radius / 2;
                    if (brushRotation == 0) {
                        g2.drawRect(mouseX - radius, mouseY - radius, diameter, diameter);
                        if (arrowSize > 0) {
                            g2.drawLine(mouseX, mouseY - radius, mouseX - arrowSize, mouseY - radius + arrowSize);
                            g2.drawLine(mouseX - arrowSize, mouseY - radius + arrowSize, mouseX + arrowSize + 1, mouseY - radius + arrowSize);
                            g2.drawLine(mouseX + arrowSize + 1, mouseY - radius + arrowSize, mouseX + 1, mouseY - radius);
                        }
                    } else {
                        AffineTransform existingTransform = g2.getTransform();
                        try {
                            if (scale > 1.0f) {
                                g2.rotate(brushRotation / 180.0 * Math.PI, mouseX + 0.5, mouseY + 0.5);
                            } else {
                                g2.rotate(brushRotation / 180.0 * Math.PI, mouseX, mouseY);
                            }
                            g2.drawRect(mouseX - radius, mouseY - radius, diameter, diameter);
                            if (arrowSize > 0) {
                                g2.drawLine(mouseX, mouseY - radius, mouseX - arrowSize, mouseY - radius + arrowSize);
                                g2.drawLine(mouseX - arrowSize, mouseY - radius + arrowSize, mouseX + arrowSize + 1, mouseY - radius + arrowSize);
                                g2.drawLine(mouseX + arrowSize + 1, mouseY - radius + arrowSize, mouseX + 1, mouseY - radius);
                            }
                        } finally {
                            g2.setTransform(existingTransform);
                        }
                    }
                    break;
                case CUSTOM:
                    AffineTransform existingTransform = g2.getTransform();
                    try {
                        g2.translate(mouseX, mouseY);
                        g2.draw(customBrushShape);
                    } finally {
                        g2.setTransform(existingTransform);
                    }
            }
        }
        if (drawViewDistance) {
            if (dashed) {
                g2.setStroke(new BasicStroke(onePixel, CAP_SQUARE, JOIN_MITER, 10.0f, new float[] { 9 * onePixel, 11 * onePixel }, 0));
            }
            final int scaledRadius = (int) Math.ceil(viewDistance / dimension.getScale());
            g2.drawOval(mouseX - scaledRadius, mouseY - scaledRadius, scaledRadius * 2, scaledRadius * 2);
        }
        if (drawWalkingDistance) {
            if (dashed) {
                g2.setStroke(new BasicStroke(onePixel, CAP_SQUARE, JOIN_MITER, 10.0f, new float[] { 19 * onePixel, 21 * onePixel }, 0));
            }
            int scaledRadius = (int) Math.ceil(DAY_NIGHT_WALK_DISTANCE_RADIUS / dimension.getScale());
            g2.drawOval(mouseX - scaledRadius, mouseY - scaledRadius, scaledRadius * 2, scaledRadius * 2);
            setFont(NORMAL_FONT.deriveFont(10 * onePixel));
            g2.drawString("day + night", mouseX - scaledRadius + onePixel * 3, mouseY);
            scaledRadius = (int) Math.ceil(DAY_WALK_DISTANCE_RADIUS / dimension.getScale());
            g2.drawOval(mouseX - scaledRadius, mouseY - scaledRadius, scaledRadius * 2, scaledRadius * 2);
            g2.drawString("1 day", mouseX - scaledRadius + onePixel * 3, mouseY);
            scaledRadius = (int) Math.ceil(FIVE_MINUTE_WALK_DISTANCE_RADIUS / dimension.getScale());
            g2.drawOval(mouseX - scaledRadius, mouseY - scaledRadius, scaledRadius * 2, scaledRadius * 2);
            g2.drawString("5 min.", mouseX - scaledRadius + onePixel * 3, mouseY);
        }
    }

    /**
     * Get the rectangular bounds of the current brush shape.
     *
     * @return The bounds of the brush.
     */
    private Rectangle getBrushBounds() {
        if (brushShape == BrushShape.CUSTOM) {
            Rectangle bounds = customBrushShape.getBounds();
            bounds.translate(mouseX, mouseY);
            return bounds;
        } else {
            return new Rectangle(mouseX - effectiveRadius, mouseY - effectiveRadius, effectiveRadius * 2 + 1, effectiveRadius * 2 + 1);
        }
    }

    private void loadOverlay(Overlay overlay) {
        File file = overlay.getFile();
        if ((file != null) && file.isFile()) {
            if (file.canRead()) {
                logger.info("Loading image");
                BufferedImage overlayImage;
                try {
                    overlayImage = ImageIO.read(file);
                } catch (IOException e) {
                    logger.error("I/O error while loading image " + file ,e);
                    doLaterOnEventThread(() -> JOptionPane.showMessageDialog(this, "An error occurred while loading the overlay image.\nIt may not be a valid or supported image file, or the file may be corrupted.", "Error Loading Image", JOptionPane.ERROR_MESSAGE));
                    overlay.setEnabled(false);
                    return;
                } catch (RuntimeException | Error e) {
                    logger.error(e.getClass().getSimpleName() + " while loading image " + file ,e);
                    doLaterOnEventThread(() -> JOptionPane.showMessageDialog(this, "An error occurred while loading the overlay image.\nThere may not be enough available memory, or the image may be too large.", "Error Loading Image", JOptionPane.ERROR_MESSAGE));
                    overlay.setEnabled(false);
                    return;
                }
                if (overlayImage != null) {
                    switch (overlayType) {
                        case OPTIMISE_ON_LOAD:
                            // "Scale" to 100%, which optimises the image for the screen environment
                            overlayImage = scaleImage(overlayImage, getGraphicsConfiguration(), 1.0f);
                            break;
                        case SCALE_ON_LOAD:
                            overlayImage = scaleImage(overlayImage, getGraphicsConfiguration(), overlay.getScale());
                            break;
                    }
                } else {
                    logger.error("Image overlay file " + file + " did not contain a recognisable image");
                    doLaterOnEventThread(() -> JOptionPane.showMessageDialog(this, "Image overlay file did not contain a recognisable image. It may have been corrupted.\n" + file, "Error Loading Image", JOptionPane.ERROR_MESSAGE));
                }
                if (overlayImage != null) {
                    overlay.setImage(overlayImage);
                } else {
                    // The loading, scaling or optimisation failed
                    overlay.setEnabled(false);
                }
            } else {
                doLaterOnEventThread(() -> JOptionPane.showMessageDialog(this, "Access denied to overlay image\n" + file, "Error Enabling Overlay", JOptionPane.ERROR_MESSAGE));
                overlay.setEnabled(false);
            }
        } else {
            doLaterOnEventThread(() -> JOptionPane.showMessageDialog(this, "Overlay image file not found\n" + file, "Error Enabling Overlay", JOptionPane.ERROR_MESSAGE));
            overlay.setEnabled(false);
        }
    }

    private BufferedImage scaleImage(BufferedImage image, GraphicsConfiguration graphicsConfiguration, float scale) {
        try {
            final boolean alpha = image.getColorModel().hasAlpha();
            if (scale == 1.0f) {
                logger.info("Optimising image");
                final BufferedImage optimumImage = graphicsConfiguration.createCompatibleImage(image.getWidth(), image.getHeight(), alpha ? Transparency.TRANSLUCENT : Transparency.OPAQUE);
                final Graphics2D g2 = optimumImage.createGraphics();
                try {
                    g2.drawImage(image, 0, 0, null);
                } finally {
                    g2.dispose();
                }
                return optimumImage;
            } else {
                logger.info("Scaling image");
                final int width = Math.round(image.getWidth() * scale);
                final int height = Math.round(image.getHeight() * scale);
                final BufferedImage optimumImage = graphicsConfiguration.createCompatibleImage(width, height, alpha ? Transparency.TRANSLUCENT : Transparency.OPAQUE);
                final Graphics2D g2 = optimumImage.createGraphics();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    g2.drawImage(image, 0, 0, width, height, null);
                } finally {
                    g2.dispose();
                }
                return optimumImage;
            }
        } catch (RuntimeException | Error e) {
            logger.error(e.getClass().getSimpleName() + " while scaling image of size " + image.getWidth() + "x" + image.getHeight() + " and type " + image.getType() + " to " + scale + "%", e);
            doLaterOnEventThread(() -> JOptionPane.showMessageDialog(null, "An error occurred while " + ((scale == 100) ? "optimising" : "scaling") + " the overlay image.\nThere may not be enough available memory, or the image may be too large.", "Error " + ((scale == 100) ? "Optimising" : "Scaling") + " Image", JOptionPane.ERROR_MESSAGE));
            return null;
        }
    }

    private void drawMinecraftBorderIfNecessary(Graphics2D g2, World2.BorderSettings borderSettings) {
        final int size = borderSettings.getSize(), radius = size / 2;
        final Rectangle border = worldToView(borderSettings.getCentreX() - radius, borderSettings.getCentreY() - radius, size, size);
        Rectangle clip = g2.getClipBounds();
        if ((border.x >= clip.x) || (border.y >= clip.y) || ((border.x + border.width) < (clip.x + clip.width)) || ((border.y + border.height) < (clip.y + clip.height))) {
            g2.setColor(Color.RED);
            g2.setStroke(new BasicStroke(1, CAP_SQUARE, JOIN_MITER, 10.0f, new float[] { 3, 3 }, 0));
            if ((border.width < 5000) && (border.height < 5000)) {
                // If it's small enough performance of drawing it at once is fine
                g2.drawRect(border.x, border.y, border.width, border.height);
            } else if (clip.intersects(border)) {
                // For very large rectangles performance of drawing it as a rect
                // tanks, so draw each line individually, constraining the
                // lengths to the clip bounds
                g2.drawLine(border.x, Math.max(border.y, clip.y), border.x, Math.min(border.y + border.height, clip.y + clip.height));
                g2.drawLine(Math.max(border.x, clip.x), border.y + border.height, Math.min(border.x + border.width, clip.x + clip.width), border.y + border.height);
                g2.drawLine(border.x + border.width, Math.min(border.y + border.height, clip.y + clip.height), border.x + border.width, Math.max(border.y, clip.y));
                g2.drawLine(Math.min(border.x + border.width, clip.x + clip.width), border.y, Math.max(border.x, clip.x), border.y);
            }
        }
    }

    /**
     * Repaint an area in world coordinates, plus a few pixels extra to
     * compensate for sloppiness in painting the brush.
     * 
     * @param x The x coordinate of the area to repaint, in world coordinates.
     * @param y The y coordinate of the area to repaint, in world coordinates.
     * @param width The width of the area to repaint, in world coordinates.
     * @param height The height of the area to repaint, in world coordinates.
     */
    private void repaintWorld(int x, int y, int width, int height) {
        Rectangle area = worldToView(x, y, width, height);
        repaint(area.x - 2, area.y - 2, area.width + 4, area.height + 4);
    }

    /**
     * Repaint an area in world coordinates, plus a few pixels extra to
     * compensate for sloppiness in painting the brush.
     *
     * @param area The the area to repaint, in world coordinates.
     */
    private void repaintWorld(Rectangle area) {
        area = worldToView(area);
        repaint(area.x - 2, area.y - 2, area.width + 4, area.height + 4);
    }

    private void drawOverlays(Graphics2D g2) {
        if (dimension.getOverlays().isEmpty()) {
            return;
        }
        Composite savedComposite = g2.getComposite();
        try {
            for (Overlay overlay: dimension.getOverlays()) {
                final float overlayTransparency = overlay.getTransparency();
                if ((! overlay.isEnabled()) || (overlay.getTransparency() == 1.0f)) {
                    // Not enabled, or fully transparent
                    continue;
                } else {
                    if (overlay.getImage() == null) {
                        loadOverlay(overlay);
                    }
                    final BufferedImage overlayImage = overlay.getImage();
                    if (overlayImage == null) {
                        // If it is _still_ null then loading has failed
                        continue;
                    }

                    // Translucent or fully opaque
                    if (overlayTransparency > 0.0f) {
                        // Translucent
                        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f - overlayTransparency));
                    }
                    final float overlayScale = overlay.getScale();
                    final int overlayOffsetX = overlay.getOffsetX(), overlayOffsetY = overlay.getOffsetY();
                    if ((overlayType == SCALE_ON_LOAD) || (overlayScale == 1.0f)) {
                        // 1:1 scale, or the image has already been scaled on loading
                        g2.drawImage(overlayImage, overlayOffsetX, overlayOffsetY, null);
                    } else {
                        final int width = Math.round(overlayImage.getWidth() * overlayScale);
                        final int height = Math.round(overlayImage.getHeight() * overlayScale);
                        final Object savedInterpolation = g2.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
                        try {
                            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                            g2.drawImage(overlayImage, overlayOffsetX, overlayOffsetY, width, height, null);
                        } finally {
                            if (savedInterpolation != null) {
                                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, savedInterpolation);
                            }
                        }
                    }
                }
            }
        } finally {
            g2.setComposite(savedComposite);
        }
    }

    /**
     * Schedules a {@link #repaint()} for {@code delay} ms after now. If a repaint was already scheduled, that is
     * postponed.
     *
     * @param delay The number of ms to delay the repaint.
     */
    private void scheduleRepaint(int delay) {
        doOnEventThread(() -> {
            if (repaintTimer != null) {
                repaintTimer.stop();
            }
            repaintTimer = new Timer(delay, event -> {
                repaint();
                repaintTimer = null;
            });
            repaintTimer.setRepeats(false);
            repaintTimer.start();
        });
    }

    private void startWPLink() {
        try {
            rpcClient = new RPCClient(InetAddress.getLoopbackAddress(), DEFAULT_PORT, this::handleWPLinkMessage);
        } catch (IOException e) {
            logger.error("I/O error connecting to WPLink plugin", e);
        }
    }

    private void handleWPLinkMessage(SocketAddress remoteAddress, Message message) {
        switch (message.getType()) {
            case "PLAYERMOVED":
                final Map<String, Object> params = message.getParams();
                final double x = Double.parseDouble((String) params.get("x"));
                final double z = Double.parseDouble((String) params.get("z"));
                doLaterOnEventThread(() -> setMarkerCoords(new Point((int) x, (int) z)));
                break;
            default:
                logger.error("Unsupported message received from WPLink plugin: {}", message);
                break;
        }
    }

    public RPCClient rpcClient;
    private HashSet<Layer> hiddenLayers = new HashSet<>();
    private final CustomBiomeManager customBiomeManager;
    private Dimension dimension, backgroundDimension; // TODO make this more generic
    private int mouseX, mouseY, radius, effectiveRadius, contourSeparation, brushRotation, backgroundDimensionZoom,
            viewDistance;
    private boolean drawBrush, drawOverlays, drawContours, drawViewDistance, drawWalkingDistance,
            drawMinecraftBorder = true, drawBorders = true, drawBiomes = true;
    private BrushShape brushShape;
    private ColourScheme colourScheme;
    private LightOrigin lightOrigin = LightOrigin.NORTHWEST;
    private WPTileProvider tileProvider, backgroundTileProvider;
    private Shape customBrushShape;
    private OverlayType overlayType;
    private ColourRamp colourRamp;
    private Timer repaintTimer;

    private static final int FIVE_MINUTE_WALK_DISTANCE_RADIUS = 1280;
    private static final int DAY_WALK_DISTANCE_RADIUS = 3328;
    private static final int DAY_NIGHT_WALK_DISTANCE_RADIUS = 5120;
    private static final Font NORMAL_FONT = new Font("SansSerif", Font.PLAIN, 10);
    private static final Logger logger = LoggerFactory.getLogger(WorldPainter.class);
    private static final long serialVersionUID = 1L;

    private static final int LAYER_BIOMES     = -3;
    private static final int LAYER_BORDER     = -2;
    private static final int LAYER_BACKGROUND = -1;
    private static final int LAYER_DETAILS    =  0;
}