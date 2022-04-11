package org.pepsoft.worldpainter;

import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.util.TextProgressReceiver;
import org.pepsoft.worldpainter.importing.MapImporter;
import org.pepsoft.worldpainter.plugins.MapImporterProvider;
import org.pepsoft.worldpainter.plugins.PlatformManager;
import org.pepsoft.worldpainter.plugins.PlatformProvider;
import org.pepsoft.worldpainter.plugins.PlatformProvider.MapInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static java.util.Collections.singleton;
import static java.util.Objects.requireNonNull;
import static org.pepsoft.worldpainter.Constants.DIM_NORMAL;
import static org.pepsoft.worldpainter.Terrain.GRASS;
import static org.pepsoft.worldpainter.importing.MapImporter.ReadOnlyOption.MAN_MADE_ABOVE_GROUND;

public class ImportPerformanceTester extends AbstractTool {
    public static void main(String[] args) throws IOException, ProgressReceiver.OperationCancelled {
        initialisePlatform();
        logger.info("Available processor count: {}", Runtime.getRuntime().availableProcessors());

        final File mapsDir = new File(args[0]);
        final PlatformManager platformManager = PlatformManager.getInstance();

        for (int threadCount = 1; threadCount <= 16; threadCount++) {
            File[] mapDirs = requireNonNull(mapsDir.listFiles());
            for (File mapDir: mapDirs) {
                final MapInfo mapInfo = platformManager.identifyMap(mapDir);
                if (mapInfo != null) {
                    testMap(mapDir, mapInfo, threadCount);
                }
            }
        }

        logger.info("Results: {}", importResults);
    }

    private static void testMap(File mapDir, MapInfo mapInfo, int threadCount) throws IOException, ProgressReceiver.OperationCancelled {
        System.setProperty("org.pepsoft.worldpainter.threads", Integer.toString(threadCount));

        logger.info("Testing import of {} with {} thread(s)", mapDir, threadCount);
        final Platform platform = mapInfo.platform;
        final PlatformProvider provider = PlatformManager.getInstance().getPlatformProvider(platform);
        final MapImporter importer = ((MapImporterProvider) provider).getImporter(mapDir, TileFactoryFactory.createNoiseTileFactory(0, GRASS, platform.minZ, platform.standardMaxHeight, 58, 62, false, true, 20, 1.0), null, MAN_MADE_ABOVE_GROUND, singleton(DIM_NORMAL)); // TODO include the other dimensions
        long start = System.currentTimeMillis();
        try {
            importer.doImport(new TextProgressReceiver());
        } finally {
            final long duration = System.currentTimeMillis() - start;
            importResults.computeIfAbsent(mapDir, f -> new TreeMap<>()).put(threadCount, duration);
            logger.info("Importing {} took {} ms", mapDir, duration);
        }
    }

    private static final Map<File, SortedMap<Integer, Long>> importResults = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(ImportPerformanceTester.class);
}