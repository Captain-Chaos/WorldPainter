/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.exporting;

import org.pepsoft.minecraft.Chunk;
import org.pepsoft.util.Box;

import java.util.Arrays;

import static org.pepsoft.minecraft.Block.BLOCK_TRANSPARENCY;
import static org.pepsoft.minecraft.Block.LIGHT_SOURCES;
import static org.pepsoft.minecraft.Constants.HIGHEST_KNOWN_BLOCK_ID;

/**
 * A lighting calculator for MinecraftWorlds.
 * 
 * <p>The process consists of two passes. In the first pass, all chunks which
 * have previously been marked as having "primary light dirty" are visited and
 * have their primary light recalculated. "Primary light" means full daylight,
 * and block light at the site of any luminous blocks such as torches.
 *
 * @author pepijn
 */
public class LightingCalculator {
    public LightingCalculator(MinecraftWorld world) {
        this.world = world;
        maxHeight = world.getMaxHeight();
    }

    public Box getDirtyArea() {
        return dirtyArea;
    }

    public void setDirtyArea(Box dirtyArea) {
        this.dirtyArea = dirtyArea;
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
        for (int x = dirtyArea.getX1(); x <= dirtyArea.getX2(); x++) {
            for (int z = dirtyArea.getZ1(); z <= dirtyArea.getZ2(); z++) {
                Chunk chunk = world.getChunk(x >> 4, z >> 4);
                if (chunk == null) {
                    continue;
                }
                for (int y = dirtyArea.getY1(); y <= dirtyArea.getY2(); y++) {
                    int blockType = world.getBlockTypeAt(x, z, y);
                    int currentSkylightLevel = world.getSkyLightLevel(x, z, y);
                    int currentBlockLightLevel = world.getBlockLightLevel(x, z, y);
                    int newSkyLightLevel;
                    int newBlockLightLevel;
                    if ((blockType <= HIGHEST_KNOWN_BLOCK_ID) && (BLOCK_TRANSPARENCY[blockType] == 15)) {
                        // Opaque block
                        newSkyLightLevel = 0;
                        newBlockLightLevel = (LIGHT_SOURCES[blockType] > 0) ? currentBlockLightLevel : 0;
                    } else {
                        // Transparent block, or unknown block. We err on the
                        // side of transparency for unknown blocks to try and
                        // cause less visible lighting bugs
                        newSkyLightLevel = (currentSkylightLevel < 15) ? calculateSkyLightLevel(x, y, z) : 15;
                        newBlockLightLevel = ((blockType <= HIGHEST_KNOWN_BLOCK_ID) && (LIGHT_SOURCES[blockType] > 0)) ? currentBlockLightLevel : calculateBlockLightLevel(x, y, z);
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
            dirtyArea.setY2(Math.min(highestY, maxHeight - 1));
        }
        return changed;
    }

    public int[] calculatePrimaryLight(Chunk chunk) {
        for (int x = 0; x < 16; x++) {
            Arrays.fill(DAYLIGHT[x], true);
            Arrays.fill(HEIGHT[x], maxHeight - 1);
        }
        // The point above which there are only transparent, non light source
        // blocks
        int lightingVolumeHighMark = 0;
        // The point below which there are only non-transparent, non light
        // source blocks
        int lightingVolumeLowMark = maxHeight - 1;
        for (int y = maxHeight - 1; y >= 0; y--) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int blockType = chunk.getBlockType(x, y, z);
                    int blockLightLevel = chunk.getBlockLightLevel(x, y, z);
                    int skyLightLevel = chunk.getSkyLightLevel(x, y, z);
                    int newBlockLightLevel, newSkyLightLevel;
                    if ((blockType > HIGHEST_KNOWN_BLOCK_ID) || (BLOCK_TRANSPARENCY[blockType] < 15)) {
                        // Transparent block, or unknown block. We err on the
                        // side of transparency for unknown blocks to try and
                        // cause less visible lighting bugs
                        if (y < lightingVolumeLowMark) {
                            lightingVolumeLowMark = y;
                        }
                        int transparency = (blockType > HIGHEST_KNOWN_BLOCK_ID) ? 0 : BLOCK_TRANSPARENCY[blockType];
                        if ((transparency == 0) && (DAYLIGHT[x][z])) {
                            // Propagate daylight down
                            newSkyLightLevel = 15;
                            HEIGHT[x][z] = y;
                        } else {
                            if ((transparency > 0) && (y > lightingVolumeHighMark)) {
                                lightingVolumeHighMark = y;
                            }
                            newSkyLightLevel = 0;
                            DAYLIGHT[x][z] = false;
                        }
                    } else {
                        if (y > lightingVolumeHighMark) {
                            lightingVolumeHighMark = y;
                        }
                        newSkyLightLevel = 0;
                        DAYLIGHT[x][z] = false;
                    }
                    if ((blockType <= HIGHEST_KNOWN_BLOCK_ID) && (LIGHT_SOURCES[blockType] > 0)) {
                        if (y > lightingVolumeHighMark) {
                            lightingVolumeHighMark = y;
                        }
                        if (y < lightingVolumeLowMark) {
                            lightingVolumeLowMark = y;
                        }
                        newBlockLightLevel = LIGHT_SOURCES[blockType];
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
                Chunk chunk = world.getChunkForEditing(x >> 4, z >> 4);
                if (chunk == null) {
                    continue;
                }
                boolean daylight = (dirtyArea.getY2() >= (world.getMaxHeight() - 1)) ? true : (world.getSkyLightLevel(x, z, dirtyArea.getY2() + 1) == 15);
                for (int y = dirtyArea.getY2(); y >= dirtyArea.getY1(); y--) {
                    int blockType = chunk.getBlockType(x & 0xf, y, z & 0xf);
                    int blockLightLevel = chunk.getBlockLightLevel(x & 0xf, y, z & 0xf);
                    int skyLightLevel = chunk.getSkyLightLevel(x & 0xf, y, z & 0xf);
                    int newBlockLightLevel, newSkyLightLevel;
                    if ((blockType > HIGHEST_KNOWN_BLOCK_ID) || (BLOCK_TRANSPARENCY[blockType] < 15)) {
                        // Transparent block, or unknown block. We err on the
                        // side of transparency for unknown blocks to try and
                        // cause less visible lighting bugs
                        int transparency = (blockType > HIGHEST_KNOWN_BLOCK_ID) ? 0 : BLOCK_TRANSPARENCY[blockType];
                        if ((transparency == 0) && daylight) {
                            // Propagate daylight down
                            newSkyLightLevel = 15;
                        } else {
                            newSkyLightLevel = 0;
                            daylight = false;
                        }
                    } else {
                        newSkyLightLevel = 0;
                        daylight = false;
                    }
                    if ((blockType <= HIGHEST_KNOWN_BLOCK_ID) && (LIGHT_SOURCES[blockType] > 0)) {
                        newBlockLightLevel = LIGHT_SOURCES[blockType];
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

    private int calculateSkyLightLevel(int x, int y, int z) {
        int blockType = world.getBlockTypeAt(x, z, y);
        int skyLightLevel = getSkyLightLevelAt(x, y + 1, z);
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
        return Math.max(highestSurroundingSkyLight - Math.max((blockType <= HIGHEST_KNOWN_BLOCK_ID) ? BLOCK_TRANSPARENCY[blockType] : 0, 1), 0);
    }

    private int calculateBlockLightLevel(int x, int y, int z) {
        int blockType = world.getBlockTypeAt(x, z, y);
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
        return Math.max(highestSurroundingBlockLight - Math.max((blockType <= HIGHEST_KNOWN_BLOCK_ID) ? BLOCK_TRANSPARENCY[blockType] : 0, 1), 0);
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
    private final int maxHeight;
    private Box dirtyArea;

    private final boolean[][] DAYLIGHT = new boolean[16][16];
    private final int[][] HEIGHT = new int[16][16];
}