package org.pepsoft.worldpainter.platforms;

import org.pepsoft.minecraft.Material;
import org.pepsoft.util.Box;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.worldpainter.exporting.ExportSettings;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;
import org.pepsoft.worldpainter.exporting.PostProcessor;
import org.pepsoft.worldpainter.objects.MinecraftWorldObject;
import org.pepsoft.worldpainter.platforms.JavaExportSettings.FloatMode;

import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.minecraft.Material.*;

/**
 * Helper class which can post process a fully rendered Minecraft 1.15 or later
 * map to make sure it doesn't violate any Minecraft rules.
 *
 * Created by Pepijn Schmitz on 15-06-15.
 */
public class Java1_15PostProcessor extends PostProcessor {
    /**
     * Post process (part of) a {@link MinecraftWorld} to make sure it conforms
     * to Minecraft's rules. For instance:
     *
     * <ul><li>Remove plants that are on the wrong underground or floating
     * in air.
     * <li>Change the lowest block of a column of Sand to Sandstone.
     * <li>Remove snow on top of blocks which don't support snow, or floating in
     * air.
     * <li>Change covered grass and mycelium blocks to dirt.
     * </ul>
     *
     * @param minecraftWorld The {@code MinecraftWorld} to post process.
     * @param volume The three dimensional area of the world to post process.
     * @param exportSettings The export settings to apply.
     * @param progressReceiver The optional progress receiver to which to report
     *                         progress. May be {@code null}.
     * @throws ProgressReceiver.OperationCancelled If the progress receiver
     * threw an {@code OperationCancelled} exception.
     */
    @Override
    public void postProcess(MinecraftWorld minecraftWorld, Box volume, ExportSettings exportSettings, ProgressReceiver progressReceiver) throws ProgressReceiver.OperationCancelled {
        if (! enabled) {
            return;
        }
        if (progressReceiver != null) {
            progressReceiver.setMessage("Post processing exported blocks (first pass)");
        }
        final int worldMinZ = minecraftWorld.getMinHeight(), worldMaxZ = minecraftWorld.getMaxHeight() - 1;
        final int x1, y1, x2, y2, minZ, maxZ;
        final JavaExportSettings settings = (exportSettings instanceof JavaExportSettings) ? (JavaExportSettings) exportSettings : new JavaExportSettings();
        final FloatMode sandMode = "false".equalsIgnoreCase(System.getProperty("org.pepsoft.worldpainter.supportSand")) ? FloatMode.LEAVE_FLOATING : settings.sandMode;
        final FloatMode gravelMode = settings.gravelMode;
        final FloatMode cementMode = settings.cementMode;
        final FloatMode waterMode = settings.waterMode;
        final FloatMode lavaMode = settings.lavaMode;
        final boolean flowLava = settings.flowLava, flowWater = settings.flowWater,
                makeAllLeavesPersistent = settings.makeAllLeavesPersistent, leavePlants = settings.leavePlants;
        if (minecraftWorld instanceof MinecraftWorldObject) {
            // Special support for MinecraftWorldObjects to constrain the area further
            Box objectVolume = ((MinecraftWorldObject) minecraftWorld).getVolume();
            objectVolume.intersect(volume);
            if (objectVolume.isEmpty()) {
                // The specified area does not intersect the volume encompassed by the minecraftWorld. Weird, but it
                // means we have nothing to do
                return;
            } else {
                x1 = objectVolume.getX1();
                x2 = objectVolume.getX2() - 1;
                y1 = objectVolume.getY1();
                y2 = objectVolume.getY2() - 1;
                minZ = objectVolume.getZ1();
                maxZ = objectVolume.getZ2() - 1;
            }
        } else {
            x1 = volume.getX1();
            y1 = volume.getY1();
            x2 = volume.getX2() - 1;
            y2 = volume.getY2() - 1;
            minZ = volume.getZ1();
            maxZ = volume.getZ2() - 1;
        }

        // Pass 1
        for (int x = x1; x <= x2; x ++) {
            for (int y = y1; y <= y2; y++) {
                // Iterate over one column from bottom to top
                Material materialBelow = (minZ <= worldMinZ) ? AIR : minecraftWorld.getMaterialAt(x, y, minZ - 1);
                Material materialAbove = (minZ < worldMinZ) ? AIR : minecraftWorld.getMaterialAt(x, y, minZ);
                // TODO: only do this for non-bottomless worlds:
//                if ((minZ == 0) && (blockTypeAbove != BLK_BEDROCK) && (blockTypeAbove != BLK_AIR) && (blockTypeAbove != BLK_STATIONARY_WATER) && (blockTypeAbove != BLK_STATIONARY_LAVA)) {
//                    logger.warn("Non-bedrock block @ " + x + "," + y + ",0: " + BLOCKS[blockTypeAbove].name);
//                }
                final int columnMaxZ = Math.min(minecraftWorld.getHighestNonAirBlock(x, y), maxZ);
                for (int z = minZ; z <= columnMaxZ; z++) {
                    Material material = materialAbove;
                    materialAbove = (z < worldMaxZ) ? minecraftWorld.getMaterialAt(x, y, z + 1) : AIR;
                    if (((materialBelow.isNamedOneOf(MC_GRASS_BLOCK, MC_MYCELIUM, MC_FARMLAND, MC_DIRT_PATH, MC_GRASS_PATH)))
                            && (material.containsWater() || (material == ICE) || material.opaque)) {
                        // Covered grass, mycelium, tilled earth etc. blocks should be dirt
                        minecraftWorld.setMaterialAt(x, y, z - 1, DIRT);
                        materialBelow = DIRT;
                    } else if (((materialBelow.isNamedOneOf(MC_WARPED_NYLIUM, MC_CRIMSON_NYLIUM)))
                            && (material.opaque)) {
                        // Covered nylium should be netherrack
                        minecraftWorld.setMaterialAt(x, y, z - 1, NETHERRACK);
                        materialBelow = NETHERRACK;
                    } else if (makeAllLeavesPersistent && material.leafBlock && (! material.is(PERSISTENT))) {
                        material = material.withProperty(PERSISTENT, true);
                        minecraftWorld.setMaterialAt(x, y, z, material);
                    }
                    if (materialBelow.hasPropertySnowy) {
                        // The material below has a "snowy" property, so make sure it is set correctly
                        if (material.isNamed(MC_SNOW) || material.isNamed(MC_SNOW_BLOCK) || material.isNamedOneOf(MC_POWDER_SNOW)) {
                            // It should be snowy, change it if it isn't
                            if (! materialBelow.is(SNOWY)) {
                                materialBelow = materialBelow.withProperty(SNOWY, true);
                                minecraftWorld.setMaterialAt(x, y, z - 1, materialBelow);
                            }
                        } else {
                            // It should NOT be snowy, change it if it is
                            if (materialBelow.is(SNOWY)) {
                                materialBelow = materialBelow.withProperty(SNOWY, false);
                                minecraftWorld.setMaterialAt(x, y, z - 1, materialBelow);
                            }
                        }
                    }
                    // Special case for backwards compatibility: turn legacy upper large flower blocks into modern ones
                    if ((material.blockType == BLK_LARGE_FLOWERS) && ((material.data & 0x8) == 0x8) && (materialBelow.blockType == BLK_LARGE_FLOWERS)) {
                        material = materialBelow.withProperty(HALF, "upper");
                        minecraftWorld.setMaterialAt(x, y, z, material);
                    }
                    switch (material.name) {
                        case MC_SAND:
                        case MC_RED_SAND:
                            if (materialBelow.veryInsubstantial) {
                                switch (sandMode) {
                                    case DROP:
                                        dropBlock(minecraftWorld, x, y, z);
                                        material = AIR;
                                        break;
                                    case SUPPORT:
                                        // All unsupported sand should be supported by sandstone
                                        minecraftWorld.setMaterialAt(x, y, z, (material == RED_SAND) ? RED_SANDSTONE : SANDSTONE);
                                        material = minecraftWorld.getMaterialAt(x, y, z);
                                        break;
                                    default:
                                        // Do nothing
                                        break;
                                }
                            }
                            break;
                        case MC_GRAVEL:
                            if (materialBelow.veryInsubstantial) {
                                switch (gravelMode) {
                                    case DROP:
                                        dropBlock(minecraftWorld, x, y, z);
                                        material = AIR;
                                        break;
                                    case SUPPORT:
                                        // All unsupported gravel should be supported by stone
                                        minecraftWorld.setMaterialAt(x, y, z, STONE);
                                        material = STONE;
                                        break;
                                    default:
                                        // Do nothing
                                        break;
                                }
                            }
                            break;
                        case MC_WHITE_CONCRETE_POWDER:
                        case MC_ORANGE_CONCRETE_POWDER:
                        case MC_MAGENTA_CONCRETE_POWDER:
                        case MC_LIGHT_BLUE_CONCRETE_POWDER:
                        case MC_YELLOW_CONCRETE_POWDER:
                        case MC_LIME_CONCRETE_POWDER:
                        case MC_PINK_CONCRETE_POWDER:
                        case MC_GRAY_CONCRETE_POWDER:
                        case MC_LIGHT_GRAY_CONCRETE_POWDER:
                        case MC_CYAN_CONCRETE_POWDER:
                        case MC_PURPLE_CONCRETE_POWDER:
                        case MC_BLUE_CONCRETE_POWDER:
                        case MC_BROWN_CONCRETE_POWDER:
                        case MC_GREEN_CONCRETE_POWDER:
                        case MC_RED_CONCRETE_POWDER:
                        case MC_BLACK_CONCRETE_POWDER:
                            if (materialBelow.veryInsubstantial) {
                                switch (cementMode) {
                                    case DROP:
                                        dropBlock(minecraftWorld, x, y, z);
                                        material = AIR;
                                        break;
                                    case SUPPORT:
                                        minecraftWorld.setMaterialAt(x, y, z, STONE);
                                        material = STONE;
                                        break;
                                    default:
                                        // Do nothing
                                        break;
                                }
                            }
                            break;
                        case MC_LAVA:
                            if (materialBelow.veryInsubstantial && materialBelow.isNotNamed(MC_LAVA)) {
                                switch (lavaMode) {
                                    case DROP:
                                        dropFluid(minecraftWorld, x, y, z);
                                        break;
                                    case SUPPORT:
                                        throw new UnsupportedOperationException("Don't know how to support lava yet");
                                    default:
                                        // Do nothing
                                        break;
                                }
                            }
                            break;
                        case MC_SNOW:
                            if ((materialBelow == ICE) || materialBelow.isNamed(MC_SNOW) || (materialBelow == AIR) || (materialBelow == PACKED_ICE)) {
                                // Snow can't be on ice, or another snow block, or air (well it could be, but it makes
                                // no sense, would disappear when touched, and it makes this algorithm remove stacks of
                                // snow blocks correctly)
                                material = clearBlock(minecraftWorld, x, y, z);
                            }
                            break;
                        case MC_FIRE:
                            // We don't know which blocks are flammable, but at least check whether the fire is not
                            // floating in the air
                            if ((materialBelow == AIR)
                                    && (materialAbove == AIR)
                                    && (minecraftWorld.getMaterialAt(x - 1, y, z) == AIR)
                                    && (minecraftWorld.getMaterialAt(x + 1, y, z) == AIR)
                                    && (minecraftWorld.getMaterialAt(x, y - 1, z) == AIR)
                                    && (minecraftWorld.getMaterialAt(x, y + 1, z) == AIR)) {
                                material = clearBlock(minecraftWorld, x, y, z);
                            }
                            break;
                    }
                    if (! leavePlants) {
                        if (! materialBelow.modded) {
                            switch (material.name) {
                                case MC_DEAD_BUSH:
                                    if (materialBelow.isNotNamedOneOf(MC_GRASS_BLOCK, MC_SAND, MC_RED_SAND, MC_DIRT, MC_TERRACOTTA, MC_PODZOL, MC_COARSE_DIRT, MC_ROOTED_DIRT, MC_MOSS_BLOCK, MC_MUD)
                                            && (! materialBelow.name.endsWith("_terracotta"))) {
                                        material = clearBlock(minecraftWorld, x, y, z);
                                    }
                                    break;
                                case MC_GRASS:
                                case MC_FERN:
                                case MC_DANDELION:
                                case MC_POPPY:
                                case MC_BLUE_ORCHID:
                                case MC_ALLIUM:
                                case MC_AZURE_BLUET:
                                case MC_RED_TULIP:
                                case MC_ORANGE_TULIP:
                                case MC_WHITE_TULIP:
                                case MC_PINK_TULIP:
                                case MC_OXEYE_DAISY:
                                case MC_CORNFLOWER:
                                case MC_LILY_OF_THE_VALLEY:
                                case MC_WITHER_ROSE:
                                case MC_SWEET_BERRY_BUSH:
                                    if (materialBelow.isNotNamedOneOf(MC_GRASS_BLOCK, MC_DIRT, MC_COARSE_DIRT, MC_PODZOL, MC_FARMLAND, MC_ROOTED_DIRT, MC_MOSS_BLOCK, MC_MUD)) {
                                        material = clearBlock(minecraftWorld, x, y, z);
                                    }
                                    break;
                                case MC_RED_MUSHROOM:
                                case MC_BROWN_MUSHROOM:
                                    if (materialBelow.isNotNamedOneOf(MC_GRASS_BLOCK, MC_DIRT, MC_PODZOL, MC_COARSE_DIRT, MC_MYCELIUM, MC_STONE, MC_GRANITE, MC_DIORITE, MC_ANDESITE)) {
                                        material = clearBlock(minecraftWorld, x, y, z);
                                    }
                                    break;
                                case MC_WHEAT:
                                    if (materialBelow.isNotNamed(MC_FARMLAND)) {
                                        material = clearBlock(minecraftWorld, x, y, z);
                                    }
                                    break;
                                case MC_SUNFLOWER:
                                case MC_LILAC:
                                case MC_TALL_GRASS:
                                case MC_LARGE_FERN:
                                case MC_ROSE_BUSH:
                                case MC_PEONY:
                                    if ("lower".equals(material.getProperty(HALF))) {
                                        // Lower half of double high plant; check there's grass or dirt below
                                        if (materialBelow.isNotNamedOneOf(MC_GRASS_BLOCK, MC_DIRT, MC_COARSE_DIRT, MC_PODZOL, MC_FARMLAND, MC_ROOTED_DIRT, MC_MOSS_BLOCK, MC_MUD)) {
                                            // Double high plants can (presumably) only exist on grass or dirt // TODO: check
                                            // The upper block will be removed below in the next iteration
                                            material = clearBlock(minecraftWorld, x, y, z);
                                        }
                                    }
                                    break;
                                case MC_CACTUS:
                                    if ((materialBelow != SAND) && (materialBelow != RED_SAND) && materialBelow.isNotNamed(MC_CACTUS)) {
                                        // Cactus blocks can only be on top of sand or other cactus blocks
                                        material = clearBlock(minecraftWorld, x, y, z);
                                    }
                                    break;
                                case MC_SUGAR_CANE:
                                    if (materialBelow.isNotNamedOneOf(MC_GRASS_BLOCK, MC_DIRT, MC_PODZOL, MC_COARSE_DIRT, MC_ROOTED_DIRT, MC_MOSS_BLOCK, MC_MUD, MC_SAND, MC_RED_SAND, MC_SUGAR_CANE)) {
                                        // Sugar cane blocks can only be on top of grass, dirt, sand or other sugar cane
                                        // blocks
                                        material = clearBlock(minecraftWorld, x, y, z);
                                    }
                                    break;
                                case MC_NETHER_WART:
                                    if (materialBelow != SOUL_SAND) {
                                        // Nether wart blocks can only be on top of soul sand
                                        material = clearBlock(minecraftWorld, x, y, z);
                                    }
                                    break;
                                case MC_CHORUS_FLOWER:
                                case MC_CHORUS_PLANT:
                                    if ((materialBelow != END_STONE) && materialBelow.isNotNamed(MC_CHORUS_PLANT)) {
                                        // Chorus flower and plant blocks can only be on top of end stone or other chorus
                                        // plant blocks
                                        material = clearBlock(minecraftWorld, x, y, z);
                                    }
                                    break;
                                case MC_KELP:
                                case MC_KELP_PLANT:
                                case MC_TALL_SEAGRASS:
                                    if (! ((materialBelow.solid && materialBelow.opaque && materialBelow.natural) || materialBelow.isNamedOneOf(MC_KELP, MC_KELP_PLANT, MC_TALL_SEAGRASS))) {
                                        material = clearBlock(minecraftWorld, x, y, z);
                                    }
                                    break;
                                case MC_SEAGRASS:
                                case MC_SEA_PICKLE:
                                case MC_TUBE_CORAL:
                                case MC_BRAIN_CORAL:
                                case MC_BUBBLE_CORAL:
                                case MC_FIRE_CORAL:
                                case MC_HORN_CORAL:
                                case MC_DEAD_TUBE_CORAL:
                                case MC_DEAD_BRAIN_CORAL:
                                case MC_DEAD_BUBBLE_CORAL:
                                case MC_DEAD_FIRE_CORAL:
                                case MC_DEAD_HORN_CORAL:
                                case MC_TUBE_CORAL_FAN:
                                case MC_BRAIN_CORAL_FAN:
                                case MC_BUBBLE_CORAL_FAN:
                                case MC_FIRE_CORAL_FAN:
                                case MC_HORN_CORAL_FAN:
                                case MC_DEAD_TUBE_CORAL_FAN:
                                case MC_DEAD_BRAIN_CORAL_FAN:
                                case MC_DEAD_BUBBLE_CORAL_FAN:
                                case MC_DEAD_FIRE_CORAL_FAN:
                                case MC_DEAD_HORN_CORAL_FAN:
                                    if (! (materialBelow.solid && materialBelow.opaque && materialBelow.natural)) {
                                        material = clearBlock(minecraftWorld, x, y, z);
                                    }
                                    break;
                                case MC_LILY_PAD:
                                    if (! materialBelow.containsWater()) {
                                        material = clearBlock(minecraftWorld, x, y, z);
                                    }
                                    break;
                            }
                        }
                        if (material.vegetation && material.isPropertySet(MC_HALF)) {
                            // Check that all lower and upper halves of plants have their corresponding opposite half
                            if ("upper".equals(material.getProperty(HALF))) {
                                if (materialBelow.isNotNamedSameAs(material) || (! materialBelow.isPropertySet(MC_HALF)) || (! "lower".equals(materialBelow.getProperty(HALF)))) {
                                    material = clearBlock(minecraftWorld, x, y, z);
                                }
                            } else {
                                if (materialAbove.isNotNamedSameAs(material) || (! materialAbove.isPropertySet(MC_HALF)) || (! "upper".equals(materialAbove.getProperty(HALF)))) {
                                    material = clearBlock(minecraftWorld, x, y, z);
                                }
                            }
                        }
                    }
                    if (material.containsWater() && materialBelow.veryInsubstantial && (! materialBelow.containsWater())) {
                        switch (waterMode) {
                            case DROP:
                                dropFluid(minecraftWorld, x, y, z);
                                break;
                            case SUPPORT:
                                throw new UnsupportedOperationException("Don't know how to support water yet");
                            default:
                                // Do nothing
                                break;
                        }
                    }
                    materialBelow = material;
                }
            }
            if (progressReceiver != null) {
                progressReceiver.setProgress((float) (x - x1 + 1) / (x2 - x1 + 1) * 0.75f);
            }
        }

        if (flowWater || flowLava) {
            if (progressReceiver != null) {
                progressReceiver.setMessage("Post processing exported blocks (fluids pass)");
            }
            // Pass 2 (water and lava pass)
            for (int x = x1; x <= x2; x++) {
                for (int y = y1; y <= y2; y++) {
                    // Iterate over one column from bottom to top
                    // Water at the lowest level can always flow down into the void, so skip that level
                    Material materialBelow = (minZ + 1 <= worldMinZ) ? AIR : minecraftWorld.getMaterialAt(x, y, minZ);
                    Material materialAbove = minecraftWorld.getMaterialAt(x, y, minZ + 1);
                    final int columnMaxZ = Math.min(minecraftWorld.getHighestNonAirBlock(x, y), maxZ);
                    for (int z = minZ + 1; z <= columnMaxZ; z++) {
                        Material material = materialAbove;
                        materialAbove = (z < worldMaxZ) ? minecraftWorld.getMaterialAt(x, y, z + 1) : AIR;
                        if (flowWater && containsAnyWater(material) && (! isWaterContained(minecraftWorld, x, y, z, materialBelow))) {
                            minecraftWorld.markForUpdateWorld(x, y, z);
                        } else if (flowLava && material.isNamed(MC_LAVA) && (! isLavaContained(minecraftWorld, x, y, z, materialBelow))) {
                            minecraftWorld.markForUpdateWorld(x, y, z);
                        }
                        materialBelow = material;
                    }
                }
                if (progressReceiver != null) {
                    progressReceiver.setProgress(0.75f + (float) (x - x1 + 1) / (x2 - x1 + 1) * 0.25f);
                }
            }
        } else if (progressReceiver != null) {
            progressReceiver.setProgress(1.0f);
        }
    }

    private static Material clearBlock(MinecraftWorld minecraftWorld, int x, int y, int z) {
        if (minecraftWorld.getMaterialAt(x, y, z).containsWater()) {
            minecraftWorld.setMaterialAt(x, y, z, STATIONARY_WATER);
            return STATIONARY_WATER;
        } else {
            minecraftWorld.setMaterialAt(x, y, z, AIR);
            return AIR;
        }
    }
}