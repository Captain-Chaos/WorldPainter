/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.exporting;

import org.pepsoft.minecraft.ChunkFactory;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.plugins.PlatformManager;

import static org.pepsoft.minecraft.Material.BEDROCK;
import static org.pepsoft.worldpainter.Platform.Capability.BIOMES;
import static org.pepsoft.worldpainter.Platform.Capability.BIOMES_3D;
import static org.pepsoft.worldpainter.biomeschemes.Minecraft1_17Biomes.BIOME_PLAINS;

/**
 *
 * @author pepijn
 */
public class BedrockWallChunk {
    public static ChunkFactory.ChunkCreationResult create(int chunkX, int chunkZ, Dimension dimension, Platform platform) {
        final int maxHeight = dimension.getMaxHeight();
        final ChunkFactory.ChunkCreationResult result = new ChunkFactory.ChunkCreationResult();
        result.chunk = PlatformManager.getInstance().createChunk(platform, chunkX, chunkZ, maxHeight);
        final int maxY = maxHeight - 1;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (platform.capabilities.contains(BIOMES)) {
                    result.chunk.setBiome(x, z, BIOME_PLAINS);
                }
                for (int y = 0; y <= maxY; y++) {
                    result.chunk.setMaterial(x, y, z, BEDROCK);
                }
                result.chunk.setHeight(x, z, maxY);
            }
        }
        if (platform.capabilities.contains(BIOMES_3D)) {
            for (int x = 0; x < 4; x++) {
                for (int z = 0; z < 4; z++) {
                    for (int y = 0; y < maxHeight; y += 4) {
                        result.chunk.set3DBiome(x, y >> 2, z, BIOME_PLAINS);
                    }
                }
            }
        }
        result.chunk.setTerrainPopulated(true);
        result.stats.landArea = 0;
        result.stats.surfaceArea = 256;
        result.stats.waterArea = 0;
        return result;
    }
}