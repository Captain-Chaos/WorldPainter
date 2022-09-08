/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.tools;

import org.pepsoft.util.ProgressReceiver.OperationCancelled;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.exporting.JavaWorldExporter;
import org.pepsoft.worldpainter.gardenofeden.Garden;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Random;

import static org.pepsoft.minecraft.Constants.DEFAULT_WATER_LEVEL;
import static org.pepsoft.worldpainter.DefaultPlugin.JAVA_ANVIL_1_15;
import static org.pepsoft.worldpainter.Dimension.Anchor.NORMAL_DETAIL;
import static org.pepsoft.worldpainter.exporting.WorldExportSettings.EXPORT_EVERYTHING;
//import org.pepsoft.worldpainter.gardenofeden.Inn;

/**
 *
 * @author pepijn
 */
public class TestTool {
    public static void main(String[] args) throws IOException, OperationCancelled {
        Random random = new Random();
        long seed = random.nextLong();
//        TileFactory tileFactory = new NoiseTileFactory(Terrain.GRASS, DEFAULT_MAX_HEIGHT_ANVIL, 58, DEFAULT_WATER_LEVEL, false, false);
        TileFactory tileFactory = TileFactoryFactory.createFlatTileFactory(seed, Terrain.GRASS, JAVA_ANVIL_1_15.minZ, JAVA_ANVIL_1_15.standardMaxHeight, DEFAULT_WATER_LEVEL, 0, false, false);
        World2 world = new World2(JAVA_ANVIL_1_15, seed, tileFactory);
        world.setName("TestWorld");
        world.setSpawnPoint(new Point(64, 64));
        world.setGameType(GameType.CREATIVE);
        Dimension dimension = world.getDimension(NORMAL_DETAIL);
        dimension.addTile(tileFactory.createTile(0, 0));
        Garden garden = dimension.getGarden();
//        Inn inn = new Inn(garden, seed, null, new Point(64, 64), 1, 9, 9, EAST, 3, RandomOne.of(ThemeManager.getInstance().getThemes()), false, true, true, true, true, true, true, true, true, true, true, Inn.createName(seed));
//        Inn inn = new Inn(garden, seed, null, new Point(48, 48), 1, 9, 9, SOUTH, 3, RandomOne.of(ThemeManager.getInstance().getThemes()), false, true, true, true, true, true, true, true, true, true, true, Inn.createName(seed));
//        garden.plantSeed(inn);
//        inn = new Inn(garden, seed + 1, null, new Point(80, 48), 1, 9, 9, WEST, 3, RandomOne.of(ThemeManager.getInstance().getThemes()), false, true, true, true, true, true, true, true, true, true, true, Inn.createName(seed));
//        garden.plantSeed(inn);
//        inn = new Inn(garden, seed + 2, null, new Point(80, 80), 1, 9, 9, NORTH, 3, RandomOne.of(ThemeManager.getInstance().getThemes()), false, true, true, true, true, true, true, true, true, true, true, Inn.createName(seed));
//        garden.plantSeed(inn);
//        inn = new Inn(garden, seed + 3, null, new Point(48, 80), 1, 9, 9, EAST, 3, RandomOne.of(ThemeManager.getInstance().getThemes()), false, true, true, true, true, true, true, true, true, true, true, Inn.createName(seed));
//        Inn inn = new Inn(garden, seed, null, new Point(32, 32), 1, 13, 12, NORTH, 4, RandomOne.of(ThemeManager.getInstance().getThemes()), false, true, true, true, true, true, true, true, true);
//        garden.plantSeed(inn);
        while (! garden.tick());
        JavaWorldExporter worldExporter = new JavaWorldExporter(world, EXPORT_EVERYTHING);
        File exportDir = new File(args[0]);
        worldExporter.export(exportDir, "TestWorld", worldExporter.selectBackupDir(exportDir, world.getName()), null);
    }
}