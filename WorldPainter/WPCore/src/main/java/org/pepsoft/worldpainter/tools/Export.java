/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.tools;

import org.pepsoft.minecraft.ChunkFactory;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.Comparator.comparing;
import static org.pepsoft.worldpainter.Dimension.Role.DETAIL;
import static org.pepsoft.worldpainter.exporting.WorldExportSettings.EXPORT_EVERYTHING;

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
        final WorldExporter exporter = platformManager.getExporter(world, EXPORT_EVERYTHING);
        final Map<Integer, ChunkFactory.Stats> stats = exporter.export(exportDir, world.getName(), exporter.selectBackupDir(exportDir, world.getName()), new TextProgressReceiver());
        System.out.println();
        stats.forEach((dim, dimStats) -> {
            logger.info("Stats for dimension {}", new Dimension.Anchor(dim, DETAIL, false, 0).getDefaultName());
            final List<Map.Entry<Object, AtomicLong>> list = new LinkedList<>(dimStats.timings.entrySet());
            list.sort(comparing(entry -> entry.getValue().get()));
            list.forEach(entry -> logger.info("    {}: {} ms", entry.getKey(), entry.getValue().get() / 1_000_000));
            long overhead = dimStats.time - (list.stream().mapToLong(entry -> entry.getValue().get()).sum() / 1_000_000);
            logger.info("    Overhead: {} ms", overhead);
        });
        logger.info("World " + world.getName() + " exported successfully");
    }

    private static final Logger logger = LoggerFactory.getLogger(Export.class);
}