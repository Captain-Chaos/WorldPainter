/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.viewer.tiled;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.TileRendererFactory;
import org.pepsoft.worldpainter.viewer.Layer;
import org.pepsoft.worldpainter.viewer.LayerView;
import static org.pepsoft.worldpainter.viewer.tiled.TileConstants.*;

/**
 *
 * @author pepijn
 */
public class TiledLayerView extends LayerView implements TilePaintQueue.Listener, org.pepsoft.worldpainter.Tile.Listener {
    public TiledLayerView(Layer layer, Dimension dimension, TileRendererFactory tileRendererFactory) {
        super(layer);
        this.dimension = dimension;
        this.tileRendererFactory = tileRendererFactory;
        setBounds(dimension.getLowestX() << TILE_SIZE_BITS,
                dimension.getLowestY() << TILE_SIZE_BITS,
                dimension.getWidth() << TILE_SIZE_BITS,
                dimension.getHeight() << TILE_SIZE_BITS);
    }

    @Override
    public void setVisibleRegion(Rectangle visibleRegion) {
        if (visibleRegion.equals(this.visibleRegion)) {
//            System.out.println("No change");
//            System.out.print(tileCacheWidth + "x" + tileCacheHeight + " -> ");
            // No change
        } else if (((visibleRegion.width >> TILE_SIZE_BITS) == (this.visibleRegion.width >> TILE_SIZE_BITS))
                && ((visibleRegion.height >> TILE_SIZE_BITS) == (this.visibleRegion.height >> TILE_SIZE_BITS))
                && ((visibleRegion.x >> TILE_SIZE_BITS) == (this.visibleRegion.x >> TILE_SIZE_BITS))
                && ((visibleRegion.y >> TILE_SIZE_BITS) == (this.visibleRegion.y >> TILE_SIZE_BITS))) {
//            System.out.println("No effective change");
//            System.out.print(tileCacheWidth + "x" + tileCacheHeight + " -> ");
            // No change in visible tiles
            this.visibleRegion = new Rectangle(visibleRegion);
        } else {
//            System.out.println("Change");
//            System.out.print(tileCacheWidth + "x" + tileCacheHeight + " -> ");
            synchronized (queue) {
                // Gather up all tiles which are no longer visible and can be reused
                List<Tile> discardedTiles = new ArrayList<Tile>(tileCacheWidth * tileCacheHeight);
                for (int x = 0; x < tileCacheWidth; x++) {
                    for (int y = 0; y < tileCacheHeight; y++) {
                        if (! visibleRegion.contains((tileOffsetX + x) << TILE_SIZE_BITS, (tileOffsetY + y) << TILE_SIZE_BITS, TILE_SIZE, TILE_SIZE)) {
                            // The tile is no longer visible; store the tile for
                            // reuse
                            Tile tile = tiles[x][y];
                            if (tile.paintJob != null) {
                                // The tile has been scheduled for painting. Tell
                                // the scheduler it is no longer necessary
                                queue.cancel(tile.paintJob);
                                tile.paintJob = null;
                            }
                            
                            // We're not interested in tile data changes for this
                            // tile any more so unregister the listener
                            org.pepsoft.worldpainter.Tile wpTile = dimension.getTile(tile.x, tile.y);
                            if (wpTile != null) {
                                wpTile.removeListener(this);
                            }
                            
                            discardedTiles.add(tile);
                            tiles[x][y] = null;
                        }
                    }
                }

                // Create the new tiles array
                Tile[][] oldTiles = tiles;
                int oldHeight = tileCacheHeight, oldOffsetX = tileOffsetX, oldOffsetY = tileOffsetY;
                int reuseableTileCount = discardedTiles.size();
                tileOffsetX = visibleRegion.x >>  TILE_SIZE_BITS;
                int tileX2 = (visibleRegion.x + visibleRegion.width - 1) >> TILE_SIZE_BITS;
                tileOffsetY = visibleRegion.y >>  TILE_SIZE_BITS;
                int tileY2 = (visibleRegion.y + visibleRegion.height - 1) >> TILE_SIZE_BITS;
                tileCacheWidth = (tileX2 - tileOffsetX) + 1;
                tileCacheHeight = (tileY2 - tileOffsetY) + 1;
                this.visibleRegion = new Rectangle(visibleRegion);
                tiles = new Tile[tileCacheWidth][tileCacheHeight];

                // Fill the new array. Reuse tiles that are still visible, recycle
                // tiles that have become invisible and only create new tiles if
                // absolutely necessary
                for (int x = tileOffsetX; x <= tileX2; x++) {
                    for (int y = tileOffsetY; y <= tileY2; y++) {
                        Tile tile = getTile(oldTiles, oldHeight, x, y, oldOffsetX, oldOffsetY);
                        if (tile == null) {
                            if (reuseableTileCount > 0) {
                                System.out.println("Recycling tile " + x + "," + y);
                                tile = discardedTiles.get(reuseableTileCount - 1);
                                reuseableTileCount--;
                                tile.x = x;
                                tile.y = y;
                                tile.dirty = true;
                            } else {
                                System.out.println("Creating tile " + x + "," + y);
                                tile = new Tile(x, y);
                            }
                            // Schedule for painting
                            queue.addJob(new TilePaintJob(tile));

                            // Register listener for tile data changes so we
                            // can repaint the tile if it changes
                            org.pepsoft.worldpainter.Tile wpTile = dimension.getTile(tile.x, tile.y);
                            if (wpTile != null) {
                                wpTile.addListener(this);
                            }
                        } else {
                            System.out.println("Reusing tile " + x + "," + y);
                        }
                        tiles[x - tileOffsetX][y - tileOffsetY] = tile;
                    }
                }
            }
        }
//        System.out.println(tileCacheWidth + "x" + tileCacheHeight);
//        for (Tile[] tileRow : tiles) {
//            for (int y = 0; y < tileRow.length; y++) {
//                if (y > 0) {
//                    System.out.print(", ");
//                }
//                System.out.print(tileRow[y]);
//            }
//            System.out.println();
//        }
    }
    
    @Override
    public void start() {
        queue.start(tileRendererFactory);
    }
    
    @Override
    public void shutdown() {
        queue.shutdown();
    }

    // TilePaintQueue.Listener

    @Override
    public void tilePainted(Tile tile) {
        synchronized (queue) {
            tile.paintJob = null;
            repaint(tile.x << TILE_SIZE_BITS, tile.y << TILE_SIZE_BITS, TILE_SIZE, TILE_SIZE);
        }
    }

    // org.pepsoft.worldpainter.Tile.Listener
    
    @Override
    public void heightMapChanged(org.pepsoft.worldpainter.Tile wpTile) {
        scheduleForPainting(wpTile.getX(), wpTile.getY());
    }

    @Override
    public void terrainChanged(org.pepsoft.worldpainter.Tile wpTile) {
        scheduleForPainting(wpTile.getX(), wpTile.getY());
    }

    @Override
    public void waterLevelChanged(org.pepsoft.worldpainter.Tile wpTile) {
        scheduleForPainting(wpTile.getX(), wpTile.getY());
    }

    @Override
    public void layerDataChanged(org.pepsoft.worldpainter.Tile wpTile, Set<org.pepsoft.worldpainter.layers.Layer> changedLayers) {
        scheduleForPainting(wpTile.getX(), wpTile.getY());
    }

    @Override
    public void allBitLayerDataChanged(org.pepsoft.worldpainter.Tile wpTile) {
        scheduleForPainting(wpTile.getX(), wpTile.getY());
    }

    @Override
    public void allNonBitlayerDataChanged(org.pepsoft.worldpainter.Tile wpTile) {
        scheduleForPainting(wpTile.getX(), wpTile.getY());
    }

    @Override
    public void seedsChanged(org.pepsoft.worldpainter.Tile wpTile) {
        scheduleForPainting(wpTile.getX(), wpTile.getY());
    }
    
    private void scheduleForPainting(int x, int y) {
        synchronized (queue) {
            Tile tile = getTile(tiles, tileCacheHeight, x, y, tileOffsetX, tileOffsetY);
            if ((tile != null) && (tile.paintJob == null)) {
                TilePaintJob paintJob = new TilePaintJob(tile);
                tile.paintJob = paintJob;
                queue.addJob(paintJob);
            }
        }
    }

    private static Tile getTile(Tile[][] array, int height, int x, int y, int offsetX, int offsetY) {
        x -= offsetX;
        y -= offsetY;
        if ((x >= 0) && (x < array.length) && (y >= 0) && (y < height)) {
            return array[x][y];
        } else {
            return null;
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        Rectangle clipBounds = g2.getClipBounds();
        Rectangle paintBounds = (clipBounds != null) ? clipBounds.intersection(visibleRegion) : visibleRegion;
        int tileX1 = paintBounds.x >>  TILE_SIZE_BITS;
        int tileX2 = (paintBounds.x + paintBounds.width - 1) >> TILE_SIZE_BITS;
        int tileY1 = paintBounds.y >>  TILE_SIZE_BITS;
        int tileY2 = (paintBounds.y + paintBounds.height - 1) >> TILE_SIZE_BITS;
        for (int x = tileX1; x <= tileX2; x++) {
            for (int y = tileY1; y <= tileY2; y++) {
                Tile tile = getTile(tiles, tileCacheHeight, x, y, tileOffsetX, tileOffsetY);
                if ((tile != null) && (! tile.dirty)) {
                    System.out.println("Painting tile " + x + "," + y + " at " + (x << TILE_SIZE_BITS) + "," + (y << TILE_SIZE_BITS));
                    g2.drawImage(tile.image, x << TILE_SIZE_BITS, y << TILE_SIZE_BITS, null);
                }
            }
        }
    }
    
    private final Dimension dimension;
    private final TileRendererFactory tileRendererFactory;
    private final TilePaintQueue queue = new TilePaintQueue(this);
    private Rectangle visibleRegion = new Rectangle(0, 0, 0, 0);
    private Tile[][] tiles = new Tile[0][];
    private int tileCacheWidth, tileCacheHeight;
    /**
     * The tile offset describes the coordinates (in tiles) of the tile in
     * tiles[0][0]
     */
    private int tileOffsetX, tileOffsetY;
}