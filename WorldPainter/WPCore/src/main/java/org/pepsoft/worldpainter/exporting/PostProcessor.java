package org.pepsoft.worldpainter.exporting;

import org.pepsoft.minecraft.Material;
import org.pepsoft.util.Box;
import org.pepsoft.util.ProgressReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;

import static org.pepsoft.minecraft.Block.BLOCKS;
import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.minecraft.Constants.BLK_STATIONARY_LAVA;
import static org.pepsoft.minecraft.Material.*;

/**
 * Helper class which can post process a fully rendered map to make sure it
 * doesn't violate any rules.
 *
 * <p>Created by Pepijn on 25-2-2017.
 */
public abstract class PostProcessor {
    /**
     * Post process (part of) a {@link MinecraftWorld} to make sure it conforms
     * to platform rules.
     *
     * @param minecraftWorld The <code>MinecraftWorld</code> to post process.
     * @param area The area of the world to post process from top to bottom.
     * @param progressReceiver The optional progress receiver to which to report
     *                         progress. May be <code>null</code>.
     * @throws ProgressReceiver.OperationCancelled If the progress receiver
     * threw an <code>OperationCancelled</code> exception.
     */
    public void postProcess(MinecraftWorld minecraftWorld, Rectangle area, ProgressReceiver progressReceiver) throws ProgressReceiver.OperationCancelled {
        postProcess(minecraftWorld, new Box(area.x, area.x + area.width, area.y, area.y + area.height, 0, minecraftWorld.getMaxHeight()), progressReceiver);
    }

    /**
     * Post process (part of) a {@link MinecraftWorld} to make sure it conforms
     * to platform rules.
     *
     * @param minecraftWorld The <code>MinecraftWorld</code> to post process.
     * @param volume The three dimensional area of the world to post process.
     * @param progressReceiver The optional progress receiver to which to report
     *                         progress. May be <code>null</code>.
     * @throws ProgressReceiver.OperationCancelled If the progress receiver
     * threw an <code>OperationCancelled</code> exception.
     */
    public abstract void postProcess(MinecraftWorld minecraftWorld, Box volume, ProgressReceiver progressReceiver) throws ProgressReceiver.OperationCancelled;

    protected void dropBlock(MinecraftWorld world, int x, int y, int z) {
        int solidFloor = z - 1;
        for (; solidFloor > 0; solidFloor--) {
            int blockType = world.getBlockTypeAt(x, y, solidFloor);
            if (BLOCKS[blockType].insubstantial) {
                // Remove insubstantial blocks (as the falling block would have
                // obliterated them) but keep looking for a solid floor
                world.setMaterialAt(x, y, solidFloor, AIR);
            } else if ((blockType != BLK_AIR) && (blockType != BLK_WATER) && (blockType != BLK_STATIONARY_WATER) && (blockType != BLK_LAVA) && (blockType != BLK_STATIONARY_LAVA)) {
                break;
            }
        }
        if (solidFloor < 0) {
            // The block would have fallen into the void, so just remove it
            world.setMaterialAt(x, y, z, AIR);
        } else if (solidFloor < z - 1) {
            Material block = world.getMaterialAt(x, y, z);
            world.setMaterialAt(x, y, z, AIR);
            world.setMaterialAt(x, y, solidFloor + 1, block);
        }
    }

    protected void dropFluid(MinecraftWorld world, int x, int y, int z) {
        boolean lava = world.getBlockTypeAt(x, y, z) == BLK_LAVA || world.getBlockTypeAt(x, y, z) == BLK_STATIONARY_LAVA;
        int solidFloor = z - 1;
        for (; solidFloor > 0; solidFloor--) {
            int blockType = world.getBlockTypeAt(x, y, solidFloor);
            if (blockType == BLK_AIR || BLOCKS[blockType].insubstantial) {
                world.setMaterialAt(x, y, solidFloor, lava ? STATIONARY_LAVA : STATIONARY_WATER);
            } else {
                break;
            }
        }
        if ((solidFloor >= 0) && (solidFloor < (z - 1))){
            // Make the lowest block flowing, so that Minecraft will continue it
            // sideways
            world.setMaterialAt(x, y, solidFloor + 1,lava ? LAVA : WATER);
        }
    }

    public static final boolean enabled = ! "false".equalsIgnoreCase(System.getProperty("org.pepsoft.worldpainter.enforceBlockRules"));

    private static final Logger logger = LoggerFactory.getLogger(PostProcessor.class);

    static {
        if (! enabled) {
            logger.warn("Block rule enforcement disabled");
        }
    }

    public enum FloatMode {DROP, SUPPORT, LEAVE_FLOATING}
}