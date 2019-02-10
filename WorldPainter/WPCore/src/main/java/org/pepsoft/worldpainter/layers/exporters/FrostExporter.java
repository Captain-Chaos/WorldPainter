/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.layers.exporters;

import org.pepsoft.util.CollectionUtils;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.exporting.AbstractLayerExporter;
import org.pepsoft.worldpainter.exporting.Fixup;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;
import org.pepsoft.worldpainter.exporting.SecondPassLayerExporter;
import org.pepsoft.worldpainter.layers.Frost;

import java.awt.*;
import java.util.BitSet;
import java.util.List;
import java.util.Random;

import static org.pepsoft.minecraft.Constants.*;

/**
 *
 * @author pepijn
 */
public class FrostExporter extends AbstractLayerExporter<Frost> implements SecondPassLayerExporter {
    public FrostExporter() {
        super(Frost.INSTANCE, new FrostSettings());
    }
    
    @Override
    public List<Fixup> render(final Dimension dimension, final Rectangle area, final Rectangle exportedArea, final MinecraftWorld minecraftWorld) {
        final FrostSettings settings = (FrostSettings) getSettings();
        final boolean frostEverywhere = settings.isFrostEverywhere();
        final int mode = settings.getMode();
        final boolean snowUnderTrees = settings.isSnowUnderTrees();
        final int maxHeight = dimension.getMaxHeight();
        final Random random = new Random(); // Only used for random snow height, so it's not a big deal if it's different every time
        final BitSet noSnowOn = (BitSet) NO_SNOW_ON.clone();
        String customNoSnowOnIds = System.getProperty("org.pepsoft.worldpainter.noSnowOn");
        if ((customNoSnowOnIds != null) && (! customNoSnowOnIds.trim().isEmpty())) {
            if (logger.isDebugEnabled()) {
                logger.debug("Not placing snow on the following additional block IDs: \"" + customNoSnowOnIds + "\"");
            }
            for (String id: customNoSnowOnIds.split("[,;]")) {
                noSnowOn.set(Integer.parseInt(id.trim()));
            }
        }
        for (int x = area.x; x < area.x + area.width; x++) {
            for (int y = area.y; y < area.y + area.height; y++) {
                if (frostEverywhere || dimension.getBitLayerValueAt(Frost.INSTANCE, x, y)) {
                    int highestNonAirBlock = minecraftWorld.getHighestNonAirBlock(x, y);
                    int previousBlockType = minecraftWorld.getBlockTypeAt(x, y, Math.min(highestNonAirBlock + 1, maxHeight - 2));
                    int leafBlocksEncountered = 0;
                    for (int height = Math.min(highestNonAirBlock, maxHeight - 2); height >= 0; height--) {
                        int blockType = minecraftWorld.getBlockTypeAt(x, y, height);
                        if (noSnowOn.get(blockType)) {
                            previousBlockType = blockType;
                            continue;
                        } else {
                            if (blockType == BLK_STATIONARY_WATER) {
                                minecraftWorld.setBlockTypeAt(x, y, height, BLK_ICE);
                                break;
                            } else if ((blockType == BLK_LEAVES)
                                    || (blockType == BLK_LEAVES2)
                                    || (blockType == BLK_WOOD)
                                    || (blockType == BLK_WOOD2)) {
                                if (previousBlockType == BLK_AIR) {
                                    minecraftWorld.setBlockTypeAt(x, y, height + 1, BLK_SNOW);
                                }
                                leafBlocksEncountered++;
                                if ((! snowUnderTrees) && (leafBlocksEncountered > 1)) {
                                    break;
                                }
                            } else {
                                // Obliterate tall grass, 'cause there is too
                                // much of it, and leaving it in would look
                                // strange. Also replace existing snow, as we
                                // might want to place thicker snow
                                if ((previousBlockType == BLK_AIR) || (previousBlockType == BLK_TALL_GRASS) || (previousBlockType == BLK_SNOW)) {
                                    if ((mode == FrostSettings.MODE_SMOOTH_AT_ALL_ELEVATIONS)
                                            || (height == dimension.getIntHeightAt(x, y))) {
                                        // Only vary the snow tickness if we're
                                        // at surface height, otherwise it looks
                                        // odd
                                        switch (mode) {
                                            case FrostSettings.MODE_FLAT:
                                                placeSnow(minecraftWorld, x, y, height, 1);
                                                break;
                                            case FrostSettings.MODE_RANDOM:
                                                placeSnow(minecraftWorld, x, y, height, random.nextInt(3) + 1);
                                                break;
                                            case FrostSettings.MODE_SMOOTH:
                                            case FrostSettings.MODE_SMOOTH_AT_ALL_ELEVATIONS:
                                                int layers = (int) ((dimension.getHeightAt(x, y) + 0.5f - dimension.getIntHeightAt(x, y)) / 0.125f) + 1;
                                                if ((layers > 1) && (! frostEverywhere)) {
                                                    layers = Math.max(Math.min(layers, dimension.getBitLayerCount(Frost.INSTANCE, x, y, 1) - 1), 1);
                                                }
                                                placeSnow(minecraftWorld, x, y, height, layers);
                                                break;
                                        }
                                    } else {
                                        // At other elevations just place a
                                        // regular thin snow block
                                        placeSnow(minecraftWorld, x, y, height, 1);
                                    }
                                }
                                break;
                            }
                        }
                        previousBlockType = blockType;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Place a snow block with a specific thickness, but only if thicker snow is
     * not already present.
     */
    private void placeSnow(MinecraftWorld minecraftWorld, int x, int y, int height, int layers) {
        if ((layers < 1) || (layers > 8)) {
            throw new IllegalArgumentException("layers " + layers);
        }
        if ((minecraftWorld.getBlockTypeAt(x, y, height + 1) != BLK_SNOW) || (minecraftWorld.getDataAt(x, y, height + 1) + 1 < layers)) {
            minecraftWorld.setBlockTypeAt(x, y, height + 1, BLK_SNOW);
            minecraftWorld.setDataAt(x, y, height + 1, layers - 1);
        }
    }

    private static final BitSet NO_SNOW_ON = CollectionUtils.bitSetOf(
        BLK_AIR, BLK_ICE, BLK_LAVA, BLK_STATIONARY_LAVA, BLK_TORCH,
        BLK_DANDELION, BLK_ROSE, BLK_BROWN_MUSHROOM, BLK_RED_MUSHROOM,
        BLK_FIRE, BLK_TALL_GRASS, BLK_DEAD_SHRUBS, BLK_WOODEN_STAIRS,
        BLK_COBBLESTONE_STAIRS, BLK_BRICK_STAIRS, BLK_NETHER_BRICK_STAIRS,
        BLK_STONE_BRICK_STAIRS, BLK_SLAB, BLK_FENCE, BLK_FENCE_GATE,
        BLK_NETHER_BRICK_FENCE, BLK_WALL_SIGN, BLK_SIGN, BLK_VINES, BLK_SAPLING,
        BLK_WATER, BLK_BED, BLK_POWERED_RAILS, BLK_RAILS, BLK_DETECTOR_RAILS,
        BLK_COBWEB, BLK_PISTON_HEAD, BLK_CHEST, BLK_REDSTONE_WIRE,
        BLK_WHEAT, BLK_BURNING_FURNACE, BLK_WOODEN_DOOR, BLK_IRON_DOOR,
        BLK_IRON_BARS, BLK_LADDER, BLK_LEVER, BLK_STONE_PRESSURE_PLATE,
        BLK_WOODEN_PRESSURE_PLATE, BLK_REDSTONE_TORCH_OFF,
        BLK_REDSTONE_TORCH_ON, BLK_STONE_BUTTON, BLK_SNOW, BLK_CACTUS,
        BLK_SUGAR_CANE, BLK_CAKE, BLK_REDSTONE_REPEATER_OFF,
        BLK_REDSTONE_REPEATER_ON, BLK_TRAPDOOR, BLK_GLASS_PANE,
        BLK_PUMPKIN_STEM, BLK_MELON_STEM, BLK_LILY_PAD, BLK_NETHER_WART,
        BLK_ENCHANTMENT_TABLE, BLK_BREWING_STAND, BLK_END_PORTAL,
        BLK_END_PORTAL_FRAME, BLK_DRAGON_EGG, BLK_WOODEN_SLAB, BLK_COCOA_PLANT,
        BLK_SANDSTONE_STAIRS, BLK_ENDER_CHEST, BLK_TRIPWIRE_HOOK, BLK_TRIPWIRE,
        BLK_PINE_WOOD_STAIRS, BLK_BIRCH_WOOD_STAIRS, BLK_JUNGLE_WOOD_STAIRS,
        BLK_COBBLESTONE_WALL, BLK_FLOWER_POT, BLK_CARROTS, BLK_POTATOES,
        BLK_WOODEN_BUTTON, BLK_HEAD, BLK_ANVIL, BLK_TRAPPED_CHEST,
        BLK_WEIGHTED_PRESSURE_PLATE_HEAVY, BLK_WEIGHTED_PRESSURE_PLATE_LIGHT,
        BLK_REDSTONE_COMPARATOR_UNPOWERED, BLK_DAYLIGHT_SENSOR,
        BLK_ACTIVATOR_RAIL, BLK_STAINED_GLASS_PANE, BLK_ACACIA_WOOD_STAIRS,
        BLK_DARK_OAK_WOOD_STAIRS, BLK_CARPET, BLK_LARGE_FLOWERS, BLK_PACKED_ICE);
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(FrostExporter.class);
    
    public static class FrostSettings implements ExporterSettings {
        @Override
        public boolean isApplyEverywhere() {
            return frostEverywhere;
        }

        @Override
        public Frost getLayer() {
            return Frost.INSTANCE;
        }

        public boolean isFrostEverywhere() {
            return frostEverywhere;
        }

        public void setFrostEverywhere(boolean frostEverywhere) {
            this.frostEverywhere = frostEverywhere;
        }

        public int getMode() {
            return mode;
        }

        public void setMode(int mode) {
            this.mode = mode;
        }

        public boolean isSnowUnderTrees() {
            return snowUnderTrees;
        }

        public void setSnowUnderTrees(boolean snowUnderTrees) {
            this.snowUnderTrees = snowUnderTrees;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final FrostSettings other = (FrostSettings) obj;
            if (this.frostEverywhere != other.frostEverywhere) {
                return false;
            }
            if (this.mode != other.mode) {
                return false;
            }
            if (this.snowUnderTrees != other.snowUnderTrees) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 23 * hash + (this.frostEverywhere ? 1 : 0);
            hash = 23 * hash + mode;
            hash = 23 * hash + (this.snowUnderTrees ? 1 : 0);
            return hash;
        }

        @Override
        public FrostSettings clone() {
            try {
                return (FrostSettings) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }
        
        private boolean frostEverywhere;
        private int mode = MODE_SMOOTH;
        private boolean snowUnderTrees = true;
        
        public static final int MODE_FLAT                     = 0; // Always place thin snow blocks
        public static final int MODE_RANDOM                   = 1; // Place random height snow blocks on the surface
        public static final int MODE_SMOOTH                   = 2; // Place smooth snow blocks on the surface
        public static final int MODE_SMOOTH_AT_ALL_ELEVATIONS = 3; // Place smooth snow blocks at any elevation
        
        private static final long serialVersionUID = 2011060801L;
    }
}
