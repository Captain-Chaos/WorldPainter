package org.pepsoft.worldpainter.layers.exporters;

import org.pepsoft.minecraft.Chunk;
import org.pepsoft.minecraft.Direction;
import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.exception.WPRuntimeException;
import org.pepsoft.worldpainter.exporting.AbstractLayerExporter;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.exporters.AbstractCavesExporter.CaveDecorationSettings.Decoration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

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
    public AbstractCavesExporter(Dimension dimension, Platform platform, CaveSettings settings, L layer) {
        super(dimension, platform, settings, layer);
        decorationSettings = settings.getCaveDecorationSettings();
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
            state.previousBlockInCavern = false;
        }
    }

    protected final void decorateBlock(MinecraftWorld world, Random rng, int x, int y, int height) {
        final Material material = world.getMaterialAt(x, y, height);
        if ((material == AIR) || material.isNamed(MC_WATER)) {
            final Material materialBelow = (height > minHeight) ? world.getMaterialAt(x, y, height - 1) : null;
            if ((materialBelow != null) && (! materialBelow.veryInsubstantial) && decorateFloor(world, rng, x, y, height, material, materialBelow)) {
                return;
            }
            final Material materialAbove = (height < (maxHeight - 1)) ? world.getMaterialAt(x, y, height + 1) : null;
            if ((materialAbove != null) && (! materialAbove.veryInsubstantial) && decorateRoof(world, rng, x, y, height, material, materialAbove)) {
                return;
            }
            final Material materialNorth = world.getMaterialAt(x, y - 1, height);
            final Material materialSouth = world.getMaterialAt(x, y + 1, height);
            final Material materialEast = world.getMaterialAt(x + 1, y, height);
            final Material materialWest = world.getMaterialAt(x - 1, y, height);
            if ((! materialNorth.veryInsubstantial) || (! materialSouth.veryInsubstantial) || (! materialEast.veryInsubstantial) || (! materialWest.veryInsubstantial)) {
                decorateWall(world, rng, x, y, height, material, materialNorth, materialSouth, materialEast, materialWest);
            }
        }
    }

    private boolean decorateFloor(MinecraftWorld world, Random rng, int x, int y, int height, Material existingMaterial, Material materialBelow) {
        if (decorationSettings.isEnabledAt(Decoration.BROWN_MUSHROOM, height) && (rng.nextInt(MUSHROOM_CHANCE) == 0) && existingMaterial.isNotNamed(MC_WATER)) {
            logger.debug("Decorating block {},{},{} with {}}", x, y, height, BROWN_MUSHROOM);
            world.setMaterialAt(x, y, height, BROWN_MUSHROOM);
            return true;
        } else if (decorationSettings.isEnabledAt(Decoration.GLOW_LICHEN, height) && rng.nextInt(MUSHROOM_CHANCE) == 0) {
            if (existingMaterial.isNamed(MC_WATER)) {
                logger.debug("Decorating block {},{},{} with {}}", x, y, height, GLOW_LICHEN_DOWN.withProperty(WATERLOGGED, true));
                world.setMaterialAt(x, y, height, GLOW_LICHEN_DOWN.withProperty(WATERLOGGED, true));
            } else {
                logger.debug("Decorating block {},{},{} with {}}", x, y, height, GLOW_LICHEN_DOWN);
                world.setMaterialAt(x, y, height, GLOW_LICHEN_DOWN);
            }
            return true;
        }
        // TODO
        return false;
    }

    private boolean decorateRoof(MinecraftWorld world, Random rng, int x, int y, int height, Material existingMaterial, Material materialAbove) {
        if (decorationSettings.isEnabledAt(Decoration.GLOW_LICHEN, height) && rng.nextInt(MUSHROOM_CHANCE) == 0) {
            if (existingMaterial.isNamed(MC_WATER)) {
                logger.debug("Decorating block {},{},{} with {}}", x, y, height, GLOW_LICHEN_UP.withProperty(WATERLOGGED, true));
                world.setMaterialAt(x, y, height, GLOW_LICHEN_UP.withProperty(WATERLOGGED, true));
            } else {
                logger.debug("Decorating block {},{},{} with {}}", x, y, height, GLOW_LICHEN_UP);
                world.setMaterialAt(x, y, height, GLOW_LICHEN_UP);
            }
            return true;
        }
        // TODO
        return false;
    }

    private void decorateWall(MinecraftWorld world, Random rng, int x, int y, int height, Material existingMaterial, Material materialNorth, Material materialSouth, Material materialEast, Material materialWest) {
        if (decorationSettings.isEnabledAt(Decoration.GLOW_LICHEN, height) && rng.nextInt(MUSHROOM_CHANCE) == 0) {
            Material material = GLOW_LICHEN_NONE;
            if (existingMaterial.isNamed(MC_WATER)) {
                material = GLOW_LICHEN_NONE.withProperty(WATERLOGGED, true);
            }
            final List<Direction> directions = new ArrayList<>(4);
            if (! materialNorth.veryInsubstantial) {
                directions.add(Direction.NORTH);
            }
            if (! materialSouth.veryInsubstantial) {
                directions.add(Direction.SOUTH);
            }
            if (! materialEast.veryInsubstantial) {
                directions.add(Direction.EAST);
            }
            if (! materialWest.veryInsubstantial) {
                directions.add(Direction.WEST);
            }
            logger.debug("Decorating block {},{},{} with {}}", x, y, height, material.withProperty(directions.get(rng.nextInt(directions.size())).name().toLowerCase(), "true"));
            world.setMaterialAt(x, y, height, material.withProperty(directions.get(rng.nextInt(directions.size())).name().toLowerCase(), "true"));
        }
        // TODO
    }

    public static class CaveDecorationSettings implements java.io.Serializable {
        public CaveDecorationSettings() {
            enabledDecorations.put(Decoration.BROWN_MUSHROOM, null);
            enabledDecorations.put(Decoration.GLOW_LICHEN, null);
        }

        public boolean isEnabledAt(Decoration decoration, int height) {
            if (enabledDecorations.containsKey(decoration)) {
                final int[] limits = enabledDecorations.get(decoration);
                return (limits == null) || ((height >= limits[0]) && (height <= limits[1]));
            } else {
                return false;
            }
        }

        /**
         * If the key is present, the decoration is enabled. If the value is {@code null}, it is enabled everywhere;
         * otherwise the value is an array with the minimum and maximum levels at which to apply the decoration.
         */
        final Map<Decoration, int[]> enabledDecorations = new HashMap<>();

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CaveDecorationSettings that = (CaveDecorationSettings) o;

            return enabledDecorations.equals(that.enabledDecorations);
        }

        @Override
        public int hashCode() {
            return enabledDecorations.hashCode();
        }

        @Override
        public CaveDecorationSettings clone() {
            try {
                return (CaveDecorationSettings) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new WPRuntimeException(e);
            }
        }

        private static final long serialVersionUID = 1L;

        public enum Decoration {
            BROWN_MUSHROOM, GLOW_LICHEN, LAVA_CAVE_PATCHES, DRIPSTONE_CAVE_PATCHES
        }
    }

    static class State {
        long seed;
        Tile tile;
        int maxY, waterLevel;
        boolean glassCeiling, breachedCeiling, surfaceBreaking, leaveWater, previousBlockInCavern, floodWithLava;
    }

    private final CaveDecorationSettings decorationSettings;

    private static final ThreadLocal<State> STATE_HOLDER = new ThreadLocal<>();
    private static final int MUSHROOM_CHANCE = 250;
    private static final Logger logger = LoggerFactory.getLogger(AbstractCavesExporter.class);
}