/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.exporting;

import org.pepsoft.minecraft.ChunkFactory;
import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.plugins.PlatformManager;
import org.pepsoft.worldpainter.util.BiomeUtils;

import static org.pepsoft.minecraft.Material.BARRIER;
import static org.pepsoft.minecraft.Material.BEDROCK;
import static org.pepsoft.worldpainter.biomeschemes.Minecraft1_17Biomes.BIOME_PLAINS;

/**
 *
 * @author pepijn
 */
public class WallChunk {
    public static ChunkFactory.ChunkCreationResult create(int chunkX, int chunkZ, Dimension dimension, Platform platform) {
        final int minHeight = platform.minZ, maxHeight = dimension.getMaxHeight();
        final BiomeUtils biomeUtils = new BiomeUtils();
        final ChunkFactory.ChunkCreationResult result = new ChunkFactory.ChunkCreationResult();
        result.chunk = PlatformManager.getInstance().createChunk(platform, chunkX, chunkZ, maxHeight);
        final int maxY = maxHeight - 1;
        final Material wallMaterial = (dimension.getWallType() == Dimension.WallType.BEDROCK) ? BEDROCK : BARRIER;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (platform.supportsBiomes()) {
                    biomeUtils.set2DBiome(result.chunk, x, z, BIOME_PLAINS);
                }
                for (int y = minHeight; y <= maxY; y++) {
                    result.chunk.setMaterial(x, y, z, wallMaterial);
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