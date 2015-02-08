/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft;

import static org.pepsoft.minecraft.Constants.*;

/**
 *
 * @author pepijn
 */
public class SimpleChunkFactory implements ChunkFactory {
    public SimpleChunkFactory(int maxHeight, int version) {
        this.maxHeight = maxHeight;
        this.version = version;
    }

    @Override
    public int getMaxHeight() {
        return maxHeight;
    }
    
    @Override
    public ChunkCreationResult createChunk(int chunkX, int chunkZ) {
        final ChunkCreationResult result = new ChunkCreationResult();
        result.chunk = (version == SUPPORTED_VERSION_1)
            ? new ChunkImpl(chunkX, chunkZ, maxHeight)
            : new ChunkImpl2(chunkX, chunkZ, maxHeight);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < maxHeight; y++) {
                    if (y == 0) {
                        result.chunk.setBlockType(x, y, z, BLK_BEDROCK);
                    } else if (y <= 3) {
                        result.chunk.setBlockType(x, y, z, BLK_STONE);
                    } else if (y <= 5) {
                        result.chunk.setBlockType(x, y, z, BLK_DIRT);
                    } else if (y == 6) {
                        result.chunk.setBlockType(x, y, z, BLK_GRASS);
                    } else {
                        result.chunk.setSkyLightLevel(x, y, z, 15);
                    }
                }
                result.chunk.setHeight(x, z, 7);
            }
        }
        result.chunk.setTerrainPopulated(true);
        result.stats.surfaceArea = 256;
        result.stats.landArea = 256;
        return result;
    }
    
    private final int maxHeight, version;
}