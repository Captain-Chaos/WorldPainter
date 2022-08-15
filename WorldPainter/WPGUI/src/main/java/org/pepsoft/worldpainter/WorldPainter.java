/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter;

import com.google.common.collect.Sets;
import org.pepsoft.util.MemoryUtils;
import org.pepsoft.worldpainter.TileRenderer.LightOrigin;
import org.pepsoft.worldpainter.biomeschemes.CustomBiomeManager;
import org.pepsoft.worldpainter.brushes.BrushShape;
import org.pepsoft.worldpainter.layers.Biome;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.tools.BiomesTileProvider;

import javax.imageio.ImageIO;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.singleton;
import static java.util.Collections.unmodifiableSet;
import static org.pepsoft.worldpainter.Constants.*;
import static org.pepsoft.worldpainter.DefaultPlugin.JAVA_ANVIL;
import static org.pepsoft.worldpainter.DefaultPlugin.JAVA_MCREGION;
import static org.pepsoft.worldpainter.Generator.DEFAULT;
import static org.pepsoft.worldpainter.Generator.LARGE_BIOMES;
import static org.pepsoft.worldpainter.TileRenderer.FLUIDS_AS_LAYER;
import static org.pepsoft.worldpainter.TileRenderer.TERRAIN_AS_LAYER;
import static org.pepsoft.worldpainter.WPTileProvider.Effect.FADE_TO_WHITE;

/**
 *
 * @author pepijn
 */
public class WorldPainter extends WorldPainterView implements MouseMotionListener, PropertyChangeListener {
    public WorldPainter(ColourScheme colourScheme, CustomBiomeManager customBiomeManager) {
        super(false, false);
        this.colourScheme = colourScheme;
        this.customBiomeManager = customBiomeManager;
        setOpaque(true);
        addMouseMotionListener(this);
        enableInputMethods(false);
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
        Dimension oldDimension = this.dimension;
        if ((oldDimension != null) && (oldDimension.getAnchor().dim == DIM_NORMAL)) {
            oldDimension.getWorld().removePropertyChangeListener("spawnPoint", this);
        }
        this.dimension = dimension;
        if (dimension != null) {
            drawContours = dimension.isContoursEnabled();
            contourSeparation = dimension.getContourSeparation();
            if (dimension.getAnchor().dim == DIM_NORMAL) {
                dimension.getWorld().addPropertyChangeListener("spawnPoint", this);
            }

            setGridSize(dimension.getGridSize());
            setPaintGrid(dimension.isGridEnabled());
            
            if (Configuration.getInstance().getOverlayType() != Configuration.OverlayType.SCALE_ON_LOAD) {
                setOverlayScale(dimension.getOverlayScale());
            } else {
                setOverlayScale(1.0f);
            }
            setOverlayTransparency(dimension.getOverlayTransparency());
            setOverlayOffsetX(dimension.getOverlayOffsetX());
            setOverlayOffsetY(dimension.getOverlayOffsetY());
            setOverlay(null);
            setDrawOverlay(dimension.isOverlayEnabled());
            setMarkerCoords((dimension.getAnchor().dim == DIM_NORMAL) ? dimension.getWorld().getSpawnPoint() : null);
        } else {
            setOverlay(null);
            setMarkerCoords(null);
        }
        firePropertyChange("dimension", oldDimension, dimension);
        refreshTiles();
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
            repaintWorld(mouseX - VIEW_DISTANCE_RADIUS, mouseY - VIEW_DISTANCE_RADIUS, VIEW_DISTANCE_DIAMETER + 1, VIEW_DISTANCE_DIAMETER + 1);
        }
    }

    public boolean isDrawWalkingDistance() {
        return drawWalkingDistance;
    }

    public void setDrawWalkingDistance(boolean drawWalkingDistance) {
        if (drawWalkingDistance != this.drawWalkingDistance) {
            this.drawWalkingDistance = drawWalkingDistance;
            firePropertyChange("drawWalkingDistance", !drawWalkingDistance, drawWalkingDistance);
            repaintWorld(mouseX - DAY_NIGHT_WALK_DISTANCE_RADIUS, mouseY - DAY_NIGHT_WALK_DISTANCE_RADIUS, DAY_NIGHT_WALK_DISTANCE_DIAMETER + 1, DAY_NIGHT_WALK_DISTANCE_DIAMETER + 1);
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

    public BufferedImage getOverlay() {
        return overlay;
    }

    public void setOverlay(BufferedImage overlay) {
        BufferedImage oldOverlay = this.overlay;
        this.overlay = overlay;
        if (overlayTransparency < 1.0f) {
            repaint();
        }
        firePropertyChange("overlay", oldOverlay, overlay);
    }

    public int getOverlayOffsetX() {
        return overlayOffsetX;
    }

    public void setOverlayOffsetX(int overlayOffsetX) {
        if (overlayOffsetX != this.overlayOffsetX) {
            int oldOverlayOffsetX = this.overlayOffsetX;
            this.overlayOffsetX = overlayOffsetX;
            if ((overlay != null) && (overlayTransparency < 1.0f)) {
                repaint();
            }
            firePropertyChange("overlayOffsetX", oldOverlayOffsetX, overlayOffsetX);
        }
    }

    public int getOverlayOffsetY() {
        return overlayOffsetY;
    }

    public void setOverlayOffsetY(int overlayOffsetY) {
        if (overlayOffsetY != this.overlayOffsetY) {
            int oldOverlayOffsetY = this.overlayOffsetY;
            this.overlayOffsetY = overlayOffsetY;
            if ((overlay != null) && (overlayTransparency < 1.0f)) {
                repaint();
            }
            firePropertyChange("overlayOffsetY", oldOverlayOffsetY, overlayOffsetY);
        }
    }

    public float getOverlayScale() {
        return overlayScale;
    }

    public void setOverlayScale(float overlayScale) {
        if (overlayScale != this.overlayScale) {
            float oldOverlayScale = this.overlayScale;
            this.overlayScale = overlayScale;
            if ((overlay != null) && (overlayTransparency < 1.0f)) {
                repaint();
            }
            firePropertyChange("overlayScale", oldOverlayScale, overlayScale);
        }
    }

    public float getOverlayTransparency() {
        return overlayTransparency;
    }

    public void setOverlayTransparency(float overlayTransparency) {
        if (overlayTransparency != this.overlayTransparency) {
            float oldOverlayTransparency = this.overlayTransparency;
            this.overlayTransparency = overlayTransparency;
            if (overlay != null) {
                repaint();
            }
            firePropertyChange("overlayTransparency", oldOverlayTransparency, overlayTransparency);
        }
    }

    public boolean isDrawOverlay() {
        return drawOverlay;
    }

    public void setDrawOverlay(boolean drawOverlay) {
        if (drawOverlay != this.drawOverlay) {
            this.drawOverlay = drawOverlay;
            if (overlay != null) {
                repaint();
            } else if (drawOverlay && (dimension.getOverlay() != null)) {
                loadOverlay();
            }
            if (drawOverlay == this.drawOverlay) {
                firePropertyChange("drawOverlay", ! drawOverlay, drawOverlay);
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
            if (drawBiomes
                    && (dimension.getAnchor().dim == DIM_NORMAL)
                    && ((dimension.getBorder() == null) || (! dimension.getBorder().isEndless()))) {
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
                        setTileProvider(LAYER_BIOMES, new BiomesTileProvider(biomeAlgorithm, dimension.getMinecraftSeed(), colourScheme, 0, true));
                    }
                }
            }

            tileProvider = new WPTileProvider(dimension, colourScheme, customBiomeManager, hiddenLayers, drawContours, contourSeparation, lightOrigin, drawBorders, true, null);
            setTileProvider(LAYER_DETAILS, tileProvider);
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
        TileRenderer tileRenderer = new TileRenderer(dimension, colourScheme, customBiomeManager, 0);
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
        if (drawViewDistance) {
            Rectangle viewDistanceArea = new Rectangle(-VIEW_DISTANCE_RADIUS, -VIEW_DISTANCE_RADIUS, VIEW_DISTANCE_DIAMETER, VIEW_DISTANCE_DIAMETER);
            if (repaintArea != null) {
                repaintArea = repaintArea.union(viewDistanceArea);
            } else {
                repaintArea = viewDistanceArea;
            }
        }
        if (drawWalkingDistance) {
            Rectangle walkingDistanceArea = new Rectangle(-DAY_NIGHT_WALK_DISTANCE_RADIUS, -DAY_NIGHT_WALK_DISTANCE_RADIUS, DAY_NIGHT_WALK_DISTANCE_DIAMETER, DAY_NIGHT_WALK_DISTANCE_DIAMETER);
            if (repaintArea != null) {
                repaintArea = repaintArea.union(walkingDistanceArea);
            } else {
                repaintArea = walkingDistanceArea;
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
        }
    }

    long getOverlayImageSize() {
        return (overlay != null) ? MemoryUtils.getSize(overlay, Collections.emptySet()) : 0L;
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
                final float onePixel = 1 / scale;
                if (drawOverlay && (overlay != null)) {
                    drawOverlay(g2);
                }
                if (drawBrush || drawViewDistance || drawWalkingDistance) {
                    g2.setColor(Color.BLACK);
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                }
                if (drawBrush) {
                    g2.setStroke(new BasicStroke(onePixel, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[] {3 * onePixel, 3 * onePixel}, 0));
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
                    g2.setStroke(new BasicStroke(onePixel, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[] {10 * onePixel, 10 * onePixel}, 0));
                    g2.drawOval(mouseX - VIEW_DISTANCE_RADIUS, mouseY - VIEW_DISTANCE_RADIUS, VIEW_DISTANCE_DIAMETER, VIEW_DISTANCE_DIAMETER);
                }
                if (drawWalkingDistance) {
                    g2.setStroke(new BasicStroke(onePixel, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[] {20 * onePixel, 20 * onePixel}, 0));
                    g2.drawOval(mouseX - DAY_NIGHT_WALK_DISTANCE_RADIUS, mouseY - DAY_NIGHT_WALK_DISTANCE_RADIUS, DAY_NIGHT_WALK_DISTANCE_DIAMETER, DAY_NIGHT_WALK_DISTANCE_DIAMETER);
                    setFont(NORMAL_FONT.deriveFont(10 * onePixel));
                    g2.drawString("day + night", mouseX - DAY_NIGHT_WALK_DISTANCE_RADIUS + onePixel * 3, mouseY);
                    g2.drawOval(mouseX - DAY_WALK_DISTANCE_RADIUS, mouseY - DAY_WALK_DISTANCE_RADIUS, DAY_WALK_DISTANCE_DIAMETER, DAY_WALK_DISTANCE_DIAMETER);
                    g2.drawString("1 day", mouseX - DAY_WALK_DISTANCE_RADIUS + onePixel * 3, mouseY);
                    g2.drawOval(mouseX - FIVE_MINUTE_WALK_DISTANCE_RADIUS, mouseY - FIVE_MINUTE_WALK_DISTANCE_RADIUS, FIVE_MINUTE_WALK_DISTANCE_DIAMETER, FIVE_MINUTE_WALK_DISTANCE_DIAMETER);
                    g2.drawString("5 min.", mouseX - FIVE_MINUTE_WALK_DISTANCE_RADIUS + onePixel * 3, mouseY);
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

    private void loadOverlay() {
        File file = dimension.getOverlay();
        if (file.isFile()) {
            if (file.canRead()) {
                logger.info("Loading image");
                BufferedImage myOverlay;
                try {
                    myOverlay = ImageIO.read(file);
                } catch (IOException e) {
                    logger.error("I/O error while loading image " + file ,e);
                    JOptionPane.showMessageDialog(this, "An error occurred while loading the overlay image.\nIt may not be a valid or supported image file, or the file may be corrupted.", "Error Loading Image", JOptionPane.ERROR_MESSAGE);
                    this.drawOverlay = false;
                    return;
                } catch (RuntimeException | Error e) {
                    logger.error(e.getClass().getSimpleName() + " while loading image " + file ,e);
                    JOptionPane.showMessageDialog(this, "An error occurred while loading the overlay image.\nThere may not be enough available memory, or the image may be too large.", "Error Loading Image", JOptionPane.ERROR_MESSAGE);
                    this.drawOverlay = false;
                    return;
                }
                switch (Configuration.getInstance().getOverlayType()) {
                    case OPTIMISE_ON_LOAD:
                        myOverlay = ConfigureViewDialog.scaleImage(myOverlay, getGraphicsConfiguration(), 100);
                        break;
                    case SCALE_ON_LOAD:
                        myOverlay = ConfigureViewDialog.scaleImage(myOverlay, getGraphicsConfiguration(), (int) (dimension.getOverlayScale() * 100));
                        break;
                }
                if (myOverlay != null) {
                    setOverlay(myOverlay);
                } else {
                    // The scaling or optimisation failed
                    this.drawOverlay = false;
                }
            } else {
                JOptionPane.showMessageDialog(this, "Access denied to overlay image\n" + file, "Error Enabling Overlay", JOptionPane.ERROR_MESSAGE);
                this.drawOverlay = false;
            }
        } else {
            JOptionPane.showMessageDialog(this, "Overlay image file not found\n" + file, "Error Enabling Overlay", JOptionPane.ERROR_MESSAGE);
            this.drawOverlay = false;
        }
    }

    private void drawMinecraftBorderIfNecessary(Graphics2D g2, World2.BorderSettings borderSettings) {
        final int size = borderSettings.getSize(), radius = size / 2;
        final Rectangle border = worldToView(borderSettings.getCentreX() - radius, borderSettings.getCentreY() - radius, size, size);
        Rectangle clip = g2.getClipBounds();
        if ((border.x >= clip.x) || (border.y >= clip.y) || ((border.x + border.width) < (clip.x + clip.width)) || ((border.y + border.height) < (clip.y + clip.height))) {
            g2.setColor(Color.RED);
            g2.setStroke(new BasicStroke(1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[] {3, 3}, 0));
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

    private void drawOverlay(Graphics2D g2) {
        if (overlayTransparency == 1.0f) {
            // Fully transparent
            return;
        } else {
            // Translucent or fully opaque
            Composite savedComposite = (overlayTransparency > 0.0f) ? g2.getComposite() : null;
            try {
                if (overlayTransparency > 0.0f) {
                    // Translucent
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f - overlayTransparency));
                }
                if (overlayScale == 1.0f) {
                    // 1:1 scale
                    g2.drawImage(overlay, overlayOffsetX, overlayOffsetY, null);
                } else {
                    int width = Math.round(overlay.getWidth() * overlayScale);
                    int height = Math.round(overlay.getHeight() * overlayScale);
                    Object savedInterpolation = g2.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
                    try {
                        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        g2.drawImage(overlay, overlayOffsetX, overlayOffsetY, width, height, null);
                    } finally {
                        if (savedInterpolation != null) {
                            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, savedInterpolation);
                        }
                    }
                }
            } finally {
                if (overlayTransparency > 0.0f) {
                    // Translucent
                    g2.setComposite(savedComposite);
                }
            }
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
    
    private HashSet<Layer> hiddenLayers = new HashSet<>();
    private final CustomBiomeManager customBiomeManager;
    private Dimension dimension;
    private int mouseX, mouseY, radius, effectiveRadius, overlayOffsetX, overlayOffsetY, contourSeparation, brushRotation;
    private boolean drawBrush, drawOverlay, drawContours, drawViewDistance, drawWalkingDistance, drawMinecraftBorder = true,
        drawBorders = true, drawBiomes = true;
    private BrushShape brushShape;
    private float overlayScale = 1.0f;
    private float overlayTransparency = 0.5f;
    private ColourScheme colourScheme;
    private BufferedImage overlay;
    private LightOrigin lightOrigin = LightOrigin.NORTHWEST;
    private WPTileProvider tileProvider;
    private Shape customBrushShape;

    private static final int VIEW_DISTANCE_RADIUS = 192; // 12 chunks (default of Minecraft 1.18.2)
    private static final int VIEW_DISTANCE_DIAMETER = 2 * VIEW_DISTANCE_RADIUS;
    private static final int FIVE_MINUTE_WALK_DISTANCE_RADIUS = 1280;
    private static final int FIVE_MINUTE_WALK_DISTANCE_DIAMETER = 2 * FIVE_MINUTE_WALK_DISTANCE_RADIUS;
    private static final int DAY_WALK_DISTANCE_RADIUS = 3328;
    private static final int DAY_WALK_DISTANCE_DIAMETER = 2 * DAY_WALK_DISTANCE_RADIUS;
    private static final int DAY_NIGHT_WALK_DISTANCE_RADIUS = 5120;
    private static final int DAY_NIGHT_WALK_DISTANCE_DIAMETER = 2 * DAY_NIGHT_WALK_DISTANCE_RADIUS;
    private static final Font NORMAL_FONT = new Font("SansSerif", Font.PLAIN, 10);
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(WorldPainter.class);
    private static final long serialVersionUID = 1L;

    private static final int LAYER_BIOMES  = -2;
    private static final int LAYER_MASTER  = -1;
    private static final int LAYER_DETAILS =  0;
}