/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

import org.jetbrains.annotations.NotNull;
import org.pepsoft.util.swing.TileListener;
import org.pepsoft.worldpainter.biomeschemes.CustomBiomeManager;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.renderers.VoidRenderer;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.pepsoft.worldpainter.Constants.DIM_NORMAL;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE;

/**
 *
 * @author pepijn
 */
public class WPTileProvider implements org.pepsoft.util.swing.TileProvider, Dimension.Listener, Tile.Listener {
    public WPTileProvider(Dimension dimension, ColourScheme colourScheme, BiomeScheme biomeScheme, CustomBiomeManager customBiomeManager, Collection<Layer> hiddenLayers, boolean contourLines, int contourSeparation, TileRenderer.LightOrigin lightOrigin, boolean showBorder, org.pepsoft.util.swing.TileProvider surroundingTileProvider, boolean active) {
        tileProvider = dimension;
        this.colourScheme = colourScheme;
        this.biomeScheme = biomeScheme;
        this.hiddenLayers = (hiddenLayers != null) ? new HashSet<Layer>(hiddenLayers) : null;
        this.contourLines = contourLines;
        this.contourSeparation = contourSeparation;
        this.lightOrigin = lightOrigin;
        this.active = active;
        this.customBiomeManager = customBiomeManager;
        this.surroundingTileProvider = surroundingTileProvider;
        this.showBorder = showBorder;
        tileRendererRef = createNewTileRendererRef();
    }

    public WPTileProvider(TileProvider tileProvider, ColourScheme colourScheme, BiomeScheme biomeScheme, CustomBiomeManager customBiomeManager, Collection<Layer> hiddenLayers, boolean contourLines, int contourSeparation, TileRenderer.LightOrigin lightOrigin, boolean showBorder, org.pepsoft.util.swing.TileProvider surroundingTileProvider) {
        this.tileProvider = tileProvider;
        this.colourScheme = colourScheme;
        this.biomeScheme = biomeScheme;
        this.hiddenLayers = (hiddenLayers != null) ? new HashSet<Layer>(hiddenLayers) : null;
        this.contourLines = contourLines;
        this.contourSeparation = contourSeparation;
        this.lightOrigin = lightOrigin;
        active = false;
        this.customBiomeManager = customBiomeManager;
        this.surroundingTileProvider = surroundingTileProvider;
        this.showBorder = showBorder;
        tileRendererRef = createNewTileRendererRef();
    }
    
    public void addHiddenLayer(Layer layer) {
        hiddenLayers.add(layer);
        tileRendererRef = createNewTileRendererRef();
    }
    
    public void removeHiddenLayer(Layer layer) {
        hiddenLayers.remove(layer);
        tileRendererRef = createNewTileRendererRef();
    }
    
    @Override
    public int getTileSize() {
        return TILE_SIZE;
    }

    @Override
    public boolean isTilePresent(int x, int y) {
        if (zoom == 0) {
            return getUnzoomedTileType(x, y) != TileType.SURROUNDS
                    || ((surroundingTileProvider != null) && surroundingTileProvider.isTilePresent(x, y));
        } else {
            final int scale = 1 << -zoom;
            for (int dx = 0; dx < scale; dx++) {
                for (int dy = 0; dy < scale; dy++) {
                    switch (getUnzoomedTileType(x * scale + dx, y * scale + dy)) {
                        case WORLD:
                        case BORDER:
                            return true;
                        case SURROUNDS:
                            if ((surroundingTileProvider != null) && surroundingTileProvider.isTilePresent(x, y)) {
                                return true;
                            }
                            break;
                    }
                }
            }
            return false;
        }
    }

    @Override
    public void paintTile(final Image tileImage, final int x, final int y) {
        try {
            if (zoom == 0) {
                paintUnzoomedTile(tileImage, x, y);
            } else {
                Graphics2D g2 = (Graphics2D) tileImage.getGraphics();
                try {
                    BufferedImage surroundingTileImage = null;
                    final Color waterColour = new Color(colourScheme.getColour(org.pepsoft.minecraft.Constants.BLK_WATER));
                    final Color lavaColour = new Color(colourScheme.getColour(org.pepsoft.minecraft.Constants.BLK_LAVA));
                    final Color voidColour = new Color(VoidRenderer.getColour());
                    final int scale = 1 << -zoom;
                    final int subSize = TILE_SIZE / scale;
                    for (int dx = 0; dx < scale; dx++) {
                        for (int dy = 0; dy < scale; dy++) {
                            switch (getUnzoomedTileType(x * scale + dx, y * scale + dy)) {
                                case WORLD:
                                    TileRenderer tileRenderer = tileRendererRef.get();
                                    tileRenderer.setTile(tileProvider.getTile(x * scale + dx, y * scale + dy));
                                    tileRenderer.renderTile(tileImage, dx * subSize, dy * subSize);
                                    break;
                                case BORDER:
                                    Color colour;
                                    switch (((Dimension) tileProvider).getBorder()) {
                                        case WATER:
                                            colour = waterColour;
                                            break;
                                        case LAVA:
                                            colour = lavaColour;
                                            break;
                                        case VOID:
                                            colour = voidColour;
                                            break;
                                        default:
                                            throw new InternalError();
                                    }
                                    g2.setColor(colour);
                                    g2.fillRect(dx * subSize, dy * subSize, subSize, subSize);

                                    // Draw border lines
                                    g2.setColor(Color.BLACK);
                                    g2.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0.0f, new float[] {4.0f, 4.0f}, 0.0f));
                                    if (tileProvider.isTilePresent(x * scale + dx, y * scale + dy - 1)) {
                                        g2.drawLine(dx       * subSize    , dy       * subSize    , (dx + 1) * subSize - 1, dy       * subSize);
                                    }
                                    if (tileProvider.isTilePresent(x * scale + dx + 1, y * scale + dy)) {
                                        g2.drawLine((dx + 1) * subSize - 1, dy       * subSize    , (dx + 1) * subSize - 1, (dy + 1) * subSize - 1);
                                    }
                                    if (tileProvider.isTilePresent(x * scale + dx, y * scale + dy + 1)) {
                                        g2.drawLine(dx       * subSize    , (dy + 1) * subSize - 1, (dx + 1) * subSize - 1, (dy + 1) * subSize - 1);
                                    }
                                    if (tileProvider.isTilePresent(x * scale + dx - 1, y * scale + dy)) {
                                        g2.drawLine(dx       * subSize    , dy       * subSize    , dx       * subSize    , (dy + 1) * subSize - 1);
                                    }
                                    break;
                                case SURROUNDS:
                                    if (surroundingTileProvider != null) {
                                        if (surroundingTileImage == null) {
                                            surroundingTileImage = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
                                            surroundingTileProvider.paintTile(surroundingTileImage, x, y);
                                        }
                                        g2.drawImage(surroundingTileImage,
                                                dx * subSize, dy * subSize, (dx + 1) * subSize, (dy + 1) * subSize,
                                                dx * subSize, dy * subSize, (dx + 1) * subSize, (dy + 1) * subSize,
                                                null);
                                    } else {
                                        g2.setColor(voidColour);
                                        g2.fillRect(dx * subSize, dy * subSize, subSize, subSize);
                                    }
                                    break;
                            }
                        }
                    }
                } finally {
                    g2.dispose();
                }
            }
        } catch (Throwable e) {
            // Log at debug level because this tends to happen when zooming in
            // and out, probably due to some state getting out of sync. It
            // doesn't so far appear to have any visible consequences.
            logger.log(Level.SEVERE, "Exception while generating image for tile at " + x + ", " + y, e);
        }
    }

    @Override
    public int getTilePriority(int x, int y) {
        if (zoom == 0) {
            return (getUnzoomedTileType(x, y) == TileType.WORLD) ? 1 : 0;
        } else {
            final int scale = 1 << -zoom;
            for (int dx = 0; dx < scale; dx++) {
                for (int dy = 0; dy < scale; dy++) {
                    if (getUnzoomedTileType(x * scale + dx, y * scale + dy) == TileType.WORLD) {
                        return 1;
                    }
                }
            }
            return 0;
        }
    }

    @Override
    public Rectangle getExtent() {
        Rectangle sourceExtent = tileProvider.getExtent();
        if (sourceExtent != null) {
            if (zoom == 0) {
                return sourceExtent;
            } else if (zoom < 0) {
                return new Rectangle(sourceExtent.x >> -zoom, sourceExtent.y >> -zoom, sourceExtent.width >> -zoom, sourceExtent.height >> -zoom);
            } else {
                return new Rectangle(sourceExtent.x << zoom, sourceExtent.y << zoom, sourceExtent.width << zoom, sourceExtent.height << zoom);
            }
        } else {
            return null;
        }
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
            if (surroundingTileProvider != null) {
                surroundingTileProvider.setZoom(zoom);
            }
        }
    }
    
    // Dimension.Listener

    @Override
    public void tileAdded(Dimension dimension, Tile tile) {
        tile.addListener(this);
        fireTileChanged(tile, true);
    }

    @Override
    public void tileRemoved(Dimension dimension, Tile tile) {
        tile.removeListener(this);
        fireTileChanged(tile, true);
    }

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

    private TileType getUnzoomedTileType(int x, int y) {
        if (tileProvider.isTilePresent(x, y)) {
            return TileType.WORLD;
        } else if (showBorder && (tileProvider instanceof Dimension) && ((Dimension) tileProvider).isBorderTile(x, y)) {
            return TileType.BORDER;
        } else {
            return TileType.SURROUNDS;
        }
    }
    
    private void paintUnzoomedTile(final Image tileImage, final int x, final int y) {
        switch (getUnzoomedTileType(x, y)) {
            case WORLD:
                Tile tile = tileProvider.getTile(x, y);
                TileRenderer tileRenderer = tileRendererRef.get();
                tileRenderer.setTile(tile);
                tileRenderer.renderTile(tileImage, 0, 0);
                break;
            case BORDER:
                int colour;
                switch (((Dimension) tileProvider).getBorder()) {
                    case WATER:
                        colour = colourScheme.getColour(org.pepsoft.minecraft.Constants.BLK_WATER);
                        break;
                    case LAVA:
                        colour = colourScheme.getColour(org.pepsoft.minecraft.Constants.BLK_LAVA);
                        break;
                    case VOID:
                        colour = VoidRenderer.getColour();
                        break;
                    default:
                        throw new InternalError();
                }
                Graphics2D g2 = (Graphics2D) tileImage.getGraphics();
                try {
                    g2.setColor(new Color(colour));
                    g2.fillRect(0, 0, TILE_SIZE, TILE_SIZE);
                    
                    // Draw border lines
                    g2.setColor(Color.BLACK);
                    g2.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0.0f, new float[] {4.0f, 4.0f}, 0.0f));
                    if (tileProvider.isTilePresent(x, y - 1)) {
                        g2.drawLine(1, 1, TILE_SIZE - 1, 1);
                    }
                    if (tileProvider.isTilePresent(x + 1, y)) {
                        g2.drawLine(TILE_SIZE - 1, 1, TILE_SIZE - 1, TILE_SIZE - 1);
                    }
                    if (tileProvider.isTilePresent(x, y + 1)) {
                        g2.drawLine(1, TILE_SIZE - 1, TILE_SIZE - 1, TILE_SIZE - 1);
                    }
                    if (tileProvider.isTilePresent(x - 1, y)) {
                        g2.drawLine(1, 1, 1, TILE_SIZE - 1);
                    }
                } finally {
                    g2.dispose();
                }
                break;
            case SURROUNDS:
                if (surroundingTileProvider != null) {
                    surroundingTileProvider.paintTile(tileImage, x, y);
                }
                break;
            default:
                throw new InternalError();
        }
    }
    
    private void fireTileChanged(Tile tile) {
        fireTileChanged(tile, false);
    }
    
    private void fireTileChanged(Tile tile, boolean includeBorder) {
        if (includeBorder && showBorder && (tileProvider instanceof Dimension) && (((Dimension) tileProvider).getDim() == DIM_NORMAL) && (((Dimension) tileProvider).getBorder() != null)) {
            final Set<Point> coordSet = new HashSet<Point>();
            final int tileX = tile.getX(), tileY = tile.getY(), borderSize = ((Dimension) tileProvider).getBorderSize();
            for (int dx = -borderSize; dx <= borderSize; dx++) {
                for (int dy = -borderSize; dy <= borderSize; dy++) {
                    coordSet.add(getTileCoordinates(tileX + dx, tileY + dy));
                }
            }
            for (Point coords: coordSet) {
                for (TileListener listener: listeners) {
                    listener.tileChanged(this, coords.x, coords.y);
                }
            }
        } else {
            Point coords = getTileCoordinates(tile);
            for (TileListener listener: listeners) {
                listener.tileChanged(this, coords.x, coords.y);
            }
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
        return new ThreadLocal<TileRenderer>() {
            @Override
            protected TileRenderer initialValue() {
                TileRenderer tileRenderer = new TileRenderer(tileProvider, colourScheme, biomeScheme, customBiomeManager, zoom);
                if (hiddenLayers != null) {
                    tileRenderer.addHiddenLayers(hiddenLayers);
                }
                tileRenderer.setContourLines(contourLines);
                tileRenderer.setContourSeparation(contourSeparation);
                tileRenderer.setLightOrigin(lightOrigin);
                return tileRenderer;
            }
        };
    }

    private final TileProvider tileProvider;
    private final ColourScheme colourScheme;
    private final BiomeScheme biomeScheme;
    private final Set<Layer> hiddenLayers;
    private final boolean contourLines, active, showBorder;
    private final int contourSeparation;
    private final TileRenderer.LightOrigin lightOrigin;
    private final List<TileListener> listeners = new ArrayList<TileListener>();
    private final CustomBiomeManager customBiomeManager;
    private final org.pepsoft.util.swing.TileProvider surroundingTileProvider;
    private int zoom = 0;
    private volatile ThreadLocal<TileRenderer> tileRendererRef;

    private static final Logger logger = Logger.getLogger(WPTileProvider.class.getName());
    
    private enum TileType {WORLD, BORDER, SURROUNDS}
}