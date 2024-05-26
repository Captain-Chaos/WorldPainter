/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.util;

import org.pepsoft.worldpainter.Constants;
import org.pepsoft.worldpainter.biomeschemes.BiomeSchemeManager;

import java.io.File;

/**
 * A provider of Minecraft jar files.
 *
 * @author pepijn
 */
public interface MinecraftJarProvider {
    /**
     * Get a Minecraft jar corresponding to a specific Minecraft version.
     *
     * <p><strong>Note:</strong> this is a very rudimentary mechanism; for better control it is recommended to use
     * {@link BiomeSchemeManager}.
     *
     * @param biomeAlgorithm The Minecraft version for which to obtain a jar file, as specified by one of the
     * {@code BIOME_ALGORITHM_*} constants in {@link Constants}.
     * @return A Minecraft jar file corresponding with the specified version, or {@code null} if such a jar file could
     * not be found.
     */
    File getMinecraftJar(int biomeAlgorithm);
}