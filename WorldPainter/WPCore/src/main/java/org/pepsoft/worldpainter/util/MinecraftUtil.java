/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.util;

import org.jetbrains.annotations.NotNull;
import org.pepsoft.minecraft.Chunk;
import org.pepsoft.util.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static org.pepsoft.worldpainter.Constants.*;

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

    @NotNull
    public static File getRegionDir(File worldDir, int dimension) {
        File regionDir;
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
                throw new IllegalArgumentException("Dimension " + dimension + " not supported");
        }
        return regionDir;
    }

    public static String blocksToWalkingTime(int blocks) {
        int mins = Math.round(blocks / 256f);
        if (mins < 60) {
            return mins + " min.";
        } else {
            int hours = mins / 60;
            mins -= 60 * hours;
            if (mins == 0) {
                return hours + "hr.";
            } else {
                return hours + "hr., " + mins + " min.";
            }
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(MinecraftUtil.class);

    @FunctionalInterface
    public interface ChunkVisitor {
        void visitChunk(Chunk chunk);
    }
}