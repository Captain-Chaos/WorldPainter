/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.util;

import org.jnbt.CompoundTag;
import org.jnbt.NBTInputStream;
import org.jnbt.Tag;
import org.pepsoft.minecraft.*;
import org.pepsoft.util.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;

import static org.pepsoft.minecraft.Constants.DATA_VERSION_MC_1_12_2;
import static org.pepsoft.minecraft.Constants.DATA_VERSION_MC_1_13;
import static org.pepsoft.minecraft.Constants.VERSION_MCREGION;
import static org.pepsoft.worldpainter.Constants.DIM_END;
import static org.pepsoft.worldpainter.Constants.DIM_NETHER;
import static org.pepsoft.worldpainter.Constants.DIM_NORMAL;

/**
 *
 * @author pepijn
 */
public class MinecraftUtil {
    private MinecraftUtil() {
        // Prevent instantiation
    }
    
    public static File findMinecraftDir() {
        File candidate;
        String env = System.getProperty("org.pepsoft.worldpainter.minecraftDir");
        if (env != null) {
            candidate = new File(env);
            if (candidate.isDirectory()) {
                return candidate;
            } else {
                logger.error("Minecraft directory from system property does not exist: {}; continuing without Minecraft installation", env);
                return null;
            }
        }
        if (SystemUtils.isWindows()) {
            String appData = System.getenv("APPDATA");
            if (appData != null) {
                candidate = new File(appData, ".minecraft");
                if (candidate.isDirectory()) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Using Minecraft installation in {}", candidate);
                    }
                    return candidate;
                }
            }
        } else if (SystemUtils.isMac()) {
            candidate = new File(System.getProperty("user.home"), "Library/Application Support/minecraft");
            if (candidate.isDirectory()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Using Minecraft installation in {}", candidate);
                }
                return candidate;
            }
        }
        candidate = new File(System.getProperty("user.home"), ".minecraft");
        if (candidate.isDirectory()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Using Minecraft installation in {}", candidate);
            }
            return candidate;
        }
        return null;
    }
    
    public static File findMinecraftJar(MinecraftJarProvider minecraftJarProvider) {
        for (int i = 10; i >= 1; i--) {
            File candidate = minecraftJarProvider.getMinecraftJar(i);
            if ((candidate != null) && candidate.isFile() && candidate.canRead()) {
                return candidate;
            }
        }
        File minecraftDir = findMinecraftDir();
        if (minecraftDir != null) {
            File candidate = new File(minecraftDir, "bin/minecraft.jar");
            if (candidate.isFile() && candidate.canRead()) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Visit all the chunks of a Minecraft map.
     *
     * @param worldDir The map directory.
     * @param dimension The ordinal of the dimension to visit.
     * @param visitor The visitor to invoke for each chunk.
     * @throws IOException If an I/O error occurs while reading the chunks.
     */
    public static void visitChunks(File worldDir, int dimension, ChunkVisitor visitor) throws IOException {
        final File levelDatFile = new File(worldDir, "level.dat");
        final Level level = Level.load(levelDatFile);
        final int version = level.getVersion(), maxHeight = level.getMaxHeight(), dataVersion = level.getDataVersion();
        final File regionDir;
        switch (dimension) {
            case DIM_NORMAL:
                regionDir = new File(worldDir, "region");
                break;
            case DIM_NETHER:
                regionDir = new File(worldDir, "DIM-1/region");
                break;
            case DIM_END:
                regionDir = new File(worldDir, "DIM1/region");
                break;
            default:
                throw new IllegalArgumentException("Don't know where to find dimension " + dimension);
        }
        final Pattern regionFilePattern = (version == VERSION_MCREGION)
                ? Pattern.compile("r\\.-?\\d+\\.-?\\d+\\.mcr")
                : Pattern.compile("r\\.-?\\d+\\.-?\\d+\\.mca");
        final File[] regionFiles = regionDir.listFiles((dir, name) -> regionFilePattern.matcher(name).matches());
        for (File file: regionFiles) {
            try (RegionFile regionFile = new RegionFile(file, true)) {
                for (int x = 0; x < 32; x++) {
                    for (int z = 0; z < 32; z++) {
                        if (regionFile.containsChunk(x, z)) {
                            final Tag tag;
                            final InputStream chunkData = regionFile.getChunkDataInputStream(x, z);
                            try (NBTInputStream in = new NBTInputStream(chunkData)) {
                                tag = in.readTag();
                            } catch (RuntimeException e) {
                                logger.error("{} while reading tag for chunk {},{}", e.getClass().getSimpleName(), x, z);
                                e.printStackTrace();
                                continue;
                            }
                            final Chunk chunk;
                            try {
                                chunk = (version == VERSION_MCREGION)
                                    ? new MCRegionChunk((CompoundTag) tag, maxHeight)
                                    : ((dataVersion > DATA_VERSION_MC_1_12_2)
                                        ? new MC113AnvilChunk((CompoundTag) tag, maxHeight)
                                        : new MC12AnvilChunk((CompoundTag) tag, maxHeight));
                            } catch (RuntimeException e) {
                                logger.error("{} while parsing tag for chunk {},{}", e.getClass().getSimpleName(), x, z);
                                e.printStackTrace();
                                logger.error(tag.toString());
                                continue;
                            }
                            visitor.visitChunk(chunk);
                        }
                    }
                }
            }
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(MinecraftUtil.class);

    @FunctionalInterface
    public interface ChunkVisitor {
        void visitChunk(Chunk chunk);
    }
}