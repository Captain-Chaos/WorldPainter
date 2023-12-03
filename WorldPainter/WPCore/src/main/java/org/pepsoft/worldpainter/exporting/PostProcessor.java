package org.pepsoft.worldpainter.exporting;

import org.pepsoft.minecraft.Material;
import org.pepsoft.util.Box;
import org.pepsoft.util.ProgressReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;

import static org.pepsoft.minecraft.Constants.*;
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
     * @param minecraftWorld The {@code MinecraftWorld} to post process.
     * @param area The area of the world to post process from top to bottom.
     * @param exportSettings The export settings to apply, if any. May be {@code null}, in which case the post processor
     *                       should use default settings. If the settings are of an unsupported type and/or belong to a
     *                       different platform, they must be silently ignored rather than cause an exception.
     * @param progressReceiver The optional progress receiver to which to report
     *                         progress. May be {@code null}.
     * @throws ProgressReceiver.OperationCancelled If the progress receiver
     * threw an {@code OperationCancelled} exception.
     */
    public void postProcess(MinecraftWorld minecraftWorld, Rectangle area, ExportSettings exportSettings, ProgressReceiver progressReceiver) throws ProgressReceiver.OperationCancelled {
        postProcess(minecraftWorld, new Box(area.x, area.x + area.width, area.y, area.y + area.height, minecraftWorld.getMinHeight(), minecraftWorld.getMaxHeight()), exportSettings, progressReceiver);
    }

    /**
     * Post process (part of) a {@link MinecraftWorld} to make sure it conforms
     * to platform rules.
     *
     * @param minecraftWorld The {@code MinecraftWorld} to post process.
     * @param volume The three dimensional area of the world to post process.
     * @param exportSettings The export settings to apply, if any. May be {@code null}, in which case the post processor
     *                       should use default settings. If the settings are of an unsupported type and/or belong to a
     *      *                different platform, they must be silently ignored rather than cause an exception.
     * @param progressReceiver The optional progress receiver to which to report
     *                         progress. May be {@code null}.
     * @throws ProgressReceiver.OperationCancelled If the progress receiver
     * threw an {@code OperationCancelled} exception.
     */
    public abstract void postProcess(MinecraftWorld minecraftWorld, Box volume, ExportSettings exportSettings, ProgressReceiver progressReceiver) throws ProgressReceiver.OperationCancelled;

    protected final void dropBlock(MinecraftWorld world, int x, int y, int z) {
        final int minZ = world.getMinHeight();
        int solidFloor = z - 1;
        for (; solidFloor > minZ; solidFloor--) {
            Material material = world.getMaterialAt(x, y, solidFloor);
            if (material.insubstantial) {
                // Remove insubstantial blocks (as the falling block would have
                // obliterated them) but keep looking for a solid floor
                world.setMaterialAt(x, y, solidFloor, AIR);
            } else if (material.isNotNamedOneOf(MC_AIR, MC_WATER, MC_LAVA)) {
                break;
            }
        }
        if (solidFloor < minZ) {
            // The block would have fallen into the void, so just remove it
            world.setMaterialAt(x, y, z, AIR);
        } else if (solidFloor < z - 1) {
            Material block = world.getMaterialAt(x, y, z);
            world.setMaterialAt(x, y, z, AIR);
            world.setMaterialAt(x, y, solidFloor + 1, block);
        }
    }

    protected final void dropFluid(MinecraftWorld world, int x, int y, int z) {
        final int minZ = world.getMinHeight();
        final boolean lava = world.getMaterialAt(x, y, z).isNamed(MC_LAVA);
        int solidFloor = z - 1;
        for (; solidFloor > minZ; solidFloor--) {
            Material material = world.getMaterialAt(x, y, solidFloor);
            if (material.empty || (material.insubstantial && material.isNotNamed(lava ? MC_LAVA : MC_WATER))) {
                world.setMaterialAt(x, y, solidFloor, lava ? FALLING_LAVA : FALLING_WATER);
            } else {
                break;
            }
        }
        if ((solidFloor >= minZ) && (solidFloor < (z - 1))) {
            if (world.getMaterialAt(x, y, solidFloor).isNamedOneOf(MC_GRASS_BLOCK, MC_MYCELIUM, MC_FARMLAND)) {
                world.setMaterialAt(x, y, solidFloor, DIRT);
            }
        }
    }

    protected final boolean isWaterContained(MinecraftWorld world, int x, int y, int z, Material materialBelow) {
        if (containsAnyWater(materialBelow)) {
            // There is already water below
            return true;
        } else if ((! materialBelow.containsWater()) && (! materialBelow.solid)) {
            // The water can flow down
            return false;
        } else {
            // Check whether the water can flow sideways
            final Material materialNorth = world.getMaterialAt(x, y - 1, z), materialEast = world.getMaterialAt(x + 1, y, z),
                    materialSouth = world.getMaterialAt(x, y + 1, z), materialWest = world.getMaterialAt(x - 1, y, z);
            return (containsAnyWater(materialNorth) || materialNorth.solid)
                    && (containsAnyWater(materialEast) || materialEast.solid)
                    && (containsAnyWater(materialSouth) || materialSouth.solid)
                    && (containsAnyWater(materialWest) || materialWest.solid);
        }
    }

    /**
     * Whether the material is any kind of water, including falling or flowing water (with a non zero level).
     */
    protected final boolean containsAnyWater(Material material) {
        return material.containsWater() || material.isNamed(MC_WATER);
    }

    protected final boolean isLavaContained(MinecraftWorld world, int x, int y, int z, Material materialBelow) {
        if (materialBelow.isNamed(MC_LAVA)) {
            // There is already lava below
            return true;
        } else if ((! materialBelow.isNamed(MC_LAVA)) && (! materialBelow.solid)) {
            // The lava can flow down
            return false;
        } else {
            // Check whether the lava can flow sideways
            final Material materialNorth = world.getMaterialAt(x, y - 1, z), materialEast = world.getMaterialAt(x + 1, y, z),
                    materialSouth = world.getMaterialAt(x, y + 1, z), materialWest = world.getMaterialAt(x - 1, y, z);
            return (materialNorth.isNamed(MC_LAVA) || materialNorth.solid)
                    && (materialEast.isNamed(MC_LAVA) || materialEast.solid)
                    && (materialSouth.isNamed(MC_LAVA) || materialSouth.solid)
                    && (materialWest.isNamed(MC_LAVA) || materialWest.solid);
        }
    }

    public static final boolean enabled = ! "false".equalsIgnoreCase(System.getProperty("org.pepsoft.worldpainter.enforceBlockRules"));

    private static final Logger logger = LoggerFactory.getLogger(PostProcessor.class);

    static {
        if (! enabled) {
            logger.warn("Block rule enforcement disabled");
        }
    }
}