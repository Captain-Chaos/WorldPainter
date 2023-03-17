package org.pepsoft.worldpainter.tools;

import com.google.common.collect.ImmutableSet;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.util.TextProgressReceiver;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.importing.MapImporter;
import org.pepsoft.worldpainter.plugins.MapImporterProvider;
import org.pepsoft.worldpainter.plugins.PlatformManager;
import org.pepsoft.worldpainter.plugins.PlatformProvider;

import java.io.File;
import java.io.IOException;

import static org.pepsoft.minecraft.Constants.DEFAULT_WATER_LEVEL;
import static org.pepsoft.worldpainter.Constants.*;
import static org.pepsoft.worldpainter.importing.MapImporter.ReadOnlyOption.MAN_MADE;

public class Import extends AbstractTool {
    public static void main(String[] args) throws ProgressReceiver.OperationCancelled, IOException {
        initialisePlatform();

        // Import the map
        final File mapDir = new File(args[0]);
        final PlatformManager platformManager = PlatformManager.getInstance();
        PlatformProvider.MapInfo mapInfo = platformManager.identifyMap(mapDir);
        final PlatformProvider platformProvider = platformManager.getPlatformProvider(mapInfo.platform);
        final TileFactory tileFactory = TileFactoryFactory.createNoiseTileFactory(0, Terrain.GRASS, mapInfo.minHeight, mapInfo.maxHeight, 58, DEFAULT_WATER_LEVEL, false, true, 20, 1.0);
        final MapImporter importer = ((MapImporterProvider) platformProvider).getImporter(mapDir, tileFactory, null, MAN_MADE, ImmutableSet.of(DIM_NORMAL, DIM_NETHER, DIM_END) /* TODO */);
        System.out.println("+---------+---------+---------+---------+---------+");
        World2 world = importer.doImport(new TextProgressReceiver());
        System.out.println("Successfully imported world " + world.getName());
    }
}