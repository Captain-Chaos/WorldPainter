package org.pepsoft.worldpainter.platforms;

import org.pepsoft.minecraft.Material;
import org.pepsoft.util.Box;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.worldpainter.exporting.ExportSettings;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;
import org.pepsoft.worldpainter.exporting.PostProcessor;
import org.pepsoft.worldpainter.objects.MinecraftWorldObject;
import org.pepsoft.worldpainter.platforms.JavaExportSettings.FloatMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.pepsoft.minecraft.Block.*;
import static org.pepsoft.minecraft.Constants.*;

/**
 * Helper class which can post process a fully rendered Minecraft 1.2 to 1.12.2
 * map to make sure it doesn't violate any Minecraft rules.
 *
 * Created by Pepijn Schmitz on 15-06-15.
 */
public class Java1_2PostProcessor extends PostProcessor {
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
        final int worldMaxZ = minecraftWorld.getMaxHeight() - 1;
        final int x1, y1, x2, y2, minZ, maxZ;
        final JavaExportSettings settings = (exportSettings instanceof JavaExportSettings) ? (JavaExportSettings) exportSettings : new JavaExportSettings();
        final FloatMode sandMode = "false".equalsIgnoreCase(System.getProperty("org.pepsoft.worldpainter.supportSand")) ? FloatMode.LEAVE_FLOATING : settings.sandMode;
        final FloatMode gravelMode = settings.gravelMode;
        final FloatMode cementMode = settings.cementMode;
        final FloatMode waterMode = settings.waterMode;
        final FloatMode lavaMode = settings.lavaMode;
        final boolean flowLava = settings.flowLava, flowWater = settings.flowWater, makeAllLeavesPersistent = settings.makeAllLeavesPersistent;
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
                x2 = objectVolume.getX2() - 1; // TODO: this is wrong, no?
                y1 = objectVolume.getY1();
                y2 = objectVolume.getY2() - 1; // TODO: this is wrong, no?
                minZ = objectVolume.getZ1();
                maxZ = objectVolume.getZ2() - 1; // TODO: this is wrong, no?
            }
        } else {
            x1 = volume.getX1();
            y1 = volume.getY1();
            x2 = volume.getX2() - 1; // TODO: this is wrong, no?
            y2 = volume.getY2() - 1; // TODO: this is wrong, no?
            minZ = volume.getZ1();
            maxZ = volume.getZ2() - 1; // TODO: this is wrong, no?
        }
        final boolean traceEnabled = logger.isTraceEnabled();
        for (int x = x1; x <= x2; x ++) {
            for (int y = y1; y <= y2; y++) {
                int blockTypeBelow = (minZ <= 0) ? BLK_AIR : minecraftWorld.getBlockTypeAt(x, y, minZ - 1);
                int blockTypeAbove = minecraftWorld.getBlockTypeAt(x, y, minZ);
                // TODO: only do this for non-bottomless worlds:
//                if ((minZ == 0) && (blockTypeAbove != BLK_BEDROCK) && (blockTypeAbove != BLK_AIR) && (blockTypeAbove != BLK_STATIONARY_WATER) && (blockTypeAbove != BLK_STATIONARY_LAVA)) {
//                    logger.warn("Non-bedrock block @ " + x + "," + y + ",0: " + BLOCKS[blockTypeAbove].name);
//                }
                final int columnMaxZ = Math.min(minecraftWorld.getHighestNonAirBlock(x, y), maxZ);
                for (int z = minZ; z <= columnMaxZ; z++) {
                    int blockType = blockTypeAbove;
                    blockTypeAbove = (z < worldMaxZ) ? minecraftWorld.getBlockTypeAt(x, y, z + 1) : BLK_AIR;
                    if (((blockTypeBelow == BLK_GRASS) || (blockTypeBelow == BLK_MYCELIUM) || (blockTypeBelow == BLK_TILLED_DIRT)) && ((blockType == BLK_WATER) || (blockType == BLK_STATIONARY_WATER) || (blockType == BLK_ICE) || ((blockType <= HIGHEST_KNOWN_BLOCK_ID) && (BLOCK_TRANSPARENCY[blockType] == 15)))) {
                        // Covered grass, mycelium or tilled earth block, should
                        // be dirt. Note that unknown blocks are treated as
                        // transparent for this check so that grass underneath
                        // custom plants doesn't turn to dirt, for instance
                        minecraftWorld.setMaterialAt(x, y, z - 1, Material.DIRT);
                        blockTypeBelow = BLK_DIRT;
                    }
                    switch (blockType) {
                        case BLK_SAND:
                            if (BLOCKS[blockTypeBelow].veryInsubstantial) {
                                switch (sandMode) {
                                    case DROP:
                                        dropBlock(minecraftWorld, x, y, z);
                                        blockType = BLK_AIR;
                                        break;
                                    case SUPPORT:
                                        // All unsupported sand should be supported by sandstone
                                        minecraftWorld.setMaterialAt(x, y, z, (minecraftWorld.getDataAt(x, y, z) == 1) ? Material.RED_SANDSTONE : Material.SANDSTONE);
                                        blockType = minecraftWorld.getBlockTypeAt(x, y, z);
                                        break;
                                    default:
                                        // Do nothing
                                        break;
                                }
                            }
                            break;
                        case BLK_GRAVEL:
                            if (BLOCKS[blockTypeBelow].veryInsubstantial) {
                                switch (gravelMode) {
                                    case DROP:
                                        dropBlock(minecraftWorld, x, y, z);
                                        blockType = BLK_AIR;
                                        break;
                                    case SUPPORT:
                                        // All unsupported gravel should be supported by stone
                                        minecraftWorld.setMaterialAt(x, y, z, Material.STONE);
                                        blockType = BLK_STONE;
                                        break;
                                    default:
                                        // Do nothing
                                        break;
                                }
                            }
                            break;
                        case BLK_CEMENT:
                            if (BLOCKS[blockTypeBelow].veryInsubstantial) {
                                switch (cementMode) {
                                    case DROP:
                                        dropBlock(minecraftWorld, x, y, z);
                                        blockType = BLK_AIR;
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
                            if ((blockTypeBelow != BLK_WATER) && (blockTypeBelow != BLK_STATIONARY_WATER) && BLOCKS[blockTypeBelow].veryInsubstantial) {
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
                            if ((blockTypeBelow != BLK_LAVA) && (blockTypeBelow != BLK_STATIONARY_LAVA) && BLOCKS[blockTypeBelow].veryInsubstantial) {
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
                            if ((blockTypeBelow != BLK_SAND) && (blockTypeBelow != BLK_DIRT) && (blockTypeBelow != BLK_STAINED_CLAY) && (blockTypeBelow != BLK_HARDENED_CLAY)) {
                                // Dead shrubs can only exist on Sand
                                minecraftWorld.setMaterialAt(x, y, z, Material.AIR);
                                blockType = BLK_AIR;
                            }
                            break;
                        case BLK_TALL_GRASS:
                        case BLK_ROSE:
                        case BLK_DANDELION:
                            if ((blockTypeBelow != BLK_GRASS) && (blockTypeBelow != BLK_DIRT)) {
                                // Tall grass and flowers can only exist on Grass or Dirt blocks
                                minecraftWorld.setMaterialAt(x, y, z, Material.AIR);
                                blockType = BLK_AIR;
                            }
                            break;
                        case BLK_RED_MUSHROOM:
                        case BLK_BROWN_MUSHROOM:
                            if ((blockTypeBelow != BLK_GRASS) && (blockTypeBelow != BLK_DIRT) && (blockTypeBelow != BLK_MYCELIUM) && (blockTypeBelow != BLK_STONE)) {
                                // Mushrooms can only exist on Grass, Dirt, Mycelium or Stone (in caves) blocks
                                minecraftWorld.setMaterialAt(x, y, z, Material.AIR);
                                blockType = BLK_AIR;
                            }
                            break;
                        case BLK_SNOW:
                            if ((blockTypeBelow == BLK_ICE) || (blockTypeBelow == BLK_SNOW) || (blockTypeBelow == BLK_AIR) || (blockTypeBelow == BLK_PACKED_ICE)) {
                                // Snow can't be on ice, or another snow block, or air
                                // (well it could be, but it makes no sense, would
                                // disappear when touched, and it makes this algorithm
                                // remove stacks of snow blocks correctly)
                                minecraftWorld.setMaterialAt(x, y, z, Material.AIR);
                                blockType = BLK_AIR;
                            }
                            break;
                        case BLK_WHEAT:
                            if (blockTypeBelow != BLK_TILLED_DIRT) {
                                // Wheat can only exist on Tilled Dirt blocks
                                minecraftWorld.setMaterialAt(x, y, z, Material.AIR);
                                blockType = BLK_AIR;
                            }
                            break;
                        case BLK_LARGE_FLOWERS:
                            int data = minecraftWorld.getDataAt(x, y, z);
                            if ((data & 0x8) == 0x8) {
                                // Bit 4 set; top half of double high plant; check
                                // there's a lower half beneath
                                // If the block below is another double high plant
                                // block we don't need to check whether it is of the
                                // correct type because the combo was already
                                // checked when the lower half was encountered
                                // in the previous iteration
                                if (blockTypeBelow != BLK_LARGE_FLOWERS) {
                                    // There's a non-double high plant block below;
                                    // replace this block with air
                                    if (traceEnabled) {
                                        logger.trace("Block @ " + x + "," + z + "," + y + " is upper large flower block; block below is " + BLOCK_TYPE_NAMES[blockTypeBelow] + "; removing block");
                                    }
                                    minecraftWorld.setMaterialAt(x, y, z, Material.AIR);
                                    blockType = BLK_AIR;
                                }
                            } else {
                                // Otherwise: lower half of double high plant; check
                                // there's a top half above and grass or dirt below
                                if (blockTypeAbove == BLK_LARGE_FLOWERS) {
                                    if ((minecraftWorld.getDataAt(x, y, z + 1) & 0x8) == 0) {
                                        // There's another lower half above. Replace
                                        // this block with air
                                        if (traceEnabled) {
                                            logger.trace("Block @ " + x + "," + z + "," + y + " is lower large flower block; block above is another lower large flower block; removing block");
                                        }
                                        minecraftWorld.setMaterialAt(x, y, z, Material.AIR);
                                        blockType = BLK_AIR;
                                    } else if ((blockTypeBelow != BLK_GRASS) && (blockTypeBelow != BLK_DIRT)) {
                                        // Double high plants can (presumably; TODO:
                                        // check) only exist on grass or dirt
                                        if (traceEnabled) {
                                            logger.trace("Block @ " + x + "," + z + "," + y + " is lower large flower block; block above is " + BLOCK_TYPE_NAMES[blockTypeBelow] + "; removing block");
                                        }
                                        minecraftWorld.setMaterialAt(x, y, z, Material.AIR);
                                        blockType = BLK_AIR;
                                    }
                                } else {
                                    // There's a non-double high plant block above;
                                    // replace this block with air
                                    if (traceEnabled) {
                                        logger.trace("Block @ " + x + "," + z + "," + y + " is lower large flower block; block above is " + BLOCK_TYPE_NAMES[blockTypeBelow] + "; removing block");
                                    }
                                    minecraftWorld.setMaterialAt(x, y, z, Material.AIR);
                                    blockType = BLK_AIR;
                                }
                            }
                            break;
                        case BLK_CACTUS:
                            if ((blockTypeBelow != BLK_SAND) && (blockTypeBelow != BLK_CACTUS)) {
                                // Cactus blocks can only be on top of sand or other cactus blocks
                                minecraftWorld.setMaterialAt(x, y, z, Material.AIR);
                                blockType = BLK_AIR;
                            }
                            break;
                        case BLK_SUGAR_CANE:
                            if ((blockTypeBelow != BLK_GRASS) && (blockTypeBelow != BLK_DIRT) && (blockTypeBelow != BLK_SAND) && (blockTypeBelow != BLK_SUGAR_CANE)) {
                                // Sugar cane blocks can only be on top of grass, dirt, sand or other sugar cane blocks
                                minecraftWorld.setMaterialAt(x, y, z, Material.AIR);
                                blockType = BLK_AIR;
                            }
                            break;
                        case BLK_NETHER_WART:
                            if (blockTypeBelow != BLK_SOUL_SAND) {
                                // Nether wart blocks can only be on top of soul sand
                                minecraftWorld.setMaterialAt(x, y, z, Material.AIR);
                                blockType = BLK_AIR;
                            }
                            break;
                        case BLK_CHORUS_FLOWER:
                        case BLK_CHORUS_PLANT:
                            if ((blockTypeBelow != BLK_END_STONE) && (blockTypeBelow != BLK_CHORUS_PLANT)) {
                                // Chorus flower and plant blocks can only be on top of end stone or other chorus plant blocks
                                minecraftWorld.setMaterialAt(x, y, z, Material.AIR);
                                blockType = BLK_AIR;
                            }
                            break;
                        case BLK_FIRE:
                            // We don't know which blocks are flammable, but at
                            // least check whether the fire is not floating in
                            // the air
                            if ((blockTypeBelow == BLK_AIR)
                                    && (blockTypeAbove == BLK_AIR)
                                    && (minecraftWorld.getBlockTypeAt(x - 1, y, z) == BLK_AIR)
                                    && (minecraftWorld.getBlockTypeAt(x + 1, y, z) == BLK_AIR)
                                    && (minecraftWorld.getBlockTypeAt(x, y - 1, z) == BLK_AIR)
                                    && (minecraftWorld.getBlockTypeAt(x, y + 1, z) == BLK_AIR)) {
                                minecraftWorld.setMaterialAt(x, y, z, Material.AIR);
                                blockType = BLK_AIR;
                            }
                            break;
                        case BLK_LEAVES:
                        case BLK_LEAVES2:
                            data = minecraftWorld.getDataAt(x, y, z);
                            if (makeAllLeavesPersistent && ((data & 0x4) == 0)) {
                                minecraftWorld.setDataAt(x, y, z, data | 0x4);
                            }
                            break;

                    }
                    blockTypeBelow = blockType;
                }
            }
            if (progressReceiver != null) {
                progressReceiver.setProgress((float) (x - x1 + 1) / (x2 - x1 + 1));
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
                    final int columnMaxZ = Math.min(minecraftWorld.getHighestNonAirBlock(x, y), maxZ);
                    // Water at the lowest level can always flow down into the void, so skip that level:
                    for (int z = minZ + 1; z <= columnMaxZ; z++) {
                        final int blockType = minecraftWorld.getBlockTypeAt(x, y, z);
                        final Material materialBelow = (z > minZ) ? minecraftWorld.getMaterialAt(x, y, z - 1) : Material.AIR;
                        if (flowWater && (blockType == BLK_STATIONARY_WATER) && (! isWaterContained(minecraftWorld, x, y, z, materialBelow))) {
                            minecraftWorld.setMaterialAt(x, y, z, Material.get(BLK_WATER, minecraftWorld.getDataAt(x, y, z)));
                        } else if (flowLava && (blockType == BLK_STATIONARY_LAVA) && (! isLavaContained(minecraftWorld, x, y, z, materialBelow))) {
                            minecraftWorld.setMaterialAt(x, y, z, Material.get(BLK_LAVA, minecraftWorld.getDataAt(x, y, z)));
                        }
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

    private static final Logger logger = LoggerFactory.getLogger(Java1_2PostProcessor.class);
}