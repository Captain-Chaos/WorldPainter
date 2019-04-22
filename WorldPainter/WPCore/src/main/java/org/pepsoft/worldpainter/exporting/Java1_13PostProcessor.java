package org.pepsoft.worldpainter.exporting;

import org.pepsoft.minecraft.Material;
import org.pepsoft.util.Box;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.worldpainter.objects.MinecraftWorldObject;

import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.minecraft.Material.*;

/**
 * Helper class which can post process a fully rendered Minecraft 1.13 or later
 * map to make sure it doesn't violate any Minecraft rules.
 *
 * Created by Pepijn Schmitz on 15-06-15.
 */
public class Java1_13PostProcessor extends PostProcessor {
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
     * @param minecraftWorld The <code>MinecraftWorld</code> to post process.
     * @param volume The three dimensional area of the world to post process.
     * @param progressReceiver The optional progress receiver to which to report
     *                         progress. May be <code>null</code>.
     * @throws ProgressReceiver.OperationCancelled If the progress receiver
     * threw an <code>OperationCancelled</code> exception.
     */
    @Override
    public void postProcess(MinecraftWorld minecraftWorld, Box volume, ProgressReceiver progressReceiver) throws ProgressReceiver.OperationCancelled {
        if (! enabled) {
            return;
        }
        if (progressReceiver != null) {
            progressReceiver.setMessage("Enforcing Minecraft rules on exported blocks");
        }
        final int worldMaxZ = minecraftWorld.getMaxHeight() - 1;
        final int x1, y1, x2, y2, minZ, maxZ;
        // TODO: make these configurable:
        final FloatMode sandMode = "false".equalsIgnoreCase(System.getProperty("org.pepsoft.worldpainter.supportSand")) ? FloatMode.LEAVE_FLOATING : FloatMode.SUPPORT;
        final FloatMode gravelMode = FloatMode.LEAVE_FLOATING;
        final FloatMode cementMode = FloatMode.LEAVE_FLOATING;
        final FloatMode waterMode = FloatMode.LEAVE_FLOATING;
        final FloatMode lavaMode = FloatMode.LEAVE_FLOATING;
        if (minecraftWorld instanceof MinecraftWorldObject) {
            // Special support for MinecraftWorldObjects to constrain the area
            // further
            Box objectVolume = ((MinecraftWorldObject) minecraftWorld).getVolume();
            objectVolume.intersect(volume);
            if (objectVolume.isEmpty()) {
                // The specified area does not intersect the volume encompassed
                // by the minecraftWorld. Weird, but it means we have nothing to
                // do
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
        for (int x = x1; x <= x2; x ++) {
            for (int y = y1; y <= y2; y++) {
                Material materialBelow = AIR;
                Material materialAbove = minecraftWorld.getMaterialAt(x, y, minZ);
                // TODO: only do this for non-bottomless worlds:
//                if ((minZ == 0) && (blockTypeAbove != BLK_BEDROCK) && (blockTypeAbove != BLK_AIR) && (blockTypeAbove != BLK_STATIONARY_WATER) && (blockTypeAbove != BLK_STATIONARY_LAVA)) {
//                    logger.warn("Non-bedrock block @ " + x + "," + y + ",0: " + BLOCKS[blockTypeAbove].name);
//                }
                for (int z = minZ; z <= maxZ; z++) {
                    Material material = materialAbove;
                    materialAbove = (z < worldMaxZ) ? minecraftWorld.getMaterialAt(x, y, z + 1) : AIR;
                    if (((materialBelow.isNamed(MC_GRASS_BLOCK)) || (materialBelow.isNamed(MC_MYCELIUM)) || (materialBelow.isNamed(MC_FARMLAND))) && ((material.isNamed(MC_WATER)) || (material == ICE) || material.opaque)) {
                        // Covered grass, mycelium or tilled earth block, should
                        // be dirt
                        minecraftWorld.setMaterialAt(x, y, z - 1, DIRT);
                        materialBelow = DIRT;
                    }
                    // TODO: can  we do this more efficiently?
                    if (materialBelow.hasProperty(MC_SNOWY)) {
                        // The material below has a "snowy" property, so make sure it is set correctly
                        if (material.isNamed(MC_SNOW) || material.isNamed(MC_SNOW_BLOCK)) {
                            // It should be snowy, change it if it isn't
                            if (! materialBelow.getProperty(SNOWY)) {
                                materialBelow = materialBelow.withProperty(SNOWY, true);
                                minecraftWorld.setMaterialAt(x, y, z - 1, materialBelow);
                            }
                        } else {
                            // It should NOT be snowy, change it if it is
                            if (materialBelow.getProperty(SNOWY)) {
                                materialBelow = materialBelow.withProperty(SNOWY, false);
                                minecraftWorld.setMaterialAt(x, y, z - 1, materialBelow);
                            }
                        }
                    }
                    // Special case for backwards compatibility: turn legacy
                    // upper large flower blocks into modern ones
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
                                        throw new UnsupportedOperationException("Don't know how to support cement yet");
                                    default:
                                        // Do nothing
                                        break;
                                }
                            }
                            break;
                        case MC_WATER:
                            if (materialBelow.veryInsubstantial) {
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
                            break;
                        case MC_LAVA:
                            if (materialBelow.veryInsubstantial) {
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
                        case MC_DEAD_BUSH:
                            if ((materialBelow != SAND) && (materialBelow != RED_SAND) && (materialBelow != DIRT) && (materialBelow != PODZOL) && (materialBelow != PERMADIRT) && (! materialBelow.name.endsWith("_terracotta")) && (materialBelow != TERRACOTTA)) {
                                // Dead shrubs can only exist on materials
                                // present in Mesa biome
                                minecraftWorld.setMaterialAt(x, y, z, AIR);
                                material = AIR;
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
                            if ((materialBelow != GRASS_BLOCK) && (materialBelow != DIRT) && (materialBelow != PODZOL) && (materialBelow != PERMADIRT)) {
                                // Tall grass and flowers can only exist on Grass or Dirt blocks
                                minecraftWorld.setMaterialAt(x, y, z, AIR);
                                material = AIR;
                            }
                            break;
                        case MC_RED_MUSHROOM:
                        case MC_BROWN_MUSHROOM:
                            if ((materialBelow != GRASS_BLOCK) && (materialBelow != DIRT) && (materialBelow != PODZOL) && (materialBelow != PERMADIRT) && (materialBelow != MYCELIUM) && (materialBelow != STONE) && (materialBelow != GRANITE) && (materialBelow != DIORITE) && (materialBelow != ANDESITE)) {
                                // Mushrooms can only exist on Grass, Dirt, Mycelium or Stone (in caves) blocks
                                minecraftWorld.setMaterialAt(x, y, z, AIR);
                                material = AIR;
                            }
                            break;
                        case MC_SNOW:
                            if ((materialBelow == ICE) || materialBelow.isNamed(MC_SNOW) || (materialBelow == AIR) || (materialBelow == PACKED_ICE)) {
                                // Snow can't be on ice, or another snow block, or air
                                // (well it could be, but it makes no sense, would
                                // disappear when touched, and it makes this algorithm
                                // remove stacks of snow blocks correctly)
                                minecraftWorld.setMaterialAt(x, y, z, AIR);
                                material = AIR;
                            }
                            break;
                        case MC_WHEAT:
                            if (materialBelow.isNotNamed(MC_FARMLAND)) {
                                // Wheat can only exist on Tilled Dirt blocks
                                minecraftWorld.setMaterialAt(x, y, z, AIR);
                                material = AIR;
                            }
                            break;
                        case MC_SUNFLOWER:
                        case MC_LILAC:
                        case MC_TALL_GRASS:
                        case MC_LARGE_FERN:
                        case MC_ROSE_BUSH:
                        case MC_PEONY:
                            // TODOMC13 recognise legacy "top half" block and replace it with the proper block
                            if (material.getProperty(HALF).equals("upper")) {
                                // Top half of double high plant.
                                if (materialBelow.isNotNamedSameAs(material)
                                        || (! materialBelow.getProperty(HALF).equals("lower"))) {
                                    // There is not a corresponding lower half
                                    // below; remove this block
                                    minecraftWorld.setMaterialAt(x, y, z, AIR);
                                    material = AIR;
                                }
                            } else {
                                // Otherwise: lower half of double high plant;
                                // check there's a corresponding top half above
                                // and grass or dirt below
                                if (materialAbove.isNamedSameAs(material) && materialAbove.getProperty(HALF).equals("upper")) {
                                    if (materialBelow.isNotNamed(MC_GRASS_BLOCK) && (materialBelow != DIRT) && (materialBelow.isNotNamed(MC_PODZOL)) && (materialBelow != PERMADIRT)) {
                                        // Double high plants can (presumably; TODO:
                                        // check) only exist on grass or dirt
                                        minecraftWorld.setMaterialAt(x, y, z, AIR);
                                        material = AIR;
                                    }
                                } else {
                                    // There's a non-double high plant block above;
                                    // replace this block with air
                                    minecraftWorld.setMaterialAt(x, y, z, AIR);
                                    material = AIR;
                                }
                            }
                            break;
                        case MC_CACTUS:
                            if ((materialBelow != SAND) && (materialBelow != RED_SAND) && materialBelow.isNotNamed(MC_CACTUS)) {
                                // Cactus blocks can only be on top of sand or other cactus blocks
                                minecraftWorld.setMaterialAt(x, y, z, AIR);
                                material = AIR;
                            }
                            break;
                        case MC_SUGAR_CANE:
                            if (materialBelow.isNotNamed(MC_GRASS_BLOCK) && (materialBelow != DIRT) && materialBelow.isNotNamed(MC_PODZOL) && (materialBelow != PERMADIRT) && (materialBelow != SAND) && (materialBelow != RED_SAND) && materialBelow.isNotNamed(MC_SUGAR_CANE)) {
                                // Sugar cane blocks can only be on top of grass, dirt, sand or other sugar cane blocks
                                minecraftWorld.setMaterialAt(x, y, z, AIR);
                                material = AIR;
                            }
                            break;
                        case MC_NETHER_WART:
                            if (materialBelow != SOUL_SAND) {
                                // Nether wart blocks can only be on top of soul sand
                                minecraftWorld.setMaterialAt(x, y, z, AIR);
                                material = AIR;
                            }
                            break;
                        case MC_CHORUS_FLOWER:
                        case MC_CHORUS_PLANT:
                            if ((materialBelow != END_STONE) && (materialBelow.isNotNamed(MC_CHORUS_PLANT))) {
                                // Chorus flower and plant blocks can only be on top of end stone or other chorus plant blocks
                                minecraftWorld.setMaterialAt(x, y, z, AIR);
                                material = AIR;
                            }
                            break;
                        case MC_FIRE:
                            // We don't know which blocks are flammable, but at
                            // least check whether the fire is not floating in
                            // the air
                            if ((materialBelow == AIR)
                                    && (materialAbove == AIR)
                                    && (minecraftWorld.getMaterialAt(x - 1, y, z) == AIR)
                                    && (minecraftWorld.getMaterialAt(x + 1, y, z) == AIR)
                                    && (minecraftWorld.getMaterialAt(x, y - 1, z) == AIR)
                                    && (minecraftWorld.getMaterialAt(x, y + 1, z) == AIR)) {
                                minecraftWorld.setMaterialAt(x, y, z, AIR);
                                material = AIR;
                            }
                            break;
                    }
                    materialBelow = material;
                }
            }
            if (progressReceiver != null) {
                progressReceiver.setProgress((float) (x - x1 + 1) / (x2 - x1 + 1));
            }
        }
    }
}