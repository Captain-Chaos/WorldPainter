/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter;

import java.awt.Point;
import static org.pepsoft.worldpainter.Constants.*;

/**
 *
 * @author pepijn
 */
public final class Coordinates {
    private Coordinates() {
        // Prevent instantiation
    }

    /**
     * Translate a block coordinate within a tile to absolute world coordinates.
     *
     * @param tile The tile in which to translate coordinates.
     * @param blockX The X coordinate of the block within the tile.
     * @param blockY The Y coordinate of the block within the tile.
     * @return The corresponding absolute world coordinates.
     */
    public static Point tileLocalToAbsolute(Tile tile, int blockX, int blockY) {
        return new Point((tile.getX() << TILE_SIZE_BITS) | blockX, (tile.getY() << TILE_SIZE_BITS) | blockY);
    }

    /**
     * Translate a chunk coordinate within a tile to absolute world coordinates.
     *
     * @param tile The tile in which to translate coordinates.
     * @param chunkX The X coordinate of the chunk within the tile.
     * @param chunkY The Y coordinate of the chunk within the tile.
     * @return The corresponding absolute world coordinates of the chunk's north
     *     west corner.
     */
    public static Point chunkInTileToAbsolute(Tile tile, int chunkX, int chunkY) {
        return new Point((tile.getX() << TILE_SIZE_BITS) | (chunkX << 4), (tile.getY() << TILE_SIZE_BITS) | (chunkY << 4));
    }
    
    /**
     * Translate absolute block coordinates to a set of chunk coordinates, and
     * local coordinates within the chunk.
     *
     * @param blockX The absolute X coordinate of the block.
     * @param blockZ The absolute Z coordinate of the block.
     * @return An array containing two sets of coordinates. The point at index 0
     *     contains the coordinates of the chunk, and point at index 1 contains
     *     the coordinates of the block within the chunk.
     */
    public static Point[] blockToChunk(int blockX, int blockZ) {
        return new Point[] {new Point(blockX >> 4, blockZ >> 4), new Point(blockX & 15, blockZ & 15)};
    }
}