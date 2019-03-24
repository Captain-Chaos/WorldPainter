package org.pepsoft.worldpainter.layers.exporters;

import org.pepsoft.minecraft.Chunk;
import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.exporting.AbstractLayerExporter;
import org.pepsoft.worldpainter.layers.Layer;

import java.util.Random;

import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.minecraft.Material.*;

public abstract class AbstractCavesExporter<L extends Layer> extends AbstractLayerExporter<L> {
    public AbstractCavesExporter(L layer, ExporterSettings defaultSettings) {
        super(layer, defaultSettings);
    }

    public AbstractCavesExporter(L layer) {
        super(layer);
    }

    protected void setupForColumn(long seed, Tile tile, int maxY, int waterLevel, boolean glassCeiling, boolean surfaceBreaking, boolean leaveWater, boolean floodWithLava) {
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

    protected void emptyBlockEncountered() {
        State state = STATE_HOLDER.get();
        state.breachedCeiling = true;
        state.previousBlockInCavern = true;
    }

    protected void processBlock(Chunk chunk, int x, int y, int z, boolean excavate) {
        State state = STATE_HOLDER.get();
        if (excavate) {
            // In a cavern
            if ((! state.breachedCeiling) && (y < state.maxY)) {
//                System.out.println("Cavern for value: " + cavernsValue);
//                if (cavernsValue < lowestValueCavern) {
//                    lowestValueCavern = cavernsValue;
//                    System.out.println("Lowest cavern value with caverns: " + lowestValueCavern);
//                }
                if (state.glassCeiling) {
                    int terrainheight = state.tile.getIntHeight(x, z);
                    for (int yy = y + 1; yy <= terrainheight; yy++) {
                        chunk.setMaterial(x, yy, z, GLASS);
                    }
                }
                if (state.surfaceBreaking) {
                    final Material blockAbove = chunk.getMaterial(x, y + 1, z);
                    if (state.leaveWater) {
                        // TODOMC13: migrate to modern materials:
                        if (blockAbove.blockType == BLK_STATIONARY_WATER) {
                            chunk.setMaterial(x, y + 1, z, WATER);
                        } else if (blockAbove.blockType == BLK_STATIONARY_LAVA) {
                            chunk.setMaterial(x, y + 1, z, LAVA);
                        }
                    } else {
                        if (blockAbove.isNamed(MC_WATER)) {
                            for (int yy = y + 1; yy <= state.maxY; yy++) {
                                final Material block = chunk.getMaterial(x, yy, z);
                                if (block.isNamed(MC_WATER)) {
                                    chunk.setMaterial(x, yy, z, AIR);
                                    // Set the surrounding water, if
                                    // any, to non-stationary, so that
                                    // it will flow into the cavern
                                    // TODOMC13: migrate to modern materials:
                                    if ((x > 0) && (chunk.getMaterial(x - 1, yy, z).blockType == BLK_STATIONARY_WATER)) {
                                        chunk.setMaterial(x - 1, yy, z, WATER);
                                    }
                                    if ((x < 15) && (chunk.getMaterial(x + 1, yy, z).blockType == BLK_STATIONARY_WATER)) {
                                        chunk.setMaterial(x + 1, yy, z, WATER);
                                    }
                                    if ((z > 0) && (chunk.getMaterial(x, yy, z - 1).blockType == BLK_STATIONARY_WATER)) {
                                        chunk.setMaterial(x, yy, z - 1, WATER);
                                    }
                                    if ((z < 15) && (chunk.getMaterial(x, yy, z + 1).blockType == BLK_STATIONARY_WATER)) {
                                        chunk.setMaterial(x, yy, z + 1, WATER);
                                    }
                                } else {
                                    break;
                                }
                            }
                        } else if (blockAbove.isNamed(MC_LAVA)) {
                            for (int yy = y + 1; yy <= state.maxY; yy++) {
                                final Material block = chunk.getMaterial(x, yy, z);
                                if (block.isNamed(MC_LAVA)) {
                                    chunk.setMaterial(x, yy, z, AIR);
                                    // Set the surrounding water, if
                                    // any, to non-stationary, so that
                                    // it will flow into the cavern
                                    // TODOMC13: migrate to modern materials:
                                    if ((x > 0) && (chunk.getMaterial(x - 1, yy, z).blockType == BLK_STATIONARY_LAVA)) {
                                        chunk.setMaterial(x - 1, yy, z, LAVA);
                                    }
                                    if ((x < 15) && (chunk.getMaterial(x + 1, yy, z).blockType == BLK_STATIONARY_LAVA)) {
                                        chunk.setMaterial(x + 1, yy, z, LAVA);
                                    }
                                    if ((z > 0) && (chunk.getMaterial(x, yy, z - 1).blockType == BLK_STATIONARY_LAVA)) {
                                        chunk.setMaterial(x, yy, z - 1, LAVA);
                                    }
                                    if ((z < 15) && (chunk.getMaterial(x, yy, z + 1).blockType == BLK_STATIONARY_LAVA)) {
                                        chunk.setMaterial(x, yy, z + 1, LAVA);
                                    }
                                } else {
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            state.breachedCeiling = true;
            if ((! state.previousBlockInCavern) && (y < state.maxY)) {
                final Material blockAbove = chunk.getMaterial(x, y + 1, z);
                // Note that the post processor will take care
                // of supporting sand with sandstone, if that is
                // not disabled
                if (blockAbove == GRAVEL) {
                    // Support gravel with stone
                    chunk.setMaterial(x, y + 1, z, STONE);
                }
            }
//            System.out.println("Cavern at " + x + ", " + y + ", " + z);
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
//            Material material = subsurfaceMaterial.getMaterial(seed, worldX, worldY, 1);
//            if (material == Material.AIR) {
            int worldX = (chunk.getxPos() << 4) | x;
            int worldZ = (chunk.getzPos() << 4) | z;
            final int rnd = new Random(state.seed + (worldX * 65537) + (worldZ * 4099)).nextInt(MUSHROOM_CHANCE);
            if (rnd == 0) {
                chunk.setMaterial(x, y + 1, z, BROWN_MUSHROOM);
//                System.out.println("Cave shroom @ " + worldX + ", " + worldY + "!");
            }
//            } else {
//                chunk.setMaterial(x, y + 1, z, material);
//            }
//            chunk.setMaterial(x, y, z, subsurfaceMaterial.getMaterial(seed, worldX, worldY, 0));
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