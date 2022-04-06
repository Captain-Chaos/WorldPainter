/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter;

import org.pepsoft.minecraft.MapGenerator;
import org.pepsoft.minecraft.SeededGenerator;
import org.pepsoft.util.AttributeKey;
import org.pepsoft.util.MathUtils;
import org.pepsoft.util.PerlinNoise;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.util.ProgressReceiver.OperationCancelled;
import org.pepsoft.util.undo.UndoManager;
import org.pepsoft.worldpainter.biomeschemes.CustomBiome;
import org.pepsoft.worldpainter.brushes.Brush;
import org.pepsoft.worldpainter.exporting.ExportSettings;
import org.pepsoft.worldpainter.gardenofeden.Garden;
import org.pepsoft.worldpainter.gardenofeden.Seed;
import org.pepsoft.worldpainter.layers.*;
import org.pepsoft.worldpainter.layers.exporters.ExporterSettings;
import org.pepsoft.worldpainter.layers.exporters.ResourcesExporter.ResourcesExporterSettings;
import org.pepsoft.worldpainter.operations.Filter;
import org.pepsoft.worldpainter.panels.DefaultFilter;
import org.pepsoft.worldpainter.selection.SelectionBlock;
import org.pepsoft.worldpainter.selection.SelectionChunk;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import javax.swing.*;
import javax.vecmath.Point3i;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;
import static org.pepsoft.minecraft.Material.*;
import static org.pepsoft.worldpainter.Constants.*;
import static org.pepsoft.worldpainter.DefaultPlugin.JAVA_ANVIL;
import static org.pepsoft.worldpainter.Generator.*;
import static org.pepsoft.worldpainter.biomeschemes.Minecraft1_18Biomes.*;

/**
 *
 * @author pepijn
 */
public class Dimension extends InstanceKeeper implements TileProvider, Serializable, Tile.Listener, Cloneable {
    public Dimension(World2 world, long minecraftSeed, TileFactory tileFactory, int dim, int minHeight, int maxHeight) {
        this(world, minecraftSeed, tileFactory, dim, minHeight, maxHeight, true);
    }

    public Dimension(World2 world, long minecraftSeed, TileFactory tileFactory, int dim, int minHeight, int maxHeight, boolean init) {
        if (world == null) {
            throw new NullPointerException("world");
        }
        this.world = world;
        this.seed = tileFactory.getSeed();
        this.minecraftSeed = minecraftSeed;
        this.tileFactory = tileFactory;
        this.dim = dim;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        if (init) {
            layerSettings.put(Resources.INSTANCE, ResourcesExporterSettings.defaultSettings(world.getPlatform(), dim, maxHeight));
            topLayerDepthNoise = new PerlinNoise(seed + TOP_LAYER_DEPTH_SEED_OFFSET);
            switch (dim) {
                case DIM_NORMAL:
                    generator = new SeededGenerator(DEFAULT, minecraftSeed);
                    break;
                case DIM_NETHER:
                    generator = new SeededGenerator(NETHER, minecraftSeed);
                    break;
                case DIM_END:
                    generator = new SeededGenerator(END, minecraftSeed);
                    break;
            }
        }
    }

    public World2 getWorld() {
        return world;
    }

    public int getDim() {
        return dim;
    }

    public String getName() {
        switch (dim) {
            case -3:
                return "End Ceiling";
            case -2:
                return "Nether Ceiling";
            case -1:
                return "Surface Ceiling";
            case 0:
                return "Surface";
            case 1:
                return "Nether";
            case 2:
                return "End";
            default:
                return "Dimension " + dim;
        }
    }

    /**
     * Get the change number. This number is updated every time the state of the
     * dimension changes and can be used to determine whether the state needs to
     * be saved.
     *
     * @return The current change number.
     */
    public long getChangeNo() {
        return changeNo;
    }

    /**
     * Update the change number; for use when some aspect of a dimension changes
     * which the {@code Dimension} class itself does not track.
     */
    public void changed() {
        changeNo++;
    }

    public long getSeed() {
        return seed;
    }

    public Terrain getSubsurfaceMaterial() {
        return subsurfaceMaterial;
    }

    public void setSubsurfaceMaterial(Terrain subsurfaceMaterial) {
        if (subsurfaceMaterial != this.subsurfaceMaterial) {
            Terrain oldSubsurfaceMaterial = this.subsurfaceMaterial;
            this.subsurfaceMaterial = subsurfaceMaterial;
            changeNo++;
            propertyChangeSupport.firePropertyChange("subsurfaceMaterial", oldSubsurfaceMaterial, subsurfaceMaterial);
        }
    }

    public boolean isPopulate() {
        return populate;
    }

    public void setPopulate(boolean populate) {
        if (populate != this.populate) {
            this.populate = populate;
            changeNo++;
            propertyChangeSupport.firePropertyChange("populate", ! populate, populate);
        }
    }

    public Border getBorder() {
        return border;
    }

    public void setBorder(Border border) {
        if (border != this.border) {
            Border oldBorder = this.border;
            this.border = border;
            changeNo++;
            propertyChangeSupport.firePropertyChange("border", oldBorder, border);
        }
    }

    public int getBorderLevel() {
        return borderLevel;
    }

    public void setBorderLevel(int borderLevel) {
        if (borderLevel != this.borderLevel) {
            int oldBorderLevel = this.borderLevel;
            this.borderLevel = borderLevel;
            changeNo++;
            propertyChangeSupport.firePropertyChange("borderLevel", oldBorderLevel, borderLevel);
        }
    }

    public int getBorderSize() {
        return borderSize;
    }

    public void setBorderSize(int borderSize) {
        if (borderSize != this.borderSize) {
            int oldBorderSize = this.borderSize;
            this.borderSize = borderSize;
            changeNo++;
            propertyChangeSupport.firePropertyChange("borderSize", oldBorderSize, borderSize);
        }
    }

    public boolean isDarkLevel() {
        return darkLevel;
    }

    public void setDarkLevel(boolean darkLevel) {
        if (darkLevel != this.darkLevel) {
            this.darkLevel = darkLevel;
            changeNo++;
            propertyChangeSupport.firePropertyChange("darkLevel", ! darkLevel, darkLevel);
        }
    }

    public boolean isBedrockWall() {
        return bedrockWall;
    }

    public void setBedrockWall(boolean bedrockWall) {
        if (bedrockWall != this.bedrockWall) {
            this.bedrockWall = bedrockWall;
            changeNo++;
            propertyChangeSupport.firePropertyChange("bedrockWall", ! bedrockWall, bedrockWall);
        }
    }

    public TileFactory getTileFactory() {
        return tileFactory;
    }

    /**
     * Determines whether a tile is present in the dimension on specific
     * coordinates.
     *
     * @param x The world X coordinate for which to determine whether a tile is
     *     present.
     * @param y The world Y coordinate for which to determine whether a tile is
     *     present.
     * @return {@code true} if the dimension contains a tile at the
     *     specified location.
     */
    @Override
    public synchronized boolean isTilePresent(final int x, final int y) {
        return tiles.containsKey(new Point(x, y));
    }

    /**
     * Indicates whether the specified tile is a border tile.
     *
     * @param x The X coordinate of the tile for which to check whether it is a
     *     border tile.
     * @param y The Y coordinate of the tile for which to check whether it is a
     *     border tile.
     * @return {@code true} if it is a border tile.
     */
    public synchronized boolean isBorderTile(int x, int y) {
        if ((border == null)
                || ((! border.isEndless())
                    && ((x < (lowestX - borderSize))
                        || (x > (highestX + borderSize))
                        || (y < (lowestY - borderSize))
                        || (y > (highestY + borderSize))))) {
            // Couldn't possibly be a border tile
            return false;
        } else if (tiles.containsKey(new Point(x, y))) {
            // There's a tile in the dimension at these coordinates, so not a
            // border tile
            return false;
        } else if (border.isEndless()) {
            // The border is an endless border, so any tile outside the
            // dimension is a border tile
            return true;
        } else {
            for (int r = 1; r <= borderSize; r++) {
                for (int i = 0; i <= (r * 2); i++) {
                    if (tiles.containsKey(new Point(x + i - r, y - r))
                        || tiles.containsKey(new Point(x + r, y + i - r))
                        || tiles.containsKey(new Point(x + r - i, y + r))
                        || tiles.containsKey(new Point(x - r, y - i + r))) {
                        // Found a tile in the dimension <= borderSize tiles
                        // away, so this is a border tile
                        return true;
                    }
                }
            }
            // No tiles in dimension <= borderSize tiles away, so not a border
            // tile
            return false;
        }
    }

    /**
     * Get the tile for a particular set of world or absolute block coordinates.
     *
     * @param x The world X coordinate for which to get the tile.
     * @param y The world Y coordinate for which to get the tile.
     * @return The tile on which the specified coordinates lie, or
     *     {@code null} if there is no tile for those coordinates
     */
    @Override
    public synchronized Tile getTile(final int x, final int y) {
        return tiles.get(new Point(x, y));
    }

    public synchronized Tile getTile(final Point coords) {
        return tiles.get(coords);
    }

    /**
     * Get the tile for a particular set of world or absolute block coordinates with the intention of modifying it. This
     * is intended to be used in combination with {@link #setEventsInhibited(boolean)}. Whenever
     * {@code eventsInhibited} is {@code true}, the dimension will automatically inhibit events on the tile,
     * mark it as dirty and fire an event for it when {@code eventsInhibited} is set to {@code false}.
     *
     * @param x The world X coordinate for which to get the tile.
     * @param y The world Y coordinate for which to get the tile.
     * @return The tile on which the specified coordinates lie, or
     *     {@code null} if there is no tile for those coordinates
     */
    public synchronized Tile getTileForEditing(final int x, final int y) {
        Tile tile = tiles.get(new Point(x, y));
        if ((tile != null) && eventsInhibited && (! tile.isEventsInhibited())) {
            tile.inhibitEvents();
            dirtyTiles.add(tile);
        }
        return tile;
    }

    /**
     * Get the tile for a particular set of world or absolute block coordinates with the intention of modifying it. This
     * is intended to be used in combination with {@link #setEventsInhibited(boolean)}. Whenever
     * {@code eventsInhibited} is {@code true}, the dimension will automatically inhibit events on the tile,
     * mark it as dirty and fire an event for it when {@code eventsInhibited} is set to {@code false}.
     *
     * @param coords The world coordinates for which to get the tile.
     * @return The tile on which the specified coordinates lie, or
     *     {@code null} if there is no tile for those coordinates
     */
    public synchronized Tile getTileForEditing(final Point coords) {
        Tile tile = tiles.get(coords);
        if ((tile != null) && eventsInhibited && (! tile.isEventsInhibited())) {
            tile.inhibitEvents();
            dirtyTiles.add(tile);
        }
        return tile;
    }

    @Override
    public Rectangle getExtent() {
        return new Rectangle(lowestX, lowestY, (highestX - lowestX) + 1, (highestY - lowestY) + 1);
    }

    public int getTileCount() {
        return tiles.size();
    }

    public Collection<? extends Tile> getTiles() {
        return Collections.unmodifiableCollection(tiles.values());
    }

    public Set<Point> getTileCoords() {
        return Collections.unmodifiableSet(tiles.keySet());
    }

    public synchronized void addTile(Tile tile) {
        if (tile.getMaxHeight() != maxHeight) {
            throw new IllegalArgumentException("Tile has different max height (" + tile.getMaxHeight() + ") than dimension (" + maxHeight + ")");
        }
        final int x = tile.getX();
        final int y = tile.getY();
        final Point key = new Point(x, y);
        if (tiles.containsKey(key)) {
            throw new IllegalStateException("Tile already set");
        }
        tile.addListener(this);
        if (undoManager != null) {
            tile.register(undoManager);
        }
        tiles.put(key, tile);
        if (x < lowestX) {
            lowestX = x;
        }
        if (x > highestX) {
            highestX = x;
        }
        if (y < lowestY) {
            lowestY = y;
        }
        if (y > highestY) {
            highestY = y;
        }
        fireTileAdded(tile);
        changeNo++;
    }

    public void removeTile(int tileX, int tileY) {
        removeTile(new Point(tileX, tileY));
    }

    public void removeTile(Tile tile) {
        removeTile(tile.getX(), tile.getY());
    }

    public synchronized void removeTile(Point coords) {
        if (! tiles.containsKey(coords)) {
            throw new IllegalStateException("Tile not set");
        }
        final Tile tile = tiles.remove(coords);
        if (undoManager != null) {
            tile.unregister();
        }
        tile.removeListener(this);
        // If the tile lies at the edge of the world it's possible the low and
        // high coordinate marks should change; so recalculate them in that case
        if ((coords.x == lowestX) || (coords.x == highestX) || (coords.y == lowestY) || (coords.y == highestY)) {
            lowestX = Integer.MAX_VALUE;
            highestX = Integer.MIN_VALUE;
            lowestY = Integer.MAX_VALUE;
            highestY = Integer.MIN_VALUE;
            for (Tile myTile: tiles.values()) {
                int myTileX = myTile.getX(), myTileY = myTile.getY();
                if (myTileX < lowestX) {
                    lowestX = myTileX;
                }
                if (myTileX > highestX) {
                    highestX = myTileX;
                }
                if (myTileY < lowestY) {
                    lowestY = myTileY;
                }
                if (myTileY > highestY) {
                    highestY = myTileY;
                }
            }
        }
        fireTileRemoved(tile);
        changeNo++;
    }

    public int getHighestX() {
        return highestX;
    }

    public int getHighestY() {
        return highestY;
    }

    public int getLowestX() {
        return lowestX;
    }

    public int getLowestY() {
        return lowestY;
    }

    public int getWidth() {
        if (highestX == Integer.MIN_VALUE) {
            return 0;
        } else {
            return highestX - lowestX + 1;
        }
    }

    public int getHeight() {
        if (highestY == Integer.MIN_VALUE) {
            return 0;
        } else {
            return highestY - lowestY + 1;
        }
    }

    public int getIntHeightAt(int x, int y) {
        return getIntHeightAt(x, y, Integer.MIN_VALUE);
    }

    public int getIntHeightAt(int x, int y, int defaultHeight) {
        Tile tile = getTile(x >> TILE_SIZE_BITS, y >> TILE_SIZE_BITS);
        if (tile != null) {
            return tile.getIntHeight(x & TILE_SIZE_MASK, y & TILE_SIZE_MASK);
        } else {
            return defaultHeight;
        }
    }

    public int getIntHeightAt(Point coords) {
        return getIntHeightAt(coords.x, coords.y, Integer.MIN_VALUE);
    }

    public int getLowestIntHeight() {
        int lowestHeight = Integer.MAX_VALUE;
        for (Tile tile: tiles.values()) {
            int tileLowestHeight = tile.getLowestIntHeight();
            if (tileLowestHeight < lowestHeight) {
                lowestHeight = tileLowestHeight;
            }
            if (lowestHeight <= minHeight) {
                return minHeight;
            }
        }
        return lowestHeight;
    }

    public int getHightestIntHeight() {
        int highestHeight = Integer.MIN_VALUE;
        for (Tile tile: tiles.values()) {
            int tileHighestHeight = tile.getHighestIntHeight();
            if (tileHighestHeight > highestHeight) {
                highestHeight = tileHighestHeight;
            }
            if (highestHeight >= (maxHeight - 1)) {
                return maxHeight;
            }
        }
        return highestHeight;
    }

    public float getHeightAt(int x, int y) {
        Tile tile = getTile(x >> TILE_SIZE_BITS, y >> TILE_SIZE_BITS);
        if (tile != null) {
            return tile.getHeight(x & TILE_SIZE_MASK, y & TILE_SIZE_MASK);
        } else {
            return Float.MIN_VALUE;
        }
    }

    public float getHeightAt(Point coords) {
        return getHeightAt(coords.x, coords.y);
    }

    public void setHeightAt(int x, int y, float height) {
        Tile tile = getTileForEditing(x >> TILE_SIZE_BITS, y >> TILE_SIZE_BITS);
        if (tile != null) {
            tile.setHeight(x & TILE_SIZE_MASK, y & TILE_SIZE_MASK, height);
        }
    }

    public void setHeightAt(Point coords, float height) {
        setHeightAt(coords.x, coords.y, height);
    }

    /**
     * Get the raw height value. This is the height times 256 (for added precision) and zero-based rather than adjusted
     * for {@code minHeight}.
     */
    public int getRawHeightAt(int x, int y) {
        Tile tile = getTile(x >> TILE_SIZE_BITS, y >> TILE_SIZE_BITS);
        if (tile != null) {
            return tile.getRawHeight(x & TILE_SIZE_MASK, y & TILE_SIZE_MASK);
        } else {
            return Integer.MIN_VALUE;
        }
    }

    /**
     * Get the raw height value. This is the height times 256 (for added precision) and zero-based rather than adjusted
     * for {@code minHeight}.
     */
    public int getRawHeightAt(Point coords) {
        return getRawHeightAt(coords.x, coords.y);
    }

    /**
     * Set the raw height value. This is the height times 256 (for added precision) and zero-based rather than adjusted
     * for {@code minHeight}.
     */
    public void setRawHeightAt(int x, int y, int rawHeight) {
        Tile tile = getTileForEditing(x >> TILE_SIZE_BITS, y >> TILE_SIZE_BITS);
        if (tile != null) {
            tile.setRawHeight(x & TILE_SIZE_MASK, y & TILE_SIZE_MASK, rawHeight);
        }
    }

    /**
     * Set the raw height value. This is the height times 256 (for added precision) and zero-based rather than adjusted
     * for {@code minHeight}.
     */
    public void setRawHeightAt(Point coords, int rawHeight) {
        setRawHeightAt(coords.x, coords.y, rawHeight);
    }

    public float getSlope(int x, int y) {
        final int xInTile = x & TILE_SIZE_MASK, yInTile = y & TILE_SIZE_MASK;
        if ((xInTile > 0) && (xInTile < (TILE_SIZE - 1)) && (yInTile > 0) && (yInTile < (TILE_SIZE - 1))) {
            // Inside one tile; delegate to tile
            Tile tile = getTile(x >> TILE_SIZE_BITS, y >> TILE_SIZE_BITS);
            if (tile != null) {
                return tile.getSlope(xInTile, yInTile);
            } else {
                return 0.0f;
            }
        } else {
            // Spanning tiles; do it ourselves
            return Math.max(Math.max(Math.abs(getHeightAt(x + 1, y) - getHeightAt(x - 1, y)) / 2,
                Math.abs(getHeightAt(x + 1, y + 1) - getHeightAt(x - 1, y - 1)) / ROOT_EIGHT),
                Math.max(Math.abs(getHeightAt(x, y + 1) - getHeightAt(x, y - 1)) / 2,
                Math.abs(getHeightAt(x - 1, y + 1) - getHeightAt(x + 1, y - 1)) / ROOT_EIGHT));
        }
    }

    public Terrain getTerrainAt(int x, int y) {
        Tile tile = getTile(x >> TILE_SIZE_BITS, y >> TILE_SIZE_BITS);
        if (tile != null) {
            return tile.getTerrain(x & TILE_SIZE_MASK, y & TILE_SIZE_MASK);
        } else {
            return null;
        }
    }

    public void setTerrainAt(int x, int y, Terrain terrain) {
        Tile tile = getTileForEditing(x >> TILE_SIZE_BITS, y >> TILE_SIZE_BITS);
        if (tile != null) {
            tile.setTerrain(x & TILE_SIZE_MASK, y & TILE_SIZE_MASK, terrain);
        }
    }

    public Set<Terrain> getAllTerrains() {
        // This method is called during deserialization of World2, and if that is being deserialized because _this_
        // dimension refers to it, tiles is null at this point:
        return (tiles != null) ? tiles.values().parallelStream().flatMap(tile -> tile.getAllTerrains().parallelStream()).collect(toSet()) : emptySet();
    }

    public void setTerrainAt(Point coords, Terrain terrain) {
        setTerrainAt(coords.x, coords.y, terrain);
    }

    public void applyTheme(int x, int y) {
        Tile tile = getTileForEditing(x >> TILE_SIZE_BITS, y >> TILE_SIZE_BITS);
        if (tile != null) {
            tileFactory.applyTheme(tile, x & TILE_SIZE_MASK, y & TILE_SIZE_MASK);
        }
    }

    public int getWaterLevelAt(int x, int y) {
        Tile tile = getTile(x >> TILE_SIZE_BITS, y >> TILE_SIZE_BITS);
        if (tile != null) {
            return tile.getWaterLevel(x & TILE_SIZE_MASK, y & TILE_SIZE_MASK);
        } else {
            return Integer.MIN_VALUE;
        }
    }

    public int getWaterLevelAt(Point coords) {
        return getWaterLevelAt(coords.x, coords.y);
    }

    public void setWaterLevelAt(int x, int y, int waterLevel) {
        Tile tile = getTileForEditing(x >> TILE_SIZE_BITS, y >> TILE_SIZE_BITS);
        if (tile != null) {
            tile.setWaterLevel(x & TILE_SIZE_MASK, y & TILE_SIZE_MASK, waterLevel);
        }
    }

    public int getLayerValueAt(Layer layer, int x, int y) {
        Tile tile = getTile(x >> TILE_SIZE_BITS, y >> TILE_SIZE_BITS);
        if (tile != null) {
            return tile.getLayerValue(layer, x & TILE_SIZE_MASK, y & TILE_SIZE_MASK);
        } else {
            return layer.getDefaultValue();
        }
    }

    public int getLayerValueAt(Layer layer, Point coords) {
        return getLayerValueAt(layer, coords.x, coords.y);
    }

    public void setLayerValueAt(Layer layer, int x, int y, int value) {
        Tile tile = getTileForEditing(x >> TILE_SIZE_BITS, y >> TILE_SIZE_BITS);
        if (tile != null) {
            tile.setLayerValue(layer, x & TILE_SIZE_MASK, y & TILE_SIZE_MASK, value);
        }
    }

    public boolean getBitLayerValueAt(Layer layer, int x, int y) {
        Tile tile = getTile(x >> TILE_SIZE_BITS, y >> TILE_SIZE_BITS);
        if (tile != null) {
            return tile.getBitLayerValue(layer, x & TILE_SIZE_MASK, y & TILE_SIZE_MASK);
        } else {
            return false;
        }
    }


    /**
     * Count the number of blocks where the specified bit layer is set in a
     * square around a particular location
     *
     * @param layer The bit layer to count.
     * @param x The global X coordinate of the location around which to count
     *     the layer.
     * @param y The global Y coordinate of the location around which to count
     *     the layer.
     * @param r The radius of the square.
     * @return The number of blocks in the specified square where the specified
     *     bit layer is set.
     */
    public synchronized int getBitLayerCount(final Layer layer, final int x, final int y, final int r) {
        final int tileX = x >> TILE_SIZE_BITS, tileY = y >> TILE_SIZE_BITS;
        if (((x - r) >> TILE_SIZE_BITS == tileX) && ((x + r) >> TILE_SIZE_BITS == tileX) && ((y - r) >> TILE_SIZE_BITS == tileY) && ((y + r) >> TILE_SIZE_BITS == tileY)) {
            // The requested area is completely contained in one tile, optimise
            // by delegating to the tile
            final Tile tile = getTile(tileX, tileY);
            if (tile != null) {
                return tile.getBitLayerCount(layer, x & TILE_SIZE_MASK, y & TILE_SIZE_MASK, r);
            } else {
                return 0;
            }
        } else {
            // The requested area overlaps tile boundaries; do it the slow way
            int count = 0;
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    if (getBitLayerValueAt(layer, x + dx, y + dy)) {
                        count++;
                    }
                }
            }
            return count;
        }
    }

    /**
     * Gets all layers that are set at the specified location, along with their
     * intensities. For bit valued layers the intensity is zero for off, one for
     * on.
     *
     * @param x The X location for which to retrieve all layers.
     * @param y The Y location for which to retrieve all layers.
     * @return A map with all layers set at the specified location, mapped to
     *     their intensities at that location. May either be {@code null}
     *     or an empty map if no layers are present.
     */
    public Map<Layer, Integer> getLayersAt(int x, int y) {
        Tile tile = getTile(x >> TILE_SIZE_BITS, y >> TILE_SIZE_BITS);
        if (tile != null) {
            return tile.getLayersAt(x & TILE_SIZE_MASK, y & TILE_SIZE_MASK);
        } else {
            return null;
        }
    }

    /**
     * Count the number of blocks that are flooded in a square around a
     * particular location
     *
     * @param x The global X coordinate of the location around which to count
     *     flooded blocks.
     * @param y The global Y coordinate of the location around which to count
     *     flooded blocks.
     * @param r The radius of the square.
     * @param lava Whether to check for lava (when {@code true}) or water
     *     (when {@code false}).
     * @return The number of blocks in the specified square that are flooded.
     */
    public synchronized int getFloodedCount(final int x, final int y, final int r, final boolean lava) {
        final int tileX = x >> TILE_SIZE_BITS, tileY = y >> TILE_SIZE_BITS;
        if (((x - r) >> TILE_SIZE_BITS == tileX) && ((x + r) >> TILE_SIZE_BITS == tileX) && ((y - r) >> TILE_SIZE_BITS == tileY) && ((y + r) >> TILE_SIZE_BITS == tileY)) {
            // The requested area is completely contained in one tile, optimise
            // by delegating to the tile
            final Tile tile = getTile(tileX, tileY);
            if (tile != null) {
                return tile.getFloodedCount(x & TILE_SIZE_MASK, y & TILE_SIZE_MASK, r, lava);
            } else {
                return 0;
            }
        } else {
            // The requested area overlaps tile boundaries; do it the slow way
            int count = 0;
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    final int xx = x + dx, yy = y + dy;
                    if ((getWaterLevelAt(xx, yy) > getIntHeightAt(xx, yy))
                            && (lava ? getBitLayerValueAt(FloodWithLava.INSTANCE, xx, yy)
                                : (! getBitLayerValueAt(FloodWithLava.INSTANCE, xx, yy)))) {
                        count++;
                    }
                }
            }
            return count;
        }
    }

    /**
     * Get the distance from the specified coordinate to the nearest pixel where
     * the specified layer is <em>not</em> set.
     *
     * @param layer The layer for which to find the distance to the nearest
     *              edge.
     * @param x The X coordinate of the location towards which to determine the distance.
     * @param y The Y coordinate of the location towards which to determine the distance.
     * @param maxDistance The maximum distance to return. If the actual distance is further, this value will be returned.
     * @return The distance from the specified location to the nearest pixel
     *     where the specified layer is not set, or maxDistance, whichever is
     *     smaller. If the layer is not set at the specified coordinates, 0 is
     *     returned.
     */
    public synchronized float getDistanceToEdge(final Layer layer, final int x, final int y, final float maxDistance) {
        final int r = (int) Math.ceil(maxDistance);
        final int tileX = x >> TILE_SIZE_BITS, tileY = y >> TILE_SIZE_BITS;
        if (((x - r) >> TILE_SIZE_BITS == tileX) && ((x + r) >> TILE_SIZE_BITS == tileX) && ((y - r) >> TILE_SIZE_BITS == tileY) && ((y + r) >> TILE_SIZE_BITS == tileY)) {
            // The requested area is completely contained in one tile, optimise
            // by delegating to the tile
            final Tile tile = getTile(tileX, tileY);
            if (tile != null) {
                return tile.getDistanceToEdge(layer, x & TILE_SIZE_MASK, y & TILE_SIZE_MASK, maxDistance);
            } else {
                return 0;
            }
        } else {
            if (! getBitLayerValueAt(layer, x, y)) {
                return 0;
            }
            float distance = maxDistance;
            for (int i = 1; i <= r; i++) {
                if (((! getBitLayerValueAt(layer, x - i, y))
                            || (! getBitLayerValueAt(layer, x + i, y))
                            || (! getBitLayerValueAt(layer, x, y - i))
                            || (! getBitLayerValueAt(layer, x, y + i)))
                        && (i < distance)) {
                    // If we get here there's no possible way a shorter
                    // distance could be found later, so return immediately
                    return i;
                }
                for (int d = 1; d <= i; d++) {
                    if ((! getBitLayerValueAt(layer, x - i, y - d))
                            || (! getBitLayerValueAt(layer, x + d, y - i))
                            || (! getBitLayerValueAt(layer, x + i, y + d))
                            || (! getBitLayerValueAt(layer, x - d, y + i))
                            || ((d < i) && ((! getBitLayerValueAt(layer, x - i, y + d))
                                || (! getBitLayerValueAt(layer, x - d, y - i))
                                || (! getBitLayerValueAt(layer, x + i, y - d))
                                || (! getBitLayerValueAt(layer, x + d, y + i))))) {
                        float tDistance = MathUtils.getDistance(i, d);
                        if (tDistance < distance) {
                            distance = tDistance;
                        }
                        // We won't find a shorter distance this round, so
                        // skip to the next round
                        break;
                    }
                }
            }
            return distance;
        }
    }

    public void setBitLayerValueAt(Layer layer, int x, int y, boolean value) {
        Tile tile = getTileForEditing(x >> TILE_SIZE_BITS, y >> TILE_SIZE_BITS);
        if (tile != null) {
            tile.setBitLayerValue(layer, x & TILE_SIZE_MASK, y & TILE_SIZE_MASK, value);
        }
    }

    public void clearLayerData(Layer layer) {
        tiles.values().stream().filter(tile -> tile.hasLayer(layer)).forEach(tile -> {
            if (eventsInhibited && (!tile.isEventsInhibited())) {
                tile.inhibitEvents();
                dirtyTiles.add(tile);
            }
            tile.clearLayerData(layer);
        });
    }

    /**
     * Clear all layer data at a particular location (by resetting to the
     * layer's default value), possibly with the exception of certain layers.
     *
     * @param x The X coordinate of the location to clear of layer data.
     * @param y The Y coordinate of the location to clear of layer data.
     * @param excludedLayers The layers to exclude, if any. May be
     *                       {@code null}.
     */
    public void clearLayerData(int x, int y, Set<Layer> excludedLayers) {
        Tile tile = getTileForEditing(x >> TILE_SIZE_BITS, y >> TILE_SIZE_BITS);
        if (tile != null) {
            tile.clearLayerData(x & TILE_SIZE_MASK, y & TILE_SIZE_MASK, excludedLayers);
        }
    }

    public void setEventsInhibited(boolean eventsInhibited) {
        if (eventsInhibited != this.eventsInhibited) {
            this.eventsInhibited = eventsInhibited;
            if (eventsInhibited == false) {
                fireTilesAdded(addedTiles);
                addedTiles.clear();
                fireTilesRemoved(removedTiles);
                removedTiles.clear();
                dirtyTiles.forEach(org.pepsoft.worldpainter.Tile::releaseEvents);
                dirtyTiles.clear();
            }
        } else {
            throw new IllegalStateException("eventsInhibited already " + eventsInhibited);
        }
    }

    public boolean isEventsInhibited() {
        return eventsInhibited;
    }

    public Map<Layer, ExporterSettings> getAllLayerSettings() {
        return Collections.unmodifiableMap(layerSettings);
    }

    @SuppressWarnings("unchecked")
    public ExporterSettings getLayerSettings(Layer layer) {
        return layerSettings.get(layer);
    }

    public void setLayerSettings(Layer layer, ExporterSettings settings) {
        if ((settings != null) ? ((! layerSettings.containsKey(layer)) || (! settings.equals(layerSettings.get(layer)))) : layerSettings.containsKey(layer)) {
            if (settings != null) {
                layerSettings.put(layer, settings);
            } else {
                layerSettings.remove(layer);
            }
            changeNo++;
        }
    }

    public long getMinecraftSeed() {
        return minecraftSeed;
    }

    public void setMinecraftSeed(long minecraftSeed) {
        if (minecraftSeed != this.minecraftSeed) {
            long oldMinecraftSeed = this.minecraftSeed;
            this.minecraftSeed = minecraftSeed;
            changeNo++;
            propertyChangeSupport.firePropertyChange("minecraftSeed", oldMinecraftSeed, minecraftSeed);
        }
    }

    public File getOverlay() {
        return overlay;
    }

    public void setOverlay(File overlay) {
        if ((overlay != null) ? (! overlay.equals(this.overlay)) : (this.overlay == null)) {
            File oldOverlay = this.overlay;
            this.overlay = overlay;
            changeNo++;
            propertyChangeSupport.firePropertyChange("overlay", oldOverlay, overlay);
        }
    }

    public int getOverlayOffsetX() {
        return overlayOffsetX;
    }

    public void setOverlayOffsetX(int overlayOffsetX) {
        if (overlayOffsetX != this.overlayOffsetX) {
            int oldOverlayOffsetX = this.overlayOffsetX;
            this.overlayOffsetX = overlayOffsetX;
            changeNo++;
            propertyChangeSupport.firePropertyChange("overlayOffsetX", oldOverlayOffsetX, overlayOffsetX);
        }
    }

    public int getOverlayOffsetY() {
        return overlayOffsetY;
    }

    public void setOverlayOffsetY(int overlayOffsetY) {
        if (overlayOffsetY != this.overlayOffsetY) {
            int oldOverlayOffsetY = this.overlayOffsetY;
            this.overlayOffsetY = overlayOffsetY;
            changeNo++;
            propertyChangeSupport.firePropertyChange("overlayOffsetY", oldOverlayOffsetY, overlayOffsetY);
        }
    }

    public float getOverlayScale() {
        return overlayScale;
    }

    public void setOverlayScale(float overlayScale) {
        if (overlayScale != this.overlayScale) {
            float oldOverlayScale = this.overlayScale;
            this.overlayScale = overlayScale;
            changeNo++;
            propertyChangeSupport.firePropertyChange("overlayScale", oldOverlayScale, overlayScale);
        }
    }

    public float getOverlayTransparency() {
        return overlayTransparency;
    }

    public void setOverlayTransparency(float overlayTransparency) {
        if (overlayTransparency != this.overlayTransparency) {
            float oldOverlayTransparency = this.overlayTransparency;
            this.overlayTransparency = overlayTransparency;
            changeNo++;
            propertyChangeSupport.firePropertyChange("overlayTransparency", oldOverlayTransparency, overlayTransparency);
        }
    }

    public boolean isGridEnabled() {
        return gridEnabled;
    }

    public void setGridEnabled(boolean gridEnabled) {
        if (gridEnabled != this.gridEnabled) {
            this.gridEnabled = gridEnabled;
            changeNo++;
            propertyChangeSupport.firePropertyChange("gridEnabled", ! gridEnabled, gridEnabled);
        }
    }

    public int getGridSize() {
        return gridSize;
    }

    public void setGridSize(int gridSize) {
        if (gridSize != this.gridSize) {
            int oldGridSize = this.gridSize;
            this.gridSize = gridSize;
            changeNo++;
            propertyChangeSupport.firePropertyChange("gridSize", oldGridSize, gridSize);
        }
    }

    public boolean isOverlayEnabled() {
        return overlayEnabled;
    }

    public void setOverlayEnabled(boolean overlayEnabled) {
        if (overlayEnabled != this.overlayEnabled) {
            this.overlayEnabled = overlayEnabled;
            changeNo++;
            propertyChangeSupport.firePropertyChange("overlayEnabled", ! overlayEnabled, overlayEnabled);
        }
    }

    public int getMinHeight() {
        return minHeight;
    }

    public void setMinHeight(int minHeight) {
        if (minHeight != this.minHeight) {
            int oldMinHeight = this.minHeight;
            this.minHeight = minHeight;
            changeNo++;
            propertyChangeSupport.firePropertyChange("minHeight", oldMinHeight, minHeight);
        }
    }

    public int getMaxHeight() {
        return maxHeight;
    }

    public void setMaxHeight(int maxHeight) {
        if (maxHeight != this.maxHeight) {
            int oldMaxHeight = this.maxHeight;
            this.maxHeight = maxHeight;
            changeNo++;
            propertyChangeSupport.firePropertyChange("maxHeight", oldMaxHeight, maxHeight);
        }
    }

    public int getContourSeparation() {
        return contourSeparation;
    }

    public void setContourSeparation(int contourSeparation) {
        if (contourSeparation != this.contourSeparation) {
            int oldContourSeparation = this.contourSeparation;
            this.contourSeparation = contourSeparation;
            changeNo++;
            propertyChangeSupport.firePropertyChange("contourSeparation", oldContourSeparation, contourSeparation);
        }
    }

    public boolean isContoursEnabled() {
        return contoursEnabled;
    }

    public void setContoursEnabled(boolean contoursEnabled) {
        if (contoursEnabled != this.contoursEnabled) {
            this.contoursEnabled = contoursEnabled;
            changeNo++;
            propertyChangeSupport.firePropertyChange("contoursEnabled", ! contoursEnabled, contoursEnabled);
        }
    }

    public int getTopLayerMinDepth() {
        return topLayerMinDepth;
    }

    public void setTopLayerMinDepth(int topLayerMinDepth) {
        if (topLayerMinDepth != this.topLayerMinDepth) {
            int oldTopLayerMinDepth = this.topLayerMinDepth;
            this.topLayerMinDepth = topLayerMinDepth;
            changeNo++;
            propertyChangeSupport.firePropertyChange("topLayerMinDepth", oldTopLayerMinDepth, topLayerMinDepth);
        }
    }

    public int getTopLayerVariation() {
        return topLayerVariation;
    }

    public void setTopLayerVariation(int topLayerVariation) {
        if (topLayerVariation != this.topLayerVariation) {
            int oldTopLayerVariation = this.topLayerVariation;
            this.topLayerVariation = topLayerVariation;
            changeNo++;
            propertyChangeSupport.firePropertyChange("topLayerVariation", oldTopLayerVariation, topLayerVariation);
        }
    }

    public boolean isBottomless() {
        return bottomless;
    }

    public void setBottomless(boolean bottomless) {
        if (bottomless != this.bottomless) {
            this.bottomless = bottomless;
            changeNo++;
            propertyChangeSupport.firePropertyChange("bottomless", ! bottomless, bottomless);
        }
    }

    public Point getLastViewPosition() {
        return lastViewPosition;
    }

    public void setLastViewPosition(Point lastViewPosition) {
        if (lastViewPosition == null) {
            throw new NullPointerException();
        }
        if (! lastViewPosition.equals(this.lastViewPosition)) {
            Point oldLastViewPosition = this.lastViewPosition;
            this.lastViewPosition = lastViewPosition;
            // Don't mark dirty just for changing the view position
            propertyChangeSupport.firePropertyChange("lastViewPosition", oldLastViewPosition, lastViewPosition);
        }
    }

    public List<CustomBiome> getCustomBiomes() {
        return customBiomes;
    }

    public void setCustomBiomes(List<CustomBiome> customBiomes) {
        if ((customBiomes != null) ? (! customBiomes.equals(this.customBiomes)) : (this.customBiomes != null)) {
            List<CustomBiome> oldCustomBiomes = this.customBiomes;
            this.customBiomes = customBiomes;
            changeNo++;
            propertyChangeSupport.firePropertyChange("customBiomes", oldCustomBiomes, customBiomes);
        }
    }

    public boolean isCoverSteepTerrain() {
        return coverSteepTerrain;
    }

    public void setCoverSteepTerrain(boolean coverSteepTerrain) {
        if (coverSteepTerrain != this.coverSteepTerrain) {
            this.coverSteepTerrain = coverSteepTerrain;
            changeNo++;
            propertyChangeSupport.firePropertyChange("coverSteepTerrain", ! coverSteepTerrain, coverSteepTerrain);
        }
    }

    public boolean isFixOverlayCoords() {
        return fixOverlayCoords;
    }

    public void setFixOverlayCoords(boolean fixOverlayCoords) {
        this.fixOverlayCoords = fixOverlayCoords;
    }

    public Garden getGarden() {
        return garden;
    }

    public List<CustomLayer> getCustomLayers() {
        return customLayers;
    }

    public void setCustomLayers(List<CustomLayer> customLayers) {
        this.customLayers = customLayers;
    }

    /**
     * Returns the set of all layers currently in use on the world, optionally
     * including layers that are included in combined layers.
     *
     * @param applyCombinedLayers Whether to include layers from combined layers
     *     which are not used independently in the dimension.
     * @return The set of all layers currently in use on the world.
     */
    public Set<Layer> getAllLayers(boolean applyCombinedLayers) {
        Set<Layer> allLayers = new HashSet<>();
        for (Tile tile: tiles.values()) {
            allLayers.addAll(tile.getLayers());
        }

        if (applyCombinedLayers) {
            Set<LayerContainer> containersProcessed = new HashSet<>();
            boolean containersFound;
            do {
                containersFound = false;
                for (Layer layer: new HashSet<>(allLayers)) {
                    if ((layer instanceof LayerContainer) && (! containersProcessed.contains(layer))) {
                        allLayers.addAll(((LayerContainer) layer).getLayers());
                        containersProcessed.add((LayerContainer) layer);
                        containersFound = true;
                    }
                }
            } while (containersFound);
        }

        return allLayers;
    }

    /**
     * Get the set of layers that has been configured to be applied everywhere.
     *
     * @return The set of layers that has been configured to be applied
     *     everywhere.
     */
    public Set<Layer> getMinimumLayers() {
        Set<Layer> layers = layerSettings.values().stream().filter(ExporterSettings::isApplyEverywhere).map(ExporterSettings::getLayer).collect(toSet());
        return layers;
    }

    public int getCeilingHeight() {
        return ceilingHeight;
    }

    public void setCeilingHeight(int ceilingHeight) {
        if (ceilingHeight != this.ceilingHeight) {
            int oldCeilingHeight = this.ceilingHeight;
            this.ceilingHeight = ceilingHeight;
            changeNo++;
            propertyChangeSupport.firePropertyChange("ceilingHeight", oldCeilingHeight, ceilingHeight);
        }
    }

    public LayerAnchor getSubsurfaceLayerAnchor() {
        return subsurfaceLayerAnchor;
    }

    public void setSubsurfaceLayerAnchor(LayerAnchor subsurfaceLayerAnchor) {
        if (subsurfaceLayerAnchor != this.subsurfaceLayerAnchor) {
            LayerAnchor oldSubsurfaceLayerAnchor = this.subsurfaceLayerAnchor;
            this.subsurfaceLayerAnchor = subsurfaceLayerAnchor;
            changeNo++;
            propertyChangeSupport.firePropertyChange("subsurfaceLayerAnchor", oldSubsurfaceLayerAnchor, subsurfaceLayerAnchor);
        }
    }

    public LayerAnchor getTopLayerAnchor() {
        return topLayerAnchor;
    }

    public void setTopLayerAnchor(LayerAnchor topLayerAnchor) {
        if (topLayerAnchor != this.topLayerAnchor) {
            LayerAnchor oldTopLayerAnchor = this.topLayerAnchor;
            this.topLayerAnchor = topLayerAnchor;
            changeNo++;
            propertyChangeSupport.firePropertyChange("topLayerAnchor", oldTopLayerAnchor, topLayerAnchor);
        }
    }

    public ExportSettings getExportSettings() {
        return exportSettings;
    }

    public void setExportSettings(ExportSettings exportSettings) {
        if ((this.exportSettings == null) ? (exportSettings != null) : (! exportSettings.equals(this.exportSettings))) {
            ExportSettings oldExportSettings = this.exportSettings;
            this.exportSettings = exportSettings;
            changeNo++;
            propertyChangeSupport.firePropertyChange("exportSettings", oldExportSettings, exportSettings);
        }
    }

    public MapGenerator getGenerator() {
        return generator;
    }

    public void setGenerator(MapGenerator generator) {
        if (propertyChangeSupport != null) {
            if (! Objects.equals(this.generator, generator)) {
                MapGenerator oldGenerator = this.generator;
                this.generator = generator;
                changeNo++;
                propertyChangeSupport.firePropertyChange("generator", oldGenerator, generator);
            }
        } else {
            // This is also called during deserialisation of Configuration, when property change support is not yet
            // activated
            this.generator = generator;
        }
    }

    public void applyTheme(Point coords) {
        applyTheme(coords.x, coords.y);
    }

    public boolean isUndoAvailable() {
        return undoManager != null;
    }

    public void register(UndoManager undoManager) {
        this.undoManager = undoManager;
        for (Tile tile: tiles.values()) {
            tile.register(undoManager);
        }
//        garden.register(undoManager);
    }

    public boolean undoChanges() {
        if (rememberedChangeNo != -1) {
            changeNo = rememberedChangeNo;
            rememberedChangeNo = -1;
        }
        if ((undoManager != null) && undoManager.isDirty()) {
            return undoManager.undo();
        } else {
            return false;
        }
    }

    public void clearUndo() {
        rememberedChangeNo = -1;
        if (undoManager != null) {
            undoManager.clear();
        }
    }

    public void armSavePoint() {
        if (undoManager != null) {
            undoManager.armSavePoint();
        }
    }

    public void rememberChanges() {
        rememberedChangeNo = changeNo;
        if (undoManager != null) {
            if (undoManager.isDirty()) {
                undoManager.savePoint();
            } else {
                undoManager.armSavePoint();
            }
        }
    }

    public void clearRedo() {
        if (undoManager != null) {
            undoManager.clearRedo();
        }
    }

    public void unregister() {
        for (Tile tile: tiles.values()) {
            tile.removeListener(this);
            tile.unregister();
        }
        undoManager = null;
    }

    public final int getAutoBiome(int x, int y) {
        switch (dim) {
            case DIM_NETHER:
            case DIM_NETHER_CEILING:
                return BIOME_HELL;
            case DIM_END:
            case DIM_END_CEILING:
                return BIOME_SKY;
            default:
                Tile tile = getTile(x >> TILE_SIZE_BITS, y >> TILE_SIZE_BITS);
                if (tile != null) {
                    return getAutoBiome(tile, x & TILE_SIZE_MASK, y & TILE_SIZE_MASK);
                } else {
                    return -1;
                }
        }
    }

    public final int getAutoBiome(Tile tile, int x, int y) {
        // TODO add platform support and Minecraft 1.18 biomes
        switch (dim) {
            case DIM_NETHER:
            case DIM_NETHER_CEILING:
                return BIOME_HELL;
            case DIM_END:
            case DIM_END_CEILING:
                return BIOME_SKY;
            default:
                int biome;
                if (tile.getBitLayerValue(Frost.INSTANCE, x, y)) {
                    if (tile.getBitLayerValue(River.INSTANCE, x, y)) {
                        biome = BIOME_FROZEN_RIVER;
                    } else if ((tile.getLayerValue(DeciduousForest.INSTANCE, x, y) > 0)
                            || (tile.getLayerValue(PineForest.INSTANCE, x, y) > 0)
                            || (tile.getLayerValue(SwampLand.INSTANCE, x, y) > 0)
                            || (tile.getLayerValue(Jungle.INSTANCE, x, y) > 0)) {
                        biome = BIOME_COLD_TAIGA;
                    } else if (tile.getTerrain(x, y) == Terrain.WATER) {
                        biome = BIOME_FROZEN_RIVER;
                    } else {
                        int waterLevel = tile.getWaterLevel(x, y) - tile.getIntHeight(x, y);
                        if ((waterLevel > 0) && (!tile.getBitLayerValue(FloodWithLava.INSTANCE, x, y))) {
                            if (waterLevel <= 5) {
                                biome = BIOME_FROZEN_RIVER;
                            } else {
                                biome = BIOME_FROZEN_OCEAN;
                            }
                        } else {
                            biome = BIOME_ICE_PLAINS;
                        }
                    }
                } else {
                    if (tile.getBitLayerValue(River.INSTANCE, x, y)) {
                        biome = BIOME_RIVER;
                    } else if (tile.getLayerValue(SwampLand.INSTANCE, x, y) > 0) {
                        biome = BIOME_SWAMPLAND;
                    } else if (tile.getLayerValue(Jungle.INSTANCE, x, y) > 0) {
                        biome = BIOME_JUNGLE;
                    } else {
                        int waterLevel = tile.getWaterLevel(x, y) - tile.getIntHeight(x, y);
                        if ((waterLevel > 0) && (!tile.getBitLayerValue(FloodWithLava.INSTANCE, x, y))) {
                            if (waterLevel <= 5) {
                                biome = BIOME_RIVER;
                            } else if (waterLevel <= 20) {
                                biome = BIOME_OCEAN;
                            } else {
                                biome = BIOME_DEEP_OCEAN;
                            }
                        } else {
                            final Terrain terrain = tile.getTerrain(x, y);
                            // TODO: we have reports from the wild of the custom terrain
                            //  returned here somehow not being configured, so check that
                            //  even though we don't understand how that could happen
                            final int defaultBiome = terrain.isConfigured() ? terrain.getDefaultBiome() : BIOME_PLAINS;
                            if (((tile.getLayerValue(DeciduousForest.INSTANCE, x, y) > 0)
                                    || (tile.getLayerValue(PineForest.INSTANCE, x, y) > 0))
                                    && (defaultBiome != BIOME_DESERT)
                                    && (defaultBiome != BIOME_DESERT_HILLS)
                                    && (defaultBiome != BIOME_DESERT_M)
                                    && (defaultBiome != BIOME_MESA)
                                    && (defaultBiome != BIOME_MESA_BRYCE)
                                    && (defaultBiome != BIOME_MESA_PLATEAU)
                                    && (defaultBiome != BIOME_MESA_PLATEAU_F)
                                    && (defaultBiome != BIOME_MESA_PLATEAU_F_M)
                                    && (defaultBiome != BIOME_MESA_PLATEAU_M)) {
                                biome = BIOME_FOREST;
                            } else {
                                biome = defaultBiome;
                            }
                        }
                    }
                }
                return biome;
        }
    }

    /**
     * Get a snapshot of the current state of this dimension. If you want this
     * snapshot to be truly static, you must execute a savepoint on the undo
     * manager after this.
     *
     * @return A snapshot of the current state of this dimension.
     */
    public Dimension getSnapshot() {
        if (undoManager == null) {
            throw new IllegalStateException("No undo manager installed");
        }
        return new DimensionSnapshot(this, undoManager.getSnapshot());
    }

    public int getTopLayerDepth(int x, int y, int z) {
        return topLayerMinDepth + Math.round((topLayerDepthNoise.getPerlinNoise(x / SMALL_BLOBS, y / SMALL_BLOBS, z / SMALL_BLOBS) + 0.5f) * topLayerVariation);
    }

    void ensureAllReadable() {
        tiles.values().forEach(org.pepsoft.worldpainter.Tile::ensureAllReadable);
    }

    public void addDimensionListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeDimensionListener(Listener listener) {
        listeners.remove(listener);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(propertyName, listener);
    }

    /**
     * Transform the tiles of this dimension horizontally, for instance
     * translating and/or rotating them. If an undo manager is installed this
     * operation will destroy all undo info.
     *
     * @param transform The transform to apply to the tiles and their contents.
     * @param progressReceiver An optional progress receiver to which progress
     *                         of the operation will be reported.
     * @throws OperationCancelled If the progress receiver threw an
     * {@code OperationCancelled} exception indicating that the user wished
     * to cancel the operation.
     */
    public void transform(CoordinateTransform transform, ProgressReceiver progressReceiver) throws OperationCancelled {
        if (progressReceiver != null) {
            progressReceiver.setMessage("transforming " + getName() + "...");
        }
        eventsInhibited = true;
        try {
            addedTiles.clear();
            removedTiles.clear();
            dirtyTiles.clear();
            changeNo++;

            Rectangle overlayCoords = null;
            if ((overlay != null) && overlay.canRead()) {
                try {
                    java.awt.Dimension overlaySize = getImageSize(overlay);
                    overlayCoords = new Rectangle(overlayOffsetX + (lowestX << TILE_SIZE_BITS), overlayOffsetY + (lowestY << TILE_SIZE_BITS), Math.round(overlaySize.width * overlayScale), Math.round(overlaySize.height * overlayScale));
                } catch (IOException e) {
                    // Don't bother user with it, just clear the overlay
                    logger.error("I/O error while trying to determine size of " + overlay, e);
                    overlay = null;
                    overlayEnabled = false;
                    overlayOffsetX = 0;
                    overlayOffsetY = 0;
                    overlayScale = 1.0f;
                }
            } else {
                overlay = null;
                overlayEnabled = false;
                overlayOffsetX = 0;
                overlayOffsetY = 0;
                overlayScale = 1.0f;
            }

            // Remove all tiles
            Set<Tile> removedTiles;
            synchronized (this) {
                Map<Point, Tile> oldTiles = tiles;
                tiles = new HashMap<>();
                removedTiles = new HashSet<>(oldTiles.values());
                for (Tile removedTile: removedTiles) {
                    removedTile.removeListener(this);
                    removedTile.unregister();
                }
            }
            clearUndo();
            for (Listener listener: listeners) {
                listener.tilesRemoved(this, removedTiles);
            }

            // Add them all back, in their transformed locations
            lowestX = Integer.MAX_VALUE;
            highestX = Integer.MIN_VALUE;
            lowestY = Integer.MAX_VALUE;
            highestY = Integer.MIN_VALUE;
            int tileCount = removedTiles.size(), tileNo = 0;
            for (Iterator<Tile> i = removedTiles.iterator(); i.hasNext(); ) {
                Tile tile = i.next();
                addTile(tile.transform(transform));
                i.remove(); // Remove each tile as we're done with it so it can be garbage collected
                tileNo++;
                if (progressReceiver != null) {
                    progressReceiver.setProgress((float) tileNo / tileCount);
                }
            }

            if (overlayCoords != null) {
                overlayCoords = transform.transform(overlayCoords);
                overlayOffsetX = overlayCoords.x - (lowestX << TILE_SIZE_BITS);
                overlayOffsetY = overlayCoords.y - (lowestY << TILE_SIZE_BITS);
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "The " + getName() + " dimension has an overlay image!\n"
                    + "The coordinates have been adjusted for you,\n"
                    + "but you need to rotate the actual image yourself\n"
                    + "using a paint program.", "Adjust Overlay Image", JOptionPane.INFORMATION_MESSAGE));
            }
        } finally {
            eventsInhibited = false;
            fireTilesAdded(addedTiles);
        }
    }

    public boolean containsOneOf(Layer... layers) {
        for (Tile tile: tiles.values()) {
            if (tile.containsOneOf(layers)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Visit the tiles in this dimension, optionally constraining the visited
     * tiles according to some basic constraints. Uses the builder pattern. To
     * visit all tiles, do:
     *
     * <pre>visitTiles().andDo(tile -> { /* process tile &#42;/ });</pre>
     *
     * <p>To restrict to a filter:
     *
     * <pre>visitTiles().forFilter(filter).andDo(tile -> { /* process tile &#42;/ });</pre>
     *
     * <p>To restrict to a brush:
     *
     * <pre>visitTiles().forBrush(brush, x, y).andDo(tile -> { /* process tile &#42;/ });</pre>
     *
     * <p>To restrict to the selection:
     *
     * <pre>visitTiles().forSelection().andDo(tile -> { /* process tile &#42;/ });</pre>
     *
     * <p>You can combine constraints:
     *
     * <pre>visitTiles().forFilter(filter).forBrush(brush, x, y).andDo(tile -> { /* process tile &#42;/ });</pre>
     *
     * @return
     */
    public TileVisitationBuilder visitTiles() {
        return new TileVisitationBuilder();
    }

    public <T> T getAttribute(AttributeKey<T> key) {
        if (attributes != null) {
            return attributes.containsKey(key.key) ? (T) attributes.get(key.key) : key.defaultValue;
        } else {
            return key.defaultValue;
        }
    }

    public <T> void setAttribute(AttributeKey<T> key, T value) {
        if ((value != null) ? value.equals(key.defaultValue) : (key.defaultValue == null)) {
            // Setting value to default
            attributes.remove(key.key);
            if (attributes.isEmpty()) {
                attributes = null;
            }
        } else {
            if (attributes == null) {
                attributes = new HashMap<>();
            }
            attributes.put(key.key, value);
        }
        changeNo++;
    }

    /**
     * Get the most prevalent biome in a 4x4 area.
     *
     * @param x X coordinate of the 4x4 area (in 4x4 areas).
     * @param y Y coordinate of the 4x4 area (in 4x4 areas).
     * @param defaultBiome The biome to return for areas where the biome is not defined.
     */
    public int getMostPrevalentBiome(final int x, final int y, final int defaultBiome) {
        final Tile tile = getTile(x >> 5, y >> 5);
        if (tile == null) {
            return defaultBiome;
        }
        final int xx1 = (x << 2) & TILE_SIZE_MASK, yy1 = (y << 2) & TILE_SIZE_MASK, xx2 = xx1 + 4, yy2 = yy1 + 4;
        final int[] histogram = biomeHistogramRef.get();
        Arrays.fill(histogram, 0);
        for (int xx = xx1; xx < xx2; xx++) {
            for (int yy = yy1; yy < yy2; yy++) {
                int biome = tile.getLayerValue(Biome.INSTANCE, xx, yy);
                if (biome == 255) {
                    biome = getAutoBiome(tile, xx, yy);
                    if ((biome < 0) || (biome > 254)) {
                        biome = defaultBiome;
                    }
                }
                histogram[biome]++;
                if (histogram[biome] > 7) {
                    // This biome will win or tie; choose it
                    return biome;
                }
            }
        }
        int mostPrevalentBiome = -1, mostPrevalentBiomePrevalence = -1;
        for (int i = 0; i < 255; i++) {
            if (histogram[i] > mostPrevalentBiomePrevalence) {
                mostPrevalentBiome = i;
                mostPrevalentBiomePrevalence = histogram[i];
            }
        }
        return mostPrevalentBiome;
    }

    // Tile.Listener

    @Override
    public void heightMapChanged(Tile tile) {
        changeNo++;
    }

    @Override
    public void terrainChanged(Tile tile) {
        changeNo++;
    }

    @Override
    public void waterLevelChanged(Tile tile) {
        changeNo++;
    }

    @Override
    public void seedsChanged(Tile tile) {
        changeNo++;
    }

    @Override
    public void layerDataChanged(Tile tile, Set<Layer> changedLayers) {
        changeNo++;
    }

    @Override
    public void allBitLayerDataChanged(Tile tile) {
        changeNo++;
    }

    @Override
    public void allNonBitlayerDataChanged(Tile tile) {
        changeNo++;
    }

    private void fireTileAdded(Tile tile) {
        if (eventsInhibited) {
            addedTiles.add(tile);
        } else {
            Set<Tile> tiles = Collections.singleton(tile);
            for (Listener listener: listeners) {
                listener.tilesAdded(this, tiles);
            }
        }
    }

    private void fireTileRemoved(Tile tile) {
        if (eventsInhibited) {
            removedTiles.add(tile);
        } else {
            Set<Tile> tiles = Collections.singleton(tile);
            for (Listener listener: listeners) {
                listener.tilesRemoved(this, tiles);
            }
        }
    }

    private void fireTilesAdded(Set<Tile> tiles) {
        if (eventsInhibited) {
            addedTiles.addAll(tiles);
        } else {
            for (Listener listener: listeners) {
                listener.tilesAdded(this, tiles);
            }
        }
    }

    private void fireTilesRemoved(Set<Tile> tiles) {
        if (eventsInhibited) {
            removedTiles.addAll(tiles);
        } else {
            for (Listener listener: listeners) {
                listener.tilesRemoved(this, tiles);
            }
        }
    }

    private java.awt.Dimension getImageSize(File image) throws IOException {
        String filename = image.getName();
        int p = filename.lastIndexOf('.');
        if (p == -1) {
            return null;
        }
        String suffix = filename.substring(p + 1).toLowerCase();
        Iterator<ImageReader> readers = ImageIO.getImageReadersBySuffix(suffix);
        if (readers.hasNext()) {
            ImageReader reader = readers.next();
            try {
                try (ImageInputStream in = new FileImageInputStream(image)) {
                    reader.setInput(in);
                    int width = reader.getWidth(reader.getMinIndex());
                    int height = reader.getHeight(reader.getMinIndex());
                    return new java.awt.Dimension(width, height);
                }
            } finally {
                reader.dispose();
            }
        } else {
            return null;
        }
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        listeners = new ArrayList<>();
        dirtyTiles = new HashSet<>();
        addedTiles = new HashSet<>();
        removedTiles = new HashSet<>();
        propertyChangeSupport = new PropertyChangeSupport(this);
        garden = new WPGarden();
        topLayerDepthNoise = new PerlinNoise(seed + TOP_LAYER_DEPTH_SEED_OFFSET);
        rememberedChangeNo = -1;
        biomeHistogramRef = ThreadLocal.withInitial(() -> new int[255]);

        for (Tile tile: tiles.values()) {
            tile.addListener(this);
            Set<Seed> seeds = tile.getSeeds();
            if (seeds != null) {
                for (Seed gardenSeed: seeds) {
                    gardenSeed.garden = garden;
                }
            }
        }

        // Legacy support
        if (wpVersion < 1) {
            if (borderSize == 0) {
                borderSize = 2;
            }
            if (overlayScale == 0.0f) {
                overlayScale = 1.0f;
            }
            if (overlayTransparency == 0.0f) {
                overlayTransparency = 0.5f;
            }
            if (gridSize == 0) {
                gridSize = 128;
            }
            if (! biomesConverted) {
                // Convert the nibble sized biomes data from a legacy map (by
                // deleting it so that it will be recalculated
                tiles.values().forEach(org.pepsoft.worldpainter.Tile::convertBiomeData);
                biomesConverted = true;
            }
            if (maxHeight == 0) {
                maxHeight = 128;
            }
            if (subsurfaceMaterial == Terrain.RESOURCES) {
                subsurfaceMaterial = Terrain.STONE;

                // Load legacy settings
                ResourcesExporterSettings settings = ResourcesExporterSettings.defaultSettings(JAVA_ANVIL, DIM_NORMAL, maxHeight);
                settings.setChance(GOLD_ORE,         1);
                settings.setChance(IRON_ORE,         5);
                settings.setChance(COAL,             9);
                settings.setChance(LAPIS_LAZULI_ORE, 1);
                settings.setChance(DIAMOND_ORE,      1);
                settings.setChance(REDSTONE_ORE,     6);
                settings.setChance(WATER,            1);
                settings.setChance(LAVA,             1);
                settings.setChance(DIRT,             9);
                settings.setChance(GRAVEL,           9);
                settings.setMaxLevel(GOLD_ORE,         Terrain.GOLD_LEVEL);
                settings.setMaxLevel(IRON_ORE,         Terrain.IRON_LEVEL);
                settings.setMaxLevel(COAL,             Terrain.COAL_LEVEL);
                settings.setMaxLevel(LAPIS_LAZULI_ORE, Terrain.LAPIS_LAZULI_LEVEL);
                settings.setMaxLevel(DIAMOND_ORE,      Terrain.DIAMOND_LEVEL);
                settings.setMaxLevel(REDSTONE_ORE,     Terrain.REDSTONE_LEVEL);
                settings.setMaxLevel(WATER,            Terrain.WATER_LEVEL);
                settings.setMaxLevel(LAVA,             Terrain.LAVA_LEVEL);
                settings.setMaxLevel(DIRT,             Terrain.DIRT_LEVEL);
                settings.setMaxLevel(GRAVEL,           Terrain.GRAVEL_LEVEL);

                layerSettings.put(Resources.INSTANCE, settings);
            }
            if (contourSeparation == 0) {
                contourSeparation = 10;
            }
            if (topLayerMinDepth == 0) {
                topLayerMinDepth = 3;
                topLayerVariation = 4;
            }
            if (lastViewPosition == null) {
                lastViewPosition = new Point();
            }
            if ((customLayers == null) || customLayers.isEmpty()) { // The customLayers.isEmpty() is to fix a bug which escaped in a beta
                customLayers = new ArrayList<>();
                customLayers.addAll(getAllLayers(false).stream().filter(layer -> layer instanceof CustomLayer).map(layer -> (CustomLayer) layer).collect(Collectors.toList()));
            }
        }
        if (wpVersion < 2) {
            if (overlay != null) {
                fixOverlayCoords = true;
            }
        }
        if (wpVersion < 3) {
            ceilingHeight = maxHeight;
        }
        if (wpVersion < 4) {
            subsurfaceLayerAnchor = LayerAnchor.BEDROCK;
            topLayerAnchor = LayerAnchor.BEDROCK;
        }
        wpVersion = CURRENT_WP_VERSION;

        // Make sure that any custom layers which somehow ended up in the world
        // are on the custom layer list so they will be added to a palette in
        // the GUI. TODO: fix this properly
        // Make sure customLayers isn't some weird read-only list
        getAllLayers(false).stream()
                .filter(layer -> (layer instanceof CustomLayer) && (! customLayers.contains(layer)))
                .forEach(layer -> {
                    if ((! (customLayers instanceof ArrayList)) && (! (customLayers instanceof LinkedList))) {
                        // Make sure customLayers isn't some weird read-only list
                        customLayers = new ArrayList<>(customLayers);
                    }
                    customLayers.add((CustomLayer) layer);
                });

        // Bug fix: fix the maxHeight of the dimension, which somehow is not
        // always correctly set (possibly only on imported worlds from
        // non-standard height maps due to a bug which should be fixed).
        if ((world != null) && (world.getMaxHeight() != 0) && (world.getMaxHeight() != maxHeight)) {
            logger.warn("Fixing maxHeight of dimension " + dim + " (was " + maxHeight + ", should be " + world.getMaxHeight() + ")");
            maxHeight = world.getMaxHeight();
        }
    }

    private final World2 world;
    private final long seed;
    private final int dim;
    private Map<Point, Tile> tiles = new HashMap<>();
    private final TileFactory tileFactory;
    private int lowestX = Integer.MAX_VALUE, highestX = Integer.MIN_VALUE, lowestY = Integer.MAX_VALUE, highestY = Integer.MIN_VALUE;
    private Terrain subsurfaceMaterial = Terrain.STONE_MIX;
    private boolean populate;
    private Border border;
    private int borderLevel = 62, borderSize = 2;
    private boolean darkLevel, bedrockWall;
    private Map<Layer, ExporterSettings> layerSettings = new HashMap<>();
    private long minecraftSeed;
    private File overlay;
    private float overlayScale = 1.0f, overlayTransparency = 0.5f;
    private int overlayOffsetX, overlayOffsetY, gridSize = 128;
    private boolean overlayEnabled, gridEnabled, biomesConverted = true;
    private int minHeight, maxHeight, contourSeparation = 10;
    private boolean contoursEnabled = true;
    private int topLayerMinDepth = 3, topLayerVariation = 4;
    private boolean bottomless;
    private Point lastViewPosition = new Point();
    private List<CustomBiome> customBiomes;
    private boolean coverSteepTerrain = true;
    private List<CustomLayer> customLayers = new ArrayList<>();
    private int wpVersion = CURRENT_WP_VERSION;
    private boolean fixOverlayCoords;
    private int ceilingHeight = maxHeight;
    private Map<String, Object> attributes;
    private LayerAnchor subsurfaceLayerAnchor = LayerAnchor.BEDROCK, topLayerAnchor = LayerAnchor.BEDROCK;
    private ExportSettings exportSettings;
    private MapGenerator generator;
    private transient List<Listener> listeners = new ArrayList<>();
    private transient boolean eventsInhibited;
    private transient Set<Tile> dirtyTiles = new HashSet<>();
    private transient Set<Tile> addedTiles = new HashSet<>();
    private transient Set<Tile> removedTiles = new HashSet<>();
    private transient UndoManager undoManager;
    private transient PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
    private transient WPGarden garden = new WPGarden();
    private transient PerlinNoise topLayerDepthNoise;
    private transient long changeNo, rememberedChangeNo = -1;
    private transient ThreadLocal<int[]> biomeHistogramRef = ThreadLocal.withInitial(() -> new int[255]);

    public static final int[] POSSIBLE_AUTO_BIOMES = {BIOME_PLAINS, BIOME_FOREST,
        BIOME_SWAMPLAND, BIOME_JUNGLE, BIOME_MESA, BIOME_DESERT, BIOME_BEACH,
        BIOME_RIVER, BIOME_OCEAN, BIOME_DEEP_OCEAN, BIOME_ICE_PLAINS,
        BIOME_COLD_TAIGA, BIOME_FROZEN_RIVER, BIOME_FROZEN_OCEAN,
        BIOME_MUSHROOM_ISLAND, BIOME_HELL, BIOME_SKY};

    private static final long TOP_LAYER_DEPTH_SEED_OFFSET = 180728193;
    private static final float ROOT_EIGHT = (float) Math.sqrt(8.0);
    private static final int CURRENT_WP_VERSION = 4;
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Dimension.class);
    private static final long serialVersionUID = 2011062401L;

    public interface Listener {
        void tilesAdded(Dimension dimension, Set<Tile> tiles);
        void tilesRemoved(Dimension dimension, Set<Tile> tiles);
    }

    public interface TileVisitor {
        void visit(Tile tile);
    }

    public enum Border {
        VOID(false), WATER(false), LAVA(false), ENDLESS_VOID(true), ENDLESS_WATER(true), ENDLESS_LAVA(true);

        Border(boolean endless) {
            this.endless = endless;
        }

        public boolean isEndless() {
            return endless;
        }

        private final boolean endless;
    }

    public enum LayerAnchor {BEDROCK, TERRAIN}

    public class TileVisitationBuilder {
        public TileVisitationBuilder forFilter(Filter filter) {
            this.filter = filter;
            return this;
        }

        public TileVisitationBuilder forBrush(Brush brush, int x, int y) {
            this.brush = brush;
            this.x = x;
            this.y = y;
            return this;
        }

        public TileVisitationBuilder forSelection() {
            selection = true;
            return this;
        }

        public void andDo(TileVisitor visitor) {
            try {
                andDo(visitor, null);
            } catch (OperationCancelled operationCancelled) {
                // Can't happen since we didn't pass in a progress receiver
                throw new InternalError();
            }
        }

        public void andDo(TileVisitor visitor, ProgressReceiver progressReceiver) throws OperationCancelled {
            if ((! selection) && (filter instanceof DefaultFilter) && ((DefaultFilter) filter).isInSelection()) {
                // The filter is set to "in selection", so we only need to process
                // the tiles intersecting the selection
                selection = true;
            }
            int totalTiles = tiles.size(), tileCount = 0;
            int tileX1 = Integer.MIN_VALUE, tileX2 = Integer.MAX_VALUE;
            int tileY1 = Integer.MIN_VALUE, tileY2 = Integer.MAX_VALUE;
            if (brush != null) {
                int effectiveRadius = brush.getEffectiveRadius();
                int x1 = x - effectiveRadius, x2 = x + effectiveRadius;
                int y1 = y - effectiveRadius, y2 = y + effectiveRadius;
                tileX1 = x1 >> TILE_SIZE_BITS;
                tileX2 = x2 >> TILE_SIZE_BITS;
                tileY1 = y1 >> TILE_SIZE_BITS;
                tileY2 = y2 >> TILE_SIZE_BITS;
            }
            for (Tile tile: tiles.values()) {
                int tileX = tile.getX(), tileY = tile.getY();
                if ((tileX >= tileX1) && (tileX <= tileX2) && (tileY >= tileY1) && (tileY <= tileY2)
                        && ((! selection) || (tile.containsOneOf(SelectionBlock.INSTANCE, SelectionChunk.INSTANCE)))) {
                    tile.inhibitEvents();
                    try {
                        visitor.visit(tile);
                    } finally {
                        tile.releaseEvents();
                    }
                }
                tileCount++;
                if (progressReceiver != null) {
                    progressReceiver.setProgress((float) tileCount / totalTiles);
                }
            }
        }

        private Filter filter;
        private Brush brush;
        private int x, y;
        private boolean selection;
    }

    private class WPGarden implements Garden {
        @Override
        public void clearLayer(int x, int y, Layer layer, int radius) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    setLayerValueAt(layer, x + dx, y + dy, 0);
                }
            }
        }

        @Override
        public void setCategory(int x, int y, int category) {
            setLayerValueAt(GardenCategory.INSTANCE, x, y, category);
        }

        @Override
        public int getCategory(int x, int y) {
            return getLayerValueAt(GardenCategory.INSTANCE, x, y);
        }

        @Override
        public Set<Seed> getSeeds() {
            Set<Seed> allSeeds = new HashSet<>();
            for (Tile tile: tiles.values()) {
                allSeeds.addAll(tile.getSeeds());
            }
            return allSeeds;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T extends Seed> List<T> findSeeds(Class<T> type, int x, int y, int radius) {
            List<T> seedsFound = new ArrayList<>();
            int topLeftTileX = (x - radius) >> 7;
            int topLeftTileY = (y - radius) >> 7;
            int bottomRightTileX = (x + radius) >> 7;
            int bottomRightTileY = (y + radius) >> 7;
//            System.out.println("Finding seeds of type " + type.getSimpleName() + " " + radius + " blocks around " + x + "," + y + " in " + ((bottomRightTileX - topLeftTileX + 1) * (bottomRightTileY - topLeftTileY + 1)) + " tiles");
            for (int tileX = topLeftTileX; tileX <= bottomRightTileX; tileX++) {
                for (int tileY = topLeftTileY; tileY <= bottomRightTileY; tileY++) {
                    Tile tile = getTile(tileX, tileY);
                    if (tile != null) {
                        Set<Seed> seeds = tile.getSeeds();
                        if (seeds != null) {
                            seeds.stream()
                                    .filter(seed -> seed.getClass() == type)
                                    .forEach(seed -> {
                                        int distance = (int) MathUtils.getDistance(seed.location.x - x, seed.location.y - y);
                                        if (distance <= radius) {
                                            seedsFound.add((T) seed);
                                        }
                                    });
                        }
                    }
                }
            }
            return seedsFound;
        }

        @Override
        public boolean isOccupied(int x, int y) {
            return (getLayerValueAt(GardenCategory.INSTANCE, x, y) != 0) || (getWaterLevelAt(x, y) > getIntHeightAt(x, y));
        }

        @Override
        public boolean isWater(int x, int y) {
            return (getLayerValueAt(GardenCategory.INSTANCE, x, y) == GardenCategory.CATEGORY_WATER) || ((getWaterLevelAt(x, y) > getIntHeightAt(x, y)) && (! getBitLayerValueAt(FloodWithLava.INSTANCE, x, y)));
        }

        @Override
        public boolean isLava(int x, int y) {
            return (getWaterLevelAt(x, y) > getIntHeightAt(x, y)) && getBitLayerValueAt(FloodWithLava.INSTANCE, x, y);
        }

        @Override
        public boolean plantSeed(Seed seed) {
            Point3i location = seed.getLocation();
            if ((location.x < lowestX * TILE_SIZE) || (location.x > (highestX + 1) * TILE_SIZE - 1) || (location.y < lowestY * TILE_SIZE) || (location.y > (highestY + 1) * TILE_SIZE - 1)) {
                return false;
            }
            Tile tile = getTileForEditing(location.x >> TILE_SIZE_BITS, location.y >> TILE_SIZE_BITS);
            if ((tile != null) && tile.plantSeed(seed)) {
                activeTiles.add(new Point(location.x >> TILE_SIZE_BITS, location.y >> TILE_SIZE_BITS));
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void removeSeed(Seed seed) {
            Point3i location = seed.getLocation();
            if ((location.x < lowestX * TILE_SIZE) || (location.x > (highestX + 1) * TILE_SIZE - 1) || (location.y < lowestY * TILE_SIZE) || (location.y > (highestY + 1) * TILE_SIZE - 1)) {
                return;
            }
            Tile tile = getTileForEditing(location.x >> 7, location.y >> 7);
            if (tile != null) {
                tile.removeSeed(seed);
            }
        }

        @Override
        public float getHeight(int x, int y) {
            return getHeightAt(x, y);
        }

        @Override
        public int getIntHeight(int x, int y) {
            return getIntHeightAt(x, y);
        }

        @Override
        @SuppressWarnings("unchecked") // Guaranteed by Java
        public boolean tick() {
            // Tick all seeds in active tiles. Clone the active tiles set, and
            // the seed sets from the tiles, because they may change out from
            // under us
            for (Point tileCoords: (HashSet<Point>) activeTiles.clone()) {
                Tile tile = getTile(tileCoords.x, tileCoords.y);
                if (tile != null) {
                    ((HashSet<Seed>) tile.getSeeds().clone())
                        .forEach(Seed::tick);
                }
            }
            // Don't cache active seeds, because they might have changed
            // Groom active tile list, and determine whether all seeds are
            // finished (have either sprouted or died)
            boolean finished = true;
            for (Iterator<Point> i = activeTiles.iterator(); i.hasNext(); ) {
                Point tileCoords = i.next();
                Tile tile = getTile(tileCoords.x, tileCoords.y);
                boolean tileFinished = true;
                if (tile != null) {
                    for (Seed seed: tile.getSeeds()) {
                        if (! seed.isFinished()) {
                            tileFinished = false;
                            break;
                        }
                    }
                }
                if (tileFinished) {
                    i.remove();
                } else {
                    finished = false;
                }
            }
            return finished;
        }

        @Override
        public void neutralise() {
            for (Point tileCoords: activeTiles) {
                Tile tile = getTile(tileCoords.x, tileCoords.y);
                if (tile != null) {
                    tile.getSeeds().stream()
                        .filter(seed -> !seed.isFinished())
                        .forEach(Seed::neutralise);
                }
            }
            activeTiles.clear();
        }

        private final HashSet<Point> activeTiles = new HashSet<>();
    }
}