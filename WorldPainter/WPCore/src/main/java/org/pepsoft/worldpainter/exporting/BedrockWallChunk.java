/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.exporting;

import static org.pepsoft.worldpainter.biomeschemes.Minecraft1_7Biomes.BIOME_PLAINS;
import org.pepsoft.minecraft.ChunkFactory;
import org.pepsoft.minecraft.ChunkImpl;
import org.pepsoft.minecraft.ChunkImpl2;
import org.pepsoft.minecraft.Constants;
import org.pepsoft.worldpainter.Dimension;
import static org.pepsoft.minecraft.Constants.*;

/**
 *
 * @author pepijn
 */
public class BedrockWallChunk {
    public static ChunkFactory.ChunkCreationResult create(int chunkX, int chunkZ, Dimension dimension) {
        final int maxHeight = dimension.getMaxHeight();
        final int version = dimension.getWorld().getVersion();
        final ChunkFactory.ChunkCreationResult result = new ChunkFactory.ChunkCreationResult();
        result.chunk = (version == Constants.SUPPORTED_VERSION_1) ? new ChunkImpl(chunkX, chunkZ, maxHeight) : new ChunkImpl2(chunkX, chunkZ, maxHeight);
        final int maxY = maxHeight - 1;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (version == SUPPORTED_VERSION_2) {
                    result.chunk.setBiome(x, z, BIOME_PLAINS);
                }
                for (int y = 0; y <= maxY; y++) {
                    result.chunk.setBlockType(x, y, z, BLK_BEDROCK);
                }
                result.chunk.setHeight(x, z, maxY);
            }
        }
        result.chunk.setTerrainPopulated(true);
        result.stats.landArea = 0;
        result.stats.surfaceArea = 256;
        result.stats.waterArea = 0;
        return result;
    }
}