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
import static org.pepsoft.util.MathUtils.clamp;
import static org.pepsoft.worldpainter.DefaultPlugin.*;

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
        minHeight = world.getMinHeight();
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
        final int x1InChunks = dirtyArea.getX1() >> 4, z1InChunks = dirtyArea.getZ1() >> 4,
                x2InChunks = (dirtyArea.getX2() - 1) >> 4, z2InChunks = (dirtyArea.getZ2() - 1) >> 4;
        maxHeightsXOffset = x1InChunks;
        maxHeightsZOffset = z1InChunks;
        maxHeights = new int[z2InChunks - z1InChunks + 1][x2InChunks - x1InChunks + 1];
        for (int[] row: maxHeights) {
            Arrays.fill(row, Integer.MIN_VALUE);
        }
        // Create a map of the highest block in each column that could possibly be affected (e.g. could have block light
        // > 0 or sky light < 15)
        for (int chunkZ = z1InChunks; chunkZ <= z2InChunks; chunkZ++) {
            for (int chunkX = x1InChunks; chunkX <= x2InChunks; chunkX++) {
                final Chunk chunk = world.getChunk(chunkX, chunkZ);
                final int highestPossibleAffectedBlock = (chunk != null) ? Math.min(chunk.getHighestNonAirBlock() + 15, maxHeight - 1) : Integer.MIN_VALUE;
                // Make sure the surrounding chunks are also at least as high, since blocks from this chunk could affect
                // the block lighting in them
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        final int xInMaxHeightsMap = chunkX + dx - maxHeightsXOffset;
                        final int zInMaxHeightsMap = chunkZ + dz - maxHeightsZOffset;
                        if ((xInMaxHeightsMap >= 0) && (zInMaxHeightsMap >= 0) && (zInMaxHeightsMap < maxHeights.length) && (xInMaxHeightsMap < maxHeights[0].length)) {
                            maxHeights[zInMaxHeightsMap][xInMaxHeightsMap] = Math.max(maxHeights[zInMaxHeightsMap][xInMaxHeightsMap], highestPossibleAffectedBlock);
                        }
                    }
                }
            }
        }
    }

    /**
     * For the selected chunk and the chunks around it, calculate the secondary
     * light, if necessary. The dirty area is constricted to the blocks that
     * were actually changed, and {@code false} is returned if no changes
     * were made at all (indicating that lighting is complete).
     */
    public boolean calculateSecondaryLight() {
        final int x1InChunks = dirtyArea.getX1() >> 4, z1InChunks = dirtyArea.getZ1() >> 4,
                x2InChunks = (dirtyArea.getX2() - 1) >> 4, z2InChunks = (dirtyArea.getZ2() - 1) >> 4;
        int lowestY = Integer.MAX_VALUE, highestY = Integer.MIN_VALUE;
        boolean changed = false;
        for (int chunkX = x1InChunks; chunkX <= x2InChunks; chunkX++) {
            for (int chunkZ = z1InChunks; chunkZ <= z2InChunks; chunkZ++) {
                final Chunk chunk = world.getChunk(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }
                final int maxY = Math.min(dirtyArea.getY2() - 1, maxHeights[chunkZ - maxHeightsZOffset][chunkX - maxHeightsXOffset]);
                for (int xInChunk = 0; xInChunk < 16; xInChunk++) {
                    for (int zInChunk = 0; zInChunk < 16; zInChunk++) {
                        final int x = (chunkX << 4) | xInChunk, z = (chunkZ << 4) | zInChunk;
                        for (int y = maxY; y >= dirtyArea.getY1() ; y--) {
                            final Material material = chunk.getMaterial(xInChunk, y, zInChunk);
                            final int currentSkylightLevel = chunk.getSkyLightLevel(xInChunk, y, zInChunk);
                            final int currentBlockLightLevel = chunk.getBlockLightLevel(xInChunk, y, zInChunk);
                            final int newSkyLightLevel;
                            final int newBlockLightLevel;
                            if (material.opaque) {
                                // Opaque block
                                newSkyLightLevel = 0;
                                newBlockLightLevel = (material.blockLight > 0) ? currentBlockLightLevel : 0;
                            } else {
                                // Transparent block, or unknown block. We err on the
                                // side of transparency for unknown blocks to try and
                                // cause less visible lighting bugs
                                newSkyLightLevel = (currentSkylightLevel < 15) ? calculateSkyLightLevel(chunk, x, y, z, material) : 15;
                                newBlockLightLevel = (material.blockLight > 0) ? currentBlockLightLevel : calculateBlockLightLevel(chunk, x, y, z);
                            }
                            if ((newSkyLightLevel != currentSkylightLevel) || (newBlockLightLevel != currentBlockLightLevel)) {
                                if (newSkyLightLevel != currentSkylightLevel) {
                                    chunk.setSkyLightLevel(xInChunk, y, zInChunk, newSkyLightLevel);
                                }
                                if (newBlockLightLevel != currentBlockLightLevel) {
                                    chunk.setBlockLightLevel(xInChunk, y, zInChunk, newBlockLightLevel);
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
            }
        }
        if (changed) {
            dirtyArea.setY1(Math.max(lowestY, minHeight));
            dirtyArea.setY2(Math.min(highestY + 1, maxHeight));
        }
        return changed;
    }

    public int[] calculatePrimaryLight(Chunk chunk) {
        for (int x = 0; x < 16; x++) {
            Arrays.fill(DAYLIGHT[x], true);
            Arrays.fill(HEIGHT[x], clamp(minHeight, chunk.getHighestNonAirBlock(), maxHeight - 1));
        }
        // The point above which there are only transparent, non light source
        // blocks
        int lightingVolumeHighMark = minHeight;
        // The point below which there are only non-transparent, non light
        // source blocks
        int lightingVolumeLowMark = maxHeight - 1;
        for (int y = clamp(minHeight - 1, chunk.getHighestNonAirBlock(), maxHeight - 1); y >= minHeight; y--) { // TODO: will this leave dark areas above the starting level? ANSWER: yes; we need to fix this
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    final Material material = chunk.getMaterial(x, y, z);
                    final int blockLightLevel = chunk.getBlockLightLevel(x, y, z);
                    final int skyLightLevel = chunk.getSkyLightLevel(x, y, z);
                    final int newBlockLightLevel, newSkyLightLevel;
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
        final int x1InChunks = dirtyArea.getX1() >> 4, z1InChunks = dirtyArea.getZ1() >> 4,
                x2InChunks = (dirtyArea.getX2() - 1) >> 4, z2InChunks = (dirtyArea.getZ2() - 1) >> 4;
        for (int chunkX = x1InChunks; chunkX <= x2InChunks; chunkX++) {
            for (int chunkZ = z1InChunks; chunkZ <= z2InChunks; chunkZ++) {
                final Chunk chunk = world.getChunk(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }
                int maxY = clamp(minHeight - 1, chunk.getHighestNonAirBlock(), dirtyArea.getY2());
                for (int xInChunk = 0; xInChunk < 16; xInChunk++) {
                    for (int zInChunk = 0; zInChunk < 16; zInChunk++) {
                        final int x = (chunkX << 4) | xInChunk, z = (chunkZ << 4) | zInChunk;
                        int skyLightLevelAbove = (maxY >= (world.getMaxHeight() - 1)) ? 15 : world.getSkyLightLevel(x, z, maxY + 1);
                        for (int y = maxY; y >= dirtyArea.getY1(); y--) {
                            final Material material = chunk.getMaterial(xInChunk, y, zInChunk);
                            final int blockLightLevel = chunk.getBlockLightLevel(xInChunk, y, zInChunk);
                            final int skyLightLevel = chunk.getSkyLightLevel(xInChunk, y, zInChunk);
                            final int newBlockLightLevel, newSkyLightLevel;
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
                                chunk.setBlockLightLevel(xInChunk, y, zInChunk, newBlockLightLevel);
                            }
                            if (newSkyLightLevel != skyLightLevel) {
                                chunk.setSkyLightLevel(xInChunk, y, zInChunk, newSkyLightLevel);
                            }
                        }
                    }
                }
            }
        }
    }

    private int getTransparency(Material material) {
        // TODOMC13: make this generic:
        if (((platform == JAVA_ANVIL_1_15) || (platform == JAVA_ANVIL_1_17) || (platform == JAVA_ANVIL_1_18)) && material.isNamed(MC_WATER)) {
            return 1;
        } else {
            return material.opacity;
        }
    }

    // MC coordinate system
    private int calculateSkyLightLevel(Chunk chunk, int x, int y, int z, Material material) {
        int skyLightLevel = getSkyLightLevelAt(chunk, x, y + 1, z);
        // TODOMC13: make this generic:
        if ((skyLightLevel == 15)
                && ((platform == JAVA_ANVIL_1_15) || (platform == JAVA_ANVIL_1_17) || (platform == JAVA_ANVIL_1_18))
                && (material.isNamed(MC_WATER))
                && ((y >= maxHeight - 1) || (world.getMaterialAt(x, z, y + 1) == AIR))) {
            // This seems to be a special case in MC 1.15. TODO: keep an eye on whether this was a bug or intended behaviour!
            return 15;
        }
        int highestSurroundingSkyLight = skyLightLevel;
        if (highestSurroundingSkyLight < 15) {
            skyLightLevel = getSkyLightLevelAt(chunk, x - 1, y, z);
            if (skyLightLevel > highestSurroundingSkyLight) {
                highestSurroundingSkyLight = skyLightLevel;
            }
            if (highestSurroundingSkyLight < 15) {
                skyLightLevel = getSkyLightLevelAt(chunk, x + 1, y, z);
                if (skyLightLevel > highestSurroundingSkyLight) {
                    highestSurroundingSkyLight = skyLightLevel;
                }
                if (highestSurroundingSkyLight < 15) {
                    skyLightLevel = getSkyLightLevelAt(chunk, x, y, z - 1);
                    if (skyLightLevel > highestSurroundingSkyLight) {
                        highestSurroundingSkyLight = skyLightLevel;
                    }
                    if (highestSurroundingSkyLight < 15) {
                        skyLightLevel = getSkyLightLevelAt(chunk, x, y, z + 1);
                        if (skyLightLevel > highestSurroundingSkyLight) {
                            highestSurroundingSkyLight = skyLightLevel;
                        }
                        if (highestSurroundingSkyLight < 15) {
                            skyLightLevel = getSkyLightLevelAt(chunk, x, y - 1, z);
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

    // MC coordinate system
    private int calculateBlockLightLevel(Chunk chunk, int x, int y, int z) {
        Material material = chunk.getMaterial(x & 0xf, y, z & 0xf);
        int blockLightLevel = getBlockLightLevelAt(chunk, x, y + 1, z);
        int highestSurroundingBlockLight = blockLightLevel;
        if (highestSurroundingBlockLight < 15) {
            blockLightLevel = getBlockLightLevelAt(chunk, x - 1, y, z);
            if (blockLightLevel > highestSurroundingBlockLight) {
                highestSurroundingBlockLight = blockLightLevel;
            }
            if (highestSurroundingBlockLight < 15) {
                blockLightLevel = getBlockLightLevelAt(chunk, x + 1, y, z);
                if (blockLightLevel > highestSurroundingBlockLight) {
                    highestSurroundingBlockLight = blockLightLevel;
                }
                if (highestSurroundingBlockLight < 15) {
                    blockLightLevel = getBlockLightLevelAt(chunk, x, y, z - 1);
                    if (blockLightLevel > highestSurroundingBlockLight) {
                        highestSurroundingBlockLight = blockLightLevel;
                    }
                    if (highestSurroundingBlockLight < 15) {
                        blockLightLevel = getBlockLightLevelAt(chunk, x, y, z + 1);
                        if (blockLightLevel > highestSurroundingBlockLight) {
                            highestSurroundingBlockLight = blockLightLevel;
                        }
                        if (highestSurroundingBlockLight < 15) {
                            blockLightLevel = getBlockLightLevelAt(chunk, x, y - 1, z);
                            if (blockLightLevel > highestSurroundingBlockLight) {
                                highestSurroundingBlockLight = blockLightLevel;
                            }
                        }
                    }
                }
            }
        }
        return Math.max(highestSurroundingBlockLight - Math.max(getTransparency(material), 1), 0);
    }

    // MC coordinate system
    private int getSkyLightLevelAt(Chunk chunk, int x, int y, int z) {
        if (y < minHeight) {
            return 0;
        } else if (y >= maxHeight) {
            return 15;
        } else if (((x >> 4) == chunk.getxPos()) && ((z >> 4) == chunk.getzPos())) {
            return chunk.getSkyLightLevel(x & 0xf, y, z & 0xf);
        } else {
            return world.getSkyLightLevel(x, z, y);
        }
    }

    // MC coordinate system
    private int getBlockLightLevelAt(Chunk chunk, int x, int y, int z) {
        if ((y < minHeight) || (y >= maxHeight)) {
            return 0;
        } else if (((x >> 4) == chunk.getxPos()) && ((z >> 4) == chunk.getzPos())) {
            return chunk.getBlockLightLevel(x & 0xf, y, z & 0xf);
        } else {
            return world.getBlockLightLevel(x, z, y);
        }
    }

    private final MinecraftWorld world;
    private final Platform platform;
    private final int minHeight, maxHeight;
    private Box dirtyArea;
    private int[][] maxHeights;
    private int maxHeightsXOffset, maxHeightsZOffset;

    private final boolean[][] DAYLIGHT = new boolean[16][16];
    private final int[][] HEIGHT = new int[16][16];
}