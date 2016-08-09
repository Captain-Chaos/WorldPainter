/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter;

import java.awt.Point;
import java.awt.Rectangle;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.pepsoft.util.undo.UndoManager;
import static org.pepsoft.worldpainter.Constants.*;
import org.pepsoft.worldpainter.layers.*;
import org.pepsoft.worldpainter.layers.exporters.ExporterSettings;

/**
 * Superseded by {@link World2}. Don't use any more!
 * 
 * @author pepijn
 */
@Deprecated
public final class World implements TileProvider, Serializable, Tile.Listener {
    private World() {
        // Prevent instantiation
    }

    public boolean isDirty() {
        return false;
    }

    public void setDirty(boolean dirty) {
        throw new UnsupportedOperationException("Deprecated");
    }

    public long getSeed() {
        return seed;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        throw new UnsupportedOperationException("Deprecated");
    }

    public Terrain getSubsurfaceMaterial() {
        return subsurfaceMaterial;
    }

    public void setSubsurfaceMaterial(Terrain subsurfaceMaterial) {
        throw new UnsupportedOperationException("Deprecated");
    }

    public boolean isCreateGoodiesChest() {
        return createGoodiesChest;
    }

    public void setCreateGoodiesChest(boolean createGoodiesChest) {
        throw new UnsupportedOperationException("Deprecated");
    }

    public boolean isPopulate() {
        return populate;
    }

    public void setPopulate(boolean populate) {
        throw new UnsupportedOperationException("Deprecated");
    }

    public Border getBorder() {
        return border;
    }

    public void setBorder(Border border) {
        throw new UnsupportedOperationException("Deprecated");
    }

    public int getBorderLevel() {
        return borderLevel;
    }

    public void setBorderLevel(int borderLevel) {
        throw new UnsupportedOperationException("Deprecated");
    }

    public boolean isDarkLevel() {
        return darkLevel;
    }

    public void setDarkLevel(boolean darkLevel) {
        throw new UnsupportedOperationException("Deprecated");
    }

    public boolean isBedrockWall() {
        return bedrockWall;
    }

    public void setBedrockWall(boolean bedrockWall) {
        throw new UnsupportedOperationException("Deprecated");
    }

    public TileFactory getTileFactory() {
        return tileFactory;
    }

    @Override
    public boolean isTilePresent(int x, int y) {
        return tiles.containsKey(new Point(x, y));
    }

    @Override
    public Tile getTile(int x, int y) {
        return tiles.get(new Point(x, y));
    }

    public Tile getTile(Point coords) {
        return getTile(coords.x, coords.y);
    }

    @Override
    public Rectangle getExtent() {
        return new Rectangle(lowestX, lowestY, (highestX - lowestX) + 1, (highestY - lowestY) + 1);
    }

    public Collection<Tile> getTiles() {
        return new ArrayList<>(tiles.values());
    }

    public void addTile(Tile tile) {
        throw new UnsupportedOperationException("Deprecated");
    }
    
    public void removeTile(Tile tile) {
        throw new UnsupportedOperationException("Deprecated");
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
    
    public Point getTileCoordinates(int worldX, int worldY) {
        int tileX = (int) Math.floor((double) worldX / TILE_SIZE);
        int tileY = (int) Math.floor((double) worldY / TILE_SIZE);
        return new Point(tileX, tileY);
    }
    
    public Point getTileCoordinates(Point worldCoords) {
        return getTileCoordinates(worldCoords.x, worldCoords.y);
    }

    public float getHeightAt(int x, int y) {
        Point tileCoords = getTileCoordinates(x, y);
        Tile tile = getTile(tileCoords);
        if (tile != null) {
            return tile.getHeight(x & (TILE_SIZE - 1), y & (TILE_SIZE - 1));
        } else {
            return Float.MIN_VALUE;
        }
    }

    public float getHeightAt(Point coords) {
        return getHeightAt(coords.x, coords.y);
    }

    public void setHeightAt(int x, int y, float height) {
        throw new UnsupportedOperationException("Deprecated");
    }

    public void setHeightAt(Point coords, int height) {
        throw new UnsupportedOperationException("Deprecated");
    }

    public Terrain getTerrainAt(int x, int y) {
        Point tileCoords = getTileCoordinates(x, y);
        Tile tile = getTile(tileCoords);
        if (tile != null) {
            return tile.getTerrain(x & (TILE_SIZE - 1), y & (TILE_SIZE - 1));
        } else {
            return null;
        }
    }

    public void setTerrainAt(int x, int y, Terrain terrain) {
        throw new UnsupportedOperationException("Deprecated");
    }

    public void setTerrainAt(Point coords, Terrain terrain) {
        throw new UnsupportedOperationException("Deprecated");
    }

    public void applyTheme(int x, int y) {
        throw new UnsupportedOperationException("Deprecated");
    }

    public int getWaterLevelAt(int x, int y) {
        Point tileCoords = getTileCoordinates(x, y);
        Tile tile = getTile(tileCoords);
        if (tile != null) {
            return tile.getWaterLevel(x & (TILE_SIZE - 1), y & (TILE_SIZE - 1));
        } else {
            return Integer.MIN_VALUE;
        }
    }

    public int getWaterLevelAt(Point coords) {
        return getWaterLevelAt(coords.x, coords.y);
    }

    public void setWaterLevelAt(int x, int y, int waterLevel) {
        throw new UnsupportedOperationException("Deprecated");
    }

    public int getLayerValueAt(Layer layer, int x, int y) {
        Point tileCoords = getTileCoordinates(x, y);
        Tile tile = getTile(tileCoords);
        if (tile != null) {
            return tile.getLayerValue(layer, x & (TILE_SIZE - 1), y & (TILE_SIZE - 1));
        } else {
            return 0;
        }
    }

    public void setLayerValueAt(Layer layer, int x, int y, int value) {
        throw new UnsupportedOperationException("Deprecated");
    }

    public boolean getBitLayerValueAt(Layer layer, int x, int y) {
        Point tileCoords = getTileCoordinates(x, y);
        Tile tile = getTile(tileCoords);
        if (tile != null) {
            return tile.getBitLayerValue(layer, x & (TILE_SIZE - 1), y & (TILE_SIZE - 1));
        } else {
            return false;
        }
    }

    public void setBitLayerValueAt(Layer layer, int x, int y, boolean value) {
        throw new UnsupportedOperationException("Deprecated");
    }

    public void setEventsInhibited(boolean eventsInhibited) {
        throw new UnsupportedOperationException("Deprecated");
    }

    public boolean isEventsInhibited() {
        return false;
    }

    public Point getSpawnPoint() {
        return spawnPoint;
    }

    public void setSpawnPoint(Point spawnPoint) {
        throw new UnsupportedOperationException("Deprecated");
    }

    public File getImportedFrom() {
        return importedFrom;
    }

    public void setImportedFrom(File importedFrom) {
        throw new UnsupportedOperationException("Deprecated");
    }

    public Map<Layer, ExporterSettings> getAllLayerSettings() {
        return Collections.unmodifiableMap(layerSettings);
    }
    
    public ExporterSettings getLayerSettings(Layer layer) {
        return layerSettings.get(layer);
    }
    
    public void setLayerSettings(Layer layer, ExporterSettings settings) {
        throw new UnsupportedOperationException("Deprecated");
    }
    
    /**
     * Get the set of layers that has been configured to be applied everywhere.
     * 
     * @return The set of layers that has been configured to be applied
     *     everywhere.
     */
    @SuppressWarnings("unchecked")
    public Set<Layer> getMinimumLayers() {
        Set<Layer> layers = layerSettings.values().stream().filter(ExporterSettings::isApplyEverywhere).map(ExporterSettings::getLayer).collect(Collectors.toSet());
        return layers;
    }

    public long getMinecraftSeed() {
        return minecraftSeed;
    }

    public void setMinecraftSeed(long minecraftSeed) {
        throw new UnsupportedOperationException("Deprecated");
    }

    public void applyTheme(Point coords) {
        throw new UnsupportedOperationException("Deprecated");
    }

    public void register(UndoManager undoManager) {
        throw new UnsupportedOperationException("Deprecated");
    }

    public void destroy() {
        for (Tile tile: tiles.values()) {
            tile.removeListener(this);
            tile.unregister();
        }
    }

    public void addWorldListener(Listener listener) {
        // Do nothing
    }

    public void removeWorldListener(Listener listener) {
        // Do nothing
    }
    
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        // Do nothing
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        // Do nothing
    }
    
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        // Do nothing
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        // Do nothing
    }
    
    // Tile.Listener

    @Override public void heightMapChanged(Tile tile) {}
    @Override public void terrainChanged(Tile tile) {}
    @Override public void waterLevelChanged(Tile tile) {}
    @Override public void seedsChanged(Tile tile) {}
    @Override public void layerDataChanged(Tile tile, Set<Layer> changedLayers) {}
    @Override public void allBitLayerDataChanged(Tile tile) {}
    @Override public void allNonBitlayerDataChanged(Tile tile) {}
    
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        for (Tile tile: tiles.values()) {
            tile.addListener(this);
        }

        // Legacy maps
        if (subsurfaceMaterial == null) {
            subsurfaceMaterial = Terrain.STONE;
            createGoodiesChest = true;
            borderLevel = 4;
        }
        if (spawnPoint == null) {
            spawnPoint = new Point(15, 0);
        }
        if (layerSettings == null) {
            layerSettings = new HashMap<>();
        }
        if (minecraftSeed == Long.MIN_VALUE) {
            minecraftSeed = seed;
        }
    }

    private final long seed = 0L;
    private final Map<Point, Tile> tiles = new HashMap<>();
    private final TileFactory tileFactory = null;
    private final int lowestX = Integer.MAX_VALUE, highestX = Integer.MIN_VALUE, lowestY = Integer.MAX_VALUE, highestY = Integer.MIN_VALUE;
    private final String name = "Generated World";
    private Terrain subsurfaceMaterial = Terrain.RESOURCES;
    private boolean createGoodiesChest = true, populate;
    private Border border;
    private int borderLevel = 4;
    private boolean darkLevel, bedrockWall;
    private Point spawnPoint = new Point(15, 0);
    private File importedFrom;
    private Map<Layer, ExporterSettings> layerSettings = new HashMap<>();
    private long minecraftSeed = Long.MIN_VALUE;

    private static final long serialVersionUID = 2011032801L;

    @Deprecated
    public interface Listener {
        void tileAdded(World world, Tile tile);
        void tileRemoved(World world, Tile tile);
    }

    @Deprecated
    public enum Border {VOID, WATER, LAVA}
}