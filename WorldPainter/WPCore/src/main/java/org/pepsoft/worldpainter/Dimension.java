/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter;

import org.jetbrains.annotations.NotNull;
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
import org.pepsoft.worldpainter.heightMaps.AbstractHeightMap;
import org.pepsoft.worldpainter.heightMaps.ConstantHeightMap;
import org.pepsoft.worldpainter.layers.*;
import org.pepsoft.worldpainter.layers.exporters.ExporterSettings;
import org.pepsoft.worldpainter.layers.exporters.ResourcesExporter.ResourcesExporterSettings;
import org.pepsoft.worldpainter.layers.tunnel.TunnelLayer;
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
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.util.Arrays.fill;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toSet;
import static org.pepsoft.minecraft.Constants.DEFAULT_WATER_LEVEL;
import static org.pepsoft.minecraft.Material.*;
import static org.pepsoft.util.CollectionUtils.copyOf;
import static org.pepsoft.worldpainter.Constants.*;
import static org.pepsoft.worldpainter.DefaultPlugin.JAVA_ANVIL;
import static org.pepsoft.worldpainter.Dimension.Anchor.*;
import static org.pepsoft.worldpainter.Dimension.Role.*;
import static org.pepsoft.worldpainter.Dimension.WallType.BEDROCK;
import static org.pepsoft.worldpainter.Generator.*;
import static org.pepsoft.worldpainter.biomeschemes.Minecraft1_21Biomes.*;
import static org.pepsoft.worldpainter.panels.DefaultFilter.buildForDimension;
import static org.pepsoft.worldpainter.util.GeometryUtil.getDistancesToCentre;

/**
 *
 * @author pepijn
 */
public class Dimension extends InstanceKeeper implements TileProvider, Serializable, Tile.Listener, Cloneable {
    public Dimension(World2 world, String name, long minecraftSeed, TileFactory tileFactory, Anchor anchor) {
        this(world, name, minecraftSeed, tileFactory, anchor, true);
    }

    public Dimension(World2 world, String name, long minecraftSeed, TileFactory tileFactory, Anchor anchor, boolean init) {
        if (world == null) {
            throw new NullPointerException("world");
        }
        this.world = world;
        this.seed = tileFactory.getSeed();
        this.name = name;
        this.minecraftSeed = minecraftSeed;
        this.tileFactory = tileFactory;
        this.anchor = anchor;
        this.minHeight = tileFactory.getMinHeight();
        this.maxHeight = tileFactory.getMaxHeight();
        ceilingHeight = maxHeight;
        if (init) {
            layerSettings.put(Resources.INSTANCE, ResourcesExporterSettings.defaultSettings(world.getPlatform(), anchor, minHeight, maxHeight));
            topLayerDepthNoise = new PerlinNoise(seed + TOP_LAYER_DEPTH_SEED_OFFSET);
            if (anchor.role == DETAIL) {
                switch (anchor.dim) {
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
    }

    public World2 getWorld() {
        return world;
    }

    public void setWorld(World2 world) {
        this.world = world;
    }

    public Anchor getAnchor() {
        return anchor;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (! Objects.equals(name, this.name)) {
            final String oldName = this.name;
            this.name = name;
            changeNo++;
            propertyChangeSupport.firePropertyChange("name", oldName, name);
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

    public TileFactory getTileFactory() {
        return tileFactory;
    }

    /**
     * Determines whether a tile is present in the dimension on specific coordinates.
     *
     * @param x The tile X coordinate for which to determine whether a tile is present.
     * @param y The tile Y coordinate for which to determine whether a tile is present.
     * @return {@code true} if the dimension contains a tile at the specified location.
     */
    @Override
    public boolean isTilePresent(final int x, final int y) {
        readLock.lock();
        try {
            return tiles.containsKey(new Point(x, y));
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Indicates whether the specified tile is a border tile.
     *
     * @param x The X coordinate of the tile for which to check whether it is a border tile.
     * @param y The Y coordinate of the tile for which to check whether it is a border tile.
     * @return {@code true} if it is a border tile.
     */
    public boolean isBorderTile(int x, int y) {
        readLock.lock();
        try {
            if ((border == null)
                    || ((!border.isEndless())
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
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Get the tile for a particular set of world or absolute block coordinates.
     *
     * @param x The tile X coordinate for which to get the tile.
     * @param y The tile Y coordinate for which to get the tile.
     * @return The tile with the specified coordinates, or {@code null} if there is no tile for those coordinates
     */
    @Override
    public Tile getTile(final int x, final int y) {
        readLock.lock();
        try {
            return tiles.get(new Point(x, y));
        } finally {
            readLock.unlock();
        }
    }

    public Tile getTile(final Point coords) {
        readLock.lock();
        try {
            return tiles.get(coords);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Get the tile for a particular set of world or absolute block coordinates with the intention of modifying it. This
     * is intended to be used in combination with {@link #setEventsInhibited(boolean)}. Whenever
     * {@code eventsInhibited} is {@code true}, the dimension will automatically inhibit events on the tile,
     * mark it as dirty and fire an event for it when {@code eventsInhibited} is set to {@code false}.
     *
     * @param x The tile X coordinate for which to get the tile.
     * @param y The tile Y coordinate for which to get the tile.
     * @return The tile with the specified coordinates, or {@code null} if there is no tile for those coordinates
     */
    public Tile getTileForEditing(final int x, final int y) {
        readLock.lock();
        try {
            Tile tile = tiles.get(new Point(x, y));
            if ((tile != null) && eventsInhibited && (!tile.isEventsInhibited())) {
                tile.inhibitEvents();
                dirtyTiles.add(tile);
            }
            return tile;
        } finally {
            readLock.unlock();
        }
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
    public Tile getTileForEditing(final Point coords) {
        readLock.lock();
        try {
            Tile tile = tiles.get(coords);
            if ((tile != null) && eventsInhibited && (!tile.isEventsInhibited())) {
                tile.inhibitEvents();
                dirtyTiles.add(tile);
            }
            return tile;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Rectangle getExtent() {
        return new Rectangle(lowestX, lowestY, (highestX - lowestX) + 1, (highestY - lowestY) + 1);
    }

    public int getTileCount() {
        return tiles.size();
    }

    /**
     * Get a collection of all extant tiles in the dimension. May not contain {@code null}s.
     */
    public Collection<? extends Tile> getTiles() {
        readLock.lock();
        try {
            return Collections.unmodifiableCollection(tiles.values());
        } finally {
            readLock.unlock();
        }
    }

    public Set<Point> getTileCoords() {
        readLock.lock();
        try {
            return Collections.unmodifiableSet(tiles.keySet());
        } finally {
            readLock.unlock();
        }
    }

    public void addTile(Tile tile) {
        writeLock.lock();
        try {
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
        } finally {
            writeLock.unlock();
        }
    }

    public void removeTile(int tileX, int tileY) {
        removeTile(new Point(tileX, tileY));
    }

    public void removeTile(Tile tile) {
        removeTile(tile.getX(), tile.getY());
    }

    public void removeTile(Point coords) {
        writeLock.lock();
        try {
            if (!tiles.containsKey(coords)) {
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
        } finally {
            writeLock.unlock();
        }
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

    /**
     * Get the terrain height at the specified coordinates, rounded to the nearest integer, if a tile exists in the
     * specified location.
     *
     * @param x The X coordinate to query.
     * @param y The Y coordinate to query.
     * @return The terrain height at the specified coordinates rounded to the nearest integer, or
     * {@link Integer#MIN_VALUE} if there is no tile at the specified coordinates.
     */
    public int getIntHeightAt(int x, int y) {
        return getIntHeightAt(x, y, Integer.MIN_VALUE);
    }

    /**
     * Get the terrain height at the specified coordinates, rounded to the nearest integer, if a tile exists in the
     * specified location.
     *
     * @param x             The X coordinate to query.
     * @param y             The Y coordinate to query.
     * @param defaultHeight The value to return if there is no tile at the specified coordinates.
     * @return The terrain height at the specified coordinates rounded to the nearest integer, or
     * {@code defaultHeight} if there is no tile at the specified coordinates.
     */
    public int getIntHeightAt(int x, int y, int defaultHeight) {
        Tile tile = getTile(x >> TILE_SIZE_BITS, y >> TILE_SIZE_BITS);
        if (tile != null) {
            return tile.getIntHeight(x & TILE_SIZE_MASK, y & TILE_SIZE_MASK);
        } else {
            return defaultHeight;
        }
    }

    /**
     * Get the terrain height at the specified coordinates, rounded to the nearest integer, if a tile exists in the
     * specified location.
     *
     * @param coords The coordinates to query.
     * @return The terrain height at the specified coordinates rounded to the nearest integer, or
     * {@link Integer#MIN_VALUE} if there is no tile at the specified coordinates.
     */
    public int getIntHeightAt(Point coords) {
        return getIntHeightAt(coords.x, coords.y, Integer.MIN_VALUE);
    }

    public int getLowestIntHeight() {
        return Math.round(getLowestRawHeight() / 256f + minHeight);
    }

    public int getHighestIntHeight() {
        return Math.round(getHighestRawHeight() / 256f + minHeight);
    }

    public int[] getIntHeightRange() {
        final int[] rawHeightRange = getRawHeightRange();
        return new int[] { Math.round(rawHeightRange[0] / 256f + minHeight), Math.round(rawHeightRange[1] / 256f + minHeight) };
    }

    /**
     * Get the terrain height at the specified coordinates, if a tile exists in the specified location.
     *
     * @param x The X coordinate to query.
     * @param y The Y coordinate to query.
     * @return The terrain height at the specified coordinates, or {@link Float#MAX_VALUE -Float.MAX_VALUE} if there is
     * no tile at the specified coordinates.
     */
    public float getHeightAt(int x, int y) {
        Tile tile = getTile(x >> TILE_SIZE_BITS, y >> TILE_SIZE_BITS);
        if (tile != null) {
            return tile.getHeight(x & TILE_SIZE_MASK, y & TILE_SIZE_MASK);
        } else {
            return -Float.MAX_VALUE;
        }
    }

    /**
     * Get the terrain height at the specified coordinates, if a tile exists in the specified location.
     *
     * @param coords The coordinates to query.
     * @return The terrain height at the specified coordinates, or {@link Float#MAX_VALUE -Float.MAX_VALUE} if there is
     * no tile at the specified coordinates.
     */
    public float getHeightAt(Point coords) {
        return getHeightAt(coords.x, coords.y);
    }

    public float getLowestHeight() {
        return getLowestRawHeight() / 256f + minHeight;
    }

    public float getHighestHeight() {
        return getHighestRawHeight() / 256f + minHeight;
    }

    public float[] getHeightRange() {
        final int[] rawHeightRange = getRawHeightRange();
        return new float[] { rawHeightRange[0] / 256f + minHeight, rawHeightRange[1] / 256f + minHeight };
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

    public int getLowestRawHeight() {
        int lowestRawHeight = Integer.MAX_VALUE;
        for (Tile tile: getTiles()) {
            final int tileLowestRawHeight = tile.getLowestRawHeight();
            if (tileLowestRawHeight < lowestRawHeight) {
                lowestRawHeight = tileLowestRawHeight;
            }
            if (lowestRawHeight <= 0) {
                return 0;
            }
        }
        return lowestRawHeight;
    }

    public int getHighestRawHeight() {
        int highestRawHeight = Integer.MIN_VALUE;
        final int maxRawHeight = (maxHeight - 1 - minHeight) * 256;
        for (Tile tile: getTiles()) {
            final int tileHighestRawHeight = tile.getHighestRawHeight();
            if (tileHighestRawHeight > highestRawHeight) {
                highestRawHeight = tileHighestRawHeight;
            }
            if (highestRawHeight >= maxRawHeight) {
                return maxRawHeight;
            }
        }
        return highestRawHeight;
    }

    public int[] getRawHeightRange() {
        int lowestRawHeight = Integer.MAX_VALUE, highestRawHeight = Integer.MIN_VALUE;
        final int maxRawHeight = (maxHeight - 1 - minHeight) * 256;
        for (Tile tile: getTiles()) {
            final int[] tileRawHeightRange = tile.getRawHeightRange();
            if (tileRawHeightRange[0] < lowestRawHeight) {
                lowestRawHeight = tileRawHeightRange[0];
            }
            if (tileRawHeightRange[1] > highestRawHeight) {
                highestRawHeight = tileRawHeightRange[1];
            }
            if ((lowestRawHeight <= 0) && (highestRawHeight >= maxRawHeight)) {
                return new int[] { 0, maxRawHeight };
            }
        }
        return new int[] { lowestRawHeight, highestRawHeight };
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
        return doGetSlope(x, y);
    }

    protected final float doGetSlope(int x, int y) {
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
     * Count the number of blocks where the specified bit layer is set in a square around a particular location
     *
     * @param layer The bit layer to count.
     * @param x The global X coordinate of the location around which to count the layer.
     * @param y The global Y coordinate of the location around which to count the layer.
     * @param r The radius of the square. The side of the square is 2&middot;r+1
     * @return The number of blocks in the specified square where the specified bit layer is set.
     */
    public int getBitLayerCount(final Layer layer, final int x, final int y, final int r) {
        readLock.lock();
        try {
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
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Gets all layers that are set at the specified location, along with their intensities or values. For bit-valued
     * layers the intensity is zero for off, one for
     * on.
     *
     * @param x The X location for which to retrieve all layers.
     * @param y The Y location for which to retrieve all layers.
     * @return A map with all layers set at the specified location, mapped to their intensities or values at that
     * location. May either be {@code null} or an empty map if no layers are present.
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
    public int getFloodedCount(final int x, final int y, final int r, final boolean lava) {
        return doGetFloodedCount(x, y, r, lava);
    }

    protected final int doGetFloodedCount(final int x, final int y, final int r, final boolean lava) {
        readLock.lock();
        try {
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
                                : (!getBitLayerValueAt(FloodWithLava.INSTANCE, xx, yy)))) {
                            count++;
                        }
                    }
                }
                return count;
            }
        } finally {
            readLock.unlock();
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
    public float getDistanceToEdge(final Layer layer, final int x, final int y, final float maxDistance) {
        return doGetDistanceToEdge(layer, x, y, maxDistance);
    }

    /**
     * Bake the edge distances for an entire painted layer and return them as a {@link HeightMap}. For high values of
     * {@code maxDistance} this is <em>much</em> quicker than interrogating the edge distance of every pixel via
     * {@link #getDistanceToEdge(Layer, int, int, float)}, but the information is static and does not update when the
     * dimension is updated.
     *
     * <p><strong>Note</strong> that the information in the height map is only valid for pixels where the layer is set
     * in the dimension! For other pixels the value is undefined.
     *
     * @param layer       The layer for which to bake the edge distances. Must have data type
     *                    {@link Layer.DataSize#BIT BIT} or {@link Layer.DataSize#BIT_PER_CHUNK BIT_PER_CHUNK}.
     * @param maxDistance The maximum edge distance to calculate. Pixels where the layer is set and the distance to the
     *                    nearest edge is higher will have this value set.
     * @return A {@link HeightMap} returning the distance to the nearest edge for every pixel where the specified layer
     * is set in this dimension.
     */
    @SuppressWarnings("UnnecessaryLocalVariable") // Clarity
    public HeightMap getDistancesToEdge(final Layer layer, final float maxDistance) {
        // Precalculate relative distances
        final float[][] distances = getDistancesToCentre(maxDistance);

        // Gather all the tiles that contain the layer so we can work on them directly rather than looking up tiles
        // continuously. visitTiles() will do the read locking
        final int[] coords = {Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE};
        final Set<Tile> tilesToProcess = new HashSet<>();
        final Set<Point> tileCoordsToProcess = new HashSet<>();
        visitTiles().forFilter(buildForDimension(this).onlyOn(layer).build()).andDo(tile -> {
            final int tileX = tile.getX(), tileY = tile.getY();
            if (tileX < coords[0]) {
                coords[0] = tileX;
            }
            if (tileX > coords[1]) {
                coords[1] = tileX;
            }
            if (tileY < coords[2]) {
                coords[2] = tileY;
            }
            if (tileY > coords[3]) {
                coords[3] = tileY;
            }
            tilesToProcess.add(tile);
            tileCoordsToProcess.add(new Point(tileX, tileY));
        });

        // If there are no tiles with the layer we're done
        if (tilesToProcess.isEmpty()) {
            return new ConstantHeightMap(0.0f);
        }

        // Also add the coordinates of all adjacent tiles, otherwise we can't detect edges that lie on a tile boundary
        final int[] deltas = {0, -1, 1, 0, 0, 1, -1, 0};
        for (Tile tile: tilesToProcess) {
            for (int i = 0; i < 4; i++) {
                final Point adjacentCoords = new Point(tile.getX() + deltas[i * 2], tile.getY() + deltas[i * 2 + 1]);
                if (tileCoordsToProcess.contains(adjacentCoords)) {
                    continue;
                }
                tileCoordsToProcess.add(adjacentCoords);
            }
        }

        // Store the tiles in a 2D array for fast access; also create the cache that will hold the distance values
        final int tileX1 = coords[0], tileX2 = coords[1], tileY1 = coords[2], tileY2 = coords[3];
        final Tile[][] tileCache = new Tile[tileX2 - tileX1 + 1][tileY2 - tileY1 + 1];
        final float[][][][] distanceCache = new float[tileX2 - tileX1 + 1][tileY2 - tileY1 + 1][][];
        final int tileXOffset = tileX1, tileYOffset = tileY1;
        for (Tile tile: tilesToProcess) {
            tileCache[tile.getX() - tileXOffset][tile.getY() - tileYOffset] = tile;
            // Initialise the entire cache to maxDistance. It is the caller's responsibility to consult it only
            // where the layer is actually set
            final float[][] cacheForTile = new float[TILE_SIZE][TILE_SIZE];
            for (float[] column: cacheForTile) {
                fill(column, maxDistance);
            }
            distanceCache[tile.getX() - tileXOffset][tile.getY() - tileYOffset] = cacheForTile;
        }

        // TODO: don't do this separately for every exported region

        // Now find all edge pixels and adjust the distances in the cache for the pixels around each one
        final int r = (int) Math.ceil(maxDistance);
        for (Point tileCoords: tileCoordsToProcess) {
            for (int x = 0; x < TILE_SIZE; x++) {
                for (int y = 0; y < TILE_SIZE; y++) {
                    final int worldX = (tileCoords.x << TILE_SIZE_BITS) | x, worldY = (tileCoords.y << TILE_SIZE_BITS) | y;
                    if ((! getBitLayerValueAt(tileCache, tileXOffset, tileYOffset, layer, worldX, worldY))
                            && (getBitLayerValueAt(tileCache, tileXOffset, tileYOffset, layer, worldX - 1, worldY)
                            || getBitLayerValueAt(tileCache, tileXOffset, tileYOffset, layer, worldX, worldY - 1)
                            || getBitLayerValueAt(tileCache, tileXOffset, tileYOffset, layer, worldX + 1, worldY)
                            || getBitLayerValueAt(tileCache, tileXOffset, tileYOffset, layer, worldX, worldY + 1))) {
                        // This pixel is directly outside the layer's painted area; adjust the pixels around it
                        // in the distance cache
                        for (int dx = -r; dx <= r; dx++) {
                            for (int dy = -r; dy <= r; dy++) {
                                setCachedValueIfLower(distanceCache, tileXOffset, tileYOffset, worldX + dx, worldY + dy, distances[dx + r][dy + r]);
                            }
                        }
                    }
                }
            }
        }

        // Return a HeightMap that will return values from the distance cache
        return new AbstractHeightMap() {
            @Override
            public Icon getIcon() {
                return null;
            }

            @Override
            public double[] getRange() {
                return new double[] {0.0f, maxDistance};
            }

            @Override
            public double getHeight(int x, int y) {
                final int tileX = (x >> TILE_SIZE_BITS) - tileXOffset, tileY = (y >> TILE_SIZE_BITS) - tileYOffset;
                if ((tileX < 0) || (tileX >= distanceCache.length) || (tileY < 0) || (tileY >= distanceCache[0].length) || (distanceCache[tileX][tileY] == null)) {
                    return 0.0f;
                }
                return distanceCache[tileX][tileY][x & TILE_SIZE_MASK][y & TILE_SIZE_MASK];
            }

            private void writeObject(ObjectOutputStream out) throws IOException {
                throw new NotSerializableException();
            }
        };
    }

    protected final float doGetDistanceToEdge(final Layer layer, final int x, final int y, final float maxDistance) {
        readLock.lock();
        try {
            final int r = (int) Math.ceil(maxDistance);
            final int tileX1 = (x - r) >> TILE_SIZE_BITS, tileX2 = (x + r) >> TILE_SIZE_BITS, tileY1 = (y - r) >> TILE_SIZE_BITS, tileY2 = (y + r) >> TILE_SIZE_BITS;
            if ((tileX1 == tileX2) && (tileY1 == tileY2)) {
                // The requested area is completely contained in one tile, optimise by delegating to the tile
                final Tile tile = getTile(tileX1, tileY1);
                if (tile != null) {
                    return tile.getDistanceToEdge(layer, x & TILE_SIZE_MASK, y & TILE_SIZE_MASK, maxDistance);
                } else {
                    return 0;
                }
            } else {
                if (! getBitLayerValueAt(layer, x, y)) {
                    return 0;
                }
                final Tile[][] tiles = new Tile[tileX2 - tileX1 + 1][tileY2 - tileY1 + 1];
                for (int tileX = tileX1; tileX <= tileX2; tileX++) {
                    for (int tileY = tileY1; tileY <= tileY2; tileY++) {
                        tiles[tileX - tileX1][tileY - tileY1] = getTile(tileX, tileY);
                    }
                }
                float distance = maxDistance;
                for (int i = 1; i <= r; i++) {
                    if (((! getBitLayerValueAt(tiles, tileX1, tileY1, layer, x - i, y))
                            || (! getBitLayerValueAt(tiles, tileX1, tileY1, layer, x + i, y))
                            || (! getBitLayerValueAt(tiles, tileX1, tileY1, layer, x, y - i))
                            || (! getBitLayerValueAt(tiles, tileX1, tileY1, layer, x, y + i)))
                            && (i < distance)) {
                        // If we get here there's no possible way a shorter distance could be found later, so return
                        // immediately
                        return i;
                    }
                    for (int d = 1; d <= i; d++) {
                        if ((! getBitLayerValueAt(tiles, tileX1, tileY1, layer, x - i, y - d))
                                || (! getBitLayerValueAt(tiles, tileX1, tileY1, layer, x + d, y - i))
                                || (! getBitLayerValueAt(tiles, tileX1, tileY1, layer, x + i, y + d))
                                || (! getBitLayerValueAt(tiles, tileX1, tileY1, layer, x - d, y + i))
                                || ((d < i) && ((! getBitLayerValueAt(tiles, tileX1, tileY1, layer, x - i, y + d))
                                || (! getBitLayerValueAt(tiles, tileX1, tileY1, layer, x - d, y - i))
                                || (! getBitLayerValueAt(tiles, tileX1, tileY1, layer, x + i, y - d))
                                || (! getBitLayerValueAt(tiles, tileX1, tileY1, layer, x + d, y + i))))) {
                            float tDistance = MathUtils.getDistance(i, d);
                            if (tDistance < distance) {
                                distance = tDistance;
                            }
                            // We won't find a shorter distance this round, so skip to the next round
                            break;
                        }
                    }
                }
                return distance;
            }
        } finally {
            readLock.unlock();
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
            if (eventsInhibited && (! tile.isEventsInhibited())) {
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
                dirtyTiles.forEach(Tile::releaseEvents);
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

    public List<Overlay> getOverlays() {
        return unmodifiableList(overlays);
    }

    public int addOverlay(Overlay overlay) {
        overlays.add(overlay);
        changeNo++;
        final int rowIndex = overlays.size() - 1;
        for (Listener listener: listeners) {
            listener.overlayAdded(this, rowIndex, overlay);
        }
        return rowIndex;
    }

    public void removeOverlay(int index) {
        final Overlay overlay = overlays.remove(index);
        changeNo++;
        for (Listener listener: listeners) {
            listener.overlayRemoved(this, index, overlay);
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

    public boolean isOverlaysEnabled() {
        return overlayEnabled;
    }

    public void setOverlaysEnabled(boolean overlaysEnabled) {
        if (overlaysEnabled != this.overlayEnabled) {
            this.overlayEnabled = overlaysEnabled;
            changeNo++;
            propertyChangeSupport.firePropertyChange("overlaysEnabled", ! overlaysEnabled, overlaysEnabled);
        }
    }

    /**
     * Get the <em>inclusive</em> lower bound of the terrain height range for this dimension.
     */
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

    /**
     * Get the <em>exclusive</em> upper bound of the terrain height range for this dimension.
     */
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
        return copyOf(customBiomes);
    }

    public void setCustomBiomes(List<CustomBiome> customBiomes) {
        if (! Objects.equals(customBiomes, this.customBiomes)) {
            final List<CustomBiome> oldCustomBiomes = this.customBiomes;
            this.customBiomes = copyOf(customBiomes);
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

    public Integer getUndergroundBiome() {
        return undergroundBiome;
    }

    public void setUndergroundBiome(Integer undergroundBiome) {
        if (! Objects.equals(undergroundBiome, this.undergroundBiome)) {
            final Integer oldUndergroundBiome = this.undergroundBiome;
            this.undergroundBiome = undergroundBiome;
            changeNo++;
            propertyChangeSupport.firePropertyChange("undergroundBiome", oldUndergroundBiome, undergroundBiome);
        }
    }

    public Garden getGarden() {
        return garden;
    }

    public List<CustomLayer> getCustomLayers() {
        return getCustomLayers(false);
    }

    @SuppressWarnings("DataFlowIssue") // The applyLayerContainer may have added non-CustomLayers
    public List<CustomLayer> getCustomLayers(boolean applyCombinedLayers) {
        final List<CustomLayer> copyOfCustomLayers = copyOf(customLayers);
        if (applyCombinedLayers) {
            applyLayerContainers(copyOfCustomLayers);
            // This may have added non-custom layers, so remove those again
            copyOfCustomLayers.removeIf((Predicate<Layer>) layer -> ! (layer instanceof CustomLayer));
        }
        return copyOfCustomLayers;
    }

    public void setCustomLayers(List<CustomLayer> customLayers) {
        this.customLayers = copyOf(customLayers);
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
        final Set<Layer> allLayers = new HashSet<>();
        for (Tile tile: tiles.values()) {
            allLayers.addAll(tile.getLayers());
        }
        if (applyCombinedLayers) {
            applyLayerContainers(allLayers);
        }
        return allLayers;
    }

    @SuppressWarnings({"rawtypes", "unchecked"}) // Necessary due to dumb Java type system that does not consider Collection<CustomLayer> compatible with Collection<Layer>
    private void applyLayerContainers(Collection layers) {
        final Set<LayerContainer> containersProcessed = new HashSet<>();
        final Set<Layer> additionalLayers = new HashSet<>();
        do {
            additionalLayers.clear();
            for (Iterator i = layers.iterator(); i.hasNext(); ) {
                final Layer layer = (Layer) i.next();
                if (layer instanceof LayerContainer) {
                    i.remove();
                    if (! containersProcessed.contains(layer)) {
                        additionalLayers.addAll(((LayerContainer) layer).getLayers());
                        containersProcessed.add((LayerContainer) layer);
                    }
                }
            }
            additionalLayers.forEach(layer -> {
                if (! layers.contains(layer)) {
                    layers.add(layer);
                }
            });
        } while (! additionalLayers.isEmpty());
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
        if (! Objects.equals(exportSettings, this.exportSettings)) {
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

    public WallType getWallType() {
        return wallType;
    }

    public void setWallType(WallType wallType) {
        if (wallType != this.wallType) {
            WallType oldWallType = this.wallType;
            this.wallType = wallType;
            changeNo++;
            propertyChangeSupport.firePropertyChange("wallType", oldWallType, wallType);
        }
    }

    public WallType getRoofType() {
        return roofType;
    }

    public void setRoofType(WallType roofType) {
        if (roofType != this.roofType) {
            WallType oldRoofType = this.roofType;
            this.roofType = roofType;
            changeNo++;
            propertyChangeSupport.firePropertyChange("roofType", oldRoofType, roofType);
        }
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        if (scale != this.scale) {
            float oldScale = this.scale;
            this.scale = scale;
            changeNo++;
            propertyChangeSupport.firePropertyChange("scale", oldScale, scale);
        }
    }

    public Set<String> getHiddenPalettes() {
        return copyOf(hiddenPalettes);
    }

    public void setHiddenPalettes(Set<String> hiddenPalettes) {
        if (! Objects.equals(hiddenPalettes, this.hiddenPalettes)) {
            final Set<String> oldHiddenPalettes = this.hiddenPalettes;
            this.hiddenPalettes = copyOf(hiddenPalettes);
            changeNo++;
            propertyChangeSupport.firePropertyChange("hiddenPalettes", oldHiddenPalettes, hiddenPalettes);
        }
    }

    public String getSoloedPalette() {
        return soloedPalette;
    }

    public void setSoloedPalette(String soloedPalette) {
        if (! Objects.equals(soloedPalette, this.soloedPalette)) {
            final String oldSoloedPalette = this.soloedPalette;
            this.soloedPalette = soloedPalette;
            changeNo++;
            propertyChangeSupport.firePropertyChange("soloedPalette", oldSoloedPalette, soloedPalette);
        }
    }

    public void applyTheme(Point coords) {
        applyTheme(coords.x, coords.y);
    }

    public boolean isUndoAvailable() {
        return undoManager != null;
    }

    public void registerUndoManager(UndoManager undoManager) {
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

    public void unregisterUndoManager() {
        for (Tile tile: tiles.values()) {
            tile.unregister();
        }
        undoManager = null;
    }

    public final int getAutoBiome(int x, int y) {
        return getAutoBiome(x, y, -1);
    }

    public final int getAutoBiome(int x, int y, int defaultBiome) {
        switch (anchor.dim) {
            case DIM_NETHER:
                return BIOME_HELL;
            case DIM_END:
                return BIOME_SKY;
            default:
                Tile tile = getTile(x >> TILE_SIZE_BITS, y >> TILE_SIZE_BITS);
                if (tile != null) {
                    return getAutoBiome(tile, x & TILE_SIZE_MASK, y & TILE_SIZE_MASK, defaultBiome);
                } else {
                    return defaultBiome;
                }
        }
    }

    public final int getAutoBiome(Tile tile, int x, int y) {
        return getAutoBiome(tile, x, y, -1);
    }

    public final int getAutoBiome(Tile tile, int x, int y, int defaultBiome) {
        // TODO add platform support and Minecraft 1.18 biomes
        switch (anchor.dim) {
            case DIM_NETHER:
                return BIOME_HELL;
            case DIM_END:
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
                        int waterDepth = tile.getWaterLevel(x, y) - tile.getIntHeight(x, y);
                        if ((waterDepth > 0) && (! tile.getBitLayerValue(FloodWithLava.INSTANCE, x, y))) {
                            if (waterDepth <= 5) {
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
                        int waterDepth = tile.getWaterLevel(x, y) - tile.getIntHeight(x, y);
                        if ((waterDepth > 0) && (!tile.getBitLayerValue(FloodWithLava.INSTANCE, x, y))) {
                            if (waterDepth <= 5) {
                                biome = BIOME_RIVER;
                            } else if (waterDepth <= 20) {
                                biome = BIOME_OCEAN;
                            } else {
                                biome = BIOME_DEEP_OCEAN;
                            }
                        } else {
                            final Terrain terrain = tile.getTerrain(x, y);
                            // TODO: we have reports from the wild of the custom terrain
                            //  returned here somehow not being configured, so check that
                            //  even though we don't understand how that could happen
                            if (terrain.isConfigured() && (terrain.getDefaultBiome() != -1)) {
                                defaultBiome = terrain.getDefaultBiome();
                            }
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
        tiles.values().forEach(Tile::ensureAllReadable);
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

            // Remove all tiles
            final Map<Point, Tile> oldTiles = new HashMap<>(tiles);
            final Set<Tile> removedTiles;
            writeLock.lock();
            try {
                tiles.clear();
                removedTiles = new HashSet<>(oldTiles.values());
                for (Tile removedTile: removedTiles) {
                    removedTile.removeListener(this);
                    removedTile.unregister();
                }
            } finally {
                writeLock.unlock();
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
            if (transform.isScaling()) {
                // Scaling requires a different approach. Note that we assume ONLY scaling!
                final ScalingHelper scalingHelper = new ScalingHelper(oldTiles, tileFactory, transform.getScale());
                final Set<Tile> scaledTiles = scalingHelper.getAllScaledTiles(progressReceiver);
                for (Tile tile: scaledTiles) {
                    // In rare circumstances (e.g. scaling up after scaling down) the tile may be entirely marked as
                    // NotPresent, in which case we want to skip it
                    if (tile.containsOneOf(NotPresent.INSTANCE)) {
                        boolean presentChunkFound = false;
                        outer:
                        for (int x = 0; x < TILE_SIZE; x += 16) {
                            for (int y = 0; y < TILE_SIZE; y += 16) {
                                if (! tile.getBitLayerValue(NotPresent.INSTANCE, x, y)) {
                                    presentChunkFound = true;
                                    break outer;
                                }
                            }
                        }
                        if (presentChunkFound) {
                            addTile(tile);
                        }
                    } else {
                        addTile(tile);
                    }
                }
            } else {
                int tileCount = removedTiles.size(), tileNo = 0;
                for (Iterator<Tile> i = removedTiles.iterator(); i.hasNext(); ) {
                    final Tile tile = i.next();
                    addTile(tile.transform(transform));
                    i.remove(); // Remove each tile as we're done with it so it can be garbage collected
                    tileNo++;
                    if (progressReceiver != null) {
                        progressReceiver.setProgress((float) tileNo / tileCount);
                    }
                }
            }

            tileFactory.transform(transform);

            for (Overlay overlay: overlays) {
                if (overlay.getFile().canRead()) {
                    try {
                        final java.awt.Dimension overlaySize = getImageSize(overlay.getFile());
                        if (overlaySize != null) {
                            final float overlayScale = overlay.getScale();
                            final Rectangle overlayCoords = transform.transform(new Rectangle(overlay.getOffsetX(), overlay.getOffsetY(), Math.round(overlaySize.width * overlayScale), Math.round(overlaySize.height * overlayScale)));
                            overlay.setOffsetX(overlayCoords.x);
                            overlay.setOffsetY(overlayCoords.y);
                            overlay.setScale(transform.transformScalar(overlayScale));
                        } else {
                            // Don't bother user with it, just clear the overlay
                            logger.error("Size of " + overlay.getFile() + " could not be determined; overlay will not be adjusted");
                            overlay.setEnabled(false);
                        }
                    } catch (IOException e) {
                        // Don't bother user with it, just clear the overlay
                        logger.error("I/O error while trying to determine size of " + overlay.getFile() + "; overlay will not be adjusted", e);
                        overlay.setEnabled(false);
                    }
                } else {
                    // Don't bother user with it, just clear the overlay
                    logger.error("Overlay " + overlay.getFile() + " could not be read; overlay will not be adjusted");
                    overlay.setEnabled(false);
                }
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
    public TileVisitationBuilder visitTilesForEditing() {
        return new TileVisitationBuilder(false);
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
        return new TileVisitationBuilder(true);
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
        fill(histogram, 0);
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

    public void save(ZipOutputStream out) throws IOException {
        readLock.lock();
        try {
            setEventsInhibited(true);
            try {
                // First serialise everything but the tiles to a separate file
                final String path = anchor + "/";
                out.putNextEntry(new ZipEntry(path + "dim-data.bin"));
                try {
                    final Map<Point, Tile> savedTiles = tiles;
                    final World2 savedWorld = world;
                    try {
                        tiles = null;
                        world = null;
                        final ObjectOutputStream dataout = new ObjectOutputStream(out);
                        dataout.writeObject(this);
                        dataout.flush();
                    } finally {
                        tiles = savedTiles;
                        world = savedWorld;
                    }
                } finally {
                    out.closeEntry();
                }

                // Then serialise the tiles, grouped by region
                final int regionX1 = lowestX >> 2, regionX2 = highestX >> 2, regionY1 = lowestY >> 2, regionY2 = highestY >> 2;
                for (int regionX = regionX1; regionX <= regionX2; regionX++) {
                    for (int regionY = regionY1; regionY <= regionY2; regionY++) {
                        final List<Tile> tileList = new ArrayList<>();
                        for (int tileX = 0; tileX < 4; tileX++) {
                            for (int tileY = 0; tileY < 4; tileY++) {
                                final Tile tile = tiles.get(new Point((regionX << 2) | tileX, (regionY << 2) | tileY));
                                if (tile != null) {
                                    tile.prepareForSaving();
                                    tileList.add(tile);
                                }
                            }
                        }

                        out.putNextEntry(new ZipEntry(path + "region-data-" + regionX + "," + regionY + ".bin"));
                        try {
                            final ObjectOutputStream dataout = new ObjectOutputStream(out);
                            dataout.writeObject(tileList);
                            dataout.flush();
                        } finally {
                            out.closeEntry();
                        }
                    }
                }
            } finally {
                setEventsInhibited(false);
            }
        } finally {
            readLock.unlock();
        }
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

    void changeAnchorToFloatingFloor() {
        if (anchor.role != CAVE_FLOOR) {
            throw new IllegalStateException();
        }
        anchor = new Anchor(anchor.dim, FLOATING_FLOOR, anchor.invert, anchor.id);
    }

    /**
     * Optimised version of {@link #getBitLayerValueAt(Layer, int, int)} which gets the information from a 2D array
     * cache of {@link Tile}s rather than looking up the tile each time.
     */
    private boolean getBitLayerValueAt(Tile[][] tileCache, int tileXOffset, int tileYOffset, Layer layer, int x, int y) {
        final int tileX = (x >> TILE_SIZE_BITS) - tileXOffset, tileY = (y >> TILE_SIZE_BITS) - tileYOffset;
        if ((tileX < 0) || (tileX >= tileCache.length) || (tileY < 0) || (tileY >= tileCache[0].length) || (tileCache[tileX][tileY] == null)) {
            return false;
        } else {
            return tileCache[tileX][tileY].getBitLayerValue(layer, x & TILE_SIZE_MASK, y & TILE_SIZE_MASK);
        }
    }

    /**
     * Updates the value in a 4D cache of {@code float}s (2D arrays of pixels per tile, arranged in a 2D array of tiles)
     * if the current value is higher.
     */
    private void setCachedValueIfLower(float[][][][] cache, int tileXOffset, int tileYOffset, int x, int y, float value) {
        final int tileX = (x >> TILE_SIZE_BITS) - tileXOffset, tileY = (y >> TILE_SIZE_BITS) - tileYOffset;
        if ((tileX < 0) || (tileX >= cache.length) || (tileY < 0) || (tileY >= cache[0].length) || (cache[tileX][tileY] == null)) {
            return;
        }
        final int xInTile = x & TILE_SIZE_MASK, yInTile = y & TILE_SIZE_MASK;
        if (value < cache[tileX][tileY][xInTile][yInTile]) {
            cache[tileX][tileY][xInTile][yInTile] = value;
        }
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
        lock = new ReentrantReadWriteLock();
        readLock = lock.readLock();
        writeLock = lock.writeLock();

        if (tiles == null) {
            tiles = new HashMap<>();
        }
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
                // Convert the nibble sized biomes data from a legacy map to byte-sized data
                tiles.values().forEach(Tile::convertBiomeData);
                biomesConverted = true;
            }
            if (maxHeight == 0) {
                maxHeight = 128;
            }
            if (subsurfaceMaterial == Terrain.RESOURCES) {
                subsurfaceMaterial = Terrain.STONE;

                // Load legacy settings
                ResourcesExporterSettings settings = ResourcesExporterSettings.defaultSettings(JAVA_ANVIL, NORMAL_DETAIL, minHeight, maxHeight);
                settings.setChance(GOLD_ORE,         1);
                settings.setChance(IRON_ORE,         5);
                settings.setChance(COAL,             9);
                settings.setChance(LAPIS_LAZULI_ORE, 1);
                settings.setChance(DIAMOND_ORE,      1);
                settings.setChance(REDSTONE_ORE,     6);
                settings.setChance(STATIONARY_WATER, 1);
                settings.setChance(STATIONARY_LAVA,  1);
                settings.setChance(DIRT,             9);
                settings.setChance(GRAVEL,           9);
                settings.setMaxLevel(GOLD_ORE,         Terrain.GOLD_LEVEL);
                settings.setMaxLevel(IRON_ORE,         Terrain.IRON_LEVEL);
                settings.setMaxLevel(COAL,             Terrain.COAL_LEVEL);
                settings.setMaxLevel(LAPIS_LAZULI_ORE, Terrain.LAPIS_LAZULI_LEVEL);
                settings.setMaxLevel(DIAMOND_ORE,      Terrain.DIAMOND_LEVEL);
                settings.setMaxLevel(REDSTONE_ORE,     Terrain.REDSTONE_LEVEL);
                settings.setMaxLevel(STATIONARY_WATER, Terrain.WATER_LEVEL);
                settings.setMaxLevel(STATIONARY_LAVA,  Terrain.LAVA_LEVEL);
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
        if (wpVersion < 5) {
            if (darkLevel) {
                roofType = BEDROCK;
                darkLevel = false;
            }
            if (bedrockWall) {
                wallType = BEDROCK;
                bedrockWall = false;
            }
        }
        if (wpVersion < 6) {
            switch (dim) {
                case -3:
                    anchor = END_DETAIL_CEILING;
                    break;
                case -2:
                    anchor = NETHER_DETAIL_CEILING;
                    break;
                case -1:
                    anchor = NORMAL_DETAIL_CEILING;
                    break;
                case DIM_NORMAL:
                    anchor = NORMAL_DETAIL;
                    break;
                case DIM_NETHER:
                    anchor = NETHER_DETAIL;
                    break;
                case DIM_END:
                    anchor = END_DETAIL;
                    break;
            }
        }
        if (wpVersion < 7) {
            scale = 1.0f;
            final StringBuilder sb = new StringBuilder();
            switch (anchor.dim) {
                case 0:
                    sb.append("Surface");
                    break;
                case 1:
                    sb.append("Nether");
                    break;
                case 2:
                    sb.append("End");
                    break;
                default:
                    sb.append("Dimension " + anchor.dim);
                    break;
            }
            if (anchor.invert) {
                sb.append(" Ceiling");
            }
            name = sb.toString();
        }
        if (wpVersion < 8) {
            id = UUID.randomUUID();
        }
        if (wpVersion < 9) {
            overlays = new ArrayList<>();
            if (this.overlay != null) {
                final Overlay overlay = new Overlay(this.overlay);
                overlay.setOffsetX(overlayOffsetX);
                overlay.setOffsetY(overlayOffsetY);
                overlay.setScale(overlayScale);
                overlay.setTransparency(overlayTransparency);
                overlays.add(overlay);
                this.overlay = null;
                overlayOffsetX = 0;
                overlayOffsetY = 0;
                overlayScale = 0.0f;
                overlayTransparency = 0.0f;
            }
        }
        wpVersion = CURRENT_WP_VERSION;

        // Make sure customLayers isn't some weird read-only list
        if (! (customLayers instanceof ArrayList)) {
            customLayers = new ArrayList<>(customLayers);
        }

        // Make sure that any custom layers which somehow ended up in the world are on the custom layer list so they
        // will be added to a palette in the GUI. TODO: fix this properly
        getAllLayers(false).stream()
                .filter(layer -> (layer instanceof CustomLayer) && (! customLayers.contains(layer)))
                .forEach(layer -> customLayers.add((CustomLayer) layer));

        // Due to an unknown problem there are sometimes duplicate layers in the customLayers list. It has something to
        // do with those layers being in a combined layer and/or not being in use on the map. Until we identify the root
        // cause, fix it here
        final Set<CustomLayer> layersEncountered = new HashSet<>();
        for (Iterator<CustomLayer> i = customLayers.iterator(); i.hasNext(); ) {
            final CustomLayer customLayer = i.next();
            if (layersEncountered.contains(customLayer)) {
                logger.warn("Removing duplicate custom layer {} from dimension", customLayer);
                i.remove();
            } else {
                layersEncountered.add(customLayer);
            }
        }

        // Because we did this fix wrong in the past, the maxHeight of the dimension may not correspond to that of its
        // tiles.
        if (! tiles.isEmpty()) {
            final int tileMaxHeight = tiles.values().iterator().next().getMaxHeight();
            if (tileMaxHeight != maxHeight) {
                logger.warn("Fixing maxHeight of dimension " + getName() + " (was " + maxHeight + ", should be " + tileMaxHeight + ")");
                maxHeight = tileMaxHeight;
            }
        }
    }

    private World2 world;
    private final long seed;
    @Deprecated
    private int dim = 0;
    Map<Point, Tile> tiles = new HashMap<>();
    private final TileFactory tileFactory;
    private int lowestX = Integer.MAX_VALUE, highestX = Integer.MIN_VALUE, lowestY = Integer.MAX_VALUE, highestY = Integer.MIN_VALUE;
    private Terrain subsurfaceMaterial = Terrain.STONE_MIX;
    private boolean populate;
    private Border border;
    private int borderLevel = DEFAULT_WATER_LEVEL, borderSize = 2;
    @Deprecated private boolean darkLevel, bedrockWall;
    private Map<Layer, ExporterSettings> layerSettings = new HashMap<>();
    private long minecraftSeed;
    @Deprecated private File overlay;
    @Deprecated private float overlayScale = 1.0f, overlayTransparency = 0.5f;
    @Deprecated private int overlayOffsetX, overlayOffsetY;
    private int gridSize = 128;
    /**
     * Should be {@code overlaysEnabled}; has the wrong name for historical purposes.
     */
    private boolean overlayEnabled;
    private boolean gridEnabled, biomesConverted = true;
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
    private int ceilingHeight;
    private Map<String, Object> attributes;
    private LayerAnchor subsurfaceLayerAnchor = LayerAnchor.BEDROCK, topLayerAnchor = LayerAnchor.BEDROCK;
    private ExportSettings exportSettings;
    private MapGenerator generator;
    private WallType wallType, roofType;
    private Anchor anchor;
    private float scale = 1.0f;
    private String name;
    private Set<String> hiddenPalettes;
    private String soloedPalette;
    private UUID id = UUID.randomUUID();
    private List<Overlay> overlays = new ArrayList<>();
    /**
     * Index of the underground biome, or {@code null} for "same as surface".
     */
    private Integer undergroundBiome;
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
    private transient ReadWriteLock lock = new ReentrantReadWriteLock();
    private transient Lock readLock = lock.readLock(), writeLock = lock.writeLock();

    public static final int[] POSSIBLE_AUTO_BIOMES = {BIOME_PLAINS, BIOME_FOREST,
        BIOME_SWAMPLAND, BIOME_JUNGLE, BIOME_MESA, BIOME_DESERT, BIOME_BEACH,
        BIOME_RIVER, BIOME_OCEAN, BIOME_DEEP_OCEAN, BIOME_ICE_PLAINS,
        BIOME_COLD_TAIGA, BIOME_FROZEN_RIVER, BIOME_FROZEN_OCEAN,
        BIOME_MUSHROOM_ISLAND, BIOME_HELL, BIOME_SKY};

    private static final long TOP_LAYER_DEPTH_SEED_OFFSET = 180728193;
    private static final float ROOT_EIGHT = (float) Math.sqrt(8.0);
    private static final int CURRENT_WP_VERSION = 9;
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Dimension.class);
    private static final long serialVersionUID = 2011062401L;

    public interface Listener {
        void tilesAdded(Dimension dimension, Set<Tile> tiles);
        void tilesRemoved(Dimension dimension, Set<Tile> tiles);
        void overlayAdded(Dimension dimension, int index, Overlay overlay);
        void overlayRemoved(Dimension dimension, int index, Overlay overlay);
    }

    public interface TileVisitor {
        void visit(Tile tile);
    }

    public enum Border {
        VOID(false), WATER(false), LAVA(false), ENDLESS_VOID(true), ENDLESS_WATER(true), ENDLESS_LAVA(true), BARRIER(false), ENDLESS_BARRIER(true);

        Border(boolean endless) {
            this.endless = endless;
        }

        public boolean isEndless() {
            return endless;
        }

        private final boolean endless;
    }

    public enum LayerAnchor {BEDROCK, TERRAIN}

    public enum WallType { BEDROCK, BARIER /* typo, but it's in the wild, so we can't easily fix it anymore... 😔 */}

    public class TileVisitationBuilder {
        public TileVisitationBuilder(boolean readOnly) {
            this.readOnly = readOnly;
        }

        /**
         * Visit tiles for a specific filter. This allows potential optimisation by skipping tiles on which the filter
         * does not apply.
         *
         * <p><strong>Please note:</strong> the only guarantee is that <em>some</em> tiles to which
         * the filter does not apply <em>may</em> be skipped. You still need to apply the entire filter to each tile
         * yourself, as there is no guarantee the filter applies at all to each tile. This method exists only for
         * potential (perhaps future) performance optimisation.
         */
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
            final boolean checkSelection = (! selection) && (filter instanceof DefaultFilter) && ((DefaultFilter) filter).isInSelection();
            final Layer layerPresent = (filter instanceof DefaultFilter) ? ((DefaultFilter) filter).getOnlyOnLayer() : null;
            final Layer layerNotPresent = (filter instanceof DefaultFilter) ? ((DefaultFilter) filter).getExceptOnLayer() : null;
            final boolean checkLayerPresent = layerPresent != null, checkLayerNotPresent = layerNotPresent != null;
            final int totalTiles = tiles.size();
            int tileCount = 0;
            int tileX1 = Integer.MIN_VALUE, tileX2 = Integer.MAX_VALUE;
            int tileY1 = Integer.MIN_VALUE, tileY2 = Integer.MAX_VALUE;
            if (brush != null) {
                final Rectangle boundingBox = brush.getBoundingBox();
                final int x1 = x + boundingBox.x, x2 = x1 + boundingBox.width - 1;
                final int y1 = y + boundingBox.y, y2 = y1 + boundingBox.height - 1;
                tileX1 = x1 >> TILE_SIZE_BITS;
                tileX2 = x2 >> TILE_SIZE_BITS;
                tileY1 = y1 >> TILE_SIZE_BITS;
                tileY2 = y2 >> TILE_SIZE_BITS;
            }
            final Set<Point> tileCoords;
            readLock.lock();
            try {
                tileCoords = getTileCoords();
            } finally {
                readLock.unlock();
            }
            for (Point coords: tileCoords) {
                final Tile tile = readOnly ? getTile(coords) : getTileForEditing(coords);
                final int tileX = tile.getX(), tileY = tile.getY();
                if ((tileX >= tileX1) && (tileX <= tileX2) && (tileY >= tileY1) && (tileY <= tileY2)
                        && ((! checkSelection) || tile.containsOneOf(SelectionBlock.INSTANCE, SelectionChunk.INSTANCE))
                        && ((! checkLayerPresent) || tile.containsOneOf(layerPresent))
                        && ((! checkLayerNotPresent) || (! tile.containsOneOf(layerNotPresent)))) { // TODO wait, this is wrong, no?
                    if (readOnly) {
                        visitor.visit(tile);
                    } else {
                        tile.inhibitEvents();
                        try {
                            visitor.visit(tile);
                        } finally {
                            tile.releaseEvents();
                        }
                    }
                }
                tileCount++;
                if (progressReceiver != null) {
                    progressReceiver.setProgress((float) tileCount / totalTiles);
                }
            }
        }

        private final boolean readOnly;
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

    public static final class Anchor implements Serializable, Comparable {
        public Anchor(int dim, Role role, boolean invert, int id) {
            if (role == null) {
                throw new NullPointerException("role");
            }
            this.dim = dim;
            this.role = role;
            this.invert = invert;
            this.id = id;
        }

        public String getDefaultName() {
            final StringBuilder sb = new StringBuilder();
            switch (dim) {
                case DIM_NORMAL:
                    sb.append("Surface");
                    break;
                case DIM_NETHER:
                    sb.append("Nether");
                    break;
                case DIM_END:
                    sb.append("End");
                    break;
                default:
                    sb.append("Dimension ");
                    sb.append(dim);
                    break;
            }
            switch (role) {
                case MASTER:
                    sb.append(" Master");
                    break;
                case CAVE_FLOOR:
                    sb.append(" Cave Floor");
                    break;
                case FLOATING_FLOOR:
                    sb.append(" Floating Floor");
                    break;
            }
            if (invert) {
                sb.append(" Ceiling");
            }
            if (id != 0) {
                sb.append(' ');
                sb.append(id);
            }
            return sb.toString();
        }

        @Override
        public String toString() {
            return dim
                    + " " + role
                    + (invert ? " CEILING" : "")
                    + ((id != 0) ? (" " + id) : "");
        }

        @Override
        public boolean equals(final Object o) {
            return (o instanceof Anchor)
                    && (((Anchor) o).dim == dim)
                    && (((Anchor) o).role == role)
                    && (((Anchor) o).invert == invert)
                    && (((Anchor) o).id == id);
        }

        @Override
        public int hashCode() {
            return 31 * (31 * (31 * dim + role.hashCode()) + (invert ? 1 : 0)) + id;
        }

        // Comparable

        @Override
        public int compareTo(@NotNull Object o) {
            return COMPARATOR.compare(this, (Anchor) o);
        }

        /**
         * Parse a string previously produced by {@link #toString()} into a new {@code Anchor} instance.
         */
        public static Anchor fromString(String str) {
            final String[] parts = str.split(" ");
            final int dim = Integer.parseInt(parts[0]);
            final Role role = Role.valueOf(parts[1]);
            final boolean invert = (parts.length > 2) && parts[2].equals("CEILING");
            final int id = invert
                    ? ((parts.length > 3) ? Integer.parseInt(parts[3]) : 0)
                    : ((parts.length > 2) ? Integer.parseInt(parts[2]) : 0);
            return new Anchor(dim, role, invert, id);
        }

        /**
         * The game dimension to which this anchor refers. See {@link Constants#DIM_NORMAL},
         * {@link Constants#DIM_NETHER} and {@link Constants#DIM_END} for predefined values. Note that they don't
         * correspond to the dimension numbers in Minecraft.
         */
        public final int dim;

        /**
         * The role this anchor plays in the specified game dimension.
         */
        public final Role role;

        /**
         * Whether this anchor should be exported inverted (e.g. as a ceiling).
         */
        public final boolean invert;

        /**
         * A unique identifier that identifies this anchor within the same ({@link #dim}, {@link #role},
         * {@link #invert}) combo. ID 0 should always exist and be the "main" or "default" anchor. Other values may or
         * may not imply an ordering.
         */
        public final int id;

        /**
         * Convenience constant for the default dimension (surface detail dimension, not inverted, layer zero).
         */
        public static final Anchor NORMAL_DETAIL = new Anchor(DIM_NORMAL, DETAIL, false, 0);

        /**
         * Convenience constant for the default Master dimension (surface master dimension, not inverted, layer zero).
         */
        public static final Anchor NORMAL_MASTER = new Anchor(DIM_NORMAL, MASTER, false, 0);

        /**
         * Convenience constant for the default Nether dimension (Nether detail dimension, not inverted, layer zero).
         */
        public static final Anchor NETHER_DETAIL = new Anchor(DIM_NETHER, DETAIL, false, 0);

        /**
         * Convenience constant for the default End dimension (End detail dimension, not inverted, layer zero).
         */
        public static final Anchor END_DETAIL = new Anchor(DIM_END, DETAIL, false, 0);

        /**
         * Convenience constant for the default dimension ceiling (surface detail dimension, inverted, layer zero).
         */
        public static final Anchor NORMAL_DETAIL_CEILING = new Anchor(DIM_NORMAL, DETAIL, true, 0);

        /**
         * Convenience constant for the default Nether dimension ceiling (Nether detail dimension, inverted, layer zero).
         */
        public static final Anchor NETHER_DETAIL_CEILING = new Anchor(DIM_NETHER, DETAIL, true, 0);

        /**
         * Convenience constant for the default End dimension ceiling (End detail dimension, inverted, layer zero).
         */
        public static final Anchor END_DETAIL_CEILING = new Anchor(DIM_END, DETAIL, true, 0);

        private static final Comparator<Anchor> COMPARATOR = Comparator
                .comparing((Anchor a) -> a.dim)
                .thenComparing(a -> a.role)
                .thenComparing(a -> a.invert)
                .thenComparing(a -> a.id);
        private static final long serialVersionUID = 1L;
    }

    /**
     * A role of a {@link Dimension} within a game dimension.
     */
    public enum Role {
        /**
         * A detail dimension.
         */
        DETAIL,

        /**
         * A master dimension that is exported where no detail dimension exists, at 1:16 scale.
         */
        MASTER,

        /**
         * A dimension associated with a {@link TunnelLayer} floor in cave mode. The {@link Anchor#id} field is used to
         * associate it with a particular layer.
         */
        CAVE_FLOOR,

        /**
         * A dimension associated with a {@link TunnelLayer} floor in floating dimension mode. The {@link Anchor#id}
         * field is used to associate it with a particular layer.
         */
        FLOATING_FLOOR
    }
}