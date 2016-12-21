package org.pepsoft.worldpainter.tools;

import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.layers.Layer;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * Created by Pepijn Schmitz on 21-09-15.
 */
public class DumpWorld {
    public static void main(String[] args) throws IOException, UnloadableWorldException {
        WorldIO worldIO = new WorldIO();
        worldIO.load(new FileInputStream(args[0]));
        World2 world = worldIO.getWorld();
        System.out.println("Name: " + world.getName());
        if (world.getMetadata() != null) {
            world.getMetadata().entrySet().forEach(entry -> System.out.println("   " + entry.getKey() + ": " + entry.getValue()));
        }
        System.out.println("History:");
        world.getHistory().forEach(entry -> System.out.println("    " + entry.getText()));
        if (world.getPlatform() != null) {
            System.out.printf("Last exported for platform: %s%n", world.getPlatform().getDisplayName());
        }
        System.out.println("Generator: " + world.getGenerator());
        if (world.getGeneratorOptions() != null) {
            System.out.println("Generator options: " + world.getGeneratorOptions());
        }
        System.out.println("Game type: " + world.getGameType());
        System.out.println("Difficulty: " + DIFFICULTIES[world.getDifficulty()]);
        if (world.getImportedFrom() != null) {
            System.out.println("Imported from: " + world.getImportedFrom());
        }
        System.out.println("Max height: " + world.getMaxHeight());
        System.out.println("Spawn point: " + world.getSpawnPoint().y + "," + world.getSpawnPoint().y);
        System.out.println("Up is: " + world.getUpIs());

        boolean headerPrinted = false;
        for (int i = 0; i < Terrain.CUSTOM_TERRAIN_COUNT; i++) {
            MixedMaterial customTerrain = world.getMixedMaterial(i);
            if (customTerrain != null) {
                if (! headerPrinted) {
                    System.out.println("Custom terrains installed:");
                    headerPrinted = true;
                }
                System.out.println("    Custom " + (i + 1));
                System.out.println("        Name: " + customTerrain.getName());
                System.out.println("        Mode: " + customTerrain.getMode());
                Arrays.stream(customTerrain.getRows()).forEach(row -> System.out.println("        Material: " + row));
            }
        }
        if (! headerPrinted) {
            System.out.println("No custom terrains installed");
        }

        for (Dimension dimension: world.getDimensions()) {
            dumpDimension(dimension);
        }
    }

    private static void dumpDimension(Dimension dimension) {
        System.out.println("Dimension: " + dimension.getName() + " (index: " + dimension.getDim() + ")");
        System.out.println("    Size: " + dimension.getWidth() + "x" + dimension.getHeight() + " tiles");
        System.out.println("    Westernmost tile: " + dimension.getLowestX() + "; easternmost tile: " + dimension.getHighestX());
        System.out.println("    Northernmost tile: " + dimension.getLowestY() + "; southernmost tile: " + dimension.getHighestY());
        System.out.println("    Total number of tiles: " + dimension.getTileCount());
        System.out.println("    WorldPainter seed: " + dimension.getSeed() + "; Minecraft seed: " + dimension.getMinecraftSeed());
        if (dimension.getBorder() != null) {
            switch (dimension.getBorder()) {
                case LAVA:
                case WATER:
                    System.out.println("    Border: " + dimension.getBorder() + " (size: " + dimension.getBorderSize() + "; level; " + dimension.getBorderLevel() + ")");
                    break;
                case VOID:
                    System.out.println("    Border: VOID (size: " + dimension.getBorderSize() + ")");
                    break;
            }
        } else {
            System.out.println("    Border: none");
        }
        if ((dimension.getDim() == Constants.DIM_NORMAL_CEILING) || (dimension.getDim() == Constants.DIM_NETHER_CEILING) || (dimension.getDim() == Constants.DIM_END_CEILING)) {
            System.out.println("    Ceiling height: " + dimension.getCeilingHeight());
        }
        System.out.println("    Max height: " + dimension.getMaxHeight());
        System.out.println("    Contour separation: " + dimension.getContourSeparation());
        System.out.println("    Grid size: " + dimension.getGridSize());
        System.out.println("    Last view position: " + dimension.getLastViewPosition().x + "," + dimension.getLastViewPosition().y);
        if (dimension.getOverlay() != null) {
            System.out.println("    Overlay image: " + dimension.getOverlay());
            System.out.println("        Offset: " + dimension.getOverlayOffsetX() + "," + dimension.getOverlayOffsetY());
            System.out.println("        Scale:" + dimension.getOverlayScale());
            System.out.println("        Transparency: " + dimension.getOverlayTransparency());
        } else {
            System.out.println("    Overlay image: none");
        }
        System.out.println("    Subfurface material: " + dimension.getSubsurfaceMaterial());
        System.out.println("    Top layer depth: " + dimension.getTopLayerMinDepth() + " - " + (dimension.getTopLayerMinDepth() + dimension.getTopLayerVariation()));

        Map<Layer, Integer> usedLayers = new HashMap<>();
        EnumSet<Terrain> terrainsUsed = EnumSet.noneOf(Terrain.class);
        float lowestSurface = Float.MAX_VALUE, highestSurface = Float.MIN_VALUE;
        int lowestWaterlevel = Integer.MAX_VALUE, highestWaterlevel = Integer.MIN_VALUE;
        for (Tile tile: dimension.getTiles()) {
            for (Layer layer: tile.getLayers()) {
                Integer count = usedLayers.get(layer);
                if (count == null) {
                    usedLayers.put(layer, 1);
                } else {
                    usedLayers.put(layer, count + 1);
                }
            }
            for (int x = 0; x < Constants.TILE_SIZE; x++) {
                for (int y = 0; y < Constants.TILE_SIZE; y++) {
                    terrainsUsed.add(tile.getTerrain(x, y));
                    float height = tile.getHeight(x, y);
                    if (height < lowestSurface) {
                        lowestSurface = height;
                    }
                    if (height > highestSurface) {
                        highestSurface = height;
                    }
                    int waterLevel = tile.getWaterLevel(x, y);
                    if (waterLevel < lowestWaterlevel) {
                        lowestWaterlevel = waterLevel;
                    }
                    if (waterLevel > highestWaterlevel) {
                        highestWaterlevel = waterLevel;
                    }
                }
            }
        }
        System.out.println("    Terrain heights: " + lowestSurface + " - " + highestSurface);
        System.out.println("    Water levels: " + lowestWaterlevel + " - " + highestWaterlevel);
        if ((dimension.getCustomBiomes() != null) && (! dimension.getCustomBiomes().isEmpty())) {
            System.out.println("    Custom biomes installed:");
            dimension.getCustomBiomes().forEach(customBiome -> System.out.println("        " + customBiome.getName() + " (" + customBiome.getId() + ")"));
        } else {
            System.out.println("    No custom biomes installed");
        }
        System.out.println("    Layers used:");
        usedLayers.entrySet().forEach(entry -> {
            Layer layer = entry.getKey();
            System.out.println("        Name: " + layer.getName());
            System.out.println("            Type: " + layer.getClass().getSimpleName());
            System.out.println("            Data size: " + layer.getDataSize());
            System.out.println("            Tile count: " + entry.getValue());
        });
        Set<Layer> unusedLayers = new HashSet<>(dimension.getCustomLayers());
        unusedLayers.removeAll(usedLayers.keySet());
        if (! unusedLayers.isEmpty()) {
            System.out.println("    Layers stored but not in use:");
            unusedLayers.forEach(layer -> {
                System.out.println("        Name: " + layer.getName());
                System.out.println("            Type: " + layer.getClass().getSimpleName());
                System.out.println("            Data size: " + layer.getDataSize());
            });
        }

        System.out.println("    Terrain types used:");
        terrainsUsed.forEach(terrain -> System.out.println("        " + terrain.getName() + " (index: " + terrain.ordinal() + ")"));

        List<String> problems = new ArrayList<>();
        if (dimension.getMaxHeight() != dimension.getWorld().getMaxHeight()) {
            problems.add("Dimension max height (" + dimension.getMaxHeight() + ") does not equal the world max height (" + dimension.getWorld().getMaxHeight() + ")");
        }
        for (Terrain terrain: terrainsUsed) {
            if (terrain.isCustom() && (! terrain.isConfigured())) {
                problems.add("Unconfigured custom terrain type " + terrain.getName() + " (index: " + terrain.ordinal() + ") encountered");
            }
        }
        if (! problems.isEmpty()) {
            System.out.println("    PROBLEMS:");
            problems.forEach(problem -> System.out.println("        " + problem));
        }

        // TODO: layer settings

        // TODO: tile factory
    }

    private static final String[] GAME_TYPES = {"SURVIVAL", "CREATIVE", "ADVENTURE", "HARDCORE"};
    private static final String[] DIFFICULTIES = {"PEACEFUL", "EASY", "NORMAL", "HARD"};
}