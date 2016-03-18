package org.pepsoft.worldpainter.exporting;

import org.pepsoft.minecraft.Material;
import org.pepsoft.util.Box;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.worldpainter.objects.MinecraftWorldObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;

import static org.pepsoft.minecraft.Block.*;
import static org.pepsoft.minecraft.Constants.*;

/**
 * Helper class which can post process a fully rendered Minecraft map to make
 * sure it doesn't violate any Minecraft rules.
 *
 * Created by Pepijn Schmitz on 15-06-15.
 */
public class PostProcessor {
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
     * @param area The area of the world to post process.
     * @param progressReceiver The optional progress receiver to which to report
     *                         progress. May be <code>null</code>.
     * @throws ProgressReceiver.OperationCancelled If the progress receiver
     * threw an <code>OperationCancelled</code> exception.
     */
    public static void postProcess(MinecraftWorld minecraftWorld, Rectangle area, ProgressReceiver progressReceiver) throws ProgressReceiver.OperationCancelled {
        if (! enabled) {
            return;
        }
        int x1 = area.x;
        int y1 = area.y;
        int x2 = x1 + area.width - 1, y2 = y1 + area.height - 1;
        int minZ = 0, maxZ = minecraftWorld.getMaxHeight() - 1;
        if (minecraftWorld instanceof MinecraftWorldObject) {
            // Special support for MinecraftWorldObjects to constrain the area
            // further
            Box volume = ((MinecraftWorldObject) minecraftWorld).getVolume();
            // In a Box the second coordinate is exclusive, so add one (and
            // subtract it later)
            volume.intersect(new Box(x1, x2 + 1, y1, y2 + 1, minZ, maxZ + 1));
            if (volume.isEmpty()) {
                // The specified area does not intersect the volume encompassed
                // by the minecraftWorld. Weird, but it means we have nothing to
                // do
                return;
            } else {
                x1 = volume.getX1();
                x2 = volume.getX2() - 1;
                y1 = volume.getY1();
                y2 = volume.getY2() - 1;
                minZ = volume.getZ1();
                maxZ = volume.getZ2() - 1;
            }
        }
        boolean traceEnabled = logger.isTraceEnabled();
        for (int x = x1; x <= x2; x ++) {
            for (int y = y1; y <= y2; y++) {
                int blockTypeBelow = minecraftWorld.getBlockTypeAt(x, y, minZ);
                int blockTypeAbove = minecraftWorld.getBlockTypeAt(x, y, minZ + 1);
                if (supportSand && (blockTypeBelow == BLK_SAND)) {
                    minecraftWorld.setMaterialAt(x, y, minZ, (minecraftWorld.getDataAt(x, y, minZ) == 1) ? Material.RED_SANDSTONE : Material.SANDSTONE);
                    blockTypeBelow = minecraftWorld.getBlockTypeAt(x, y, minZ);
                }
                for (int z = minZ + 1; z <= maxZ; z++) {
                    int blockType = blockTypeAbove;
                    blockTypeAbove = (z < maxZ) ? minecraftWorld.getBlockTypeAt(x, y, z + 1) : BLK_AIR;
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
                            if (supportSand && BLOCKS[blockTypeBelow].veryInsubstantial) {
                                // All unsupported sand should be supported by sandstone
                                minecraftWorld.setMaterialAt(x, y, z, (minecraftWorld.getDataAt(x, y, z - 1) == 1) ? Material.RED_SANDSTONE : Material.SANDSTONE);
                                blockType = minecraftWorld.getBlockTypeAt(x, y, z);
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
                    }
                    blockTypeBelow = blockType;
                }
            }
            if (progressReceiver != null) {
                progressReceiver.setProgress((x - x1 + 1) / (x2 - x1 + 1));
            }
        }
    }

    private static final boolean enabled = ! "false".equalsIgnoreCase(System.getProperty("org.pepsoft.worldpainter.enforceBlockRules"));
    private static final boolean supportSand = ! "false".equalsIgnoreCase(System.getProperty("org.pepsoft.worldpainter.supportSand"));
    private static final Logger logger = LoggerFactory.getLogger(PostProcessor.class);

    static {
        if (! enabled) {
            logger.warn("Block rule enforcement disabled");
        }
    }
}