package org.pepsoft.worldpainter.layers.exporters;

import org.pepsoft.minecraft.Chunk;
import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.exporting.AbstractLayerExporter;
import org.pepsoft.worldpainter.layers.Layer;

import java.util.Random;

import static org.pepsoft.minecraft.Constants.MC_LAVA;
import static org.pepsoft.minecraft.Constants.MC_WATER;
import static org.pepsoft.minecraft.Material.*;

/**
 * Abstract base class for cave carving exporters, providing common
 * functionality for excavating caves. Intended use:
 *
 * <ul><li>For each column (x and y coordinate):
 *     <li>Invoke {@link #setupForColumn(long, Tile, int, int, boolean, boolean, boolean, boolean)} once
 *     <li>Going from the top to the bottom of the cave for that column, invoke {@link #processBlock(Chunk, int, int, int, boolean)}
 * </ul>
 *
 * @param <L> The cave layer type.
 */
public abstract class AbstractCavesExporter<L extends Layer> extends AbstractLayerExporter<L> {
    public AbstractCavesExporter(L layer, ExporterSettings defaultSettings) {
        super(layer, defaultSettings);
    }

    public AbstractCavesExporter(L layer) {
        super(layer);
    }

    protected final void setupForColumn(long seed, Tile tile, int maxY, int waterLevel, boolean glassCeiling, boolean surfaceBreaking, boolean leaveWater, boolean floodWithLava) {
        State state = new State();
        state.seed = seed;
        state.tile = tile;
        state.maxY = maxY;
        state.waterLevel = waterLevel;
        state.glassCeiling = glassCeiling;
        state.surfaceBreaking = surfaceBreaking;
        state.leaveWater = leaveWater;
        state.floodWithLava = floodWithLava;
        STATE_HOLDER.set(state);
    }

    protected final void resetColumn() {
        final State state = STATE_HOLDER.get();
        state.breachedCeiling = false;
        state.previousBlockInCavern = false;
    }

    protected final void emptyBlockEncountered() {
        State state = STATE_HOLDER.get();
        state.breachedCeiling = true;
        state.previousBlockInCavern = true;
    }

    protected final void processBlock(Chunk chunk, int x, int y, int z, boolean excavate) {
        final State state = STATE_HOLDER.get();
        if (excavate) {
            // In a cavern
            if ((! state.breachedCeiling) && (y < state.maxY)) {
                if (state.glassCeiling) {
                    final int terrainheight = state.tile.getIntHeight(x, z);
                    for (int yy = y + 1; yy <= terrainheight; yy++) {
                        chunk.setMaterial(x, yy, z, GLASS);
                    }
                }
                if (state.surfaceBreaking) {
                    final Material blockAbove = chunk.getMaterial(x, y + 1, z);
                    if (! state.leaveWater) {
                        final int terrainheight = state.tile.getIntHeight(x, z);
                        if (blockAbove.isNamed(MC_WATER)) {
                            for (int yy = y + 1; yy <= terrainheight; yy++) {
                                if (chunk.getMaterial(x, yy, z).isNamed(MC_WATER)) {
                                    chunk.setMaterial(x, yy, z, AIR);
                                } else {
                                    break;
                                }
                            }
                        } else if (blockAbove.isNamed(MC_LAVA)) {
                            for (int yy = y + 1; yy <= terrainheight; yy++) {
                                if (chunk.getMaterial(x, yy, z).isNamed(MC_LAVA)) {
                                    chunk.setMaterial(x, yy, z, AIR);
                                } else {
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            state.breachedCeiling = true;
            if (y > state.waterLevel) {
                chunk.setMaterial(x, y, z, AIR);
                state.previousBlockInCavern = true;
            } else {
                if (state.floodWithLava) {
                    chunk.setMaterial(x, y, z, LAVA);
                } else {
                    chunk.setMaterial(x, y, z, WATER);
                }
                state.previousBlockInCavern = false;
            }
        } else if (state.previousBlockInCavern
                && (y >= state.waterLevel)
                && (! chunk.getMaterial(x, y, z).veryInsubstantial)) {
            int worldX = (chunk.getxPos() << 4) | x;
            int worldZ = (chunk.getzPos() << 4) | z;
            final int rnd = new Random(state.seed + (worldX * 65537L) + (worldZ * 4099L)).nextInt(MUSHROOM_CHANCE);
            if ((rnd == 0) && (y < state.maxY)) {
                chunk.setMaterial(x, y + 1, z, BROWN_MUSHROOM);
            }
            state.previousBlockInCavern = false;
        }
    }

    static class State {
        long seed;
        Tile tile;
        int maxY, waterLevel;
        boolean glassCeiling, breachedCeiling, surfaceBreaking, leaveWater, previousBlockInCavern, floodWithLava;
    }

    private static final ThreadLocal<State> STATE_HOLDER = new ThreadLocal<>();
    private static final int MUSHROOM_CHANCE = 250;
}