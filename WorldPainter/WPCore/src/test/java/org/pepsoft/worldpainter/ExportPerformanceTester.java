package org.pepsoft.worldpainter;

import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.util.TextProgressReceiver;
import org.pepsoft.worldpainter.exporting.WorldExportSettings;
import org.pepsoft.worldpainter.exporting.WorldExporter;
import org.pepsoft.worldpainter.plugins.PlatformManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.SortedMap;
import java.util.TreeMap;

import static java.util.Collections.singleton;
import static org.pepsoft.util.FileUtils.deleteDir;
import static org.pepsoft.worldpainter.Constants.DIM_NORMAL;

public class ExportPerformanceTester extends AbstractTool {
    public static void main(String[] args) throws IOException, ProgressReceiver.OperationCancelled {
        initialisePlatform();
        logger.info("Available processor count: {}", Runtime.getRuntime().availableProcessors());

        final WorldIO worldIO = new WorldIO();
        worldIO.load(new FileInputStream(args[0]));
        final World2 world = worldIO.getWorld();
        for (int i = 0; i < Terrain.CUSTOM_TERRAIN_COUNT; i++) {
            MixedMaterial material = world.getMixedMaterial(i);
            Terrain.setCustomMaterial(i, material);
        }

        for (int threadCount = 1; threadCount <= 16; threadCount++) {
            testMap(world, threadCount);
        }

        logger.info("Results: {}", exportResults);
    }

    private static void testMap(World2 world, int threadCount) throws IOException, ProgressReceiver.OperationCancelled {
        System.setProperty("org.pepsoft.worldpainter.threads", Integer.toString(threadCount));

        logger.info("Testing export of {} with {} thread(s)", world.getName(), threadCount);
        final WorldExporter exporter = PlatformManager.getInstance().getExporter(world, new WorldExportSettings(singleton(DIM_NORMAL), null, null));
        final File baseDir = new File(System.getProperty("user.dir"), "tmp-exported-worlds");
        final File backupDir = exporter.selectBackupDir(baseDir, world.getName());
        long start = System.currentTimeMillis();
        try {
            exporter.export(baseDir, world.getName(), backupDir, new TextProgressReceiver());
        } finally {
            final long duration = System.currentTimeMillis() - start;
            exportResults.put(threadCount, duration);
            logger.info("Exporting {} took {} ms", world.getName(), duration);
            if (baseDir.isDirectory()) {
                deleteDir(baseDir);
            }
        }
    }

    private static final SortedMap<Integer, Long> exportResults = new TreeMap<>();
    private static final Logger logger = LoggerFactory.getLogger(ExportPerformanceTester.class);
}