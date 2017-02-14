/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.tools;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import static org.pepsoft.minecraft.Constants.*;
import org.pepsoft.minecraft.Level;
import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.minecraft.RegionFile;
import org.pepsoft.util.ColourUtils;
import org.pepsoft.worldpainter.ColourScheme;
import org.pepsoft.worldpainter.DefaultPlugin;
import org.pepsoft.worldpainter.Version;
import org.pepsoft.worldpainter.colourschemes.DynMapColourScheme;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;
import org.pepsoft.worldpainter.exporting.WorldRegion;

/**
 *
 * @author pepijn
 */
public class Mapper {

    public static void main(String[] args) throws IOException, InterruptedException {
        File worldDir = null;
        int dim = 0;
        String colourSchemeName = "classic";
        File output = null;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i].trim();
            switch (arg) {
                case "-d":
                    if (i < args.length - 1) {
                        i++;
                        try {
                            dim = Integer.parseInt(args[i].trim());
                        } catch (NumberFormatException e) {
                            error("Invalid argument to -d option: \"" + args[i] + "\"");
                        }
                    } else {
                        error("Missing argument to -d option");
                    }
                    break;
                case "-c":
                    if (i < args.length - 1) {
                        i++;
                        colourSchemeName = args[i].trim();
                    } else {
                        error("Missing argument to -c option");
                    }
                    break;
                case "-o":
                    if (i < args.length - 1) {
                        i++;
                        String outputName = args[i].trim();
                        if (!outputName.toLowerCase().endsWith(".png")) {
                            error("Only PNG format suppored for output file");
                        }
                        output = new File(outputName);
                        if (output.getParentFile() != null) {
                            if (!output.isDirectory()) {
                                error("Parent directory of output file does not exist or is not a directory: \"" + output.getParentFile() + "\"");
                            } else if (!output.canWrite()) {
                                error("Parent directory of output file is not writeable: \"" + output.getParentFile() + "\"");
                            }
                        }
                    } else {
                        error("Missing argument to -o option");
                    }
                    break;
                default:
                    if (worldDir != null) {
                        error("Unrecognised option: \"" + arg + "\"");
                    } else {
                        worldDir = new File(arg);
                    }
                    break;
            }
        }
        if (worldDir == null) {
            error("Map directory not specified");
        } else if (!worldDir.isDirectory()) {
            error("Map directory does not exist or is not a directory: \"" + worldDir + "\"");
        }
        if ((dim < 0) || (dim > 2)) {
            error("Invalid dimension specified: " + dim);
        }
        System.out.println("WorldPainter Mapper tool - version " + Version.VERSION + " - © 2012 - 2014 pepsoft.org");
        ColourScheme colourScheme = new DynMapColourScheme(colourSchemeName, true);
        if (output == null) {
            output = new File(worldDir.getName().toLowerCase() + ".png");
        }
        map(worldDir, dim, colourScheme, output);
    }

    private static void map(final File worldDir, final int dim, final ColourScheme colourScheme, File output) throws IOException, InterruptedException {
        File levelDatFile = new File(worldDir, "level.dat");
        Level level = Level.load(levelDatFile);
        final Platform platform = level.getVersion() == SUPPORTED_VERSION_1 ? DefaultPlugin.JAVA_MCREGION : DefaultPlugin.JAVA_ANVIL;
        maxHeight = level.getMaxHeight();
        File dimensionDir;
        switch (dim) {
            case 0:
                dimensionDir = worldDir;
                break;
            case 1:
                dimensionDir = new File(worldDir, "DIM-1");
                break;
            case 2:
                dimensionDir = new File(worldDir, "DIM1");
                break;
            default:
                throw new IllegalArgumentException(Integer.toString(dim));
        }
        final File regionDir = new File(dimensionDir, "region");
        if (!regionDir.exists()) {
            error("Map does not have dimension " + dim);
        }
        System.out.println("Mapping " + worldDir);
        System.out.println("Name: " + level.getName());
        System.out.println("Seed: " + level.getSeed());
        if (level.getGeneratorName() != null) {
            System.out.println("Generator: " + level.getGeneratorName() + " (version " + level.getGeneratorVersion() + ")");
        }
        System.out.println("Map height: " + maxHeight);
        System.out.println("Storage format: " + (platform.equals(DefaultPlugin.JAVA_MCREGION) ? "McRegion (Minecraft 1.1 or earlier)" : "Anvil (Minecraft 1.2 or later)"));

        // Determine size
        File[] regionFiles = regionDir.listFiles(platform.equals(DefaultPlugin.JAVA_MCREGION)
            ? (dir, name) -> name.toLowerCase().endsWith(".mcr")
                : (FilenameFilter) (dir, name) -> name.toLowerCase().endsWith(".mca"));
        int tmpLowestRegionX = Integer.MAX_VALUE, tmpHighestRegionX = Integer.MIN_VALUE;
        int tmpLowestRegionZ = Integer.MAX_VALUE, tmpHighestRegionZ = Integer.MIN_VALUE;
        for (File regionFile: regionFiles) {
            String[] parts = regionFile.getName().split("\\.");
            int regionX = Integer.parseInt(parts[1]);
            int regionZ = Integer.parseInt(parts[2]);
            if (regionX < tmpLowestRegionX) {
                tmpLowestRegionX = regionX;
            }
            if (regionX > tmpHighestRegionX) {
                tmpHighestRegionX = regionX;
            }
            if (regionZ < tmpLowestRegionZ) {
                tmpLowestRegionZ = regionZ;
            }
            if (regionZ > tmpHighestRegionZ) {
                tmpHighestRegionZ = regionZ;
            }
        }
        final int lowestRegionX = tmpLowestRegionX, highestRegionX = tmpHighestRegionX;
        final int lowestRegionZ = tmpLowestRegionZ, highestRegionZ = tmpHighestRegionZ;
        int tmpLowestChunkX = Integer.MAX_VALUE, tmpHighestChunkX = Integer.MIN_VALUE;
        int tmpLowestChunkZ = Integer.MAX_VALUE, tmpHighestChunkZ = Integer.MIN_VALUE;
        for (int regionX = lowestRegionX; regionX <= highestRegionX; regionX++) {
            File file = new File(regionDir, "r." + regionX + "." + lowestRegionZ + (platform.equals(DefaultPlugin.JAVA_MCREGION) ? ".mcr" : ".mca"));
            if (file.exists()) {
                int regionChunkX = regionX << 5;
                int regionChunkZ = lowestRegionZ << 5;
                RegionFile region = new RegionFile(file);
                try {
                    for (int chunkX = 0; chunkX < 32; chunkX++) {
                        for (int chunkZ = 0; chunkZ < 32; chunkZ++) {
                            if (region.containsChunk(chunkX, chunkZ)) {
                                int x = regionChunkX + chunkX;
                                int z = regionChunkZ + chunkZ;
                                if (x < tmpLowestChunkX) {
                                    tmpLowestChunkX = x;
                                }
                                if (x > tmpHighestChunkX) {
                                    tmpHighestChunkX = x;
                                }
                                if (z < tmpLowestChunkZ) {
                                    tmpLowestChunkZ = z;
                                }
                                if (z > tmpHighestChunkZ) {
                                    tmpHighestChunkZ = z;
                                }
                            }
                        }
                    }
                } finally {
                    region.close();
                }
            }
            file = new File(regionDir, "r." + regionX + "." + highestRegionZ + (platform.equals(DefaultPlugin.JAVA_MCREGION) ? ".mcr" : ".mca"));
            if (file.exists()) {
                int regionChunkX = regionX << 5;
                int regionChunkZ = highestRegionZ << 5;
                RegionFile region = new RegionFile(file);
                try {
                    for (int chunkX = 0; chunkX < 32; chunkX++) {
                        for (int chunkZ = 0; chunkZ < 32; chunkZ++) {
                            if (region.containsChunk(chunkX, chunkZ)) {
                                int x = regionChunkX + chunkX;
                                int z = regionChunkZ + chunkZ;
                                if (x < tmpLowestChunkX) {
                                    tmpLowestChunkX = x;
                                }
                                if (x > tmpHighestChunkX) {
                                    tmpHighestChunkX = x;
                                }
                                if (z < tmpLowestChunkZ) {
                                    tmpLowestChunkZ = z;
                                }
                                if (z > tmpHighestChunkZ) {
                                    tmpHighestChunkZ = z;
                                }
                            }
                        }
                    }
                } finally {
                    region.close();
                }
            }
        }
        for (int regionZ = lowestRegionZ + 1; regionZ <= highestRegionZ - 1; regionZ++) {
            File file = new File(regionDir, "r." + lowestRegionX + "." + regionZ + (platform.equals(DefaultPlugin.JAVA_MCREGION) ? ".mcr" : ".mca"));
            if (file.exists()) {
                int regionChunkX = lowestRegionX << 5;
                int regionChunkZ = regionZ << 5;
                RegionFile region = new RegionFile(file);
                try {
                    for (int chunkX = 0; chunkX < 32; chunkX++) {
                        for (int chunkZ = 0; chunkZ < 32; chunkZ++) {
                            if (region.containsChunk(chunkX, chunkZ)) {
                                int x = regionChunkX + chunkX;
                                int z = regionChunkZ + chunkZ;
                                if (x < tmpLowestChunkX) {
                                    tmpLowestChunkX = x;
                                }
                                if (x > tmpHighestChunkX) {
                                    tmpHighestChunkX = x;
                                }
                                if (z < tmpLowestChunkZ) {
                                    tmpLowestChunkZ = z;
                                }
                                if (z > tmpHighestChunkZ) {
                                    tmpHighestChunkZ = z;
                                }
                            }
                        }
                    }
                } finally {
                    region.close();
                }
            }
            file = new File(regionDir, "r." + highestRegionX + "." + regionZ + (platform.equals(DefaultPlugin.JAVA_MCREGION) ? ".mcr" : ".mca"));
            if (file.exists()) {
                int regionChunkX = highestRegionX << 5;
                int regionChunkZ = regionZ << 5;
                RegionFile region = new RegionFile(file);
                try {
                    for (int chunkX = 0; chunkX < 32; chunkX++) {
                        for (int chunkZ = 0; chunkZ < 32; chunkZ++) {
                            if (region.containsChunk(chunkX, chunkZ)) {
                                int x = regionChunkX + chunkX;
                                int z = regionChunkZ + chunkZ;
                                if (x < tmpLowestChunkX) {
                                    tmpLowestChunkX = x;
                                }
                                if (x > tmpHighestChunkX) {
                                    tmpHighestChunkX = x;
                                }
                                if (z < tmpLowestChunkZ) {
                                    tmpLowestChunkZ = z;
                                }
                                if (z > tmpHighestChunkZ) {
                                    tmpHighestChunkZ = z;
                                }
                            }
                        }
                    }
                } finally {
                    region.close();
                }
            }
        }
        final int lowestChunkX = tmpLowestChunkX;
        final int lowestChunkZ = tmpLowestChunkZ;
        int widthChunks = (tmpHighestChunkX - tmpLowestChunkX + 1);
        int heightChunks = (tmpHighestChunkZ - tmpLowestChunkZ + 1);
        System.out.println("Width: " + (widthChunks << 4));
        System.out.println("Height: " + (heightChunks << 4));
        
        final BufferedImage image = new BufferedImage(widthChunks << 4, heightChunks << 4, BufferedImage.TYPE_INT_ARGB);
        final int imageOffsetX = lowestChunkX << 4;
        final int imageOffsetY = lowestChunkZ << 4;
        final int waterColour = colourScheme.getColour(Material.WATER);
        final int lavaColour = colourScheme.getColour(Material.LAVA);
        final int snowColour = colourScheme.getColour(Material.SNOW);
        int threads = Runtime.getRuntime().availableProcessors();
        System.out.print("Mapping");
        ExecutorService executorService = Executors.newFixedThreadPool(threads);
        for (File file: regionFiles) {
            final File finalFile = file;
            executorService.submit(() -> {
                try {
                    String[] parts = finalFile.getName().split("\\.");
                    int regionX = Integer.parseInt(parts[1]);
                    int regionY = Integer.parseInt(parts[2]);
                    WorldRegion world = new WorldRegion(worldDir, dim, regionX, regionY, maxHeight, platform);
                    int[][] heightCache = new int[544][544];
                    for (int i = 0; i < 544; i++) {
                        Arrays.fill(heightCache[i], -1);
                    }
                    for (int chunkX = 0; chunkX < 32; chunkX++) {
                        for (int chunkY = 0; chunkY < 32; chunkY++) {
                            int worldChunkX = (regionX << 5) | chunkX;
                            int worldChunkY = (regionY << 5) | chunkY;
                            if (world.isChunkPresent(worldChunkX, worldChunkY)) {
                                for (int x = 0; x < 16; x++) {
                                    for (int y = 0; y < 16; y++) {
                                        int worldX = (worldChunkX << 4) | x;
                                        int worldY = (worldChunkY << 4) | y;
                                        boolean snow = false, water = false, lava = false;
                                        int waterLevel = 0;
                                        for (int height = maxHeight - 1; height >= 0; height--) {
                                            int blockType = world.getBlockTypeAt(worldX, worldY, height);
                                            if (blockType != BLK_AIR) {
                                                if (blockType == BLK_SNOW) {
                                                    snow = true;
                                                } else if ((blockType == BLK_STATIONARY_WATER) || (blockType == BLK_WATER) || (blockType == BLK_STATIONARY_LAVA) || (blockType == BLK_LAVA)) {
                                                    if ((world.getDataAt(worldX, worldY, height) == 0) && (waterLevel == 0)) {
                                                        waterLevel = height;
                                                        if ((blockType == BLK_LAVA) || (blockType == BLK_STATIONARY_LAVA)) {
                                                            lava = true;
                                                        } else {
                                                            water = true;
                                                        }
                                                    }
                                                } else if (TERRAIN_BLOCKS.contains(blockType)) {
                                                    // Terrain found
                                                    int data = world.getDataAt(worldX, worldY, height);
                                                    int depth = waterLevel - height;
                                                    int fluidAlpha = 0xff >> Math.min(depth, 3);
                                                    int colour = colourScheme.getColour(blockType, data);
                                                    if (depth > 0) {
                                                        colour = ColourUtils.multiply(colour, getBrightenAmount(world, heightCache, ((chunkX + 1) << 4) | x, ((chunkY + 1) << 4) | y, regionX, regionY));
                                                    }
                                                    if (water) {
                                                        colour = ColourUtils.mix(colour, waterColour, fluidAlpha);
                                                    } else if (lava) {
                                                        colour = ColourUtils.mix(colour, lavaColour, fluidAlpha);
                                                    }
                                                    if (snow) {
                                                        colour = ColourUtils.mix(colour, snowColour, 64);
                                                    }
                                                    if (depth <= 0) {
                                                        colour = ColourUtils.multiply(colour, getBrightenAmount(world, heightCache, ((chunkX + 1) << 4) | x, ((chunkY + 1) << 4) | y, regionX, regionY));
                                                    }
                                                    image.setRGB(worldX - imageOffsetX, worldY - imageOffsetY, 0xff000000 | colour);
                                                    break;
                                                } else {
                                                    // Non-terrain block found (not shaded)
                                                    int data = world.getDataAt(worldX, worldY, height);
                                                    int depth = waterLevel - height;
                                                    int fluidAlpha = 0xff >> Math.min(depth, 3);
                                                    int colour = colourScheme.getColour(blockType, data);
                                                    if (water) {
                                                        colour = ColourUtils.mix(colour, waterColour, fluidAlpha);
                                                    } else if (lava) {
                                                        colour = ColourUtils.mix(colour, lavaColour, fluidAlpha);
                                                    }
                                                    if (snow) {
                                                        colour = ColourUtils.mix(colour, snowColour);
                                                    }
                                                    image.setRGB(worldX - imageOffsetX, worldY - imageOffsetY, 0xff000000 | colour);
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    System.out.print('.');
                    System.out.flush();
                } catch (Throwable t) {
                    t.printStackTrace();
                    System.exit(1);
                }
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.DAYS);
        System.out.println();
        
        // Save image
        System.out.println("Saving image to " + output + "...");
        ImageIO.write(image, "PNG", output);
        
        System.out.println("Finished");
    }

    private static void error(String message) {
        System.err.println(message);
        System.exit(2);
    }
    
    private static int getBrightenAmount(MinecraftWorld world, int[][] heightCache, int x, int y, int regionX, int regionY) {
        return Math.max(0, ((getHeight(world, heightCache, x + 1, y, regionX, regionY) - getHeight(world, heightCache, x - 1, y, regionX, regionY) + getHeight(world, heightCache, x, y + 1, regionX, regionY) - getHeight(world, heightCache, x, y - 1, regionX, regionY)) << 5) + 256);
    }
    
    private static int getHeight(MinecraftWorld world, int[][] heightCache, int x, int y, int regionX, int regionY) {
        if (heightCache[x][y] == -1) {
            int worldX = (regionX << 9) + x - 16;
            int worldY = (regionY << 9) + y - 16;
            for (int height = maxHeight - 1; height >= 0; height--) {
                if (TERRAIN_BLOCKS.contains(world.getBlockTypeAt(worldX, worldY, height))) {
                    heightCache[x][y] = height;
                    return height;
                }
            }
            heightCache[x][y] = 62;
        }
        return heightCache[x][y];
    }
    
//    private static JavaMinecraftWorld world;
    private static int maxHeight;
    private static final Set<Integer> TERRAIN_BLOCKS = new HashSet<>();
    
    static {
        TERRAIN_BLOCKS.add(BLK_STONE);
        TERRAIN_BLOCKS.add(BLK_GRASS);
        TERRAIN_BLOCKS.add(BLK_DIRT);
        TERRAIN_BLOCKS.add(BLK_BEDROCK);
        TERRAIN_BLOCKS.add(BLK_SAND);
        TERRAIN_BLOCKS.add(BLK_GRAVEL);
        TERRAIN_BLOCKS.add(BLK_GOLD_ORE);
        TERRAIN_BLOCKS.add(BLK_IRON_ORE);
        TERRAIN_BLOCKS.add(BLK_COAL);
        TERRAIN_BLOCKS.add(BLK_LAPIS_LAZULI_ORE);
        TERRAIN_BLOCKS.add(BLK_SANDSTONE);
        TERRAIN_BLOCKS.add(BLK_OBSIDIAN);
        TERRAIN_BLOCKS.add(BLK_DIAMOND_ORE);
        TERRAIN_BLOCKS.add(BLK_TILLED_DIRT);
        TERRAIN_BLOCKS.add(BLK_REDSTONE_ORE);
        TERRAIN_BLOCKS.add(BLK_GLOWING_REDSTONE_ORE);
        TERRAIN_BLOCKS.add(BLK_SNOW_BLOCK);
        TERRAIN_BLOCKS.add(BLK_CLAY);
        TERRAIN_BLOCKS.add(BLK_NETHERRACK);
        TERRAIN_BLOCKS.add(BLK_SOUL_SAND);
        TERRAIN_BLOCKS.add(BLK_MYCELIUM);
    }
}
