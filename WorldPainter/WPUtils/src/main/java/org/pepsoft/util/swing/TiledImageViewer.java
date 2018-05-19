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

import static org.pepsoft.util.GUIUtils.UI_SCALE;

/**
 * A generic visual component which can display one or more layers of large or
 * even endless tile-based images, with support for scrolling and scaling the
 * images.
 * 
 * <p>The tiles are provided by {@link TileProvider tile providers}. The tiles
 * are requested asynchronously on multiple threads and are cached. This means
 * that tile providers have to do no caching themselves and are free to
 * calculate or generate each tile on request, even if that is relatively slow.
 * 
 * <p>When zooming in, this viewer performs all the scaling itself. When zooming
 * out, for tile providers which indicate that they support zooming, the scaling
 * is delegated to the tile providers.
 *
 * <p>This class does not provide scrollbars, however it can be encapsulated in
 * a {@link TiledImageViewerContainer} which will surround it with scrollbars,
 * with support for the tile providers'
 * {@link TileProvider#getExtent() extents}.
 * 
 * @author pepijn
 */
public class TiledImageViewer extends JComponent implements TileListener, MouseListener, MouseMotionListener, ComponentListener, HierarchyListener {
    /**
     * Create a new tiled image viewer which allows panning by dragging with the
     * left mouse button, uses as many background tile rendering threads as
     * there are available processors and which paints the central crosshair.
     */
    public TiledImageViewer() {
        this(true, Runtime.getRuntime().availableProcessors(), true);
    }

    /**
     * Create a new tiled image viewer.
     *
     * @param leftClickDrags Whether dragging with the left mouse button should
     *                       pan the image.
     * @param threads The number of background tile rendering threads to use.
     * @param paintCentre Whether the central crosshair indicating the current
     *                    location should be painted.
     */
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

    /**
     * Get an unmodifiable view of the currently configured list of tile
     * providers.
     *
     * @return An unmodifyable view of the currently configured list of tile
     * providers.
     */
    public List<TileProvider> getTileProviders() {
        return Collections.unmodifiableList(tileProviders);
    }

    /**
     * Add a new tile provider to the end of the list of tile providers.
     *
     * @param tileProvider The tile provider to add.
     */
    public void addTileProvider(TileProvider tileProvider) {
        addTileProvider(tileProviders.size(), tileProvider);
    }

    /**
     * Get the number of currently configured tile providers.
     *
     * @return The number of currently configured tile providers.
     */
    public int getTileProviderCount() {
        return tileProviders.size();
    }

    /**
     * Set the first tile provider. Mainly meant as a convenience method for
     * clients that will only ever use one tile provider at a time. Will add the
     * tile provider if there are none yet configured, or replace the first tile
     * provider on the list if one or more tile providers are already set.
     *
     * @param tileProvider The tile provider to set.
     */
    public void setTileProvider(TileProvider tileProvider) {
        if (tileProviders.isEmpty()) {
            addTileProvider(tileProvider);
        } else {
            setTileProvider(0, tileProvider);
        }
    }

    /**
     * Replace a tile provider and remove all cached tile images for the
     * existing provider.
     *
     * @param index The index of the tile provider to replace.
     * @param tileProvider The tile provider with which to replace the existing
     *                     provider at the specified index.
     */
    public void setTileProvider(int index, TileProvider tileProvider) {
        removeTileProvider(index);
        addTileProvider(index, tileProvider);
    }

    /**
     * Replace a tile provider and reuse the existing provider's cached tile
     * images as stale tile images for the new provider.
     *
     * @param oldTileProvider The tile provider to replace.
     * @param newTileProvider The tile provider with which to replace it.
     */
    public void replaceTileProvider(TileProvider oldTileProvider, TileProvider newTileProvider) {
        replaceTileProvider(tileProviders.indexOf(oldTileProvider), newTileProvider);
    }
    
    /**
     * Replace a tile provider and reuse the existing provider's cached tile
     * images as stale tile images for the new provider.
     *
     * @param index The index of the tile provider to replace.
     * @param newTileProvider The tile provider with which to replace it.
     */
    public void replaceTileProvider(int index, TileProvider newTileProvider) {
        synchronized (TILE_CACHE_LOCK) {
            TileProvider tileProvider = tileProviders.remove(index);
            Point offset = offsets.remove(tileProvider);
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
            // We're not completely sure how, but sometimes we reach here
            // without the renderers having been started, so check whether there
            // actually is a queue
            if (queue != null) {
                // Prune the queue of jobs related to this tile provider
                for (Iterator<Runnable> i = queue.iterator(); i.hasNext(); ) {
                    if (((TileRenderJob) i.next()).tileProvider == tileProvider) {
                        i.remove();
                    }
                }
            }
            
            if (newTileProvider.isZoomSupported()) {
                newTileProvider.setZoom((zoom <= 0) ? zoom : 0);
            }
            newTileProvider.addTileListener(this);
            tileProviders.add(index, newTileProvider);
            offsets.put(newTileProvider, offset);
            tileCaches.put(newTileProvider, new HashMap<>());
            dirtyTileCaches.put(newTileProvider, dirtyTileCache);

            // We're not completely sure how, but sometimes we reach here
            // without the renderers having been started, so start them now (if
            // we're visible of course)
            startRenderersIfApplicable();
        }
        fireViewChangedEvent();
        repaint();
    }

    /**
     * Remove a tile provider.
     *
     * @param tileProvider The tile provider to remove.
     */
    public void removeTileProvider(TileProvider tileProvider) {
        removeTileProvider(tileProviders.indexOf(tileProvider));
    }
    
    /**
     * Remove a tile provider.
     *
     * @param index The index of the tile provider to remove.
     */
    public void removeTileProvider(int index) {
        synchronized (TILE_CACHE_LOCK) {
            TileProvider tileProvider = tileProviders.remove(index);
            offsets.remove(tileProvider);
            tileProvider.removeTileListener(this);
            tileCaches.remove(tileProvider);
            dirtyTileCaches.remove(tileProvider);
            // We're not completely sure how, but sometimes we reach here
            // without the renderers having been started, so check whether there
            // actually is a queue
            if (queue != null) {
                // Prune the queue of jobs related to this tile provider
                for (Iterator<Runnable> i = queue.iterator(); i.hasNext(); ) {
                    if (((TileRenderJob) i.next()).tileProvider == tileProvider) {
                        i.remove();
                    }
                }
            }
        }
        fireViewChangedEvent();
        repaint();
    }

    /**
     * Remove all tile providers.
     */
    public void removeAllTileProviders() {
        synchronized (TILE_CACHE_LOCK) {
            for (TileProvider tileProvider: tileProviders) {
                tileProvider.removeTileListener(this);
            }
            tileProviders.clear();
            offsets.clear();
            if (queue != null) {
                queue.clear();
            }
            tileCaches.clear();
            dirtyTileCaches.clear();
        }
        fireViewChangedEvent();
        repaint();
    }

    /**
     * Add or insert a new tile provider at a specific index in the list.
     *
     * @param index The index at which to add or insert the new tile provider.
     * @param tileProvider The tile provider to add or insert.
     */
    public void addTileProvider(int index, TileProvider tileProvider) {
        if (tileProvider == null) {
            throw new NullPointerException();
        }
        tileProviders.add(index, tileProvider);
        offsets.put(tileProvider, new Point());
        synchronized (TILE_CACHE_LOCK) {
            if (tileProvider.isZoomSupported()) {
                tileProvider.setZoom((zoom <= 0) ? zoom : 0);
            }
            tileProvider.addTileListener(this);
            tileCaches.put(tileProvider, new HashMap<>());
            dirtyTileCaches.put(tileProvider, new HashMap<>());
            startRenderersIfApplicable();
        }
        fireViewChangedEvent();
        repaint();
    }

    /**
     * Get the X coordinate in image coordinates of the centre of the view.
     * 
     * @return The X coordinate in image coordinates of the centre of the view.
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
     * Get the Y coordinate in image coordinates of the centre of the view.
     * 
     * @return The Y coordinate in image coordinates of the centre of the view.
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
     * @see TileProvider#getExtent()
     */
    public Rectangle getExtent() {
        Rectangle extent = null;
        for (TileProvider tileProvider: tileProviders) {
            Rectangle providerExtent = tileProvider.getExtent();
            if (providerExtent != null) {
                providerExtent = getTileBounds(tileProvider, providerExtent.x, providerExtent.y, providerExtent.width, providerExtent.height, zoom);
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
     * Get the coordinate in image coordinates of the centre of the view.
     * 
     * @return The coordinate in image coordinates of the centre of the view.
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

    public void resetZoom() {
        setZoom((UI_SCALE == 1) ? 0 : 1);
    }

    /**
     * Get the coordinates in image coordinates of the marker (displayed as a red
     * crosshair), if configured.
     *
     * @return The coordinates in image coordinates of the marker, or
     * <code>null</code> if no marker is configured.
     */
    public Point getMarkerCoords() {
        return paintMarker ? new Point(markerX, markerY) : null;
    }

    /**
     * Set the coordinates in image coordinates where a red crosshair marker
     * should be displayed, if any.
     *
     * @param markerCoords The coordinates in image coordinates where a red
     *                     crosshair marker should be displayed, or
     *                     <code>null</code> if no marker should be displayed.
     */
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
     * Determine whether the grid is currently painted.
     *
     * @return <code>true</code> if the grid is currently painted.
     */
    public boolean isPaintGrid() {
        return paintGrid;
    }

    /**
     * Set whether the gid should be painted.
     *
     * @param paintGrid <code>true</code> if the grid should be painted.
     */
    public void setPaintGrid(boolean paintGrid) {
        if (paintGrid != this.paintGrid) {
            this.paintGrid = paintGrid;
            repaint();
        }
    }

    /**
     * Get the current size in image coordinates of the grid.
     *
     * @return The current size in image coordinates of the grid.
     */
    public int getGridSize() {
        return gridSize;
    }

    /**
     * Set the size in image coordinates at which the grid should be painted.
     *
     * @param gridSize The size in image coordinates at which the grid should be
     *                 painted.
     */
    public void setGridSize(int gridSize) {
        if (gridSize != this.gridSize) {
            this.gridSize = gridSize;
            repaint();
        }
    }

    /**
     * Centre the view on a particular location in image coordinates.
     * 
     * @param coords The coordinates in image coordinates of the location to
     *     centre.
     */
    public void moveTo(Point coords) {
        moveTo(coords.x, coords.y);
    }
    
    /**
     * Centre the view on a particular location in image coordinates.
     * 
     * @param x The X coordinate in image coordinates of the location to centre.
     * @param y The Y coordinate in image coordinates of the location to centre.
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

    /**
     * Centre the view on the currently configured marker. Does nothing if no
     * marker is configured.
     */
    public void moveToMarker() {
        if (paintMarker) {
            moveTo(markerX, markerY);
        }
    }

    /**
     * Centre the view on the image origin (coordinates 0,0).
     */
    public void moveToOrigin() {
        moveTo(0, 0);
    }

    /**
     * Move the view by a number of pixels. The actual movement in image
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

    /**
     * Refresh a single tile for a single tile provider. If the tile is visible
     * it will immediately be scheduled for background rendering. Otherwise it
     * will be scheduled when it next becomes visible. If there is a fresh tile
     * image in the cache that will be used as a stale tile image until it has
     * been re-rendered.
     *
     * @param tileProvider The tile provider.
     * @param x The X coordinate of the tile in tiles relative to the image
     *          origin.
     * @param y The Y coordinate of the tile in tiles relative to the image
     *          origin.
     */
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
    
    /**
     * Refresh a number of tiles for a single tile provider. Tiles that are
     * currently visible will immediately be scheduled for background rendering.
     * Otherwise they will be scheduled when they next become visible. Any fresh
     * tile images in the cache will be used as stale tile images until the
     * tiles are re-rendered.
     *
     * @param tileProvider The tile provider.
     * @param tiles A set of tile coordinates of the tiles to refresh, in tiles
     *              relative to the image origin.
     */
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

    /**
     * Reset the location to the origin, and the zoom level to -2.
     */
    @Deprecated
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

    /**
     * Transform coordinates from image (world) coordinates to component (pixels
     * relative to the top left corner) coordinates, taking the current zoom
     * level into account.
     */
    public final Point worldToView(Point coords) {
        return worldToView(coords.x, coords.y);
    }
    
    /**
     * Transform coordinates from image (world) coordinates to component (pixels
     * relative to the top left corner) coordinates, taking the current zoom
     * level into account.
     */
    public final Point worldToView(int x, int y) {
        return (zoom == 0)
            ? new Point(x - viewX + xOffset, y - viewY + yOffset)
            : ((zoom < 0)
                ? new Point((x >> -zoom) - viewX + xOffset, (y >> -zoom) - viewY + yOffset)
                : new Point((x << zoom) - viewX + xOffset, (y << zoom) - viewY + yOffset));
    }
    
    /**
     * Transform coordinates from component (pixels relative to the top left
     * corner) coordinates to image (world) coordinates, taking the current zoom
     * level into account.
     */
    public final Point viewToWorld(Point coords) {
        return viewToWorld(coords.x, coords.y, zoom);
    }
    
    /**
     * Transform coordinates from component (pixels relative to the top left
     * corner) coordinates to image (world) coordinates, taking the current zoom
     * level into account. This version does not take tile-provider-specific
     * offset into acount.
     */
    public final Point viewToWorld(int x, int y) {
        return viewToWorld(x, y, zoom);
    }
    
    /**
     * Transform coordinates from component (pixels relative to the top left
     * corner) coordinates to image (world) coordinates, taking the current zoom
     * level into account and for the specified tile provider.
     *
     * @throws UnknownTileProviderException If the specified tile provider is
     *     not configured on this image viewer.
     */
    public final Point viewToWorld(TileProvider tileProvider, int x, int y) {
        return viewToWorld(tileProvider, x, y, zoom);
    }

    /**
     * Transform coordinates from component (pixels relative to the top left
     * corner) coordinates to image (world) coordinates, using a specific zoom
     * level. This version does not take per-tile-provider offsets into account.
     */
    public final Point viewToWorld(Point coords, int effectiveZoom) {
        return viewToWorld(coords.x, coords.y, effectiveZoom);
    }
    
    /**
     * Transform coordinates from component (pixels relative to the top left
     * corner) coordinates to image (world) coordinates, using a specific zoom
     * level and for a specific tile provider.
     *
     * @throws UnknownTileProviderException If the specified tile provider is
     *     not configured on this image viewer.
     */
    public final Point viewToWorld(TileProvider tileProvider, Point coords, int effectiveZoom) {
        return viewToWorld(tileProvider, coords.x, coords.y, effectiveZoom);
    }

    /**
     * Transform coordinates from component (pixels relative to the top left
     * corner) coordinates to image (world) coordinates, using a specific zoom
     * level. This version does not take per-tile-provider offsets into account.
     */
    public final Point viewToWorld(int x, int y, int effectiveZoom) {
        return (effectiveZoom == 0)
            ? new Point(x + viewX - xOffset, y + viewY - yOffset)
            : ((effectiveZoom < 0)
                ? new Point((x + viewX - xOffset) << -effectiveZoom, (y + viewY - yOffset) << -effectiveZoom)
                : new Point((x + viewX - xOffset) >> effectiveZoom, (y + viewY - yOffset) >> effectiveZoom));
    }
    
    /**
     * Transform coordinates from component (pixels relative to the top left
     * corner) coordinates to image (world) coordinates, using a specific zoom
     * level and for a specific tile provider.
     *
     * @throws UnknownTileProviderException If the specified tile provider is
     *     not configured on this image viewer.
     */
    public final Point viewToWorld(TileProvider tileProvider, int x, int y, int effectiveZoom) {
        try {
            Point myOffset = offsets.get(tileProvider);
            return (effectiveZoom == 0)
                    ? new Point(x + viewX - xOffset - myOffset.x, y + viewY - yOffset - myOffset.y)
                    : ((effectiveZoom < 0)
                    ? new Point(((x + viewX - xOffset) << -effectiveZoom) - myOffset.x, ((y + viewY - yOffset) << -effectiveZoom) - myOffset.y)
                    : new Point(((x + viewX - xOffset) >> effectiveZoom) - myOffset.x, ((y + viewY - yOffset) >> effectiveZoom) - myOffset.y));
        } catch (NullPointerException e) {
            throw new UnknownTileProviderException(tileProvider);
        }
    }

    /**
     * Transform coordinates from image (world) coordinates to component (pixels
     * relative to the top left corner) coordinates, taking the current zoom
     * level into account.
     */
    public final Rectangle worldToView(Rectangle coords) {
        return worldToView(coords.x, coords.y, coords.width, coords.height, zoom);
    }
    
    /**
     * Transform coordinates from image (world) coordinates to component (pixels
     * relative to the top left corner) coordinates, taking the current zoom
     * level into account.
     */
    public final Rectangle worldToView(int x, int y, int width, int height) {
        return worldToView(x, y, width, height, zoom);
    }
    
    /**
     * Transform coordinates from image (world) coordinates to component (pixels
     * relative to the top left corner) coordinates, using a specific zoom
     * level.
     */
    public final Rectangle worldToView(Rectangle coords, int effectiveZoom) {
        return worldToView(coords.x, coords.y, coords.width, coords.height, effectiveZoom);
    }
    
    /**
     * Transform coordinates from image (world) coordinates to component (pixels
     * relative to the top left corner) coordinates, using a specific zoom
     * level. This version does not take tile-provider-specific offsets into
     * account.
     */
    public final Rectangle worldToView(int x, int y, int width, int height, int effectiveZoom) {
        return (effectiveZoom == 0)
            ? new Rectangle(x - viewX + xOffset, y - viewY + yOffset, width, height)
            : ((effectiveZoom < 0)
                ? new Rectangle((x >> -effectiveZoom) - viewX + xOffset, (y >> -effectiveZoom) - viewY + yOffset, width >> -effectiveZoom, height >> -effectiveZoom)
                : new Rectangle((x << effectiveZoom) - viewX + xOffset, (y << effectiveZoom) - viewY + yOffset, width << effectiveZoom, height << effectiveZoom));
    }
    
    /**
     * Transform coordinates from image (world) coordinates to component (pixels
     * relative to the top left corner) coordinates, using a specific zoom
     * level and for a specific tile provider.
     *
     * @throws UnknownTileProviderException If the specified tile provider is
     *     not configured on this image viewer.
     */
    public final Rectangle worldToView(TileProvider tileProvider, int x, int y, int width, int height, int effectiveZoom) {
        try {
            Point myOffset = offsets.get(tileProvider);
            return (effectiveZoom == 0)
                    ? new Rectangle(x - viewX + xOffset + myOffset.x, y - viewY + yOffset + myOffset.y, width, height)
                    : ((effectiveZoom < 0)
                    ? new Rectangle(((x + myOffset.x) >> -effectiveZoom) - viewX + xOffset, ((y + myOffset.y) >> -effectiveZoom) - viewY + yOffset, width >> -effectiveZoom, height >> -effectiveZoom)
                    : new Rectangle(((x + myOffset.x) << effectiveZoom) - viewX + xOffset, ((y + myOffset.y) << effectiveZoom) - viewY + yOffset, width << effectiveZoom, height << effectiveZoom));
        } catch (NullPointerException e) {
            throw new UnknownTileProviderException(tileProvider);
        }
    }

    /**
     * Transform coordinates from component (pixels relative to the top left
     * corner) coordinates to image (world) coordinates, taking the current zoom
     * level into account.
     */
    public final Rectangle viewToWorld(Rectangle coords) {
        return viewToWorld(coords.x, coords.y, coords.width, coords.height);
    }
    
    /**
     * Transform coordinates from component (pixels relative to the top left
     * corner) coordinates to image (world) coordinates, taking the current zoom
     * level into account.
     */
    public final Rectangle viewToWorld(int x, int y, int width, int height) {
        return (zoom == 0)
            ? new Rectangle(x + viewX - xOffset, y + viewY - yOffset, width, height)
            : ((zoom < 0)
                ? new Rectangle((x + viewX - xOffset) << -zoom, (y + viewY - yOffset) << -zoom, width << -zoom, height << -zoom)
                : new Rectangle((x + viewX - xOffset) >> zoom, (y + viewY - yOffset) >> zoom, width >> zoom, height >> zoom));
    }

    /**
     * Set the view listener.
     *
     * @return The view listener to set.
     */
    public ViewListener getViewListener() {
        return viewListener;
    }

    /**
     * Get the currently configured view listener, if any.
     *
     * @param viewListener The currently configured view listener, or
     *                     <code>null</code> if there is none.
     */
    public void setViewListener(ViewListener viewListener) {
        this.viewListener = viewListener;
    }

    /**
     * Add an overlay. An overlay is an image which is overlaid on the viewport,
     * on the left or right edge of the view, vertically tracking some other
     * component. The image may be partially transparent.
     *
     * @param key The unique key of the overlay to add.
     * @param x The horizontal distance from the left edge to paint the overlay,
     *          or if negative: the horizontal distance from the right edge.
     * @param componentToTrack The component of which the height should be
     *                         tracked.
     * @param overlay The image to overlay on the view.
     */
    public void addOverlay(String key, int x, Component componentToTrack, BufferedImage overlay) {
        overlays.put(key, new Overlay(componentToTrack, key, x, overlay));
        repaint();
    }

    /**
     * Remove a previously added overlay.
     *
     * @param key The unique key of the overlay to remove.
     */
    public void removeOverlay(String key) {
        if (overlays.containsKey(key)) {
            overlays.remove(key);
            repaint();
        }
    }

    /**
     * Get the colour in which the grid is painted.
     *
     * @return The colour in which the grid is painted.
     */
    public Color getGridColour() {
        return gridColour;
    }

    public void setTileProviderOffset(TileProvider tileProvider, Point offset) {
        if (! offset.equals(offsets.get(tileProvider))) {
            System.out.println("Offset -> " + offset);
            offsets.put(tileProvider, offset);
            fireViewChangedEvent();
            repaint();
        }
    }

    public Point getTileProviderOffset(TileProvider tileProvider) {
        return offsets.get(tileProvider);
    }

    public void moveTileProviderBy(TileProvider tileProvider, int dx, int dy) {
        Point offset = offsets.get(tileProvider);
        offset.x += dx;
        offset.y += dy;
        fireViewChangedEvent();
        repaint();
    }

    /**
     * Set the colour in which to paint the grid.
     *
     * @param gridColour The colour in which to paint the grid.
     */
    public void setGridColour(Color gridColour) {
        if (gridColour == null) {
            throw new NullPointerException();
        }
        if (! gridColour.equals(this.gridColour)) {
            this.gridColour = gridColour;
            repaint();
        }
    }

    public BufferedImage getBackgroundImage() {
        return backgroundImage;
    }

    public void setBackgroundImage(BufferedImage backgroundImage) {
        this.backgroundImage = backgroundImage;
        repaint();
    }

    public BackgroundImageMode getBackgroundImageMode() {
        return backgroundImageMode;
    }

    public void setBackgroundImageMode(BackgroundImageMode backgroundImageMode) {
        if (backgroundImageMode != this.backgroundImageMode) {
            this.backgroundImageMode = backgroundImageMode;
            if (backgroundImage != null) {
                repaint();
            }
        }
    }

    public boolean isInhibitUpdates() {
        return inhibitUpdates;
    }

    public void setInhibitUpdates(boolean inhibitUpdates) {
        if (inhibitUpdates != this.inhibitUpdates) {
            this.inhibitUpdates = inhibitUpdates;
            if (! inhibitUpdates) {
                refresh(true);
            }
        }
    }

    /**
     * Determine whether a tile is currently visible in the viewport.
     *
     * @param x The X coordinate of the tile to check for visibility.
     * @param y The Y coordinate of the tile to check for visibility.
     * @param effectiveZoom The zoom level to take into account.
     * @return <code>true</code> if any part of the specified tile intersects
     * the viewport.
     */
    protected final boolean isTileVisible(int x, int y, int effectiveZoom) {
        return new Rectangle(0, 0, getWidth(), getHeight()).intersects(getTileBounds(x, y, effectiveZoom));
    }

    /**
     * Get the bounds of a tile in component coordinates, taking the current
     * zoom level into account.
     *
     * @param x The X coordinate of the tile for which to determine the bounds.
     * @param y The X coordinate of the tile for which to determine the bounds.
     * @return The area in component coordinates taken up by the specified tile.
     */
    protected final Rectangle getTileBounds(int x, int y) {
        return getTileBounds(x, y, zoom);
    }

    /**
     * Get the bounds of a tile in component coordinates, taking a specific
     * zoom level into account. This version does not take per-tile-provider
     * offsets into account.
     *
     * @param x The X coordinate of the tile for which to determine the bounds.
     * @param y The X coordinate of the tile for which to determine the bounds.
     * @param effectiveZoom The zoom level to take into account.
     * @return The area in component coordinates taken up by the specified tile.
     */
    protected final Rectangle getTileBounds(int x, int y, int effectiveZoom) {
        return worldToView(x << TILE_SIZE_BITS, y << TILE_SIZE_BITS, TILE_SIZE, TILE_SIZE, effectiveZoom);
    }
    
    /**
     * Get the bounds of a tile in component coordinates, taking a specific
     * zoom level into account and for a specific tile provider.
     *
     * @param x The X coordinate of the tile for which to determine the bounds.
     * @param y The X coordinate of the tile for which to determine the bounds.
     * @param effectiveZoom The zoom level to take into account.
     * @return The area in component coordinates taken up by the specified tile.
     * @throws UnknownTileProviderException If the specified tile provider is
     *     not configured on this image viewer.
     */
    protected final Rectangle getTileBounds(TileProvider tileProvider, int x, int y, int effectiveZoom) {
        return worldToView(tileProvider, x << TILE_SIZE_BITS, y << TILE_SIZE_BITS, TILE_SIZE, TILE_SIZE, effectiveZoom);
    }

    /**
     * Get the bounds of a rectangular area of tiles in component coordinates,
     * taking a specific zoom level into account. This version does not take
     * tile-provider-specific offsets into account.
     *
     * @param x The X coordinate of the top left tile of the area for which to
     *          determine the bounds.
     * @param y The X coordinate of the top left tile of the area for which to
     *          determine the bounds.
     * @param width The width in tiles of the area for which to determine the
     *              bounds.
     * @param height The height in tiles of the area for which to determine the
     *               bounds.
     * @param effectiveZoom The zoom level to take into account.
     * @return The area in component coordinates taken up by the specified
     * rectangle of tiles.
     */
    protected final Rectangle getTileBounds(int x, int y, int width, int height, int effectiveZoom) {
        return worldToView(x << TILE_SIZE_BITS, y << TILE_SIZE_BITS, TILE_SIZE * width, TILE_SIZE * height, effectiveZoom);
    }

    /**
     * Get the bounds of a rectangular area of tiles in component coordinates,
     * taking a specific zoom level into account and for a specific tile
     * provider.
     *
     * @param x The X coordinate of the top left tile of the area for which to
     *          determine the bounds.
     * @param y The X coordinate of the top left tile of the area for which to
     *          determine the bounds.
     * @param width The width in tiles of the area for which to determine the
     *              bounds.
     * @param height The height in tiles of the area for which to determine the
     *               bounds.
     * @param effectiveZoom The zoom level to take into account.
     * @return The area in component coordinates taken up by the specified
     * rectangle of tiles.
     * @throws UnknownTileProviderException If the specified tile provider is
     *     not configured on this image viewer.
     */
    protected final Rectangle getTileBounds(TileProvider tileProvider, int x, int y, int width, int height, int effectiveZoom) {
        return worldToView(tileProvider, x << TILE_SIZE_BITS, y << TILE_SIZE_BITS, TILE_SIZE * width, TILE_SIZE * height, effectiveZoom);
    }

    /**
     * Apply translation and scaling to a graphics canvas according to the
     * current location and zoom settings such that it can be painted using
     * image coordinates.
     *
     * @param g2 The graphics canvas to which to apply the transforms.
     * @return The scaling factor to apply to image coordinates to account for
     * the zoom level.
     */
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

    private void paintMarkerIfApplicable(Graphics g2) {
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

    private void startRenderersIfApplicable() {
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
            int effectiveGridSize = gridSize;
            if (zoom < 0) {
                // Increase the effective grid size if necessary to prevent the
                // lines being too close together
                int minGridSize = Math.min(gridSize, 32);
                while ((effectiveGridSize >> -zoom) < minGridSize) {
                    effectiveGridSize *= 2;
                }
            }

            // Determine which grid lines to draw, in image coordinates
            final Rectangle clipInWorld = viewToWorld(g2.getClipBounds());
            final int x1 = ((clipInWorld.x / effectiveGridSize) - 1) * effectiveGridSize;
            final int x2 = ((clipInWorld.x + clipInWorld.width) / effectiveGridSize + 1) * effectiveGridSize;
            final int y1 = ((clipInWorld.y / effectiveGridSize) - 1) * effectiveGridSize;
            final int y2 = ((clipInWorld.y + clipInWorld.height) / effectiveGridSize + 1) * effectiveGridSize;
            g2.setColor(gridColour);

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
            int xLabelSkip = effectiveGridSize, yLabelSkip = effectiveGridSize;
            final float scale = (float) Math.pow(2.0, getZoom());

            // Determine per how many grid lines minimum a label can be draw
            // so that they don't obscure one another, for the horizontal and
            // vertical direction
            while ((xLabelSkip * scale) < fontWidth) {
                xLabelSkip += effectiveGridSize;
            }
            while ((yLabelSkip * scale) < fontHeight) {
                yLabelSkip += effectiveGridSize;
            }

            // Initial setup of the graphics canvas
            g2.setStroke(normalStroke);
            g2.setFont(NORMAL_FONT);
            boolean normalFontInstalled = true;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
            boolean normalStrokeInstalled = true;

            // Draw the vertical grid lines and corresponding labels
            for (int x = x1; x <= x2; x += effectiveGridSize) {
                if ((x == 0) || (drawRegionBorders && ((x % 512) == 0))) {
                    g2.setStroke(regionBorderStroke);
                    normalStrokeInstalled = false;
                } else if (!normalStrokeInstalled) {
                    g2.setStroke(normalStroke);
                    normalStrokeInstalled = true;
                }
                Point lineStartInView = worldToView(x, 0);
                if (lineStartInView.x + 2 >= leftClear) {
                    if ((x % xLabelSkip) == 0) {
                        g2.drawLine(lineStartInView.x, 0, lineStartInView.x, height);
                        if (drawRegionBorders && ((x % 512) == 0)) {
                            g2.setFont(BOLD_FONT);
                            normalFontInstalled = false;
                        } else if (!normalFontInstalled) {
                            g2.setFont(NORMAL_FONT);
                            normalFontInstalled = true;
                        }
                        g2.drawString(Integer.toString(x), lineStartInView.x + 2, fontHeight + 2);
                    } else {
                        g2.drawLine(lineStartInView.x, topClear, lineStartInView.x, height);
                    }
                }
            }

            // Draw the horizontal grid lines and corresponding labels
            for (int y = y1; y <= y2; y += effectiveGridSize) {
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
        Rectangle clipBounds = g2.getClipBounds();
        g2.setColor(getBackground());
        paintBackground(g2, clipBounds);
        if (tileProviders.isEmpty()) {
            return;
        }

        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        GraphicsConfiguration gc = getGraphicsConfiguration();
        for (TileProvider tileProvider : tileProviders) {
            final int effectiveZoom = (tileProvider.isZoomSupported() && (zoom < 0)) ? 0 : zoom;
            final Point topLeftTileCoords = viewToWorld(tileProvider, clipBounds.getLocation(), effectiveZoom);
            final int leftTile = topLeftTileCoords.x >> TILE_SIZE_BITS;
            final int topTile = topLeftTileCoords.y >> TILE_SIZE_BITS;
            final Point bottomRightTileCoords = viewToWorld(tileProvider, new Point(clipBounds.x + clipBounds.width - 1, clipBounds.y + clipBounds.height - 1), effectiveZoom);
            final int rightTile = bottomRightTileCoords.x >> TILE_SIZE_BITS;
            final int bottomTile = bottomRightTileCoords.y >> TILE_SIZE_BITS;

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

        paintGridIfApplicable(g2);

        paintMarkerIfApplicable(g2);

        int myWidth = getWidth();
        int myHeight = getHeight();
        if (paintCentre) {
            final int middleX = myWidth / 2;
            final int middleY = myHeight / 2;
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
        final Rectangle viewBounds = new Rectangle(0, 0, myWidth, myHeight);
        synchronized (TILE_CACHE_LOCK) {
            for (Iterator<Runnable> i = queue.iterator(); i.hasNext(); ) {
                TileRenderJob job = (TileRenderJob) i.next();
                if (! getTileBounds(job.tileProvider, job.coords.x, job.coords.y, job.effectiveZoom).intersects(viewBounds)) {
                    i.remove();
                    // Remove the RENDERING flag for this tile from the cache,
                    // otherwise it won't be rendered the next time it becomes
                    // visible:
                    tileCaches.get(job.tileProvider).remove(job.coords);
                }
            }
        }
    }

    private void paintBackground(Graphics2D g2, Rectangle clipBounds) {
        if (backgroundImage != null) {
            int width = getWidth(), height = getHeight();
            switch (backgroundImageMode) {
                case CENTRE:
                    g2.fillRect(clipBounds.x, clipBounds.y, clipBounds.width, clipBounds.height);
                    int imageWidth = backgroundImage.getWidth();
                    int imageHeight = backgroundImage.getHeight();
                    int imageX = (width - imageWidth) / 2;
                    int imageY = (height - imageHeight) / 2;
                    if (clipBounds.intersects(imageX, imageY, imageWidth, imageHeight)) {
                        g2.drawImage(backgroundImage, imageX, imageY, null);
                    }
                    break;
                case CENTRE_REPEAT:
                    if (backgroundImage.getTransparency() != Transparency.OPAQUE) {
                        g2.fillRect(clipBounds.x, clipBounds.y, clipBounds.width, clipBounds.height);
                    }
                    repeatImage(g2, clipBounds, backgroundImage, (width - backgroundImage.getWidth()) / 2, (height - backgroundImage.getHeight()) / 2, backgroundImage.getWidth(), backgroundImage.getHeight());
                    break;
                case FIT:
                case FIT_REPEAT:
                    imageWidth = backgroundImage.getWidth();
                    imageHeight = backgroundImage.getHeight();
                    float myRatio = (float) width / height;
                    float imageRatio = (float) imageWidth / imageHeight;
                    if (imageRatio > myRatio) {
                        imageWidth = width;
                        imageHeight = (int) (imageWidth / imageRatio);
                    } else {
                        imageHeight = height;
                        imageWidth = (int) (imageHeight * imageRatio);
                    }
                    imageX = (width - imageWidth) / 2;
                    imageY = (height - imageHeight) / 2;
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    if (backgroundImageMode == TiledImageViewer.BackgroundImageMode.FIT) {
                        g2.fillRect(clipBounds.x, clipBounds.y, clipBounds.width, clipBounds.height);
                        if (clipBounds.intersects(imageX, imageY, imageWidth, imageHeight)) {
                            g2.drawImage(backgroundImage, imageX, imageY, imageWidth, imageHeight, null);
                        }
                    } else {
                        if (backgroundImage.getTransparency() != Transparency.OPAQUE) {
                            g2.fillRect(clipBounds.x, clipBounds.y, clipBounds.width, clipBounds.height);
                        }
                        repeatImage(g2, clipBounds, backgroundImage, imageX, imageY, imageWidth, imageHeight);
                    }
                    break;
                case REPEAT:
                    if (backgroundImage.getTransparency() != Transparency.OPAQUE) {
                        g2.fillRect(clipBounds.x, clipBounds.y, clipBounds.width, clipBounds.height);
                    }
                    repeatImage(g2, clipBounds, backgroundImage, 0, 0, backgroundImage.getWidth(), backgroundImage.getHeight());
                    break;
                case STRETCH:
                    if (backgroundImage.getTransparency() != Transparency.OPAQUE) {
                        g2.fillRect(clipBounds.x, clipBounds.y, clipBounds.width, clipBounds.height);
                    }
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2.drawImage(backgroundImage, 0, 0, width, height, null);
                    break;
            }
        } else {
            g2.fillRect(clipBounds.x, clipBounds.y, clipBounds.width, clipBounds.height);
        }
    }

    private void repeatImage(final Graphics2D g2, Rectangle clipBounds, final BufferedImage image, int x, int y, final int width, final int height) {
        while (y > 0) y-= height;
        do {
            while (x > 0) x -= width;
            do {
                if (clipBounds.intersects(x, y, width, height)) {
                    g2.drawImage(image, x, y, width, height, null);
                }
                x += width;
            } while (x < getWidth());
            y += height;
        } while (y < getHeight());
    }

    private void paintOverlays(Graphics2D g2) {
        overlays.values().forEach(overlay -> {
            int x = overlay.x >= 0 ? overlay.x : getWidth() + overlay.x;
            Point coords = SwingUtilities.convertPoint(overlay.componentToTrack, 0, 0, this);
            g2.drawImage(overlay.image, x, coords.y, null);
        });
    }

    /**
     * Immediately paint a specific tile from a specific provider. If a fresh
     * tile is not available the tile will be scheduled for repainting in the
     * background, unless the tile provider indicates that the tile is not
     * present. If a stale version of the tile is available that will be
     * painted, otherwise the area of the tile will be filled with the canvas'
     * current colour.
     *
     * @param g2 The canvas on which to paint the tile.
     * @param gc The graphics configuration associated with the canvas; used for
     *           volatile (accelerated) image management.
     * @param tileProvider The tile provider.
     * @param x The X coordinate of the tile to paint, in tiles relative to the
     *          image origin.
     * @param y The Y coordinate of the tile to paint, in tiles relative to the
     *          image origin.
     * @param effectiveZoom The zoom level to apply.
     * @throws UnknownTileProviderException If the specified tile provider is
     *     not configured on this image viewer.
     */
    private void paintTile(Graphics2D g2, GraphicsConfiguration gc, TileProvider tileProvider, int x, int y, int effectiveZoom) {
        Rectangle tileBounds = getTileBounds(tileProvider, x, y, effectiveZoom);
        Image tile = getTile(tileProvider, x, y, effectiveZoom, gc);
        if (tile != null) {
            if (zoom > 0) {
                g2.drawImage(tile, tileBounds.x, tileBounds.y, tileBounds.width, tileBounds.height, this);
            } else {
                g2.drawImage(tile, tileBounds.x, tileBounds.y, this);
            }
        }
    }

    /**
     * Get a cached copy of a specific tile from a specific provider. If a fresh
     * tile is available it will be returned. Otherwise the tile will be
     * scheduled for repainting in the background, unless the tile provider
     * indicates that the tile is not present. If a stale version of the tile is
     * available that will be returned, otherwise <code>null</code> will be
     * returned.
     *
     * @param tileProvider The tile provider.
     * @param x The X coordinate of the tile to get, in tiles relative to the
     *          image origin.
     * @param y The Y coordinate of the tile to get, in tiles relative to the
     *          image origin.
     * @param effectiveZoom The zoom level to apply.
     * @param gc The graphics configuration to use for volatile (accelerated)
     *           image management.
     * @return The freshest copy of the tile available from the cache, or
     * <code>null</code> if no version of the tile is available, or if the tile
     * is not present according to the tile provider.
     */
    private Image getTile(TileProvider tileProvider, int x, int y, int effectiveZoom, GraphicsConfiguration gc) {
        synchronized (TILE_CACHE_LOCK) {
            final Point coords = new Point(x, y);
            final Map<Point, Reference<? extends Image>> tileCache = tileCaches.get(tileProvider),
                    dirtyTileCache = dirtyTileCaches.get(tileProvider);
            if ((tileCache == null) || (dirtyTileCache == null)) {
                // We have reports from the wild about this happening. It has to
                // do with the 3D dynmap previews and happens when adding custom
                // objects. TODO: how is that possible? Race condition? Threading issue?
                logger.warn("tileCache or dirtyTileCache null! Proceeding without a tile...");
                return null;
            }
            final Reference<? extends Image> ref = tileCache.get(coords);
            if (ref == RENDERING) {
                // The tile is already queued for rendering. Return a dirty tile if
                // we have one.
                return getDirtyTile(coords, dirtyTileCache, gc);
            } else if (ref != null) {
                final Image tile = ref.get();
                if (tile == null) {
                    // The image was garbage collected; remove the reference from
                    // the cache and schedule it to be rendered again
                    tileCache.remove(coords);
                    scheduleTile(tileCache, coords, tileProvider, dirtyTileCache, effectiveZoom, null);
                    return getDirtyTile(coords, dirtyTileCache, gc);
                } else if (tile == NO_TILE) {
                    // There is no tile here according to the tile provider
                    return null;
                } else if (tile instanceof VolatileImage) {
                    switch (((VolatileImage) tile).validate(gc)) {
                        case VolatileImage.IMAGE_OK:
                            return tile;
                        case VolatileImage.IMAGE_RESTORED:
                            // The image was restored and the contents "may"
                            // have been affected. schedule it to be rendered
                            // again
                            // TODO: should we be returning it anyway?
                            scheduleTile(tileCache, coords, tileProvider, dirtyTileCache, effectiveZoom, tile);
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
                return getDirtyTile(coords, dirtyTileCache, gc);
            }
        }
    }

    /**
     * Get a cached stale copy of a specific tile from a specific provider.
     *
     * <p><strong>Please note:</strong> this method must be invoked while
     * holding the lock on {@link #TILE_CACHE_LOCK}.
     *
     * @param coords The coordinates of the tile to get, in tiles relative to
     *               the image origin.
     * @param dirtyTileCache The cache from which to get the stale tile.
     * @param gc The graphics configuration to use for volatile (accelerated)
     *           image management.
     * @return The stale copy of the tile from the cache, or <code>null</code>
     * if the tile is not available from the cache, or if the tile is not
     * present according to the tile provider.
     */
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

    /**
     * Schedule a tile for background rendering, or remove it from the cache if
     * the tile provider indicates it is not present.
     *
     * @param tileCache The cache in which the rendered tile should be stored.
     * @param coords The coordinates of the tile to render, in tiles relative to
     *               the image origin.
     * @param tileProvider The tile provider.
     * @param dirtyTileCache The stale tile cache in which any currently cached
     *                       version of the tile will be stored as a stale copy.
     * @param effectiveZoom The zoom level to apply.
     * @param image The currently cached tile image for the tile, if any.
     */
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
                try {
                    repaint(getTileBounds(tileProvider, coords.x, coords.y, effectiveZoom));
                } catch (UnknownTileProviderException e) {
                    // This means the tile provider is no longer configured on
                    // this image viewer, meaning there's not much point in us
                    // trying to paint it, so give up silently
                }
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
        if (! inhibitUpdates) {
            if (SwingUtilities.isEventDispatchThread()) {
                refresh(source, x, y);
            } else {
                SwingUtilities.invokeLater(() -> refresh(source, x, y));
            }
        }
    }

    @Override
    public void tilesChanged(final TileProvider source, final Set<Point> tiles) {
        if (! inhibitUpdates) {
            if (SwingUtilities.isEventDispatchThread()) {
                refresh(source, tiles);
            } else {
                SwingUtilities.invokeLater(() -> refresh(source, tiles));
            }
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

    /**
     * Whether dragging with the left mouse button should pan the image.
     */
    private final boolean leftClickDrags;
    /**
     * Whether the centre of the view should be painted as a white crosshair.
     */
    private final boolean paintCentre;
    /**
     * The maximum number of background threads to use for rendering tiles.
     */
    private final int threads;
    /**
     * A monitor for coordinating multithreaded access to the tile caches.
     */
    private final Object TILE_CACHE_LOCK = new Object();
    /**
     * The currently configured tile providers.
     */
    private final List<TileProvider> tileProviders = new ArrayList<>();
    /**
     * The fresh and stale tile caches for each tile provider.
     */
    private final Map<TileProvider, Map<Point, Reference<? extends Image>>> tileCaches = new HashMap<>(),
            dirtyTileCaches = new HashMap<>();
    /**
     * Per-tile-provider offsets.
     */
    private final Map<TileProvider, Point> offsets = new HashMap<>();
    /**
     * The currently configured overlays.
     */
    private final Map<String, Overlay> overlays = new HashMap<>();
    /**
     * The currently displayed location in scaled coordinates.
     */
    protected int viewX, viewY;
    /**
     * The previously displayed location in scaled coordinates. Used during mouse
     * drag operations.
     */
    protected int previousX, previousY;
    /**
     * The image coordindates of the marker (if any) to paint as a red
     * crosshair.
     */
    protected int markerX, markerY;
    /**
     * The offset to apply to the image so that the view coordinates are
     * displayed in the centre of the view.
     */
    protected int xOffset, yOffset;
    /**
     * The zoom level in the form of an exponent of 2. I.e. the scale is 2^n,
     * meaning that 0 represents no zoom, -1 means half size (so zooming out),
     * 1 means double size (so zooming in), etc.
     *
     * <p>The default zoom is 1 (200%) for HiDPI displays and 0 (100%) for
     * regular displays.
     */
    private int zoom = (UI_SCALE == 1) ? 0 : 1;
    /**
     * The size in image coordinates of the grid to paint, if any.
     */
    private int gridSize = 128;
    /**
     * The executor service to use for executing tile render jobs in the
     * background.
     */
    private ExecutorService tileRenderers;
    /**
     * Whether a mouse drag operation is currently in progress.
     */
    private boolean dragging;
    /**
     * Whether the marker (a red crosshair) should be painted.
     */
    private boolean paintMarker;
    /**
     * Whether the grid should be painted.
     */
    private boolean paintGrid;
    /**
     * The queue for submitting background tile render jobs.
     */
    private BlockingQueue<Runnable> queue;
    /**
     * The currently configured external listener interested in changes to the
     * view.
     */
    private ViewListener viewListener;
    /**
     * The colour in which to paint the grid.
     */
    private Color gridColour = Color.BLACK;
    private BufferedImage backgroundImage;
    private BackgroundImageMode backgroundImageMode = BackgroundImageMode.CENTRE_REPEAT;
    private volatile boolean inhibitUpdates;

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
    private static final Font NORMAL_FONT = new Font("SansSerif", Font.PLAIN, 10 * UI_SCALE);
    private static final Font BOLD_FONT = new Font("SansSerif", Font.BOLD, 10 * UI_SCALE);
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
            if (image instanceof VolatileImage) {
                // This image was previously created by us, here, so really it
                // should still be compatible
                tile = (VolatileImage) image;
            } else {
                GraphicsConfiguration gc = getGraphicsConfiguration();
                tile = gc.createCompatibleVolatileImage(tileSize, tileSize, Transparency.TRANSLUCENT);
                tile.validate(gc);
            }
            if (tileProvider.paintTile(tile, coords.x, coords.y, 0, 0)) {
                synchronized (TILE_CACHE_LOCK) {
                    tileCache.put(coords, new SoftReference<>(tile));
                    if (dirtyTileCache.containsKey(coords)) {
                        dirtyTileCache.remove(coords);
                    }
                }
            } else {
                // The tile failed to be painted for some reason; treat it as
                // a permanent condition and register it as "no tile present"
                synchronized (TILE_CACHE_LOCK) {
                    tileCache.put(coords, new SoftReference<>(NO_TILE));
                    if (dirtyTileCache.containsKey(coords)) {
                        dirtyTileCache.remove(coords);
                    }
                }
                // Repaint still needed, as a dirty tile may have been painted
                // in its location
            }
            try {
                repaint(getTileBounds(tileProvider, coords.x, coords.y, effectiveZoom));
            } catch (UnknownTileProviderException e) {
                // This means the tile provider is no longer configured on the
                // viewer, meaning there's not much point in us painting the
                // tile, so just give up silently
            }
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

    /**
     * A listener for changes to a {@link TiledImageViewer} view.
     */
    public interface ViewListener {
        /**
         * Invoked when the view has changed in one of these ways:
         * <ul><li>The location has changed
         * <li>The zoom level has changed
         * <li>A {@link TileProvider} has been added, replaced or removed
         * <li>The offset of a tile provider has changed
         * </ul>
         * @param source The tiled image viewer which has changed.
         */
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

    public enum BackgroundImageMode {CENTRE, STRETCH, FIT, REPEAT, CENTRE_REPEAT, FIT_REPEAT}
}