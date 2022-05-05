package org.pepsoft.worldpainter;

import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.util.TextProgressReceiver;
import org.pepsoft.worldpainter.importing.JavaMapImporter;
import org.pepsoft.worldpainter.importing.MapImporter;
import org.pepsoft.worldpainter.plugins.BlockBasedPlatformProvider;
import org.pepsoft.worldpainter.plugins.PlatformManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Random;

import static com.google.common.primitives.Ints.asList;
import static org.pepsoft.minecraft.Constants.DEFAULT_WATER_LEVEL;
import static org.pepsoft.worldpainter.Terrain.GRASS;

public class ImportTester extends AbstractTool {
    public static void main(String[] args) throws IOException, ProgressReceiver.OperationCancelled {
        initialisePlatform();

        File savesDir = new File(args[0]);
        for (File mapDir: savesDir.listFiles()) {
            if (mapDir.isDirectory()) {
                Platform platform = PlatformManager.getInstance().identifyPlatform(mapDir);
                TileFactory tileFactory = TileFactoryFactory.createNoiseTileFactory(new Random().nextLong(), GRASS, platform.minZ, platform.standardMaxHeight, 58, DEFAULT_WATER_LEVEL, false, true, 20, 1.0);
                JavaMapImporter importer = new JavaMapImporter(platform, tileFactory, new File(mapDir, "level.dat"),
                        false, null, MapImporter.ReadOnlyOption.NONE,
                        new HashSet<>(asList(((BlockBasedPlatformProvider) PlatformManager.getInstance().getPlatformProvider(platform)).getDimensions(platform, mapDir))));
                System.out.println("+---------+---------+---------+---------+---------+");
                World2 world = importer.doImport(new TextProgressReceiver());
                logger.info("Successfully imported world {}", world.getName());
            }
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(ImportTester.class);
}