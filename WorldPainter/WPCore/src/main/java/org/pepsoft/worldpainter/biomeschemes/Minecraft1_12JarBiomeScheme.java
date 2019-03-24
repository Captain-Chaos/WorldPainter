/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.biomeschemes;

import org.pepsoft.util.Checksum;
import org.pepsoft.util.FileUtils;
import org.pepsoft.worldpainter.BiomeScheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import static org.pepsoft.worldpainter.biomeschemes.MinecraftRuntimeUtils.getClassLoader;

/**
 * An abstract base class for {@link BiomeScheme}s which can invoke Minecraft
 * code from a Minecraft jar file for version 1.12 to calculate biomes.
 *
 * @author pepijn
 */
public abstract class Minecraft1_12JarBiomeScheme extends AbstractMinecraft1_7BiomeScheme {
    public Minecraft1_12JarBiomeScheme(File minecraftJar, File libDir, Checksum md5Sum, Map<Checksum, String[]> hashesToClassNames) {
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
        try {
            init(hashesToClassNames.get(md5Sum), getClassLoader(minecraftJar, libDir));
        } catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException e) {
            throw new RuntimeException("Not a valid minecraft.jar of the correct version", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Access denied while trying to initialise Minecraft", e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Exception thrown while trying to initialise Minecraft", e);
        }
    }

    @Override
    public void setSeed(long seed) {
        if ((landscape == null) || (seed != this.seed)) {
            try {
                landscape = ((Object[]) getLandscapesMethod.invoke(null, seed, null, null))[1];
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

    protected void init(String[] classNames, ClassLoader classLoader) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        String landscapeClassName             = classNames[0];
        String bufferManagerClassName         = classNames[1];
        String worldGeneratorClassName        = classNames[2];
        String initClassName                  = classNames[3];
        String generatorSettingsClassName     = classNames[4];
        Class<?> landscapeClass = classLoader.loadClass(landscapeClassName);
        worldGeneratorClass = classLoader.loadClass(worldGeneratorClassName);
        Class<?> generatorSettingsClass = classLoader.loadClass(generatorSettingsClassName);
        getLandscapesMethod = landscapeClass.getMethod("a", long.class, worldGeneratorClass, generatorSettingsClass);
        getBiomesMethod = landscapeClass.getMethod("a", int.class, int.class, int.class, int.class);
        Class<?> bufferManagerClass = classLoader.loadClass(bufferManagerClassName);
        clearBuffersMethod = bufferManagerClass.getMethod("a");

        // Initialise Minecraft
        // Minecraft replaces the system in- and output streams, but we don't
        // want that
        InputStream in = System.in;
        PrintStream out = System.out;
        PrintStream err = System.err;
        try {
            Class<?> initClass = classLoader.loadClass(initClassName);
            Method initMethod = initClass.getMethod("c");
            initMethod.invoke(null);
        } finally {
            System.setIn(in);
            System.setOut(out);
            System.setErr(err);
        }
    }

    Class<?> worldGeneratorClass;
    Method getLandscapesMethod, getBiomesMethod, clearBuffersMethod;
    Object landscape;
    long seed = Long.MIN_VALUE;

    private static final Logger logger = LoggerFactory.getLogger(Minecraft1_12JarBiomeScheme.class);
}