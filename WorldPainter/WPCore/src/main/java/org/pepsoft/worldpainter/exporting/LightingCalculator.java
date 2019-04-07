/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.exporting;

import org.pepsoft.minecraft.Chunk;
import org.pepsoft.minecraft.Material;
import org.pepsoft.util.Box;
import org.pepsoft.worldpainter.Platform;

import java.util.Arrays;

import static org.pepsoft.minecraft.Constants.MC_WATER;
import static org.pepsoft.minecraft.Material.AIR;
import static org.pepsoft.worldpainter.DefaultPlugin.JAVA_ANVIL_1_13;

/**
 * A lighting calculator for MinecraftWorlds.
 * 
 * <p>The process consists of two passes. In the first pass, all chunks which
 * have previously been marked as having "primary light dirty" are visited and
 * have their primary light recalculated. "Primary light" means full daylight,
 * and block light at the site of any luminous blocks such as torches.
 *
 * <p>In the second pass, the previously calculated primary light is propagated
 * into the surrounding blocks. The second pass should be repeated until no
 * changes result from it, meaning the area has been fully lighted.
 *
 * <p>This class uses the Minecraft coordinate system.
 *
 * @author pepijn
 */
public class LightingCalculator {
    public LightingCalculator(MinecraftWorld world, Platform platform) {
        this.world = world;
        this.platform = platform;
        maxHeight = world.getMaxHeight();
    }

    /**
     * Get the current light dirty area in Minecraft coordinates.
     *
     * @return The current light dirty area in Minecraft coordinates.
     */
    public Box getDirtyArea() {
        return dirtyArea;
    }

    /**
     * Set the light dirty area in Minecraft coordinates.
     *
     * @param dirtyArea The light dirty area in Minecraft coordinates to set.
     */
    public void setDirtyArea(Box dirtyArea) {
        this.dirtyArea = dirtyArea;
        maxHeightsXOffset = dirtyArea.getX1();
        maxHeightsZOffset = dirtyArea.getZ1();
        maxHeights = new int[dirtyArea.getHeight()][dirtyArea.getWidth()];
        if (maxHeight > 256) {
            // For tall worlds, for which processing the entire dirty volume on
            // each iteration might be extremely inefficient, create a map of
            // the highest block in each column that could possibly be affected
            // (e.g. could have block light > 0 or sky light < 15)
            // TODO optimise by going chunk by chunk
            for (int z = dirtyArea.getZ1(); z < dirtyArea.getZ2(); z++) {
                for (int x = dirtyArea.getX1(); x < dirtyArea.getX2(); x++) {
                    int highestPossibleAffectedBlock = Math.min(world.getHighestNonAirBlock(x, z) + 15, maxHeight - 1);
                    for (int dz = -15; dz <= 15; dz++) {
                        for (int dx = -15; dx <= 15; dx++) {
                            int xInMaxHeightsMap = x + dx - maxHeightsXOffset;
                            int zInMaxHeightsMap = z + dz - maxHeightsZOffset;
                            if ((xInMaxHeightsMap >= 0) && (zInMaxHeightsMap >= 0) && (zInMaxHeightsMap < maxHeights.length) && (xInMaxHeightsMap < maxHeights[0].length)) {
                                maxHeights[zInMaxHeightsMap][xInMaxHeightsMap] = Math.max(maxHeights[zInMaxHeightsMap][xInMaxHeightsMap], highestPossibleAffectedBlock);
                            }
                        }
                    }
                }
            }
        } else {
            for (int[] row: maxHeights) {
                Arrays.fill(row, maxHeight - 1);
            }
        }
    }

    /**
     * For the selected chunk and the chunks around it, calculate the secondary
     * light, if necessary. The dirty area is constricted to the blocks that
     * were actually changed, and <code>false</code> is returned if no changes
     * were made at all (indicating that lighting is complete).
     */
    public boolean calculateSecondaryLight() {
        int lowestY = Integer.MAX_VALUE, highestY = Integer.MIN_VALUE;
        boolean changed = false;
        for (int x = dirtyArea.getX1(); x < dirtyArea.getX2(); x++) {
            for (int z = dirtyArea.getZ1(); z < dirtyArea.getZ2(); z++) {
                // TODO go chunk by chunk so as not to have to do this check for each column of every chunk:
                Chunk chunk = world.getChunk(x >> 4, z >> 4);
                if (chunk == null) {
                    continue;
                }
                int maxY = Math.min(dirtyArea.getY2() - 1, maxHeights[z - maxHeightsZOffset][x - maxHeightsXOffset]);
                for (int y = dirtyArea.getY1(); y <= maxY; y++) {
                    Material material = world.getMaterialAt(x, z, y);
                    int currentSkylightLevel = world.getSkyLightLevel(x, z, y);
                    int currentBlockLightLevel = world.getBlockLightLevel(x, z, y);
                    int newSkyLightLevel;
                    int newBlockLightLevel;
                    if (material.opaque) {
                        // Opaque block
                        newSkyLightLevel = 0;
                        newBlockLightLevel = (material.blockLight > 0) ? currentBlockLightLevel : 0;
                    } else {
                        // Transparent block, or unknown block. We err on the
                        // side of transparency for unknown blocks to try and
                        // cause less visible lighting bugs
                        newSkyLightLevel = (currentSkylightLevel < 15) ? calculateSkyLightLevel(x, y, z, material) : 15;
                        newBlockLightLevel = (material.blockLight > 0) ? currentBlockLightLevel : calculateBlockLightLevel(x, y, z);
                    }
                    if ((newSkyLightLevel != currentSkylightLevel) || (newBlockLightLevel != currentBlockLightLevel)) {
                        if (newSkyLightLevel != currentSkylightLevel) {
                            world.setSkyLightLevel(x, z, y, newSkyLightLevel);
                        }
                        if (newBlockLightLevel != currentBlockLightLevel) {
                            world.setBlockLightLevel(x, z, y, newBlockLightLevel);
                        }
                        changed = true;
                        if (y - 1 < lowestY) {
                            lowestY = y - 1;
                        }
                        if (y + 1 > highestY) {
                            highestY = y + 1;
                        }
                    }
                }
            }
        }
        if (changed) {
            dirtyArea.setY1(Math.max(lowestY, 0));
            dirtyArea.setY2(Math.min(highestY + 1, maxHeight));
        }
        return changed;
    }

    public int[] calculatePrimaryLight(Chunk chunk) {
        for (int x = 0; x < 16; x++) {
            Arrays.fill(DAYLIGHT[x], true);
            Arrays.fill(HEIGHT[x], Math.min(chunk.getHighestNonAirBlock(), maxHeight - 1));
        }
        // The point above which there are only transparent, non light source
        // blocks
        int lightingVolumeHighMark = 0;
        // The point below which there are only non-transparent, non light
        // source blocks
        int lightingVolumeLowMark = maxHeight - 1;
        for (int y = Math.min(chunk.getHighestNonAirBlock(), maxHeight - 1); y >= 0; y--) { // TODO: will this leave dark areas above the starting level?
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    Material material = chunk.getMaterial(x, y, z);
                    int blockLightLevel = chunk.getBlockLightLevel(x, y, z);
                    int skyLightLevel = chunk.getSkyLightLevel(x, y, z);
                    int newBlockLightLevel, newSkyLightLevel;
                    if (! material.opaque) {
                        // Transparent or translucent block
                        if (y < lightingVolumeLowMark) {
                            lightingVolumeLowMark = y;
                        }
                        int transparency = getTransparency(material);
                        if ((transparency == 0) && (DAYLIGHT[x][z])) {
                            // Propagate daylight down
                            newSkyLightLevel = 15;
                            HEIGHT[x][z] = y;
                        } else {
                            if ((transparency > 0) && (y > lightingVolumeHighMark)) {
                                lightingVolumeHighMark = y;
                            }
                            newSkyLightLevel = 0; // TODO adjust with transparency of block above and 1-per-block falloff rather than going straight to zero
                            DAYLIGHT[x][z] = false;
                        }
                    } else {
                        if (y > lightingVolumeHighMark) {
                            lightingVolumeHighMark = y;
                        }
                        newSkyLightLevel = 0;
                        DAYLIGHT[x][z] = false;
                    }
                    if (material.blockLight > 0) {
                        if (y > lightingVolumeHighMark) {
                            lightingVolumeHighMark = y;
                        }
                        if (y < lightingVolumeLowMark) {
                            lightingVolumeLowMark = y;
                        }
                        newBlockLightLevel = material.blockLight;
                    } else {
                        newBlockLightLevel = 0;
                    }
                    if (newBlockLightLevel != blockLightLevel) {
                        chunk.setBlockLightLevel(x, y, z, newBlockLightLevel);
                    }
                    if (newSkyLightLevel != skyLightLevel) {
                        chunk.setSkyLightLevel(x, y, z, newSkyLightLevel);
                    }
                }
            }
        }
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (chunk.getHeight(x, z) != HEIGHT[x][z]) {
                    chunk.setHeight(x, z, HEIGHT[x][z]);
                }
            }
        }
//        System.out.println("Lighting volume low mark: " + lightingVolumeLowMark + ", high mark: " + lightingVolumeHighMark);
        return new int[] {lightingVolumeLowMark, lightingVolumeHighMark};
    }

    public void recalculatePrimaryLight() {
        for (int x = dirtyArea.getX1(); x <= dirtyArea.getX2(); x++) {
            for (int z = dirtyArea.getZ1(); z <= dirtyArea.getZ2(); z++) {
                // TODO go chunk by chunk so as not to have to do this check for each column of every chunk:
                Chunk chunk = world.getChunkForEditing(x >> 4, z >> 4);
                if (chunk == null) {
                    continue;
                }
                int maxY = Math.min(chunk.getHighestNonAirBlock(), dirtyArea.getY2());
                int skyLightLevelAbove = (maxY >= (world.getMaxHeight() - 1)) ? 15 : world.getSkyLightLevel(x, z, maxY + 1);
                for (int y = maxY; y >= dirtyArea.getY1(); y--) {
                    Material material = chunk.getMaterial(x & 0xf, y, z & 0xf);
                    int blockLightLevel = chunk.getBlockLightLevel(x & 0xf, y, z & 0xf);
                    int skyLightLevel = chunk.getSkyLightLevel(x & 0xf, y, z & 0xf);
                    int newBlockLightLevel, newSkyLightLevel;
                    if (! material.opaque) {
                        // Transparent block, or unknown block. We err on the
                        // side of transparency for unknown blocks to try and
                        // cause less visible lighting bugs
                        int transparency = getTransparency(material);
                        if ((transparency == 0) && (skyLightLevelAbove == 15)) {
                            // Propagate daylight down
                            newSkyLightLevel = 15;
                        } else {
                            newSkyLightLevel = Math.max(skyLightLevelAbove - Math.max(transparency, 1), 0);
                        }
                    } else {
                        newSkyLightLevel = 0;
                    }
                    skyLightLevelAbove = newSkyLightLevel;
                    if (material.blockLight > 0) {
                        newBlockLightLevel = material.blockLight;
                    } else {
                        newBlockLightLevel = 0;
                    }
                    if (newBlockLightLevel != blockLightLevel) {
                        chunk.setBlockLightLevel(x & 0xf, y, z & 0xf, newBlockLightLevel);
                    }
                    if (newSkyLightLevel != skyLightLevel) {
                        chunk.setSkyLightLevel(x & 0xf, y, z & 0xf, newSkyLightLevel);
                    }
                }
            }
        }
    }

    private int getTransparency(Material material) {
        // TODOMC13: make this generic:
        if ((platform == JAVA_ANVIL_1_13) && material.isNamed(MC_WATER)) {
            return 1;
        } else {
            return material.opacity;
        }
    }

    private int calculateSkyLightLevel(int x, int y, int z, Material material) {
        int skyLightLevel = getSkyLightLevelAt(x, y + 1, z);
        // TODOMC13: make this generic:
        if ((skyLightLevel == 15) && (platform == JAVA_ANVIL_1_13) && (material.isNamed(MC_WATER)) && ((y >= maxHeight - 1) || (world.getMaterialAt(x, z, y + 1) == AIR))) {
            // This seems to be a special case in MC 1.13. TODO: keep an eye on whether this was a bug or intended behaviour!
            return 15;
        }
        int highestSurroundingSkyLight = skyLightLevel;
        if (highestSurroundingSkyLight < 15) {
            skyLightLevel = getSkyLightLevelAt(x - 1, y, z);
            if (skyLightLevel > highestSurroundingSkyLight) {
                highestSurroundingSkyLight = skyLightLevel;
            }
            if (highestSurroundingSkyLight < 15) {
                skyLightLevel = getSkyLightLevelAt(x + 1, y, z);
                if (skyLightLevel > highestSurroundingSkyLight) {
                    highestSurroundingSkyLight = skyLightLevel;
                }
                if (highestSurroundingSkyLight < 15) {
                    skyLightLevel = getSkyLightLevelAt(x, y, z - 1);
                    if (skyLightLevel > highestSurroundingSkyLight) {
                        highestSurroundingSkyLight = skyLightLevel;
                    }
                    if (highestSurroundingSkyLight < 15) {
                        skyLightLevel = getSkyLightLevelAt(x, y, z + 1);
                        if (skyLightLevel > highestSurroundingSkyLight) {
                            highestSurroundingSkyLight = skyLightLevel;
                        }
                        if (highestSurroundingSkyLight < 15) {
                            skyLightLevel = getSkyLightLevelAt(x, y - 1, z);
                            if (skyLightLevel > highestSurroundingSkyLight) {
                                highestSurroundingSkyLight = skyLightLevel;
                            }
                        }
                    }
                }
            }
        }
        return Math.max(highestSurroundingSkyLight - Math.max(getTransparency(material), 1), 0);
    }

    private int calculateBlockLightLevel(int x, int y, int z) {
        Material material = world.getMaterialAt(x, z, y);
        int blockLightLevel = getBlockLightLevelAt(x, y + 1, z);
        int highestSurroundingBlockLight = blockLightLevel;
        blockLightLevel = getBlockLightLevelAt(x - 1, y, z);
        if (blockLightLevel > highestSurroundingBlockLight) {
            highestSurroundingBlockLight = blockLightLevel;
        }
        blockLightLevel = getBlockLightLevelAt(x + 1, y, z);
        if (blockLightLevel > highestSurroundingBlockLight) {
            highestSurroundingBlockLight = blockLightLevel;
        }
        blockLightLevel = getBlockLightLevelAt(x, y, z - 1);
        if (blockLightLevel > highestSurroundingBlockLight) {
            highestSurroundingBlockLight = blockLightLevel;
        }
        blockLightLevel = getBlockLightLevelAt(x, y, z + 1);
        if (blockLightLevel > highestSurroundingBlockLight) {
            highestSurroundingBlockLight = blockLightLevel;
        }
        blockLightLevel = getBlockLightLevelAt(x, y - 1, z);
        if (blockLightLevel > highestSurroundingBlockLight) {
            highestSurroundingBlockLight = blockLightLevel;
        }
        return Math.max(highestSurroundingBlockLight - Math.max(getTransparency(material), 1), 0);
    }

    private int getSkyLightLevelAt(int x, int y, int z) {
        if (y < 0) {
            return 0;
        } else if (y >= maxHeight) {
            return 15;
        } else {
            return world.getSkyLightLevel(x, z, y);
        }
    }

    private int getBlockLightLevelAt(int x, int y, int z) {
        if ((y < 0) || (y >= maxHeight)) {
            return 0;
        } else {
            return world.getBlockLightLevel(x, z, y);
        }
    }

    private final MinecraftWorld world;
    private final Platform platform;
    private final int maxHeight;
    private Box dirtyArea;
    private int[][] maxHeights;
    private int maxHeightsXOffset, maxHeightsZOffset;

    private final boolean[][] DAYLIGHT = new boolean[16][16];
    private final int[][] HEIGHT = new int[16][16];
}