/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.threedeeview;

import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.biomeschemes.CustomBiomeManager;
import org.pepsoft.worldpainter.layers.Layer;

import javax.swing.Timer;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;

import static java.awt.image.BufferedImage.TYPE_BYTE_BINARY;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static org.pepsoft.minecraft.Constants.DEFAULT_WATER_LEVEL;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE;

/**
 *
 * @author pepijn
 */
public class ThreeDeeView extends JComponent implements Dimension.Listener, Tile.Listener, HierarchyListener, ActionListener, Scrollable {
    public ThreeDeeView(Dimension dimension, ColourScheme colourScheme, CustomBiomeManager customBiomeManager, int rotation, int zoom) {
        this.dimension = dimension;
        this.colourScheme = colourScheme;
        this.customBiomeManager = customBiomeManager;
        this.rotation = rotation;
        this.zoom = zoom;
        scale = (int) Math.pow(2.0, Math.abs(zoom - 1));
//        System.out.println("Zoom " + zoom + " -> scale " + scale);
        minHeight = dimension.getMinHeight();
        maxHeight = dimension.getMaxHeight();
        if (dimension.getTileFactory() instanceof HeightMapTileFactory) {
            waterLevel = ((HeightMapTileFactory) dimension.getTileFactory()).getWaterHeight();
        } else {
            waterLevel = DEFAULT_WATER_LEVEL;
        }
        upsideDown = dimension.getAnchor().invert; // Ceiling dimension
        zSortedTiles = new TreeMap<>();
        for (Tile tile: dimension.getTiles()) {
            final Rectangle tileBaseBounds = getTileBounds(tile.getX(), tile.getY(), 0, 0, 0);
            zSortedTiles.computeIfAbsent(tileBaseBounds.y, y -> new TreeMap<>()).put(tileBaseBounds.x, tile);
        }
        threeDeeRenderManager = new ThreeDeeRenderManager(dimension, colourScheme, customBiomeManager, rotation);

        dimension.addDimensionListener(this);
        for (Tile tile: dimension.getTiles()) {
            tile.addListener(this);
        }

        int width = dimension.getWidth() * TILE_SIZE + dimension.getHeight() * TILE_SIZE;
        int height = width / 2 + maxHeight - minHeight - 1;
//        maxX = dimension.getHighestX();
//        maxY = dimension.getHighestY();
        maxX = maxY = 0;
//        xOffset = 512;
//        yOffset = 256;
//        xOffset = yOffset = 0;
        switch (rotation) {
            case 0:
                xOffset = -getTileBounds(dimension.getLowestX(), dimension.getHighestY(), maxHeight).x;
                yOffset = -getTileBounds(dimension.getLowestX(), dimension.getLowestY(), maxHeight).y;
                break;
            case 1:
                xOffset = -getTileBounds(dimension.getHighestX(), dimension.getHighestY(), maxHeight).x;
                yOffset = -getTileBounds(dimension.getLowestX(), dimension.getHighestY(), maxHeight).y;
                break;
            case 2:
                xOffset = -getTileBounds(dimension.getHighestX(), dimension.getLowestY(), maxHeight).x;
                yOffset = -getTileBounds(dimension.getHighestX(), dimension.getHighestY(), maxHeight).y;
                break;
            case 3:
                xOffset = -getTileBounds(dimension.getLowestX(), dimension.getLowestY(), maxHeight).x;
                yOffset = -getTileBounds(dimension.getHighestX(), dimension.getLowestY(), maxHeight).y;
                break;
            default:
                throw new IllegalArgumentException();
        }
//        System.out.println("xOffset: " + xOffset + ", yOffset: " + yOffset);
        java.awt.Dimension preferredSize = zoom(new java.awt.Dimension(width, height));
        setPreferredSize(preferredSize);
        setMinimumSize(preferredSize);
        setMaximumSize(preferredSize);
        setSize(preferredSize);

        addHierarchyListener(this);
    }

    public RefreshMode getRefreshMode() {
        return refreshMode;
    }

    public void setRefreshMode(RefreshMode refreshMode) {
        this.refreshMode = refreshMode;
    }

    public BufferedImage getImage(ProgressReceiver progressReceiver) throws ProgressReceiver.OperationCancelled {
        final Tile3DRenderer renderer = new Tile3DRenderer(dimension, colourScheme, customBiomeManager, rotation);

        // Paint the complete image
        Rectangle imageBounds = null;
        int tileCount = 0;
        for (Map<Integer, Tile> row: zSortedTiles.values()) {
            for (Tile tile: row.values()) {
                if (imageBounds == null) {
                    imageBounds = getTileBounds(tile);
                } else {
                    imageBounds = imageBounds.union(getTileBounds(tile));
                }
                tileCount++;
            }
        }
        final BufferedImage image = new BufferedImage(imageBounds.width, imageBounds.height, TYPE_INT_ARGB);
        final Graphics2D g2 = image.createGraphics();
        try {
            int tileNo = 0;
            for (Map<Integer, Tile> row: zSortedTiles.values()) {
                for (Tile tile: row.values()) {
                    final Rectangle tileBounds = getTileBounds(tile);
                    g2.drawImage(renderer.render(tile), tileBounds.x - imageBounds.x, tileBounds.y - imageBounds.y, null);
                    if (progressReceiver != null) {
                        tileNo++;
                        progressReceiver.setProgress((float) tileNo / tileCount);
                    }
                }
            }
        } finally {
            g2.dispose();
        }
        return image;
    }

    public Point worldToView(int x, int y) {
//        highlightTile = new Point(x >> 7, y >> 7);
        switch (rotation) {
            case 0:
                return zoom(new Point(xOffset + TILE_SIZE + x - y, yOffset + maxHeight - minHeight - 1 - TILE_SIZE / 2 + (y + x) / 2));
            case 1:
                return zoom(new Point(xOffset + TILE_SIZE * 2 - x - y, yOffset + maxHeight - minHeight - 1 - (y - x) / 2));
            case 2:
                return zoom(new Point(xOffset + TILE_SIZE - x + y, yOffset + maxHeight - minHeight - 1 + TILE_SIZE / 2 - (y + x) / 2));
            case 3:
                return zoom(new Point(xOffset + x + y, yOffset + maxHeight - minHeight - 1 + (y - x) / 2));
            default:
                throw new IllegalArgumentException();
        }
    }
    
    public Tile getCentreMostTile() {
        return centreTile;
    }

    public Point getHighlightTile() {
        return highlightTile;
    }

    public void setHighlightTile(Point highlightTile) {
        this.highlightTile = highlightTile;
        repaint();
    }

    public int getZoom() {
        return zoom;
    }

    public void setZoom(int zoom) {
        this.zoom = zoom;
        scale = (int) Math.pow(2.0, Math.abs(zoom - 1));
//        System.out.println("Zoom " + zoom + " -> scale " + scale);
        int width = dimension.getWidth() * TILE_SIZE + dimension.getHeight() * TILE_SIZE;
        int height = width / 2 + maxHeight - minHeight - 1;
        java.awt.Dimension preferredSize = zoom(new java.awt.Dimension(width, height));
        setPreferredSize(preferredSize);
        setMinimumSize(preferredSize);
        setMaximumSize(preferredSize);
        setSize(preferredSize);
        repaint();
    }
    
    /**
     * Centre the view on a particular tile. Specifically, centre the view on a
     * point <em>waterLevel</em> above the centre of the floor of the tile.
     * 
     * @param tile The tile to centre the view on.
     */
    public void moveToTile(Tile tile) {
        Rectangle tileBounds = zoom(getTileBounds(tile));
        moveTo(new Point(tileBounds.x + tileBounds.width / 2, tileBounds.y + tileBounds.height - TILE_SIZE / 2));
//        highlightTile = new Point(tileX, tileY);
    }
    
    /**
     * Centre the view on a particular point in the world. Specifically, centre
     * the view on a point <em>waterLevel</em> above the floor of the world at
     * the specified coordinates.
     * 
     * @param x The X coordinate in blocks on which to centre the view.
     * @param y The Y coordinate in blocks on which to centre the view. 
     */
    public void moveTo(int x, int y) {
        Point coords = worldToView(x, y);
//        highlightPoint = coords;
        moveTo(coords);
    }

    public void refresh() {
        threeDeeRenderManager.stop();
        renderedTiles.clear();
        repaint();
    }
    
    // Dimension.Listener
    
    @Override
    public void tilesAdded(Dimension dimension, Set<Tile> tiles) {
//        threeDeeRenderManager.renderTile(tile);
        tiles.forEach(tile -> {
            final Rectangle tileBaseBounds = getTileBounds(tile.getX(), tile.getY(), 0, 0, 0);
            zSortedTiles.computeIfAbsent(tileBaseBounds.y, y -> new TreeMap<>()).put(tileBaseBounds.x, tile);
            tile.addListener(this);
        });
    }

    @Override
    public void tilesRemoved(Dimension dimension, Set<Tile> tiles) {
        for (Tile tile: tiles) {
            tile.removeListener(this);
            final Rectangle tileBaseBounds = getTileBounds(tile.getX(), tile.getY(), 0, 0, 0);
            if (zSortedTiles.containsKey(tileBaseBounds.y)) {
                zSortedTiles.get(tileBaseBounds.y).remove(tileBaseBounds.x);
            }
        }
//        renderedTiles.remove(new Point(tile.getX(), tile.getY()));
        // TODO: the tile will be re-added if it was on the render queue, but
        // since this can currently never happen anyway we will deal with that
        // when it becomes necessary
    }

    @Override public void overlayAdded(Dimension dimension, int index, Overlay overlay) {}
    @Override public void overlayRemoved(Dimension dimension, int index, Overlay overlay) {}

    // Tile.Listener
    
    @Override
    public void heightMapChanged(Tile tile) {
        scheduleTileForRendering(tile);
    }

    @Override
    public void terrainChanged(Tile tile) {
        scheduleTileForRendering(tile);
    }

    @Override
    public void waterLevelChanged(Tile tile) {
        scheduleTileForRendering(tile);
    }

    @Override
    public void seedsChanged(Tile tile) {
        scheduleTileForRendering(tile);
    }

    @Override
    public void layerDataChanged(Tile tile, Set<Layer> changedLayers) {
        for (Layer layer: changedLayers) {
            if (! Tile3DRenderer.DEFAULT_HIDDEN_LAYERS.contains(layer)) {
                scheduleTileForRendering(tile);
                return;
            }
        }
    }

    @Override
    public void allBitLayerDataChanged(Tile tile) {
        scheduleTileForRendering(tile);
    }

    @Override
    public void allNonBitlayerDataChanged(Tile tile) {
        scheduleTileForRendering(tile);
    }

    // HierarchyListener
    
    @Override
    public void hierarchyChanged(HierarchyEvent event) {
        if ((event.getChangeFlags() & HierarchyEvent.DISPLAYABILITY_CHANGED) != 0) {
            if (isDisplayable()) {
//                for (Tile tile: dimension.getTiles()) {
//                    threeDeeRenderManager.renderTile(tile);
//                }
                timer = new Timer(250, this);
                timer.start();
            } else {
                timer.stop();
                timer = null;
                threeDeeRenderManager.stop();
                for (Tile tile : dimension.getTiles()) {
                    tile.removeListener(this);
                }
                dimension.removeDimensionListener(this);
            }
        }
    }

    // ActionListener
    
    @Override
    public void actionPerformed(ActionEvent e) {
        // Send tiles to be rendered
        if ((!tilesWaitingToBeRendered.isEmpty()) && ((System.currentTimeMillis() - lastTileChange) > 250)) {
            tilesWaitingToBeRendered.forEach(threeDeeRenderManager::renderTile);
            tilesWaitingToBeRendered.clear();
        }

        // Collect rendered tiles
        Set<RenderResult> renderResults = threeDeeRenderManager.getRenderedTiles();
        Rectangle repaintArea = null;
        for (RenderResult renderResult : renderResults) {
            Tile tile = renderResult.getTile();
            int x = tile.getX(), y = tile.getY();
            renderedTiles.put(tile, renderResult.getImage());
            Rectangle tileBounds = zoom(getTileBounds(tile));
            if (repaintArea == null) {
                repaintArea = tileBounds;
            } else {
                repaintArea = repaintArea.union(tileBounds);
            }
        }
        if (repaintArea != null) {
//            System.out.println("Repainting " + repaintArea);
            repaint(repaintArea);
        }
    }

    // Scrollable
    @Override
    public java.awt.Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 10;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 100;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return false;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    @Override
    protected void paintComponent(Graphics g) {
//        System.out.println("Drawing");
        final Graphics2D g2 = (Graphics2D) g;
        if (zoom != 1) {
            final double scaleFactor = Math.pow(2.0, zoom - 1);
//            System.out.println("Scaling with factor " + scaleFactor);
            g2.scale(scaleFactor, scaleFactor);
            if (zoom > 1) {
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            } else {
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            }
        }
        if (upsideDown) {
            g2.scale(1.0, -1.0);
            g2.translate(0, -getHeight());
        }
        final Rectangle visibleRect = unzoom(getVisibleRect());
//        System.out.println("Unzoomed visible rectangle: " + visibleRect);
        final int centerX = visibleRect.x + visibleRect.width / 2;
        final int centerY = visibleRect.y + visibleRect.height / 2 + waterLevel;
        Tile mostCentredTile = null;
        int smallestDistance = Integer.MAX_VALUE;
        final Rectangle clipBounds = g2.getClipBounds();

        for (SortedMap<Integer, Tile> row: zSortedTiles.subMap(clipBounds.y - yOffset - maxHeight, clipBounds.y + clipBounds.height - yOffset + maxHeight).values()) {
            for (Tile tile: row.subMap(clipBounds.x - xOffset - TILE_SIZE * 2, clipBounds.x + clipBounds.width - xOffset).values()) {
                Rectangle tileBounds = getTileBounds(tile);
//                System.out.print("Tile bounds: " + tileBounds);
                if (tileBounds.intersects(clipBounds)) {
//                    System.out.println(" intersects");
                    int dx = tileBounds.x + tileBounds.width / 2 - centerX;
                    int dy = tileBounds.y + tileBounds.height - TILE_SIZE / 2 - centerY;
                    int dist = (int) Math.sqrt((dx * dx) + (dy * dy));
                    if (dist < smallestDistance) {
                        smallestDistance = dist;
                        mostCentredTile = tile;
                    }
                    BufferedImage tileImg = renderedTiles.get(tile);
                    if (tileImg != null) {
                        if (tileImg != TILE_NOT_RENDERABLE) {
                            g2.drawImage(tileImg, tileBounds.x, tileBounds.y, null);
                        } else {
                            g2.setColor(Color.RED);
                            g2.drawRect(tileBounds.x, tileBounds.y, tileBounds.width, tileBounds.height);
                            g2.drawLine(tileBounds.x, tileBounds.y, tileBounds.x + tileBounds.width, tileBounds.y + tileBounds.height);
                            g2.drawLine(tileBounds.x + tileBounds.width, tileBounds.y, tileBounds.x, tileBounds.y + tileBounds.height);
                        }
                    } else {
                        tilesWaitingToBeRendered.add(0, tile);
                    }
//                } else {
//                    System.out.println(" does NOT intersect");
                }
            }
        }
        if (mostCentredTile != null) {
            centreTile = mostCentredTile;
        }
        if (highlightTile != null) {
            g2.setColor(Color.RED);
            Rectangle rect = getTileBounds(highlightTile.x, highlightTile.y, maxHeight);
            g2.drawRect(rect.x, rect.y, rect.width, rect.height);
        }
        if (highlightPoint != null) {
            g2.setColor(Color.RED);
            g2.drawLine(highlightPoint.x - 2, highlightPoint.y, highlightPoint.x + 2, highlightPoint.y);
            g2.drawLine(highlightPoint.x, highlightPoint.y - 2, highlightPoint.x, highlightPoint.y + 2);
        }
//        for (Map.Entry<Point, BufferedImage> entry: renderedTiles.entrySet()) {
//            Point tileCoords = entry.getKey();
//            BufferedImage tileImg = entry.getValue();
//            Rectangle tileBounds = getTileBounds(tileCoords.x, tileCoords.y);
//            if (tileBounds.intersects(clipBounds)) {
//                g2.drawImage(tileImg, tileBounds.x, tileBounds.y, null);
////                g2.setColor(Color.RED);
////                g2.drawRect(tileBounds.x, tileBounds.y, tileBounds.width, tileBounds.height);
//            }
//        }
    }

    private void scheduleTileForRendering(final Tile tile) {
//        System.out.println("Scheduling tile for rendering: " + tile.getX() + ", " + tile.getY());
        final JViewport parent = (JViewport) getParent();
        if (parent == null) {
            // This has been observed in the wild; possible after the 3D view has been removed from the 3D frame?
            return;
        }
        if (SwingUtilities.isEventDispatchThread()) {
            Rectangle visibleArea = parent.getViewRect();
            Rectangle tileBounds = zoom(getTileBounds(tile));
            if (tileBounds.intersects(visibleArea)) {
                // The tile is (partially) visible, so it should be repainted
                // immediately
                switch (refreshMode) {
                    case IMMEDIATE:
                        threeDeeRenderManager.renderTile(tile);
                        break;
                    case DELAYED:
                        tilesWaitingToBeRendered.add(tile);
                        lastTileChange = System.currentTimeMillis();
                        break;
                    case MANUAL:
                        // Do nothing
                        break;
                    default:
                        throw new InternalError();
                }
            } else {
                // The tile is not visible, so repaint it when it becomes visible
                tilesWaitingToBeRendered.remove(tile);
                renderedTiles.remove(tile);
            }
        } else {
            SwingUtilities.invokeLater(() -> {
                Rectangle visibleArea = parent.getViewRect();
                Rectangle tileBounds = zoom(getTileBounds(tile));
                if (tileBounds.intersects(visibleArea)) {
                    // The tile is (partially) visible, so it should be repainted
                    // immediately
                    switch (refreshMode) {
                        case IMMEDIATE:
                            threeDeeRenderManager.renderTile(tile);
                            break;
                        case DELAYED:
                            tilesWaitingToBeRendered.add(tile);
                            lastTileChange = System.currentTimeMillis();
                            break;
                        case MANUAL:
                            // Do nothing
                            break;
                        default:
                            throw new InternalError();
                    }
                } else {
                    // The tile is not visible, so repaint it when it becomes visible
                    tilesWaitingToBeRendered.remove(tile);
                    renderedTiles.remove(tile);
                }
            });
        }
    }

    private Rectangle getTileBounds(final Tile tile) {
        return getTileBounds(tile.getX(), tile.getY(), Math.max(tile.getHighestIntHeight(), tile.getHighestWaterLevel()) + 1);
    }

    private Rectangle getTileBounds(final int x, final int y, final int maxHeight) {
        return getTileBounds(x, y, maxHeight, xOffset, yOffset);
    }

    private Rectangle getTileBounds(final int x, final int y, final int maxHeight, final int xOffset, final int yOffset) {
        switch (rotation) {
            case 0:
                return new Rectangle(xOffset + (x - y) * TILE_SIZE,
                        yOffset + (x + y) * TILE_SIZE / 2 + (this.maxHeight - maxHeight),
                        2 * TILE_SIZE,
                        TILE_SIZE + maxHeight - minHeight - 1);
            case 1:
                return new Rectangle(xOffset + ((maxY - y) - x) * TILE_SIZE,
                        yOffset + ((maxY - y) + x) * TILE_SIZE / 2 + (this.maxHeight - maxHeight),
                        2 * TILE_SIZE,
                        TILE_SIZE + maxHeight - minHeight - 1);
            case 2:
                return new Rectangle(xOffset + ((maxX - x) - (maxY - y)) * TILE_SIZE,
                        yOffset + ((maxX - x) + (maxY - y)) * TILE_SIZE / 2 + (this.maxHeight - maxHeight),
                        2 * TILE_SIZE,
                        TILE_SIZE + maxHeight - minHeight - 1);
            case 3:
                return new Rectangle(xOffset + (y - (maxX - x)) * TILE_SIZE,
                        yOffset + (y + (maxX - x)) * TILE_SIZE / 2 + (this.maxHeight - maxHeight),
                        2 * TILE_SIZE,
                        TILE_SIZE + maxHeight - minHeight - 1);
            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     * Centre the view on a particular point in view coordinates.
     * 
     * @param coords The point to centre on in view coordinates.
     */
    private void moveTo(Point coords) {
        Rectangle visibleRect = getVisibleRect();
        scrollRectToVisible(new Rectangle(coords.x - visibleRect.width / 2, coords.y - visibleRect.height / 2, visibleRect.width, visibleRect.height));
    }
    
    private java.awt.Dimension zoom(java.awt.Dimension dimension) {
        if (zoom < 1) {
            dimension.width /= scale;
            dimension.height /= scale;
        } else if (zoom > 1) {
            dimension.width *= scale;
            dimension.height *= scale;
        }
        return dimension;
    }

    private Point zoom(Point point) {
        if (zoom < 1) {
            point.x /= scale;
            point.y /= scale;
        } else if (zoom > 1) {
            point.x *= scale;
            point.y *= scale;
        }
        return point;
    }
    
    private Rectangle zoom(Rectangle rectangle) {
        if (zoom < 1) {
            rectangle.x /= scale;
            rectangle.y /= scale;
            rectangle.width /= scale;
            rectangle.height /= scale;
        } else if (zoom > 1) {
            rectangle.x *= scale;
            rectangle.y *= scale;
            rectangle.width *= scale;
            rectangle.height *= scale;
        }
        return rectangle;
    }

    private Rectangle unzoom(Rectangle rectangle) {
        if (zoom < 1) {
            rectangle.x *= scale;
            rectangle.y *= scale;
            rectangle.width *= scale;
            rectangle.height *= scale;
        } else if (zoom > 1) {
            rectangle.x /= scale;
            rectangle.y /= scale;
            rectangle.width /= scale;
            rectangle.height /= scale;
        }
        return rectangle;
    }
    
    private java.awt.Dimension unzoom(java.awt.Dimension dimension) {
        if (zoom < 1) {
            dimension.width *= scale;
            dimension.height *= scale;
        } else if (zoom > 1) {
            dimension.width /= scale;
            dimension.height /= scale;
        }
        return dimension;
    }
    
    private final Dimension dimension;
    private final Map<Tile, BufferedImage> renderedTiles = new HashMap<>();
    private final ThreeDeeRenderManager threeDeeRenderManager;
    private final ColourScheme colourScheme;
    private final List<Tile> tilesWaitingToBeRendered = new LinkedList<>();
    private final int minHeight, maxHeight;
    private final int xOffset, yOffset, maxX, maxY;
    private final int rotation;
    private final SortedMap<Integer, SortedMap<Integer, Tile>> zSortedTiles;
    private final CustomBiomeManager customBiomeManager;
    private final boolean upsideDown;
    private Timer timer;
    private long lastTileChange;
    private RefreshMode refreshMode = RefreshMode.DELAYED;
    private Tile centreTile;
    private int waterLevel, zoom = 1, scale = 1;
    private Point highlightTile, highlightPoint;

    public static final BufferedImage TILE_NOT_RENDERABLE = new BufferedImage(1, 1, TYPE_BYTE_BINARY);

//    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ThreeDeeView.class);
    private static final long serialVersionUID = 2011101701L;

    public enum RefreshMode {IMMEDIATE, DELAYED, MANUAL}
}