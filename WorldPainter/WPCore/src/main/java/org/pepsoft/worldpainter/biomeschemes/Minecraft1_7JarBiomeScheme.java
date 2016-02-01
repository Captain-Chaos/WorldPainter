/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.biomeschemes;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import org.pepsoft.util.Checksum;
import org.pepsoft.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author pepijn
 */
public abstract class Minecraft1_7JarBiomeScheme extends AbstractMinecraft1_7BiomeScheme {
    public Minecraft1_7JarBiomeScheme(File minecraftJar, File libDir, Checksum md5Sum, Map<Checksum, String[]> hashesToClassNames, String version) {
        if (logger.isDebugEnabled()) {
            logger.debug("Creating biome scheme using Minecraft jar {}", minecraftJar);
        }
        if (md5Sum == null) {
            try {
                md5Sum = FileUtils.getMD5(minecraftJar);
            } catch (IOException e) {
                throw new RuntimeException("I/O error calculating hash for " + minecraftJar, e);
            }
        }
        String[] classNames = hashesToClassNames.get(md5Sum);
        String landscapeClassName = classNames[0];
        String bufferManagerClassName = classNames[1];
        String worldGeneratorClassName = classNames[2];
        try {
            ClassLoader classLoader = getClassLoader(minecraftJar, libDir);
            Class<?> landscapeClass = classLoader.loadClass(landscapeClassName);
            worldGeneratorClass = classLoader.loadClass(worldGeneratorClassName);
            getLandscapesMethod = landscapeClass.getMethod("a", long.class, worldGeneratorClass);
            getBiomesMethod = landscapeClass.getMethod("a", int.class, int.class, int.class, int.class);
            Class<?> bufferManagerClass = classLoader.loadClass(bufferManagerClassName);
            clearBuffersMethod = bufferManagerClass.getMethod("a");
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            throw new RuntimeException("Not a valid " + version + " minecraft.jar", e);
        }
    }

    @Override
    public void setSeed(long seed) {
        if ((seed != this.seed) || (landscape == null)) {
            try {
                landscape = ((Object[]) getLandscapesMethod.invoke(null, seed, null))[1];
                this.seed = seed;
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Access denied while trying to set the seed", e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException("Exception thrown while trying to set the seed", e);
            }
        }
    }
    
    @Override
    public final synchronized void getBiomes(int x, int y, int width, int height, int[] buffer) {
        try {
            int[] biomes = (int[]) getBiomesMethod.invoke(landscape, x, y, width, height);
            clearBuffersMethod.invoke(null);
            System.arraycopy(biomes, 0, buffer, 0, Math.min(biomes.length, buffer.length));
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Access denied while trying to calculate biomes", e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Exception thrown while trying to calculate biomes", e);
        }
    }
    
    final Class<?> worldGeneratorClass;
    final Method getLandscapesMethod, getBiomesMethod, clearBuffersMethod;
    Object landscape;
    long seed = Long.MIN_VALUE;

    private static final Logger logger = LoggerFactory.getLogger(Minecraft1_7JarBiomeScheme.class);
}