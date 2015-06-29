/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

import java.util.BitSet;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.pepsoft.util.undo.BufferKey;
import org.pepsoft.util.undo.Snapshot;
import org.pepsoft.util.undo.UndoManager;
import org.pepsoft.worldpainter.layers.Layer;

/**
 *
 * @author pepijn
 */
public final class TileSnapshot extends Tile {
    public TileSnapshot(Tile tile, Snapshot snapshot) {
        super(tile.getX(), tile.getY(), tile.getMaxHeight());
        this.snapshot = snapshot;
        HEIGHTMAP_BUFFER_KEY      = new TileUndoBufferKey<>(tile, TileBuffer.HEIGHTMAP);
        TERRAIN_BUFFER_KEY        = new TileUndoBufferKey<>(tile, TileBuffer.TERRAIN);
        WATERLEVEL_BUFFER_KEY     = new TileUndoBufferKey<>(tile, TileBuffer.WATERLEVEL);
        LAYER_DATA_BUFFER_KEY     = new TileUndoBufferKey<>(tile, TileBuffer.LAYER_DATA);
        BIT_LAYER_DATA_BUFFER_KEY = new TileUndoBufferKey<>(tile, TileBuffer.BIT_LAYER_DATA);
    }

    @Override
    public void addListener(Listener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unregister() {
        // Do nothing
    }

    @Override
    protected void ensureReadable(TileBuffer buffer) {
        if (! readableBuffers.contains(buffer)) {
            switch (buffer) {
                case HEIGHTMAP:
                    heightMap = snapshot.getBuffer(HEIGHTMAP_BUFFER_KEY);
                    break;
                case TERRAIN:
                    terrain = snapshot.getBuffer(TERRAIN_BUFFER_KEY);
                    break;
                case WATERLEVEL:
                    waterLevel = snapshot.getBuffer(WATERLEVEL_BUFFER_KEY);
                    break;
                case LAYER_DATA:
                    layerData = snapshot.getBuffer(LAYER_DATA_BUFFER_KEY);
                    break;
                case BIT_LAYER_DATA:
                    bitLayerData = snapshot.getBuffer(BIT_LAYER_DATA_BUFFER_KEY);
                    break;
            }
            readableBuffers.add(buffer);
        }
    }

    @Override
    public boolean isEventsInhibited() {
        return false;
    }

    @Override
    public void register(UndoManager undoManager) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setBitLayerValue(Layer layer, int x, int y, boolean value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized void inhibitEvents() {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized void releaseEvents() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setHeight(int x, int y, float height) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setLayerValue(Layer layer, int x, int y, int value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTerrain(int x, int y, Terrain terrain) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setWaterLevel(int x, int y, int waterLevel) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return "TileSnapshot[x=" + getX() + ",y=" + getY() + "]";
    }
    
    private final Snapshot snapshot;
    private final Set<TileBuffer> readableBuffers = EnumSet.noneOf(TileBuffer.class);

    private transient BufferKey<short[]>            HEIGHTMAP_BUFFER_KEY;
    private transient BufferKey<byte[]>             TERRAIN_BUFFER_KEY;
    private transient BufferKey<byte[]>             WATERLEVEL_BUFFER_KEY;
    private transient BufferKey<Map<Layer, byte[]>> LAYER_DATA_BUFFER_KEY;
    private transient BufferKey<Map<Layer, BitSet>> BIT_LAYER_DATA_BUFFER_KEY;

    private static final long serialVersionUID = 2011101501L;
}