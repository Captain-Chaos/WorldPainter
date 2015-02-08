/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.util.swing;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

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
            Map<Point, Reference<BufferedImage>> dirtyTileCache = dirtyTileCaches.remove(tileProvider);
            Map<Point, Reference<BufferedImage>> tileCache = tileCaches.remove(tileProvider);
            // Add all live tile images from the tile cache to the dirty tile
            // cache, for use as dirty tile for the new tile provider
            for (Map.Entry<Point, Reference<BufferedImage>> entry: tileCache.entrySet()) {
                Reference<BufferedImage> tileImageRef = entry.getValue();
                if (tileImageRef != RENDERING) {
                    BufferedImage tileImage = tileImageRef.get();
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
            tileCaches.put(newTileProvider, new HashMap<Point, Reference<BufferedImage>>());
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
            tileCaches.put(tileProvider, new HashMap<Point, Reference<BufferedImage>>());
            dirtyTileCaches.put(tileProvider, new HashMap<Point, Reference<BufferedImage>>());
            if (tileRenderers == null) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("Starting " + threads + " tile rendering threads");
                }
                queue = new PriorityBlockingQueue<Runnable>();
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
    
    public int getZoom() {
        return zoom;
    }

    public void setZoom(int zoom) {
        setZoom(zoom, xOffset, yOffset);
    }
    
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
                    dirtyTileCaches.put(tileProvider, new HashMap<Point, Reference<BufferedImage>>());
                    tileCaches.put(tileProvider, new HashMap<Point, Reference<BufferedImage>>());
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
    
    public void refresh() {
        queue.clear();
        synchronized (TILE_CACHE_LOCK) {
            for (TileProvider tileProvider: tileProviders) {
                dirtyTileCaches.put(tileProvider, new HashMap<Point, Reference<BufferedImage>>());
                tileCaches.put(tileProvider, new HashMap<Point, Reference<BufferedImage>>());
            }
        }
        repaint();
    }
    
    public void refresh(TileProvider tileProvider, int x, int y) {
        synchronized (TILE_CACHE_LOCK) {
            Point coords = new Point(x, y);
            Map<Point, Reference<BufferedImage>> tileCache = tileCaches.get(tileProvider);
            Reference<BufferedImage> tileRef = tileCache.get(coords);
            if (tileRef != RENDERING) {
                tileCache.remove(coords);
                BufferedImage tile = (tileRef != null) ? tileRef.get() : null;
                if (tile != null) {
                    // The old tile is still available; move it to the dirty
                    // tile cache so we have something to paint while the tile
                    // is being rendered
                    dirtyTileCaches.get(tileProvider).put(coords, tileRef);
                }
                int effectiveZoom = (tileProvider.isZoomSupported() && (zoom < 0)) ? 0 : zoom;
                if (isTileVisible(x, y, effectiveZoom)) {
                    // The tile is visible; immediately schedule it to be
                    //rendered
                    scheduleTile(tileCache, coords, tileProvider, dirtyTileCaches.get(tileProvider), effectiveZoom);
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
        float scale = (float) Math.pow(2.0, zoom);
        if (zoom != 0) {
            g2.scale(scale, scale);
        }
        return scale;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(getBackground());
        Rectangle clipBounds = g.getClipBounds();
        if (tileProviders.isEmpty()) {
            g.fillRect(clipBounds.x, clipBounds.y, clipBounds.width, clipBounds.height);
            return;
        }
        
        for (TileProvider tileProvider: tileProviders) {
            int effectiveZoom = (tileProvider.isZoomSupported() && (zoom < 0)) ? 0 : zoom;
            Point topLeftTileCoords = viewToWorld(clipBounds.getLocation(), effectiveZoom);
            int leftTile = topLeftTileCoords.x >> TILE_SIZE_BITS;
            int topTile = topLeftTileCoords.y >> TILE_SIZE_BITS;
            Point bottomRightTileCoords = viewToWorld(new Point(clipBounds.x + clipBounds.width, clipBounds.y + clipBounds.height), effectiveZoom);
            int rightTile = bottomRightTileCoords.x >> TILE_SIZE_BITS;
            int bottomTile = bottomRightTileCoords.y >> TILE_SIZE_BITS;

            int middleTileX = (leftTile + rightTile) / 2;
            int middleTileY = (topTile + bottomTile) / 2;
            int radius = Math.max(
                Math.max(middleTileX - leftTile, rightTile - middleTileX),
                Math.max(middleTileY - topTile, bottomTile - middleTileY));

            paintTile(g, clipBounds, tileProvider, middleTileX, middleTileY, effectiveZoom);
            for (int r = 1; r <= radius; r++) {
                for (int i = 0; i <= (r * 2); i++) {
                    paintTile(g, clipBounds, tileProvider, middleTileX + i - r, middleTileY - r, effectiveZoom);
                    paintTile(g, clipBounds, tileProvider, middleTileX + r, middleTileY + i - r, effectiveZoom);
                    paintTile(g, clipBounds, tileProvider, middleTileX + r - i, middleTileY + r, effectiveZoom);
                    paintTile(g, clipBounds, tileProvider, middleTileX - r, middleTileY - i + r, effectiveZoom);
                }
            }
        }
        
        if (paintMarker) {
            g.setColor(Color.RED);
            Point markerCoords = worldToView(markerX, markerY);
            g.drawLine(markerCoords.x - 5, markerCoords.y,     markerCoords.x + 5, markerCoords.y);
            g.drawLine(markerCoords.x,     markerCoords.y - 5, markerCoords.x,     markerCoords.y + 5);
        }
        
        if (paintCentre) {
            int middleX = getWidth() / 2;
            int middleY = getHeight() / 2;
            g.setColor(Color.BLACK);
            g.drawLine(middleX - 4, middleY + 1, middleX + 6, middleY + 1);
            g.drawLine(middleX + 1, middleY - 4, middleX + 1, middleY + 6);
            g.setColor(Color.WHITE);
            g.drawLine(middleX - 5, middleY, middleX + 5, middleY);
            g.drawLine(middleX, middleY - 5, middleX, middleY + 5);
        }
        
        // Unschedule tiles which were scheduled to be rendered but are no
        // longer visible
        Rectangle viewBounds = new Rectangle(0, 0, getWidth(), getHeight());
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

    private void paintTile(Graphics g, Rectangle clipBounds, TileProvider tileProvider, int x, int y, int effectiveZoom) {
        Rectangle tileBounds = getTileBounds(x, y, effectiveZoom);
        if (! clipBounds.intersects(tileBounds)) {
            return;
        }
        BufferedImage tile = getTile(tileProvider, x, y, effectiveZoom);
        if (tile != null) {
            if (zoom > 0) {
                g.drawImage(tile, tileBounds.x, tileBounds.y, tileBounds.width, tileBounds.height, this);
            } else {
                g.drawImage(tile, tileBounds.x, tileBounds.y, this);
            }
        } else {
            g.fillRect(tileBounds.x, tileBounds.y, tileBounds.width, tileBounds.height);
        }
    }
    
    private BufferedImage getTile(TileProvider tileProvider, int x, int y, int effectiveZoom) {
        synchronized (TILE_CACHE_LOCK) {
            Point coords = new Point(x, y);
            Map<Point, Reference<BufferedImage>> tileCache = tileCaches.get(tileProvider),
                    dirtyTileCache = dirtyTileCaches.get(tileProvider);
            Reference<BufferedImage> ref = tileCache.get(coords);
            if (ref == RENDERING) {
                // The tile is already queued for rendering. Return a dirty tile if
                // we have one.
                Reference<BufferedImage> dirtyRef = dirtyTileCache.get(coords);
                BufferedImage dirtyTile = (dirtyRef != null) ? dirtyRef.get() : null;
                return (dirtyTile != NO_TILE) ? dirtyTile : null;
            }
            BufferedImage tile = (ref != null) ? ref.get() : null;
            if (tile == null) {
                Reference<BufferedImage> dirtyRef = dirtyTileCache.get(coords);
                if (dirtyRef != null) {
                    tile = dirtyRef.get();
                }
                scheduleTile(tileCache, coords, tileProvider, dirtyTileCache, effectiveZoom);
            }
            return (tile != NO_TILE) ? tile : null;
        }
    }

    private void scheduleTile(final Map<Point, Reference<BufferedImage>> tileCache, final Point coords, final TileProvider tileProvider, final Map<Point, Reference<BufferedImage>> dirtyTileCache, final int effectiveZoom) {
        synchronized (TILE_CACHE_LOCK) {
            tileCache.put(coords, RENDERING);
            tileRenderers.execute(new TileRenderJob(tileCache, dirtyTileCache, coords, tileProvider, effectiveZoom));
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
        Runnable task = new Runnable() {
            @Override
            public void run() {
                refresh(source, x, y);
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
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
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine("Starting " + threads + " tile rendering threads");
                    }
                    queue = new PriorityBlockingQueue<Runnable>();
                    tileRenderers = new ThreadPoolExecutor(threads, threads, 0, TimeUnit.MILLISECONDS, queue);
                }
            } else {
                if (tileRenderers != null) {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine("Shutting down " + threads + " tile rendering threads");
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
    private final List<TileProvider> tileProviders = new ArrayList<TileProvider>();
    private final Map<TileProvider, Map<Point, Reference<BufferedImage>>> tileCaches = new HashMap<TileProvider, Map<Point, Reference<BufferedImage>>>(),
            dirtyTileCaches = new HashMap<TileProvider, Map<Point, Reference<BufferedImage>>>();
    protected int viewX, viewY, previousX, previousY, markerX, markerY, xOffset, yOffset;
    /**
     * The zoom level in the form of an exponent of 2. I.e. the scale is 2^n,
     * meaning that 0 represents no zoom, -1 means half size (so zooming out),
     * 1 means double size (so zooming in), etc.
     */
    private int zoom = 0;
    private ExecutorService tileRenderers;
    private boolean dragging, paintMarker;
    private BlockingQueue<Runnable> queue;
    private ViewListener viewListener;
 
    public static final int TILE_SIZE = 128, TILE_SIZE_BITS = 7, TILE_SIZE_MASK = 0x7f;
    
    static final AtomicLong jobSeq = new AtomicLong(Long.MIN_VALUE);

    private static final Reference<BufferedImage> RENDERING = new SoftReference<BufferedImage>(null);
    private static final BufferedImage NO_TILE = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(TiledImageViewer.class.getName());
    
    class TileRenderJob implements Runnable, Comparable<TileRenderJob> {
        TileRenderJob(Map<Point, Reference<BufferedImage>> tileCache, Map<Point, Reference<BufferedImage>> dirtyTileCache, Point coords, TileProvider tileProvider, int effectiveZoom) {
            this.tileCache = tileCache;
            this.dirtyTileCache = dirtyTileCache;
            this.coords = coords;
            this.tileProvider = tileProvider;
            this.effectiveZoom = effectiveZoom;
            seq = jobSeq.getAndIncrement();
            priority = tileProvider.getTilePriority(coords.x, coords.y);
        }
        
        @Override
        public void run() {
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("Rendering tile " + coords);
            }
            BufferedImage tile = tileProvider.getTile(coords.x, coords.y);
            if (tile == null) {
                tile = NO_TILE;
            }
            synchronized (TILE_CACHE_LOCK) {
                tileCache.put(coords, new SoftReference<BufferedImage>(tile));
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
        private final Map<Point, Reference<BufferedImage>> tileCache, dirtyTileCache;
        private final Point coords;
        private final TileProvider tileProvider;
        private final int effectiveZoom, priority;
    }
    
    public interface ViewListener {
        void viewChanged(TiledImageViewer source);
    }
}