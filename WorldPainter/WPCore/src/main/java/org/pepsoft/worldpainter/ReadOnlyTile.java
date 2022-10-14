package org.pepsoft.worldpainter;

import org.pepsoft.util.undo.BufferKey;
import org.pepsoft.util.undo.UndoManager;
import org.pepsoft.worldpainter.gardenofeden.Seed;
import org.pepsoft.worldpainter.layers.Layer;

import java.io.PrintStream;
import java.util.Set;

/**
 * Abstract base class for a read-only tile. All methods that would make changes throw an
 * {@link UnsupportedOperationException}. All methods concerning listeners, undo support, etc. do nothing.
 */
public abstract class ReadOnlyTile extends Tile {
    protected ReadOnlyTile(int x, int y, int minHeight, int maxHeight, boolean init) {
        super(x, y, minHeight, maxHeight, init);
    }

    @Override
    public final void addListener(Listener listener) {
        // Do nothing
    }

    @Override
    public final void unregister() {
        // Do nothing
    }

    @Override
    public final void setMinMaxHeight(int minHeight, int maxHeight, HeightTransform heightTransform) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void setRawHeight(int x, int y, int rawHeight) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void clearLayerData(Layer layer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void clearLayerData(int x, int y, Set<Layer> excludedLayers) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final boolean plantSeed(Seed seed) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void removeSeed(Seed seed) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void removeListener(Listener listener) {
        // Do nothing
    }

    @Override
    public final Tile transform(CoordinateTransform transform) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean repair(int minHeight, int maxHeight, PrintStream out) {
        return true;
    }

    @Override
    public final void savePointArmed() {
        // Do nothing
    }

    @Override
    public final void savePointCreated() {
        // Do nothing
    }

    @Override
    public final void undoPerformed() {
        // Do nothing
    }

    @Override
    public final void redoPerformed() {
        // Do nothing
    }

    @Override
    public final void bufferChanged(BufferKey<?> key) {
        // Do nothing
    }

    @Override
    final void ensureAllReadable() {
        // Do nothing
    }

    @Override
    final void convertBiomeData() {
        // Do nothing
    }

    @Override
    protected final void ensureReadable(TileBuffer buffer) {
        // Do nothing
    }

    @Override
    public final boolean isEventsInhibited() {
        return false;
    }

    @Override
    public final void register(UndoManager undoManager) {
        // Do nothing
    }

    @Override
    public final void setBitLayerValue(Layer layer, int x, int y, boolean value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void inhibitEvents() {
        // Do nothing
    }

    @Override
    public final void releaseEvents() {
        // Do nothing
    }

    @Override
    public final void setHeight(int x, int y, float height) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void setLayerValue(Layer layer, int x, int y, int value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void setTerrain(int x, int y, Terrain terrain) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void setWaterLevel(int x, int y, int waterLevel) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return "ReadOnlyTile[x=" + getX() + ",y=" + getY() + "]";
    }

    private static final long serialVersionUID = 1L;
}