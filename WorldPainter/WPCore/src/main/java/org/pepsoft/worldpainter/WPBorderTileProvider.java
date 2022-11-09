/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

import org.pepsoft.util.IconUtils;
import org.pepsoft.util.swing.TileListener;
import org.pepsoft.util.swing.TiledImageViewer;
import org.pepsoft.worldpainter.layers.renderers.VoidRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;

import static org.pepsoft.minecraft.Material.*;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE;

/**
 * WorldPainter tile provider for {@link TiledImageViewer}s. Provides tiles based on a WorldPainter {@link Dimension}
 * and paints exclusively the border tiles for it, if any.
 *
 * @author pepijn
 */
public class WPBorderTileProvider implements org.pepsoft.util.swing.TileProvider {
    public WPBorderTileProvider(Dimension dimension, ColourScheme colourScheme, Effect effect) {
        this.dimension = dimension;
        this.colourScheme = colourScheme;
        this.effect = effect;
    }

    public WPBorderTileProvider(Dimension dimension, ColourScheme colourScheme) {
        this(dimension, colourScheme, null);
    }
    
    @Override
    public int getTileSize() {
        return TILE_SIZE;
    }

    @Override
    public boolean isTilePresent(int x, int y) {
        if (zoom == 0) {
            final TileType unzoomedTileType = getUnzoomedTileType(x, y);
            return (unzoomedTileType != TileType.SURROUNDS) && (unzoomedTileType != TileType.WORLD);
        } else {
            final int scale = 1 << -zoom;
            for (int dx = 0; dx < scale; dx++) {
                for (int dy = 0; dy < scale; dy++) {
                    switch (getUnzoomedTileType(x * scale + dx, y * scale + dy)) {
                        case BORDER:
                        case BEDROCK_WALL:
                        case BARRIER_WALL:
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
                    final Color waterColour = new Color(colourScheme.getColour(WATER));
                    final Color lavaColour = new Color(colourScheme.getColour(LAVA));
                    final Color voidColour = new Color(0x00ffffff & VoidRenderer.getColour(), true);
                    final Color bedrockColour = new Color(colourScheme.getColour(BEDROCK));
                    final int scale = 1 << -zoom;
                    final int subSize = TILE_SIZE / scale;
                    for (int dx = 0; dx < scale; dx++) {
                        for (int dy = 0; dy < scale; dy++) {
                            final TileType tileType = getUnzoomedTileType(x * scale + dx, y * scale + dy);
                            switch (tileType) {
                                case BORDER:
                                    final Paint paint;
                                    switch (dimension.getBorder()) {
                                        case WATER:
                                        case ENDLESS_WATER:
                                            paint = waterColour;
                                            break;
                                        case LAVA:
                                        case ENDLESS_LAVA:
                                            paint = lavaColour;
                                            break;
                                        case VOID:
                                        case ENDLESS_VOID:
                                            paint = voidColour;
                                            break;
                                        case BARRIER:
                                        case ENDLESS_BARRIER:
                                            paint = BARRIER_PAINT; // TODO apply effects
                                            break;
                                        default:
                                            throw new InternalError();
                                    }
                                    g2.setPaint(paint);
                                    g2.fillRect(imageX + dx * subSize, imageY + dy * subSize, subSize, subSize);

                                    // Draw border lines
                                    g2.setColor(Color.BLACK);
                                    g2.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0.0f, new float[] {4.0f, 4.0f}, 0.0f));
                                    if (dimension.isTilePresent(x * scale + dx, y * scale + dy - 1)) {
                                        g2.drawLine(imageX + dx       * subSize    , imageY + dy       * subSize    , imageX + (dx + 1) * subSize - 1, imageY + dy       * subSize);
                                    }
                                    if (dimension.isTilePresent(x * scale + dx + 1, y * scale + dy)) {
                                        g2.drawLine(imageX + (dx + 1) * subSize - 1, imageY + dy       * subSize    , imageX + (dx + 1) * subSize - 1, imageY + (dy + 1) * subSize - 1);
                                    }
                                    if (dimension.isTilePresent(x * scale + dx, y * scale + dy + 1)) {
                                        g2.drawLine(imageX + dx       * subSize    , imageY + (dy + 1) * subSize - 1, imageX + (dx + 1) * subSize - 1, imageY + (dy + 1) * subSize - 1);
                                    }
                                    if (dimension.isTilePresent(x * scale + dx - 1, y * scale + dy)) {
                                        g2.drawLine(imageX + dx       * subSize    , imageY + dy       * subSize    , imageX + dx       * subSize    , imageY + (dy + 1) * subSize - 1);
                                    }
                                    break;
                                case BEDROCK_WALL:
                                case BARRIER_WALL:
                                    g2.setColor(voidColour);
                                    g2.fillRect(imageX + dx * subSize, imageY + dy * subSize, subSize, subSize);
                                    g2.setPaint((tileType == TileType.BEDROCK_WALL) ? bedrockColour : BARRIER_PAINT);
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
                                    break;
                                default:
                                    g2.clearRect(imageX + dx * subSize, imageY + dy * subSize, subSize, subSize);
                                    break;
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
        return -1;
    }

    @Override
    public Rectangle getExtent() {
        return null;
    }
    
    @Override
    public void addTileListener(TileListener tileListener) {
        // Do nothing
    }

    @Override
    public void removeTileListener(TileListener tileListener) {
        // Do nothing
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
        }
    }
    
    private TileType getUnzoomedTileType(int x, int y) {
        if (dimension.isTilePresent(x, y)) {
            return TileType.WORLD;
        } else {
            if (dimension.isBorderTile(x, y)) {
                return TileType.BORDER;
            } else if ((dimension.getWallType() != null)
                    && ((dimension.getBorder() != null)
                        ? (dimension.isBorderTile(x - 1, y)
                            || dimension.isBorderTile(x, y - 1)
                            || dimension.isBorderTile(x + 1, y)
                            || dimension.isBorderTile(x, y + 1))
                        : (this.dimension.isTilePresent(x - 1, y)
                            || this.dimension.isTilePresent(x, y - 1)
                            || this.dimension.isTilePresent(x + 1, y)
                            || this.dimension.isTilePresent(x, y + 1)))) {
                return (dimension.getWallType() == Dimension.WallType.BEDROCK) ? TileType.BEDROCK_WALL : TileType.BARRIER_WALL;
            }
        }
        return TileType.SURROUNDS;
    }
    
    private boolean paintUnzoomedTile(final Image tileImage, final int x, final int y, final int dx, final int dy) {
        final TileType tileType = getUnzoomedTileType(x, y);
        switch (tileType) {
            case WORLD:
            case SURROUNDS:
                return false;
            case BORDER:
                final Paint paint;
                switch (dimension.getBorder()) {
                    case WATER:
                    case ENDLESS_WATER:
                        paint = new Color(0xff000000 | colourScheme.getColour(WATER));
                        break;
                    case LAVA:
                    case ENDLESS_LAVA:
                        paint = new Color(0xff000000 | colourScheme.getColour(LAVA));
                        break;
                    case VOID:
                    case ENDLESS_VOID:
                        paint = new Color(0x00ffffff & VoidRenderer.getColour());
                        break;
                    case BARRIER:
                    case ENDLESS_BARRIER:
                        paint = BARRIER_PAINT;
                        break;
                    default:
                        throw new InternalError();
                }
                Graphics2D g2 = (Graphics2D) tileImage.getGraphics();
                try {
                    g2.setPaint(paint);
                    g2.setComposite(AlphaComposite.Src);
                    g2.fillRect(dx, dy, TILE_SIZE, TILE_SIZE);
                    
                    // Draw border lines
                    g2.setColor(Color.BLACK);
                    g2.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0.0f, new float[] {4.0f, 4.0f}, 0.0f));
                    if (dimension.isTilePresent(x, y - 1)) {
                        g2.drawLine(dx + 1, dy + 1, dx + TILE_SIZE - 1, dy + 1);
                    }
                    if (dimension.isTilePresent(x + 1, y)) {
                        g2.drawLine(dx + TILE_SIZE - 1, dy + 1, dx + TILE_SIZE - 1, dy + TILE_SIZE - 1);
                    }
                    if (dimension.isTilePresent(x, y + 1)) {
                        g2.drawLine(dx + 1, dy + TILE_SIZE - 1, dx + TILE_SIZE - 1, dy + TILE_SIZE - 1);
                    }
                    if (dimension.isTilePresent(x - 1, y)) {
                        g2.drawLine(dx + 1, dy + 1, dx + 1, dy + TILE_SIZE - 1);
                    }
                    applyEffects(g2);
                } finally {
                    g2.dispose();
                }
                return true;
            case BEDROCK_WALL:
            case BARRIER_WALL:
                g2 = (Graphics2D) tileImage.getGraphics();
                try {
                    g2.setColor(new Color(0x00ffffff & VoidRenderer.getColour(), true));
                    g2.setComposite(AlphaComposite.Src);
                    g2.fillRect(dx, dy, TILE_SIZE, TILE_SIZE);
                    if (tileType == TileType.BARRIER_WALL) {
                        g2.setPaint(BARRIER_PAINT);
                    } else {
                        g2.setColor(new Color(colourScheme.getColour(BEDROCK)));
                    }
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
                    applyEffects(g2);
                } finally {
                    g2.dispose();
                }
                return true;
            default:
                throw new InternalError();
        }
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

    private final Dimension dimension;
    private final ColourScheme colourScheme;
    private final Effect effect;

    private int zoom = 0;

    private static final Paint BARRIER_PAINT = new TexturePaint(IconUtils.loadUnscaledImage("org/pepsoft/worldpainter/icons/barrier.png"), new Rectangle(0, 0, 16, 16));
    private static final Logger logger = LoggerFactory.getLogger(WPBorderTileProvider.class);
    
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
         * The tile is outside the WorldPainter world and border but does contain part of a bedrock wall.
         */
        BEDROCK_WALL,
        /**
         * The tile is outside the WorldPainter world and border but does contain part of a barrier wall.
         */
        BARRIER_WALL
    }

    public enum Effect {
        FADE_TO_FIFTY_PERCENT, FADE_TO_TWENTYFIVE_PERCENT
    }
}