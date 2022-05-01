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

import static org.pepsoft.minecraft.Constants.MC_DISTANCE;
import static org.pepsoft.minecraft.Constants.MC_WATER;
import static org.pepsoft.minecraft.Material.*;
import static org.pepsoft.worldpainter.DefaultPlugin.*;

/**
 * A block properties calculator for MinecraftWorlds. This can calculate properties of blocks that are influenced by
 * neighbouring blocks and may there need multiple passes. It can currently calculate three properties: daylight, block
 * light and leaf distance. It can also remove leaf blocks for which the distance is too great.
 * 
 * <p>The process consists of three passes. In the first pass, the blocks are set to their initial values:
 *
 * <ul>
 *     <li>Daylight is initialised to full daylight for those blocks not covered from above
 *     <li>Block light is initialised to the appropriate value for each material according to the materials database
 *     <li>The leaf distance property is removed for all leaf blocks
 * </ul>
 *
 * <p>In the second pass, the previously calculated values are propagated into the surrounding blocks. The second pass
 * should be repeated until no changes result from it, meaning the area has been fully processed.
 *
 * <p>In the third pass the process is finalised, e.g. any floating leaf blocks are removed. The third pass should be
 * executed by after the second pass has returned {@code false}.
 *
 * <p>This class uses the Minecraft coordinate system.
 *
 * @author pepijn
 */
public class BlockPropertiesCalculator {
    public BlockPropertiesCalculator(MinecraftWorld world, Platform platform, BlockBasedExportSettings exportSettings) {
        this.world = world;
        this.platform = platform;
        skyLight = exportSettings.isCalculateSkyLight();
        blockLight = exportSettings.isCalculateBlockLight();
        leafDistance = exportSettings.isCalculateLeafDistance();
        removeFloatingLeaves = exportSettings.isRemoveFloatingLeaves();
        if (removeFloatingLeaves && (! leafDistance)) {
            throw new IllegalArgumentException("removeFloatingLeaves requires calculateLeafDistance");
        } else if ((! skyLight) && (! blockLight) && (! leafDistance)) {
            throw new IllegalArgumentException("Nothing to do");
        }
        minHeight = world.getMinHeight();
        maxHeight = world.getMaxHeight();
    }

    /**
     * Get the current dirty area in Minecraft coordinates.
     *
     * @return The current dirty area in Minecraft coordinates.
     */
    public Box getDirtyArea() {
        return dirtyArea;
    }

    /**
     * Set the dirty area in Minecraft coordinates.
     *
     * @param dirtyArea The dirty area in Minecraft coordinates to set.
     */
    public void setDirtyArea(Box dirtyArea) {
        this.dirtyArea = dirtyArea;
        originalDirtyArea = dirtyArea.clone();
        final int x1InChunks = dirtyArea.getX1() >> 4, z1InChunks = dirtyArea.getZ1() >> 4,
                x2InChunks = (dirtyArea.getX2() - 1) >> 4, z2InChunks = (dirtyArea.getZ2() - 1) >> 4;
        maxHeightsXOffset = x1InChunks;
        maxHeightsZOffset = z1InChunks;
        maxHeights = new int[z2InChunks - z1InChunks + 1][x2InChunks - x1InChunks + 1];
        for (int[] row: maxHeights) {
            Arrays.fill(row, Integer.MIN_VALUE);
        }
        // Create a map of the highest block in each column that could possibly be affected (e.g. could have block light
        // > 0, sky light < 15, or leaf blocks)
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
     * For the current dirty area, propagate the selected block properties to surrounding blocks. The dirty area is
     * constricted to the blocks that were actually changed, and {@code false} is returned if no changes were made at
     * all (indicating that the process is complete).
     */
    public boolean secondPass() {
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
                            boolean changedBlock = false;
                            Material material = chunk.getMaterial(xInChunk, y, zInChunk);
                            if (leafDistance && material.name.endsWith("_leaves")) {
                                final int currentDistance = material.getProperty(DISTANCE, 8);
                                final int distance = Math.min(currentDistance, calculateDistance(chunk, x, y, z));
                                if (distance != currentDistance) {
                                    material = material.withProperty(DISTANCE, distance);
                                    chunk.setMaterial(xInChunk, y, zInChunk, material);
                                    changedBlock = true;
                                }
                            }
                            if (skyLight) {
                                final int currentSkylightLevel = chunk.getSkyLightLevel(xInChunk, y, zInChunk);
                                final int newSkyLightLevel;
                                if (material.opaque) {
                                    // Opaque block
                                    newSkyLightLevel = 0;
                                } else {
                                    // Transparent block, or unknown block. We err on the
                                    // side of transparency for unknown blocks to try and
                                    // cause less visible lighting bugs
                                    newSkyLightLevel = (currentSkylightLevel < 15) ? calculateSkyLightLevel(chunk, x, y, z, material) : 15;
                                }
                                if (newSkyLightLevel != currentSkylightLevel) {
                                    chunk.setSkyLightLevel(xInChunk, y, zInChunk, newSkyLightLevel);
                                    changedBlock = true;
                                }
                            }
                            if (blockLight) {
                                final int currentBlockLightLevel = chunk.getBlockLightLevel(xInChunk, y, zInChunk);
                                final int newBlockLightLevel;
                                if (material.opaque) {
                                    // Opaque block
                                    newBlockLightLevel = (material.blockLight > 0) ? currentBlockLightLevel : 0;
                                } else {
                                    // Transparent block, or unknown block. We err on the
                                    // side of transparency for unknown blocks to try and
                                    // cause less visible lighting bugs
                                    newBlockLightLevel = (material.blockLight > 0) ? currentBlockLightLevel : calculateBlockLightLevel(chunk, x, y, z);
                                }
                                if (newBlockLightLevel != currentBlockLightLevel) {
                                    chunk.setBlockLightLevel(xInChunk, y, zInChunk, newBlockLightLevel);
                                    changedBlock = true;
                                }
                            }
                            if (changedBlock) {
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

    /**
     * Set the blocks to their initial values for one entire chunk.
     */
    public int[] firstPass(Chunk chunk) {
        for (int x = 0; x < 16; x++) {
            Arrays.fill(DAYLIGHT[x], true);
            Arrays.fill(HEIGHT[x], Math.min(chunk.getHighestNonAirBlock(), maxHeight - 1));
        }
        // The point above which there are only transparent, non light source and non-leaf blocks
        int dirtyVolumeHighMark = minHeight;
        // The point below which there are only non-transparent, non light source and non-leaf blocks
        int dirtyVolumeLowMark = maxHeight - 1;
        for (int y = Math.min(chunk.getHighestNonAirBlock(), maxHeight - 1); y >= minHeight; y--) { // TODO: will this leave dark areas above the starting level?
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    Material material = chunk.getMaterial(x, y, z);
                    if (leafDistance && material.name.endsWith("_leaves")) {
                        if (material.isPropertySet(MC_DISTANCE)) {
                            material = material.withoutProperty(MC_DISTANCE);
                            chunk.setMaterial(x, y, z, material);
                        }
                        if (y < dirtyVolumeLowMark) {
                            dirtyVolumeLowMark = y;
                        }
                        if (y > dirtyVolumeHighMark) {
                            dirtyVolumeHighMark = y;
                        }
                    }
                    if (skyLight) {
                        final int skyLightLevel = chunk.getSkyLightLevel(x, y, z);
                        final int newSkyLightLevel;
                        if (! material.opaque) {
                            // Transparent or translucent block
                            if (y < dirtyVolumeLowMark) {
                                dirtyVolumeLowMark = y;
                            }
                            int transparency = getTransparency(material);
                            if ((transparency == 0) && (DAYLIGHT[x][z])) {
                                // Propagate daylight down
                                newSkyLightLevel = 15;
                                HEIGHT[x][z] = y;
                            } else {
                                if ((transparency > 0) && (y > dirtyVolumeHighMark)) {
                                    dirtyVolumeHighMark = y;
                                }
                                newSkyLightLevel = 0; // TODO adjust with transparency of block above and 1-per-block falloff rather than going straight to zero
                                DAYLIGHT[x][z] = false;
                            }
                        } else {
                            if (y > dirtyVolumeHighMark) {
                                dirtyVolumeHighMark = y;
                            }
                            newSkyLightLevel = 0;
                            DAYLIGHT[x][z] = false;
                        }
                        if (newSkyLightLevel != skyLightLevel) {
                            chunk.setSkyLightLevel(x, y, z, newSkyLightLevel);
                        }
                    }
                    if (blockLight) {
                        final int blockLightLevel = chunk.getBlockLightLevel(x, y, z);
                        final int newBlockLightLevel;
                        if (material.blockLight > 0) {
                            if (y > dirtyVolumeHighMark) {
                                dirtyVolumeHighMark = y;
                            }
                            if (y < dirtyVolumeLowMark) {
                                dirtyVolumeLowMark = y;
                            }
                            newBlockLightLevel = material.blockLight;
                        } else {
                            newBlockLightLevel = 0;
                        }
                        if (newBlockLightLevel != blockLightLevel) {
                            chunk.setBlockLightLevel(x, y, z, newBlockLightLevel);
                        }
                    }
                }
            }
        }
        if (skyLight) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    if (chunk.getHeight(x, z) != HEIGHT[x][z]) {
                        chunk.setHeight(x, z, HEIGHT[x][z]);
                    }
                }
            }
        }
        return new int[] { dirtyVolumeLowMark, dirtyVolumeHighMark} ;
    }

    /**
     * Set the blocks to their initial values for the current dirty area.
     */
    public void firstPass() {
        final int x1InChunks = dirtyArea.getX1() >> 4, z1InChunks = dirtyArea.getZ1() >> 4,
                x2InChunks = (dirtyArea.getX2() - 1) >> 4, z2InChunks = (dirtyArea.getZ2() - 1) >> 4;
        for (int chunkX = x1InChunks; chunkX <= x2InChunks; chunkX++) {
            for (int chunkZ = z1InChunks; chunkZ <= z2InChunks; chunkZ++) {
                final Chunk chunk = world.getChunk(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }
                int maxY = Math.min(chunk.getHighestNonAirBlock(), dirtyArea.getY2());
                for (int xInChunk = 0; xInChunk < 16; xInChunk++) {
                    for (int zInChunk = 0; zInChunk < 16; zInChunk++) {
                        final int x = (chunkX << 4) | xInChunk, z = (chunkZ << 4) | zInChunk;
                        int skyLightLevelAbove = (maxY >= (world.getMaxHeight() - 1)) ? 15 : world.getSkyLightLevel(x, z, maxY + 1);
                        for (int y = maxY; y >= dirtyArea.getY1(); y--) {
                            Material material = chunk.getMaterial(xInChunk, y, zInChunk);
                            if (leafDistance && material.name.endsWith("_leaves")) {
                                if (material.isPropertySet(MC_DISTANCE)) {
                                    material = material.withoutProperty(MC_DISTANCE);
                                    chunk.setMaterial(xInChunk, y, zInChunk, material);
                                }
                            }
                            if (skyLight) {
                                final int skyLightLevel = chunk.getSkyLightLevel(xInChunk, y, zInChunk);
                                final int newSkyLightLevel;
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
                                if (newSkyLightLevel != skyLightLevel) {
                                    chunk.setSkyLightLevel(xInChunk, y, zInChunk, newSkyLightLevel);
                                }
                            }
                            if (blockLight) {
                                final int blockLightLevel = chunk.getBlockLightLevel(xInChunk, y, zInChunk);
                                final int newBlockLightLevel;
                                if (material.blockLight > 0) {
                                    newBlockLightLevel = material.blockLight;
                                } else {
                                    newBlockLightLevel = 0;
                                }
                                if (newBlockLightLevel != blockLightLevel) {
                                    chunk.setBlockLightLevel(xInChunk, y, zInChunk, newBlockLightLevel);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * This should be invoked once after {@link #secondPass()} has returned {@code false}, to take any necessary final
     * steps of the process, such as removing floating leaf blocks.
     */
    public void finalise() {
        if (! removeFloatingLeaves) {
            return;
        }
        final int x1InChunks = originalDirtyArea.getX1() >> 4, z1InChunks = originalDirtyArea.getZ1() >> 4,
                x2InChunks = (originalDirtyArea.getX2() - 1) >> 4, z2InChunks = (originalDirtyArea.getZ2() - 1) >> 4;
        for (int chunkX = x1InChunks; chunkX <= x2InChunks; chunkX++) {
            for (int chunkZ = z1InChunks; chunkZ <= z2InChunks; chunkZ++) {
                final Chunk chunk = world.getChunk(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }
                final int maxY = Math.min(originalDirtyArea.getY2() - 1, maxHeights[chunkZ - maxHeightsZOffset][chunkX - maxHeightsXOffset]);
                for (int xInChunk = 0; xInChunk < 16; xInChunk++) {
                    for (int zInChunk = 0; zInChunk < 16; zInChunk++) {
                        final int x = (chunkX << 4) | xInChunk, z = (chunkZ << 4) | zInChunk;
                        for (int y = maxY; y >= originalDirtyArea.getY1() ; y--) {
                            Material material = chunk.getMaterial(xInChunk, y, zInChunk);
                            if (material.name.endsWith("_leaves") && material.isPropertySet(MC_DISTANCE) && (material.getProperty(DISTANCE) > 6) && (! material.is(PERSISTENT))) {
                                material = AIR;
                                chunk.setMaterial(xInChunk, y, zInChunk, material);
                                if (skyLight) {
                                    final int currentSkylightLevel = chunk.getSkyLightLevel(xInChunk, y, zInChunk);
                                    final int newSkyLightLevel = (currentSkylightLevel < 15) ? calculateSkyLightLevel(chunk, x, y, z, material) : 15;
                                    if (newSkyLightLevel != currentSkylightLevel) {
                                        chunk.setSkyLightLevel(xInChunk, y, zInChunk, newSkyLightLevel);
                                        // As a quick hack to avoid the worst lighting bugs, propagate new daylight down
                                        // until we hit a non-transparent block
                                        if (newSkyLightLevel == 15) {
                                            for (int y2 = y - 1; y2 >= originalDirtyArea.getY1() ; y2--) {
                                                if (chunk.getMaterial(xInChunk, y2, zInChunk).transparent) {
                                                    chunk.setSkyLightLevel(xInChunk, y, zInChunk, newSkyLightLevel);
                                                } else {
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }
                                if (blockLight) {
                                    final int currentBlockLightLevel = chunk.getBlockLightLevel(xInChunk, y, zInChunk);
                                    final int newBlockLightLevel = (material.blockLight > 0) ? currentBlockLightLevel : calculateBlockLightLevel(chunk, x, y, z);
                                    if (newBlockLightLevel != currentBlockLightLevel) {
                                        chunk.setBlockLightLevel(xInChunk, y, zInChunk, newBlockLightLevel);
                                    }
                                }
                                // NOTE: in theory we should start all the way over with the lighting calculations, but
                                // that would take a huge amount of time again, so instead we just hope the lighting
                                // bugs are not too obvious
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

    // MC coordinate system
    private int calculateDistance(Chunk chunk, int x, int y, int z) {
        int distance = getLeafDistanceTo(chunk, x, y, z + 1);
        if (distance == 1) {
            return distance;
        }
        distance = Math.min(distance, getLeafDistanceTo(chunk, x - 1, y, z));
        if (distance == 1) {
            return distance;
        }
        distance = Math.min(distance, getLeafDistanceTo(chunk, x, y - 1, z));
        if (distance == 1) {
            return distance;
        }
        distance = Math.min(distance, getLeafDistanceTo(chunk, x + 1, y, z));
        if (distance == 1) {
            return distance;
        }
        distance = Math.min(distance, getLeafDistanceTo(chunk, x, y + 1, z));
        if (distance == 1) {
            return distance;
        }
        return Math.min(distance, getLeafDistanceTo(chunk, x, y, z - 1));
    }

    // MC coordinate system
    private int getLeafDistanceTo(Chunk chunk, int x, int y, int z) {
        final Material material;
        if ((y < minHeight) || (y >= maxHeight)) {
            return 7;
        } else if (((x >> 4) == chunk.getxPos()) && ((z >> 4) == chunk.getzPos())) {
            material = chunk.getMaterial(x & 0xf, y, z & 0xf);
        } else {
            material = world.getMaterialAt(x, z, y);
        }
        if (material.name.endsWith("_log") || material.name.endsWith("_wood")) {
            return 1;
        } else if (material.name.endsWith("_leaves") && material.isPropertySet(MC_DISTANCE)) {
            return material.getProperty(DISTANCE) + 1;
        } else {
            return 7;
        }
    }

    private final MinecraftWorld world;
    private final Platform platform;
    private final boolean skyLight, blockLight, leafDistance, removeFloatingLeaves;
    private final int minHeight, maxHeight;
    private Box originalDirtyArea, dirtyArea;
    private int[][] maxHeights;
    private int maxHeightsXOffset, maxHeightsZOffset;

    private final boolean[][] DAYLIGHT = new boolean[16][16];
    private final int[][] HEIGHT = new int[16][16];
}