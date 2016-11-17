/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter;

import java.awt.Point;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.pepsoft.util.MathUtils;
import org.pepsoft.util.undo.BufferKey;
import org.pepsoft.util.undo.UndoListener;
import org.pepsoft.util.undo.UndoManager;

import static org.pepsoft.util.ObjectUtils.copyObject;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE;
import org.pepsoft.worldpainter.gardenofeden.Seed;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.Layer.DataSize;

import static org.pepsoft.worldpainter.Constants.TILE_SIZE_BITS;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE_MASK;
import org.pepsoft.worldpainter.layers.Biome;
import org.pepsoft.worldpainter.layers.FloodWithLava;

import static org.pepsoft.worldpainter.Tile.TileBuffer.*;
import static org.pepsoft.worldpainter.layers.Layer.DataSize.*;

/**
 *
 * @author pepijn
 */
public class Tile extends InstanceKeeper implements Serializable, UndoListener, Cloneable {
    public Tile(int x, int y, int maxHeight) {
        this(x, y, maxHeight, true);
    }

    protected Tile(int x, int y, int maxHeight, boolean init) {
        this.x = x;
        this.y = y;
        this.maxHeight = maxHeight;
        if (maxHeight > 256) {
            tall = true;
        }
        if (init) {
            terrain = new byte[TILE_SIZE * TILE_SIZE];
            if (maxHeight > 256) {
                tallHeightMap = new int[TILE_SIZE * TILE_SIZE];
                tallWaterLevel = new short[TILE_SIZE * TILE_SIZE];
            } else {
                heightMap = new short[TILE_SIZE * TILE_SIZE];
                waterLevel = new byte[TILE_SIZE * TILE_SIZE];
            }
            layerData = new HashMap<>();
            bitLayerData = new HashMap<>();
            init();
        }
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public synchronized int getMaxHeight() {
        return maxHeight;
    }
    
    public synchronized void setMaxHeight(int maxHeight, HeightTransform heightTransform) {
        if (maxHeight != this.maxHeight) {
            this.maxHeight = maxHeight;
            maxY = maxHeight - 1;
            boolean newTall = maxHeight > 256;
            if (newTall == tall) {
                // Tallness is not changing
                if (! heightTransform.isIdentity()) {
                    for (int x = 0; x < TILE_SIZE; x++) {
                        for (int y = 0; y < TILE_SIZE; y++) {
                            setHeight(x, y, clamp(heightTransform.transformHeight(getHeight(x, y))));
                            setWaterLevel(x, y, clamp(heightTransform.transformHeight(getWaterLevel(x, y))));
                        }
                    }
                } else {
                    for (int x = 0; x < TILE_SIZE; x++) {
                        for (int y = 0; y < TILE_SIZE; y++) {
                            setHeight(x, y, clamp(getHeight(x, y)));
                            setWaterLevel(x, y, clamp(getWaterLevel(x, y)));
                        }
                    }
                }
            } else if (tall) {
                // Going from tall to not tall
                heightMap = new short[TILE_SIZE * TILE_SIZE];
                waterLevel = new byte[TILE_SIZE * TILE_SIZE];
                if (undoManager != null) {
                    undoManager.addBuffer(HEIGHTMAP_BUFFER_KEY, heightMap, this);
                    undoManager.addBuffer(WATERLEVEL_BUFFER_KEY, waterLevel, this);
                    readableBuffers.add(HEIGHTMAP);
                    readableBuffers.add(WATERLEVEL);
                    writeableBuffers.add(HEIGHTMAP);
                    writeableBuffers.add(WATERLEVEL);
                }
                tall = false;
                for (int x = 0; x < TILE_SIZE; x++) {
                    for (int y = 0; y < TILE_SIZE; y++) {
                        setHeight(x, y, clamp(heightTransform.transformHeight(tallHeightMap[x | (y << TILE_SIZE_BITS)] / 256f)));
                        setWaterLevel(x, y, clamp(heightTransform.transformHeight(tallWaterLevel[x | (y << TILE_SIZE_BITS)])));
                    }
                }
                if (undoManager != null) {
                    undoManager.removeBuffer(TALL_HEIGHTMAP_BUFFER_KEY);
                    undoManager.removeBuffer(TALL_WATERLEVEL_BUFFER_KEY);
                    readableBuffers.remove(TALL_HEIGHTMAP);
                    readableBuffers.remove(TALL_WATERLEVEL);
                    writeableBuffers.remove(TALL_HEIGHTMAP);
                    writeableBuffers.remove(TALL_WATERLEVEL);
                }
                tallHeightMap = null;
                tallWaterLevel = null;
            } else {
                // Going from not tall to tall
                tallHeightMap = new int[TILE_SIZE * TILE_SIZE];
                tallWaterLevel = new short[TILE_SIZE * TILE_SIZE];
                if (undoManager != null) {
                    undoManager.addBuffer(TALL_HEIGHTMAP_BUFFER_KEY, tallHeightMap, this);
                    undoManager.addBuffer(TALL_WATERLEVEL_BUFFER_KEY, tallWaterLevel, this);
                    readableBuffers.add(TALL_HEIGHTMAP);
                    readableBuffers.add(TALL_WATERLEVEL);
                    writeableBuffers.add(TALL_HEIGHTMAP);
                    writeableBuffers.add(TALL_WATERLEVEL);
                }
                tall = true;
                for (int x = 0; x < TILE_SIZE; x++) {
                    for (int y = 0; y < TILE_SIZE; y++) {
                        setHeight(x, y, clamp(heightTransform.transformHeight((heightMap[x | (y << TILE_SIZE_BITS)] & 0xFFFF) / 256f)));
                        setWaterLevel(x, y, clamp(heightTransform.transformHeight(waterLevel[x | (y << TILE_SIZE_BITS)] & 0xFF)));
                    }
                }
                if (undoManager != null) {
                    undoManager.removeBuffer(HEIGHTMAP_BUFFER_KEY);
                    undoManager.removeBuffer(WATERLEVEL_BUFFER_KEY);
                    readableBuffers.remove(HEIGHTMAP);
                    readableBuffers.remove(WATERLEVEL);
                    writeableBuffers.remove(HEIGHTMAP);
                    writeableBuffers.remove(WATERLEVEL);
                }
                heightMap = null;
                waterLevel = null;
            }
            heightMapChanged();
            waterLevelChanged();
        } else if (! heightTransform.isIdentity()) {
            for (int x = 0; x < TILE_SIZE; x++) {
                for (int y = 0; y < TILE_SIZE; y++) {
                    setHeight(x, y, clamp(heightTransform.transformHeight(getHeight(x, y))));
                    setWaterLevel(x, y, clamp(heightTransform.transformHeight(getWaterLevel(x, y))));
                }
            }
            heightMapChanged();
            waterLevelChanged();
        }
    }

    public int getIntHeight(int x, int y) {
        return (int) (getHeight(x, y) + 0.5f);
    }
    
    public synchronized float getHeight(int x, int y) {
        if (tall) {
            ensureReadable(TALL_HEIGHTMAP);
            return tallHeightMap[x | (y << TILE_SIZE_BITS)] / 256f;
        } else {
            ensureReadable(HEIGHTMAP);
            return (heightMap[x | (y << TILE_SIZE_BITS)] & 0xFFFF) / 256f;
        }
    }

    public synchronized void setHeight(int x, int y, float height) {
        if (tall) {
            ensureWriteable(TALL_HEIGHTMAP);
            tallHeightMap[x | (y << TILE_SIZE_BITS)] = (int) (height * 256);
        } else {
            ensureWriteable(HEIGHTMAP);
            heightMap[x | (y << TILE_SIZE_BITS)] = (short) (height * 256);
        }
        heightMapChanged();
    }

    public synchronized int getRawHeight(int x, int y) {
        if (tall) {
            ensureReadable(TALL_HEIGHTMAP);
            return tallHeightMap[x | (y << TILE_SIZE_BITS)];
        } else {
            ensureReadable(HEIGHTMAP);
            return heightMap[x | (y << TILE_SIZE_BITS)] & 0xFFFF;
        }
    }

    public synchronized void setRawHeight(int x, int y, int rawHeight) {
        if (tall) {
            ensureWriteable(TALL_HEIGHTMAP);
            tallHeightMap[x | (y << TILE_SIZE_BITS)] = rawHeight;
        } else {
            ensureWriteable(HEIGHTMAP);
            heightMap[x | (y << TILE_SIZE_BITS)] = (short) rawHeight;
        }
        heightMapChanged();
    }
    
    public synchronized float getSlope(int x, int y) {
        return Math.max(Math.max(Math.abs(getHeight(x + 1, y) - getHeight(x - 1, y)) / 2,
            Math.abs(getHeight(x + 1, y + 1) - getHeight(x - 1, y - 1)) / SQRT_OF_EIGHT),
            Math.max(Math.abs(getHeight(x, y + 1) - getHeight(x, y - 1)) / 2,
            Math.abs(getHeight(x - 1, y + 1) - getHeight(x + 1, y - 1)) / SQRT_OF_EIGHT));
    }

    public synchronized Terrain getTerrain(int x, int y) {
        ensureReadable(TERRAIN);
        return TERRAIN_VALUES[terrain[x | (y << TILE_SIZE_BITS)] & 0xFF];
    }

    public synchronized void setTerrain(int x, int y, Terrain terrain) {
        ensureWriteable(TERRAIN);
        this.terrain[x | (y << TILE_SIZE_BITS)] = (byte) terrain.ordinal();
        terrainChanged();
    }

    public synchronized int getWaterLevel(int x, int y) {
        if (tall) {
            ensureReadable(TALL_WATERLEVEL);
            return tallWaterLevel[x | (y << TILE_SIZE_BITS)];
        } else {
            ensureReadable(WATERLEVEL);
            return waterLevel[x | (y << TILE_SIZE_BITS)] & 0xFF;
        }
    }

    public synchronized void setWaterLevel(int x, int y, int waterLevel) {
        if (tall) {
            ensureWriteable(TALL_WATERLEVEL);
            this.tallWaterLevel[x | (y << TILE_SIZE_BITS)] = (short) waterLevel;
        } else {
            ensureWriteable(WATERLEVEL);
            this.waterLevel[x | (y << TILE_SIZE_BITS)] = (byte) waterLevel;
        }
        waterLevelChanged();
    }

    public synchronized List<Layer> getLayers() {
        if (cachedLayers == null) {
            ensureReadable(LAYER_DATA);
            ensureReadable(BIT_LAYER_DATA);
            List<Layer> layers = new ArrayList<>();
            layers.addAll(layerData.keySet());
            layers.addAll(bitLayerData.keySet());
            Collections.sort(layers);
            cachedLayers = Collections.unmodifiableList(layers);
        }
        return cachedLayers;
    }

    public synchronized boolean hasLayer(Layer layer) {
        DataSize dataSize = layer.getDataSize();
        if ((dataSize == DataSize.BIT) || (dataSize == DataSize.BIT_PER_CHUNK)) {
            ensureReadable(BIT_LAYER_DATA);
            return bitLayerData.containsKey(layer);
        } else {
            ensureReadable(LAYER_DATA);
            return layerData.containsKey(layer);
        }
    }
    
    public List<Layer> getActiveLayers(int x, int y) {
        ensureReadable(BIT_LAYER_DATA);
        ensureReadable(LAYER_DATA);
        List<Layer> activeLayers = new ArrayList<>(bitLayerData.size() + layerData.size());
        for (Map.Entry<Layer, BitSet> entry: bitLayerData.entrySet()) {
            final Layer layer = entry.getKey();
            final DataSize dataSize = layer.getDataSize();
            if (((dataSize == DataSize.BIT) && getBitPerBlockLayerValue(entry.getValue(), x, y))
                || ((dataSize == DataSize.BIT_PER_CHUNK) && getBitPerChunkLayerValue(entry.getValue(), x, y))) {
                activeLayers.add(layer);
            }
        }
        for (Map.Entry<Layer, byte[]> entry: layerData.entrySet()) {
            final Layer layer = entry.getKey();
            final DataSize dataSize = layer.getDataSize();
            final int defaultValue = layer.getDefaultValue();
            if (dataSize == DataSize.NIBBLE) {
                final int byteOffset = x | (y << TILE_SIZE_BITS);
                final byte _byte = entry.getValue()[byteOffset / 2];
                if ((byteOffset % 2 == 0) ? ((_byte & 0x0F) != defaultValue) : (((_byte & 0xF0) >> 4) != defaultValue)) {
                    activeLayers.add(layer);
                }
            } else if ((entry.getValue()[x | (y << TILE_SIZE_BITS)] & 0xFF) != defaultValue) {
                activeLayers.add(layer);
            }
        }
        return activeLayers;
    }

    /**
     * Get a list of all layers in use in the tile, as well as the set of
     * additional layers provided, the total sorted by layer priority.
     * 
     * @param additionalLayers The additional layers to include in the list.
     * @return The list of all layers provided or in use on the tile, sorted by
     *     layer priority.
     */
    public List<Layer> getLayers(Set<Layer> additionalLayers) {
        SortedSet<Layer> layers = new TreeSet<>(additionalLayers);
        layers.addAll(getLayers());
        return new ArrayList<>(layers);
    }
    
    public synchronized boolean getBitLayerValue(Layer layer, int x, int y) {
        if ((layer.getDataSize() != Layer.DataSize.BIT) && (layer.getDataSize() != Layer.DataSize.BIT_PER_CHUNK)) {
            throw new IllegalArgumentException("Layer is not bit sized");
        }
        ensureReadable(BIT_LAYER_DATA);
        BitSet bitSet = bitLayerData.get(layer);
        if (bitSet == null) {
            return false;
        } else {
            if (layer.getDataSize() == Layer.DataSize.BIT) {
                return getBitPerBlockLayerValue(bitSet, x, y);
            } else {
                return getBitPerChunkLayerValue(bitSet, x, y);
            }
        }
    }

    /**
     * Count the number of blocks where the specified bit layer is set in a
     * square around a particular location
     * 
     * @param layer The bit layer to count.
     * @param x The X coordinate (local to the tile) of the location around
     *     which to count the layer.
     * @param y The Y coordinate (local to the tile) of the location around
     *     which to count the layer.
     * @param r The radius of the square.
     * @return The number of blocks in the specified square where the specified
     *     bit layer is set.
     */
    public synchronized int getBitLayerCount(Layer layer, int x, int y, int r) {
        if ((layer.getDataSize() != Layer.DataSize.BIT) && (layer.getDataSize() != Layer.DataSize.BIT_PER_CHUNK)) {
            throw new IllegalArgumentException("Layer is not bit sized");
        }
        if (((x - r) < 0) || ((x + r) >= TILE_SIZE) || ((y - r) < 0) || ((y + r) >= TILE_SIZE)) {
            throw new IllegalArgumentException("Requested area not contained entirely on tile");
        }
        ensureReadable(BIT_LAYER_DATA);
        BitSet bitSet = bitLayerData.get(layer);
        if (bitSet == null) {
            return 0;
        } else {
            boolean bitPerChunk = layer.getDataSize() == Layer.DataSize.BIT_PER_CHUNK;
            int count = 0, bitOffset;
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    if (bitPerChunk) {
                        bitOffset = ((x + dx) / 16) + ((y + dy) / 16) * (TILE_SIZE / 16);
                    } else {
                        bitOffset = x + dx + (y + dy) * TILE_SIZE;
                    }
                    if (bitSet.get(bitOffset)) {
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
     *     their intensities at that location. May either be <code>null</code>
     *     or an empty map if no layers are present.
     */
    public Map<Layer, Integer> getLayersAt(int x, int y) {
        Map<Layer, Integer> layers = null;
        ensureReadable(LAYER_DATA);
        for (Map.Entry<Layer, byte[]> entry: layerData.entrySet()) {
            Layer layer = entry.getKey();
            byte[] layerValues = entry.getValue();
            int value;
            if (layer.getDataSize() == DataSize.NIBBLE) {
                int byteOffset = x | (y << TILE_SIZE_BITS);
                byte _byte = layerValues[byteOffset / 2];
                if (byteOffset % 2 == 0) {
                    value = _byte & 0x0F;
                } else {
                    value = (_byte & 0xF0) >> 4;
                }
            } else {
                value = layerValues[x | (y << TILE_SIZE_BITS)] & 0xFF;
            }
            if (value != layer.getDefaultValue()) {
                if (layers == null) {
                    layers = new HashMap<>();
                }
                layers.put(layer, value);
            }
        }
        ensureReadable(BIT_LAYER_DATA);
        for (Map.Entry<Layer, BitSet> entry: bitLayerData.entrySet()) {
            Layer layer = entry.getKey();
            BitSet layerValues = entry.getValue();
            int value;
            if (layer.getDataSize() == Layer.DataSize.BIT) {
                value = layerValues.get(x | (y << TILE_SIZE_BITS)) ? 1 : 0;
            } else {
                value = layerValues.get((x >> 4) + (y >> 4) * (TILE_SIZE >> 4)) ? 1 : 0;
            }
            if (value != layer.getDefaultValue()) {
                if (layers == null) {
                    layers = new HashMap<>();
                }
                layers.put(layer, value);
            }
        }
        return layers;
    }

    /**
     * Count the number of blocks that are flooded in a square around a
     * particular location
     * 
     * @param x The X coordinate (local to the tile) of the location around
     *     which to count flooded blocks.
     * @param y The Y coordinate (local to the tile) of the location around
     *     which to count flooded blocks.
     * @param r The radius of the square.
     * @param lava Whether to check for lava (when <code>true</code>) or water
     *     (when <code>false</code>).
     * @return The number of blocks in the specified square that are flooded.
     */
    public synchronized int getFloodedCount(final int x, final int y, final int r, final boolean lava) {
        if (((x - r) < 0) || ((x + r) >= TILE_SIZE) || ((y - r) < 0) || ((y + r) >= TILE_SIZE)) {
            throw new IllegalArgumentException("Requested area not contained entirely on tile");
        }
        if (tall) {
            ensureReadable(TALL_HEIGHTMAP);
            ensureReadable(TALL_WATERLEVEL);
            ensureReadable(BIT_LAYER_DATA);
            final BitSet floodWithLava = bitLayerData.get(FloodWithLava.INSTANCE);
            int count = 0;
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    final int xx = x + dx, yy = y + dy;
                    if (((tallWaterLevel[xx + yy * TILE_SIZE]) > ((int) (tallHeightMap[xx + yy * TILE_SIZE] / 256f + 0.5f)))
                            && (lava ? ((floodWithLava != null) && getBitPerBlockLayerValue(floodWithLava, xx, yy))
                                : ((floodWithLava == null) || (! getBitPerBlockLayerValue(floodWithLava, xx, yy))))) {
                        count++;
                    }
                }
            }
            return count;
        } else {
            ensureReadable(HEIGHTMAP);
            ensureReadable(WATERLEVEL);
            ensureReadable(BIT_LAYER_DATA);
            final BitSet floodWithLava = bitLayerData.get(FloodWithLava.INSTANCE);
            int count = 0;
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    final int xx = x + dx, yy = y + dy;
                    if (((waterLevel[xx + yy * TILE_SIZE] & 0xFF) > ((int) ((heightMap[xx + yy * TILE_SIZE] & 0xFFFF) / 256f + 0.5f)))
                            && (lava ? ((floodWithLava != null) && getBitPerBlockLayerValue(floodWithLava, xx, yy))
                                : ((floodWithLava == null) || (! getBitPerBlockLayerValue(floodWithLava, xx, yy))))) {
                        count++;
                    }
                }
            }
            return count;
        }
    }
    
    public synchronized float getDistanceToEdge(final Layer layer, final int x, final int y, final float maxDistance) {
        if ((layer.getDataSize() != Layer.DataSize.BIT) && (layer.getDataSize() != Layer.DataSize.BIT_PER_CHUNK)) {
            throw new IllegalArgumentException("Layer is not bit sized");
        }
        int r = (int) Math.ceil(maxDistance);
        if (((x - r) < 0) || ((x + r) >= TILE_SIZE) || ((y - r) < 0) || ((y + r) >= TILE_SIZE)) {
            throw new IllegalArgumentException("Requested area not contained entirely on tile");
        }
        ensureReadable(BIT_LAYER_DATA);
        BitSet bitSet = bitLayerData.get(layer);
        if (bitSet == null) {
            return 0;
        } else {
            float distance = maxDistance;
            if (layer.getDataSize() == DataSize.BIT) {
                if (! getBitPerBlockLayerValue(bitSet, x, y)) {
                    return 0;
                }
                for (int i = 1; i <= r; i++) {
                    if (((! getBitPerBlockLayerValue(bitSet, x - i, y))
                                || (! getBitPerBlockLayerValue(bitSet, x + i, y))
                                || (! getBitPerBlockLayerValue(bitSet, x, y - i))
                                || (! getBitPerBlockLayerValue(bitSet, x, y + i)))
                            && (i < distance)) {
                        // If we get here there's no possible way a shorter
                        // distance could be found later, so return immediately
                        return i;
                    }
                    for (int d = 1; d <= i; d++) {
                        if ((! getBitPerBlockLayerValue(bitSet, x - i, y - d))
                                || (! getBitPerBlockLayerValue(bitSet, x + d, y - i))
                                || (! getBitPerBlockLayerValue(bitSet, x + i, y + d))
                                || (! getBitPerBlockLayerValue(bitSet, x - d, y + i))
                                || ((d < i) && ((! getBitPerBlockLayerValue(bitSet, x - i, y + d))
                                    || (! getBitPerBlockLayerValue(bitSet, x - d, y - i))
                                    || (! getBitPerBlockLayerValue(bitSet, x + i, y - d))
                                    || (! getBitPerBlockLayerValue(bitSet, x + d, y + i))))) {
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
            } else {
                if (! getBitPerChunkLayerValue(bitSet, x, y)) {
                    return 0;
                }
                for (int i = 1; i <= r; i++) {
                    if (((! getBitPerChunkLayerValue(bitSet, x - i, y))
                                || (! getBitPerChunkLayerValue(bitSet, x + i, y))
                                || (! getBitPerChunkLayerValue(bitSet, x, y - i))
                                || (! getBitPerChunkLayerValue(bitSet, x, y + i)))
                            && (i < distance)) {
                        // If we get here there's no possible way a shorter
                        // distance could be found later, so return immediately
                        return i;
                    }
                    for (int d = 1; d <= i; d++) {
                        if ((! getBitPerChunkLayerValue(bitSet, x - i, y - d))
                                || (! getBitPerChunkLayerValue(bitSet, x + d, y - i))
                                || (! getBitPerChunkLayerValue(bitSet, x + i, y + d))
                                || (! getBitPerChunkLayerValue(bitSet, x - d, y + i))
                                || ((d < i) && ((! getBitPerChunkLayerValue(bitSet, x - i, y + d))
                                    || (! getBitPerChunkLayerValue(bitSet, x - d, y - i))
                                    || (! getBitPerChunkLayerValue(bitSet, x + i, y - d))
                                    || (! getBitPerChunkLayerValue(bitSet, x + d, y + i))))) {
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
            }
            return distance;
        }
    }

    public synchronized void setBitLayerValue(Layer layer, int x, int y, boolean value) {
        if ((layer.getDataSize() != Layer.DataSize.BIT) && (layer.getDataSize() != Layer.DataSize.BIT_PER_CHUNK)) {
            throw new IllegalArgumentException("Layer is not bit sized");
        }
        ensureWriteable(BIT_LAYER_DATA);
        BitSet bitSet = bitLayerData.get(layer);
        if (bitSet == null) {
            if (value) {
                cachedLayers = null;
                if (layer.getDataSize() == Layer.DataSize.BIT) {
                    bitSet = new BitSet(TILE_SIZE * TILE_SIZE);
                } else {
                    bitSet = new BitSet(TILE_SIZE * TILE_SIZE / 256);
                }
                bitLayerData.put(layer, bitSet);
            } else {
                // If there is no bitset the default value is false, so if we're
                // setting to false anyway there's no point in creating the
                // bitset
                return;
            }
        }
        int bitOffset;
        if (layer.getDataSize() == Layer.DataSize.BIT) {
            bitOffset = x | (y << TILE_SIZE_BITS);
        } else {
            bitOffset = (x / 16) + (y / 16) * (TILE_SIZE / 16);
        }
        bitSet.set(bitOffset, value);
        layerDataChanged(layer);
    }

    public synchronized int getLayerValue(Layer layer, int x, int y) {
        ensureReadable(LAYER_DATA);
        byte[] layerValues = layerData.get(layer);
        if (layerValues == null) {
            return layer.getDefaultValue();
        } else {
            switch (layer.getDataSize()) {
                case BIT:
                case BIT_PER_CHUNK:
                    throw new IllegalArgumentException("Can't get bits using this method");
                case NIBBLE:
                    int byteOffset = x | (y << TILE_SIZE_BITS);
                    byte _byte = layerValues[byteOffset / 2];
                    if (byteOffset % 2 == 0) {
                        return _byte & 0x0F;
                    } else {
                        return (_byte & 0xF0) >> 4;
                    }
                case BYTE:
                    byteOffset = x | (y << TILE_SIZE_BITS);
                    return layerValues[byteOffset] & 0xFF;
                default:
                    throw new InternalError();
            }
        }
    }

    public synchronized void setLayerValue(Layer layer, int x, int y, int value) {
        ensureWriteable(LAYER_DATA);
        byte[] layerValues = layerData.get(layer);
        if (layerValues == null) {
            if (value == layer.getDefaultValue()) {
                // There is no data buffer and we're setting the value to the
                // default, so we don't need to create it
                return;
            }
            cachedLayers = null;
            switch (layer.getDataSize()) {
                case BIT:
                case BIT_PER_CHUNK:
                    throw new IllegalArgumentException("Can't set bits using this method");
                case NIBBLE:
                    layerValues = new byte[TILE_SIZE * TILE_SIZE / 2];
                    if (layer.getDefaultValue() != 0) {
                        byte defaultValue = (byte) (layer.getDefaultValue() << 4 | layer.getDefaultValue());
                        Arrays.fill(layerValues, defaultValue);
                    }
                    break;
                case BYTE:
                    layerValues = new byte[TILE_SIZE * TILE_SIZE];
                    if (layer.getDefaultValue() != 0) {
                        byte defaultValue = (byte) layer.getDefaultValue();
                        Arrays.fill(layerValues, defaultValue);
                    }
                    break;
                default:
                    throw new InternalError();
            }
            layerData.put(layer, layerValues);
        }
        switch (layer.getDataSize()) {
            case BIT:
            case BIT_PER_CHUNK:
                throw new IllegalArgumentException("Can't set bits using this method");
            case NIBBLE:
                if ((value < 0) || (value > 15)) {
                    throw new IllegalArgumentException("Illegal value for nibble sized layer: " + value);
                }
                int byteOffset = x | (y << TILE_SIZE_BITS);
                byte _byte = layerValues[byteOffset / 2];
                if (byteOffset % 2 == 0) {
                    _byte &= 0xF0;
                    _byte |= value;
                } else {
                    _byte &= 0x0F;
                    _byte |= (value << 4);
                }
                layerValues[byteOffset / 2] = _byte;
                break;
            case BYTE:
                if ((value < 0) || (value > 255)) {
                    throw new IllegalArgumentException("Illegal value for byte sized layer: " + value);
                }
                byteOffset = x | (y << TILE_SIZE_BITS);
                layerValues[byteOffset] = (byte) value;
                break;
            default:
                throw new InternalError();
        }
        layerDataChanged(layer);
    }

    public synchronized void clearLayerData(Layer layer) {
        if ((layer.getDataSize() == Layer.DataSize.BIT) || (layer.getDataSize() == Layer.DataSize.BIT_PER_CHUNK)) {
            ensureReadable(BIT_LAYER_DATA);
            if (bitLayerData.containsKey(layer)) {
                ensureWriteable(BIT_LAYER_DATA);
                bitLayerData.remove(layer);
                layerDataChanged(layer);
                cachedLayers = null;
            }
        } else {
            ensureReadable(LAYER_DATA);
            if (layerData.containsKey(layer)) {
                ensureWriteable(LAYER_DATA);
                layerData.remove(layer);
                layerDataChanged(layer);
                cachedLayers = null;
            }
        }
    }

    /**
     * Clear all layer data at a particular location (by resetting to the
     * layer's default value), possibly with the exception of certain layers.
     *
     * @param x The X coordinate of the location to clear of layer data.
     * @param y The Y coordinate of the location to clear of layer data.
     * @param excludedLayers The layers to exclude, if any. May be
     *                       <code>null</code>.
     */
    public void clearLayerData(int x, int y, Set<Layer> excludedLayers) {
        ensureWriteable(BIT_LAYER_DATA);
        for (Map.Entry<Layer, BitSet> entry: bitLayerData.entrySet()) {
            Layer layer = entry.getKey();
            if ((excludedLayers != null) && excludedLayers.contains(layer)) {
                continue;
            }
            int bitOffset;
            if (layer.getDataSize() == Layer.DataSize.BIT) {
                bitOffset = x | (y << TILE_SIZE_BITS);
            } else {
                bitOffset = (x / 16) + (y / 16) * (TILE_SIZE / 16);
            }
            entry.getValue().set(bitOffset, layer.getDefaultValue() != 0);
            layerDataChanged(layer);
        }
        ensureWriteable(LAYER_DATA);
        for (Map.Entry<Layer, byte[]> entry: layerData.entrySet()) {
            Layer layer = entry.getKey();
            if ((excludedLayers != null) && excludedLayers.contains(layer)) {
                continue;
            }
            byte[] layerValues = entry.getValue();
            switch (layer.getDataSize()) {
                case NIBBLE:
                    int byteOffset = x | (y << TILE_SIZE_BITS);
                    byte _byte = layerValues[byteOffset / 2];
                    if (byteOffset % 2 == 0) {
                        _byte &= 0xF0;
                        _byte |= layer.getDefaultValue();
                    } else {
                        _byte &= 0x0F;
                        _byte |= (layer.getDefaultValue() << 4);
                    }
                    layerValues[byteOffset / 2] = _byte;
                    break;
                case BYTE:
                    byteOffset = x | (y << TILE_SIZE_BITS);
                    layerValues[byteOffset] = (byte) layer.getDefaultValue();
                    break;
                default:
                    throw new InternalError();
            }
            layerDataChanged(layer);
        }
    }

    public synchronized HashSet<Seed> getSeeds() {
        if (seeds != null) {
            ensureReadable(SEEDS);
            return seeds;
        } else {
            return null;
        }
    }

    public synchronized boolean plantSeed(Seed seed) {
        if (seeds == null) {
            seeds = new HashSet<>();
            if (undoManager != null) {
                undoManager.addBuffer(SEEDS_BUFFER_KEY, seeds, this);
                readableBuffers.add(SEEDS);
                writeableBuffers.add(SEEDS);
            }
        } else {
            ensureWriteable(SEEDS);
        }
        seeds.add(seed);
        seedsChanged();
        return true;
    }

    public synchronized void removeSeed(Seed seed) {
        if (seeds == null) {
            seeds = new HashSet<>();
            if (undoManager != null) {
                undoManager.addBuffer(SEEDS_BUFFER_KEY, seeds, this);
                readableBuffers.add(SEEDS);
                writeableBuffers.add(SEEDS);
            }
        } else {
            ensureWriteable(SEEDS);
        }
        seeds.remove(seed);
        seedsChanged();
    }

    public synchronized void addListener(Listener listener) {
        listeners.add(listener);
    }

    public synchronized void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public synchronized boolean isEventsInhibited() {
        return eventInhibitionCounter != 0;
    }

    /**
     * Stop firing events when the tile is modified, until {@link #releaseEvents()} is invoked. Make sure that
     * <code>releaseEvents()</code> is always invoked, even if an exception is thrown, by using a try-finally statement:
     *
     * <p><code>tile.inhibitEvents();<br>
     * try {<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;// modify the tile<br>
     * } finally {<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;tile.releaseEvents();<br>
     * }</code>
     *
     * <p><strong>Note</strong> that calls to these methods may be nested, and if so, events will only be released after
     * the final invocation of <code>releaseEvents()</code>.
     */
    public synchronized void inhibitEvents() {
        eventInhibitionCounter++;
    }

    /**
     * Release an inhibition on firing events. Will fire all appropriate events at this time, if the tile was modified
     * since the first invocation of {@link #inhibitEvents()}, but only if this is the last invocation of
     * <code>releaseEvents()</code> in a nested set.
     */
    public synchronized void releaseEvents() {
        if (eventInhibitionCounter > 0) {
            eventInhibitionCounter--;
            if (eventInhibitionCounter == 0) {
                if (heightMapDirty) {
                    heightMapChanged();
                    heightMapDirty = false;
                }
                if (terrainDirty) {
                    terrainChanged();
                    terrainDirty = false;
                }
                if (waterLevelDirty) {
                    waterLevelChanged();
                    waterLevelDirty = false;
                }
                if (bitLayersDirty) {
                    allBitLayerDataChanged();
                    bitLayersDirty = false;
                    for (Iterator<Layer> i = dirtyLayers.iterator(); i.hasNext(); ) {
                        DataSize dataSize = i.next().getDataSize();
                        if ((dataSize == DataSize.BIT) || (dataSize == DataSize.BIT_PER_CHUNK)) {
                            i.remove();
                        }
                    }
                }
                if (nonBitLayersDirty) {
                    allNonBitLayerDataChanged();
                    nonBitLayersDirty = false;
                    for (Iterator<Layer> i = dirtyLayers.iterator(); i.hasNext(); ) {
                        DataSize dataSize = i.next().getDataSize();
                        if ((dataSize != DataSize.BIT) && (dataSize != DataSize.BIT_PER_CHUNK)) {
                            i.remove();
                        }
                    }
                }
                if (! dirtyLayers.isEmpty()) {
                    Set<Layer> changedLayers = Collections.unmodifiableSet(dirtyLayers);
                    for (Listener listener: listeners) {
                        listener.layerDataChanged(this, changedLayers);
                    }
                    dirtyLayers.clear();
                }
                if (seedsDirty) {
                    seedsChanged();
                    seedsDirty = false;
                }
            }
        } else {
            throw new IllegalStateException("Events not inhibited");
        }
    }

    public synchronized void register(UndoManager undoManager) {
        this.undoManager = undoManager;
        registerUndoBuffers();
        undoManager.addListener(this);
    }

    public synchronized void unregister() {
        if (undoManager != null) {
            undoManager.removeListener(this);
            unregisterUndoBuffers();
            undoManager = null;
        }
    }

    /**
     * Create a new tile based on this one but horizontally transformed according to some transformation.
     *
     * @param transform The transform to apply.
     * @return A new tile with the same contents, except transformed according to the specified transform (including the
     * X and Y coordinates).
     */
    public synchronized Tile transform(CoordinateTransform transform) {
        Point transformedCoords = transform.transform(x << TILE_SIZE_BITS, y << TILE_SIZE_BITS);
        Tile transformedTile;
        boolean transformContents = ((transformedCoords.x & TILE_SIZE_MASK) != 0) || ((transformedCoords.y & TILE_SIZE_MASK) != 0);
        if (transformContents) {
            transformedTile = new Tile(transformedCoords.x >> TILE_SIZE_BITS, transformedCoords.y >> TILE_SIZE_BITS, maxHeight);
            for (int x = 0; x < TILE_SIZE; x++) {
                for (int y = 0; y < TILE_SIZE; y++) {
                    transformedCoords.x = x;
                    transformedCoords.y = y;
                    transform.transformInPlace(transformedCoords);
                    transformedCoords.x &= TILE_SIZE_MASK;
                    transformedCoords.y &= TILE_SIZE_MASK;
                    transformedTile.setTerrain(transformedCoords.x, transformedCoords.y, getTerrain(x, y));
                    transformedTile.setRawHeight(transformedCoords.x, transformedCoords.y, getRawHeight(x, y));
                    transformedTile.setWaterLevel(transformedCoords.x, transformedCoords.y, getWaterLevel(x, y));
                }
            }
            for (Layer layer: getLayers()) {
                if ((layer.getDataSize() == Layer.DataSize.BIT) || (layer.getDataSize() == Layer.DataSize.BIT_PER_CHUNK)) {
                    for (int x = 0; x < TILE_SIZE; x++) {
                        for (int y = 0; y < TILE_SIZE; y++) {
                            if (getBitLayerValue(layer, x, y)) {
                                transformedCoords.x = x;
                                transformedCoords.y = y;
                                transform.transformInPlace(transformedCoords);
                                transformedCoords.x &= TILE_SIZE_MASK;
                                transformedCoords.y &= TILE_SIZE_MASK;
                                transformedTile.setBitLayerValue(layer, transformedCoords.x, transformedCoords.y, true);
                            }
                        }
                    }
                } else if (layer.getDataSize() != Layer.DataSize.NONE) {
                    for (int x = 0; x < TILE_SIZE; x++) {
                        for (int y = 0; y < TILE_SIZE; y++) {
                            int value = getLayerValue(layer, x, y);
                            if (value > 0) {
                                transformedCoords.x = x;
                                transformedCoords.y = y;
                                transform.transformInPlace(transformedCoords);
                                transformedCoords.x &= TILE_SIZE_MASK;
                                transformedCoords.y &= TILE_SIZE_MASK;
                                transformedTile.setLayerValue(layer, transformedCoords.x, transformedCoords.y, value);
                            }
                        }
                    }
                }
            }
        } else {
            // The transformation does not affect intra-tile coordinates, so just copy the buffers without transforming them
            transformedTile = new Tile(transformedCoords.x >> TILE_SIZE_BITS, transformedCoords.y >> TILE_SIZE_BITS, maxHeight, false);
            transformedTile.heightMap = copyObject(heightMap);
            transformedTile.tallHeightMap = copyObject(tallHeightMap);
            transformedTile.terrain = terrain.clone();
            transformedTile.waterLevel = copyObject(waterLevel);
            transformedTile.tallWaterLevel = copyObject(tallWaterLevel);
            transformedTile.layerData = copyObject(layerData);
            transformedTile.bitLayerData = copyObject(bitLayerData);
            transformedTile.init();
        }
        if (seeds != null) {
            transformedTile.seeds = new HashSet<>();
            transformedTile.seeds.addAll(seeds);
            for (Seed seed: transformedTile.seeds) {
                seed.transform(transform);
            }
        }
        return transformedTile;
    }
    
    public boolean repair(int maxHeight, PrintStream out) {
        // Repair as much as possible if the tile was not read in completely
        this.maxHeight = maxHeight;
        maxY = maxHeight - 1;
        if (maxHeight > 256) {
            tall = true;
            if (tallHeightMap == null) {
                out.println("Height map for tile " + x + "," + y + " lost");
                tallHeightMap = new int[TILE_SIZE * TILE_SIZE];
            }
            if (tallWaterLevel == null) {
                out.println("Water level map for tile " + x + "," + y + " lost");
                tallWaterLevel = new short[TILE_SIZE * TILE_SIZE];
            }
            heightMap = null;
            waterLevel = null;
        } else {
            tall = false;
            if (heightMap == null) {
                out.println("Height map for tile " + x + "," + y + " lost");
                heightMap = new short[TILE_SIZE * TILE_SIZE];
            }
            if (waterLevel == null) {
                out.println("Water level map for tile " + x + "," + y + " lost");
                waterLevel = new byte[TILE_SIZE * TILE_SIZE];
            }
            tallHeightMap = null;
            tallWaterLevel = null;
        }
        if (terrain == null) {
            out.println("Terrain type map for tile " + x + "," + y + " lost");
            terrain = new byte[TILE_SIZE * TILE_SIZE];
        }
        if (layerData == null) {
            out.println("Non-bit valued layer data for tile " + x + "," + y + " lost");
            layerData = new HashMap<>();
        }
        if (bitLayerData == null) {
            out.println("Bit valued layer data for tile " + x + "," + y + " lost");
            bitLayerData = new HashMap<>();
        }
        init();
        return true;
    }

    public boolean containsOneOf(Layer... layers) {
        boolean bitLayersAvailable = false, nonBitLayersAvailable = false;
        for (Layer layer: layers) {
            switch (layer.getDataSize()) {
                case BIT:
                case BIT_PER_CHUNK:
                    if (! bitLayersAvailable) {
                        ensureReadable(BIT_LAYER_DATA);
                        bitLayersAvailable = true;
                    }
                    if (bitLayerData.containsKey(layer)) {
                        return true;
                    }
                    break;
                case BYTE:
                case NIBBLE:
                    if (! nonBitLayersAvailable) {
                        ensureReadable(LAYER_DATA);
                        nonBitLayersAvailable = true;
                    }
                    if (layerData.containsKey(layer)) {
                        return true;
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Data size " + layer.getDataSize() + " not supported");
            }
        }
        return false;
    }

    // UndoListener

    @Override
    public synchronized void savePointArmed() {
        if (logger.isTraceEnabled()) {
            logger.trace("Save point armed; clearing writable buffers");
        }
        writeableBuffers.clear();
    }

    @Override
    public synchronized void savePointCreated() {
        if (logger.isTraceEnabled()) {
            logger.trace("Save point created; clearing writable buffers");
        }
        writeableBuffers.clear();
    }

    @Override
    public void undoPerformed() {
        // Do nothing
    }

    @Override
    public void redoPerformed() {
        // Do nothing
    }

    @Override
    public synchronized void bufferChanged(BufferKey<?> key) {
        TileUndoBufferKey<?> tileKey = (TileUndoBufferKey<?>) key;
        if (logger.isTraceEnabled()) {
            logger.trace("Buffer " + key + " changed; clearing buffer cache for type " + tileKey.buffer + " and notifying listeners");
        }
        switch (tileKey.buffer) {
            case BIT_LAYER_DATA:
                readableBuffers.remove(BIT_LAYER_DATA);
                writeableBuffers.remove(BIT_LAYER_DATA);
                allBitLayerDataChanged();
                cachedLayers = null;
                break;
            case HEIGHTMAP:
                readableBuffers.remove(HEIGHTMAP);
                writeableBuffers.remove(HEIGHTMAP);
                heightMapChanged();
                break;
            case TALL_HEIGHTMAP:
                readableBuffers.remove(TALL_HEIGHTMAP);
                writeableBuffers.remove(TALL_HEIGHTMAP);
                heightMapChanged();
                break;
            case LAYER_DATA:
                readableBuffers.remove(LAYER_DATA);
                writeableBuffers.remove(LAYER_DATA);
                allNonBitLayerDataChanged();
                cachedLayers = null;
                break;
            case TERRAIN:
                readableBuffers.remove(TERRAIN);
                writeableBuffers.remove(TERRAIN);
                terrainChanged();
                break;
            case WATERLEVEL:
                readableBuffers.remove(WATERLEVEL);
                writeableBuffers.remove(WATERLEVEL);
                waterLevelChanged();
                break;
            case TALL_WATERLEVEL:
                readableBuffers.remove(TALL_WATERLEVEL);
                writeableBuffers.remove(TALL_WATERLEVEL);
                waterLevelChanged();
                break;
            case SEEDS:
                readableBuffers.remove(SEEDS);
                writeableBuffers.remove(SEEDS);
                seedsChanged();
                break;
        }
    }

    // Object

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Tile other = (Tile) obj;
        if (this.x != other.x) {
            return false;
        }
        if (this.y != other.y) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + this.x;
        hash = 17 * hash + this.y;
        return hash;
    }

    @Override
    public String toString() {
        return "Tile[x=" + x + ",y=" + y + "]";
    }

    synchronized void ensureAllReadable() {
        if (tall) {
            ensureReadable(TALL_HEIGHTMAP);
            ensureReadable(TALL_WATERLEVEL);
        } else {
            ensureReadable(HEIGHTMAP);
            ensureReadable(WATERLEVEL);
        }
        ensureReadable(TERRAIN);
        ensureReadable(LAYER_DATA);
        ensureReadable(BIT_LAYER_DATA);
        ensureReadable(SEEDS);
    }

    void convertBiomeData() {
        byte[] biomeData = layerData.remove(Biome.INSTANCE);
        byte[] newBiomeData = new byte[biomeData.length * 2];
        for (int i = 0; i < biomeData.length; i++) {
            newBiomeData[i * 2] = (byte) (biomeData[i] & 0x0f);
            newBiomeData[i * 2 + 1] = (byte) ((biomeData[i] & 0xf0) >> 4);
        }
        layerData.put(Biome.INSTANCE, newBiomeData);
    }

    private boolean getBitPerBlockLayerValue(BitSet bitSet, int x, int y) {
        return bitSet.get(x | (y << TILE_SIZE_BITS));
    }

    private boolean getBitPerChunkLayerValue(BitSet bitSet, int x, int y) {
        return bitSet.get((x >> 4) + (y >> 4) * (TILE_SIZE >> 4));
    }

    private void registerUndoBuffers() {
        if (tall) {
            undoManager.addBuffer(TALL_HEIGHTMAP_BUFFER_KEY,  tallHeightMap,  this);
            undoManager.addBuffer(TALL_WATERLEVEL_BUFFER_KEY, tallWaterLevel, this);
            readableBuffers = EnumSet.of(TALL_HEIGHTMAP, TALL_WATERLEVEL, TERRAIN, LAYER_DATA, BIT_LAYER_DATA);
            writeableBuffers = EnumSet.of(TALL_HEIGHTMAP, TALL_WATERLEVEL, TERRAIN, LAYER_DATA, BIT_LAYER_DATA);
        } else {
            undoManager.addBuffer(HEIGHTMAP_BUFFER_KEY,  heightMap,  this);
            undoManager.addBuffer(WATERLEVEL_BUFFER_KEY, waterLevel, this);
            readableBuffers = EnumSet.of(HEIGHTMAP, WATERLEVEL, TERRAIN, LAYER_DATA, BIT_LAYER_DATA);
            writeableBuffers = EnumSet.of(HEIGHTMAP, WATERLEVEL, TERRAIN, LAYER_DATA, BIT_LAYER_DATA);
        }
        undoManager.addBuffer(TERRAIN_BUFFER_KEY,        terrain,      this);
        undoManager.addBuffer(LAYER_DATA_BUFFER_KEY,     layerData,    this);
        undoManager.addBuffer(BIT_LAYER_DATA_BUFFER_KEY, bitLayerData, this);
        if (seeds != null) {
            undoManager.addBuffer(SEEDS_BUFFER_KEY, seeds, this);
            readableBuffers.add(SEEDS);
            writeableBuffers.add(SEEDS);
        }
    }

    private void unregisterUndoBuffers() {
        // Also make sure that we have all the data from the current undo level
        // references, because after this we can't get at it any more
        if (tall) {
            ensureReadable(TALL_HEIGHTMAP);
            undoManager.removeBuffer(TALL_HEIGHTMAP_BUFFER_KEY);
            ensureReadable(TALL_WATERLEVEL);
            undoManager.removeBuffer(TALL_WATERLEVEL_BUFFER_KEY);
        } else {
            ensureReadable(HEIGHTMAP);
            undoManager.removeBuffer(HEIGHTMAP_BUFFER_KEY);
            ensureReadable(WATERLEVEL);
            undoManager.removeBuffer(WATERLEVEL_BUFFER_KEY);
        }
        ensureReadable(TERRAIN);
        undoManager.removeBuffer(TERRAIN_BUFFER_KEY);
        ensureReadable(LAYER_DATA);
        undoManager.removeBuffer(LAYER_DATA_BUFFER_KEY);
        ensureReadable(BIT_LAYER_DATA);
        undoManager.removeBuffer(BIT_LAYER_DATA_BUFFER_KEY);
        if (seeds != null) {
            ensureReadable(SEEDS);
            undoManager.removeBuffer(SEEDS_BUFFER_KEY);
        }
        readableBuffers = writeableBuffers = null;
    }

    protected synchronized void ensureReadable(TileBuffer buffer) {
        if ((undoManager != null) && (! readableBuffers.contains(buffer))) {
            switch (buffer) {
                case HEIGHTMAP:
                    heightMap = undoManager.getBuffer(HEIGHTMAP_BUFFER_KEY);
                    break;
                case TALL_HEIGHTMAP:
                    tallHeightMap = undoManager.getBuffer(TALL_HEIGHTMAP_BUFFER_KEY);
                    break;
                case TERRAIN:
                    terrain = undoManager.getBuffer(TERRAIN_BUFFER_KEY);
                    break;
                case WATERLEVEL:
                    waterLevel = undoManager.getBuffer(WATERLEVEL_BUFFER_KEY);
                    break;
                case TALL_WATERLEVEL:
                    tallWaterLevel = undoManager.getBuffer(TALL_WATERLEVEL_BUFFER_KEY);
                    break;
                case LAYER_DATA:
                    layerData = undoManager.getBuffer(LAYER_DATA_BUFFER_KEY);
                    break;
                case BIT_LAYER_DATA:
                    bitLayerData = undoManager.getBuffer(BIT_LAYER_DATA_BUFFER_KEY);
                    break;
                case SEEDS:
                    seeds = undoManager.getBuffer(SEEDS_BUFFER_KEY);
                    break;
            }
            readableBuffers.add(buffer);
        }
    }

    private void ensureWriteable(TileBuffer buffer) {
        if ((undoManager != null) && (! writeableBuffers.contains(buffer))) {
            switch (buffer) {
                case HEIGHTMAP:
                    heightMap = undoManager.getBufferForEditing(HEIGHTMAP_BUFFER_KEY);
                    break;
                case TALL_HEIGHTMAP:
                    tallHeightMap = undoManager.getBufferForEditing(TALL_HEIGHTMAP_BUFFER_KEY);
                    break;
                case TERRAIN:
                    terrain = undoManager.getBufferForEditing(TERRAIN_BUFFER_KEY);
                    break;
                case WATERLEVEL:
                    waterLevel = undoManager.getBufferForEditing(WATERLEVEL_BUFFER_KEY);
                    break;
                case TALL_WATERLEVEL:
                    tallWaterLevel = undoManager.getBufferForEditing(TALL_WATERLEVEL_BUFFER_KEY);
                    break;
                case LAYER_DATA:
                    layerData = undoManager.getBufferForEditing(LAYER_DATA_BUFFER_KEY);
                    break;
                case BIT_LAYER_DATA:
                    bitLayerData = undoManager.getBufferForEditing(BIT_LAYER_DATA_BUFFER_KEY);
                    break;
                case SEEDS:
                    seeds = undoManager.getBufferForEditing(SEEDS_BUFFER_KEY);
                    break;
            }
            readableBuffers.add(buffer);
            writeableBuffers.add(buffer);
        }
    }

    private void heightMapChanged() {
        if (eventInhibitionCounter != 0) {
            heightMapDirty = true;
        } else {
            for (Listener listener: listeners) {
                listener.heightMapChanged(this);
            }
        }
    }

    private void terrainChanged() {
        if (eventInhibitionCounter != 0) {
            terrainDirty = true;
        } else {
            for (Listener listener: listeners) {
                listener.terrainChanged(this);
            }
        }
    }

    private void waterLevelChanged() {
        if (eventInhibitionCounter != 0) {
            waterLevelDirty = true;
        } else {
            for (Listener listener: listeners) {
                listener.waterLevelChanged(this);
            }
        }
    }

    private void layerDataChanged(Layer layer) {
        if (eventInhibitionCounter != 0) {
            dirtyLayers.add(layer);
        } else {
            Set<Layer> changedLayers = Collections.singleton(layer);
            for (Listener listener: listeners) {
                listener.layerDataChanged(this, changedLayers);
            }
        }
    }

    private void allBitLayerDataChanged() {
        if (eventInhibitionCounter != 0) {
            bitLayersDirty = true;
        } else {
            for (Listener listener: listeners) {
                listener.allBitLayerDataChanged(this);
            }
        }
    }

    private void allNonBitLayerDataChanged() {
        if (eventInhibitionCounter != 0) {
            nonBitLayersDirty = true;
        } else {
            for (Listener listener: listeners) {
                listener.allNonBitlayerDataChanged(this);
            }
        }
    }
    
    private void seedsChanged() {
        if (eventInhibitionCounter != 0) {
            seedsDirty = true;
        } else {
            for (Listener listener: listeners) {
                listener.seedsChanged(this);
            }
        }
    }
    
    private float clamp(float level) {
        if (level < 0.0f) {
            return 0.0f;
        } else if (level > maxY) {
            return maxY;
        } else {
            return level;
        }
    }
    
    private int clamp(int level) {
        if (level < 0) {
            return 0;
        } else if (level > maxY) {
            return maxY;
        } else {
            return level;
        }
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        init();
    }
    
    private synchronized void writeObject(ObjectOutputStream out) throws IOException {
        // Make sure all buffers are current, otherwise we may save out of date
        // data to disk
        ensureAllReadable();

        // Take the opportunity to save memory and disk space by throwing away "empty" layer buffers. Since this is
        // functionally a null operation there is no need to notify listeners, make the buffer writable or otherwise
        // notify the undo manager
        for (Iterator<Map.Entry<Layer, BitSet>> i = bitLayerData.entrySet().iterator(); i.hasNext(); ) {
            final Map.Entry<Layer, BitSet> entry = i.next();
            if (entry.getValue().isEmpty()) {
                i.remove();
                cachedLayers = null;
            }
        }
layerLoop: for (Iterator<Map.Entry<Layer, byte[]>> i = layerData.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry<Layer, byte[]> entry = i.next();
            final Layer layer = entry.getKey();
            final byte[] buffer = entry.getValue();
            if (layer.getDataSize() == NIBBLE) {
                final byte defaultByte = (byte) (layer.getDefaultValue() << 4 | layer.getDefaultValue());
                for (byte bufferByte: buffer) {
                    if (bufferByte != defaultByte) {
                        continue layerLoop;
                    }
                }
                // If we reach here all bytes were default bytes
                i.remove();
                cachedLayers = null;
            } else if (layer.getDataSize() == BYTE) {
                final byte defaultByte = (byte) layer.getDefaultValue();
                for (byte bufferByte: buffer) {
                    if (bufferByte != defaultByte) {
                        continue layerLoop;
                    }
                }
                // If we reach here all bytes were default bytes
                i.remove();
                cachedLayers = null;
            }
        }

        out.defaultWriteObject();
    }

    private void init() {
        listeners = new ArrayList<>();
        HEIGHTMAP_BUFFER_KEY = new TileUndoBufferKey<>(this, HEIGHTMAP);
        TALL_HEIGHTMAP_BUFFER_KEY = new TileUndoBufferKey<>(this, TALL_HEIGHTMAP);
        TERRAIN_BUFFER_KEY = new TileUndoBufferKey<>(this, TERRAIN);
        WATERLEVEL_BUFFER_KEY = new TileUndoBufferKey<>(this, WATERLEVEL);
        TALL_WATERLEVEL_BUFFER_KEY = new TileUndoBufferKey<>(this, TALL_WATERLEVEL);
        LAYER_DATA_BUFFER_KEY = new TileUndoBufferKey<>(this, LAYER_DATA);
        BIT_LAYER_DATA_BUFFER_KEY = new TileUndoBufferKey<>(this, BIT_LAYER_DATA);
        SEEDS_BUFFER_KEY = new TileUndoBufferKey<>(this, SEEDS);
        dirtyLayers = new HashSet<>();
        maxY = maxHeight - 1;
        
        // Legacy map support
        if (maxHeight == 0) {
            maxHeight = 128;
            tall = false;
        }
        if ((seeds != null) && seeds.isEmpty()) {
            seeds = null;
        }
    }

    private final int x, y;
    private int maxHeight;
    private boolean tall;
    protected short[] heightMap;
    protected int[] tallHeightMap;
    protected byte[] terrain;
    protected byte[] waterLevel;
    protected short[] tallWaterLevel;
    protected Map<Layer, byte[]> layerData;
    protected Map<Layer, BitSet> bitLayerData;
    private HashSet<Seed> seeds;
    private transient List<Listener> listeners;
    private transient boolean heightMapDirty, terrainDirty, waterLevelDirty, seedsDirty, bitLayersDirty, nonBitLayersDirty;
    private transient Set<TileBuffer> readableBuffers;
    private transient Set<TileBuffer> writeableBuffers;
    private transient UndoManager undoManager;
    private transient List<Layer> cachedLayers;
    private transient Set<Layer> dirtyLayers;
    private transient int maxY, eventInhibitionCounter;

    private transient BufferKey<short[]>            HEIGHTMAP_BUFFER_KEY;
    private transient BufferKey<int[]>              TALL_HEIGHTMAP_BUFFER_KEY;
    private transient BufferKey<byte[]>             TERRAIN_BUFFER_KEY;
    private transient BufferKey<byte[]>             WATERLEVEL_BUFFER_KEY;
    private transient BufferKey<short[]>            TALL_WATERLEVEL_BUFFER_KEY;
    private transient BufferKey<Map<Layer, byte[]>> LAYER_DATA_BUFFER_KEY;
    private transient BufferKey<Map<Layer, BitSet>> BIT_LAYER_DATA_BUFFER_KEY;
    private transient BufferKey<HashSet<Seed>>      SEEDS_BUFFER_KEY;
    
    private static final Terrain[] TERRAIN_VALUES = Terrain.values();

    private static final float SQRT_OF_EIGHT = (float) Math.sqrt(8.0);
    
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Tile.class);
    
    private static final long serialVersionUID = 2011040101L;

    public interface Listener {
        void heightMapChanged(Tile tile);
        void terrainChanged(Tile tile);
        void waterLevelChanged(Tile tile);
        void layerDataChanged(Tile tile, Set<Layer> changedLayers);
        void allBitLayerDataChanged(Tile tile);
        void allNonBitlayerDataChanged(Tile tile);
        void seedsChanged(Tile tile);
    }

    static class TileUndoBufferKey<T> implements BufferKey<T> {
        public TileUndoBufferKey(Tile tile, TileBuffer buffer) {
            this.tile = tile;
            this.buffer = buffer;
        }

        @Override
        public boolean equals(Object obj) {
            return (obj instanceof TileUndoBufferKey)
                && (tile == ((TileUndoBufferKey) obj).tile)
                && (buffer == ((TileUndoBufferKey) obj).buffer);
        }

        @Override
        public int hashCode() {
            return (31 + System.identityHashCode(tile)) * 31 + buffer.hashCode();
        }

        @Override
        public String toString() {
            return "[" + tile.x + ", " + tile.y + ", " + buffer + "]";
        }

        final Tile tile;
        final TileBuffer buffer;
    }

    public enum TileBuffer {
        HEIGHTMAP, TERRAIN, WATERLEVEL, LAYER_DATA, BIT_LAYER_DATA, TALL_HEIGHTMAP, TALL_WATERLEVEL, SEEDS
    }
}