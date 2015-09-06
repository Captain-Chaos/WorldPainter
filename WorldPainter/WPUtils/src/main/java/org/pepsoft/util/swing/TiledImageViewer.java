/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.util.swing;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.image.VolatileImage;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A generic visual component which can display one or more layers of large or
 * even endless tile-based images, with support for scrolling and scaling the
 * images.
 * 
 * <p>The tiles are provided by tile providers. The tiles are requested
 * asynchronously on multiple threads and are cached. This means that tile
 * providers have to do no caching themselves and are free to calculate or
 * generate each tile on request, even if that is relatively slow.
 * 
 * <p>When zooming in, this viewer performs all the scaling itself. When zooming
 * out, for tile providers which indicate that they support zooming, the scaling
 * is delegated to the tile providers.
 * 
 * @author pepijn
 */
public class TiledImageViewer extends JComponent implements TileListener, MouseListener, MouseMotionListener, ComponentListener, HierarchyListener {
    public TiledImageViewer() {
        this(true, Runtime.getRuntime().availableProcessors(), true);
    }
    
    @SuppressWarnings("LeakingThisInConstructor")
    public TiledImageViewer(boolean leftClickDrags, int threads, boolean paintCentre) {
        if (threads < 1) {
            throw new IllegalArgumentException("threads < 1");
        }
        this.leftClickDrags = leftClickDrags;
        this.threads = threads;
        this.paintCentre = paintCentre;
        addMouseListener(this);
        addMouseMotionListener(this);
        addComponentListener(this);
        addHierarchyListener(this);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setOpaque(true);
    }
    
    public List<TileProvider> getTileProviders() {
        return Collections.unmodifiableList(tileProviders);
    }

    public void addTileProvider(TileProvider tileProvider) {
        addTileProvider(tileProviders.size(), tileProvider);
    }
    
    public int getTileProviderCount() {
        return tileProviders.size();
    }

    public void setTileProvider(TileProvider tileProvider) {
        if (tileProviders.isEmpty()) {
            addTileProvider(tileProvider);
        } else {
            setTileProvider(0, tileProvider);
        }
    }
    
    public void setTileProvider(int index, TileProvider tileProvider) {
        removeTileProvider(index);
        addTileProvider(index, tileProvider);
    }
    
    public void replaceTileProvider(TileProvider oldTileProvider, TileProvider newTileProvider) {
        replaceTileProvider(tileProviders.indexOf(oldTileProvider), newTileProvider);
    }
    
    public void replaceTileProvider(int index, TileProvider newTileProvider) {
        synchronized (TILE_CACHE_LOCK) {
            TileProvider tileProvider = tileProviders.remove(index);
            tileProvider.removeTileListener(this);
            Map<Point, Reference<? extends Image>> dirtyTileCache = dirtyTileCaches.remove(tileProvider);
            Map<Point, Reference<? extends Image>> tileCache = tileCaches.remove(tileProvider);
            // Add all live tile images from the tile cache to the dirty tile
            // cache, for use as dirty tile for the new tile provider
            for (Map.Entry<Point, Reference<? extends Image>> entry: tileCache.entrySet()) {
                Reference<? extends Image> tileImageRef = entry.getValue();
                if (tileImageRef != RENDERING) {
                    Image tileImage = tileImageRef.get();
                    if (tileImage != null) {
                        dirtyTileCache.put(entry.getKey(), tileImageRef);
                    }
                }
            }
            // Prune the queue of jobs related to this tile provider
            for (Iterator<Runnable> i = queue.iterator(); i.hasNext(); ) {
                if (((TileRenderJob) i.next()).tileProvider == tileProvider) {
                    i.remove();
                }
            }
            
            if (newTileProvider.isZoomSupported()) {
                newTileProvider.setZoom((zoom <= 0) ? zoom : 0);
            }
            newTileProvider.addTileListener(this);
            tileProviders.add(index, newTileProvider);
            tileCaches.put(newTileProvider, new HashMap<>());
            dirtyTileCaches.put(newTileProvider, dirtyTileCache);
        }
        fireViewChangedEvent();
        repaint();
    }
    
    public void removeTileProvider(TileProvider tileProvider) {
        removeTileProvider(tileProviders.indexOf(tileProvider));
    }
    
    public void removeTileProvider(int index) {
        synchronized (TILE_CACHE_LOCK) {
            TileProvider tileProvider = tileProviders.remove(index);
            tileProvider.removeTileListener(this);
            tileCaches.remove(tileProvider);
            dirtyTileCaches.remove(tileProvider);
            // Prune the queue of jobs related to this tile provider
            for (Iterator<Runnable> i = queue.iterator(); i.hasNext(); ) {
                if (((TileRenderJob) i.next()).tileProvider == tileProvider) {
                    i.remove();
                }
            }
        }
        fireViewChangedEvent();
        repaint();
    }
    
    public void removeAllTileProviders() {
        synchronized (TILE_CACHE_LOCK) {
            for (TileProvider tileProvider: tileProviders) {
                tileProvider.removeTileListener(this);
            }
            if (queue != null) {
                queue.clear();
            }
            tileCaches.clear();
            dirtyTileCaches.clear();
        }
        fireViewChangedEvent();
        repaint();
    }
    
    public void addTileProvider(int index, TileProvider tileProvider) {
        if (tileProvider == null) {
            throw new NullPointerException();
        }
        tileProviders.add(index, tileProvider);
        synchronized (TILE_CACHE_LOCK) {
            if (tileProvider.isZoomSupported()) {
                tileProvider.setZoom((zoom <= 0) ? zoom : 0);
            }
            tileProvider.addTileListener(this);
            tileCaches.put(tileProvider, new HashMap<>());
            dirtyTileCaches.put(tileProvider, new HashMap<>());
            if ((tileRenderers == null) && isDisplayable()) {
                // The component is already visible but had no tile providers
                // installed yet; start the background threads
                if (logger.isDebugEnabled()) {
                    logger.debug("Starting " + threads + " tile rendering threads");
                }
                queue = new PriorityBlockingQueue<>();
                tileRenderers = new ThreadPoolExecutor(threads, threads, 0, TimeUnit.MILLISECONDS, queue);
            }
        }
        fireViewChangedEvent();
        repaint();
    }

    /**
     * Get the X coordinate in world coordinates of the centre of the view.
     * 
     * @return The X coordinate in world coordinates of the centre of the view.
     */
    public int getViewX() {
        if (zoom == 0) {
            return viewX;
        } else if (zoom < 0) {
            return viewX << -zoom;
        } else {
            return viewX >> zoom;
        }
    }

    /**
     * Get the Y coordinate in world coordinates of the centre of the view.
     * 
     * @return The Y coordinate in world coordinates of the centre of the view.
     */
    public int getViewY() {
        if (zoom == 0) {
            return viewY;
        } else if (zoom < 0) {
            return viewY << -zoom;
        } else {
            return viewY >> zoom;
        }
    }

    /**
     * Get the combined and zoom-corrected extent or main area of interest of
     * all the tile providers, in component coordinates.
     * 
     * @return The combined and zoom-corrected extent or main area of interest
     *     of all the tile providers, or <code>null</code> if there are no tile
     *     providers configured, or none of them indicate an extent.
     */
    public Rectangle getExtent() {
        Rectangle extent = null;
        for (TileProvider tileProvider: tileProviders) {
            Rectangle providerExtent = tileProvider.getExtent();
            if (providerExtent != null) {
                providerExtent = getTileBounds(providerExtent.x, providerExtent.y, providerExtent.width, providerExtent.height, (tileProvider.isZoomSupported() && (zoom < 0)) ? 0 : zoom);
                if (extent == null) {
                    extent = providerExtent;
                } else {
                    extent = extent.union(providerExtent);
                }
            }
        }
        return extent;
    }

    /**
     * Get the coordinate in world coordinates of the centre of the view.
     * 
     * @return The coordinate in world coordinates of the centre of the view.
     */
    public Point getViewLocation() {
        if (zoom == 0) {
            return new Point(viewX, viewY);
        } else if (zoom < 0) {
            return new Point(viewX << -zoom, viewY << -zoom);
        } else {
            return new Point(viewX >> zoom, viewY >> zoom);
        }
    }

    /**
     * Get the current zoom level in powers of two.
     *
     * @return The current zoom level as a power of two. The scale is
     *             2<sup>zoom</sup>.
     */
    public int getZoom() {
        return zoom;
    }

    /**
     * Set the zoom level in powers of two. 0 means "native size"; positive
     * numbers zoom in (result in a larger scale); negative numbers zoom out
     * (result in a smaller scale).
     *
     * @param zoom The new zoom level as a power of two. The scale will be
     *             2<sup>zoom</sup>.
     */
    public void setZoom(int zoom) {
        setZoom(zoom, xOffset, yOffset);
    }
    
    /**
     * Set the zoom level in powers of two. 0 means "native size"; positive
     * numbers zoom in (result in a larger scale); negative numbers zoom out
     * (result in a smaller scale).
     *
     * @param zoom The new zoom level as a power of two. The scale will be
     *             2<sup>zoom</sup>.
     */
    public void setZoom(int zoom, int locusX, int locusY) {
        // TODO: implement zoom locus support
        if (zoom != this.zoom) {
            int dZoom = zoom - this.zoom;
            this.zoom = zoom;
            if (queue != null) {
                queue.clear();
            }
            synchronized (TILE_CACHE_LOCK) {
                for (TileProvider tileProvider: tileProviders) {
                    if (tileProvider.isZoomSupported()) {
                        // Only use the tile provider's own zoom support for
                        // zooming out:
                        tileProvider.setZoom((zoom <= 0) ? zoom : 0);
                    }
                    dirtyTileCaches.put(tileProvider, new HashMap<>());
                    tileCaches.put(tileProvider, new HashMap<>());
                }
            }
            // Adjust view location, since it is in unzoomed coordinates
            if (dZoom < 0) {
                viewX >>= -dZoom;
                viewY >>= -dZoom;
            } else {
                viewX <<= dZoom;
                viewY <<= dZoom;
            }
            fireViewChangedEvent();
            repaint();
        }
    }
    
    public Point getMarkerCoords() {
        return paintMarker ? new Point(markerX, markerY) : null;
    }
    
    public void setMarkerCoords(Point markerCoords) {
        if (markerCoords != null) {
            markerX = markerCoords.x;
            markerY = markerCoords.y;
            paintMarker = true;
        } else {
            paintMarker = false;
        }
        repaint();
    }

    public boolean isPaintGrid() {
        return paintGrid;
    }

    public void setPaintGrid(boolean paintGrid) {
        if (paintGrid != this.paintGrid) {
            this.paintGrid = paintGrid;
            repaint();
        }
    }

    public int getGridSize() {
        return gridSize;
    }

    public void setGridSize(int gridSize) {
        if (gridSize != this.gridSize) {
            this.gridSize = gridSize;
            repaint();
        }
    }

    /**
     * Centre the view on a particular location in world coordinates.
     * 
     * @param coords The coordinates in world coordinates of the location to
     *     centre.
     */
    public void moveTo(Point coords) {
        moveTo(coords.x, coords.y);
    }
    
    /**
     * Centre the view on a particular location in world coordinates.
     * 
     * @param x The X coordinate in world coordinates of the location to centre.
     * @param y The Y coordinate in world coordinates of the location to centre.
     */
    public void moveTo(int x, int y) {
        if (zoom < 0) {
            x >>= -zoom;
            y >>= -zoom;
        } else if (zoom > 0) {
            x <<= zoom;
            y <<= zoom;
        }
        if ((viewX != x) || (viewY != y)) {
            viewX = x;
            viewY = y;
            fireViewChangedEvent();
            repaint();
        }
    }
    
    public void moveToMarker() {
        if (paintMarker) {
            moveTo(markerX, markerY);
        }
    }

    public void moveToOrigin() {
        moveTo(0, 0);
    }

    /**
     * Move the view by a number of pixels. The actual movement in world
     * coordinates may be different if the zoom level is not zero.
     * 
     * @param dx The number of pixels to move the view right.
     * @param dy The number of pixels to move the view down.
     */
    public void moveBy(int dx, int dy) {
        if ((dx != 0) || (dy != 0)) {
            viewX += dx;
            viewY += dy;
            fireViewChangedEvent();
            repaint();
        }
    }

    /**
     * Immediately throws away and refreshes all tiles of all tile providers.
     */
    public void refresh() {
        refresh(false);
    }

    /**
     * Refreshes all tiles of all tile providers.
     *
     * @param keepDirtyTiles Whether to keep displaying the old tiles while the
     *                       new ones are being rendered.
     */
    public void refresh(boolean keepDirtyTiles) {
        queue.clear();
        synchronized (TILE_CACHE_LOCK) {
            for (TileProvider tileProvider: tileProviders) {
                if (keepDirtyTiles) {
                    Map<Point, Reference<? extends Image>> dirtyTileCache = tileCaches.get(tileProvider);
                    // Remove all dirty tiles which don't exist any more
                    // according to the tile provider, otherwise they won't be
                    // repainted
                    for (Iterator<Map.Entry<Point, Reference<? extends Image>>> i = dirtyTileCache.entrySet().iterator(); i.hasNext(); ) {
                        Map.Entry<Point, Reference<? extends Image>> entry = i.next();
                        Point coords = entry.getKey();
                        if (! tileProvider.isTilePresent(coords.x, coords.y)) {
                            i.remove();
                        }
                    }
                    dirtyTileCaches.put(tileProvider, dirtyTileCache);
                } else {
                    dirtyTileCaches.put(tileProvider, new HashMap<>());
                }
                tileCaches.put(tileProvider, new HashMap<>());
            }
        }
        repaint();
    }
    
    public void refresh(TileProvider tileProvider, int x, int y) {
        synchronized (TILE_CACHE_LOCK) {
            final Point coords = new Point(x, y);
            final Map<Point, Reference<? extends Image>> tileCache = tileCaches.get(tileProvider);
            final Reference<? extends Image> tileRef = tileCache.remove(coords);
            final int effectiveZoom = (tileProvider.isZoomSupported() && (zoom < 0)) ? 0 : zoom;
            if (tileRef != RENDERING) {
                final Image tile = (tileRef != null) ? tileRef.get() : null;
                if (tile != null) {
                    // The old tile is still available; move it to the dirty
                    // tile cache so we have something to paint while the tile
                    // is being rendered
                    dirtyTileCaches.get(tileProvider).put(coords, tileRef);
                }
                if (isTileVisible(x, y, effectiveZoom)) {
                    // The tile is visible; immediately schedule it to be
                    // rendered
                    scheduleTile(tileCache, coords, tileProvider, dirtyTileCaches.get(tileProvider), effectiveZoom, (tile != NO_TILE) ? tile : null);
                }
            } else if (isTileVisible(x, y, effectiveZoom)) {
                // The tile is already rendering, but apparently it has changed so schedule it anyway (if visible)
                scheduleTile(tileCache, coords, tileProvider, dirtyTileCaches.get(tileProvider), effectiveZoom, null);
            }
        }
    }
    
    public void refresh(TileProvider tileProvider, Set<Point> tiles) {
        synchronized (TILE_CACHE_LOCK) {
            final Map<Point, Reference<? extends Image>> tileCache = tileCaches.get(tileProvider);
            final Map<Point, Reference<? extends Image>> dirtyTileCache = dirtyTileCaches.get(tileProvider);
            final int effectiveZoom = (tileProvider.isZoomSupported() && (zoom < 0)) ? 0 : zoom;
            for (Point coords: tiles) {
                final Reference<? extends Image> tileRef = tileCache.remove(coords);
                if (tileRef != RENDERING) {
                    final Image tile = (tileRef != null) ? tileRef.get() : null;
                    if (tile != null) {
                        // The old tile is still available; move it to the dirty
                        // tile cache so we have something to paint while the tile
                        // is being rendered
                        dirtyTileCache.put(coords, tileRef);
                    }
                    if (isTileVisible(coords.x, coords.y, effectiveZoom)) {
                        // The tile is visible; immediately schedule it to be
                        // rendered
                        scheduleTile(tileCache, coords, tileProvider, dirtyTileCache, effectiveZoom, (tile != NO_TILE) ? tile : null);
                    }
                } else if (isTileVisible(coords.x, coords.y, effectiveZoom)) {
                    // The tile is already rendering, but apparently it has changed so schedule it anyway (if visible)
                    scheduleTile(tileCache, coords, tileProvider, dirtyTileCache, effectiveZoom, null);
                }
            }
        }
    }

    public void reset() {
        viewX = 0;
        viewY = 0;
        if (zoom == -2) {
            fireViewChangedEvent();
        } else {
            setZoom(-2);
        }
        repaint();
    }
    
    public final Point worldToView(Point coords) {
        return worldToView(coords.x, coords.y);
    }
    
    public final Point worldToView(int x, int y) {
        return (zoom == 0)
            ? new Point(x - viewX + xOffset, y - viewY + yOffset)
            : ((zoom < 0)
                ? new Point((x >> -zoom) - viewX + xOffset, (y >> -zoom) - viewY + yOffset)
                : new Point((x << zoom) - viewX + xOffset, (y << zoom) - viewY + yOffset));
    }
    
    public final Point viewToWorld(Point coords) {
        return viewToWorld(coords.x, coords.y, zoom);
    }
    
    public final Point viewToWorld(int x, int y) {
        return viewToWorld(x, y, zoom);
    }
    
    public final Point viewToWorld(Point coords, int effectiveZoom) {
        return viewToWorld(coords.x, coords.y, effectiveZoom);
    }
    
    public final Point viewToWorld(int x, int y, int effectiveZoom) {
        return (effectiveZoom == 0)
            ? new Point(x + viewX - xOffset, y + viewY - yOffset)
            : ((effectiveZoom < 0)
                ? new Point((x + viewX - xOffset) << -effectiveZoom, (y + viewY - yOffset) << -effectiveZoom)
                : new Point((x + viewX - xOffset) >> effectiveZoom, (y + viewY - yOffset) >> effectiveZoom));
    }
    
    public final Rectangle worldToView(Rectangle coords) {
        return worldToView(coords.x, coords.y, coords.width, coords.height, zoom);
    }
    
    public final Rectangle worldToView(int x, int y, int width, int height) {
        return worldToView(x, y, width, height, zoom);
    }
    
    public final Rectangle worldToView(Rectangle coords, int effectiveZoom) {
        return worldToView(coords.x, coords.y, coords.width, coords.height, effectiveZoom);
    }
    
    public final Rectangle worldToView(int x, int y, int width, int height, int effectiveZoom) {
        return (effectiveZoom == 0)
            ? new Rectangle(x - viewX + xOffset, y - viewY + yOffset, width, height)
            : ((effectiveZoom < 0)
                ? new Rectangle((x >> -effectiveZoom) - viewX + xOffset, (y >> -effectiveZoom) - viewY + yOffset, width >> -effectiveZoom, height >> -effectiveZoom)
                : new Rectangle((x << effectiveZoom) - viewX + xOffset, (y << effectiveZoom) - viewY + yOffset, width << effectiveZoom, height << effectiveZoom));
    }
    
    public final Rectangle viewToWorld(Rectangle coords) {
        return viewToWorld(coords.x, coords.y, coords.width, coords.height);
    }
    
    public final Rectangle viewToWorld(int x, int y, int width, int height) {
        return (zoom == 0)
            ? new Rectangle(x + viewX - xOffset, y + viewY - yOffset, width, height)
            : ((zoom < 0)
                ? new Rectangle((x + viewX - xOffset) << -zoom, (y + viewY - yOffset) << -zoom, width << -zoom, height << -zoom)
                : new Rectangle((x + viewX - xOffset) >> zoom, (y + viewY - yOffset) >> zoom, width >> zoom, height >> zoom));
    }

    public ViewListener getViewListener() {
        return viewListener;
    }

    public void setViewListener(ViewListener viewListener) {
        this.viewListener = viewListener;
    }

    public void addOverlay(String key, int x, Component componentToTrack, BufferedImage overlay) {
        overlays.put(key, new Overlay(componentToTrack, key, x, overlay));
        repaint();
    }

    public void removeOverlay(String key) {
        if (overlays.containsKey(key)) {
            overlays.remove(key);
            repaint();
        }
    }

    protected final boolean isTileVisible(int x, int y, int effectiveZoom) {
        return new Rectangle(0, 0, getWidth(), getHeight()).intersects(getTileBounds(x, y, effectiveZoom));
    }

    protected final Rectangle getTileBounds(int x, int y) {
        return getTileBounds(x, y, zoom);
    }

    protected final Rectangle getTileBounds(int x, int y, int effectiveZoom) {
        return worldToView(x << TILE_SIZE_BITS, y << TILE_SIZE_BITS, TILE_SIZE, TILE_SIZE, effectiveZoom);
    }
    
    protected final Rectangle getTileBounds(int x, int y, int width, int height, int effectiveZoom) {
        return worldToView(x << TILE_SIZE_BITS, y << TILE_SIZE_BITS, TILE_SIZE * width, TILE_SIZE * height, effectiveZoom);
    }
    
    protected final float transformGraphics(Graphics2D g2) {
        g2.translate(getWidth() / 2, getHeight() / 2);
        g2.translate(-viewX, -viewY);
        if (zoom != 0) {
            float scale = (float) Math.pow(2.0, zoom);
            g2.scale(scale, scale);
            return scale;
        } else {
            return 1.0f;
        }
    }

    protected final void paintMarkerIfApplicable(Graphics g2) {
        if (paintMarker) {
            Color savedColour = g2.getColor();
            try {
                g2.setColor(Color.RED);
                Point markerCoords = worldToView(markerX, markerY);
                g2.drawLine(markerCoords.x - 5, markerCoords.y, markerCoords.x + 5, markerCoords.y);
                g2.drawLine(markerCoords.x, markerCoords.y - 5, markerCoords.x, markerCoords.y + 5);
            } finally {
                g2.setColor(savedColour);
            }
        }
    }

    private void paintGridIfApplicable(Graphics2D g2) {
        if (! paintGrid) {
            return;
        }

        // Save the current graphics canvas configuration
        final Color savedColour = g2.getColor();
        final Stroke savedStroke = g2.getStroke();
        final Font savedFont = g2.getFont();
        final Object savedTextAAHint = g2.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);

        try {
            // Determine which grid lines to draw, in world coordinates
            final Rectangle clipInWorld = viewToWorld(g2.getClipBounds());
            final int x1 = ((clipInWorld.x / gridSize) - 1) * gridSize;
            final int x2 = ((clipInWorld.x + clipInWorld.width) / gridSize + 1) * gridSize;
            final int y1 = ((clipInWorld.y / gridSize) - 1) * gridSize;
            final int y2 = ((clipInWorld.y + clipInWorld.height) / gridSize + 1) * gridSize;
            g2.setColor(Color.BLACK);

            // Determine the exclusion zone for preventing labels from being
            // obscured by grid lines or other labels
            final Rectangle2D fontBounds = BOLD_FONT.getStringBounds("-00000", g2.getFontRenderContext());
            final int fontHeight = (int) (fontBounds.getHeight() + 0.5), fontWidth = (int) (fontBounds.getWidth() + 0.5);
            final int leftClear = fontWidth + 4, topClear = fontHeight + 6;

            // Create and install strokes and fonts
            final Stroke normalStroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{2f, 2f}, 0.0f);
            final Stroke regionBorderStroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{6f, 2f}, 0.0f);

            final boolean drawRegionBorders = (gridSize <= 512) && (gridSize & (gridSize - 1)) == 0; // Power of two
            final int width = getWidth(), height = getHeight();
            int xLabelSkip = gridSize, yLabelSkip = gridSize;
            final float scale = (float) Math.pow(2.0, getZoom());

            // Determine per how many grid lines minimum a label can be draw
            // so that they don't obscure one another, for the horizontal and
            // vertical direction
            while ((xLabelSkip * scale) < fontWidth) {
                xLabelSkip += gridSize;
            }
            while ((yLabelSkip * scale) < fontHeight) {
                yLabelSkip += gridSize;
            }

            // Initial setup of the graphics canvas
            g2.setStroke(normalStroke);
            g2.setFont(NORMAL_FONT);
            boolean normalFontInstalled = true;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
            boolean normalStrokeInstalled = true;

            // Draw the vertical grid lines and corresponding labels
            for (int x = x1; x <= x2; x += gridSize) {
                if ((x == 0) || (drawRegionBorders && ((x % 512) == 0))) {
                    g2.setStroke(regionBorderStroke);
                    normalStrokeInstalled = false;
                } else if (!normalStrokeInstalled) {
                    g2.setStroke(normalStroke);
                    normalStrokeInstalled = true;
                }
                Point lineStartInView = worldToView(x, 0);
                if ((x % xLabelSkip) == 0) {
                    if (lineStartInView.x + 2 >= leftClear) {
                        g2.drawLine(lineStartInView.x, 0, lineStartInView.x, height);
                    }
                    if (drawRegionBorders && ((x % 512) == 0)) {
                        g2.setFont(BOLD_FONT);
                        normalFontInstalled = false;
                    } else if (!normalFontInstalled) {
                        g2.setFont(NORMAL_FONT);
                        normalFontInstalled = true;
                    }
                    g2.drawString(Integer.toString(x), lineStartInView.x + 2, fontHeight + 2);
                } else if (lineStartInView.x + 2 >= leftClear) {
                    g2.drawLine(lineStartInView.x, topClear, lineStartInView.x, height);
                }
            }

            // Draw the horizontal grid lines and corresponding labels
            for (int y = y1; y <= y2; y += gridSize) {
                if ((y == 0) || (drawRegionBorders && ((y % 512) == 0))) {
                    g2.setStroke(regionBorderStroke);
                    normalStrokeInstalled = false;
                } else if (!normalStrokeInstalled) {
                    g2.setStroke(normalStroke);
                    normalStrokeInstalled = true;
                }
                Point lineStartInView = worldToView(0, y);
                if ((y % yLabelSkip) == 0) {
                    if (lineStartInView.y + 2 >= topClear) {
                        g2.drawLine(0, lineStartInView.y, width, lineStartInView.y);
                    }
                    if (drawRegionBorders && ((y % 512) == 0)) {
                        g2.setFont(BOLD_FONT);
                        normalFontInstalled = false;
                    } else if (!normalFontInstalled) {
                        g2.setFont(NORMAL_FONT);
                        normalFontInstalled = true;
                    }
                    g2.drawString(Integer.toString(y), 2, lineStartInView.y - 2);
                } else if (lineStartInView.y + 2 >= topClear) {
                    g2.drawLine(leftClear, lineStartInView.y, width, lineStartInView.y);
                }
            }
        } finally {
            // Restore the original graphics canvas configuration
            g2.setColor(savedColour);
            g2.setStroke(savedStroke);
            g2.setFont(savedFont);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, savedTextAAHint);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        final Graphics2D g2 = (Graphics2D) g;
        g2.setColor(getBackground());
        Rectangle clipBounds = g2.getClipBounds();
        if (tileProviders.isEmpty()) {
            g2.fillRect(clipBounds.x, clipBounds.y, clipBounds.width, clipBounds.height);
            return;
        }
        
        GraphicsConfiguration gc = getGraphicsConfiguration();
//        if (firstPaint) {
//            System.out.println("Graphics2D instance: " + g2);
//            System.out.println("GraphicsConfiguration instance: " + gc);
//            firstPaint = false;
//        }
        for (TileProvider tileProvider: tileProviders) {
            final int effectiveZoom = (tileProvider.isZoomSupported() && (zoom < 0)) ? 0 : zoom;
            final Point topLeftTileCoords = viewToWorld(clipBounds.getLocation(), effectiveZoom);
            final int leftTile = topLeftTileCoords.x >> TILE_SIZE_BITS;
            final int topTile = topLeftTileCoords.y >> TILE_SIZE_BITS;
            final Point bottomRightTileCoords = viewToWorld(new Point(clipBounds.x + clipBounds.width - 1, clipBounds.y + clipBounds.height - 1), effectiveZoom);
            final int rightTile = bottomRightTileCoords.x >> TILE_SIZE_BITS;
            final int bottomTile = bottomRightTileCoords.y >> TILE_SIZE_BITS;
//            System.out.println("Painting tiles " + leftTile + "," + topTile + " -> " + rightTile + "," + bottomTile + " for tile provider " + tileProvider);

            final int middleTileX = (leftTile + rightTile) / 2;
            final int middleTileY = (topTile + bottomTile) / 2;
            final int radius = Math.max(
                Math.max(middleTileX - leftTile, rightTile - middleTileX),
                Math.max(middleTileY - topTile, bottomTile - middleTileY));

            // Paint the tiles in a spiralish fashion, so that missing tiles are generated in that order
            paintTile(g2, gc, tileProvider, middleTileX, middleTileY, effectiveZoom);
            for (int r = 1; r <= radius; r++) {
                for (int i = 0; i < (r * 2); i++) {
                    int tileX = middleTileX + i - r, tileY = middleTileY - r;
                    if ((tileX >= leftTile) && (tileX <= rightTile) && (tileY >= topTile) && (tileY <= bottomTile)) {
                        paintTile(g2, gc, tileProvider, tileX, tileY, effectiveZoom);
                    }
                    tileX = middleTileX + r;
                    tileY = middleTileY + i - r;
                    if ((tileX >= leftTile) && (tileX <= rightTile) && (tileY >= topTile) && (tileY <= bottomTile)) {
                        paintTile(g2, gc, tileProvider, tileX, tileY, effectiveZoom);
                    }
                    tileX = middleTileX + r - i;
                    tileY = middleTileY + r;
                    if ((tileX >= leftTile) && (tileX <= rightTile) && (tileY >= topTile) && (tileY <= bottomTile)) {
                        paintTile(g2, gc, tileProvider, tileX, tileY, effectiveZoom);
                    }
                    tileX = middleTileX - r;
                    tileY = middleTileY - i + r;
                    if ((tileX >= leftTile) && (tileX <= rightTile) && (tileY >= topTile) && (tileY <= bottomTile)) {
                        paintTile(g2, gc, tileProvider, tileX, tileY, effectiveZoom);
                    }
                }
            }
        }
//        System.out.println();

        paintGridIfApplicable(g2);

        paintMarkerIfApplicable(g2);
        
        if (paintCentre) {
            final int middleX = getWidth() / 2;
            final int middleY = getHeight() / 2;
            g2.setColor(Color.BLACK);
            g2.drawLine(middleX - 4, middleY + 1, middleX + 6, middleY + 1);
            g2.drawLine(middleX + 1, middleY - 4, middleX + 1, middleY + 6);
            g2.setColor(Color.WHITE);
            g2.drawLine(middleX - 5, middleY, middleX + 5, middleY);
            g2.drawLine(middleX, middleY - 5, middleX, middleY + 5);
        }

        paintOverlays(g2);

        // Unschedule tiles which were scheduled to be rendered but are no
        // longer visible
        final Rectangle viewBounds = new Rectangle(0, 0, getWidth(), getHeight());
        synchronized (TILE_CACHE_LOCK) {
            for (Iterator<Runnable> i = queue.iterator(); i.hasNext(); ) {
                TileRenderJob job = (TileRenderJob) i.next();
                if (! getTileBounds(job.coords.x, job.coords.y, job.effectiveZoom).intersects(viewBounds)) {
                    i.remove();
                    tileCaches.get(job.tileProvider).remove(job.coords);
                }
            }
        }
    }

    private void paintOverlays(Graphics2D g2) {
        overlays.values().stream().forEach(overlay -> {
            int x = overlay.x >= 0 ? overlay.x : getWidth() + overlay.x;
            Point coords = SwingUtilities.convertPoint(overlay.componentToTrack, 0, 0, this);
            g2.drawImage(overlay.image, x, coords.y, null);
        });
    }

    private void paintTile(Graphics2D g2, GraphicsConfiguration gc, TileProvider tileProvider, int x, int y, int effectiveZoom) {
        Rectangle tileBounds = getTileBounds(x, y, effectiveZoom);
        Image tile = getTile(tileProvider, x, y, effectiveZoom, gc);
        if (tile != null) {
//            if ((tile instanceof VolatileImage) && (((VolatileImage) tile).validate(gc) != VolatileImage.IMAGE_OK)) {
//                logger.error("Image not OK right before painting!");
//            }
//            ImageCapabilities capabilities = tile.getCapabilities(gc);
//            System.out.print(capabilities.isAccelerated() ? 'a' : '-');
//            System.out.print(capabilities.isTrueVolatile() ? 't' : '-');
            if (zoom > 0) {
                g2.drawImage(tile, tileBounds.x, tileBounds.y, tileBounds.width, tileBounds.height, this);
            } else {
                g2.drawImage(tile, tileBounds.x, tileBounds.y, this);
            }
        } else {
            g2.fillRect(tileBounds.x, tileBounds.y, tileBounds.width, tileBounds.height);
        }
    }
    
    private Image getTile(TileProvider tileProvider, int x, int y, int effectiveZoom, GraphicsConfiguration gc) {
        synchronized (TILE_CACHE_LOCK) {
            final Point coords = new Point(x, y);
            final Map<Point, Reference<? extends Image>> tileCache = tileCaches.get(tileProvider),
                    dirtyTileCache = dirtyTileCaches.get(tileProvider);
            final Reference<? extends Image> ref = tileCache.get(coords);
            if (ref == RENDERING) {
                // The tile is already queued for rendering. Return a dirty tile if
                // we have one.
//                System.out.print('r');
                return getDirtyTile(coords, dirtyTileCache, gc);
            } else if (ref != null) {
                final Image tile = ref.get();
                if (tile == null) {
                    // The image was garbage collected; remove the reference from
                    // the cache and schedule it to be rendered again
                    tileCache.remove(coords);
                    scheduleTile(tileCache, coords, tileProvider, dirtyTileCache, effectiveZoom, null);
//                    System.out.print('g');
                    return getDirtyTile(coords, dirtyTileCache, gc);
                } else if (tile == NO_TILE) {
                    // There is no tile here according to the tile provider
//                    System.out.print(' ');
                    return null;
                } else if (tile instanceof VolatileImage) {
                    switch (((VolatileImage) tile).validate(gc)) {
                        case VolatileImage.IMAGE_OK:
//                            System.out.print('.');
                            return tile;
                        case VolatileImage.IMAGE_RESTORED:
                            // The image was restored and the contents "may"
                            // have been affected. schedule it to be rendered
                            // again
                            // TODO: should we be returning it anyway?
                            scheduleTile(tileCache, coords, tileProvider, dirtyTileCache, effectiveZoom, tile);
//                            System.out.print('R');
                            return tile;
                        case VolatileImage.IMAGE_INCOMPATIBLE:
                            // Weirdly, the image is no longer compatible with
                            // the graphics configuration. Schedule it to be
                            // rendered again. Not much point in checking the
                            // dirty tile cache; those tiles probably aren't
                            // compatible any more also. TODO: can this even
                            // happen?
                            tileCache.remove(coords);
                            scheduleTile(tileCache, coords, tileProvider, dirtyTileCache, effectiveZoom, null);
//                            System.out.print('i');
                            return null;
                        default:
                            throw new InternalError("Unknown validation result");
                    }
                } else {
                    return tile;
                }
            } else {
                // Tile not present in cache
                scheduleTile(tileCache, coords, tileProvider, dirtyTileCache, effectiveZoom, null);
//                System.out.print('x');
                return getDirtyTile(coords, dirtyTileCache, gc);
            }
        }
    }

    private Image getDirtyTile(Point coords, Map<Point, Reference<? extends Image>> dirtyTileCache, GraphicsConfiguration gc) {
        final Reference<? extends Image> dirtyRef = dirtyTileCache.get(coords);
        if (dirtyRef != null) {
            final Image dirtyTile = dirtyRef.get();
            if (dirtyTile == null) {
                // The image was garbage collected; remove the reference
                // from the cache
                dirtyTileCache.remove(coords);
                return null;
            } else if (dirtyTile == NO_TILE) {
                // There was no tile here according to the tile provider
                return null;
            } else if (dirtyTile instanceof VolatileImage) {
                switch (((VolatileImage) dirtyTile).validate(gc)) {
                    case VolatileImage.IMAGE_OK:
                        return dirtyTile;
                    case VolatileImage.IMAGE_RESTORED:
                        // The image was restored and the contents "may" have
                        // been affected. Oh well, it was a dirty tile anyway
                        // TODO: should we be returning it anyway?
                        return dirtyTile;
                    case VolatileImage.IMAGE_INCOMPATIBLE:
                        // Weirdly, the image is no longer compatible with the
                        // graphics configuration. Oh well, it was a dirty tile
                        // anyway. TODO: can this even happen?
                        dirtyTileCache.remove(coords);
                        return null;
                    default:
                        throw new InternalError("Unknown validation result");
                }
            } else {
                return dirtyTile;
            }
        } else {
            return null;
        }
    }

    private void scheduleTile(final Map<Point, Reference<? extends Image>> tileCache, final Point coords, final TileProvider tileProvider, final Map<Point, Reference<? extends Image>> dirtyTileCache, final int effectiveZoom, final Image image) {
        synchronized (TILE_CACHE_LOCK) {
            if (tileProvider.isTilePresent(coords.x, coords.y)) {
                tileCache.put(coords, RENDERING);
                tileRenderers.execute(new TileRenderJob(tileCache, dirtyTileCache, coords, tileProvider, effectiveZoom, image));
            } else {
                tileCache.put(coords, new SoftReference<>(NO_TILE));
                if (dirtyTileCache.containsKey(coords)) {
                    dirtyTileCache.remove(coords);
                }
                repaint(getTileBounds(coords.x, coords.y, effectiveZoom));
            }
        }
    }

    private void fireViewChangedEvent() {
        if (viewListener != null) {
            viewListener.viewChanged(this);
        }
    }
    
    // ComponentListener
    
    @Override
    public void componentResized(ComponentEvent e) {
        xOffset = getWidth() / 2;
        yOffset = getHeight() / 2;
        fireViewChangedEvent();
        repaint();
    }
    
    @Override public void componentShown(ComponentEvent e) {}
    @Override public void componentMoved(ComponentEvent e) {}
    @Override public void componentHidden(ComponentEvent e) {}
    
    // TileListener

    @Override
    public void tileChanged(final TileProvider source, final int x, final int y) {
        if (SwingUtilities.isEventDispatchThread()) {
            refresh(source, x, y);
        } else {
            SwingUtilities.invokeLater(() -> refresh(source, x, y));
        }
    }

    @Override
    public void tilesChanged(final TileProvider source, final Set<Point> tiles) {
        if (SwingUtilities.isEventDispatchThread()) {
            refresh(source, tiles);
        } else {
            SwingUtilities.invokeLater(() -> refresh(source, tiles));
        }
    }

    // MouseListener
    
    @Override
    public void mousePressed(MouseEvent e) {
        if ((! leftClickDrags) && (e.getButton() == MouseEvent.BUTTON1)) {
            return;
        }
        previousX = e.getX();
        previousY = e.getY();
        setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        dragging = true;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if ((! leftClickDrags) && (e.getButton() == MouseEvent.BUTTON1)) {
            return;
        }
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        dragging = false;
    }
    
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}

    // MouseMotionListener
    
    @Override
    public void mouseDragged(MouseEvent e) {
        if (! dragging) {
            return;
        }
        int dx = e.getX() - previousX;
        int dy = e.getY() - previousY;
        viewX -= dx;
        viewY -= dy;
        previousX = e.getX();
        previousY = e.getY();
        fireViewChangedEvent();
        repaint();
    }

    @Override public void mouseMoved(MouseEvent e) {}
 
    // HierarchyListener
    
    @Override
    public void hierarchyChanged(HierarchyEvent event) {
        if (((event.getChangeFlags() & HierarchyEvent.DISPLAYABILITY_CHANGED) != 0)) {
            // The JIDE framework temporarily removes the view from the
            // hierarchy when the layout is reset, so we have to be prepared to
            // reinitialise the render queue when the view is re-added to the
            // hierarchy
            if (isDisplayable()) {
                if (! tileProviders.isEmpty()) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Starting " + threads + " tile rendering threads");
                    }
                    queue = new PriorityBlockingQueue<>();
                    tileRenderers = new ThreadPoolExecutor(threads, threads, 0, TimeUnit.MILLISECONDS, queue);
                }
            } else {
                if (tileRenderers != null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Shutting down " + threads + " tile rendering threads");
                    }
                    queue.clear();
                    tileRenderers.shutdownNow();
                    queue = null;
                    tileRenderers = null;
                }
            }
        }
    }
    
    private final boolean leftClickDrags, paintCentre;
    private final int threads;
    private final Object TILE_CACHE_LOCK = new Object();
    private final List<TileProvider> tileProviders = new ArrayList<>();
    private final Map<TileProvider, Map<Point, Reference<? extends Image>>> tileCaches = new HashMap<>(),
            dirtyTileCaches = new HashMap<>();
    private final Map<String, Overlay> overlays = new HashMap<>();
    protected int viewX, viewY, previousX, previousY, markerX, markerY, xOffset, yOffset;
    /**
     * The zoom level in the form of an exponent of 2. I.e. the scale is 2^n,
     * meaning that 0 represents no zoom, -1 means half size (so zooming out),
     * 1 means double size (so zooming in), etc.
     */
    private int zoom = 0, gridSize = 128;
    private ExecutorService tileRenderers;
    private boolean dragging, paintMarker, paintGrid;
    private BlockingQueue<Runnable> queue;
    private ViewListener viewListener;
//    private boolean firstPaint = true;
 
    public static final int TILE_SIZE = 128, TILE_SIZE_BITS = 7, TILE_SIZE_MASK = 0x7f;
    
    static final AtomicLong jobSeq = new AtomicLong(Long.MIN_VALUE);

    private static final Reference<VolatileImage> RENDERING = new SoftReference<>(null);
    private static final VolatileImage NO_TILE = new VolatileImage() {
        @Override public BufferedImage getSnapshot() {return null;}
        @Override public int getWidth() {return 0;}
        @Override public int getHeight() {return 0;}
        @Override public Graphics2D createGraphics() {return null;}
        @Override public int validate(GraphicsConfiguration gc) {return 0;}
        @Override public boolean contentsLost() {return false;}
        @Override public ImageCapabilities getCapabilities() {return null;}
        @Override public int getWidth(ImageObserver observer) {return 0;}
        @Override public int getHeight(ImageObserver observer) {return 0;}
        @Override public Object getProperty(String name, ImageObserver observer) {return null;}
    };
    private static final Font NORMAL_FONT = new Font("SansSerif", Font.PLAIN, 10);
    private static final Font BOLD_FONT = new Font("SansSerif", Font.BOLD, 10);
    private static final long serialVersionUID = 1L;
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TiledImageViewer.class);
    
    class TileRenderJob implements Runnable, Comparable<TileRenderJob> {
        TileRenderJob(Map<Point, Reference<? extends Image>> tileCache, Map<Point, Reference<? extends Image>> dirtyTileCache, Point coords, TileProvider tileProvider, int effectiveZoom, Image image) {
            this.tileCache = tileCache;
            this.dirtyTileCache = dirtyTileCache;
            this.coords = coords;
            this.tileProvider = tileProvider;
            this.effectiveZoom = effectiveZoom;
            this.image = image;
            seq = jobSeq.getAndIncrement();
            priority = tileProvider.getTilePriority(coords.x, coords.y);
        }
        
        @Override
        public void run() {
            if (logger.isTraceEnabled()) {
                logger.trace("Rendering tile " + coords.x + "," + coords.y);
            }
            final int tileSize = tileProvider.getTileSize();
            VolatileImage tile;
            GraphicsConfiguration gc = getGraphicsConfiguration();
            if (image instanceof VolatileImage) {
                // This image was previously created by us, here, so really it
                // should still be compatible
                tile = (VolatileImage) image;
            } else {
                tile = gc.createCompatibleVolatileImage(tileSize, tileSize);
                tile.validate(gc);
//                switch (tile.validate(gc)) {
//                    case VolatileImage.IMAGE_OK:
//                        // As expected
//                        break;
//                    case VolatileImage.IMAGE_RESTORED:
//                        // Weird, but shouldn't be a problem as we're about to paint it
//                        if (logger.isDebugEnabled()) {
//                            logger.debug("Volatile image validation result IMAGE_RESTORED right after creation!");
//                        }
//                        break;
//                    case VolatileImage.IMAGE_INCOMPATIBLE:
//                        logger.error("Volatile image validation result IMAGE_INCOMPATIBLE right after creation!");
//                        break;
//                }
            }
            tileProvider.paintTile(tile, coords.x, coords.y, 0, 0);
//            if (tile.validate(gc) != VolatileImage.IMAGE_OK) {
//                logger.error("Image not OK right after rendering!");
//            }
            synchronized (TILE_CACHE_LOCK) {
                tileCache.put(coords, new SoftReference<Image>(tile));
                if (dirtyTileCache.containsKey(coords)) {
                    dirtyTileCache.remove(coords);
                }
            }
            repaint(getTileBounds(coords.x, coords.y, effectiveZoom));
        }

        @Override
        public int compareTo(TileRenderJob o) {
            if (priority != o.priority) {
                return o.priority - priority;
            } else {
                return (seq > o.seq) ? 1 : -1;
            }
        }
        
        private final long seq;
        private final Map<Point, Reference<? extends Image>> tileCache, dirtyTileCache;
        private final Point coords;
        private final TileProvider tileProvider;
        private final int effectiveZoom, priority;
        private final Image image;
    }
    
    public interface ViewListener {
        void viewChanged(TiledImageViewer source);
    }

    class Overlay {
        Overlay(Component componentToTrack, String key, int x, BufferedImage image) {
            this.componentToTrack = componentToTrack;
            this.key = key;
            this.x = x;
            this.image = image;
        }

        final String key;
        final int x;
        final Component componentToTrack;
        final BufferedImage image;
    }
}