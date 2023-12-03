package org.pepsoft.worldpainter.layers.exporters;

import com.google.common.collect.ImmutableSet;
import org.pepsoft.minecraft.Chunk;
import org.pepsoft.minecraft.Direction;
import org.pepsoft.minecraft.Material;
import org.pepsoft.util.mdc.MDCWrappingRuntimeException;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.NoiseSettings;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.exporting.AbstractLayerExporter;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;
import org.pepsoft.worldpainter.heightMaps.NoiseHeightMap;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.exporters.AbstractCavesExporter.CaveDecorationSettings.Decoration;
import org.pepsoft.worldpainter.util.BiomeUtils;

import java.util.*;

import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.minecraft.Material.*;
import static org.pepsoft.worldpainter.Constants.V_1_17;
import static org.pepsoft.worldpainter.DefaultPlugin.ATTRIBUTE_MC_VERSION;
import static org.pepsoft.worldpainter.Platform.Capability.BIOMES_3D;
import static org.pepsoft.worldpainter.Platform.Capability.NAMED_BIOMES;
import static org.pepsoft.worldpainter.biomeschemes.Minecraft1_17Biomes.BIOME_DRIPSTONE_CAVES;
import static org.pepsoft.worldpainter.biomeschemes.Minecraft1_17Biomes.BIOME_LUSH_CAVES;
import static org.pepsoft.worldpainter.layers.exporters.AbstractCavesExporter.CaveDecorationSettings.Decoration.DRIPSTONE_CAVE_PATCHES;
import static org.pepsoft.worldpainter.layers.exporters.AbstractCavesExporter.CaveDecorationSettings.Decoration.LUSH_CAVE_PATCHES;
import static org.pepsoft.worldpainter.layers.exporters.WPObjectExporter.renderObject;
import static org.pepsoft.worldpainter.layers.exporters.WPObjectExporter.renderObjectInverted;
import static org.pepsoft.worldpainter.layers.plants.Plants.GRASS;
import static org.pepsoft.worldpainter.layers.plants.Plants.MOSS_CARPET;
import static org.pepsoft.worldpainter.layers.plants.Plants.SPORE_BLOSSOM;
import static org.pepsoft.worldpainter.layers.plants.Plants.*;

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
        decorationSettings = (settings != null) ? settings.getCaveDecorationSettings() : null;
        if (decorationSettings != null) {
            decorateBrownMushrooms = decorationSettings.isEnabled(Decoration.BROWN_MUSHROOM);
            final boolean mcVersionAtLeast1_17 = platform.getAttribute(ATTRIBUTE_MC_VERSION).isAtLeast(V_1_17);
            decorateGlowLichen = decorationSettings.isEnabled(Decoration.GLOW_LICHEN) && mcVersionAtLeast1_17;
            decorateLushCaves = decorationSettings.isEnabled(Decoration.LUSH_CAVE_PATCHES) && mcVersionAtLeast1_17;
            decorateDripstoneCaves = decorationSettings.isEnabled(Decoration.DRIPSTONE_CAVE_PATCHES) && mcVersionAtLeast1_17;
            decorationEnabled = decorateBrownMushrooms || decorateGlowLichen || decorateLushCaves || decorateDripstoneCaves;
            lushCaveNoise = decorateLushCaves ? new NoiseHeightMap(decorationSettings.noiseSettingsMap.get(LUSH_CAVE_PATCHES), dimension.getSeed() + 1) : null;
            dripstoneCaveNoise = decorateDripstoneCaves ? new NoiseHeightMap(decorationSettings.noiseSettingsMap.get(DRIPSTONE_CAVE_PATCHES), dimension.getSeed() + 2) : null;
            biomeUtils = new BiomeUtils(dimension);
            setBiomes = platform.capabilities.contains(BIOMES_3D) || platform.capabilities.contains(NAMED_BIOMES);
        } else {
            decorationEnabled = decorateBrownMushrooms = decorateGlowLichen = decorateLushCaves = decorateDripstoneCaves = setBiomes = false;
            lushCaveNoise = dripstoneCaveNoise = null;
            biomeUtils = null;
        }
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
        if ((! decorationEnabled) || (height < minZ) || (height > maxZ)) {
            return;
        }
        final Material material = world.getMaterialAt(x, y, height);
        // TODO fine tune default noise settings
        final boolean inLushCave = decorateLushCaves && decorationSettings.isEnabledAt(LUSH_CAVE_PATCHES, height) && (lushCaveNoise.getValue(x, y, height * 2.0) >= LUSH_CAVE_THRESHOLD);
        final boolean inDripstoneCave = decorateDripstoneCaves && decorationSettings.isEnabledAt(Decoration.DRIPSTONE_CAVE_PATCHES, height) && (dripstoneCaveNoise.getValue(x, y, height * 2.0) >= DRIPSTONE_CAVE_THRESHOLD);
        if (setBiomes && (inLushCave || inDripstoneCave)) {
            final int terrainHeight = dimension.getIntHeightAt(x, y);
            final int biomeUpperLimit = (((terrainHeight - dimension.getTopLayerDepth(x, y, terrainHeight)) >> 2) << 2) - 1;
            if (height <= biomeUpperLimit) {
                biomeUtils.set3DBiome(world.getChunkForEditing(x >> 4, y >> 4), (x & 0xf) >> 2, height >> 2, (y & 0xf) >> 2, inLushCave ? BIOME_LUSH_CAVES : BIOME_DRIPSTONE_CAVES);
            }
        }
        if (material.empty || material.isNamed(MC_WATER)) {
            final Material materialBelow = (height > minHeight) ? world.getMaterialAt(x, y, height - 1) : null;
            if ((materialBelow != null) && (! materialBelow.veryInsubstantial) && (! materialBelow.isNamed(MC_POINTED_DRIPSTONE))) {
                int waterDepth = material.empty ? 1 : 0, spaceAvailable = 1;
                for (int dz = 1; (dz < 7) && (height + dz < maxHeight); dz++) {
                    if (world.getMaterialAt(x, y, height + dz).isNamed(MC_WATER)) {
                        waterDepth++;
                    } else if ((! world.getMaterialAt(x, y, height + dz).empty)) {
                        break;
                    }
                    spaceAvailable++;
                }
                if (decorateFloor(world, rng, x, y, height, inLushCave, inDripstoneCave, material, materialBelow, spaceAvailable, waterDepth)) {
                    return;
                }
            }
            final Material materialAbove = (height < (maxHeight - 1)) ? world.getMaterialAt(x, y, height + 1) : null;
            if ((materialAbove != null) && (! materialAbove.veryInsubstantial) && (! materialAbove.isNamed(MC_POINTED_DRIPSTONE))) {
                int spaceAvailable = 1, drySpaceAvailable = material.empty ? 1 : 0;
                for (int dz = 1; (dz < 7) && (height - dz >= minHeight); dz++) {
                    final Material material1 = world.getMaterialAt(x, y, height - dz);
                    if (((! material1.empty)) && material1.isNotNamed(MC_WATER)) {
                        break;
                    } else if (material1.empty && (drySpaceAvailable != 0)){
                        drySpaceAvailable++;
                    }
                    spaceAvailable++;
                }
                if (decorateRoof(world, rng, x, y, height, inLushCave, inDripstoneCave, material, materialAbove, spaceAvailable, drySpaceAvailable)) {
                    return;
                }
            }
            final Material materialNorth = world.getMaterialAt(x, y - 1, height);
            final Material materialSouth = world.getMaterialAt(x, y + 1, height);
            final Material materialEast = world.getMaterialAt(x + 1, y, height);
            final Material materialWest = world.getMaterialAt(x - 1, y, height);
            if (((! materialNorth.veryInsubstantial) && (! materialNorth.isNamed(MC_POINTED_DRIPSTONE)))
                    || ((! materialSouth.veryInsubstantial) && (! materialSouth.isNamed(MC_POINTED_DRIPSTONE)))
                    || ((! materialEast.veryInsubstantial) && (! materialEast.isNamed(MC_POINTED_DRIPSTONE)))
                    || ((! materialWest.veryInsubstantial) && (! materialWest.isNamed(MC_POINTED_DRIPSTONE)))) {
                decorateWall(world, rng, x, y, height, inLushCave, material, materialNorth, materialSouth, materialEast, materialWest);
            }
        }
    }

    private boolean decorateFloor(MinecraftWorld world, Random rng, int x, int y, int height, boolean inLushCave, boolean inDripstoneCave, Material existingMaterial, Material materialBelow, int spaceAvailable, int waterDepth) {
        if (decorateBrownMushrooms && decorationSettings.isEnabledAt(Decoration.BROWN_MUSHROOM, height) && (rng.nextInt(MUSHROOM_CHANCE) == 0) && existingMaterial.isNotNamed(MC_WATER)) {
            world.setMaterialAt(x, y, height, BROWN_MUSHROOM);
            return true;
        } else if (decorateGlowLichen && decorationSettings.isEnabledAt(Decoration.GLOW_LICHEN, height) && rng.nextInt(MUSHROOM_CHANCE) == 0) {
            if (existingMaterial.isNamed(MC_WATER)) {
                world.setMaterialAt(x, y, height, GLOW_LICHEN_DOWN.withProperty(WATERLOGGED, true));
            } else {
                world.setMaterialAt(x, y, height, GLOW_LICHEN_DOWN);
            }
            return true;
        } else if (inDripstoneCave && (waterDepth <= 2)) {
            // TODO make this more configurable and evaluate
            if (rng.nextInt(4) == 0) {
                // Check whether we are actually below something that could generate a stalactite
                for (int z = height + 1; z <= dimension.getIntHeightAt(x, y); z++) {
                    final Material material = world.getMaterialAt(x, y, z);
                    if (SUPPORTS_DRIPSTONE.contains(material.name) || material.isNamed(MC_POINTED_DRIPSTONE)) {
                        // TODO make the length dependent on how deep in the patch we are
                        renderStalagmite(world, x, y, height, rng.nextInt(Math.min(5, spaceAvailable)) + 1, rng);
                        return true;
                    } else if (material.solid) {
                        break;
                    }
                }
            }
        }
        if (inLushCave) {
            if ((height - 1) >= minZ) {
                world.setMaterialAt(x, y, height - 1, MOSS_BLOCK);
            }
            if (existingMaterial.isNamed(MC_WATER)) {
                switch (rng.nextInt(10)) {
                    case 0:
                    case 1:
                        if (spaceAvailable > 1) {
                            renderObject(world, dimension, platform, SMALL_DRIPLEAF.realise(2, platform), x, y, height);
                        }
                        break;
                    case 2:
                    case 3:
                        if (spaceAvailable > 2) {
                            renderObject(world, dimension, platform, BIG_DRIPLEAF.realise(rng.nextInt(Math.min(5, spaceAvailable - 2)) + 3, platform), x, y, height);
                        }
                        break;
                }
            } else {
                switch (rng.nextInt(20)) {
                    case 0:
                    case 1:
                    case 2:
                        renderObject(world, dimension, platform, GRASS.realise(1, platform), x, y, height);
                        break;
                    case 3:
                    case 4:
                    case 5:
                        if (spaceAvailable > 1) {
                            renderObject(world, dimension, platform, TALL_GRASS.realise(2, platform), x, y, height);
                        }
                        break;
                    case 6:
                        renderObject(world, dimension, platform, SAPLING_FLOWERING_AZALEA.realise(1, platform), x, y, height);
                        break;
                    case 7:
                    case 8:
                    case 9:
                        renderObject(world, dimension, platform, MOSS_CARPET.realise(1, platform), x, y, height);
                        break;
                }
            }
        }
        return false;
    }

    private boolean decorateRoof(MinecraftWorld world, Random rng, int x, int y, int height, boolean inLushCave, boolean inDripstoneCave, Material existingMaterial, Material materialAbove, int spaceAvailable, int drySpaceAvailable) {
        if (decorateGlowLichen && decorationSettings.isEnabledAt(Decoration.GLOW_LICHEN, height) && rng.nextInt(MUSHROOM_CHANCE) == 0) {
            if (existingMaterial.isNamed(MC_WATER)) {
                world.setMaterialAt(x, y, height, GLOW_LICHEN_UP.withProperty(WATERLOGGED, true));
            } else {
                world.setMaterialAt(x, y, height, GLOW_LICHEN_UP);
            }
            return true;
        } else if (inDripstoneCave && SUPPORTS_DRIPSTONE.contains(materialAbove.name)) {
            // TODO make this more configurable and evaluate
            if (rng.nextInt(4) == 0) {
                // TODO make the length dependent on how deep in the patch we are
                renderStalactite(world, x, y, height, rng.nextInt(Math.min(5, spaceAvailable)) + 1, rng);
            }
        }
        if (inLushCave) {
            if ((height + 1) <= maxZ) {
                world.setMaterialAt(x, y, height + 1, MOSS_BLOCK);
            }
            if (! existingMaterial.isNamed(MC_WATER)) {
                switch (rng.nextInt(50)) {
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                        renderObjectInverted(world, platform, GLOW_BERRIES.realise(rng.nextInt(Math.min(5, drySpaceAvailable)) + 1, platform), x, y, height);
                        break;
                    case 5:
                        renderObject(world, dimension, platform, SPORE_BLOSSOM.realise(1, platform), x, y, height);
                        break;
                }
            }
        }
        return false;
    }

    private void decorateWall(MinecraftWorld world, Random rng, int x, int y, int height, boolean inLushCave, Material existingMaterial, Material materialNorth, Material materialSouth, Material materialEast, Material materialWest) {
        if (decorateGlowLichen && decorationSettings.isEnabledAt(Decoration.GLOW_LICHEN, height) && rng.nextInt(MUSHROOM_CHANCE) == 0) {
            Material material = GLOW_LICHEN_NONE;
            if (existingMaterial.isNamed(MC_WATER)) {
                material = GLOW_LICHEN_NONE.withProperty(WATERLOGGED, true);
            }
            final List<Direction> directions = new ArrayList<>(4);
            if ((! materialNorth.veryInsubstantial) && (! materialNorth.isNamed(MC_POINTED_DRIPSTONE))) {
                directions.add(Direction.NORTH);
            }
            if ((! materialSouth.veryInsubstantial) && (! materialSouth.isNamed(MC_POINTED_DRIPSTONE))) {
                directions.add(Direction.SOUTH);
            }
            if ((! materialEast.veryInsubstantial) && (! materialEast.isNamed(MC_POINTED_DRIPSTONE))) {
                directions.add(Direction.EAST);
            }
            if ((! materialWest.veryInsubstantial) && (! materialWest.isNamed(MC_POINTED_DRIPSTONE))) {
                directions.add(Direction.WEST);
            }
            world.setMaterialAt(x, y, height, material.withProperty(directions.get(rng.nextInt(directions.size())).name().toLowerCase(), "true"));
        }
        if (inLushCave) {
            if ((! materialNorth.veryInsubstantial) && (! materialNorth.isNamed(MC_POINTED_DRIPSTONE))) {
                world.setMaterialAt(x, y - 1, height, MOSS_BLOCK);
            }
            if ((! materialSouth.veryInsubstantial) && (! materialSouth.isNamed(MC_POINTED_DRIPSTONE))) {
                world.setMaterialAt(x, y + 1, height, MOSS_BLOCK);
            }
            if ((! materialEast.veryInsubstantial) && (! materialEast.isNamed(MC_POINTED_DRIPSTONE))) {
                world.setMaterialAt(x + 1, y, height, MOSS_BLOCK);
            }
            if ((! materialWest.veryInsubstantial) && (! materialWest.isNamed(MC_POINTED_DRIPSTONE))) {
                world.setMaterialAt(x - 1, y, height, MOSS_BLOCK);
            }
        }
    }

    private void renderStalagmite(MinecraftWorld world, int x, int y, int height, int length, Random rng) {
        if ((height - 1) >= minZ) {
            world.setMaterialAt(x, y, height - 1, DRIPSTONE_BLOCK);
        }
        for (int dz = -2; dz <= 0; dz++) {
            if ((height + dz) < minZ) {
                continue;
            }
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (((dx != 0) || (dy != 0) || (dz != -1)) && (rng.nextInt(3) > 0)) {
                        final Material material = world.getMaterialAt(x + dx, y + dy, height + dz);
                        if (material.opaque && material.solid && material.natural && (material != DRIPSTONE_BLOCK)) {
                            world.setMaterialAt(x + dx, y + dy, height + dz, DRIPSTONE_BLOCK);
                        }
                    }
                }
            }
        }
        for (int dz = 0; dz < Math.max(length, 3); dz++) {
            final Material material = world.getMaterialAt(x, y, height + dz);
            if ((! material.insubstantial) && ((! material.empty)) && material.isNotNamed(MC_WATER)) {
                length = dz;
                break;
            }
        }
        for (int dz = 0; dz < length; dz++) {
            if (dz == (length - 1)) {
                setWaterloggedBlock(world, x, y, height + dz, POINTED_DRIPSTONE_UP_TIP);
            } else if (dz == (length - 2)) {
                setWaterloggedBlock(world, x, y, height + dz, POINTED_DRIPSTONE_UP_FRUSTUM);
            } else if (dz == 0) {
                setWaterloggedBlock(world, x, y, height + dz, POINTED_DRIPSTONE_UP_BASE);
            } else {
                setWaterloggedBlock(world, x, y, height + dz, POINTED_DRIPSTONE_UP_MIDDLE);
            }
        }
    }

    private void renderStalactite(MinecraftWorld world, int x, int y, int height, int length, Random rng) {
        if ((height + 1) <= maxZ) {
            world.setMaterialAt(x, y, height + 1, DRIPSTONE_BLOCK);
        }
        for (int dz = 0; dz <= 2; dz++) {
            if ((height + dz) > maxZ) {
                continue;
            }
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (((dx != 0) || (dy != 0) || (dz != 1)) && (rng.nextInt(3) > 0)) {
                        final Material material = world.getMaterialAt(x + dx, y + dy, height + dz);
                        if (material.opaque && material.solid && material.natural && (material != DRIPSTONE_BLOCK)) {
                            world.setMaterialAt(x + dx, y + dy, height + dz, DRIPSTONE_BLOCK);
                        }
                    }
                }
            }
        }
        for (int dz = 0; dz < length; dz++) {
            final Material material = world.getMaterialAt(x, y, height - dz);
            if ((! material.insubstantial) && ((! material.empty))) {
                length = dz;
                break;
            }
        }
        for (int dz = 0; dz < length; dz++) {
            if (dz == (length - 1)) {
                setWaterloggedBlock(world, x, y, height - dz, POINTED_DRIPSTONE_DOWN_TIP);
            } else if (dz == (length - 2)) {
                setWaterloggedBlock(world, x, y, height - dz, POINTED_DRIPSTONE_DOWN_FRUSTUM);
            } else if (dz == 0) {
                setWaterloggedBlock(world, x, y, height - dz, POINTED_DRIPSTONE_DOWN_BASE);
            } else {
                setWaterloggedBlock(world, x, y, height - dz, POINTED_DRIPSTONE_DOWN_MIDDLE);
            }
        }
    }

    private void setWaterloggedBlock(MinecraftWorld world, int x, int y, int height, Material material) {
        world.setMaterialAt(x, y, height, material.withProperty(WATERLOGGED, world.getMaterialAt(x, y, height).isNamed(MC_WATER)));
    }

    public static class CaveDecorationSettings implements java.io.Serializable, Cloneable {
        public CaveDecorationSettings() {
            enabledDecorations.put(Decoration.BROWN_MUSHROOM, null);
            enabledDecorations.put(Decoration.GLOW_LICHEN, null);
        }

        /**
         * Temporary simple constructor.
         */
        public CaveDecorationSettings(boolean brownMushrooms, boolean glowLichen, boolean lushCavePatches, boolean dripstoneCavePatches) {
            if (brownMushrooms) {
                enabledDecorations.put(Decoration.BROWN_MUSHROOM, null);
            }
            if (glowLichen) {
                enabledDecorations.put(Decoration.GLOW_LICHEN, null);
            }
            if (lushCavePatches) {
                enabledDecorations.put(LUSH_CAVE_PATCHES, null);
                noiseSettingsMap.put(LUSH_CAVE_PATCHES, new NoiseSettings(0L, 500, 1, 2.5f));
            }
            if (dripstoneCavePatches) {
                enabledDecorations.put(Decoration.DRIPSTONE_CAVE_PATCHES, null);
                noiseSettingsMap.put(Decoration.DRIPSTONE_CAVE_PATCHES, new NoiseSettings(1L, 500, 1, 2.5f));
            }
        }

        public boolean isEnabled(Decoration decoration) {
            return enabledDecorations.containsKey(decoration);
        }

        public boolean isEnabledAt(Decoration decoration, int height) {
            if (enabledDecorations.containsKey(decoration)) {
                final int[] limits = enabledDecorations.get(decoration);
                return (limits == null) || ((height >= limits[0]) && (height <= limits[1]));
            } else {
                return false;
            }
        }

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
                throw new MDCWrappingRuntimeException(e);
            }
        }

        /**
         * If the key is present, the decoration is enabled. If the value is {@code null}, it is enabled everywhere;
         * otherwise the value is an array with the minimum and maximum levels at which to apply the decoration.
         */
        final Map<Decoration, int[]> enabledDecorations = new HashMap<>();
        final Map<Decoration, NoiseSettings> noiseSettingsMap = new HashMap<>();

        private static final long serialVersionUID = 1L;

        public enum Decoration {
            BROWN_MUSHROOM, GLOW_LICHEN, LUSH_CAVE_PATCHES, DRIPSTONE_CAVE_PATCHES
        }
    }

    static class State {
        long seed;
        Tile tile;
        int maxY, waterLevel;
        boolean glassCeiling, breachedCeiling, surfaceBreaking, leaveWater, previousBlockInCavern, floodWithLava;
    }

    protected final boolean decorationEnabled;

    private final CaveDecorationSettings decorationSettings;
    private final NoiseHeightMap lushCaveNoise, dripstoneCaveNoise;
    private final BiomeUtils biomeUtils;
    private final boolean decorateBrownMushrooms, decorateGlowLichen, decorateLushCaves, decorateDripstoneCaves, setBiomes;

    private static final ThreadLocal<State> STATE_HOLDER = new ThreadLocal<>();
    private static final int MUSHROOM_CHANCE = 250;
    private static final float LUSH_CAVE_THRESHOLD = 600;
    private static final float DRIPSTONE_CAVE_THRESHOLD = 675;
    private static final Set<String> SUPPORTS_DRIPSTONE = ImmutableSet.of(MC_BEDROCK, MC_STONE, MC_GRANITE, MC_ANDESITE, MC_DIORITE, MC_CALCITE, MC_BASALT, MC_DEEPSLATE,
            MC_COAL_ORE, MC_IRON_ORE, MC_GOLD_ORE, MC_REDSTONE_ORE, MC_DIAMOND_ORE, MC_LAPIS_ORE, MC_EMERALD_ORE, MC_NETHER_QUARTZ_ORE, MC_COPPER_ORE,
            MC_DEEPSLATE_COAL_ORE, MC_DEEPSLATE_IRON_ORE, MC_DEEPSLATE_GOLD_ORE, MC_DEEPSLATE_REDSTONE_ORE, MC_DEEPSLATE_DIAMOND_ORE, MC_DEEPSLATE_LAPIS_ORE, MC_DEEPSLATE_EMERALD_ORE, MC_DEEPSLATE_COPPER_ORE);
}