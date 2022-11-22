/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

import org.jetbrains.annotations.NotNull;
import org.pepsoft.util.swing.TileListener;
import org.pepsoft.util.swing.TiledImageViewer;
import org.pepsoft.worldpainter.biomeschemes.CustomBiomeManager;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.renderers.VoidRenderer;
import org.pepsoft.worldpainter.ramps.ColourRamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.unmodifiableSet;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE;

/**
 * WorldPainter tile provider for {@link TiledImageViewer}s. Provides tiles based on a {@link TileProvider}, e.g. a
 * WorldPainter {@link Dimension}.
 *
 * @author pepijn
 */
public class WPTileProvider implements org.pepsoft.util.swing.TileProvider, Dimension.Listener, Tile.Listener {
    public WPTileProvider(TileProvider tileProvider, ColourScheme colourScheme, CustomBiomeManager customBiomeManager, Set<Layer> hiddenLayers, boolean contourLines, int contourSeparation, TileRenderer.LightOrigin lightOrigin, boolean active, Effect effect, boolean transparentVoid, ColourRamp colourRamp) {
        this.tileProvider = tileProvider;
        this.colourScheme = colourScheme;
        this.hiddenLayers = ((hiddenLayers != null) && (hiddenLayers != HIDE_ALL_LAYERS)) ? new HashSet<>(hiddenLayers) : null;
        this.hideAllLayers = hiddenLayers == HIDE_ALL_LAYERS;
        this.contourLines = contourLines;
        this.contourSeparation = contourSeparation;
        this.lightOrigin = lightOrigin;
        this.active = active;
        this.customBiomeManager = customBiomeManager;
        tileRendererRef = createNewTileRendererRef();
        this.effect = effect;
        this.transparentVoid = transparentVoid;
        this.colourRamp = colourRamp;
    }

    public WPTileProvider(TileProvider tileProvider, ColourScheme colourScheme, CustomBiomeManager customBiomeManager, Set<Layer> hiddenLayers, boolean contourLines, int contourSeparation, TileRenderer.LightOrigin lightOrigin, ColourRamp colourRamp) {
        this(tileProvider, colourScheme, customBiomeManager, hiddenLayers, contourLines, contourSeparation, lightOrigin, false, null, true, colourRamp);
    }
    
    public synchronized void setHiddenLayers(Set<Layer> hiddenLayers) {
        if (hideAllLayers) {
            throw new IllegalStateException("Cannot set hiddenLayers when hideAllLayers is set");
        }
        this.hiddenLayers.clear();
        if (hiddenLayers != null) {
            this.hiddenLayers.addAll(hiddenLayers);
        }
        tileRendererRef = createNewTileRendererRef();
    }

    public synchronized Set<Layer> getHiddenLayers() {
        return unmodifiableSet(hiddenLayers);
    }

    public boolean isHideAllLayers() {
        return hideAllLayers;
    }

    public void setHideAllLayers(boolean hideAllLayers) {
        if (hideAllLayers != this.hideAllLayers) {
            this.hideAllLayers = hideAllLayers;
            tileRendererRef = createNewTileRendererRef();
        }
    }

    @Override
    public int getTileSize() {
        return TILE_SIZE;
    }

    @Override
    public boolean isTilePresent(int x, int y) {
        if (zoom == 0) {
            return isUnzoomedTilePresent(x, y);
        } else {
            final int scale = 1 << -zoom;
            for (int dx = 0; dx < scale; dx++) {
                for (int dy = 0; dy < scale; dy++) {
                    if (isUnzoomedTilePresent(x * scale + dx, y * scale + dy)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    @Override
    public boolean paintTile(final Image tileImage, final int x, final int y, final int imageX, final int imageY) {
        try {
            if (zoom == 0) {
                return paintUnzoomedTile(tileImage, x, y, imageX, imageY);
            } else {
                final Graphics2D g2 = (Graphics2D) tileImage.getGraphics();
                try {
                    g2.setComposite(AlphaComposite.Src);
                    g2.setBackground(new Color(0x00ffffff & VoidRenderer.getColour(), true));
                    final int scale = 1 << -zoom;
                    final int subSize = TILE_SIZE / scale;
                    for (int dx = 0; dx < scale; dx++) {
                        for (int dy = 0; dy < scale; dy++) {
                            if (isUnzoomedTilePresent(x * scale + dx, y * scale + dy)) {
                                final Tile tile = tileProvider.getTile(x * scale + dx, y * scale + dy);
                                final TileRenderer tileRenderer = tileRendererRef.get();
                                tileRenderer.renderTile(tile, tileImage, dx * subSize, dy * subSize);
                            } else {
                                g2.clearRect(imageX + dx * subSize, imageY + dy * subSize, subSize, subSize);
                            }
                        }
                    }
                    applyEffects(g2);
                } finally {
                    g2.dispose();
                }
                return true;
            }
        } catch (Throwable e) {
            logger.error("Exception while generating image for tile at {}, {}", x, y, e);
            return false;
        }
    }

    @Override
    public int getTilePriority(int x, int y) {
        if (zoom == 0) {
            return isUnzoomedTilePresent(x, y) ? 1 : 0;
        } else {
            final int scale = 1 << -zoom;
            for (int dx = 0; dx < scale; dx++) {
                for (int dy = 0; dy < scale; dy++) {
                    if (isUnzoomedTilePresent(x * scale + dx, y * scale + dy)) {
                        return 1;
                    }
                }
            }
            return 0;
        }
    }

    @Override
    public Rectangle getExtent() {
        return tileProvider.getExtent();
    }
    
    @Override
    public void addTileListener(TileListener tileListener) {
        if (active && listeners.isEmpty()) {
            ((Dimension) tileProvider).addDimensionListener(this);
            for (Tile tile: ((Dimension) tileProvider).getTiles()) {
                tile.addListener(this);
            }
        }
        if (! listeners.contains(tileListener)) {
            listeners.add(tileListener);
        }
    }

    @Override
    public void removeTileListener(TileListener tileListener) {
        listeners.remove(tileListener);
        if (active && listeners.isEmpty()) {
            for (Tile tile: ((Dimension) tileProvider).getTiles()) {
                tile.removeListener(this);
            }
            ((Dimension) tileProvider).removeDimensionListener(this);
        }
    }

    @Override
    public boolean isZoomSupported() {
        return true;
    }

    @Override
    public int getZoom() {
        return zoom;
    }

    @Override
    public void setZoom(int zoom) {
        if (zoom != this.zoom) {
            if (zoom > 0) {
                throw new UnsupportedOperationException("Zooming in not supported");
            }
            this.zoom = zoom;
            tileRendererRef = createNewTileRendererRef();
        }
    }
    
    // Dimension.Listener

    @Override
    public void tilesAdded(Dimension dimension, Set<Tile> tiles) {
        for (Tile tile: tiles) {
            tile.addListener(this);
        }
        fireTilesChanged(tiles);
    }

    @Override
    public void tilesRemoved(Dimension dimension, Set<Tile> tiles) {
        for (Tile tile: tiles) {
            tile.removeListener(this);
        }
        fireTilesChanged(tiles);
    }

    @Override public void overlayAdded(Dimension dimension, int index, Overlay overlay) {}
    @Override public void overlayRemoved(Dimension dimension, int index, Overlay overlay) {}

    // Tile.Listener
    
    @Override
    public void heightMapChanged(Tile tile) {
        fireTileChanged(tile);
    }

    @Override
    public void terrainChanged(Tile tile) {
        fireTileChanged(tile);
    }

    @Override
    public void waterLevelChanged(Tile tile) {
        fireTileChanged(tile);
    }

    @Override
    public void layerDataChanged(Tile tile, Set<Layer> changedLayers) {
        fireTileChanged(tile);
    }

    @Override
    public void allBitLayerDataChanged(Tile tile) {
        fireTileChanged(tile);
    }

    @Override
    public void allNonBitlayerDataChanged(Tile tile) {
        fireTileChanged(tile);
    }

    @Override
    public void seedsChanged(Tile tile) {
        fireTileChanged(tile);
    }

    private boolean isUnzoomedTilePresent(int x, int y) {
        return tileProvider.isTilePresent(x, y);
    }
    
    private boolean paintUnzoomedTile(final Image tileImage, final int x, final int y, final int dx, final int dy) {
        if (isUnzoomedTilePresent(x, y)) {
            tileRendererRef.get().renderTile(tileProvider.getTile(x, y), tileImage, dx, dy);
            if (effect != null) {
                final Graphics2D g2 = (Graphics2D) tileImage.getGraphics();
                try {
                    applyEffects(g2);
                } finally {
                    g2.dispose();
                }
            }
            return true;
        } else {
            return false;
        }
    }
    
    private void fireTileChanged(Tile tile) {
        Point coords = getTileCoordinates(tile);
        for (TileListener listener: listeners) {
            listener.tileChanged(this, coords.x, coords.y);
        }
    }
    
    private void fireTilesChanged(Set<Tile> tiles) {
        Set<Point> coords = tiles.stream().map(this::getTileCoordinates).collect(Collectors.toSet());
        for (TileListener listener: listeners) {
            listener.tilesChanged(this, coords);
        }
    }
    
    /**
     * Convert the actual tile coordinates to zoom-corrected (tile provider
     * coordinate system) coordinates.
     * 
     * @param tile The tile of which to convert the coordinates.
     * @return The coordinates of the tile in the tile provider coordinate
     *     system (corrected for zoom).
     */
    private Point getTileCoordinates(Tile tile) {
        return getTileCoordinates(tile.getX(), tile.getY());
    }

    /**
     * Convert the actual tile coordinates to zoom-corrected (tile provider
     * coordinate system) coordinates.
     * 
     * @param tileX The X tile coordinate to convert.
     * @param tileY The Y tile coordinate to convert.
     * @return The coordinates of the tile in the tile provider coordinate
     *     system (corrected for zoom).
     */
    private Point getTileCoordinates(final int tileX, final int tileY) {
        if (zoom == 0) {
            return new Point(tileX, tileY);
        } else if (zoom < 0) {
            return new Point(tileX >> -zoom, tileY >> -zoom);
        } else {
            return new Point(tileX << zoom, tileY << zoom);
        }
    }

    @NotNull
    private ThreadLocal<TileRenderer> createNewTileRendererRef() {
        return ThreadLocal.withInitial(() -> {
            TileRenderer tileRenderer = new TileRenderer(tileProvider, colourScheme, customBiomeManager, zoom, transparentVoid, colourRamp);
            synchronized (WPTileProvider.this) {
                if (hideAllLayers) {
                    tileRenderer.setHideAllLayers(true);
                } else if (hiddenLayers != null) {
                    tileRenderer.addHiddenLayers(hiddenLayers);
                }
            }
            tileRenderer.setContourLines(contourLines);
            tileRenderer.setContourSeparation(contourSeparation);
            tileRenderer.setLightOrigin(lightOrigin);
            return tileRenderer;
        });
    }

    private void applyEffects(Graphics2D g2) {
        if (effect == null) {
            return;
        }
        switch (effect) {
            case FADE_TO_FIFTY_PERCENT:
                g2.setComposite(AlphaComposite.SrcOver.derive(0.5f));
                g2.setColor(Color.WHITE);
                g2.fillRect(0, 0, TILE_SIZE, TILE_SIZE);
                break;
            case FADE_TO_TWENTYFIVE_PERCENT:
                g2.setComposite(AlphaComposite.SrcOver.derive(0.75f));
                g2.setColor(Color.WHITE);
                g2.fillRect(0, 0, TILE_SIZE, TILE_SIZE);
                break;
        }
    }

    private final TileProvider tileProvider;
    private final ColourScheme colourScheme;
    private final Set<Layer> hiddenLayers;
    private final boolean contourLines, active, transparentVoid;
    private final int contourSeparation;
    private final TileRenderer.LightOrigin lightOrigin;
    private final List<TileListener> listeners = new ArrayList<>();
    private final CustomBiomeManager customBiomeManager;
    private final Effect effect;
    private final ColourRamp colourRamp;

    private int zoom = 0;
    private boolean hideAllLayers;
    private volatile ThreadLocal<TileRenderer> tileRendererRef;

    private static final Logger logger = LoggerFactory.getLogger(WPTileProvider.class);

    public static final Set<Layer> HIDE_ALL_LAYERS = Collections.emptySet();

    public enum Effect {
        FADE_TO_FIFTY_PERCENT, FADE_TO_TWENTYFIVE_PERCENT
    }
}