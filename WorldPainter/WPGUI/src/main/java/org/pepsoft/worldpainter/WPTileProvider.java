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
import org.pepsoft.worldpainter.layers.NotPresent;
import org.pepsoft.worldpainter.layers.renderers.VoidRenderer;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.worldpainter.Constants.DIM_NORMAL;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE;

/**
 * WorldPainter tile provider for
 * {@link TiledImageViewer}s. Provides tiles based on a WorldPainter
 * {@link Dimension} and optionally another tile provider which is used where
 * the dimension has no tiles, or where chunks are marked {@link NotPresent}.
 *
 * @author pepijn
 */
public class WPTileProvider implements org.pepsoft.util.swing.TileProvider, Dimension.Listener, Tile.Listener {
    public WPTileProvider(Dimension dimension, ColourScheme colourScheme, BiomeScheme biomeScheme, CustomBiomeManager customBiomeManager, Collection<Layer> hiddenLayers, boolean contourLines, int contourSeparation, TileRenderer.LightOrigin lightOrigin, boolean showBorder, org.pepsoft.util.swing.TileProvider surroundingTileProvider, boolean active) {
        tileProvider = dimension;
        this.colourScheme = colourScheme;
        this.biomeScheme = biomeScheme;
        this.hiddenLayers = (hiddenLayers != null) ? new HashSet<>(hiddenLayers) : null;
        this.contourLines = contourLines;
        this.contourSeparation = contourSeparation;
        this.lightOrigin = lightOrigin;
        this.active = active;
        this.customBiomeManager = customBiomeManager;
        this.surroundingTileProvider = surroundingTileProvider;
        if (surroundingTileProvider != null) {
            surroundingTileImageRef = createNewSurroundingTileImageRef();
        } else {
            surroundingTileImageRef = null;
        }
        this.showBorder = showBorder;
        tileRendererRef = createNewTileRendererRef();
    }

    public WPTileProvider(TileProvider tileProvider, ColourScheme colourScheme, BiomeScheme biomeScheme, CustomBiomeManager customBiomeManager, Collection<Layer> hiddenLayers, boolean contourLines, int contourSeparation, TileRenderer.LightOrigin lightOrigin, boolean showBorder, org.pepsoft.util.swing.TileProvider surroundingTileProvider) {
        this.tileProvider = tileProvider;
        this.colourScheme = colourScheme;
        this.biomeScheme = biomeScheme;
        this.hiddenLayers = (hiddenLayers != null) ? new HashSet<>(hiddenLayers) : null;
        this.contourLines = contourLines;
        this.contourSeparation = contourSeparation;
        this.lightOrigin = lightOrigin;
        active = false;
        this.customBiomeManager = customBiomeManager;
        this.surroundingTileProvider = surroundingTileProvider;
        if (surroundingTileProvider != null) {
            surroundingTileImageRef = createNewSurroundingTileImageRef();
        } else {
            surroundingTileImageRef = null;
        }
        this.showBorder = showBorder;
        tileRendererRef = createNewTileRendererRef();
    }
    
    public synchronized void addHiddenLayer(Layer layer) {
        hiddenLayers.add(layer);
        tileRendererRef = createNewTileRendererRef();
    }
    
    public synchronized void removeHiddenLayer(Layer layer) {
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
                        case WALL:
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
    public boolean paintTile(final Image tileImage, final int x, final int y, final int imageX, final int imageY) {
        try {
            if (zoom == 0) {
                return paintUnzoomedTile(tileImage, x, y, imageX, imageY);
            } else {
                Graphics2D g2 = (Graphics2D) tileImage.getGraphics();
                g2.setComposite(AlphaComposite.Src);
                try {
                    Boolean surroundingTileImageAvailable = null;
                    BufferedImage surroundingTileImage = null;
                    final Color waterColour = new Color(colourScheme.getColour(BLK_WATER));
                    final Color lavaColour = new Color(colourScheme.getColour(BLK_LAVA));
                    final Color voidColour = new Color(0x00ffffff & VoidRenderer.getColour(), true);
                    final Color bedrockColour = new Color(colourScheme.getColour(BLK_BEDROCK));
                    final int scale = 1 << -zoom;
                    final int subSize = TILE_SIZE / scale;
                    for (int dx = 0; dx < scale; dx++) {
                        for (int dy = 0; dy < scale; dy++) {
                            TileType tileType = getUnzoomedTileType(x * scale + dx, y * scale + dy);
                            switch (tileType) {
                                case WORLD:
                                    Tile tile = tileProvider.getTile(x * scale + dx, y * scale + dy);
                                    TileRenderer tileRenderer = tileRendererRef.get();
                                    tileRenderer.renderTile(tile, tileImage, dx * subSize, dy * subSize);

                                    // If the tile has chunks that are marked "not present" and
                                    // there is a surrounding tile provider, copy those chunks from
                                    // there
                                    if (tile.hasLayer(NotPresent.INSTANCE) && (surroundingTileProvider != null)) {
                                        if (surroundingTileImageAvailable == null) {
                                            surroundingTileImage = surroundingTileImageRef.get();
                                            surroundingTileImageAvailable = surroundingTileProvider.paintTile(surroundingTileImage, x, y, 0, 0);
                                        }
                                        if (surroundingTileImageAvailable) {
                                            for (int chunkX = 0; chunkX < 8; chunkX++) {
                                                final int xInTile = chunkX << 4;
                                                // Copy entire vertical columns of chunks at
                                                // once, if possible, as a performance
                                                // optimisation
                                                int runStart = -1;
                                                boolean previousChunkNotPresent = false;
                                                for (int chunkY = 0; chunkY < 8; chunkY++) {
                                                    final int yInTile = chunkY << 4;
                                                    if (tile.getBitLayerValue(NotPresent.INSTANCE, xInTile, yInTile) != previousChunkNotPresent) {
                                                        if (! previousChunkNotPresent) {
                                                            runStart = chunkY;
                                                            previousChunkNotPresent = true;
                                                        } else {
                                                            // Copy the previous run of not present chunks
                                                            g2.drawImage(surroundingTileImage,
                                                                    imageX + dx * subSize + (chunkX << 4) / scale, imageY + dy * subSize + (runStart << 4) / scale, imageX + dx * subSize + ((chunkX + 1) << 4) / scale, imageY + dy * subSize + (chunkY << 4) / scale,
                                                                    dx * subSize + (chunkX << 4) / scale, dy * subSize + (runStart << 4) / scale, dx * subSize + ((chunkX + 1) << 4) / scale, dy * subSize + (chunkY << 4) / scale,
                                                                    null);
                                                            previousChunkNotPresent = false;
                                                        }
                                                    }
                                                }
                                                if (previousChunkNotPresent) {
                                                    // Copy the last run of not present chunks
                                                    g2.drawImage(surroundingTileImage,
                                                            imageX + dx * subSize + (chunkX << 4) / scale, imageY + dy * subSize + (runStart << 4) / scale, imageX + dx * subSize + ((chunkX + 1) << 4) / scale, imageY + dy * subSize + (8 << 4) / scale,
                                                            dx * subSize + (chunkX << 4) / scale, dy * subSize + (runStart << 4) / scale, dx * subSize + ((chunkX + 1) << 4) / scale, dy * subSize + (8 << 4) / scale,
                                                            null);
                                                }
                                            }
                                        }
                                    }
                                    break;
                                case BORDER:
                                    Color colour;
                                    switch (((Dimension) tileProvider).getBorder()) {
                                        case WATER:
                                        case ENDLESS_WATER:
                                            colour = waterColour;
                                            break;
                                        case LAVA:
                                        case ENDLESS_LAVA:
                                            colour = lavaColour;
                                            break;
                                        case VOID:
                                        case ENDLESS_VOID:
                                            colour = voidColour;
                                            break;
                                        default:
                                            throw new InternalError();
                                    }
                                    g2.setColor(colour);
                                    g2.fillRect(imageX + dx * subSize, imageY + dy * subSize, subSize, subSize);

                                    // Draw border lines
                                    g2.setColor(Color.BLACK);
                                    g2.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0.0f, new float[] {4.0f, 4.0f}, 0.0f));
                                    if (tileProvider.isTilePresent(x * scale + dx, y * scale + dy - 1)) {
                                        g2.drawLine(imageX + dx       * subSize    , imageY + dy       * subSize    , imageX + (dx + 1) * subSize - 1, imageY + dy       * subSize);
                                    }
                                    if (tileProvider.isTilePresent(x * scale + dx + 1, y * scale + dy)) {
                                        g2.drawLine(imageX + (dx + 1) * subSize - 1, imageY + dy       * subSize    , imageX + (dx + 1) * subSize - 1, imageY + (dy + 1) * subSize - 1);
                                    }
                                    if (tileProvider.isTilePresent(x * scale + dx, y * scale + dy + 1)) {
                                        g2.drawLine(imageX + dx       * subSize    , imageY + (dy + 1) * subSize - 1, imageX + (dx + 1) * subSize - 1, imageY + (dy + 1) * subSize - 1);
                                    }
                                    if (tileProvider.isTilePresent(x * scale + dx - 1, y * scale + dy)) {
                                        g2.drawLine(imageX + dx       * subSize    , imageY + dy       * subSize    , imageX + dx       * subSize    , imageY + (dy + 1) * subSize - 1);
                                    }
                                    break;
                                case SURROUNDS:
                                case WALL:
                                    if (surroundingTileProvider != null) {
                                        if (surroundingTileImageAvailable == null) {
                                            surroundingTileImage = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
                                            surroundingTileImageAvailable = surroundingTileProvider.paintTile(surroundingTileImage, x, y, 0, 0);
                                        }
                                        if (surroundingTileImageAvailable) {
                                            g2.drawImage(surroundingTileImage,
                                                    imageX + dx * subSize, imageY + dy * subSize, imageX + (dx + 1) * subSize, imageY + (dy + 1) * subSize,
                                                    imageX + dx * subSize, imageY + dy * subSize, imageX + (dx + 1) * subSize, imageY + (dy + 1) * subSize,
                                                    null);
                                        }
                                    }
                                    if (! Boolean.TRUE.equals(surroundingTileImageAvailable)) {
                                        g2.setColor(voidColour);
                                        g2.fillRect(imageX + dx * subSize, imageY + dy * subSize, subSize, subSize);
                                    }
                                    if (tileType == TileType.WALL) {
                                        g2.setColor(bedrockColour);
                                        TileType neighbourType = getUnzoomedTileType(x * scale + dx, y * scale + dy - 1);
                                        int wallWidth = Math.max(subSize / 8, 1);
                                        if ((neighbourType == TileType.WORLD) || (neighbourType == TileType.BORDER)) {
                                            g2.fillRect(imageX + dx * subSize, imageY + dy * subSize, subSize, wallWidth);
                                        }
                                        neighbourType = getUnzoomedTileType(x * scale + dx + 1, y * scale + dy);
                                        if ((neighbourType == TileType.WORLD) || (neighbourType == TileType.BORDER)) {
                                            g2.fillRect(imageX + (dx + 1) * subSize - wallWidth, imageY + dy * subSize, wallWidth, subSize);
                                        }
                                        neighbourType = getUnzoomedTileType(x * scale + dx, y * scale + dy + 1);
                                        if ((neighbourType == TileType.WORLD) || (neighbourType == TileType.BORDER)) {
                                            g2.fillRect(imageX + dx * subSize, imageY + (dy + 1) * subSize - wallWidth, subSize, wallWidth);
                                        }
                                        neighbourType = getUnzoomedTileType(x * scale + dx - 1, y * scale + dy);
                                        if ((neighbourType == TileType.WORLD) || (neighbourType == TileType.BORDER)) {
                                            g2.fillRect(imageX + dx * subSize, imageY + dy * subSize, wallWidth, subSize);
                                        }
                                    }
                                    break;
                            }
                        }
                    }
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
            if (surroundingTileProvider != null) {
                surroundingTileProvider.setZoom(zoom);
            }
        }
    }
    
    // Dimension.Listener

    @Override
    public void tilesAdded(Dimension dimension, Set<Tile> tiles) {
        for (Tile tile: tiles) {
            tile.addListener(this);
        }
        fireTilesChangedIncludeBorder(tiles);
    }

    @Override
    public void tilesRemoved(Dimension dimension, Set<Tile> tiles) {
        for (Tile tile: tiles) {
            tile.removeListener(this);
        }
        fireTilesChangedIncludeBorder(tiles);
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
        } else if (showBorder && (tileProvider instanceof Dimension)) {
            Dimension dimension = (Dimension) tileProvider;
            if (dimension.isBorderTile(x, y)) {
                return TileType.BORDER;
            } else if (dimension.isBedrockWall()
                    && ((dimension.getBorder() != null)
                        ? (dimension.isBorderTile(x - 1, y)
                            || dimension.isBorderTile(x, y - 1)
                            || dimension.isBorderTile(x + 1, y)
                            || dimension.isBorderTile(x, y + 1))
                        : (tileProvider.isTilePresent(x - 1, y)
                            || tileProvider.isTilePresent(x, y - 1)
                            || tileProvider.isTilePresent(x + 1, y)
                            || tileProvider.isTilePresent(x, y + 1)))) {
                return TileType.WALL;
            }
        }
        return TileType.SURROUNDS;
    }
    
    private boolean paintUnzoomedTile(final Image tileImage, final int x, final int y, final int dx, final int dy) {
        TileType tileType = getUnzoomedTileType(x, y);
        switch (tileType) {
            case WORLD:
                Tile tile = tileProvider.getTile(x, y);
                TileRenderer tileRenderer = tileRendererRef.get();
                tileRenderer.renderTile(tile, tileImage, dx, dy);

                // If the tile has chunks that are marked "not present" and
                // there is a surrounding tile provider, copy those chunks from
                // there
                if (tile.hasLayer(NotPresent.INSTANCE) && (surroundingTileProvider != null)) {
                    Graphics2D g2 = (Graphics2D) tileImage.getGraphics();
                    try {
                        BufferedImage surroundingTileImage = surroundingTileImageRef.get();
                        if (surroundingTileProvider.paintTile(surroundingTileImage, x, y, 0, 0)) {
                            for (int chunkX = 0; chunkX < 8; chunkX++) {
                                final int xInTile = chunkX << 4;
                                // Copy entire vertical columns of chunks at
                                // once, if possible, as a performance
                                // optimisation
                                int runStart = -1;
                                boolean previousChunkNotPresent = false;
                                for (int chunkY = 0; chunkY < 8; chunkY++) {
                                    final int yInTile = chunkY << 4;
                                    if (tile.getBitLayerValue(NotPresent.INSTANCE, xInTile, yInTile) != previousChunkNotPresent) {
                                        if (! previousChunkNotPresent) {
                                            runStart = chunkY;
                                            previousChunkNotPresent = true;
                                        } else {
                                            // Copy the previous run of not present chunks
                                            final int runStartYInTile = runStart << 4;
                                            g2.drawImage(surroundingTileImage,
                                                    dx + xInTile, dy + runStartYInTile, dx + xInTile + 16, dy + yInTile,
                                                    xInTile, runStartYInTile, xInTile + 16, yInTile,
                                                    null);
                                            previousChunkNotPresent = false;
                                        }
                                    }
                                }
                                if (previousChunkNotPresent) {
                                    // Copy the last run of not present chunks
                                    final int runStartYInTile = runStart << 4;
                                    g2.drawImage(surroundingTileImage,
                                            dx + xInTile, dy + runStartYInTile, dx + xInTile + 16, dy + TILE_SIZE,
                                            xInTile, runStartYInTile, xInTile + 16, TILE_SIZE,
                                            null);
                                }
                            }
                        }
                    } finally {
                        g2.dispose();
                    }
                }
                return true;
            case BORDER:
                int colour;
                switch (((Dimension) tileProvider).getBorder()) {
                    case WATER:
                    case ENDLESS_WATER:
                        colour = 0xff000000 | colourScheme.getColour(BLK_WATER);
                        break;
                    case LAVA:
                    case ENDLESS_LAVA:
                        colour = 0xff000000 | colourScheme.getColour(BLK_LAVA);
                        break;
                    case VOID:
                    case ENDLESS_VOID:
                        colour = 0x00ffffff & VoidRenderer.getColour();
                        break;
                    default:
                        throw new InternalError();
                }
                Graphics2D g2 = (Graphics2D) tileImage.getGraphics();
                try {
                    g2.setColor(new Color(colour, true));
                    g2.setComposite(AlphaComposite.Src);
                    g2.fillRect(dx, dy, TILE_SIZE, TILE_SIZE);
                    
                    // Draw border lines
                    g2.setColor(Color.BLACK);
                    g2.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0.0f, new float[] {4.0f, 4.0f}, 0.0f));
                    if (tileProvider.isTilePresent(x, y - 1)) {
                        g2.drawLine(dx + 1, dy + 1, dx + TILE_SIZE - 1, dy + 1);
                    }
                    if (tileProvider.isTilePresent(x + 1, y)) {
                        g2.drawLine(dx + TILE_SIZE - 1, dy + 1, dx + TILE_SIZE - 1, dy + TILE_SIZE - 1);
                    }
                    if (tileProvider.isTilePresent(x, y + 1)) {
                        g2.drawLine(dx + 1, dy + TILE_SIZE - 1, dx + TILE_SIZE - 1, dy + TILE_SIZE - 1);
                    }
                    if (tileProvider.isTilePresent(x - 1, y)) {
                        g2.drawLine(dx + 1, dy + 1, dx + 1, dy + TILE_SIZE - 1);
                    }
                } finally {
                    g2.dispose();
                }
                return true;
            case WALL:
                boolean backgroundPainted = false;
                if (surroundingTileProvider != null) {
                    backgroundPainted = surroundingTileProvider.paintTile(tileImage, x, y, dx, dy);
                }
                g2 = (Graphics2D) tileImage.getGraphics();
                try {
                    if (! backgroundPainted) {
                        // A surrounding tile provider would have completely
                        // filled the image, but if there isn't one or it didn't
                        // work we have to make sure of that ourselves
                        g2.setColor(new Color(0x00ffffff & VoidRenderer.getColour(), true));
                        g2.setComposite(AlphaComposite.Src);
                        g2.fillRect(dx, dy, TILE_SIZE, TILE_SIZE);
                    }
                    g2.setColor(new Color(colourScheme.getColour(BLK_BEDROCK)));
                    TileType neighbourType = getUnzoomedTileType(x, y - 1);
                    if ((neighbourType == TileType.WORLD) || (neighbourType == TileType.BORDER)) {
                        g2.fillRect(dx, dy, TILE_SIZE, 16);
                    }
                    neighbourType = getUnzoomedTileType(x + 1, y);
                    if ((neighbourType == TileType.WORLD) || (neighbourType == TileType.BORDER)) {
                        g2.fillRect(dx + TILE_SIZE - 16, dy, 16, TILE_SIZE);
                    }
                    neighbourType = getUnzoomedTileType(x, y + 1);
                    if ((neighbourType == TileType.WORLD) || (neighbourType == TileType.BORDER)) {
                        g2.fillRect(dx, dy + TILE_SIZE - 16, TILE_SIZE, 16);
                    }
                    neighbourType = getUnzoomedTileType(x - 1, y);
                    if ((neighbourType == TileType.WORLD) || (neighbourType == TileType.BORDER)) {
                        g2.fillRect(dx, dy, 16, TILE_SIZE);
                    }
                } finally {
                    g2.dispose();
                }
                return true;
            case SURROUNDS:
                if (surroundingTileProvider != null) {
                    return surroundingTileProvider.paintTile(tileImage, x, y, dx, dy);
                }
                return false;
            default:
                throw new InternalError();
        }
    }
    
    private void fireTileChanged(Tile tile) {
        Point coords = getTileCoordinates(tile);
        for (TileListener listener: listeners) {
            listener.tileChanged(this, coords.x, coords.y);
        }
    }
    
    private void fireTilesChangedIncludeBorder(Set<Tile> tiles) {
        if (showBorder
                && (tileProvider instanceof Dimension)
                && (((Dimension) tileProvider).getDim() == DIM_NORMAL)
                && (((Dimension) tileProvider).getBorder() != null)
                && (! ((Dimension) tileProvider).getBorder().isEndless())) {
            final Set<Point> coordSet = new HashSet<>();
            for (Tile tile: tiles) {
                final int tileX = tile.getX(), tileY = tile.getY(), borderSize = ((Dimension) tileProvider).getBorderSize();
                for (int dx = -borderSize; dx <= borderSize; dx++) {
                    for (int dy = -borderSize; dy <= borderSize; dy++) {
                        coordSet.add(getTileCoordinates(tileX + dx, tileY + dy));
                    }
                }
            }
            for (TileListener listener: listeners) {
                listener.tilesChanged(this, coordSet);
            }
        } else {
            Set<Point> coords = tiles.stream().map(this::getTileCoordinates).collect(Collectors.toSet());
            for (TileListener listener: listeners) {
                listener.tilesChanged(this, coords);
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
        return ThreadLocal.withInitial(() -> {
            TileRenderer tileRenderer = new TileRenderer(tileProvider, colourScheme, biomeScheme, customBiomeManager, zoom);
            synchronized (WPTileProvider.this) {
                if (hiddenLayers != null) {
                    tileRenderer.addHiddenLayers(hiddenLayers);
                }
            }
            tileRenderer.setContourLines(contourLines);
            tileRenderer.setContourSeparation(contourSeparation);
            tileRenderer.setLightOrigin(lightOrigin);
            return tileRenderer;
        });
    }

    @NotNull
    private ThreadLocal<BufferedImage> createNewSurroundingTileImageRef() {
        return ThreadLocal.withInitial(() -> new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_RGB));
    }

    private final TileProvider tileProvider;
    private final ColourScheme colourScheme;
    private final BiomeScheme biomeScheme;
    private final Set<Layer> hiddenLayers;
    private final boolean contourLines, active, showBorder;
    private final int contourSeparation;
    private final TileRenderer.LightOrigin lightOrigin;
    private final List<TileListener> listeners = new ArrayList<>();
    private final CustomBiomeManager customBiomeManager;
    private final org.pepsoft.util.swing.TileProvider surroundingTileProvider;
    private final ThreadLocal<BufferedImage> surroundingTileImageRef;

    private int zoom = 0;
    private volatile ThreadLocal<TileRenderer> tileRendererRef;

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(WPTileProvider.class);
    
    private enum TileType {
        /**
         * The tile is part of the WorldPainter world.
         */
        WORLD,
        /**
         * The tile is part of the WorldPainter border, or an endless border.
         */
        BORDER,
        /**
         * The tile contains no WorldPainter-generated chunks.
         */
        SURROUNDS,
        /**
         * The tile is outside the WorldPainter world and border but does
         * contain part of a wall.
         */
        WALL
    }
}