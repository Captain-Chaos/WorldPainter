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
public abstract class HeightMapChunkFactory implements ChunkFactory {
    public HeightMapChunkFactory(int maxHeight, int version) {
        this.maxHeight = maxHeight;
        this.version = version;
    }

    @Override
    public final int getMaxHeight() {
        return maxHeight;
    }
    
    @Override
    public final ChunkCreationResult createChunk(int chunkX, int chunkZ) {
        final ChunkCreationResult result = new ChunkCreationResult();
        result.chunk = (version == SUPPORTED_VERSION_1) ? new ChunkImpl(chunkX, chunkZ, maxHeight) : new ChunkImpl2(chunkX, chunkZ, maxHeight);
        final int maxY = maxHeight - 1;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int height = getHeight(chunkX * 16 + x, chunkZ * 16 + z);
                for (int y = 0; y <= maxY; y++) {
                    if (y == 0) {
                        result.chunk.setBlockType(x, y, z, BLK_BEDROCK);
                    } else if (y <= (height - 3)) {
                        result.chunk.setBlockType(x, y, z, BLK_STONE);
                    } else if (y < height) {
                        result.chunk.setBlockType(x, y, z, BLK_DIRT);
                    } else if (y == height) {
                        result.chunk.setBlockType(x, y, z, BLK_GRASS);
                    } else {
                        result.chunk.setSkyLightLevel(x, y, z, 15);
                    }
                }
                result.chunk.setHeight(x, z, (height < maxY) ? (height + 1): maxY);
            }
        }
        result.chunk.setTerrainPopulated(true);
        result.stats.surfaceArea = 256;
        result.stats.landArea = 256;
        return result;
    }

    protected abstract int getHeight(int x, int z);
    
    protected final int maxHeight, version;
}