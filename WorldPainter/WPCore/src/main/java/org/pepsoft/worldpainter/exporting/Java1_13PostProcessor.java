package org.pepsoft.worldpainter.exporting;

import org.pepsoft.minecraft.Material;
import org.pepsoft.util.Box;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.worldpainter.objects.MinecraftWorldObject;

import static org.pepsoft.minecraft.Block.BLOCKS;
import static org.pepsoft.minecraft.Block.BLOCK_TRANSPARENCY;
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
                // TODO: port all properties of Blocks to Material and change the references
                for (int z = minZ; z <= maxZ; z++) {
                    Material material = materialAbove;
                    materialAbove = (z < worldMaxZ) ? minecraftWorld.getMaterialAt(x, y, z + 1) : AIR;
                    if (((materialBelow == GRASS) || (materialBelow == MYCELIUM) || (materialBelow.blockType == BLK_TILLED_DIRT)) && ((material == WATER) || (material == STATIONARY_WATER) || (material == ICE) || ((material.blockType >= 0) && (material.blockType <= HIGHEST_KNOWN_BLOCK_ID) && (BLOCK_TRANSPARENCY[material.blockType] == 15)))) {
                        // Covered grass, mycelium or tilled earth block, should
                        // be dirt. Note that unknown blocks are treated as
                        // transparent for this check so that grass underneath
                        // custom plants doesn't turn to dirt, for instance
                        minecraftWorld.setMaterialAt(x, y, z - 1, DIRT);
                        materialBelow = DIRT;
                    }
                    // TODO: can  we do this more efficiently?
                    if (materialBelow.hasProperty(SNOWY)) {
                        // The material below has a "snowy" property, so make sure it is set correctly
                        if ((material.blockType == BLK_SNOW) || (material.blockType == BLK_SNOW_BLOCK)) {
                            // It should be snowy, change it if it isn't
                            if (! materialBelow.getProperty(SNOWY)) {
                                minecraftWorld.setMaterialAt(x, y, z - 1, materialBelow.withProperty(SNOWY, true));
                            }
                        } else {
                            // It should NOT be snowy, change it if it is
                            if (materialBelow.getProperty(SNOWY)) {
                                minecraftWorld.setMaterialAt(x, y, z - 1, materialBelow.withProperty(SNOWY, false));
                            }
                        }
                    }
                    switch (material.blockType) {
                        case BLK_SAND:
                            if (BLOCKS[materialBelow.blockType].veryInsubstantial) {
                                switch (sandMode) {
                                    case DROP:
                                        dropBlock(minecraftWorld, x, y, z);
                                        material = AIR;
                                        break;
                                    case SUPPORT:
                                        // All unsupported sand should be supported by sandstone
                                        minecraftWorld.setMaterialAt(x, y, z, (minecraftWorld.getDataAt(x, y, z) == 1) ? RED_SANDSTONE : SANDSTONE);
                                        material = minecraftWorld.getMaterialAt(x, y, z);
                                        break;
                                    default:
                                        // Do nothing
                                        break;
                                }
                            }
                            break;
                        case BLK_GRAVEL:
                            if (BLOCKS[materialBelow.blockType].veryInsubstantial) {
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
                        case BLK_CEMENT:
                            if (BLOCKS[materialBelow.blockType].veryInsubstantial) {
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
                        case BLK_WATER:
                        case BLK_STATIONARY_WATER:
                            if (BLOCKS[materialBelow.blockType].veryInsubstantial) {
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
                        case BLK_LAVA:
                        case BLK_STATIONARY_LAVA:
                            if (BLOCKS[materialBelow.blockType].veryInsubstantial) {
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
                        case BLK_DEAD_SHRUBS:
                            if ((materialBelow != SAND) && (materialBelow != RED_SAND) && (materialBelow != DIRT) && (materialBelow != PODZOL) && (materialBelow != PERMADIRT) && (materialBelow.blockType != BLK_STAINED_CLAY) && (materialBelow.blockType != BLK_HARDENED_CLAY)) {
                                // Dead shrubs can only exist on Sand
                                minecraftWorld.setMaterialAt(x, y, z, AIR);
                                material = AIR;
                            }
                            break;
                        case BLK_TALL_GRASS:
                        case BLK_ROSE:
                        case BLK_DANDELION:
                            if ((materialBelow != GRASS) && (materialBelow != DIRT) && (materialBelow != PODZOL) && (materialBelow != PERMADIRT)) {
                                // Tall grass and flowers can only exist on Grass or Dirt blocks
                                minecraftWorld.setMaterialAt(x, y, z, AIR);
                                material = AIR;
                            }
                            break;
                        case BLK_RED_MUSHROOM:
                        case BLK_BROWN_MUSHROOM:
                            if ((materialBelow != GRASS) && (materialBelow != DIRT) && (materialBelow != PODZOL) && (materialBelow != PERMADIRT) && (materialBelow != MYCELIUM) && (materialBelow.blockType != BLK_STONE)) {
                                // Mushrooms can only exist on Grass, Dirt, Mycelium or Stone (in caves) blocks
                                minecraftWorld.setMaterialAt(x, y, z, AIR);
                                material = AIR;
                            }
                            break;
                        case BLK_SNOW:
                            if ((materialBelow == ICE) || (materialBelow == SNOW) || (materialBelow == AIR) || (materialBelow == PACKED_ICE)) {
                                // Snow can't be on ice, or another snow block, or air
                                // (well it could be, but it makes no sense, would
                                // disappear when touched, and it makes this algorithm
                                // remove stacks of snow blocks correctly)
                                minecraftWorld.setMaterialAt(x, y, z, AIR);
                                material = AIR;
                            }
                            break;
                        case BLK_WHEAT:
                            if (materialBelow.blockType != BLK_TILLED_DIRT) {
                                // Wheat can only exist on Tilled Dirt blocks
                                minecraftWorld.setMaterialAt(x, y, z, AIR);
                                material = AIR;
                            }
                            break;
                        case BLK_LARGE_FLOWERS:
                            if ((material.data & 0x8) == 0x8) {
                                // Bit 4 set; top half of double high plant.
                                // This is no longer how things are done in
                                // Minecraft 1.13, so as a special favour,
                                // change it to the right material (if there is
                                // in fact a lower half below).
                                if (materialBelow.blockType != BLK_LARGE_FLOWERS) {
                                    // There's a non-double high plant block below;
                                    // replace this block with air
                                    minecraftWorld.setMaterialAt(x, y, z, AIR);
                                    material = AIR;
                                } else {
                                    // There is a lower half below; put the
                                    // corresponding upper half here
                                    Material upperHalf = materialBelow.withProperty("half", "upper");
                                    minecraftWorld.setMaterialAt(x, y, z, upperHalf);
                                    material = upperHalf;
                                }
                            } else {
                                // Otherwise: lower half of double high plant; check
                                // there's a top half above and grass or dirt below
                                if ((materialAbove.blockType == BLK_LARGE_FLOWERS) || (materialAbove.getName().equals(material.getName()) && materialAbove.getProperty("half").equals("upper"))) {
                                    if ((materialAbove.data & 0x8) == 0) {
                                        // There's another lower half above. Replace
                                        // this block with air
                                        minecraftWorld.setMaterialAt(x, y, z, AIR);
                                        material = AIR;
                                    } else if ((materialBelow != GRASS) && (materialBelow != DIRT) && (materialBelow != PODZOL) && (materialBelow != PERMADIRT)) {
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
                        case BLK_CACTUS:
                            if ((materialBelow != SAND) && (materialBelow != RED_SAND) && (materialBelow != CACTUS)) {
                                // Cactus blocks can only be on top of sand or other cactus blocks
                                minecraftWorld.setMaterialAt(x, y, z, AIR);
                                material = AIR;
                            }
                            break;
                        case BLK_SUGAR_CANE:
                            if ((materialBelow != GRASS) && (materialBelow != DIRT) && (materialBelow != PODZOL) && (materialBelow != PERMADIRT) && (materialBelow != SAND) && (materialBelow != RED_SAND) && (materialBelow != SUGAR_CANE)) {
                                // Sugar cane blocks can only be on top of grass, dirt, sand or other sugar cane blocks
                                minecraftWorld.setMaterialAt(x, y, z, AIR);
                                material = AIR;
                            }
                            break;
                        case BLK_NETHER_WART:
                            if (materialBelow != SOUL_SAND) {
                                // Nether wart blocks can only be on top of soul sand
                                minecraftWorld.setMaterialAt(x, y, z, AIR);
                                material = AIR;
                            }
                            break;
                        case BLK_CHORUS_FLOWER:
                        case BLK_CHORUS_PLANT:
                            if ((materialBelow != END_STONE) && (materialBelow != CHORUS_PLANT)) {
                                // Chorus flower and plant blocks can only be on top of end stone or other chorus plant blocks
                                minecraftWorld.setMaterialAt(x, y, z, AIR);
                                material = AIR;
                            }
                            break;
                        case BLK_FIRE:
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