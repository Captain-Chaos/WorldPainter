package org.dynmap;

import org.dynmap.hdmap.*;
import org.pepsoft.util.Version;
import org.pepsoft.worldpainter.Configuration;
import org.pepsoft.worldpainter.biomeschemes.BiomeSchemeManager;
import org.pepsoft.worldpainter.dynmap.DynmapBlockStateHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

/**
 * An alternative implementation of {@link HDMapManager} which creates a hard
 * coded perspective, shader and lighting instead of instantiating them from a
 * configuration file. Used by this module's private implementation of
 * {@link MapManager}.
 *
 * <p>Created by Pepijn Schmitz on 08-06-15.
 */
class WPHDMapManager extends HDMapManager {
    void init(ConfigurationNode configNode) {
        final Version minecraftVersion = BiomeSchemeManager.getLatestMinecraftVersion();
        final DynmapCore core = new DynmapCore(minecraftVersion);
        perspectives.put("default", new IsoHDPerspective(core, configNode));
        final DefaultHDShader solidShader = new DefaultHDShader(core, configNode);
        shaders.put("solid", solidShader);
        shaders.put("default", solidShader);
//        if (minecraftVersion == null) {
//            logger.info("No Minecraft jars found; falling back to solid shading for 3D dynmap previews");
//        } else {
//            final File minecraftJar = BiomeSchemeManager.getMinecraftJar(minecraftVersion);
//            if (checkDynmapResources(minecraftJar)) {
                // Note that technically we're reporting the wrong version number here and theoretically it could be
                // wrong. In practice it should usually be right though:
//                logger.info("Using textures from Minecraft jar " + minecraftJar.getName() + " for 3D dynmap previews");
                configNode.put("texturepack", "standard");
                DynmapBlockStateHelper.initialise();
                HDBlockModels.loadModels(core, configNode);
                TexturePack.loadTextureMapping(core, configNode);
                // Force initialisation of texture pack to get early errors:
                TexturePack.getTexturePack(core, "standard");
                final TexturePackHDShader texturedShader = new TexturePackHDShader(core, configNode);
                shaders.put("textured", texturedShader);
                shaders.put("default", texturedShader);
//            } else {
//                // Could not copy the textures for dynmap for whatever reason;
//                // fall back to solid colours
//                logger.error("Error copying textures from Minecraft; falling back to solid shading for 3D dynmap previews");
//            }
//        }
        shaders.put("caves", new CaveHDShader(core, configNode));
        lightings.put("default", new DefaultHDLighting(core, configNode));
    }

    /**
     * Dynmap is tightly coupled to certain resources existing on the filesystem. Check that they are there and if not
     * create them.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored") // Implicitly checked later
    private boolean checkDynmapResources(File latestJar) {
        final File texPackDir = new File(Configuration.getConfigDir(), "dynmap/texturepacks");
        if (! texPackDir.isDirectory()) {
            texPackDir.mkdirs();
        }
        final File existingJar = new File(texPackDir, "standard");
        if ((! existingJar.isFile()) || (latestJar.lastModified() > existingJar.lastModified())) {
            logger.info("Copying textures from Minecraft jar " + latestJar.getName() + " for 3D dynmap previews");
            return createDynmapResources(latestJar, existingJar);
        } else {
            return true;
        }
    }

    /**
     * Create the filesystem resources dynmap needs in order to operate
     * correctly.
     */
    private boolean createDynmapResources(File latestJar, File existingJar) {
        try {
            try (JarFile jarFile = new JarFile(latestJar, false); JarOutputStream out = new JarOutputStream(new FileOutputStream(existingJar))) {
                final byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;

                // Dynmap needs the terrain.png file to exist, otherwise it doesn't
                // work correctly, even though resource packs don't in fact contain
                // a terrain.png file
                if (logger.isDebugEnabled()) {
                    logger.debug("Writing terrain.png to " + existingJar);
                }
                out.putNextEntry(new JarEntry("terrain.png"));
                try (InputStream in = ClassLoader.getSystemResourceAsStream("terrain.png")) {
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }

                // Copy the rest, leaving out unnecessary files
                for (Enumeration<JarEntry> e = jarFile.entries(); e.hasMoreElements(); ) {
                    final JarEntry entry = e.nextElement();
                    if ((! entry.getName().endsWith(".class")) && (! entry.getName().startsWith("META-INF/"))) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Copying " + entry + " from " + latestJar + " to " + existingJar);
                        }
                        out.putNextEntry(entry);
                        try (InputStream in = jarFile.getInputStream(entry)) {
                            while ((bytesRead = in.read(buffer)) != -1) {
                                out.write(buffer, 0, bytesRead);
                            }
                        }
                    } else if (logger.isDebugEnabled()) {
                        logger.debug("Skipping " + entry + " from " + latestJar);
                    }
                }
            }
            return true;
        } catch (IOException e) {
            // The file may be corrupt at this point, so delete it and try again next time
            existingJar.delete();
            logger.error("I/O error copying Minecraft jar to WorldPainter config directory to serve as dynmap texture pack", e);
            return false;
        }
    }

    private static final int BUFFER_SIZE = 32768;
    private static final Logger logger = LoggerFactory.getLogger(WPHDMapManager.class);
}