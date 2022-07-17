/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.tools;

import org.pepsoft.minecraft.Constants;
import org.pepsoft.util.DesktopUtils;
import org.pepsoft.util.ProgressReceiver.OperationCancelled;
import org.pepsoft.util.TextProgressReceiver;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.exporting.WorldExporter;
import org.pepsoft.worldpainter.plugins.PlatformManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.cert.CertificateException;

/**
 *
 * @author pepijn
 */
public class Export extends AbstractTool {
    public static void main(String[] args) throws IOException, ClassNotFoundException, OperationCancelled, CertificateException, UnloadableWorldException {
        initialisePlatform();

        File worldFile = new File(args[0]);
        logger.info("Loading " + worldFile);
        World2 world;
        try (FileInputStream in = new FileInputStream(worldFile)) {
            WorldIO worldIO = new WorldIO();
            worldIO.load(in);
            world = worldIO.getWorld();
        }

        for (int i = 0; i < Terrain.CUSTOM_TERRAIN_COUNT; i++) {
            MixedMaterial material = world.getMixedMaterial(i);
            Terrain.setCustomMaterial(i, material);
        }
        if (world.getPlatform() == null) {
            if (world.getMaxHeight() == Constants.DEFAULT_MAX_HEIGHT_ANVIL) {
                world.setPlatform(DefaultPlugin.JAVA_ANVIL);
            } else {
                world.setPlatform(DefaultPlugin.JAVA_MCREGION);
            }
        }

        Platform platform = world.getPlatform();
        File exportDir;
        PlatformManager platformManager = PlatformManager.getInstance();
        if (args.length > 1) {
            exportDir = new File(args[1]);
        } else {
            exportDir = platformManager.getDefaultExportDir(platform);
            if (exportDir == null) {
                exportDir = DesktopUtils.getDocumentsFolder();
            }
        }
        logger.info("Exporting to " + exportDir);
        WorldExporter exporter = platformManager.getExporter(world);
        exporter.export(exportDir, world.getName(), exporter.selectBackupDir(exportDir, world.getName()), new TextProgressReceiver());
        System.out.println();
        logger.info("World " + world.getName() + " exported successfully");
    }

    private static final Logger logger = LoggerFactory.getLogger(Export.class);
}